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
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.Range;
import org.apache.commons.lang.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExPatternSearchHandler extends PatternSearchCaretHandler<Matcher> {
    private final Pattern myPattern;

    public RegExPatternSearchHandler(boolean backwards, boolean lineMode, boolean singleLine, boolean singleMatch, String pattern) {
        super(backwards, lineMode, singleLine, singleMatch);
        myPattern = Pattern.compile(pattern);
        if (backwards) {
            throw new NotImplementedException("Backwards regex search not implemented");
        }
    }

    @Override
    Matcher prepareMatcher(@NotNull LineSelectionManager adjuster, @NotNull Caret caret, @NotNull Range range, @NotNull BasedSequence chars) {
        return myPattern.matcher(chars);
    }

    @Override
    @Nullable
    Match nextMatch(Matcher matcher, BasedSequence chars, @NotNull Range range, @Nullable Match previousMatch) {
        Match match = null;

        if (!myBackwards) {
            boolean found;

            if (previousMatch == null) {
                found = matcher.find(chars.getStartOffset());
            } else {
                found = matcher.find();
            }

            if (found && range.contains(matcher.start(), matcher.end() + 1)) {
                int offset = matcher.start();
                match = new Match(offset, matcher.end() + 1 - offset, offset, offset);
            }
        } else {
            // without rewriting regex search one char at a time, stepping backwards until we start matching and once started keep going 
            // until we fail
            // a better solution is to rewrite the regex
            throw new NotImplementedException("Backwards regex search not implemented");
        }
        return match;
    }
}
