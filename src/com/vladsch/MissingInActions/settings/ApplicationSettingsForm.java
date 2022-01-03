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

import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.ex.DefaultColorSchemesManager;
import com.intellij.openapi.editor.colors.impl.DefaultColorsScheme;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.ui.ComponentUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.vladsch.MissingInActions.Bundle;
import com.vladsch.MissingInActions.Plugin;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.flexmark.util.html.ui.BackgroundColor;
import com.vladsch.flexmark.util.html.ui.HtmlBuilder;
import com.vladsch.flexmark.util.html.ui.HtmlHelpers;
import com.vladsch.plugin.util.ui.CheckBoxWithColorChooser;
import com.vladsch.plugin.util.ui.ColorIterable;
import com.vladsch.plugin.util.ui.Settable;
import com.vladsch.plugin.util.ui.SettingsComponents;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.HyperlinkEvent;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

@SuppressWarnings("WeakerAccess")
public class ApplicationSettingsForm implements Disposable, RegExSettingsHolder {
    private JPanel myMainPanel;
    final private ApplicationSettings mySettings;

    private CustomizedBoundaryForm myCustomizedNextWordBounds;
    private CustomizedBoundaryForm myCustomizedNextWordEndBounds;
    private CustomizedBoundaryForm myCustomizedNextWordStartBounds;
    private CustomizedBoundaryForm myCustomizedPrevWordBounds;
    private CustomizedBoundaryForm myCustomizedPrevWordEndBounds;
    private CustomizedBoundaryForm myCustomizedPrevWordStartBounds;
    @SuppressWarnings("unused") private CustomizedBoundaryLabelForm myCustomizedBoundaryLabelForm;
    private HyperlinkLabel myPreambleLabel;
    private HyperlinkLabel mySetVirtualSpace;
    JBCheckBox myAddPrefixOnPaste;
    JBCheckBox myAutoIndent;
    JBCheckBox myCopyLineOrLineSelection;
    JBCheckBox myDeleteOperations;
    JBCheckBox myDuplicateAtStartOrEnd;
    JBCheckBox myIndentUnindent;
    JBCheckBox mySelectionEndExtended;
    JBCheckBox mySelectionStartExtended;
    JBCheckBox myLeftRightMovement;
    JBCheckBox myMouseCamelHumpsFollow;
    JBCheckBox myMouseLineSelection;
    JBCheckBox myMultiPasteShowEolInViewer;
    JBCheckBox myMultiPasteShowEolInList;
    JBCheckBox myMultiPasteShowInstructions;
    JBCheckBox myMultiPastePreserveOriginal;
    JBCheckBox myMultiPasteDeleteRepeatedCaretData;
    JBCheckBox myMultiPasteDeleteReplacedCaretData;
    JBCheckBox myOverrideStandardPaste;
    JBCheckBox myPreserveCamelCaseOnPaste;
    JBCheckBox myPreserveScreamingSnakeCaseOnPaste;
    JBCheckBox myPreserveSnakeCaseOnPaste;
    JBCheckBox myPreserveDashCaseOnPaste;
    JBCheckBox myPreserveDotCaseOnPaste;
    JBCheckBox myPreserveSlashCaseOnPaste;
    JBCheckBox myRemovePrefixOnPaste;
    JBCheckBox mySelectPasted;
    JBCheckBox mySelectPastedMultiCaret;
    JBCheckBox myStartEndAsLineSelection;
    JBCheckBox myTypingDeletesLineSelection;
    JBCheckBox myUnselectToggleCase;
    JBCheckBox myUpDownMovement;
    JBCheckBox myUpDownSelection;
    JBTextField myPrefixOnPasteText;
    JButton myEditRegExButton;
    JComboBox<String> myAutoLineMode;
    JComboBox<String> myCaretOnMoveSelectionDown;
    JComboBox<String> myCaretOnMoveSelectionUp;
    JComboBox<String> myDuplicateAtStartOrEndPredicate;
    JComboBox<String> myLinePasteCaretAdjustment;
    JComboBox<String> myMouseModifier;
    JComboBox<String> myRemovePrefixOnPastePattern;
    JComboBox<String> mySelectPastedMultiCaretPredicate;
    JComboBox<String> mySelectPastedPredicate;
    JCheckBox mySearchCancelOnEscape;
    JCheckBox myPreservePrimaryCaretOnEscape;
    JCheckBox mySpawnSmartPrefixSearch;
    JCheckBox mySpawnMatchBoundarySearch;
    JCheckBox myOnPastePreserve;
    JCheckBox myHideDisabledButtons;
    JSpinner myAutoIndentDelay;
    JSpinner mySelectionStashLimit;
    JComboBox<String> myPrimaryCaretThickness;
    CheckBoxWithColorChooser myPrimaryCaretColor;
    JComboBox<String> mySearchStartCaretThickness;
    CheckBoxWithColorChooser mySearchStartCaretColor;
    JComboBox<String> mySearchStartFoundCaretThickness;
    CheckBoxWithColorChooser mySearchStartMatchedCaretColor;
    JComboBox<String> mySearchFoundCaretThickness;
    CheckBoxWithColorChooser mySearchFoundCaretColor;
    JTextPane myCaretVisualAttributesPane;
    CheckBoxWithColorChooser myRecalledSelectionColor;
    CheckBoxWithColorChooser myIsolatedForegroundColor;
    CheckBoxWithColorChooser myIsolatedBackgroundColor;
    JLabel myVersion;
    JSpinner myGradientSaturationMin;
    JSpinner myGradientSaturationMax;
    JSpinner myGradientSaturationSteps;
    JSpinner myGradientBrightnessMin;
    JSpinner myGradientBrightnessMax;
    JSpinner myGradientBrightnessSteps;
    JSpinner myGradientHueMin;
    JSpinner myGradientHueMax;
    JSpinner myGradientHueSteps;
    JTextPane myHighlightGradientPane;
    JLabel mySampleText;
    JBTextField mySpliceDelimiterText;
    JBTextField myOpenQuoteText;
    JBTextField myClosedQuoteText;
    private JLabel mySpliceDelimiterTextLabel;
    JBCheckBox myOnlyLatestBlankClipboard;
    JBCheckBox mySpawnNumericHexSearch;
    JBCheckBox mySpawnNumericSearch;
    CustomDeleteBackspaceForm myCustomDeleteBackspaceForm;
    JBCheckBox myDisableParameterInfo;
    JBCheckBox myShowGenerateException;
    JBCheckBox myRegisterCaretStateTransferable;
    JBCheckBox myHighlightProjectViewNodes;

    private @NotNull String myRegexSampleText;
    private final EditingCommitter myEditingCommitter;

    private final SettingsComponents<ApplicationSettings> components;
    private final LafManagerListener myLafManagerListener;

    public ApplicationSettingsForm(ApplicationSettings settings) {
        mySettings = settings;

        components = new SettingsComponents<ApplicationSettings>() {
            @Override
            protected Settable[] createComponents(@NotNull ApplicationSettings i) {
                return new Settable[] {
                        component(AutoLineModeType.ADAPTER, myAutoLineMode, i::getAutoLineMode, i::setAutoLineMode),
                        component(CaretAdjustmentType.ADAPTER, myCaretOnMoveSelectionDown, i::getCaretOnMoveSelectionDown, i::setCaretOnMoveSelectionDown),
                        component(CaretAdjustmentType.ADAPTER, myCaretOnMoveSelectionUp, i::getCaretOnMoveSelectionUp, i::setCaretOnMoveSelectionUp),
                        component(LinePasteCaretAdjustmentType.ADAPTER, myLinePasteCaretAdjustment, i::getLinePasteCaretAdjustment, i::setLinePasteCaretAdjustment),
                        component(MouseModifierType.ADAPTER, myMouseModifier, i::getMouseModifier, i::setMouseModifier),
                        component(CaretThicknessType.ADAPTER, myPrimaryCaretThickness, i::getPrimaryCaretThickness, i::setPrimaryCaretThickness),
                        component(CaretThicknessType.ADAPTER, mySearchStartCaretThickness, i::getSearchStartCaretThickness, i::setSearchStartCaretThickness),
                        component(CaretThicknessType.ADAPTER, mySearchStartFoundCaretThickness, i::getSearchStartMatchedCaretThickness, i::setSearchStartMatchedCaretThickness),
                        component(CaretThicknessType.ADAPTER, mySearchFoundCaretThickness, i::getSearchFoundCaretThickness, i::setSearchFoundCaretThickness),
                        component(myPrimaryCaretColor, i::primaryCaretColorRGB, i::primaryCaretColorRGB),
                        componentEnabled(myPrimaryCaretColor, i::isPrimaryCaretColorEnabled, i::setPrimaryCaretColorEnabled),
                        component(myIsolatedForegroundColor, i::isolatedForegroundColorRGB, i::isolatedForegroundColorRGB),
                        componentEnabled(myIsolatedForegroundColor, i::isIsolatedForegroundColorEnabled, i::setIsolatedForegroundColorEnabled),
                        component(myIsolatedBackgroundColor, i::isolatedBackgroundColorRGB, i::isolatedBackgroundColorRGB),
                        componentEnabled(myIsolatedBackgroundColor, i::isIsolatedBackgroundColorEnabled, i::setIsolatedBackgroundColorEnabled),
                        component(mySearchStartCaretColor, i::searchStartCaretColorRGB, i::searchStartCaretColorRGB),
                        componentEnabled(mySearchStartCaretColor, i::isSearchStartCaretColorEnabled, i::setSearchStartCaretColorEnabled),
                        component(mySearchStartMatchedCaretColor, i::searchStartMatchedCaretColorRGB, i::searchStartMatchedCaretColorRGB),
                        componentEnabled(mySearchStartMatchedCaretColor, i::isSearchStartMatchedCaretColorEnabled, i::setSearchStartMatchedCaretColorEnabled),
                        component(mySearchFoundCaretColor, i::searchFoundCaretColorRGB, i::searchFoundCaretColorRGB),
                        componentEnabled(mySearchFoundCaretColor, i::isSearchFoundCaretColorEnabled, i::setSearchFoundCaretColorEnabled),
                        component(myRecalledSelectionColor, i::recalledSelectionColorRGB, i::recalledSelectionColorRGB),
                        componentEnabled(myRecalledSelectionColor, i::isRecalledSelectionColorEnabled, i::setRecalledSelectionColorEnabled),
                        component(myAddPrefixOnPaste, i::isAddPrefixOnPaste, i::setAddPrefixOnPaste),
                        component(myAutoIndent, i::isAutoIndent, i::setAutoIndent),
                        component(myAutoIndentDelay, i::getAutoIndentDelay, i::setAutoIndentDelay),
                        component(mySelectionStashLimit, i::getSelectionStashLimit, i::setSelectionStashLimit),
                        component(myGradientSaturationMin, i::getSaturationMin, i::setSaturationMin),
                        component(myGradientSaturationMax, i::getSaturationMax, i::setSaturationMax),
                        component(myGradientSaturationSteps, i::getSaturationSteps, i::setSaturationSteps),
                        component(myGradientBrightnessMin, i::getBrightnessMin, i::setBrightnessMin),
                        component(myGradientBrightnessMax, i::getBrightnessMax, i::setBrightnessMax),
                        component(myGradientBrightnessSteps, i::getBrightnessSteps, i::setBrightnessSteps),
                        component(myGradientHueMin, i::getHueMin, i::setHueMin),
                        component(myGradientHueMax, i::getHueMax, i::setHueMax),
                        component(myGradientHueSteps, i::getHueSteps, i::setHueSteps),
                        component(myCopyLineOrLineSelection, i::isCopyLineOrLineSelection, i::setCopyLineOrLineSelection),
                        component(myDeleteOperations, i::isDeleteOperations, i::setDeleteOperations),
                        component(myDuplicateAtStartOrEnd, i::isDuplicateAtStartOrEnd, i::setDuplicateAtStartOrEnd),
                        component(mySearchCancelOnEscape, i::isSearchCancelOnEscape, i::setSearchCancelOnEscape),
                        component(myPreservePrimaryCaretOnEscape, i::isPreservePrimaryCaretOnEscape, i::setPreservePrimaryCaretOnEscape),
                        component(mySpawnSmartPrefixSearch, i::isSpawnSmartPrefixSearch, i::setSpawnSmartPrefixSearch),
                        component(mySpawnMatchBoundarySearch, i::isSpawnMatchBoundarySearch, i::setSpawnMatchBoundarySearch),
                        component(myOnPastePreserve, i::isOnPastePreserve, i::setOnPastePreserve),
                        component(myHideDisabledButtons, i::isHideDisabledButtons, i::setHideDisabledButtons),
                        component(myIndentUnindent, i::isIndentUnindent, i::setIndentUnindent),
                        component(myLeftRightMovement, i::isLeftRightMovement, i::setLeftRightMovement),
                        component(myMouseCamelHumpsFollow, i::isMouseCamelHumpsFollow, i::setMouseCamelHumpsFollow),
                        component(myMouseLineSelection, i::isMouseLineSelection, i::setMouseLineSelection),
                        component(myMultiPasteShowEolInList, i::isMultiPasteShowEolInList, i::setMultiPasteShowEolInList),
                        component(myMultiPasteShowEolInViewer, i::isMultiPasteShowEolInViewer, i::setMultiPasteShowEolInViewer),
                        component(myMultiPasteShowInstructions, i::isMultiPasteShowInstructions, i::setMultiPasteShowInstructions),
                        component(myOnlyLatestBlankClipboard, i::isOnlyLatestBlankClipboard, i::setOnlyLatestBlankClipboard),
                        component(mySpawnNumericHexSearch, i::isSpawnNumericHexSearch, i::setSpawnNumericHexSearch),
                        component(mySpawnNumericSearch, i::isSpawnNumericSearch, i::setSpawnNumericSearch),
                        component(myDisableParameterInfo, i::isDisableParameterInfo, i::setDisableParameterInfo),
                        component(myShowGenerateException, i::isShowGenerateException, i::setShowGenerateException),
                        component(myRegisterCaretStateTransferable, i::isRegisterCaretStateTransferable, i::setRegisterCaretStateTransferable),
                        component(myHighlightProjectViewNodes, i::isHighlightProjectViewNodes, i::setHighlightProjectViewNodes),
                        component(myMultiPastePreserveOriginal, i::isMultiPastePreserveOriginal, i::setMultiPastePreserveOriginal),
                        component(myMultiPasteDeleteRepeatedCaretData, i::isMultiPasteDeleteRepeatedCaretData, i::setMultiPasteDeleteRepeatedCaretData),
                        component(myMultiPasteDeleteReplacedCaretData, i::isMultiPasteDeleteReplacedCaretData, i::setMultiPasteDeleteReplacedCaretData),
                        component(myOverrideStandardPaste, i::isOverrideStandardPaste, i::setOverrideStandardPaste),
                        component(myPreserveCamelCaseOnPaste, i::isPreserveCamelCaseOnPaste, i::setPreserveCamelCaseOnPaste),
                        component(myPreserveScreamingSnakeCaseOnPaste, i::isPreserveScreamingSnakeCaseOnPaste, i::setPreserveScreamingSnakeCaseOnPaste),
                        component(myPreserveSnakeCaseOnPaste, i::isPreserveSnakeCaseOnPaste, i::setPreserveSnakeCaseOnPaste),
                        component(myPreserveDashCaseOnPaste, i::isPreserveDashCaseOnPaste, i::setPreserveDashCaseOnPaste),
                        component(myPreserveDotCaseOnPaste, i::isPreserveDotCaseOnPaste, i::setPreserveDotCaseOnPaste),
                        component(myPreserveSlashCaseOnPaste, i::isPreserveSlashCaseOnPaste, i::setPreserveSlashCaseOnPaste),
                        component(myRemovePrefixOnPaste, i::isRemovePrefixOnPaste, i::setRemovePrefixOnPaste),
                        component(myPrefixOnPasteText, i::getPrefixesOnPasteText, i::setPrefixesOnPasteText),
                        component(mySpliceDelimiterText, i::getSpliceDelimiterText, i::setSpliceDelimiterText),
                        component(myOpenQuoteText, i::getOpenQuoteText, i::setOpenQuoteText),
                        component(myClosedQuoteText, i::getClosedQuoteText, i::setClosedQuoteText),
                        component(mySelectionEndExtended, i::isSelectionEndExtended, i::setSelectionEndExtended),
                        component(mySelectionStartExtended, i::isSelectionStartExtended, i::setSelectionStartExtended),
                        component(mySelectPasted, i::isSelectPasted, i::setSelectPasted),
                        component(mySelectPastedMultiCaret, i::isSelectPastedMultiCaret, i::setSelectPastedMultiCaret),
                        component(myStartEndAsLineSelection, i::isStartEndAsLineSelection, i::setStartEndAsLineSelection),
                        component(myTypingDeletesLineSelection, i::isTypingDeletesLineSelection, i::setTypingDeletesLineSelection),
                        component(myUnselectToggleCase, i::isUnselectToggleCase, i::setUnselectToggleCase),
                        component(myUpDownMovement, i::isUpDownMovement, i::setUpDownMovement),
                        component(myUpDownSelection, i::isUpDownSelection, i::setUpDownSelection),
                        component(PrefixOnPastePatternType.ADAPTER, myRemovePrefixOnPastePattern, i::getPrefixOnPastePattern, i::setPrefixOnPastePattern),
                        component(SelectionPredicateType.ADAPTER, myDuplicateAtStartOrEndPredicate, i::getDuplicateAtStartOrEndPredicate, i::setDuplicateAtStartOrEndPredicate),
                        component(SelectionPredicateType.ADAPTER, mySelectPastedMultiCaretPredicate, i::getSelectPastedMultiCaretPredicate, i::setSelectPastedMultiCaretPredicate),
                        component(SelectionPredicateType.ADAPTER, mySelectPastedPredicate, i::getSelectPastedPredicate, i::setSelectPastedPredicate),
                };
            }
        };

        myRegexSampleText = settings.getRegexSampleText();

        final ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                updateOptions(false);
            }
        };

        if (!Plugin.getInstance().isParameterHintsAvailable()) {
            myDisableParameterInfo.setEnabled(false);
            myDisableParameterInfo.setSelected(false);
            myDisableParameterInfo.setToolTipText(Bundle.message("settings.multi-caret.disable-parameter-info.not-available.description"));
        }

        myLafManagerListener = new LafManagerListener() {
            boolean darculaUI = UIUtil.isUnderDarcula();

            @Override
            public void lookAndFeelChanged(@NotNull final LafManager source) {
                boolean underDarcula = UIUtil.isUnderDarcula();
                if (darculaUI != underDarcula) {
                    darculaUI = underDarcula;

                    SettingsComponents<ApplicationSettings> colorComponents = new SettingsComponents<ApplicationSettings>() {
                        @Override
                        protected Settable[] createComponents(@NotNull ApplicationSettings i) {
                            return new Settable[] {
                                    component(myPrimaryCaretColor, i::primaryCaretColorRGB, i::primaryCaretColorRGB),
                                    component(myIsolatedForegroundColor, i::isolatedForegroundColorRGB, i::isolatedForegroundColorRGB),
                                    component(myIsolatedBackgroundColor, i::isolatedBackgroundColorRGB, i::isolatedBackgroundColorRGB),
                                    component(mySearchStartCaretColor, i::searchStartCaretColorRGB, i::searchStartCaretColorRGB),
                                    component(mySearchStartMatchedCaretColor, i::searchStartMatchedCaretColorRGB, i::searchStartMatchedCaretColorRGB),
                                    component(mySearchFoundCaretColor, i::searchFoundCaretColorRGB, i::searchFoundCaretColorRGB),
                                    component(myRecalledSelectionColor, i::recalledSelectionColorRGB, i::recalledSelectionColorRGB),
                                    component(myGradientSaturationMin, i::getSaturationMin, i::setSaturationMin),
                                    component(myGradientSaturationMax, i::getSaturationMax, i::setSaturationMax),
                                    component(myGradientSaturationSteps, i::getSaturationSteps, i::setSaturationSteps),
                                    component(myGradientBrightnessMin, i::getBrightnessMin, i::setBrightnessMin),
                                    component(myGradientBrightnessMax, i::getBrightnessMax, i::setBrightnessMax),
                                    component(myGradientBrightnessSteps, i::getBrightnessSteps, i::setBrightnessSteps),
                                    component(myGradientHueMin, i::getHueMin, i::setHueMin),
                                    component(myGradientHueMax, i::getHueMax, i::setHueMax),
                                    component(myGradientHueSteps, i::getHueSteps, i::setHueSteps),
                            };
                        }
                    };

                    colorComponents.reset(ApplicationSettings.getInstance());
                }
            }
        };

        ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(LafManagerListener.TOPIC, myLafManagerListener);

        myVersion.setText(Bundle.message("settings.version.label") + " " + Plugin.fullProductVersion());

        myMouseLineSelection.addActionListener(actionListener);
        myUpDownSelection.addActionListener(actionListener);
        myAutoIndent.addActionListener(actionListener);
        mySelectPasted.addActionListener(actionListener);
        mySelectPastedMultiCaret.addActionListener(actionListener);
        myDuplicateAtStartOrEnd.addActionListener(actionListener);
        myMouseCamelHumpsFollow.addActionListener(actionListener);
        myRemovePrefixOnPaste.addActionListener(actionListener);
        myAddPrefixOnPaste.addActionListener(actionListener);
        myOverrideStandardPaste.addActionListener(actionListener);
        myRemovePrefixOnPastePattern.addActionListener(actionListener);
        myAutoLineMode.addActionListener(e -> updateOptions(true));
        mySpawnNumericSearch.addActionListener(actionListener);

        final DocumentAdapter documentAdapter = new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull final DocumentEvent e) {
                updateOptions(false);
            }
        };

        final DocumentAdapter openQuoteDocumentAdapter = new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull final DocumentEvent e) {
                myClosedQuoteText.setText(EditHelpers.getCorrespondingQuoteLike(myOpenQuoteText.getText()));
                updateOptions(false);
            }
        };

        myOpenQuoteText.getDocument().addDocumentListener(openQuoteDocumentAdapter);
        myClosedQuoteText.getDocument().addDocumentListener(documentAdapter);

        myEditRegExButton.addActionListener(e -> {
            boolean valid = RegExTestDialog.showDialog(myMainPanel, this);
            myRemovePrefixOnPaste.setSelected(valid);
            myAddPrefixOnPaste.setSelected(valid);
        });

        ChangeListener changeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateGradient();
            }
        };

        myGradientSaturationMin.addChangeListener(changeListener);
        myGradientSaturationMax.addChangeListener(changeListener);
        myGradientSaturationSteps.addChangeListener(changeListener);
        myGradientBrightnessMin.addChangeListener(changeListener);
        myGradientBrightnessMax.addChangeListener(changeListener);
        myGradientBrightnessSteps.addChangeListener(changeListener);
        myGradientHueMin.addChangeListener(changeListener);
        myGradientHueMax.addChangeListener(changeListener);
        myGradientHueSteps.addChangeListener(changeListener);

        myEditingCommitter = new EditingCommitter();
        IdeEventQueue.getInstance().addDispatcher(myEditingCommitter, this);

        if (!LineSelectionManager.isCaretAttributeAvailable()) {
            myPrimaryCaretThickness.setEnabled(false);
            myPrimaryCaretColor.setEnabled(false);
            mySearchStartCaretThickness.setEnabled(false);
            mySearchStartCaretColor.setEnabled(false);
            mySearchStartFoundCaretThickness.setEnabled(false);
            mySearchStartMatchedCaretColor.setEnabled(false);
            mySearchFoundCaretThickness.setEnabled(false);
            mySearchFoundCaretColor.setEnabled(false);
            myCaretVisualAttributesPane.setVisible(true);
            setContentBody(Bundle.message("settings.caret-visual-attributes.description"));
        } else {
            myCaretVisualAttributesPane.setVisible(false);
            mySearchStartCaretColor.setUpdateRunnable(() -> {
                mySearchStartMatchedCaretColor.setUnselectedColor(mySearchStartCaretColor.getColor());
            });
        }

        DefaultColorsScheme scheme = DefaultColorSchemesManager.getInstance().getFirstScheme();
        Color color = scheme.getColor(EditorColors.SELECTION_BACKGROUND_COLOR);
        myRecalledSelectionColor.setUnselectedColor(color);

        myCaretVisualAttributesPane.validate();
        myMainPanel.validate();
        myCustomDeleteBackspaceForm.reset(mySettings);
        updateOptions(true);
    }

    public void setContentBody(String text) {
        JLabel label = new JLabel();
        Font font = label.getFont();
        Color textColor = label.getForeground();
        String out = "<html><head></head><body><div style='font-family:" + font.getFontName() + ";" + "font-size:" + JBUI.scale(font.getSize()) + "pt; color:" + HtmlHelpers.toRgbString(textColor) + "'>" +

                (text == null ? "" : text) +
                "</div></body></html>";
        myCaretVisualAttributesPane.setText(out);
        myMainPanel.validate();
    }

    private void updateGradient() {
        HtmlBuilder html = new HtmlBuilder();
        html.tag("html").style("margin:2px;vertical-align:middle;").attr(mySampleText.getFont()).tag("body").tag("code");

        ColorIterable.ColorIterator iterator = new ColorIterable(
                (int) myGradientHueMin.getValue(),
                (int) myGradientHueMax.getValue(),
                (int) myGradientHueSteps.getValue(),
                (int) myGradientSaturationMin.getValue(),
                (int) myGradientSaturationMax.getValue(),
                (int) myGradientSaturationSteps.getValue(),
                (int) myGradientBrightnessMin.getValue(),
                (int) myGradientBrightnessMax.getValue(),
                (int) myGradientBrightnessSteps.getValue()
        ).iterator();

        while (iterator.hasNext()) {
            Color hsbColor = iterator.next();

            if (iterator.isHueStart() && iterator.getHueSteps() > 1) {
                html.append("<br>");
            }

            if (iterator.isSaturationStart() && iterator.getSaturationSteps() > 1) {
                html.append("<br>");
            }

            BackgroundColor color = BackgroundColor.of(hsbColor);
            html.attr(color).span().append(String.format("&nbsp;%03d&nbsp;", iterator.getIndex() + 1)).closeSpan();
        }

        html.closeTag("code").closeTag("body");
        html.closeTag("html");
        //myHighlightGradientPane.setVisible(true);
        myHighlightGradientPane.setText(html.toFinalizedString());
        //myHighlightGradientPane.revalidate();
        //Component component = myHighlightGradientPane;
        //
        //List<Component> components = new ArrayList<>();
        //while (component != null && component != myMainPanel) {
        //    components.add(component);
        //    component = component.getParent();
        //}
        //
        //for (int i = components.size(); i-- > 0; ) {
        //    components.get(i).revalidate();
        //}
    }

    // @formatter:off
    @NotNull @Override public String getPatternText() { return myPrefixOnPasteText.getText().trim(); }
    @Override public void setPatternText(final String patternText) { myPrefixOnPasteText.setText(patternText); }

    @NotNull @Override public String getSampleText() { return myRegexSampleText; }
    @Override public void setSampleText(final String sampleText) { myRegexSampleText = sampleText; }
    @Override public boolean isCaseSensitive() { return true; }
    @Override public boolean isBackwards() { return false; }
    @Override public void setCaseSensitive(final boolean isCaseSensitive) { }
    @Override public void setBackwards(final boolean isBackwards) { }
    @Override public boolean isCaretToGroupEnd() { return false; }
    @Override public void setCaretToGroupEnd(final boolean isCaretToGroupEnd) { }
    // @formatter:on

    public JComponent getComponent() {
        return myMainPanel;
    }

    public boolean isModified() {
        int wordMask = EditHelpers.START_OF_WORD | EditHelpers.END_OF_WORD;
        //noinspection PointlessBooleanExpression
        return false
                || (myCustomizedNextWordBounds.getValue() & ~wordMask) != (mySettings.getCustomizedNextWordBounds() & ~wordMask)
                || (myCustomizedNextWordEndBounds.getValue() & ~wordMask) != (mySettings.getCustomizedNextWordEndBounds() & ~wordMask)
                || (myCustomizedNextWordStartBounds.getValue() & ~wordMask) != (mySettings.getCustomizedNextWordStartBounds() & ~wordMask)
                || (myCustomizedPrevWordBounds.getValue() & ~wordMask) != (mySettings.getCustomizedPrevWordBounds() & ~wordMask)
                || (myCustomizedPrevWordEndBounds.getValue() & ~wordMask) != (mySettings.getCustomizedPrevWordEndBounds() & ~wordMask)
                || (myCustomizedPrevWordStartBounds.getValue() & ~wordMask) != (mySettings.getCustomizedPrevWordStartBounds() & ~wordMask)
                || !myRegexSampleText.equals(mySettings.getRegexSampleText())

                || components.isModified(mySettings)
                || myCustomDeleteBackspaceForm.isModified(mySettings)
                ;
    }

    public void apply() {
        mySettings.setCustomizedNextWordBounds(myCustomizedNextWordBounds.getValue());
        mySettings.setCustomizedNextWordEndBounds(myCustomizedNextWordEndBounds.getValue());
        mySettings.setCustomizedNextWordStartBounds(myCustomizedNextWordStartBounds.getValue());
        mySettings.setCustomizedPrevWordBounds(myCustomizedPrevWordBounds.getValue());
        mySettings.setCustomizedPrevWordEndBounds(myCustomizedPrevWordEndBounds.getValue());
        mySettings.setCustomizedPrevWordStartBounds(myCustomizedPrevWordStartBounds.getValue());
        mySettings.setRegexSampleText(myRegexSampleText);

        components.apply(mySettings);

        if (mySettings.isMouseCamelHumpsFollow()) {
            EditorSettingsExternalizable settings = EditorSettingsExternalizable.getInstance();
            settings.setMouseClickSelectionHonorsCamelWords(settings.isCamelWords());
        }

        myCustomDeleteBackspaceForm.apply(mySettings);
    }

    public void reset() {
        myCustomizedNextWordBounds.setValue(mySettings.getCustomizedNextWordBounds());
        myCustomizedNextWordEndBounds.setValue(mySettings.getCustomizedNextWordEndBounds());
        myCustomizedNextWordStartBounds.setValue(mySettings.getCustomizedNextWordStartBounds());
        myCustomizedPrevWordBounds.setValue(mySettings.getCustomizedPrevWordBounds());
        myCustomizedPrevWordEndBounds.setValue(mySettings.getCustomizedPrevWordEndBounds());
        myCustomizedPrevWordStartBounds.setValue(mySettings.getCustomizedPrevWordStartBounds());
        myRegexSampleText = mySettings.getRegexSampleText();

        components.reset(mySettings);
        myCustomDeleteBackspaceForm.reset(mySettings);

        updateOptions(false);
        updateGradient();
    }

    @Override
    public void dispose() {
        IdeEventQueue.getInstance().removeDispatcher(myEditingCommitter);
    }

    @SuppressWarnings("ConstantConditions")
    void updateOptions(boolean typeChanged) {
        AutoLineModeType type = AutoLineModeType.ADAPTER.get(myAutoLineMode);
        boolean enabled = false;
        boolean selected = false;
        boolean untestedSelected = false;
        boolean forced = false;

        if (type == AutoLineModeType.ENABLED) {
            enabled = false;
            selected = true;
        } else if (type == AutoLineModeType.EXPERT) {
            enabled = true;
            selected = true;
            forced = true;
        } else {
            typeChanged = true;
        }

        if (typeChanged) myMouseLineSelection.setSelected(selected);
        if (typeChanged) myUpDownSelection.setSelected(selected);

        boolean modeEnabled = true;// || myMouseLineSelection.isSelected() || myUpDownSelection.isSelected();
        if (typeChanged && !forced) myDeleteOperations.setSelected(selected && modeEnabled);
        if (typeChanged && !forced && !modeEnabled) myLeftRightMovement.setSelected(false);
        if (typeChanged && !forced) myUpDownMovement.setSelected(selected && modeEnabled);
        if (typeChanged && !forced) myIndentUnindent.setSelected(selected && modeEnabled);

        myMouseLineSelection.setEnabled(selected);
        mySelectionEndExtended.setEnabled(selected || forced);
        mySelectionStartExtended.setEnabled(selected || forced);
        myMouseModifier.setEnabled(selected && myMouseLineSelection.isSelected());
        myUpDownSelection.setEnabled(enabled);
        myUpDownMovement.setEnabled(enabled && modeEnabled);
        myStartEndAsLineSelection.setEnabled(enabled && modeEnabled);
        myIndentUnindent.setEnabled(enabled && modeEnabled);
        myDeleteOperations.setEnabled(enabled && modeEnabled);
        myLeftRightMovement.setEnabled(enabled && modeEnabled);
        myCopyLineOrLineSelection.setEnabled(enabled && modeEnabled);
        mySelectPastedPredicate.setEnabled(mySelectPasted.isEnabled() && mySelectPasted.isSelected());
        mySelectPastedMultiCaretPredicate.setEnabled(mySelectPastedMultiCaret.isEnabled() && mySelectPastedMultiCaret.isSelected());
        myMultiPasteShowInstructions.setEnabled(myOverrideStandardPaste.isEnabled() && myOverrideStandardPaste.isSelected());
        mySpliceDelimiterText.setEnabled(myOverrideStandardPaste.isEnabled() && myOverrideStandardPaste.isSelected());
        myOpenQuoteText.setEnabled(myOverrideStandardPaste.isEnabled() && myOverrideStandardPaste.isSelected());
        myClosedQuoteText.setEnabled(myOverrideStandardPaste.isEnabled() && myOverrideStandardPaste.isSelected());
        mySpliceDelimiterTextLabel.setEnabled(myOverrideStandardPaste.isEnabled() && myOverrideStandardPaste.isSelected());
        myMultiPastePreserveOriginal.setEnabled(myOverrideStandardPaste.isEnabled() && myOverrideStandardPaste.isSelected());
        myMultiPasteDeleteRepeatedCaretData.setEnabled(myOverrideStandardPaste.isEnabled() && myOverrideStandardPaste.isSelected());
        myMultiPasteDeleteReplacedCaretData.setEnabled(myOverrideStandardPaste.isEnabled() && myOverrideStandardPaste.isSelected());
        myMultiPasteShowEolInViewer.setEnabled(myOverrideStandardPaste.isEnabled() && myOverrideStandardPaste.isSelected());
        myMultiPasteShowEolInList.setEnabled(myOverrideStandardPaste.isEnabled() && myOverrideStandardPaste.isSelected());
        mySpawnNumericHexSearch.setEnabled(mySpawnNumericSearch.isEnabled() && mySpawnNumericSearch.isSelected());

        final boolean regexPrefixes = PrefixOnPastePatternType.ADAPTER.get(myRemovePrefixOnPastePattern) == PrefixOnPastePatternType.REGEX;
        final boolean enablePrefixes = !regexPrefixes &&
                (myRemovePrefixOnPaste.isSelected() && myRemovePrefixOnPaste.isEnabled()
                        || myAddPrefixOnPaste.isSelected() && myAddPrefixOnPaste.isEnabled());

        myPrefixOnPasteText.setEnabled(enablePrefixes);
        myEditRegExButton.setVisible(regexPrefixes);

        myDuplicateAtStartOrEndPredicate.setEnabled(myDuplicateAtStartOrEnd.isEnabled() && myDuplicateAtStartOrEnd.isSelected());
        // no longer needed, using clipboard listener
        //myLinePasteCaretAdjustment.setEnabled(myOverrideStandardPaste.isEnabled() && myOverrideStandardPaste.isSelected());
        myAutoIndentDelay.setEnabled(myAutoIndent.isEnabled() && myAutoIndent.isSelected());

        boolean isVirtualSpace = EditorSettingsExternalizable.getInstance().isVirtualSpace();
        boolean makeVisible;
        if (modeEnabled) {
            makeVisible = !isVirtualSpace;
        } else {
            makeVisible = isVirtualSpace && mySettings.isWeSetVirtualSpace();
        }

        if (mySetVirtualSpace.isVisible() != makeVisible || typeChanged) {
            mySetVirtualSpace.setVisible(makeVisible);
            myPreambleLabel.setVisible(makeVisible);
            if (makeVisible) {
                if (!isVirtualSpace) {
                    myPreambleLabel.setText(Bundle.message("settings.enable-virtual-space.preamble.description"));
                    mySetVirtualSpace.setHtmlText("<html>"
                            + Bundle.message("settings.enable-virtual-space.before.description")
                            + " <a href=\"=\">" + Bundle.message("settings.enable-virtual-spaces-link") + "</a>"
                            + " " + Bundle.message("settings.enable-virtual-space.after.description")
                            + "</html>"
                    );
                } else {
                    myPreambleLabel.setText(Bundle.message("settings.disable-virtual-space.preamble.description"));
                    mySetVirtualSpace.setHtmlText("<html>"
                            + Bundle.message("settings.disable-virtual-space.before.description")
                            + " <a href=\"=\">" + Bundle.message("settings.disable-virtual-spaces-link") + "</a>"
                            + " " + Bundle.message("settings.disable-virtual-space.after.description")
                            + "</html>"
                    );
                }
            }
        }
    }

    private void createUIComponents() {
        myAutoLineMode = AutoLineModeType.ADAPTER.createComboBox();
        mySelectPastedPredicate = SelectionPredicateType.ADAPTER.createComboBox();
        mySelectPastedMultiCaretPredicate = SelectionPredicateType.ADAPTER.createComboBox();
        myDuplicateAtStartOrEndPredicate = SelectionPredicateType.ADAPTER.createComboBox();
        myLinePasteCaretAdjustment = LinePasteCaretAdjustmentType.ADAPTER.createComboBox();
        myCaretOnMoveSelectionDown = CaretAdjustmentType.ADAPTER.createComboBox();
        myCaretOnMoveSelectionUp = CaretAdjustmentType.ADAPTER.createComboBox();
        myRemovePrefixOnPastePattern = PrefixOnPastePatternType.ADAPTER.createComboBox();
        myMouseModifier = MouseModifierType.ADAPTER.createComboBox();

        final SpinnerNumberModel model = new SpinnerNumberModel(500, 0, 10000, 50);
        myAutoIndentDelay = new JSpinner(model);

        final SpinnerNumberModel limitModel = new SpinnerNumberModel(10, 1, 50, 1);
        mySelectionStashLimit = new JSpinner(limitModel);

        myGradientSaturationMin = new JSpinner(new SpinnerNumberModel(10, 0, 100, 1));
        myGradientSaturationMax = new JSpinner(new SpinnerNumberModel(10, 0, 100, 1));
        myGradientSaturationSteps = new JSpinner(new SpinnerNumberModel(16, 1, 32, 1));
        myGradientBrightnessMin = new JSpinner(new SpinnerNumberModel(100, 0, 100, 1));
        myGradientBrightnessMax = new JSpinner(new SpinnerNumberModel(100, 0, 100, 1));
        myGradientBrightnessSteps = new JSpinner(new SpinnerNumberModel(16, 1, 32, 1));
        myGradientHueMin = new JSpinner(new SpinnerNumberModel(0, 0, 360, 6));
        myGradientHueMax = new JSpinner(new SpinnerNumberModel(0, 0, 360, 6));
        myGradientHueSteps = new JSpinner(new SpinnerNumberModel(24, 1, 60, 1));

        mySetVirtualSpace = new HyperlinkLabel();
        mySetVirtualSpace.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                boolean isVirtualSpace = EditorSettingsExternalizable.getInstance().isVirtualSpace();
                EditorSettingsExternalizable.getInstance().setVirtualSpace(!isVirtualSpace);
                mySettings.setWeSetVirtualSpace(!isVirtualSpace);
                updateOptions(false);
            }
        });

        myPrimaryCaretThickness = CaretThicknessType.ADAPTER.createComboBox();
        myPrimaryCaretColor = new CheckBoxWithColorChooser(Bundle.message("settings.primary-caret-color.label"), false, JBColor.BLACK);
        mySearchStartCaretThickness = CaretThicknessType.ADAPTER.createComboBox();
        mySearchStartCaretColor = new CheckBoxWithColorChooser(Bundle.message("settings.primary-caret-color.label"), false, JBColor.black);
        mySearchStartFoundCaretThickness = CaretThicknessType.ADAPTER.createComboBox();
        mySearchStartMatchedCaretColor = new CheckBoxWithColorChooser(Bundle.message("settings.primary-caret-color.label"), false, JBColor.black);
        mySearchFoundCaretThickness = CaretThicknessType.ADAPTER.createComboBox();
        mySearchFoundCaretColor = new CheckBoxWithColorChooser(Bundle.message("settings.primary-caret-color.label"), false, JBColor.black);
        myRecalledSelectionColor = new CheckBoxWithColorChooser(Bundle.message("settings.primary-caret-color.label"), false, JBColor.cyan);
        myIsolatedForegroundColor = new CheckBoxWithColorChooser(Bundle.message("settings.primary-caret-color.label"), false, JBColor.cyan);
        myIsolatedBackgroundColor = new CheckBoxWithColorChooser(Bundle.message("settings.primary-caret-color.label"), false, JBColor.cyan);
    }

    private static class EditingCommitter implements IdeEventQueue.EventDispatcher {
        @Override
        public boolean dispatch(@NotNull AWTEvent e) {
            if (e instanceof KeyEvent && e.getID() == KeyEvent.KEY_PRESSED && ((KeyEvent) e).getKeyCode() == KeyEvent.VK_ENTER) {
                if ((((KeyEvent) e).getModifiersEx() & ~(InputEvent.CTRL_DOWN_MASK)) == 0) {
                    Component owner = ComponentUtil.findParentByCondition(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(), component -> component instanceof JTable);

                    if (owner instanceof JTable && ((JTable) owner).isEditing()) {
                        ((JTable) owner).editingStopped(null);
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
