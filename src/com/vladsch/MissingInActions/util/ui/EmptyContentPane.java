/*
 * Copyright (c) 2016-2016 Vladimir Schneider <vladimir.schneider@gmail.com>
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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBUI;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.Utils;

import javax.swing.*;
import java.awt.*;

public class EmptyContentPane {
    private final ApplicationSettings mySettings;
    public JTextPane myTextPane;
    public JPanel myPanel;
    private JBCheckBox myShowInstructions;
    private JBCheckBox myShowTerminatingEOL;
    private Runnable mySettingsChangedRunnable;
    private String myTextContent;

    public EmptyContentPane() {
        mySettingsChangedRunnable = null;
        mySettings = ApplicationSettings.getInstance();
        myTextContent = "";

        myShowInstructions.addActionListener(event -> {
            updateTextPane();
        });

        myShowTerminatingEOL.addActionListener(e -> {
            mySettings.setMultiPasteShowEOL(myShowTerminatingEOL.isSelected());
            if (mySettingsChangedRunnable != null) {
                mySettingsChangedRunnable.run();
            }
        });

        myShowInstructions.setSelected(mySettings.isMultiPasteShowInstructions());
        myTextPane.setVisible(myShowInstructions.isSelected());
        myShowTerminatingEOL.setSelected(mySettings.isMultiPasteShowEOL());
    }

    private void updateTextPane() {
        mySettings.setMultiPasteShowInstructions(myShowInstructions.isSelected());
        myTextPane.setVisible(myShowInstructions.isSelected());
        myTextPane.validate();
        myPanel.validate();
        myPanel.getParent().validate();
    }

    public void setSettingsChangedRunnable(final Runnable settingsChangedRunnable) {
        mySettingsChangedRunnable = settingsChangedRunnable;
    }

    public boolean getShowInstructions() {
        return myShowInstructions.isSelected();
    }

    public void setShowInstructions(final boolean showInstructions) {
        myShowInstructions.setSelected(showInstructions);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    public void setContentBody(String text) {
        JLabel label = new JLabel();
        Font font = label.getFont();
        Color textColor = label.getForeground();
        String out = "<html><head></head><body><div style='font-family:" + font.getFontName() + ";" + "font-size:" + JBUI.scale(font.getSize()) + "pt; color:" + Utils.toRgbString(textColor) + "'>" +
                (text == null ? "" : text) +
                "</div></body></html>";
        myTextContent = out;
        myTextPane.setText(out);
        myPanel.validate();
    }

    public void invalidate() {
        myPanel.invalidate();
        myTextPane.invalidate();
    }
}
