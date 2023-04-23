// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.util;

public class CaretOffsets {
    public final int pos;
    public final int start;
    public final int end;

    public CaretOffsets(final int pos, final int start, final int end) {
        this.pos = pos;
        this.start = start;
        this.end = end;
    }
}
