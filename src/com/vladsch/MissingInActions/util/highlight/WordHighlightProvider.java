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
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.Map;
import java.util.regex.Pattern;

public interface WordHighlightProvider extends HighlightProvider {
    int BEGIN_WORD = 1;
    int END_WORD = 2;
    int CASE_INSENSITIVE = 4;
    int CASE_SENSITIVE = 8;

    default int encodeFlags(boolean beginWord, boolean endWord, Boolean caseSensitive) {
        return (beginWord ? BEGIN_WORD : 0) | (endWord ? END_WORD : 0) | (caseSensitive == null ? 0 : caseSensitive ? CASE_SENSITIVE : CASE_INSENSITIVE);
    }

    boolean isWordHighlighted(CharSequence word);
    @Nullable Map<String, Integer> getHighlightWords();
    @Nullable Map<String, Integer> getHighlightWordIndices();
    @Nullable Map<String, Integer> getHighlightCaseInsensitiveWordIndices();
    Pattern getHighlightPattern();
    int getHighlightWordIndex(final String word);
    boolean isHighlightWordsCaseSensitive();
    void setHighlightWordsCaseSensitive(boolean highlightWordsCaseSensitive);
    void addHighlightWord(CharSequence word, boolean beginWord, boolean endWord, Boolean caseSensitive);
    void addHighlightWord(CharSequence word, int flags);
    void removeHighlightWord(CharSequence word);
    void updateHighlightPattern();
    WordHighlighter getHighlighter(@NotNull Editor editor);
}
