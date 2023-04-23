// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.MissingInActions.util.MiaComboBoxAction;
import com.vladsch.flexmark.util.sequence.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SwapSelectionTextListAction extends SelectionListActionBase {
    protected SwapSelectionTextListAction() {

    }

    @Override
    protected boolean removeRangeMarker(final AnActionEvent e, Editor editor, @Nullable final RangeMarker previousSelection) {
        return true;
    }

    @Nullable
    @Override
    protected Range excludeOverlap(Editor editor) {
        return editor != null && editor.getSelectionModel().hasSelection() ? Range.of(editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd()) : null;
    }

    @Override
    protected void actionPerformed(final AnActionEvent e, Editor editor, @Nullable final RangeMarker previousSelection) {
        LineSelectionManager manager = LineSelectionManager.getInstance(editor);
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

    @Override
    public void update(@NotNull final AnActionEvent e) {
        super.update(e);

        Editor editor = MiaComboBoxAction.getEventEditor(e);
        boolean enabled = !(editor == null || editor.getCaretModel().getCaretCount() > 1 || !editor.getSelectionModel().hasSelection());
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
    }
}
