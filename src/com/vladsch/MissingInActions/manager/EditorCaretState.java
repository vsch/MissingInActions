// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.manager;

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
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
