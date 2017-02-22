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
    public void update(final AnActionEvent e) {
        super.update(e);
        e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
    }

    private static class Handler extends EditorActionHandler {
        public Handler() {
            super(false);
        }

        @Override
        protected boolean isEnabledForCaret( @NotNull final Editor editor, @NotNull final Caret caret, final DataContext dataContext) {
            return LineSelectionManager.getInstance(editor).canSwapSelection() && editor.getCaretModel().getCaretCount() == 1;
        }

        @Override
        protected void doExecute(final Editor editor, @Nullable final Caret caret, final DataContext dataContext) {
            LineSelectionManager manager = LineSelectionManager.getInstance(editor);
            RangeMarker previousSelection = manager.getDummyRangeMarker();
            manager.recallLastSelection(0, true, false, true);
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
    }
}
