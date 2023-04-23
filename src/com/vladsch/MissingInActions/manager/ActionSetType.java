// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.manager;

public enum ActionSetType {
    PASTE_ACTION,
    MOVE_LINE_UP_AUTO_INDENT_TRIGGER,
    MOVE_LINE_DOWN_AUTO_INDENT_TRIGGER,
    SELECTING_ACTION, CUT_ACTION, DUPLICATE_ACTION,

    // used for selection stash
    SELECTION_STASH_ACTIONS,

    // used for selection stash
    SELECTION_ALWAYS_STASH,

    // used for search/filter multi-caret
    MOVE_SEARCH_CARET_ACTION,

    // do nothing about carets for these
    SEARCH_AWARE_CARET_ACTION,
}
