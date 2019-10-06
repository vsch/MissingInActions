/*
 * Copyright (c) 2016-2019 Vladimir Schneider <vladimir.schneider@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.vladsch.MissingInActions.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.util.text.CharArrayUtil;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.MissingInActions.manager.EditorPosition;
import com.vladsch.MissingInActions.manager.EditorPositionFactory;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.flexmark.util.sequence.Range;
import org.jetbrains.annotations.Nullable;

public class ActionUtils {

    public static void toggleCaretsLineSelection(@Nullable EditorEx editor, boolean wantBlankLines, boolean wantNonBlankLines, boolean convertCharacterSelectionToCarets, boolean convertCharacterSelectionToLines) {
        if (editor == null) return;

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
                    if (!caret.isValid()) continue;
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
            } else if (selectionModel.hasSelection() && (wantBlankLines || wantNonBlankLines)) {
                // if not line selection then we convert it to line selection, next time to carets
                final DocumentEx doc = editor.getDocument();
                EditorPosition pos = editorCaret.getCaretPosition();
                boolean convertLinesToCarets = true;

                if (!editorCaret.isLine() && (convertCharacterSelectionToCarets || convertCharacterSelectionToLines)) {
                    editorCaret.trimOrExpandToLineSelection();
                    editorCaret.normalizeCaretPosition();
                    if (editorCaret.hasLines() || !convertCharacterSelectionToCarets) {
                        // only commit if actually have lines or do not want carets
                        editorCaret.commit();
                    }
                    convertLinesToCarets = convertCharacterSelectionToCarets;
                }

                if (editorCaret.isLine() && convertLinesToCarets) {
                    EditorPosition selStart = f.fromOffset(selectionModel.getSelectionStart());
                    EditorPosition selEnd = f.fromOffset(selectionModel.getSelectionEnd());

                    caretModel.removeSecondaryCarets();

                    selectionModel.removeSelection();
                    editor.setColumnMode(false);

                    int selectionLineCount = editorCaret.getSelectionLineCount();
                    if (selectionLineCount == 1) {
                        // one liner, we restore char selection
                        editorCaret.toCharSelection()
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
                            if (isBlank && wantBlankLines || !isBlank && wantNonBlankLines) {
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
                }
            }
        });
    }

    @Nullable
    public static EditorEx getEditor(@Nullable AnActionEvent e) {
        if (e == null) return null;

        return (EditorEx) CommonDataKeys.EDITOR.getData(e.getDataContext());
    }
}
