// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.DumbAware;
import com.vladsch.MissingInActions.actions.ActionUtils;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.plugin.util.ui.highlight.HighlightProvider;
import com.vladsch.plugin.util.ui.highlight.WordHighlightProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

abstract public class KeepWordHighlightCaretsActionBase extends AnAction implements DumbAware {
    private final boolean myIsRemoveCaret;

    public KeepWordHighlightCaretsActionBase(final boolean isRemoveCaret) {
        myIsRemoveCaret = isRemoveCaret;
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        final EditorEx editor = ActionUtils.getEditor(e);
        boolean enabled = false;
        boolean selected = false;

        if (editor != null && editor.getSelectionModel().hasSelection()) {
            final HighlightProvider<ApplicationSettings> highlightProvider = LineSelectionManager.getInstance(editor).getHighlightProvider();
            enabled = !myIsRemoveCaret || highlightProvider instanceof WordHighlightProvider && highlightProvider.haveHighlights();
        }
        e.getPresentation().setEnabled(enabled);
        super.update(e);

        e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor != null) {
            final Document document = editor.getDocument();
            final CaretModel caretModel = editor.getCaretModel();
            final CharSequence chars = document.getCharsSequence();
            final HighlightProvider<ApplicationSettings> highlightProvider = LineSelectionManager.getInstance(editor).getHighlightProvider();
            if (highlightProvider instanceof WordHighlightProvider) {
                final WordHighlightProvider<ApplicationSettings> wordHighlightProvider = (WordHighlightProvider<ApplicationSettings>) highlightProvider;

                final List<Caret> removedCarets = new ArrayList<>();
                int removedCaretCount = 0;

                for (Caret caret : caretModel.getAllCarets()) {
                    if (!caret.isValid()) continue;
                    boolean isHighlighted = false;
                    if (caret.hasSelection()) {
                        isHighlighted = wordHighlightProvider.isRangeHighlighted(chars.subSequence(caret.getSelectionStart(), caret.getSelectionEnd()).toString());
                    }

                    if (myIsRemoveCaret == isHighlighted) {
                        removedCarets.add(caret);
                        removedCaretCount++;
                    }
                }

                if (removedCaretCount == caretModel.getCaretCount()) {
                    caretModel.removeSecondaryCarets();
                    caretModel.getPrimaryCaret().removeSelection();
                } else {
                    for (Caret caret : removedCarets) {
                        caretModel.removeCaret(caret);
                    }
                }
            }
        }
    }
}
