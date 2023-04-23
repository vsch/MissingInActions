// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

@SuppressWarnings("WeakerAccess")
abstract public class PatternSearchCaretHandler<T> extends RangeLimitedCaretSpawningHandler {
    /**
     * Pattern search range limited caret spawning handler
     * <p>
     * use single match when the caret is to be moved to a new position based on match location
     * instead of spawning new carets for every match location.
     *
     * @param backwards search is backwards from caret offset
     */
    protected PatternSearchCaretHandler(boolean backwards) {
        super(backwards);
    }

    public abstract boolean isCaseSensitive();
    abstract protected boolean isSingleMatch();

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

    abstract protected boolean isMoveFirstMatch();

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
    protected abstract T prepareMatcher(@NotNull LineSelectionManager manager, @NotNull Caret caret, @NotNull Range range, @NotNull BasedSequence chars);

    protected boolean perform(@NotNull LineSelectionManager manager, @NotNull Caret caret, @NotNull Range range, @NotNull ArrayList<CaretState> createCarets) {
        Editor editor = caret.getEditor();
        final BasedSequence chars = BasedSequence.of(editor.getDocument().getCharsSequence());
        boolean keepCaret = !isMoveFirstMatch();

        T matcher = prepareMatcher(manager, caret, range, chars);
        if (matcher != null) {
            // forward search withing range in document
            CaretMatch lastMatch = null;
            while (true) {
                CaretMatch match = nextMatch(matcher, chars, range, lastMatch);
                if (match == null) break;

                // found it, create or move caret here
                if (!keepCaret) {
                    keepCaret = true;
                    if (isMoveFirstMatch()) {
                        caret.moveToOffset(match.caretOffset);
                        caret.setSelection(match.selectionStart, match.selectionEnd);
                    }
                } else {
                    // create a new position if caret moved
                    LogicalPosition offset = editor.offsetToLogicalPosition(match.caretOffset);
                    LogicalPosition startOffset = editor.offsetToLogicalPosition(match.selectionStart);
                    LogicalPosition endOffset = editor.offsetToLogicalPosition(match.selectionEnd);
                    CaretState caretState = new CaretState(offset, startOffset, endOffset);
                    createCarets.add(caretState);
                }

                if (isSingleMatch()) break;
                lastMatch = match;
            }
        }

        return keepCaret || isSingleMatch();
    }
}
