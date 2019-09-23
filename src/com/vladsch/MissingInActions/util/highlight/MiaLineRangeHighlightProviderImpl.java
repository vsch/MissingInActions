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

import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.plugin.util.CancelableJobScheduler;
import com.vladsch.plugin.util.ui.ColorIterable;
import com.vladsch.plugin.util.ui.highlight.LineRangeHighlightProviderBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;

public class MiaLineRangeHighlightProviderImpl extends LineRangeHighlightProviderBase<ApplicationSettings> {
    public MiaLineRangeHighlightProviderImpl(@NotNull ApplicationSettings settings) {
        super(settings);
    }

    @Override
    protected void subscribeSettingsChanged() {
        MiaHighlightProviderUtils.subscribeSettingsChanged(this);
    }

    @Nullable
    @Override
    protected CancelableJobScheduler getCancellableJobScheduler() {
        return MiaHighlightProviderUtils.getCancellableJobScheduler();
    }

    @NotNull
    @Override
    protected ColorIterable getColors(@NotNull final ApplicationSettings settings) {
        return MiaHighlightProviderUtils.getColors(settings);
    }

    /**
     * Must call getHighlightPattern() before calling this function for the first time to ensure
     * the cached structures are updated.
     *
     * @param index       highlighted line number
     * @param flags
     * @param startOffset start offset in editor
     * @param endOffset   end offset in editor
     *
     * @return text attributes to use for highlight or null if not highlighted
     */
    @Override
    @Nullable
    public TextAttributes getHighlightAttributes(final int index, final int flags, final int startOffset, final int endOffset, final @Nullable Color foregroundColor, final @Nullable Color effectColor, final @Nullable EffectType effectType, final int fontType) {
        Color foreground = foregroundColor != null ? foregroundColor : mySettings.isIsolatedForegroundColorEnabled() ? mySettings.isolatedForegroundColorRGB() : null;
        Color background = mySettings.isIsolatedBackgroundColorEnabled() ? mySettings.isolatedBackgroundColorRGB() : null;
        if (index >= 0) {
            return new TextAttributes(foreground, background, effectColor, effectType, fontType);
        }
        return null;
    }
}
