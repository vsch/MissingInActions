// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions;

import com.intellij.openapi.editor.Caret;
import org.jetbrains.annotations.Nullable;

import static java.lang.Math.abs;

public class CaretOffsetPreserver {
    final private int myOffset;
    private int matchedDelta = Integer.MAX_VALUE;
    private int matchedIndex = -1;
    private int myIndex = 0;
    private @Nullable Caret myMatchedCaret = null;

    public CaretOffsetPreserver(int offset) {
        myOffset = offset;
    }

    public void tryCaret(Caret caret) {
        int delta = abs(myOffset - caret.getOffset());
        if (matchedDelta > delta) {
            matchedDelta = delta;
            matchedIndex = myIndex;
            myMatchedCaret = caret;
        }
        myIndex++;
    }

    public void tryOffset(int offset) {
        if (offset >= 0) {
            int delta = abs(myOffset - offset);
            if (matchedDelta > delta) {
                matchedDelta = delta;
                matchedIndex = myIndex;
                myMatchedCaret = null;
            }
        }
        myIndex++;
    }

    public int getOffset() {
        return myOffset;
    }

    public int getMatchedDelta() {
        return matchedDelta;
    }

    public int getMatchedIndex() {
        return matchedIndex;
    }

    public int getTriedCount() {
        return myIndex;
    }

    public boolean isFirst() {
        return  myIndex == 0;
    }

    @Nullable
    public Caret getMatchedCaret() {
        return myMatchedCaret;
    }
}
