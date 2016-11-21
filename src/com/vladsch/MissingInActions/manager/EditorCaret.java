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

import com.intellij.openapi.editor.*;
import com.vladsch.MissingInActions.util.EditHelpers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a caret in the editor with line selection type and anchor position
 * <p>
 * The stored selection is always in its character variant so that we have the right anchor position
 * <p>
 * However the returned values are adjusted to reflect the line selection mode of the caret
 */
@SuppressWarnings("WeakerAccess")
public class EditorCaret {
    final private EditorPositionFactory myFactory;
    final private @NotNull EditorPosition myCaretPosition;
    final private @NotNull EditorPosition mySelectionStart;
    final private @NotNull EditorPosition mySelectionEnd;
    final private boolean myStartIsAnchor;
    final private boolean myIsLine;

    // @formatter:off
    public EditorCaret withCaretPosition(@NotNull EditorPosition myCaretPosition) { return new EditorCaret(myFactory, myCaretPosition, mySelectionStart, mySelectionEnd, myStartIsAnchor, myIsLine); }
    public EditorCaret withSelectionStart(@NotNull EditorPosition mySelectionStart) { return new EditorCaret(myFactory, myCaretPosition, mySelectionStart, mySelectionEnd, myStartIsAnchor, myIsLine); }
    public EditorCaret withSelectionEnd(@NotNull EditorPosition mySelectionEnd) { return new EditorCaret(myFactory, myCaretPosition, mySelectionStart, mySelectionEnd, myStartIsAnchor, myIsLine); }
    public EditorCaret withStartIsAnchor(boolean myStartIsAnchor) { return new EditorCaret(myFactory, myCaretPosition, mySelectionStart, mySelectionEnd, myStartIsAnchor, myIsLine); }
    public EditorCaret withIsLine(boolean myIsLine) { return new EditorCaret(myFactory, myCaretPosition, mySelectionStart, mySelectionEnd, myStartIsAnchor, myIsLine); }
    
    public EditorCaret withSelection(@NotNull EditorPosition mySelectionStart, @NotNull EditorPosition mySelectionEnd) { return new EditorCaret(myFactory, myCaretPosition, mySelectionStart, mySelectionEnd, myStartIsAnchor, myIsLine); }
    // @formatter:on

    /**
     * @param factory        editor caret factory
     * @param caretPosition  caret position
     * @param selectionStart non-line selection start
     * @param selectionEnd   non-line selection end
     * @param startIsAnchor  true if lead offset is at start of selection
     * @param isLine         this selection is as a line selection (start/end will be converted to their line mode values)
     */
    public EditorCaret(@NotNull EditorPositionFactory factory, @NotNull LogicalPosition caretPosition, @NotNull LogicalPosition selectionStart, @NotNull LogicalPosition selectionEnd, boolean startIsAnchor, boolean isLine) {
        myFactory = factory;
        myCaretPosition = myFactory.fromPosition(caretPosition);
        mySelectionStart = myFactory.fromPosition(selectionStart);
        mySelectionEnd = myFactory.fromPosition(selectionEnd);
        myStartIsAnchor = startIsAnchor;
        myIsLine = isLine;
    }

    public EditorCaret(@NotNull LineSelectionManager manager, @NotNull LogicalPosition caretPosition, @NotNull LogicalPosition selectionStart, @NotNull LogicalPosition selectionEnd, boolean startIsAnchor, boolean isLine) {
        this(manager.getPositionFactory(), caretPosition, selectionStart, selectionEnd, startIsAnchor, isLine);
    }

    public EditorCaret(@NotNull LineSelectionManager manager, @NotNull Caret caret) {
        this(manager.getPositionFactory(), caret, manager.getSelectionState(caret));
    }

    public EditorCaret(@NotNull EditorPositionFactory factory, @NotNull Caret caret, @NotNull LineSelectionState state) {
        this(factory, caret, state.getAnchorOffset(0), state.getAnchorOffset(0) <= caret.getOffset(), state.isLine());
    }

    public EditorCaret(@NotNull EditorPositionFactory factory, @NotNull Caret caret, int anchorOffset, boolean startIsAnchor, boolean isLine) {
        myFactory = factory;
        myCaretPosition = myFactory.fromPosition(caret.getLogicalPosition());

        if (!caret.hasSelection()) {
            myIsLine = false;
            myStartIsAnchor = true;
            mySelectionStart = myCaretPosition;
            mySelectionEnd = myCaretPosition;
            LineSelectionState state = myFactory.getManager().getSelectionState(caret);
            state.reset();
            caret.setSelection(caret.getOffset(), caret.getOffset());
        } else {
            if (isLine) {
                if (startIsAnchor) {
                    mySelectionStart = myFactory.fromOffset(anchorOffset);
                    mySelectionEnd = myFactory.fromOffset(caret.getSelectionEnd());
                    myStartIsAnchor = true;
                } else {
                    mySelectionStart = myFactory.fromOffset(caret.getSelectionStart());
                    mySelectionEnd = myFactory.fromOffset(anchorOffset);
                    myStartIsAnchor = false;
                }
                myIsLine = true;
            } else {
                mySelectionStart = myFactory.fromOffset(caret.getSelectionStart());
                mySelectionEnd = myFactory.fromOffset(caret.getSelectionEnd());
                myStartIsAnchor = caret.getSelectionStart() == caret.getLeadSelectionOffset();
                myIsLine = false;
            }
        }
    }

    public EditorPositionFactory getFactory() {
        return myFactory;
    }

    public boolean isStartIsAnchor() {
        return myStartIsAnchor;
    }

    public void copyPositionTo(@NotNull Caret caret) {
        if (!myCaretPosition.equals(caret.getLogicalPosition())) {
            myFactory.getManager().guard(() -> {
                caret.moveToLogicalPosition(myCaretPosition);
                EditHelpers.scrollToCaret(myFactory.getEditor());
            });
        }
    }

    public void copySelectionTo(@NotNull Caret caret) {
        myFactory.getManager().guard(() -> {
            boolean hasSelection = hasSelection();
            int offset = caret.getOffset();
            int startOffset = hasSelection ? getSelectionStart().getOffset() : offset;
            int endOffset = hasSelection ? getSelectionEnd().getOffset() : offset;

            if (caret.getSelectionStart() != startOffset || caret.getSelectionEnd() != endOffset) {
                caret.setSelection(startOffset, endOffset);
            }

            saveSelectionStateFor(caret);
        });
    }

    public void saveSelectionStateFor(@NotNull Caret caret) {
        LineSelectionState state = myFactory.getManager().getSelectionState(caret);
        if (hasSelection()) {
            state.setLine(myIsLine);
            state.setAnchorOffsets(myStartIsAnchor ? mySelectionStart.getOffset() : mySelectionEnd.getOffset());
        } else {
            state.reset();
            caret.setSelection(caret.getOffset(), caret.getOffset());
        } 
    }

    public void copyTo(@NotNull Caret caret) {
        myFactory.getManager().guard(() -> {
            copyPositionTo(caret);
            copySelectionTo(caret);
        });
    }

    public boolean startIsAnchor() {
        return myStartIsAnchor;
    }

    public boolean isLine() {
        return myIsLine;
    }

    public boolean hasSelection() {
        return !mySelectionStart.equals(mySelectionEnd);
    }

    @NotNull
    public EditorPosition getCaretPosition() {
        return myCaretPosition;
    }

    @NotNull
    public EditorPosition getCharSelectionStart() {
        return mySelectionStart;
    }

    @NotNull
    public EditorPosition getSelectionStart() {
        return myIsLine ? mySelectionStart.atStartOfLine() : mySelectionStart;
    }

    @NotNull
    public EditorPosition getCharSelectionEnd() {
        return mySelectionEnd;
    }

    @NotNull
    public EditorPosition getSelectionEnd() {
        return myIsLine ? mySelectionEnd.atEndOfLine() : mySelectionEnd;
    }

    @NotNull
    public EditorCaret atColumns(@Nullable CaretState otherColumns) {
        return otherColumns == null ? this
                : new EditorCaret(myFactory
                , atColumn(getCaretPosition(), otherColumns.getCaretPosition())
                , atColumn(getCharSelectionStart(), otherColumns.getSelectionStart())
                , atColumn(getCharSelectionEnd(), otherColumns.getSelectionEnd())
                , myStartIsAnchor
                , myIsLine
        );
    }

    @NotNull
    public EditorCaret atColumn(@Nullable LogicalPosition otherColumn) {
        return otherColumn == null ? this : new EditorCaret(myFactory
                , atColumn(myCaretPosition, otherColumn)
                , mySelectionStart
                , mySelectionEnd
                , myStartIsAnchor
                , myIsLine
        );
    }

    @NotNull
    public EditorCaret atColumn(int otherColumn) {
        return new EditorCaret(myFactory
                , myCaretPosition.atColumn(otherColumn)
                , mySelectionStart
                , mySelectionEnd
                , myStartIsAnchor
                , myIsLine
        );
    }

    @NotNull
    public EditorCaret atColumns(@Nullable LogicalPosition otherColumn
            , @Nullable LogicalPosition startColumn
            , @Nullable LogicalPosition endColumn) {
        return otherColumn == null && startColumn == null && endColumn == null
                ? this : new EditorCaret(myFactory
                , atColumn(myCaretPosition, otherColumn)
                , atColumn(mySelectionStart, startColumn)
                , atColumn(mySelectionEnd, endColumn)
                , myStartIsAnchor
                , myIsLine
        );
    }

    @NotNull
    public EditorCaret onLines(@Nullable CaretState otherLine) {
        return otherLine == null ? this : new EditorCaret(myFactory
                , onLine(myCaretPosition, otherLine.getCaretPosition())
                , onLine(mySelectionStart, otherLine.getSelectionStart())
                , onLine(mySelectionEnd, otherLine.getSelectionEnd())
                , myStartIsAnchor
                , myIsLine
        );
    }

    @NotNull
    public EditorCaret onLine(@Nullable LogicalPosition otherLine) {
        return otherLine == null ? this : new EditorCaret(myFactory
                , onLine(myCaretPosition, otherLine)
                , mySelectionStart
                , mySelectionEnd
                , myStartIsAnchor
                , myIsLine
        );
    }

    @NotNull
    public EditorCaret onLines(@Nullable LogicalPosition otherLine
            , @Nullable LogicalPosition startLine
            , @Nullable LogicalPosition endLine) {
        return otherLine == null && startLine == null && endLine == null
                ? this : new EditorCaret(myFactory
                , atColumn(myCaretPosition, otherLine)
                , atColumn(mySelectionStart, startLine)
                , atColumn(mySelectionEnd, endLine)
                , myStartIsAnchor
                , myIsLine
        );
    }

    @NotNull
    public EditorCaret withNormalizedCharSelectionPosition() {
        if (hasSelection()) {
            EditorPosition newPos;

            if (myIsLine) {
                newPos = myStartIsAnchor ? mySelectionEnd : mySelectionStart;
            } else {
                newPos = myStartIsAnchor ? getSelectionEnd() : getSelectionStart();
            }

            if (!newPos.equals(myCaretPosition)) {
                return new EditorCaret(myFactory, newPos, mySelectionStart, mySelectionEnd, myStartIsAnchor, myIsLine);
            }
        }
        return this;
    }

    @NotNull
    public EditorCaret withNormalizedPosition() {
        if (myIsLine) {
            EditorPosition start = getSelectionStart();
            EditorPosition end = getSelectionEnd();
            EditorPosition newPos;

            if (myStartIsAnchor) {
                // mySelectionStart is anchor move caret to mySelectionEnd
                if (myFactory.getManager().getCaretInSelection()) {
                    newPos = myCaretPosition.onLine(end.line - 1);
                } else {
                    newPos = myCaretPosition.onLine(end.line);
                }
            } else {
                // mySelectionEnd is anchor move caret to mySelectionStart
                if (myFactory.getManager().getCaretInSelection()) {
                    newPos = myCaretPosition.onLine(start.line);
                } else {
                    newPos = myCaretPosition.onLine(start.line > 0 ? start.line - 1 : start.line);
                }
            }

            if (!newPos.equals(myCaretPosition)) {
                return new EditorCaret(myFactory, newPos, mySelectionStart, mySelectionEnd, myStartIsAnchor, myIsLine);
            }
        }
        return this;
    }

    @SuppressWarnings("SameParameterValue")
    public EditorCaret toLineSelection() {
        return toLineSelection(true);
    }

    @SuppressWarnings("SameParameterValue")
    public EditorCaret toLineSelection(boolean alwaysLine) {
        EditorCaret result = this;

        if (hasSelection() && !isLine()) {
            if (mySelectionStart.line != mySelectionEnd.line || alwaysLine) {
                result = new EditorCaret(myFactory, myCaretPosition, mySelectionStart, mySelectionEnd, myStartIsAnchor, true);
            }
        }
        return result;
    }

    @SuppressWarnings("SameParameterValue")
    public EditorCaret toTrimmedOrExpandedLineSelection() {
        return toLineSelection(true, true);
    }

    @SuppressWarnings("SameParameterValue")
    public EditorCaret toLineSelection(boolean alwaysLine, boolean trimOrExpandToFullLines) {
        EditorCaret result = this;

        if (trimOrExpandToFullLines) {
            result = result.toTrimmedOrExpandedFullLines();
        }
        result = result.toLineSelection(alwaysLine);
        return result;
    }

    @NotNull
    public EditorCaret toTrimmedOrExpandedFullLines() {
        EditorCaret result = this;

        if (hasSelection() && mySelectionStart.line != mySelectionEnd.line) {
            if (mySelectionStart.column != 0 || mySelectionEnd.column != 0) {
                EditorPosition newStart = mySelectionStart.toTrimmedOrExpandedFullLine();
                EditorPosition newEnd = mySelectionEnd.toTrimmedOrExpandedFullLine();

                if (!mySelectionStart.equals(newStart) || !mySelectionEnd.equals(newEnd)) {
                    result = new EditorCaret(myFactory, myCaretPosition, newStart, newEnd, myStartIsAnchor, myIsLine);
                }
            }
        }
        return result;
    }

    public EditorCaret toCharSelection() {
        EditorCaret result = this;

        if (myIsLine) {
            result = new EditorCaret(myFactory, myCaretPosition, mySelectionStart, mySelectionEnd, myStartIsAnchor, false);
        }
        return result;
    }

    public EditorCaret withSelectionPosition(int line, int column) {
        if (myCaretPosition.line != line || myCaretPosition.column != column) {
            return new EditorCaret(myFactory, myFactory.fromPosition(line, column), mySelectionStart, mySelectionEnd, myStartIsAnchor, myIsLine);
        }
        return this;
    }

    public EditorCaret withSelectionStart(int line, int column) {
        if (mySelectionStart.line != line || mySelectionStart.column != column) {
            return new EditorCaret(myFactory, myCaretPosition, myFactory.fromPosition(line, column), mySelectionEnd, myStartIsAnchor, myIsLine);
        }
        return this;
    }

    public EditorCaret withSelectionEnd(int line, int column) {
        if (mySelectionEnd.line != line || mySelectionEnd.column != column) {
            return new EditorCaret(myFactory, myCaretPosition, mySelectionStart, myFactory.fromPosition(line, column), myStartIsAnchor, myIsLine);
        }
        return this;
    }

    public EditorCaret withSelectionStart(int startOffset) {
        return withSelection(startOffset, mySelectionEnd.getOffset());
    }

    public EditorCaret withSelectionEnd(int endOffset) {
        return withSelection(mySelectionStart.getOffset(), endOffset);
    }

    public EditorCaret withSelection(int startOffset, int endOffset) {
        EditorCaret result = this;

        if (startOffset != mySelectionStart.getOffset() || endOffset != mySelectionEnd.getOffset()) {
            result = new EditorCaret(myFactory, myCaretPosition, myFactory.fromOffset(startOffset), myFactory.fromOffset(endOffset), myStartIsAnchor, false);
        }
        return result;
    }

    @Override
    public String toString() {
        return "EditorCaret{" +
                "myCaretPosition=" + myCaretPosition +
                ", mySelectionStart=" + mySelectionStart +
                ", mySelectionEnd=" + mySelectionEnd +
                ", getSelectionStart=" + getSelectionStart() +
                ", getSelectionEnd=" + getSelectionEnd() +
                ", myIsLine=" + myIsLine +
                ", myStartIsAnchor=" + myStartIsAnchor +
                '}';
    }

    @NotNull
    public static EditorPosition atColumn(@NotNull EditorPosition position, @Nullable LogicalPosition otherColumn) {
        return otherColumn == null ? position : position.atColumn(otherColumn.column);
    }

    @NotNull
    public static LogicalPosition onLine(@NotNull EditorPosition position, @Nullable LogicalPosition otherLine) {
        return otherLine == null ? position : position.onLine(otherLine.line);
    }

    public boolean hasLines() {
        return mySelectionStart.line < mySelectionEnd.line;
    }

    public int getAnchorOffset() {
        return myStartIsAnchor ? mySelectionStart.getOffset() : mySelectionEnd.getOffset();
    }

    @NotNull
    public EditorCaret withCaretPosition(int offset) {
        return withCaretPosition(myFactory.fromOffset(offset));
    }
}
