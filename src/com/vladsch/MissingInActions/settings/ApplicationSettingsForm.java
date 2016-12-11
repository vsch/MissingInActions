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

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

@SuppressWarnings("WeakerAccess")
public class ApplicationSettingsForm implements Disposable {
    private JPanel myMainPanel;
    final private ApplicationSettings mySettings;

    private HyperlinkLabel mySetVirtualSpace;
    private HyperlinkLabel myPreambleLabel;
    private JComboBox myAutoLineMode;
    private JBCheckBox myMouseLineSelection;
    private JBCheckBox myUpDownSelection;
    private JBCheckBox myDeleteOperations;
    private JBCheckBox myUpDownMovement;
    private JBCheckBox myLeftRightMovement;
    private JBCheckBox myCopyLineOrLineSelection;
    private JBCheckBox myStartEndAsLineSelection;
    private JComboBox myMouseModifier;
    private JBCheckBox myAutoIndent;
    private JBCheckBox mySelectPasted;
    private JBCheckBox mySelectPastedMultiCaret;
    private JBCheckBox myPreserveCamelCaseOnPaste;
    private JBCheckBox myPreserveScreamingSnakeCaseOnPaste;
    private JBCheckBox myPreserveSnakeCaseOnPaste;
    private JBCheckBox myRemovePrefixOnPaste;
    private JBCheckBox myAddPrefixOnPaste;
    private JBCheckBox myOverrideStandardPaste;
    private JBCheckBox myOverrideStandardPasteShowInstructions;
    private JBTextField myRemovePrefixOnPaste1;
    private JBTextField myRemovePrefixOnPaste2;
    private JComboBox mySelectPastedPredicate;
    private JBCheckBox myUnselectToggleCase;
    private JSpinner myAutoIndentDelay;
    private JBCheckBox myDuplicateAtStartOrEnd;
    private JComboBox myDuplicateAtStartOrEndPredicate;
    private JComboBox myLinePasteCaretAdjustment;
    private JBCheckBox myMouseCamelHumpsFollow;
    private CustomizedBoundaryForm myCustomizedNextWordBounds;
    private CustomizedBoundaryForm myCustomizedNextWordStartBounds;
    private CustomizedBoundaryForm myCustomizedNextWordEndBounds;
    private CustomizedBoundaryForm myCustomizedPrevWordBounds;
    private CustomizedBoundaryForm myCustomizedPrevWordStartBounds;
    private CustomizedBoundaryForm myCustomizedPrevWordEndBounds;
    private CustomizedBoundaryLabelForm myCustomizedBoundaryLabelForm;
    private JBCheckBox myIsSelectionEndExtendedPastCaret;
    private JBCheckBox myIsSelectionStartExtendedBeforeCaret;
    private JBCheckBox myTypingDeletesLineSelection;
    private JBCheckBox myIndentUnindent;
    private JComboBox myCaretOnMoveSelectionDown;
    private JComboBox myCaretOnMoveSelectionUp;
    private JComboBox myRemovePrefixOnPasteType;

    public ApplicationSettingsForm(ApplicationSettings settings) {
        mySettings = settings;

        myMouseLineSelection.addActionListener(e -> updateOptions(false));
        myUpDownSelection.addActionListener(e -> updateOptions(false));
        myAutoIndent.addActionListener(e -> updateOptions(false));
        mySelectPasted.addActionListener(e -> updateOptions(false));
        myDuplicateAtStartOrEnd.addActionListener(e -> updateOptions(false));
        myMouseCamelHumpsFollow.addActionListener(e -> updateOptions(false));
        myRemovePrefixOnPaste.addActionListener(e -> updateOptions(false));
        myAddPrefixOnPaste.addActionListener(e -> updateOptions(false));
        myOverrideStandardPaste.addActionListener(e -> updateOptions(false));

        updateOptions(true);
    }

    public JComponent getComponent() {
        return myMainPanel;
    }

    public boolean isModified() {
        int wordMask = EditHelpers.START_OF_WORD | EditHelpers.END_OF_WORD;
        //noinspection PointlessBooleanExpression
        return false
                || AutoLineSettingType.ADAPTER.findEnum((String) myAutoLineMode.getSelectedItem()).getIntValue() != mySettings.getAutoLineMode()
                || MouseModifierType.ADAPTER.findEnum((String) myMouseModifier.getSelectedItem()).getIntValue() != mySettings.getMouseModifier()
                || myMouseLineSelection.isSelected() != mySettings.isMouseLineSelection()
                || myIsSelectionEndExtendedPastCaret.isSelected() != mySettings.isSelectionEndExtended()
                || myIsSelectionStartExtendedBeforeCaret.isSelected() != mySettings.isSelectionStartExtended()
                || myDeleteOperations.isSelected() != mySettings.isDeleteOperations()
                || myUpDownMovement.isSelected() != mySettings.isUpDownMovement()
                || myIndentUnindent.isSelected() != mySettings.isIndentUnindent()
                || myLeftRightMovement.isSelected() != mySettings.isLeftRightMovement()
                || myUpDownSelection.isSelected() != mySettings.isUpDownSelection()
                || myCopyLineOrLineSelection.isSelected() != mySettings.isCopyLineOrLineSelection()
                || myStartEndAsLineSelection.isSelected() != mySettings.isStartEndAsLineSelection()
                || myAutoIndent.isSelected() != mySettings.isAutoIndent()
                || mySelectPasted.isSelected() != mySettings.isSelectPasted()
                || mySelectPastedMultiCaret.isSelected() != mySettings.isSelectPastedMultiCaret()
                || myPreserveCamelCaseOnPaste.isSelected() != mySettings.isPreserveCamelCaseOnPaste()
                || myPreserveScreamingSnakeCaseOnPaste.isSelected() != mySettings.isPreserveScreamingSnakeCaseOnPaste()
                || myPreserveSnakeCaseOnPaste.isSelected() != mySettings.isPreserveSnakeCaseOnPaste()
                || myRemovePrefixOnPaste.isSelected() != mySettings.isRemovePrefixOnPaste()
                || myAddPrefixOnPaste.isSelected() != mySettings.isAddPrefixOnPaste()
                || myOverrideStandardPaste.isSelected() != mySettings.isOverrideStandardPaste()
                || myOverrideStandardPasteShowInstructions.isSelected() != mySettings.isOverrideStandardPasteShowInstructions()
                || !myRemovePrefixOnPaste1.getText().trim().equals(mySettings.getRemovePrefixOnPaste1().trim())
                || !myRemovePrefixOnPaste2.getText().trim().equals(mySettings.getRemovePrefixOnPaste2().trim())
                || myUnselectToggleCase.isSelected() != mySettings.isUnselectToggleCase()
                || myDuplicateAtStartOrEnd.isSelected() != mySettings.isDuplicateAtStartOrEnd()
                || (Integer) myAutoIndentDelay.getValue() != mySettings.getAutoIndentDelay()
                || myMouseCamelHumpsFollow.isSelected() != mySettings.isMouseCamelHumpsFollow()
                || myTypingDeletesLineSelection.isSelected() != mySettings.isTypingDeletesLineSelection()
                || (myCustomizedNextWordStartBounds.getValue() & ~wordMask) != (mySettings.getCustomizedNextWordStartBounds() & ~wordMask)
                || (myCustomizedPrevWordStartBounds.getValue() & ~wordMask) != (mySettings.getCustomizedPrevWordStartBounds() & ~wordMask)
                || (myCustomizedNextWordBounds.getValue() & ~wordMask) != (mySettings.getCustomizedNextWordBounds() & ~wordMask)
                || (myCustomizedPrevWordBounds.getValue() & ~wordMask) != (mySettings.getCustomizedPrevWordBounds() & ~wordMask)
                || (myCustomizedNextWordEndBounds.getValue() & ~wordMask) != (mySettings.getCustomizedNextWordEndBounds() & ~wordMask)
                || (myCustomizedPrevWordEndBounds.getValue() & ~wordMask) != (mySettings.getCustomizedPrevWordEndBounds() & ~wordMask)

                || SelectionPredicateType.ADAPTER.findEnum((String) mySelectPastedPredicate.getSelectedItem()).intValue != mySettings.getSelectPastedPredicate()
                || SelectionPredicateType.ADAPTER.findEnum((String) myDuplicateAtStartOrEndPredicate.getSelectedItem()).intValue != mySettings.getDuplicateAtStartOrEndPredicate()
                || LinePasteCaretAdjustmentType.ADAPTER.findEnum((String) myLinePasteCaretAdjustment.getSelectedItem()).intValue != mySettings.getLinePasteCaretAdjustment()
                || CaretAdjustmentType.ADAPTER.findEnum((String) myCaretOnMoveSelectionDown.getSelectedItem()).intValue != mySettings.getCaretOnMoveSelectionDown()
                || CaretAdjustmentType.ADAPTER.findEnum((String) myCaretOnMoveSelectionUp.getSelectedItem()).intValue != mySettings.getCaretOnMoveSelectionUp()
                || RemovePrefixOnPasteType.ADAPTER.findEnum((String) myRemovePrefixOnPasteType.getSelectedItem()).intValue != mySettings.getRemovePrefixOnPasteType()
                ;
    }

    public void apply() {
        mySettings.setAutoLineMode(AutoLineSettingType.ADAPTER.findEnum((String) myAutoLineMode.getSelectedItem()).getIntValue());
        mySettings.setMouseModifier(MouseModifierType.ADAPTER.findEnum((String) myMouseModifier.getSelectedItem()).getIntValue());
        mySettings.setMouseLineSelection(myMouseLineSelection.isSelected());
        mySettings.setSelectionEndExtended(myIsSelectionEndExtendedPastCaret.isSelected());
        mySettings.setSelectionStartExtended(myIsSelectionStartExtendedBeforeCaret.isSelected());
        mySettings.setDeleteOperations(myDeleteOperations.isSelected());
        mySettings.setUpDownMovement(myUpDownMovement.isSelected());
        mySettings.setIndentUnindent(myIndentUnindent.isSelected());
        mySettings.setLeftRightMovement(myLeftRightMovement.isSelected());
        mySettings.setUpDownSelection(myUpDownSelection.isSelected());
        mySettings.setCopyLineOrLineSelection(myCopyLineOrLineSelection.isSelected());
        mySettings.setStartEndAsLineSelection(myStartEndAsLineSelection.isSelected());
        mySettings.setAutoIndent(myAutoIndent.isSelected());
        mySettings.setSelectPasted(mySelectPasted.isSelected());
        mySettings.setSelectPastedMultiCaret(mySelectPastedMultiCaret.isSelected());
        mySettings.setPreserveCamelCaseOnPaste(myPreserveCamelCaseOnPaste.isSelected());
        mySettings.setPreserveScreamingSnakeCaseOnPaste(myPreserveScreamingSnakeCaseOnPaste.isSelected());
        mySettings.setPreserveSnakeCaseOnPaste(myPreserveSnakeCaseOnPaste.isSelected());
        mySettings.setRemovePrefixOnPaste(myRemovePrefixOnPaste.isSelected());
        mySettings.setAddPrefixOnPaste(myAddPrefixOnPaste.isSelected());
        mySettings.setOverrideStandardPaste(myOverrideStandardPaste.isSelected());
        mySettings.setOverrideStandardPasteShowInstructions(myOverrideStandardPasteShowInstructions.isSelected());
        mySettings.setRemovePrefixOnPaste1(myRemovePrefixOnPaste1.getText().trim());
        mySettings.setRemovePrefixOnPaste2(myRemovePrefixOnPaste2.getText().trim());
        mySettings.setUnselectToggleCase(myUnselectToggleCase.isSelected());
        mySettings.setDuplicateAtStartOrEnd(myDuplicateAtStartOrEnd.isSelected());
        mySettings.setMouseCamelHumpsFollow(myMouseCamelHumpsFollow.isSelected());
        mySettings.setTypingDeletesLineSelection(myTypingDeletesLineSelection.isSelected());
        mySettings.setAutoIndentDelay((Integer) myAutoIndentDelay.getValue());
        mySettings.setCustomizedNextWordStartBounds(myCustomizedNextWordStartBounds.getValue());
        mySettings.setCustomizedPrevWordStartBounds(myCustomizedPrevWordStartBounds.getValue());
        mySettings.setCustomizedNextWordBounds(myCustomizedNextWordBounds.getValue());
        mySettings.setCustomizedPrevWordBounds(myCustomizedPrevWordBounds.getValue());
        mySettings.setCustomizedNextWordEndBounds(myCustomizedNextWordEndBounds.getValue());
        mySettings.setCustomizedPrevWordEndBounds(myCustomizedPrevWordEndBounds.getValue());

        mySettings.setSelectPastedPredicate(SelectionPredicateType.ADAPTER.findEnum((String) mySelectPastedPredicate.getSelectedItem()).intValue);
        mySettings.setDuplicateAtStartOrEndPredicate(SelectionPredicateType.ADAPTER.findEnum((String) myDuplicateAtStartOrEndPredicate.getSelectedItem()).intValue);
        mySettings.setLinePasteCaretAdjustment(LinePasteCaretAdjustmentType.ADAPTER.findEnum((String) myLinePasteCaretAdjustment.getSelectedItem()).intValue);

        mySettings.setCaretOnMoveSelectionDown(CaretAdjustmentType.ADAPTER.findEnum((String) myCaretOnMoveSelectionDown.getSelectedItem()).intValue);
        mySettings.setCaretOnMoveSelectionUp(CaretAdjustmentType.ADAPTER.findEnum((String) myCaretOnMoveSelectionUp.getSelectedItem()).intValue);
        mySettings.setRemovePrefixOnPasteType(RemovePrefixOnPasteType.ADAPTER.findEnum((String) myRemovePrefixOnPasteType.getSelectedItem()).intValue);

        if (mySettings.isMouseCamelHumpsFollow()) {
            EditorSettingsExternalizable settings = EditorSettingsExternalizable.getInstance();
            settings.setMouseClickSelectionHonorsCamelWords(settings.isCamelWords());
        }
    }

    public void reset() {
        myAutoLineMode.setSelectedItem(AutoLineSettingType.ADAPTER.findEnum(mySettings.getAutoLineMode()).getDisplayName());
        myMouseModifier.setSelectedItem(MouseModifierType.ADAPTER.findEnum(mySettings.getMouseModifier()).getDisplayName());
        myMouseLineSelection.setSelected(mySettings.isMouseLineSelection());
        myIsSelectionEndExtendedPastCaret.setSelected(mySettings.isSelectionEndExtended());
        myIsSelectionStartExtendedBeforeCaret.setSelected(mySettings.isSelectionStartExtended());
        myDeleteOperations.setSelected(mySettings.isDeleteOperations());
        myUpDownMovement.setSelected(mySettings.isUpDownMovement());
        myIndentUnindent.setSelected(mySettings.isIndentUnindent());
        myLeftRightMovement.setSelected(mySettings.isLeftRightMovement());
        myUpDownSelection.setSelected(mySettings.isUpDownSelection());
        myCopyLineOrLineSelection.setSelected(mySettings.isCopyLineOrLineSelection());
        myStartEndAsLineSelection.setSelected(mySettings.isStartEndAsLineSelection());
        myAutoIndent.setSelected(mySettings.isAutoIndent());
        mySelectPasted.setSelected(mySettings.isSelectPasted());
        mySelectPastedMultiCaret.setSelected(mySettings.isSelectPastedMultiCaret());
        myPreserveCamelCaseOnPaste.setSelected(mySettings.isPreserveCamelCaseOnPaste());
        myPreserveScreamingSnakeCaseOnPaste.setSelected(mySettings.isPreserveScreamingSnakeCaseOnPaste());
        myPreserveSnakeCaseOnPaste.setSelected(mySettings.isPreserveSnakeCaseOnPaste());
        myRemovePrefixOnPaste.setSelected(mySettings.isRemovePrefixOnPaste());
        myAddPrefixOnPaste.setSelected(mySettings.isAddPrefixOnPaste());
        myOverrideStandardPaste.setSelected(mySettings.isOverrideStandardPaste());
        myOverrideStandardPasteShowInstructions.setSelected(mySettings.isOverrideStandardPasteShowInstructions());
        myRemovePrefixOnPaste1.setText(mySettings.getRemovePrefixOnPaste1().trim());
        myRemovePrefixOnPaste2.setText(mySettings.getRemovePrefixOnPaste2().trim());

        mySelectPastedPredicate.setSelectedItem(SelectionPredicateType.ADAPTER.findEnum(mySettings.getSelectPastedPredicate()).displayName);
        myDuplicateAtStartOrEndPredicate.setSelectedItem(SelectionPredicateType.ADAPTER.findEnum(mySettings.getDuplicateAtStartOrEndPredicate()).displayName);
        myLinePasteCaretAdjustment.setSelectedItem(LinePasteCaretAdjustmentType.ADAPTER.findEnum(mySettings.getLinePasteCaretAdjustment()).displayName);
        myCaretOnMoveSelectionDown.setSelectedItem(CaretAdjustmentType.ADAPTER.findEnum(mySettings.getCaretOnMoveSelectionDown()).displayName);
        myRemovePrefixOnPasteType.setSelectedItem(RemovePrefixOnPasteType.ADAPTER.findEnum(mySettings.getRemovePrefixOnPasteType()).displayName);

        myUnselectToggleCase.setSelected(mySettings.isUnselectToggleCase());
        myDuplicateAtStartOrEnd.setSelected(mySettings.isDuplicateAtStartOrEnd());
        myMouseCamelHumpsFollow.setSelected(mySettings.isMouseCamelHumpsFollow());
        myTypingDeletesLineSelection.setSelected(mySettings.isTypingDeletesLineSelection());
        myAutoIndentDelay.setValue(mySettings.getAutoIndentDelay());
        myCustomizedNextWordStartBounds.setValue(mySettings.getCustomizedNextWordStartBounds());
        myCustomizedPrevWordStartBounds.setValue(mySettings.getCustomizedPrevWordStartBounds());
        myCustomizedNextWordBounds.setValue(mySettings.getCustomizedNextWordBounds());
        myCustomizedPrevWordBounds.setValue(mySettings.getCustomizedPrevWordBounds());
        myCustomizedNextWordEndBounds.setValue(mySettings.getCustomizedNextWordEndBounds());
        myCustomizedPrevWordEndBounds.setValue(mySettings.getCustomizedPrevWordEndBounds());
        updateOptions(false);
    }

    @Override
    public void dispose() {

    }

    @SuppressWarnings("ConstantConditions")
    void updateOptions(boolean typeChanged) {
        AutoLineSettingType type = AutoLineSettingType.ADAPTER.findEnum((String) myAutoLineMode.getSelectedItem());
        boolean enabled = false;
        boolean selected = false;
        boolean untestedSelected = false;
        boolean forced = false;

        if (type == AutoLineSettingType.ENABLED) {
            enabled = false;
            selected = true;
        } else if (type == AutoLineSettingType.EXPERT) {
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
        myIsSelectionEndExtendedPastCaret.setEnabled(selected || forced);
        myIsSelectionStartExtendedBeforeCaret.setEnabled(selected || forced);
        myMouseModifier.setEnabled(selected && myMouseLineSelection.isSelected());
        myUpDownSelection.setEnabled(enabled);
        myUpDownMovement.setEnabled(enabled && modeEnabled);
        myStartEndAsLineSelection.setEnabled(enabled && modeEnabled);
        myIndentUnindent.setEnabled(enabled && modeEnabled);
        myDeleteOperations.setEnabled(enabled && modeEnabled);
        myLeftRightMovement.setEnabled(enabled && modeEnabled);
        myCopyLineOrLineSelection.setEnabled(enabled && modeEnabled);
        mySelectPastedPredicate.setEnabled(mySelectPasted.isEnabled() && mySelectPasted.isSelected());
        mySelectPastedMultiCaret.setEnabled(mySelectPasted.isEnabled() && mySelectPasted.isSelected());
        myOverrideStandardPasteShowInstructions.setEnabled(myOverrideStandardPaste.isEnabled() && myOverrideStandardPaste.isSelected());

        final boolean enablePrefixes = myRemovePrefixOnPaste.isSelected() && myRemovePrefixOnPaste.isEnabled()
                || myAddPrefixOnPaste.isSelected() && myAddPrefixOnPaste.isEnabled();

        myRemovePrefixOnPaste1.setEnabled(enablePrefixes);
        myRemovePrefixOnPaste2.setEnabled(enablePrefixes);

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
        myAutoLineMode = new JComboBox();
        AutoLineSettingType.fillComboBox(myAutoLineMode);
        myAutoLineMode.addActionListener(e -> updateOptions(true));

        mySelectPastedPredicate = new JComboBox();
        SelectionPredicateType.fillComboBox(mySelectPastedPredicate);

        myDuplicateAtStartOrEndPredicate = new JComboBox();
        SelectionPredicateType.fillComboBox(myDuplicateAtStartOrEndPredicate);

        myLinePasteCaretAdjustment = new JComboBox();
        LinePasteCaretAdjustmentType.fillComboBox(myLinePasteCaretAdjustment);

        myCaretOnMoveSelectionDown = new JComboBox();
        CaretAdjustmentType.fillComboBox(myCaretOnMoveSelectionDown);
        myCaretOnMoveSelectionUp = new JComboBox();
        CaretAdjustmentType.fillComboBox(myCaretOnMoveSelectionUp);

        myRemovePrefixOnPasteType = new JComboBox();
        RemovePrefixOnPasteType.fillComboBox(myRemovePrefixOnPasteType);

        myMouseModifier = new JComboBox();
        MouseModifierType.fillComboBox(myMouseModifier);
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
