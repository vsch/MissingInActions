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
import com.intellij.ui.components.JBCheckBox;

import javax.swing.*;

@SuppressWarnings("WeakerAccess")
public class ApplicationSettingsForm implements Disposable {
    private JPanel myMainPanel;
    final private ApplicationSettings mySettings;

    private JComboBox myAutoLineMode;
    private JBCheckBox myMouseLineSelection;
    private JBCheckBox myUpDownSelection;
    private JBCheckBox myDeleteOperations;
    private JBCheckBox myUpDownMovement;
    private JBCheckBox myLeftRightMovement;
    private JComboBox myMouseModifier;
    private JBCheckBox myAutoIndent;
    private JSpinner myAutoIndentDelay;

    public ApplicationSettingsForm(ApplicationSettings settings) {
        mySettings = settings;

        myMouseLineSelection.addActionListener(e -> updateOptions(false));
        myUpDownSelection.addActionListener(e -> updateOptions(false));
        myAutoIndent.addActionListener(e -> updateOptions(false));
        
        updateOptions(true);
    }

    public JComponent getComponent() {
        return myMainPanel;
    }

    public boolean isModified() {
        //noinspection PointlessBooleanExpression
        return false
                || AutoLineSettingType.ADAPTER.findEnum((String) myAutoLineMode.getSelectedItem()).getIntValue() != mySettings.getAutoLineMode()
                || MouseModifierType.ADAPTER.findEnum((String) myMouseModifier.getSelectedItem()).getIntValue() != mySettings.getMouseModifier()
                || myMouseLineSelection.isSelected() != mySettings.isMouseLineSelection()
                || myDeleteOperations.isSelected() != mySettings.isDeleteOperations()
                || myUpDownMovement.isSelected() != mySettings.isUpDownMovement()
                || myLeftRightMovement.isSelected() != mySettings.isLeftRightMovement()
                || myUpDownSelection.isSelected() != mySettings.isUpDownSelection()
                || myAutoIndent.isSelected() != mySettings.isAutoIndent()
                || (Integer)myAutoIndentDelay.getValue() != mySettings.getAutoIndentDelay()
                ;
    }

    public void apply() {
        mySettings.setAutoLineMode(AutoLineSettingType.ADAPTER.findEnum((String) myAutoLineMode.getSelectedItem()).getIntValue());
        mySettings.setMouseModifier(MouseModifierType.ADAPTER.findEnum((String) myMouseModifier.getSelectedItem()).getIntValue());
        mySettings.setMouseLineSelection(myMouseLineSelection.isSelected());
        mySettings.setDeleteOperations(myDeleteOperations.isSelected());
        mySettings.setUpDownMovement(myUpDownMovement.isSelected());
        mySettings.setLeftRightMovement(myLeftRightMovement.isSelected());
        mySettings.setUpDownSelection(myUpDownSelection.isSelected());
        mySettings.setAutoIndent(myAutoIndent.isSelected());
        mySettings.setAutoIndentDelay((Integer) myAutoIndentDelay.getValue());
    }

    public void reset() {
        myAutoLineMode.setSelectedItem(AutoLineSettingType.ADAPTER.findEnum(mySettings.getAutoLineMode()).getDisplayName());
        myMouseModifier.setSelectedItem(MouseModifierType.ADAPTER.findEnum(mySettings.getMouseModifier()).getDisplayName());
        myMouseLineSelection.setSelected(mySettings.isMouseLineSelection());
        myDeleteOperations.setSelected(mySettings.isDeleteOperations());
        myUpDownMovement.setSelected(mySettings.isUpDownMovement());
        myLeftRightMovement.setSelected(mySettings.isLeftRightMovement());
        myUpDownSelection.setSelected(mySettings.isUpDownSelection());
        myAutoIndent.setSelected(mySettings.isAutoIndent());
        myAutoIndentDelay.setValue(mySettings.getAutoIndentDelay());
        updateOptions(false);
    }

    @Override
    public void dispose() {

    }

    void updateOptions(boolean typeChanged) {
        AutoLineSettingType type = AutoLineSettingType.ADAPTER.findEnum((String) myAutoLineMode.getSelectedItem());
        boolean enabled = false;
        boolean selected = false;
        boolean untestedSelected = false;
        
        if (type == AutoLineSettingType.ENABLED) {
            enabled = false;
            selected = true;
        } else if (type == AutoLineSettingType.EXPERT) {
            enabled = true;
            selected = true;
        } else {
            typeChanged = true;
        }
        
        if (typeChanged) myMouseLineSelection.setSelected(selected);
        if (typeChanged) myUpDownSelection.setSelected(selected);

        boolean modeEnabled = myMouseLineSelection.isSelected() || myUpDownSelection.isSelected();
        if (typeChanged || !modeEnabled) myDeleteOperations.setSelected(selected && modeEnabled);
        if (typeChanged || !modeEnabled) myLeftRightMovement.setSelected(selected && modeEnabled);
        if (typeChanged || !modeEnabled) myUpDownMovement.setSelected(selected && modeEnabled);

        myMouseLineSelection.setEnabled(selected);
        myMouseModifier.setEnabled(selected && myMouseLineSelection.isSelected());
        myUpDownSelection.setEnabled(enabled);
        myUpDownMovement.setEnabled(enabled && modeEnabled);
        myDeleteOperations.setEnabled(enabled && modeEnabled);
        myLeftRightMovement.setEnabled(enabled && modeEnabled);
        
        myAutoIndentDelay.setEnabled(myAutoIndent.isEnabled() && myAutoIndent.isSelected());
    }

    private void createUIComponents() {
        myAutoLineMode = new JComboBox();
        AutoLineSettingType.fillComboBox(myAutoLineMode);
        myAutoLineMode.addActionListener(e -> updateOptions(true));
        myMouseModifier = new JComboBox();
        MouseModifierType.fillComboBox(myMouseModifier);
        final SpinnerNumberModel model = new SpinnerNumberModel(500, 0, 10000, 50);
        myAutoIndentDelay = new JSpinner(model);
    }
}
