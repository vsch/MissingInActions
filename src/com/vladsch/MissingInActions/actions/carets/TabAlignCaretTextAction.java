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

import com.intellij.application.options.CodeStyle;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings.IndentOptions;
import com.vladsch.MissingInActions.actions.ActionUtils;
import com.vladsch.MissingInActions.actions.LineSelectionAware;
import com.vladsch.MissingInActions.manager.EditorPosition;
import com.vladsch.MissingInActions.manager.EditorPositionFactory;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.BasedSequenceImpl;
import com.vladsch.flexmark.util.sequence.RepeatedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;

public class TabAlignCaretTextAction extends AnAction implements LineSelectionAware {
    public TabAlignCaretTextAction() {
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
            e.getPresentation().setEnabled(editor.getCaretModel().getCaretCount() > 1 && !editor.getSelectionModel().hasSelection());
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
        final BasedSequence chars = BasedSequenceImpl.of(doc.getCharsSequence());
        LineSelectionManager manager = LineSelectionManager.getInstance(editor);
        final EditorPositionFactory f = manager.getPositionFactory();

        if (caretModel.getCaretCount() > 1 && !editor.getSelectionModel().hasSelection()) {
            // insert enough spaces after each caret to move their text to next tab stop and have all carets aligned
            HashMap<Caret, Integer> insertMap = new HashMap<>();
            int column = -1;
            boolean unevenColumns = false;
            List<Caret> carets = caretModel.getAllCarets();
            for (Caret caret : carets) {
                EditorPosition position = f.fromPosition(caret.getLogicalPosition());
                int spaces = chars.countLeading(BasedSequence.WHITESPACE_NO_EOL_CHARS, position.getOffset());
                if (position.column + spaces >= position.atEndColumn().column) continue;

                if (column < position.column + spaces) {
                    if (column != -1) unevenColumns = true;
                    column = position.column + spaces;
                }
            }

            Project project = editor.getProject();
            if (project != null) {
                VirtualFile virtualFile = editor.getVirtualFile();
                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                Language language = psiFile == null ? null : psiFile.getLanguage();
                FileType fileType = virtualFile.getFileType();
                @SuppressWarnings("deprecation")
                CommonCodeStyleSettings styleSettings = psiFile != null ? CodeStyle.getLanguageSettings(psiFile, language) : CodeStyleSettingsManager.getSettings(project);
                IndentOptions indentOptions = styleSettings.getIndentOptions();
                int tabSize = indentOptions == null ? 4 : indentOptions.TAB_SIZE;
                column += column % tabSize == 0 && unevenColumns ? 0 : tabSize - (column % tabSize);

                // do the editor preview update from source editor, include carets and selections, replacing selections with numbers
                int finalColumn = column;
                carets.sort((o1, o2) -> Comparing.compare(o2.getOffset(), o1.getOffset()));

                WriteCommandAction.runWriteCommandAction(project, () -> {
                    for (Caret caret : carets) {
                        EditorPosition position = f.fromPosition(caret.getLogicalPosition());
                        int spaces = chars.countLeading(BasedSequence.WHITESPACE_NO_EOL_CHARS, position.getOffset());

                        int count = finalColumn - position.column - spaces;
                        if (count <= 0 || position.column >= position.atEndColumn().column) {
                            caret.moveToLogicalPosition(position.atColumn(finalColumn));
                            continue;
                        }

                        int offset = position.getOffset() + spaces;
                        doc.insertString(offset, RepeatedCharSequence.of(' ', count));
                        caret.moveToLogicalPosition(position.atColumn(finalColumn));
                    }
                });
            }
        }
    }
}
