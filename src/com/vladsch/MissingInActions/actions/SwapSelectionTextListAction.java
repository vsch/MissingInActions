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
package com.vladsch.MissingInActions.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.flexmark.util.sequence.Range;
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
        return editor != null && editor.getSelectionModel().hasSelection() ? new Range(editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd()) : null;
    }

    @Override
    protected void actionPerformed(final AnActionEvent e, Editor editor, @Nullable final RangeMarker previousSelection) {
        LineSelectionManager manager = LineSelectionManager.getInstance(editor);
        RangeMarker rangeMarker = manager.getDummyRangeMarker();
        boolean handled = false;

        if (rangeMarker != null && previousSelection != null) {
            final Range range1 = new Range(rangeMarker.getStartOffset(), rangeMarker.getEndOffset());
            final Range range2 = new Range(previousSelection.getStartOffset(), previousSelection.getEndOffset());

            handled = EditHelpers.swapRangeText(editor, range1, range2);
        }

        if (!handled && previousSelection != null) {
            manager.pushSelection(true, false, false);
            editor.getSelectionModel().setSelection(previousSelection.getStartOffset(), previousSelection.getEndOffset());
            manager.recallLastSelection(0, true, true, true);
        }
    }

    @Override
    public void update(final AnActionEvent e) {
        super.update(e);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);

        if (e.getPresentation().isEnabled() && (editor == null || editor.getCaretModel().getCaretCount() > 1 || !editor.getSelectionModel().hasSelection())) {
            e.getPresentation().setEnabled(false);
        }

        e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
    }
}
