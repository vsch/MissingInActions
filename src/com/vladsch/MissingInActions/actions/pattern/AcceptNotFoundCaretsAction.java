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

package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.vladsch.MissingInActions.actions.CaretSearchAwareAction;
import com.vladsch.MissingInActions.manager.CaretEx;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AcceptNotFoundCaretsAction extends EditorAction implements CaretSearchAwareAction {
    public AcceptNotFoundCaretsAction() {
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
        protected boolean isEnabledForCaret(@NotNull final Editor editor, @NotNull final Caret caret, final DataContext dataContext) {
            return LineSelectionManager.getInstance(editor).getCaretSpawningHandler() != null;
        }

        @Override
        protected void doExecute(final Editor editor, @Nullable final Caret caret, final DataContext dataContext) {
            LineSelectionManager manager = LineSelectionManager.getInstance(editor);
            List<CaretState> caretStates = manager.getStartCaretStates();

            if (caretStates != null) {
                Set<CaretEx> startMatchedCarets = manager.getStartMatchedCarets();
                if (startMatchedCarets != null) {
                    Set<Long> excludeList = CaretEx.getExcludedCoordinates(null, startMatchedCarets);
                    Set<CaretEx> foundCarets = manager.getFoundCarets();
                    excludeList = CaretEx.getExcludedCoordinates(excludeList, foundCarets);
                    List<CaretState> keepCarets = new ArrayList<>(caretStates.size() - startMatchedCarets.size());

                    for (CaretState caretState : caretStates) {
                        if (excludeList != null && excludeList.contains(CaretEx.getCoordinates(caretState.getCaretPosition()))) continue;
                        keepCarets.add(caretState);
                    }

                    manager.clearSearchFoundCarets();
                    editor.getCaretModel().setCaretsAndSelections(keepCarets);
                } else {
                    manager.clearSearchFoundCarets();
                    editor.getCaretModel().setCaretsAndSelections(caretStates);
                }
            }
        }
    }
}