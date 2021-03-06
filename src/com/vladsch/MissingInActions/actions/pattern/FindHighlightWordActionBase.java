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
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.vladsch.MissingInActions.Plugin;
import com.vladsch.MissingInActions.actions.ActionUtils;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.plugin.util.ui.highlight.WordHighlighter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract public class FindHighlightWordActionBase extends AnAction implements DumbAware {
    final boolean myBackwardSearch;

    public FindHighlightWordActionBase(boolean backwardSearch) {
        myBackwardSearch = backwardSearch;
    }

    @Nullable
    Integer find(final AnActionEvent e) {
        EditorEx editor = ActionUtils.getEditor(e);
        if (editor != null) {
            int offset = editor.getCaretModel().getOffset();
            TextRange range = myBackwardSearch ? findPrevious(editor, offset) : findNext(editor, offset);
            if (range != null) {
                //return myBackwardSearch ? range.getStartOffset() : range.getEndOffset();
                return range.getStartOffset();
            }
        }
        return null;
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        boolean enabled = find(e) != null;

        e.getPresentation().setEnabled(enabled);
        super.update(e);

        e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
    }

    public static TextRange findNext(EditorEx editor, int offset) {
        if (editor != null && Plugin.getInstance().haveHighlights()) {
            WordHighlighter<ApplicationSettings> highlighter = (WordHighlighter<ApplicationSettings>) LineSelectionManager.getInstance(editor).getHighlighter();

            if (highlighter != null) {
                RangeHighlighter rangeHighlighter = highlighter.getNextRangeHighlighter(offset);
                if (rangeHighlighter != null) {
                    if (offset >= rangeHighlighter.getStartOffset() && offset <= rangeHighlighter.getEndOffset()) {
                        rangeHighlighter = highlighter.getNextRangeHighlighter(rangeHighlighter.getEndOffset());
                    }
                    if (rangeHighlighter != null) {
                        return TextRange.create(rangeHighlighter.getStartOffset(), rangeHighlighter.getEndOffset());
                    }
                }
            }
        }
        return null;
    }

    public static TextRange findPrevious(EditorEx editor, int offset) {
        if (editor != null && Plugin.getInstance().haveHighlights()) {
            WordHighlighter<ApplicationSettings> highlighter = (WordHighlighter<ApplicationSettings>) LineSelectionManager.getInstance(editor).getHighlighter();

            if (highlighter != null) {
                RangeHighlighter rangeHighlighter = highlighter.getPreviousRangeHighlighter(offset);
                if (rangeHighlighter != null) {
                    if (offset >= rangeHighlighter.getStartOffset() && offset <= rangeHighlighter.getEndOffset()) {
                        rangeHighlighter = highlighter.getPreviousRangeHighlighter(rangeHighlighter.getStartOffset());
                    }
                    if (rangeHighlighter != null) {
                        return TextRange.create(rangeHighlighter.getStartOffset(), rangeHighlighter.getEndOffset());
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        Integer offset = find(e);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor != null && offset != null) {
            Document document = editor.getDocument();
            if (offset <= document.getTextLength()) {
                editor.getCaretModel().getPrimaryCaret().moveToOffset(offset);
                EditHelpers.scrollToSelection(editor);
            }
        }
    }
}
