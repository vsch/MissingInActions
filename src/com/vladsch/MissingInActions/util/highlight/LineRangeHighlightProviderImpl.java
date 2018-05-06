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
import java.util.BitSet;

public class LineRangeHighlightProviderImpl extends HighlightProviderBase implements LineRangeHighlightProvider {
    @Nullable protected BitSet myHighlightLines;
    protected boolean myInvertedHighlights;

    public LineRangeHighlightProviderImpl(@NotNull ApplicationSettings settings) {
        super(settings);
        myHighlightsMode = false;
        myInvertedHighlights = true;
    }

    @Override
    public boolean isInvertedHighlights() {
        return myInvertedHighlights;
    }

    @Override
    public void setInvertedHighlights(final boolean invertedHighlights) {
        myInvertedHighlights = invertedHighlights;
    }

    @Override
    public LineRangeHighlighter getHighlighter(@NotNull final Editor editor) {
        return new LineRangeHighlighter(this, editor);
    }

    public void clearHighlights() {
        myHighlightLines = null;
        fireHighlightsChanged();
    }

    @Override
    public boolean isLineHighlighted(int line) {
        return myHighlightLines != null && myHighlightLines.get(line);
    }

    @Override
    public boolean haveHighlights() {
        return myHighlightLines != null;
    }

    // LineHighlightProvider
    @Override
    public boolean isShowHighlights() {
        return isHighlightsMode() && myHighlightLines != null && !myHighlightLines.isEmpty();
    }

    @Override
    public int getHighlightLineIndex(final int line) {
        return myHighlightLines != null && myHighlightLines.get(line) ? line : -1;
    }

    @Override
    public void addHighlightLine(final int line) {
        // remove and add so flags will be modified and it will be moved to the end of list (which is considered the head)
        if (myHighlightLines == null) {
            myHighlightLines = new BitSet();
            myHighlightLines.set(line);
            fireHighlightsChanged();
        } else if (!myHighlightLines.get(line)) {
            myHighlightLines.set(line);
            fireHighlightsChanged();
        }
    }

    @Override
    public void removeHighlightLine(final int line) {
        if (myHighlightLines != null) {
            if (myHighlightLines.get(line)) {
                myHighlightLines.clear(line);
                fireHighlightsChanged();
            }
        }
    }

    @Nullable
    @Override
    public BitSet getHighlightLines() {
        return myHighlightLines;
    }

    @Override
    public void setHighlightLines(final BitSet bitSet, Boolean highlightMode) {
        if (bitSet != null && !bitSet.isEmpty()) {
            boolean highlightsMode = highlightMode != null ? highlightMode : isHighlightsMode();
            enterUpdateRegion();
            setHighlightsMode(highlightsMode);
            myHighlightLines = (BitSet) bitSet.clone();
            fireHighlightsChanged();
            leaveUpdateRegion();
        } else {
            clearHighlights();
        }
    }

    @Nullable
    @Override
    public BitSet addHighlightLines(final int startLine, final int endLine) {
        if (startLine >= 0 && startLine < endLine) {
            if (myHighlightLines == null) myHighlightLines = new BitSet();
            myHighlightLines.set(startLine, endLine);
            fireHighlightsChanged();
        }
        return myHighlightLines;
    }

    @Nullable
    @Override
    public BitSet addHighlightLines(@Nullable final BitSet bitSet) {
        if (bitSet != null && !bitSet.isEmpty()) {
            if (myHighlightLines == null) myHighlightLines = new BitSet();
            myHighlightLines.or(bitSet);
            fireHighlightsChanged();
        }
        return myHighlightLines;
    }

    @Nullable
    @Override
    public BitSet removeHighlightLines(@Nullable final BitSet bitSet) {
        if (myHighlightLines != null && bitSet != null && !bitSet.isEmpty()) {
            myHighlightLines.andNot(bitSet);
            if (myHighlightLines.isEmpty()) myHighlightLines = null;
            fireHighlightsChanged();
        }
        return myHighlightLines;
    }

    @Nullable
    @Override
    public BitSet removeHighlightLines(final int startLine, final int endLine) {
        if (myHighlightLines != null && startLine >= 0 && startLine < endLine) {
            myHighlightLines.clear(startLine, endLine);
            if (myHighlightLines.isEmpty()) myHighlightLines = null;
            fireHighlightsChanged();
        }
        return myHighlightLines;
    }

    /**
     * Must call getHighlightPattern() before calling this function for the first time to ensure
     * the cached structures are updated.
     *
     * @param index       highlighted line number
     * @param startOffset start offset in editor
     * @param endOffset   end offset in editor
     * @return text attributes to use for highlight or null if not highlighted
     */
    @Override
    @Nullable
    public TextAttributes getHighlightAttributes(final int index, final int startOffset, final int endOffset, final @Nullable Color foregroundColor, final @Nullable Color effectColor, final @Nullable EffectType effectType, final int fontType) {
        Color foreground = foregroundColor != null ? foregroundColor : mySettings.isIsolatedForegroundColorEnabled() ? mySettings.isolatedForegroundColorRGB() : null;
        Color background = mySettings.isIsolatedBackgroundColorEnabled() ? mySettings.isolatedBackgroundColorRGB() : null;
        if (index >= 0) {
            return new TextAttributes(foreground, background, effectColor, effectType, fontType);
        }
        return null;
    }
}
