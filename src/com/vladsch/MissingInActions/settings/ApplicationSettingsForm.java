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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.vladsch.MissingInActions.Bundle;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.MissingInActions.util.ui.Settable;
import com.vladsch.MissingInActions.util.ui.SettingsComponents;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
    private CustomizedBoundaryLabelForm myCustomizedBoundaryLabelForm;
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
    JBCheckBox myOverrideStandardPaste;
    JBCheckBox myPreserveCamelCaseOnPaste;
    JBCheckBox myPreserveScreamingSnakeCaseOnPaste;
    JBCheckBox myPreserveSnakeCaseOnPaste;
    JBCheckBox myRemovePrefixOnPaste;
    JBCheckBox mySelectPasted;
    JBCheckBox myStartEndAsLineSelection;
    JBCheckBox myTypingDeletesLineSelection;
    JBCheckBox myUnselectToggleCase;
    JBCheckBox myUpDownMovement;
    JBCheckBox myUpDownSelection;
    JBTextField myRemovePrefixOnPaste1;
    JBTextField myRemovePrefixOnPaste2;
    JButton myEditRegExButton;
    JComboBox myAutoLineMode;
    JComboBox myCaretOnMoveSelectionDown;
    JComboBox myCaretOnMoveSelectionUp;
    JComboBox myDuplicateAtStartOrEndPredicate;
    JComboBox myLinePasteCaretAdjustment;
    JComboBox myMouseModifier;
    JComboBox myRemovePrefixOnPastePattern;
    JComboBox mySelectPastedMultiCaretPredicate;
    JComboBox mySelectPastedPredicate;
    JSpinner myAutoIndentDelay;

    private @NotNull String mySample1Text;
    private @NotNull String mySample2Text;

    private final SettingsComponents<ApplicationSettings> components;

    public ApplicationSettingsForm(ApplicationSettings settings) {
        mySettings = settings;

        components = new SettingsComponents<ApplicationSettings>() {
            @Override
            protected Settable[] getComponents(ApplicationSettings i) {
                return new Settable[] {
                        component(AutoLineModeType.ADAPTER, myAutoLineMode, i::getAutoLineMode, i::setAutoLineMode),
                        component(CaretAdjustmentType.ADAPTER, myCaretOnMoveSelectionDown, i::getCaretOnMoveSelectionDown, i::setCaretOnMoveSelectionDown),
                        component(CaretAdjustmentType.ADAPTER, myCaretOnMoveSelectionUp, i::getCaretOnMoveSelectionUp, i::setCaretOnMoveSelectionUp),
                        component(LinePasteCaretAdjustmentType.ADAPTER, myLinePasteCaretAdjustment, i::getLinePasteCaretAdjustment, i::setLinePasteCaretAdjustment),
                        component(MouseModifierType.ADAPTER, myMouseModifier, i::getMouseModifier, i::setMouseModifier),
                        component(myAddPrefixOnPaste, i::isAddPrefixOnPaste, i::setAddPrefixOnPaste),
                        component(myAutoIndent, i::isAutoIndent, i::setAutoIndent),
                        component(myAutoIndentDelay, i::getAutoIndentDelay, i::setAutoIndentDelay),
                        component(myCopyLineOrLineSelection, i::isCopyLineOrLineSelection, i::setCopyLineOrLineSelection),
                        component(myDeleteOperations, i::isDeleteOperations, i::setDeleteOperations),
                        component(myDuplicateAtStartOrEnd, i::isDuplicateAtStartOrEnd, i::setDuplicateAtStartOrEnd),
                        component(myIndentUnindent, i::isIndentUnindent, i::setIndentUnindent),
                        component(myLeftRightMovement, i::isLeftRightMovement, i::setLeftRightMovement),
                        component(myMouseCamelHumpsFollow, i::isMouseCamelHumpsFollow, i::setMouseCamelHumpsFollow),
                        component(myMouseLineSelection, i::isMouseLineSelection, i::setMouseLineSelection),
                        component(myMultiPasteShowEolInList, i::isMultiPasteShowEolInList, i::setMultiPasteShowEolInList),
                        component(myMultiPasteShowEolInViewer, i::isMultiPasteShowEolInViewer, i::setMultiPasteShowEolInViewer),
                        component(myMultiPasteShowInstructions, i::isMultiPasteShowInstructions, i::setMultiPasteShowInstructions),
                        component(myOverrideStandardPaste, i::isOverrideStandardPaste, i::setOverrideStandardPaste),
                        component(myPreserveCamelCaseOnPaste, i::isPreserveCamelCaseOnPaste, i::setPreserveCamelCaseOnPaste),
                        component(myPreserveScreamingSnakeCaseOnPaste, i::isPreserveScreamingSnakeCaseOnPaste, i::setPreserveScreamingSnakeCaseOnPaste),
                        component(myPreserveSnakeCaseOnPaste, i::isPreserveSnakeCaseOnPaste, i::setPreserveSnakeCaseOnPaste),
                        component(myRemovePrefixOnPaste, i::isRemovePrefixOnPaste, i::setRemovePrefixOnPaste),
                        component(myRemovePrefixOnPaste1, i::getRemovePrefixOnPaste1, i::setRemovePrefixOnPaste1),
                        component(myRemovePrefixOnPaste2, i::getRemovePrefixOnPaste2, i::setRemovePrefixOnPaste2),
                        component(mySelectionEndExtended, i::isSelectionEndExtended, i::setSelectionEndExtended),
                        component(mySelectionStartExtended, i::isSelectionStartExtended, i::setSelectionStartExtended),
                        component(mySelectPasted, i::isSelectPasted, i::setSelectPasted),
                        component(myStartEndAsLineSelection, i::isStartEndAsLineSelection, i::setStartEndAsLineSelection),
                        component(myTypingDeletesLineSelection, i::isTypingDeletesLineSelection, i::setTypingDeletesLineSelection),
                        component(myUnselectToggleCase, i::isUnselectToggleCase, i::setUnselectToggleCase),
                        component(myUpDownMovement, i::isUpDownMovement, i::setUpDownMovement),
                        component(myUpDownSelection, i::isUpDownSelection, i::setUpDownSelection),
                        component(RemovePrefixOnPastePatternType.ADAPTER, myRemovePrefixOnPastePattern, i::getRemovePrefixOnPastePattern, i::setRemovePrefixOnPastePattern),
                        component(SelectionPredicateType.ADAPTER, myDuplicateAtStartOrEndPredicate, i::getDuplicateAtStartOrEndPredicate, i::setDuplicateAtStartOrEndPredicate),
                        component(SelectionPredicateType.ADAPTER, mySelectPastedMultiCaretPredicate, i::getSelectPastedMultiCaretPredicate, i::setSelectPastedMultiCaretPredicate),
                        component(SelectionPredicateType.ADAPTER, mySelectPastedPredicate, i::getSelectPastedPredicate, i::setSelectPastedPredicate),
                };
            }
        };

        mySample1Text = settings.getRegexSample1Text();
        mySample2Text = settings.getRegexSample2Text();

        final ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {ApplicationSettingsForm.this.updateOptions(false);}
        };

        myMouseLineSelection.addActionListener(actionListener);
        myUpDownSelection.addActionListener(actionListener);
        myAutoIndent.addActionListener(actionListener);
        mySelectPasted.addActionListener(actionListener);
        myDuplicateAtStartOrEnd.addActionListener(actionListener);
        myMouseCamelHumpsFollow.addActionListener(actionListener);
        myRemovePrefixOnPaste.addActionListener(actionListener);
        myAddPrefixOnPaste.addActionListener(actionListener);
        myOverrideStandardPaste.addActionListener(actionListener);
        myRemovePrefixOnPastePattern.addActionListener(actionListener);
        myAutoLineMode.addActionListener(e -> updateOptions(true));

        myEditRegExButton.addActionListener(e -> {
            boolean valid = RegExTestForm.showDialog(myMainPanel, this);
            myRemovePrefixOnPaste.setSelected(valid);
            myAddPrefixOnPaste.setSelected(valid);
        });

        updateOptions(true);
    }

    // @formatter:off
    @NotNull @Override public String getPattern1() { return myRemovePrefixOnPaste1.getText().trim(); }
    @NotNull @Override public String getPattern2() { return myRemovePrefixOnPaste2.getText().trim(); }
    @NotNull @Override public String getSample1() { return mySample1Text; }
    @NotNull @Override public String getSample2() { return mySample2Text; }
    @Override public void setPattern1(final String pattern1) { myRemovePrefixOnPaste1.setText(pattern1); }
    @Override public void setPattern2(final String pattern2) { myRemovePrefixOnPaste2.setText(pattern2); }
    @Override public void setSample1(final String sample1) { mySample1Text = sample1; }
    @Override public void setSample2(final String sample2) { mySample2Text = sample2; }
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
                || !mySample1Text.equals(mySettings.getRegexSample1Text())
                || !mySample2Text.equals(mySettings.getRegexSample2Text())
                
                || components.isModified(mySettings)
                ;
    }

    public void apply() {
        mySettings.setCustomizedNextWordBounds(myCustomizedNextWordBounds.getValue());
        mySettings.setCustomizedNextWordEndBounds(myCustomizedNextWordEndBounds.getValue());
        mySettings.setCustomizedNextWordStartBounds(myCustomizedNextWordStartBounds.getValue());
        mySettings.setCustomizedPrevWordBounds(myCustomizedPrevWordBounds.getValue());
        mySettings.setCustomizedPrevWordEndBounds(myCustomizedPrevWordEndBounds.getValue());
        mySettings.setCustomizedPrevWordStartBounds(myCustomizedPrevWordStartBounds.getValue());
        mySettings.setRegexSample1Text(mySample1Text);
        mySettings.setRegexSample2Text(mySample2Text);

        components.apply(mySettings);

        if (mySettings.isMouseCamelHumpsFollow()) {
            EditorSettingsExternalizable settings = EditorSettingsExternalizable.getInstance();
            settings.setMouseClickSelectionHonorsCamelWords(settings.isCamelWords());
        }
    }

    public void reset() {
        myCustomizedNextWordBounds.setValue(mySettings.getCustomizedNextWordBounds());
        myCustomizedNextWordEndBounds.setValue(mySettings.getCustomizedNextWordEndBounds());
        myCustomizedNextWordStartBounds.setValue(mySettings.getCustomizedNextWordStartBounds());
        myCustomizedPrevWordBounds.setValue(mySettings.getCustomizedPrevWordBounds());
        myCustomizedPrevWordEndBounds.setValue(mySettings.getCustomizedPrevWordEndBounds());
        myCustomizedPrevWordStartBounds.setValue(mySettings.getCustomizedPrevWordStartBounds());
        mySample1Text = mySettings.getRegexSample1Text();
        mySample2Text = mySettings.getRegexSample2Text();

        components.reset(mySettings);

        updateOptions(false);
    }

    @Override
    public void dispose() {

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
        mySelectPastedMultiCaretPredicate.setEnabled(mySelectPasted.isEnabled() && mySelectPasted.isSelected());
        myMultiPasteShowInstructions.setEnabled(myOverrideStandardPaste.isEnabled() && myOverrideStandardPaste.isSelected());
        myMultiPasteShowEolInViewer.setEnabled(myOverrideStandardPaste.isEnabled() && myOverrideStandardPaste.isSelected());
        myMultiPasteShowEolInList.setEnabled(myOverrideStandardPaste.isEnabled() && myOverrideStandardPaste.isSelected());

        final boolean regexPrefixes = RemovePrefixOnPastePatternType.ADAPTER.get(myRemovePrefixOnPastePattern) == RemovePrefixOnPastePatternType.REGEX;
        final boolean enablePrefixes =
                myRemovePrefixOnPaste.isSelected() && myRemovePrefixOnPaste.isEnabled()
                        || myAddPrefixOnPaste.isSelected() && myAddPrefixOnPaste.isEnabled();

        myRemovePrefixOnPaste1.setEnabled(enablePrefixes);
        myRemovePrefixOnPaste2.setEnabled(enablePrefixes);
        myEditRegExButton.setVisible(regexPrefixes);

        myDuplicateAtStartOrEndPredicate.setEnabled(myDuplicateAtStartOrEnd.isEnabled() && myDuplicateAtStartOrEnd.isSelected());
        myLinePasteCaretAdjustment.setEnabled(myOverrideStandardPaste.isEnabled() && myOverrideStandardPaste.isSelected());
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
        myRemovePrefixOnPastePattern = RemovePrefixOnPastePatternType.ADAPTER.createComboBox();
        myMouseModifier = MouseModifierType.ADAPTER.createComboBox();

        final SpinnerNumberModel model = new SpinnerNumberModel(500, 0, 10000, 50);
        myAutoIndentDelay = new JSpinner(model);

        mySetVirtualSpace = new HyperlinkLabel();
        mySetVirtualSpace.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(final HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    boolean isVirtualSpace = EditorSettingsExternalizable.getInstance().isVirtualSpace();
                    EditorSettingsExternalizable.getInstance().setVirtualSpace(!isVirtualSpace);
                    mySettings.setWeSetVirtualSpace(!isVirtualSpace);
                    updateOptions(false);
                    //DataContext context = DataManager.getInstance().getDataContextFromFocus().getResult();
                    //if (context != null) {
                    //    Settings settings = Settings.KEY.getData(context);
                    //    if (settings != null) {
                    //        Configurable configurable = settings.find(EditorOptions.ID);
                    //        settings.select(configurable);
                    //    }
                    //}
                }
            }
        });
    }
}
