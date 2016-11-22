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

import com.intellij.openapi.editor.LogicalPosition;
import com.vladsch.MissingInActions.util.EditHelpers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EditorPosition extends LogicalPosition {
    final private @NotNull EditorPositionFactory myFactory;
    private int myIndentColumn;

    EditorPosition(@NotNull EditorPositionFactory factory, int line, int column) throws IllegalArgumentException {
        super(line, column);
        myFactory = factory;
    }

    EditorPosition(@NotNull EditorPositionFactory factory, @NotNull LogicalPosition other) throws IllegalArgumentException {
        this(factory, other.line, other.column);
    }

    @NotNull
    public EditorPositionFactory getFactory() {
        return myFactory;
    }

    @SuppressWarnings("WeakerAccess")
    public EditorPosition atPosition(int line, int column) {
        if (column < 0) column = 0;
        if (line < 0) line = 0;
        else line = Math.min(line, myFactory.getDocumentLineCount());
        return this.line == line && this.column == column ? this : new EditorPosition(myFactory, line, column);
    }

    public EditorPosition onLine(int line) {
        return atPosition(line, column);
    }

    public EditorPosition atColumn(int column) {
        return atPosition(line, column);
    }

    public EditorPosition atColumn(@Nullable LogicalPosition other) {
        return other == null ? this : atColumn(other.column);
    }

    public EditorPosition onLine(@Nullable LogicalPosition other) {
        return other == null ? this : onLine(other.line);
    }

    public EditorPosition atStartOfLine() {
        return atPosition(line, 0);
    }

    public int getOffset() {
        return myFactory.getOffset(this);
    }

    public EditorPosition atEndOfLine() {
        if (line < myFactory.getDocumentLineCount()) {
            int lineOffset = column > 0 ? 1 : 0;
            return atPosition(line + lineOffset, 0);
        } else {
            return myFactory.getDocumentEndPosition();
        }
    }

    public EditorPosition atStartOfNextLine() {
        if (line < myFactory.getDocumentLineCount()) {
            return atPosition(line + 1, 0);
        } else {
            return myFactory.getDocumentEndPosition();
        }
    }

    @SuppressWarnings("WeakerAccess")
    @NotNull
    public EditorPosition toTrimmedOrExpandedFullLine() {
        EditorPosition result = this;
        if (column != 0) {
            if (column <= getIndentColumn()) {
                // all before start and after end is blank, we can safely convert it by expanding to full lines
                result = atStartOfLine();
            } else if (column >= getTrimmedEndColumn()) {
                // all after start and before end is blank, we can safely convert it to by trimming out the empty first and last line of the selection
                result = atEndOfLine();
            }
        }
        return result;
    }

    public boolean equals(EditorPosition other) {
        return line == other.line && column == other.column;
    }

    public EditorPosition addLine(int i) {
        int lineCount = myFactory.getDocumentLineCount();
        int useLine = line + i > lineCount ? lineCount : line + i < 0 ? 0 : line + i;
        return line != useLine ? onLine(useLine) : this;
    }

    public EditorPosition addColumn(int i) {
        return (i > 0) || (i < 0 && column >= i) ? atColumn(column + i) : this;
    }

    @SuppressWarnings("WeakerAccess")
    public static boolean haveTopLineSelection(@NotNull EditorPosition start, @NotNull EditorPosition end) {
        return start.line == 0 && start.column == 0 && end.column == 0 && end.line > 0;
    }

    public static boolean haveLineSelection(@NotNull EditorPosition start, @NotNull EditorPosition end) {
        return start.column == 0 && end.column == 0 && end.line > start.line;
    }

    public int getIndentColumn() {
        return EditHelpers.countWhiteSpace(myFactory.getEditor().getDocument().getCharsSequence(), atStartOfLine().getOffset(), atStartOfNextLine().getOffset());
    }

    public int getTrimmedEndColumn() {
        CharSequence chars = myFactory.getEditor().getDocument().getCharsSequence();
        int endOfLine = atEndOfLine().getOffset();
        int startOfLine = atStartOfLine().getOffset();

        if (endOfLine < chars.length()) {
            endOfLine--;
        }

        return endOfLine - startOfLine - EditHelpers.countWhiteSpaceReversed(chars, startOfLine, endOfLine);
    }

    @NotNull
    public EditorPosition atTrimmedStart() {
        return column != 0 && column >= getTrimmedEndColumn() ? atEndOfLine() : this;
    }

    @NotNull
    public EditorPosition atTrimmedEnd() {
        return column != 0 && column <= getIndentColumn() ? atStartOfLine() : this;
    }

    public EditorPosition atOffset(int offset) {
        return myFactory.fromOffset(offset);
    }
}
