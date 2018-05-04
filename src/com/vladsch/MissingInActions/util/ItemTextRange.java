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

package com.vladsch.MissingInActions.util;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

class ItemTextRange<T> extends TextRange {
    private T myItem;

    public ItemTextRange(final T item, final @NotNull TextRange other) {
        super(other.getStartOffset(), other.getEndOffset());
        myItem = item;
    }

    public ItemTextRange(final T item, final int startOffset, final int endOffset) {
        super(startOffset, endOffset);
        myItem = item;
    }

    public ItemTextRange(final T item, final int startOffset, final int endOffset, final boolean checkForProperTextRange) {
        super(startOffset, endOffset, checkForProperTextRange);
        myItem = item;
    }

    public T getItem() {
        return myItem;
    }
}
