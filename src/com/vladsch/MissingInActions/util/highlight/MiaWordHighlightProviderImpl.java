// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.util.highlight;

import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.plugin.util.CancelableJobScheduler;
import com.vladsch.plugin.util.ui.ColorIterable;
import com.vladsch.plugin.util.ui.highlight.WordHighlightProviderBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MiaWordHighlightProviderImpl extends WordHighlightProviderBase<ApplicationSettings> {
    public MiaWordHighlightProviderImpl(@NotNull ApplicationSettings settings) {
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
}
