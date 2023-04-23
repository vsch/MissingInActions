// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.character;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import org.jetbrains.annotations.NotNull;

import static com.vladsch.MissingInActions.settings.ApplicationSettings.getInstance;

public class ToggleCamelModeAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        EditorSettingsExternalizable settingsExternalizable = EditorSettingsExternalizable.getInstance();
        boolean camelMode = settingsExternalizable.isCamelWords();
        settingsExternalizable.setCamelWords(!camelMode);

        if (getInstance().isMouseCamelHumpsFollow()) {
            settingsExternalizable.setMouseClickSelectionHonorsCamelWords(!camelMode);
        }
    }
}
