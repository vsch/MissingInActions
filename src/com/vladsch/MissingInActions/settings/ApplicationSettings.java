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
    private int myAutoLineMode = AutoLineSettingType.DEFAULT.getIntValue();
    private int myMouseModifier = MouseModifierType.DEFAULT.getIntValue();
    private boolean myMouseLineSelection = false;
    private boolean myDeleteOperations = false;
    private boolean myUpDownMovement = false;
    private boolean myLeftRightMovement = false;
    private boolean myUpDownSelection = false;
    private int myAutoIndentDelay = 300;
    private boolean myAutoIndent = false;
    private boolean mySelectPasted = false;
    private boolean mySelectPastedLineOnly = true;
    private boolean myUnselectToggleCase = false;

    public boolean isLineModeEnabled() {
        return myAutoLineMode != AutoLineSettingType.DISABLED.getIntValue() && (
                myMouseLineSelection
                        || myUpDownSelection
                        //|| myDeleteOperations
                        //|| myUpDownMovement
                        //|| myLeftRightMovement
        );
    }

    public boolean isSelectPastedLineOnly() {
        return mySelectPastedLineOnly;
    }

    public void setSelectPastedLineOnly(boolean selectPastedLineOnly) {
        mySelectPastedLineOnly = selectPastedLineOnly;
    }

    public boolean isUnselectToggleCase() {
        return myUnselectToggleCase;
    }

    public void setUnselectToggleCase(boolean unselectToggleCase) {
        myUnselectToggleCase = unselectToggleCase;
    }

    public int getMouseModifier() {
        return myMouseModifier;
    }

    public void setMouseModifier(int mouseModifier) {
        myMouseModifier = mouseModifier;
    }

    public boolean isUpDownSelection() {
        return myUpDownSelection;
    }

    public void setUpDownSelection(boolean upDownSelection) {
        myUpDownSelection = upDownSelection;
    }

    public boolean isMouseLineSelection() {
        return myMouseLineSelection;
    }

    public void setMouseLineSelection(boolean mouseLineSelection) {
        myMouseLineSelection = mouseLineSelection;
    }

    public int getAutoLineMode() {
        return myAutoLineMode;
    }

    public void setAutoLineMode(int autoLineMode) {
        myAutoLineMode = autoLineMode;
    }

    public boolean isDeleteOperations() {
        return myDeleteOperations;
    }

    public void setDeleteOperations(boolean deleteOperations) {
        myDeleteOperations = deleteOperations;
    }

    public boolean isUpDownMovement() {
        return myUpDownMovement;
    }

    public void setUpDownMovement(boolean upDownMovement) {
        myUpDownMovement = upDownMovement;
    }

    public boolean isLeftRightMovement() {
        return myLeftRightMovement;
    }

    public void setLeftRightMovement(boolean leftRightMovement) {
        myLeftRightMovement = leftRightMovement;
    }

    public int getAutoIndentDelay() {
        return myAutoIndentDelay;
    }

    public void setAutoIndentDelay(int autoIndentDelay) {
        myAutoIndentDelay = autoIndentDelay;
    }

    public boolean isAutoIndent() {
        return myAutoIndent;
    }

    public void setAutoIndent(boolean autoIndent) {
        myAutoIndent = autoIndent;
    }

    public boolean isSelectPasted() {
        return mySelectPasted;
    }

    public void setSelectPasted(boolean selectPasted) {
        mySelectPasted = selectPasted;
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
