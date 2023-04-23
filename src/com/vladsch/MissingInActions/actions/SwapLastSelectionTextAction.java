// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.flexmark.util.sequence.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SwapLastSelectionTextAction extends EditorAction {
    public SwapLastSelectionTextAction() {
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
            return LineSelectionManager.getInstance(editor).canSwapSelection() && editor.getCaretModel().getCaretCount() == 1;
        }

        @Override
        protected void doExecute(@NotNull final Editor editor, @Nullable final Caret caret, final DataContext dataContext) {
            LineSelectionManager manager = LineSelectionManager.getInstance(editor);
            RangeMarker previousSelection = manager.getEditorSelectionRangeMarker();
            manager.recallLastSelection(0, true, false, true);
            RangeMarker rangeMarker = manager.getEditorSelectionRangeMarker();
            boolean handled = false;

            if (rangeMarker != null && previousSelection != null) {
                final Range range1 = Range.of(rangeMarker.getStartOffset(), rangeMarker.getEndOffset());
                final Range range2 = Range.of(previousSelection.getStartOffset(), previousSelection.getEndOffset());

                handled = EditHelpers.swapRangeText(editor, range1, range2);
            }

            if (!handled && previousSelection != null) {
                manager.pushSelection(true, false, false);
                editor.getSelectionModel().setSelection(previousSelection.getStartOffset(), previousSelection.getEndOffset());
                manager.recallLastSelection(0, true, true, true);
            }
        }
    }
}
