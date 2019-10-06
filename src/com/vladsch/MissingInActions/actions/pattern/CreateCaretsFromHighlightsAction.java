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
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.DumbAware;
import com.vladsch.MissingInActions.Plugin;
import com.vladsch.MissingInActions.actions.ActionUtils;
import com.vladsch.MissingInActions.actions.LineSelectionAware;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.MissingInActions.manager.EditorCaretState;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.plugin.util.ui.highlight.WordHighlighter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CreateCaretsFromHighlightsAction extends AnAction implements LineSelectionAware, DumbAware {
    public CreateCaretsFromHighlightsAction() {
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        final EditorEx editor = ActionUtils.getEditor(e);
        boolean enabled = false;
        boolean selected = false;

        if (editor != null && Plugin.getInstance().haveHighlights()) {
            LineSelectionManager manager = LineSelectionManager.getInstance(editor);
            WordHighlighter<ApplicationSettings> highlighter = (WordHighlighter<ApplicationSettings>) manager.getHighlighter();

            if (highlighter != null) {
                EditorCaret editorCaret = getSelectionRange(editor, manager);
                int selectionStart = editorCaret.getSelectionStart().getOffset();
                int selectionEnd = editorCaret.getSelectionEnd().getOffset();
                RangeHighlighter rangeHighlighter = getRangeHighlighter(highlighter, selectionStart, false);
                enabled = rangeHighlighter != null && rangeHighlighter.getEndOffset() <= selectionEnd;
            }
        }
        e.getPresentation().setEnabled(enabled);
        super.update(e);

        e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor != null && Plugin.getInstance().haveHighlights()) {
            LineSelectionManager manager = LineSelectionManager.getInstance(editor);
            WordHighlighter<ApplicationSettings> highlighter = (WordHighlighter<ApplicationSettings>) manager.getHighlighter();

            if (highlighter != null) {
                final CaretModel caretModel = editor.getCaretModel();
                EditorCaret editorCaret = getSelectionRange(editor, manager);

                int selectionStart = editorCaret.getSelectionStart().getOffset();
                int selectionEnd = editorCaret.getSelectionEnd().getOffset();

                if (caretModel.getCaretCount() == 1) {
                    editor.getSelectionModel().removeSelection();
                    int offset = selectionStart;
                    Caret caret = null;

                    while (offset < selectionEnd) {
                        boolean newPrimary = caret == null;
                        RangeHighlighter rangeHighlighter = getRangeHighlighter(highlighter, offset, caret != null);

                        if (rangeHighlighter != null && rangeHighlighter.getEndOffset() <= selectionEnd) {
                            VisualPosition visualPosition = editor.offsetToVisualPosition(rangeHighlighter.getEndOffset());
                            caret = caretModel.addCaret(visualPosition, newPrimary);
                            if (caret == null) break;
                            if (newPrimary) caretModel.removeSecondaryCarets();
                            caret.setSelection(rangeHighlighter.getStartOffset(), rangeHighlighter.getEndOffset());
                            offset = rangeHighlighter.getEndOffset();
                            continue;
                        }
                        break;
                    }
                } else {
                    // we adjust each caret's selection
                    Caret caret = null;

                    List<CaretState> caretStates = caretModel.getCaretsAndSelections();
                    editor.getSelectionModel().removeSelection();
                    caretModel.removeSecondaryCarets();

                    for (CaretState state : caretStates) {
                        boolean isHighlighted = false;
                        boolean newPrimary = caret == null;
                        EditorCaretState caretState = new EditorCaretState(manager.getPositionFactory(), state);
                        if (!(caretState.getSelectionStart() != null && caretState.getSelectionEnd() != null && caretState.hasSelection())) continue;

                        if (caretState.getSelectionStart().getOffset() < caretState.getSelectionEnd().getOffset()) {
                            RangeHighlighter rangeHighlighter = getRangeHighlighter(highlighter, caretState.getSelectionStart().getOffset(), false);
                            if (rangeHighlighter != null && rangeHighlighter.getEndOffset() <= caretState.getSelectionEnd().getOffset()) {
                                VisualPosition visualPosition = editor.offsetToVisualPosition(rangeHighlighter.getEndOffset());
                                caret = caretModel.addCaret(visualPosition, newPrimary);
                                if (caret == null) break;
                                if (newPrimary) caretModel.removeSecondaryCarets();
                                caret.setSelection(rangeHighlighter.getStartOffset(), rangeHighlighter.getEndOffset());
                            }
                        }
                    }

                    if (caret == null) {
                        // don't remove anything. The user can do it easily enough
                        caretModel.setCaretsAndSelections(caretStates);
                    }
                }
            }
        }
    }

    public static RangeHighlighter getRangeHighlighter(WordHighlighter<ApplicationSettings> highlighter, int offset, boolean excludeOffset) {
        RangeHighlighter rangeHighlighter = highlighter.getNextRangeHighlighter(offset);
        if (rangeHighlighter != null) {
            if (excludeOffset && offset >= rangeHighlighter.getStartOffset() && offset <= rangeHighlighter.getEndOffset()) {
                rangeHighlighter = highlighter.getNextRangeHighlighter(rangeHighlighter.getEndOffset());
            }
        }
        return rangeHighlighter;
    }

    @NotNull
    public static EditorCaret getSelectionRange(Editor editor, LineSelectionManager manager) {
        CaretModel caretModel = editor.getCaretModel();
        EditorCaret editorCaret = manager.getEditorCaret(caretModel.getPrimaryCaret());

        if (editor.getSelectionModel().hasSelection()) {
            editorCaret.setSelection(editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd());
//            if (editorCaret.hasSelection()) editorCaret.toLineSelection().normalizeCaretPosition();
        } else {
            editorCaret.setSelection(0, editor.getDocument().getTextLength());
        }
        return editorCaret;
    }
}
