/*
 * Copyright (c) 2016-2016 Vladimir Schneider <vladimir.schneider@gmail.com>
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

package com.vladsch.MissingInActions.actions.carets;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ex.EditorEx;
import com.vladsch.MissingInActions.actions.LineSelectionAware;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.util.EditHelpers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

abstract public class MovePrimaryCaretToNextPrevCaretBase extends AnAction implements LineSelectionAware {
    final protected int myDelta;

    public MovePrimaryCaretToNextPrevCaretBase(int delta) {
        myDelta = delta;
        setEnabledInModalContext(true);
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }

    @Override
    public void update(AnActionEvent e) {
        EditorEx editor = getEditor(e);
        if (editor == null || editor.isOneLineMode()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setVisible(false);
        } else {
            e.getPresentation().setEnabled(editor.getCaretModel().getCaretCount() > 1);
            e.getPresentation().setVisible(true);
            super.update(e);
        }
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final EditorEx editor = getEditor(e);
        final CaretModel caretModel = editor.getCaretModel();
        if (caretModel.getCaretCount() > 1) {
            LineSelectionManager adjuster = LineSelectionManager.getInstance(editor);

            adjuster.guard(() -> {
                Caret primaryCaret = caretModel.getPrimaryCaret();
                List<CaretState> caretStates = caretModel.getCaretsAndSelections();
                caretStates.sort(Comparator.comparing(CaretState::getCaretPosition));

                int index = 0;
                LogicalPosition logicalPosition = primaryCaret.getLogicalPosition();
                for (CaretState caretState : caretStates) {
                    if (logicalPosition.equals(caretState.getCaretPosition())) {
                        break;
                    }
                    index++;
                }

                int newIndex = index + myDelta % caretStates.size();
                if (newIndex < 0) newIndex += caretStates.size(); 

                if (newIndex != index) {
                    // need to move the primary to last position in the list
                    // the data will no change just the position in the list, so we swap the two
                    caretModel.removeSecondaryCarets();

                    ArrayList<CaretState> reOrderedStates = new ArrayList<CaretState>(caretStates);

                    int i = 0;
                    for (CaretState caretState : reOrderedStates) {
                        LogicalPosition position = caretState.getCaretPosition();

                        if (position != null) {
                            Caret caret = i == 0 ? caretModel.getPrimaryCaret() : caretModel.addCaret(position.toVisualPosition(), i == newIndex);
                            EditHelpers.restoreState(caret, caretState, true);
                            i++;
                        }
                    }
                    
                    adjuster.updateCaretHighlights();
                }

                EditHelpers.scrollToCaret(editor);
            });
        }
    }

    private static EditorEx getEditor(AnActionEvent e) {
        return (EditorEx) CommonDataKeys.EDITOR.getData(e.getDataContext());
    }
}
