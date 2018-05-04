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

import com.vladsch.MissingInActions.manager.EditorPosition;
import org.jetbrains.annotations.NotNull;

/**
 * Interface that provides read-only access to caret data
 */
public interface EditorCaretSnapshot {
    EditorCaretSnapshot NULL = new EditorCaretSnapshot() {
        // @formatter:off
        @Override public boolean isStartAnchor() { return false; }
        @NotNull @Override public EditorPosition getCaretPosition() { return EditorPosition.NULL; }
        @Override public boolean isLine() { return false; }
        @Override public boolean hasLines() { return false; }
        @Override public boolean hasSelection() { return false; }
        @NotNull @Override public EditorPosition getSelectionStart() { return EditorPosition.NULL; }
        @NotNull @Override public EditorPosition getSelectionEnd() { return EditorPosition.NULL; }
        @Override public int getSelectionLineCount() { return 0; }
        @NotNull @Override public EditorPosition getLineSelectionStart() { return EditorPosition.NULL; }
        @NotNull @Override public EditorPosition getLineSelectionEnd() { return EditorPosition.NULL; }
        @NotNull @Override public EditorPosition getAnchorPosition() { return EditorPosition.NULL; }
        @NotNull @Override public EditorPosition getAntiAnchorPosition() { return EditorPosition.NULL; }
        @Override public int getAnchorColumn() { return -1; }
        @Override public int getColumn() { return 0; }
        @Override public int getIndent() { return 0; }
        @NotNull @Override public EditorCaretSnapshot removeSelection() { return this; }
        // @formatter:on
    };

    // *
    // * EditorCaretSnapshot
    // *
    boolean isStartAnchor();
    @NotNull
    EditorPosition getCaretPosition();
    boolean isLine();
    boolean hasLines();
    boolean hasSelection();

    @NotNull
    EditorPosition getSelectionStart();
    @NotNull
    EditorPosition getSelectionEnd();
    int getSelectionLineCount();
    @NotNull
    EditorPosition getLineSelectionStart();
    @NotNull
    EditorPosition getLineSelectionEnd();
    @NotNull
    EditorPosition getAnchorPosition();
    @NotNull
    EditorPosition getAntiAnchorPosition();
    int getColumn();
    int getIndent();
    int getAnchorColumn();
    @NotNull
    EditorCaretSnapshot removeSelection();
}
