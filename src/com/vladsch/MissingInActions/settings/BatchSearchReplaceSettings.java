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

//@XmlRootElement(name = "BatchSearchReplaceSettings")
public class BatchSearchReplaceSettings implements Serializable {
    @Property @NotNull BatchSearchReplace batchSearchReplace = new BatchSearchReplace();
    @XMap @NotNull HashMap<String, BatchSearchReplace> batchPresets = new HashMap<>();
    @OptionTag @Nullable String batchPresetName = null;

    public BatchSearchReplaceSettings() {
    }

    public BatchSearchReplaceSettings(final BatchSearchReplaceSettings other) {
        copyFrom(other);
    }

    public void copyFrom(final BatchSearchReplaceSettings other) {
        batchSearchReplace.copyFrom(other.batchSearchReplace);
        batchPresets.clear();
        batchPresets.putAll(other.batchPresets);
        batchPresetName = other.batchPresetName;
    }

    @NotNull
    @Transient
    public HashMap<String, BatchSearchReplace> getBatchPresets() {
        return batchPresets;
    }

    @Transient
    public void setBatchPresets(@NotNull final HashMap<String, BatchSearchReplace> batchPresets) {
        this.batchPresets.clear();
        this.batchPresets.putAll(batchPresets);
    }

    @Nullable
    @Transient
    public String getBatchPresetName() {
        return batchPresetName;
    }

    @Transient
    public void setBatchPresetName(@Nullable final String batchPresetName) {
        this.batchPresetName = batchPresetName;
    }

    @NotNull
    @Transient
    public BatchSearchReplace getBatchSearchReplace() {
        return batchSearchReplace;
    }

    @Transient
    public void setBatchSearchReplace(@NotNull final BatchSearchReplace batchSearchReplace) {
        this.batchSearchReplace.copyFrom(batchSearchReplace);
    }

    @Transient
    public BatchSearchReplace getPreset(@NotNull final String presetName) {
        return batchPresets.get(presetName);
    }

    public BatchSearchReplace savePreset(@NotNull final String presetName) {
        return batchPresets.put(presetName, new BatchSearchReplace(batchSearchReplace));
    }

    public BatchSearchReplace loadPreset(@NotNull final String presetName) {
        BatchSearchReplace searchReplace = batchPresets.get(presetName);
        if (searchReplace != null) {
            batchSearchReplace.copyFrom(searchReplace);
            batchPresetName = presetName;
        }
        return searchReplace;
    }
}
