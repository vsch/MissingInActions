// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.settings;

import com.vladsch.MissingInActions.Bundle;
import com.vladsch.plugin.util.ui.ComboBoxAdaptable;
import com.vladsch.plugin.util.ui.ComboBoxAdapter;
import com.vladsch.plugin.util.ui.ComboBoxAdapterImpl;
import org.jetbrains.annotations.NotNull;

public enum CaretThicknessType implements ComboBoxAdaptable<CaretThicknessType> {
    NORMAL(0, Bundle.message("settings.caret-thickness.normal")),
    THIN(1, Bundle.message("settings.caret-thickness.thin")),
    HEAVY(2, Bundle.message("settings.caret-thickness.heavy")),
    ;

    public final int intValue;
    public final @NotNull String displayName;

    CaretThicknessType(int intValue, @NotNull String displayName) {
        this.intValue = intValue;
        this.displayName = displayName;
    }

    public static Static<CaretThicknessType> ADAPTER = new Static<>(new ComboBoxAdapterImpl<>(NORMAL));

    @NotNull
    @Override
    public ComboBoxAdapter<CaretThicknessType> getAdapter() {
        return ADAPTER;
    }

    @Override
    public int getIntValue() { return intValue; }

    @NotNull
    public String getDisplayName() { return displayName; }

    @NotNull
    public CaretThicknessType[] getValues() { return values(); }
}
