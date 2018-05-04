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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.util.Disposer;
import com.vladsch.MissingInActions.util.AwtRunnable;
import com.vladsch.MissingInActions.util.OneTimeRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;

public abstract class Highlighter {
    @NotNull protected final Editor myEditor;
    @NotNull protected final HighlightProvider myHighlightProvider;
    @Nullable protected List<RangeHighlighter> myHighlighters = null;
    @Nullable protected List<Integer> myHighlighterIndexList = null;
    private OneTimeRunnable myHighlightRunner = OneTimeRunnable.NULL;
    private final HashSet<HighlightListener> myHighlightListeners;

    public Highlighter(@NotNull final Editor editor, @NotNull final HighlightProvider highlightProvider) {
        myEditor = editor;
        myHighlightProvider = highlightProvider;
        myHighlightListeners = new HashSet<>();
    }

    public void addHighlightListener(@NotNull HighlightListener highlightListener, @NotNull Disposable parent) {
        if (!myHighlightListeners.contains(highlightListener)) {
            myHighlightListeners.add(highlightListener);
            Disposer.register(parent, new Disposable() {
                @Override
                public void dispose() {
                    myHighlightListeners.remove(highlightListener);
                }
            });
        }
    }

    public void removeHighlightListener(@NotNull HighlightListener highlightListener) {
        myHighlightListeners.remove(highlightListener);
    }

    public void fireHighlightsChanged() {
        myHighlightRunner.cancel();
        if (!myHighlightListeners.isEmpty()) {
            myHighlightRunner = OneTimeRunnable.schedule(250, new AwtRunnable(true, () -> {
                for (HighlightListener listener : myHighlightListeners) {
                    if (listener == null) continue;
                    listener.highlightsChanged();
                }
            }));
        }
    }


    static public void clearHighlighters(Editor editor, List<RangeHighlighter> highlighters) {
        if (highlighters != null) {
            MarkupModel markupModel = editor.getMarkupModel();
            for (RangeHighlighter marker : highlighters) {
                if (marker.isValid()) {
                    markupModel.removeHighlighter(marker);
                    marker.dispose();
                }
            }
            editor.getContentComponent().invalidate();
        }
    }

    protected void removeHighlightsRaw() {
        clearHighlighters(myEditor, myHighlighters);
        myHighlighters = null;
        myHighlighterIndexList = null;
    }

    public void removeHighlights() {
        if (myHighlighters != null && !myHighlighters.isEmpty()) {
            removeHighlightsRaw();
            fireHighlightsChanged();
        }
    }

    public RangeHighlighter getRangeHighlighter(int offset) {
        if (myHighlighters != null) {
            for (RangeHighlighter rangeHighlighter : myHighlighters) {
                if (offset >= rangeHighlighter.getStartOffset() && offset <= rangeHighlighter.getEndOffset()) {
                    return rangeHighlighter;
                }
            }
        }
        return null;
    }

    public RangeHighlighter getNextRangeHighlighter(int offset) {
        if (myHighlighters != null) {
            for (RangeHighlighter rangeHighlighter : myHighlighters) {
                if (rangeHighlighter.getStartOffset() >= offset) {
                    return rangeHighlighter;
                }
            }
        }
        return null;
    }

    public RangeHighlighter getPreviousRangeHighlighter(int offset) {
        if (myHighlighters != null) {
            int iMax = myHighlighters.size();
            for (int i = iMax; i-- > 0; ) {
                RangeHighlighter rangeHighlighter = myHighlighters.get(i);
                if (rangeHighlighter.getEndOffset() <= offset) {
                    return rangeHighlighter;
                }
            }
        }
        return null;
    }

    public int getIndex(RangeHighlighter rangeHighlighter) {
        if (myHighlighters != null) {
            return myHighlighters.indexOf(rangeHighlighter);
        }
        return -1;
    }

    public int getOriginalIndex(RangeHighlighter rangeHighlighter) {
        int index = getIndex(rangeHighlighter);
        if (myHighlighterIndexList != null && index >= 0 && index < myHighlighterIndexList.size()) {
            return myHighlighterIndexList.get(index);
        }
        return index;
    }

    public abstract void updateHighlights();
}
