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
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.actions.TextEndWithSelectionAction;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.util.TextRange;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.settings.CaretAdjustmentType;
import com.vladsch.MissingInActions.settings.LinePasteCaretAdjustmentType;
import com.vladsch.MissingInActions.settings.SelectionPredicateType;
import com.vladsch.MissingInActions.util.*;
import com.vladsch.flexmark.util.ValueRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.Transferable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.intellij.openapi.diagnostic.Logger.getInstance;
import static com.vladsch.MissingInActions.manager.ActionSetType.MOVE_LINE_DOWN_AUTO_INDENT_TRIGGER;
import static com.vladsch.MissingInActions.manager.ActionSetType.MOVE_LINE_UP_AUTO_INDENT_TRIGGER;
import static com.vladsch.MissingInActions.manager.AdjustmentType.*;

public class ActionSelectionAdjuster implements AnActionListener, Disposable {
    private static final Logger logger = getInstance("com.vladsch.MissingInActions.manager");

    final private @NotNull AfterActionList myAfterActions = new AfterActionList();
    final private @NotNull AfterActionList myAfterActionsCleanup = new AfterActionList();
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

    ActionSelectionAdjuster(@NotNull LineSelectionManager manager, @NotNull ActionAdjustmentMap normalAdjustmentMap) {
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
        if (CommonDataKeys.EDITOR.getData(dataContext) == myEditor) {
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

            if (myManager.isLineSelectionSupported()) {
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
    }

    private boolean canSaveAsLastSelection() {
        return myEditor.getCaretModel().getCaretCount() == 1 && myEditor.getSelectionModel().hasSelection();
    }

    @Override
    public void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
        Collection<Runnable> runnable = myAfterActions.getAfterAction(event);
        Collection<Runnable> cleanup = myAfterActionsCleanup.getAfterAction(event);

        if (runnable != null) {
            try {
                if (debug) System.out.println("running After " + action + ", nesting: " + myNestingLevel.get() + "\n");
                // after actions should not check for support, that was done in before, just do what is in the queue
                guard(() -> runnable.forEach(Runnable::run));
                if (cleanup != null) cleanup.forEach(Runnable::run);
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

            if (nesting == 0) {
                if (myTentativeSelectionMarker != null) {
                    if (myLastSelectionMarker != null) myLastSelectionMarker.dispose();
                    myLastSelectionMarker = myTentativeSelectionMarker;
                    myTentativeSelectionMarker = null;
                }
            }
        }
    }

    @Override
    public void beforeEditorTyping(char c, DataContext dataContext) {
        if (CommonDataKeys.EDITOR.getData(dataContext) == myEditor) {
            runBeforeTriggeredActions();

            ApplicationSettings settings = ApplicationSettings.getInstance();

            // DONE: add option for typing not deleting a line selection
            CaretModel caretModel = myEditor.getCaretModel();
            if (!settings.isTypingDeletesLineSelection()
                    && myEditor.getSelectionModel().hasSelection()
                    && caretModel.getCaretCount() == 1
                    && myManager.getEditorCaret(caretModel.getPrimaryCaret()).isLine()) {
                myEditor.getSelectionModel().removeSelection();
            }
        }
    }

    private void runAfterAction(AnActionEvent event, Runnable runnable) {
        myAfterActions.addAfterAction(event, runnable);
    }

    private void afterActionForAllCarets(AnActionEvent event, ValueRunnable<Caret> runnable) {
        myAfterActions.addAfterAction(event, () -> forAllCarets(runnable));
    }

    private void forAllCarets(ValueRunnable<Caret> runnable) {
        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
            runnable.run(caret);
        }
    }

    private void forAllCarets(@NotNull AnActionEvent event, ValueRunnable<Caret> runnable, @NotNull ValueRunnable<Caret> afterAction) {
        forAllCarets(runnable);
        afterActionForAllCarets(event, afterAction);
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
    @SuppressWarnings("SameParameterValue")
    private AnActionEvent createAnEvent(AnAction action, boolean autoTriggered) {
        Presentation presentation = action.getTemplatePresentation().clone();
        DataContext context = DataManager.getInstance().getDataContext(myEditor.getComponent());
        return new AnActionEvent(null, dataContext(context, true), ActionPlaces.UNKNOWN, presentation, ActionManager.getInstance(), 0);
    }

    private static DataKey<Boolean> AUTO_TRIGGERED_ACTION = DataKey.create("MissingInActions.AUTO_TRIGGERED_ACTION");

    @SuppressWarnings("SameParameterValue")
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
            OneTimeRunnable.schedule(triggeredAction.getDelay(), runnable);
        }
    }

    private interface CaretBeforeAction {
        void run(@NotNull Caret caret, @NotNull ActionContext context);
    }

    private interface CaretAfterAction {
        void run(@NotNull Caret caret, @Nullable CaretSnapshot snapshot);
    }

    private void forAllCarets(@NotNull AnActionEvent event, CaretBeforeAction runnable, @NotNull CaretAfterAction afterAction) {
        final ActionContext context = new ActionContext();

        for (Caret caret1 : myEditor.getCaretModel().getAllCarets()) {
            runnable.run(caret1, context);
        }

        myAfterActions.addAfterAction(event, () -> {
            for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                afterAction.run(caret, context.get(caret));
            }
        });
    }

    private interface EditorCaretBeforeAction {
        boolean run(@NotNull EditorCaret editorCaret, @NotNull CaretSnapshot snapshot);
    }

    private interface EditorCaretAfterAction {
        void run(@NotNull EditorCaret editorCaret, @Nullable CaretSnapshot snapshot);
    }

    private void forAllEditorCarets(@NotNull EditorCaretBeforeAction runnable) {
        final ActionContext context = new ActionContext();
        forAllEditorCarets(context, runnable);
    }

    private void forAllEditorCarets(@NotNull ActionContext context, @NotNull EditorCaretBeforeAction runnable) {
        int index = 0;
        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
            EditorCaret editorCaret = myManager.getEditorCaret(caret);
            CaretSnapshot snapshot = new CaretSnapshot(editorCaret, index++);

            if (runnable.run(editorCaret, snapshot)) {
                context.add(editorCaret, snapshot);
            }
        }
    }

    private void forAllEditorCarets(@NotNull AnActionEvent event, @NotNull EditorCaretBeforeAction runnable, @NotNull EditorCaretAfterAction afterAction) {
        final ActionContext context = new ActionContext();
        forAllEditorCarets(context, runnable);

        myAfterActions.addAfterAction(event, () -> {
            for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                afterAction.run(myManager.getEditorCaret(caret), context.get(caret));
            }
        });
    }

    private void forAllEditorCaretsInWriteAction(@NotNull AnActionEvent event, boolean inWriteAction, @NotNull EditorCaretBeforeAction runnable, @NotNull EditorCaretAfterAction afterAction) {
        final ActionContext context = new ActionContext();
        forAllEditorCarets(context, runnable);

        if (inWriteAction) {
            myAfterActions.addAfterAction(event, () -> {
                WriteCommandAction.runWriteCommandAction(myEditor.getProject(), () -> {
                    for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                        afterAction.run(myManager.getEditorCaret(caret), context.get(caret));
                    }
                });
            });
        } else {
            myAfterActions.addAfterAction(event, () -> {
                for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                    afterAction.run(myManager.getEditorCaret(caret), context.get(caret));
                }
            });
        }
    }

    @SuppressWarnings({ "WeakerAccess", "UnnecessaryReturnStatement" })
    protected void adjustBeforeAction(ApplicationSettings settings, AnAction action, AdjustmentType adjustments, AnActionEvent event) {
        boolean isLineModeEnabled = settings.isLineModeEnabled();

        if (isLineModeEnabled) {
            // DONE: all selection models
            if (adjustments == UNDOE_REDO___NOTHING__NOTHING) {
                // need to clear state after for all carets
                afterActionForAllCarets(event, myManager::resetSelectionState);
                return;
            }

            if (adjustments == MOVE_CARET_LEFT_RIGHT___REMOVE_LINE__NOTHING) {
                // DONE: all selection models
                // if it is a line selection, then remove it
                if (myEditor.getSelectionModel().hasSelection()) {
                    boolean leftRightMovement = settings.isLeftRightMovement();
                    if (leftRightMovement) {
                        forAllEditorCarets(event, (editorCaret, snapshot) -> {
                            if (editorCaret.isLine()) {
                                // store snapshot so we can restore column after movement
                                // need caret at selection start for this to be done by the ide
                                final Caret caret = editorCaret.getCaret();
                                caret.moveToOffset(caret.getSelectionStart());
                                return true;
                            }
                            return false;
                        }, (editorCaret, snapshot) -> {
                            if (snapshot != null) snapshot.restoreColumn();
                        });
                    } else {
                        forAllEditorCarets((editorCaret, snapshot) -> {
                            if (editorCaret.isLine()) {
                                // just remove selection
                                snapshot.removeSelection();
                            }
                            return false;
                        });
                    }
                }
                return;
            }

            if (adjustments == MOVE_CARET_UP_DOWN_W_SELECTION___TO_CHAR__TO_LINE) {
                // DONE: all selection models
                adjustMoveCaretUpDownWithSelection(settings, action, event);
                return;
            }

            if (adjustments == MOVE_CARET_LEFT_RIGHT_W_SELECTION___TO_CHAR__NOTHING) {
                // DONE: all selection models
                if (myEditor.getSelectionModel().hasSelection()) {
                    forAllEditorCarets((editorCaret, snapshot) -> {
                        if (editorCaret.isLine()) {
                            editorCaret
                                    .toCharSelectionForCaretPositionBasedLineSelection()
                                    .normalizeCaretPosition()
                                    .commit();
                            return true;
                        }
                        return false;
                    });
                }
                return;
            }

            if (adjustments == MOVE_CARET_START_END_W_SELECTION___TO_CHAR__TO_LINE_OR_NOTHING) {
                // DONE: all selection models
                forAllEditorCarets(event, (editorCaret, snapshot) -> {
                    if (editorCaret.isLine()) {
                        editorCaret
                                .toCharSelectionForCaretPositionBasedLineSelection()
                                .normalizeCaretPosition()
                                .commit();
                    }
                    return settings.isStartEndAsLineSelection();
                }, (editorCaret, snapshot) -> {
                    if (settings.isStartEndAsLineSelection()) {
                        editorCaret
                                .restoreColumn(snapshot)
                                .setSelectionStart(editorCaret.getSelectionStart().atColumn(editorCaret.getCaretPosition()))
                                .setAnchorColumn(editorCaret.getCaretPosition().column)
                                .setIsStartAnchor(action instanceof TextEndWithSelectionAction)
                                .toLineSelection()
                                .normalizeCaretPosition()
                                .restoreColumn(snapshot)
                                .commit();
                    }
                });
                return;
            }

            if (adjustments == INDENT_UNINDENT___TO_CHAR__IF_HAS_LINES_TO_LINE_RESTORE_COLUMN) {
                // DONE: all selection models
                if (settings.isIndentUnindent()) {
                    forAllEditorCarets(event, (editorCaret, snapshot) -> {
                        if (editorCaret.hasSelection()) {
                            if (editorCaret.isLine() || editorCaret.hasLines()) {
                                editorCaret
                                        .toLineEquivalentCharSelection()
                                        .normalizeCaretPosition()
                                        .commit();
                            }
                            return true;
                        }
                        return false;
                    }, (editorCaret, snapshot) -> {
                        if (editorCaret.hasLines()) {
                            if (snapshot != null) {
                                editorCaret.restoreColumn(snapshot.getColumn(), snapshot.getIndent());
                            }

                            editorCaret
                                    .toLineSelection()
                                    .normalizeCaretPosition()
                                    .commit();
                        }
                    });
                }
                return;
            }

            if (adjustments == DELETE_LINE_SELECTION___IF_LINE__RESTORE_COLUMN) {
                // DONE: with both selection models
                // delete line selection action
                if (settings.isDeleteOperations()) {
                    forAllEditorCarets(event, (editorCaret, snapshot) -> {
                        return editorCaret.hasSelection() && editorCaret.isLine();
                    }, (editorCaret, snapshot) -> {
                        if (snapshot != null) snapshot.restoreColumn();
                    });
                }
                return;
            }

            if (adjustments == MOVE_CARET_UP_DOWN___REMOVE_LINE__RESTORE_COLUMN) {
                // DONE: all selection models
                if (settings.isUpDownMovement()) {
                    forAllEditorCarets(event, (editorCaret, snapshot) -> {
                        editorCaret.removeSelection().commit();
                        return true;
                    }, (editorCaret, snapshot) -> {
                        if (snapshot != null) snapshot.restoreColumn();
                    });
                }
                return;
            }

            if (adjustments == COPY___IF_NO_SELECTION__TO_LINE_RESTORE_COLUMN) {
                // DONE: all selection models
                if (settings.isCopyLineOrLineSelection()) {
                    forAllEditorCarets(event, (editorCaret, snapshot) -> {
                        return !editorCaret.hasSelection() || editorCaret.isLine();
                    }, (editorCaret, snapshot) -> {
                        if (editorCaret.canSafelyTrimOrExpandToFullLineSelection()) {
                            editorCaret
                                    .trimOrExpandToLineSelection()
                                    .toLineSelection();
                        }

                        if (snapshot != null) {
                            snapshot.restoreColumn(editorCaret);
                        }

                        editorCaret
                                //.normalizeCaretPosition()
                                .commit();
                    });
                }
                return;
            }
        }

        if (adjustments == DUPLICATE__CUT___IF_NO_SELECTION__REMOVE_SELECTION___IF_LINE_RESTORE_COLUMN) {
            // DONE: all selection models
            adjustCutAndDuplicateAction(settings, action, event);
            return;
        }

        if (adjustments == TOGGLE_CASE___IF_NO_SELECTION__REMOVE_SELECTION) {
            // DONE: all selection models
            // toggle case adjustment
            if (settings.isUnselectToggleCase()) {
                forAllEditorCarets(event, (editorCaret, snapshot) -> {
                    return !editorCaret.hasSelection();
                }, (editorCaret, snapshot) -> {
                    if (editorCaret.hasSelection() && snapshot != null) {
                        snapshot.removeSelection();
                        myManager.resetSelectionState(editorCaret.getCaret());
                    }
                });
            }
            return;
        }

        if (adjustments == JOIN__MOVE_LINES_UP_DOWN___NOTHING__NORMALIZE_CARET_POSITION) {
            // DONE: all selection models
            forAllEditorCarets(event, (editorCaret, snapshot) -> {
                if (editorCaret.isLine()) {
                    editorCaret
                            .toLineEquivalentCharSelection()
                            .normalizeCaretPosition()
                            .commit();
                    return true;
                }
                return false;
            }, (editorCaret, snapshot) -> {
                if (snapshot != null && snapshot.isLine() && editorCaret.isLine()) {
                    editorCaret
                            .setIsStartAnchor(snapshot.isStartAnchor())
                            .setAnchorColumn(editorCaret.getAnchorPosition().atColumn(snapshot.getAnchorColumn()))
                            .restoreColumn(snapshot)
                            .normalizeCaretPosition()
                    ;

                    int value = -1;
                    if (myAdjustmentsMap.isInSet(action.getClass(), MOVE_LINE_UP_AUTO_INDENT_TRIGGER)) {
                        value = settings.getCaretOnMoveSelectionUp();
                    } else if (myAdjustmentsMap.isInSet(action.getClass(), MOVE_LINE_DOWN_AUTO_INDENT_TRIGGER)) {
                        value = settings.getCaretOnMoveSelectionDown();
                    }

                    if (value != -1) {
                        boolean doneIt = CaretAdjustmentType.onFirst(value, map -> map
                                .to(CaretAdjustmentType.TO_START, () -> {
                                    editorCaret.setIsStartAnchorUpdateAnchorColumn(false);
                                })
                                .to(CaretAdjustmentType.TO_END, () -> {
                                    editorCaret.setIsStartAnchorUpdateAnchorColumn(true);
                                })
                                .to(CaretAdjustmentType.TO_ANCHOR, () -> {
                                })
                                .to(CaretAdjustmentType.TO_ANTI_ANCHOR, () -> {
                                    editorCaret.setIsStartAnchorUpdateAnchorColumn(!editorCaret.isStartAnchor());
                                })
                        );

                        editorCaret.commit();
                    }
                }
            });
            return;
        }

        if (adjustments == PASTE___MOVE_TO_START__RESTORE_IF0_OR_BLANK_BEFORE) {
            // DONE: all selection models
            adjustPasteAction(settings, action, event);
            return;
        }

        if (adjustments == AUTO_INDENT_LINES) {
            // DONE: all selection models
            autoIndentLines(settings, action, event);
            return;
        }
    }

    private void adjustCutAndDuplicateAction(ApplicationSettings settings, AnAction action, AnActionEvent event) {
        // duplicate lines and cut action
        forAllEditorCarets(event, (editorCaret, snapshot) -> {
            boolean saveSnapshot = false;

            if (!editorCaret.hasSelection()) {
                saveSnapshot = true;
            } else {
                if (editorCaret.isLine()) {
                    // need to restore column
                    saveSnapshot = true;
                }

                if (myAdjustmentsMap.isInSet(action.getClass(), ActionSetType.DUPLICATE_ACTION)) {
                    if (settings.isDuplicateAtStartOrEnd()) {
                        if (SelectionPredicateType.ADAPTER.findEnum(settings.getDuplicateAtStartOrEndPredicate()).isEnabled(editorCaret.getSelectionLineCount())) {
                            if (!editorCaret.isStartAnchor()) {
                                saveSnapshot = true;
                            }
                        }
                    }
                }
            }
            return saveSnapshot;
        }, (editorCaret, snapshot) -> {
            if (snapshot != null) {
                editorCaret.restoreColumn(snapshot);

                if (editorCaret.hasSelection()) {
                    if (!snapshot.hasSelection()) {
                        snapshot.removeSelection();
                    } else {
                        if (myAdjustmentsMap.isInSet(action.getClass(), ActionSetType.DUPLICATE_ACTION)) {
                            if (settings.isDuplicateAtStartOrEnd()) {
                                int column = editorCaret.getCaretPosition().column;

                                if (!snapshot.isStartAnchor() && snapshot.getSelectionStart().getOffset() < snapshot.getSelectionEnd().getOffset()) {
                                    editorCaret
                                            .setCaretPosition(editorCaret.getCaretPosition().atOffset(snapshot.getSelectionStart().getOffset()))
                                            .setSelectionEnd(snapshot.getSelectionEnd().getOffset())
                                            .setSelectionStart(editorCaret.getCaretPosition())
                                            .setIsStartAnchor(false)
                                            .setAnchorColumn(editorCaret.getSelectionEnd())
                                    ;
                                }

                                if (editorCaret.canSafelyTrimOrExpandToFullLineSelection()) {
                                    // can safely be changed to a line selection
                                    editorCaret.trimOrExpandToLineSelection();
                                }

                                editorCaret.normalizeCaretPosition();

                                if (editorCaret.isLine() ||
                                        (column > editorCaret.getCaretPosition().column
                                                && (editorCaret.getCaretPosition().column == 0 || editorCaret.getCaretPosition().column < editorCaret.getCaretPosition().getIndentColumn()))) {
                                    editorCaret.setCaretPosition(editorCaret.getCaretPosition().atColumn(column));
                                }
                            }
                        }
                    }
                }
                editorCaret.commit();
            }
        });
    }

    private void adjustPasteAction(ApplicationSettings settings, AnAction action, AnActionEvent event) {
        // these can replace selections, need to move to start, after if pasted was lines, then we should restore caret pos
        class Params extends CaretSnapshot.Params<Params> {
            private long timestamp;
            private CaseFormatPreserver preserver = new CaseFormatPreserver();
            private boolean restoreColumn;

            private Params(CaretSnapshot snapshot) {
                super(snapshot);
            }
        }

        final int[] cumulativeCaretDelta = new int[] { 0 };

        // Multiple paste may change this but all the rest should be ok, that is why we override with
        // our own multiple paste which updates this value, otherwise we could store it in a variable
        final Transferable transferable = ClipboardContext.getTransferable(myEditor, event.getDataContext());
        final ClipboardContext clipboardData = transferable != null ? ClipboardContext.studyPrePasteTransferable(myEditor, transferable) : null;
        myEditor.putUserData(ClipboardContext.LAST_PASTED_CLIPBOARD_CONTEXT, clipboardData);
        if (clipboardData != null) {
            myAfterActionsCleanup.addAfterAction(event, () -> {
                myEditor.putUserData(ClipboardContext.LAST_PASTED_CLIPBOARD_CONTEXT, null);
            });
        }

        final boolean inWriteAction = (settings.isPreserveCamelCaseOnPaste()
                || settings.isPreserveSnakeCaseOnPaste()
                || settings.isPreserveScreamingSnakeCaseOnPaste()
                || settings.isRemovePrefixOnPaste() && !(settings.getRemovePrefixOnPaste1().isEmpty() && settings.getRemovePrefixOnPaste2().isEmpty()))
                && myAdjustmentsMap.isInSet(action.getClass(), ActionSetType.PASTE_ACTION);

        forAllEditorCaretsInWriteAction(event, inWriteAction, (editorCaret, snapshot) -> {
            Params params = new Params(snapshot);
            params.timestamp = myEditor.getDocument().getModificationStamp();

            params.preserver.studyFormatBefore(editorCaret
                    , settings.isRemovePrefixOnPaste() ? settings.getRemovePrefixOnPaste1() : ""
                    , settings.isRemovePrefixOnPaste() ? settings.getRemovePrefixOnPaste2() : ""
            );

            Caret caret = editorCaret.getCaret();
            if (editorCaret.isLine()) {
                if (!editorCaret.isStartAnchor()) {
                    caret.moveToOffset(caret.getSelectionStart());
                } else {
                    caret.moveToOffset(caret.getSelectionEnd());
                }
            } else {
                final LinePasteCaretAdjustmentType adjustment = LinePasteCaretAdjustmentType.ADAPTER.findEnum(settings.getLinePasteCaretAdjustment());
                if (!editorCaret.hasSelection() && clipboardData != null && adjustment != LinePasteCaretAdjustmentType.NONE) {
                    if (clipboardData.isFullLine(snapshot.getIndex())) {
                        // we are changing where the paste will go, need to adjust the clipboard data text range
                        final int offset = adjustment.getPastePosition(editorCaret.getCaretPosition()).getOffset();
                        clipboardData.shiftCaretRangeRight(snapshot.getIndex(), offset - caret.getOffset());
                        caret.moveToOffset(offset);
                        params.restoreColumn = true;
                    }
                }
            }

            return true;
        }, (EditorCaret editorCaret, CaretSnapshot snapshot) -> {
            if (snapshot != null) {
                Params params = (Params) snapshot.get(CaretSnapshot.PARAMS);

                // if leave selected is enabled
                if (myAdjustmentsMap.isInSet(action.getClass(), ActionSetType.PASTE_ACTION)) {
                    if (params.timestamp < myEditor.getDocument().getModificationStamp()) {
                        ClipboardContext clipboardContext = myEditor.getUserData(ClipboardContext.LAST_PASTED_CLIPBOARD_CONTEXT);

                        TextRange[] ranges = clipboardContext != null ? clipboardContext.getTextRanges() : null;
                        TextRange textRange = myEditor.getUserData(EditorEx.LAST_PASTED_REGION);
                        TextRange rawRange = ranges != null && ranges.length > snapshot.getIndex() ? ranges[snapshot.getIndex()] : textRange;

                        if (rawRange != null) {
                            CharSequence beforeShift = rawRange.subSequence(editorCaret.getDocumentChars());
                            TextRange range = rawRange.shiftRight(cumulativeCaretDelta[0]);
                            CharSequence afterShift = range.subSequence(editorCaret.getDocumentChars());
                            editorCaret.setSelection(range.getStartOffset(), range.getEndOffset());

                            boolean selectPasted = settings.isSelectPasted()
                                    && SelectionPredicateType.ADAPTER.findEnum(settings.getSelectPastedPredicate()).isEnabled(editorCaret.getSelectionLineCount());

                            boolean isSingleLineChar = !editorCaret.hasLines();

                            if (isSingleLineChar) {
                                cumulativeCaretDelta[0] += params.preserver.preserveFormatAfter(editorCaret, range, !inWriteAction
                                        , selectPasted || settings.isSelectPastedMultiCaret() && myEditor.getCaretModel().getCaretCount() > 1
                                        , settings.isPreserveCamelCaseOnPaste()
                                        , settings.isPreserveSnakeCaseOnPaste()
                                        , settings.isPreserveScreamingSnakeCaseOnPaste()
                                        , settings.isRemovePrefixOnPaste() ? settings.getRemovePrefixOnPaste1() : ""
                                        , settings.isRemovePrefixOnPaste() ? settings.getRemovePrefixOnPaste2() : ""
                                );
                            } else {
                                if (selectPasted) {
                                    if (editorCaret.hasLines()) {
                                        editorCaret.trimOrExpandToLineSelection().normalizeCaretPosition();
                                    } else {
                                        editorCaret.normalizeCaretPosition();
                                    }

                                    if (params.restoreColumn) {
                                        editorCaret.restoreColumn(snapshot);
                                    }
                                    editorCaret.commit();
                                }
                            }
                        } else {
                            snapshot.restoreColumn();
                        }
                    }
                }
            }
        });
    }

    private void autoIndentLines(ApplicationSettings settings, AnAction action, AnActionEvent event) {
        Boolean AUTO_TRIGGERED = AUTO_TRIGGERED_ACTION.getData(event.getDataContext());
        boolean autoTriggered = AUTO_TRIGGERED != null && AUTO_TRIGGERED;

        // only process this if auto-triggered, not manual
        if (autoTriggered) {
            forAllEditorCarets(event, (editorCaret, snapshot) -> {
                return true;
            }, (editorCaret, snapshot) -> {
                if (snapshot != null) {
                    if (!snapshot.hasSelection() && !editorCaret.hasSelection()) {
                        // need to fix column position for indentation change
                        EditorPosition position = editorCaret.getCaretPosition();
                        if (position.column > 0) {
                            int indent = snapshot.getIndent();
                            editorCaret.atColumn(Math.max(0, indent + (snapshot.getColumn() - snapshot.getIndent())));
                        }

                        if (editorCaret.getCaretPosition().line > 0) {
                            editorCaret.setCaretPosition(editorCaret.getCaretPosition().addLine(-1));
                        }
                    } else {
                        // fix for IDEA-164143
                        EditorPosition selStart = editorCaret.getSelectionStart();
                        if (selStart.column > 0) {
                            editorCaret.setSelectionStart(selStart.atColumn(selStart.getIndentColumn() + Math.max(0, selStart.column - snapshot.getIndent())));
                        } else if (editorCaret.getSelectionEnd().column == 0) {
                            editorCaret.toLineSelection();
                        }
                    }

                    if (editorCaret.hasSelection()) editorCaret.normalizeCaretPosition();

                    editorCaret.commit();
                }
            });
        }
    }

    private void adjustMoveCaretUpDownWithSelection(ApplicationSettings settings, AnAction action, AnActionEvent event) {
        if (settings.isUpDownSelection()) {
            forAllEditorCarets(event, (editorCaret, snapshot) -> {
                editorCaret
                        .toCharSelectionForCaretPositionBasedLineSelection()
                        .normalizeCaretPosition()
                        .resetAnchorState()
                        .commit();

                //logger.debug("before line select editorCaret out: " + editorCaret);
                return true;
            }, (editorCaret, snapshot) -> {
                if (snapshot != null) {
                    int deltaLines = editorCaret.getSelectionLineCount() - snapshot.getSelectionLineCount();
                    editorCaret.restoreColumn(snapshot);

                    //logger.debug("after line select deltaLines: " + deltaLines + "editorCaret in: " + editorCaret);

                    // since dall are coming in as non-line, we will have to make the adjustments ourselves
                    boolean oneLine = false;
                    if (!snapshot.isLine()) {
                        // pause on line only if just created a line selection
                        boolean pauseOnLine = editorCaret.getSelectionLineCount() == 2;

                        if (editorCaret.isStartAnchor() && myManager.isSelectionEndExtended() && editorCaret.getCaretPosition().column != 0) {
                            if (!snapshot.hasSelection() || pauseOnLine && deltaLines > 0) {
                                // need to keep caret on same line, freshly minted line selection
                                editorCaret.setSelectionEnd(editorCaret.getSelectionEnd().addLine(-1));
                                oneLine = !snapshot.hasSelection();
                            }
                        } else if (!editorCaret.isStartAnchor() && myManager.isSelectionStartExtended() && editorCaret.getCaretPosition().column != 0) {
                            if (!snapshot.hasSelection() || pauseOnLine && deltaLines > 0) {
                                // need to keep caret on same line, freshly minted line selection
                                editorCaret.setSelectionStart(editorCaret.getSelectionStart().addLine(1));
                                oneLine = !snapshot.hasSelection();
                            }
                        }
                    }

                    //logger.debug("after line select deltaLines: " + deltaLines + "editorCaret after adjust: " + editorCaret);

                    if (oneLine) {
                        EditorPosition anchor = editorCaret.getAnchorPosition();
                        editorCaret.setAnchorColumn(anchor)
                                .setSelectionStart(anchor.atStartOfLine())
                                .setSelectionEnd(anchor.atStartOfNextLine())
                                .toLineSelection()
                                .normalizeCaretPosition()
                                .commit();

                        //logger.debug("after line select one liner editorCaret: " + editorCaret);
                    } else {
                        int column = editorCaret.getCaretPosition().column;

                        if (editorCaret.getSelectionLineCount() == 0 && editorCaret.getCaretPosition().line == editorCaret.getSelectionStart().line) {
                            // see if it would make a character mark
                            editorCaret.normalizeCaretPosition();

                            //logger.debug("after line select editorCaret out: " + editorCaret);
                        } else {
                            //logger.debug("after line select before toCaretPosBased editorCaret: " + editorCaret);

                            // if it was a char selection we should use it's anchor position
                            //if (snapshot.hasSelection() && !snapshot.isLine()) {
                            //    editorCaret.setAnchorColumn(snapshot.getAnchorColumn());
                            //}

                            editorCaret
                                    .toCaretPositionBasedLineSelection()
                                    .normalizeCaretPosition();

                            //logger.debug("after line select editorCaret out: " + editorCaret);
                        }

                        editorCaret
                                .restoreColumn(column)
                                .commit();
                    }
                }
            });
        }
    }

    protected static ApplicationSettings getSettings() {
        return ApplicationSettings.getInstance();
    }

    @SuppressWarnings("WeakerAccess")
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
