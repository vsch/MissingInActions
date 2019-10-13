/*
 * Copyright (c) 2016-2019 Vladimir Schneider <vladimir.schneider@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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
