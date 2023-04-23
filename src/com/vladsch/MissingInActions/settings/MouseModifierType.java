// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.settings;

import com.vladsch.MissingInActions.Bundle;
import com.vladsch.plugin.util.ui.ComboBoxAdaptable;
import com.vladsch.plugin.util.ui.ComboBoxAdapter;
import com.vladsch.plugin.util.ui.ComboBoxAdapterImpl;
import org.jetbrains.annotations.NotNull;

public enum MouseModifierType implements ComboBoxAdaptable<MouseModifierType> {
    CTRL_CHAR(0, Bundle.message("settings.auto-line.modifier.ctrl-chars")),
    CTRL_LINE(1, Bundle.message("settings.auto-line.modifier.ctrl-line"));

    public final int intValue;
    public final @NotNull String displayName;

    MouseModifierType(int intValue, @NotNull String displayName) {
        this.intValue = intValue;
        this.displayName = displayName;
    }

    public static Static<MouseModifierType> ADAPTER = new Static<>(new ComboBoxAdapterImpl<>(CTRL_CHAR));

    @NotNull
    @Override
    public ComboBoxAdapter<MouseModifierType> getAdapter() {
        return ADAPTER;
    }

    @Override
    public int getIntValue() { return intValue; }

    @NotNull
    public String getDisplayName() { return displayName; }

    @NotNull
    public MouseModifierType[] getValues() { return values(); }
}
