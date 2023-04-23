// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.settings;

import com.vladsch.MissingInActions.Bundle;
import com.vladsch.plugin.util.ui.ComboBoxAdaptable;
import com.vladsch.plugin.util.ui.ComboBoxAdapter;
import com.vladsch.plugin.util.ui.ComboBoxAdapterImpl;
import org.jetbrains.annotations.NotNull;

public enum CaretAdjustmentType implements ComboBoxAdaptable<CaretAdjustmentType> {
    NONE(0, Bundle.message("settings.line-selection.caret-adjustment.none")),
    TO_START(1, Bundle.message("settings.line-selection.caret-adjustment.to-start")),
    TO_END(2, Bundle.message("settings.line-selection.caret-adjustment.to-end")),
//    TO_ANCHOR(3, Bundle.message("settings.line-selection.caret-adjustment.to-anchor")),
//    TO_ANTI_ANCHOR(4, Bundle.message("settings.line-selection.caret-adjustment.to-anti-anchor")),
    ;

    public final int intValue;
    public final @NotNull String displayName;

    CaretAdjustmentType(int intValue, @NotNull String displayName) {
        this.intValue = intValue;
        this.displayName = displayName;
    }

    public static Static<CaretAdjustmentType> ADAPTER = new Static<>(new ComboBoxAdapterImpl<>(NONE));

    @NotNull
    @Override
    public ComboBoxAdapter<CaretAdjustmentType> getAdapter() {
        return ADAPTER;
    }

    @Override
    public int getIntValue() { return intValue; }

    @NotNull
    public String getDisplayName() { return displayName; }

    @NotNull
    public CaretAdjustmentType[] getValues() { return values(); }
}
