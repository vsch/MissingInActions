// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GenerateExceptionAction extends EditorAction {
    public GenerateExceptionAction() {
        super(new Handler());
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        super.update(e);
        e.getPresentation().setVisible(ApplicationSettings.getInstance().isShowGenerateException());
    }

    private static class Handler extends EditorActionHandler {
        Handler() {
            super(true);
        }

        @Override
        protected boolean isEnabledForCaret(@NotNull final Editor editor, @NotNull final Caret caret, final DataContext dataContext) {
            return true;
        }

        @Override
        protected void doExecute(@NotNull final Editor editor, @Nullable final Caret caret, final DataContext dataContext) {
            throw new IllegalStateException("Always Illegal State for this Action");
        }
    }
}
