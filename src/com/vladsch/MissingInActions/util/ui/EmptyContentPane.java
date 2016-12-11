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

import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBUI;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.Utils;

import javax.swing.*;
import java.awt.*;

public class EmptyContentPane {
    public JTextPane myTextPane;
    public JPanel myPanel;
    private JBCheckBox myDoNotShowInstructions;

    public EmptyContentPane() {
        myDoNotShowInstructions.addActionListener(event->{
            ApplicationSettings.getInstance().setOverrideStandardPasteShowInstructions(!myDoNotShowInstructions.isSelected());
            myTextPane.setVisible(!myDoNotShowInstructions.isSelected());
        });
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
        myTextPane.setText(out);
        myPanel.validate();
    }

    public void invalidate() {
        myPanel.invalidate();
        myTextPane.invalidate();
    }
}
