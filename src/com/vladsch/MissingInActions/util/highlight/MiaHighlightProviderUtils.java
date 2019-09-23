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
        //noinspection ThisEscapedInObjectConstruction
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
