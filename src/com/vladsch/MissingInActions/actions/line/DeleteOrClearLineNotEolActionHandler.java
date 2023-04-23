// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.line;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.editor.actions.EditorActionUtil;
import com.intellij.util.text.CharArrayUtil;
import com.vladsch.MissingInActions.util.EditHelpers;
import org.jetbrains.annotations.Nullable;

public class DeleteOrClearLineNotEolActionHandler extends EditorWriteActionHandler {
    final private boolean myToLineStart;
    final private boolean myClearOnly;

    public DeleteOrClearLineNotEolActionHandler(boolean toLineStart, boolean clearOnly) {
        super(true);
        myToLineStart = toLineStart;
        myClearOnly = clearOnly;
    }

    @Override
    public void executeWriteAction(Editor editor, @Nullable Caret caret, DataContext dataContext) {
        CommandProcessor.getInstance().setCurrentCommandGroupId(EditorActionUtil.DELETE_COMMAND_GROUP);
        if (caret == null) {
            caret = editor.getCaretModel().getCurrentCaret();
        }

        if (caret.hasSelection()) {
            EditHelpers.delete(editor, caret, caret.getSelectionStart(), caret.getSelectionEnd(), myClearOnly);
            return;
        }

        final Document doc = editor.getDocument();
        int caretOffset = caret.getOffset();
        final int lineNumber = doc.getLineNumber(caretOffset);
        int lineEndOffset = doc.getLineEndOffset(lineNumber);
        int start;
        int end;

        if (myToLineStart) {
            int lineStartOffset = doc.getLineStartOffset(lineNumber);
            int indent = EditHelpers.countWhiteSpace(doc.getCharsSequence(), lineStartOffset, lineEndOffset);

            start = lineStartOffset + indent;
            end = caretOffset;
            if (caretOffset <= lineStartOffset + indent) {
                if (editor.getCaretModel().getCaretCount() > 1) return;
                start = lineStartOffset;
                end = caretOffset;
            }
        } else {
            start = caretOffset;
            end = lineEndOffset;
            if (lineEndOffset < doc.getTextLength() && CharArrayUtil.isEmptyOrSpaces(doc.getCharsSequence(), caretOffset, lineEndOffset)) {
                end++;
            }

            if (end > lineEndOffset) {
                end = lineEndOffset;
            }

            if (end > lineEndOffset || end <= start) {
                return;
            }
        }

        EditHelpers.delete(editor, caret, start, end, myClearOnly);
    }
}
