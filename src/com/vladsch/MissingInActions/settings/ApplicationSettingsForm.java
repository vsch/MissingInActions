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

import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.vladsch.MissingInActions.Bundle;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.MissingInActions.util.Utils;
import com.vladsch.MissingInActions.util.ui.CheckBoxWithColorChooser;
import com.vladsch.MissingInActions.util.ui.Settable;
import com.vladsch.MissingInActions.util.ui.SettingsComponents;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import static com.vladsch.MissingInActions.manager.LineSelectionManager.*;

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
    JBCheckBox myMultiPastePreserveOriginal;
    JBCheckBox myMultiPasteDeleteRepeatedCaretData;
    JBCheckBox myOverrideStandardPaste;
    JBCheckBox myPreserveCamelCaseOnPaste;
    JBCheckBox myPreserveScreamingSnakeCaseOnPaste;
    JBCheckBox myPreserveSnakeCaseOnPaste;
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
    private JComboBox myPrimaryCaretThickness;
    private CheckBoxWithColorChooser myPrimaryCaretColor;
    private JComboBox mySearchStartCaretThickness;
    private CheckBoxWithColorChooser mySearchStartCaretColor;
    private JComboBox mySearchFoundCaretThickness;
    private CheckBoxWithColorChooser mySearchFoundCaretColor;
    private JTextPane myCaretVisualAttributesPane;

    private @NotNull String myRegexSampleText;
    private final EditingCommitter myEditingCommitter;

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
                        component(CaretThicknessType.ADAPTER, myPrimaryCaretThickness, i::getPrimaryCaretThickness, i::setPrimaryCaretThickness),
                        component(CaretThicknessType.ADAPTER, mySearchStartCaretThickness, i::getSearchStartCaretThickness, i::setSearchStartCaretThickness),
                        component(CaretThicknessType.ADAPTER, mySearchFoundCaretThickness, i::getSearchFoundCaretThickness, i::setSearchFoundCaretThickness),
                        component(myPrimaryCaretColor, i::primaryCaretColorRGB, i::primaryCaretColorRGB),
                        componentEnabled(myPrimaryCaretColor, i::isPrimaryCaretColorEnabled, i::setPrimaryCaretColorEnabled),
                        component(mySearchStartCaretColor, i::searchStartCaretColorRGB, i::searchStartCaretColorRGB),
                        componentEnabled(mySearchStartCaretColor, i::isSearchStartCaretColorEnabled, i::setSearchStartCaretColorEnabled),
                        component(mySearchFoundCaretColor, i::searchFoundCaretColorRGB, i::searchFoundCaretColorRGB),
                        componentEnabled(mySearchFoundCaretColor, i::isSearchFoundCaretColorEnabled, i::setSearchFoundCaretColorEnabled),
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
                        component(myMultiPastePreserveOriginal, i::isMultiPastePreserveOriginal, i::setMultiPastePreserveOriginal),
                        component(myMultiPasteDeleteRepeatedCaretData, i::isMultiPasteDeleteRepeatedCaretData, i::setMultiPasteDeleteRepeatedCaretData),
                        component(myOverrideStandardPaste, i::isOverrideStandardPaste, i::setOverrideStandardPaste),
                        component(myPreserveCamelCaseOnPaste, i::isPreserveCamelCaseOnPaste, i::setPreserveCamelCaseOnPaste),
                        component(myPreserveScreamingSnakeCaseOnPaste, i::isPreserveScreamingSnakeCaseOnPaste, i::setPreserveScreamingSnakeCaseOnPaste),
                        component(myPreserveSnakeCaseOnPaste, i::isPreserveSnakeCaseOnPaste, i::setPreserveSnakeCaseOnPaste),
                        component(myRemovePrefixOnPaste, i::isRemovePrefixOnPaste, i::setRemovePrefixOnPaste),
                        component(myPrefixOnPasteText, i::getPrefixesOnPasteText, i::setPrefixesOnPasteText),
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
            public void actionPerformed(final ActionEvent e) {ApplicationSettingsForm.this.updateOptions(false);}
        };

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

        myEditRegExButton.addActionListener(e -> {
            boolean valid = RegExTestDialog.showDialog(myMainPanel, this);
            myRemovePrefixOnPaste.setSelected(valid);
            myAddPrefixOnPaste.setSelected(valid);
        });

        myEditingCommitter = new EditingCommitter();
        IdeEventQueue.getInstance().addDispatcher(myEditingCommitter, this);

        if (!LineSelectionManager.isCaretAttributeAvailable()) {
            myPrimaryCaretThickness.setEnabled(false);
            myPrimaryCaretColor.setEnabled(false);
            mySearchStartCaretThickness.setEnabled(false);
            mySearchStartCaretColor.setEnabled(false);
            mySearchFoundCaretThickness.setEnabled(false);
            mySearchFoundCaretColor.setEnabled(false);
            myCaretVisualAttributesPane.setVisible(true);
            setContentBody(Bundle.message("settings.caret-visual-attributes.description"));
        } else {
            myCaretVisualAttributesPane.setVisible(false);
        }
        myCaretVisualAttributesPane.validate();
        myMainPanel.validate();

        updateOptions(true);
    }

    public void setContentBody(String text) {
        JLabel label = new JLabel();
        Font font = label.getFont();
        Color textColor = label.getForeground();
        String out = "<html><head></head><body><div style='font-family:" + font.getFontName() + ";" + "font-size:" + JBUI.scale(font.getSize()) + "pt; color:" + Utils.toRgbString(textColor) + "'>" +
                (text == null ? "" : text) +
                "</div></body></html>";
        myCaretVisualAttributesPane.setText(out);
        myMainPanel.validate();
    }

    // @formatter:off
    @NotNull @Override public String getPatternText() { return myPrefixOnPasteText.getText().trim(); }
    @NotNull @Override public String getSampleText() { return myRegexSampleText; }
    @Override public void setPatternText(final String patternText) { myPrefixOnPasteText.setText(patternText); }
    @Override public void setSampleText(final String sampleText) { myRegexSampleText = sampleText; }
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

        updateOptions(false);
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
        myMultiPastePreserveOriginal.setEnabled(myOverrideStandardPaste.isEnabled() && myOverrideStandardPaste.isSelected());
        myMultiPasteDeleteRepeatedCaretData.setEnabled(myOverrideStandardPaste.isEnabled() && myOverrideStandardPaste.isSelected());
        myMultiPasteShowEolInViewer.setEnabled(myOverrideStandardPaste.isEnabled() && myOverrideStandardPaste.isSelected());
        myMultiPasteShowEolInList.setEnabled(myOverrideStandardPaste.isEnabled() && myOverrideStandardPaste.isSelected());

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

        myPrimaryCaretThickness = CaretThicknessType.ADAPTER.createComboBox();
        myPrimaryCaretColor = new CheckBoxWithColorChooser(Bundle.message("settings.primary-caret-color.label"), false, Color.black);
        mySearchStartCaretThickness = CaretThicknessType.ADAPTER.createComboBox();
        mySearchStartCaretColor = new CheckBoxWithColorChooser(Bundle.message("settings.primary-caret-color.label"), false, Color.black);
        mySearchFoundCaretThickness = CaretThicknessType.ADAPTER.createComboBox();
        mySearchFoundCaretColor = new CheckBoxWithColorChooser(Bundle.message("settings.primary-caret-color.label"), false, Color.black);
    }

    private class EditingCommitter implements IdeEventQueue.EventDispatcher {
        @Override
        public boolean dispatch(AWTEvent e) {
            if (e instanceof KeyEvent && e.getID() == KeyEvent.KEY_PRESSED && ((KeyEvent) e).getKeyCode() == KeyEvent.VK_ENTER) {
                if ((((KeyEvent) e).getModifiers() & ~(InputEvent.CTRL_DOWN_MASK | InputEvent.CTRL_MASK)) == 0) {
                    Component owner = UIUtil.findParentByCondition(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(), component -> component instanceof JTable);

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
