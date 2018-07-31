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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordHighlighter extends Highlighter {
    private int[] myIndexedWordCount = null;

    public WordHighlighter(@NotNull WordHighlightProvider highlightProvider, @NotNull final Editor editor) {
        super(highlightProvider, editor);
    }

    // Override to customize
    public TextAttributes getAttributes(@Nullable TextAttributes attributes, final String word, final int startOffset, int endOffset) {
        return attributes;
    }

    public RangeHighlighter rangeHighlighterCreated(RangeHighlighter rangeHighlighter, final String word, final int index, final int startOffset, int endOffset) {
        return rangeHighlighter;
    }

    public void updateHighlights() {
        boolean handled = false;

        WordHighlightProvider highlightProvider = (WordHighlightProvider) myHighlightProvider;
        if (highlightProvider.isShowHighlights()) {
            Pattern pattern = highlightProvider.getHighlightPattern();
            Map<String, Integer> highlightWordFlags = highlightProvider.getHighlightWordFlags();
            if (pattern != null && highlightWordFlags != null) {
                handled = true;
                removeHighlightsRaw();
                Document document = myEditor.getDocument();
                MarkupModel markupModel = myEditor.getMarkupModel();
                Matcher matcher = pattern.matcher(document.getCharsSequence());
                myIndexedWordCount = new int[highlightProvider.getMaxHighlightWordIndex()];

                while (matcher.find()) {
                    // create a highlighter
                    String group = matcher.group();
                    int startOffset = matcher.start();
                    int endOffset = matcher.end();
                    int flags = highlightWordFlags.get(group);
                    int index = highlightProvider.getHighlightWordIndex(group);
                    TextAttributes attributes = highlightProvider.getHighlightAttributes(index, flags, startOffset, endOffset, null, null, EffectType.BOLD_DOTTED_LINE, 0);
                    attributes = getAttributes(attributes, group, startOffset, endOffset);
                    if (attributes != null) {
                        RangeHighlighter rangeHighlighter = markupModel.addRangeHighlighter(startOffset, endOffset, HighlighterLayer.SELECTION - 2, attributes, HighlighterTargetArea.EXACT_RANGE);
                        rangeHighlighter = rangeHighlighterCreated(rangeHighlighter, group, index, startOffset, endOffset);
                        if (myHighlighters == null || myHighlighterIndexList == null) {
                            myHighlighters = new ArrayList<>();
                            myHighlighterIndexList = new ArrayList<>();
                        }

                        myHighlighterIndexList.add(index);
                        myIndexedWordCount[index]++;
                        myHighlighters.add(rangeHighlighter);
                    }
                }

                myHighlightProvider.fireHighlightsUpdated();
            }
        }

        if (!handled) {
            removeHighlightsRaw();
        }
    }

    public int getIndexedWordCount(int wordIndex) {
        if (myIndexedWordCount != null && wordIndex >= 0 && wordIndex < myIndexedWordCount.length) {
            return myIndexedWordCount[wordIndex];
        }
        return 0;
    }

    public int[] getIndexedWordCounts() {
        return myIndexedWordCount;
    }
}
