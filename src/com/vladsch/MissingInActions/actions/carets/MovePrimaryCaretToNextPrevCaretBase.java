// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.carets;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.ex.EditorEx;
import com.vladsch.MissingInActions.actions.ActionUtils;
import com.vladsch.MissingInActions.actions.LineSelectionAware;
import com.vladsch.MissingInActions.util.EditHelpers;
import org.jetbrains.annotations.NotNull;

abstract public class MovePrimaryCaretToNextPrevCaretBase extends AnAction implements LineSelectionAware {
    final protected int myDelta;

    public MovePrimaryCaretToNextPrevCaretBase(int delta) {
        myDelta = delta;
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
            e.getPresentation().setEnabled(editor.getCaretModel().getCaretCount() > 1);
            e.getPresentation().setVisible(true);
            super.update(e);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final EditorEx editor = ActionUtils.getEditor(e);
        if (editor == null) return;

        final CaretModel caretModel = editor.getCaretModel();
        int caretCount = caretModel.getCaretCount();
        if (caretCount > 1) {

            int index = ActionUtils.getPrimaryCaretIndex(editor, true);
            int newIndex = (index + myDelta) % caretCount;
            if (newIndex < 0) newIndex += caretCount;

            if (ActionUtils.setPrimaryCaretIndex(editor, newIndex, true)) {
                EditHelpers.scrollToCaret(editor);
            }
        }
    }
}
