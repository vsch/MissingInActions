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

package com.vladsch.MissingInActions.settings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.HashMap;

//@XmlRootElement(name = "BulkSearchReplaceSettings")
public class BulkSearchReplaceSettings implements Serializable {
    final @NotNull BulkSearchReplace mySearchReplace = new BulkSearchReplace();
    final @NotNull HashMap<String, BulkSearchReplace> myPresets = new HashMap<>();
    @Nullable String myPresetName = null;

    public BulkSearchReplaceSettings() {
    }

    public BulkSearchReplaceSettings(final BulkSearchReplaceSettings other) {
        copyFrom(other);
    }

    public void copyFrom(final BulkSearchReplaceSettings other) {
        mySearchReplace.copyFrom(other.mySearchReplace);
        myPresets.clear();
        myPresets.putAll(other.myPresets);
        myPresetName = other.myPresetName;
    }

    @NotNull
    public HashMap<String, BulkSearchReplace> getPresets() {
        return myPresets;
    }

    public void setPresets(@NotNull final HashMap<String, BulkSearchReplace> presets) {
        myPresets.clear();
        myPresets.putAll(presets);
    }

    @Nullable
    public String getPresetName() {
        return myPresetName;
    }

    public void setPresetName(@Nullable final String presetName) {
        myPresetName = presetName;
    }

    @NotNull
    public BulkSearchReplace getSearchReplace() {
        return mySearchReplace;
    }

    public void setSearchReplace(@NotNull final BulkSearchReplace searchReplace) {
        mySearchReplace.copyFrom(searchReplace);
    }

    public BulkSearchReplace getPreset(@NotNull final  String presetName) {
        return myPresets.get(presetName);
    }

    public BulkSearchReplace savePreset(@NotNull final  String presetName) {
        return myPresets.put(presetName, mySearchReplace);
    }

    public BulkSearchReplace loadPreset(@NotNull final  String presetName) {
        BulkSearchReplace searchReplace = myPresets.get(presetName);
        if (searchReplace != null) {
            mySearchReplace.copyFrom(searchReplace);
            myPresetName = presetName;
        }
        return searchReplace;
    }
}
