// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.character;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.editor.actions.EditorActionUtil;
import com.intellij.openapi.ide.CopyPasteManager;
import com.vladsch.MissingInActions.util.EditHelpers;

public class DeleteSpacesActionHandler extends EditorWriteActionHandler {
    public DeleteSpacesActionHandler() {
        super(true);
    }

    @Override
    public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
        CommandProcessor.getInstance().setCurrentCommandGroupId(EditorActionUtil.DELETE_COMMAND_GROUP);
        CopyPasteManager.getInstance().stopKillRings();

        if (editor.getSelectionModel().hasSelection()) {
            EditHelpers.deleteSelectedText(editor);
            return;
        }

        deleteSpaces(editor);
    }

    static void deleteSpaces(Editor editor) {
        int startOffset = editor.getCaretModel().getOffset();
        int endOffset = getSpacesEndOffset(editor, startOffset);
        if (endOffset > startOffset) {
            Document document = editor.getDocument();
            document.deleteString(startOffset, endOffset);
        }
    }

    private static int getSpacesEndOffset(Editor editor, int offset) {
        Document document = editor.getDocument();
        CharSequence text = document.getCharsSequence();

        if (offset >= document.getTextLength() - 1)
            return offset;

        int newOffset = offset;
        int lineNumber = editor.getCaretModel().getLogicalPosition().line;
        int maxOffset = document.getLineEndOffset(lineNumber);

        if (newOffset + 1 > maxOffset) {
            return offset;
        }

        int spaces = EditHelpers.countWhiteSpace(text, newOffset, maxOffset);
        return newOffset + spaces;
    }
}
