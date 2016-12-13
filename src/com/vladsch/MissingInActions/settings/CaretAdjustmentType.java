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

import com.intellij.openapi.util.Pair;
import com.vladsch.MissingInActions.Bundle;
import com.vladsch.MissingInActions.util.ui.ComboBoxAdaptable;
import com.vladsch.MissingInActions.util.ui.ComboBoxAdapter;
import com.vladsch.MissingInActions.util.ui.ComboBoxAdapterImpl;
import com.vladsch.MissingInActions.util.ui.OnMap;

import javax.swing.*;

public enum CaretAdjustmentType implements ComboBoxAdaptable<CaretAdjustmentType> {
    NONE(0, Bundle.message("settings.line-selection.caret-adjustment.none")),
    TO_START(1, Bundle.message("settings.line-selection.caret-adjustment.to-start")),
    TO_END(2, Bundle.message("settings.line-selection.caret-adjustment.to-end")),
    TO_ANCHOR(3, Bundle.message("settings.line-selection.caret-adjustment.to-anchor")),
    TO_ANTI_ANCHOR(4, Bundle.message("settings.line-selection.caret-adjustment.to-anti-anchor")),;

    public final String displayName;
    public final int intValue;

    CaretAdjustmentType(int intValue, String displayName) {
        this.intValue = intValue;
        this.displayName = displayName;
    }

    public boolean isEnabled(int lineCount) {
        return lineCount >= intValue;
    }

    public static final CaretAdjustmentType DEFAULT = NONE;
    public static final ComboBoxAdapter<CaretAdjustmentType> ADAPTER = new ComboBoxAdapterImpl<>(DEFAULT);

    public static CaretAdjustmentType get(JComboBox comboBox) {
        return ADAPTER.findEnum((String) comboBox.getSelectedItem());
    }

    public static CaretAdjustmentType get(int value) {
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

    public static CaretAdjustmentType findEnum(int intValue) { return ADAPTER.findEnum(intValue); }

    public static boolean onFirst(int intValue, OnMap map) { return ComboBoxAdapter.onFirst(ADAPTER, intValue, map); }

    public static boolean onAll(int intValue, OnMap map) { return ComboBoxAdapter.onAll(ADAPTER, intValue, map); }

    public CaretAdjustmentType findEnum(String displayName) { return ADAPTER.findEnum(displayName); }

    @Override
    public CaretAdjustmentType[] getEnumValues() {
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
