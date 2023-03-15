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

package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.vladsch.MissingInActions.BatchSearchReplaceToolWindow;
import com.vladsch.MissingInActions.PluginProjectComponent;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR;

public class BatchSearchAction extends EditorAction {
    public BatchSearchAction() {
        super(new Handler());
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        super.update(e);
        e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
    }

    private static class Handler extends EditorActionHandler {
        public Handler() {
            super(false);
        }

        @Override
        protected boolean isEnabledForCaret(@NotNull final Editor editor, @NotNull final Caret caret, final DataContext dataContext) {
            return editor instanceof EditorEx && editor.getProject() != null;
        }

        @Override
        protected void doExecute(@NotNull final Editor editor, @Nullable final Caret caret, final DataContext dataContext) {
            showBatchSearchWindow(editor);
        }
    }

    public static void showBatchSearchWindow(@NotNull final DataContext dataContext) {
        showBatchSearchWindow(EDITOR.getData(dataContext));
    }

    public static void showBatchSearchWindow(@Nullable final Editor editor) {
        if (editor instanceof EditorEx) {
            Project project = editor.getProject();
            if (project != null) {
                PluginProjectComponent.getInstance(project).showBatchSearchReplace();
            }
        }
    }

    public static void hideBatchSearchWindow(@NotNull final DataContext dataContext) {
        hideBatchSearchWindow(EDITOR.getData(dataContext));
    }

    public static void hideBatchSearchWindow(@Nullable final Editor editor) {
        if (editor instanceof EditorEx) {
            Project project = editor.getProject();
            if (project != null) {
                PluginProjectComponent.getInstance(project).hideBatchSearchReplace();
            }
        }
    }

    public static boolean isShowingBatchSearchWindow(@NotNull final DataContext dataContext) {
        return isShowingBatchSearchWindow(EDITOR.getData(dataContext));
    }

    public static boolean isShowingBatchSearchWindow(@Nullable final Editor editor) {
        if (editor instanceof EditorEx) {
            Project project = editor.getProject();
            if (project != null) {
                BatchSearchReplaceToolWindow toolWindow = PluginProjectComponent.getInstance(project).getSearchReplaceToolWindow();
                return toolWindow != null && toolWindow.isShowing();
            }
        }
        return false;
    }
}
