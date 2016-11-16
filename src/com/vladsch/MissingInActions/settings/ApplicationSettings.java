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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "MissingInAction",
        storages = {
                @Storage(id = "MissingInActionSettings", file = StoragePathMacros.APP_CONFIG + "/MissingInAction.xml", roamingType = RoamingType.DISABLED)
        }
)
@SuppressWarnings("WeakerAccess")
public class ApplicationSettings implements ApplicationComponent, PersistentStateComponent<ApplicationSettings> {
    private boolean myMouseLineSelection;
    private boolean myEditorBackspaceKey;
    private boolean myEditorDeleteKey;
    private boolean myEditorUpDownKeys;
    private boolean myEditorLeftRightKeys;

    public boolean isMouseLineSelection() {
        return myMouseLineSelection;
    }

    public void setMouseLineSelection(boolean mouseLineSelection) {
        myMouseLineSelection = mouseLineSelection;
    }

    public boolean isEditorBackspaceKey() {
        return myEditorBackspaceKey;
    }

    public void setEditorBackspaceKey(boolean editorBackspaceKey) {
        myEditorBackspaceKey = editorBackspaceKey;
    }

    public boolean isEditorDeleteKey() {
        return myEditorDeleteKey;
    }

    public void setEditorDeleteKey(boolean editorDeleteKey) {
        myEditorDeleteKey = editorDeleteKey;
    }

    public boolean isEditorUpDownKeys() {
        return myEditorUpDownKeys;
    }

    public void setEditorUpDownKeys(boolean editorUpDownKeys) {
        myEditorUpDownKeys = editorUpDownKeys;
    }

    public boolean isEditorLeftRightKeys() {
        return myEditorLeftRightKeys;
    }

    public void setEditorLeftRightKeys(boolean editorLeftRightKeys) {
        myEditorLeftRightKeys = editorLeftRightKeys;
    }

    @Nullable
    public ApplicationSettings getState() {
        return this;
    }

    public void loadState(ApplicationSettings applicationSettings) {
        XmlSerializerUtil.copyBean(applicationSettings, this);
        notifySettingsChanged();
    }

    public void initComponent() {
    }

    public void disposeComponent() {
    }

    public void notifySettingsChanged() {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(ApplicationSettingsListener.TOPIC).onSettingsChange(this);
    }

    @NotNull
    public String getComponentName() {
        return this.getClass().getName();
    }

    public static ApplicationSettings getInstance() {
        return ApplicationManager.getApplication().getComponent(ApplicationSettings.class);
    }

}
