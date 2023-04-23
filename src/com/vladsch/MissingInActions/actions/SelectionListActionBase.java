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
package com.vladsch.MissingInActions.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.util.Key;
import com.intellij.ui.components.JBList;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.MiaComboBoxAction;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.PrefixedSubSequence;
import com.vladsch.flexmark.util.sequence.Range;
import icons.PluginIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.BitSet;

abstract public class SelectionListActionBase extends MiaComboBoxAction implements DumbAware {
    private final static Key<RangeHighlighter> RANGE_HIGHLIGHTER = Key.create("MIA_RANGE_HIGHLIGHTER");
    private final static Key<Integer> SCROLL_OFFSET = Key.create("MIA_SCROLL_OFFSET");

    private @Nullable BitSet myDisabledMarkers = null;

    protected SelectionListActionBase() {
        setShowNumbers(true);
    }

    @NotNull
    @Override
    public JComponent createCustomComponent(@NotNull final Presentation presentation, @NotNull String place) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(super.createCustomComponent(presentation, place), BorderLayout.CENTER);
        return panel;
    }

    @Override
    public boolean displayTextInToolbar() {
        return false;
    }

    protected abstract boolean removeRangeMarker(final AnActionEvent e, Editor editor, @Nullable RangeMarker previousSelection);

    protected abstract void actionPerformed(final AnActionEvent e, Editor editor, @Nullable RangeMarker previousSelection);

    @Nullable
    protected Range excludeOverlap(Editor editor) {
        return null;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean canIncludeSelectionRange(Document document, Range range, Range exclusionRange) {
        return !(range.doesContain(exclusionRange) || exclusionRange.doesContain(range));
    }

    @NotNull
    protected DefaultActionGroup createPopupActionGroup(JComponent button, @Nullable Editor editor) {
        final DefaultActionGroup group = new DefaultActionGroup();

        if (editor != null) {
            final LineSelectionManager manager = LineSelectionManager.getInstance(editor);
            final RangeMarker[] markers = manager.getSavedSelections();
            BasedSequence chars = BasedSequence.of(editor.getDocument().getCharsSequence());
            final Range exclusionRange = excludeOverlap(editor);

            //System.out.println("createPopAction: saved markers" + markers.length);

            for (int i = markers.length; i-- > 0; ) {
                RangeMarker marker = markers[i];

                BasedSequence text = chars.subSequence(marker.getStartOffset(), marker.getEndOffset());
                BasedSequence label = text;

                if (text.length() > 40) {
                    label = text.subSequence(0, 20).append(PrefixedSubSequence.prefixOf("…", text.subSequence(20, 20))).append(text.endSequence(20, 0));
                }

                label = label.replace("\n", "⏎");

                final int index = markers.length - i - 1;
                Document document = marker.getDocument();
                final int startLine = document.getLineNumber(marker.getStartOffset());
                final int endLine = document.getLineNumber(marker.getEndOffset());
                final int startColumn = marker.getStartOffset() - document.getLineStartOffset(startLine);
                final Icon icon;
                boolean disabled = false;
                boolean isSameRange = false;

                if (exclusionRange != null) {
                    Range range = Range.of(marker.getStartOffset(), marker.getEndOffset());
                    disabled = isSameRange = exclusionRange.equals(range);
                    if (!disabled) {
                        disabled = !canIncludeSelectionRange(document, range, exclusionRange);
                    }
                }

                if (disabled) {
                    icon = PluginIcons.Clipboard_disabled_caret;
                    // only the range which is fully contained and the first one (because selections are not duplicated so there can only be one extra one)
                    if (isSameRange) {
                        if (myDisabledMarkers == null) myDisabledMarkers = new BitSet();
                        myDisabledMarkers.set(i);
                    }
                } else {
                    if (text.endsWith("\n") && startColumn == 0) {
                        icon = PluginIcons.Clipboard_line_caret;
                    } else if (startLine != endLine) {
                        icon = PluginIcons.Clipboard_char_lines_caret;
                    } else {
                        icon = PluginIcons.Clipboard_char_caret;
                    }
                }

                final String actionText = String.format("line %d \"%s\"", startLine, text);
                final boolean isDisabled = disabled;

                DumbAwareAction action = new DumbAwareAction("\"" + label + "\"", actionText, icon) {
                    @Override
                    public void actionPerformed(@NotNull final AnActionEvent e) {
                        if (!isDisabled) {
                            RangeMarker selectionRangeMarker = manager.getRangeMarker();
                            manager.recallLastSelection(index, removeRangeMarker(e, editor, selectionRangeMarker), false, true);
                            SelectionListActionBase.this.actionPerformed(e, editor, selectionRangeMarker);
                        }
                    }
                };

                group.add(action);
            }
        } else {
            //noinspection unused
            int tmp = 0;
        }

        return group;
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void actionSelected(@Nullable final Editor editor, final ListSelectionEvent e) {
        if (editor != null) {
            JBList listPopup = (JBList) e.getSource();
            showSelection(editor, listPopup.getSelectedIndex());
        }
    }

    private void showSelection(final @Nullable Editor editor, int listIndex) {
        if (editor == null) return;

        MarkupModel markupModel = editor.getMarkupModel();
        RangeHighlighter oldHighlighter = editor.getUserData(RANGE_HIGHLIGHTER);
        if (oldHighlighter != null) markupModel.removeHighlighter(oldHighlighter);
        final LineSelectionManager manager = LineSelectionManager.getInstance(editor);
        RangeMarker[] markers = manager.getSavedSelections();

        int markerIndex = markers.length - listIndex - 1;
        if (markerIndex < 0 || markerIndex >= markers.length) {
            //System.out.println("showSelection: saved markers" + markers.length);
            return;
        }

        if (myDisabledMarkers != null && myDisabledMarkers.get(markerIndex)) {
            return;
        }

        RangeMarker marker = markers[markerIndex];

        ApplicationSettings settings = ApplicationSettings.getInstance();
        Color color = settings.isRecalledSelectionColorEnabled() ? settings.recalledSelectionColorRGB() : EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.SELECTION_BACKGROUND_COLOR);
        RangeHighlighter rangeHighlighter = markupModel.addRangeHighlighter(marker.getStartOffset(), marker.getEndOffset(), HighlighterLayer.LAST + 1, new TextAttributes(null, color, null, EffectType.BOLD_DOTTED_LINE, 0), HighlighterTargetArea.EXACT_RANGE);
        editor.putUserData(RANGE_HIGHLIGHTER, rangeHighlighter);
        oldHighlighter = editor.getUserData(RANGE_HIGHLIGHTER);
        assert rangeHighlighter == oldHighlighter;

        editor.getScrollingModel().scrollTo(editor.offsetToLogicalPosition(marker.getEndOffset()), ScrollType.MAKE_VISIBLE);
        editor.getScrollingModel().scrollTo(editor.offsetToLogicalPosition(marker.getStartOffset()), ScrollType.MAKE_VISIBLE);
    }

    @Override
    protected void popupStart(@Nullable final Editor editor) {
        super.popupStart(editor);

        if (editor != null) {
            editor.putUserData(SCROLL_OFFSET, editor.getScrollingModel().getVerticalScrollOffset());
            showSelection(editor, 0);
        }
    }

    @Override
    protected void popupDone(@Nullable final Editor editor) {
        super.popupDone(editor);

        if (editor != null) {
            MarkupModel markupModel = editor.getMarkupModel();
            RangeHighlighter oldHighlighter = editor.getUserData(RANGE_HIGHLIGHTER);
            if (oldHighlighter != null) {
                markupModel.removeHighlighter(oldHighlighter);
            }
            editor.putUserData(RANGE_HIGHLIGHTER, null);

            Integer offset = editor.getUserData(SCROLL_OFFSET);
            if (offset != null) {
                editor.getScrollingModel().scroll(editor.getScrollingModel().getHorizontalScrollOffset(), offset);
            }
            editor.putUserData(SCROLL_OFFSET, null);
        }

        myDisabledMarkers = null;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);

        Editor editor = getEventEditor(e);
        if (editor != null) {
            final LineSelectionManager manager = LineSelectionManager.getInstance(editor);
            e.getPresentation().setEnabled(manager.canRecallSelection());
        } else {
            e.getPresentation().setEnabled(false);
        }

        e.getPresentation().setVisible(true);
    }
}
