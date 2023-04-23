// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.vladsch.MissingInActions.Plugin;
import com.vladsch.MissingInActions.settings.ApplicationSettings;

public class ClearWordHighlightsAction extends AnAction implements DumbAware {
    @Override
    public void update(final AnActionEvent e) {
        e.getPresentation().setEnabled(Plugin.getInstance().haveHighlights() || BatchSearchAction.isShowingBatchSearchWindow(e.getDataContext()));
        super.update(e);

        e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        BatchSearchAction.hideBatchSearchWindow(e.getDataContext());
        
        Plugin plugin = Plugin.getInstance();
        plugin.clearHighlights();
    }
}
