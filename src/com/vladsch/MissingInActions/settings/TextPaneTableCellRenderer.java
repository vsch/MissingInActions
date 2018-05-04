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
package com.vladsch.MissingInActions.settings;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class TextPaneTableCellRenderer extends JPanel implements TableCellRenderer {
    public static final TableCellRenderer INSTANCE = new TextPaneTableCellRenderer();

    /**
     * DefaultTableCellRenderer, that displays JTextPane on selected value.
     */
    public static final TableCellRenderer TEXT_PANE_RENDERER = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    };

    private static final Logger LOG = Logger.getInstance("#com.intellij.util.ui.ComboBoxTableCellRenderer");

    private final JTextPane myPane = new JTextPane();

    public TextPaneTableCellRenderer() {
        this(null);
    }

    public TextPaneTableCellRenderer(@Nullable String contentType) {
        super(new GridBagLayout());
        if (contentType != null) {
            myPane.setContentType(contentType);
        }

        add(myPane,
                new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0));
    }

    public void setText(@Nullable String text) {
        myPane.setText(text);
    }

    public @NotNull
    String getText() {
        return myPane.getText();
    }

    @Override
    public TextPaneTableCellRenderer getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        myPane.setText((String) value);
        return this;
    }

    public JTextPane getTextComponent() {
        return myPane;
    }
}
