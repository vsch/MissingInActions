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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.*;
import com.vladsch.MissingInActions.Bundle;
import com.vladsch.flexmark.util.DelimitedBuilder;
import com.vladsch.flexmark.util.html.ui.BackgroundColor;
import com.vladsch.flexmark.util.html.ui.HtmlBuilder;
import com.vladsch.flexmark.util.html.ui.HtmlHelpers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vladsch.plugin.util.ui.Helpers.errorColor;
import static com.vladsch.plugin.util.ui.Helpers.warningColor;

public class RegExTestDialog extends DialogWrapper {
    JPanel myMainPanel;
    private JTextField myPattern;
    JTextField mySampleText;
    private JPanel myViewPanel;
    private JTextPane myTextPane;
    private JPanel myTablesPanel;
    private ListTableModel<RegExSampleSet> myTextModel;
    TableView<RegExSampleSet> myTextTable;
    private boolean myIsBadRegEx;

    @SuppressWarnings("FieldCanBeLocal") private @NotNull String myOriginalSampleText;
    @SuppressWarnings("FieldCanBeLocal") private @NotNull String myOriginalPatternText;
    private int myTableRowPadding = 0;
    private boolean myIsBadRegex;

    static class RegExSampleSet {
        @NotNull String sample;
        @NotNull String resultHtml;
        @Nullable String toolTipText;

        public RegExSampleSet() {
            sample = "";
            resultHtml = "";
            toolTipText = null;
        }

        public RegExSampleSet(@NotNull String sample) {
            this.sample = sample;
            resultHtml = "";
            toolTipText = null;
        }
    }

    private final RegExSettingsHolder mySettingsHolder;

    private BackgroundColor getInvalidTextFieldBackground() {
        return BackgroundColor.of(errorColor(UIUtil.getTextFieldBackground()));
    }

    private BackgroundColor getWarningTextFieldBackground() {
        return BackgroundColor.of(warningColor(UIUtil.getTextFieldBackground()));
    }

    private BackgroundColor getValidTextFieldBackground() {
        return BackgroundColor.of(UIUtil.getTextFieldBackground());
    }

    private BackgroundColor getSelectedTextFieldBackground() {
        return BackgroundColor.of(mySampleText.getSelectionColor());
    }

    private BackgroundColor getInvalidTableBackground(boolean isSelected) {
        return BackgroundColor.of(errorColor(UIUtil.getTableBackground(isSelected)));
    }

    BackgroundColor getTableBackground(boolean isSelected) {
        return BackgroundColor.of(UIUtil.getTableBackground(isSelected));
    }

    public boolean saveSettings(boolean onlySamples) {
        // save settings return false if regex is not valid
        mySettingsHolder.setSampleText(mySampleText.getText());

        if (!onlySamples) {
            mySettingsHolder.setPatternText(myPattern.getText().trim());
        }

        return onlySamples || updateResults().isEmpty();
    }

    public RegExTestDialog(JComponent parent, @NotNull RegExSettingsHolder settingsHolder) {
        super(parent, false);

        mySettingsHolder = settingsHolder;

        myOriginalSampleText = settingsHolder.getSampleText();
        myOriginalPatternText = settingsHolder.getPatternText();

        mySampleText.setText(myOriginalSampleText);
        myPattern.setText(myOriginalPatternText);

        mySampleText.setVisible(false);

        final DocumentAdapter listener = new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                validateResults(false);
            }
        };

        myPattern.getDocument().addDocumentListener(listener);

        myTextModel.addTableModelListener(e -> {
            updateResults();
        });

        validateResults(true);

        init();
    }

    private void validateResults(boolean init) {
        List<RegExSampleSet> sampleSets;
        if (init) {
            String[] samples = mySampleText.getText().split(ApplicationSettings.PREFIX_SPLIT_REGEX);
            sampleSets = new ArrayList<>();
            for (String sample : samples) {
                if (!sample.isEmpty()) {
                    RegExSampleSet sampleSet = new RegExSampleSet(sample);
                    sampleSets.add(sampleSet);
                }
            }
            myTextModel.setItems(sampleSets);
        } else {
            sampleSets = new ArrayList<>(myTextModel.getItems());
        }

        for (RegExSampleSet sampleSet : sampleSets) {
            checkRegEx(myPattern, sampleSet);
            if (myIsBadRegEx) break;
        }

        myTextTable.repaint();
    }

    private String updateResults() {
        DelimitedBuilder error = new DelimitedBuilder("\n");
        DelimitedBuilder samples = new DelimitedBuilder(ApplicationSettings.PREFIX_DELIMITER);
        for (RegExSampleSet sampleSet : myTextModel.getItems()) {
            if (!sampleSet.sample.isEmpty()) {
                samples.append(sampleSet.sample).mark();
                String err = checkRegEx(myPattern, sampleSet);
                if (!err.isEmpty()) {
                    error.append(err).mark();
                }
                if (myIsBadRegEx) break;
            }
        }
        mySampleText.setText(samples.toString());
        return error.toString();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return myMainPanel;
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        super.createDefaultActions();
        return new Action[] { getOKAction(), getCancelAction() };
    }

    public static boolean showDialog(JComponent parent, @NotNull RegExSettingsHolder settingsHolder) {
        RegExTestDialog dialog = new RegExTestDialog(parent, settingsHolder);
        boolean save = dialog.showAndGet();
        return dialog.saveSettings(!save);
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        String error = updateResults();

        if (!error.isEmpty()) {
            return new ValidationInfo(error, myPattern);
        }
        return super.doValidate();
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return "MissingInActions.RegExTestDialog";
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return myPattern;
    }

    private String checkRegEx(final JTextField pattern, RegExSampleSet sampleSet) {
        final String patternText = pattern.getText().trim();
        Color validBackground = getValidTextFieldBackground();
        Color selectedBackground = getSelectedTextFieldBackground();
        Color invalidBackground = getInvalidTextFieldBackground();
        Color warningBackground = getWarningTextFieldBackground();
        String error = "";
        String warning = "";

        Pattern regexPattern;
        Matcher matcher = null;
        final String text = sampleSet.sample.trim();
        myIsBadRegEx = false;

        if (!myPattern.getText().trim().isEmpty()) {
            try {
                regexPattern = Pattern.compile(patternText);
                matcher = regexPattern.matcher(text);
                if (matcher.find()) {
                    if (matcher.start() != 0) {
                        error = Bundle.message("regex.matched-middle.description");
                    } else {
                        // see if it will match in the middle
                        Matcher matcher2 = regexPattern.matcher(text + text);
                        if (matcher2.find() && matcher2.find()) {
                            warning = Bundle.message("regex.also-matches-middle.description");
                        }
                    }
                } else {
                    error = "not matched";
                }
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                error = e.getMessage();
                myIsBadRegEx = true;
            }
        } else {
            error = "empty pattern";
        }

        if (error.isEmpty() && matcher != null) {
            // have match
            HtmlBuilder html = new HtmlBuilder();
            html.tag("html").style("margin:2px").attr(getValidTextFieldBackground(), mySampleText.getFont()).tag("body");
            html.attr(warning.isEmpty() ? selectedBackground : warningBackground).span(text.substring(0, matcher.end()));
            html.span(text.substring(matcher.end()));
            html.closeTag("body");
            html.closeTag("html");

            myViewPanel.setVisible(true);
            myTextPane.setVisible(false);
            sampleSet.resultHtml = html.toFinalizedString();
            sampleSet.toolTipText = warning;
        } else if (!myIsBadRegEx) {
            HtmlBuilder html = new HtmlBuilder();
            html.tag("html").style("margin:2px;vertical-align:middle;").attr(getValidTextFieldBackground(), mySampleText.getFont()).tag("body");
            if (matcher == null || error.equals("not matched")) {
                html.attr(error.equals("not matched") ? invalidBackground : warningBackground).span(text);
            } else {
                html.span(text.substring(0, matcher.start()));
                html.attr(invalidBackground).span(text.substring(matcher.start(), matcher.end()));
                html.span(text.substring(matcher.end()));
            }
            html.closeTag("body");
            html.closeTag("html");

            myViewPanel.setVisible(true);
            myTextPane.setVisible(false);
            sampleSet.resultHtml = html.toFinalizedString();
            sampleSet.toolTipText = error.isEmpty() ? null : error;
        } else {
            myViewPanel.setVisible(false);
            HtmlHelpers.setRegExError(error, myTextPane, mySampleText.getFont(), getValidTextFieldBackground(), getWarningTextFieldBackground());
        }
        return error;
    }

    private void createUIComponents() {
        ElementProducer<RegExSampleSet> producer = new ElementProducer<RegExSampleSet>() {
            @Override
            public RegExSampleSet createElement() {
                return new RegExSampleSet();
            }

            @Override
            public boolean canCreateElement() {
                return true;
            }
        };

        GridConstraints constraints = new GridConstraints(0, 0, 1, 1
                , GridConstraints.ANCHOR_CENTER
                , GridConstraints.FILL_BOTH
                , GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW
                , GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW
                , null, null, null);

        ColumnInfo[] sampleTextColumns = { new SampleColumn(), new ResultColumn() };
        myTextModel = new ListTableModel<>(sampleTextColumns, new ArrayList<>(), 0);
        myTextTable = new TableView<RegExSampleSet>(myTextModel) {
            @Override
            public void editingCanceled(ChangeEvent e) {
                super.editingCanceled(e);
                ApplicationManager.getApplication().invokeLater(() -> validateResults(false));
            }
        };

        myTextTable.setPreferredScrollableViewportSize(JBUI.size(-1, 500));
        myTextTable.setRowSelectionAllowed(true);

        //int height = (int) myTextTable.getTableHeader().getPreferredSize().getHeight() + 10;
        //myTextTable.setRowHeight(height);
        int height = myTextTable.getRowHeight();
        myTableRowPadding = 8;
        myTextTable.setRowHeight(height + myTableRowPadding);

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myTextTable, producer);
        myTablesPanel = new JPanel(new GridLayoutManager(1, 1));
        myTablesPanel.add(decorator.createPanel(), constraints);

        myViewPanel = new JPanel(new BorderLayout());
        myViewPanel.add(myTablesPanel, BorderLayout.CENTER);
    }

    class SampleColumn extends MyColumnInfo<String> {

        SampleColumn() {
            super(Bundle.message("settings.regex.sample.label"));
        }

        @Override
        public TableCellRenderer getRenderer(final RegExSampleSet item) {
            return new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    final Component rendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    setText(item.sample);
                    return rendererComponent;
                }
            };
        }

        @Override
        public boolean isCellEditable(RegExSampleSet set) {
            return true;
        }

        @Override
        public TableCellEditor getEditor(final RegExSampleSet item) {
            JTextField textField = new JTextField();

            textField.getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(DocumentEvent e) {
                    String sample = item.sample;
                    item.sample = textField.getText();
                    checkRegEx(myPattern, item);
                    item.sample = sample;
                    myTextTable.repaint();
                }
            });

            return new DefaultCellEditor(textField) {
                @Override
                public boolean isCellEditable(EventObject anEvent) {
                    return true;
                }
            };
        }

        @Override
        public String valueOf(final RegExSampleSet object) {
            return object.sample;
        }

        @Override
        public void setValue(final RegExSampleSet item, final String value) {
            if (item != null) {
                item.sample = value;
                checkRegEx(myPattern, item);
                myTextTable.repaint();
            }
        }
    }

    class ResultColumn extends MyColumnInfo<String> {
        private String tooltipText;

        ResultColumn() {
            super(Bundle.message("settings.regex.results.label"));
            tooltipText = null;
        }

        @Override
        public TableCellRenderer getRenderer(final RegExSampleSet item) {
            TextPaneTableCellRenderer cellRenderer = new TextPaneTableCellRenderer("text/html") {
                private Color myInvalidBackground = getInvalidTableBackground(false);
                private Color myInvalidSelectedBackground = getInvalidTableBackground(true);

                public TextPaneTableCellRenderer getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    final TextPaneTableCellRenderer rendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                    tooltipText = item.toolTipText;
                    String resultHtml = item.resultHtml;
                    if (myTableRowPadding > 0) {
                        resultHtml = resultHtml.replace("margin:2px", "margin:" + String.valueOf(myTableRowPadding / 2) + "px 2px 0 2px");
                    }
                    rendererComponent.setText(resultHtml);
                    return rendererComponent;
                }

                @Override
                public String getToolTipText() {
                    return tooltipText != null ? tooltipText : super.getToolTipText();
                }
            };

            JTextPane textPane = cellRenderer.getTextComponent();
            textPane.setFont(mySampleText.getFont());
            textPane.setBackground(mySampleText.getBackground());
            textPane.setOpaque(true);
            return cellRenderer;
        }

        @Override
        public TableCellEditor getEditor(final RegExSampleSet item) {
            return null;
        }

        @Override
        public String valueOf(final RegExSampleSet object) {
            return object.resultHtml;
        }

        @Override
        public void setValue(final RegExSampleSet item, final String value) {
            if (item != null) {
                item.resultHtml = value;
            }
        }
    }

    private abstract class MyColumnInfo<T> extends ColumnInfo<RegExSampleSet, T> {
        MyColumnInfo(final String name) {
            super(name);
        }
    }
}
