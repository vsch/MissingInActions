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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DuplicateBeforeAfterActionHandler extends EditorWriteActionHandler {
    public DuplicateBeforeAfterActionHandler() {
        super(true);
    }

    public static void duplicateLineOrSelectedBlockAtCaret(Editor editor) {
        Document document = editor.getDocument();
        CaretModel caretModel = editor.getCaretModel();
        ScrollingModel scrollingModel = editor.getScrollingModel();
        if (editor.getSelectionModel().hasSelection()) {
            int start = editor.getSelectionModel().getSelectionStart();
            int end = editor.getSelectionModel().getSelectionEnd();
            String s = document.getCharsSequence().subSequence(start, end).toString();
            document.insertString(end, s);
            if (start == editor.getSelectionModel().getLeadSelectionOffset()) {
                // selection is anchored at start, select newly copied lines and move there 
                caretModel.moveToOffset(end + s.length());
                scrollingModel.scrollToCaret(ScrollType.RELATIVE);
                editor.getSelectionModel().removeSelection();
                editor.getSelectionModel().setSelection(end, end + s.length());
            }
        } else {
            duplicateLinesRange(editor, document, caretModel.getVisualPosition(), caretModel.getVisualPosition());
        }
    }

    @SuppressWarnings("WeakerAccess")
    @Nullable
    public static Couple<Integer> duplicateLinesRange(Editor editor, Document document, VisualPosition rangeStart, VisualPosition rangeEnd) {
        Pair<LogicalPosition, LogicalPosition> lines = EditorUtil.calcSurroundingRange(editor, rangeStart, rangeEnd);
        int offset = editor.getCaretModel().getOffset();

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

        editor.getCaretModel().moveToOffset(newOffset);
        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        return Couple.of(end, end + s.length());
    }

    @Override
    public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
        duplicateLineOrSelectedBlockAtCaret(editor);
    }

    @Override
    public boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
        return !editor.isOneLineMode() || editor.getSelectionModel().hasSelection();
    }
}