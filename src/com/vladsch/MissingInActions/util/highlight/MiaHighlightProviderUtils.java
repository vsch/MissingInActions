// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.util.highlight;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBusConnection;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.settings.ApplicationSettingsListener;
import com.vladsch.MissingInActions.util.MiaCancelableJobScheduler;
import com.vladsch.plugin.util.CancelableJobScheduler;
import com.vladsch.plugin.util.ui.ColorIterable;
import com.vladsch.plugin.util.ui.highlight.HighlightProviderBase;
import org.jetbrains.annotations.NotNull;

public class MiaHighlightProviderUtils {

    public static CancelableJobScheduler getCancellableJobScheduler() {
        return MiaCancelableJobScheduler.getInstance();
    }

    public static void subscribeSettingsChanged(HighlightProviderBase<ApplicationSettings> highlightProvider) {
        MessageBusConnection messageBusConnection = ApplicationManager.getApplication().getMessageBus().connect(highlightProvider);
        messageBusConnection.subscribe(ApplicationSettingsListener.TOPIC, settings1 -> highlightProvider.settingsChanged(getColors(settings1), settings1));
        highlightProvider.getDelayedRunner().addRunnable(messageBusConnection::disconnect);
    }

    public static ColorIterable getColors(@NotNull ApplicationSettings settings) {
        return getColorIterable(settings);
    }

    public static ColorIterable getColorIterable(@NotNull ApplicationSettings settings) {
        return new ColorIterable(
                settings.getHueMin(),
                settings.getHueMax(),
                settings.getHueSteps(),
                settings.getSaturationMin(),
                settings.getSaturationMax(),
                settings.getSaturationSteps(),
                settings.getBrightnessMin(),
                settings.getBrightnessMax(),
                settings.getBrightnessSteps()
        );
    }
}
