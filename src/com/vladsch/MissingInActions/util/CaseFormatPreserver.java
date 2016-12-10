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
import com.vladsch.MissingInActions.settings.RemovePrefixOnPasteType;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.vladsch.MissingInActions.util.EditHelpers.getNextWordEndAtOffset;
import static com.vladsch.MissingInActions.util.EditHelpers.getPreviousWordStartAtOffset;
import static com.vladsch.MissingInActions.util.StudiedWord.*;

@SuppressWarnings("WeakerAccess")
public class CaseFormatPreserver {
    private boolean startWasLowerCase;
    private boolean startWasUpperCase;
    private boolean startWasOnBound;
    private boolean endWasOnBound;
    private boolean toScreamingSnakeCase;
    private boolean toSnakeCase;
    private boolean toPascalCase;
    private boolean toCamelCase;
    private boolean hadStartOfWord;
    private boolean hadStartOfWordWithPrefix1;
    private boolean hadStartOfWordWithPrefix2;
    private boolean hadSelection;

    public CaseFormatPreserver() {
        clear();
    }

    public void clear() {
        startWasLowerCase = false;
        startWasUpperCase = false;
        startWasOnBound = false;
        endWasOnBound = false;
        toScreamingSnakeCase = false;
        toSnakeCase = false;
        toPascalCase = false;
        toCamelCase = false;
        hadStartOfWord = false;
        hadStartOfWordWithPrefix1 = false;
        hadStartOfWordWithPrefix2 = false;
        hadSelection = false;
    }

    public void studyFormatBefore(@NotNull EditorCaret editorCaret
            , final @Nullable String removePrefix1, final @Nullable String removePrefix2
            , final @Nullable RemovePrefixOnPasteType prefixOnPasteType
    ) {
        final Caret caret = editorCaret.getCaret();
        studyFormatBefore(editorCaret.getDocumentChars(), caret.getOffset(), caret.getSelectionStart(), caret.getSelectionEnd(), removePrefix1, removePrefix2, prefixOnPasteType);
    }

    public void studyFormatBefore(
            final BasedSequence chars
            , final int offset
            , final int selectionStart
            , final int selectionEnd
            , final @Nullable String removePrefix1
            , final @Nullable String removePrefix2
            , final @Nullable RemovePrefixOnPasteType prefixOnPasteType
    ) {
        String prefix1 = removePrefix1 == null ? "" : removePrefix1;
        String prefix2 = removePrefix2 == null ? "" : removePrefix2;
        int beforeOffset = offset;
        int afterOffset = beforeOffset;
        RemovePrefixOnPasteType prefixType = prefixOnPasteType == null ? RemovePrefixOnPasteType.DEFAULT : prefixOnPasteType;

        clear();

        if (selectionStart != selectionEnd && !((selectionStart == 0 || chars.charAt(selectionStart - 1) == '\n') && chars.charAt(selectionEnd - 1) == '\n')) {
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
                if (hadStartOfWord) {
                    if (prefixType.isMatched(w.word(), prefix1)) hadStartOfWordWithPrefix1 = true;
                    else if (prefixType.isMatched(w.word(), prefix2)) hadStartOfWordWithPrefix2 = true;
                }
            }
        }

        toScreamingSnakeCase = e.isScreamingSnakeCase() || e.hasUnderscore() && e.hasNoLowerCase() && e.hasUpperCase();
        if (!toScreamingSnakeCase) {
            toSnakeCase = e.isSnakeCase();
            if (!toSnakeCase) {
                if (e.isPascalCase()) {
                    toPascalCase = true;
                } else if (e.isCamelCase() || e.just(LOWER|DIGITS) && e.first(LOWER)) {
                    toCamelCase = true;
                }

                // still do the bounds if not fully camel case
                // non-alpha|non-alpha : start=do nothing, end=do nothing
                // alpha|non-alpha: start=make bound, end=do nothing
                // non-alpha|alpha: start=do nothing, end=make bound
                // alpha|alpha: start=make bound, end=make bound
                boolean identifierStart = w.isIdentifierStartBefore(true);
                boolean identifierEnd = w.isIdentifierEndBefore(true);
                startWasLowerCase = identifierStart && w.isLowerCaseAtStart() && w.studiedWord().only(LOWER | UPPER | UNDER | EMPTY);
                startWasUpperCase = identifierStart && w.isUpperCaseAtStart() && w.studiedWord().only(LOWER | UPPER | UNDER | EMPTY);
                startWasOnBound = identifierStart || identifierEnd;
                endWasOnBound = w.isIdentifierStartAfter(true) || w.isIdentifierEndAfter(true);
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
            , final @Nullable RemovePrefixOnPasteType prefixOnPasteType
            , final boolean addPrefix
    ) {
        InsertedRangeContext i = preserveFormatAfter(
                editorCaret.getDocumentChars()
                , range
                , preserveCamelCase, preserveSnakeCase, preserveScreamingSnakeCase
                , removePrefix1, removePrefix2
                , prefixOnPasteType, addPrefix
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
            , final @Nullable RemovePrefixOnPasteType prefixOnPasteType
            , final boolean addPrefix
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

            if (!i.isIsolated() || hadSelection) {
                RemovePrefixOnPasteType prefixType = prefixOnPasteType == null ? RemovePrefixOnPasteType.DEFAULT : prefixOnPasteType;

                if (!(prefix1.isEmpty() && prefix2.isEmpty())) {
                    if (hadStartOfWord && hadStartOfWordWithPrefix1 && prefixType.isMatched(i.word(), prefix1)) {

                    } else if (hadStartOfWord && hadStartOfWordWithPrefix2 && prefixType.isMatched(i.word(), prefix2)) {

                    } else {
                        if (i.studiedWord().only(UPPER | LOWER | DIGITS)) {
                            // TODO: change prefix type to enumeration
                            i.removeCamelCasePrefixOnce(prefix1);
                            i.removeCamelCasePrefixOnce(prefix2);
                        }
                    }
                }

                // adjust for camel case preservation
                if (i.studiedWord().only(UPPER | LOWER | UNDER | DIGITS) && (preserveCamelCase || preserveSnakeCase || preserveScreamingSnakeCase)) {
                    if (preserveScreamingSnakeCase && toScreamingSnakeCase) {
                        // remove prefix if converting to snake case
                        i.removeCamelCasePrefixOnce(prefix1);
                        i.removeCamelCasePrefixOnce(prefix2);
                        i.makeScreamingSnakeCase();
                    } else if (preserveSnakeCase && toSnakeCase ) {
                        // remove prefix if converting to snake case
                        i.removeCamelCasePrefixOnce(prefix1);
                        i.removeCamelCasePrefixOnce(prefix2);
                        i.makeSnakeCase();
                    } else if (preserveCamelCase) {
                        if (toPascalCase) {
                            i.makePascalCase();
                        } else if (toCamelCase) {
                            i.makeCamelCase();
                        }

                        if (addPrefix && hadStartOfWord && hadStartOfWordWithPrefix1 && !prefixType.isMatched(i.word(), prefix1)) {
                            i.prefixWithCamelCase(prefix1);
                        } else if (addPrefix && hadStartOfWord && hadStartOfWordWithPrefix2 && !prefixType.isMatched(i.word(), prefix2)) {
                            i.prefixWithCamelCase(prefix2);
                        } else if (startWasUpperCase && i.isLowerCaseAtStart()) {
                            i.removeCamelCasePrefixOnce(prefix1);
                            i.removeCamelCasePrefixOnce(prefix2);
                            i.prefixToUpperCase(1);
                        } else {
                            if (startWasLowerCase && i.isUpperCaseAtStart() && !i.hasNoLowerCaseAfterPrefix(1)) {
                                i.prefixToLowerCase(1);
                            } else if (startWasOnBound && i.isAlphabeticAtStart()) {
                                // try to change case so that it is hump bound
                                if (i.isLowerCaseAtStart() && i.isHumpBoundIdentifierAtStart(String::toUpperCase) && !i.isHumpBoundIdentifierAtStart()) {
                                    i.prefixToUpperCase(1);
                                } else if (i.isUpperCaseAtStart() && i.isHumpBoundIdentifierAtStart(String::toLowerCase) && !i.isHumpBoundIdentifierAtStart()) {
                                    i.prefixToLowerCase(1);
                                }
                            }
                        }

                        if ((endWasOnBound || i.isExpandedCamelCase()) && i.isAlphabeticAfter()) {
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
