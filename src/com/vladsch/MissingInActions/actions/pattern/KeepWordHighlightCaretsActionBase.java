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

package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.DumbAware;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.plugin.util.ui.highlight.HighlightProvider;
import com.vladsch.plugin.util.ui.highlight.WordHighlightProvider;

import java.util.ArrayList;
import java.util.List;

abstract public class KeepWordHighlightCaretsActionBase extends AnAction implements DumbAware {
    private final boolean myIsRemoveCaret;

    public KeepWordHighlightCaretsActionBase(final boolean isRemoveCaret) {
        myIsRemoveCaret = isRemoveCaret;
    }

    @Override
    public void update(final AnActionEvent e) {
        final EditorEx editor = (EditorEx) CommonDataKeys.EDITOR.getData(e.getDataContext());
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
