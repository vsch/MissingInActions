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

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.wm.impl.FocusManagerImpl;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.MissingInActions.util.MiaCancelableJobScheduler;
import com.vladsch.flexmark.util.html.ui.HtmlHelpers;
import com.vladsch.plugin.util.CancellableRunnable;
import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class MultiPasteOptionsPane {
    final ApplicationSettings mySettings;
    public JTextPane myTextPane;
    public JPanel myPanel;
    JBCheckBox myShowInstructions;
    JBCheckBox myMultiPasteShowEolInViewer;
    JBCheckBox myMultiPasteShowEolInList;
    JBCheckBox myMultiPastePreserveOriginal;
    JBCheckBox myMultiPasteDeleteRepeatedCaretData;
    JBCheckBox myReplaceMacroVariables;
    JBCheckBox myIncludeUserDefinedMacro;
    JBCheckBox myRegexUserDefinedMacro;
    JBCheckBox myUserDefinedMacroClipContent;
    JBCheckBox myUserDefinedMacroSmartReplace;
    JTextField myUserDefinedMacroSearch;
    JTextField myUserDefinedMacroReplace;
    JBCheckBox myShowMacroResultPreview;
    JComboBox<String> myUserDefinedMacroReplaceClipContent;
    JBTextField mySpliceDelimiterText;
    JBTextField myOpenQuoteText;
    JBTextField myClosedQuoteText;
    JLabel mySpliceDelimiterTextLabel;
    private Consumer<Integer> mySettingsChangedRunnable;
    private String myTextContent;
    private Border myNormalBorder;
    private Border myHighlightBorder;
    private Border myNormalPanelBorder;
    private List<Integer> myUserDefinedMacroReplaceClipIndices = Collections.emptyList();
    CancellableRunnable myViewerUpdater = CancellableRunnable.NULL;

    final public static int LIST_CHANGED = 1;
    final public static int USER_MACRO_CHANGED = 2;
    final public static int OTHER_CHANGED = 4;

    public MultiPasteOptionsPane() {
        mySettingsChangedRunnable = null;
        mySettings = ApplicationSettings.getInstance();
        myTextContent = "";

        myShowInstructions.setSelected(mySettings.isMultiPasteShowInstructions());
        myMultiPasteShowEolInViewer.setSelected(mySettings.isMultiPasteShowEolInViewer());
        myMultiPasteShowEolInList.setSelected(mySettings.isMultiPasteShowEolInList());
        myMultiPastePreserveOriginal.setSelected(mySettings.isMultiPastePreserveOriginal());
        myMultiPasteDeleteRepeatedCaretData.setSelected(mySettings.isMultiPasteDeleteRepeatedCaretData());
        myReplaceMacroVariables.setSelected(mySettings.isReplaceMacroVariables());
        myIncludeUserDefinedMacro.setSelected(mySettings.isIncludeUserDefinedMacro());
        myRegexUserDefinedMacro.setSelected(mySettings.isRegexUserDefinedMacro());
        myUserDefinedMacroClipContent.setSelected(mySettings.isUserDefinedMacroClipContent());
        myUserDefinedMacroSmartReplace.setSelected(mySettings.isUserDefinedMacroSmartReplace());
        myShowMacroResultPreview.setSelected(mySettings.isShowMacroResultPreview());
        myUserDefinedMacroSearch.setText(mySettings.getUserDefinedMacroSearch());
        mySpliceDelimiterText.setText(mySettings.getSpliceDelimiterText());
        myOpenQuoteText.setText(mySettings.getOpenQuoteText());
        myClosedQuoteText.setText(mySettings.getClosedQuoteText());
        myUserDefinedMacroReplace.setText(mySettings.getUserDefinedMacroReplace());

        myShowInstructions.addActionListener(event -> updateTextPane());

        final ActionListener actionListener = e -> updateSettings(OTHER_CHANGED);

        final ActionListener userMacroActionListener = e -> {
            updateSettings(OTHER_CHANGED);
            if (!myUserDefinedMacroClipContent.isSelected()) {
                MiaCancelableJobScheduler.getInstance().schedule(250, () -> {
                    myUserDefinedMacroReplace.requestFocus();
                });
            }
        };

        myMultiPasteShowEolInViewer.addActionListener(actionListener);
        myMultiPasteShowEolInList.addActionListener(actionListener);
        myMultiPastePreserveOriginal.addActionListener(actionListener);
        myMultiPasteDeleteRepeatedCaretData.addActionListener(actionListener);
        myReplaceMacroVariables.addActionListener(actionListener);
        myIncludeUserDefinedMacro.addActionListener(actionListener);
        myUserDefinedMacroClipContent.addActionListener(userMacroActionListener);
        myUserDefinedMacroSmartReplace.addActionListener(userMacroActionListener);
        myRegexUserDefinedMacro.addActionListener(actionListener);
        myShowMacroResultPreview.addActionListener(actionListener);

        final DocumentAdapter documentAdapter = new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull final DocumentEvent e) {
                updateSettings(0);
            }
        };

        final DocumentAdapter openQuoteDocumentAdapter = new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull final DocumentEvent e) {
                myClosedQuoteText.setText(EditHelpers.getCorrespondingQuoteLike(myOpenQuoteText.getText()));
                updateSettings(0);
            }
        };

        DocumentAdapter updatingDocumentAdapter = new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull final DocumentEvent e) {
                if (myUserDefinedMacroSearch != null) {
                    updateSettings(myUserDefinedMacroSearch.isEnabled() ? USER_MACRO_CHANGED : 0);
                }
            }
        };

        myUserDefinedMacroSearch.getDocument().addDocumentListener(updatingDocumentAdapter);
        myUserDefinedMacroReplace.getDocument().addDocumentListener(updatingDocumentAdapter);

        mySpliceDelimiterText.getDocument().addDocumentListener(documentAdapter);
        myOpenQuoteText.getDocument().addDocumentListener(openQuoteDocumentAdapter);
        myClosedQuoteText.getDocument().addDocumentListener(documentAdapter);
        myClosedQuoteText.getDocument().addDocumentListener(documentAdapter);

        updateUIState();

        myTextPane.setVisible(myShowInstructions.isSelected());

        myNormalBorder = mySpliceDelimiterText.getBorder();
        myNormalPanelBorder = myPanel.getBorder();
        myHighlightBorder = BorderFactory.createLineBorder(JBColor.MAGENTA, 3, true);
    }

    public void updateSettings(final int informChangeFlags) {
        mySettings.setMultiPasteShowEolInViewer(myMultiPasteShowEolInViewer.isSelected());
        mySettings.setMultiPasteShowEolInList(myMultiPasteShowEolInList.isSelected());
        mySettings.setMultiPastePreserveOriginal(myMultiPastePreserveOriginal.isSelected());
        mySettings.setMultiPasteDeleteRepeatedCaretData(myMultiPasteDeleteRepeatedCaretData.isSelected());
        mySettings.setReplaceMacroVariables(myReplaceMacroVariables.isSelected());
        mySettings.setIncludeUserDefinedMacro(myIncludeUserDefinedMacro.isSelected());
        mySettings.setRegexUserDefinedMacro(myRegexUserDefinedMacro.isSelected());
        mySettings.setUserDefinedMacroClipContent(myUserDefinedMacroClipContent.isSelected());
        mySettings.setUserDefinedMacroSmartReplace(myUserDefinedMacroSmartReplace.isSelected());
        mySettings.setShowMacroResultPreview(myShowMacroResultPreview.isSelected());
        mySettings.setUserDefinedMacroSearch(myUserDefinedMacroSearch.getText());
        mySettings.setSpliceDelimiterText(mySpliceDelimiterText.getText());
        mySettings.setOpenQuoteText(myOpenQuoteText.getText());
        mySettings.setClosedQuoteText(myClosedQuoteText.getText());
        mySettings.setUserDefinedMacroReplace(myUserDefinedMacroReplace.getText());

        updateUIState();

        if (informChangeFlags > 0 && mySettingsChangedRunnable != null) {
            mySettingsChangedRunnable.accept(informChangeFlags);
        }
    }

    public void updateUIState() {
        myIncludeUserDefinedMacro.setEnabled(myReplaceMacroVariables.isSelected());
        myShowMacroResultPreview.setEnabled(myReplaceMacroVariables.isSelected());
        myRegexUserDefinedMacro.setEnabled(myReplaceMacroVariables.isSelected() && myIncludeUserDefinedMacro.isSelected());
        myUserDefinedMacroClipContent.setEnabled(myReplaceMacroVariables.isSelected() && myIncludeUserDefinedMacro.isSelected());
        myUserDefinedMacroSmartReplace.setEnabled(myReplaceMacroVariables.isSelected() && myIncludeUserDefinedMacro.isSelected());
        myUserDefinedMacroSearch.setEnabled(myReplaceMacroVariables.isSelected() && myIncludeUserDefinedMacro.isSelected());
        myUserDefinedMacroReplace.setEnabled(myReplaceMacroVariables.isSelected() && myIncludeUserDefinedMacro.isSelected() && !myUserDefinedMacroClipContent.isSelected());
        myUserDefinedMacroReplaceClipContent.setEnabled(myReplaceMacroVariables.isSelected() && myIncludeUserDefinedMacro.isSelected() && myUserDefinedMacroClipContent.isSelected());

        myUserDefinedMacroReplace.setVisible(!myUserDefinedMacroClipContent.isSelected());
        myUserDefinedMacroReplaceClipContent.setVisible(myUserDefinedMacroClipContent.isSelected());
    }

    private void updateTextPane() {
        mySettings.setMultiPasteShowInstructions(myShowInstructions.isSelected());
        myTextPane.setVisible(myShowInstructions.isSelected());
        myTextPane.validate();
        myPanel.validate();
        myPanel.getParent().validate();
    }

    public void updatedClipboardContents(final List<String> selections, final List<Integer> indices) {
        myUserDefinedMacroReplaceClipContent.removeAllItems();
        myUserDefinedMacroReplaceClipIndices = indices;
        for (String item : selections) {
            myUserDefinedMacroReplaceClipContent.addItem(item);
        }
    }

    public int getSelectedClipboardContentIndex() {
        int index = myUserDefinedMacroReplaceClipContent.getSelectedIndex();
        return index >= 0 && index < myUserDefinedMacroReplaceClipIndices.size() ? myUserDefinedMacroReplaceClipIndices.get(index) : -1;
    }

    public void setSettingsChangedRunnable(final Runnable settingsChangedRunnable) {
        mySettingsChangedRunnable = (flags) -> settingsChangedRunnable.run();
    }

    public void setSettingsChangedRunnable(final Consumer<Integer> settingsChangedRunnable) {
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
        myUserDefinedMacroReplaceClipContent = new ComboBox<>();
    }

    public void setSpliceDelimiterTextHighlight(boolean highlight) {
        mySpliceDelimiterText.setBorder(highlight ? myHighlightBorder : myNormalBorder);
    }

    public void setQuoteTextHighlight(boolean highlight) {
        myOpenQuoteText.setBorder(highlight ? myHighlightBorder : myNormalBorder);
        myClosedQuoteText.setBorder(highlight ? myHighlightBorder : myNormalBorder);
    }

    public void setPanelHighlight(boolean highlight) {
        myPanel.setBorder(highlight ? myHighlightBorder : myNormalPanelBorder);
    }

    public void setContentBody(String text) {
        JLabel label = new JLabel();
        Font font = label.getFont();
        Color textColor = label.getForeground();
        String out = "<html><head></head><body><div style='font-family:" + font.getFontName() + ";" + "font-size:" + JBUI.scale(font.getSize()) + "pt; color:" + HtmlHelpers.toRgbString(textColor) + "'>" +
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
