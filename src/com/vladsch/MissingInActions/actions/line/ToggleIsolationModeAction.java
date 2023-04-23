// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.line;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.DumbAware;
import com.vladsch.MissingInActions.actions.ActionUtils;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import org.jetbrains.annotations.NotNull;

public class ToggleIsolationModeAction extends ToggleAction implements DumbAware {
    @Override
    public boolean isSelected(@NotNull final AnActionEvent e) {
        final EditorEx editor = ActionUtils.getEditor(e);
        boolean selected = false;

        if (editor != null) {
            LineSelectionManager manager = LineSelectionManager.getInstance(editor);
            selected = manager.isIsolatedMode() && manager.haveIsolatedLines();
        }
        return selected;
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        final EditorEx editor = ActionUtils.getEditor(e);
        boolean enabled = false;
        boolean selected = false;

        if (editor != null) {
            LineSelectionManager manager = LineSelectionManager.getInstance(editor);
            selected = manager.isIsolatedMode() && manager.haveIsolatedLines();
            enabled = selected || manager.haveIsolatedLines();
        }
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
        super.update(e);
    }

    @Override
    public void setSelected(final AnActionEvent e, final boolean state) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor != null) {
            LineSelectionManager manager = LineSelectionManager.getInstance(editor);
            manager.setIsolatedMode(state);
        }
    }
}
