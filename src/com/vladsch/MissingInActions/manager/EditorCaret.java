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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.vladsch.MissingInActions.util.CaretSnapshot;
import com.vladsch.MissingInActions.util.EditorCaretSnapshot;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.diagnostic.Logger.getInstance;

/**
 * Represents a caret in the editor with line selection type and anchor position
 * <p>
 * The stored selection is always in its character variant, when in line mode the original
 * anchored (leadOffset) is stored as offset.
 */
@SuppressWarnings("WeakerAccess")
public class EditorCaret implements EditorCaretSnapshot {
    private static final Logger logger = getInstance("com.vladsch.MissingInActions.manager");

    final private @NotNull EditorPositionFactory myFactory;
    final private @NotNull Caret myCaret;
    private @NotNull EditorPosition myCaretPosition;
    private @NotNull EditorPosition mySelectionStart;
    private @NotNull EditorPosition mySelectionEnd;
    private int myAnchorColumn;
    private boolean myIsStartAnchor;
    private boolean myIsLine;

    // *
    // * EditorCaretSnapshot
    // *
    @Override
    public boolean isStartAnchor() { return myIsStartAnchor; }

    @Override
    @NotNull
    public EditorPosition getCaretPosition() { return myCaretPosition; }

    @Override
    public boolean isLine() { return myIsLine; }

    @Override
    public boolean hasSelection() {
        return mySelectionStart.line != mySelectionEnd.line
                || mySelectionStart.getOffset() != mySelectionEnd.getOffset();
    }

    @Override
    public boolean hasLines() {
        return getSelectionLineCount() > 0;
    }

    @Override
    @NotNull
    public EditorPosition getSelectionStart() {
        return mySelectionStart;
    }

    @Override
    @NotNull
    public EditorPosition getSelectionEnd() {
        return mySelectionEnd;
    }

    @Override
    public int getSelectionLineCount() {
        int lineCount = mySelectionEnd.line - mySelectionStart.line;

        if (!myIsLine && lineCount > 0 && hasSelection() && mySelectionEnd.column > 0) {
            lineCount++;
        }
        return lineCount;
    }

    @Override
    @NotNull
    public EditorPosition getLineSelectionStart() {
        return mySelectionStart.atStartOfLine();
    }

    @Override
    @NotNull
    public EditorPosition getLineSelectionEnd() {
        return mySelectionEnd.atEndOfLineSelection();
    }

    @Override
    @NotNull
    public EditorPosition getAnchorPosition() {
        return myIsStartAnchor ? mySelectionStart : mySelectionEnd;
    }

    @Override
    @NotNull
    public EditorPosition getAntiAnchorPosition() {
        return !myIsStartAnchor ? mySelectionStart : mySelectionEnd;
    }

    @Override
    public int getColumn() {
        return myCaretPosition.column;
    }

    @Override
    public int getIndent() {
        return myCaretPosition.getIndentColumn();
    }

    @NotNull
    public Caret getCaret() {
        return myCaret;
    }

    public boolean isValid() {
        return myCaret.isValid();
    }

    /**
     * @param factory editor caret factory
     * @param caret   caret
     */
    EditorCaret(@NotNull EditorPositionFactory factory, @NotNull Caret caret, @NotNull LineSelectionState state) {
        myFactory = factory;
        myCaret = caret;

        // first, get caret's view of the world
        myCaretPosition = myFactory.fromPosition(myCaret.getLogicalPosition());
        mySelectionStart = myFactory.fromOffset(myCaret.getSelectionStart());
        mySelectionEnd = myFactory.fromOffset(myCaret.getSelectionEnd());

        // restore anchor offset
        boolean hasSelection = hasSelection();

        if (state.anchorColumn != -1 && hasSelection && mySelectionStart.line != mySelectionEnd.line) {
            myAnchorColumn = state.anchorColumn;
            myIsStartAnchor = state.isStartAnchor;
        } else {
            myIsStartAnchor = myCaret.getLeadSelectionOffset() == myCaret.getSelectionStart();
            myAnchorColumn = myCaretPosition.atOffset(caret.getLeadSelectionOffset()).column;
        }

        myIsLine = hasSelection && mySelectionStart.column == 0 && mySelectionEnd.column == 0;

        if (myCaretPosition.atStartOfLine().getOffset() + 1 == myCaretPosition.atStartOfNextLine().getOffset()) {
            if (!myIsLine) {
                if (myIsStartAnchor || !hasSelection) {
                    if (myCaretPosition.getOffset() == mySelectionEnd.getOffset()) {
                        if (myCaretPosition.column != mySelectionEnd.column) {
                            // on an empty line the selection end offset will always convert to 0 column regardless of the caret column
                            // this makes it look like the selection ends on previous line, when it ends on this one
                            // need to adjust the end to compensate for the blank line behavior
                            mySelectionEnd = myCaretPosition;
                        }
                    }
                } else {
                    if (mySelectionStart.line + 1 == mySelectionEnd.line && mySelectionEnd.atEndColumn().column == 0) {
                        // on an empty line the selection end offset will always convert to 0 column regardless of the caret column
                        // this makes it look like the selection ends on previous line, when it ends on this one
                        // need to adjust the end to compensate for the blank line behavior
                        mySelectionEnd = mySelectionEnd.atStartOfNextLine().atColumn(myAnchorColumn);
                    }
                }
            }
        }
    }

    private EditorCaret(@NotNull EditorCaret other) {
        myFactory = other.myFactory;
        myCaret = other.myCaret;
        myCaretPosition = other.myCaretPosition.copy();
        mySelectionStart = other.mySelectionStart.copy();
        mySelectionEnd = other.mySelectionEnd.copy();
        myAnchorColumn = other.myAnchorColumn;
        myIsStartAnchor = other.myIsStartAnchor;
        myIsLine = other.myIsLine;
    }

    public EditorCaret copy() {
        return new EditorCaret(this);
    }

    @NotNull
    public EditorCaret restoreColumn(int preservedColumn) {
        return restoreColumn(preservedColumn, -1);
    }

    @NotNull
    public EditorCaret restoreColumn(@Nullable CaretSnapshot snapshot) {
        return snapshot == null ? this : restoreColumn(snapshot.getColumn(), -1);
    }

    @NotNull
    public EditorCaret restoreColumn(@NotNull CaretSnapshot snapshot, boolean indentRelative) {
        return restoreColumn(snapshot.getColumn(), indentRelative ? snapshot.getIndent() : -1);
    }

    @NotNull
    public EditorCaret restoreColumn(int preservedColumn, int preservedIndent) {
        if (isUseSoftWraps()) return this;

        if (preservedColumn != -1) {
            if (preservedIndent != -1) {
                // restore indent relative
                if (hasSelection()) {
                    int indentColumn = getAntiAnchorSelectedLineCaretPosition().getIndentColumn();
                    myCaretPosition = myCaretPosition.atColumn(indentColumn + (preservedColumn - preservedIndent));
                } else {
                    int indentColumn = myCaretPosition.getIndentColumn();
                    myCaretPosition = myCaretPosition.atColumn(indentColumn + (preservedColumn - preservedIndent));
                }
            } else {
                myCaretPosition = myCaretPosition.atColumn(preservedColumn);
            }
        }
        return this;
    }

    @NotNull
    public EditorCaret resetAnchorState() {
        myAnchorColumn = -1;
        return this;
    }

    @NotNull
    public EditorCaret setAnchorColumn(@Nullable EditorPosition position) {
        return position == null ? this : setAnchorColumn(position.column);
    }

    @NotNull
    public EditorCaret setAnchorColumn(int column) {
        myAnchorColumn = column;
        return this;
    }

    @Override
    public int getAnchorColumn() {
        return myAnchorColumn;
    }

    @NotNull
    public EditorCaret setIsStartAnchorUpdateAnchorColumn(boolean isStartAnchor) {
        if (myIsStartAnchor != isStartAnchor) {
            if (myIsLine) {
                toCharSelection();
                normalizeCaretPosition();

                setAnchorColumn(getCaretPosition());
                setIsStartAnchor(isStartAnchor);
                normalizeCaretPosition();

                toLineSelection();
            } else {
                setAnchorColumn(getCaretPosition());
                setIsStartAnchor(isStartAnchor);
            }

            normalizeCaretPosition();
        }
        return this;
    }

    /**
     * Save the caret line selection information for later
     */
    public void commitState() {
        myFactory.getManager().setLineSelectionState(myCaret
                , myAnchorColumn
                , myIsStartAnchor
        );
    }

    /**
     * Apply caret information to actual caret and scroll to caret position
     */
    public void commit() {
        commit(true);
    }

    /**
     * Apply caret information to actual caret and scroll to caret position
     * <p>
     * if selection is marked as line type then selection start and end will be extended to full lines and
     * caret position will be changed to its normalized caret position for selection extension
     * options.
     *
     * @param scrollToCaret if true then scroll to caret position of actual caret differs
     */
    @SuppressWarnings("SameParameterValue")
    public void commit(boolean scrollToCaret) {
        if (myCaret.isValid()) {
            myFactory.getManager().guard(() -> {
                // validate caret first
                if (myCaret.isValid()) {
                    if (!myCaretPosition.equals(myCaret.getLogicalPosition())) {
                        myCaret.moveToLogicalPosition(myCaretPosition);
                        if (scrollToCaret) scrollToCaret();
                    }

                    if (myIsLine && (mySelectionStart.column != 0 || mySelectionEnd.column != 0)) {
                        mySelectionStart = getLineSelectionStart();
                        mySelectionEnd = getLineSelectionEnd();
                        normalizeCaretPosition();
                    }

                    int startOffset = mySelectionStart.getOffset();
                    int endOffset = mySelectionEnd.getOffset();
                    if (startOffset != myCaret.getSelectionStart() || endOffset != myCaret.getSelectionEnd()) {
                        myCaret.setSelection(startOffset, endOffset);
                    }

                    commitState();
                }
            });
        }
    }

    /**
     * Scroll to caret position if it is the primary caret
     */
    public void scrollToCaret() {
        if (myCaret.isValid()) {
            Editor editor = myFactory.getEditor();
            if (myCaret == editor.getCaretModel().getPrimaryCaret()) {
                editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
            }
        }
    }

    /**
     * Remove selection from this caret and reset its state to not line type
     *
     * @return this
     */
    @Override
    @NotNull
    public EditorCaret removeSelection() {
        mySelectionStart = myCaretPosition;
        mySelectionEnd = myCaretPosition;
        myIsLine = false;
        myIsStartAnchor = true;
        return this;
    }

    /**
     * Convert to line selection if it is not one already and if it spans more than one line
     *
     * @return this
     */
    @NotNull
    public EditorCaret toLineSelectionIfHasLines() {
        if (!myIsLine && mySelectionStart.line != mySelectionEnd.line) {
            toLineSelection();
        }
        return this;
    }

    /**
     * This restores the original character selection from this line selection
     * can be obtained by extending it to full lines
     *
     * @return this
     */
    @NotNull
    public EditorCaret toCharSelection() {
        if (myIsLine) {
            mySelectionStart = getCharSelectionStart();
            mySelectionEnd = getCharSelectionEnd();
            myIsLine = false;
        }
        return this;
    }

    /**
     * inverse of getLineSelectionStart
     *
     * @return char selection start
     */
    @NotNull
    private EditorPosition getCharSelectionStart() {
        if (myIsLine) {
            if (!myIsStartAnchor) {
                return mySelectionStart.atColumn(myCaretPosition);
            } else {
                return mySelectionStart.atColumn(myAnchorColumn);
            }
        } else {
            return mySelectionStart;
        }
    }

    @NotNull
    private EditorPosition getCharSelectionEnd() {
        if (myIsLine) {
            if (myIsStartAnchor) {
                return myCaretPosition.column == 0 ? mySelectionEnd.atStartOfLine() : mySelectionEnd.addLine(-1).atColumn(myCaretPosition);
            } else {
                return myAnchorColumn == 0 ? mySelectionEnd.atStartOfLine() : mySelectionEnd.addLine(-1).atColumn(myAnchorColumn);
            }
        } else {
            return mySelectionEnd;
        }
    }

    /**
     * This converts the line selection to its equivalent char selection and looses
     * the character selection from which it was obtained
     *
     * @return this
     */
    @NotNull
    public EditorCaret toLineEquivalentCharSelection() {
        if (myIsLine) {
            mySelectionStart = getLineSelectionStart();
            mySelectionEnd = getLineSelectionEnd();
            myAnchorColumn = 0;
            myIsLine = false;
        }
        return this;
    }

    /**
     * Convert the current selection to line selection,
     * if it is not a line selection will extend start and end to result in a line selection.
     *
     * @return this
     */
    @NotNull
    public EditorCaret toLineSelection() {
        if (!myIsLine && hasSelection()) {
            expandToFullLines();
            myIsLine = true;
        }
        return this;
    }

    /**
     * Convert the current selection to line selection using selection extension rules
     * to expand/trim start/end based on isStartAnchor
     *
     * @return this
     */
    @NotNull
    public EditorCaret toCaretPositionBasedLineSelection() {
        return toCaretPositionBasedLineSelection(null, null);
    }

    @NotNull
    public EditorCaret toCaretPositionBasedLineSelection(@Nullable Boolean isSelectionStartExtended, @Nullable Boolean isSelectionEndExtended) {
        if (myIsStartAnchor) {
            // adjust end
            boolean isEndExtended = isSelectionEndExtended != null ? isSelectionEndExtended : myFactory.getManager().isSelectionEndExtended();
            if (!isEndExtended || myCaretPosition.column == 0) {
                mySelectionEnd = mySelectionEnd.atStartOfLine();
            } else {
                mySelectionEnd = mySelectionEnd.atEndOfLineSelection();
            }
        } else {
            // adjust start
            boolean isStartExtended = isSelectionStartExtended != null ? isSelectionStartExtended : myFactory.getManager().isSelectionStartExtended();
            if (!isStartExtended && myCaretPosition.column != 0) {
                mySelectionStart = mySelectionStart.atStartOfNextLine();
            } else {
                mySelectionStart = mySelectionStart.atStartOfLine();
            }
        }

        myIsLine = true;
        return this;
    }

    /**
     * This restores the character selection for use in line selection extending actions
     *
     * @return this
     */
    @NotNull
    public EditorCaret toCharSelectionForCaretPositionBasedLineSelection() {
        return toCharSelectionForCaretPositionBasedLineSelection(null, null);
    }

    @SuppressWarnings("SameParameterValue")
    @NotNull
    public EditorCaret toCharSelectionForCaretPositionBasedLineSelection(@Nullable Boolean isSelectionStartExtended, @Nullable Boolean isSelectionEndExtended) {
        if (myIsLine) {
            if (myIsStartAnchor) {
                boolean isEndExtended = isSelectionEndExtended != null ? isSelectionEndExtended : myFactory.getManager().isSelectionEndExtended();
                if (isEndExtended && myCaretPosition.column != 0) {
                    mySelectionEnd = mySelectionEnd.addLine(-1).atColumn(myCaretPosition);
                } else {
                    mySelectionEnd = mySelectionEnd.atColumn(myCaretPosition);
                }
                mySelectionStart = getCharSelectionStart();
            } else {
                boolean isStartExtended = isSelectionStartExtended != null ? isSelectionStartExtended : myFactory.getManager().isSelectionStartExtended();
                if (!isStartExtended && myCaretPosition.column != 0) {
                    mySelectionStart = mySelectionStart.atColumn(myCaretPosition).addLine(-1, EditorPosition::atStartOfLine);
                } else {
                    mySelectionStart = mySelectionStart.atColumn(myCaretPosition);
                }
                mySelectionEnd = getCharSelectionEnd();
            }

            if (mySelectionStart.line > mySelectionEnd.line || (mySelectionStart.line == mySelectionEnd.line && mySelectionStart.column > mySelectionEnd.column)) {
                EditorPosition pos = mySelectionStart;
                mySelectionStart = mySelectionEnd;
                mySelectionEnd = pos;
                myIsStartAnchor = !myIsStartAnchor;
                myAnchorColumn = getAnchorPosition().column;
            }
            myIsLine = false;
        }
        return this;
    }

    /**
     * Trim or expand to full lines before forcing to line selection
     *
     * @return this
     */
    @NotNull
    public EditorCaret trimOrExpandToLineSelection() {
        return trimOrExpandToFullLines().toLineSelection();
    }

    public boolean canSafelyTrimOrExpandToFullLineSelection() {
        if (!myIsLine && mySelectionStart.line != mySelectionEnd.line) {
            if (mySelectionStart.column != 0 || mySelectionEnd.column != 0) {
                EditorPosition selectionStart = mySelectionStart.toTrimmedOrExpandedFullLine();
                EditorPosition selectionEnd = mySelectionEnd.toTrimmedOrExpandedFullLine();
                return selectionStart.column == 0 && selectionEnd.column == 0;
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Trim or expand to full lines without changing selection type to line
     *
     * @return this
     */
    @NotNull
    public EditorCaret trimOrExpandToFullLines() {
        if ((mySelectionStart.column != 0 || mySelectionEnd.column != 0) && hasSelection()) {
            mySelectionStart = mySelectionStart.toTrimmedOrExpandedFullLine();
            mySelectionEnd = mySelectionEnd.toTrimmedOrExpandedFullLine();
        }
        return this;
    }

    @NotNull
    public EditorCaret expandToFullLines() {
        if (!myIsLine && hasSelection()) {
            mySelectionStart = getLineSelectionStart();
            mySelectionEnd = getLineSelectionEnd();
        }
        return this;
    }

    //*
    //*  Selection Start/End Expanded Handling
    //*

    /**
     * Set the caret position to reflect the current selection and selection extension options if it is a line selection
     *
     * @return this
     */
    @SuppressWarnings({ "SameParameterValue", "ConstantConditions" })
    @NotNull
    public EditorCaret normalizeCaretPosition() {
        if (myIsLine) {
            if (myIsStartAnchor) {
                // caret on end of selection
                if (myFactory.getManager().isSelectionEndExtended()) {
                    myCaretPosition = myCaretPosition.column == 0 ? myCaretPosition.onLine(mySelectionEnd) : myCaretPosition.onLine(mySelectionEnd.line - 1);
                } else {
                    myCaretPosition = myCaretPosition.onLine(mySelectionEnd);
                }
            } else {
                // caret at top of selection
                if (myFactory.getManager().isSelectionStartExtended()) {
                    myCaretPosition = myCaretPosition.onLine(mySelectionStart);
                } else {
                    if (myCaretPosition.line == 0 && myCaretPosition.column != 0) {
                        myCaretPosition = myCaretPosition.atColumn(0);
                    } else {
                        myCaretPosition = myCaretPosition.column == 0 ? myCaretPosition.onLine(mySelectionStart) : myCaretPosition.onLine(mySelectionStart.line - 1);
                    }
                }
            }
        } else {
            myCaretPosition = !myIsStartAnchor ? mySelectionStart : mySelectionEnd;
        }
        return this;
    }

    /**
     * Get the position for the selected line given by the anti anchor position
     *
     * @return caret position with line adjusted to the selected anti anchor line, column is preserved
     */
    @NotNull
    public EditorPosition getAntiAnchorSelectedLineCaretPosition() {
        if (myIsStartAnchor) {
            // caret on end of selection
            if (myFactory.getManager().isSelectionEndExtended()) {
                return myCaretPosition.column == 0 ? mySelectionEnd.addLine(-1) : mySelectionEnd.atColumn(myCaretPosition);
            } else {
                return mySelectionEnd.addLine(-1).atColumn(myCaretPosition);
            }
        } else {
            // caret at top of selection
            return mySelectionStart.atColumn(myCaretPosition);
            //if (myFactory.getManager().isSelectionStartExtended()) {
            //} else {
            //    return myCaretPosition.column != 0 ? mySelectionStart.addLine(1).atColumn(myCaretPosition) : mySelectionStart.atColumn(myCaretPosition);
            //}
        }
    }

    //*
    //*  Selection/Anchor/AntiAnchor Related Functions
    //*

    @NotNull
    public EditorCaret setAnchorPosition(@Nullable EditorPosition other) {
        if (other != null) {
            if (myIsStartAnchor) {
                mySelectionStart = other;
            } else {
                mySelectionEnd = other;
            }
        }
        return this;
    }

    @NotNull
    public EditorCaret setAntiAnchorPosition(@Nullable EditorPosition other) {
        if (other != null) {
            if (myIsStartAnchor) {
                mySelectionEnd = other;
            } else {
                mySelectionStart = other;
            }
        }
        return this;
    }

    @NotNull
    public EditorCaret setSelectionStart(@Nullable EditorPosition position) {
        if (position != null) {
            mySelectionStart = position;
        }
        return this;
    }

    @NotNull
    public EditorCaret setSelectionEnd(@Nullable EditorPosition position) {
        if (position != null) {
            mySelectionEnd = position;
        }
        return this;
    }

    //*
    //*  Helper functions for getting/setting: line/column/offset
    //*

    @NotNull
    public EditorCaret setCaretPosition(int line, int column) {
        myCaretPosition = myCaretPosition.atPosition(line, column);
        return this;
    }

    @NotNull
    public EditorCaret setCaretPosition(int offset) {
        myCaretPosition = myCaretPosition.atOffset(offset);
        return this;
    }

    @NotNull
    public EditorCaret setCaretPosition(@Nullable EditorPosition position) {
        if (position != null) {
            myCaretPosition = position;
        }
        return this;
    }

    @NotNull
    public EditorCaret setSelectionStart(int line, int column) {
        mySelectionStart = myCaretPosition.atPosition(line, column);
        return this;
    }

    @NotNull
    public EditorCaret setSelectionStart(int offset) {
        mySelectionStart = myCaretPosition.atOffset(offset);
        return this;
    }

    @NotNull
    public EditorCaret setSelectionEnd(int line, int column) {
        mySelectionEnd = myCaretPosition.atPosition(line, column);
        return this;
    }

    @NotNull
    public EditorCaret setSelectionEnd(int offset) {
        mySelectionEnd = myCaretPosition.atOffset(offset);
        return this;
    }

    @NotNull
    public EditorCaret setSelection(int startOffset, int endOffset) {
        mySelectionStart = myCaretPosition.atOffset(startOffset);
        mySelectionEnd = myCaretPosition.atOffset(endOffset);
        return this;
    }

    @NotNull
    public EditorCaret setSelection(EditorPosition startOffset, EditorPosition endOffset) {
        mySelectionStart = startOffset;
        mySelectionEnd = endOffset;
        return this;
    }

    @NotNull
    public EditorCaret atColumn(@Nullable LogicalPosition other) {
        if (other != null) {
            myCaretPosition = myCaretPosition.atColumn(other.column);
        }
        return this;
    }

    @NotNull
    @SuppressWarnings("UnusedReturnValue")
    public EditorCaret atColumn(int other) {
        myCaretPosition = myCaretPosition.atColumn(other);
        return this;
    }

    @NotNull
    public EditorCaret onLine(@Nullable LogicalPosition other) {
        if (other != null) {
            myCaretPosition = myCaretPosition.onLine(other.line);
        }
        return this;
    }

    @NotNull
    public EditorCaret onLine(int other) {
        myCaretPosition = myCaretPosition.onLine(other);
        return this;
    }

    //*
    //*  Simple Getters/Setters
    //*

    @NotNull
    public EditorCaret setIsStartAnchor(boolean isStartAnchor) {
        myIsStartAnchor = isStartAnchor;
        return this;
    }

    public boolean isUseSoftWraps() {
        return myFactory.getEditor().getSettings().isUseSoftWraps();
    }

    @NotNull
    public EditorPositionFactory getFactory() { return myFactory; }

    @NotNull
    public LineSelectionManager getManager() { return myFactory.getManager(); }

    public int getDocumentTextLength() { return myFactory.getDocumentTextLength(); }

    public int getDocumentLineCount() { return myFactory.getDocumentLineCount(); }

    @NotNull
    public BasedSequence getDocumentChars() { return myFactory.getDocumentChars(); }

    @NotNull
    public Editor getEditor() { return myFactory.getEditor(); }

    @NotNull
    public Document getDocument() { return myFactory.getDocument(); }

    @NotNull
    public Project getProject() { return getEditor().getProject(); }

    @NotNull
    public EditorPosition adjustIndentRelative(EditorPosition position, int preservedColumn, int preservedIndent) {
        return adjustIndentRelative(position, position.getIndentColumn(), preservedColumn, preservedIndent);
    }

    public EditorPosition adjustIndentRelative(EditorPosition position, int ourIndent, int preservedColumn, int preservedIndent) {
        return position.atColumn(ourIndent + (preservedColumn - preservedIndent));
    }

    public String toString() {
        return "EditorCaret{" +
                ", anchorPosition=" + myAnchorColumn +
                ", isLine=" + myIsLine +
                ", caretPosition=" + myCaretPosition +
                ", selectionStart=" + mySelectionStart +
                ", selectionEnd=" + mySelectionEnd +
                ", isStartAnchor=" + myIsStartAnchor +
                '}';
    }
}
