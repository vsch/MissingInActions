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

package com.vladsch.MissingInActions.util.ui;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.ui.UIUtil;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * @author max
 */
public abstract class TextFieldAction extends AnAction implements CustomComponentAction {
    protected static final String TEXT_FIELD_ORIGINAL = "textFieldOriginal";
    protected static final String TEXT_FIELD_BACKGROUND = "textFieldBackground";

    protected TextFieldAction() {}

    protected TextFieldAction(final String text) {
        super(text);
    }

    protected TextFieldAction(final String text, final String description, final Icon icon) {
        super(text, description, icon);
    }

    protected void updateOnFocusLost(String text, Presentation presentation) {

    }

    protected void updateOnFocusGained(String text, Presentation presentation) {

    }

    protected void updateOnTextChange(String text, Presentation presentation) {

    }

    @Override
    public JComponent createCustomComponent(final Presentation presentation) {
        // this component cannot be stored right here because of action system architecture:
        // one action can be shown on multiple toolbars simultaneously
        final JTextField textField = new JTextField();
        textField.setOpaque(true);

        textField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(final FocusEvent e) {
                presentation.putClientProperty(TEXT_FIELD_ORIGINAL, textField.getText());
                updateOnFocusGained(textField.getText(), presentation);
            }

            @Override
            public void focusLost(final FocusEvent e) {
                updateOnFocusLost(textField.getText(), presentation);
            }
        });

        textField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(final DocumentEvent e) {
                String text = textField.getText();
                if (!text.equals(presentation.getText())) {
                    presentation.setText(text);
                    updateOnTextChange(textField.getText(), presentation);
                }
            }
        });

        updateCustomComponent(textField, presentation);
        return textField;
    }

    protected void doAction(final FocusEvent e) {
        JTextField textField = (JTextField) e.getSource();
        ActionToolbar actionToolbar = UIUtil.getParentOfType(ActionToolbar.class, textField);
        DataContext dataContext =
                actionToolbar != null ? actionToolbar.getToolbarDataContext() : DataManager.getInstance().getDataContext(textField);
        TextFieldAction.this.actionPerformed(AnActionEvent.createFromAnAction(TextFieldAction.this, null, ActionPlaces.UNKNOWN, dataContext));
    }

    @Override
    public void update(final AnActionEvent e) {
        super.update(e);
        Presentation presentation = e.getPresentation();
        Object property = presentation.getClientProperty(CUSTOM_COMPONENT_PROPERTY);
        if (property instanceof JTextField) {
            JTextField textField = (JTextField) property;
            updateCustomComponent(textField, presentation);
        }
    }

    protected void updateCustomComponent(JTextField textField, Presentation presentation) {
        String text = presentation.getText();
        if (!textField.getText().equals(text)) {
            ApplicationManager.getApplication().invokeLater(() -> {
                textField.setText(text == null ? "" : text);
            });
        }

        textField.setToolTipText(presentation.getDescription());

        Object background = presentation.getClientProperty(TEXT_FIELD_BACKGROUND);
        if (background instanceof Color) {
            textField.setBackground((Color) background);
        }
        //textField.setMnemonic(presentation.getMnemonic());
        //textField.setDisplayedMnemonicIndex(presentation.getDisplayedMnemonicIndex());
        //textField.setSelected(Boolean.TRUE.equals(presentation.getClientProperty(SELECTED_PROPERTY)));
        textField.setEnabled(false);
        textField.setVisible(presentation.isVisible());
    }
}
