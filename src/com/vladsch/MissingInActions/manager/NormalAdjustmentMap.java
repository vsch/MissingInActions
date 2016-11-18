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

package com.vladsch.MissingInActions.manager;

import com.intellij.codeInsight.actions.ReformatCodeAction;
import com.intellij.codeInsight.editorActions.moveUpDown.MoveLineDownAction;
import com.intellij.codeInsight.editorActions.moveUpDown.MoveLineUpAction;
import com.intellij.codeInsight.generation.actions.AutoIndentLinesAction;
import com.intellij.ide.actions.RedoAction;
import com.intellij.ide.actions.UndoAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.actions.*;
import com.vladsch.MissingInActions.settings.ApplicationSettings;

import java.util.Collections;

public class NormalAdjustmentMap extends ActionAdjustmentMap {
    public static NormalAdjustmentMap getInstance() {
        return ServiceManager.getService(NormalAdjustmentMap.class);
    }

    NormalAdjustmentMap() {
        addActionAdjustment(AdjustmentType.REMOVE_LINE__NOTHING
                , MoveCaretLeftAction.class
                , MoveCaretRightAction.class
        );

        addActionAdjustment(AdjustmentType.TO_CHAR__TO_LINE
                , MoveCaretDownWithSelectionAction.class
                , MoveCaretUpWithSelectionAction.class
                , MoveDownWithSelectionAndScrollAction.class
                , MoveUpWithSelectionAndScrollAction.class
                , PageBottomWithSelectionAction.class
                , PageDownWithSelectionAction.class
                , PageTopWithSelectionAction.class
                , PageUpWithSelectionAction.class
        );

        addActionAdjustment(AdjustmentType.TO_CHAR__NOTHING
                , LineEndWithSelectionAction.class
                , LineStartWithSelectionAction.class
                , MoveCaretLeftWithSelectionAction.class
                , MoveCaretRightWithSelectionAction.class
                , NextWordInDifferentHumpsModeWithSelectionAction.class
                , NextWordWithSelectionAction.class
                , PreviousWordInDifferentHumpsModeWithSelectionAction.class
                , PreviousWordWithSelectionAction.class
                , TextEndWithSelectionAction.class
                , TextStartWithSelectionAction.class
        );

        addActionAdjustment(AdjustmentType.TO_CHAR__TO_ALWAYS_LINE
        );

        addActionAdjustment(AdjustmentType.IF_LINE__FIX_CARET
                , BackspaceAction.class
                , CutAction.class
                , DeleteAction.class
                , DeleteToLineEndAction.class
                , DeleteToWordEndAction.class
                , DeleteToWordEndInDifferentHumpsModeAction.class
                , DeleteToWordStartAction.class
                , DeleteToWordStartInDifferentHumpsModeAction.class
        );

        addActionAdjustment(AdjustmentType.NOTHING__RESTORE_COLUMN
                , PageBottomAction.class
                , PageDownAction.class
                , PageTopAction.class
                , PageUpAction.class
                , MoveCaretDownAction.class
                , MoveCaretUpAction.class
        );

        addActionAdjustment(AdjustmentType.NOTHING__RESTORE_COLUMN_LINE_END_RELATIVE
                , ReformatCodeAction.class
        );

        addActionAdjustment(AdjustmentType.MOVE_TO_START__RESTORE_IF0
                , com.intellij.ide.actions.PasteAction.class
                , com.intellij.openapi.editor.actions.PasteAction.class
                , MultiplePasteAction.class
                , PasteFromX11Action.class
                , SimplePasteAction.class
        );

        addActionAdjustment(AdjustmentType.NOTHING__NOTHING
                , UndoAction.class
                , RedoAction.class
        );

        addTriggeredAction(new TriggeredAction(new AutoIndentLinesAction(), () -> ApplicationSettings.getInstance().getAutoIndentDelay(), () -> ApplicationSettings.getInstance().isAutoIndent())
                , MoveLineDownAction.class
                , MoveLineUpAction.class
        );
    }
}
