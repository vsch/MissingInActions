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

import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.plugin.util.CancelableJobScheduler;
import com.vladsch.plugin.util.ui.ColorIterable;
import com.vladsch.plugin.util.ui.highlight.LineHighlightProviderBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MiaLineHighlightProviderImpl extends LineHighlightProviderBase<ApplicationSettings> {
    public MiaLineHighlightProviderImpl(@NotNull ApplicationSettings settings) {
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
