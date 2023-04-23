// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: May 14, 2002
 * Time: 7:40:40 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.vladsch.MissingInActions.actions.line;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.vladsch.MissingInActions.actions.DumbAwareEditorAction;
import com.vladsch.MissingInActions.actions.LineSelectionAware;
import com.vladsch.MissingInActions.actions.NumberActionHandler;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import org.jetbrains.annotations.NotNull;

public class NumberAction extends DumbAwareEditorAction implements LineSelectionAware {
    public NumberAction() {
        super(new NumberActionHandler());
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        super.update(e);
        e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
    }
}
