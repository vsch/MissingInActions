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
import com.vladsch.MissingInActions.manager.EditorPosition;
import com.vladsch.MissingInActions.util.ui.ComboBoxAdaptable;
import com.vladsch.MissingInActions.util.ui.ComboBoxAdapter;
import com.vladsch.MissingInActions.util.ui.ComboBoxAdapterImpl;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.regex.Pattern;

public enum RemovePrefixOnPasteType implements ComboBoxAdaptable<RemovePrefixOnPasteType> {
    ANY(0, Bundle.message("settings.remove-prefix-on-paste-type.any")),
    CAMEL(1, Bundle.message("settings.line-settings.remove-prefix-on-paste-type.camel")),
    REGEX(2, Bundle.message("settings.remove-prefix-on-paste-type.regex"));

    public final String displayName;
    public final int intValue;

    RemovePrefixOnPasteType(int intValue, String displayName) {
        this.intValue = intValue;
        this.displayName = displayName;
    }

    public boolean isEnabled(int lineCount) {
        return lineCount >= intValue;
    }

    /**
     * Convert to caret position for paste depending on where the caret is relative
     * to indent column and setting
     *
     * @param text   text which is to match to the prefix
     * @param prefix prefix pattern
     * @return true if text has matching prefix
     */
    public boolean isMatched(@NotNull String text, @NotNull String prefix) {
        if (this == ANY) return text.startsWith(prefix) && text.length() > prefix.length();
        if (this == CAMEL) return text.startsWith(prefix) && text.length() > prefix.length() && Character.isLowerCase(text.charAt(prefix.length() - 1));
        if (this == REGEX) {
            try {
                return text.matches(prefix);
            } catch (Throwable ignored) {

            }
        }
        return false;
    }

    public static final RemovePrefixOnPasteType DEFAULT = CAMEL;
    public static final ComboBoxAdapter<RemovePrefixOnPasteType> ADAPTER = new ComboBoxAdapterImpl<>(DEFAULT);

    public static void fillComboBox(JComboBox comboBox) { ADAPTER.fillComboBox(comboBox); }

    public static RemovePrefixOnPasteType findEnum(int intValue) { return ADAPTER.findEnum(intValue); }

    public RemovePrefixOnPasteType findEnum(String displayName) { return ADAPTER.findEnum(displayName); }

    @Override
    public RemovePrefixOnPasteType[] getEnumValues() {
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
