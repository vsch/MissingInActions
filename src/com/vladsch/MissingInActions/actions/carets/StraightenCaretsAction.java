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

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: May 14, 2002
 * Time: 7:40:40 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.vladsch.MissingInActions.actions.carets;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.vladsch.MissingInActions.actions.ActionUtils;
import com.vladsch.MissingInActions.actions.LineSelectionAware;
import org.jetbrains.annotations.NotNull;

public class StraightenCaretsAction extends AnAction implements LineSelectionAware {
    public StraightenCaretsAction() {
        setEnabledInModalContext(true);
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        EditorEx editor = ActionUtils.getEditor(e);
        if (editor == null || editor.isOneLineMode()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setVisible(true);
        } else {
            e.getPresentation().setEnabled(editor.getCaretModel().getCaretCount() > 1 && !editor.getSelectionModel().hasSelection());
            e.getPresentation().setVisible(true);
            super.update(e);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final EditorEx editor = ActionUtils.getEditor(e);
        if (editor == null) return;

        final CaretModel caretModel = editor.getCaretModel();
        final DocumentEx doc = editor.getDocument();

        if (caretModel.getCaretCount() > 1 && !editor.getSelectionModel().hasSelection()) {
            // move all carets to column of primary
            int column = caretModel.getPrimaryCaret().getLogicalPosition().column;
            for (Caret caret : caretModel.getAllCarets()) {
                if (!caret.isValid()) continue;
                caret.moveToLogicalPosition(new LogicalPosition(doc.getLineNumber(caret.getOffset()), column));
            }
        }
    }
}
