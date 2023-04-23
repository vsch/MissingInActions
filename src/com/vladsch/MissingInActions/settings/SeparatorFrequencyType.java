// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.settings;

import com.vladsch.MissingInActions.Bundle;
import com.vladsch.plugin.util.ui.ComboBoxAdaptable;
import com.vladsch.plugin.util.ui.ComboBoxAdapter;
import com.vladsch.plugin.util.ui.ComboBoxAdapterImpl;
import org.jetbrains.annotations.NotNull;

public enum SeparatorFrequencyType implements ComboBoxAdaptable<SeparatorFrequencyType> {
    NONE(0, Bundle.message("renumber.separator-frequency.none")),
    EVERY_2(2, Bundle.message("renumber.separator-frequency.2")),
    EVERY_3(3, Bundle.message("renumber.separator-frequency.3")),
    EVERY_4(4, Bundle.message("renumber.separator-frequency.4")),
    EVERY_8(8, Bundle.message("renumber.separator-frequency.8")),
    EVERY_12(12, Bundle.message("renumber.separator-frequency.12")),
    EVERY_16(16, Bundle.message("renumber.separator-frequency.16"));

    public final int intValue;
    public final @NotNull String displayName;

    SeparatorFrequencyType(int intValue, @NotNull String displayName) {
        this.intValue = intValue;
        this.displayName = displayName;
    }

    public static Static<SeparatorFrequencyType> ADAPTER = new Static<>(new ComboBoxAdapterImpl<>(EVERY_3));

    @NotNull
    @Override
    public ComboBoxAdapter<SeparatorFrequencyType> getAdapter() {
        return ADAPTER;
    }

    @Override
    public int getIntValue() { return intValue; }

    @NotNull
    public String getDisplayName() { return displayName; }

    @NotNull
    public SeparatorFrequencyType[] getValues() { return values(); }

    @Override
    public boolean isDefault() {
        return this == ADAPTER.getDefault();
    }
}
