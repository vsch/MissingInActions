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

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: May 13, 2002
 * Time: 9:58:23 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.vladsch.MissingInActions.actions;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.vladsch.MissingInActions.Plugin;
import com.vladsch.MissingInActions.util.LineSelectionAdjuster;
import com.vladsch.MissingInActions.util.LineSelectionState;
import com.vladsch.MissingInActions.util.LogPos;
import org.jetbrains.annotations.Nullable;

public class MoveCaretLineUpOrDownActionHandler extends EditorActionHandler {
    final boolean myMoveUp;
    final boolean myWithSelection;

    public MoveCaretLineUpOrDownActionHandler(boolean moveUp, boolean withSelection) {
        super(true);
        this.myMoveUp = moveUp;
        this.myWithSelection = withSelection;
    }

    @Override
    public void doExecute(final Editor editor, final @Nullable Caret caret, final DataContext dataContext) {
        final LineSelectionAdjuster adjuster = LineSelectionAdjuster.getInstance(editor);

        if (myWithSelection) {
            if (!editor.getCaretModel().supportsMultipleCarets()) {
                performWithSelection(editor, adjuster, caret);
            } else {
                if (editor.isColumnMode()) {
                    new CloneCaretActionHandler(myMoveUp).execute(editor, caret, dataContext);
                } else {
                    if (caret == null) {
                        editor.getCaretModel().runForEachCaret(caret1 -> performWithSelection(editor, adjuster, caret1));
                    } else {
                        performWithSelection(editor, adjuster, caret);
                    }
                }
            }
        } else {
            adjuster.guard(() -> {
                if (!editor.getCaretModel().supportsMultipleCarets()) {
                    perform(editor, adjuster, caret);
                } else {
                    if (caret == null) {
                        editor.getCaretModel().runForEachCaret(caret1 -> perform(editor, adjuster, caret1));
                    } else {
                        perform(editor, adjuster, caret);
                    }
                }
            });
        }
    }

    private void perform(Editor editor, LineSelectionAdjuster adjuster, Caret caret) {
        assert caret != null;

        adjuster.adjustLineSelectionToCharacterSelection(caret, false);
        if (myMoveUp) {
            int lineNumber = editor.getDocument().getLineNumber(caret.getOffset());

            if (lineNumber > 0) {
                editor.getCaretModel().moveCaretRelatively(0, -1, false, false, caret == editor.getCaretModel().getPrimaryCaret());
            }
        } else {
            int lineNumber = editor.getDocument().getLineNumber(caret.getOffset());
            if (lineNumber <= editor.getDocument().getLineCount()) {
                editor.getCaretModel().moveCaretRelatively(0, 1, false, false, caret == editor.getCaretModel().getPrimaryCaret());
            }
        }
    }

    private void performWithSelection(Editor editor, LineSelectionAdjuster adjuster, Caret caret) {
        assert caret != null;

        LogPos.Factory f = LogPos.factory(editor);
        LogPos pos = f.fromPos(caret.getLogicalPosition());
        LogPos start = f.fromOffset(caret.getSelectionStart());
        LogPos end = f.fromOffset(caret.getSelectionEnd());
        LogPos newStart = start;
        LogPos newEnd = end;
        LogPos newPos = pos;
        LineSelectionState state = adjuster.getSelectionState(caret);

        boolean handled = false;
        boolean startIsAnchor = true;
        int lineCount = editor.getDocument().getLineCount();

        if (!caret.hasSelection()) {
            state.setAnchorOffset(pos.toOffset());
        } else {
            if (!state.isLine()) {
                state.setAnchorOffset(caret.getLeadSelectionOffset());
            }
        }

        if (myMoveUp) {
            newPos = pos.addLine(-1);
        } else {
            newPos = pos.addLine(1);
        }

        if (Plugin.getCaretInSelection()) {
            if (caret.hasSelection()) {
                // need to figure out whether start or end of selection is the anchor
                boolean atStart = pos.line == start.line;
                boolean atEnd = newPos.line == end.atEndOfLine().line;

                if (atStart && atEnd) {
                    startIsAnchor = !myMoveUp;
                } else {
                    startIsAnchor = !atStart;
                }

                if (myMoveUp) {
                    if (startIsAnchor) {
                        // moving up shortening bottom
                        newStart = start.atStartOfLine();
                        newEnd = newPos.atEndOfNextLine();
                    } else {
                        // was on start, keep end and move start
                        newStart = newPos.atStartOfLine();
                        newEnd = newEnd.atEndOfLine();
                    }
                } else {
                    if (startIsAnchor) {
                        // moving up shortening bottom
                        newStart = newStart.atStartOfLine();
                        newEnd = newEnd.atEndOfNextLine();
                    } else {
                        // was on start, keep end and move start
                        newStart = newPos.atStartOfLine();
                        newEnd = newEnd.atEndOfLine();
                    }
                }
            } else {
                // start a new selection and keep cursor on the same line
                if (myMoveUp) {
                    startIsAnchor = true;
                    newStart = pos.atStartOfLine();
                    newEnd = pos.atEndOfNextLine();
                } else {
                    startIsAnchor = false; // it will be inverted on text step in same direction
                    newStart = pos.atStartOfLine();
                    newEnd = pos.atEndOfNextLine();
                }
                newPos = pos;
            }
        } else {
            if (pos.line == 0) {
                // line selection at top and going up: do nothing 
                // line selection at top and going down: stay on top, move selection one line down 
                // no line selection at top and going down, normal handling 
                // no line selection at top and going up, make top line selection

                if (state.isLine() && LogPos.haveTopLineSelection(start, end)) {
                    if (myMoveUp) {
                        // line selection at top and going up: do nothing 
                        newStart = start;
                        newEnd = end;
                        newPos = pos;
                        handled = true;
                    } else {
                        // line selection at top and going down: stay on top, move selection one line down 
                        newStart = pos.atEndOfNextLine();
                        newEnd = end.atEndOfLine();
                        newPos = pos;
                        handled = true;
                    }
                } else {
                    if (myMoveUp) {
                        // no line selection at top and going up, make top line selection
                        newStart = pos.atStartOfLine();
                        newEnd = end.line == pos.line ? pos.atEndOfNextLine() : end.atEndOfLine();
                        handled = true;
                    } else {
                        // no line selection at top and going down, normal handling 
                        handled = false;
                    }
                }
            }

            if (!handled) {
                if (newPos.equals(pos)) {
                    if (pos.line == lineCount) {
                        // we are at bottommost line and tried to move down
                        newStart = start.atStartOfLine();
                        newEnd = end.atEndOfLine();
                    } else {
                        if (myMoveUp) {
                            newStart = newPos.atEndOfNextLine();
                            newEnd = end.atEndOfLine();
                        } else {
                            newStart = start.atStartOfLine();
                            newEnd = newPos.atStartOfLine();
                        }
                    }
                } else {
                    if (start.line == end.line) {
                        if (myMoveUp) {
                            newStart = newPos.atEndOfNextLine();
                            newEnd = pos.atEndOfNextLine();
                        } else {
                            newStart = pos.atStartOfLine();
                            newEnd = newPos.atStartOfLine();
                        }
                    } else {
                        if (myMoveUp) {
                            // we are going up, if start is lead then it is below pos
                            startIsAnchor = !(pos.line < start.line);
                        } else {
                            // we are going down, if end is leading then it is the same as pos
                            startIsAnchor = (pos.line >= end.line && pos.column >= end.column);
                        }

                        if (!startIsAnchor) {
                            newStart = newPos.atEndOfNextLine();
                            newEnd = end.atEndOfLine();
                        } else {
                            newStart = start.atStartOfLine();
                            newEnd = newPos.atEndOfNextLine();
                        }
                    }
                }
            }
        }

        LineSelectionAdjuster.setCaretLineSelection(editor, caret, newPos, newStart, newEnd, startIsAnchor, state);
    }
}
