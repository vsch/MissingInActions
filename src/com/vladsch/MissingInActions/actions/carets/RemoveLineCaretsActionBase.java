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

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: May 14, 2002
 * Time: 7:40:40 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.vladsch.MissingInActions.actions.carets;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.text.CharArrayUtil;
import com.vladsch.MissingInActions.actions.ActionUtils;
import com.vladsch.MissingInActions.actions.LineSelectionAware;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.util.LineCommentProcessor;
import org.jetbrains.annotations.NotNull;

import static com.vladsch.MissingInActions.actions.carets.RemoveLineCaretsActionBase.OpType.*;

@SuppressWarnings({ "ComponentNotRegistered", "WeakerAccess" })
public class RemoveLineCaretsActionBase extends AnAction implements LineSelectionAware {

    public enum OpType {
        REMOVE_NONE(false, false, false, false, false),
        REMOVE_SMART(false, false, false, false, false),  // if has code lines, remove code lines else if has comment remove comment else keep all
        REMOVE_BLANK_LINES(true, false, false, false, false),
        REMOVE_LINE_COMMENTS(false, true, false, false, false),
        REMOVE_CODE_LINES(false, false, true, false, false),
        REMOVE_NON_BLANK_LINES(false, true, true, false, false),
        REMOVE_NON_LINE_COMMENTS(true, false, true, false, false),
        REMOVE_NON_CODE_LINES(true, true, false, false, false),
        REMOVE_WITH_SELECTION(false, false, false, true, false),
        KEEP_ALL(false, false, false, false, false),
        KEEP_SMART(false, false, false, false, false),    // if has code lines, remove non code lines else if has comment remove blank else keep all
        KEEP_BLANK_LINES(false, true, true, false, false),
        KEEP_LINE_COMMENTS(true, false, true, false, false),
        KEEP_CODE_LINES(true, true, false, false, false),
        KEEP_NON_BLANK_LINES(true, false, false, false, false),
        KEEP_NON_LINE_COMMENTS(false, true, false, false, false),
        KEEP_NON_CODE_LINES(false, false, true, false, false),
        REMOVE_WITHOUT_SELECTION(false, false, false, false, true),
        ;

        public final boolean removeBlankLines;
        public final boolean removeLineComments;
        public final boolean removeCodeLines;
        public final boolean removeWithSelection;
        public final boolean removeWithoutSelection;

        OpType(boolean removeBlankLines, boolean removeLineComments, boolean removeCodeLines, boolean removeWithSelection, boolean removeWithoutSelection) {
            this.removeBlankLines = removeBlankLines;
            this.removeLineComments = removeLineComments;
            this.removeCodeLines = removeCodeLines;
            this.removeWithSelection = removeWithSelection;
            this.removeWithoutSelection = removeWithoutSelection;
        }
    }

    final private OpType myOpType;

    public RemoveLineCaretsActionBase(OpType opType) {
        myOpType = opType;
        setEnabledInModalContext(true);
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        EditorEx editor = ActionUtils.getEditor(e);
        if (editor == null || editor.isOneLineMode()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setVisible(true);
        } else {
            boolean enabled = editor.getCaretModel().getCaretCount() > 1;
            if (!enabled) {
                EditorCaret editorCaret = LineSelectionManager.getInstance(editor).getEditorCaret(editor.getCaretModel().getPrimaryCaret());
                enabled = editorCaret.hasSelection() && editorCaret.hasLines();
            }
            e.getPresentation().setEnabled(enabled);
            e.getPresentation().setVisible(true);
            super.update(e);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final EditorEx editor = ActionUtils.getEditor(e);
        if (editor == null) return;

        final CaretModel caretModel = editor.getCaretModel();
        final DocumentEx doc = editor.getDocument();

        if (!(editor.getCaretModel().getCaretCount() > 1)) {
            // convert to
            EditorCaret editorCaret = LineSelectionManager.getInstance(editor).getEditorCaret(editor.getCaretModel().getPrimaryCaret());
            if (editorCaret.hasSelection() && editorCaret.hasLines()) {
                ActionUtils.toggleCaretsLineSelection(editor, true, true, true, true);
            }
        }

        if (editor.getCaretModel().getCaretCount() > 1) {
            OpType opType = myOpType;
            OpType opType2 = null;
            final Project project = editor.getProject();
            final PsiFile psiFile = project == null || editor.getVirtualFile() == null ? null : PsiManager.getInstance(project).findFile(editor.getVirtualFile());
            final LineCommentProcessor lineCommentProcessor = psiFile == null ? null : new LineCommentProcessor(editor, psiFile);

            boolean hadCodeLine = false;
            boolean hadLineComment = false;
            boolean hadBlankLine = false;
            boolean hadSelection = false;
            boolean hadNoSelection = false;

            for (Caret caret : caretModel.getAllCarets()) {
                if (!caret.isValid()) continue;
                if (myOpType == REMOVE_WITH_SELECTION || myOpType == REMOVE_WITHOUT_SELECTION) {
                    if (caret.hasSelection()) {
                        hadSelection = true;
                    } else {
                        hadNoSelection = true;
                    }
                } else {
                    final int lineNumber = doc.getLineNumber(caret.getOffset());
                    final int lineEndOffset = doc.getLineEndOffset(lineNumber);
                    final int lineStartOffset = doc.getLineStartOffset(lineNumber);

                    if (caret.hasSelection()) {
                        hadSelection = true;
                    } else {
                        hadNoSelection = true;
                    }

                    if (CharArrayUtil.isEmptyOrSpaces(doc.getCharsSequence(), lineStartOffset, lineEndOffset)) {
                        hadBlankLine = true;
                    } else {
                        if (lineCommentProcessor != null && lineCommentProcessor.isLineCommented(lineStartOffset, lineEndOffset)) {
                            hadLineComment = true;
                        } else {
                            hadCodeLine = true;
                        }
                    }
                }
            }

            if (myOpType == KEEP_SMART) {
                if (hadCodeLine) {
                    opType = REMOVE_NON_CODE_LINES;
                } else if (hadLineComment) {
                    opType = REMOVE_NON_LINE_COMMENTS;
                } else {
                    opType = REMOVE_NON_BLANK_LINES;
                }

                if (hadSelection && hadNoSelection) {
                    opType2 = REMOVE_WITHOUT_SELECTION;
                }
            } else if (myOpType == REMOVE_SMART) {
                if (hadCodeLine) {
                    opType = REMOVE_CODE_LINES;
                } else if (hadLineComment) {
                    opType = REMOVE_LINE_COMMENTS;
                } else {
                    opType = REMOVE_BLANK_LINES;
                }

                if (hadSelection && hadNoSelection) {
                    opType2 = REMOVE_WITH_SELECTION;
                }
            }

            boolean allRemoved = opType.removeBlankLines && !(hadCodeLine || hadLineComment)
                    || opType.removeLineComments && !(hadBlankLine || hadCodeLine)
                    || opType.removeCodeLines && !(hadBlankLine || hadLineComment)
                    || opType.removeWithSelection && !(hadNoSelection)
                    || opType.removeWithoutSelection && !(hadSelection);

            if (!allRemoved) {
                if (opType.removeWithoutSelection && hadNoSelection ||
                        opType.removeWithSelection && hadSelection ||
                        opType.removeBlankLines && hadBlankLine ||
                        opType.removeLineComments && hadLineComment ||
                        opType.removeCodeLines && hadCodeLine) {
                    for (Caret caret : caretModel.getAllCarets()) {
                        if (!caret.isValid()) continue;
                        if (opType.removeWithSelection || opType.removeWithoutSelection) {
                            if (opType.removeWithSelection && caret.hasSelection() || opType.removeWithoutSelection && !caret.hasSelection()) {
                                editor.getCaretModel().removeCaret(caret);
                            }
                        } else {
                            final int lineNumber = doc.getLineNumber(caret.getOffset());
                            final int lineEndOffset = doc.getLineEndOffset(lineNumber);
                            final int lineStartOffset = doc.getLineStartOffset(lineNumber);

                            if (opType2 != null) {
                                if (opType2.removeWithSelection && caret.hasSelection() || opType2.removeWithoutSelection && !caret.hasSelection()) {
                                    editor.getCaretModel().removeCaret(caret);
                                    continue;
                                }
                            }

                            if (CharArrayUtil.isEmptyOrSpaces(doc.getCharsSequence(), lineStartOffset, lineEndOffset)) {

                                if (opType.removeBlankLines) {
                                    editor.getCaretModel().removeCaret(caret);
                                }
                            } else {
                                if (lineCommentProcessor != null && lineCommentProcessor.isLineCommented(lineStartOffset, lineEndOffset)) {
                                    if (opType.removeLineComments) {
                                        editor.getCaretModel().removeCaret(caret);
                                    }
                                } else {
                                    if (opType.removeCodeLines) {
                                        editor.getCaretModel().removeCaret(caret);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
