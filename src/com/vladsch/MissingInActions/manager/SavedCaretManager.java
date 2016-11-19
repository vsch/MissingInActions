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
import com.intellij.openapi.editor.RangeMarker;
import com.vladsch.MissingInActions.util.EditorCaretState;
import com.vladsch.MissingInActions.util.LogPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class SavedCaretManager implements Disposable {
    private final ArrayList<SavedCaret> myCarets = new ArrayList<>();
    private final LineSelectionManager myManager;
    private final Document myDocument;
    private int mySaveLimit = 5;

    public SavedCaretManager(LineSelectionManager manager) {
        myManager = manager;
        myDocument = manager.getEditor().getDocument();
    }

    public int getSaveLimit() {
        return mySaveLimit;
    }

    public void setSaveLimit(int saveLimit) {
        mySaveLimit = Math.max(0, saveLimit);
        removeOverLimit();
    }

    public void removeOverLimit() {
        while (myCarets.size() > mySaveLimit) {
            myCarets.remove(myCarets.size() - 1);
        }
    }

    public void releaseUnused() {
        removeOverLimit();

        int iMax = myCarets.size();
        for (int i = 0; i < iMax; i++) {
            SavedCaret caret = myCarets.get(i);
            if (caret != null && !caret.myRangeMarker.isValid()) {
                caret.myRangeMarker.dispose();
                myCarets.set(i, null);
            }
        }
    }

    public int size() {
        return Math.min(myCarets.size(), mySaveLimit);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean isValidAt(int index) {
        return index >= 0 && index < size() && myCarets.get(index) != null;
    }
    
    public void releaseAt(int index) {
        releaseUnused();
        if (isValidAt(index)) {
            SavedCaret caret = myCarets.get(index);
            if (caret != null) {
                caret.myRangeMarker.dispose();
                myCarets.set(index, null);
            }
        }
    }

    public void push(Caret caret) {
        add(myCarets.size(), caret);
    }

    public void remove(int index) {
        SavedCaret caret = myCarets.remove(index);

        if (caret != null) {
            caret.myRangeMarker.dispose();
        }
    }

    @Nullable
    public EditorCaretState pop() {
        releaseUnused();
        EditorCaretState caretState = getRaw(size()-1,true,true);
        return caretState;
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
            SavedCaret savedCaret;

            if (removeIt) {
                if (getIt) {
                    savedCaret = myCarets.remove(index);
                } else {
                    myCarets.remove(index);
                    savedCaret = null;
                }
            } else {
                savedCaret = myCarets.get(index);
            }

            if (savedCaret != null) {
                LogPos.Factory f = LogPos.factory(myManager.getEditor());
                LogPos selStart = f.fromOffset(savedCaret.myRangeMarker.getStartOffset());
                LogPos selEnd = f.fromOffset(savedCaret.myRangeMarker.getEndOffset());
                LogPos pos = savedCaret.myStartIsAnchor ? selEnd : selStart;

                caretState = new EditorCaretState(f, pos, selStart, selEnd, savedCaret.myIsLine);
            }
        }
        return caretState;
    }

    public void add(int index, @NotNull Caret caret) {
        LineSelectionState state = myManager.getSelectionState(caret);
        add(index, caret.getSelectionStart(), caret.getSelectionEnd(), state.getAnchorOffset() == caret.getLeadSelectionOffset(), state.isLine());
    }
    
    public void add(int index, int start, int end, boolean startIsAnchor, boolean isLine) {
        releaseUnused();

        if (index < 0 || index >= mySaveLimit) {
            throw new IllegalArgumentException("index "+index+" out of range of [0, " + mySaveLimit + ")");
        }

        RangeMarker marker = myDocument.createRangeMarker(start, end);
        SavedCaret savedCaret = new SavedCaret(marker, startIsAnchor, isLine);
        myCarets.add(index, savedCaret);
    }

    @Override
    public void dispose() {
        for (SavedCaret caret : myCarets) {
            if (caret != null) {
                caret.myRangeMarker.dispose();
            }
        }
    }

    private static class SavedCaret {
        final RangeMarker myRangeMarker; // selection and position
        final boolean myStartIsAnchor; // meaning end of marker is also the caret pos
        final boolean myIsLine; // meaning end of marker is also the caret pos

        public SavedCaret(RangeMarker rangeMarker, boolean startIsAnchor, boolean isLine) {
            myRangeMarker = rangeMarker;
            myStartIsAnchor = startIsAnchor;
            myIsLine = isLine;
        }
    }
}
