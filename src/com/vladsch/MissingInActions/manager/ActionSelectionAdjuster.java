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

import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.util.TextRange;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.OneTimeRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.vladsch.MissingInActions.manager.ActionSetType.MOVE_LINE_DOWN_AUTO_INDENT_TRIGGER;
import static com.vladsch.MissingInActions.manager.ActionSetType.MOVE_LINE_UP_AUTO_INDENT_TRIGGER;
import static com.vladsch.MissingInActions.manager.AdjustmentType.*;

public class ActionSelectionAdjuster implements AnActionListener, Disposable {
    final private @NotNull AfterActionList myAfterActions = new AfterActionList();
    final private @NotNull LineSelectionManager myManager;
    final private @NotNull Editor myEditor;
    private @NotNull ActionAdjustmentMap myAdjustmentsMap = ActionAdjustmentMap.EMPTY;
    private final @NotNull AtomicInteger myNestingLevel = new AtomicInteger(0);
    private @Nullable RangeMarker myLastSelectionMarker = null;
    private @Nullable RangeMarker myTentativeSelectionMarker = null;

    final private boolean debug = false;

    // this one is indexed by the class for which the runnable has to be run before
    final private HashSet<OneTimeRunnable> myRunBeforeActions = new HashSet<>();

    // this one is indexed by the class for which the runnable should be canceled since the action is running again
    final private HashMap<Class, HashSet<OneTimeRunnable>> myCancelActionsMap = new HashMap<>();

    public ActionSelectionAdjuster(@NotNull LineSelectionManager manager, @NotNull ActionAdjustmentMap normalAdjustmentMap) {
        myManager = manager;
        myEditor = manager.getEditor();
        myAdjustmentsMap = normalAdjustmentMap;

        ActionManager.getInstance().addAnActionListener(this);
    }

    public void recallLastSelection(boolean swapWithCurrent) {
        if (myLastSelectionMarker != null) {
            if (myLastSelectionMarker.isValid()) {
                // if we have a valid selection we can swap it
                RangeMarker nextSelectionMarker = null;

                if (swapWithCurrent && canSaveAsLastSelection()) {
                    nextSelectionMarker = myEditor.getDocument().createRangeMarker(myEditor.getSelectionModel().getSelectionStart(), myEditor.getSelectionModel().getSelectionEnd());
                }

                // recall the selection
                myEditor.getSelectionModel().setSelection(myLastSelectionMarker.getStartOffset(), myLastSelectionMarker.getEndOffset());

                if (nextSelectionMarker != null) {
                    myLastSelectionMarker.dispose();
                    myLastSelectionMarker = nextSelectionMarker;
                }
            } else {
                myLastSelectionMarker.dispose();
                myLastSelectionMarker = null;
            }
        }
    }

    public boolean haveLastSelection() {
        if (myLastSelectionMarker != null) {
            if (!myLastSelectionMarker.isValid()) {
                myLastSelectionMarker.dispose();
                myLastSelectionMarker = null;
            }
        }
        return myLastSelectionMarker != null;
    }

    @Override
    public void dispose() {
        ActionManager.getInstance().removeAnActionListener(this);
        if (myLastSelectionMarker != null) myLastSelectionMarker.dispose();
        if (myTentativeSelectionMarker != null) myTentativeSelectionMarker.dispose();
        myLastSelectionMarker = null;
        myTentativeSelectionMarker = null;
    }

    @Override
    public void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
        int nesting = myNestingLevel.incrementAndGet();
        myTentativeSelectionMarker = null;

        if (nesting == 1 && canSaveAsLastSelection()) {
            // top level, can tentatively save the current selection
            try {
                myTentativeSelectionMarker = myEditor.getDocument().createRangeMarker(myEditor.getSelectionModel().getSelectionStart(), myEditor.getSelectionModel().getSelectionEnd());
            } catch (UnsupportedOperationException e) {
                myTentativeSelectionMarker = null;
            }
        }

        if (myManager.isLineSelectionSupported() && CommonDataKeys.EDITOR.getData(dataContext) == myEditor) {
            if (!myEditor.isColumnMode()) {
                if (debug) System.out.println("Before " + action + ", nesting: " + myNestingLevel.get());
                cancelTriggeredAction(action.getClass());
                if (!myAdjustmentsMap.hasTriggeredAction(action.getClass())) {
                    runBeforeTriggeredActions();
                }

                AdjustmentType adjustments = myAdjustmentsMap.getAdjustment(action.getClass());
                if (adjustments != null && adjustments != UNDOE_REDO___NOTHING__NOTHING) {

                    //if (debug) System.out.println("running Before " + action);
                    guard(() -> {
                        ApplicationSettings settings = getSettings();
                        adjustBeforeAction(settings, action, adjustments, event);
                    });
                }

                if (myAdjustmentsMap.hasTriggeredAction(action.getClass())) {
                    runAfterAction(event, () -> addTriggeredAction(action.getClass()));
                }
            } else {
                if (debug) System.out.println("Before " + action);
                cancelTriggeredAction(action.getClass());
                runBeforeTriggeredActions();

                // this is not necessarily line mode dependent
                if (myAdjustmentsMap.hasTriggeredAction(action.getClass())) {
                    runAfterAction(event, () -> addTriggeredAction(action.getClass()));
                }
            }
        }
    }

    private boolean canSaveAsLastSelection() {
        return myEditor.getCaretModel().getCaretCount() == 1 && myEditor.getSelectionModel().hasSelection();
    }

    @Override
    public void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
        Collection<Runnable> runnable = myAfterActions.getAfterAction(event);

        if (debug) System.out.println("running After " + action + ", nesting: " + myNestingLevel.get() + "\n");
        try {
            if (runnable != null) {
                // after actions should not check for support, that was done in before, just do what is in the queue
                guard(() -> runnable.forEach(Runnable::run));
            }
        } finally {
            myNestingLevel.decrementAndGet();
        }

        int nesting = myNestingLevel.get();

        if (myLastSelectionMarker != null && !myLastSelectionMarker.isValid()) {
            myLastSelectionMarker.dispose();
            myLastSelectionMarker = null;
        }

        if (myTentativeSelectionMarker != null && !myTentativeSelectionMarker.isValid()) {
            myTentativeSelectionMarker.dispose();
            myTentativeSelectionMarker = null;
        }

        if (nesting == 0 && myTentativeSelectionMarker != null) {
            if (myLastSelectionMarker != null) myLastSelectionMarker.dispose();
            myLastSelectionMarker = myTentativeSelectionMarker;
            myTentativeSelectionMarker = null;
        }
    }

    @Override
    public void beforeEditorTyping(char c, DataContext dataContext) {
        runBeforeTriggeredActions();
        if (myEditor.getSelectionModel().hasSelection() && myManager.getSelectionState(myEditor.getCaretModel().getPrimaryCaret()).isLine() && myEditor.getCaretModel().getCaretCount() == 1) {
            // typing does not delete the selection
            myEditor.getSelectionModel().removeSelection();
        }
    }

    private void runAfterAction(AnActionEvent event, Runnable runnable) {
        myAfterActions.addAfterAction(event, runnable);
    }

    private void guard(Runnable runnable) {
        myManager.guard(runnable);
    }

    /**
     * run commands that are scheduled to run before any other actions
     */
    private void runBeforeTriggeredActions() {
        // remove them from the list of cancellable commands
        ArrayList<OneTimeRunnable> list = new ArrayList<>(myRunBeforeActions);
        myRunBeforeActions.clear();

        for (OneTimeRunnable runnable : list) {
            List<Class> classes = new ArrayList<>(myCancelActionsMap.keySet());
            for (Class key : classes) {
                HashSet<OneTimeRunnable> values = myCancelActionsMap.get(key);
                values.remove(runnable);
                if (values.isEmpty()) {
                    myCancelActionsMap.remove(key);
                }
            }
        }

        list.forEach((runnable) -> {
            if (debug) System.out.println("Running before triggered task " + runnable);
            runnable.run();
        });
    }

    private void cancelTriggeredAction(Class action) {
        Set<OneTimeRunnable> oneTimeRunnables = myCancelActionsMap.remove(action);
        if (oneTimeRunnables != null) {
            for (OneTimeRunnable runnable : oneTimeRunnables) {
                if (debug) System.out.println("Cancelling triggered task " + runnable);
                runnable.cancel();
                myRunBeforeActions.remove(runnable);
            }
        }
    }

    @NotNull
    private AnActionEvent createAnEvent(AnAction action, boolean autoTriggered) {
        Presentation presentation = action.getTemplatePresentation().clone();
        DataContext context = DataManager.getInstance().getDataContext(myEditor.getComponent());
        return new AnActionEvent(null, dataContext(context, true), ActionPlaces.UNKNOWN, presentation, ActionManager.getInstance(), 0);
    }

    private static DataKey<Boolean> AUTO_TRIGGERED_ACTION = DataKey.create("MissingInActions.AUTO_TRIGGERED_ACTION");

    private static DataContext dataContext(@Nullable DataContext parent, boolean autoTriggered) {
        HashMap<String, Object> dataMap = new HashMap<>();
        //dataMap.put(CommonDataKeys.PROJECT.getName(), project);
        //if (editor != null) dataMap.put(CommonDataKeys.EDITOR.getName(), editor);
        dataMap.put(AUTO_TRIGGERED_ACTION.getName(), autoTriggered);
        return SimpleDataContext.getSimpleContext(dataMap, parent);
    }

    private void runAction(AnAction action) {
        AnActionEvent event = createAnEvent(action, true);
        beforeActionPerformed(action, event.getDataContext(), event);
        ActionUtil.performActionDumbAware(action, event);
        afterActionPerformed(action, event.getDataContext(), event);
    }

    /**
     * Schedule a triggered action to run for the given class
     *
     * @param action class of action that just completed
     */
    private void addTriggeredAction(Class action) {
        TriggeredAction triggeredAction = myAdjustmentsMap.getTriggeredAction(action);
        if (triggeredAction != null && triggeredAction.isEnabled()) {
            OneTimeRunnable runnable = new OneTimeRunnable(true, () -> runAction(triggeredAction.getAction()));

            HashSet<OneTimeRunnable> actions = myCancelActionsMap.computeIfAbsent(action, anAction -> new HashSet<>());
            if (debug) System.out.println("Adding triggered task " + runnable);
            actions.add(runnable);

            myRunBeforeActions.add(runnable);

            // now schedule it to run
            OneTimeRunnable.schedule(runnable, triggeredAction.getDelay());
        }
    }

    @SuppressWarnings({ "WeakerAccess", "UnnecessaryReturnStatement" })
    protected void adjustBeforeAction(ApplicationSettings settings, AnAction action, AdjustmentType adjustments, AnActionEvent event) {
        boolean isLineModeEnabled = settings.isLineModeEnabled();

        if (isLineModeEnabled) {
            if (adjustments == MOVE_CARET_LEFT_RIGHT___REMOVE_LINE__NOTHING) {
                // if it is a line selection, then remove it 
                if (settings.isLeftRightMovement()) {
                    if (myEditor.getSelectionModel().hasSelection()) {
                        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                            EditorCaret editorCaret = myManager.getEditorCaret(caret);
                            if (editorCaret.isLine()) {
                                editorCaret.removeSelection();
                                editorCaret.commit();
                            }
                        }
                    }
                }
                return;
            }

            if (adjustments == MOVE_CARET_UP_DOWN_W_SELECTION___TO_CHAR__TO_LINE) {
                if (settings.isUpDownSelection()) {
                    class Params extends LineSelectionState {
                        int column;
                        boolean hadSelection;

                        public Params(int anchorOffset, boolean isStartAnchor, boolean isLine, int column, boolean hadSelection) {
                            super(anchorOffset, isStartAnchor, isLine);
                            this.column = column;
                            this.hadSelection = hadSelection;
                        }

                        public Params(LineSelectionState other, int column, boolean hadSelection) {
                            super(other);
                            this.column = column;
                            this.hadSelection = hadSelection;
                        }
                    }

                    HashMap<Caret, Params> caretParams = new HashMap<>();
                    for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                        EditorCaret editorCaret = myManager.getEditorCaret(caret);
                        if (editorCaret.hasSelection()) {
                            // need to save state locally because it may wind up without selection after the move
                            Params params = new Params(myManager.getSelectionState(caret), editorCaret.getCaretPosition().column, true);
                            caretParams.put(caret, params);

                            editorCaret.setCharSelection()
                                    .normalizeCaretPosition()
                                    .commit();
                        } else {
                            caretParams.put(caret, new Params(editorCaret.getCaretPosition().getOffset(), true, false, editorCaret.getCaretPosition().column, false));
                        }
                    }

                    runAfterAction(event, () -> {
                        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                            Params state = caretParams.get(caret);

                            if (state != null) {
                                if (!caret.hasSelection()) {
                                    myManager.resetSelectionState(caret);
                                }
                                
                                EditorCaret editorCaret = myManager.getEditorCaret(caret);
                                boolean isStartAnchor = state.isStartAnchor();

                                boolean selectionExtendsPastCaret = myManager.isSelectionExtendsPastCaret();
                                if (!state.hadSelection) {
                                    // had no selection
                                    if (caret.hasSelection()) {
                                        // had no selection and has selection: moved up or down 
                                        isStartAnchor = caret.getSelectionStart() == caret.getLeadSelectionOffset();
                                        System.out.println("None before isLine: " + state.isLine() + " Have now isStartAnchor " + isStartAnchor + " anchorOffset: " + state.getAnchorOffset());

                                        if (selectionExtendsPastCaret) {
                                            if (isStartAnchor) {
                                                // did not have a selection, keep caret on same line
                                                editorCaret.onLine(editorCaret.getCaretPosition().atOffset(caret.getLeadSelectionOffset()));
                                            }
                                            editorCaret.removeSelection();
                                            editorCaret.setIsStartAnchor(isStartAnchor);
                                            editorCaret.setLineSelection();
                                            editorCaret.atColumn(state.column)
                                                    .normalizeCaretPosition()
                                                    .commit();
                                        } else {
                                            editorCaret.setLineSelection();
                                            editorCaret.atColumn(state.column)
                                                    .normalizeCaretPosition()
                                                    .commit();
                                        }
                                        
                                    } else {
                                        // had no selection and has no selection - did not move
                                        if (debug) System.out.println("None before None now");
                                    }
                                } else {
                                    if (!caret.hasSelection()) {
                                        // had a selection, but do not now. reset selection anchor offset 
                                        // or we changed direction and need to reset anchor to new caret position
                                        System.out.println("Had before None now, isStartAnchor " + isStartAnchor + " anchorOffset: " + state.getAnchorOffset());

                                        editorCaret.removeSelection();
                                        editorCaret.setIsStartAnchor(state.isStartAnchor());
                                        
                                        if (selectionExtendsPastCaret) {
                                            // keep char to remove the selection
                                            editorCaret.setLineSelection();
                                        }

                                        editorCaret.normalizeCaretPosition()
                                                .commit();
                                    } else {
                                        // had a selection, and has one now, need to adjust the un-anchored end
                                        System.out.println("state.anchor: " + state.getAnchorOffset() + " caret.offset " + caret.getOffset() + " leadOffs: " + caret.getLeadSelectionOffset());

                                        isStartAnchor = caret.getOffset() >= caret.getLeadSelectionOffset();

                                        System.out.println("Had before isStartAnchor: " + state.isStartAnchor() + " Have now isStartAnchor " + isStartAnchor + " anchorOffset: " + state.getAnchorOffset());

                                        editorCaret.atColumn(state.column);

                                        if (editorCaret.isStartAnchor() != isStartAnchor) {
                                            // it will do all adjusting
                                            editorCaret.setIsStartAnchor(isStartAnchor);
                                            editorCaret.setLineSelection();
                                            editorCaret.normalizeCaretPosition()
                                                    .commit();
                                        } else {
                                            // move the non-anchor to the right place
                                            if (selectionExtendsPastCaret || editorCaret.hasLines()) {
                                                editorCaret.setLineSelection();
                                            }
                                            editorCaret.normalizeCaretPosition()
                                                    .commit();
                                        }
                                    }
                                }
                            }
                        }
                    });
                }
                return;
            }

            if (adjustments == MOVE_CARET_LEFT_RIGHT_W_SELECTION___TO_CHAR__NOTHING) {
                if (myEditor.getSelectionModel().hasSelection()) {
                    for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                        if (myManager.hasLineSelection(caret)) {
                            EditorCaret editorCaret = myManager.getEditorCaret(caret);
                            editorCaret.setCharSelection()
                                    .normalizeCaretPosition()
                                    .commit();
                        }
                    }
                }
                return;
            }

            if (adjustments == INDENT_UNINDENT___TO_CHAR__IF_HAS_LINES_TO_LINE_RESTORE_COLUMN) {
                // TODO: may want to create a separate setting for indent/unindent
                if (settings.isUpDownSelection()) {
                    final HashMap<Caret, Integer> caretColumns = new HashMap<>();
                    for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                        if (caret.hasSelection()) {
                            EditorCaret editorCaret = myManager.getEditorCaret(caret);

                            caretColumns.put(caret, editorCaret.getCaretPosition().column);
                            editorCaret.setCharSelection()
                                    .normalizeCaretPosition()
                                    .commit();
                        }
                    }

                    runAfterAction(event, () -> {
                        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                            EditorCaret editorCaret = myManager.getEditorCaret(caret);

                            if (editorCaret.hasLines()) {
                                editorCaret.setLineSelection()
                                        .atColumn(caretColumns.get(caret))
                                        .normalizeCaretPosition()
                                        .commit();
                            }
                        }
                    });
                }
                return;
            }

            if (adjustments == DELETE_LINE_SELECTION___IF_LINE__RESTORE_COLUMN) {
                // delete line selection action
                if (settings.isDeleteOperations()) {
                    final HashMap<Caret, Integer> caretColumns = new HashMap<>();
                    for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                        if (myManager.hasLineSelection(caret)) {
                            caretColumns.put(caret, caret.getLogicalPosition().column);
                        }
                    }

                    if (!caretColumns.isEmpty()) {
                        runAfterAction(event, () -> {
                            for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                                if (caretColumns.containsKey(caret)) {
                                    LogicalPosition pos = new LogicalPosition(caret.getLogicalPosition().line, caretColumns.get(caret));
                                    caret.moveToLogicalPosition(pos);
                                }
                            }
                        });
                    }
                }
                return;
            }

            if (adjustments == MOVE_CARET_UP_DOWN___REMOVE_LINE__RESTORE_COLUMN) {
                if (settings.isUpDownMovement()) {
                    final HashMap<Caret, Integer> caretColumns = new HashMap<>();
                    for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                        caretColumns.put(caret, caret.getLogicalPosition().column);
                        if (myManager.hasLineSelection(caret)) {
                            myManager.resetSelectionState(caret);
                        }
                    }

                    if (!caretColumns.isEmpty()) {
                        runAfterAction(event, () -> {
                            for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                                if (caretColumns.containsKey(caret)) {
                                    LogicalPosition pos = new LogicalPosition(caret.getLogicalPosition().line, caretColumns.get(caret));
                                    caret.moveToLogicalPosition(pos);
                                }
                            }
                        });
                    }
                }
                return;
            }

            if (adjustments == COPY___IF_NO_SELECTION__TO_LINE_RESTORE_COLUMN) {
                final HashMap<Caret, Integer> caretsToFix = new HashMap<>();
                for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                    if (!caret.hasSelection()) {
                        caretsToFix.put(caret, caret.getLogicalPosition().column);
                    }
                }

                runAfterAction(event, () -> {
                    for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                        if (caretsToFix.containsKey(caret)) {
                            EditorCaret editorCaret = myManager.getEditorCaret(caret);
                            editorCaret.atColumn(caretsToFix.get(caret))
                                    .setLineSelection()
                                    .commit();
                        }
                    }
                });

                return;
            }
        }

        if (adjustments == DUPLICATE__CUT___IF_NO_SELECTION__REMOVE_SELECTION___IF_LINE_RESTORE_COLUMN) {
            // duplicate lines and cut action
            class DuplicateCutParams {
                boolean removeSelection = true;
                int fixColumn;
                boolean selectAndMoveCaretToStart;
                //int selStart; // need this to restore selection when duplicate lines before is desired
                //int selEnd;
            }

            final HashMap<Caret, DuplicateCutParams> caretsParams = new HashMap<>();
            for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                if (!caret.hasSelection()) {
                    caretsParams.put(caret, new DuplicateCutParams());
                } else {
                    DuplicateCutParams params = new DuplicateCutParams();
                    params.removeSelection = false;
                    caretsParams.put(caret, params);

                    if (settings.isDuplicateAtStartOrEnd() && myAdjustmentsMap.isInSet(action.getClass(), ActionSetType.DUPLICATE_ACTION)) {
                        EditorCaret editorCaret = myManager.getEditorCaret(caret);
                        if (editorCaret.hasLines() || !settings.isDuplicateAtStartOrEndLineOnly()) {
                            if (!editorCaret.isStartAnchor()) {
                                params.selectAndMoveCaretToStart = true;
                                //params.selStart = editorCaret.getSelectionStart().getOffset();
                                //params.selEnd = editorCaret.getSelectionEnd().getOffset();
                            }

                            if (editorCaret.isLine()) {
                                params.fixColumn = caret.getLogicalPosition().column;
                            }
                        }
                    }
                }
            }

            runAfterAction(event, () -> {
                for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                    EditorCaret editorCaret = myManager.getEditorCaret(caret);
                    DuplicateCutParams params = caretsParams.get(caret);

                    if (params == null || params.removeSelection) {
                        editorCaret.removeSelection();
                    } else {
                        if (editorCaret.hasSelection()) {
                            editorCaret.setIsStartAnchor(!params.selectAndMoveCaretToStart);

                            // TODO: test this is no longer needed
                            //if (params.selectAndMoveCaretToStart && params.selStart < params.selEnd) {
                            //    editorCaret.setSelection(params.selStart, params.selEnd);
                            //    editorCaret.setCaretPosition(params.selStart);
                            //}

                            if (!editorCaret.isLine() && editorCaret.canTrimOrExpandToFullLineSelection()) {
                                // can safely be changed to a line selection
                                editorCaret.trimOrExpandToLineSelection();
                            }

                            if (editorCaret.isLine()) {
                                editorCaret.atColumn(params.fixColumn);
                            }

                            editorCaret.normalizeCaretPosition()
                                    .commit();
                        }
                    }
                }
            });
            return;
        }

        if (adjustments == TOGGLE_CASE___IF_NO_SELECTION__REMOVE_SELECTION) {
            // toggle case adjustment
            if (settings.isUnselectToggleCase()) {
                final HashMap<Caret, Integer> caretColumns = new HashMap<>();
                for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                    if (!caret.hasSelection()) {
                        caretColumns.put(caret, caret.getLogicalPosition().column);
                    }
                }

                runAfterAction(event, () -> {
                    for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                        if (caretColumns.containsKey(caret) && caret.hasSelection()) {
                            caret.removeSelection();
                            myManager.resetSelectionState(caret);
                        }
                    }
                });
            }
            return;
        }

        if (adjustments == JOIN__MOVE_LINES_UP_DOWN___NOTHING__NORMALIZE_CARET_POSITION) {
            // toggle case adjustment
            final HashMap<Caret, Boolean> caretIsStartAnchor = new HashMap<>();
            for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                if (caret.hasSelection()) {
                    caretIsStartAnchor.put(caret, caret.getLeadSelectionOffset() == caret.getSelectionStart());
                }
            }

            runAfterAction(event, () -> {
                for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                    if (caret.hasSelection()) {
                        if (caretIsStartAnchor.containsKey(caret)) {
                            EditorCaret editorCaret = myManager.getEditorCaret(caret);

                            if (myAdjustmentsMap.isInSet(action.getClass(), MOVE_LINE_UP_AUTO_INDENT_TRIGGER)) {
                                // move caret to start of selection
                                editorCaret.setIsStartAnchor(false);
                            } else if (myAdjustmentsMap.isInSet(action.getClass(), MOVE_LINE_DOWN_AUTO_INDENT_TRIGGER)) {
                                // move caret to end of selection
                                editorCaret.setIsStartAnchor(true);
                            } else {
                                editorCaret.setIsStartAnchor(caretIsStartAnchor.get(caret));
                            }

                            editorCaret.normalizeCaretPosition()
                                    .commit();
                        }
                    } else {
                        myManager.resetSelectionState(caret);
                    }
                }
            });
            return;
        }

        if (adjustments == PASTE___MOVE_TO_START__RESTORE_IF0_OR_BLANK_BEFORE) {
            // these can replace selections, need to move to start, after if pasted was lines, then we should restore caret pos
            final HashMap<Caret, Integer> caretColumns = new HashMap<>();
            for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                caretColumns.put(caret, caret.getLogicalPosition().column);

                if (myManager.hasLineSelection(caret)) {
                    EditorCaret editorCaret = myManager.getEditorCaret(caret);

                    if (!editorCaret.isStartAnchor()) {
                        caret.moveToOffset(caret.getSelectionStart());
                    } else {
                        caret.moveToOffset(caret.getSelectionEnd());
                    }
                }
            }

            if (!caretColumns.isEmpty()) {
                runAfterAction(event, () -> {
                    for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                        if (caretColumns.containsKey(caret)) {
                            LogicalPosition position = caret.getLogicalPosition();
                            if (position.column == 0) {
                                LogicalPosition pos = new LogicalPosition(position.line, caretColumns.get(caret));
                                caret.moveToLogicalPosition(pos);
                            }

                            // if leave selected is enabled
                            if (settings.isSelectPasted() && myAdjustmentsMap.isInSet(action.getClass(), ActionSetType.PASTE_ACTION)) {
                                TextRange range = myEditor.getUserData(EditorEx.LAST_PASTED_REGION);
                                if (range != null) {
                                    //myEditor.getCaretModel().moveToOffset(range.getStartOffset());
                                    EditorCaret editorCaret = myManager.getEditorCaret(myEditor.getCaretModel().getPrimaryCaret());

                                    editorCaret.setSelection(range.getStartOffset(), range.getEndOffset());

                                    if (editorCaret.hasLines() || !settings.isSelectPastedLineOnly()) {
                                        if (editorCaret.hasLines()) {
                                            editorCaret.trimOrExpandToLineSelection().normalizeCaretPosition();
                                        } else {
                                            editorCaret.normalizeCaretPosition();
                                        }
                                        editorCaret.commit();
                                    }
                                }
                            }
                        }
                    }
                });
            }
            return;
        }

        if (adjustments == AUTO_INDENT_LINES) {
            Boolean AUTO_TRIGGERED_ACTIONData = AUTO_TRIGGERED_ACTION.getData(event.getDataContext());
            boolean autoTriggered = AUTO_TRIGGERED_ACTIONData != null && AUTO_TRIGGERED_ACTIONData;

            // only process this if auto-triggered, not manual
            if (autoTriggered) {
                class Params {
                    private EditorPosition savedPosition;
                    private int savedIndent;
                    private boolean hadSelection;

                    private Params(EditorPosition savedPosition, boolean hadSelection) {
                        this.savedPosition = savedPosition;
                        this.savedIndent = savedPosition.getIndentColumn();
                        this.hadSelection = hadSelection;
                    }
                }

                final List<Caret> carets = myEditor.getCaretModel().getAllCarets();
                HashMap<Caret, Params> paramList = new HashMap<>();
                EditorPositionFactory f = myManager.getPositionFactory();
                carets.forEach(caret -> {
                    EditorCaret editorCaret = myManager.getEditorCaret(caret);
                    paramList.put(caret, new Params(editorCaret.getCaretPosition(), editorCaret.hasSelection()));
                });

                if (!paramList.isEmpty()) {
                    runAfterAction(event, () -> {
                        // we now restore only caret line not column
                        // and convert selection to line if it was line
                        List<Caret> caretList = myEditor.getCaretModel().getAllCarets();
                        ArrayList<CaretState> caretStates = new ArrayList<>();
                        caretList.forEach(caret -> {
                            Params params = paramList.get(caret);
                            EditorPosition savedPos = params == null ? null : params.savedPosition;
                            Integer savedIndent = params == null ? null : params.savedIndent;
                            boolean hadSelection = params != null && params.hadSelection;

                            if (savedIndent != null && savedPos != null) {
                                EditorCaret editorCaret = myManager.getEditorCaret(caret);
                                int indent = savedPos.getIndentColumn();

                                //editorCaret = editorCaret.withStartIsAnchor(savedCaret.startIsAnchor()).withNormalizedPosition();

                                // copy line state
                                if (!hadSelection) {
                                    // need to fix column position for indentation change
                                    EditorPosition position = editorCaret.getCaretPosition();
                                    if (position.column > 0) {
                                        editorCaret.atColumn(Math.max(0, indent + (savedPos.column - savedIndent)));
                                    }

                                    if (editorCaret.getCaretPosition().line > 0) {
                                        editorCaret.setCaretPosition(editorCaret.getCaretPosition().addLine(-1));
                                    }
                                } else {
                                    // fix for IDEA-164143
                                    EditorPosition selStart = editorCaret.getSelectionStart();
                                    if (selStart.column > 0) {
                                        editorCaret.setSelectionStart(selStart.atColumn(Math.max(0, indent + (selStart.column - savedIndent))));
                                    } else if (editorCaret.getCharSelectionEnd().column == 0) {
                                        editorCaret.setLineSelection();
                                    }
                                }

                                editorCaret.normalizeCaretPosition()
                                        .commit();
                            }
                        });
                    });
                }
            }
            return;
        }
    }

    protected static ApplicationSettings getSettings() {
        return ApplicationSettings.getInstance();
    }

    public static class AfterActionList {
        final private AtomicInteger count = new AtomicInteger(0);
        final private HashMap<AnActionEvent, AfterAction> myEventMap = new HashMap<>();

        public void addAfterAction(@NotNull AnActionEvent event, @NotNull Runnable runnable) {
            AfterAction item = myEventMap.computeIfAbsent(event, e -> new AfterAction(event, count.incrementAndGet()));
            item.add(runnable);
        }

        @Nullable
        public Collection<Runnable> getAfterAction(AnActionEvent event) {
            AfterAction afterAction = myEventMap.remove(event);
            if (afterAction != null) {
                return afterAction.runnables;
            }
            return null;
        }

        private static class AfterAction {
            final AnActionEvent event;
            final LinkedList<Runnable> runnables;
            final int serial;

            AfterAction(AnActionEvent event, int serial) {
                this.event = event;
                this.runnables = new LinkedList<>();
                this.serial = serial;
            }

            public void add(Runnable runnable) {
                runnables.add(runnable);
            }
        }
    }
}
