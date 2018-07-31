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

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.regex.Pattern;

public interface WordHighlightProvider extends HighlightProvider {
    int BEGIN_WORD = 1;
    int END_WORD = 2;
    int CASE_INSENSITIVE = 4;
    int CASE_SENSITIVE = 8;
    int IDE_WARNING = 16;  // marks this highlight as using standard ide warning highlight
    int IDE_ERROR = 32;  // marks this highlight as using standard ide error highlight
    int IDE_HIGHLIGHT = IDE_WARNING | IDE_ERROR;  // marks this highlight as using standard ide highlights

    TextAttributesKey TYPO_ATTRIBUTES_KEY = TextAttributesKey.createTextAttributesKey("TYPO");
    TextAttributesKey ERROR_ATTRIBUTES_KEY = CodeInsightColors.MARKED_FOR_REMOVAL_ATTRIBUTES;
    TextAttributesKey WARNING_ATTRIBUTES_KEY = TYPO_ATTRIBUTES_KEY;// CodeInsightColors.WEAK_WARNING_ATTRIBUTES;

    default int encodeFlags(boolean beginWord, boolean endWord, final boolean ideWarning, final boolean ideError, Boolean caseSensitive) {
        //noinspection PointlessBitwiseExpression
        return 0
                | (beginWord ? BEGIN_WORD : 0)
                | (endWord ? END_WORD : 0)
                | (ideError ? IDE_ERROR : (ideWarning ? IDE_WARNING : 0))
                | (caseSensitive == null ? 0 : caseSensitive ? CASE_SENSITIVE : CASE_INSENSITIVE);
    }

    boolean isWordHighlighted(CharSequence word);
    @Nullable Map<String, Integer> getHighlightWordFlags();
    @Nullable Map<String, Integer> getHighlightWordIndices();
    @Nullable Map<String, Integer> getHighlightCaseInsensitiveWordIndices();
    Pattern getHighlightPattern();
    int getMaxHighlightWordIndex();
    int getHighlightWordIndex(final String word);
    boolean isHighlightWordsCaseSensitive();
    void setHighlightWordsCaseSensitive(boolean highlightWordsCaseSensitive);
    void addHighlightWord(CharSequence word, boolean beginWord, boolean endWord, final boolean ideWarning, final boolean ideError, Boolean caseSensitive);
    void addHighlightWord(CharSequence word, int flags);
    void removeHighlightWord(CharSequence word);
    void updateHighlightPattern();
    WordHighlighter getHighlighter(@NotNull Editor editor);
}
