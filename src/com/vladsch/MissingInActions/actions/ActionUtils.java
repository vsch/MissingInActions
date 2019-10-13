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
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.util.text.CharArrayUtil;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.MissingInActions.manager.EditorPosition;
import com.vladsch.MissingInActions.manager.EditorPositionFactory;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.flexmark.util.sequence.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;

import static java.lang.Math.abs;

public class ActionUtils {

    public static void toggleLineCharacterSelection(@NotNull LineSelectionManager manager, @Nullable Caret caret, boolean wantLine, boolean wantCharacter, boolean preservePrimaryCaretOffset) {
        if (caret == null) return;

        EditorCaret editorCaret = manager.getEditorCaret(caret);

        if (editorCaret.hasSelection()) {
            boolean done = false;
            if (editorCaret.isLine()) {
                if (wantCharacter) {
                    editorCaret.toCharSelection();
                    done = true;
                }
            } else if (wantLine) {
                editorCaret.toLineSelection();
                done = true;
            }

            if (done) {
                if (preservePrimaryCaretOffset) {
                    int caretOffset = manager.getEditor().getCaretModel().getPrimaryCaret().getOffset();
                    editorCaret.setCaretPosition(caretOffset);
                } else {
                    editorCaret.normalizeCaretPosition();
                }
                editorCaret.commit();
            }
        }
    }

    public static void toggleCaretsLineSelection(@Nullable EditorEx editor, boolean wantBlankLines, boolean wantNonBlankLines, boolean convertCharacterSelectionToCarets, boolean convertCharacterSelectionToLines, boolean preservePrimaryCaretOffset) {
        if (editor == null) return;

        final SelectionModel selectionModel = editor.getSelectionModel();
        final CaretModel caretModel = editor.getCaretModel();
        LineSelectionManager manager = LineSelectionManager.getInstance(editor);
        final EditorPositionFactory f = manager.getPositionFactory();

        Caret primaryCaret = caretModel.getPrimaryCaret();
        EditorCaret editorCaret = manager.getEditorCaret(primaryCaret);

        manager.guard(() -> {
            int caretOffset = primaryCaret.getOffset();

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
                        .trimOrExpandToLineSelection();

                // keep caret offset the same as primary caret before toggle
                if (preservePrimaryCaretOffset) editorCaret.setCaretPosition(caretOffset);
                else editorCaret.normalizeCaretPosition();
                editorCaret.commit();
            } else if (selectionModel.hasSelection() && (wantBlankLines || wantNonBlankLines)) {
                // if not line selection then we convert it to line selection, next time to carets
                final DocumentEx doc = editor.getDocument();
                EditorPosition pos = editorCaret.getCaretPosition();
                boolean convertLinesToCarets = true;

                if (!editorCaret.isLine() && (convertCharacterSelectionToCarets || convertCharacterSelectionToLines)) {
                    editorCaret.trimOrExpandToLineSelection();
                    // keep caret offset the same as primary caret before toggle
                    // keep caret offset the same as primary caret before toggle
                    if (preservePrimaryCaretOffset) editorCaret.setCaretPosition(caretOffset);
                    else editorCaret.normalizeCaretPosition();

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
                        int primaryIndex = -1;
                        int primaryOffsetDelta = Integer.MAX_VALUE;
                        int i = 0;

                        for (int lineNumber = selStart.line; lineNumber < endLine; lineNumber++) {
                            // just filter out, blank or non-blank lines
                            int lineEndOffset = doc.getLineEndOffset(lineNumber);
                            int lineStartOffset = doc.getLineStartOffset(lineNumber);

                            boolean isBlank = CharArrayUtil.isEmptyOrSpaces(doc.getCharsSequence(), lineStartOffset, lineEndOffset);
                            if (isBlank && wantBlankLines || !isBlank && wantNonBlankLines) {
                                EditorPosition editorPosition = pos.onLine(lineNumber);
                                Caret caret = i == 0 ? caretModel.getPrimaryCaret() : caretModel.addCaret(editorPosition.toVisualPosition());
                                if (caret != null) {
                                    caret.moveToLogicalPosition(editorPosition);
                                    int offset = editorPosition.getOffset();
                                    caret.setSelection(offset, offset);

                                    int delta = abs(caretOffset - offset);
                                    if (primaryOffsetDelta > delta) {
                                        primaryOffsetDelta = delta;
                                        primaryIndex = i;
                                    }

                                    manager.resetSelectionState(caret);
                                }
                                i++;
                            }
                        }

                        if (preservePrimaryCaretOffset) {
                            // reset to this index
                            setPrimaryCaretIndex(editor, primaryIndex, false);
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

    public static int getPrimaryCaretIndex(@Nullable EditorEx editor, boolean positionSorted) {
        if (editor == null) return 0;
        return getPrimaryCaretIndex(editor.getCaretModel(), positionSorted);
    }

    public static int getPrimaryCaretIndex(@NotNull CaretModel caretModel, boolean positionSorted) {
        int index;
        if (positionSorted) {
            ArrayList<Caret> carets = new ArrayList<>(caretModel.getAllCarets());
            carets.sort(Comparator.comparing(Caret::getLogicalPosition));
            index = carets.indexOf(caretModel.getPrimaryCaret());
        } else {
            index = caretModel.getAllCarets().indexOf(caretModel.getPrimaryCaret());
        }
        return index;
    }

    @Nullable
    public static Caret getCaretAtIndex(@NotNull CaretModel caretModel, int index, boolean positionSorted) {
        if (index < 0 || index > caretModel.getCaretCount()) return null;

        if (positionSorted) {
            ArrayList<Caret> carets = new ArrayList<>(caretModel.getAllCarets());
            carets.sort(Comparator.comparing(Caret::getLogicalPosition));
            return carets.get(index);
        } else {
            return caretModel.getAllCarets().get(index);
        }
    }

    public static boolean setPrimaryCaretIndex(@Nullable EditorEx editor, int newIndex, boolean positionSorted) {
        if (editor == null || newIndex < 0) return false;

        final CaretModel caretModel = editor.getCaretModel();
        if (caretModel.getCaretCount() > newIndex) {
            LineSelectionManager manager = LineSelectionManager.getInstance(editor);
            boolean[] result = { false };

            manager.guard(() -> {
                int index = getPrimaryCaretIndex(caretModel, positionSorted);

                if (newIndex != index) {
                    // need to move the primary to last position in the list
                    // the data will not change just the position in the list, so we swap the two

//                    caretModel.removeSecondaryCarets();
//
//                    ArrayList<CaretState> reOrderedStates = new ArrayList<>(caretStates);
//
//                    int i = 0;
//                    for (CaretState caretState : reOrderedStates) {
//                        LogicalPosition position = caretState.getCaretPosition();
//
//                        if (position != null) {
//                            Caret caret = i == 0 ? caretModel.getPrimaryCaret() : caretModel.addCaret(editor.logicalToVisualPosition(position), i == newIndex);
//                            EditHelpers.restoreState(caret, caretState, true);
//                            i++;
//                        }
//                    }
                    Caret caret = getCaretAtIndex(caretModel, newIndex, positionSorted);
                    if (caret != null) {
                        VisualPosition visualPosition = caret.getVisualPosition();
                        caretModel.removeCaret(caret);
                        caretModel.addCaret(visualPosition, true);
                        manager.updateCaretHighlights();
                        result[0] = true;
                    }
                }
            });

            return result[0];
        }

        return false;
    }
}
