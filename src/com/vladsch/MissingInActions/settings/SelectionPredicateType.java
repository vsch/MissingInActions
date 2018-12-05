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

public enum SelectionPredicateType implements ComboBoxAdaptable<SelectionPredicateType> {
    WHEN_HAS_ANY(0, Bundle.message("settings.selection-predicate.always")),
    WHEN_CHAR_ONLY(-1, Bundle.message("settings.selection-predicate.char-only")),
    WHEN_HAS_1_PLUS_LINES(1, Bundle.message("settings.selection-predicate.has-lines")),
    WHEN_HAS_2_PLUS_LINES(2, Bundle.message("settings.selection-predicate.has-2-plus-lines")),
    WHEN_HAS_3_PLUS_LINES(3, Bundle.message("settings.selection-predicate.has-3-plus-lines")),
    WHEN_HAS_4_PLUS_LINES(4, Bundle.message("settings.selection-predicate.has-4-plus-lines")),
    WHEN_HAS_5_PLUS_LINES(5, Bundle.message("settings.selection-predicate.has-5-plus-lines"));

    public boolean isEnabled(int lineCount) {
        return lineCount >= intValue || lineCount == 0 && intValue == -1;
    }

    public static boolean isEnabled(int value, int lineCount) {
        return ADAPTER.findEnum(value).isEnabled(lineCount);
    }

    public final int intValue;
    public final @NotNull String displayName;

    SelectionPredicateType(int intValue, @NotNull String displayName) {
        this.intValue = intValue;
        this.displayName = displayName;
    }

    public static Static<SelectionPredicateType> ADAPTER = new Static<>(new ComboBoxAdapterImpl<>(WHEN_HAS_1_PLUS_LINES));

    @Override
    public ComboBoxAdapter<SelectionPredicateType> getAdapter() {
        return ADAPTER;
    }

    @Override
    public int getIntValue() { return intValue; }

    @NotNull
    public String getDisplayName() { return displayName; }

    @NotNull
    public SelectionPredicateType[] getValues() { return values(); }
}
