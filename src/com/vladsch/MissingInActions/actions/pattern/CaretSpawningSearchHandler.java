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

            if (!myBackwards) {
                // check what is ahead of caret
                boolean afterEnd = offset >= chars.length() || caretPos.column >= endOfLineColumn;
                //char prevC = offset == 0 || offset-1 >= chars.length() || caretPos.column == 0 || caretPos.column-1 >= endOfLineColumn ? ' ' : chars.charAt(offset);
                char c = afterEnd ? ' ' : chars.charAt(offset);
                //BasedSequence afterCaret = chars.subSequence(afterEnd ? endOfLineColumn : offset, endOfLineColumn);

                if (Character.isWhitespace(c)) {
                    // match next non-whitespace
                    //myPattern = ForwardPattern.compile("(\\s*)\\S+");
                    myPattern = ForwardPattern.compile("((?<=\\s)\\s*|^|(?<=\\S)\\S*\\s+)\\S+");
                    myCaretToEndGroup = true;
                } else if (EditHelpers.isIdentifierPart(c)) {
                    // find end of identifier
                    int end = offset;
                    while (end < range.getEnd() && EditHelpers.isIdentifierPart(chars.charAt(end)))
                        end++;
                    BasedSequence text = chars.subSequence(offset, end);
                    boolean hexPrefix = spawnNumericHexSearch && text.startsWith("0x", true);
                    String endBreak = text.charAt(text.length() - 1) == '$' ? "(?!\\Q$\\E|\\w)" : "\\b";
                    String startBreak = text.charAt(0) == '$' ? "(?<!\\Q$\\E|\\w)" : "\\b";
                    if (spawnNumericSearch && text.indexOfAny("0123456789") != -1
                            && ((spawnNumericHexSearch
                            && (hexPrefix && text.indexOfAnyNot("01234567890ABCDEFabcdef", 2) == -1)
                            || text.indexOfAnyNot("01234567890ABCDEFabcdef") == -1)
                            || (text.startsWith("0") && text.indexOfAnyNot("01234567") == -1)
                            || (text.startsWith("-") && text.indexOfAnyNot("0123456789", 1) == -1)
                            || (text.indexOfAnyNot("0123456789") == -1)
                    )) {
                        // hex, octal or decimal, look for numeric sequence
                        if (offset == 0 || !EditHelpers.isIdentifierPart(chars.charAt(offset - 1))) {
                            if (hexPrefix) {
                                myPattern = ForwardPattern.compile("\\b(0[xX][0-9a-fA-F]+|0[0-7]*|-?[0-9]+)\\b");
                            } else if (spawnNumericHexSearch) {
                                myPattern = ForwardPattern.compile("\\b([0-9a-fA-F]+|0[0-7]*|-?[0-9]+)\\b");
                            } else {
                                myPattern = ForwardPattern.compile("\\b([0-9]+|0[0-7]*|-?[0-9]+)\\b");
                            }
                        } else {
                            if (hexPrefix) {
                                myPattern = ForwardPattern.compile("(0[xX][0-9a-fA-F]+|0[0-7]*|-?[0-9]+)\\b");
                            } else if (spawnNumericHexSearch) {
                                myPattern = ForwardPattern.compile("([0-9a-fA-F]+|0[0-7]*|-?[0-9]+)\\b");
                            } else {
                                myPattern = ForwardPattern.compile("([0-9]+|0[0-7]*|-?[0-9]+)\\b");
                            }
                        }
                    } else {
                        String quotedText;
                        String textToSearch = text.toString();
                        boolean isStart = offset == 0 || !EditHelpers.isIdentifierPart(chars.charAt(offset - 1));
                        Pattern prefixPattern = spawnSmartPrefixSearch ? ApplicationSettings.getInstance().getPrefixesOnPastePattern() : null;

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

                        if (isStart) {
                            // we are at start of identifier even if first char is not a valid java identifier
                            // check if it is a numeric sequence base 10 or hex starting with 0x
                            myPattern = ForwardPattern.compile(startBreak + "(" + quotedText + ")" + endBreak, myCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
                        } else {
                            myPattern = ForwardPattern.compile("(" + quotedText + ")" + endBreak, myCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
                        }
                    }
                } else {
                    if (mySingleMatch) {
                        // now just look for a continuous span of characters
                        String quote = Pattern.quote(String.valueOf(c));
                        myPattern = ForwardPattern.compile("(" + quote + "+)");
                    } else {
                        // neither, just look for the character span of matching characters
                        int end = offset;
                        while (end < range.getEnd() && c == chars.charAt(end)) end++;
                        BasedSequence text = chars.subSequence(offset, end);
                        String quote = Pattern.quote(text.toString());
                        myPattern = ForwardPattern.compile("(?<!" + Pattern.quote(String.valueOf(c)) + ")(" + quote + ")(?!" + Pattern.quote(String.valueOf(c)) + ")");
                    }
                }
            } else {
                // check what is behind of caret
                char c = offset == 0 || caretPos.column - 1 >= endOfLineColumn ? ' ' : chars.charAt(offset - 1);
                if (Character.isWhitespace(c)) {
                    // match previous non-whitespace
                    //myPattern = ReversePattern.compile("\\S+(\\s+)");
                    myPattern = ReversePattern.compile("\\S+(\\s*(?=\\s)|$|\\s+(?=\\S+))");
                    myCaretToEndGroup = true;
                } else if (EditHelpers.isIdentifierPart(c)) {
                    // find start of identifier
                    int start = offset;
                    while (start > range.getStart() && EditHelpers.isIdentifierPart(chars.charAt(start - 1)))
                        start--;

                    BasedSequence text = chars.subSequence(start, offset);
                    boolean hexPrefix = text.startsWith("0x", true);

                    String endBreak = text.charAt(text.length() - 1) == '$' ? "(?!\\Q$\\E|\\w)" : "\\b";
                    String startBreak = text.charAt(0) == '$' ? "(?<!\\Q$\\E|\\w)" : "\\b";

                    if (spawnNumericSearch && text.indexOfAny("0123456789") != -1
                            && ((spawnNumericHexSearch
                            && (hexPrefix && text.indexOfAnyNot("01234567890ABCDEFabcdef", 2) == -1)
                            || text.indexOfAnyNot("01234567890ABCDEFabcdef") == -1)
                            || (text.startsWith("0") && text.indexOfAnyNot("01234567") == -1)
                            || (text.startsWith("-") && text.indexOfAnyNot("0123456789", 1) == -1)
                            || (text.indexOfAnyNot("0123456789") == -1)
                    )) {
                        // hex, octal or decimal, look for numeric sequence
                        if (offset >= chars.length() || !EditHelpers.isIdentifierPart(chars.charAt(offset))) {
                            if (hexPrefix) {
                                myPattern = ReversePattern.compile("\\b(0[xX][0-9a-fA-F]+|0[0-7]*|-?[0-9]+)\\b");
                            } else {
                                myPattern = ReversePattern.compile("\\b([0-9a-fA-F]+|0[0-7]*|-?[0-9]+)\\b");
                            }
                        } else {
                            if (hexPrefix) {
                                myPattern = ReversePattern.compile("\\b(0[xX][0-9a-fA-F]+|0[0-7]*|-?[0-9]+)");
                            } else {
                                myPattern = ReversePattern.compile("\\b([0-9a-fA-F]+|0[0-7]*|-?[0-9]+)");
                            }
                        }
                    } else {
                        String quotedText;
                        boolean isStart = offset >= chars.length() || !EditHelpers.isIdentifierPart(chars.charAt(offset));
                        String textToSearch = chars.subSequence(start, offset).toString();
                        Pattern prefixPattern = spawnSmartPrefixSearch ? ApplicationSettings.getInstance().getPrefixesOnPastePattern() : null;

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

                        if (isStart) {
                            // we are at start of identifier even if first char is not a valid java identifier
                            myPattern = ReversePattern.compile(startBreak + "(" + quotedText + ")" + endBreak, myCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
                        } else {
                            myPattern = ReversePattern.compile(startBreak + "(" + quotedText + ")", myCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
                        }
                    }
                } else {
                    if (mySingleMatch) {
                        // now just look for a continuous span of characters
                        String quote = Pattern.quote(String.valueOf(c));
                        myPattern = ReversePattern.compile("(" + quote + "+)");
                    } else {
                        // neither, just look for the character span of matching characters
                        int start = offset;
                        while (start > range.getStart() && c == chars.charAt(start - 1))
                            start--;
                        BasedSequence text = chars.subSequence(start, offset);
                        String quote = Pattern.quote(text.toString());
                        myPattern = ReversePattern.compile("(?<!" + Pattern.quote(String.valueOf(c)) + ")(" + quote + ")(?!" + Pattern.quote(String.valueOf(c)) + ")");
                    }
                }
            }
        }

        return myPattern;
    }
}
