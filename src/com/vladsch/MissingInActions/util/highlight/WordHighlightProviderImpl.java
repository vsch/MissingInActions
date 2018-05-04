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
import com.intellij.openapi.util.Pair;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class WordHighlightProviderImpl extends HighlightProviderBase implements WordHighlightProvider {
    @Nullable protected Map<String, Integer> myHighlightWords;
    @Nullable protected Map<String, Integer> myOriginalIndexMap;
    protected int myOriginalOrderIndex = 0;
    protected boolean myHighlightWordsCaseSensitive = true;
    @Nullable protected Pattern myHighlightPattern;
    @Nullable protected Map<String, Integer> myHighlightWordIndices;
    @Nullable protected Map<String, Integer> myHighlightCaseInsensitiveWordIndices;

    public WordHighlightProviderImpl(@NotNull ApplicationSettings settings) {
        super(settings);
    }

    @Override
    public WordHighlighter getHighlighter(@NotNull final Editor editor) {
        return new WordHighlighter(this, editor);
    }

    @Override
    public void disposeComponent() {
        clearHighlights();

        super.disposeComponent();
    }

    @Override
    public void clearHighlights() {
        myHighlightWords = null;
        myOriginalIndexMap = null;
        myOriginalOrderIndex = 0;
        myHighlightPattern = null;
        myHighlightWordIndices = null;
        myHighlightCaseInsensitiveWordIndices = null;
        fireHighlightsChanged();
    }

    @Override
    public boolean isWordHighlighted(CharSequence word) {
        if (myHighlightWords == null) return false;
        String wordText = word instanceof String ? (String) word : String.valueOf(word);
        if (myHighlightWords.containsKey(wordText)) return true;

        if (!myHighlightWordsCaseSensitive) {
            updateHighlightPattern();
            return myHighlightCaseInsensitiveWordIndices != null && myHighlightCaseInsensitiveWordIndices.containsKey(wordText.toLowerCase());
        }
        return false;
    }

    @Override
    @Nullable
    public Map<String, Integer> getHighlightWords() {
        return myHighlightWords;
    }

    @Override
    @Nullable
    public Map<String, Integer> getHighlightWordIndices() {
        updateHighlightPattern();
        return myHighlightWordIndices;
    }

    @Override
    @Nullable
    public Map<String, Integer> getHighlightCaseInsensitiveWordIndices() {
        updateHighlightPattern();
        return myHighlightCaseInsensitiveWordIndices;
    }

    // WordHighlightProvider
    @Override
    public boolean isShowHighlights() {
        return isHighlightsMode() && myHighlightWords != null && !myHighlightWords.isEmpty() && getHighlightWordIndices() != null && getHighlightCaseInsensitiveWordIndices() != null;
    }

    @Nullable
    public Pattern getHighlightPattern() {
        updateHighlightPattern();
        return myHighlightPattern;
    }

    @SuppressWarnings("MethodMayBeStatic")
    @Nullable
    public Pair<Color, EffectType> getHighlightEffect(final String word, final int wordIndex, final int startOffset, final int endOffset) {
        return null;
    }

    /**
     * Must call getHighlightPattern() before calling this function for the first time to ensure
     * the cached structures are updated.
     *
     * @param index           highlighted word index
     * @param startOffset     start offset in editor
     * @param endOffset       end offset in editor
     * @param foregroundColor
     * @param effectColor
     * @param effectType
     * @param fontType
     * @return text attributes to use for highlight or null if not highlighted
     */
    @Override
    @Nullable
    public TextAttributes getHighlightAttributes(final int index, final int startOffset, final int endOffset, final Color foregroundColor, final Color effectColor, final EffectType effectType, final int fontType) {
        if (index >= 0 && myHighlightColors != null) {
            int colorRepeatIndex = myHighlightColorRepeatIndex;
            int colorRepeatSteps = myHighlightColors.length - colorRepeatIndex;
            return new TextAttributes(foregroundColor,
                    myHighlightColors[index < colorRepeatIndex ? index : colorRepeatIndex + ((index - colorRepeatIndex) % colorRepeatSteps)],
                    effectColor,
                    effectType,
                    fontType
            );
        }
        return null;
    }

    /**
     * Must call getHighlightPattern() before calling this function for the first time to ensure
     * the cached structures are updated.
     *
     * @param word highlighted word
     * @return original index of word
     */
    @Override
    public int getHighlightWordIndex(final String word) {
        if (myHighlightWordIndices != null && myHighlightCaseInsensitiveWordIndices != null) {
            Integer index = myHighlightWordIndices.get(word);
            if (index == null) {
                // check for case insensitive word index
                index = myHighlightCaseInsensitiveWordIndices.get(word.toLowerCase());
            }

            if (index != null) {
                return index;
            }
        }
        return -1;
    }

    @Override
    public boolean haveHighlights() {
        return myHighlightWords != null && !myHighlightWords.isEmpty();
    }

    @Override
    public boolean isHighlightWordsCaseSensitive() {
        return myHighlightWordsCaseSensitive;
    }

    @Override
    public void setHighlightWordsCaseSensitive(final boolean highlightWordsCaseSensitive) {
        if (myHighlightWordsCaseSensitive != highlightWordsCaseSensitive) {
            myHighlightWordsCaseSensitive = highlightWordsCaseSensitive;
            myHighlightPattern = null;
            myHighlightWordIndices = null;
            myHighlightCaseInsensitiveWordIndices = null;
            if (haveHighlights()) {
                fireHighlightsChanged();
            }
        }
    }

    @Override
    public void addHighlightWord(CharSequence word, boolean beginWord, boolean endWord, Boolean caseSensitive) {
        addHighlightWord(word, encodeFlags(beginWord, endWord, caseSensitive));
    }

    @Override
    public void addHighlightWord(CharSequence word, int flags) {
        if ((flags & BEGIN_WORD) != 0 && (word.length() == 0 || word.charAt(0) == '$')) {
            flags &= ~BEGIN_WORD;
        }
        if ((flags & END_WORD) != 0 && (word.length() == 0 || word.charAt(word.length() - 1) == '$')) {
            flags &= ~END_WORD;
        }

        String wordText = word instanceof String ? (String) word : String.valueOf(word);

        // remove and add so flags will be modified and it will be moved to the end of list (which is considered the head)
        if (myHighlightWords != null && myOriginalIndexMap != null) {
            myHighlightWords.remove(wordText);
        } else {
            myHighlightWords = new HashMap<>();
            myOriginalIndexMap = new HashMap<>();
        }

        int originalOrderIndex = myOriginalOrderIndex++;

        myHighlightWords.put(wordText, flags);
        myOriginalIndexMap.put(wordText, originalOrderIndex);

        myHighlightPattern = null;
        myHighlightWordIndices = null;
        myHighlightCaseInsensitiveWordIndices = null;

        fireHighlightsChanged();
    }

    @Override
    public void removeHighlightWord(CharSequence word) {
        if (myHighlightWords != null) {
            String wordText = word instanceof String ? (String) word : String.valueOf(word);
            if (myHighlightWords.containsKey(wordText)) {
                // remove and add
                myHighlightWords.remove(wordText);
                myHighlightPattern = null;
                myHighlightWordIndices = null;
                myHighlightCaseInsensitiveWordIndices = null;
                fireHighlightsChanged();
            }
        }
    }

    @Override
    public void updateHighlightPattern() {
        if (myHighlightPattern == null && myHighlightWords != null && !myHighlightWords.isEmpty() && myOriginalIndexMap != null) {
            StringBuilder sb = new StringBuilder();
            String sep = "";
            int iMax = myHighlightWords.size();
            myHighlightWordIndices = new HashMap<>(iMax);
            myHighlightCaseInsensitiveWordIndices = new HashMap<>(iMax);
            boolean isCaseSensitive = true;

            ArrayList<Entry<String, Integer>> entries = new ArrayList<>(myHighlightWords.entrySet());

            entries.sort(Comparator.comparing((entry) -> -entry.getKey().length()));

            for (Entry<String, Integer> entry : entries) {
                sb.append(sep);
                sep = "|";

                boolean nextCaseSensitive = myHighlightWordsCaseSensitive;
                if ((entry.getValue() & WordHighlightProvider.CASE_INSENSITIVE) != 0) {
                    nextCaseSensitive = false;
                }
                if ((entry.getValue() & WordHighlightProvider.CASE_SENSITIVE) != 0) {
                    nextCaseSensitive = true;
                }
                if (isCaseSensitive != nextCaseSensitive) {
                    isCaseSensitive = nextCaseSensitive;
                    sb.append(isCaseSensitive ? "(?-i)" : "(?i)");
                }

                if ((entry.getValue() & WordHighlightProvider.BEGIN_WORD) != 0) sb.append("\\b");
                sb.append("\\Q").append(entry.getKey()).append("\\E");
                if ((entry.getValue() & WordHighlightProvider.END_WORD) != 0) sb.append("\\b");

                if (isCaseSensitive) {
                    myHighlightWordIndices.put(entry.getKey(), myOriginalIndexMap.get(entry.getKey()));
                } else {
                    myHighlightCaseInsensitiveWordIndices.put(entry.getKey().toLowerCase(), myOriginalIndexMap.get(entry.getKey()));
                }
            }

            myHighlightPattern = Pattern.compile(sb.toString());//, myHighlightWordsCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
        }
    }
}
