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

import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.MissingInActions.util.Utils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
    JComboBox<String> myUserDefinedMacroReplaceClipContent;
    JBTextField mySpliceDelimiterText;
    JBTextField myOpenQuoteText;
    JBTextField myClosedQuoteText;
    JBCheckBox myQuoteSplicedItems;
    JLabel mySpliceDelimiterTextLabel;
    Runnable mySettingsChangedRunnable;
    String myTextContent;

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
        myUserDefinedMacroSearch.setText(mySettings.getUserDefinedMacroSearch());
        mySpliceDelimiterText.setText(mySettings.getSpliceDelimiterText());
        myOpenQuoteText.setText(mySettings.getOpenQuoteText());
        myClosedQuoteText.setText(mySettings.getClosedQuoteText());
        myQuoteSplicedItems.setSelected(mySettings.isQuoteSplicedItems());
        myUserDefinedMacroReplace.setText(mySettings.getUserDefinedMacroReplace());

        myShowInstructions.addActionListener(event -> {
            updateTextPane();
        });

        final ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                updateSettings(true);
            }
        };

        final ActionListener userMacroActionListener = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                updateSettings(true);
                myUserDefinedMacroReplace.requestFocus();
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
        myQuoteSplicedItems.addActionListener(actionListener);

        final DocumentAdapter documentAdapter = new DocumentAdapter() {
            @Override
            protected void textChanged(final DocumentEvent e) {
                updateSettings(false);
            }
        };

        final DocumentAdapter openQuoteDocumentAdapter = new DocumentAdapter() {
            @Override
            protected void textChanged(final DocumentEvent e) {
                myClosedQuoteText.setText(EditHelpers.getCorrespondingQuoteLike(myOpenQuoteText.getText()));
                updateSettings(false);
            }
        };
        myUserDefinedMacroSearch.getDocument().addDocumentListener(documentAdapter);
        myUserDefinedMacroReplace.getDocument().addDocumentListener(documentAdapter);
        mySpliceDelimiterText.getDocument().addDocumentListener(documentAdapter);
        myOpenQuoteText.getDocument().addDocumentListener(openQuoteDocumentAdapter);
        myClosedQuoteText.getDocument().addDocumentListener(documentAdapter);
        myClosedQuoteText.getDocument().addDocumentListener(documentAdapter);

        updateUIState();

        myTextPane.setVisible(myShowInstructions.isSelected());
    }

    public void updateSettings(final boolean informChange) {
        mySettings.setMultiPasteShowEolInViewer(myMultiPasteShowEolInViewer.isSelected());
        mySettings.setMultiPasteShowEolInList(myMultiPasteShowEolInList.isSelected());
        mySettings.setMultiPastePreserveOriginal(myMultiPastePreserveOriginal.isSelected());
        mySettings.setMultiPasteDeleteRepeatedCaretData(myMultiPasteDeleteRepeatedCaretData.isSelected());
        mySettings.setReplaceMacroVariables(myReplaceMacroVariables.isSelected());
        mySettings.setIncludeUserDefinedMacro(myIncludeUserDefinedMacro.isSelected());
        mySettings.setRegexUserDefinedMacro(myRegexUserDefinedMacro.isSelected());
        mySettings.setUserDefinedMacroClipContent(myUserDefinedMacroClipContent.isSelected());
        mySettings.setUserDefinedMacroSmartReplace(myUserDefinedMacroSmartReplace.isSelected());
        mySettings.setUserDefinedMacroSearch(myUserDefinedMacroSearch.getText());
        mySettings.setSpliceDelimiterText(mySpliceDelimiterText.getText());
        mySettings.setOpenQuoteText(myOpenQuoteText.getText());
        mySettings.setClosedQuoteText(myClosedQuoteText.getText());
        mySettings.setQuoteSplicedItems(myQuoteSplicedItems.isSelected());
        mySettings.setUserDefinedMacroReplace(myUserDefinedMacroReplace.getText());

        updateUIState();

        if (informChange && mySettingsChangedRunnable != null) {
            mySettingsChangedRunnable.run();
        }
    }

    public void updateUIState() {
        myIncludeUserDefinedMacro.setEnabled(myReplaceMacroVariables.isSelected());
        myRegexUserDefinedMacro.setEnabled(myReplaceMacroVariables.isSelected() && myIncludeUserDefinedMacro.isSelected());
        myUserDefinedMacroClipContent.setEnabled(myReplaceMacroVariables.isSelected() && myIncludeUserDefinedMacro.isSelected());
        myUserDefinedMacroSmartReplace.setEnabled(myReplaceMacroVariables.isSelected() && myIncludeUserDefinedMacro.isSelected());
        myUserDefinedMacroSearch.setEnabled(myReplaceMacroVariables.isSelected() && myIncludeUserDefinedMacro.isSelected());
        myUserDefinedMacroReplace.setEnabled(myReplaceMacroVariables.isSelected() && myIncludeUserDefinedMacro.isSelected() && !myUserDefinedMacroClipContent.isSelected());
        myUserDefinedMacroReplaceClipContent.setEnabled(myReplaceMacroVariables.isSelected() && myIncludeUserDefinedMacro.isSelected() && myUserDefinedMacroClipContent.isSelected());

        myUserDefinedMacroReplace.setVisible(!myUserDefinedMacroClipContent.isSelected());
        myUserDefinedMacroReplaceClipContent.setVisible(myUserDefinedMacroClipContent.isSelected());

        myQuoteSplicedItems.setEnabled(!(myOpenQuoteText.getText().isEmpty() && myClosedQuoteText.getText().isEmpty()));
    }

    private void updateTextPane() {
        mySettings.setMultiPasteShowInstructions(myShowInstructions.isSelected());
        myTextPane.setVisible(myShowInstructions.isSelected());
        myTextPane.validate();
        myPanel.validate();
        myPanel.getParent().validate();
    }

    public void updatedClipboardContents(final java.util.List<String> selections) {
        myUserDefinedMacroReplaceClipContent.removeAllItems();
        for (String item : selections) {
            myUserDefinedMacroReplaceClipContent.addItem(item);
        }
    }

    public int getSelectedClipboardContentIndex() {
        return myUserDefinedMacroReplaceClipContent.getSelectedIndex();
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
        myUserDefinedMacroReplaceClipContent = new JComboBox();
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
