// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.settings;

import com.vladsch.MissingInActions.Bundle;
import com.vladsch.plugin.util.ui.ComboBoxAdaptable;
import com.vladsch.plugin.util.ui.ComboBoxAdapter;
import com.vladsch.plugin.util.ui.ComboBoxAdapterImpl;
import org.jetbrains.annotations.NotNull;

public enum AutoLineModeType implements ComboBoxAdaptable<AutoLineModeType> {
    DISABLED(0, Bundle.message("settings.auto-line.types.disabled")),
    ENABLED(1, Bundle.message("settings.auto-line.types.enabled")),
    EXPERT(2, Bundle.message("settings.auto-line.types.expert"));

    public final int intValue;
    public final @NotNull String displayName;

    AutoLineModeType(int intValue, @NotNull String displayName) {
        this.intValue = intValue;
        this.displayName = displayName;
    }

    public static Static<AutoLineModeType> ADAPTER = new Static<>(new ComboBoxAdapterImpl<>(DISABLED));

    @NotNull
    @Override
    public ComboBoxAdapter<AutoLineModeType> getAdapter() {
        return ADAPTER;
    }

    @Override
    public int getIntValue() { return intValue; }

    @NotNull
    public String getDisplayName() { return displayName; }

    @NotNull
    public AutoLineModeType[] getValues() { return values(); }
}
