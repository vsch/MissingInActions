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

import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.TableView;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ElementProducer;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.UIUtil;
import com.vladsch.MissingInActions.Bundle;
import com.vladsch.ReverseRegEx.util.ForwardPattern;
import com.vladsch.ReverseRegEx.util.RegExMatcher;
import com.vladsch.ReverseRegEx.util.RegExPattern;
import com.vladsch.ReverseRegEx.util.ReversePattern;
import com.vladsch.flexmark.util.html.ui.BackgroundColor;
import com.vladsch.flexmark.util.html.ui.Color;
import com.vladsch.flexmark.util.html.ui.HtmlBuilder;
import com.vladsch.flexmark.util.html.ui.HtmlHelpers;
import com.vladsch.plugin.util.AppUtils;
import com.vladsch.plugin.util.ui.Helpers;
import com.vladsch.plugin.util.ui.Settable;
import com.vladsch.plugin.util.ui.SettingsComponents;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractCellEditor;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class CustomDeleteBackspaceForm {
    JPanel myMainPanel;
    JPanel myViewPanel;
    JBTextField myDeleteSpacesRegEx;
    JBTextField myDeleteAlternatingRegEx;
    JBTextField myDeleteWordRegEx;
    JBTextField myBackspaceSpacesRegEx;
    JBTextField myBackspaceAlternatingRegEx;
    JBTextField myBackspaceWordRegEx;
    JBTextField myDeleteWordExcludingSpaceRegEx;
    JBTextField myBackspaceWordExcludingSpaceRegEx;
    JBCheckBox myDeleteLineBound;
    JBCheckBox myDeleteMultiCaretLineBound;
    JBCheckBox myBackspaceLineBound;
    JBCheckBox myBackspaceMultiCaretLineBound;
    private JTextPane myErrorTextPane;
    private JTextField mySampleText;
    private ListTableModel<TextMapEntry> myTextModel;
    private TableView<TextMapEntry> myTextTable;

    private JBTextField myFocusedRegEx = null;
    private final List<JBTextField> myRegExFields;
    private boolean myFocusedRegExReversed = false;

    private final SettingsComponents<ApplicationSettings> components;
    private final String CARET_COLOR_STRING = "#CC0030";
    private final Color CARET_COLOR = Color.of(CARET_COLOR_STRING);

    private static class MyAbstractHookingTableEditor extends AbstractHookingTableEditor {
        private final JBTextField myEditor = new JBTextField();

        public Object getCellEditorValue() {
            return myEditor.getText();
        }

        @Override
        public JComponent getEditorComponent() {
            return myEditor;
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            myEditor.setText((String) value);
            return myEditor;
        }
    }

    private class RegExDocumentListener implements DocumentListener {
        @Override
        public void insertUpdate(final DocumentEvent e) {
            updateResult();
        }

        @Override
        public void removeUpdate(final DocumentEvent e) {
            updateResult();
        }

        @Override
        public void changedUpdate(final DocumentEvent e) {
            updateResult();
        }
    }

    private class RegExFocusListener implements FocusListener {
        @Override
        public void focusGained(final FocusEvent e) {
            if (e.getComponent() instanceof JBTextField) {
                myFocusedRegEx = (JBTextField) e.getComponent();
                myFocusedRegExReversed = (myRegExFields.indexOf(myFocusedRegEx) & 1) != 0;
                updateResult();
            }
        }

        @Override
        public void focusLost(final FocusEvent e) {
            myFocusedRegEx = null;
            updateResult();
        }
    }

    public CustomDeleteBackspaceForm() {
        //Color background = myMainPanel.getBackground();
        myRegExFields = new ArrayList<>();
        myRegExFields.add(myDeleteSpacesRegEx);
        myRegExFields.add(myBackspaceSpacesRegEx);
        myRegExFields.add(myDeleteAlternatingRegEx);
        myRegExFields.add(myBackspaceAlternatingRegEx);
        myRegExFields.add(myDeleteWordExcludingSpaceRegEx);
        myRegExFields.add(myBackspaceWordExcludingSpaceRegEx);
        myRegExFields.add(myDeleteWordRegEx);
        myRegExFields.add(myBackspaceWordRegEx);

        components = new SettingsComponents<ApplicationSettings>() {
            @Override
            protected Settable[] createComponents(@NotNull ApplicationSettings i) {
                return new Settable[] {
                        component(myDeleteSpacesRegEx, i::getDeleteSpacesRegEx, i::setDeleteSpacesRegEx),
                        component(myDeleteAlternatingRegEx, i::getDeleteAlternatingRegEx, i::setDeleteAlternatingRegEx),
                        component(myDeleteWordRegEx, i::getDeleteWordRegEx, i::setDeleteWordRegEx),
                        component(myBackspaceSpacesRegEx, i::getBackspaceSpacesRegEx, i::setBackspaceSpacesRegEx),
                        component(myBackspaceAlternatingRegEx, i::getBackspaceAlternatingRegEx, i::setBackspaceAlternatingRegEx),
                        component(myBackspaceWordRegEx, i::getBackspaceWordRegEx, i::setBackspaceWordRegEx),
                        component(myDeleteWordExcludingSpaceRegEx, i::getDeleteWordExcludingSpaceRegEx, i::setDeleteWordExcludingSpaceRegEx),
                        component(myBackspaceWordExcludingSpaceRegEx, i::getBackspaceWordExcludingSpaceRegEx, i::setBackspaceWordExcludingSpaceRegEx),

                        component(myDeleteLineBound, i::isDeleteLineBound, i::setDeleteLineBound),
                        component(myDeleteMultiCaretLineBound, i::isDeleteMultiCaretLineBound, i::setDeleteMultiCaretLineBound),
                        component(myBackspaceLineBound, i::isBackspaceLineBound, i::setBackspaceLineBound),
                        component(myBackspaceMultiCaretLineBound, i::isBackspaceMultiCaretLineBound, i::setBackspaceMultiCaretLineBound),
                };
            }
        };

        RegExFocusListener focusListener = new RegExFocusListener();
        DocumentListener documentListener = new RegExDocumentListener();
        for (JBTextField regExField : myRegExFields) {
            regExField.addFocusListener(focusListener);
            regExField.getDocument().addDocumentListener(documentListener);
        }

        // set row height to fit one line, each row height will need adjusting based on the test results
        if (myTextTable.getRowHeight() != mySampleText.getPreferredSize().height + ROW_HEIGHT_OFFSET * 2) myTextTable.setRowHeight(mySampleText.getPreferredSize().height + ROW_HEIGHT_OFFSET);

        myErrorTextPane.setVisible(false);
        mySampleText.setVisible(false);
    }

    public void reset(ApplicationSettings settings) {
        components.reset(settings);

        // Need to copy test samples
        String[] tests = settings.myDeleteBackspaceTests;
        String[] carets = settings.myDeleteBackspaceTestCaretMarkers;
        ArrayList<TextMapEntry> textMapEntries = new ArrayList<>(tests.length);
        int iMax = tests.length;
        for (int i = 0; i < iMax; i++) {
            String test = tests[i];
            String caretMarker = carets.length > i && !carets[i].isEmpty() ? carets[i] : "|";
            textMapEntries.add(new TextMapEntry(caretMarker, test, test));
        }
        myTextModel.setItems(textMapEntries);
        updateResult();
    }

    public boolean isModified(ApplicationSettings settings) {
        if (components.isModified(settings)) return true;
        String[] tests = settings.myDeleteBackspaceTests;
        String[] carets = settings.myDeleteBackspaceTestCaretMarkers;
        List<TextMapEntry> textMapEntries = myTextModel.getItems();
        if (tests.length != textMapEntries.size()) return true;

        int iMax = tests.length;
        for (int i = 0; i < iMax; i++) {
            if (!tests[i].equals(textMapEntries.get(i).myTestSample)) {
                return true;
            }

            String caret = carets.length > i && !carets[i].isEmpty() ? carets[i] : "|";
            String caretMarker = textMapEntries.get(i).getCaretMarker();
            if (!caret.equals(caretMarker)) {
                return true;
            }
        }
        return false;
    }

    public void apply(ApplicationSettings settings) {
        components.apply(settings);
        ArrayList<String> tests = new ArrayList<>();
        ArrayList<String> carets = new ArrayList<>();
        for (TextMapEntry test : myTextModel.getItems()) {
            if (!test.myTestSample.isEmpty()) {
                tests.add(test.myTestSample);
                carets.add(test.getCaretMarker());
            }
        }
        settings.myDeleteBackspaceTests = tests.toArray(new String[0]);
        settings.myDeleteBackspaceTestCaretMarkers = carets.toArray(new String[0]);
    }

    static class TextMapEntry {
        String myCaretMarker;
        String myTestSample;
        String myTestResults;

        public TextMapEntry(final String caretMarker, final String testSample, final String testResults) {
            myCaretMarker = caretMarker;
            myTestSample = testSample;
            myTestResults = testResults;
        }

        @NotNull
        String getCaretMarker() {
            return myCaretMarker.isEmpty() ? "|" : myCaretMarker;
        }

        @NotNull
        String getTestSample(boolean reverseSearch) {
            String caretMarker = getCaretMarker();
            int pos = myTestSample.indexOf(caretMarker);
            if (pos == -1) {
                return reverseSearch ? myTestSample + caretMarker : caretMarker + myTestSample;
            }
            return myTestSample;
        }
    }

    private BackgroundColor getSelectedTextFieldBackground() {
        return BackgroundColor.of(mySampleText.getSelectionColor());
    }

    public interface OnElementTypeChange {
        void onElementTypeChange(CustomDeleteBackspaceForm entryGroupConfigurable);
    }

    static String getRegExError(final String regEx) {
        try {
            Pattern pattern = Pattern.compile(regEx);
            return null;
        } catch (PatternSyntaxException e) {
            return e.getMessage();
        }
    }

    int updateTestResult(TextMapEntry entry, String regEx, boolean isReversed) {
        String sample = entry.getTestSample(isReversed);
        RegExPattern pattern = isReversed ? ReversePattern.compile("(?:" + regEx + ")$") : ForwardPattern.compile("^(?:" + regEx + ")");
        int caretPos = isReversed ? sample.lastIndexOf(entry.getCaretMarker()) : sample.indexOf(entry.getCaretMarker());

        HtmlBuilder html = new HtmlBuilder();
        BackgroundColor validTextFieldBackground = AppUtils.getValidTextFieldBackground();
        BackgroundColor warningTextFieldBackground = AppUtils.getWarningTextFieldBackground();
        BackgroundColor selectedTextFieldBackground = getSelectedTextFieldBackground();

        html.tag("html").style("margin:2px;vertical-align:top").attr(validTextFieldBackground, mySampleText.getFont()).tag("body");

        sample = sample.substring(0, caretPos) + sample.substring(caretPos + 1);
        int lineCount = 0;

        while (true) {
            RegExMatcher matcher = pattern.matcher(sample);
            if (isReversed) {
                matcher.region(0, caretPos);
            } else {
                matcher.region(caretPos, sample.length());
            }
            matcher.useTransparentBounds(true);

            String nextSample = sample;

            if (isReversed) {
                if (matcher.find()) {
                    // have a match
                    if (matcher.start() > 0) {
                        html.span().append(sample.substring(0, matcher.start()).replace(" ", "␠").replace("\t", "␉")).closeSpan();
                    }

                    html.attr(selectedTextFieldBackground).span().append(sample.substring(matcher.start(), caretPos).replace(" ", "␠").replace("\t", "␉")).closeSpan();
                    nextSample = sample.substring(0, matcher.start()) + sample.substring(caretPos);

                    html.attr(CARET_COLOR).span("|");

                    if (caretPos < sample.length()) {
                        html.span().append(sample.substring(caretPos).replace(" ", "␠").replace("\t", "␉")).closeSpan();
                    }

                    caretPos = matcher.start();
                } else {
                    if (caretPos > 0) {
                        html.span().append(sample.substring(0, caretPos).replace(" ", "␠").replace("\t", "␉")).closeSpan();
                    }

                    html.attr(CARET_COLOR).span("|");

                    if (caretPos < sample.length()) {
                        html.span().append(sample.substring(caretPos).replace(" ", "␠").replace("\t", "␉")).closeSpan();
                    }
                }
            } else {
                if (caretPos > 0) {
                    html.span().append(sample.substring(0, caretPos).replace(" ", "␠").replace("\t", "␉")).closeSpan();
                }

                html.attr(CARET_COLOR).span("|");

                int lastPos = caretPos;
                if (matcher.find()) {
                    // have a match
                    html.attr(selectedTextFieldBackground).span().append(sample.substring(lastPos, matcher.end()).replace(" ", "␠").replace("\t", "␉")).closeSpan();
                    lastPos = matcher.end();
                    nextSample = sample.substring(0, caretPos) + sample.substring(matcher.end());
                }
                if (lastPos < sample.length()) {
                    html.span().append(sample.substring(lastPos).replace(" ", "␠").replace("\t", "␉")).closeSpan();
                }
            }

            html.append("<br/>\n");
            lineCount++;
            if (nextSample.equals(sample)) break;
            sample = nextSample;
        }
        html.closeTag("body");
        html.closeTag("html");

        entry.myTestResults = html.toFinalizedString();
        return lineCount;
    }

    // validate regex fields and for focused one show potential errors, if valid show sample results
    void updateResult() {
        Color validBackground = Color.of(AppUtils.getValidTextFieldBackground());
        Color warningBackground = Color.of(AppUtils.getWarningTextFieldBackground());

        boolean errorPaneVisible = false;
        for (JBTextField regExField : myRegExFields) {
            String regExError = getRegExError(regExField.getText());
            if (regExError != null) {
                if (myFocusedRegEx == regExField) {
                    // can display full error
                    errorPaneVisible = true;
                    HtmlHelpers.setRegExError(regExError, myErrorTextPane, mySampleText.getFont(), AppUtils.getValidTextFieldBackground(), AppUtils.getWarningTextFieldBackground());
                }

                regExField.setBackground(warningBackground);
            } else {
                if (myFocusedRegEx == regExField) {
                    // need to update sample results for this regex
                    final int lineHeight = mySampleText.getFontMetrics(mySampleText.getFont()).getHeight();
                    int rowNumber = 0;
                    for (TextMapEntry entry : myTextModel.getItems()) {
                        int rowLines = updateTestResult(entry, regExField.getText(), myFocusedRegExReversed);
                        myTextTable.setRowHeight(rowNumber, Math.max(lineHeight * rowLines, mySampleText.getPreferredSize().height) + ROW_HEIGHT_OFFSET);
                        rowNumber++;
                    }
                }
                regExField.setBackground(validBackground);
            }
        }

        if (!errorPaneVisible) myErrorTextPane.setVisible(false);

        //myViewPanel.setVisible(!errorPaneVisible);
        myTextTable.repaint();
    }

    public static final int ROW_HEIGHT_OFFSET = 2;

    private void createUIComponents() {
        ElementProducer<TextMapEntry> producer = new TextMapEntryElementProducer();

        GridConstraints constraints = new GridConstraints(0, 0, 1, 1
                , GridConstraints.ANCHOR_CENTER
                , GridConstraints.FILL_BOTH
                , GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK
                , GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK
                , null, null, null);

        ColumnInfo[] linkTextColumns = { new CaretMarkerColumn(), new SampleTextColumn(), new ResultColumn() };
        myTextModel = new ListTableModel<>(linkTextColumns, new ArrayList<>(), 0);
        myTextTable = new TableView<>(myTextModel);
        myTextTable.setPreferredScrollableViewportSize(JBUI.size(-1, 50));
        myTextTable.setRowSelectionAllowed(false);

        myViewPanel = new JPanel(new GridLayoutManager(1, 1));
        ToolbarDecorator linkTextDecorator = ToolbarDecorator.createDecorator(myTextTable, producer);
        myViewPanel.add(linkTextDecorator.createPanel(), constraints);

        myTextModel.addTableModelListener(e -> updateResult());
    }

    private static class TextMapEntryElementProducer implements ElementProducer<TextMapEntry> {
        @Override
        public TextMapEntry createElement() {
            return new TextMapEntry("|", "", "");
        }

        @Override
        public boolean canCreateElement() {
            return true;
        }
    }

    class CaretMarkerColumn extends MyColumnInfo<String> {
        CaretMarkerColumn() {
            super(Bundle.message("delete-backspace.test-caret-marker.label"));
        }

        public TableCellRenderer getRenderer(final TextMapEntry linkMapEntry) {
            return new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    final Component rendererComponent = super.getTableCellRendererComponent(table, value, false, false, row, column);
                    ((JComponent) rendererComponent).setAlignmentY(BOTTOM_ALIGNMENT);
                    setText(linkMapEntry.getCaretMarker());
                    return rendererComponent;
                }
            };
        }

        public TableCellEditor getEditor(final TextMapEntry linkMapEntry) {
            return new MyAbstractHookingTableEditor();
        }

        @Override
        public boolean isCellEditable(final TextMapEntry item) {
            return true;
        }

        public String valueOf(final TextMapEntry object) {
            return object.myCaretMarker;
        }

        public void setValue(final TextMapEntry mapEntry, final String value) {
            if (mapEntry != null) {
                mapEntry.myCaretMarker = value;
            }
        }

        @Override
        public int getWidth(JTable table) {
            return 100;
        }
    }

    class SampleTextColumn extends MyColumnInfo<String> {
        SampleTextColumn() {
            super(Bundle.message("delete-backspace.test-sample.label"));
        }

        public TableCellRenderer getRenderer(final TextMapEntry linkMapEntry) {
            return new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    final Component rendererComponent = super.getTableCellRendererComponent(table, value, false, false, row, column);
                    ((JComponent) rendererComponent).setAlignmentY(TOP_ALIGNMENT);
                    setText("<html><body>" + linkMapEntry.getTestSample(myFocusedRegExReversed).replace(" ", "&nbsp;").replace(linkMapEntry.getCaretMarker(), "<strong style='color:" + CARET_COLOR_STRING + "'>|</strong>") + "</body></html>");
                    return rendererComponent;
                }
            };
        }

        public TableCellEditor getEditor(final TextMapEntry linkMapEntry) {
            return new MyAbstractHookingTableEditor();
        }

        @Override
        public boolean isCellEditable(final TextMapEntry item) {
            return true;
        }

        public String valueOf(final TextMapEntry object) {
            return object.myTestSample;
        }

        public void setValue(final TextMapEntry mapEntry, final String value) {
            if (mapEntry != null) {
                mapEntry.myTestSample = value;
            }
        }
    }

    class ResultColumn extends MyColumnInfo<String> {
        ResultColumn() {
            super(Bundle.message("delete-backspace.test-result.label"));
        }

        public TableCellRenderer getRenderer(final TextMapEntry linkMapEntry) {
            return new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    final Component rendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    ((JComponent) rendererComponent).setAlignmentY(TOP_ALIGNMENT);
                    setText("<html><body>" + linkMapEntry.myTestResults.replace(linkMapEntry.getCaretMarker(), "<strong style='color:" + CARET_COLOR_STRING + "'>|</strong>") + "</body></html>");
                    return rendererComponent;
                }
            };
        }

        public String valueOf(final TextMapEntry object) {
            return object.myTestResults;
        }

        @Override
        public boolean isCellEditable(final TextMapEntry item) {
            return false;
        }
    }

    protected interface HookingCellEditor extends TableCellEditor {
        JComponent getEditorComponent();
    }

    static abstract class AbstractHookingTableEditor extends AbstractCellEditor implements HookingCellEditor {
    }

    private abstract class MyColumnInfo<T> extends ColumnInfo<TextMapEntry, T> {
        final JTable myTable = getTable();

        JTable getTable() {
            return myTextTable;
        }

        protected MyColumnInfo(final String name) {
            super(name);
        }
    }
}
