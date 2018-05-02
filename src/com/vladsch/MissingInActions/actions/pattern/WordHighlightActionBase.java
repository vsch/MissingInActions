/*
 * Copyright (c) 2016-2017 Vladimir Schneider <vladimir.schneider@gmail.com>
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
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.DumbAware;
import com.vladsch.MissingInActions.Plugin;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.EditHelpers;

abstract public class WordHighlightActionBase extends AnAction implements DumbAware {
    private final boolean myIsRemoveWord;

    public WordHighlightActionBase(final boolean isRemoveWord) {
        myIsRemoveWord = isRemoveWord;
    }

    @Override
    public void update(final AnActionEvent e) {
        final EditorEx editor = (EditorEx) CommonDataKeys.EDITOR.getData(e.getDataContext());
        boolean enabled = false;
        boolean selected = false;

        if (editor != null && editor.getSelectionModel().hasSelection()) {
            enabled = !myIsRemoveWord || Plugin.getInstance().haveHighlightedWords();
        }
        e.getPresentation().setEnabled(enabled);
        super.update(e);

        e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor != null) {
            Document document = editor.getDocument();
            CharSequence chars = document.getCharsSequence();
            Plugin plugin = Plugin.getInstance();

            for (Caret caret : editor.getCaretModel().getAllCarets()) {
                if (caret.hasSelection()) {
                    boolean startWord = EditHelpers.isWordStart(chars, caret.getSelectionStart(), false);
                    boolean endWord = EditHelpers.isWordEnd(chars, caret.getSelectionEnd(), false);
                    if (myIsRemoveWord) {
                        plugin.removeHighlightWord(chars.subSequence(caret.getSelectionStart(), caret.getSelectionEnd()));
                    } else {
                        plugin.addHighlightWord(chars.subSequence(caret.getSelectionStart(), caret.getSelectionEnd()), startWord, endWord, null);
                    }
                }
            }
        }
    }
}
