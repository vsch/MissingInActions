// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RecallSelectionListAction extends SelectionListActionBase {
    protected RecallSelectionListAction() {

    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        super.update(e);
        //e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
    }

    @Override
    protected boolean removeRangeMarker(final AnActionEvent e, Editor editor, @Nullable final RangeMarker previousSelection) {
        return true;
    }

    @Override
    protected void actionPerformed(final AnActionEvent e, Editor editor, @Nullable final RangeMarker previousSelection) {
        if (previousSelection != null) {
            LineSelectionManager.getInstance(editor).pushSelection(previousSelection, true, true, true);
        }
    }
}
