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

package com.vladsch.MissingInActions.actions.line;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Pair;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.MissingInActions.manager.EditorPosition;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.util.ClipboardCaretContent;
import com.vladsch.MissingInActions.util.EditHelpers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DuplicateForClipboardCaretsActionHandler extends EditorWriteActionHandler {
    public DuplicateForClipboardCaretsActionHandler() {
        super(false);
    }

    public static Couple<Integer> duplicateLineOrSelectedBlockAtCaret(Editor editor, final Document document, @NotNull Caret caret, final boolean moveCaret) {
        if (caret.hasSelection()) {
            int start = caret.getSelectionStart();
            int end = caret.getSelectionEnd();
            String s = document.getCharsSequence().subSequence(start, end).toString();
            document.insertString(end, s);
            if (moveCaret) {
                // select newly copied lines and move there
                caret.moveToOffset(end + s.length());
                editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                caret.removeSelection();
                caret.setSelection(end, end + s.length());
            }
            return Couple.of(end, end + s.length());
        } else {
            return duplicateLinesRange(editor, document, caret, caret.getVisualPosition(), caret.getVisualPosition(), moveCaret);
        }
    }

    @SuppressWarnings("WeakerAccess")
    @Nullable
    public static Couple<Integer> duplicateLinesRange(Editor editor, Document document, @NotNull Caret caret, VisualPosition rangeStart, VisualPosition rangeEnd, boolean moveCaret) {
        Pair<LogicalPosition, LogicalPosition> lines = EditorUtil.calcSurroundingRange(editor, rangeStart, rangeEnd);
        int offset = caret.getOffset();

        LogicalPosition lineStart = lines.first;
        LogicalPosition nextLineStart = lines.second;
        int start = editor.logicalPositionToOffset(lineStart);
        int end = editor.logicalPositionToOffset(nextLineStart);
        if (end <= start) {
            return null;
        }
        String s = document.getCharsSequence().subSequence(start, end).toString();
        final int lineToCheck = nextLineStart.line - 1;

        int newOffset = end + offset - start;
        if (lineToCheck == document.getLineCount() /* empty document */
                || lineStart.line == document.getLineCount() - 1 /* last line*/
                || document.getLineSeparatorLength(lineToCheck) == 0) {
            s = "\n" + s;
            newOffset++;
        }
        document.insertString(end, s);

        if (moveCaret) {
            caret.moveToOffset(newOffset);

            editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        }
        return Couple.of(end, end + s.length());
    }

    @Override
    public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
        if (editor.getCaretModel().getCaretCount() > 1) {
            // already multi-caret, just execute for each caret
            editor.getCaretModel().runForEachCaret(caret1 -> {
                duplicateLineOrSelectedBlockAtCaret(editor, editor.getDocument(), caret1, true);
            });
        } else {
            ClipboardCaretContent clipboardCaretContent = ClipboardCaretContent.studyClipboard(editor, dataContext);
            LineSelectionManager manager = LineSelectionManager.getInstance(editor);
            EditorCaret editorCaret = manager.getEditorCaret(editor.getCaretModel().getPrimaryCaret());
            int iMax = clipboardCaretContent == null ? 1 : clipboardCaretContent.getCaretCount();
            List<Couple<Integer>> copies = new ArrayList<>(iMax);

            int selectionSize = editorCaret.getSelectionEnd().getOffset() - editorCaret.getSelectionStart().getOffset();
            boolean isStartAnchor = editorCaret.isStartAnchor();

            if (editorCaret.hasLines() || editorCaret.isLine()) {
                editorCaret.trimOrExpandToFullLines();
                selectionSize = 0;
            } else {
                // dupe the line with selection replicated for each caret
                editorCaret.removeSelection();
                editor.getSelectionModel().removeSelection();
            }

            for (int i = 0; i < iMax; i++) {
                final Couple<Integer> couple = duplicateLineOrSelectedBlockAtCaret(editor, editor.getDocument(), editor.getCaretModel().getPrimaryCaret(), true);
                if (couple != null) copies.add(couple);
            }

            // create multiple carets on first line of every copy
            EditorPosition pos = editorCaret.getCaretPosition();
            editorCaret.removeSelection();

            Document doc = editor.getDocument();
            CaretModel caretModel = editor.getCaretModel();

            // build the carets
            for (int i = 0; i < iMax; i++) {
                Couple<Integer> couple = copies.get(i);

                int lineNumber = doc.getLineNumber(couple.first);
                int lineEndOffset = doc.getLineEndOffset(lineNumber);
                int lineStartOffset = doc.getLineStartOffset(lineNumber);

                EditorPosition editorPosition = pos.onLine(lineNumber);
                Caret caret1 = i == 0 ? caretModel.getPrimaryCaret() : caretModel.addCaret(editorPosition.toVisualPosition());
                if (caret1 != null) {
                    caret1.moveToLogicalPosition(editorPosition);
                    int offset = editorPosition.getOffset();

                    // replicate selection to this position
                    if (isStartAnchor) {
                        caret1.setSelection(offset - selectionSize, offset);
                    } else {
                        caret1.setSelection(offset, offset + selectionSize);
                    }
                    manager.resetSelectionState(caret1);
                }
            }

            EditHelpers.scrollToCaret(editor);
        }
    }

    @Override
    public boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
        return true;
    }
}
