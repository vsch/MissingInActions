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

package com.vladsch.MissingInActions.actions.character;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.util.EditHelpers;
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
    protected void doExecute(Editor editor, @Nullable Caret caret, DataContext dataContext) {
        assert caret != null;

        if (EditorUtil.isPasswordEditor(editor)) {
            int selectionStartOffset = caret.getLeadSelectionOffset();
            caret.moveToOffset(isNext() ? editor.getDocument().getTextLength() : 0);
            if (isWithSelection()) caret.setSelection(selectionStartOffset, caret.getOffset());
        } else {
            EditorCaret editorCaret = LineSelectionManager.getInstance(editor).getEditorCaret(caret);
            editorCaret.setCharSelection()
                    .normalizeCaretPosition()
                    .commit();
            
            VisualPosition currentPosition = caret.getVisualPosition();
            if (caret.isAtBidiRunBoundary() && (isNext() ^ currentPosition.leansRight)) {
                int selectionStartOffset = caret.getLeadSelectionOffset();
                VisualPosition selectionStartPosition = caret.getLeadSelectionPosition();
                caret.moveToVisualPosition(currentPosition.leanRight(!currentPosition.leansRight));
                if (isWithSelection()) {
                    caret.setSelection(selectionStartPosition, selectionStartOffset, caret.getVisualPosition(), caret.getOffset());
                }
            } else {
                if (isNext() ^ caret.isAtRtlLocation()) {
                    EditHelpers.moveCaretToNextWordStartOrEnd(editor, isWithSelection(), isInDifferentHumpsMode() ^ editor.getSettings().isCamelWords(), getBoundaryFlags());
                } else {
                    EditHelpers.moveCaretToPreviousWordStartOrEnd(editor, isWithSelection(), isInDifferentHumpsMode() ^ editor.getSettings().isCamelWords(), getBoundaryFlags());
                }
            }
        }
    }
}
