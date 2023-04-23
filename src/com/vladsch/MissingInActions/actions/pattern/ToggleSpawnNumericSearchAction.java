// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.vladsch.MissingInActions.actions.CaretSearchAwareAction;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import org.jetbrains.annotations.NotNull;

public class ToggleSpawnNumericSearchAction extends ToggleAction implements DumbAware, CaretSearchAwareAction {
    @Override
    public boolean isSelected(@NotNull final AnActionEvent e) {
        ApplicationSettings instance = ApplicationSettings.getInstance();
        return instance.isSpawnNumericSearch() && !instance.isSpawnNumericHexSearch();
    }

    @Override
    public void setSelected(@NotNull final AnActionEvent e, final boolean state) {
        ApplicationSettings instance = ApplicationSettings.getInstance();
        instance.setSpawnNumericSearch(state);
        if (state) instance.setSpawnNumericHexSearch(false);
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        e.getPresentation().setEnabled(true);
        super.update(e);
    }
}
