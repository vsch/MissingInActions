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
import com.vladsch.MissingInActions.actions.character.word.*;
import com.vladsch.MissingInActions.actions.character.word.NextWordInDifferentHumpsModeWithSelectionAction;
import com.vladsch.MissingInActions.actions.character.word.NextWordWithSelectionAction;
import com.vladsch.MissingInActions.actions.character.word.PreviousWordInDifferentHumpsModeWithSelectionAction;
import com.vladsch.MissingInActions.actions.character.word.PreviousWordWithSelectionAction;
import com.vladsch.MissingInActions.actions.line.DuplicateBeforeAfterAction;
import com.vladsch.MissingInActions.settings.ApplicationSettings;

public class NormalAdjustmentMap extends ActionAdjustmentMap {
    public static NormalAdjustmentMap getInstance() {
        NormalAdjustmentMap service = ServiceManager.getService(NormalAdjustmentMap.class);
        return service;
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

        addActionSet(ActionSetType.SELECTING_ACTION
                , "com.intellij.codeInsight.editorActions.CodeBlockEndWithSelectionAction"
                , "com.intellij.codeInsight.editorActions.CodeBlockStartWithSelectionAction"
                , LineEndWithSelectionAction.class
                , LineStartWithSelectionAction.class
                , MoveCaretDownWithSelectionAction.class
                , MoveCaretLeftWithSelectionAction.class
                , MoveCaretRightWithSelectionAction.class
                , MoveCaretUpWithSelectionAction.class
                , MoveDownWithSelectionAndScrollAction.class
                , MoveUpWithSelectionAndScrollAction.class
                , NextWordInDifferentHumpsModeWithSelectionAction.class
                , NextWordWithSelectionAction.class
                , PageBottomWithSelectionAction.class
                , PageDownWithSelectionAction.class
                , PageTopWithSelectionAction.class
                , PageUpWithSelectionAction.class
                , PreviousWordInDifferentHumpsModeWithSelectionAction.class
                , PreviousWordWithSelectionAction.class
                , TextEndWithSelectionAction.class
                , TextStartWithSelectionAction.class

                , NextWordEndInDifferentHumpsModeWithSelectionAction.class
                , NextWordEndWithSelectionAction.class
                , NextWordStartInDifferentHumpsModeWithSelectionAction.class
                , NextWordStartWithSelectionAction.class
                , PreviousWordEndInDifferentHumpsModeWithSelectionAction.class
                , PreviousWordEndWithSelectionAction.class
                , PreviousWordStartInDifferentHumpsModeWithSelectionAction.class
                , PreviousWordStartWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.line.ToLineSelectionAction.class
                , com.vladsch.MissingInActions.actions.line.ToggleSelectionAnchorAction.class
                , com.vladsch.MissingInActions.actions.line.ToCharacterSelectionAction.class
                , com.vladsch.MissingInActions.actions.line.ToggleLineCharacterSelectionAction.class
        );

        addActionAdjustment(AdjustmentType.TO_CHAR__TO_ALWAYS_LINE
                , IndentLineOrSelectionAction.class
                , UnindentSelectionAction.class
                , IndentSelectionAction.class
        );

        addActionAdjustment(AdjustmentType.IF_LINE__FIX_CARET
                , BackspaceAction.class
                , DeleteAction.class
                , DeleteToLineEndAction.class
                , DeleteToWordEndAction.class
                , DeleteToWordEndInDifferentHumpsModeAction.class
                , DeleteToWordStartAction.class
                , DeleteToWordStartInDifferentHumpsModeAction.class
        );

        addActionAdjustment(AdjustmentType.REMOVE_LINE__RESTORE_COLUMN
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

        addActionAdjustment(AdjustmentType.NOTHING__NOTHING
                , UndoAction.class
                , RedoAction.class
        );

        addActionAdjustment(AdjustmentType.IF_NO_SELECTION__REMOVE_SELECTION
                , ToggleCaseAction.class
        );

        addActionAdjustment(AdjustmentType.IF_NO_SELECTION__TO_LINE_RESTORE_COLUMN
                , com.intellij.ide.actions.CopyAction.class
                , com.intellij.openapi.editor.actions.CopyAction.class
        );

        addActionAdjustment(AdjustmentType.MOVE_TO_START__RESTORE_IF0_OR_BLANK_BEFORE
                , com.intellij.ide.actions.PasteAction.class
                , com.intellij.openapi.editor.actions.PasteAction.class
                , MultiplePasteAction.class
                , PasteFromX11Action.class
                , SimplePasteAction.class
        );

        addActionSet(ActionSetType.PASTE_ACTION
                , com.intellij.ide.actions.PasteAction.class
                , com.intellij.openapi.editor.actions.PasteAction.class
                , MultiplePasteAction.class
                , PasteFromX11Action.class
                , SimplePasteAction.class
        );

        addActionAdjustment(AdjustmentType.AUTO_INDENT_LINES
                , AutoIndentLinesAction.class
        );

        addActionAdjustment(AdjustmentType.IF_NO_SELECTION__REMOVE_SELECTION___IF_LINE_RESTORE_COLUMN
                , DuplicateAction.class
                , DuplicateBeforeAfterAction.class
                , DuplicateLinesAction.class
                , CutAction.class
        );

        addActionSet(ActionSetType.CUT_ACTION
                , CutAction.class
        );

        addActionAdjustment(AdjustmentType.NOTHING__NORMALIZE_CARET_POSITION
                , JoinLinesAction.class
                , MoveLineDownAction.class
                , MoveLineUpAction.class
        );

        addTriggeredAction(new TriggeredAction(new AutoIndentLinesAction(), () -> ApplicationSettings.getInstance().getAutoIndentDelay(), () -> ApplicationSettings.getInstance().isAutoIndent())
                , MoveLineDownAction.class
                , MoveLineUpAction.class
        );

        addActionSet(ActionSetType.MOVE_LINE_UP_AUTO_INDENT_TRIGGER
                , MoveLineUpAction.class
        );
        
        addActionSet(ActionSetType.MOVE_LINE_DOWN_AUTO_INDENT_TRIGGER
                , MoveLineDownAction.class
        );
    }
}
