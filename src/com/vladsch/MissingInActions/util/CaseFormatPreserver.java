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

package com.vladsch.MissingInActions.util;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.util.TextRange;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.vladsch.MissingInActions.util.EditHelpers.getNextWordEndAtOffset;
import static com.vladsch.MissingInActions.util.EditHelpers.getPreviousWordStartAtOffset;
import static com.vladsch.MissingInActions.util.WordStudy.*;

@SuppressWarnings("WeakerAccess")
public class CaseFormatPreserver {
    private boolean startToLowerCase;
    private boolean startToUpperCase;
    private boolean startOnBound;
    private boolean endOnBound;
    private boolean toScreamingSnakeCase;
    private boolean toSnakeCase;
    private boolean toCamelCase;
    private boolean hadStartOfWord;
    private boolean hadStartOfWordWithPrefix;
    private boolean hadSelection;

    public CaseFormatPreserver() {
        clear();
    }

    public void clear() {
        startToLowerCase = false;
        startToUpperCase = false;
        startOnBound = false;
        endOnBound = false;
        toScreamingSnakeCase = false;
        toSnakeCase = false;
        toCamelCase = false;
        hadStartOfWord = false;
        hadStartOfWordWithPrefix = false;
        hadSelection = false;
    }

    public void studyFormatBefore(@NotNull EditorCaret editorCaret, final @Nullable String removePrefix1, final @Nullable String removePrefix2) {
        final Caret caret = editorCaret.getCaret();
        studyFormatBefore(editorCaret.getDocumentChars(), caret.getOffset(), caret.getSelectionStart(), caret.getSelectionEnd(), removePrefix1, removePrefix2);
    }

    public void studyFormatBefore(
            final BasedSequence chars
            , final int offset
            , final int selectionStart
            , final int selectionEnd
            , final @Nullable String removePrefix1
            , final @Nullable String removePrefix2
    ) {
        String prefix1 = removePrefix1 == null ? "" : removePrefix1;
        String prefix2 = removePrefix2 == null ? "" : removePrefix2;
        int beforeOffset = offset;
        int afterOffset = beforeOffset;

        clear();

        if (selectionStart != selectionEnd && !((selectionStart == 0 || chars.charAt(selectionStart-1)=='\n') && chars.charAt(selectionEnd-1) == '\n')) {
            beforeOffset = selectionStart;
            afterOffset = selectionEnd;
            hadSelection = true;
        }

        int expandedBeforeOffset = getPreviousWordStartAtOffset(chars, beforeOffset, EditHelpers.WORD_IDENTIFIER, false, true);
        int expandedAfterOffset = getNextWordEndAtOffset(chars, afterOffset, EditHelpers.WORD_IDENTIFIER, false, true);

        InsertedRangeContext e = new InsertedRangeContext(chars, expandedBeforeOffset, expandedAfterOffset);
        InsertedRangeContext w = new InsertedRangeContext(chars, beforeOffset, afterOffset);

        if (!w.isEmpty()) {
            if (!prefix1.isEmpty() || !prefix2.isEmpty()) {
                hadStartOfWord = w.isIdentifierStartBefore(false);
                if ((!prefix1.isEmpty() && w.word().startsWith(prefix1) || !prefix2.isEmpty() && w.word().startsWith(prefix2))) {
                    hadStartOfWordWithPrefix = true;
                }
            }
        }

        toScreamingSnakeCase = e.isScreamingSnakeCase() || e.hasUnderscore() && e.hasNoLowerCase() && e.hasUpperCase();
        if (!toScreamingSnakeCase) {
            toSnakeCase = e.isSnakeCase();
            if (!toSnakeCase) {
                if (e.isCamelCase() || e.hasNoUpperCase()) {
                    toCamelCase = true;
                }

                // still do the bounds if not fully camel case
                // non-alpha|non-alpha : start=do nothing, end=do nothing
                // alpha|non-alpha: start=make bound, end=do nothing
                // non-alpha|alpha: start=do nothing, end=make bound
                // alpha|alpha: start=make bound, end=make bound
                boolean identifierStart = w.isIdentifierStartBefore(true);
                boolean identifierEnd = w.isIdentifierEndBefore(true);
                startToLowerCase = identifierStart && w.isLowerCaseAtStart() && w.wordStudy().only(LOWER|UPPER|UNDER|EMPTY);
                startToUpperCase = identifierStart && w.isUpperCaseAtStart() && w.wordStudy().only(LOWER|UPPER|UNDER|EMPTY);
                startOnBound = identifierStart || identifierEnd;
                endOnBound = w.isIdentifierStartAfter(true) || w.isIdentifierEndAfter(true);
            }
        }
    }

    public int preserveFormatAfter(final @NotNull EditorCaret editorCaret
            , final @NotNull TextRange range
            , final boolean startWriteAction
            , final boolean selectRange
            , final boolean preserveCamelCase
            , final boolean preserveSnakeCase
            , final boolean preserveScreamingSnakeCase
            , final @Nullable String removePrefix1
            , final @Nullable String removePrefix2
    ) {
        InsertedRangeContext i = preserveFormatAfter(
                editorCaret.getDocumentChars()
                , range
                , preserveCamelCase, preserveSnakeCase, preserveScreamingSnakeCase
                , removePrefix1, removePrefix2
        );

        if (i != null) {
            if (!i.isEqualsInserted()) {
                final int finalBeforeOffset = i.beforeOffset;
                final int finalAfterOffset = i.afterOffset - i.getCaretDelta();
                final String finalWord = i.word();
                final int finalCaretDelta = i.getCaretDelta();
                if (startWriteAction) {
                    WriteCommandAction.runWriteCommandAction(editorCaret.getProject(), () -> {
                        editorCaret.getDocument().replaceString(finalBeforeOffset, finalAfterOffset, finalWord);
                        editorCaret.getCaret().moveToOffset(finalBeforeOffset + finalWord.length() + finalCaretDelta);
                    });
                } else {
                    editorCaret.getDocument().replaceString(finalBeforeOffset, finalAfterOffset, finalWord);
                    editorCaret.getCaret().moveToOffset(finalBeforeOffset + finalWord.length() + finalCaretDelta);
                }
            }

            if (selectRange) {
                editorCaret.getCaret().setSelection(i.beforeOffset, i.beforeOffset + i.word().length() + i.getCaretDelta());
            }
            return i.getCumulativeCaretDelta();
        }
        return 0;
    }

    public InsertedRangeContext preserveFormatAfter(final BasedSequence chars
            , final @NotNull TextRange range
            , final boolean preserveCamelCase
            , final boolean preserveSnakeCase
            , final boolean preserveScreamingSnakeCase
            , final @Nullable String removePrefix1
            , final @Nullable String removePrefix2
    ) {
        String prefix1 = removePrefix1 == null ? "" : removePrefix1;
        String prefix2 = removePrefix2 == null ? "" : removePrefix2;
        BasedSequence insertedRange = (BasedSequence) range.subSequence(chars);
        boolean isSingleLineChar = insertedRange.indexOf('\n') == -1;
        InsertedRangeContext i = null;

        if (isSingleLineChar) {
            // remove prefixes first so camel case adjustment works right
            i = new InsertedRangeContext(chars, range.getStartOffset(), range.getEndOffset());
            int caretDelta = 0;
            boolean removedPrefix = false;

            if (!i.isIsolated() || hadSelection) {
                if (!(prefix1.isEmpty() && prefix2.isEmpty())) {
                    if (!(hadStartOfWord && hadStartOfWordWithPrefix) && !i.isIsolated() && i.wordStudy().only(UPPER|LOWER|DIGITS)) {
                        if (!i.word().equals(prefix1) && i.removePrefix(prefix1)) removedPrefix = true;
                        else if (!i.word().equals(prefix2) && i.removePrefix(prefix2)) removedPrefix = true;
                    }
                }

                // adjust for camel case preservation
                if (preserveCamelCase || preserveSnakeCase || preserveScreamingSnakeCase) {
                    if (preserveScreamingSnakeCase && toScreamingSnakeCase && i.wordStudy().only(UPPER|LOWER|UNDER|DIGITS)) {
                        // remove prefix if converting to snake case
                        if (!removedPrefix && !i.word().equals(prefix1) && i.removePrefix(prefix1)) removedPrefix = true;
                        else if (!removedPrefix && !i.word().equals(prefix2) && i.removePrefix(prefix2)) removedPrefix = true;
                        i.makeScreamingSnakeCase();
                    } else if (preserveSnakeCase && toSnakeCase && i.wordStudy().only(UPPER|LOWER|UNDER|DIGITS)) {
                        // remove prefix if converting to snake case
                        if (!removedPrefix && !i.word().equals(prefix1) && i.removePrefix(prefix1)) removedPrefix = true;
                        else if (!removedPrefix && !i.word().equals(prefix2) && i.removePrefix(prefix2)) removedPrefix = true;
                        i.makeSnakeCase();
                    } else if (preserveCamelCase) {
                        if (toCamelCase && i.wordStudy().only(UPPER|LOWER|UNDER|DIGITS)) {
                            i.makeCamelCase();
                        }

                        if (startToUpperCase && i.isLowerCaseAtStart()) {
                            i.prefixToUpperCase(1);
                        } else if (startToLowerCase && i.isUpperCaseAtStart() && !i.hasNoLowerCaseAfterPrefix(1)) {
                            i.prefixToLowerCase(1);
                        } else if (startOnBound && i.isAlphabeticAtStart()) {
                            // try to change case so that it is hump bound
                            if (i.isLowerCaseAtStart() && i.isHumpBoundIdentifierAtStart(String::toUpperCase) && !i.isHumpBoundIdentifierAtStart()) {
                                i.prefixToUpperCase(1);
                            } else if (i.isUpperCaseAtStart() && i.isHumpBoundIdentifierAtStart(String::toLowerCase) && !i.isHumpBoundIdentifierAtStart()) {
                                i.prefixToLowerCase(1);
                            }
                        }

                        if ((endOnBound || i.isExpandedCamelCase()) && i.isAlphabeticAfter()) {
                            // try to change case so that it is a hump bound
                            if (i.isLowerCaseAfter() && i.isHumpBoundIdentifierAtEnd(String::toUpperCase) && !i.isHumpBoundIdentifierAtEnd()) {
                                // make upper case after insertion point
                                i.suffixWithFromAfter(1, String::toUpperCase);
                                caretDelta--;
                            }
                        }
                    }
                }
            }

            i.setCaretDelta(caretDelta);
        }

        return i;
    }
}
