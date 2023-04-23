// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.vladsch.MissingInActions.actions.CaretSearchAwareAction;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CancelSearchCaretsAction extends EditorAction implements CaretSearchAwareAction {
    public CancelSearchCaretsAction() {
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
            return LineSelectionManager.getInstance(editor).getCaretSpawningHandler() != null;
        }

        @Override
        protected void doExecute(@NotNull final Editor editor, @Nullable final Caret caret, final DataContext dataContext) {
            LineSelectionManager manager = LineSelectionManager.getInstance(editor);
            List<CaretState> caretStates = manager.getStartCaretStates();
            if (caretStates != null) {
                manager.clearSearchFoundCarets();
                editor.getCaretModel().setCaretsAndSelections(caretStates);
            }
        }
    }
}
