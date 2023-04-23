// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.line;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;

public class AddIsolationLinesAction extends EditorAction {
    public AddIsolationLinesAction() {
        super(new Handler());
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        super.update(e);
        e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
    }

    private static class Handler extends EditorActionHandler {
        public Handler() {
            super(false);
        }

        @Override
        protected boolean isEnabledForCaret(@NotNull final Editor editor, @NotNull final Caret caret, final DataContext dataContext) {
            LineSelectionManager manager = LineSelectionManager.getInstance(editor);
            if (!manager.isIsolatedMode()) return true;

            BitSet bitSet = manager.getIsolatedLines();
            Document document = editor.getDocument();

            if (bitSet == null) return true;

            if (editor.getCaretModel().getCaretCount() == 1) {
                if (caret.hasSelection()) {
                    int startLine = document.getLineNumber(caret.getSelectionStart());
                    int endLine = document.getLineNumber(caret.getSelectionEnd() > 0 ? caret.getSelectionEnd() - 1 : 0);
                    BitSet selectedLines = new BitSet();
                    selectedLines.set(startLine, endLine + 1);
                    selectedLines.and(bitSet);
                    if (selectedLines.cardinality() < endLine - startLine + 1) return true;
                } else {
                    if (!bitSet.get(caret.getLogicalPosition().line)) return true;
                }
            } else {
                for (Caret caret1 : editor.getCaretModel().getAllCarets()) {
                    if (caret1.hasSelection()) {
                        int startLine = document.getLineNumber(caret1.getSelectionStart());
                        int endLine = document.getLineNumber(caret1.getSelectionEnd() > 0 ? caret1.getSelectionEnd() - 1 : 0);
                        BitSet selectedLines = new BitSet();
                        selectedLines.set(startLine, endLine + 1);
                        selectedLines.and(bitSet);
                        if (selectedLines.cardinality() < endLine - startLine + 1) return true;
                    } else {
                        if (!bitSet.get(caret1.getLogicalPosition().line)) return true;
                    }
                }
            }

            return false;
        }

        @Override
        protected void doExecute(@NotNull final Editor editor, @Nullable final Caret caret, final DataContext dataContext) {
            LineSelectionManager manager = LineSelectionManager.getInstance(editor);
            BitSet bitSet = manager.isIsolatedMode() ? manager.getIsolatedLines() : null;
            Document document = editor.getDocument();
            if (bitSet == null) bitSet = new BitSet();

            if (caret != null) {
                if (caret.hasSelection()) {
                    int startLine = document.getLineNumber(caret.getSelectionStart());
                    int endLine = document.getLineNumber(caret.getSelectionEnd() > 0 ? caret.getSelectionEnd() - 1 : 0);
                    bitSet.set(startLine, endLine + 1);
                } else {
                    bitSet.set(caret.getLogicalPosition().line);
                }
            } else {
                for (Caret caret1 : editor.getCaretModel().getAllCarets()) {
                    if (caret1.hasSelection()) {
                        int startLine = document.getLineNumber(caret1.getSelectionStart());
                        int endLine = document.getLineNumber(caret1.getSelectionEnd() > 0 ? caret1.getSelectionEnd() - 1 : 0);
                        bitSet.set(startLine, endLine + 1);
                    } else {
                        bitSet.set(caret1.getLogicalPosition().line);
                    }
                }
            }

            manager.setIsolatedLines(bitSet, true);
        }
    }
}
