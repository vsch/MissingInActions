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

package com.vladsch.MissingInActions.actions;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.keymap.impl.ModifierKeyDoubleClickHandler;
import com.intellij.openapi.util.registry.Registry;
import com.vladsch.MissingInActions.util.LineSelectionAdjuster;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MoveCaretLineLeftOrRightHandler extends EditorActionHandler {
    private final boolean myMoveRight;
    final boolean myWithSelection;

    public MoveCaretLineLeftOrRightHandler(boolean moveRight, boolean withSelection) {
        super(true);
        myMoveRight = moveRight;
        myWithSelection = withSelection;
    }

    @Override
    protected boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
        return !myWithSelection ||
                !ModifierKeyDoubleClickHandler.getInstance().isRunningAction() ||
                EditorSettingsExternalizable.getInstance().addCaretsOnDoubleCtrl();
    }

    @Override
    protected void doExecute(Editor editor, @Nullable Caret caret, DataContext dataContext) {
        final LineSelectionAdjuster adjuster = LineSelectionAdjuster.getInstance(editor);

        adjuster.guard(() -> {
            if (!editor.getCaretModel().supportsMultipleCarets()) {
                perform(editor, adjuster, caret);
            } else {
                if (caret == null) {
                    editor.getCaretModel().runForEachCaret(caret1 -> perform(editor, adjuster, caret1));
                } else {
                    perform(editor, adjuster, caret);
                }
            }
        });
    }

    private void perform(Editor editor, LineSelectionAdjuster adjuster, Caret caret) {
        assert caret != null;

        adjuster.adjustLineSelectionToCharacterSelection(caret, !myWithSelection);

        if (myWithSelection) {
            int offset = caret.getOffset();
            VisualPosition currentPosition = caret.getVisualPosition();
            if (caret.isAtBidiRunBoundary() && (myMoveRight ^ currentPosition.leansRight)) {
                int selectionStartToUse = caret.getLeadSelectionOffset();
                VisualPosition selectionStartPositionToUse = caret.getLeadSelectionPosition();
                caret.moveToVisualPosition(currentPosition.leanRight(!currentPosition.leansRight));
                caret.setSelection(selectionStartPositionToUse, selectionStartToUse, caret.getVisualPosition(), offset);
            } else {
                editor.getCaretModel().moveCaretRelatively(myMoveRight ? 1 : -1, 0, true, editor.isColumnMode(),
                        caret == editor.getCaretModel().getPrimaryCaret());
            }
        } else {
            final SelectionModel selectionModel = editor.getSelectionModel();
            final CaretModel caretModel = editor.getCaretModel();
            ScrollingModel scrollingModel = editor.getScrollingModel();

            if (selectionModel.hasSelection() && (!(editor instanceof EditorEx) || !((EditorEx) editor).isStickySelection())) {
                int start = selectionModel.getSelectionStart();
                int end = selectionModel.getSelectionEnd();
                int caretOffset = caretModel.getOffset();

                if (start <= caretOffset && end >= caretOffset) { // See IDEADEV-36957

                    VisualPosition targetPosition = null;
                    if (Registry.is("editor.new.rendering")) {
                        targetPosition = myMoveRight ? caret.getSelectionEndPosition() : caret.getSelectionStartPosition();
                    } else if (caretModel.supportsMultipleCarets() && editor.isColumnMode()) {
                        targetPosition = myMoveRight ?
                                selectionModel.getSelectionEndPosition() : selectionModel.getSelectionStartPosition();
                    }

                    selectionModel.removeSelection();
                    if (targetPosition != null) {
                        caretModel.moveToVisualPosition(targetPosition);
                    } else {
                        caretModel.moveToOffset(myMoveRight ^ caret.isAtRtlLocation() ? end : start);
                    }
                    if (caret == editor.getCaretModel().getPrimaryCaret()) {
                        scrollingModel.scrollToCaret(ScrollType.RELATIVE);
                    }
                    return;
                }
            }
            VisualPosition currentPosition = caret.getVisualPosition();
            if (caret.isAtBidiRunBoundary() && (myMoveRight ^ currentPosition.leansRight)) {
                caret.moveToVisualPosition(currentPosition.leanRight(!currentPosition.leansRight));
            } else {
                final boolean scrollToCaret = (!(editor instanceof EditorImpl) || ((EditorImpl) editor).isScrollToCaret())
                        && caret == editor.getCaretModel().getPrimaryCaret();
                caretModel.moveCaretRelatively(myMoveRight ? 1 : -1, 0, false, false, scrollToCaret);
            }
        }
    }
}
