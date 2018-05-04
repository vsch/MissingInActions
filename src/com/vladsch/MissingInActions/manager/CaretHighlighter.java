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

package com.vladsch.MissingInActions.manager;

import com.intellij.openapi.editor.Caret;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

public interface CaretHighlighter {
    CaretHighlighter NULL = new CaretHighlighter() {
        // @formatter:off
        @Override public void caretAdded(@NotNull Caret caret) { }
        @Override public void caretRemoved(@NotNull Caret caret) { }
        @Override public CaretEx getPrimaryCaret() { return null; }
        @Override public void highlightCaretList( @Nullable final Collection<CaretEx> carets, @NotNull final CaretAttributeType attributeType, @Nullable final Set<Long> excludeList) { }
        @Override public void highlightCarets() { }
        @Override public void settingsChanged(ApplicationSettings settings) { }
        @Override public void removeCaretHighlight() { }
        @Override public void updateCaretHighlights() { }
        @Override public void setPrimaryCaret(@Nullable Caret caret) { }
        // @formatter:on
    };

    void updateCaretHighlights();
    @Nullable
    CaretEx getPrimaryCaret();
    void settingsChanged(ApplicationSettings settings);
    void removeCaretHighlight();
    void highlightCaretList(@Nullable Collection<CaretEx> carets, @NotNull CaretAttributeType attributeType, @Nullable Set<Long> excludeList);
    void highlightCarets();
    void caretAdded(@NotNull Caret caret);
    void caretRemoved(@NotNull Caret caret);
    void setPrimaryCaret(@Nullable Caret caret);
}
