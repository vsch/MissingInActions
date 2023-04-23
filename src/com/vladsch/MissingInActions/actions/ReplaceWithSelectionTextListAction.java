// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.util.Pair;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.MissingInActions.util.MiaComboBoxAction;
import com.vladsch.flexmark.util.sequence.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReplaceWithSelectionTextListAction extends SelectionListActionBase {
    protected ReplaceWithSelectionTextListAction() {

    }

    @Override
    protected boolean removeRangeMarker(final AnActionEvent e, Editor editor, @Nullable final RangeMarker previousSelection) {
        return false;
    }

    @Nullable
    @Override
    protected Range excludeOverlap(Editor editor) {
        return editor != null && editor.getSelectionModel().hasSelection() ? Range.of(editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd()) : null;
    }

    @Override
    protected boolean canIncludeSelectionRange(final Document document, final Range range, final Range exclusionRange) {
        if (super.canIncludeSelectionRange(document, range, exclusionRange)) {
            Pair<String, String> pair = EditHelpers.getRangeText(document, range, exclusionRange);
            return pair != null && !pair.first.equals(pair.second);
        }
        return false;
    }

    @Override
    protected void actionPerformed(final AnActionEvent e, Editor editor, @Nullable final RangeMarker previousSelection) {
        LineSelectionManager manager = LineSelectionManager.getInstance(editor);
        RangeMarker recalledSelection = manager.getEditorSelectionRangeMarker();
        boolean handled = false;

        if (recalledSelection != null && previousSelection != null) {
            final Range range1 = Range.of(previousSelection.getStartOffset(), previousSelection.getEndOffset());
            final Range range2 = Range.of(recalledSelection.getStartOffset(), recalledSelection.getEndOffset());

            handled = EditHelpers.replaceRangeText(editor, range1, range2);
            // remove the selection, we don't need it
            editor.getSelectionModel().removeSelection();
        }

        if (!handled && previousSelection != null) {
            editor.getSelectionModel().setSelection(previousSelection.getStartOffset(), previousSelection.getEndOffset());
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
