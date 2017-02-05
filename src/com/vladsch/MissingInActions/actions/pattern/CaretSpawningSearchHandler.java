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

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.Editor;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.MissingInActions.manager.EditorPosition;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.ReverseRegEx.util.ForwardPattern;
import com.vladsch.ReverseRegEx.util.RegExMatcher;
import com.vladsch.ReverseRegEx.util.RegExPattern;
import com.vladsch.ReverseRegEx.util.ReversePattern;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

import static java.lang.Character.isJavaIdentifierPart;

public class CaretSpawningSearchHandler extends RegExCaretSearchHandler {
    private boolean myLineMode;
    private boolean mySingleLine;
    private boolean mySingleMatch;
    private boolean myMoveFirstMatch;
    private boolean myCaseSensitive;
    private RegExPattern myPattern;
    private Set<Caret> myStartSearchCarets;
    private List<CaretState> myStartCarets;
    private Caret myPatternCaret;
    private boolean myCaretToEndGroup;

    public CaretSpawningSearchHandler(final boolean backwards) {
        super(backwards);
        myCaseSensitive = true;
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
    public boolean isCaseSensitive() {
        return myCaseSensitive;
    }

    public void setCaseSensitive(final boolean caseSensitive) {
        myCaseSensitive = caseSensitive;
    }

    @Override
    protected boolean isSingleMatch() {
        return mySingleMatch;
    }

    @Override
    protected boolean isMoveFirstMatch() {
        return myMoveFirstMatch;
    }

    public boolean isCaretToEndGroup() {
        return myCaretToEndGroup;
    }

    public void setCaretToEndGroup(final boolean caretToEndGroup) {
        myCaretToEndGroup = caretToEndGroup;
    }

    @Override
    protected void updateCarets(final Editor editor, final List<Caret> caretList) {
        LineSelectionManager manager = LineSelectionManager.getInstance(editor);
        if (mySingleMatch) {
            List<Caret> startMatchedCarets = new ArrayList<>();
            for (Caret caret : caretList) {
                for (Caret startCaret : myStartSearchCarets) {
                    if (startCaret.getLogicalPosition().line == caret.getLogicalPosition().line) {
                        startMatchedCarets.add(startCaret);
                        break;
                    }
                }
            }

            manager.setSearchFoundCaretSpawningHandler(this, myStartCarets, myStartSearchCarets, startMatchedCarets, caretList);
        } else {
            // just regular carets
            manager.clearSearchFoundCarets();
        }
    }

    @Override
    public void caretsChanged(final Editor editor) {
        myStartCarets = editor.getCaretModel().getCaretsAndSelections();
        myStartSearchCarets = new HashSet<>(editor.getCaretModel().getAllCarets());
        myPatternCaret = editor.getCaretModel().getPrimaryCaret();
    }

    public void copySettings(final CaretSpawningSearchHandler other, final Editor editor) {
        myLineMode = other.myLineMode;
        mySingleLine = other.mySingleLine;
        mySingleMatch = other.mySingleMatch;
        myMoveFirstMatch = other.myMoveFirstMatch;
        myCaretToEndGroup = other.myCaretToEndGroup;
        myPattern = null;
        myStartSearchCarets = null;
        myPatternCaret = null;

        if (mySingleMatch) {
            // this is search forward/backward
            caretsChanged(editor);
        }
    }


    @Override
    protected void analyzeContext(final Editor editor, @Nullable final Caret caret, @NotNull final LineSelectionManager manager) {
        int previousCaretLine = -1;
        boolean haveMultipleCaretsPerLine = false;
        boolean haveMultiLineSelection = false;
        int caretCount = editor.getCaretModel().getCaretCount();
        boolean haveMultipleCarets = caretCount > 1;

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
        mySingleMatch = haveMultipleCarets;
        myMoveFirstMatch = !mySingleMatch;
        myPattern = null;
        myStartSearchCarets = null;
        myPatternCaret = null;
        myCaretToEndGroup = false;

        if (mySingleMatch) {
            // this is search forward/backward
            caretsChanged(editor);
        }
    }

    @Override
    protected Caret getPatternCaret() {
        return myPatternCaret;
    }

    @Override
    protected String getPattern() {
        return myPattern == null ? "" : myPattern instanceof ReversePattern ? ((ReversePattern) myPattern).originalPattern() : myPattern.pattern();
    }

    @Override
    protected void setPattern(String pattern) {
        if (myBackwards) {
            myPattern = ReversePattern.compile(pattern, myCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
        } else {
            myPattern = ForwardPattern.compile(pattern, myCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
        }
    }

    @Override
    protected void preparePattern(@NotNull final LineSelectionManager manager, @NotNull final Caret caret, @NotNull final Range range, @NotNull final BasedSequence chars) {
        assert caret == myPatternCaret;
        myPattern = null;
        getPattern(manager, caret, range, chars);
    }

    @Override
    protected CaretMatch getCaretMatch(final RegExMatcher matcher, int selStart, int selEnd) {
        int caretOffset = myCaretToEndGroup ? (myBackwards ? selStart : selEnd) : (myBackwards ? matcher.end() : matcher.start());
        if (isSingleMatch()) {
            return new CaretMatch(caretOffset, matcher.end() - matcher.start(), caretOffset, caretOffset);
        } else {
            return new CaretMatch(caretOffset, matcher.end() - matcher.start(), selStart, selEnd);
        }
    }

    @Nullable
    @Override
    protected RegExPattern getPattern(@NotNull final LineSelectionManager manager, @NotNull Caret caret, @NotNull final Range range, @NotNull final BasedSequence chars) {
        if (!mySingleMatch || myPattern == null) {
            EditorPosition caretPos = manager.getPositionFactory().fromPosition(caret.getLogicalPosition());
            int offset = caretPos.getOffset();
            int endOfLineColumn = caretPos.atEndColumn().column;

            myPattern = null;

            if (!myBackwards) {
                // check what is ahead of caret
                char c = offset >= chars.length() || caretPos.column >= endOfLineColumn ? ' ' : chars.charAt(offset);
                if (Character.isWhitespace(c)) {
                    // match next non-whitespace
                    //myPattern = ForwardPattern.compile("(\\s*)\\S+");
                    myPattern = ForwardPattern.compile("((?<=\\s)\\s*|^|(?<=\\S)\\S*\\s+)\\S+");
                    myCaretToEndGroup = true;
                } else if (isJavaIdentifierPart(c)) {
                    // find end of identifier
                    int end = offset;
                    while (end < range.getEnd() && isJavaIdentifierPart(chars.charAt(end))) end++;

                    if (offset == 0 || !isJavaIdentifierPart(chars.charAt(offset - 1))) {
                        // we are at start of identifier even if first char is not a valid java identifier
                        myPattern = ForwardPattern.compile("\\b(" + Pattern.quote(chars.subSequence(offset, end).toString()) + ")\\b", myCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
                    } else {
                        myPattern = ForwardPattern.compile("(" + Pattern.quote(chars.subSequence(offset, end).toString()) + ")\\b", myCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
                    }
                } else {
                    // neither, just look for the character
                    String quote = Pattern.quote(String.valueOf(c));
                    myPattern = ForwardPattern.compile(quote + "\\s*([^" + quote + "]*)\\s*");
                }
            } else {
                // check what is behind of caret
                char c = offset == 0 || caretPos.column - 1 >= endOfLineColumn ? ' ' : chars.charAt(offset - 1);
                if (Character.isWhitespace(c)) {
                    // match previous non-whitespace
                    //myPattern = ReversePattern.compile("\\S+(\\s+)");
                    myPattern = ReversePattern.compile("\\S+(\\s*(?=\\s)|$|\\s+(?=\\S+))");
                    myCaretToEndGroup = true;
                } else if (isJavaIdentifierPart(c)) {
                    // find start of identifier
                    int start = offset;
                    while (start > range.getStart() && isJavaIdentifierPart(chars.charAt(start - 1))) start--;

                    if (offset >= chars.length() || !isJavaIdentifierPart(chars.charAt(offset))) {
                        // we are at start of identifier even if first char is not a valid java identifier
                        myPattern = ReversePattern.compile("\\b(" + Pattern.quote(chars.subSequence(start, offset).toString()) + ")\\b", myCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
                    } else {
                        myPattern = ReversePattern.compile("\\b(" + Pattern.quote(chars.subSequence(start, offset).toString()) + ")", myCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
                    }
                } else {
                    // neither, just look for the character
                    String quote = Pattern.quote(String.valueOf(c));
                    myPattern = ReversePattern.compile("\\s*([^" + quote + "]*)\\s*" + quote);
                }
            }
        }

        return myPattern;
    }
}
