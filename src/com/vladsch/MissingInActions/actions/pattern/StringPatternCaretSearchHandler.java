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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

abstract public class StringPatternCaretSearchHandler extends RegExPatternCaretSearchHandler {
    public StringPatternCaretSearchHandler(boolean backwards, boolean lineMode, boolean singleLine, boolean singleMatch) {
        super(backwards, lineMode, singleLine, singleMatch);
    }

    protected static class StringPattern {
        final String pattern;
        final int flags;

        public StringPattern(String pattern, int flags) {
            this.pattern = pattern;
            this.flags = flags;
        }
    }

    @Nullable
    protected abstract StringPattern getString(@NotNull LineSelectionManager adjuster, @NotNull Caret caret, @NotNull Range range, @NotNull BasedSequence chars);

    @Override
    @Nullable
    final protected Pattern getPattern(@NotNull LineSelectionManager adjuster, @NotNull Caret caret, @NotNull Range range, @NotNull BasedSequence chars) {
        StringPattern stringPattern = getString(adjuster, caret, range, chars);
        Pattern pattern = null;

        if (stringPattern != null) {
            try {
                pattern = Pattern.compile(Pattern.quote(stringPattern.pattern), stringPattern.flags);
            } catch (IllegalArgumentException e) {
                pattern = null;
            }
        }
        return pattern;
    }
}
