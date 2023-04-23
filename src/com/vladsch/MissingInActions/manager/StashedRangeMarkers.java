// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.manager;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

@SuppressWarnings("WeakerAccess")
public class StashedRangeMarkers implements Disposable {
    private final ArrayList<RangeMarker> myMarkers = new ArrayList<>();
    private final LineSelectionManager myManager;
    private int myStashLimit;

    public StashedRangeMarkers(LineSelectionManager manager) {
        this(manager, 10);
    }

    public StashedRangeMarkers(LineSelectionManager manager, int stashLimit) {
        myManager = manager;
        myStashLimit = stashLimit;
    }

    public static boolean isEmpty(@Nullable RangeMarker rangeMarker) {
        return rangeMarker == null || !rangeMarker.isValid() || rangeMarker.getStartOffset() >= rangeMarker.getEndOffset();
    }

    public ArrayList<RangeMarker> getMarkers() {
        return myMarkers;
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
        while (myMarkers.size() > myStashLimit) {
            myMarkers.remove(myMarkers.size() - 1);
        }
    }

    @NotNull
    public EditorPositionFactory getLogPosFactory() {
        return myManager.getPositionFactory();
    }

    public void releaseUnused() {
        int iMax = myMarkers.size();
        RangeMarker[] markers = new RangeMarker[myStashLimit];
        int index = 0;

        for (int i = iMax; i-- > 0; ) {
            RangeMarker marker = myMarkers.get(i);
            if (index < myStashLimit && !isEmpty(marker)) {
                markers[index++] = marker;
            } else {
                if (marker != null) marker.dispose();
            }
        }

        myMarkers.clear();

        for (int i = index; i-- > 0; ) {
            myMarkers.add(markers[i]);
        }
    }

    public int size() {
        return Math.min(myMarkers.size(), myStashLimit);
    }

    public int actualSize() {
        return myMarkers.size();
    }

    public boolean isEmpty() {
        return myMarkers.isEmpty();
    }

    public boolean isValidAt(int index) {
        return index >= 0 && index < size() && myMarkers.get(index) != null;
    }

    public void releaseAt(int index) {
        if (isValidAt(index)) {
            RangeMarker marker = myMarkers.get(index);
            if (marker != null) {
                marker.dispose();
                myMarkers.set(index, null);
            }
        }
    }

    public void push(RangeMarker marker) {
        releaseUnused();
        add(myMarkers.size(), marker);
    }

    @Nullable
    public RangeMarker pop() {
        releaseUnused();
        RangeMarker marker = getRaw(size() - 1, true, true);
        return marker;
    }

    @Nullable
    public RangeMarker pop(int deltaFromTop) {
        releaseUnused();
        RangeMarker marker = getRaw(size() - 1 - deltaFromTop, true, true);
        return marker;
    }

    @Nullable
    public RangeMarker peek() {
        releaseUnused();
        RangeMarker marker = getRaw(size() - 1, true, false);
        return marker;
    }

    @Nullable
    public RangeMarker peek(int deltaFromTop) {
        releaseUnused();
        RangeMarker marker = getRaw(size() - 1 - deltaFromTop, true, false);
        return marker;
    }

    public void remove(int deltaFromTop) {
        releaseUnused();
        final int index = size() - 1 - deltaFromTop;
        RangeMarker marker = isValidAt(index) ? myMarkers.remove(index) : null;

        if (marker != null) {
            marker.dispose();
        }
    }

    public int getStoredIndex(RangeMarker other) {
        int index = 0;
        for (int i = myMarkers.size(); i-- > 0; ) {
            RangeMarker marker = myMarkers.get(i);
            if (marker == null || !marker.isValid()) continue;

            if (marker.getStartOffset() == other.getStartOffset() && marker.getEndOffset() == other.getEndOffset()) {
                return index;
            }

            index++;
        }
        return -1;
    }

    public boolean isStored(RangeMarker other) {
        for (RangeMarker marker : myMarkers) {
            if (marker == null || !marker.isValid()) continue;
            if (marker.getStartOffset() == other.getStartOffset() && marker.getEndOffset() == other.getEndOffset()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public RangeMarker get(int deltaFromTop) {
        releaseUnused();
        RangeMarker marker = getRaw(size() - 1 - deltaFromTop, true, false);
        return marker;
    }

    @Nullable
    private RangeMarker getRaw(int index, boolean getIt, boolean removeIt) {
        RangeMarker marker = null;
        if (isValidAt(index)) {
            if (removeIt) {
                marker = myMarkers.remove(index);
                assert marker != null;

                if (getIt) {
                    RangeMarker dummy = new SelectionRangeMarker(marker);
                    marker.dispose();
                    marker = dummy;
                } else {
                    marker.dispose();
                    marker = null;
                }
            } else if (getIt) {
                marker = myMarkers.get(index);
            }
        }
        return marker;
    }

    public void add(int index, @NotNull Caret caret) {
        EditorCaret editorCaret = myManager.getEditorCaret(caret);
        add(index, caret.getSelectionStart(), caret.getSelectionEnd(), editorCaret.isStartAnchor(), editorCaret.isLine());
    }

    public void add(int index, int start, int end, boolean isStartAnchor, boolean isLine) {
        RangeMarker marker = myManager.getEditor().getDocument().createRangeMarker(start, end);
        add(index, marker);
    }

    public void add(int index, @Nullable RangeMarker marker) {
        if (index < 0 || index > myMarkers.size()) {
            throw new IllegalArgumentException("index " + index + " out of range of [0, " + myMarkers.size() + "]");
        }

        myMarkers.add(index, marker);
    }

    @NotNull
    public RangeMarker[] getRangeMarkers() {
        releaseUnused();
        RangeMarker[] markers = new RangeMarker[myMarkers.size()];
        int i = 0;
        for (RangeMarker marker : myMarkers) {
            markers[i++] = new SelectionRangeMarker(marker);
        }
        return markers;
    }

    @Override
    public void dispose() {
        for (RangeMarker caret : myMarkers) {
            if (caret != null) {
                caret.dispose();
            }
        }
    }

    public static class SelectionRangeMarker implements RangeMarker {
        private final int myStartOffset;
        private final int myEndOffset;
        private boolean myGreedyToLeft;
        private boolean myGreedyToRight;
        private final Document myDocument;

        public SelectionRangeMarker(RangeMarker other) {
            myStartOffset = other.getStartOffset();
            myEndOffset = other.getEndOffset();
            myDocument = other.getDocument();
            myGreedyToLeft = other.isGreedyToLeft();
            myGreedyToRight = other.isGreedyToRight();
        }

        public SelectionRangeMarker(Editor editor) {
            myStartOffset = editor.getSelectionModel().getSelectionStart();
            myEndOffset = editor.getSelectionModel().getSelectionEnd();
            myDocument = editor.getDocument();
            myGreedyToLeft = false;
            myGreedyToRight = false;
        }

        @NotNull
        @Override
        public Document getDocument() {
            return myDocument;
        }

        @Override
        public int getStartOffset() {
            return myStartOffset;
        }

        @Override
        public int getEndOffset() {
            return myEndOffset;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public void setGreedyToLeft(final boolean greedy) {
            myGreedyToLeft = greedy;
        }

        @Override
        public void setGreedyToRight(final boolean greedy) {
            myGreedyToRight = greedy;
        }

        @Override
        public boolean isGreedyToRight() {
            return myGreedyToRight;
        }

        @Override
        public boolean isGreedyToLeft() {
            return myGreedyToLeft;
        }

        @Override
        public void dispose() {

        }

        @Nullable
        @Override
        public <T> T getUserData(@NotNull final Key<T> key) {
            return null;
        }

        @Override
        public <T> void putUserData(@NotNull final Key<T> key, @Nullable final T value) {

        }
    }
}
