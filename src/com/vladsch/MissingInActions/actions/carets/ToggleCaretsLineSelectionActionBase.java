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

package com.vladsch.MissingInActions.actions.carets;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.util.text.CharArrayUtil;
import com.vladsch.MissingInActions.actions.LineSelectionAware;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.manager.LineSelectionState;
import com.vladsch.MissingInActions.util.*;
import com.vladsch.flexmark.util.sequence.Range;

import static com.vladsch.MissingInActions.Plugin.getCaretInSelection;

abstract public class ToggleCaretsLineSelectionActionBase extends AnAction implements LineSelectionAware {
    final protected boolean myWantBlankLines;
    final protected boolean myWantNonBlankLines;

    public ToggleCaretsLineSelectionActionBase(boolean wantBlankLines, boolean wantNonBlankLines) {
        myWantBlankLines = wantBlankLines;
        myWantNonBlankLines = wantNonBlankLines;
        setEnabledInModalContext(true);
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }

    @Override
    public void update(AnActionEvent e) {
        EditorEx editor = getEditor(e);
        if (editor == null || editor.isOneLineMode()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setVisible(false);
        } else {
            e.getPresentation().setEnabled(editor.getCaretModel().supportsMultipleCarets() && editor.getSelectionModel().hasSelection() || editor.getCaretModel().getCaretCount() > 1);
            e.getPresentation().setVisible(true);
            super.update(e);
        }
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final EditorEx editor = getEditor(e);
        final SelectionModel selectionModel = editor.getSelectionModel();
        final CaretModel caretModel = editor.getCaretModel();
        final DocumentEx doc = editor.getDocument();
        final LogPos.Factory f = LogPos.factory(editor);
        LineSelectionManager adjuster = LineSelectionManager.getInstance(editor);

        Caret primaryCaret = caretModel.getPrimaryCaret();
        LineSelectionState state = adjuster.getSelectionState(primaryCaret);

        adjuster.guard(() -> {
            LogPos pos = f.fromPos(primaryCaret.getLogicalPosition());

            if (caretModel.getCaretCount() > 1) {
                // switch to line mode from top most caret to bottom most caret
                Range selRange = Range.NULL;
                for (Caret caret : caretModel.getAllCarets()) {
                    int line = caret.getLogicalPosition().line;
                    selRange = selRange.include(line);
                }
                caretModel.removeSecondaryCarets();
                editor.setColumnMode(false);

                // create a line selection that includes minOffset/maxOffset
                LogPos selStart = f.fromLine(selRange.getStart(), 0);
                LogPos selEnd = f.fromLine(selRange.getEnd(), 0).atEndOfNextLine();

                if (getCaretInSelection()) {
                    pos = selEnd.onLine(selEnd.line - 1).atColumn(pos.column);
                } else {
                    pos = selEnd.atColumn(pos.column);
                }

                adjuster.setCaretLineSelection(primaryCaret, pos, selStart, selEnd, true, state);
            } else if (selectionModel.hasSelection() && (myWantBlankLines || myWantNonBlankLines)) {
                // if not line selection then we convert it to line selection, next time to carets
                if (state.isLine()) {
                    LogPos selStart = f.fromOffset(selectionModel.getSelectionStart());
                    LogPos selEnd = f.fromOffset(selectionModel.getSelectionEnd());

                    caretModel.removeSecondaryCarets();
                    //selectionModel.setSelection(primaryCaret.getOffset(), primaryCaret.getOffset());
                    selectionModel.removeSelection();
                    editor.setColumnMode(false);

                    if (selStart.line + 1 == selEnd.line) {
                        // one liner, we restore char selection
                        LineSelectionManager.adjustLineSelectionToCharacterSelection(editor, primaryCaret, false);
                    } else {
                        int endLine = selEnd.line + (!getCaretInSelection() ? 1 : 0);

                        // build the list of carets
                        boolean first = true;
                        for (int lineNumber = selStart.line; lineNumber < endLine; lineNumber++) {
                            // just filter out, blank or non-blank lines
                            int lineEndOffset = doc.getLineEndOffset(lineNumber);
                            int lineStartOffset = doc.getLineStartOffset(lineNumber);

                            boolean isBlank = CharArrayUtil.isEmptyOrSpaces(doc.getCharsSequence(), lineStartOffset, lineEndOffset);
                            if (isBlank && myWantBlankLines || !isBlank && myWantNonBlankLines) {
                                LogPos logPos = pos.onLine(lineNumber);
                                Caret caret = first ? caretModel.getPrimaryCaret() : caretModel.addCaret(logPos.toVisualPosition());
                                if (caret != null) {
                                    caret.moveToLogicalPosition(logPos);
                                }
                                first = false;
                            }
                        }
                        EditHelpers.scrollToCaret(editor);
                    }
                } else {
                    adjuster.adjustCharacterSelectionToLineSelection(primaryCaret, true, true);
                }
            }
        });
    }

    private static EditorEx getEditor(AnActionEvent e) {
        return (EditorEx) CommonDataKeys.EDITOR.getData(e.getDataContext());
    }
}
