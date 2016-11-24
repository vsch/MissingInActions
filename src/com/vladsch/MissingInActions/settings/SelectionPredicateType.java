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

public enum SelectionPredicateType implements ComboBoxAdaptable<SelectionPredicateType> {
    WHEN_HAS_ANY(0, Bundle.message("settings.selection-predicate.always")),
    WHEN_HAS_1_PLUS_LINES(1, Bundle.message("settings.selection-predicate.has-lines")),
    WHEN_HAS_2_PLUS_LINES(2, Bundle.message("settings.selection-predicate.has-2-plus-lines")),
    WHEN_HAS_3_PLUS_LINES(2, Bundle.message("settings.selection-predicate.has-3-plus-lines")),
    WHEN_HAS_4_PLUS_LINES(2, Bundle.message("settings.selection-predicate.has-4-plus-lines")),
    WHEN_HAS_5_PLUS_LINES(2, Bundle.message("settings.selection-predicate.has-5-plus-lines"));

    public final String displayName;
    public final int intValue;

    SelectionPredicateType(int intValue, String displayName) {
        this.intValue = intValue;
        this.displayName = displayName;
    }

    public boolean isEnabled(int lineCount) {
        return lineCount >= intValue;
    }

    public static final SelectionPredicateType DEFAULT = WHEN_HAS_1_PLUS_LINES;
    public static final ComboBoxAdapter<SelectionPredicateType> ADAPTER = new ComboBoxAdapterImpl<>(DEFAULT);

    public static void fillComboBox(JComboBox comboBox) { ADAPTER.fillComboBox(comboBox); }

    public static SelectionPredicateType findEnum(int intValue) { return ADAPTER.findEnum(intValue); }

    public SelectionPredicateType findEnum(String displayName) { return ADAPTER.findEnum(displayName); }

    @Override
    public SelectionPredicateType[] getEnumValues() {
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
