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
    private JBCheckBox myMouseLineSelection;
    private JBCheckBox myEditorBackspaceKey;
    private JBCheckBox myEditorDeleteKey;
    private JBCheckBox myEditorUpDownKeys;
    private JBCheckBox myEditorLeftRightKeys;
    private ApplicationSettings mySettings;

    public ApplicationSettingsForm(ApplicationSettings settings) {
        mySettings = settings;
        myEditorLeftRightKeys.setVisible(false);
    }

    public JComponent getComponent() {
        return myMainPanel;
    }

    public boolean isModified() {
        //noinspection PointlessBooleanExpression
        return false
                || myMouseLineSelection.isSelected() != mySettings.isMouseLineSelection()
                || myEditorBackspaceKey.isSelected() != mySettings.isEditorBackspaceKey()
                || myEditorDeleteKey.isSelected() != mySettings.isEditorDeleteKey()
                || myEditorUpDownKeys.isSelected() != mySettings.isEditorUpDownKeys()
                || myEditorLeftRightKeys.isSelected() != mySettings.isEditorLeftRightKeys()
                ;
    }

    public void apply() {
        mySettings.setMouseLineSelection(myMouseLineSelection.isSelected());
        mySettings.setEditorBackspaceKey(myEditorBackspaceKey.isSelected());
        mySettings.setEditorDeleteKey(myEditorDeleteKey.isSelected());
        mySettings.setEditorUpDownKeys(myEditorUpDownKeys.isSelected());
        mySettings.setEditorLeftRightKeys(myEditorLeftRightKeys.isSelected());
    }

    public void reset() {
        myMouseLineSelection.setSelected(mySettings.isMouseLineSelection());
        myEditorBackspaceKey.setSelected(mySettings.isEditorBackspaceKey());
        myEditorDeleteKey.setSelected(mySettings.isEditorDeleteKey());
        myEditorUpDownKeys.setSelected(mySettings.isEditorUpDownKeys());
        myEditorLeftRightKeys.setSelected(mySettings.isEditorLeftRightKeys());
    }

    @Override
    public void dispose() {

    }
}
