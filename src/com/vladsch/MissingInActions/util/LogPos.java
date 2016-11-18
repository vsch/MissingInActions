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

package com.vladsch.MissingInActions.util;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LogPos extends LogicalPosition {
    final private @NotNull Factory myFactory;

    private LogPos(@NotNull Factory factory, int line, int column) throws IllegalArgumentException {
        super(line, column);
        myFactory = factory;
    }

    private LogPos(@NotNull Factory factory, @NotNull LogicalPosition other) throws IllegalArgumentException {
        this(factory, other.line, other.column);
    }

    public LogPos with(int line, int column) {
        return this.line == line && this.column == column ? this : new LogPos(myFactory, line, column);
    }

    public LogPos onLine(int line) {
        return with(line, column);
    }

    public LogPos atColumn(int column) {
        return with(line, column);
    }

    public LogPos atColumn(@Nullable LogicalPosition other) {
        return other == null ? this : atColumn(other.column);
    }

    public LogPos onLine(@Nullable LogicalPosition other) {
        return other == null ? this : onLine(other.line);
    }

    public LogPos atStartOfLine() {
        return with(line, 0);
    }

    public LogPos atEndOfLine() {
        if (line < myFactory.getLineCount()) {
            int lineOffset = column > 0 ? 1 : 0;
            return with(line + lineOffset, 0);
        } else {
            return myFactory.getEndPosition();
        }
    }

    public LogPos atEndOfNextLine() {
        if (line < myFactory.getLineCount()) {
            return with(line + 1, 0);
        } else {
            return myFactory.getEndPosition();
        }
    }

    public int toOffset() {
        return myFactory.getEditor().logicalPositionToOffset(this);
    }

    public boolean equals(LogPos other) {
        return line == other.line && column == other.column;
    }

    public LogPos addLine(int i) {
        return (i > 0 && line + i <= myFactory.getLineCount()) || (i < 0 && line >= i) ? onLine(line + i) : this;
    }

    public LogPos addColumn(int i) {
        return (i > 0) || (i < 0 && column >= i) ? atColumn(column + i) : this;
    }

    public static boolean haveTopLineSelection(@NotNull LogPos start, @NotNull LogPos end) {
        return start.line == 0 && start.column == 0 && end.column == 0 && end.line > 0;
    }

    public static boolean haveLineSelection(@NotNull LogPos start, @NotNull LogPos end) {
        return start.column == 0 && end.column == 0 && end.line > start.line; 
    }

    public static class Factory {
        final private Editor myEditor;
        private LogPos myEndPosition = null;

        private Factory(Editor editor) {
            myEditor = editor;
        }

        public LogPos fromPos(@Nullable LogicalPosition other) {
            return other == null ? null : other instanceof LogPos ? (LogPos) other : new LogPos(this, other);
        }

        public LogPos fromLine(int line, int column) {
            return new LogPos(this, line, column);
        }

        public LogPos fromOffset(int offset) {
            return new LogPos(this, myEditor.offsetToLogicalPosition(offset));
        }

        public Editor getEditor() {
            return myEditor;
        }

        public LogPos getEndPosition() {
            if (myEndPosition == null) {
                myEndPosition = new LogPos(this, myEditor.offsetToLogicalPosition(myEditor.getDocument().getTextLength()));
            }
            return myEndPosition;
        }

        public int getLineCount() {
            return getEndPosition().line;
        }
    }

    public static Factory factory(Editor editor) {
        return new Factory(editor);
    }
}
