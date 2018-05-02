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

import com.intellij.util.xmlb.annotations.OptionTag;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Transient;
import com.intellij.util.xmlb.annotations.XMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.HashMap;

//@XmlRootElement(name = "BulkSearchReplaceSettings")
public class BulkSearchReplaceSettings implements Serializable {
    @Property @NotNull BulkSearchReplace bulkSearchReplace = new BulkSearchReplace();
    @XMap @NotNull HashMap<String, BulkSearchReplace> bulkPresets = new HashMap<>();
    @OptionTag @Nullable String bulkPresetName = null;

    public BulkSearchReplaceSettings() {
    }

    public BulkSearchReplaceSettings(final BulkSearchReplaceSettings other) {
        copyFrom(other);
    }

    public void copyFrom(final BulkSearchReplaceSettings other) {
        bulkSearchReplace.copyFrom(other.bulkSearchReplace);
        bulkPresets.clear();
        bulkPresets.putAll(other.bulkPresets);
        bulkPresetName = other.bulkPresetName;
    }

    @NotNull
    @Transient
    public HashMap<String, BulkSearchReplace> getBulkPresets() {
        return bulkPresets;
    }

    @Transient
    public void setBulkPresets(@NotNull final HashMap<String, BulkSearchReplace> bulkPresets) {
        this.bulkPresets.clear();
        this.bulkPresets.putAll(bulkPresets);
    }

    @Nullable
    @Transient
    public String getBulkPresetName() {
        return bulkPresetName;
    }

    @Transient
    public void setBulkPresetName(@Nullable final String bulkPresetName) {
        this.bulkPresetName = bulkPresetName;
    }

    @NotNull
    @Transient
    public BulkSearchReplace getBulkSearchReplace() {
        return bulkSearchReplace;
    }

    @Transient
    public void setBulkSearchReplace(@NotNull final BulkSearchReplace bulkSearchReplace) {
        this.bulkSearchReplace.copyFrom(bulkSearchReplace);
    }

    @Transient
    public BulkSearchReplace getPreset(@NotNull final String presetName) {
        return bulkPresets.get(presetName);
    }

    public BulkSearchReplace savePreset(@NotNull final String presetName) {
        return bulkPresets.put(presetName, bulkSearchReplace);
    }

    public BulkSearchReplace loadPreset(@NotNull final String presetName) {
        BulkSearchReplace searchReplace = bulkPresets.get(presetName);
        if (searchReplace != null) {
            bulkSearchReplace.copyFrom(searchReplace);
            bulkPresetName = presetName;
        }
        return searchReplace;
    }
}
