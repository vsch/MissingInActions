/*
 * Copyright (c) 2016-2019 Vladimir Schneider <vladimir.schneider@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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
