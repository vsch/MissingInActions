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
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.util.Key;
import com.intellij.ui.JBColor;
import com.intellij.util.messages.MessageBusConnection;
import com.vladsch.MissingInActions.Plugin;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.settings.ApplicationSettingsListener;
import com.vladsch.MissingInActions.settings.MouseModifierType;
import com.vladsch.MissingInActions.util.DelayedRunner;
import com.vladsch.MissingInActions.util.LogPos;
import com.vladsch.MissingInActions.util.ReEntryGuard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.intellij.openapi.editor.event.EditorMouseEventArea.EDITING_AREA;
import static com.intellij.util.text.CharArrayUtil.isEmptyOrSpaces;
import static com.vladsch.MissingInActions.Plugin.getCaretInSelection;

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
    final private HashMap<Caret, LineSelectionState> mySelectionStates = new HashMap<>();
    final private @NotNull DelayedRunner myDelayedRunner = new DelayedRunner();
    final private MessageBusConnection myMessageBusConnection;
    final private @Nullable Key<CaretAttributes> myCustomAttributesKey;
    final private @NotNull ActionSelectionAdjuster myActionSelectionAdjuster;

    final private LineSelectionState myPrimarySelectionState = new LineSelectionState();
    private int myMouseAnchor = -1;
    @Nullable private Caret myPrimaryCaret = null;
    @Nullable private Caret mySecondaryCaret = null;
    private @Nullable CaretAttributes myPrimaryAttributes = null;
    private @Nullable CaretAttributes mySecondaryAttributes = null;

    @Override
    public void dispose() {
        //println("LineSelectionAdjuster disposed");
        myDelayedRunner.runAll();
        myActionSelectionAdjuster.dispose();
        myMessageBusConnection.disconnect();
    }

    public LineSelectionManager(Editor editor) {
        myEditor = editor;

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

    public void updateCaretHighlights() {
        highlightCarets();
    }

    private void settingsChanged(@NotNull ApplicationSettings settings) {
        myDelayedRunner.runAll();
        hookListeners(settings);
        removeCaretHighlight();
        highlightCarets();

        if (myCustomAttributesKey != null) {
            myPrimaryAttributes = new CaretAttributes(null, CaretAttributes.Weight.HEAVY);
            mySecondaryAttributes = new CaretAttributes(JBColor.RED, CaretAttributes.Weight.THIN);
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
        if (isAdjustmentSupported()) {
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

    public boolean isAdjustmentSupported() {
        return myEditor.getProject() != null && !myEditor.isOneLineMode() && myEditor.getCaretModel().supportsMultipleCarets();
    }

    public void guard(Runnable runnable) {
        myCaretGuard.guard(runnable);
    }

    @NotNull
    public static LineSelectionManager getInstance(@NotNull Editor editor) {
        return Plugin.getInstance().getSelectionAdjuster(editor);
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
        LineSelectionManager.getInstance(editor).adjustLineSelectionToCharacterSelection(caret, isMoveOnly);
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

    public void adjustCharacterSelectionToLineSelection(@NotNull Caret caret, boolean alwaysLine, boolean trimmedLine) {
        if (caret.hasSelection()) {
            LineSelectionState state = getSelectionState(caret);
            if (!state.isLine()) {
                myCaretGuard.guard(() -> {
                    final LogPos.Factory f = LogPos.factory(myEditor);
                    LogPos start = f.fromOffset(caret.getSelectionStart());
                    LogPos end = f.fromOffset(caret.getSelectionEnd());

                    if (start.line == end.line && !alwaysLine) {
                        state.setLine(false);
                    } else {
                        if (trimmedLine) {
                            // if from start to end of line is all blanks, then move start to beginning of next line
                            // and if from end to start of line is all blanks then move end to start of line
                            if (isEmptyOrSpaces(myEditor.getDocument().getCharsSequence(), start.toOffset(), start.atEndOfLine().toOffset())) {
                                start = start.atEndOfLine();
                            }
                            if (isEmptyOrSpaces(myEditor.getDocument().getCharsSequence(), end.atStartOfLine().toOffset(), end.toOffset())) {
                                end = end.atStartOfLine();
                            }
                        }

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

            highlightCarets();
        }
    }

    @Override
    public void caretAdded(CaretEvent e) {
        //println("caretAdded " + e.getCaret());
        highlightCarets();
    }

    @Override
    public void caretRemoved(CaretEvent e) {
        //println("caretRemoved " + e.toString());
        mySelectionStates.remove(e.getCaret());
        removeCaretHighlight();
        highlightCarets();
    }

    public Editor getEditor() {
        return myEditor;
    }
}