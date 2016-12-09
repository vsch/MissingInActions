/*
 * Copyright (c) 2016-2016 Vladimir Schneider <vladimir.schneider@gmail.com>
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
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.MissingInActions.manager.EditorPosition;
import com.vladsch.MissingInActions.util.ui.ComboBoxAdaptable;
import com.vladsch.MissingInActions.util.ui.ComboBoxAdapter;
import com.vladsch.MissingInActions.util.ui.ComboBoxAdapterImpl;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public enum LinePasteCaretAdjustmentType implements ComboBoxAdaptable<LinePasteCaretAdjustmentType> {
    NONE(0, Bundle.message("settings.line-paste-adjust.none")),
    ABOVE(1, Bundle.message("settings.line-paste-adjust.above")),
    INDENT_ABOVE(2, Bundle.message("settings.line-paste-adjust.at-indent.above")),
    BELOW(3, Bundle.message("settings.line-paste-adjust.below"));

    public final String displayName;
    public final int intValue;

    LinePasteCaretAdjustmentType(int intValue, String displayName) {
        this.intValue = intValue;
        this.displayName = displayName;
    }

    public boolean isEnabled(int lineCount) {
        return lineCount >= intValue;
    }

    /**
     * Convert to caret position for paste depending on where the caret is relative
     * to indent column and setting
     * @param position caret position
     * @return new caret position, may be the same as input
     */
    @NotNull
    public EditorPosition getPastePosition(@NotNull EditorPosition position) {
        if (this == INDENT_ABOVE) {
            if (position.column <= position.getIndentColumn()) return position.atIndentColumn();
            else return position.atStartOfNextLine().atIndentColumn();
        }
        if (this == ABOVE) return position.atIndentColumn();
        if (this == BELOW) return position.atStartOfNextLine().atIndentColumn();
        return position;
    }

    public static final LinePasteCaretAdjustmentType DEFAULT = NONE;
    public static final ComboBoxAdapter<LinePasteCaretAdjustmentType> ADAPTER = new ComboBoxAdapterImpl<>(DEFAULT);

    public static void fillComboBox(JComboBox comboBox) { ADAPTER.fillComboBox(comboBox); }

    public static LinePasteCaretAdjustmentType findEnum(int intValue) { return ADAPTER.findEnum(intValue); }

    public LinePasteCaretAdjustmentType findEnum(String displayName) { return ADAPTER.findEnum(displayName); }

    @Override
    public LinePasteCaretAdjustmentType[] getEnumValues() {
        return values();
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isDefault() {
        return this == DEFAULT;
    }

    public int getIntValue() {
        return intValue;
    }
}
