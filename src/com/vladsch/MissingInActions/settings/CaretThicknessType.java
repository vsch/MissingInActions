/*
 * Copyright (c) 2016-2017 Vladimir Schneider <vladimir.schneider@gmail.com>
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

public enum CaretThicknessType implements ComboBoxAdaptable<CaretThicknessType> {
    NORMAL(0, Bundle.message("settings.caret-thickness.normal")),
    THIN(1, Bundle.message("settings.caret-thickness.thin")),
    HEAVY(2, Bundle.message("settings.caret-thickness.heavy")),;

    public final int intValue;
    public final @NotNull String displayName;

    CaretThicknessType(int intValue, @NotNull String displayName) {
        this.intValue = intValue;
        this.displayName = displayName;
    }

    public static Static<CaretThicknessType> ADAPTER = new Static<>(new ComboBoxAdapterImpl<>(NORMAL));

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
