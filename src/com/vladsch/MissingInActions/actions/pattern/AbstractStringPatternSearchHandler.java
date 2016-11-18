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

import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractStringPatternSearchHandler<T> extends PatternSearchCaretHandler<T> {
    public AbstractStringPatternSearchHandler(boolean backwards, boolean lineMode, boolean singleLine, boolean singleMatch) {
        super(backwards, lineMode, singleLine, singleMatch);
    }
    
    abstract @NotNull String getPattern(T matcher);

    @Override
    @Nullable
    Match nextMatch(T matcher, BasedSequence chars, @NotNull Range range, @Nullable Match previousMatch) {
        int pos;
        if (!myBackwards) {
            int lastPos = previousMatch == null ? range.getStart() : previousMatch.caretOffset + previousMatch.matchLength;
            pos = chars.indexOf(getPattern(matcher), lastPos);
        } else {
            int lastPos = previousMatch == null ? range.getEnd() : previousMatch.caretOffset + previousMatch.matchLength;
            pos = chars.lastIndexOf(getPattern(matcher), lastPos);
        }
        
        if (pos != -1 && range.contains(pos, pos + getPattern(matcher).length())) {
            return new Match(pos, getPattern(matcher).length(), pos, pos);
        }
        return null;
    }
}
