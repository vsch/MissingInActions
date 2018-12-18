/*
 * Copyright (c) 2016-2018 Vladimir Schneider <vladimir.schneider@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import org.jetbrains.annotations.NotNull;

public class ToggleSpawnNumericHexSearchAction extends ToggleAction implements DumbAware {
    @Override
    public boolean isSelected(final AnActionEvent e) {
        ApplicationSettings instance = ApplicationSettings.getInstance();
        return instance.isSpawnNumericSearch() && instance.isSpawnNumericHexSearch();
    }

    @Override
    public void setSelected(final AnActionEvent e, final boolean state) {
        ApplicationSettings instance = ApplicationSettings.getInstance();
        instance.setSpawnNumericHexSearch(state);
        instance.setSpawnNumericSearch(state);
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        e.getPresentation().setEnabled(true);
        super.update(e);
    }
}
