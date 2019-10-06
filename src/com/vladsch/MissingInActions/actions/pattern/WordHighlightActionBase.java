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
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.DumbAware;
import com.vladsch.MissingInActions.Plugin;
import com.vladsch.MissingInActions.actions.ActionUtils;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.plugin.util.ui.highlight.WordHighlighter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

abstract public class WordHighlightActionBase extends AnAction implements DumbAware {
    private final boolean myRemoveWord;
    private final boolean myTandemLine;

    public WordHighlightActionBase(final boolean isRemoveWord, boolean isTandemLine) {
        myRemoveWord = isRemoveWord;
        myTandemLine = isTandemLine;
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        final EditorEx editor = ActionUtils.getEditor(e);
        boolean enabled = false;
        boolean selected = false;

        if (editor != null && editor.getSelectionModel().hasSelection()) {
            enabled = !myRemoveWord || Plugin.getInstance().haveHighlights();
        }
        e.getPresentation().setEnabled(enabled || BatchSearchAction.isShowingBatchSearchWindow(editor));
        super.update(e);

        e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor != null) {
            BatchSearchAction.hideBatchSearchWindow(editor);
            if (!myRemoveWord) Plugin.getInstance().setHighlightsMode(true);

            Document document = editor.getDocument();
            CharSequence chars = document.getCharsSequence();
            Plugin plugin = Plugin.getInstance();
            LineSelectionManager manager = LineSelectionManager.getInstance(editor);
            HashMap<Integer, Integer> lineColorMap = new HashMap<>();
            WordHighlighter<ApplicationSettings> highlighter = (WordHighlighter<ApplicationSettings>) manager.getHighlighter();

            for (Caret caret : editor.getCaretModel().getAllCarets()) {
                if (caret.hasSelection()) {
                    boolean startWord = EditHelpers.isWordStart(chars, caret.getSelectionStart(), false);
                    boolean endWord = EditHelpers.isWordEnd(chars, caret.getSelectionEnd(), false);
                    if (myRemoveWord) {
                        plugin.removeHighlightRange(chars.subSequence(caret.getSelectionStart(), caret.getSelectionEnd()).toString());
                    } else {
                        EditorCaret editorCaret = manager.getEditorCaret(caret);
                        if (myTandemLine && editorCaret.getSelectionStart().line == editorCaret.getSelectionEnd().line) {
                            int tandemIndex = lineColorMap.getOrDefault(editorCaret.getSelectionStart().line, -1);

                            if (tandemIndex == -1 && highlighter != null) {
                                tandemIndex = getRangeHighlighterIndex(highlighter, editorCaret.getSelectionStart().getOffset(), false);
                            }

                            if (tandemIndex == -1) {
                                tandemIndex = plugin.addHighlightRange(chars.subSequence(caret.getSelectionStart(), caret.getSelectionEnd()).toString(), startWord, endWord, false, false, null);
                                lineColorMap.put(editorCaret.getSelectionStart().line, tandemIndex);
                            } else {
                                // on the same line as another highlight, give it the same color
                                plugin.restartHighlightSet(tandemIndex);
                                plugin.addHighlightRange(chars.subSequence(caret.getSelectionStart(), caret.getSelectionEnd()).toString(), startWord, endWord, false, false, null);
                                plugin.endHighlightSet();
                            }
                        } else {
                            plugin.addHighlightRange(chars.subSequence(caret.getSelectionStart(), caret.getSelectionEnd()).toString(), startWord, endWord, false, false, null);
                        }
                    }
                }
            }
        }
    }

    public static int getRangeHighlighterIndex(WordHighlighter<ApplicationSettings> highlighter, int offset, boolean excludeOffset) {
        RangeHighlighter rangeHighlighter = highlighter.getNextRangeHighlighter(offset);
        if (rangeHighlighter != null) {
            if (excludeOffset && offset >= rangeHighlighter.getStartOffset() && offset <= rangeHighlighter.getEndOffset()) {
                rangeHighlighter = highlighter.getNextRangeHighlighter(rangeHighlighter.getEndOffset());
            }
            if (rangeHighlighter != null) {
                return highlighter.getRangeHighlighterIndex(rangeHighlighter);
            }
        }
        return -1;
    }
}
