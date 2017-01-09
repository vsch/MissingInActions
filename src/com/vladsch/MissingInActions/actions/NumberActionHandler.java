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

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: May 13, 2002
 * Time: 9:58:23 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.vladsch.MissingInActions.actions;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.ex.EditorEx;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.settings.NumberingOptions;
import com.vladsch.MissingInActions.settings.RenumberingDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NumberActionHandler extends EditorActionHandler {
    public NumberActionHandler() {
        super(false);
    }

    @Override
    protected boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
        return !(editor instanceof EditorEx) || editor.getCaretModel().supportsMultipleCarets();
    }

    @Override
    public void doExecute(final Editor editor, final @Nullable Caret caret, final DataContext dataContext) {
        final LineSelectionManager manager = LineSelectionManager.getInstance(editor);

        if (editor.getCaretModel().supportsMultipleCarets()) {
            boolean apply = RenumberingDialog.showDialog(editor.getComponent(), (EditorEx) editor);

            if (apply) {
                NumberingOptions options = ApplicationSettings.getInstance().getLastNumberingOptions();
                
                manager.guard(() -> {
                });
            }
        }
    }

    private void perform(Editor editor, LineSelectionManager manager, Caret caret) {
        assert caret != null;

        if (caret.hasSelection()) {
            EditorCaret editorCaret = manager.getEditorCaret(caret);
            if (editorCaret.getSelectionLineCount() > 1) {
                int column = editorCaret.getCaretPosition().column;
                editorCaret.setIsStartAnchorUpdateAnchorColumn(!editorCaret.isStartAnchor());
                if (column != 0 && editorCaret.getCaretPosition().column == 0) {
                    editorCaret.restoreColumn(column);
                }
                editorCaret.commit();
            } else {
                // swap start/end
                int startOffset = caret.getSelectionStart();
                int endOffset = caret.getSelectionEnd();
                if (caret.getLeadSelectionOffset() == caret.getSelectionStart()) {
                    caret.moveToOffset(startOffset);
                } else {
                    caret.moveToOffset(endOffset);
                }
                caret.setSelection(endOffset, startOffset);
            }
        }
    }
}