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
import com.vladsch.MissingInActions.manager.*;
import com.vladsch.MissingInActions.util.*;
import com.vladsch.flexmark.util.sequence.Range;

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
        LineSelectionManager manager = LineSelectionManager.getInstance(editor);
        final EditorPositionFactory f = manager.getPositionFactory();

        Caret primaryCaret = caretModel.getPrimaryCaret();
        EditorCaret editorCaret = manager.getEditorCaret(primaryCaret);

        manager.guard(() -> {
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
                EditorPosition selStart = f.fromPosition(selRange.getStart(), 0);
                EditorPosition selEnd = f.fromPosition(selRange.getEnd(), 0).atStartOfNextLine();
                editorCaret.setSelection(selStart, selEnd)
                        .trimOrExpandToLineSelection()
                        .normalizeCaretPosition()
                        .commit();
            } else if (selectionModel.hasSelection() && (myWantBlankLines || myWantNonBlankLines)) {
                // if not line selection then we convert it to line selection, next time to carets
                final DocumentEx doc = editor.getDocument();
                EditorPosition pos = editorCaret.getCaretPosition();
                
                if (editorCaret.isLine()) {
                    EditorPosition selStart = f.fromOffset(selectionModel.getSelectionStart());
                    EditorPosition selEnd = f.fromOffset(selectionModel.getSelectionEnd());

                    caretModel.removeSecondaryCarets();
                    
                    selectionModel.removeSelection();
                    editor.setColumnMode(false);

                    int selectionLineCount = editorCaret.getSelectionLineCount();
                    if (selectionLineCount == 1) {
                        // one liner, we restore char selection
                        editorCaret.setCharSelection()
                                .commit();
                    } else {
                        int endLine = selStart.line + selectionLineCount;
                        editorCaret.removeSelection();

                        // build the list of carets
                        boolean first = true;
                        for (int lineNumber = selStart.line; lineNumber < endLine; lineNumber++) {
                            // just filter out, blank or non-blank lines
                            int lineEndOffset = doc.getLineEndOffset(lineNumber);
                            int lineStartOffset = doc.getLineStartOffset(lineNumber);

                            boolean isBlank = CharArrayUtil.isEmptyOrSpaces(doc.getCharsSequence(), lineStartOffset, lineEndOffset);
                            if (isBlank && myWantBlankLines || !isBlank && myWantNonBlankLines) {
                                EditorPosition editorPosition = pos.onLine(lineNumber);
                                Caret caret = first ? caretModel.getPrimaryCaret() : caretModel.addCaret(editorPosition.toVisualPosition());
                                if (caret != null) {
                                    caret.moveToLogicalPosition(editorPosition);
                                    int offset = editorPosition.getOffset();
                                    caret.setSelection(offset, offset);
                                    manager.resetSelectionState(caret);
                                }
                                first = false;
                            }
                        }
                        EditHelpers.scrollToCaret(editor);
                    }
                } else {
                    editorCaret.trimOrExpandToLineSelection()
                            .normalizeCaretPosition()
                            .commit();
                }
            }
        });
    }

    private static EditorEx getEditor(AnActionEvent e) {
        return (EditorEx) CommonDataKeys.EDITOR.getData(e.getDataContext());
    }
}
