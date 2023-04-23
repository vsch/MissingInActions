// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.manager;

public enum AdjustmentType {
    // no adjustment before, no adjustment after
    UNDOE_REDO___NOTHING__NOTHING,

    // if line then remove selection, used for left/right keys or they jump to begining/end of selection
    MOVE_CARET_LEFT_RIGHT___REMOVE_LINE__NOTHING,

    // change line to char selection before, do nothing after
    MOVE_CARET_LEFT_RIGHT_W_SELECTION___TO_CHAR__NOTHING,

    // change line to char selection before and change back to line if spans more than one line
    MOVE_CARET_UP_DOWN_W_SELECTION___TO_CHAR__TO_LINE,

    // change line to char selection before and always change to line after
    INDENT_UNINDENT___TO_CHAR__IF_HAS_LINES_TO_LINE_RESTORE_COLUMN,

    // if selection is line before action, then after action restore caret column to what it was before action
    DELETE_LINE_SELECTION___IF_LINE__RESTORE_COLUMN,

    // nothing before, always restore caret column after
    MOVE_CARET_UP_DOWN___REMOVE_LINE__RESTORE_COLUMN,

    // nothing before, restore caret column after to position relative to end of line
    REFORMAT_CODE___NOTHING__RESTORE_COLUMN_LINE_END_RELATIVE,

    // move caret to start of selection before, restore caret column after if it is at column 0
    PASTE___MOVE_TO_START__RESTORE_IF0_OR_BLANK_BEFORE,

    // if did not have selection before then remove it after, ToggleCase leaves its selection behind, but if
    // there was no selection then no need to leave it selected, the next invocation will affect the same text range. Duh!
    TOGGLE_CASE___IF_NO_SELECTION__REMOVE_SELECTION,

    DUPLICATE__IF_NO_SELECTION__REMOVE_SELECTION___IF_LINE_RESTORE_COLUMN,
    CUT___IF_NO_SELECTION__REMOVE_SELECTION___IF_LINE_RESTORE_COLUMN,

    COPY___IF_NO_SELECTION__TO_LINE_RESTORE_COLUMN,
    JOIN__MOVE_LINES_UP_DOWN___NOTHING__NORMALIZE_CARET_POSITION,

    AUTO_INDENT_LINES, MOVE_CARET_START_END_W_SELECTION___TO_CHAR__TO_LINE_OR_NOTHING,
}
