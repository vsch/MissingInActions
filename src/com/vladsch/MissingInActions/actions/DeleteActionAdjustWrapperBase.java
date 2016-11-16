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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.project.Project;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.MissingInActions.util.LineSelectionAdjuster;
import com.vladsch.MissingInActions.util.LogPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

import static com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR;

public abstract class DeleteActionAdjustWrapperBase extends AnAction {
    final private EditorAction myAction;

    public DeleteActionAdjustWrapperBase(EditorAction action) {
        myAction = action;
    }

    @Override
    public void setInjectedContext(boolean worksInInjected) {
        myAction.setInjectedContext(worksInInjected);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Editor editor = getEditor(dataContext);
        actionPerformed(editor, dataContext);
    }

    protected Editor getEditor(@NotNull DataContext dataContext) {
        return EDITOR.getData(dataContext);
    }

    public void actionPerformed(Editor editor, @NotNull DataContext dataContext) {
        // here we snapshot carets that have line selections to remember their columns
        // after the action we move them to the logical position on the same line
        HashMap<Caret, Integer> caretColumns = new HashMap<>();
        LogPos.Factory f = LogPos.factory(editor);
        LineSelectionAdjuster adjuster = LineSelectionAdjuster.getInstance(editor);

        for (Caret caret : editor.getCaretModel().getAllCarets()) {
            if (caret.hasSelection()) {
                if (adjuster.getSelectionState(caret).isLine()) {
                    LogPos pos = f.fromPos(caret.getLogicalPosition());
                    caretColumns.put(caret, pos.column);
                }
            }
        }

        myAction.actionPerformed(editor, dataContext);

        for (Caret caret : editor.getCaretModel().getAllCarets()) {
            if (caretColumns.containsKey(caret)) {
                // we move it
                LogPos pos = f.fromPos(caret.getLogicalPosition());
                caret.moveToLogicalPosition(pos.atColumn(caretColumns.get(caret)));
            }
        }

        EditHelpers.scrollToCaret(editor);
    }

    @Override
    public void update(AnActionEvent e) {
        myAction.update(e);
    }

    // @formatter:off
    @Nullable public static Project getEventProject(AnActionEvent e) {return AnAction.getEventProject(e);}
    @Override public boolean displayTextInToolbar() {return myAction.displayTextInToolbar();}
    @Override public boolean isDefaultIcon() {return myAction.isDefaultIcon();}
    @Override public boolean isDumbAware() {return myAction.isDumbAware();}
    @Override public boolean isInInjectedContext() {return myAction.isInInjectedContext();}
    @Override public boolean isTransparentUpdate() {return myAction.isTransparentUpdate();}
    @Override public boolean startInTransaction() {return myAction.startInTransaction();}
    @Override public void beforeActionPerformedUpdate(@NotNull AnActionEvent e) {myAction.beforeActionPerformedUpdate(e);}
    public void update(Editor editor, Presentation presentation, DataContext dataContext) { myAction.update(editor, presentation, dataContext); }
    public void updateForKeyboardAccess(Editor editor, Presentation presentation, DataContext dataContext) { myAction.updateForKeyboardAccess(editor, presentation, dataContext); }
    // @formatter:on
}
