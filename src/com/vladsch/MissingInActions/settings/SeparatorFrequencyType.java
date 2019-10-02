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
