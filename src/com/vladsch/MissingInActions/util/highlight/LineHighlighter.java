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

public class LineHighlighter extends Highlighter {
    public LineHighlighter(@NotNull LineHighlightProvider highlightProvider, @NotNull final Editor editor) {
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
        LineHighlightProvider highlightProvider = (LineHighlightProvider) myHighlightProvider;
        if (highlightProvider.isShowHighlights()) {
            removeHighlightsRaw();
            Document document = myEditor.getDocument();
            MarkupModel markupModel = myEditor.getMarkupModel();
            int iMax = document.getLineCount();
            for (int i = 0; i < iMax; i++) {
                // create a highlighter
                int startOffset = document.getLineStartOffset(i);
                int endOffset = document.getLineEndOffset(i);
                int index = highlightProvider.getHighlightLineIndex(i);
                TextAttributes attributes = highlightProvider.getHighlightAttributes(index, startOffset, endOffset, null, null, null, 0);
                attributes = getAttributes(attributes, i, startOffset, endOffset);

                if (attributes != null) {
                    RangeHighlighter rangeHighlighter = markupModel.addRangeHighlighter(startOffset, endOffset, HighlighterLayer.SELECTION - 2, attributes, HighlighterTargetArea.LINES_IN_RANGE);
                    rangeHighlighter = rangeHighlighterCreated(rangeHighlighter, i, index, startOffset, endOffset);
                    if (myHighlighters == null) {
                        myHighlighters = new ArrayList<>();
                    }

                    myHighlighters.add(rangeHighlighter);
                }
            }

            myHighlightProvider.fireHighlightsUpdated();
        } else {
            removeHighlights();
        }
    }
}
