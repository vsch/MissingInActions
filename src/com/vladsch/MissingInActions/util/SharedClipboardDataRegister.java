/*
 * Copyright (c) 2016-2019 Vladimir Schneider <vladimir.schneider@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.vladsch.MissingInActions.util;

import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.function.BiFunction;

public interface SharedClipboardDataRegister {
    /**
     * register shared clipboard {@link TextBlockTransferableData} type loader 
     * 
     * @param dataFlavor  data flavor
     * @param dataLoader function used to extract data from transferable.
     *                   Needed if text block deserialization is not provided by IDE and must be done in plugin's container
     */
    void registerClipboardDataLoader(@NotNull DataFlavor dataFlavor, @Nullable BiFunction<Transferable, DataFlavor, Object> dataLoader);
}
