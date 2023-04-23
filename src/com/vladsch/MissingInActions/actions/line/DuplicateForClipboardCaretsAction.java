// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: May 14, 2002
 * Time: 7:18:30 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.vladsch.MissingInActions.actions.line;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import org.jetbrains.annotations.NotNull;

public class DuplicateForClipboardCaretsAction extends EditorAction {
    public DuplicateForClipboardCaretsAction() {
        super(new DuplicateForClipboardCaretsActionHandler());
    }

    public DuplicateForClipboardCaretsAction(boolean doPaste, boolean insertBlankLine) {
        super(new DuplicateForClipboardCaretsActionHandler(doPaste, insertBlankLine));
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        super.update(e);
        e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
    }

    //@Override
    //public void update(final Editor editor, final Presentation presentation, final DataContext dataContext) {
    //    super.update(editor, presentation, dataContext);
    //    if (editor.getSelectionModel().hasSelection()) {
    //        if (editor.getSelectionModel().getLeadSelectionOffset() == editor.getSelectionModel().getSelectionStart()) {
    //            presentation.setText(Bundle.message("action.duplicate-before-after.after-selection"), true);
    //        } else {
    //            presentation.setText(Bundle.message("action.duplicate-before-after.before-selection"), true);
    //        }
    //    } else {
    //        presentation.setText(EditorBundle.message("action.duplicate.line"), true);
    //    }
    //}
}
