// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.Disposer;
import com.vladsch.MissingInActions.Bundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

public class ApplicationConfigurable implements SearchableConfigurable {

    @Nullable private ApplicationSettingsForm myForm = null;
    @NotNull final private ApplicationSettings myApplicationSettings;

    public ApplicationConfigurable() {
        myApplicationSettings = ApplicationSettings.getInstance();
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
