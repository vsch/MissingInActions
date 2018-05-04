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
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class LineHighlightProviderImpl extends HighlightProviderBase implements LineHighlightProvider {
    @Nullable private Map<Integer, Integer> myHighlightLines;
    int myOriginalOrderIndex = 0;

    public LineHighlightProviderImpl(@NotNull ApplicationSettings settings) {
        super(settings);
    }

    @Override
    public LineHighlighter getHighlighter(@NotNull final Editor editor) {
        return new LineHighlighter(this, editor);
    }

    public void clearHighlights() {
        myHighlightLines = null;
        myOriginalOrderIndex = 0;
        fireHighlightsChanged();
    }

    @Override
    public boolean isLineHighlighted(int line) {
        return myHighlightLines != null && myHighlightLines.containsKey(line);
    }

    @Override
    @Nullable
    public Map<Integer, Integer> getHighlightLines() {
        return myHighlightLines;
    }

    @Override
    public boolean haveHighlights() {
        return myHighlightLines != null && isHighlightsMode();
    }

    // WordHighlightProvider
    @Override
    public boolean isShowHighlights() {
        return isHighlightsMode() && myHighlightLines != null && !myHighlightLines.isEmpty();
    }

    /**
     * Must call getHighlightPattern() before calling this function for the first time to ensure
     * the cached structures are updated.
     *
     * @param index        highlighted line number
     * @param startOffset start offset in editor
     * @param endOffset   end offset in editor
     * @return text attributes to use for highlight or null if not highlighted
     */
    @Override
    @Nullable
    public TextAttributes getHighlightAttributes(final int index, final int startOffset, final int endOffset, final Color foregroundColor, final Color effectColor, final EffectType effectType, final int fontType) {
        if (myHighlightLines != null && myHighlightColors != null) {
            if (index >= 0) {
                int colorRepeatIndex = myHighlightColorRepeatIndex;
                int colorRepeatSteps = myHighlightColors.length - colorRepeatIndex;
                return new TextAttributes(foregroundColor,
                        myHighlightColors[index < colorRepeatIndex ? index : colorRepeatIndex + ((index - colorRepeatIndex) % colorRepeatSteps)],
                        effectColor,
                        effectType,
                        fontType
                );
            }
        }
        return null;
    }

    @Override
    public int getHighlightLineIndex(final int line) {
        return myHighlightLines != null && myHighlightLines.containsKey(line) ? myHighlightLines.get(line) : -1;
    }

    @Override
    public void addHighlightLine(final int line) {
        // remove and add so flags will be modified and it will be moved to the end of list (which is considered the head)
        if (myHighlightLines != null) {
            myHighlightLines.remove(line);
        } else {
            myHighlightLines = new HashMap<>();
        }

        int index = myOriginalOrderIndex++;
        myHighlightLines.put(line, index);

        fireHighlightsChanged();
    }

    @Override
    public void removeHighlightLine(final int line) {
        if (myHighlightLines != null) {
            if (myHighlightLines.containsKey(line)) {
                // remove and add
                myHighlightLines.remove(line);
                fireHighlightsChanged();
            }
        }
    }
}
