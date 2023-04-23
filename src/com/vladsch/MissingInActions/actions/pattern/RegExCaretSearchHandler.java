// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.openapi.editor.Caret;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.ReverseRegEx.util.RegExMatcher;
import com.vladsch.ReverseRegEx.util.RegExPattern;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract public class RegExCaretSearchHandler extends PatternSearchCaretHandler<RegExMatcher> {
    protected RegExCaretSearchHandler(boolean backwards) {
        super(backwards);
    }

    @Nullable
    protected abstract RegExPattern getPattern(@NotNull LineSelectionManager manager, @NotNull Caret caret, @NotNull Range range, @NotNull BasedSequence chars);

    @Override
    @Nullable
    final protected RegExMatcher prepareMatcher(@NotNull LineSelectionManager manager, @NotNull Caret caret, @NotNull Range range, @NotNull BasedSequence chars) {
        RegExPattern pattern = getPattern(manager, caret, range, chars);
        int offset = caret.getOffset();
        RegExMatcher myMatcher = null;

        if (pattern != null) {
            myMatcher = pattern.matcher(chars);

            if (!myBackwards) {
                if (offset > range.getEnd()) {
                    int tmp = 0;
                }
                if (offset >= range.getEnd()) {
                    return null;
                }
                myMatcher.region(offset, range.getEnd());
            } else {
                if (offset <= range.getStart()) {
                   return null; 
                }
                myMatcher.region(range.getStart(), offset);
            }
            myMatcher.useTransparentBounds(true);
            myMatcher.useAnchoringBounds(false);

            if (!myMatcher.find()) {
                myMatcher = null;
            }
        }
        return myMatcher;
    }

    protected abstract CaretMatch getCaretMatch(RegExMatcher matcher, int selStart, int selEnd);

    @Override
    @Nullable
    final protected CaretMatch nextMatch(RegExMatcher matcher, BasedSequence chars, @NotNull Range range, @Nullable CaretMatch previousMatch) {
        CaretMatch match = null;
        boolean found;

        found = previousMatch == null || matcher.find();
        if (found) {
            int start = matcher.start();
            int end = matcher.end();
            if (range.contains(start, end)) {
                // see if there are captured groups, then we select the spanning range of the groups
                int selStart = myBackwards ? end : start;
                int selEnd = selStart;

                int groupCount = matcher.groupCount();
                if (groupCount > 0) {
                    selStart = matcher.end();
                    selEnd = matcher.start();

                    for (int i = 1; i <= groupCount; i++) {
                        if (selStart > matcher.start(i)) selStart = matcher.start(i);
                        if (selEnd < matcher.end(i)) selEnd = matcher.end(i);
                    }
                }

                match = getCaretMatch(matcher, selStart, selEnd);
            } else {
                int tmp = 0;
            }
        }
        return match;
    }
}
