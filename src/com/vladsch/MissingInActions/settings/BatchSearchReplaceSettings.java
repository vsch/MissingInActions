// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
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
