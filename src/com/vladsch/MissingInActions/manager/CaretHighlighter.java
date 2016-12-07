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

package com.vladsch.MissingInActions.manager;

import com.intellij.openapi.editor.Caret;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import org.jetbrains.annotations.NotNull;

public interface CaretHighlighter {
    CaretHighlighter NULL = new CaretHighlighter() {
        // @formatter:off
        @Override public void caretAdded(@NotNull Caret caret) { }
        @Override public void caretRemoved(@NotNull Caret caret) { }
        @Override public void highlightCarets() { }
        @Override public void removeCaretHighlight() { }
        @Override public void settingsChanged(ApplicationSettings settings) { }
        @Override public void updateCaretHighlights() { }
        // @formatter:on
    };

    void updateCaretHighlights();
    void settingsChanged(ApplicationSettings settings);
    void removeCaretHighlight();
    void highlightCarets();
    void caretAdded(@NotNull Caret caret);
    void caretRemoved(@NotNull Caret caret);
}
