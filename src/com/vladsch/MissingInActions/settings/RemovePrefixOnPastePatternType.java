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
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum RemovePrefixOnPastePatternType implements ComboBoxAdaptable<RemovePrefixOnPastePatternType> {
    ANY(0, Bundle.message("settings.remove-prefix-on-paste-type.any")),
    CAMEL(1, Bundle.message("settings.line-settings.remove-prefix-on-paste-type.camel")),
    REGEX(2, Bundle.message("settings.remove-prefix-on-paste-type.regex"));

    public final String displayName;
    public final int intValue;

    RemovePrefixOnPastePatternType(int intValue, String displayName) {
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
                Pattern pattern;
                Matcher matcher;

                pattern = Pattern.compile(prefix);
                matcher = pattern.matcher(text);
                return matcher.find() && matcher.start() == 0;
            } catch (Throwable ignored) {

            }
        }
        return false;
    }

    /**
     * Convert to caret position for paste depending on where the caret is relative
     * to indent column and setting
     *
     * @param text    text which is to match to the prefix
     * @param prefix1 prefix pattern, if regex this is the match pattern
     * @param prefix2 prefix pattern. if regex this is the replace pattern to extract the prefix
     * @return matched prefix or empty string if text does not match prefix
     */
    public String getMatched(@NotNull String text, @NotNull String prefix1, @NotNull String prefix2) {
        if (this == ANY) {
            if (text.startsWith(prefix1) && text.length() > prefix1.length()) return prefix1;
            if (text.startsWith(prefix2) && text.length() > prefix2.length()) return prefix2;
        } else if (this == CAMEL) {
            if (text.startsWith(prefix1) && text.length() > prefix1.length() && Character.isLowerCase(text.charAt(prefix1.length() - 1))) return prefix1;
            if (text.startsWith(prefix2) && text.length() > prefix2.length() && Character.isLowerCase(text.charAt(prefix2.length() - 1))) return prefix2;
        }
        if (this == REGEX) {
            try {
                Pattern pattern;
                Matcher matcher;

                pattern = Pattern.compile(prefix1);
                matcher = pattern.matcher(text);
                if (matcher.find() && matcher.start() == 0) return matcher.group();

                pattern = Pattern.compile(prefix2);
                matcher = pattern.matcher(text);
                if (matcher.find() && matcher.start() == 0) return matcher.group();
            } catch (Throwable ignored) {

            }
        }
        return "";
    }

    public static final RemovePrefixOnPastePatternType DEFAULT = CAMEL;
    public static final ComboBoxAdapter<RemovePrefixOnPastePatternType> ADAPTER = new ComboBoxAdapterImpl<>(DEFAULT);

    public static RemovePrefixOnPastePatternType get(JComboBox comboBox) {
        return ADAPTER.findEnum((String) comboBox.getSelectedItem());
    }

    public static RemovePrefixOnPastePatternType get(int value) {
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

    public static RemovePrefixOnPastePatternType findEnum(int intValue) { return ADAPTER.findEnum(intValue); }

    public RemovePrefixOnPastePatternType findEnum(String displayName) { return ADAPTER.findEnum(displayName); }

    @Override
    public RemovePrefixOnPastePatternType[] getEnumValues() {
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
