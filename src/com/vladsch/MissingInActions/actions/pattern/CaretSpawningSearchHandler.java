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
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.Editor;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.MissingInActions.manager.EditorPosition;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.ReverseRegEx.util.ForwardPattern;
import com.vladsch.ReverseRegEx.util.RegExMatcher;
import com.vladsch.ReverseRegEx.util.RegExPattern;
import com.vladsch.ReverseRegEx.util.ReversePattern;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    protected boolean wantEmptyRanges() {
        return mySingleMatch;
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
        if (mySingleMatch) {
            // search forwards/backwards
            myStartCarets = editor.getCaretModel().getCaretsAndSelections();
            myStartSearchCarets = new HashSet<>(editor.getCaretModel().getAllCarets());
        }
        myPatternCaret = editor.getCaretModel().getPrimaryCaret();
    }

    public void copySettings(final CaretSpawningSearchHandler other, final Editor editor) {
        myLineMode = other.myLineMode;
        mySingleLine = other.mySingleLine;
        mySingleMatch = other.mySingleMatch;
        myMoveFirstMatch = other.myMoveFirstMatch;
        myCaretToEndGroup = other.myCaretToEndGroup;
        myPattern = other.myPattern;
        myStartCarets = null;
        myStartSearchCarets = null;
        myPatternCaret = null;

        caretsChanged(editor);
    }

    @Override
    protected void analyzeContext(
            final Editor editor,
            @Nullable final Caret caret,
            @NotNull final LineSelectionManager manager
    ) {
        int previousCaretLine = -1;
        boolean haveMultipleCaretsPerLine = false;
        boolean haveMultiLineSelection = false;
        //int caretCount = editor.getCaretModel().getCaretCount();
        boolean haveMultipleCaretLines = false;
        boolean haveMultipleCarets = editor.getCaretModel().getCaretCount() > 1;

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
                } else {
                    haveMultipleCaretLines = true;
                }
            }
            previousCaretLine = caretLine;

            if (haveMultiLineSelection && haveMultipleCaretsPerLine && haveMultipleCaretLines)
                break;
        }

        mySingleLine = !haveMultiLineSelection;
        mySingleMatch = !haveMultipleCaretsPerLine && haveMultipleCarets;
        myLineMode = mySingleMatch;
        myMoveFirstMatch = !mySingleMatch;
        myPattern = null;
        myStartSearchCarets = null;
        myPatternCaret = null;
        myCaretToEndGroup = false;

        // get primary caret
        caretsChanged(editor);
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
    protected void preparePattern(
            @NotNull final LineSelectionManager manager,
            @NotNull final Caret caret,
            @NotNull final Range range,
            @NotNull final BasedSequence chars
    ) {
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
    protected RegExPattern getPattern(
            @NotNull final LineSelectionManager manager,
            @NotNull Caret caret,
            @NotNull final Range range,
            @NotNull final BasedSequence chars
    ) {
        if (myPattern == null) {
            EditorPosition caretPos = manager.getPositionFactory().fromPosition(caret.getLogicalPosition());
            int offset = caretPos.getOffset();
            int endOfLineColumn = caretPos.atEndColumn().column;
            boolean spawnNumericSearch = ApplicationSettings.getInstance().isSpawnNumericSearch();
            boolean spawnNumericHexSearch = ApplicationSettings.getInstance().isSpawnNumericHexSearch();
            boolean spawnSmartPrefixSearch = ApplicationSettings.getInstance().isSpawnSmartPrefixSearch();
            String patternText;
            int searchFlags = 0;

            // check what is ahead of caret
            char c = myBackwards ? offset == 0 || caretPos.column - 1 >= endOfLineColumn ? ' ' : chars.charAt(offset - 1)
                    : offset >= chars.length() || caretPos.column >= endOfLineColumn ? ' ' : chars.charAt(offset);

            // check what is behind of caret
            if (Character.isWhitespace(c)) {
                myCaretToEndGroup = true;
                // match previous non-whitespace
                // match next non-whitespace
                patternText = myBackwards ? "\\S+(\\s*(?=\\s)|$|\\s+(?=\\S+))" : "((?<=\\s)\\s*|^|(?<=\\S)\\S*\\s+)\\S+";
            } else if (EditHelpers.isIdentifierPart(c)) {
                // find start of identifier
                int start = offset;
                int end = offset;

                if (myBackwards) {
                    while (start > range.getStart() && EditHelpers.isIdentifierPart(chars.charAt(start - 1))) start--;
                } else {
                    while (end < range.getEnd() && EditHelpers.isIdentifierPart(chars.charAt(end))) end++;
                }

                BasedSequence text = chars.subSequence(start, end);
                boolean hexPrefix = spawnNumericHexSearch && isHexPrefix(text);
                String endBreak = getEndBreak(text);
                String startBreak = getStartBreak(text);
                boolean isNumericSpawnSearch = isNumericSearch(text, hexPrefix, spawnNumericSearch, spawnNumericHexSearch);

                boolean isStart = myBackwards ? offset >= chars.length() || !EditHelpers.isIdentifierPart(chars.charAt(offset))
                        : offset == 0 || !EditHelpers.isIdentifierPart(chars.charAt(offset - 1));

                if (isNumericSpawnSearch) {
                    // hex, octal or decimal, look for numeric sequence
                    patternText = getNumericPatternText(spawnNumericHexSearch, isStart, hexPrefix, startBreak);
                } else {
                    // if these are numbers make sure we match start/end word only
                    boolean isNumericSearch = isNumeric(text, hexPrefix);
                    String quotedText = getSmartPrefixedText(text.toString(), spawnSmartPrefixSearch);

                    String starPattern = myBackwards || isStart ? startBreak : (isNumericSearch ? "\\B" : "");
                    String endPattern = !myBackwards || isStart ? endBreak : (isNumericSearch ? "\\B" : "");

                    searchFlags = myCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                    patternText = starPattern + "(" + quotedText + ")" + endPattern;
                }
            } else {
                if (mySingleMatch) {
                    // now just look for a continuous span of characters
                    patternText = "(" + Pattern.quote(String.valueOf(c)) + "+)";
                } else {
                    // neither, just look for the character span of matching characters
                    int start = offset;
                    int end = offset;

                    if (myBackwards) {
                        while (start > range.getStart() && c == chars.charAt(start - 1)) start--;
                    } else {
                        while (end < range.getEnd() && c == chars.charAt(end)) end++;
                    }

                    String quote = Pattern.quote(chars.subSequence(start, end).toString());
                    patternText = "(?<!" + Pattern.quote(String.valueOf(c)) + ")(" + quote + ")(?!" + Pattern.quote(String.valueOf(c)) + ")";
                }
            }

            myPattern = myBackwards ? ReversePattern.compile(patternText, searchFlags)
                    : ForwardPattern.compile(patternText, searchFlags);
        }

        return myPattern;
    }

    @NotNull
    private static String getStartBreak(final BasedSequence text) {
        return text.charAt(0) == '$' ? "(?<!\\Q$\\E|\\w)" : "\\b";
    }

    @NotNull
    private static String getEndBreak(final BasedSequence text) {
        return text.charAt(text.length() - 1) == '$' ? "(?!\\Q$\\E|\\w)" : "\\b";
    }

    private static boolean isHexPrefix(final BasedSequence text) {
        return text.startsWith("0x", true);
    }

    @NotNull
    private static String getNumericPatternText(final boolean spawnNumericHexSearch, final boolean isStart, final boolean hexPrefix, final String startBreak) {
        String patternText;
        if (hexPrefix) {
            patternText = (isStart ? startBreak : "") + "(0[xX][0-9a-fA-F]+|0[0-7]*|-?[0-9]+)\\b";
        } else if (spawnNumericHexSearch) {
            patternText = (isStart ? startBreak : "") + "([0-9a-fA-F]+|0[0-7]*|-?[0-9]+)\\b";
        } else {
            patternText = (isStart ? startBreak : "") + "([0-9]+|0[0-7]*|-?[0-9]+)\\b";
        }
        return patternText;
    }

    private static boolean isNumericSearch(final BasedSequence text, final boolean hexPrefix, final boolean spawnNumericSearch, final boolean spawnNumericHexSearch) {
        return spawnNumericSearch && text.indexOfAny("0123456789") != -1
                && ((spawnNumericHexSearch
                && (hexPrefix && text.indexOfAnyNot("01234567890ABCDEFabcdef", 2) == -1)
                || text.indexOfAnyNot("01234567890ABCDEFabcdef") == -1)
                || (text.startsWith("0") && text.indexOfAnyNot("01234567") == -1)
                || (text.startsWith("-") && text.indexOfAnyNot("0123456789", 1) == -1)
                || (text.indexOfAnyNot("0123456789") == -1))
                ;
    }

    private static boolean isNumeric(final BasedSequence text, final boolean hexPrefix) {
        return text.indexOfAny("0123456789") != -1
                && (((hexPrefix && text.indexOfAnyNot("01234567890ABCDEFabcdef", 2) == -1)
                || text.indexOfAnyNot("01234567890ABCDEFabcdef") == -1)
                || (text.startsWith("0") && text.indexOfAnyNot("01234567") == -1)
                || (text.startsWith("-") && text.indexOfAnyNot("0123456789", 1) == -1)
                || (text.indexOfAnyNot("0123456789") == -1))
                ;
    }

    @NotNull
    private static String getSmartPrefixedText(@NotNull String textToSearch, final boolean spawnSmartPrefixSearch) {
        Pattern prefixPattern = spawnSmartPrefixSearch ? ApplicationSettings.getInstance().getPrefixesOnPastePattern() : null;
        String quotedText;

        if (prefixPattern != null) {
            // add prefixed variations of the text
            Matcher matcher = prefixPattern.matcher(textToSearch);
            if (matcher.find()) {
                textToSearch = textToSearch.substring(matcher.group().length());
                quotedText = "(?:" + prefixPattern.pattern().replace("(?=[A-Z])", "") + Pattern.quote(textToSearch.substring(0, 1).toUpperCase() + textToSearch.substring(1)) + ")|" +
                        "(?:" + Pattern.quote(textToSearch.substring(0, 1).toLowerCase() + textToSearch.substring(1)) + ")";
            } else {
                quotedText = Pattern.quote(textToSearch);
            }
        } else {
            quotedText = Pattern.quote(textToSearch);
        }
        return quotedText;
    }
}
