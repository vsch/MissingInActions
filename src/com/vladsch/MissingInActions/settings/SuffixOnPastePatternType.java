/*
 * Copyright (c) 2016-2018 Vladimir Schneider <vladimir.schneider@gmail.com>
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
import com.vladsch.plugin.util.ui.ComboBoxAdaptable;
import com.vladsch.plugin.util.ui.ComboBoxAdapter;
import com.vladsch.plugin.util.ui.ComboBoxAdapterImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum SuffixOnPastePatternType implements ComboBoxAdaptable<SuffixOnPastePatternType> {
    ANY(0, Bundle.message("settings.ignore-suffix-on-paste-type.any")),
    REGEX(2, Bundle.message("settings.ignore-suffix-on-paste-type.regex"));


    /**
     * Convert to caret position for paste depending on where the caret is relative
     * to indent column and setting
     *
     * @param text     text which is to match to the prefix
     * @param suffixes suffix patterns, if regex then only the first entry is used an it is the match pattern
     * @return matched suffix or empty string if text does not match prefix
     */
    public String getMatched(@NotNull String text, @Nullable String[] suffixes) {
        if (suffixes == null || suffixes.length == 0) return "";

        if (this == ANY) {
            for (String suffix : suffixes) {
                if (text.endsWith(suffix) && text.length() > suffix.length()) return suffix;
            }
        } else if (this == REGEX) {
            try {
                Pattern pattern;
                Matcher matcher;

                pattern = Pattern.compile(suffixes[0]);
                matcher = pattern.matcher(text);
                if (matcher.find() && matcher.end() == text.length()) return matcher.group();
            } catch (Throwable ignored) {

            }
        }
        return "";
    }

    public final int intValue;
    public final @NotNull String displayName;

    SuffixOnPastePatternType(int intValue, @NotNull String displayName) {
        this.intValue = intValue;
        this.displayName = displayName;
    }

    public static Static<SuffixOnPastePatternType> ADAPTER = new Static<>(new ComboBoxAdapterImpl<>(ANY));

    @NotNull
    @Override
    public ComboBoxAdapter<SuffixOnPastePatternType> getAdapter() {
        return ADAPTER;
    }

    @Override
    public int getIntValue() { return intValue; }

    @NotNull
    public String getDisplayName() { return displayName; }

    @NotNull
    public SuffixOnPastePatternType[] getValues() { return values(); }
}
