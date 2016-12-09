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
import com.intellij.openapi.util.TextRange;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.vladsch.MissingInActions.util.EditHelpers.*;
import static java.lang.Character.isLowerCase;
import static java.lang.Character.isUpperCase;

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
        String prefix1 = removePrefix1 == null ? "" : removePrefix1;
        String prefix2 = removePrefix2 == null ? "" : removePrefix2;
        int beforeOffset = editorCaret.getCaretPosition().getOffset();
        int afterOffset = beforeOffset;
        BasedSequence charSequence = editorCaret.getDocumentChars();

        if (editorCaret.hasSelection() && !editorCaret.isLine()) {
            beforeOffset = editorCaret.getSelectionStart().getOffset();
            afterOffset = editorCaret.getSelectionEnd().getOffset();
            hadSelection = true;
        }

        int expandedBeforeOffset = getPreviousWordStartAtOffset(charSequence, beforeOffset, EditHelpers.WORD_IDENTIFIER, false, true);
        int expandedAfterOffset = getNextWordEndAtOffset(charSequence, afterOffset, EditHelpers.WORD_IDENTIFIER, false, true);

        InsertedRangeContext e = new InsertedRangeContext(charSequence, expandedBeforeOffset, expandedAfterOffset);
        InsertedRangeContext w = new InsertedRangeContext(charSequence, beforeOffset, afterOffset);

        if (!w.isEmpty()) {
            if (!prefix1.isEmpty() || !prefix2.isEmpty()) {
                hadStartOfWord = w.isIdentifierStartBefore(false);
                if ((!prefix1.isEmpty() && w.word().startsWith(prefix1) || !prefix2.isEmpty() && w.word().startsWith(prefix2))) {
                    hadStartOfWordWithPrefix = true;
                }
            }
        }

        toScreamingSnakeCase = e.isScreamingSnakeCase() || e.hasNoLowerCase();
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
                startToLowerCase = identifierStart && w.isLowerCaseAtStart();
                startToUpperCase = identifierStart && w.isUpperCaseAtStart();
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
        String prefix1 = removePrefix1 == null ? "" : removePrefix1;
        String prefix2 = removePrefix2 == null ? "" : removePrefix2;
        editorCaret.setSelection(range.getStartOffset(), range.getEndOffset());
        boolean isSingleLineChar = !editorCaret.hasLines();
        int cumulativeCaretDelta = 0;

        if (isSingleLineChar) {
            // remove prefixes first so camel case adjustment works right
            InsertedRangeContext i = new InsertedRangeContext(editorCaret.getDocumentChars(), range.getStartOffset(), range.getEndOffset());
            int caretDelta = 0;
            boolean removedPrefix = false;

            if (!i.isIsolated() || hadSelection) {
                if (!(prefix1.isEmpty() && prefix2.isEmpty())) {
                    if (!(hadStartOfWord && hadStartOfWordWithPrefix) && !i.isIsolated()) {
                        if (!i.word().equals(prefix1) && i.removePrefix(prefix1)) removedPrefix = true;
                        else if (!i.word().equals(prefix2) && i.removePrefix(prefix2)) removedPrefix = true;
                    }
                }

                // adjust for camel case preservation
                if (preserveCamelCase || preserveSnakeCase || preserveScreamingSnakeCase) {
                    if (preserveScreamingSnakeCase && toScreamingSnakeCase) {
                        i.makeScreamingSnakeCase();
                    } else if (preserveSnakeCase && toSnakeCase) {
                        i.makeSnakeCase();
                    } else if (preserveCamelCase) {
                        if (toCamelCase) {
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

            if (!i.isEqualsInserted()) {
                final int finalBeforeOffset = i.beforeOffset;
                final int finalAfterOffset = i.afterOffset - caretDelta;
                final String finalWord = i.word();
                final int finalCaretDelta = caretDelta;
                if (startWriteAction) {
                    WriteCommandAction.runWriteCommandAction(editorCaret.getProject(), () -> {
                        editorCaret.getDocument().replaceString(finalBeforeOffset, finalAfterOffset, finalWord);
                        editorCaret.getCaret().moveToOffset(finalBeforeOffset + finalWord.length() + finalCaretDelta);
                    });
                } else {
                    editorCaret.getDocument().replaceString(finalBeforeOffset, finalAfterOffset, finalWord);
                    editorCaret.getCaret().moveToOffset(finalBeforeOffset + finalWord.length() + finalCaretDelta);
                }

                // adjust for the rest of the carets
                cumulativeCaretDelta -= i.inserted.length() - i.word().length() - caretDelta;
            }

            if (selectRange) {
                editorCaret.getCaret().setSelection(i.beforeOffset, i.beforeOffset + i.word().length() + caretDelta);
            }
        }

        return cumulativeCaretDelta;
    }
}
