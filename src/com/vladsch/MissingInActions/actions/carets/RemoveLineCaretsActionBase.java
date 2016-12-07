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
 * Date: May 14, 2002
 * Time: 7:40:40 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.vladsch.MissingInActions.actions.carets;

import com.intellij.codeInsight.generation.CommentByBlockCommentHandler;
import com.intellij.lang.Commenter;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.impl.AbstractFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.text.CharArrayUtil;
import com.vladsch.MissingInActions.actions.LineSelectionAware;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.SubSequence;
import org.jetbrains.annotations.Nullable;

import static com.vladsch.MissingInActions.actions.carets.RemoveLineCaretsActionBase.OpType.*;

@SuppressWarnings({ "ComponentNotRegistered", "WeakerAccess" })
public class RemoveLineCaretsActionBase extends AnAction implements LineSelectionAware {

    public enum OpType {
        REMOVE_NONE(false, false, false),
        REMOVE_SMART(false, false, false),  // if has code lines, remove code lines else if has comment remove comment else keep all
        REMOVE_BLANK_LINES(true, false, false),
        REMOVE_LINE_COMMENTS(false, true, false),
        REMOVE_CODE_LINES(false, false, true),
        REMOVE_NON_BLANK_LINES(false, true, true),
        REMOVE_NON_LINE_COMMENTS(true, false, true),
        REMOVE_NON_CODE_LINES(true, true, false),
        KEEP_ALL(false, false, false),
        KEEP_SMART(false, false, false),    // if has code lines, remove non code lines else if has comment remove blank else keep all
        KEEP_BLANK_LINES(false, true, true),
        KEEP_LINE_COMMENTS(true, false, true),
        KEEP_CODE_LINES(true, true, false),
        KEEP_NON_BLANK_LINES(true, false, false),
        KEEP_NON_LINE_COMMENTS(false, true, false),
        KEEP_NON_CODE_LINES(false, false, true);

        public final boolean removeBlankLines;
        public final boolean removeLineComments;
        public final boolean removeCodeLines;

        OpType(boolean removeBlankLines, boolean removeLineComments, boolean removeCodeLines) {
            this.removeBlankLines = removeBlankLines;
            this.removeLineComments = removeLineComments;
            this.removeCodeLines = removeCodeLines;
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
    public void update(AnActionEvent e) {
        EditorEx editor = getEditor(e);
        if (editor == null || editor.isOneLineMode()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setVisible(false);
        } else {
            e.getPresentation().setEnabled(editor.getCaretModel().getCaretCount() > 1);
            e.getPresentation().setVisible(true);
            super.update(e);
        }
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final EditorEx editor = getEditor(e);
        final CaretModel caretModel = editor.getCaretModel();
        final DocumentEx doc = editor.getDocument();

        if (caretModel.getCaretCount() > 1) {
            OpType opType = myOpType;
            Project project = editor.getProject();
            PsiFile psiFile = project == null ? null : PsiManager.getInstance(project).findFile(editor.getVirtualFile());

            boolean hadCodeLine = false;
            boolean hadLineComment = false;
            boolean hadBlankLine = false;

            for (Caret caret : caretModel.getAllCarets()) {
                int lineNumber = doc.getLineNumber(caret.getOffset());
                int lineEndOffset = doc.getLineEndOffset(lineNumber);
                int lineStartOffset = doc.getLineStartOffset(lineNumber);

                if (CharArrayUtil.isEmptyOrSpaces(doc.getCharsSequence(), lineStartOffset, lineEndOffset)) {
                    hadBlankLine = true;
                } else {
                    if (isLineCommented(findCommenter(editor, psiFile, lineNumber), doc.getCharsSequence(), lineStartOffset, lineEndOffset)) {
                        hadLineComment = true;
                    } else {
                        hadCodeLine = true;
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
            } else if (myOpType == REMOVE_SMART) {
                if (hadCodeLine) {
                    opType = REMOVE_CODE_LINES;
                } else if (hadLineComment) {
                    opType = REMOVE_LINE_COMMENTS;
                } else {
                    opType = REMOVE_BLANK_LINES;
                }
            }

            boolean allRemoved = opType.removeBlankLines && !(hadCodeLine || hadLineComment)
                    || opType.removeLineComments && !(hadBlankLine || hadCodeLine)
                    || opType.removeCodeLines && !(hadBlankLine || hadLineComment);

            if (!allRemoved && (opType.removeBlankLines && hadBlankLine || opType.removeLineComments && hadLineComment || opType.removeCodeLines && hadCodeLine)) {
                for (Caret caret : caretModel.getAllCarets()) {
                    int lineNumber = doc.getLineNumber(caret.getOffset());
                    int lineEndOffset = doc.getLineEndOffset(lineNumber);
                    int lineStartOffset = doc.getLineStartOffset(lineNumber);

                    if (CharArrayUtil.isEmptyOrSpaces(doc.getCharsSequence(), lineStartOffset, lineEndOffset)) {
                        if (opType.removeBlankLines) {
                            editor.getCaretModel().removeCaret(caret);
                        }
                    } else {
                        if (isLineCommented(findCommenter(editor, psiFile, lineNumber), doc.getCharsSequence(), lineStartOffset, lineEndOffset)) {
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

    private static EditorEx getEditor(AnActionEvent e) {
        return (EditorEx) CommonDataKeys.EDITOR.getData(e.getDataContext());
    }

    public static boolean isLineCommented(@Nullable Commenter commenter, CharSequence charSequence, int lineStartOffset, int lineEndOffset) {
        if (commenter != null) {
            String lineCommentPrefix = commenter.getLineCommentPrefix();
            String blockCommentPrefix = commenter.getCommentedBlockCommentPrefix();
            if (blockCommentPrefix == null) blockCommentPrefix = commenter.getBlockCommentPrefix();
            String blockCommentSuffix = commenter.getCommentedBlockCommentSuffix();
            if (blockCommentSuffix == null) blockCommentSuffix = commenter.getBlockCommentSuffix();
            BasedSequence chars = new SubSequence(charSequence, lineStartOffset, lineEndOffset);
            BasedSequence trimmed = chars.trim();

            return lineCommentPrefix != null && trimmed.startsWith(lineCommentPrefix) || blockCommentPrefix != null && blockCommentSuffix != null && trimmed.startsWith(blockCommentPrefix) && trimmed.endsWith(blockCommentSuffix);
        } else {
            return false;
        }
    }

    @Nullable
    public static Commenter findCommenter(Editor editor, @Nullable PsiFile file, final int line) {
        if (file != null) {
            final FileType fileType = file.getFileType();
            if (fileType instanceof AbstractFileType) {
                return ((AbstractFileType) fileType).getCommenter();
            }

            Document document = editor.getDocument();
            int lineStartOffset = document.getLineStartOffset(line);
            int lineEndOffset = document.getLineEndOffset(line) - 1;
            final CharSequence charSequence = document.getCharsSequence();
            lineStartOffset = Math.max(0, CharArrayUtil.shiftForward(charSequence, lineStartOffset, " \t"));
            lineEndOffset = Math.max(0, CharArrayUtil.shiftBackward(charSequence, lineEndOffset < 0 ? 0 : lineEndOffset, " \t"));
            final Language lineStartLanguage = PsiUtilCore.getLanguageAtOffset(file, lineStartOffset);
            final Language lineEndLanguage = PsiUtilCore.getLanguageAtOffset(file, lineEndOffset);
            return CommentByBlockCommentHandler.getCommenter(file, editor, lineStartLanguage, lineEndLanguage);
        } else {
            return null;
        }
    }
}
