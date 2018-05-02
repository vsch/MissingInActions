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

package com.vladsch.MissingInActions.actions.line;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.Couple;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.MissingInActions.manager.EditorPosition;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.ClipboardCaretContent;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.flexmark.util.sequence.Range;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;

public class DuplicateForClipboardCaretsActionHandler extends EditorWriteActionHandler {
    private final boolean myInsertBlankLine;
    private final boolean myDoPaste;

    public DuplicateForClipboardCaretsActionHandler() {
        super(false);
        myInsertBlankLine = false;
        myDoPaste = false;
    }

    public DuplicateForClipboardCaretsActionHandler(boolean doPaste, boolean insertBlankLine) {
        super(false);
        myDoPaste = doPaste;
        myInsertBlankLine = insertBlankLine;
    }

    @Override
    public void executeWriteAction(Editor editor, Caret unusedCaret, DataContext dataContext) {
        ClipboardCaretContent clipboardCaretContent = ClipboardCaretContent.studyClipboard(editor, dataContext);
        if (clipboardCaretContent == null) return;

        EditorEx editorEx = editor instanceof EditorEx ? (EditorEx) editor : null;
        int iMax = clipboardCaretContent.getCaretCount();
        LineSelectionManager manager = LineSelectionManager.getInstance(editor);
        Document doc = editor.getDocument();
        CaretModel caretModel = editor.getCaretModel();
        List<Couple<Integer>> copies = new ArrayList<>(iMax);
        boolean duplicateForCaretsPreserveOriginal = ApplicationSettings.getInstance().isMultiPastePreserveOriginal();

        if (editor.getCaretModel().getCaretCount() > 1) {
            // already multi-caret, use the caret span to figure out which block to copy,
            // span: line block from first to last caret
            // if span == 1 line, keep all carets and all selections
            // if span > 1 line:
            // if have selections: keep carets with selections relative to start of block after duplication
            // if no selections: remove first and last, if they have no selection
            boolean haveSelections = false;
            Transferable mergedTransferable = null;

            EditHelpers.StudiedCarets studiedCarets = EditHelpers.studyCarets(editor, caretModel.getAllCarets());

            caretModel.removeSecondaryCarets();
            if (editorEx != null) editorEx.setColumnMode(false);

            if (studiedCarets.range.getSpan() == 0) {
                // remove carets without selections
                if (studiedCarets.caretSelections.total > 0) {
                    // remove carets without selections
                    studiedCarets.carets.removeIf(caret -> !caret.hasSelection());
                }
            } else {
                if (studiedCarets.caretSelections.total > 0) {
                    // remove carets without selections on corresponding lines
                    studiedCarets.carets.removeIf((editorCaret) -> !editorCaret.hasSelection());
                } else {
                    // if not all lines have same number of carets, remove first and last, if they have no selection assuming they are there to mark the span of copied lines
                    if (studiedCarets.eachLineCarets.total == -1
                            && studiedCarets.eachLineCarets.code <= 0 && studiedCarets.eachLineCarets.comment <= 0 && studiedCarets.eachLineCarets.blank <= 0) {
                        // remove first and last carets
                        studiedCarets.carets.removeIf((editorCaret) -> editorCaret == null
                                || editorCaret == studiedCarets.firstLineCaret && !studiedCarets.firstLineCaret.hasSelection()
                                || editorCaret == studiedCarets.lastLineCaret && !studiedCarets.lastLineCaret.hasSelection());
                    }
                }
            }

            ArrayList<EditorCaret> carets = studiedCarets.carets;
            Range selRange = studiedCarets.range;

            // we need to duplicate the content, each caret in content duplicated for number of caret copies
            if (carets.size() > 1 || manager.haveOnPasteReplacements()) {
                // make dupes so that the block carets are duped
                mergedTransferable = EditHelpers.getSplitRepeatedTransferable(editor, clipboardCaretContent, carets.size());
            }

            // now we are ready to duplicate selRange block, and put carets on each duplicate relative to the first block which will not be included
            EditorCaret editorCaret = carets.get(0).copy();
            EditorPosition startPosition = editorCaret.getCaretPosition().onLine(selRange.getStart()).atStartOfLine();
            EditorPosition endPosition = editorCaret.getCaretPosition().onLine(selRange.getEnd()).atStartOfNextLine();
            editorCaret.setCaretPosition(startPosition);
            int offset = startPosition.getOffset();

            // do it in reverse order so as not to affect the offset
            String s = doc.getCharsSequence().subSequence(startPosition.getOffset(), endPosition.getOffset()).toString();
            int inserted = 0;
            int span = selRange.getSpan() + 1;
            for (int i = iMax; i-- > 0; ) {
                Couple<Integer> couple;
                if (i == 0 && !duplicateForCaretsPreserveOriginal) {
                    // don't insert, re-use
                    couple = new Couple<>(selRange.getStart(), selRange.getEnd());
                } else {
                    doc.insertString(startPosition.getOffset(), s);
                    inserted += span;
                    couple = new Couple<>(selRange.getStart() + inserted, selRange.getEnd() + inserted);
                }
                copies.add(couple);
            }

            manager.guard(() -> {
                // create multiple carets, copies from carets, but relative to start of original selection
                editorCaret.removeSelection();
                caretModel.removeSecondaryCarets();

                // build the carets
                int accumulatedOffset = 0;
                boolean firstCaret = true;
                for (int i = 0; i < iMax; i++) {
                    Couple<Integer> couple = copies.get(i);

                    int firstLine = couple.first;

                    for (EditorCaret copyCaret : carets) {
                        EditorPosition editorPosition = copyCaret.getCaretPosition().onLine(copyCaret.getCaretPosition().line - selRange.getStart() + firstLine).copy();
                        EditorPosition selectionStart = copyCaret.getSelectionStart().onLine(copyCaret.getSelectionStart().line - selRange.getStart() + firstLine).copy();
                        EditorPosition selectionEnd = copyCaret.getSelectionEnd().onLine(copyCaret.getSelectionEnd().line - selRange.getStart() + firstLine).copy();

                        Caret caret = firstCaret ? caretModel.getPrimaryCaret() : caretModel.addCaret(editorPosition.toVisualPosition());
                        if (caret != null) {
                            firstCaret = false;
                            accumulatedOffset += editorPosition.ensureRealSpaces();
                            accumulatedOffset += selectionStart.ensureRealSpaces();
                            accumulatedOffset += selectionEnd.ensureRealSpaces();

                            // replicate selection to this position
                            int selectionSize = selectionEnd.getOffset() - selectionStart.getOffset();
                            if (selectionSize > 0) {
                                caret.moveToOffset(selectionEnd.getOffset());
                                caret.setSelection(selectionStart.getOffset(), selectionEnd.getOffset());
                                manager.resetSelectionState(caret);
                            } else {
                                caret.moveToLogicalPosition(editorPosition);
                            }
                        }
                    }
                }

                ClipboardCaretContent.setLastPastedClipboardCarets(editor, null);
            });

            if (myDoPaste) {
                // clear last pasted information, it is no good and will be cleared by running our action
                if (mergedTransferable != null) {
                    CopyPasteManager.getInstance().setContents(mergedTransferable);
                }

                // now we paste
                final AnAction pasteAction = ActionManager.getInstance().getAction(IdeActions.ACTION_PASTE);
                LineSelectionManager.getInstance(editor).runActionWithAdjustments(pasteAction);
                //AnActionEvent newEvent = AnActionEvent.createFromDataContext("MiaMultiPaste Recreate Carets",null,dataContext);
                //pasteAction.actionPerformed(newEvent);
            } else {
                if (mergedTransferable != null) {
                    CopyPasteManager.getInstance().setContents(mergedTransferable);
                }
            }

            EditHelpers.scrollToCaret(editor);
        } else {
            manager.guard(() -> {
                EditorCaret editorCaret = manager.getEditorCaret(editor.getCaretModel().getPrimaryCaret());

                int selectionSize = editorCaret.getSelectionEnd().getOffset() - editorCaret.getSelectionStart().getOffset();
                boolean isStartAnchor = editorCaret.isStartAnchor();

                if (myInsertBlankLine) {
                    // we create a line above/below and recreate carets
                    final ApplicationSettings settings = ApplicationSettings.getInstance();

                    final EditorPosition pastePosition = settings.getLinePasteCaretAdjustmentType().getPastePosition(editorCaret.getCaretPosition());
                    doc.insertString(pastePosition.atColumn(0).getOffset(), "\n");
                    editorCaret.setCaretPosition(pastePosition.atColumn(editorCaret.getColumn()));
                    editorCaret.commit();
                }

                if (editorCaret.hasLines() || editorCaret.isLine()) {
                    editorCaret.trimOrExpandToFullLines();
                    selectionSize = 0;
                } else {
                    // dupe the line with selection replicated for each caret
                    editorCaret.removeSelection();
                    editor.getSelectionModel().removeSelection();
                }

                boolean useFirstLine = myInsertBlankLine || !duplicateForCaretsPreserveOriginal;
                for (int i = 0; i < iMax; i++) {
                    final Couple<Integer> couple;
                    if (useFirstLine) {
                        useFirstLine = false;
                        EditorPosition position = editorCaret.getCaretPosition();
                        couple = new Couple<>(position.atStartOfLine().getOffset(), position.atStartOfNextLine().getOffset());
                    } else {
                        couple = EditHelpers.duplicateLineOrSelectedBlockAtCaret(editor, editor.getDocument(), editor.getCaretModel().getPrimaryCaret(), true);
                    }

                    if (couple != null) copies.add(couple);
                }

                // create multiple carets on first line of every copy
                EditorPosition pos = editorCaret.getCaretPosition();
                editorCaret.removeSelection();

                // build the carets
                int accumulatedOffset = 0;
                for (int i = 0; i < iMax; i++) {
                    Couple<Integer> couple = copies.get(i);

                    int lineNumber = doc.getLineNumber(couple.first + accumulatedOffset);

                    EditorPosition editorPosition = pos.onLine(lineNumber);
                    Caret caret1 = i == 0 ? caretModel.getPrimaryCaret() : caretModel.addCaret(editorPosition.toVisualPosition());
                    if (caret1 != null) {
                        accumulatedOffset += editorPosition.ensureRealSpaces();
                        int offset = editorPosition.getOffset();

                        caret1.moveToLogicalPosition(editorPosition);

                        // replicate selection to this position
                        if (isStartAnchor) {
                            caret1.setSelection(offset - selectionSize, offset);
                        } else {
                            caret1.setSelection(offset, offset + selectionSize);
                        }
                        manager.resetSelectionState(caret1);
                    }
                }
            });

            if (myDoPaste) {
                // clear last pasted information, it is no good and will be cleared by running our action
                ClipboardCaretContent.setLastPastedClipboardCarets(editor, null);

                // now we paste
                final AnAction pasteAction = ActionManager.getInstance().getAction(IdeActions.ACTION_PASTE);
                LineSelectionManager.getInstance(editor).runActionWithAdjustments(pasteAction);
                //AnActionEvent newEvent = AnActionEvent.createFromDataContext("MiaMultiPaste Recreate Carets",null,dataContext);
                //pasteAction.actionPerformed(newEvent);
            } else {
                // clear clipboard information so adjustments don't get messed up
                ClipboardCaretContent.setLastPastedClipboardCarets(editor, null);
            }

            EditHelpers.scrollToCaret(editor);
        }
    }

    @Override
    public boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
        return true;
    }
}
