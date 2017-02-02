/*
 * Copyright (c) 2016-2017 Vladimir Schneider <vladimir.schneider@gmail.com>
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.BasedSequenceImpl;
import com.vladsch.flexmark.util.sequence.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * These limit their range of effect within their own selection, start/end of line, or the next/prev caret location
 */

abstract public class RangeLimitedCaretSpawningHandler extends EditorActionHandler {
    protected final boolean myBackwards;

    /**
     * Create a range limited caret spawning handler
     *
     * @param backwards search is backwards from caret offset
     */
    public RangeLimitedCaretSpawningHandler(boolean backwards) {
        super(false);
        myBackwards = backwards;
    }

    public abstract void caretsChanged(final Editor editor);

    protected abstract boolean isLineMode();
    protected abstract boolean isSingleLine();

    protected abstract void updateCarets(final Editor editor, final List<Caret> caretList);

    // gives the handler opportunity to analyze current context and adjust line mode/single line and other values
    protected abstract void analyzeContext(final Editor editor, final @Nullable Caret caret, @NotNull LineSelectionManager manager);

    // execute pattern match
    protected abstract boolean perform(@NotNull LineSelectionManager manager, @NotNull Caret caret, @NotNull Range range, @NotNull ArrayList<CaretState> createCarets);

    protected abstract Caret getPatternCaret();
    protected abstract void preparePattern(@NotNull LineSelectionManager manager, @NotNull Caret caret, @NotNull Range range, @NotNull BasedSequence chars);

    @Override
    public void doExecute(final Editor editor, final @Nullable Caret caret, final DataContext dataContext) {
        final LineSelectionManager manager = LineSelectionManager.getInstance(editor);

        analyzeContext(editor, caret, manager);

        manager.guard(() -> {
            doAction(manager, editor, caret, getPatternCaret());
        });
    }

    public void doAction(final LineSelectionManager manager, final Editor editor, final @Nullable Caret caret, @Nullable final Caret patternCaret) {
        Caret useCaret = caret;
        ArrayList<CaretState> createList = new ArrayList<>();
        HashSet<Caret> keptCarets = new HashSet<>();
        CaretModel caretModel = editor.getCaretModel();

        if (!caretModel.supportsMultipleCarets()) {
            if (useCaret == null) useCaret = caretModel.getCurrentCaret();
            Range range = EditHelpers.getCaretRange(useCaret, myBackwards, isLineMode(), isSingleLine());
            if (range != null) perform(manager, useCaret, range, createList);
        } else {
            List<Caret> caretList;
            boolean removePrimary;
            Caret primaryCaret;

            caretList = caretModel.getAllCarets();
            primaryCaret = caretModel.getPrimaryCaret();
            Map<Caret, Range> caretRanges = new HashMap<>();

            for (Caret caret1 : caretList) {
                Range range = EditHelpers.getCaretRange(caret1, myBackwards, isLineMode(), isSingleLine());
                caretRanges.put(caret1,range);
            }

            caretRanges = EditHelpers.limitCaretRange(myBackwards, caretRanges);

            if (useCaret == null) {
                if (patternCaret != null) {
                    final BasedSequence chars = BasedSequenceImpl.of(editor.getDocument().getCharsSequence());
                    preparePattern(manager, patternCaret, caretRanges.get(patternCaret), chars);
                }

                // here we adjust
                for (Caret caret1 : caretList) {
                    Range range = caretRanges.get(caret1);
                    if (range == null) continue;

                    if (perform(manager, caret1, range, createList)) {
                        keptCarets.add(caret1);
                    }
                }
            } else {
                caretList = Collections.singletonList(useCaret);
                primaryCaret = useCaret;
                Range range = caretRanges.get(useCaret);

                if (range != null) {
                    if (perform(manager, useCaret, range, createList)) {
                        keptCarets.add(useCaret);
                    }
                }
            }

            removePrimary = !keptCarets.contains(primaryCaret);

            List<Caret> createdCarets = new ArrayList<>();

            if (keptCarets.isEmpty() && createList.isEmpty()) {
                // remove all but primary
                caretModel.removeSecondaryCarets();
            } else {
                // create new carets
                for (CaretState caretState : createList) {
                    LogicalPosition caretPosition = caretState.getCaretPosition();
                    if (caretPosition != null) {
                        Caret newCaret = removePrimary ? primaryCaret : caretModel.addCaret(caretPosition.toVisualPosition());
                        if (newCaret != null) {
                            EditHelpers.restoreState(newCaret, caretState, false);
                            removePrimary = false;

                            createdCarets.add(newCaret);
                        } else {
                            // caret already exists, we add that one
                            for (Caret caret1 : keptCarets) {
                                if (caretPosition.equals(caret1.getLogicalPosition())) {
                                    createdCarets.add(caret1);
                                    break;
                                }
                            }
                        }
                    }
                }

                if (removePrimary) {
                    // move primary to first kept and remove first
                    Caret firstCaret = keptCarets.iterator().next();
                    primaryCaret.moveToLogicalPosition(firstCaret.getLogicalPosition());
                    primaryCaret.setSelection(firstCaret.getSelectionStart(), firstCaret.getSelectionEnd());
                    keptCarets.remove(firstCaret);
                    removePrimary = false;
                }

                // keep only ones in list
                for (Caret caret1 : caretList) {
                    if (!keptCarets.contains(caret1)) {
                        caretModel.removeCaret(caret1);
                    }
                }
            }

            updateCarets(editor, createdCarets);
        }
    }
}
