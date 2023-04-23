// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
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
