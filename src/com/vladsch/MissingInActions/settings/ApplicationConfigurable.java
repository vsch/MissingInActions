/*
 * Copyright (c) 2016-2016 Vladimir Schneider <vladimir.schneider@gmail.com>
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
package com.vladsch.MissingInActions.settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.Disposer;
import com.vladsch.MissingInActions.Bundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ApplicationConfigurable implements SearchableConfigurable {

    @Nullable
    private ApplicationSettingsForm myForm = null;
    @NotNull
    final private ApplicationSettings myApplicationSettings;

    public ApplicationConfigurable(@NotNull ApplicationSettings applicationSettings) {
        myApplicationSettings = applicationSettings;
    }

    @NotNull
    @Override
    public String getId() {
        return "MarkdownNavigator.Settings.Application";
    }

    @Nullable
    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return Bundle.message("plugin.name");
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        return getForm().getComponent();
    }

    @NotNull
    public ApplicationSettingsForm getForm() {
        if (myForm == null) {
            myForm = new ApplicationSettingsForm(myApplicationSettings);
        }
        return myForm;
    }

    @Override
    public boolean isModified() {
        return getForm().isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        // save update stream
        getForm().apply();
        myApplicationSettings.notifySettingsChanged();
    }

    @Override
    public void reset() {
        // reset update stream
        getForm().reset();
    }

    @Override
    public void disposeUIResources() {
        if (myForm != null) {
            Disposer.dispose(myForm);
            myForm = null;
        }
    }
}
