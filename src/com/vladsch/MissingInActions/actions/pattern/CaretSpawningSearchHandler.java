/*
 * Copyright (c) 2016-2017 Vladimir Schneider <vladimir.schneider@gmail.com>
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

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.MissingInActions.manager.EditorPosition;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.ReverseRegEx.util.*;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isJavaIdentifierStart;

public class CaretSpawningSearchHandler extends RegExCaretSearchHandler {
    private boolean myLineMode;
    private boolean mySingleLine;
    private boolean mySingleMatch;

    public CaretSpawningSearchHandler(final boolean backwards) {
        super(backwards);
    }

    @Override
    protected boolean isLineMode() {
        return myLineMode;
    }

    @Override
    protected boolean isSingleLine() {
        return mySingleLine;
    }

    @Override
    protected boolean isSingleMatch() {
        return mySingleMatch;
    }

    @Override
    protected void analyzeContext(final Editor editor, @Nullable final Caret caret, final DataContext dataContext, @NotNull final LineSelectionManager manager) {
        int previousCaretLine = -1;
        boolean haveMultipleCaretsPerLine = false;
        boolean haveMultiLineSelection = false;

        for (Caret caret1 : editor.getCaretModel().getAllCarets()) {
            int caretLine = caret1.getLogicalPosition().line;

            if (!haveMultiLineSelection && caret1.hasSelection()) {
                EditorCaret editorCaret = manager.getEditorCaret(caret1);

                if (editorCaret.hasLines()) {
                    haveMultiLineSelection = true;
                }
            }

            if (!haveMultipleCaretsPerLine && previousCaretLine != -1) {
                if (caretLine == previousCaretLine) {
                    haveMultipleCaretsPerLine = true;
                }
            }
            previousCaretLine = caretLine;

            if (haveMultiLineSelection && haveMultipleCaretsPerLine) break;
        }

        myLineMode = !haveMultipleCaretsPerLine;
        mySingleLine = !haveMultiLineSelection;
        mySingleMatch = false;
    }

    @Nullable
    @Override
    protected RegExPattern getPattern(@NotNull final LineSelectionManager manager, @NotNull final Caret caret, @NotNull final Range range, @NotNull final BasedSequence chars) {
        EditorPosition caretPos = manager.getPositionFactory().fromPosition(caret.getLogicalPosition());
        int offset = caretPos.getOffset();
        int endOfLineColumn = caretPos.atEndColumn().column;

        if (!myBackwards) {
            // check what is ahead of caret
            char c = offset >= chars.length() || caretPos.column >= endOfLineColumn ? ' ' : chars.charAt(offset);
            if (Character.isWhitespace(c)) {
                // match next non-whitespace
                return ForwardPattern.compile("(\\s+)\\S+");
            } else if (isJavaIdentifierPart(c)) {
                // find end of identifier
                int end = offset;
                while (end < range.getEnd() && isJavaIdentifierPart(chars.charAt(end))) end++;

                if (offset == 0 || !isJavaIdentifierPart(chars.charAt(offset - 1))) {
                    // we are at start of identifier even if first char is not a valid java identifier
                    return ForwardPattern.compile("\\b(" + Pattern.quote(chars.subSequence(offset, end).toString()) + ")\\b");
                } else {
                    return ForwardPattern.compile("(" + Pattern.quote(chars.subSequence(offset, end).toString()) + ")\\b");
                }
            } else {
                // neither, just look for the character
                String quote = Pattern.quote(String.valueOf(c));
                return ForwardPattern.compile(quote + "\\s*([^" + quote + "]*)\\s*");
            }
        } else {
            // check what is behind of caret
            char c = offset == 0 || caretPos.column - 1 >= endOfLineColumn ? ' ' : chars.charAt(offset - 1);
            if (Character.isWhitespace(c)) {
                // match previous non-whitespace
                return ReversePattern.compile("\\S+(\\s+)");
            } else if (isJavaIdentifierPart(c)) {
                // find start of identifier
                int start = offset;
                while (start > range.getStart() && isJavaIdentifierPart(chars.charAt(start - 1))) start--;

                if (offset >= chars.length() || !isJavaIdentifierPart(chars.charAt(offset))) {
                    // we are at start of identifier even if first char is not a valid java identifier
                    return ReversePattern.compile("\\b(" + Pattern.quote(chars.subSequence(start, offset).toString()) + ")\\b");
                } else {
                    return ReversePattern.compile("\\b(" + Pattern.quote(chars.subSequence(start, offset).toString()) + ")");
                }
            } else {
                // neither, just look for the character
                String quote = Pattern.quote(String.valueOf(c));
                return ReversePattern.compile("\\s*([^" + quote + "]*)\\s*" + quote);
            }
        }
    }
}
