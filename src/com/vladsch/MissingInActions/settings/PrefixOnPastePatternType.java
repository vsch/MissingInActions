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

public enum PrefixOnPastePatternType implements ComboBoxAdaptable<PrefixOnPastePatternType> {
    ANY(0, Bundle.message("settings.remove-prefix-on-paste-type.any")),
    CAMEL(1, Bundle.message("settings.remove-prefix-on-paste-type.camel")),
    REGEX(2, Bundle.message("settings.remove-prefix-on-paste-type.regex"));

    ///**
    // * Convert to caret position for paste depending on where the caret is relative
    // * to indent column and setting
    // *
    // * @param text   text which is to match to the prefix
    // * @param prefix prefix pattern
    // * @return true if text has matching prefix
    // */
    //public boolean isMatched(@NotNull String text, @NotNull String prefix) {
    //    if (this == ANY) return text.startsWith(prefix) && text.length() > prefix.length();
    //    if (this == CAMEL) return text.startsWith(prefix) && text.length() > prefix.length() && Character.isLowerCase(text.charAt(prefix.length() - 1));
    //    if (this == REGEX) {
    //        try {
    //            Pattern pattern;
    //            Matcher matcher;
    //
    //            pattern = Pattern.compile(prefix);
    //            matcher = pattern.matcher(text);
    //            return matcher.find() && matcher.start() == 0;
    //        } catch (Throwable ignored) {
    //
    //        }
    //    }
    //    return false;
    //}

    /**
     * Convert to caret position for paste depending on where the caret is relative
     * to indent column and setting
     *
     * @param text     text which is to match to the prefix
     * @param prefixes prefix patterns, if regex then only the first entry is used an it is the match pattern
     * @return matched prefix or empty string if text does not match prefix
     */
    public String getMatched(@NotNull String text, @Nullable String[] prefixes) {
        if (prefixes == null || prefixes.length == 0) return "";

        if (this == ANY) {
            for (String prefix : prefixes) {
                if (text.startsWith(prefix) && text.length() > prefix.length()) return prefix;
            }
        } else if (this == CAMEL) {
            for (String prefix : prefixes) {
                if (text.startsWith(prefix) && text.length() > prefix.length() && Character.isUpperCase(text.charAt(prefix.length()))) return prefix;
            }
        } else if (this == REGEX) {
            try {
                Pattern pattern;
                Matcher matcher;

                pattern = Pattern.compile(prefixes[0]);
                matcher = pattern.matcher(text);
                if (matcher.find() && matcher.start() == 0) return matcher.group();

                //pattern = Pattern.compile(prefix2);
                //matcher = pattern.matcher(text);
                //if (matcher.find() && matcher.start() == 0) return matcher.group();
            } catch (Throwable ignored) {

            }
        }
        return "";
    }

    public final int intValue;
    public final @NotNull String displayName;

    PrefixOnPastePatternType(int intValue, @NotNull String displayName) {
        this.intValue = intValue;
        this.displayName = displayName;
    }

    public static Static<PrefixOnPastePatternType> ADAPTER = new Static<>(new ComboBoxAdapterImpl<>(CAMEL));

    @NotNull
    @Override
    public ComboBoxAdapter<PrefixOnPastePatternType> getAdapter() {
        return ADAPTER;
    }

    @Override
    public int getIntValue() { return intValue; }

    @NotNull
    public String getDisplayName() { return displayName; }

    @NotNull
    public PrefixOnPastePatternType[] getValues() { return values(); }
}
