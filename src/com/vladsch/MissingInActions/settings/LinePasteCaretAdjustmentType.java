// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.settings;

import com.vladsch.MissingInActions.Bundle;
import com.vladsch.MissingInActions.manager.EditorPosition;
import com.vladsch.plugin.util.ui.ComboBoxAdaptable;
import com.vladsch.plugin.util.ui.ComboBoxAdapter;
import com.vladsch.plugin.util.ui.ComboBoxAdapterImpl;
import org.jetbrains.annotations.NotNull;

public enum LinePasteCaretAdjustmentType implements ComboBoxAdaptable<LinePasteCaretAdjustmentType> {
    NONE(0, Bundle.message("settings.line-paste-adjust.none")),
    ABOVE(1, Bundle.message("settings.line-paste-adjust.above")),
    INDENT_ABOVE(2, Bundle.message("settings.line-paste-adjust.at-indent.above")),
    BELOW(3, Bundle.message("settings.line-paste-adjust.below"));

    /**
     * Convert to caret position for paste depending on where the caret is relative
     * to indent column and setting
     *
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

    public final int intValue;
    public final @NotNull String displayName;

    LinePasteCaretAdjustmentType(int intValue, @NotNull String displayName) {
        this.intValue = intValue;
        this.displayName = displayName;
    }

    public static Static<LinePasteCaretAdjustmentType> ADAPTER = new Static<>(new ComboBoxAdapterImpl<>(NONE));

    @NotNull
    @Override
    public ComboBoxAdapter<LinePasteCaretAdjustmentType> getAdapter() {
        return ADAPTER;
    }

    @Override
    public int getIntValue() { return intValue; }

    @NotNull
    public String getDisplayName() { return displayName; }

    @NotNull
    public LinePasteCaretAdjustmentType[] getValues() { return values(); }
}
