// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.carets;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.ex.EditorEx;
import com.vladsch.MissingInActions.actions.ActionUtils;
import com.vladsch.MissingInActions.actions.LineSelectionAware;
import org.jetbrains.annotations.NotNull;

abstract public class ToggleCaretsLineSelectionActionBase extends AnAction implements LineSelectionAware {
    final protected boolean myWantBlankLines;
    final protected boolean myWantNonBlankLines;

    public ToggleCaretsLineSelectionActionBase(boolean wantBlankLines, boolean wantNonBlankLines) {
        myWantBlankLines = wantBlankLines;
        myWantNonBlankLines = wantNonBlankLines;
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
            e.getPresentation().setEnabled(editor.getCaretModel().supportsMultipleCarets() && editor.getSelectionModel().hasSelection() || editor.getCaretModel().getCaretCount() > 1);
            e.getPresentation().setVisible(true);
        }
        super.update(e);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final EditorEx editor = ActionUtils.getEditor(e);
        ActionUtils.toggleCaretsLineSelection(editor, myWantBlankLines, myWantNonBlankLines, true, true, true);
    }
}
