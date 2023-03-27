/*
 * Copyright (c) 2016-2018 Vladimir Schneider <vladimir.schneider@gmail.com>
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

import com.intellij.ide.CopyPasteManagerEx;
import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.CaretStateTransferableData;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.actions.TextEndWithSelectionAction;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.vladsch.MissingInActions.Plugin;
import com.vladsch.MissingInActions.actions.ActionUtils;
import com.vladsch.MissingInActions.actions.CaretMoveAction;
import com.vladsch.MissingInActions.actions.CaretSearchAwareAction;
import com.vladsch.MissingInActions.actions.DeleteAfterPasteTransferableData;
import com.vladsch.MissingInActions.actions.pattern.RangeLimitedCaretSpawningHandler;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.settings.CaretAdjustmentType;
import com.vladsch.MissingInActions.settings.LinePasteCaretAdjustmentType;
import com.vladsch.MissingInActions.settings.SelectionPredicateType;
import com.vladsch.MissingInActions.settings.SuffixOnPastePatternType;
import com.vladsch.MissingInActions.util.ActionContext;
import com.vladsch.MissingInActions.util.CaretSnapshot;
import com.vladsch.MissingInActions.util.CaseFormatPreserver;
import com.vladsch.MissingInActions.util.ClipboardCaretContent;
import com.vladsch.MissingInActions.util.EditorActionListener;
import com.vladsch.MissingInActions.util.MiaCancelableJobScheduler;
import com.vladsch.flexmark.util.misc.Pair;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.plugin.util.OneTimeRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR;
import static com.intellij.openapi.diagnostic.Logger.getInstance;
import static com.vladsch.MissingInActions.manager.ActionSetType.MOVE_LINE_DOWN_AUTO_INDENT_TRIGGER;
import static com.vladsch.MissingInActions.manager.ActionSetType.MOVE_LINE_UP_AUTO_INDENT_TRIGGER;
import static com.vladsch.MissingInActions.manager.ActionSetType.MOVE_SEARCH_CARET_ACTION;
import static com.vladsch.MissingInActions.manager.ActionSetType.SEARCH_AWARE_CARET_ACTION;
import static com.vladsch.MissingInActions.manager.ActionSetType.SELECTION_ALWAYS_STASH;
import static com.vladsch.MissingInActions.manager.ActionSetType.SELECTION_STASH_ACTIONS;
import static com.vladsch.MissingInActions.manager.AdjustmentType.*;

@SuppressWarnings("WeakerAccess")
public class ActionSelectionAdjuster implements EditorActionListener, Disposable {
    public static final AnActionEvent[] EMPTY_EVENTS = new AnActionEvent[0];
    private static final Logger LOG = getInstance("com.vladsch.MissingInActions.manager");
    private static final AnActionEvent LAST_CLEANUP_EVENT = null;

    final private @NotNull AfterActionList myAfterActions = new AfterActionList();
    final private @NotNull AfterActionList myAfterActionsCleanup = new AfterActionList();
    final private @NotNull LinkedHashMap<AnActionEvent, AnAction> myActionEventActionMap = new LinkedHashMap<>();
    final private @NotNull LineSelectionManager myManager;
    final private @NotNull Editor myEditor;
    private @NotNull ActionAdjustmentMap myAdjustmentsMap = ActionAdjustmentMap.EMPTY;
    private final @NotNull AtomicInteger myNestingLevel = new AtomicInteger(0);
    //private @Nullable RangeMarker myLastSelectionMarker = null;
    private @Nullable RangeMarker myTentativeSelectionMarker = null;
    private boolean myRerunCaretHandler = false;
    private final StashedRangeMarkers myRangeMarkers;

    final private boolean debug = false;

    // this one is indexed by the class for which the runnable has to be run before
    final private HashSet<OneTimeRunnable> myRunBeforeActions = new HashSet<>();

    // this one is indexed by the class for which the runnable should be canceled since the action is running again
    final private HashMap<Class, HashSet<OneTimeRunnable>> myCancelActionsMap = new HashMap<>();

    ActionSelectionAdjuster(@NotNull LineSelectionManager manager, @NotNull ActionAdjustmentMap normalAdjustmentMap) {
        myManager = manager;
        myEditor = manager.getEditor();
        myAdjustmentsMap = normalAdjustmentMap;
        myRangeMarkers = new StashedRangeMarkers(myManager);

        Plugin.getInstance().addEditorActionListener(myEditor, this, myManager);
    }

    @NotNull
    @Override
    public Editor getEditor() {
        return myEditor;
    }

    public void recallLastSelection(int offsetFromTop, boolean removeSelection, boolean swapWithCurrent) {
        RangeMarker marker = removeSelection ? myRangeMarkers.pop(offsetFromTop) : myRangeMarkers.get(offsetFromTop);

        if (marker != null) {
            if (swapWithCurrent && canSaveSelection()) {
                RangeMarker nextSelectionMarker = getCurrentSelectionMarker();
                myRangeMarkers.push(nextSelectionMarker);
            }

            // recall the selection
            myEditor.getSelectionModel().setSelection(marker.getStartOffset(), marker.getEndOffset());
        }
    }

    @NotNull
    public RangeMarker getCurrentSelectionMarker() {
        return myEditor.getDocument().createRangeMarker(myEditor.getSelectionModel().getSelectionStart(), myEditor.getSelectionModel().getSelectionEnd());
    }

    @Nullable
    public RangeMarker getEditorSelectionRangeMarker() {
        return canSaveSelection() ? new StashedRangeMarkers.SelectionRangeMarker(myEditor) : null;
    }

    public boolean canRecallSelection() {
        RangeMarker marker = myRangeMarkers.peek();
        return marker != null;
    }

    public void setSelectionStashLimit(int maxLimit) {
        myRangeMarkers.setStashLimit(maxLimit);
    }

    public boolean canSwapSelection() {
        RangeMarker myLastSelectionMarker = myRangeMarkers.peek();
        return !StashedRangeMarkers.isEmpty(myLastSelectionMarker) && canSaveSelection();
    }

    @NotNull
    public RangeMarker[] getSavedSelections() {
        return myRangeMarkers.getRangeMarkers();
    }

    @Override
    public void dispose() {
        if (myTentativeSelectionMarker != null) myTentativeSelectionMarker.dispose();
        myTentativeSelectionMarker = null;
        Disposer.dispose(myRangeMarkers);
    }

    final private static Pair<String, String> ACTION_BUTTON_ACTION = new Pair<>("com.intellij.openapi.actionSystem.impl.ActionButton", "performAction");
    final private static Pair<String, String> IDE_KEY_EVENT_DISPATCHER_PROCESS_ACTION = new Pair<>("com.intellij.openapi.keymap.impl.IdeKeyEventDispatcher", "processAction");

    // treat immediately followed duplicates as one call
    final private static Pair<String, String> EDITOR_ACTION_ACTION_PERFORMED = new Pair<>("com.intellij.openapi.editor.actionSystem.EditorAction", "actionPerformed");

    private boolean matchStackElement(StackTraceElement stackTraceElement, Pair<String, String> classMethod) {
        return stackTraceElement.getClassName().equals(classMethod.getFirst()) && stackTraceElement.getMethodName().equals(classMethod.getSecond());
    }

    private int nestedStackActions(StackTraceElement[] stackTrace) {
        // these are source action triggers
        int levels = 0;
        int i = stackTrace.length;
        boolean foundStart = false;
        int lastEditorActionPerformed = -1;
        while (i-- > 0) {
            StackTraceElement stackTraceElement = stackTrace[i];
            if (!foundStart) {
                if (matchStackElement(stackTraceElement, IDE_KEY_EVENT_DISPATCHER_PROCESS_ACTION)
                        || matchStackElement(stackTraceElement, ACTION_BUTTON_ACTION)) {
                    foundStart = true;
                }
            } else {
                if (matchStackElement(stackTraceElement, EDITOR_ACTION_ACTION_PERFORMED)) {
                    if (lastEditorActionPerformed != i - 1) {
                        levels++;
                    }
                    lastEditorActionPerformed = i;
                }
            }
        }
        return levels;
    }

    @Override
    public void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
        if (EDITOR.getData(dataContext) != myEditor) {
            assert EDITOR.getData(dataContext) == myEditor;
        }

        int nesting = myNestingLevel.incrementAndGet();
        if (nesting > 1) {
            // need to validate that previous nested action(s) did not crap out or was cancelled by manager
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            int stackNesting = nestedStackActions(stackTrace);

            boolean cancelledActions = stackNesting < nesting - 1;

            if (cancelledActions) {
                // cancel all above what we can see on the stack
                AnActionEvent[] actionEvents = myActionEventActionMap.isEmpty() ? EMPTY_EVENTS : myActionEventActionMap.keySet().toArray(EMPTY_EVENTS);
                int i = actionEvents.length;

                // set current level to true level so last cleanup gets done
                myNestingLevel.set(actionEvents.length);

                while (i-- > stackNesting) {
                    AnActionEvent actionEvent = actionEvents[i];
                    AnAction anAction = myActionEventActionMap.get(actionEvent);
                    boolean eventCrappedOut = anAction != null;
                    if (eventCrappedOut) {
                        afterActionPerformed(anAction, actionEvent, true);
                    }
                }

                nesting = stackNesting + 1;
                myNestingLevel.set(nesting);
            }
        }

        myActionEventActionMap.put(event, action);

        if (nesting == 1 && canSaveSelection()) {
            // top level, can tentatively save the current selection
            try {
                myTentativeSelectionMarker = getCurrentSelectionMarker();
            } catch (UnsupportedOperationException e) {
                myTentativeSelectionMarker = null;
            }
        }

        RangeLimitedCaretSpawningHandler caretSpawningHandler = myManager.getCaretSpawningHandler();
        myRerunCaretHandler = false;

        if (caretSpawningHandler != null) {
            if (myManager.getStartCaretStates() != null) {
                if (action instanceof CaretSearchAwareAction || myAdjustmentsMap.isInSet(action.getClass(), SEARCH_AWARE_CARET_ACTION)) {
                    // do nothing the action will handle it
                } else if (action instanceof CaretMoveAction || myAdjustmentsMap.isInSet(action.getClass(), MOVE_SEARCH_CARET_ACTION)) {
                    // create start position carets
                    myEditor.getCaretModel().setCaretsAndSelections(myManager.getStartCaretStates());

                    // rerun on new caret position after action
                    myRerunCaretHandler = true;
                } else {
                    ActionUtils.acceptSearchCarets(myManager, true, true);
                }
            } else {
                myManager.clearSearchFoundCarets();
                caretSpawningHandler = null;
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
                        try {
                            adjustBeforeAction(settings, action, adjustments, event);
                        } catch (Throwable e) {
                            LOG.error("adjustBeforeAction exception", e);

                            // remove stuff added for after action and cleanup
                            Collection<Runnable> runnable = myAfterActions.getAfterAction(event);
                            Collection<Runnable> cleanup = myAfterActionsCleanup.getAfterAction(event);
                            if (cleanup != null) cleanup.forEach(Runnable::run);
                        }
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

    public boolean canSaveSelection() {
        return myEditor.getCaretModel().getCaretCount() == 1 && myEditor.getSelectionModel().hasSelection();
    }

    @Override
    public void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
        afterActionPerformed(action, event, false);
    }

    public void afterActionPerformed(AnAction action, AnActionEvent event, boolean wasCancelled) {
        myActionEventActionMap.remove(event);

        Collection<Runnable> runnable = myAfterActions.getAfterAction(event);
        Collection<Runnable> cleanup = myAfterActionsCleanup.getAfterAction(event);

        try {
            if (runnable != null && !wasCancelled) {
                if (debug) System.out.println("running After " + action + ", nesting: " + myNestingLevel.get() + "\n");
                // after actions should not check for support, that was done in before, just do what is in the queue
                guard(() -> runnable.forEach(Runnable::run));
                if (cleanup != null) cleanup.forEach(Runnable::run);
            } else if (cleanup != null) {
                cleanup.forEach(Runnable::run);
            }
        } catch (Throwable e) {
            LOG.error("lastActionCleanup error", e);
        } finally {
            myNestingLevel.decrementAndGet();
        }

        int nesting = myNestingLevel.get();

        if (nesting == 0) {
            Collection<Runnable> lastActionCleanup = myAfterActionsCleanup.getAfterAction(LAST_CLEANUP_EVENT);
            if (lastActionCleanup != null) {
                try {
                    lastActionCleanup.forEach(Runnable::run);
                } catch (Throwable e) {
                    LOG.error("lastActionCleanup error", e);
                }
            }
        }

        if (myTentativeSelectionMarker != null && !myTentativeSelectionMarker.isValid()) {
            myTentativeSelectionMarker.dispose();
            myTentativeSelectionMarker = null;
        }

        if (nesting == 0) {
            RangeMarker marker = null;
            if (myTentativeSelectionMarker != null) {
                marker = myTentativeSelectionMarker;
                myTentativeSelectionMarker = null;
            }

            if (marker != null && !myAdjustmentsMap.isInSet(action.getClass(), SELECTION_STASH_ACTIONS) && !myManager.isInSelectionStackPopup()) {
                saveSelectionMarker(marker, !myAdjustmentsMap.isInSet(action.getClass(), SELECTION_ALWAYS_STASH), true, true, true);
            } else {
                if (marker != null) {
                    marker.dispose();
                }
            }
        }

        RangeLimitedCaretSpawningHandler caretSpawningHandler = myManager.getCaretSpawningHandler();
        if (caretSpawningHandler != null && myRerunCaretHandler) {
            // update start position carets
            caretSpawningHandler.caretsChanged(myEditor);

            // rerun on new caret position after action
            final RangeLimitedCaretSpawningHandler finalCaretSpawningHandler = caretSpawningHandler;
            myManager.guard(() -> {
                finalCaretSpawningHandler.doAction(myManager, myEditor, null, null);
            });
        }
    }

    public void saveSelectionMarker(RangeMarker marker, boolean onlyIfNotSelection, boolean onlyIfNotTop, boolean onlyIfNotStored, boolean moveToTop) {
        boolean save = true;

        if (onlyIfNotSelection) {
            RangeMarker other = getEditorSelectionRangeMarker();
            if (other != null && marker.getStartOffset() == other.getStartOffset() && marker.getEndOffset() == other.getEndOffset()) {
                save = false;
            }
        }

        if (onlyIfNotStored) {
            if (moveToTop) {
                int index = myRangeMarkers.getStoredIndex(marker);
                if (index >= 0) {
                    myRangeMarkers.remove(index);
                }
            } else {
                save = !myRangeMarkers.isStored(marker);
            }
        } else if (onlyIfNotTop) {
            RangeMarker other = myRangeMarkers.peek();
            if (other != null && marker.getStartOffset() == other.getStartOffset() && marker.getEndOffset() == other.getEndOffset()) {
                save = false;
            }
        }

        if (save) {
            myRangeMarkers.push(marker);
        } else {
            marker.dispose();
        }
    }

    @Override
    public void beforeEditorTyping(char c, DataContext dataContext) {
        assert EDITOR.getData(dataContext) == myEditor;
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

        RangeLimitedCaretSpawningHandler caretSpawningHandler = myManager.getCaretSpawningHandler();

        if (caretSpawningHandler != null) {
            if (myManager.getStartCaretStates() != null) {
                // keep only found position carets
                Set<CaretEx> foundCarets = myManager.getFoundCarets();
                if (foundCarets != null) {
                    Set<Caret> carets = new HashSet<>(foundCarets.size());
                    for (CaretEx caretEx : foundCarets) {
                        carets.add(caretEx.getCaret());
                    }

                    for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                        if (!carets.contains(caret)) {
                            myEditor.getCaretModel().removeCaret(caret);
                        }
                    }
                }
            }

            myManager.clearSearchFoundCarets();
        }
    }

    private void runAfterAction(AnActionEvent event, Runnable runnable) {
        myAfterActions.addAfterAction(event, runnable);
    }

    private void afterActionForAllCarets(AnActionEvent event, Consumer<Caret> runnable) {
        myAfterActions.addAfterAction(event, () -> forAllCarets(runnable));
    }

    private void forAllCarets(Consumer<Caret> runnable) {
        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
            runnable.accept(caret);
        }
    }

    private void forAllCarets(@NotNull AnActionEvent event, Consumer<Caret> runnable, @NotNull Consumer<Caret> afterAction) {
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

    private static final DataKey<Boolean> AUTO_TRIGGERED_ACTION = DataKey.create("MissingInActions.AUTO_TRIGGERED_ACTION");

    @SuppressWarnings("SameParameterValue")
    private static DataContext dataContext(@Nullable DataContext parent, boolean autoTriggered) {
        return SimpleDataContext.getSimpleContext(AUTO_TRIGGERED_ACTION, autoTriggered, parent);
    }

    @SuppressWarnings("WeakerAccess")
    public void runAction(AnAction action, boolean autoTriggered) {
        AnActionEvent event = createAnEvent(action, autoTriggered);
        Editor editor = EDITOR.getData(event.getDataContext());
        if (editor == myEditor) {
            ActionUtil.performActionDumbAwareWithCallbacks(action, event);
        }
    }

    /**
     * Schedule a triggered action to run for the given class
     *
     * @param action class of action that just completed
     */
    private void addTriggeredAction(Class<?> action) {
        TriggeredAction triggeredAction = myAdjustmentsMap.getTriggeredAction(action);
        if (triggeredAction != null && triggeredAction.isEnabled()) {
            OneTimeRunnable runnable = new OneTimeRunnable(true, () -> runAction(triggeredAction.getAction(), true));

            HashSet<OneTimeRunnable> actions = myCancelActionsMap.computeIfAbsent(action, anAction -> new HashSet<>());
            if (debug) System.out.println("Adding triggered task " + runnable);
            actions.add(runnable);

            myRunBeforeActions.add(runnable);

            // now schedule it to run
            OneTimeRunnable.schedule(MiaCancelableJobScheduler.getInstance(), triggeredAction.getDelay(), runnable);
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

    private void forAllEditorCaretsInWriteAction(
            @NotNull AnActionEvent event,
            boolean inWriteAction,
            @NotNull EditorCaretBeforeAction runnable,
            @NotNull EditorCaretAfterAction afterAction
    ) {
        forAllEditorCaretsInWriteAction(event, inWriteAction, runnable, afterAction, null);
    }

    private void forAllEditorCaretsInWriteAction(
            @NotNull AnActionEvent event,
            boolean inWriteAction,
            @NotNull EditorCaretBeforeAction runnable,
            @NotNull EditorCaretAfterAction afterAction,
            final @Nullable Runnable afterAllCaretsAction
    ) {
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

                if (afterAllCaretsAction != null) {
                    afterAllCaretsAction.run();
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
                        if (snapshot != null && !snapshot.hasSelection()) {
                            if (editorCaret.canSafelyTrimOrExpandToFullLineSelection()) {
                                editorCaret
                                        .trimOrExpandToLineSelection()
                                        .toLineSelection();
                            }

                            editorCaret.restoreColumn(snapshot);

                            if (!editorCaret.isStartAnchor()) {
                                // one line selection, flip anchor
                                editorCaret.setIsStartAnchorUpdateAnchorColumn(true);
                            }

                            editorCaret.normalizeCaretPosition()
                                    .commit();
                        }
                    });
                }
                return;
            }

            if (adjustments == DUPLICATE__IF_NO_SELECTION__REMOVE_SELECTION___IF_LINE_RESTORE_COLUMN) {
                // DONE: all selection models
                adjustCutAndDuplicateAction(settings, action, event);
                return;
            }

            if (adjustments == CUT___IF_NO_SELECTION__REMOVE_SELECTION___IF_LINE_RESTORE_COLUMN) {
                // DONE: all selection models
                if (settings.isDeleteOperations()) {
                    adjustCutAndDuplicateAction(settings, action, event);
                    return;
                }
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

                        if (myAdjustmentsMap.isInSet(action.getClass(), MOVE_LINE_UP_AUTO_INDENT_TRIGGER, MOVE_LINE_DOWN_AUTO_INDENT_TRIGGER)) {
                            if (value != -1) {
                                boolean doneIt = CaretAdjustmentType.ADAPTER.onFirst(value, map -> map
                                                .to(CaretAdjustmentType.TO_START, () -> {
                                                    if (editorCaret.isLine()) {
                                                        editorCaret.setStartAnchor(false);
                                                    } else {
                                                        editorCaret.setIsStartAnchorUpdateAnchorColumn(false);
                                                    }
                                                })
                                                .to(CaretAdjustmentType.TO_END, () -> {
                                                    if (editorCaret.isLine()) {
                                                        editorCaret.setStartAnchor(true);
                                                    } else {
                                                        editorCaret.setIsStartAnchorUpdateAnchorColumn(true);
                                                    }
                                                })
//                                        .to(CaretAdjustmentType.TO_ANCHOR, () -> {
//                                        })
//                                        .to(CaretAdjustmentType.TO_ANTI_ANCHOR, () -> {
//                                            editorCaret.setIsStartAnchorUpdateAnchorColumn(!editorCaret.isStartAnchor());
//                                        })
                                );

                                editorCaret.commit();
                            }
                        }
                    }
                });
                return;
            }
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
                        if (SelectionPredicateType.ADAPTER.get(settings.getDuplicateAtStartOrEndPredicate()).isEnabled(editorCaret.getSelectionLineCount())) {
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
            long timestamp;
            CaseFormatPreserver preserver = new CaseFormatPreserver();

            Params(CaretSnapshot snapshot) {
                super(snapshot);
            }
        }

        final LinePasteCaretAdjustmentType adjustment = LinePasteCaretAdjustmentType.ADAPTER.findEnum(settings.getLinePasteCaretAdjustment());
        updateLastPastedClipboardCarets(ClipboardCaretContent.getTransferable(myEditor, event.getDataContext()), adjustment, true);
        final CopyPasteManager.ContentChangedListener contentChangedListener = (oldTransferable, newTransferable) -> {
            updateLastPastedClipboardCarets(newTransferable, adjustment, false);
        };

        final CopyPasteManagerEx copyPasteManager = CopyPasteManagerEx.getInstanceEx();
        copyPasteManager.addContentChangedListener(contentChangedListener);

        myAfterActionsCleanup.addAfterAction(event, () -> {
            copyPasteManager.removeContentChangedListener(contentChangedListener);

            ClipboardCaretContent caretContent = ClipboardCaretContent.getLastPastedClipboardCarets(myEditor);
            if (caretContent != null && settings.isMultiPasteDeleteRepeatedCaretData()) {
                Transferable[] allContents = copyPasteManager.getAllContents();
                final Transferable transferable = allContents.length > 0 ? allContents[0] : null;
                if (transferable != null && transferable.isDataFlavorSupported(DeleteAfterPasteTransferableData.FLAVOR)) {
                    // we delete this one, it is a repeat and only useful for exact copy of duped lines, but on after action of all nested actions are done
                    myAfterActionsCleanup.addAfterAction(LAST_CLEANUP_EVENT, () -> {
                        copyPasteManager.removeContent(transferable);
                    });
                }
            }

            ClipboardCaretContent.setLastPastedClipboardCarets(myEditor, null);
            myEditor.putUserData(EditorEx.LAST_PASTED_REGION, null);
        });

        final boolean inWriteAction = settings.isOnPastePreserve() && (settings.isPreserveCamelCaseOnPaste()
                || settings.isPreserveSnakeCaseOnPaste()
                || settings.isPreserveScreamingSnakeCaseOnPaste()
                || (settings.isRemovePrefixesOnPaste() || settings.isAddPrefixOnPaste()) && !(settings.getPrefixesOnPasteText().isEmpty()))
                && myAdjustmentsMap.isInSet(action.getClass(), ActionSetType.PASTE_ACTION);

        final int[] cumulativeCaretDelta = new int[] { 0 };

        forAllEditorCaretsInWriteAction(event, inWriteAction, (editorCaret, snapshot) -> {
            Params params = new Params(snapshot);
            params.timestamp = myEditor.getDocument().getModificationStamp();

            int separators = settings.getPreserveOnPasteSeparators();

            params.preserver.studyFormatBefore(editorCaret
                    , (settings.isRemovePrefixesOnPaste() || settings.isAddPrefixOnPaste()) ? settings.getPrefixesOnPasteList() : null
                    , settings.getRemovePrefixOnPastePatternType()
                    , (settings.isIgnoreSuffixesOnPaste()) ? settings.getSuffixesOnPasteList() : null
                    , SuffixOnPastePatternType.ANY
                    , separators
            );

            Caret caret = editorCaret.getCaret();
            if (editorCaret.isLine()) {
                if (!editorCaret.isStartAnchor()) {
                    caret.moveToOffset(caret.getSelectionStart());
                } else {
                    caret.moveToOffset(caret.getSelectionEnd());
                }
                //} else {
                // // adjustments now done in ClipboardContext.studyPrePasteTransferable via ClipboardContext.CaretOffsetAdjuster
            }

            return true;
        }, (EditorCaret editorCaret, CaretSnapshot snapshot) -> {
            if (snapshot != null) {
                Params params = (Params) CaretSnapshot.PARAMS.get(snapshot);

                // if leave selected is enabled
                if (myAdjustmentsMap.isInSet(action.getClass(), ActionSetType.PASTE_ACTION)) {
                    if (params.timestamp < myEditor.getDocument().getModificationStamp()) {
                        ClipboardCaretContent caretContent = ClipboardCaretContent.getLastPastedClipboardCarets(myEditor);
                        if (caretContent != null) {
                            TextRange rawRange = ClipboardCaretContent.getLastPastedTextRange(myEditor, snapshot.getIndex());
                            if (rawRange != null) {
                                final BasedSequence documentChars = editorCaret.getDocumentChars();
                                if (rawRange.getEndOffset() < documentChars.length()) {
                                    CharSequence beforeShift = rawRange.subSequence(documentChars);
                                    TextRange range = rawRange.shiftRight(cumulativeCaretDelta[0]);
                                    CharSequence afterShift = range.subSequence(documentChars);
                                    if (!caretContent.getContent().isDataFlavorSupported(CaretStateTransferableData.FLAVOR)) {
                                        // pasted text does not have caret information, so do not restore text
                                        int tmp = 0;
                                    } else {
                                        editorCaret.setSelection(range.getStartOffset(), range.getEndOffset());

                                        if (!editorCaret.hasLines()) {
                                            if (settings.isOnPastePreserve()) {
                                                cumulativeCaretDelta[0] -= params.preserver.preserveFormatAfter(editorCaret, range, !inWriteAction
                                                        , (myEditor.getCaretModel().getCaretCount() == 1 && settings.getSelectPastedPredicate() == SelectionPredicateType.WHEN_HAS_ANY.getIntValue())
                                                                || (myEditor.getCaretModel().getCaretCount() > 1
                                                                && settings.isSelectPastedMultiCaret() && settings.getSelectPastedMultiCaretPredicateType().isEnabled(editorCaret.getSelectionLineCount())
                                                        )
                                                        , settings.isPreserveCamelCaseOnPaste()
                                                        , settings.isPreserveSnakeCaseOnPaste()
                                                        , settings.isPreserveScreamingSnakeCaseOnPaste()
                                                        , settings.isPreserveDashCaseOnPaste()
                                                        , settings.isPreserveDotCaseOnPaste()
                                                        , settings.isPreserveSlashCaseOnPaste()
                                                        , settings.isRemovePrefixesOnPaste()
                                                        , settings.isAddPrefixOnPaste()
                                                        , settings.getPrefixesOnPasteList()
                                                        , settings.getRemovePrefixOnPastePatternType()
                                                        , settings.getSuffixesOnPasteList()
                                                        , settings.getIgnoreSuffixOnPastePatternType()
                                                );
                                            }
                                        } else {
                                            if (caretContent.isFullLine(snapshot.getIndex())) {
                                                if (editorCaret.hasLines()) {
                                                    editorCaret.trimOrExpandToLineSelection();
                                                }

                                                int caretColumn = ClipboardCaretContent.getLastPastedCaretColumn(myEditor, snapshot.getIndex());
                                                if (caretColumn != -1) {
                                                    editorCaret.restoreColumn(caretColumn);
                                                }
                                            }

                                            boolean selectPasted = settings.isSelectPasted()
                                                    && SelectionPredicateType.isEnabled(settings.getSelectPastedPredicate(), editorCaret.getSelectionLineCount());

                                            if (!selectPasted) {
                                                editorCaret.removeSelection();
                                            }

                                            editorCaret.normalizeCaretPosition();
                                            editorCaret.commit();
                                        }
                                    }
                                } else {
                                    snapshot.restoreColumn();
                                }
                            } else {
                                snapshot.restoreColumn();
                            }
                        }
                    }
                }
            }
        });
    }

    private void updateLastPastedClipboardCarets(final Transferable transferable, final LinePasteCaretAdjustmentType adjustment, boolean setIfNone) {
        // if we already saved state, it means we might have adjusted caret position for line selections of the clipboard content at the time
        // we need to restore caret column positions to undo adjustments made for clipboard content that no longer applies
        if (!myEditor.isDisposed()) {
            final ClipboardCaretContent lastClipboardData = ClipboardCaretContent.getLastPastedClipboardCarets(myEditor);
            if (lastClipboardData != null) {
                int i = 0;
                for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                    EditorPosition position = myManager.getPositionFactory().fromPosition(caret.getLogicalPosition());
                    final EditorPosition atColumn = position.atColumn(lastClipboardData.getCaretColumn(i++));
                    if (atColumn != position) {
                        caret.moveToLogicalPosition(atColumn);
                    }
                }
            }

            if (lastClipboardData != null || setIfNone) {
                final ClipboardCaretContent clipboardData = transferable != null ? ClipboardCaretContent.saveLastPastedCaretsForTransferable(myEditor, transferable, adjustment == LinePasteCaretAdjustmentType.NONE ? null : (caret, isFullLine) -> {
                    if (!caret.hasSelection() && isFullLine) {
                        caret.moveToOffset(adjustment.getPastePosition(myManager.getPositionFactory().fromPosition(caret.getLogicalPosition())).getOffset());
                    }
                    return caret.getOffset();
                }) : null;

                ClipboardCaretContent.setLastPastedClipboardCarets(myEditor, clipboardData);
            }
        }
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
                    final boolean endOnBlank = editorCaret.getSelectionEnd().atEndColumn().column == 0;
                    final boolean startOnBlank = editorCaret.getSelectionStart().atEndColumn().column == 0;

                    if (!snapshot.isLine() && snapshot.getCaretPosition().line == editorCaret.getCaretPosition().line + 1 && endOnBlank && editorCaret.getCaretPosition().column > 0) {
                        // line completely empty, we need to move the bottom of the selection down to next start of line
                        editorCaret.setSelectionEnd(editorCaret.getSelectionEnd().addLine(1).atColumn(0));
                    }

                    if (!snapshot.isLine()) {
                        // pause on line only if just created a line selection
                        boolean pauseOnLine = editorCaret.getSelectionLineCount() == 2;

                        if (editorCaret.isStartAnchor()) {
                            if (myManager.isSelectionEndExtended() && editorCaret.getCaretPosition().column != 0) {
                                if (!snapshot.hasSelection() || pauseOnLine && deltaLines > 0) {
                                    // need to keep caret on same line, freshly minted line selection
                                    editorCaret.setSelectionEnd(editorCaret.getSelectionEnd().addLine(-1));
                                    oneLine = !snapshot.hasSelection();
                                }
                            }
                        } else {
                            if (myManager.isSelectionStartExtended()) {
                                if (editorCaret.getCaretPosition().column != 0 && editorCaret.getSelectionEnd().line != editorCaret.getDocumentLineCount()) {
                                    if (!snapshot.hasSelection() || pauseOnLine && deltaLines > 0) {
                                        // need to keep caret on same line, freshly minted line selection
                                        if (endOnBlank) {
                                            // blank lines cause problems, need to pretend selection is one line higher here
                                            if (!snapshot.hasSelection()) {
                                                editorCaret.setAnchorPosition(editorCaret.getAnchorPosition().addLine(-1));
                                            }
                                        }
                                        editorCaret.setSelectionStart(editorCaret.getSelectionStart().addLine(1));
                                        oneLine = !snapshot.hasSelection();
                                    }
                                } else if (endOnBlank && editorCaret.getCaretPosition().column > 0) {
                                    // move end one line up
                                    editorCaret.setSelectionEnd(editorCaret.getSelectionEnd().addLine(-1));
                                }
                            }
                        }
                    } else {
                        // blank lines cause problems, need to adjust here
                        if (editorCaret.isStartAnchor()) {
                            if (myManager.isSelectionEndExtended() && editorCaret.getCaretPosition().column != 0) {
                                if (startOnBlank && endOnBlank) {
                                    if (editorCaret.getSelectionLineCount() != 0) {
                                        editorCaret.setSelectionEnd(editorCaret.getSelectionEnd().addLine(1));
                                    }
                                }
                            }
                        } else {
                            if (myManager.isSelectionStartExtended() && editorCaret.getCaretPosition().column != 0) {
                                if (endOnBlank && editorCaret.getSelectionLineCount() == 1) {
                                    editorCaret.setAnchorPosition(editorCaret.getAnchorPosition().addLine(-1));
                                }
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

        public void addAfterAction(@Nullable AnActionEvent event, @NotNull Runnable runnable) {
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
