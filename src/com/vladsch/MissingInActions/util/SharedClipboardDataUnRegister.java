// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.util;

import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.function.BiFunction;

public interface SharedClipboardDataUnRegister {
    /**
     * register shared clipboard {@link TextBlockTransferableData} type loader 
     * 
     * @param dataFlavor  data flavor
     * @param dataLoader function used to extract data from transferable.
     *                   Needed if text block deserialization is not provided by IDE and must be done in plugin's container
     */
    void unregisterClipboardDataLoader(@NotNull DataFlavor dataFlavor, @Nullable BiFunction<Transferable, DataFlavor, Object> dataLoader);
}
