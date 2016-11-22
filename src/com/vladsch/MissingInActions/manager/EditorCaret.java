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
    final private Caret myCaret;
    private @NotNull EditorPosition myCaretPosition;
    private @NotNull EditorPosition mySelectionStart;
    private @NotNull EditorPosition mySelectionEnd;
    private boolean myIsStartAnchor;
    private boolean myIsLine;

    /**
     * @param factory editor caret factory
     * @param caret   caret
     */
    EditorCaret(@NotNull EditorPositionFactory factory, @NotNull Caret caret, LineSelectionState state) {
        myFactory = factory;
        myCaret = caret;
        myCaretPosition = myFactory.fromPosition(caret.getLogicalPosition());

        if (!caret.hasSelection() && state.isLine()) {
            myIsLine = false;
            myIsStartAnchor = true;
            mySelectionStart = myCaretPosition;
            mySelectionEnd = myCaretPosition;
            factory.getManager().resetSelectionState(myCaret);
        } else {
            if (state.isLine()) {
                if (state.isStartAnchor()) {
                    mySelectionStart = myFactory.fromOffset(state.getAnchorOffset());
                    mySelectionEnd = myCaretPosition;
                    myIsStartAnchor = true;
                } else {
                    mySelectionStart = myCaretPosition;
                    mySelectionEnd = myFactory.fromOffset(state.getAnchorOffset());
                    myIsStartAnchor = false;
                }
                myIsLine = true;
            } else {
                mySelectionStart = myFactory.fromOffset(caret.getSelectionStart());
                mySelectionEnd = myFactory.fromOffset(caret.getSelectionEnd());
                myIsStartAnchor = caret.getSelectionStart() == caret.getLeadSelectionOffset();
                myIsLine = false;
            }
        }
    }

    public void commitPosition() {
        commit(true, false);
    }

    public void commitSelection() {
        commit(false, true);
    }

    private void commitState() {
        if (hasSelection() && isLine()) {
            LineSelectionState state = myFactory.getManager().getSelectionState(myCaret);
            myFactory.getManager().setLineSelectionState(myCaret, myIsStartAnchor ? mySelectionStart.getOffset() : mySelectionEnd.getOffset(), myIsStartAnchor);
        } else {
            myFactory.getManager().resetSelectionState(myCaret);
        }
    }

    public void commit() {
        commit(true, true);
    }

    private void commit(boolean position, boolean selection) {
        myFactory.getManager().guard(() -> {
            if (position) {
                if (!myCaretPosition.equals(myCaret.getLogicalPosition())) {
                    myCaret.moveToLogicalPosition(myCaretPosition);
                    EditHelpers.scrollToCaret(myFactory.getEditor());
                }
            }

            if (selection) {
                boolean hasSelection = hasSelection();
                int offset = myCaret.getOffset();
                int startOffset = hasSelection ? getSelectionStart().getOffset() : offset;
                int endOffset = hasSelection ? getSelectionEnd().getOffset() : offset;

                myCaret.setSelection(startOffset, endOffset);
            }

            commitState();
        });
    }

    @NotNull
    public EditorCaret removeSelection() {
        mySelectionStart = myCaretPosition;
        mySelectionEnd = myCaretPosition;
        myIsLine = false;
        myIsStartAnchor = true;
        commitState();
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

    @NotNull
    public EditorPosition caretPositionToLineSelectionStart(@NotNull EditorPosition position) {
        if (myFactory.getManager().isSelectionExtendsPastCaret()) {
            return position.column == 0 ? position : position.atEndOfLine();
        } else {
            return position.column == 0 ? position : position.atEndOfLine();
        }
    }

    @NotNull
    public EditorPosition caretPositionToLineSelectionEnd(@NotNull EditorPosition position) {
        if (myFactory.getManager().isSelectionExtendsPastCaret()) {
            return position.atEndOfLine();
        } else {
            return position.atStartOfLine();
        }
    }

    @NotNull
    public EditorCaret setIsStartAnchor(boolean isStartAnchor) {
        if (myIsStartAnchor != isStartAnchor && hasSelection()) {
            // flipping anchors, reset the anchor offset
            // also need to move the other end of selection one line up
            if (isStartAnchor) {
                mySelectionStart = mySelectionEnd;
                mySelectionEnd = myCaretPosition;
            } else {
                mySelectionEnd = mySelectionStart;
                mySelectionStart = myCaretPosition;
            }
        }
        myIsStartAnchor = isStartAnchor;
        return this;
    }

    @NotNull
    public EditorCaret normalizeCaretPosition() {
        if (hasSelection()) {
            if (myIsLine) {
                if (myIsStartAnchor) {
                    myCaretPosition = mySelectionEnd.atColumn(myCaretPosition);
                } else {
                    myCaretPosition = mySelectionStart.atColumn(myCaretPosition);
                }
            } else {
                if (myIsStartAnchor) {
                    myCaretPosition = mySelectionEnd;
                } else {
                    myCaretPosition = mySelectionStart;
                }
            }
        }
        return this;
    }

    @NotNull
    public EditorCaret normalizeCaretPositionForUser() {
        if (hasSelection()) {
            if (myIsLine) {
                if (myIsStartAnchor) {
                    myCaretPosition = mySelectionEnd.atColumn(myCaretPosition);
                } else {
                    // need to adjust this 
                    if (myCaretPosition.column > 0) {
                        myCaretPosition = mySelectionStart.atColumn(myCaretPosition).addLine(-1);
                    } else {
                        myCaretPosition = mySelectionStart.atColumn(myCaretPosition);
                    }
                }
            } else {
                if (myIsStartAnchor) {
                    myCaretPosition = mySelectionEnd;
                } else {
                    myCaretPosition = mySelectionStart;
                }
            }
        }
        return this;
    }

    @NotNull
    public EditorCaret normalizeCaretToCharSelection() {
        if (hasSelection()) {
            if (myIsLine) {
                if (myIsStartAnchor) {
                    boolean extendsPastCaret = myFactory.getManager().isSelectionExtendsPastCaret();
                    myCaretPosition = !extendsPastCaret ? mySelectionEnd.atStartOfLine() : mySelectionEnd.atColumn(myCaretPosition);
                } else {
                    myCaretPosition = mySelectionStart.atColumn(myCaretPosition);
                }
            } else {
                if (myIsStartAnchor) {
                    myCaretPosition = mySelectionEnd;
                } else {
                    myCaretPosition = mySelectionStart;
                }
            }
        }
        return this;
    }

    @NotNull
    public EditorPosition anchorPositionToLineSelectionStart(@NotNull EditorPosition position) {
        return position.atStartOfLine();
    }

    @NotNull
    public EditorPosition anchorPositionToLineSelectionEnd(@NotNull EditorPosition position) {
        if (myFactory.getManager().isSelectionExtendsPastCaret()) {
            return position.atEndOfLine();
        } else {
            return position.atEndOfLine();
        }
    }

    public int getAnchorOffset() {
        return myIsStartAnchor ? mySelectionStart.getOffset() : mySelectionEnd.getOffset();
    }

    public EditorPosition getAnchorPosition() {
        return myIsStartAnchor ? mySelectionStart : mySelectionEnd;
    }

    public EditorPosition getAntiAnchorPosition() {
        return !myIsStartAnchor ? mySelectionStart : mySelectionEnd;
    }

    public EditorCaret setAnchoredPosition(@Nullable EditorPosition other) {
        if (other != null) {
            if (myIsStartAnchor) {
                mySelectionStart = other;
            } else {
                mySelectionEnd = other;
            }
        }
        return this;
    }

    public EditorCaret setAntiAnchoredPosition(@Nullable EditorPosition other) {
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
    public EditorPosition getCharSelectionStart() { return mySelectionStart; }

    @NotNull
    public EditorPosition getSelectionStart() {
        if (myIsLine) {
            if (mySelectionEnd.getOffset() == mySelectionStart.getOffset() || myIsStartAnchor) {
                // one line with no selection always anchor, or anchor
                return anchorPositionToLineSelectionStart(mySelectionStart);
            } else {
                // it is not anchored, adjust as if it was caret position
                return caretPositionToLineSelectionStart(mySelectionStart);
            }
        } else {
            return mySelectionStart;
        }
    }

    @NotNull
    public EditorPosition getCharSelectionEnd() { return mySelectionEnd; }

    @NotNull
    public EditorPosition getSelectionEnd() {
        if (myIsLine) {
            if (mySelectionEnd.getOffset() == mySelectionStart.getOffset() || !myIsStartAnchor) {
                // one line with no selection always anchor, or anchor
                return anchorPositionToLineSelectionEnd(mySelectionEnd);
            } else {
                // it is not anchored, adjust as if it was caret position
                return caretPositionToLineSelectionEnd(mySelectionEnd);
            }
        } else {
            return mySelectionEnd;
        }
    }

    public EditorCaret setLineSelectionIfHasLines() {
        if (!myIsLine && mySelectionStart.line != mySelectionEnd.line) {
            setLineSelection();
        }
        return this;
    }

    /**
     * This restores the original character selection from which the line selection was made
     *
     * @return this
     */
    public EditorCaret setCharSelection() {
        myIsLine = false;
        return this;
    }

    /**
     * This converts the line selection to its equivalent char selection and looses the original char
     * selection from which it was made
     *
     * @return this
     */
    @NotNull
    public EditorCaret setEffectiveCharSelection() {
        if (myIsLine) {
            EditorPosition selectionStart = getSelectionStart();
            EditorPosition selectionEnd = getSelectionEnd();
            myCaretPosition = myIsStartAnchor ? selectionEnd : selectionStart;
            setSelection(selectionStart.atStartOfLine(), selectionEnd.atStartOfLine());
            myIsLine = false;
        }
        return this;
    }

    public EditorCaret setLineSelection() {
        myIsLine = true;
        return this;
    }

    public EditorCaret trimOrExpandToLineSelection() {
        return trimOrExpandToFullLines().setLineSelection();
    }

    public EditorCaret expandToLineSelection() {
        return expandToFullLines().setLineSelection();
    }

    public boolean canTrimOrExpandToFullLineSelection() {
        if (hasSelection()) {
            if (isLine()) {
                return true;
            } else {
                if (mySelectionStart.line != mySelectionEnd.line) {
                    if (mySelectionStart.column != 0 || mySelectionEnd.column != 0) {
                        EditorPosition selectionStart = mySelectionStart.toTrimmedOrExpandedFullLine();
                        EditorPosition selectionEnd = mySelectionEnd.toTrimmedOrExpandedFullLine();
                        return selectionStart.column == 0 && selectionEnd.column == 0;
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @NotNull
    public EditorCaret trimOrExpandToFullLines() {
        if (hasSelection() && mySelectionStart.line != mySelectionEnd.line) {
            if (mySelectionStart.column != 0 || mySelectionEnd.column != 0) {
                mySelectionStart = mySelectionStart.toTrimmedOrExpandedFullLine();
                mySelectionEnd = mySelectionEnd.toTrimmedOrExpandedFullLine();
            }
        }
        return this;
    }

    @NotNull
    public EditorCaret expandToFullLines() {
        if (hasSelection()) {
            if (myIsLine) {
                mySelectionStart = mySelectionStart.atColumn(mySelectionStart.getIndentColumn());
                mySelectionEnd = mySelectionEnd.atColumn(mySelectionEnd.getTrimmedEndColumn());
            } else {
                if (mySelectionStart.column != 0 || mySelectionEnd.column != 0) {
                    mySelectionStart = mySelectionStart.atStartOfLine();
                    mySelectionEnd = mySelectionEnd.atEndOfLine();
                }
            }
        }
        return this;
    }

    public EditorCaret setCaretPosition(int line, int column) {
        myCaretPosition = myCaretPosition.atPosition(line, column);
        return this;
    }

    public EditorCaret setCaretPosition(int offset) {
        myCaretPosition = myCaretPosition.atOffset(offset);
        return this;
    }

    public EditorCaret setCaretPosition(@Nullable EditorPosition position) {
        if (position != null) {
            myCaretPosition = position;
        }
        return this;
    }

    public EditorCaret setSelectionStart(int line, int column) {
        mySelectionStart = mySelectionStart.atPosition(line, column);
        return this;
    }

    public EditorCaret setSelectionStart(int offset) {
        mySelectionStart = mySelectionStart.atOffset(offset);
        return this;
    }

    public EditorCaret setSelectionStart(@Nullable EditorPosition position) {
        if (position != null) {
            mySelectionStart = position;
        }
        return this;
    }

    public EditorCaret setSelectionEnd(int line, int column) {
        mySelectionEnd = mySelectionEnd.atPosition(line, column);
        return this;
    }

    public EditorCaret setSelectionEnd(int offset) {
        mySelectionEnd = mySelectionEnd.atOffset(offset);
        return this;
    }

    public EditorCaret setSelectionEnd(@Nullable EditorPosition position) {
        if (position != null) {
            mySelectionEnd = position;
        }
        return this;
    }

    public EditorCaret setSelection(int startOffset, int endOffset) {
        mySelectionStart = mySelectionStart.atOffset(startOffset);
        mySelectionEnd = mySelectionEnd.atOffset(endOffset);
        return this;
    }

    public EditorCaret setSelection(EditorPosition startOffset, EditorPosition endOffset) {
        mySelectionStart = startOffset;
        mySelectionEnd = endOffset;
        return this;
    }

    public EditorPositionFactory getFactory() { return myFactory; }

    public boolean isStartAnchor() { return myIsStartAnchor; }

    @NotNull
    public EditorPosition getCaretPosition() { return myCaretPosition; }

    @NotNull
    public EditorPosition getSelectedLineCaretPosition() {
        if (myIsStartAnchor) {
            // caret on end of selection
            if (myFactory.getManager().isSelectionExtendsPastCaret()) {
                return myCaretPosition.column == 0 ? myCaretPosition.addLine(-1) : myCaretPosition;
            } else {
                return myCaretPosition.addLine(-1);
            }
        } else {
            // caret at top of selection
            return myCaretPosition.column != 0 ? myCaretPosition.addLine(1) : myCaretPosition;
        }
    }

    public EditorCaret atColumnPreserveLineSelection(int wasColumn, int column) {
        if (!isStartAnchor()) {
            // starts are affected the same way
            myCaretPosition = myCaretPosition.atColumn(column);
            if (myCaretPosition.column != 0 && myCaretPosition.line == mySelectionStart.line) {
                setCaretPosition(getCaretPosition().addLine(-1));
                mySelectionStart = myCaretPosition;
            } else {
                if (myCaretPosition.column == 0 && myCaretPosition.line != mySelectionStart.line) {
                    myCaretPosition.onLine(mySelectionStart);
                }
            }
        } else {
            myCaretPosition = myCaretPosition.atColumn(column);
            if (myFactory.getManager().isSelectionExtendsPastCaret()) {
                if (getCaretPosition().column == 0 && wasColumn != 0) {
                    // need to adjust selection or we lost or gained a line
                    if (isStartAnchor()) {
                        // need to move down one line
                        setCaretPosition(getCaretPosition().addLine(1));
                        mySelectionEnd = myCaretPosition;
                    }
                } else if (getCaretPosition().column != 0 && wasColumn == 0) {
                    // need to adjust selection or we lost or gained a line
                    if (isStartAnchor()) {
                        // need to move down one line
                        setCaretPosition(getCaretPosition().addLine(-1));
                        mySelectionEnd = myCaretPosition;
                    }
                }
            }
        }
        return this;
    }

    public boolean isLine() { return myIsLine; }

    public boolean hasSelection() { return myIsLine || mySelectionStart.getOffset() != mySelectionEnd.getOffset(); }

    public boolean hasLines() { return mySelectionStart.line != mySelectionEnd.line; }

    public int getSelectionLineCount() {
        int lineCount = mySelectionEnd.line - mySelectionStart.line;

        if (isLine()) {
            if (lineCount == 0) {
                lineCount++;
            }
        } else if (hasSelection()) {
            if (lineCount > 0 && mySelectionEnd.column > 0) {
                lineCount++;
            }
        }
        return lineCount;
    }

    public String toString() {
        return "EditorCaret{" +
                ", myCaretPosition=" + myCaretPosition +
                ", mySelectionStart=" + mySelectionStart +
                ", mySelectionEnd=" + mySelectionEnd +
                ", myIsStartAnchor=" + myIsStartAnchor +
                ", myIsLine=" + myIsLine +
                '}';
    }
}
