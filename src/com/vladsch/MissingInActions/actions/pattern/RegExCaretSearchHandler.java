/*
 * Copyright (c) 2016-2018 Vladimir Schneider <vladimir.schneider@gmail.com>
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
