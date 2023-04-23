// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.settings;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

public interface ApplicationSettingsListener {
    Topic<ApplicationSettingsListener> TOPIC = Topic.create("MissingInAction.ApplicationSettingsChanged", ApplicationSettingsListener.class);

    void onSettingsChange(@NotNull ApplicationSettings settings);
}
