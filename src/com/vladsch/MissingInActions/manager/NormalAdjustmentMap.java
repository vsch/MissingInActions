// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.manager;

import com.intellij.codeInsight.actions.ReformatCodeAction;
import com.intellij.codeInsight.editorActions.moveUpDown.MoveLineDownAction;
import com.intellij.codeInsight.editorActions.moveUpDown.MoveLineUpAction;
import com.intellij.codeInsight.generation.actions.AutoIndentLinesAction;
import com.intellij.ide.actions.RedoAction;
import com.intellij.ide.actions.UndoAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.actions.*;
import com.vladsch.MissingInActions.actions.PopSelectionAction;
import com.vladsch.MissingInActions.actions.RecallSelectionListAction;
import com.vladsch.MissingInActions.actions.ReplaceWithLastSelectionTextAction;
import com.vladsch.MissingInActions.actions.ReplaceWithSelectionTextListAction;
import com.vladsch.MissingInActions.actions.SwapSelectionAction;
import com.vladsch.MissingInActions.actions.character.MiaMultiplePasteAction;
import com.vladsch.MissingInActions.actions.line.DuplicateForClipboardCaretsAction;
import com.vladsch.MissingInActions.actions.line.DuplicateForMultipleClipboardCaretsAction;
import com.vladsch.MissingInActions.settings.ApplicationSettings;

public class NormalAdjustmentMap extends ActionAdjustmentMap {
    public static NormalAdjustmentMap getInstance() {
        NormalAdjustmentMap service = ApplicationManager.getApplication().getService(NormalAdjustmentMap.class);
        return service;
    }

    NormalAdjustmentMap() {
        addActionAdjustment(AdjustmentType.MOVE_CARET_LEFT_RIGHT___REMOVE_LINE__NOTHING
                , MoveCaretLeftAction.class
                , MoveCaretRightAction.class
        );

        addActionAdjustment(AdjustmentType.MOVE_CARET_UP_DOWN_W_SELECTION___TO_CHAR__TO_LINE
                , MoveCaretDownWithSelectionAction.class
                , MoveCaretUpWithSelectionAction.class
                , MoveDownWithSelectionAndScrollAction.class
                , MoveUpWithSelectionAndScrollAction.class
                , PageBottomWithSelectionAction.class
                , PageDownWithSelectionAction.class
                , PageTopWithSelectionAction.class
                , PageUpWithSelectionAction.class
        );

        addActionAdjustment(AdjustmentType.MOVE_CARET_LEFT_RIGHT_W_SELECTION___TO_CHAR__NOTHING
                , LineEndWithSelectionAction.class
                , LineStartWithSelectionAction.class
                , MoveCaretLeftWithSelectionAction.class
                , MoveCaretRightWithSelectionAction.class
                , NextWordInDifferentHumpsModeWithSelectionAction.class
                , NextWordWithSelectionAction.class
                , PreviousWordInDifferentHumpsModeWithSelectionAction.class
                , PreviousWordWithSelectionAction.class
        );

        addActionAdjustment(AdjustmentType.MOVE_CARET_START_END_W_SELECTION___TO_CHAR__TO_LINE_OR_NOTHING
                , TextStartWithSelectionAction.class
                , TextEndWithSelectionAction.class
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

                , com.vladsch.MissingInActions.actions.line.ToLineSelectionAction.class
                , com.vladsch.MissingInActions.actions.line.ToggleSelectionAnchorAction.class
                , com.vladsch.MissingInActions.actions.line.ClearIsolatedLinesAction.class
                , com.vladsch.MissingInActions.actions.line.ToCharacterSelectionAction.class
                , com.vladsch.MissingInActions.actions.line.ToggleLineCharacterSelectionAction.class

                , com.vladsch.MissingInActions.actions.character.word.NextWordEndInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.NextWordEndWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.NextWordStartInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.NextWordStartWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.PreviousWordEndInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.PreviousWordEndWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.PreviousWordStartInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.PreviousWordStartWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.NextWordInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.NextWordWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.PreviousWordInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.PreviousWordWithSelectionAction.class

                , com.vladsch.MissingInActions.actions.character.identifier.NextWordEndInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.NextWordEndWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.NextWordStartInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.NextWordStartWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.PreviousWordEndInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.PreviousWordEndWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.PreviousWordStartInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.PreviousWordStartWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.NextWordInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.NextWordWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.PreviousWordInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.PreviousWordWithSelectionAction.class

                , com.vladsch.MissingInActions.actions.character.custom.NextWordEndInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.NextWordEndWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.NextWordStartInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.NextWordStartWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.PreviousWordEndInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.PreviousWordEndWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.PreviousWordStartInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.PreviousWordStartWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.NextWordInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.NextWordWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.PreviousWordInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.PreviousWordWithSelectionAction.class
        );

        addActionAdjustment(AdjustmentType.INDENT_UNINDENT___TO_CHAR__IF_HAS_LINES_TO_LINE_RESTORE_COLUMN
                , LangIndentSelectionAction.class
                , IndentLineOrSelectionAction.class
                , UnindentSelectionAction.class
                , IndentSelectionAction.class
        );

        addActionAdjustment(AdjustmentType.DELETE_LINE_SELECTION___IF_LINE__RESTORE_COLUMN
                , BackspaceAction.class
                , DeleteAction.class
                , DeleteToLineEndAction.class
                , DeleteToWordEndAction.class
                , DeleteToWordEndInDifferentHumpsModeAction.class
                , DeleteToWordStartAction.class
                , DeleteToWordStartInDifferentHumpsModeAction.class
        );

        addActionAdjustment(AdjustmentType.MOVE_CARET_UP_DOWN___REMOVE_LINE__RESTORE_COLUMN
                , PageBottomAction.class
                , PageDownAction.class
                , PageTopAction.class
                , PageUpAction.class
                , MoveCaretDownAction.class
                , MoveCaretUpAction.class
        );

        addActionAdjustment(AdjustmentType.REFORMAT_CODE___NOTHING__RESTORE_COLUMN_LINE_END_RELATIVE
                , ReformatCodeAction.class
        );

        addActionAdjustment(AdjustmentType.UNDOE_REDO___NOTHING__NOTHING
                , UndoAction.class
                , RedoAction.class
        );

        addActionAdjustment(AdjustmentType.TOGGLE_CASE___IF_NO_SELECTION__REMOVE_SELECTION
                , ToggleCaseAction.class
        );

        addActionAdjustment(AdjustmentType.COPY___IF_NO_SELECTION__TO_LINE_RESTORE_COLUMN
                , com.intellij.ide.actions.CopyAction.class
                , com.intellij.openapi.editor.actions.CopyAction.class
        );

        addActionAdjustment(AdjustmentType.PASTE___MOVE_TO_START__RESTORE_IF0_OR_BLANK_BEFORE
                , com.intellij.ide.actions.PasteAction.class
                , com.intellij.openapi.editor.actions.PasteAction.class
                , MultiplePasteAction.class
                , PasteFromX11Action.class
                , SimplePasteAction.class
                , MiaMultiplePasteAction.class
                , DuplicateForMultipleClipboardCaretsAction.class
        );

        addActionSet(ActionSetType.PASTE_ACTION
                , com.intellij.ide.actions.PasteAction.class
                , com.intellij.openapi.editor.actions.PasteAction.class
                , MultiplePasteAction.class
                , PasteFromX11Action.class
                , SimplePasteAction.class
                , MiaMultiplePasteAction.class
                , DuplicateForMultipleClipboardCaretsAction.class
        );

        addActionAdjustment(AdjustmentType.AUTO_INDENT_LINES
                , AutoIndentLinesAction.class
        );

        addActionAdjustment(AdjustmentType.DUPLICATE__IF_NO_SELECTION__REMOVE_SELECTION___IF_LINE_RESTORE_COLUMN
                , DuplicateAction.class
                , DuplicateForClipboardCaretsAction.class
                , DuplicateLinesAction.class
        );

        addActionAdjustment(AdjustmentType.CUT___IF_NO_SELECTION__REMOVE_SELECTION___IF_LINE_RESTORE_COLUMN
                , CutAction.class
        );

        addActionSet(ActionSetType.DUPLICATE_ACTION
                , DuplicateAction.class
                , DuplicateLinesAction.class
        );

        addActionSet(ActionSetType.CUT_ACTION
                , CutAction.class
        );

        addActionAdjustment(AdjustmentType.JOIN__MOVE_LINES_UP_DOWN___NOTHING__NORMALIZE_CARET_POSITION
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

        // here are all caret move actions that affect the search carets
        addActionSet(ActionSetType.SELECTION_ALWAYS_STASH
                , ReplaceAction.class
                , IncrementalFindAction.class
        );

        // these actions do not automatically store selection changes in the stash
        addActionSet(ActionSetType.SELECTION_STASH_ACTIONS
                , PopSelectionAction.class
                , SwapSelectionAction.class
                , LineEndWithSelectionAction.class
                , LineStartWithSelectionAction.class
                , MoveCaretDownWithSelectionAction.class
                , MoveCaretLeftWithSelectionAction.class
                , MoveCaretRightWithSelectionAction.class
                , MoveCaretUpWithSelectionAction.class
                , MoveDownWithSelectionAndScrollAction.class
                , MoveUpWithSelectionAndScrollAction.class
                , NextWordWithSelectionAction.class
                , NextWordInDifferentHumpsModeWithSelectionAction.class
                , PageBottomWithSelectionAction.class
                , PageDownWithSelectionAction.class
                , PageTopWithSelectionAction.class
                , PageUpWithSelectionAction.class
                , PreviousWordWithSelectionAction.class
                , PreviousWordInDifferentHumpsModeWithSelectionAction.class
                , TextEndWithSelectionAction.class
                , TextStartWithSelectionAction.class
                , ReplaceWithSelectionTextListAction.class
                , ReplaceWithLastSelectionTextAction.class
                , RecallSelectionListAction.class

                , com.vladsch.MissingInActions.actions.character.word.NextWordEndInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.NextWordEndWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.NextWordStartInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.NextWordStartWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.PreviousWordEndInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.PreviousWordEndWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.PreviousWordStartInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.PreviousWordStartWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.NextWordInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.NextWordWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.PreviousWordInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.word.PreviousWordWithSelectionAction.class

                , com.vladsch.MissingInActions.actions.character.identifier.NextWordEndInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.NextWordEndWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.NextWordStartInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.NextWordStartWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.PreviousWordEndInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.PreviousWordEndWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.PreviousWordStartInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.PreviousWordStartWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.NextWordInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.NextWordWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.PreviousWordInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.PreviousWordWithSelectionAction.class

                , com.vladsch.MissingInActions.actions.character.custom.NextWordEndInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.NextWordEndWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.NextWordStartInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.NextWordStartWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.PreviousWordEndInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.PreviousWordEndWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.PreviousWordStartInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.PreviousWordStartWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.NextWordInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.NextWordWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.PreviousWordInDifferentHumpsModeWithSelectionAction.class
                , com.vladsch.MissingInActions.actions.character.custom.PreviousWordWithSelectionAction.class
        );

        // here are all caret move actions that affect the search carets
        addActionSet(ActionSetType.MOVE_SEARCH_CARET_ACTION
                , LineEndAction.class
                , LineStartAction.class
                , MoveCaretDownAction.class
                , MoveCaretLeftAction.class
                , MoveCaretRightAction.class
                , MoveCaretUpAction.class
                , MoveDownAndScrollAction.class
                , MoveLineDownAction.class
                , MoveUpAndScrollAction.class
                , NextWordAction.class
                , NextWordInDifferentHumpsModeAction.class
                , PageBottomAction.class
                , PageDownAction.class
                , PageTopAction.class
                , PageUpAction.class
                , PreviousWordAction.class
                , PreviousWordInDifferentHumpsModeAction.class
                , TextEndAction.class
                , TextStartAction.class
                , CloneCaretAbove.class
                , CloneCaretBelow.class

                , com.vladsch.MissingInActions.actions.character.word.NextWordEndInDifferentHumpsModeAction.class
                , com.vladsch.MissingInActions.actions.character.word.NextWordEndAction.class
                , com.vladsch.MissingInActions.actions.character.word.NextWordStartInDifferentHumpsModeAction.class
                , com.vladsch.MissingInActions.actions.character.word.NextWordStartAction.class
                , com.vladsch.MissingInActions.actions.character.word.PreviousWordEndInDifferentHumpsModeAction.class
                , com.vladsch.MissingInActions.actions.character.word.PreviousWordEndAction.class
                , com.vladsch.MissingInActions.actions.character.word.PreviousWordStartInDifferentHumpsModeAction.class
                , com.vladsch.MissingInActions.actions.character.word.PreviousWordStartAction.class
                , com.vladsch.MissingInActions.actions.character.word.NextWordInDifferentHumpsModeAction.class
                , com.vladsch.MissingInActions.actions.character.word.NextWordAction.class
                , com.vladsch.MissingInActions.actions.character.word.PreviousWordInDifferentHumpsModeAction.class
                , com.vladsch.MissingInActions.actions.character.word.PreviousWordAction.class

                , com.vladsch.MissingInActions.actions.character.identifier.NextWordEndInDifferentHumpsModeAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.NextWordEndAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.NextWordStartInDifferentHumpsModeAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.NextWordStartAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.PreviousWordEndInDifferentHumpsModeAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.PreviousWordEndAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.PreviousWordStartInDifferentHumpsModeAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.PreviousWordStartAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.NextWordInDifferentHumpsModeAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.NextWordAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.PreviousWordInDifferentHumpsModeAction.class
                , com.vladsch.MissingInActions.actions.character.identifier.PreviousWordAction.class

                , com.vladsch.MissingInActions.actions.character.custom.NextWordEndInDifferentHumpsModeAction.class
                , com.vladsch.MissingInActions.actions.character.custom.NextWordEndAction.class
                , com.vladsch.MissingInActions.actions.character.custom.NextWordStartInDifferentHumpsModeAction.class
                , com.vladsch.MissingInActions.actions.character.custom.NextWordStartAction.class
                , com.vladsch.MissingInActions.actions.character.custom.PreviousWordEndInDifferentHumpsModeAction.class
                , com.vladsch.MissingInActions.actions.character.custom.PreviousWordEndAction.class
                , com.vladsch.MissingInActions.actions.character.custom.PreviousWordStartInDifferentHumpsModeAction.class
                , com.vladsch.MissingInActions.actions.character.custom.PreviousWordStartAction.class
                , com.vladsch.MissingInActions.actions.character.custom.NextWordInDifferentHumpsModeAction.class
                , com.vladsch.MissingInActions.actions.character.custom.NextWordAction.class
                , com.vladsch.MissingInActions.actions.character.custom.PreviousWordInDifferentHumpsModeAction.class
                , com.vladsch.MissingInActions.actions.character.custom.PreviousWordAction.class

                // caret manipulation actions
                , com.vladsch.MissingInActions.actions.carets.RemoveCodeLineCaretsAction.class
                , com.vladsch.MissingInActions.actions.carets.PrimaryCaretToNextAction.class
                , com.vladsch.MissingInActions.actions.carets.KeepBlankLineCaretsAction.class
                , com.vladsch.MissingInActions.actions.carets.KeepSelectionCaretsAction.class
                , com.vladsch.MissingInActions.actions.carets.KeepLineCommentCaretsAction.class
                , com.vladsch.MissingInActions.actions.carets.PrimaryCaretToPrevAction.class
                , com.vladsch.MissingInActions.actions.carets.MovePrimaryCaretToNextPrevCaretBase.class
                , com.vladsch.MissingInActions.actions.carets.RemoveBlankLineCaretsAction.class
                , com.vladsch.MissingInActions.actions.carets.RemoveSelectionCaretsAction.class
                , com.vladsch.MissingInActions.actions.carets.KeepCodeLineCaretsAction.class
                , com.vladsch.MissingInActions.actions.carets.RemoveLineCommentCaretsAction.class
                , com.vladsch.MissingInActions.actions.carets.SmartKeepLineCaretsAction.class
                , com.vladsch.MissingInActions.actions.carets.SmartRemoveLineCaretsAction.class
                , com.vladsch.MissingInActions.actions.carets.StraightenCaretsAction.class
                , com.vladsch.MissingInActions.actions.pattern.SearchCaretsOptionsAction.class
                , com.vladsch.MissingInActions.actions.pattern.BatchSearchAction.class
                //, com.vladsch.MissingInActions.actions.pattern.CancelSearchCaretsAction.class
        );

        // here are all caret move actions that manipulate both search and found carets
        addActionSet(ActionSetType.SEARCH_AWARE_CARET_ACTION
                //, com.vladsch.MissingInActions.actions.pattern.AcceptNotFoundCaretsAction.class
        );
    }
}
