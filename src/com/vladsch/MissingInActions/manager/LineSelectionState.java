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

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("WeakerAccess")
public class LineSelectionState {
    final public @Nullable EditorPosition anchorPosition;
    final public boolean isStartAnchor;
    final public boolean isLine;
    final public boolean hadSelection;
    final public boolean hadLineSelection;
    final public int preservedColumn; 
    final public int preservedIndent; 

    LineSelectionState(@Nullable EditorPosition anchorPosition, boolean isStartAnchor, boolean isLine, boolean hadSelection, boolean hadLineSelection, int preservedColumn, int preservedIndent) {
        this.anchorPosition = anchorPosition;
        this.isStartAnchor = isStartAnchor;
        this.isLine = isLine;
        this.hadSelection = hadSelection;
        this.hadLineSelection = hadLineSelection;
        this.preservedColumn = preservedColumn;
        this.preservedIndent = preservedIndent;
    }

    LineSelectionState(LineSelectionState other) {
        this.anchorPosition = other.anchorPosition;
        this.isStartAnchor = other.isStartAnchor;
        this.isLine = other.isLine;
        this.hadSelection = other.hadSelection;
        this.hadLineSelection = other.hadLineSelection;
        this.preservedColumn = other.preservedColumn;
        this.preservedIndent = other.preservedIndent;
    }

    public LineSelectionState(int preservedColumn, int preservedIndent) {
        this.anchorPosition = null;
        this.isStartAnchor = false;
        this.isLine = false;
        this.hadSelection = false;
        this.hadLineSelection = false;

        this.preservedColumn = preservedColumn;
        this.preservedIndent = preservedIndent;
    }

    @Override
    public String toString() {
        return "LineSelectionState{" +
                "anchorPosition=" + anchorPosition +
                ", isLine=" + isLine +
                ", hadSelection=" + hadSelection +
                ", hadLineSelection=" + hadLineSelection +
                ", isStartAnchor=" + isStartAnchor +
                ", preservedColumn=" + preservedColumn +
                ", preservedIndent=" + preservedIndent +
                '}';
    }
}
