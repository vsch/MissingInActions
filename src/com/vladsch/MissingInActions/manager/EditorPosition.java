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

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@SuppressWarnings("WeakerAccess")
public class EditorPosition extends LogicalPosition {
    final public static EditorPosition NULL = new EditorPosition(EditorPositionFactory.NULL, 0, 0);
    final private @NotNull EditorPositionFactory myFactory;

    EditorPosition(@NotNull EditorPositionFactory factory, int line, int column) throws IllegalArgumentException {
        super(line, column);
        myFactory = factory;
    }

    EditorPosition(@NotNull EditorPositionFactory factory, @NotNull LogicalPosition other) throws IllegalArgumentException {
        this(factory, other.line, other.column);
    }

    public EditorPosition copy() {
        return new EditorPosition(myFactory, line, column);
    }

    @NotNull
    public EditorPositionFactory getFactory() {
        return myFactory;
    }

    @SuppressWarnings("WeakerAccess")
    @NotNull
    public EditorPosition atPosition(int line, int column) {
        if (column < 0) column = 0;
        if (line < 0) line = 0;
        else line = Math.min(line, myFactory.getDocumentLineCount());
        return this.line == line && this.column == column ? this : new EditorPosition(myFactory, line, column);
    }

    public LineSelectionManager getManager() { return myFactory.getManager(); }

    public int getDocumentTextLength() { return myFactory.getDocumentTextLength(); }

    public int getDocumentLineCount() { return myFactory.getDocumentLineCount(); }

    @NotNull
    public BasedSequence getDocumentChars() { return myFactory.getDocumentChars(); }

    @NotNull
    public EditorPosition onLine(int line) {
        return atPosition(line, column);
    }

    @NotNull
    public EditorPosition atColumn(int column) {
        return atPosition(line, column);
    }

    @NotNull
    public EditorPosition atColumn(@Nullable LogicalPosition other) {
        return other == null ? this : atColumn(other.column);
    }

    @NotNull
    public EditorPosition onLine(@Nullable LogicalPosition other) {
        return other == null ? this : onLine(other.line);
    }

    @NotNull
    public EditorPosition atStartOfLine() {
        return atPosition(line, 0);
    }

    public int getOffset() {
        return myFactory.getOffset(this);
    }

    @NotNull
    public EditorPosition atEndOfLineSelection() {
        if (line < myFactory.getDocumentLineCount()) {
            int lineOffset = column > 0 ? 1 : 0;
            return atPosition(line + lineOffset, 0);
        } else {
            return myFactory.getDocumentEndPosition();
        }
    }

    @NotNull
    public EditorPosition atStartColumn() {
        return atPosition(line, 0);
    }

    @NotNull
    public EditorPosition atTrimmedEndColumn() {
        return atPosition(line, getTrimmedEndColumn());
    }

    @NotNull
    public EditorPosition atEndColumn() {
        return atPosition(line, getEndColumn());
    }

    public boolean isAtBoundary(int flags) {
        // TODO: add code
        return false;
    }

    @NotNull
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
            int indentColumn = getIndentColumn();
            int trimmedEndColumn = getTrimmedEndColumn();
            if (this.column <= indentColumn || indentColumn == trimmedEndColumn) {
                // all before start and after end is blank, we can safely convert it by expanding to full lines
                result = atStartOfLine();
            } else if (this.column >= trimmedEndColumn) {
                // all after start and before end is blank, we can safely convert it to by trimming out the empty first and last line of the selection
                result = atEndOfLineSelection();
            }
        }
        return result;
    }

    @SuppressWarnings("WeakerAccess")
    @NotNull
    public EditorPosition toExpandedOrTrimmedFullLine() {
        EditorPosition result = this;
        if (column != 0) {
            if (column >= getTrimmedEndColumn()) {
                // all after start and before end is blank, we can safely convert it to by trimming out the empty first and last line of the selection
                result = atEndOfLineSelection();
            } else if (column <= getIndentColumn()) {
                // all before start and after end is blank, we can safely convert it by expanding to full lines
                result = atStartOfLine();
            }
        }
        return result;
    }

    public void scrollTo(ScrollType scrollType) {
        myFactory.getEditor().getScrollingModel().scrollTo(this, scrollType);
    }

    public boolean equals(EditorPosition other) {
        return line == other.line && column == other.column;
    }

    @NotNull
    public EditorPosition addLine(int i) {
        int lineCount = myFactory.getDocumentLineCount();
        int useLine = line + i > lineCount ? lineCount : line + i < 0 ? 0 : line + i;
        return line != useLine ? onLine(useLine) : this;
    }

    @NotNull
    public EditorPosition addColumn(int i) {
        return (i > 0) || (i < 0 && column >= i) ? atColumn(column + i) : this;
    }

    @NotNull
    public EditorPosition addLine(int i, @NotNull Function<EditorPosition, EditorPosition> onSame) {
        EditorPosition position = addLine(i);
        if (position == this) return onSame.apply(position);
        else return position;
    }

    @NotNull
    public EditorPosition addColumn(int i, @NotNull Function<EditorPosition, EditorPosition> onSame) {
        EditorPosition position = addColumn(i);
        if (position == this) return onSame.apply(position);
        else return position;
    }

    @SuppressWarnings("WeakerAccess")
    public static boolean haveTopLineSelection(@NotNull EditorPosition start, @NotNull EditorPosition end) {
        return start.line == 0 && start.column == 0 && end.column == 0 && end.line > 0;
    }

    public static boolean haveLineSelection(@NotNull EditorPosition start, @NotNull EditorPosition end) {
        return start.column == 0 && end.column == 0 && end.line > start.line;
    }

    @NotNull
    public Editor getEditor() {
        return myFactory.getEditor();
    }

    @NotNull
    public Document getDocument() {
        return myFactory.getEditor().getDocument();
    }

    public int getIndentColumn() {
        return EditHelpers.countWhiteSpace(myFactory.getEditor().getDocument().getCharsSequence(), atStartOfLine().getOffset(), atStartOfNextLine().getOffset());
    }

    public int getTrimmedEndColumn() {
        CharSequence chars = myFactory.getEditor().getDocument().getCharsSequence();
        int endOfLine = atEndOfLineSelection().getOffset();
        int startOfLine = atStartOfLine().getOffset();

        if (endOfLine < chars.length()) {
            endOfLine--;
        }

        return endOfLine - startOfLine - EditHelpers.countWhiteSpaceReversed(chars, startOfLine, endOfLine);
    }

    public int getEndColumn() {
        Document document = getDocument();
        return line >= document.getLineCount() ? 0 : document.getLineEndOffset(line) - document.getLineStartOffset(line);
    }

    @NotNull
    public EditorPosition atTrimmedStart() {
        return column != 0 && column >= getTrimmedEndColumn() ? atEndOfLineSelection() : this;
    }

    @NotNull
    public EditorPosition atTrimmedEnd() {
        return column != 0 && column <= getIndentColumn() ? atStartOfLine() : this;
    }

    @NotNull
    public EditorPosition atOffset(int offset) {
        return offset == getOffset() && this.column <= atEndColumn().column ? this : myFactory.fromOffset(offset);
    }

    public EditorPosition atIndentColumn() {
        return atColumn(getIndentColumn());
    }

    public int ensureRealSpaces() {
        return EditHelpers.ensureRealSpaces(this);
    }

    public boolean isBlankLine() {
        return atIndentColumn().column >= atEndColumn().column;
    }
}
