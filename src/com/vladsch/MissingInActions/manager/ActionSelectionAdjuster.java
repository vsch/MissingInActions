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
import com.intellij.openapi.editor.actions.MoveCaretUpWithSelectionAction;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.util.TextRange;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.MissingInActions.util.OneTimeRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.vladsch.MissingInActions.Plugin.getCaretInSelection;
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
            myTentativeSelectionMarker = myEditor.getDocument().createRangeMarker(myEditor.getSelectionModel().getSelectionStart(), myEditor.getSelectionModel().getSelectionEnd());
        }

        if (myManager.isAdjustmentSupported() && CommonDataKeys.EDITOR.getData(dataContext) == myEditor) {
            if (getSettings().isLineModeEnabled() && !myEditor.isColumnMode()) {

                if (debug) System.out.println("Before " + action + ", nesting: " + myNestingLevel.get());
                cancelTriggeredAction(action.getClass());
                if (!myAdjustmentsMap.hasTriggeredAction(action.getClass())) {
                    runBeforeTriggeredActions();
                }

                AdjustmentType adjustments = myAdjustmentsMap.getAdjustment(action.getClass());
                if (adjustments != null && adjustments != NOTHING__NOTHING) {

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
        if (runnable != null) {
            // after actions should not check for support, that was done in before, just do what is in the queue
            if (debug) System.out.println("running After " + action + ", nesting: " + myNestingLevel.get() + "\n");
            guard(() -> runnable.forEach(Runnable::run));
        }

        int nesting = myNestingLevel.decrementAndGet();

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
        for (OneTimeRunnable runnable : myRunBeforeActions) {
            for (Class key : myCancelActionsMap.keySet()) {
                HashSet<OneTimeRunnable> values = myCancelActionsMap.get(key);
                values.remove(runnable);
                if (values.isEmpty()) {
                    myCancelActionsMap.remove(key);
                }
            }

            if (debug) System.out.println("Running before triggered task " + runnable);
            runnable.run();
        }

        myRunBeforeActions.clear();
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
            if (adjustments == REMOVE_LINE__NOTHING) {
                // if it is a line selection, then remove it 
                if (settings.isLeftRightMovement()) {
                    if (myEditor.getSelectionModel().hasSelection()) {
                        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                            LineSelectionState state = myManager.getSelectionStateIfExists(caret);
                            if (state != null && state.isLine()) {
                                // remove the caret's selection 
                                caret.setSelection(caret.getOffset(), caret.getOffset());
                            }
                        }
                    }
                }
                return;
            }

            if (adjustments == TO_CHAR__TO_LINE) {
                if (settings.isUpDownSelection()) {
                    final EditorPositionFactory f = myManager.getPositionFactory();
                    final HashMap<Caret, UpDownParams> caretColumns = new HashMap<>();

                    for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                        EditorPosition pos = f.fromPosition(caret.getLogicalPosition());
                        EditorPosition start = f.fromOffset(caret.getSelectionStart());
                        EditorPosition end = f.fromOffset(caret.getSelectionEnd());
                        UpDownParams params = new UpDownParams(pos, start, end, caret.getLeadSelectionOffset(), myManager.getSelectionState(caret));

                        caretColumns.put(caret, params);
                        if (caret.hasSelection()) {
                            LineSelectionState state = myManager.getSelectionStateIfExists(caret);
                            if (state != null && state.isLine()) {
                                myManager.adjustLineSelectionToCharacterSelection(caret, false);
                            }
                        }
                    }

                    runAfterAction(event, () -> {
                        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                            UpDownParams params = caretColumns.get(caret);
                            if (params != null) {
                                EditorPosition newPos = f.fromPosition(caret.getLogicalPosition());
                                myManager.getSelectionState(caret).copyFrom(params.state);

                                adjustUpDownWithSelectionAfter(myEditor,
                                        myManager, caret,
                                        params.pos, params.start, params.end, newPos,
                                        action instanceof MoveCaretUpWithSelectionAction,
                                        params.leadSelectionOffset);
                            }
                        }
                    });
                }
                return;
            }

            if (adjustments == TO_CHAR__NOTHING) {
                if (myEditor.getSelectionModel().hasSelection()) {
                    for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                        LineSelectionState state = myManager.getSelectionStateIfExists(caret);
                        if (state != null && state.isLine()) {
                            // remove the caret's selection 
                            myManager.adjustLineSelectionToCharacterSelection(caret, false);
                        }
                    }
                }
                return;
            }

            if (adjustments == TO_CHAR__TO_ALWAYS_LINE) {
                //throw new IllegalStateException("Uncomment code before adding these types");
                // these are indent/unindent
                if (settings.isUpDownSelection()) {
                    final HashMap<Caret, Integer> caretColumns = new HashMap<>();
                    for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                        caretColumns.put(caret, caret.getLogicalPosition().column);
                        EditorCaret editorCaret = myManager.getEditorCaret(caret);

                        if (editorCaret.isLine() && caret.hasSelection()) {
                            editorCaret = editorCaret.toTrimmedOrExpandedFullLines().withNormalizedCharSelectionPosition();
                            caret.moveToOffset(editorCaret.getCaretPosition().getOffset());
                            caret.setSelection(editorCaret.getSelectionStart().getOffset(), editorCaret.getSelectionEnd().getOffset());
                            //} else {
                            //    if (caret.hasSelection()) {
                            //        state.setAnchorOffset(caret.getLeadSelectionOffset());
                            //    } else {
                            //        state.setAnchorOffset(caret.getOffset());
                            //    }
                            //    state.setLine(false);
                        }
                    }

                    runAfterAction(event, () -> {
                        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                            EditorCaret editorCaret = myManager.getEditorCaret(caret);
                            EditorCaret finalCaret = editorCaret;

                            if (caret.hasSelection()) {
                                finalCaret = editorCaret.toTrimmedOrExpandedLineSelection().withNormalizedPosition();
                            }

                            if (caretColumns.containsKey(caret)) {
                                finalCaret = finalCaret.withCaretPosition(finalCaret.getCaretPosition().onLine(caret.getLogicalPosition().line).atColumn(caretColumns.get(caret)));
                                //LogicalPosition pos = new LogicalPosition(caret.getLogicalPosition().line, caretColumns.get(caret));
                                //caret.moveToLogicalPosition(pos);
                            }
                            
                            finalCaret.copyTo(caret);
                        }
                    });
                }
                return;
            }

            if (adjustments == IF_LINE__FIX_CARET) {
                if (settings.isDeleteOperations()) {
                    final HashMap<Caret, Integer> caretColumns = new HashMap<>();
                    for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                        if (caret.hasSelection()) {
                            LineSelectionState state = myManager.getSelectionStateIfExists(caret);
                            if (state != null && state.isLine()) {
                                caretColumns.put(caret, caret.getLogicalPosition().column);
                            }
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

            if (adjustments == NOTHING__RESTORE_COLUMN) {
                if (settings.isUpDownMovement()) {
                    final HashMap<Caret, Integer> caretColumns = new HashMap<>();
                    for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                        caretColumns.put(caret, caret.getLogicalPosition().column);
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

            if (adjustments == NOTHING__TO_LINE_IF_LOOKS_IT) {
                // here we try our best and do not change anything except preserve caret column if after operation there is a selection 
                // that is a line selection and the caret is at 0 and there was no selection before or it was a line selection
                final HashMap<Caret, Integer> caretColumns = new HashMap<>();
                for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                    caretColumns.put(caret, caret.getLogicalPosition().column);
                    myManager.adjustLineSelectionToCharacterSelection(caret, false);
                }

                runAfterAction(event, () -> {
                    for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                        // if it looks like a line then it is a line selection
                        if (caret.hasSelection()) {
                            if (myEditor.offsetToLogicalPosition(caret.getSelectionStart()).column == 0
                                    && myEditor.offsetToLogicalPosition(caret.getSelectionEnd()).column == 0) {
                                if (caretColumns.containsKey(caret)) {
                                    LogicalPosition position = caret.getLogicalPosition();
                                    if (position.column == 0) {
                                        LogicalPosition pos = new LogicalPosition(position.line, caretColumns.get(caret));
                                        caret.moveToLogicalPosition(pos);
                                    }
                                }

                                LineSelectionState state = myManager.getSelectionState(caret);
                                state.setLine(true);
                            }
                        }
                    }
                });
                return;
            }

            if (adjustments == IF_NO_SELECTION__TO_LINE_RESTORE_COLUMN) {
                final HashMap<Caret, Integer> caretsToFix = new HashMap<>();
                for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                    if (!caret.hasSelection()) {
                        caretsToFix.put(caret, caret.getLogicalPosition().column);
                    }
                }

                runAfterAction(event, () -> {
                    for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                        if (caretsToFix.containsKey(caret)) {
                            if (myEditor.offsetToLogicalPosition(caret.getSelectionStart()).column == 0
                                    && myEditor.offsetToLogicalPosition(caret.getSelectionEnd()).column == 0) {
                                if (caretsToFix.containsKey(caret)) {
                                    LogicalPosition position = caret.getLogicalPosition();
                                    LogicalPosition pos = new LogicalPosition(position.line, caretsToFix.get(caret));
                                    caret.moveToLogicalPosition(pos);
                                }

                                LineSelectionState state = myManager.getSelectionState(caret);
                                state.setLine(true);
                            }
                        }
                    }
                });

                return;
            }
        }

        if (adjustments == IF_NO_SELECTION__REMOVE_SELECTION___IF_LINE_RESTORE_COLUMN) {
            // duplicate lines and cut action
            // TODO: add setting to control this
            class DuplicateCutParams {
                boolean removeCaret = true;
                int fixColumn;
                boolean moveToStart;
                int selStart;
                int selEnd;
            }

            final HashMap<Caret, DuplicateCutParams> caretsParams = new HashMap<>();
            for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                if (!caret.hasSelection()) {
                    caretsParams.put(caret, new DuplicateCutParams());
                } else {
                    DuplicateCutParams params = new DuplicateCutParams();
                    params.removeCaret = false;
                    caretsParams.put(caret, params);

                    if (settings.isDuplicateAtStartOrEnd() && !myAdjustmentsMap.isInSet(action.getClass(), ActionSetType.CUT_ACTION)) {
                        EditorCaret editorCaret = myManager.getEditorCaret(caret);
                        if (editorCaret.hasLines() || !settings.isDuplicateAtStartOrEndLineOnly()) {
                            if (editorCaret.isLine()) {
                                params.fixColumn = caret.getLogicalPosition().column;
                                int anchorOffset = myManager.getSelectionState(caret).getAnchorOffset();
                                boolean startIsAnchor = anchorOffset <= caret.getOffset();

                                if (!startIsAnchor) {
                                    params.moveToStart = true;
                                    params.selStart = caret.getSelectionStart();
                                    params.selEnd = caret.getSelectionEnd();
                                }
                            } else {
                                // move to start if not startIsAnchor so duplicate will duplicate up if at the top 
                                if (caret.getSelectionStart() != caret.getLeadSelectionOffset()) {
                                    params.moveToStart = true;
                                    params.selStart = caret.getSelectionStart();
                                    params.selEnd = caret.getSelectionEnd();
                                }
                            }
                        }
                    }
                }
            }

            runAfterAction(event, () -> {
                for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                    DuplicateCutParams params = caretsParams.get(caret);
                    if ((params == null || params.removeCaret) && !caret.hasSelection()) {
                        caret.removeSelection();
                        continue;
                    }

                    if (params != null && caret.hasSelection()) {
                        if (params.moveToStart && params.selStart < params.selEnd) {
                            caret.setSelection(params.selStart, params.selEnd);
                            caret.moveToOffset(params.selStart);
                        }

                        LogicalPosition selEnd = myEditor.offsetToLogicalPosition(caret.getSelectionEnd());
                        LogicalPosition selStart = myEditor.offsetToLogicalPosition(caret.getSelectionStart());

                        if (selStart.column == 0 && selEnd.column == 0) {
                            LogicalPosition position = caret.getLogicalPosition();
                            LogicalPosition pos;

                            // move to proper position as per inside settings
                            if (params.moveToStart) {
                                if (getCaretInSelection()) {
                                    pos = new LogicalPosition(selStart.line, params.fixColumn);
                                } else {
                                    pos = new LogicalPosition(selStart.line - (selStart.line > 0 ? 1 : 0), params.fixColumn);
                                }
                            } else {
                                if (getCaretInSelection()) {
                                    pos = new LogicalPosition(selEnd.line - 1, params.fixColumn);
                                } else {
                                    pos = new LogicalPosition(position.line, params.fixColumn);
                                }
                            }
                            caret.moveToLogicalPosition(pos);

                            LineSelectionState state = myManager.getSelectionState(caret);
                            state.setLine(true);
                        }
                    }
                }
            });
            return;
        }

        if (adjustments == IF_NO_SELECTION__REMOVE_SELECTION) {
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
                        }
                    }
                });
            }
            return;
        }

        if (adjustments == MOVE_TO_START__RESTORE_IF0_OR_BLANK_BEFORE) {
            // these can replace selections, need to move to start, after if pasted was lines, then we should restore caret pos
            final HashMap<Caret, Integer> caretColumns = new HashMap<>();
            for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                caretColumns.put(caret, caret.getLogicalPosition().column);
                if (caret.hasSelection()) {
                    LineSelectionState state = myManager.getSelectionStateIfExists(caret);
                    if (state != null && state.isLine()) {
                        caret.moveToOffset(caret.getSelectionStart());
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
                                    EditorCaret rangeCaret = editorCaret.withSelection(range.getStartOffset(), range.getEndOffset());

                                    if (rangeCaret.hasLines() || !settings.isSelectPastedLineOnly()) {
                                        if (rangeCaret.hasLines()) {
                                            rangeCaret = rangeCaret.toTrimmedOrExpandedLineSelection().withNormalizedPosition();
                                        } else {
                                            rangeCaret = rangeCaret.toTrimmedOrExpandedFullLines().withNormalizedPosition();
                                        }
                                        rangeCaret.copyTo(myEditor.getCaretModel().getPrimaryCaret());
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
            class Params {
                private EditorCaret savedCaret;
                private int savedIndent;

                private Params(EditorCaret savedCaret, int savedIndent) {
                    this.savedCaret = savedCaret;
                    this.savedIndent = savedIndent;
                }
            }

            final List<Caret> carets = myEditor.getCaretModel().getAllCarets();
            HashMap<Caret, Params> paramList = new HashMap<>();
            EditorPositionFactory f = myManager.getPositionFactory();
            carets.forEach(caret -> {
                EditorCaret editorCaret = myManager.getEditorCaret(caret);
                paramList.put(caret, new Params(editorCaret, editorCaret.getCaretPosition().getIndentColumn()));
            });

            if (!paramList.isEmpty()) {
                runAfterAction(event, () -> {
                    // we now restore only caret line not column
                    // and convert selection to line if it was line
                    List<Caret> caretList = myEditor.getCaretModel().getAllCarets();
                    ArrayList<CaretState> caretStates = new ArrayList<>();
                    caretList.forEach(caret -> {

                        Params params = paramList.get(caret);
                        EditorCaret savedCaret = params == null ? null : params.savedCaret;
                        EditorPosition savedPos = savedCaret == null ? null : savedCaret.getCaretPosition();
                        Integer savedIndent = params == null ? null : params.savedIndent;

                        if (savedIndent != null && savedCaret != null) {
                            // we need to load it with our saved params, the state was probably destroyed by the action
                            EditorCaret editorCaret = myManager.getEditorCaret(caret, savedCaret);
                            int indent = savedPos.getIndentColumn();

                            //editorCaret = editorCaret.withStartIsAnchor(savedCaret.startIsAnchor()).withNormalizedPosition();

                            // copy line state
                            if (!savedCaret.hasSelection()) {
                                // need to fix column position for indentation change
                                EditorPosition position = editorCaret.getCaretPosition();
                                if (position.column > 0) {
                                    editorCaret = editorCaret.atColumn(savedPos.atColumn(Math.max(0, indent + (savedPos.column - savedIndent))));
                                }
                            } else {
                                // fix for IDEA-164143
                                //LogPos selStart = caretState.getSelectionStart();
                                //LogPos savedStart = caretState.getSelectionStart();
                                //if (selStart != null && selStart.column > 0 && savedStart != null) {
                                //    int indent = EditHelpers.countWhiteSpace(myEditor.getDocument().getCharsSequence(), savedPos.atColumn(0).toOffset(), savedPos.with(savedPos.line + 1, 0).toOffset());
                                //    caretState = new EditorCaretState(f, caretState.getCaretPosition(), savedStart.atColumn(Math.max(0, indent + (savedStart.column - savedIndent))), caretState.getSelectionEnd());
                                //}

                                EditorPosition selStart = editorCaret.getSelectionStart();
                                if (selStart.column > 0) {
                                    editorCaret = editorCaret.withSelectionStart(selStart.atColumn(Math.max(0, indent + (selStart.column - savedIndent))));
                                }
                            }

                            EditorCaret finalCaret = editorCaret.toTrimmedOrExpandedLineSelection().withNormalizedPosition();
                            finalCaret.copyTo(caret);
                        }
                    });
                });
            }
            return;
        }
    }

    public static void adjustUpDownWithSelectionAfter(Editor editor, LineSelectionManager manager, Caret caret, EditorPosition pos, EditorPosition start, EditorPosition end, EditorPosition newPos, boolean movedUp, int leadSelectionOffset) {
        EditorPosition newStart = start;
        EditorPosition newEnd = end;
        LineSelectionState state = manager.getSelectionState(caret);

        boolean handled = false;
        boolean startIsAnchor = true;
        int lineCount = editor.getDocument().getLineCount();

        boolean hadSelection = !start.equals(end);

        if (!hadSelection) {
            state.setAnchorOffset(pos.getOffset());
        } else {
            if (!state.isLine()) {
                state.setAnchorOffset(leadSelectionOffset);
            }
        }

        if (getCaretInSelection()) {
            if (hadSelection) {
                // need to figure out whether start or end of selection is the anchor
                boolean atStart = pos.line == start.line;
                boolean atEnd = newPos.line == end.atEndOfLine().line;

                if (atStart && atEnd) {
                    startIsAnchor = !movedUp;
                } else {
                    startIsAnchor = !atStart;
                }

                if (movedUp) {
                    if (startIsAnchor) {
                        // moving up shortening bottom
                        newStart = start.atStartOfLine();
                        newEnd = newPos.atStartOfNextLine();
                    } else {
                        // was on start, keep end and move start
                        newStart = newPos.atStartOfLine();
                        newEnd = newEnd.atEndOfLine();
                    }
                } else {
                    if (startIsAnchor) {
                        // moving up shortening bottom
                        newStart = newStart.atStartOfLine();
                        newEnd = newEnd.atStartOfNextLine();
                    } else {
                        // was on start, keep end and move start
                        newStart = newPos.atStartOfLine();
                        newEnd = newEnd.atEndOfLine();
                    }
                }
            } else {
                // start a new selection and keep cursor on the same line
                if (movedUp) {
                    startIsAnchor = true;
                    newStart = pos.atStartOfLine();
                    newEnd = pos.atStartOfNextLine();
                } else {
                    startIsAnchor = false; // it will be inverted on text step in same direction
                    newStart = pos.atStartOfLine();
                    newEnd = pos.atStartOfNextLine();
                }
                newPos = pos;
            }
        } else {
            if (pos.line == 0) {
                // line selection at top and going up: do nothing 
                // line selection at top and going down: stay on top, move selection one line down 
                // no line selection at top and going down, normal handling 
                // no line selection at top and going up, make top line selection

                if (state.isLine() && EditorPosition.haveTopLineSelection(start, end)) {
                    if (movedUp) {
                        // line selection at top and going up: do nothing 
                        newStart = start;
                        newEnd = end;
                        newPos = pos;
                        handled = true;
                    } else {
                        // line selection at top and going down: stay on top, move selection one line down 
                        newStart = pos.atStartOfNextLine();
                        newEnd = end.atEndOfLine();
                        newPos = pos;
                        handled = true;
                    }
                } else {
                    if (movedUp) {
                        // no line selection at top and going up, make top line selection
                        newStart = pos.atStartOfLine();
                        newEnd = end.line == pos.line ? pos.atStartOfNextLine() : end.atEndOfLine();
                        handled = true;
                    } else {
                        // no line selection at top and going down, normal handling 
                        handled = false;
                    }
                }
            }

            if (!handled) {
                if (newPos.equals(pos)) {
                    if (pos.line == lineCount) {
                        // we are at bottommost line and tried to move down
                        newStart = start.atStartOfLine();
                        newEnd = end.atEndOfLine();
                    } else {
                        if (movedUp) {
                            newStart = newPos.atStartOfNextLine();
                            newEnd = end.atEndOfLine();
                        } else {
                            newStart = start.atStartOfLine();
                            newEnd = newPos.atStartOfLine();
                        }
                    }
                } else {
                    if (start.line == end.line) {
                        if (movedUp) {
                            newStart = newPos.atStartOfNextLine();
                            newEnd = pos.atStartOfNextLine();
                        } else {
                            newStart = pos.atStartOfLine();
                            newEnd = newPos.atStartOfLine();
                        }
                    } else {
                        if (movedUp) {
                            // we are going up, if start is lead then it is below pos
                            startIsAnchor = !(pos.line < start.line);
                        } else {
                            // we are going down, if end is leading then it is the same as pos
                            startIsAnchor = (pos.line >= end.line && pos.column >= end.column);
                        }

                        if (!startIsAnchor) {
                            newStart = newPos.atStartOfNextLine();
                            newEnd = end.atEndOfLine();
                        } else {
                            newStart = start.atStartOfLine();
                            newEnd = newPos.atStartOfNextLine();
                        }
                    }
                }
            }
        }

        manager.setCaretLineSelection(caret, newPos, newStart, newEnd, startIsAnchor, state);
    }

    protected static ApplicationSettings getSettings() {
        return ApplicationSettings.getInstance();
    }

    public static class UpDownParams {
        final public EditorPosition pos;
        final public EditorPosition start;
        final public EditorPosition end;
        final public int leadSelectionOffset;
        final public LineSelectionState state;

        public UpDownParams(EditorPosition pos, EditorPosition start, EditorPosition end, int leadSelectionOffset, LineSelectionState state) {
            this.pos = pos;
            this.start = start;
            this.end = end;
            this.leadSelectionOffset = leadSelectionOffset;
            this.state = new LineSelectionState();
            this.state.copyFrom(state);
        }
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
