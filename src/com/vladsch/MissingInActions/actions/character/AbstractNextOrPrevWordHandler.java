// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.character;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.vladsch.MissingInActions.util.EditHelpers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("WeakerAccess")
abstract public class AbstractNextOrPrevWordHandler extends EditorActionHandler {
    abstract public boolean isNext();
    abstract public boolean isWithSelection();
    abstract public boolean isInDifferentHumpsMode();
    abstract public int getBoundaryFlags();

    public AbstractNextOrPrevWordHandler() {
        super(true);
    }

    @Override
    protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
        assert caret != null;

        boolean withSelection = isWithSelection();
        boolean isNext = isNext();
        boolean differentHumpsMode = isInDifferentHumpsMode();
        int boundaryFlags = getBoundaryFlags();

        if (EditHelpers.isPasswordEditor(editor)) {
            int selectionStartOffset = caret.getLeadSelectionOffset();
            caret.moveToOffset(isNext ? editor.getDocument().getTextLength() : 0);
            if (withSelection) caret.setSelection(selectionStartOffset, caret.getOffset());
        } else {
            VisualPosition currentPosition = caret.getVisualPosition();
            if (caret.isAtBidiRunBoundary() && (isNext ^ currentPosition.leansRight)) {
                int selectionStartOffset = caret.getLeadSelectionOffset();
                VisualPosition selectionStartPosition = caret.getLeadSelectionPosition();
                caret.moveToVisualPosition(currentPosition.leanRight(!currentPosition.leansRight));
                if (withSelection) {
                    caret.setSelection(selectionStartPosition, selectionStartOffset, caret.getVisualPosition(), caret.getOffset());
                }
            } else {
                if (isNext ^ caret.isAtRtlLocation()) {
                    EditHelpers.moveCaretToNextWordStartOrEnd(editor, withSelection, differentHumpsMode ^ editor.getSettings().isCamelWords(), boundaryFlags);
                } else {
                    EditHelpers.moveCaretToPreviousWordStartOrEnd(editor, withSelection, differentHumpsMode ^ editor.getSettings().isCamelWords(), boundaryFlags);
                }
            }
        }
    }
}
