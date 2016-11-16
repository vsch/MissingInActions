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
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.MissingInActions.util.LineSelectionAdjuster;
import org.jetbrains.annotations.Nullable;

class SelectNextOrPrevWordHandler extends EditorActionHandler {
    private final boolean myNext;
    private final boolean myInDifferentHumpsMode;

    SelectNextOrPrevWordHandler(boolean next, boolean inDifferentHumpsMode) {
        super(true);
        myNext = next;
        myInDifferentHumpsMode = inDifferentHumpsMode;
    }

    @Override
    protected void doExecute(Editor editor, @Nullable Caret caret, DataContext dataContext) {
        assert caret != null;
        if (EditorUtil.isPasswordEditor(editor)) {
            int selectionStartOffset = caret.getLeadSelectionOffset();
            caret.moveToOffset(myNext ? editor.getDocument().getTextLength() : 0);
            caret.setSelection(selectionStartOffset, caret.getOffset());
        } else {
            LineSelectionAdjuster.adjustLineSelectionToCharacterSelection(editor, caret, false);
            VisualPosition currentPosition = caret.getVisualPosition();
            if (caret.isAtBidiRunBoundary() && (myNext ^ currentPosition.leansRight)) {
                int selectionStartOffset = caret.getLeadSelectionOffset();
                VisualPosition selectionStartPosition = caret.getLeadSelectionPosition();
                caret.moveToVisualPosition(currentPosition.leanRight(!currentPosition.leansRight));
                caret.setSelection(selectionStartPosition, selectionStartOffset, caret.getVisualPosition(), caret.getOffset());
            } else {
                if (myNext ^ caret.isAtRtlLocation()) {
                    EditHelpers.moveCaretToNextWordStart(editor, true, myInDifferentHumpsMode ^ editor.getSettings().isCamelWords(), true, false, false);
                } else {
                    EditHelpers.moveCaretToPreviousWordStart(editor, true, myInDifferentHumpsMode ^ editor.getSettings().isCamelWords(), false, false);
                }
            }
        }
    }
}
