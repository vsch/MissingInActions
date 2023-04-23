// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.util;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

class ItemTextRange<T> extends TextRange {
    private T myItem;

    public ItemTextRange(final T item, final @NotNull TextRange other) {
        super(other.getStartOffset(), other.getEndOffset());
        myItem = item;
    }

    public ItemTextRange(final T item, final int startOffset, final int endOffset) {
        super(startOffset, endOffset);
        myItem = item;
    }

    public ItemTextRange(final T item, final int startOffset, final int endOffset, final boolean checkForProperTextRange) {
        super(startOffset, endOffset, checkForProperTextRange);
        myItem = item;
    }

    public T getItem() {
        return myItem;
    }
}
