// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.character;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.vladsch.MissingInActions.util.EditHelpers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.vladsch.MissingInActions.util.EditHelpers.END_OF_WORD;
import static com.vladsch.MissingInActions.util.EditHelpers.isIdentifier;

@SuppressWarnings("WeakerAccess")
abstract public class AbstractSelectAndMoveToNextOrPrevWordHandler extends EditorActionHandler {
    abstract public boolean isNext();
    abstract public boolean isInDifferentHumpsMode();
    abstract public int getBoundaryFlags();

    public AbstractSelectAndMoveToNextOrPrevWordHandler() {
        super(true);
    }

    @Override
    protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
        assert caret != null;

        boolean isNext = isNext();
        boolean differentHumpsMode = isInDifferentHumpsMode();
        int boundaryFlags = getBoundaryFlags();

        if (EditHelpers.isPasswordEditor(editor)) {

        } else {
            VisualPosition currentPosition = caret.getVisualPosition();
            if (caret.isAtBidiRunBoundary() && (isNext ^ currentPosition.leansRight)) {

            } else {
                Document document = editor.getDocument();

                int length = document.getTextLength();
                if (isNext ^ caret.isAtRtlLocation()) {
                    // move to next
                    if (caret.getOffset() < length) {
                        CharSequence text = document.getCharsSequence();
                        int offset = caret.getOffset();

                        if (!isIdentifier(text, offset)) {
                            offset = moveToPreviousIdentifier(text, offset);
                            if (offset <= 0) return;
                            offset++;
                        } else {
                            offset++;
                            if (offset >= length) return;
                        }

                        caret.moveToOffset(offset);
                        selectNextWord(editor, caret, differentHumpsMode, boundaryFlags, offset);
                        int startOffset = caret.getSelectionStart();
                        int endOffset = caret.getSelectionEnd();

                        offset = moveToNextIdentifier(text, endOffset + 1);
                        if (offset < length) {
                            caret.moveToOffset(offset);
                        }
                        //EditHelpers.moveCaretToNextWordStartOrEnd(editor, false, differentHumpsMode ^ editor.getSettings().isCamelWords(), boundaryFlags);
                        caret.setSelection(startOffset, endOffset);
                    }
                } else {
                    // move to previous
                    if (caret.getOffset() > 0) {
                        CharSequence text = document.getCharsSequence();
                        int offset = caret.getOffset() - 1;

                        if (!isIdentifier(text, offset)) {
                            offset = moveToNextIdentifier(text, offset);
                            if (offset >= length) return;
                        } else {
                            if (offset <= 0) return;
                        }

                        caret.moveToOffset(offset);
                        selectPrevWord(editor, caret, differentHumpsMode, boundaryFlags, offset);
                        int startOffset = caret.getSelectionStart();
                        int endOffset = caret.getSelectionEnd();

                        offset = moveToPreviousIdentifier(text, startOffset - 1);
                        if (offset >= 0) {
                            caret.moveToOffset(offset + 1);
                        }

                        EditHelpers.moveCaretToPreviousWordStartOrEnd(editor, false, differentHumpsMode ^ editor.getSettings().isCamelWords(), boundaryFlags & ~END_OF_WORD);
                        caret.setSelection(startOffset, endOffset);
                    }
                }
            }
        }
    }

    private static void selectPrevWord(final Editor editor, @NotNull final Caret caret, final boolean differentHumpsMode, final int boundaryFlags, final int offset) {
        caret.moveToOffset(offset);
        EditHelpers.moveCaretToNextWordStartOrEnd(editor, false, differentHumpsMode ^ editor.getSettings().isCamelWords(), boundaryFlags);
        EditHelpers.moveCaretToPreviousWordStartOrEnd(editor, true, differentHumpsMode ^ editor.getSettings().isCamelWords(), boundaryFlags);
    }

    private static void selectNextWord(final Editor editor, @NotNull final Caret caret, final boolean differentHumpsMode, final int boundaryFlags, final int offset) {
        caret.moveToOffset(offset);
        EditHelpers.moveCaretToPreviousWordStartOrEnd(editor, false, differentHumpsMode ^ editor.getSettings().isCamelWords(), boundaryFlags);
        EditHelpers.moveCaretToNextWordStartOrEnd(editor, true, differentHumpsMode ^ editor.getSettings().isCamelWords(), boundaryFlags);
    }

    private static int moveToPreviousIdentifier(final CharSequence text, int offset) {
        while (offset > 0 && !isIdentifier(text, offset)) {
            offset--;
        }
        return offset;
    }

    private static int moveToNextIdentifier(final CharSequence text, int offset) {
        while (offset < text.length() && !isIdentifier(text, offset)) {
            offset++;
        }
        return offset;
    }
}
