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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretAttributes;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.util.Key;
import com.intellij.ui.JBColor;
import com.intellij.util.messages.MessageBusConnection;
import com.vladsch.MissingInActions.Plugin;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.settings.ApplicationSettingsListener;
import com.vladsch.MissingInActions.settings.MouseModifierType;
import com.vladsch.MissingInActions.util.DelayedRunner;
import com.vladsch.MissingInActions.util.ReEntryGuard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;

import static com.intellij.openapi.editor.event.EditorMouseEventArea.EDITING_AREA;

/**
 * Adjust a line selection to a normal selection when selection is adjusted by moving the caret
 */
public class LineSelectionManager implements CaretListener
        , EditorMouseListener
        , EditorMouseMotionListener
        , Disposable
{
    private static boolean ourHaveCustomCaretAttributes = true;

    final private Editor myEditor;
    final private ReEntryGuard myCaretGuard = new ReEntryGuard();
    final private HashMap<Caret, StoredLineSelectionState> mySelectionStates = new HashMap<>();
    final private @NotNull DelayedRunner myDelayedRunner = new DelayedRunner();
    final private MessageBusConnection myMessageBusConnection;
    final private @Nullable Key<CaretAttributes> myCustomAttributesKey;
    final private @NotNull ActionSelectionAdjuster myActionSelectionAdjuster;
    final private @NotNull EditorPositionFactory myPositionFactory;

    final private StoredLineSelectionState myPrimarySelectionState = new StoredLineSelectionState();
    private int myMouseAnchor = -1;
    @Nullable private Caret myPrimaryCaret = null;
    @Nullable private Caret mySecondaryCaret = null;
    private @Nullable CaretAttributes myPrimaryAttributes = null;
    private @Nullable CaretAttributes mySecondaryAttributes = null;
    private boolean mySelectionExtendsPastCaret;

    @Override
    public void dispose() {
        //println("LineSelectionAdjuster disposed");
        myDelayedRunner.runAll();
        myActionSelectionAdjuster.dispose();
        myMessageBusConnection.disconnect();
    }

    public LineSelectionManager(Editor editor) {
        myEditor = editor;
        myPositionFactory = new EditorPositionFactory(this);

        Key<CaretAttributes> key = null;
        if (ourHaveCustomCaretAttributes) {
            try {
                key = CaretAttributes.KEY;
            } catch (Throwable ignored) {
                ourHaveCustomCaretAttributes = false;
            }
        }

        myActionSelectionAdjuster = new ActionSelectionAdjuster(this, NormalAdjustmentMap.getInstance());
        //myCustomAttributesKey = key;
        myCustomAttributesKey = null;

        ApplicationSettings settings = ApplicationSettings.getInstance();
        settingsChanged(settings);

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

    public void resetSelectionState(Caret caret) {
        if (caret == caret.getCaretModel().getPrimaryCaret()) {
            System.out.println("Commit reset on primary");
            myPrimarySelectionState.resetToDefault();
        } else {
            System.out.println("Commit reset on secondary");
            mySelectionStates.remove(caret);
        }
    }

    @NotNull
    public LineSelectionState getSelectionState(@NotNull Caret caret) {
        StoredLineSelectionState state = getStoredSelectionState(caret);
        return new LineSelectionState(state.myAnchorOffset, state.myIsStartAnchor, state.myIsLine);
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

    void setLineSelectionState(@NotNull Caret caret, int anchorOffset, boolean isStartAnchor) {
        StoredLineSelectionState state = getStoredSelectionState(caret);
        System.out.println("Commit Line: " + isStartAnchor + " anchorOffset: " + anchorOffset);
        state.myAnchorOffset = anchorOffset;
        state.myIsStartAnchor = isStartAnchor;
        state.myIsLine = true;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean hasLineSelection(@NotNull Caret caret) {
        if (caret.hasSelection()) {
            StoredLineSelectionState state = getStoredSelectionStateIfExists(caret);
            return state != null && state.myIsLine;
        } else {
            resetSelectionState(caret);
            return false;
        }
    }

    public void updateCaretHighlights() {
        highlightCarets();
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isSelectionExtendsPastCaret() {
        return mySelectionExtendsPastCaret;
    }

    private void settingsChanged(@NotNull ApplicationSettings settings) {
        // unhook all the stuff for settings registration
        HashMap<Caret, Boolean> lineCarets = new HashMap<>();
        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
            EditorCaret editorCaret = getEditorCaret(caret);
            if (editorCaret.isLine()) {
                lineCarets.put(caret, true);
                editorCaret.setCharSelection()
                        .normalizeCaretPosition()
                        .commit();
            }
        }

        myDelayedRunner.runAll();
        
        // change our mode
        mySelectionExtendsPastCaret = ApplicationSettings.getInstance().isSelectionExtendsPastCaret();
        
        hookListeners(settings);
        removeCaretHighlight();
        highlightCarets();

        if (myCustomAttributesKey != null) {
            myPrimaryAttributes = new CaretAttributes(null, CaretAttributes.Weight.HEAVY);
            mySecondaryAttributes = new CaretAttributes(JBColor.RED, CaretAttributes.Weight.THIN);
        }
        
        // change all selections that were lines back to lines
        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
            if (lineCarets.containsKey(caret)) {
                EditorCaret editorCaret = getEditorCaret(caret);
                editorCaret.setLineSelection().normalizeCaretPosition().commit();
            }
        }
    }

    private void removeCaretHighlight() {
        if (myCustomAttributesKey != null) {
            if (myPrimaryCaret != null) {
                myPrimaryCaret.putUserData(myCustomAttributesKey, null);
            }
            if (mySecondaryCaret != null) {
                mySecondaryCaret.putUserData(myCustomAttributesKey, null);
            }
        }
        myPrimaryCaret = null;
        mySecondaryCaret = null;
    }

    private void highlightCarets() {
        if (myCustomAttributesKey != null) {
            Caret caret = myEditor.getCaretModel().getPrimaryCaret();

            int caretCount = myEditor.getCaretModel().getCaretCount();
            if (myPrimaryCaret != null && (caretCount == 1 || myPrimaryCaret != caret)) {
                removeCaretHighlight();
            }

            if (caretCount > 1) {
                myPrimaryCaret = caret;
                myPrimaryCaret.putUserData(myCustomAttributesKey, myPrimaryAttributes);

                List<Caret> carets = caret.getCaretModel().getAllCarets();
                mySecondaryCaret = carets.get(0) == myPrimaryCaret ? carets.get(1) : carets.get(0);
                mySecondaryCaret.putUserData(myCustomAttributesKey, mySecondaryAttributes);
            }
        }
    }

    private void hookListeners(ApplicationSettings settings) {
        // wire ourselves in
        if (isLineSelectionSupported()) {
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
        }
    }

    private static ApplicationSettings getSettings() {
        return ApplicationSettings.getInstance();
    }

    public boolean isLineSelectionSupported() {
        return myEditor.getProject() != null && !myEditor.isOneLineMode();
    }

    public boolean isMultiCaretSupported() {
        return myEditor.getProject() != null && !myEditor.isOneLineMode() && myEditor.getCaretModel().supportsMultipleCarets();
    }

    public void guard(Runnable runnable) {
        myCaretGuard.guard(runnable);
    }

    @NotNull
    public static LineSelectionManager getInstance(@NotNull Editor editor) {
        return Plugin.getInstance().getSelectionAdjuster(editor);
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
        final int offset = caret.getOffset();

        boolean isStartAnchor = mouseAnchor <= offset;
        int startOffset = isStartAnchor ? mouseAnchor : offset;
        int endOffset = isStartAnchor ? offset : mouseAnchor;

        StoredLineSelectionState state = getStoredSelectionState(caret);
        state.myAnchorOffset = mouseAnchor;
        state.myIsStartAnchor = isStartAnchor;
        state.myIsLine = false;

        final EditorPosition start = myPositionFactory.fromOffset(startOffset);
        final EditorPosition end = myPositionFactory.fromOffset(endOffset);

        myCaretGuard.guard(() -> {
            if (start.line == end.line || alwaysChar) {
                final EditorPosition pos = myPositionFactory.fromPosition(caret.getLogicalPosition());
                caret.setSelection(startOffset, endOffset);
                caret.moveToLogicalPosition(pos);
                state.myIsLine = false;

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
                    if (mySelectionExtendsPastCaret) {
                        if (isStartAnchor) {
                            caret.moveToLogicalPosition(end.addLine(-1));
                        } else {
                            caret.moveToLogicalPosition(start.atEndOfLine());
                        }
                    } else {
                        if (isStartAnchor) {
                            caret.moveToLogicalPosition(end);
                        } else {
                            caret.moveToLogicalPosition(start);
                        }
                    } 
                    caret.setSelection(isStartAnchor ? startOffset : caret.getOffset(), isStartAnchor ? caret.getOffset() : endOffset);
                    EditorCaret editorCaret = new EditorCaret(myPositionFactory, caret, new LineSelectionState(state.myAnchorOffset, state.myIsStartAnchor, state.myIsLine));
                    editorCaret.setLineSelection()
                            .normalizeCaretPosition()
                            .commit();
                } else {
                    if (isStartAnchor) {
                        caret.setSelection(start.atStartOfLine().getOffset(), end.atStartOfLine().getOffset());
                    } else {
                        caret.setSelection(start.atEndOfLine().getOffset(), end.atEndOfLine().getOffset());
                    } 
                }
            }
        });
    }

    @Override
    public void caretPositionChanged(CaretEvent e) {
        Caret caret = e.getCaret();
        if (myMouseAnchor == -1 && caret != null) {
            myCaretGuard.ifUnguarded(this::highlightCarets);
        }
    }

    @Override
    public void caretAdded(CaretEvent e) {
        //println("caretAdded " + e.getCaret());
        myCaretGuard.ifUnguarded(this::highlightCarets);
    }

    @Override
    public void caretRemoved(CaretEvent e) {
        //println("caretRemoved " + e.toString());
        mySelectionStates.remove(e.getCaret());
        myCaretGuard.ifUnguarded(() -> {
            removeCaretHighlight();
            highlightCarets();
        });
    }

    private static class StoredLineSelectionState {
        int myAnchorOffset = 0;
        boolean myIsStartAnchor = true;
        boolean myIsLine = false;

        void resetToDefault() {
            myAnchorOffset = 0;
            myIsStartAnchor = true;
            myIsLine = false;
        }

        @Override
        public String toString() {
            return "LineSelectionState{" +
                    "myAnchorOffset=" + myAnchorOffset +
                    ", myIsLine=" + myIsLine +
                    ", myIsStartAnchor=" + myIsStartAnchor +
                    '}';
        }
    }
}
