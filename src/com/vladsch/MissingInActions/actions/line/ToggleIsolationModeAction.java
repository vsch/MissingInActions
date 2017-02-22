/*
 * Copyright (c) 2016-2017 Vladimir Schneider <vladimir.schneider@gmail.com>
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.vladsch.MissingInActions.actions.line;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.DumbAware;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;

public class ToggleIsolationModeAction extends ToggleAction implements DumbAware {
    @Override
    public boolean isSelected(final AnActionEvent e) {
        final EditorEx editor = (EditorEx) CommonDataKeys.EDITOR.getData(e.getDataContext());
        boolean enabled = false;
        boolean selected = false;

        if (editor != null) {
            LineSelectionManager manager = LineSelectionManager.getInstance(editor);
            selected = manager.isIsolatedMode();
            enabled = selected || manager.haveIsolatedLines();
        }
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
        return selected;
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
