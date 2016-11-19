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

package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.vladsch.MissingInActions.actions.RangeLimitedSpawningHandler;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.Range;
import com.vladsch.flexmark.util.sequence.SubSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

@SuppressWarnings("WeakerAccess")
abstract public class PatternCaretSearchHandler<T> extends RangeLimitedSpawningHandler {
    final private boolean mySingleMatch;

    protected PatternCaretSearchHandler(boolean backwards, boolean lineMode, boolean singleLine, boolean singleMatch) {
        super(backwards, lineMode, singleLine);
        mySingleMatch = singleMatch;
    }

    protected static class CaretMatch {
        public final int caretOffset;
        public final int matchLength;
        public final int selectionStart;
        public final int selectionEnd;

        public CaretMatch(int caretOffset, int matchLength, int selectionStart, int selectionEnd) {
            this.caretOffset = caretOffset;
            this.matchLength = matchLength;
            this.selectionStart = selectionStart;
            this.selectionEnd = selectionEnd;
        }
    }

    /**
     * find match in chars using myBackwards for direction flag
     *
     * @param chars         sequence to search
     * @param range         range to limit the search
     * @param previousMatch where to start searching, start at 0 if null  @return match or null if not found
     */
    @Nullable
    protected abstract CaretMatch nextMatch(T matcher, BasedSequence chars, @NotNull Range range, @Nullable CaretMatch previousMatch);

    @Nullable
    protected abstract T prepareMatcher(@NotNull LineSelectionManager adjuster, @NotNull Caret caret, @NotNull Range range, @NotNull BasedSequence chars);

    protected boolean perform(@NotNull LineSelectionManager adjuster, @NotNull Caret caret, @NotNull Range range, @NotNull ArrayList<CaretState> createCarets) {
        Editor editor = caret.getEditor();
        SubSequence chars = new SubSequence(editor.getDocument().getCharsSequence());
        boolean keepCaret = false;

        T matcher = prepareMatcher(adjuster, caret, range, chars);
        if (matcher != null) {
            // forward search withing range in document
            CaretMatch lastMatch = null;
            while (true) {
                CaretMatch match = nextMatch(matcher, chars, range, lastMatch);
                if (match == null) break;

                // found it, create or move caret here
                if (!keepCaret) {
                    keepCaret = true;
                    caret.moveToOffset(match.caretOffset);
                    caret.setSelection(match.selectionStart, match.selectionEnd);
                } else {
                    // create a new position
                    LogicalPosition offset = editor.offsetToLogicalPosition(range.getStart() + match.caretOffset);
                    LogicalPosition startOffset = editor.offsetToLogicalPosition(range.getStart() + match.selectionStart);
                    LogicalPosition endOffset = editor.offsetToLogicalPosition(range.getEnd() + match.selectionEnd);
                    CaretState caretState = new CaretState(offset, startOffset, endOffset);
                    createCarets.add(caretState);
                }

                if (mySingleMatch || match.caretOffset + match.matchLength >= chars.length()) break;
                lastMatch = match;
            }
        }

        return keepCaret;
    }
}
