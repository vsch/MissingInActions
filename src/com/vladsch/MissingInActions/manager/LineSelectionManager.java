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

import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.*;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import com.vladsch.MissingInActions.Plugin;
import com.vladsch.MissingInActions.actions.character.MiaMultiplePasteAction;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.settings.ApplicationSettingsListener;
import com.vladsch.MissingInActions.settings.MouseModifierType;
import com.vladsch.MissingInActions.util.CommonUIShortcuts;
import com.vladsch.MissingInActions.util.DelayedRunner;
import com.vladsch.MissingInActions.util.ReEntryGuard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

import static com.intellij.openapi.editor.event.EditorMouseEventArea.EDITING_AREA;

/**
 * Adjust a line selection to a normal selection when selection is adjusted by moving the caret
 */
public class LineSelectionManager implements
        CaretListener
        //, SelectionListener
        , PropertyChangeListener
        , EditorMouseListener
        , EditorMouseMotionListener
        , Disposable
{

    final private Editor myEditor;
    final private ReEntryGuard myCaretGuard = new ReEntryGuard();
    final private HashMap<Caret, StoredLineSelectionState> mySelectionStates = new HashMap<>();
    final private @NotNull DelayedRunner myDelayedRunner = new DelayedRunner();
    final private MessageBusConnection myMessageBusConnection;
    final private @NotNull ActionSelectionAdjuster myActionSelectionAdjuster;
    final private @NotNull EditorPositionFactory myPositionFactory;

    final private StoredLineSelectionState myPrimarySelectionState = new StoredLineSelectionState();
    private int myMouseAnchor = -1;
    private boolean myIsSelectionEndExtended;
    private boolean myIsSelectionStartExtended;
    private final CaretHighlighter myCaretHighlighter;
    private ApplicationSettings mySettings;
    private AnAction myMultiPasteAction;
    //private PasteOverrider myPasteOverrider;

    //private AwtRunnable myInvalidateStoredLineStateRunnable = new AwtRunnable(true, this::invalidateStoredLineState);
    private boolean myIsActiveLookup;  // true if a lookup is active in the editor

    @Override
    public void dispose() {
        //println("LineSelectionAdjuster disposed");
        myDelayedRunner.runAll();
        myActionSelectionAdjuster.dispose();
        myMessageBusConnection.disconnect();
    }

    @NotNull
    public static LineSelectionManager getInstance(@NotNull Editor editor) {
        return Plugin.getInstance().getSelectionAdjuster(editor);
    }

    public LineSelectionManager(Editor editor) {
        myEditor = editor;
        myPositionFactory = new EditorPositionFactory(this);

        // this can fail if caret visual attributes are not implemented in the IDE (since 2017.1)
        CaretHighlighter caretHighlighter;
        try {
            caretHighlighter = new CaretHighlighterImpl(this);
        } catch (Throwable ignored) {
            caretHighlighter = CaretHighlighter.NULL;
        }

        myCaretHighlighter = caretHighlighter;
        myActionSelectionAdjuster = new ActionSelectionAdjuster(this, NormalAdjustmentMap.getInstance());

        myMultiPasteAction = null;
        //myPasteOverrider = null;

        mySettings = ApplicationSettings.getInstance();
        settingsChanged(mySettings);

        myMessageBusConnection = ApplicationManager.getApplication().getMessageBus().connect(this);
        myMessageBusConnection.subscribe(ApplicationSettingsListener.TOPIC, this::settingsChanged);
    }

    @NotNull
    public EditorPositionFactory getPositionFactory() {
        return myPositionFactory;
    }

    public Editor getEditor() {
        return myEditor;
    }

    @NotNull
    public EditorCaret getEditorCaret(@NotNull Caret caret) {
        return new EditorCaret(myPositionFactory, caret, getSelectionState(caret));
    }

    @NotNull
    private StoredLineSelectionState getStoredSelectionState(@NotNull Caret caret) {
        if (caret == caret.getCaretModel().getPrimaryCaret()) {
            return myPrimarySelectionState;
        } else {
            return mySelectionStates.computeIfAbsent(caret, k -> new StoredLineSelectionState());
        }
    }

    @Nullable
    private StoredLineSelectionState getStoredSelectionStateIfExists(@NotNull Caret caret) {
        if (caret == caret.getCaretModel().getPrimaryCaret()) {
            return myPrimarySelectionState;
        } else {
            return mySelectionStates.get(caret);
        }
    }

    @NotNull
    public LineSelectionState getSelectionState(@NotNull Caret caret) {
        StoredLineSelectionState state = getStoredSelectionState(caret);
        return new LineSelectionState(state.anchorColumn, state.isStartAnchor);
    }

    void setLineSelectionState(@NotNull Caret caret, int anchorColumn, boolean isStartAnchor) {
        StoredLineSelectionState state = getStoredSelectionState(caret);
        state.anchorColumn = anchorColumn;
        state.isStartAnchor = isStartAnchor;
    }

    public void resetSelectionState(Caret caret) {
        if (caret == caret.getCaretModel().getPrimaryCaret()) {
            myPrimarySelectionState.resetToDefault();
        } else {
            StoredLineSelectionState state = getStoredSelectionStateIfExists(caret);
            if (state != null) {
                mySelectionStates.remove(caret);
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isSelectionEndExtended() {
        return myIsSelectionEndExtended;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isSelectionStartExtended() {
        return myIsSelectionStartExtended;
    }

    private void settingsChanged(@NotNull ApplicationSettings settings) {
        // unhook all the stuff for settings registration
        mySettings = settings;

        boolean startExtended = settings.isSelectionStartExtended();
        boolean endExtended = settings.isSelectionEndExtended();

        HashMap<Caret, Boolean> lineCarets = new HashMap<>();
        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
            EditorCaret editorCaret = getEditorCaret(caret);
            if (editorCaret.isLine()) {
                lineCarets.put(caret, true);
                editorCaret
                        .toCharSelection()
                        .normalizeCaretPosition()
                        .commit();
            }
        }

        myDelayedRunner.runAll();

        // change our mode
        myIsSelectionEndExtended = settings.isSelectionEndExtended();
        myIsSelectionStartExtended = settings.isSelectionStartExtended();

        hookListeners(settings);
        myCaretHighlighter.removeCaretHighlight();
        myCaretHighlighter.updateCaretHighlights();
        myCaretHighlighter.settingsChanged(settings);

        // change all selections that were lines back to lines
        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
            if (lineCarets.containsKey(caret)) {
                EditorCaret editorCaret = getEditorCaret(caret);
                editorCaret
                        .toLineSelection()
                        .normalizeCaretPosition()
                        .commit();
            }
        }
    }

    ///**
    // * This is needed to override paste actions only if there are multiple carets
    // * <p>
    // * Otherwise, formatting after paste will not work right in single caret mode and
    // * without the override multi-caret select after paste and all the smart paste
    // * adjustments don't work because the IDE does not provide data for last pasted
    // * ranges.
    // */
    //private class PasteOverrider implements IdeEventQueue.EventDispatcher {
    //    @Override
    //    public boolean dispatch(@NotNull AWTEvent e) {
    //        if (e instanceof KeyEvent && e.getID() == KeyEvent.KEY_PRESSED) {
    //            Component owner = UIUtil.findParentByCondition(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(), component -> {
    //                return component == myEditor.getComponent();
    //            });
    //
    //            if (owner == myEditor.getComponent()) {
    //                boolean registerPasteOverrides = mySettings.isOverrideStandardPaste() && myEditor.getCaretModel().getCaretCount() > 1;
    //                if (registerPasteOverrides == (myMultiPasteAction == null)) {
    //                    if (!registerPasteOverrides) {
    //                        // unregister them
    //                        unRegisterPasteOverrides();
    //                    } else {
    //                        // we register our own pastes to handle multi-caret
    //                        registerPasteOverrides();
    //                    }
    //                }
    //            }
    //        }
    //        return false;
    //    }
    //}

    private void registerPasteOverrides() {
        myMultiPasteAction = new MiaMultiplePasteAction();
        myMultiPasteAction.registerCustomShortcutSet(CommonUIShortcuts.getMultiplePaste(), myEditor.getContentComponent());
    }

    private void unRegisterPasteOverrides() {
        if (myMultiPasteAction != null) myMultiPasteAction.unregisterCustomShortcutSet(myEditor.getContentComponent());
        myMultiPasteAction = null;
    }

    private void hookListeners(ApplicationSettings settings) {
        // wire ourselves in
        if (isLineSelectionSupported()) {
            myEditor.getCaretModel().addCaretListener(this);
            myDelayedRunner.addRunnable("CaretListener", () -> {
                myEditor.getCaretModel().removeCaretListener(this);
            });

            //myEditor.getSelectionModel().addSelectionListener(this);
            //myDelayedRunner.addRunnable("CaretListener", () -> {
            //    myEditor.getSelectionModel().removeSelectionListener(this);
            //});

            if (myEditor.getProject() != null) {
                LookupManager.getInstance(myEditor.getProject()).addPropertyChangeListener(this);

                myDelayedRunner.addRunnable("LookupManagerPropertyListener", () -> {
                    LookupManager.getInstance(myEditor.getProject()).removePropertyChangeListener(this);
                });
            }

            if (settings.isMouseLineSelection()) {
                myEditor.addEditorMouseListener(this);
                myEditor.addEditorMouseMotionListener(this);
                myDelayedRunner.addRunnable("MouseListener", () -> {
                    myEditor.removeEditorMouseListener(this);
                    myEditor.removeEditorMouseMotionListener(this);
                });
            }

            // override standard pastes
            if (settings.isOverrideStandardPaste()) {
                registerPasteOverrides();
                myDelayedRunner.addRunnable("Override Paste", this::unRegisterPasteOverrides);
                //if (settings.isOverrideStandardPasteShowInstructions()) {
                //    myPasteOverrider = new PasteOverrider();
                //    IdeEventQueue.getInstance().addDispatcher(myPasteOverrider, this);
                //
                //    myDelayedRunner.addRunnable("Override Paste", () -> {
                //        unRegisterPasteOverrides();
                //        if (myPasteOverrider != null) IdeEventQueue.getInstance().removeDispatcher(myPasteOverrider);
                //        myMultiPasteAction = null;
                //    });
                //} else {
                //    registerPasteOverrides();
                //    myDelayedRunner.addRunnable("Override Paste", this::unRegisterPasteOverrides);
                //}
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (LookupManager.PROP_ACTIVE_LOOKUP.equals(evt.getPropertyName())) {
            final JComponent rootPane = myEditor.getComponent();
            Editor newEditor = null;
            Editor oldEditor = null;
            if (evt.getNewValue() instanceof LookupImpl) {
                LookupImpl lookup = (LookupImpl) evt.getNewValue();
                newEditor = lookup.getEditor();
            }
            if (evt.getOldValue() instanceof LookupImpl) {
                LookupImpl lookup = (LookupImpl) evt.getOldValue();
                oldEditor = lookup.getEditor();
            }
            myIsActiveLookup = newEditor == myEditor;
            int tmp = 0;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isLineSelectionSupported() {
        return myEditor.getProject() != null && !myEditor.isOneLineMode() && !myIsActiveLookup;
    }

    public void guard(Runnable runnable) {
        myCaretGuard.guard(runnable);
    }

    public void ifUnguarded(@NotNull Runnable runnable) {myCaretGuard.ifUnguarded(runnable);}

    public void ifUnguarded(boolean ifGuardedRunOnExit, @NotNull Runnable runnable) {myCaretGuard.ifUnguarded(ifGuardedRunOnExit, runnable);}

    public void ifUnguarded(@NotNull Runnable runnable, @Nullable Runnable runOnGuardExit) {myCaretGuard.ifUnguarded(runnable, runOnGuardExit);}

    public boolean unguarded() {return myCaretGuard.unguarded();}

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
        if (e.getArea() == EDITING_AREA && !myEditor.getSettings().isUseSoftWraps() && !myEditor.isColumnMode()) {
            int offset = myEditor.getCaretModel().getOffset();
            //println("mouse released offset: " + offset + " anchor: " + myMouseAnchor /*+ " event:" + e.getMouseEvent()*/ + " isConsumed " + e.isConsumed());

            // adjust it one final time
            if (myMouseAnchor != -1 && myEditor.getSelectionModel().hasSelection()) {
                final int mouseAnchor = myMouseAnchor;
                final boolean controlledSelect = isControlledSelect(e);
                adjustMouseSelection(mouseAnchor, controlledSelect, true);
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
        if (e.getArea() == EDITING_AREA && !myEditor.getSettings().isUseSoftWraps() && !myEditor.isColumnMode()) {
            // TODO: get mouse anchor in Visual
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
        // DONE: in all modes
        Caret caret = myEditor.getCaretModel().getPrimaryCaret();

        // mouse selection is between mouseAnchor and the caret offset
        // if they are on the same line then it is a char mark, else line mark
        final int offset = caret.getOffset();

        boolean isStartAnchor = mouseAnchor <= offset;
        int startOffset = isStartAnchor ? mouseAnchor : offset;
        int endOffset = isStartAnchor ? offset : mouseAnchor;

        StoredLineSelectionState state = getStoredSelectionState(caret);
        state.anchorColumn = myPositionFactory.fromOffset(mouseAnchor).column;
        state.isStartAnchor = isStartAnchor;

        final EditorPosition start = myPositionFactory.fromOffset(startOffset);
        final EditorPosition end = myPositionFactory.fromOffset(endOffset);

        myCaretGuard.guard(() -> {
            if (start.line == end.line || alwaysChar) {
                final EditorPosition pos = myPositionFactory.fromPosition(caret.getLogicalPosition());
                caret.setSelection(startOffset, endOffset);
                caret.moveToLogicalPosition(pos);

                if (finalAdjustment && state != myPrimarySelectionState) {
                    mySelectionStates.remove(caret);
                }
            } else if (!caret.hasSelection()) {
                if (finalAdjustment && state != myPrimarySelectionState) {
                    mySelectionStates.remove(caret);
                } else {
                    state.resetToDefault();
                }
            } else {
                if (finalAdjustment) {
                    // need to adjust final caret position for inside or outside the selection
                    if (myIsSelectionEndExtended) {
                        if (isStartAnchor) {
                            caret.moveToLogicalPosition(end.addLine(-1).atColumn(caret.getLogicalPosition()));
                        } else {
                            caret.moveToLogicalPosition(start.atEndOfLineSelection().atColumn(caret.getLogicalPosition()));
                        }
                    } else {
                        if (isStartAnchor) {
                            caret.moveToLogicalPosition(end.atColumn(caret.getLogicalPosition()));
                        } else {
                            caret.moveToLogicalPosition(start.atColumn(caret.getLogicalPosition()));
                        }
                    }
                    state.isStartAnchor = isStartAnchor;
                    caret.setSelection(isStartAnchor ? startOffset : caret.getOffset(), isStartAnchor ? caret.getOffset() : endOffset);
                    EditorCaret editorCaret = new EditorCaret(myPositionFactory, caret, new LineSelectionState(state.anchorColumn, state.isStartAnchor));
                    editorCaret
                            .toCaretPositionBasedLineSelection(true, false)
                            .normalizeCaretPosition()
                            .commit();
                } else {
                    if (isStartAnchor) {
                        caret.setSelection(start.atStartOfLine().getOffset(), end.atStartOfLine().getOffset());
                    } else {
                        caret.setSelection(start.atStartOfLine().getOffset(), end.atEndOfLineSelection().getOffset());
                    }
                }
            }
        });
    }

    @Override
    public void caretPositionChanged(CaretEvent e) {
        myCaretGuard.ifUnguarded(() -> {
            Caret caret = e.getCaret();
            if (myMouseAnchor == -1 && caret != null) {
                myCaretHighlighter.updateCaretHighlights();
            }
        });
    }

    private void invalidateStoredLineState() {
        // clear any states for carets that don't have selection to eliminate using a stale state
        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
            StoredLineSelectionState state = mySelectionStates.get(caret);
            if (state != null) {
                if (state == myPrimarySelectionState) {
                    state.resetToDefault();
                } else {
                    mySelectionStates.remove(caret);
                }
            }
        }
    }

    //@Override
    //public void selectionChanged(SelectionEvent e) {
    //    //if (e.getEditor() == myEditor) {
    //    //    myCaretGuard.ifUnguarded(myInvalidateStoredLineStateRunnable, false);
    //    //}
    //}

    @Override
    public void caretAdded(CaretEvent e) {
        Caret caret = e.getCaret();
        if (myMouseAnchor == -1 && caret != null) {
            myCaretHighlighter.caretAdded(caret);
        }
    }

    @Override
    public void caretRemoved(CaretEvent e) {
        mySelectionStates.remove(e.getCaret());
        Caret caret = e.getCaret();
        if (myMouseAnchor == -1 && caret != null) {
            myCaretHighlighter.caretRemoved(caret);
        }
    }

    public void updateCaretHighlights() {
        myCaretHighlighter.updateCaretHighlights();
    }

    private static class StoredLineSelectionState {
        int anchorColumn = -1;
        boolean isStartAnchor = true;

        void resetToDefault() {
            anchorColumn = -1;
            isStartAnchor = true;
        }

        @Override
        public String toString() {
            return "StoredLineSelectionState{" +
                    "anchorColumn=" + anchorColumn +
                    ", isStartAnchor=" + isStartAnchor +
                    '}';
        }
    }
}
