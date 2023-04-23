// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.ex.EditorEx;
import com.vladsch.MissingInActions.actions.ActionUtils;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;

@SuppressWarnings("ComponentNotRegistered")
public class ForwardSearchCaretSpawningAction extends EditorAction implements Toggleable {
    public ForwardSearchCaretSpawningAction() {
        super(new CaretSpawningSearchHandler(false));
    }

    @Override
    public void update(final AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        boolean enabled = presentation.isEnabled();
        boolean selected = false;

        if (enabled) {
            final EditorEx editor = ActionUtils.getEditor(e);
            if (editor != null) {
                LineSelectionManager manager = LineSelectionManager.getInstance(editor);
                RangeLimitedCaretSpawningHandler spawningHandler = manager.getCaretSpawningHandler();
                if (spawningHandler != null && !spawningHandler.isBackwards()) {
                    selected = true;
                }
            }
        }

        Toggleable.setSelected(presentation, selected);

        e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
        super.update(e);
    }
}
