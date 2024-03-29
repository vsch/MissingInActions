// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.vladsch.MissingInActions.actions.ActionUtils;
import com.vladsch.MissingInActions.actions.CaretOffsetPreserver;
import com.vladsch.MissingInActions.manager.CaretUtils;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.vladsch.MissingInActions.manager.CaretUtils.getCoordinates;

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

    public boolean isBackwards() {
        return myBackwards;
    }

    protected abstract boolean isLineMode();

    protected abstract boolean isSingleLine();

    protected abstract boolean wantEmptyRanges();

    /**
     * update carets
     *
     * @param editor    editor
     * @param caretList caret list
     *
     * @return true if can set primary top closest it was before selection search, false to leave all alone
     */
    protected abstract boolean updateCarets(final Editor editor, final List<Caret> caretList);

    // gives the handler opportunity to analyze current context and adjust line mode/single line and other values
    protected abstract void analyzeContext(final Editor editor, final @Nullable Caret caret, @NotNull LineSelectionManager manager);

    // execute pattern match
    protected abstract boolean perform(@NotNull LineSelectionManager manager, @NotNull Caret caret, @NotNull Range range, @NotNull ArrayList<CaretState> createCarets);

    protected abstract String getPattern();

    protected abstract void setPattern(String pattern);

    protected abstract Caret getPatternCaret();

    protected abstract void preparePattern(@NotNull LineSelectionManager manager, @NotNull Caret caret, @NotNull Range range, @NotNull BasedSequence chars);

    @Override
    public void doExecute(@NotNull final Editor editor, final @Nullable Caret caret, final DataContext dataContext) {
        final LineSelectionManager manager = LineSelectionManager.getInstance(editor);

        analyzeContext(editor, caret, manager);

        manager.guard(() -> {
            doAction(manager, editor, caret, getPatternCaret());
        });
    }

    public void doAction(final LineSelectionManager manager, final Editor editor, final @Nullable Caret editCaret, @Nullable final Caret patternCaret) {
        Caret useCaret = editCaret;
        ArrayList<CaretState> createList = new ArrayList<>();
        Map<Long, Caret> keptCarets = new LinkedHashMap<>();
        CaretModel caretModel = editor.getCaretModel();

        if (!caretModel.supportsMultipleCarets()) {
            if (useCaret == null) useCaret = caretModel.getCurrentCaret();
            Range range = EditHelpers.getCaretRange(useCaret, myBackwards, isLineMode(), isSingleLine());
            if (range != null) perform(manager, useCaret, range, createList);
        } else {
            List<Caret> caretList;
            boolean removePrimary;
            boolean removedPrimary = false;
            Caret primaryCaret;

            caretList = caretModel.getAllCarets();
            primaryCaret = caretModel.getPrimaryCaret();
            CaretOffsetPreserver preserver = new CaretOffsetPreserver(primaryCaret.getOffset());
            Map<Caret, Range> caretRanges = new HashMap<>();

            for (Caret caret : caretList) {
                Range range = EditHelpers.getCaretRange(caret, myBackwards, isLineMode(), isSingleLine());
                caretRanges.put(caret, range);
            }

            caretRanges = EditHelpers.limitCaretRange(myBackwards, caretRanges, wantEmptyRanges());

            if (useCaret == null) {
                if (patternCaret != null) {
                    Range caretRange = caretRanges.get(patternCaret);
                    if (caretRange != null) {
                        final BasedSequence chars = BasedSequence.of(editor.getDocument().getCharsSequence());
                        preparePattern(manager, patternCaret, caretRange, chars);
                    }
                }

                // here we adjust
                for (Caret caret : caretList) {
                    Range range = caretRanges.get(caret);
                    if (range == null) continue;

                    if (perform(manager, caret, range, createList)) {
                        keptCarets.put(getCoordinates(caret), caret);
                    }
                }
            } else {
                caretList = Collections.singletonList(useCaret);
                primaryCaret = useCaret;
                Range range = caretRanges.get(useCaret);

                if (range != null) {
                    if (perform(manager, useCaret, range, createList)) {
                        keptCarets.put(getCoordinates(useCaret), useCaret);
                    }
                }
            }

            removePrimary = !keptCarets.containsKey(CaretUtils.getCoordinates(primaryCaret));
            List<Caret> createdCarets = new ArrayList<>();

            if (keptCarets.isEmpty() && createList.isEmpty()) {
                // remove all but primary
                caretModel.removeSecondaryCarets();
            } else {
                // create new carets
                for (CaretState caretState : createList) {
                    LogicalPosition caretPosition = caretState.getCaretPosition();
                    if (caretPosition != null) {
                        Caret newCaret = removePrimary ? primaryCaret : caretModel.addCaret(editor.logicalToVisualPosition(caretPosition));
                        if (newCaret != null) {
                            EditHelpers.restoreState(newCaret, caretState, false);
                            removePrimary = false;
                            createdCarets.add(newCaret);
                        } else {
                            // caret already exists, we add that one
                            if (keptCarets.containsKey(getCoordinates(caretPosition))) {
                                createdCarets.add(keptCarets.get(getCoordinates(caretPosition)));
                            }
                        }
                    }
                }

                if (removePrimary) {
                    // move primary to first kept and remove first
                    Caret firstCaret = keptCarets.values().iterator().next();
                    primaryCaret.moveToLogicalPosition(firstCaret.getLogicalPosition());
                    primaryCaret.setSelection(firstCaret.getSelectionStart(), firstCaret.getSelectionEnd());
                    keptCarets.remove(CaretUtils.getCoordinates(firstCaret));
                }

                // keep only ones in list
                for (Caret caret1 : caretList) {
                    if (!keptCarets.containsKey(CaretUtils.getCoordinates(caret1))) {
                        caretModel.removeCaret(caret1);
                    }
                }
            }

            if (updateCarets(editor, createdCarets)) {
                for (Caret caret : editor.getCaretModel().getAllCarets()) {
                    preserver.tryCaret(caret);
                }

                int matchedIndex = preserver.getMatchedIndex();
                ActionUtils.setPrimaryCaretIndex(editor, matchedIndex, false);
            }
        }
    }
}
