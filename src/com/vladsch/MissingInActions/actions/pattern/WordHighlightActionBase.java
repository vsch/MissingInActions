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
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.DumbAware;
import com.vladsch.MissingInActions.Plugin;
import com.vladsch.MissingInActions.actions.ActionUtils;
import com.vladsch.MissingInActions.manager.EditorPosition;
import com.vladsch.MissingInActions.manager.EditorPositionFactory;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.plugin.util.ui.highlight.WordHighlighter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import static com.vladsch.MissingInActions.util.EditHelpers.isWordEnd;
import static com.vladsch.MissingInActions.util.EditHelpers.isWordStart;

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

        if (editor != null) {
            if (editor.getSelectionModel().hasSelection()) {
                enabled = editor.getSelectionModel().hasSelection()
                        || !myRemoveWord || Plugin.getInstance().haveHighlights()
                        || BatchSearchAction.isShowingBatchSearchWindow(editor);
            }

            if (!enabled) {
                // check is a caret can have word selected 
                CaretModel caretModel = editor.getCaretModel();

                for (Caret caret : caretModel.getAllCarets()) {
                    if (canSelectWord(editor, caret)) {
                        enabled = true;
                        break;
                    }
                }
            }
        }

        e.getPresentation().setEnabled(enabled || BatchSearchAction.isShowingBatchSearchWindow(editor));
        super.update(e);

        e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
    }

    public static boolean canSelectWord(@NotNull Editor editor, @NotNull Caret caret) {
        int offset = caret.getOffset();
        return EditHelpers.isIdentifier(editor.getDocument().getImmutableCharSequence(), offset) || isWordEnd(editor, offset, editor.getSettings().isCamelWords());
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
            boolean haveSelection = editor.getSelectionModel().hasSelection(true);

            for (Caret caret : editor.getCaretModel().getAllCarets()) {
                int selectionStart = 0;
                int selectionEnd = 0;
                int caretOffset = caret.getOffset();

                if (haveSelection) {
                    if (caret.hasSelection()) {
                        selectionStart = caret.getSelectionStart();
                        selectionEnd = caret.getSelectionEnd();
                    }
                } else {
                    // see if we can select a word
                    if (EditHelpers.isIdentifier(chars, caretOffset)) {
                        selectionStart = caretOffset;
                        if (!isWordStart(editor, caretOffset, editor.getSettings().isCamelWords())) {
                            // go to previous word stat
                            selectionStart = EditHelpers.getPreviousWordStartOrEndOffset(editor, caretOffset, editor.getSettings().isCamelWords()
                                    , EditHelpers.START_OF_WORD | EditHelpers.END_OF_WORD | EditHelpers.END_OF_LEADING_BLANKS | EditHelpers.MULTI_CARET_SINGLE_LINE, true);
                        }

                        selectionEnd = EditHelpers.getNextWordStartOrEndOffset(editor, caretOffset, editor.getSettings().isCamelWords()
                                , EditHelpers.START_OF_WORD | EditHelpers.END_OF_WORD | EditHelpers.START_OF_TRAILING_BLANKS | EditHelpers.MULTI_CARET_SINGLE_LINE, true);
                    } else {
                        if (caretOffset > 0 && isWordEnd(editor, caretOffset, editor.getSettings().isCamelWords())) {
                            // go to previous word stat
                            selectionEnd = caretOffset;
                            selectionStart = EditHelpers.getPreviousWordStartOrEndOffset(editor, caretOffset, editor.getSettings().isCamelWords()
                                    , EditHelpers.START_OF_WORD | EditHelpers.END_OF_WORD | EditHelpers.END_OF_LEADING_BLANKS | EditHelpers.MULTI_CARET_SINGLE_LINE, true);
                        }
                    }
                }

                if (selectionEnd > selectionStart) {
                    boolean startWord = isWordStart(chars, selectionStart, false);
                    boolean endWord = EditHelpers.isWordEnd(chars, selectionEnd, false);
                    if (myRemoveWord) {
                        plugin.removeHighlightRange(chars.subSequence(selectionStart, selectionEnd).toString());
                    } else {
                        boolean done = false;

                        if (myTandemLine) {
                            @NotNull EditorPosition mySelectionStart;
                            @NotNull EditorPosition mySelectionEnd;
                            @NotNull EditorPositionFactory myFactory = LineSelectionManager.getInstance(editor).getPositionFactory();
                            mySelectionStart = myFactory.fromOffset(selectionStart);
                            mySelectionEnd = myFactory.fromOffset(selectionEnd);

                            if (mySelectionStart.line == mySelectionEnd.line) {
                                int tandemIndex = lineColorMap.getOrDefault(mySelectionEnd.line, -1);

                                if (tandemIndex == -1 && highlighter != null) {
                                    tandemIndex = getRangeHighlighterIndex(highlighter, mySelectionEnd.getOffset(), false);
                                }

                                if (tandemIndex == -1) {
                                    tandemIndex = plugin.addHighlightRange(chars.subSequence(selectionStart, selectionEnd).toString(), startWord, endWord, 0, null);
                                    lineColorMap.put(mySelectionEnd.line, tandemIndex);
                                } else {
                                    // on the same line as another highlight, give it the same color
                                    plugin.restartHighlightSet(tandemIndex);
                                    plugin.addHighlightRange(chars.subSequence(selectionStart, selectionEnd).toString(), startWord, endWord, 0, null);
                                    plugin.endHighlightSet();
                                }
                                done = true;
                            }
                        }

                        if (!done) {
                            plugin.addHighlightRange(chars.subSequence(selectionStart, selectionEnd).toString(), startWord, endWord, 0, null);
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
