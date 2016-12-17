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

import com.intellij.codeInsight.editorActions.TextBlockTransferable;
import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.MissingInActions.manager.EditorPosition;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.ClipboardCaretContent;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.MissingInActions.util.RepeatedCharSequence;
import com.vladsch.flexmark.util.sequence.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public static Couple<Integer> duplicateLineOrSelectedBlockAtCaret(Editor editor, final Document document, @NotNull Caret caret, final boolean moveCaret) {
        if (caret.hasSelection()) {
            int start = caret.getSelectionStart();
            int end = caret.getSelectionEnd();
            String s = document.getCharsSequence().subSequence(start, end).toString();
            document.insertString(end, s);
            if (moveCaret) {
                // select newly copied lines and move there
                caret.moveToOffset(end + s.length());
                editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                caret.removeSelection();
                caret.setSelection(end, end + s.length());
            }
            return Couple.of(end, end + s.length());
        } else {
            return duplicateLinesRange(editor, document, caret, caret.getOffset(), caret.getVisualPosition(), caret.getVisualPosition(), moveCaret);
        }
    }

    @SuppressWarnings("WeakerAccess")
    @Nullable
    public static Couple<Integer> duplicateLinesRange(Editor editor, Document document, @Nullable Caret caret, int offset, VisualPosition rangeStart, VisualPosition rangeEnd, boolean moveCaret) {
        Pair<LogicalPosition, LogicalPosition> lines = EditorUtil.calcSurroundingRange(editor, rangeStart, rangeEnd);

        LogicalPosition lineStart = lines.first;
        LogicalPosition nextLineStart = lines.second;
        int start = editor.logicalPositionToOffset(lineStart);
        int end = editor.logicalPositionToOffset(nextLineStart);
        if (end <= start) {
            return null;
        }
        String s = document.getCharsSequence().subSequence(start, end).toString();
        final int lineToCheck = nextLineStart.line - 1;

        int newOffset = end + offset - start;
        if (lineToCheck == document.getLineCount() /* empty document */
                || lineStart.line == document.getLineCount() - 1 /* last line*/
                || document.getLineSeparatorLength(lineToCheck) == 0) {
            s = "\n" + s;
            newOffset++;
        }
        document.insertString(end, s);

        if (moveCaret && caret != null) {
            caret.moveToOffset(newOffset);

            editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        }
        return Couple.of(end, end + s.length());
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

        if (editor.getCaretModel().getCaretCount() > 1) {
            // already multi-caret, use the caret span to figure out which block to copy, 
            // span: line block from first to last caret
            // if span == 1 line, keep all carets and all selections
            // if span > 1 line:
            // if have selections: keep carets with selections relative to start of block after duplication
            // if no selections: put a caret on the first line of every block
            boolean haveSelections = false;
            Transferable mergedTransferable = null;

            Range selRange = Range.NULL;
            ArrayList<EditorCaret> carets = new ArrayList<>();
            for (Caret caret : caretModel.getAllCarets()) {
                EditorCaret editorCaret = manager.getEditorCaret(caret);
                carets.add(editorCaret);

                selRange = selRange.include(editorCaret.getCaretPosition().line);
                if (editorCaret.hasSelection()) {
                    haveSelections = true;
                    if (editorCaret.hasLines()) {
                        selRange = selRange.include(editorCaret.getSelectionStart().line);
                        selRange = selRange.include(editorCaret.getSelectionEnd().line);
                    }
                }
            }

            caretModel.removeSecondaryCarets();
            if (editorEx != null) editorEx.setColumnMode(false);

            if (selRange.getSpan() == 0) {
                // remove carets without selections
                if (haveSelections) {
                    // remove carets without selections
                    carets.removeIf(caret -> !caret.hasSelection());
                } else {
                    // single line, keep all carets, dupe line and create carets with selections
                }
            } else {
                if (haveSelections) {
                    // remove carets without selections
                    carets.removeIf(caret -> !caret.hasSelection());
                } else {
                    // just put a caret on first line of every block, take the first caret that is on the line, 
                    // if none are then take first one from the list and use its column position and offset
                    EditorCaret bestCaret = null;
                    for (EditorCaret editorCaret : carets) {
                        if (editorCaret.getCaretPosition().line == selRange.getStart()) {
                            if (bestCaret == null || bestCaret.getCaretPosition().column > editorCaret.getCaretPosition().column) {
                                bestCaret = editorCaret;
                            }
                        }
                    }

                    if (bestCaret == null) {
                        bestCaret = carets.get(0).onLine(selRange.getStart());
                    }

                    carets.clear();
                    carets.add(bestCaret);
                }
            }

            // we need to duplicate the content, each caret in content duplicated for number of caret copies
            if (carets.size() > 1) {
                // make dupes so that the block carets are duped
                List<Transferable> list = new ArrayList<>(clipboardCaretContent.getCaretCount());
                String sep = "\n";
                int iMax1 = clipboardCaretContent.getCaretCount();
                final String[] texts = clipboardCaretContent.getTexts();
                assert texts != null;

                StringBuilder sb = new StringBuilder();
                List<TextRange> ranges = new ArrayList<>();

                for (int i = 0; i < iMax1; i++) {
                    for (int j = 0; j < carets.size(); j++) {
                        if (clipboardCaretContent.isFullLine(i)) {
                            int startOffset = sb.length();
                            sb.append(texts[i]);
                            int endOffset = sb.length();
                            ranges.add(new TextRange(startOffset, endOffset));
                        } else if (clipboardCaretContent.isCharLine(i)) {
                            int startOffset = sb.length();
                            sb.append(texts[i]);
                            int endOffset = sb.length();
                            sb.append(sep);

                            ranges.add(new TextRange(startOffset, endOffset));
                        } else {
                            int startOffset = sb.length();
                            sb.append(texts[i]);
                            int endOffset = sb.length();
                            sb.append(sep);
                            ranges.add(new TextRange(startOffset, endOffset));
                        }
                    }
                }

                final List<TextBlockTransferableData> transferableData = new ArrayList<>();
                int[] startOffsets = new int[ranges.size()];
                int[] endOffsets = new int[ranges.size()];
                int i = 0;
                for (TextRange range : ranges) {
                    startOffsets[i] = range.getStartOffset();
                    endOffsets[i] = range.getEndOffset();
                    i++;
                }

                transferableData.add(new CaretStateTransferableData(startOffsets, endOffsets));
                mergedTransferable = new TextBlockTransferable(sb.toString(), transferableData, null);
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
                doc.insertString(startPosition.getOffset(), s);
                inserted += span;
                Couple<Integer> couple = new Couple<>(selRange.getStart() + inserted, selRange.getEnd() + inserted);
                copies.add(couple);
            }

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

            for (int i = 0; i < iMax; i++) {
                final Couple<Integer> couple = duplicateLineOrSelectedBlockAtCaret(editor, editor.getDocument(), editor.getCaretModel().getPrimaryCaret(), true);
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
                    int offset = editorPosition.getOffset();
                    final EditorPosition atOffset = editorPosition.atOffset(offset);
                    if (atOffset.column != editorPosition.column) {
                        // virtual spaces, add real ones
                        final int inserted = editorPosition.column - atOffset.column;
                        doc.insertString(offset, new RepeatedCharSequence(' ', inserted));
                        offset = editorPosition.getOffset();
                        accumulatedOffset += inserted;
                    }

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
