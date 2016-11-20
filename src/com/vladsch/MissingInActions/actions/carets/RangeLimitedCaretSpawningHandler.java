/*
 * Copyright (c) 2016-2016 Vladimir Schneider <vladimir.schneider@gmail.com>
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

package com.vladsch.MissingInActions.actions.carets;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.flexmark.util.sequence.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * These limit their range of effect withing their own selection, start/end of line, or the next/prev caret location
 */

abstract public class RangeLimitedCaretSpawningHandler extends EditorActionHandler {
    protected final boolean myBackwards;
    protected final boolean myLineMode;
    protected final boolean mySingleLine;

    public RangeLimitedCaretSpawningHandler(boolean backwards, boolean lineMode, boolean singleLine) {
        super(false);
        myBackwards = backwards;
        myLineMode = lineMode;
        mySingleLine = singleLine;
    }

    // execute pattern match 
    abstract protected boolean perform(@NotNull LineSelectionManager manager, @NotNull Caret caret, @NotNull Range range, @NotNull ArrayList<CaretState> createCarets);

    @Override
    public void doExecute(final Editor editor, final @Nullable Caret caret, final DataContext dataContext) {
        final LineSelectionManager manager = LineSelectionManager.getInstance(editor);

        manager.guard(() -> {
            Caret useCaret = caret;
            ArrayList<CaretState> createList = new ArrayList<>();
            HashSet<Caret> keptCarets = new HashSet<>();
            CaretModel caretModel = editor.getCaretModel();

            if (!caretModel.supportsMultipleCarets()) {
                if (useCaret == null) useCaret = caretModel.getCurrentCaret();
                perform(manager, useCaret, EditHelpers.getCaretRange(useCaret, myBackwards, myLineMode, mySingleLine), createList);
            } else {
                List<Caret> caretList;
                boolean removePrimary;
                Caret primaryCaret;

                if (useCaret == null) {
                    // here we adjust            
                    caretList = caretModel.getAllCarets();
                    primaryCaret = caretModel.getPrimaryCaret();

                    for (Caret caret1 : caretList) {
                        if (perform(manager, caret1, EditHelpers.getCaretRange(caret1, myBackwards, myLineMode, mySingleLine), createList)) {
                            keptCarets.add(caret1);
                        }
                    }
                    removePrimary = !keptCarets.contains(primaryCaret);
                } else {
                    caretList = Collections.singletonList(useCaret);
                    primaryCaret = useCaret;
                    removePrimary = true;
                    if (perform(manager, useCaret, EditHelpers.getCaretRange(useCaret, myBackwards, myLineMode, mySingleLine), createList)) {
                        removePrimary = false;
                        keptCarets.add(useCaret);
                    }
                }

                if (keptCarets.isEmpty() && createList.isEmpty()) {
                    // remove all but primary
                    caretModel.removeSecondaryCarets();
                } else {
                    // create new carets
                    for (CaretState caretState : createList) {
                        LogicalPosition caretPosition = caretState.getCaretPosition();
                        if (caretPosition != null) {
                            Caret newCaret = removePrimary ? primaryCaret : caretModel.addCaret(caretPosition.toVisualPosition());
                            EditHelpers.restoreState(newCaret, caretState, false);
                            removePrimary = false;
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
            }
        });
    }
}
