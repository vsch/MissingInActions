// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class HighlightElement {
    final int textIndex;
    final String text;
    private final ArrayList<HighlightRange> rangeList = new ArrayList<>();
    public int endOffset;

    public HighlightElement(final int textIndex, final String text) {
        this(textIndex, text, 0);
    }

    public HighlightElement(final int textIndex, final String text, final int endOffset) {
        this.textIndex = textIndex;
        this.text = text;
        this.endOffset = endOffset;
    }

    public void addRange(@NotNull TextRange range, @Nullable String text) {
        rangeList.add(new HighlightRange(range, text));
    }

    public HighlightRange getRange(int index) {
        HighlightRange range = rangeList.get(index);
        return new HighlightRange(range.range.shiftRight(endOffset),range.foundText);
    }

    public ArrayList<HighlightRange> getRangeList() {
        if (endOffset == 0) return rangeList;

        ArrayList<HighlightRange> list = new ArrayList<>();
        for (HighlightRange range : rangeList) {
            list.add(new HighlightRange(range.range.shiftRight(endOffset), range.foundText));
        }
        return list;
    }

    public static class HighlightRange {
        public final @NotNull TextRange range;
        public final @Nullable String foundText;

        public HighlightRange(@NotNull final TextRange range, @Nullable final String foundText) {
            this.range = range;
            this.foundText = foundText;
        }
    }
}
