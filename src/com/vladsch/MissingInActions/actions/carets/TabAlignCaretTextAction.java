// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
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
import com.intellij.openapi.project.Project;
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
import com.vladsch.flexmark.util.misc.CharPredicate;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.RepeatedSequence;
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
        final BasedSequence chars = BasedSequence.of(doc.getCharsSequence());
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
                int spaces = chars.countLeading(CharPredicate.SPACE_TAB, position.getOffset());
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
                /* From the deprecation comment:
                    Use one of the following methods:
                    
                    CodeStyle.getLanguageSettings(PsiFile, Language) to get common settings for a language.
                    CodeStyle.getCustomSettings(PsiFile, Class) to get custom settings.
                    
                    If PsiFile is not applicable, use CodeStyle.getSettings(Project) but only in cases when using main project settings 
                    is logically the only choice in a given context. It shouldn't be used just because the existing code doesn't allow 
                    to easily retrieve a PsiFile. Otherwise, the code will not catch up with proper file code style settings since the 
                    settings may differ for different files depending on their scope.
                    
                    vsch: in this case it is definitely the only choice, since the psiFile is not available when this call is made
                 */
                @SuppressWarnings("deprecation")
                CommonCodeStyleSettings styleSettings = psiFile != null ? CodeStyle.getLanguageSettings(psiFile, language) : CodeStyleSettingsManager.getSettings(project);
                IndentOptions indentOptions = styleSettings.getIndentOptions();
                int tabSize = indentOptions == null ? 4 : indentOptions.TAB_SIZE;
                column += column % tabSize == 0 && unevenColumns ? 0 : tabSize - (column % tabSize);

                // do the editor preview update from source editor, include carets and selections, replacing selections with numbers
                int finalColumn = column;
                carets.sort((o1, o2) -> o2.getOffset() - o1.getOffset());

                WriteCommandAction.runWriteCommandAction(project, () -> {
                    for (Caret caret : carets) {
                        EditorPosition position = f.fromPosition(caret.getLogicalPosition());
                        int spaces = chars.countLeading(CharPredicate.SPACE_TAB, position.getOffset());

                        int count = finalColumn - position.column - spaces;
                        if (count <= 0 || position.column >= position.atEndColumn().column) {
                            caret.moveToLogicalPosition(position.atColumn(finalColumn));
                            continue;
                        }

                        int offset = position.getOffset() + spaces;
                        doc.insertString(offset, RepeatedSequence.ofSpaces(count));
                        caret.moveToLogicalPosition(position.atColumn(finalColumn));
                    }
                });
            }
        }
    }
}
