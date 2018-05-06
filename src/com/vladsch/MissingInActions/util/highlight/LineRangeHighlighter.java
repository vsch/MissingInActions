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

package com.vladsch.MissingInActions.util.highlight;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;

public class LineRangeHighlighter extends LineHighlighter {
    public LineRangeHighlighter(@NotNull LineRangeHighlightProvider highlightProvider, @NotNull final Editor editor) {
        super(highlightProvider, editor);
    }

    // Override to customize
    public TextAttributes getAttributes(@Nullable TextAttributes attributes, final int line, final int startOffset, int endOffset) {
        return attributes;
    }

    @Override
    public int getOriginalIndex(final RangeHighlighter rangeHighlighter) {
        return getIndex(rangeHighlighter);
    }

    public RangeHighlighter rangeHighlighterCreated(RangeHighlighter rangeHighlighter, final int line, final int index, final int startOffset, int endOffset) {
        return rangeHighlighter;
    }

    public void updateHighlights() {
        LineRangeHighlightProvider highlightProvider = (LineRangeHighlightProvider) myHighlightProvider;
        if (highlightProvider.isShowHighlights() && highlightProvider.getHighlightLines() != null) {
            removeHighlightsRaw();
            Document document = myEditor.getDocument();
            MarkupModel markupModel = myEditor.getMarkupModel();

            int startLine = 0;
            int startOffset = 0;
            int endOffset = document.getTextLength();
            int endLine = document.getLineCount();
            BitSet bitSet = highlightProvider.getHighlightLines();
            boolean isInvertedHighlights = highlightProvider.isInvertedHighlights();

            while (startOffset < endOffset && startLine < endLine) {
                int nextLine = isInvertedHighlights ? bitSet.nextSetBit(startLine) : bitSet.nextClearBit(startLine);
                int firstOffset = nextLine == -1 ? endOffset : document.getLineStartOffset(nextLine) - 1;

                if (startOffset < firstOffset) {
                    TextAttributes attributes = highlightProvider.getHighlightAttributes(startLine, startOffset, firstOffset, null, null, null, 0);
                    attributes = getAttributes(attributes, startLine, startOffset, firstOffset);

                    if (attributes != null) {
                        RangeHighlighter rangeHighlighter = markupModel.addRangeHighlighter(startOffset, firstOffset, HighlighterLayer.SELECTION - 2, attributes, HighlighterTargetArea.LINES_IN_RANGE);
                        rangeHighlighter = rangeHighlighterCreated(rangeHighlighter, startLine, startLine, startOffset, firstOffset);
                        if (myHighlighters == null) {
                            myHighlighters = new ArrayList<>();
                        }

                        myHighlighters.add(rangeHighlighter);
                    }
                }

                if (nextLine == -1) break;

                int lastLine = isInvertedHighlights ? bitSet.nextClearBit(nextLine) : bitSet.nextSetBit(nextLine);
                if (lastLine == -1 || lastLine >= endLine) break;

                int lastOffset = document.getLineStartOffset(lastLine);

                startLine = lastLine;
                startOffset = lastOffset;
            }

            myHighlightProvider.fireHighlightsUpdated();
        } else {
            removeHighlights();
        }
    }
}
