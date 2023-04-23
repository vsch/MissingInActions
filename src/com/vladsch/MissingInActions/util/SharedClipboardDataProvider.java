// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.util;

import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.Transferable;

public interface SharedClipboardDataProvider {
    /**
     * Called on component init to let providers know that extension point is active
     * to allow alternate clipboard handling if mia is not installed and to register
     * shared custom data loaders.
     * <p>
     * a loader is a function used to extract data from transferable.
     * Needed if text block deserialization is not provided by IDE and must be done in plugin's container
     */
    void initialize(@NotNull SharedClipboardDataRegister register);

    /**
     * Called when plugins are unloaded to allow data loader to be removed
     * 
     * @param unRegister   registrar to use for removing loaders
     */
    void removeLoaderIfNeeded(@NotNull SharedClipboardDataUnRegister unRegister);

    /**
     * Opportunity to augment clipboard data if needed
     *
     * @param transferable current transferable clipboard content
     * @param builder      builder used to add data and data loaders
     */
    void addSharedClipboardData(@NotNull Transferable transferable, @NotNull SharedClipboardDataBuilder builder);
}
