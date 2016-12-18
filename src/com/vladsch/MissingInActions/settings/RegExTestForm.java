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

package com.vladsch.MissingInActions.settings;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.ui.UIUtil;
import com.vladsch.MissingInActions.util.FastEncoder;
import com.vladsch.MissingInActions.util.HelpersKt;
import com.vladsch.MissingInActions.util.HtmlStringBuilder;
import com.vladsch.MissingInActions.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExTestForm extends DialogWrapper {
    JPanel myMainPanel;
    private JTextField myPattern1;
    JTextField mySample1;
    JTextField mySample2;
    private JTextPane myExtractedPrefix1;
    private JTextPane myExtractedPrefix2;
    private JTextField myResult;
    private JPanel myExtractedPrefixWrapper1;
    private JPanel myExtractedPrefixWrapper2;

    private @NotNull String mySample1Text;
    private @NotNull String mySample2Text;
    private @NotNull String myPattern1Text;

    static class RegExSampleSet {
        final JTextField sample;
        final JTextPane extractedPrefix;
        final JPanel extractedPrefixWrapper;

        public RegExSampleSet(final JTextField sample, final JTextPane extractedPrefix, final JPanel extractedPrefixWrapper) {
            this.sample = sample;
            this.extractedPrefix = extractedPrefix;
            this.extractedPrefixWrapper = extractedPrefixWrapper;
        }
    }

    private final RegExSampleSet mySampleSet1;
    private final RegExSampleSet mySampleSet2;

    private final RegExSettingsHolder mySettingsHolder;

    private Color getInvalidTextFieldBackground() {
        return Utils.errorColor(UIUtil.getTextFieldBackground());
    }

    private Color getWarningTextFieldBackground() {
        return Utils.warningColor(UIUtil.getTextFieldBackground());
    }

    private Color getValidTextFieldBackground() {
        return UIUtil.getTextFieldBackground();
    }

    private Color getSelectedTextFieldBackground() {
        return mySample1.getSelectionColor();
    }

    public boolean saveSettings(boolean onlySamples) {
        // save settings return false if regex is not valid
        mySettingsHolder.setSample1(mySample1.getText().trim());
        mySettingsHolder.setSample2(mySample2.getText().trim());

        if (!onlySamples) {
            mySettingsHolder.setPattern1(myPattern1.getText().trim());
            //mySettingsHolder.setPattern2(myPattern2.getText().trim());
        }

        if (!myPattern1.getText().trim().isEmpty()) {
            return checkRegEx(myPattern1, mySampleSet1).isEmpty() || checkRegEx(myPattern1, mySampleSet2).isEmpty();
        }
        return true;
    }

    public RegExTestForm(JComponent parent, @NotNull RegExSettingsHolder settingsHolder) {
        super(parent, false);

        mySettingsHolder = settingsHolder;

        mySampleSet1 = new RegExSampleSet(mySample1, myExtractedPrefix1, myExtractedPrefixWrapper1);
        mySampleSet2 = new RegExSampleSet(mySample2, myExtractedPrefix2, myExtractedPrefixWrapper2);

        mySample1Text = settingsHolder.getSample1();
        mySample2Text = settingsHolder.getSample2();
        myPattern1Text = settingsHolder.getPattern1();

        mySample1.setText(mySample1Text);
        mySample2.setText(mySample2Text);
        myPattern1.setText(myPattern1Text);

        final DocumentAdapter listener = new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                updateResult();
            }
        };

        myPattern1.getDocument().addDocumentListener(listener);
        mySample1.getDocument().addDocumentListener(listener);
        mySample2.getDocument().addDocumentListener(listener);

        Color background = myMainPanel.getBackground();
        myExtractedPrefix1.setFont(mySample1.getFont());
        myExtractedPrefix2.setFont(mySample2.getFont());

        updateResult();

        init();
    }

    private void updateResult() {
        if (!myPattern1.getText().trim().isEmpty()) {
            checkRegEx(myPattern1, mySampleSet1);
            checkRegEx(myPattern1, mySampleSet2);
        }
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
        RegExTestForm dialog = new RegExTestForm(parent, settingsHolder);
        boolean save = dialog.showAndGet();
        return dialog.saveSettings(!save);
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        String err1 = "";
        String err2 = "";
        if (!myPattern1.getText().trim().isEmpty()) {
            err1 = checkRegEx(myPattern1, mySampleSet1);
            err2 = checkRegEx(myPattern1, mySampleSet2);
        }

        String err = err1;
        if (!err2.isEmpty()) err += (err.isEmpty() ? "" : "\n") + err2;

        if (!err1.isEmpty() || !err2.isEmpty()) {
            return new ValidationInfo(err, myPattern1);
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
        return myPattern1;
    }

    private String checkRegEx(final JTextField pattern, RegExSampleSet sampleSet) {
        return checkRegEx(pattern, sampleSet, true);
    }

    private String checkRegEx(final JTextField pattern, RegExSampleSet sampleSet, boolean showResults) {
        final String patternText = pattern.getText().trim();
        Color validBackground = getValidTextFieldBackground();
        Color selectedBackground = getSelectedTextFieldBackground();
        Color invalidBackground = getInvalidTextFieldBackground();
        Color warningBackground = getWarningTextFieldBackground();
        String error = "";
        String warning = "";

        if (patternText.isEmpty() && !showResults) {
            sampleSet.extractedPrefix.setText("");
            sampleSet.extractedPrefix.setOpaque(false);
            sampleSet.extractedPrefix.setToolTipText("");
        } else {
            Pattern regexPattern;
            Matcher matcher = null;
            final String text = sampleSet.sample.getText().trim();
            boolean badRegEx = false;

            try {
                regexPattern = Pattern.compile(patternText);
                matcher = regexPattern.matcher(text);
                if (matcher.find()) {
                    if (matcher.start() != 0) {
                        error = "match not at beginning of text. Add ^ prefix to pattern";
                    } else if (matcher.group().isEmpty()) {
                        error = "also matches pattern not at the beginning of text. Add ^ prefix to pattern";
                    } else {
                        // see if it will match in the middle
                        Matcher matcher2 = regexPattern.matcher(text + text);
                        if (matcher2.find() && matcher2.find()) {
                            warning = "also matches pattern not at the beginning of text. Add ^ prefix to pattern";
                        }
                    }
                } else if (showResults) {
                    error = "not matched";
                }
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                error = e.getMessage();
                badRegEx = true;
            }

            if (showResults) {
                if (error.isEmpty() && matcher != null) {
                    // have match
                    HtmlStringBuilder html = new HtmlStringBuilder();
                    html.tag("html").tag("body", "style='margin: 2px'");
                    html.span(FastEncoder.encode(text.substring(0, matcher.end())), null, warning.isEmpty() ? selectedBackground : warningBackground);
                    html.span(FastEncoder.encode(text.substring(matcher.end())), null, null);
                    html.closeTag("body");
                    html.closeTag("html");
                    String htmlResult = html.toHtml();

                    sampleSet.extractedPrefix.setText(htmlResult);
                    sampleSet.extractedPrefix.setOpaque(true);
                    sampleSet.extractedPrefix.setToolTipText(warning);
                    sampleSet.extractedPrefixWrapper.setBackground(myMainPanel.getBackground());
                } else if (!badRegEx) {
                    HtmlStringBuilder html = new HtmlStringBuilder();
                    html.tag("html").tag("body", "style='margin: 2px'");
                    if (error.equals("not matched")) {
                        html.span(FastEncoder.encode(text), null, null);
                        sampleSet.extractedPrefix.setOpaque(false);
                    } else {
                        html.span(FastEncoder.encode(text.substring(0, matcher.start())), null, null);
                        html.span(FastEncoder.encode(text.substring(matcher.start(), matcher.end())), null, invalidBackground);
                        html.span(FastEncoder.encode(text.substring(matcher.end())), null, null);
                        sampleSet.extractedPrefix.setOpaque(false);
                    }
                    html.closeTag("body");
                    html.closeTag("html");
                    String htmlResult = html.toHtml();

                    sampleSet.extractedPrefix.setText(htmlResult);
                    sampleSet.extractedPrefix.setToolTipText(error);
                    sampleSet.extractedPrefixWrapper.setBackground(warningBackground);
                } else {
                    HtmlStringBuilder html = new HtmlStringBuilder();
                    html.tag("html").tag("body", "style='margin: 2px'");
                    //noinspection ConstantConditions
                    html.append(HelpersKt.toHtmlError(error, true));

                    html.closeTag("body");
                    html.closeTag("html");
                    String htmlResult = html.toHtml();

                    sampleSet.extractedPrefix.setText(htmlResult);
                    sampleSet.extractedPrefix.setOpaque(false);
                    sampleSet.extractedPrefix.setToolTipText(error);
                    sampleSet.extractedPrefixWrapper.setBackground(warningBackground);
                }
            }
        }
        return error;
    }

    private void createUIComponents() {
    }
}
