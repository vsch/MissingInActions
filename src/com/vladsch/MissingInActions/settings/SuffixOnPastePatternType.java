// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
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
