/*
 * Copyright (c) 2016-2018 Vladimir Schneider <vladimir.schneider@gmail.com>
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

package com.vladsch.MissingInActions.actions.character;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.vladsch.MissingInActions.util.EditHelpers;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("WeakerAccess")
abstract public class AbstractSelectAndMoveToNextOrPrevWordHandler extends EditorActionHandler {
    abstract public boolean isNext();
    abstract public boolean isInDifferentHumpsMode();
    abstract public int getBoundaryFlags();

    public AbstractSelectAndMoveToNextOrPrevWordHandler() {
        super(true);
    }

    @Override
    protected void doExecute(Editor editor, @Nullable Caret caret, DataContext dataContext) {
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

                if (isNext ^ caret.isAtRtlLocation()) {
                    // move to next
                    int length = document.getTextLength();
                    if (caret.getOffset() < length) {
                        CharSequence text = document.getCharsSequence();
                        int offset = caret.getOffset();

                        while (offset < length && !EditHelpers.isIdentifier(text, offset)) {
                            offset++;
                        }

                        if (offset >= length) return;
                        caret.moveToOffset(offset + 1);

                        EditHelpers.moveCaretToPreviousWordStartOrEnd(editor, false, differentHumpsMode ^ editor.getSettings().isCamelWords(), boundaryFlags);
                        EditHelpers.moveCaretToNextWordStartOrEnd(editor, true, differentHumpsMode ^ editor.getSettings().isCamelWords(), boundaryFlags);

                        int startOffset = caret.getSelectionStart();
                        int endOffset = caret.getSelectionEnd();
                        EditHelpers.moveCaretToNextWordStartOrEnd(editor, false, differentHumpsMode ^ editor.getSettings().isCamelWords(), boundaryFlags);
                        caret.setSelection(startOffset, endOffset);
                    }
                } else {
                    // move to previous
                    if (caret.getOffset() > 0) {
                        CharSequence text = document.getCharsSequence();
                        int offset = caret.getOffset() - 1;

                        while (offset > 0 && !EditHelpers.isIdentifier(text, offset)) {
                            offset--;
                        }

                        if (offset <= 0) return;
                        caret.moveToOffset(offset);
                        EditHelpers.moveCaretToNextWordStartOrEnd(editor, false, differentHumpsMode ^ editor.getSettings().isCamelWords(), boundaryFlags);
                        EditHelpers.moveCaretToPreviousWordStartOrEnd(editor, true, differentHumpsMode ^ editor.getSettings().isCamelWords(), boundaryFlags);

                        int startOffset = caret.getSelectionStart();
                        int endOffset = caret.getSelectionEnd();
                        EditHelpers.moveCaretToPreviousWordStartOrEnd(editor, false, differentHumpsMode ^ editor.getSettings().isCamelWords(), boundaryFlags);
                        caret.setSelection(startOffset, endOffset);
                    }
                }
            }
        }
    }
}
