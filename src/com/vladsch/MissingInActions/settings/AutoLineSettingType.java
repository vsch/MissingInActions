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

import com.vladsch.MissingInActions.Bundle;
import com.vladsch.MissingInActions.util.ui.ComboBoxAdaptable;
import com.vladsch.MissingInActions.util.ui.ComboBoxAdapter;
import com.vladsch.MissingInActions.util.ui.ComboBoxAdapterImpl;

import javax.swing.*;

public enum AutoLineSettingType implements ComboBoxAdaptable<AutoLineSettingType> {
    DISABLED(0, Bundle.message("settings.auto-line.types.disabled")),
    ENABLED(1, Bundle.message("settings.auto-line.types.enabled")),
    EXPERT(2, Bundle.message("settings.auto-line.types.expert"));

    public final String displayName;
    public final int intValue;

    AutoLineSettingType(int intValue, String displayName) {
        this.intValue = intValue;
        this.displayName = displayName;
    }

    public static final AutoLineSettingType DEFAULT = DISABLED;
    public static final ComboBoxAdapter<AutoLineSettingType> ADAPTER = new ComboBoxAdapterImpl<>(DEFAULT);

    public static AutoLineSettingType get(JComboBox comboBox) {
        return ADAPTER.findEnum((String) comboBox.getSelectedItem());
    }

    public static AutoLineSettingType get(int value) {
        return ADAPTER.findEnum(value);
    }

    public static int getInt(JComboBox comboBox) {
        return ADAPTER.findEnum((String) comboBox.getSelectedItem()).intValue;
    }

    static void set(JComboBox comboBox, int intValue) {
        comboBox.setSelectedItem(ADAPTER.findEnum(intValue).displayName);
    }

    public static JComboBox createComboBox() {
        JComboBox comboBox = new JComboBox();
        ADAPTER.fillComboBox(comboBox);
        return comboBox;
    }

    public static AutoLineSettingType findEnum(int intValue) { return ADAPTER.findEnum(intValue); }

    public AutoLineSettingType findEnum(String displayName) { return ADAPTER.findEnum(displayName); }

    @Override
    public AutoLineSettingType[] getEnumValues() {
        return values();
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isDefault() {
        return this == DEFAULT;
    }

    public int getIntValue() {
        return intValue;
    }
}
