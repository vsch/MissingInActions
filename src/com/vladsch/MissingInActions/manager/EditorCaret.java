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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
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
public class EditorCaret {
    private static final Logger logger = getInstance("com.vladsch.MissingInActions.manager");

    final private EditorPositionFactory myFactory;
    final private Caret myCaret;
    private boolean myHadSelection;
    private boolean myHadLineSelection;
    private boolean myNextHadSelection;
    private boolean myNextHadLineSelection;
    private @NotNull EditorPosition myCaretPosition;
    private @NotNull EditorPosition mySelectionStart;
    private @NotNull EditorPosition mySelectionEnd;
    private @NotNull EditorPosition myAnchorPosition;
    private boolean myIsStartAnchor;
    private boolean myIsLine;
    private int myPreservedColumn;
    private int myPreservedIndent;
    private boolean myAnchorReset;

    /**
     * @param factory editor caret factory
     * @param caret   caret
     */
    EditorCaret(@NotNull EditorPositionFactory factory, @NotNull Caret caret, @NotNull LineSelectionState state) {
        myFactory = factory;
        myCaret = caret;
        myHadSelection = state.hadSelection;
        myHadLineSelection = state.hadLineSelection;
        myAnchorReset = false;

        // first, get caret's view of the world
        myCaretPosition = myFactory.fromPosition(caret.getLogicalPosition());
        mySelectionStart = myFactory.fromOffset(myCaret.getSelectionStart());
        mySelectionEnd = myFactory.fromOffset(myCaret.getSelectionEnd());

        // restore anchor offset
        boolean hasSelection = hasSelection();

        if (state.anchorPosition != null && hasSelection) {
            myAnchorPosition = state.anchorPosition;
            myIsStartAnchor = state.isStartAnchor;
        } else {
            myIsStartAnchor = myCaret.getLeadSelectionOffset() == myCaret.getSelectionStart();
            myAnchorPosition = myCaretPosition.atOffset(caret.getLeadSelectionOffset());
        }

        myIsLine = hasSelection && mySelectionStart.column == 0 && mySelectionEnd.column == 0;
        myNextHadSelection = hasSelection;
        myNextHadLineSelection = myIsLine;

        if (myCaretPosition.atStartOfLine().getOffset() + 1 == myCaretPosition.atStartOfNextLine().getOffset()) {
            if (!myIsLine && (myIsStartAnchor || !hasSelection)) {
                if (myCaretPosition.getOffset() == mySelectionEnd.getOffset()) {
                    if (myCaretPosition.column != mySelectionEnd.column) {
                        // on an empty line the selection end offset will always convert to 0 column regardless of the caret column
                        // this makes it look like the selection ends on previous line, when it ends on this one
                        // need to adjust the end to compensate for the blank line behavior
                        mySelectionEnd = myCaretPosition;
                    }
                }
            }

            if (!myIsLine && (!myIsStartAnchor || !hasSelection)) {
                if (myCaretPosition.getOffset() == mySelectionStart.getOffset()) {
                    if (myCaretPosition.column != mySelectionStart.column) {
                        // on an empty line the selection start offset will always convert to 0 column regardless of the caret column
                        // this makes it look like the selection ends on previous line, when it ends on this one
                        // need to adjust the end to compensate for the blank line behavior
                        mySelectionStart = myCaretPosition;
                    }
                }
            }
        }

        myPreservedColumn = -1;
        myPreservedIndent = -1;

        if (state.preservedColumn != -1) {
            // can restore column
            int desiredColumn;

            if (state.preservedIndent != -1) {
                // restore indent relative
                if (hasSelection) {
                    int indentColumn = getAntiAnchorSelectedLineCaretPosition().getIndentColumn();
                    desiredColumn = indentColumn + (state.preservedColumn - state.preservedIndent);
                    myCaretPosition = myCaretPosition.atColumn(desiredColumn);
                } else {
                    int indentColumn = myCaretPosition.getIndentColumn();
                    desiredColumn = indentColumn + (state.preservedColumn - state.preservedIndent);
                    myCaretPosition = myCaretPosition.atColumn(desiredColumn);
                }
            } else {
                myCaretPosition = myCaretPosition.atColumn(state.preservedColumn);
                desiredColumn = state.preservedColumn;
            }

            if (myIsLine) {
                // may need to adjust column in order to preserve selection based on extension type
                // if we cannot do it, then save the column for next time
                EditorPosition caretPos = myCaretPosition;

                normalizeCaretPosition();
                if (myCaretPosition.column != desiredColumn && desiredColumn != 0) {
                    myPreservedColumn = state.preservedColumn;
                    myPreservedIndent = state.preservedIndent;
                }

                myCaretPosition = caretPos;
            }
        }
    }

    /**
     * Preserve column when re-creating this caret.
     *
     * @param indentRelative true if restored column should be relative to the indent of the anti-anchor selected line
     * @return this
     */
    @SuppressWarnings("SameParameterValue")
    @NotNull
    public EditorCaret preserveColumn(boolean indentRelative) {
        return preserveColumn(myCaretPosition.column, indentRelative);
    }

    /**
     * Preserve caret column when re-creating this caret.
     *
     * @return this
     */
    @NotNull
    public EditorCaret preserveColumn() {
        return preserveColumn(myCaretPosition.column, false);
    }

    /**
     * Preserve column when re-creating this caret.
     *
     * @param preservedColumn the column to restore
     * @param indentRelative  true if restored column should be relative to the indent of the anti-anchor selected line
     * @return this
     */
    @NotNull
    public EditorCaret preserveColumn(int preservedColumn, boolean indentRelative) {
        myPreservedColumn = preservedColumn;
        if (indentRelative) {
            if (hasSelection()) {
                myPreservedIndent = getAntiAnchorSelectedLineCaretPosition().getIndentColumn();
            } else {
                myPreservedIndent = myCaretPosition.getIndentColumn();
            }
        } else {
            myPreservedIndent = -1;
        }
        return this;
    }

    public int getPreservedColumn() {
        return myPreservedColumn;
    }

    public int getPreservedIndent() {
        return myPreservedIndent;
    }

    @NotNull
    public EditorCaret resetAnchorState() {
        myAnchorReset = true;
        return this;
    }

    @NotNull
    public EditorCaret setSavedAnchor(@Nullable EditorPosition anchorPosition) {
        if (anchorPosition == null) {
            myAnchorReset = true;
        } else {
            myAnchorPosition = anchorPosition;
        }
        return this;
    }

    /**
     * Save the caret line selection information for later
     */
    public void commitState() {
        myFactory.getManager().setLineSelectionState(myCaret
                , myAnchorReset ? null : myAnchorPosition
                , myIsStartAnchor
                , myPreservedColumn
                , myPreservedIndent
                , myIsLine
                , myNextHadSelection
                , myNextHadLineSelection
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
        myFactory.getManager().guard(() -> {
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
        });
    }

    /**
     * Scroll to caret position if it is the primary caret
     */
    public void scrollToCaret() {
        Editor editor = myFactory.getEditor();
        if (myCaret == editor.getCaretModel().getPrimaryCaret()) {
            editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        }
    }

    /**
     * Remove selection from this caret and reset its state to not line type
     *
     * @return this
     */
    @NotNull
    public EditorCaret removeSelection() {
        mySelectionStart = myCaretPosition;
        mySelectionEnd = myCaretPosition;
        myIsLine = false;
        myIsStartAnchor = true;
        myPreservedColumn = -1;
        myPreservedIndent = -1;
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
            if (myIsStartAnchor) {
                mySelectionEnd = getCharSelectionEnd();
                mySelectionStart = myAnchorPosition;
            } else {
                mySelectionStart = getCharSelectionStart();
                mySelectionEnd = myAnchorPosition;
            }
            myIsLine = false;
        }
        return this;
    }

    @NotNull
    public EditorPosition getLineSelectionStart() {
        return mySelectionStart.atStartOfLine();
    }

    @NotNull
    public EditorPosition getLineSelectionEnd() {
        return mySelectionEnd.atEndOfLine();
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
                return myAnchorPosition;
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
                return myAnchorPosition;
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
            myAnchorPosition = getAnchorPosition();
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
        if (!myIsLine) {
            if (myIsStartAnchor) {
                // adjust end
                boolean isEndExtended = isSelectionEndExtended != null ? isSelectionEndExtended : myFactory.getManager().isSelectionEndExtended();
                if (!isEndExtended || mySelectionEnd.column == 0) {
                    mySelectionEnd = mySelectionEnd.atStartOfLine();
                } else {
                    mySelectionEnd = mySelectionEnd.atEndOfLine();
                }
            } else {
                // adjust start
                boolean isStartExtended = isSelectionStartExtended != null ? isSelectionStartExtended : myFactory.getManager().isSelectionStartExtended();
                if (!isStartExtended && mySelectionStart.column != 0) {
                    mySelectionStart = mySelectionStart.atStartOfNextLine();
                } else {
                    mySelectionStart = mySelectionStart.atStartOfLine();
                }
            }

            myIsLine = true;
        }
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
                mySelectionStart = myAnchorPosition;
            } else {
                boolean isStartExtended = isSelectionStartExtended != null ? isSelectionStartExtended : myFactory.getManager().isSelectionStartExtended();
                if (!isStartExtended && myCaretPosition.column != 0) {
                    mySelectionStart = mySelectionStart.atColumn(myCaretPosition).addLine(-1, EditorPosition::atStartOfLine);
                } else {
                    mySelectionStart = mySelectionStart.atColumn(myCaretPosition);
                }
                mySelectionEnd = myAnchorPosition;
            }

            if (mySelectionStart.line > mySelectionEnd.line || (mySelectionStart.line == mySelectionEnd.line && mySelectionStart.column > mySelectionEnd.column)) {
                EditorPosition pos = mySelectionStart;
                mySelectionStart = mySelectionEnd;
                mySelectionEnd = pos;
                myIsStartAnchor = !myIsStartAnchor;
                myAnchorPosition = getAnchorPosition();
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
                EditorPosition selectionEnd = mySelectionEnd.toExpandedOrTrimmedFullLine();
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
            mySelectionEnd = mySelectionEnd.toExpandedOrTrimmedFullLine();
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
    public EditorPosition getAnchorPosition() {
        return myIsStartAnchor ? mySelectionStart : mySelectionEnd;
    }

    @NotNull
    public EditorPosition getAntiAnchorPosition() {
        return !myIsStartAnchor ? mySelectionStart : mySelectionEnd;
    }

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
    public EditorPosition getSelectionStart() {
        return mySelectionStart;
    }

    @NotNull
    public EditorPosition getSelectionEnd() {
        return mySelectionEnd;
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

    public int getSelectionLineCount() {
        int lineCount = mySelectionEnd.line - mySelectionStart.line;

        if (!myIsLine && lineCount > 0 && hasSelection() && mySelectionEnd.column > 0) {
            lineCount++;
        }
        return lineCount;
    }

    //*
    //*  Simple Getters/Setters
    //*

    @NotNull
    public EditorCaret setIsStartAnchor(boolean isStartAnchor) {
        myIsStartAnchor = isStartAnchor;
        return this;
    }

    @Nullable
    public EditorPositionFactory getFactory() { return myFactory; }

    public boolean isStartAnchor() { return myIsStartAnchor; }

    @NotNull
    public EditorPosition getCaretPosition() { return myCaretPosition; }

    public boolean isLine() { return myIsLine; }

    public boolean hasSelection() {
        return mySelectionStart.line != mySelectionEnd.line
                || mySelectionStart.getOffset() != mySelectionEnd.getOffset();
    }

    public boolean hadSelection() {
        return myHadSelection;
    }

    public boolean hadLineSelection() {
        return myHadLineSelection;
    }

    public boolean hasLines() {
        return getSelectionLineCount() > 0;
    }

    public EditorPosition adjustIndentRelative(EditorPosition position, int preservedColumn, int preservedIndent) {
        return adjustIndentRelative(position, position.getIndentColumn(), preservedColumn, preservedIndent);
    }

    public EditorPosition adjustIndentRelative(EditorPosition position, int ourIndent, int preservedColumn, int preservedIndent) {
        return position.atColumn(ourIndent + (preservedColumn - preservedIndent));
    }

    public String toString() {
        return "EditorCaret{" +
                ", anchorPosition=" + myAnchorPosition +
                ", isLine=" + myIsLine +
                ", hadSelection=" + myHadSelection +
                ", hadLineSelection=" + myHadLineSelection +
                ", caretPosition=" + myCaretPosition +
                ", selectionStart=" + mySelectionStart +
                ", selectionEnd=" + mySelectionEnd +
                ", isStartAnchor=" + myIsStartAnchor +
                '}';
    }
}
