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
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;

public interface HighlightProvider {
    void settingsChanged(ApplicationSettings settings);
    void clearHighlights();
    boolean haveHighlights();
    boolean isHighlightsMode();
    void setHighlightsMode(boolean highlightsMode);
    boolean isShowHighlights();

    void initComponent();
    void disposeComponent();
    void enterUpdateRegion();
    void leaveUpdateRegion();

    void addHighlightListener(@NotNull HighlightListener highlightListener, @NotNull Disposable parent);
    void removeHighlightListener(HighlightListener highlightListener);
    void fireHighlightsChanged();
    void fireHighlightsUpdated();

    @Nullable
    TextAttributes getHighlightAttributes(int index, int startOffset, int endOffset, @Nullable Color foregroundColor, @Nullable Color effectColor, @Nullable EffectType effectType, int fontType);
    Highlighter getHighlighter(@NotNull Editor editor);
}
