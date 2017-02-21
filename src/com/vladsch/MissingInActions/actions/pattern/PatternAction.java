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

package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.util.ui.UIUtil;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.util.Utils;
import com.vladsch.MissingInActions.util.ui.BackgroundColor;
import com.vladsch.MissingInActions.util.ui.TextFieldAction;

import javax.swing.*;
import java.util.regex.Pattern;

public class PatternAction extends TextFieldAction {
    protected PatternAction() {
        super();
    }

    protected PatternAction(final String text) {
        super(text);
    }

    protected PatternAction(final String text, final String description, final Icon icon) {
        super(text, description, icon);
    }

    @Override
    protected void updateOnFocusLost(final String text, final Presentation presentation) {

    }

    @Override
    protected void updateOnFocusGained(final String text, final Presentation presentation) {

    }

    @Override
    protected void updateOnTextChange(final String text, final Presentation presentation) {
        try {
            Pattern pattern = Pattern.compile(text);
            presentation.putClientProperty(TEXT_FIELD_BACKGROUND, null);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            presentation.setDescription(e.getMessage());
            presentation.putClientProperty(TEXT_FIELD_BACKGROUND, BackgroundColor.of(Utils.errorColor(UIUtil.getTextFieldBackground())));
        }
    }

    @Override
    public void update(final AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        final EditorEx editor = (EditorEx) CommonDataKeys.EDITOR.getData(e.getDataContext());
        boolean enabled = false;

        if (editor != null) {
            LineSelectionManager manager = LineSelectionManager.getInstance(editor);
            RangeLimitedCaretSpawningHandler spawningHandler = manager.getCaretSpawningHandler();
            if (spawningHandler != null) {
                enabled = true;
                presentation.setText(spawningHandler.getPattern());
            }
        }
        if (!enabled) {
            presentation.setText("RegEx");
        }
        presentation.setEnabled(enabled);
        super.update(e);
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        // apply new pattern
        Presentation presentation = e.getPresentation();
        String text = presentation.getText();
        if (text != null && !text.isEmpty()) {
            final EditorEx editor = (EditorEx) CommonDataKeys.EDITOR.getData(e.getDataContext());
            if (editor != null) {
                LineSelectionManager manager = LineSelectionManager.getInstance(editor);
                final RangeLimitedCaretSpawningHandler spawningHandler = manager.getCaretSpawningHandler();
                if (spawningHandler != null) {
                    try {
                        Pattern pattern = Pattern.compile(text);
                        spawningHandler.setPattern(text);

                        // rerun on new caret position after action
                        manager.guard(() -> {
                            spawningHandler.doAction(manager, editor, null, null);
                        });
                    } catch (IllegalArgumentException | IndexOutOfBoundsException ignored) {
                    }
                }
            }
        }
    }
}
