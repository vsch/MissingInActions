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
import com.vladsch.MissingInActions.util.IndexMapper;
import com.vladsch.MissingInActions.util.LogPos;
import com.vladsch.MissingInActions.util.ReversedCharSequence;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.Range;
import org.apache.commons.lang.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract public class RegExPatternCaretSearchHandler extends PatternCaretSearchHandler<RegExPatternCaretSearchHandler.MyMatcher> {
    protected RegExPatternCaretSearchHandler(boolean backwards, boolean lineMode, boolean singleLine, boolean singleMatch) {
        super(backwards, lineMode, singleLine, singleMatch);
        if (backwards) {
            throw new NotImplementedException("Backwards regex search not implemented");
        }
    }

    @Nullable
    protected abstract Pattern getPattern(@NotNull LineSelectionManager adjuster, @NotNull Caret caret, @NotNull Range range, @NotNull BasedSequence chars);

    protected static class MyMatcher {
        final Matcher matcher;
        final IndexMapper indexMapper;

        MyMatcher(Matcher matcher, IndexMapper indexMapper) {
            this.matcher = matcher;
            this.indexMapper = indexMapper;
        }

        // here the coordinates are mapped from our original to possibly reversed and back
        // @formatter:off
        int start()                          { return indexMapper.map(matcher.start()); }
        int start(String name)               { return indexMapper.map(matcher.start(name)); }
        int end()                            { return indexMapper.map(matcher.end()); }
        int end(String name)                 { return indexMapper.map(matcher.end(name)); }
        int regionStart()                    { return indexMapper.map(matcher.regionStart()); }
        int regionEnd()                      { return indexMapper.map(matcher.regionEnd()); }
        int start(int group)                 { return indexMapper.map(matcher.start(group)); }
        int end(int group)                   { return indexMapper.map(matcher.end(group)); }
                                                    
        boolean find(int start)              { return matcher.find(indexMapper.map(start)); }
        Matcher region(int start, int end)   { return matcher.region(indexMapper.map(start), indexMapper.map(end));}

        // these are not mapped
        Pattern pattern() {return matcher.pattern();}
        //MatchResult toMatchResult() {return matcher.toMatchResult();}
        Matcher usePattern(Pattern newPattern) {return matcher.usePattern(newPattern);}
        Matcher reset() {return matcher.reset();}
        Matcher reset(CharSequence input) {return matcher.reset(input);}
        String group() {return matcher.group();}
        String group(int group) {return matcher.group(group);}
        String group(String name) {return matcher.group(name);}
        int groupCount() {return matcher.groupCount();}
        boolean matches() {return matcher.matches();}
        boolean find() {return matcher.find();}
        boolean lookingAt() {return matcher.lookingAt();}
        static String quoteReplacement(String s) {return Matcher.quoteReplacement(s);}
        //Matcher appendReplacement(StringBuffer sb, String replacement) {return matcher.appendReplacement(sb, replacement);}
        StringBuffer appendTail(StringBuffer sb) {return matcher.appendTail(sb);}
        String replaceAll(String replacement) {return matcher.replaceAll(replacement);}
        String replaceFirst(@NotNull String replacement) {return matcher.replaceFirst(replacement);}
        boolean hasTransparentBounds() {return matcher.hasTransparentBounds();}
        Matcher useTransparentBounds(boolean b) {return matcher.useTransparentBounds(b);}
        boolean hasAnchoringBounds() {return matcher.hasAnchoringBounds();}
        Matcher useAnchoringBounds(boolean b) {return matcher.useAnchoringBounds(b);}
        boolean hitEnd() {return matcher.hitEnd();}
        boolean requireEnd() {return matcher.requireEnd();}
        // @formatter:on
    }

    @Override
    @Nullable
    final protected MyMatcher prepareMatcher(@NotNull LineSelectionManager adjuster, @NotNull Caret caret, @NotNull Range range, @NotNull BasedSequence chars) {
        Pattern pattern = getPattern(adjuster, caret, range, chars);
        MyMatcher myMatcher = null;
        
        if (pattern != null) {

            if (!myBackwards) {
                Matcher matcher = pattern.matcher(chars);
                myMatcher = new MyMatcher(matcher, IndexMapper.NULL);
            } else {
                ReversedCharSequence reversed = new ReversedCharSequence(chars);
                Matcher matcher = pattern.matcher(reversed);
                myMatcher = new MyMatcher(matcher, reversed.getIndexMapper());
            }
            // we extend the range to include the start of line at start offset and end of line+1 at end offset, however we start searching 
            // at range start and any match that ends >= range end is treated as a non-match
            LogPos.Factory f = LogPos.factory(adjuster.getEditor());
            int start = f.fromOffset(range.getStart()).atStartOfLine().toOffset();
            int end = f.fromOffset(range.getEnd()).atEndOfLine().toOffset();

            myMatcher.region(start, end);
            myMatcher.useTransparentBounds(!mySingleLine);

            if (!myMatcher.find()) {
                myMatcher = null;
            }
        }
        return myMatcher;
    }

    @Override
    @Nullable
    final protected CaretMatch nextMatch(MyMatcher matcher, BasedSequence chars, @NotNull Range range, @Nullable CaretMatch previousMatch) {
        CaretMatch match = null;
        boolean found;

        found = previousMatch == null || matcher.find();
        if (found) {
            assert range.contains(matcher.start(), matcher.end() + 1);

            int offset = matcher.start();
            match = new CaretMatch(offset, matcher.end() + 1 - offset, offset, offset);
        }
        return match;
    }
}
