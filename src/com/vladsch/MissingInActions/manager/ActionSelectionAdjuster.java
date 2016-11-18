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
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.actions.MoveCaretUpWithSelectionAction;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.EditorCaretState;
import com.vladsch.MissingInActions.util.LogPos;
import com.vladsch.MissingInActions.util.OneTimeRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.vladsch.MissingInActions.Plugin.getCaretInSelection;
import static com.vladsch.MissingInActions.manager.AdjustmentType.*;

public class ActionSelectionAdjuster implements AnActionListener, Disposable {
    final private @NotNull AfterActionList myAfterActions = new AfterActionList();
    final private @NotNull LineSelectionManager myManager;
    final private @NotNull Editor myEditor;
    private @NotNull ActionAdjustmentMap myAdjustmentsMap = ActionAdjustmentMap.EMPTY;
    
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

    @Override
    public void dispose() {
        ActionManager.getInstance().removeAnActionListener(this);
    }

    @Override
    public void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
        if (myManager.isAdjustmentSupported()
                && !myEditor.isColumnMode()
                && CommonDataKeys.EDITOR.getData(dataContext) == myEditor && getSettings().isLineModeEnabled()) {

            //if (debug) System.out.println("Before " + action);
            cancelTriggeredAction(action.getClass());
            runBeforeTriggeredActions();

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
        }
    }

    @Override
    public void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
        Collection<Runnable> runnable = myAfterActions.getAfterAction(event);
        if (runnable != null) {
            if (debug) System.out.println("After " + action);
            if (myManager.isAdjustmentSupported() && getSettings().isLineModeEnabled()) {
                if (debug) System.out.println("running After " + action);
                guard(() -> runnable.forEach(Runnable::run));
            }
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
            for (Map.Entry<Class, HashSet<OneTimeRunnable>> entry : myCancelActionsMap.entrySet()) {
                entry.getValue().remove(runnable);
                if (entry.getValue().isEmpty()) {
                    myCancelActionsMap.remove(entry.getKey());
                }
            }

            if (debug) System.out.println("Running before triggered task " + runnable);
            runnable.run();
        }
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

    private void runAction(AnAction action) {
        AnActionEvent event = createAnEvent(action);
        List<Caret> carets = myEditor.getCaretModel().getAllCarets();
        HashMap<Caret, LogPos> savedStates = new HashMap<>();
        LogPos.Factory f = LogPos.factory(myEditor);
        carets.forEach(caret -> {
            savedStates.put(caret, f.fromPos(caret.getLogicalPosition()));
        });

        ActionUtil.performActionDumbAware(action, event);

        // we now restore only caret line not column
        carets = myEditor.getCaretModel().getAllCarets();
        ArrayList<CaretState> caretStates = new ArrayList<>();
        carets.forEach(caret -> {
            EditorCaretState caretState = new EditorCaretState(f, caret);

            LogPos savedState = savedStates.get(caret);
            caretStates.add(caretState.onLine(savedState));
        });

        myEditor.getCaretModel().setCaretsAndSelections(caretStates);
    }

    @Nullable
    private AnActionEvent createAnEvent(AnAction action) {
        Presentation presentation = action.getTemplatePresentation().clone();
        DataContext context = DataManager.getInstance().getDataContext(myEditor.getComponent());
        return new AnActionEvent(null, context, ActionPlaces.UNKNOWN, presentation, ActionManager.getInstance(), 0);
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

            // now schedule it to run
            OneTimeRunnable.schedule(runnable, triggeredAction.getDelay());
        }
    }

    @SuppressWarnings("WeakerAccess")
    protected void adjustBeforeAction(ApplicationSettings settings, AnAction action, AdjustmentType adjustments, AnActionEvent event) {
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
                final LogPos.Factory f = LogPos.factory(myEditor);
                final HashMap<Caret, UpDownParams> caretColumns = new HashMap<>();

                for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                    LogPos pos = f.fromPos(caret.getLogicalPosition());
                    LogPos start = f.fromOffset(caret.getSelectionStart());
                    LogPos end = f.fromOffset(caret.getSelectionEnd());
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
                            LogPos newPos = f.fromPos(caret.getLogicalPosition());
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
            // these are up/down with selection
            if (settings.isUpDownSelection()) {
                final HashMap<Caret, Integer> caretColumns = new HashMap<>();
                for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                    caretColumns.put(caret, caret.getLogicalPosition().column);
                    LineSelectionState state = myManager.getSelectionState(caret);
                    if (state.isLine() && caret.hasSelection()) {
                        myManager.adjustLineSelectionToCharacterSelection(caret, false);
                    } else {
                        if (caret.hasSelection()) {
                            state.setAnchorOffset(caret.getLeadSelectionOffset());
                        } else {
                            state.setAnchorOffset(caret.getOffset());
                        }
                        state.setLine(false);
                    }
                }

                runAfterAction(event, () -> {
                    for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                        if (caret.hasSelection()) {
                            myManager.adjustCharacterSelectionToLineSelection(caret, false, false);
                        }

                        if (caretColumns.containsKey(caret)) {
                            LogicalPosition pos = new LogicalPosition(caret.getLogicalPosition().line, caretColumns.get(caret));
                            caret.moveToLogicalPosition(pos);
                        }
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

        if (adjustments == MOVE_TO_START__RESTORE_IF0) {
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
                        }
                    }
                });
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
            //noinspection UnnecessaryReturnStatement
            return;
        }
    }

    public static void adjustUpDownWithSelectionAfter(Editor editor, LineSelectionManager adjuster, Caret caret, LogPos pos, LogPos start, LogPos end, LogPos newPos, boolean movedUp, int leadSelectionOffset) {
        LogPos newStart = start;
        LogPos newEnd = end;
        LineSelectionState state = adjuster.getSelectionState(caret);

        boolean handled = false;
        boolean startIsAnchor = true;
        int lineCount = editor.getDocument().getLineCount();

        boolean hadSelection = !start.equals(end);

        if (!hadSelection) {
            state.setAnchorOffset(pos.toOffset());
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
                        newEnd = newPos.atEndOfNextLine();
                    } else {
                        // was on start, keep end and move start
                        newStart = newPos.atStartOfLine();
                        newEnd = newEnd.atEndOfLine();
                    }
                } else {
                    if (startIsAnchor) {
                        // moving up shortening bottom
                        newStart = newStart.atStartOfLine();
                        newEnd = newEnd.atEndOfNextLine();
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
                    newEnd = pos.atEndOfNextLine();
                } else {
                    startIsAnchor = false; // it will be inverted on text step in same direction
                    newStart = pos.atStartOfLine();
                    newEnd = pos.atEndOfNextLine();
                }
                newPos = pos;
            }
        } else {
            if (pos.line == 0) {
                // line selection at top and going up: do nothing 
                // line selection at top and going down: stay on top, move selection one line down 
                // no line selection at top and going down, normal handling 
                // no line selection at top and going up, make top line selection

                if (state.isLine() && LogPos.haveTopLineSelection(start, end)) {
                    if (movedUp) {
                        // line selection at top and going up: do nothing 
                        newStart = start;
                        newEnd = end;
                        newPos = pos;
                        handled = true;
                    } else {
                        // line selection at top and going down: stay on top, move selection one line down 
                        newStart = pos.atEndOfNextLine();
                        newEnd = end.atEndOfLine();
                        newPos = pos;
                        handled = true;
                    }
                } else {
                    if (movedUp) {
                        // no line selection at top and going up, make top line selection
                        newStart = pos.atStartOfLine();
                        newEnd = end.line == pos.line ? pos.atEndOfNextLine() : end.atEndOfLine();
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
                            newStart = newPos.atEndOfNextLine();
                            newEnd = end.atEndOfLine();
                        } else {
                            newStart = start.atStartOfLine();
                            newEnd = newPos.atStartOfLine();
                        }
                    }
                } else {
                    if (start.line == end.line) {
                        if (movedUp) {
                            newStart = newPos.atEndOfNextLine();
                            newEnd = pos.atEndOfNextLine();
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
                            newStart = newPos.atEndOfNextLine();
                            newEnd = end.atEndOfLine();
                        } else {
                            newStart = start.atStartOfLine();
                            newEnd = newPos.atEndOfNextLine();
                        }
                    }
                }
            }
        }

        adjuster.setCaretLineSelection(caret, newPos, newStart, newEnd, startIsAnchor, state);
    }

    protected static ApplicationSettings getSettings() {
        return ApplicationSettings.getInstance();
    }

    public static class UpDownParams {
        final public LogPos pos;
        final public LogPos start;
        final public LogPos end;
        final public int leadSelectionOffset;
        final public LineSelectionState state;

        public UpDownParams(LogPos pos, LogPos start, LogPos end, int leadSelectionOffset, LineSelectionState state) {
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
