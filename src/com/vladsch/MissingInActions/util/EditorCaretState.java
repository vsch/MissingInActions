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

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.LogicalPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("WeakerAccess")
public class EditorCaretState extends CaretState {
    final private @NotNull LogPos.Factory myFactory;

    public EditorCaretState(@NotNull LogPos.Factory factory, @Nullable LogicalPosition position, @Nullable LogicalPosition start, @Nullable LogicalPosition end) {
        super(position, start, end);
        myFactory = factory;
    }

    public EditorCaretState(@NotNull LogPos.Factory factory, @NotNull Caret caret) {
        super(caret.getLogicalPosition()
                , caret.hasSelection() ? factory.fromOffset(caret.getSelectionStart()) : null
                , caret.hasSelection() ? factory.fromOffset(caret.getSelectionEnd()) : null
        );
        myFactory = factory;
    }

    public EditorCaretState(@NotNull LogPos.Factory factory, @NotNull CaretState other) {
        super(other.getCaretPosition(), other.getSelectionStart(), other.getSelectionEnd());
        myFactory = factory;
    }

    @Nullable
    @Override
    public LogPos getCaretPosition() {
        return myFactory.fromPos(super.getCaretPosition());
    }

    @Nullable
    @Override
    public LogPos getSelectionStart() {
        return myFactory.fromPos(super.getSelectionStart());
    }

    @Nullable
    @Override
    public LogPos getSelectionEnd() {
        return myFactory.fromPos(super.getSelectionEnd());
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
    public EditorCaretState atColumns(@Nullable LogicalPosition otherColumn
            , @Nullable LogicalPosition startColumn
            , @Nullable LogicalPosition endColumn) {
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
    public EditorCaretState onLines(@Nullable LogicalPosition otherLine
            , @Nullable LogicalPosition startLine
            , @Nullable LogicalPosition endLine) {
        return otherLine == null && startLine == null && endLine == null
                ? this : new EditorCaretState(myFactory
                , atColumn(getCaretPosition(), otherLine)
                , atColumn(getSelectionStart(), startLine)
                , atColumn(getSelectionEnd(), endLine)
        );
    }
    
    @Nullable
    public static LogicalPosition atColumn(@Nullable LogPos position, @Nullable LogicalPosition otherColumn) {
        return position == null ? otherColumn : otherColumn == null ? position : position.atColumn(otherColumn.column);
    }

    @Nullable
    public static LogicalPosition onLine(@Nullable LogPos position, @Nullable LogicalPosition otherLine) {
        return position == null ? otherLine : otherLine == null ? position : position.onLine(otherLine.line);
    }
}
