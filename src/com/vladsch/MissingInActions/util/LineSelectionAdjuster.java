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

package com.vladsch.MissingInActions.util;

import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.ide.actions.PasteAction;
import com.intellij.ide.actions.UndoAction;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.actions.*;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.messages.MessageBusConnection;
import com.vladsch.MissingInActions.Plugin;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.settings.ApplicationSettingsListener;
import com.vladsch.MissingInActions.settings.MouseModifierType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.intellij.openapi.editor.event.EditorMouseEventArea.EDITING_AREA;
import static com.vladsch.MissingInActions.Plugin.getCaretInSelection;

/**
 * Adjust a line selection to a normal selection when selection is adjusted by moving the caret
 */
public class LineSelectionAdjuster implements CaretListener
        , SelectionListener
        , PropertyChangeListener
        , EditorMouseListener
        , EditorMouseMotionListener
        , AnActionListener
        , Disposable
{
    private final Editor myEditor;
    final private ReEntryGuard myCaretGuard = new ReEntryGuard();
    final private HashMap<Caret, LineSelectionState> mySelectionStates = new HashMap<>();
    final private @NotNull DelayedRunner myDelayedRunner = new DelayedRunner();
    final private @Nullable AnAction myEditorUpAction;
    final private @Nullable AnAction myEditorDownAction;
    final private MessageBusConnection myMessageBusConnection;

    private @NotNull final HashMap<AnActionEvent, Runnable> myAfterActions = new HashMap<>();
    private final LineSelectionState myPrimarySelectionState = new LineSelectionState();
    private int myMouseAnchor = -1;

    final private static Set<Class> TO_CHAR_BEFORE_ACTIONS = new HashSet<>(Arrays.asList(
            LineEndWithSelectionAction.class,
            LineStartWithSelectionAction.class,
            MoveCaretLeftWithSelectionAction.class,
            MoveCaretRightWithSelectionAction.class,
            NextWordInDifferentHumpsModeWithSelectionAction.class,
            NextWordWithSelectionAction.class,
            PreviousWordInDifferentHumpsModeWithSelectionAction.class,
            PreviousWordWithSelectionAction.class,
            TextEndWithSelectionAction.class,
            TextStartWithSelectionAction.class
    ));

    final private static Set<Class> TO_CHAR_BEFORE_TO_LINE_AFTER_ACTIONS = new HashSet<>(Arrays.asList(
            MoveCaretUpWithSelectionAction.class,
            MoveDownWithSelectionAndScrollAction.class,
            MoveUpWithSelectionAndScrollAction.class,
            PageTopWithSelectionAction.class,
            PageUpWithSelectionAction.class,
            MoveUpWithSelectionAndScrollAction.class,
            PageBottomWithSelectionAction.class,
            PageDownWithSelectionAction.class
    ));

    // these need caret column restored after their execution, line should remain the same
    final private static Set<Class> RESTORE_COLUMN_AFTER_EDITING_ACTIONS = new HashSet<>(Arrays.asList(
            PageBottomAction.class,
            PageDownAction.class,
            PageTopAction.class,
            PageUpAction.class,
            MoveCaretDownAction.class,
            MoveCaretUpAction.class
    ));

    // these need selection to be removed before them if it is a line selection
    final private static Set<Class> REMOVE_LINE_SELECTION_BEFORE_ACTIONS = new HashSet<>(Arrays.asList(
            MoveCaretLeftAction.class,
            MoveCaretRightAction.class
    ));

    // these need selection to be removed before them if it is a line selection
    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    final private static Set<Class> MAKE_LINE_IF_LOOKS_IT_AFTER_ACTIONS = new HashSet<>(Arrays.asList(
            UndoAction.class
    ));

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    final private static Set<Class> MOVE_TO_START_RESTORE_IF0_AFTER_ACTIONS = new HashSet<>(Arrays.asList(
            com.intellij.openapi.editor.actions.PasteAction.class,
            PasteAction.class,
            PasteFromX11Action.class,
            MultiplePasteAction.class,
            SimplePasteAction.class
    ));

    final private static Set<Class> IF_LINE_FIX_CARET_AFTER_EDITING_ACTIONS = new HashSet<>(Arrays.asList(
            BackspaceAction.class,
            CutAction.class,
            DeleteAction.class,
            DeleteToLineEndAction.class,
            DeleteToWordEndAction.class,
            DeleteToWordEndInDifferentHumpsModeAction.class,
            DeleteToWordStartAction.class,
            DeleteToWordStartInDifferentHumpsModeAction.class
    ));

    @Override
    public void dispose() {
        //println("LineSelectionAdjuster disposed");
        myDelayedRunner.runAll();
        myMessageBusConnection.disconnect();
    }

    public LineSelectionAdjuster(Editor editor) {
        myEditor = editor;

        AnAction editorUp = ActionManager.getInstance().getAction("EditorUp");
        AnAction editorDown = ActionManager.getInstance().getAction("EditorDown");
        myEditorUpAction = editorUp != null ? EmptyAction.wrap(editorUp) : null;
        myEditorDownAction = editorDown != null ? EmptyAction.wrap(editorDown) : null;

        ApplicationSettings settings = ApplicationSettings.getInstance();
        hookListeners(settings);
        setupKeys(settings);

        myMessageBusConnection = ApplicationManager.getApplication().getMessageBus().connect(this);
        myMessageBusConnection.subscribe(ApplicationSettingsListener.TOPIC, this::settingsChanged);
    }

    @Override
    public void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
        if (isEditorSupported() && !myEditor.isColumnMode()) {
            guard(() -> {
                ApplicationSettings settings = getSettings();

                if (REMOVE_LINE_SELECTION_BEFORE_ACTIONS.contains(action.getClass())) {
                    // if it is a line selection, then remove it 
                    if (settings.isLeftRightMovement()) {
                        if (myEditor.getSelectionModel().hasSelection()) {
                            for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                                LineSelectionState state = getSelectionStateIfExists(caret);
                                if (state != null && state.isLine()) {
                                    // remove the caret's selection 
                                    caret.setSelection(caret.getOffset(), caret.getOffset());
                                }
                            }
                        }
                    }
                } else if (TO_CHAR_BEFORE_ACTIONS.contains(action.getClass())) {
                    if (myEditor.getSelectionModel().hasSelection()) {
                        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                            LineSelectionState state = getSelectionStateIfExists(caret);
                            if (state != null && state.isLine()) {
                                // remove the caret's selection 
                                adjustLineSelectionToCharacterSelection(caret, false);
                            }
                        }
                    }
                } else if (TO_CHAR_BEFORE_TO_LINE_AFTER_ACTIONS.contains(action.getClass())) {
                    final HashMap<Caret, Integer> caretColumns = new HashMap<>();
                    if (settings.isUpDownSelection()) {
                        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                            caretColumns.put(caret, caret.getLogicalPosition().column);
                            if (caret.hasSelection()) {
                                LineSelectionState state = getSelectionStateIfExists(caret);
                                if (state != null && state.isLine()) {
                                    adjustLineSelectionToCharacterSelection(caret, false);
                                }
                            }
                        }

                        myAfterActions.put(event, () -> {
                            for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                                if (caret.hasSelection()) {
                                    adjustCharacterSelectionToLineSelection(caret, false);
                                }

                                if (caretColumns.containsKey(caret)) {
                                    LogicalPosition pos = new LogicalPosition(caret.getLogicalPosition().line, caretColumns.get(caret));
                                    caret.moveToLogicalPosition(pos);
                                }
                            }
                        });
                    }
                } else if (IF_LINE_FIX_CARET_AFTER_EDITING_ACTIONS.contains(action.getClass())) {
                    final HashMap<Caret, Integer> caretColumns = new HashMap<>();
                    if (settings.isDeleteOperations()) {
                        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                            if (caret.hasSelection()) {
                                LineSelectionState state = getSelectionStateIfExists(caret);
                                if (state != null && state.isLine()) {
                                    caretColumns.put(caret, caret.getLogicalPosition().column);
                                }
                            }
                        }

                        if (!caretColumns.isEmpty()) {
                            myAfterActions.put(event, () -> {
                                for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                                    if (caretColumns.containsKey(caret)) {
                                        LogicalPosition pos = new LogicalPosition(caret.getLogicalPosition().line, caretColumns.get(caret));
                                        caret.moveToLogicalPosition(pos);
                                    }
                                }
                            });
                        }
                    }
                } else if (RESTORE_COLUMN_AFTER_EDITING_ACTIONS.contains(action.getClass())) {
                    final HashMap<Caret, Integer> caretColumns = new HashMap<>();
                    if (settings.isUpDownMovement()) {
                        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                            caretColumns.put(caret, caret.getLogicalPosition().column);
                        }

                        if (!caretColumns.isEmpty()) {
                            myAfterActions.put(event, () -> {
                                for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                                    if (caretColumns.containsKey(caret)) {
                                        LogicalPosition pos = new LogicalPosition(caret.getLogicalPosition().line, caretColumns.get(caret));
                                        caret.moveToLogicalPosition(pos);
                                    }
                                }
                            });
                        }
                    }
                } else if (MOVE_TO_START_RESTORE_IF0_AFTER_ACTIONS.contains(action.getClass())) {
                    // these can replace selections, need to move to start, after if pasted was lines, then we should restore caret pos
                    final HashMap<Caret, Integer> caretColumns = new HashMap<>();
                    for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                        caretColumns.put(caret, caret.getLogicalPosition().column);
                        if (caret.hasSelection()) {
                            LineSelectionState state = getSelectionStateIfExists(caret);
                            if (state != null && state.isLine()) {
                                caret.moveToOffset(caret.getSelectionStart());
                            }
                        }
                    }

                    if (!caretColumns.isEmpty()) {
                        myAfterActions.put(event, () -> {
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
                } else if (MAKE_LINE_IF_LOOKS_IT_AFTER_ACTIONS.contains(action.getClass())) {
                    myAfterActions.put(event, () -> {
                        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                            // if it looks like a line then it is a line selection
                            if (caret.hasSelection()) {
                                if (myEditor.offsetToLogicalPosition(caret.getSelectionStart()).column == 0
                                        && myEditor.offsetToLogicalPosition(caret.getSelectionEnd()).column == 0) {
                                    LineSelectionState state = getSelectionState(caret);
                                    state.setLine(true);
                                }
                            }
                        }
                    });
                }
            });
        }
    }

    @Override
    public void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
        if (isEditorSupported()) {
            final Runnable runnable = myAfterActions.remove(event);
            if (runnable != null && !myEditor.isColumnMode()) {
                guard(runnable);
            }
        }
    }

    @Override
    public void beforeEditorTyping(char c, DataContext dataContext) {
        if (isEditorSupported()) {

        }
    }

    private void settingsChanged(@NotNull ApplicationSettings settings) {
        myDelayedRunner.runAll();
        hookListeners(settings);
        setupKeys(settings);
    }

    private void hookListeners(ApplicationSettings settings) {
        // wire ourselves in
        if (isEditorSupported()) {
            myEditor.getCaretModel().addCaretListener(this);
            myDelayedRunner.addRunnable("CaretListener", () -> {
                myEditor.getCaretModel().removeCaretListener(this);
            });

            if (settings.isMouseLineSelection()) {
                myEditor.addEditorMouseMotionListener(this);
                myEditor.addEditorMouseListener(this);
                myDelayedRunner.addRunnable("MouseListener", () -> {
                    myEditor.removeEditorMouseListener(this);
                    myEditor.removeEditorMouseMotionListener(this);
                });
            }

            if (myEditor.getProject() != null) {
                LookupManager.getInstance(myEditor.getProject()).addPropertyChangeListener(this);
            }
            myDelayedRunner.addRunnable("LookupManagerPropertyListener", () -> {
                LookupManager.getInstance(myEditor.getProject()).removePropertyChangeListener(this);
            });
        }
    }

    private ApplicationSettings getSettings() {
        return ApplicationSettings.getInstance();
    }

    private boolean isEditorSupported() {
        return myEditor.getProject() != null && !myEditor.isOneLineMode() && myEditor.getCaretModel().supportsMultipleCarets() && getSettings().isLineModeEnabled();
    }

    private void setupKeys(ApplicationSettings settings) {
        //addKeyOverride(settings.isEditorBackspaceKey(), "isEditorBackspaceKey", new BackspaceAction(), KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
        //addKeyOverride(settings.isEditorDeleteKey(), "isEditorDeleteKey", new DeleteAction(), KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        //addKeyOverride(settings.isEditorUpDownKeys(), "isEditorUpDownKeys", new MoveCaretLineUpAction(), KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));
        //addKeyOverride(settings.isEditorUpDownKeys(), "isEditorUpDownKeys", new MoveCaretLineDownAction(), KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
        //addKeyOverride(settings.isEditorLeftRightKeys(), "isEditorLeftRightKeys", new MoveCaretLineLeftAction(), KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0));
        //addKeyOverride(settings.isEditorLeftRightKeys(), "isEditorLeftRightKeys", new MoveCaretRightAction(), KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0));
    }

    private void addKeyOverride(boolean predicate, String runnerGroup, final AnAction action, final KeyStroke keyStroke) {
        if (predicate) {
            if (keyStroke != null) {
                final AnAction wrappedAction = EmptyAction.wrap(action);
                wrappedAction.registerCustomShortcutSet(keyStroke.getKeyCode(), keyStroke.getModifiers(), myEditor.getContentComponent());
                myDelayedRunner.addRunnable(runnerGroup, () -> wrappedAction.unregisterCustomShortcutSet(myEditor.getContentComponent()));
            }
        }
    }

    private void registerUpDownShortcuts(@NotNull JComponent component) {
        if (myEditorUpAction != null) myEditorUpAction.registerCustomShortcutSet(KeyEvent.VK_UP, 0, component);
        if (myEditorDownAction != null) myEditorDownAction.registerCustomShortcutSet(KeyEvent.VK_DOWN, 0, component);
    }

    private void unregisterUpDownShortcuts(@NotNull JComponent component) {
        if (myEditorUpAction != null) myEditorUpAction.unregisterCustomShortcutSet(component);
        if (myEditorDownAction != null) myEditorDownAction.unregisterCustomShortcutSet(component);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (LookupManager.PROP_ACTIVE_LOOKUP.equals(evt.getPropertyName())) {
            final JComponent rootPane = myEditor.getComponent();
            if (evt.getNewValue() != null) {
                //println("Active Lookup: Registering standard shortcuts");
                // pop-up active, register up/down to editor up down
                registerUpDownShortcuts(rootPane);
            } else {
                //println("Lookup deactivated: UnRegistering standard shortcuts");
                unregisterUpDownShortcuts(rootPane);
            }
        }
    }

    public void guard(Runnable runnable) {
        myCaretGuard.guard(runnable);
    }

    @NotNull
    public static LineSelectionAdjuster getInstance(@NotNull Editor editor) {
        return Plugin.getInstance().getSelectionAdjuster(editor);
    }

    public static void setCaretLineSelection(Editor editor, Caret caret, LogPos newPos, LogPos newStart, LogPos newEnd, boolean startIsAnchor, LineSelectionState state) {
        LineSelectionAdjuster.getInstance(editor).setCaretLineSelection(caret, newPos, newStart, newEnd, startIsAnchor, state);
    }

    public void setCaretLineSelection(Caret caret, LogPos newPos, LogPos newStart, LogPos newEnd, boolean startIsAnchor, LineSelectionState state) {
        myCaretGuard.guard(() -> {
            if (newStart.equals(newEnd)) {
                caret.moveToLogicalPosition(newPos);
                int offset = newPos.toOffset();
                caret.setSelection(offset, offset);
            } else {
                int startOffset = newStart.toOffset();
                int endOffset = newEnd.toOffset();
                //caret.moveToOffset(startIsAnchor ? endOffset : startOffset);
                caret.setSelection(startOffset, endOffset);
                caret.moveToLogicalPosition(newPos);
            }
            myEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
            state.setLine(true);

            LineSelectionState caretState = getSelectionState(caret);
            // copy values, do not replace so no state is split
            caretState.copyFrom(state);
        });
    }

    // use to remove or adjust selection, if it is a move then it will be cleared. If it has a line selection then the selection will be moved to the 
    // caret position
    public static void adjustLineSelectionToCharacterSelection(@NotNull Editor editor, @NotNull Caret caret, boolean isMoveOnly) {
        LineSelectionAdjuster.getInstance(editor).adjustLineSelectionToCharacterSelection(caret, isMoveOnly);
    }

    public void adjustLineSelectionToCharacterSelection(@NotNull Caret caret, boolean isMoveOnly) {
        myCaretGuard.guard(() -> {
            LineSelectionState state = getSelectionState(caret);
            int offset = caret.getOffset();

            if (isMoveOnly) {
                // reset it
                caret.setSelection(offset, offset);
                state.setAnchorOffset(offset);
            } else if (state.isLine()) {
                int anchorOffset = state.getAnchorOffset();
                //LogPos.Factory f = LogPos.factory(myEditor);
                //LogPos anchor = f.fromOffset(anchorOffset);
                //LogPos start = f.fromOffset(caret.getSelectionStart());
                //LogPos end = f.fromOffset(caret.getSelectionEnd());

                boolean startIsAnchor = anchorOffset <= offset;
                if (startIsAnchor) {
                    caret.setSelection(anchorOffset, offset);
                } else {
                    caret.setSelection(offset, anchorOffset);
                }
            }
            state.setLine(false);
        });
    }

    public void adjustCharacterSelectionToLineSelection(@NotNull Caret caret, boolean alwaysLine) {
        if (caret.hasSelection()) {
            LineSelectionState state = getSelectionState(caret);
            if (!state.isLine()) {
                final LogPos.Factory f = LogPos.factory(myEditor);
                final LogPos start = f.fromOffset(caret.getSelectionStart());
                final LogPos end = f.fromOffset(caret.getSelectionEnd());

                myCaretGuard.guard(() -> {
                    if (start.line == end.line && !alwaysLine) {
                        state.setLine(false);
                    } else {
                        LogPos pos = f.fromPos(caret.getLogicalPosition());
                        LogPos newStart = start;
                        LogPos newEnd = end;
                        LogPos newPos = pos;

                        newStart = start.atStartOfLine();
                        newEnd = end.atEndOfLine();

                        int anchorOffset = caret.getLeadSelectionOffset();
                        boolean startIsAnchor = anchorOffset <= caret.getOffset();

                        if (startIsAnchor) {
                            // start is anchor move caret to end
                            if (getCaretInSelection()) {
                                newPos = pos.onLine(newEnd.line - 1);
                            } else {
                                newPos = pos.onLine(newEnd.line);
                            }
                        } else {
                            // end is anchor move caret to start
                            if (getCaretInSelection()) {
                                newPos = pos.onLine(newStart.line);
                            } else {
                                newPos = pos.onLine(newStart.line > 0 ? newStart.line - 1 : newStart.line);
                            }
                        }

                        setCaretLineSelection(caret, newPos, newStart, newEnd, startIsAnchor, state);
                    }
                });
            }
        }
    }

    @NotNull
    public static LineSelectionState getSelectionState(@NotNull Editor editor, @NotNull Caret caret) {
        return Plugin.getInstance().getSelectionAdjuster(editor).getSelectionState(caret);
    }

    @NotNull
    public LineSelectionState getSelectionState(@NotNull Caret caret) {
        if (caret == caret.getCaretModel().getPrimaryCaret()) {
            return myPrimarySelectionState;
        } else {
            return mySelectionStates.computeIfAbsent(caret, k -> new LineSelectionState());
        }
    }

    @Nullable
    public LineSelectionState getSelectionStateIfExists(@NotNull Caret caret) {
        if (caret == caret.getCaretModel().getPrimaryCaret()) {
            return myPrimarySelectionState;
        } else {
            return mySelectionStates.get(caret);
        }
    }

    private void println(String message) {
        if (Plugin.isFeatureLicensed(Plugin.FEATURE_DEVELOPMENT)) {
            System.out.println(message);
        }
    }

    @Override
    public void mousePressed(EditorMouseEvent e) {
        if (e.getArea() == EDITING_AREA) {
            //int offset = myEditor.getCaretModel().getOffset();
            //println("mouse pressed offset: " + offset /*+ " event:" + e.getMouseEvent() */ + " isConsumed" + e.isConsumed());

            if (myMouseAnchor == -1) {
                // if we have a selection then we need to move the caret out of it otherwise the editor interprets it as a continuation of selection
                Caret caret = myEditor.getCaretModel().getPrimaryCaret();
                if (caret.hasSelection()) {
                    caret.moveToOffset(caret.getSelectionStart());
                }
            }
        }
    }

    @Override
    public void mouseClicked(EditorMouseEvent e) {

    }

    private boolean isControlledSelect(EditorMouseEvent e) {
        boolean ctrl = (e.getMouseEvent().getModifiers() & (MouseEvent.CTRL_MASK)) != 0;
        return ctrl ^ (ApplicationSettings.getInstance().getMouseModifier() == MouseModifierType.CTRL_LINE.intValue);
    }

    @Override
    public void mouseReleased(EditorMouseEvent e) {
        if (e.getArea() == EDITING_AREA) {
            int offset = myEditor.getCaretModel().getOffset();
            //println("mouse released offset: " + offset + " anchor: " + myMouseAnchor /*+ " event:" + e.getMouseEvent()*/ + " isConsumed " + e.isConsumed());

            // adjust it one final time
            if (myMouseAnchor != -1 && myEditor.getSelectionModel().hasSelection()) {
                final int mouseAnchor = myMouseAnchor;
                final boolean controlledSelect = isControlledSelect(e);
                //ApplicationManager.getApplication().invokeLater(() -> {
                //    adjustMouseSelection(mouseAnchor, controlledSelect, true);
                //});
                adjustMouseSelection(mouseAnchor, controlledSelect, true);
                //e.consume();
            }
        }

        myMouseAnchor = -1;
    }

    @Override
    public void mouseEntered(EditorMouseEvent e) {

    }

    @Override
    public void mouseExited(EditorMouseEvent e) {

    }

    @Override
    public void mouseMoved(EditorMouseEvent e) {

    }

    @Override
    public void mouseDragged(EditorMouseEvent e) {
        if (e.getArea() == EDITING_AREA) {
            int offset = myEditor.getCaretModel().getOffset();
            //println("mouseDragged offset: " + offset + " ctrl:" + isControlledSelect(e) + " anchor: " + myMouseAnchor + " event:" + e.getMouseEvent() + " isConsumed" + e.isConsumed());
            if (myMouseAnchor == -1) {
                // first drag event, take the selection's anchor
                myMouseAnchor = myEditor.getCaretModel().getPrimaryCaret().getLeadSelectionOffset();
            } else {
                final int mouseAnchor = myMouseAnchor;
                adjustMouseSelection(mouseAnchor, isControlledSelect(e), false);
                //e.consume();
            }
        }
    }

    public void adjustMouseSelection(int mouseAnchor, boolean alwaysChar, boolean finalAdjustment) {
        Caret caret = myEditor.getCaretModel().getPrimaryCaret();

        // mouse selection is between mouseAnchor and the caret offset
        // if they are on the same line then it is a char mark, else line mark
        final LogPos.Factory f = LogPos.factory(myEditor);
        final int offset = caret.getOffset();
        final LogPos pos = f.fromPos(caret.getLogicalPosition());

        boolean startIsAnchor = caret.getLeadSelectionOffset() <= offset;
        int startOffset = startIsAnchor ? mouseAnchor : offset;
        int endOffset = startIsAnchor ? offset : mouseAnchor;

        final LogPos start = f.fromOffset(startOffset);
        final LogPos end = f.fromOffset(endOffset);
        LineSelectionState state = getSelectionState(caret);

        state.setAnchorOffset(mouseAnchor);

        myCaretGuard.guard(() -> {
            if (start.line == end.line || alwaysChar) {
                caret.setSelection(startOffset, endOffset);
                caret.moveToLogicalPosition(pos);
                state.setLine(false);
            } else if (caret.hasSelection()) {
                LogPos newStart = start.atStartOfLine();
                LogPos newEnd = end.atStartOfLine();
                LogPos newPos = pos;

                if (finalAdjustment) {
                    //println("final selection adjustment ctrl: " + alwaysChar + " startAnchor: " + startIsAnchor);
                    if (startIsAnchor) {
                        // start is anchor move caret to end
                        if (getCaretInSelection()) {
                            newPos = pos.onLine(end.line - 1);
                        } else {
                            newPos = pos.onLine(end.line);
                        }
                    } else {
                        // end is anchor move caret to start
                        if (getCaretInSelection()) {
                            newPos = pos.onLine(start.line);
                        } else {
                            newPos = pos.onLine(start.line > 0 ? start.line - 1 : start.line);
                        }
                    }

                    //println("final selection adjustment pos " + newPos);
                    caret.moveToLogicalPosition(newPos);
                    state.setLine(true);
                }

                //if (finalAdjustment) {
                //    println("final selection adjustment newStart: " + newStart + " newEnd: " + newEnd);
                //} else {
                //    println("selection adjustment newStart: " + newStart + " newEnd: " + newEnd);
                //}
                caret.setSelection(newStart.toOffset(), newEnd.toOffset());
            }
        });
    }

    @Override
    public void caretPositionChanged(CaretEvent e) {
        Caret caret = e.getCaret();
        if (myMouseAnchor == -1 && caret != null) {
            myCaretGuard.ifUnguarded(() -> {
                // reset to char if we can
                adjustLineSelectionToCharacterSelection(caret, !caret.hasSelection());
            });
        }
    }

    @Override
    public void caretAdded(CaretEvent e) {
        //println("caretAdded " + e.getCaret());
    }

    @Override
    public void caretRemoved(CaretEvent e) {
        //println("caretRemoved " + e.toString());
        mySelectionStates.remove(e.getCaret());
    }

    @Override
    public void selectionChanged(SelectionEvent e) {
        StringBuilder stringBuilder = new StringBuilder();
        int i;

        stringBuilder.append("selectionChanged\n");
        i = 0;
        for (TextRange range : e.getOldRanges()) {
            stringBuilder.append(" old range[").append(i++).append("]: ").append(range.toString()).append("\n");
        }

        i = 0;
        for (TextRange range : e.getNewRanges()) {
            stringBuilder.append(" new range[").append(i++).append("]: ").append(range.toString()).append("\n");
        }

        stringBuilder.append("\n");
        println(stringBuilder.toString());
    }
}
