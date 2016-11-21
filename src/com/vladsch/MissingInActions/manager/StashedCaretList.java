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
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class StashedCaretList implements Disposable {
    private final ArrayList<StashedCaret> myCarets = new ArrayList<>();
    private final LineSelectionManager myManager;
    private int myStashLimit;

    public StashedCaretList(LineSelectionManager manager) {
        this(manager, Integer.MAX_VALUE);
    }
    
    public StashedCaretList(LineSelectionManager manager, int stashLimit) {
        myManager = manager;
        myStashLimit = stashLimit;
    }

    public ArrayList<StashedCaret> getCarets() {
        return myCarets;
    }

    public LineSelectionManager getManager() {
        return myManager;
    }

    public Editor getEditor() {
        return myManager.getEditor();
    }

    public Document getDocument() {
        return myManager.getEditor().getDocument();
    }

    public int getStashLimit() {
        return myStashLimit;
    }

    public void setStashLimit(int stashLimit) {
        myStashLimit = Math.max(0, stashLimit);
        while (myCarets.size() > myStashLimit) {
            myCarets.remove(myCarets.size() - 1);
        }
    }

    @NotNull
    public EditorPositionFactory getLogPosFactory() {
        return myManager.getPositionFactory();
    }

    public void releaseUnused() {
        int iMax = myCarets.size();
        for (int i = iMax; i-- > 0; ) {
            StashedCaret caret = myCarets.get(i);
            if (i >= myStashLimit) {
                myCarets.remove(i);
                if (caret != null) caret.myRangeMarker.dispose();
            } else if (caret != null && !caret.myRangeMarker.isValid()) {
                caret.myRangeMarker.dispose();
                myCarets.set(i, null);
            }
        }
    }

    public int size() {
        return Math.min(myCarets.size(), myStashLimit);
    }

    public int actualSize() {
        return myCarets.size();
    }

    public boolean isEmpty() {
        return myCarets.isEmpty();
    }

    public boolean isValidAt(int index) {
        return index >= 0 && index < size() && myCarets.get(index) != null;
    }
    
    public void releaseAt(int index) {
        if (isValidAt(index)) {
            StashedCaret caret = myCarets.get(index);
            if (caret != null) {
                caret.myRangeMarker.dispose();
                myCarets.set(index, null);
            }
        }
    }

    public void push(Caret caret) {
        add(myCarets.size(), caret);
    }

    @Nullable
    public EditorCaretState pop() {
        releaseUnused();
        EditorCaretState caretState = getRaw(size()-1,true,true);
        return caretState;
    }

    public void remove(int index) {
        StashedCaret caret = myCarets.remove(index);

        if (caret != null) {
            caret.myRangeMarker.dispose();
        }
    }

    @Nullable
    public EditorCaretState get(int index) {
        releaseUnused();
        EditorCaretState caretState = getRaw(index, true,false);
        return caretState;
    }

    @Nullable
    private EditorCaretState getRaw(int index, boolean getIt, boolean removeIt) {
        EditorCaretState caretState = null;
        if (isValidAt(index)) {
            StashedCaret stashedCaret;

            if (removeIt) {
                if (getIt) {
                    stashedCaret = myCarets.remove(index);
                } else {
                    myCarets.remove(index);
                    stashedCaret = null;
                }
            } else {
                stashedCaret = myCarets.get(index);
            }

            if (stashedCaret != null) {
                EditorPositionFactory f = getLogPosFactory();
                EditorPosition selStart = f.fromOffset(stashedCaret.myRangeMarker.getStartOffset());
                EditorPosition selEnd = f.fromOffset(stashedCaret.myRangeMarker.getEndOffset());
                EditorPosition pos = stashedCaret.myStartIsAnchor ? selEnd : selStart;

                caretState = new EditorCaretState(f, pos, selStart, selEnd);
            }
        }
        return caretState;
    }

    public void add(int index, @NotNull Caret caret) {
        LineSelectionState state = myManager.getSelectionState(caret);
        add(index, caret.getSelectionStart(), caret.getSelectionEnd(), state.getAnchorOffset(caret.getSelectionStart()) == caret.getLeadSelectionOffset(), state.isLine());
    }
    
    public void add(int index, int start, int end, boolean startIsAnchor, boolean isLine) {
        releaseUnused();

        if (index < 0 || index >= myStashLimit) {
            throw new IllegalArgumentException("index "+index+" out of range of [0, " + myStashLimit + ")");
        }

        RangeMarker marker = myManager.getEditor().getDocument().createRangeMarker(start, end);
        StashedCaret stashedCaret = new StashedCaret(marker, startIsAnchor, isLine);
        myCarets.add(index, stashedCaret);
    }

    @Override
    public void dispose() {
        for (StashedCaret caret : myCarets) {
            if (caret != null) {
                caret.myRangeMarker.dispose();
            }
        }
    }

    private static class StashedCaret {
        final RangeMarker myRangeMarker; // selection and position
        final boolean myStartIsAnchor; // meaning end of marker is also the caret pos
        final boolean myIsLine; // meaning end of marker is also the caret pos

        public StashedCaret(RangeMarker rangeMarker, boolean startIsAnchor, boolean isLine) {
            myRangeMarker = rangeMarker;
            myStartIsAnchor = startIsAnchor;
            myIsLine = isLine;
        }
    }
}
