/*
 * Copyright (c) 2016-2018 Vladimir Schneider <vladimir.schneider@gmail.com>
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

package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.ex.EditorEx;
import com.vladsch.MissingInActions.actions.ActionUtils;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;

public class BackwardSearchCaretSpawningAction extends EditorAction implements Toggleable {
    public BackwardSearchCaretSpawningAction() {
        super(new CaretSpawningSearchHandler(true));
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
                if (spawningHandler != null && spawningHandler.isBackwards()) {
                    selected = true;
                }
            }
        }

        presentation.putClientProperty(Toggleable.SELECTED_PROPERTY, selected);

        e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
        super.update(e);
    }
}
