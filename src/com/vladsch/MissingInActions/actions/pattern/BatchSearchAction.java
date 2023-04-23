// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
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
                ToolWindow toolWindow = PluginProjectComponent.getInstance(project).getSearchReplaceToolWindow();
                return toolWindow != null && toolWindow.isVisible();
            }
        }
        return false;
    }
}
