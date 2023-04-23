// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.manager;

public enum PreserveColumn {
    PRESERVE_COLUMN(1),
    INDENT_RELATIVE(2),
    WITH_SELECTION(4),
    WITH_LINES(8),
    WITH_LINE_SELECTION(16);

    final int flags;

    public static boolean has(int flags, PreserveColumn option) {
        return (flags & option.flags) != 0;
    }

    public static int getFlags(PreserveColumn... options) {
        int flags = 0;
        for (PreserveColumn option : options) {
            flags |= option.flags;
        }
        return flags;
    }

    PreserveColumn(int flags) {
        this.flags = flags;
    }
}
