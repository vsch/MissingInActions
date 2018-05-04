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

import com.intellij.openapi.editor.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("WeakerAccess")
public class EditorCaretState extends CaretState {
    final @NotNull EditorPositionFactory myFactory;

    public EditorCaretState(@NotNull EditorPositionFactory factory, @Nullable LogicalPosition position, @Nullable LogicalPosition start, @Nullable LogicalPosition end) {
        super(position, start, end);
        myFactory = factory;
    }

    @NotNull
    public EditorPositionFactory getFactory() {
        return myFactory;
    }

    @NotNull
    public Editor getEditor() {
        return myFactory.getEditor();
    }

    @NotNull
    public Document getDocument() {
        return myFactory.getEditor().getDocument();
    }

    public EditorCaretState(@NotNull EditorPositionFactory factory, @NotNull Caret caret) {
        this(factory, caret.getLogicalPosition()
                , caret.hasSelection() ? factory.fromOffset(caret.getSelectionStart()) : null
                , caret.hasSelection() ? factory.fromOffset(caret.getSelectionEnd()) : null
        );
    }

    public EditorCaretState(@NotNull EditorPositionFactory factory, @NotNull CaretState other) {
        this(factory, other.getCaretPosition(), other.getSelectionStart(), other.getSelectionEnd());
    }

    public boolean hasSelection() {
        return getSelectionStart() != null && getSelectionEnd() != null && getSelectionStart().getOffset() != getSelectionEnd().getOffset();
    }

    @Nullable
    @Override
    public EditorPosition getCaretPosition() {
        return myFactory.fromPosition(super.getCaretPosition());
    }

    @Nullable
    @Override
    public EditorPosition getSelectionStart() {
        return myFactory.fromPosition(super.getSelectionStart());
    }

    @Nullable
    @Override
    public EditorPosition getSelectionEnd() {
        return myFactory.fromPosition(super.getSelectionEnd());
    }

    @NotNull
    public EditorCaretState atColumns(@Nullable CaretState otherColumns) {
        return otherColumns == null ? this : new EditorCaretState(myFactory
                , atColumn(getCaretPosition(), otherColumns.getCaretPosition())
                , atColumn(getSelectionStart(), otherColumns.getSelectionStart())
                , atColumn(getSelectionEnd(), otherColumns.getSelectionEnd())
        );
    }

    @NotNull
    public EditorCaretState atColumn(@Nullable LogicalPosition otherColumn) {
        return otherColumn == null ? this : new EditorCaretState(myFactory
                , atColumn(getCaretPosition(), otherColumn)
                , getSelectionStart()
                , getSelectionEnd()
        );
    }

    @NotNull
    public EditorCaretState atColumns(
            @Nullable LogicalPosition otherColumn
            , @Nullable LogicalPosition startColumn
            , @Nullable LogicalPosition endColumn
    ) {
        return otherColumn == null && startColumn == null && endColumn == null
                ? this : new EditorCaretState(myFactory
                , atColumn(getCaretPosition(), otherColumn)
                , atColumn(getSelectionStart(), startColumn)
                , atColumn(getSelectionEnd(), endColumn)
        );
    }

    @NotNull
    public EditorCaretState onLines(@Nullable CaretState otherLine) {
        return otherLine == null ? this : new EditorCaretState(myFactory
                , onLine(getCaretPosition(), otherLine.getCaretPosition())
                , onLine(getSelectionStart(), otherLine.getSelectionStart())
                , onLine(getSelectionEnd(), otherLine.getSelectionEnd())
        );
    }

    @NotNull
    public EditorCaretState onLine(@Nullable LogicalPosition otherLine) {
        return otherLine == null ? this : new EditorCaretState(myFactory
                , onLine(getCaretPosition(), otherLine)
                , getSelectionStart()
                , getSelectionEnd()
        );
    }

    @NotNull
    public EditorCaretState onLines(
            @Nullable LogicalPosition otherLine
            , @Nullable LogicalPosition startLine
            , @Nullable LogicalPosition endLine
    ) {
        return otherLine == null && startLine == null && endLine == null
                ? this : new EditorCaretState(myFactory
                , atColumn(getCaretPosition(), otherLine)
                , atColumn(getSelectionStart(), startLine)
                , atColumn(getSelectionEnd(), endLine)
        );
    }

    @Nullable
    public static LogicalPosition atColumn(@Nullable EditorPosition position, @Nullable LogicalPosition otherColumn) {
        return position == null ? otherColumn : otherColumn == null ? position : position.atColumn(otherColumn.column);
    }

    @Nullable
    public static LogicalPosition onLine(@Nullable EditorPosition position, @Nullable LogicalPosition otherLine) {
        return position == null ? otherLine : otherLine == null ? position : position.onLine(otherLine.line);
    }
}
