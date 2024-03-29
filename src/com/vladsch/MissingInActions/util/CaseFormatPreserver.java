// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.util;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.util.TextRange;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.MissingInActions.settings.PrefixOnPastePatternType;
import com.vladsch.MissingInActions.settings.SuffixOnPastePatternType;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.plugin.util.StudiedWord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.vladsch.MissingInActions.util.EditHelpers.getNextWordEndAtOffset;
import static com.vladsch.MissingInActions.util.EditHelpers.getPreviousWordStartAtOffset;
import static com.vladsch.plugin.util.StudiedWord.DASH;
import static com.vladsch.plugin.util.StudiedWord.DIGITS;
import static com.vladsch.plugin.util.StudiedWord.DOT;
import static com.vladsch.plugin.util.StudiedWord.EMPTY;
import static com.vladsch.plugin.util.StudiedWord.LOWER;
import static com.vladsch.plugin.util.StudiedWord.SLASH;
import static com.vladsch.plugin.util.StudiedWord.UNDER;
import static com.vladsch.plugin.util.StudiedWord.UPPER;

@SuppressWarnings("WeakerAccess")
public class CaseFormatPreserver {
    private boolean startWasLowerCase;
    private boolean startWasUpperCase;
    private boolean startWasOnBound;
    private boolean endWasOnBound;
    private boolean toMixedCase;
    private boolean toFirstCapCase;
    private boolean toScreamingCase;
    private boolean toSnakeCase;
    private boolean toPascalCase;
    private boolean toCamelCase;
    private boolean toDashCase;
    private boolean toDotCase;
    private boolean toSlashCase;
    private boolean toUpperCase;
    private boolean hadStartOfWord;
    private String hadWordWithPrefix;
    private String hadWordWithSecondPrefix;
    private boolean hadSelection;
    private String hadWordWithSuffix;

    public CaseFormatPreserver() {
        clear();
    }

    public void clear() {
        startWasLowerCase = false;
        startWasUpperCase = false;
        startWasOnBound = false;
        endWasOnBound = false;

        toMixedCase = false;           // these check variants
        toFirstCapCase = false;        // these check variants
        toScreamingCase = false;       // these check variants

        toSnakeCase = false;           // these check case style
        toDashCase = false;            // these check case style
        toDotCase = false;             // these check case style
        toSlashCase = false;           // these check case style

        toPascalCase = false;
        toCamelCase = false;
        toUpperCase = false;
        hadStartOfWord = false;
        hadWordWithPrefix = "";
        hadWordWithSecondPrefix = "";
        hadSelection = false;
    }

    public void studyFormatBefore(
            @NotNull EditorCaret editorCaret
            , final @Nullable String[] prefixList
            , final @Nullable PrefixOnPastePatternType prefixType
            , final @Nullable String[] suffixList
            , final @Nullable SuffixOnPastePatternType suffixType
            , final int separators
    ) {
        final Caret caret = editorCaret.getCaret();
        studyFormatBefore(editorCaret.getDocumentChars(), caret.getOffset(), caret.getSelectionStart(), caret.getSelectionEnd(), prefixType, prefixList, suffixType, suffixList, separators);
    }

    private boolean isCaseAndStyle(boolean isAnyCase, boolean isScreamingCase, boolean isMixedCase, boolean isFirstCapCase) {
        if (isAnyCase) {
            if (isScreamingCase) {
                toScreamingCase = true;
            } else if (isFirstCapCase) {
                toFirstCapCase = true;
            } else if (isMixedCase) {
                toMixedCase = true;
            }
        }
        return isAnyCase;
    }

    public void studyFormatBefore(
            final BasedSequence chars
            , final int offset
            , final int selectionStart
            , final int selectionEnd
            , final @Nullable PrefixOnPastePatternType prefixType, final @Nullable String[] prefixList
            , final @Nullable SuffixOnPastePatternType suffixType, final @Nullable String[] suffixList
            , final int separators
    ) {
        int beforeOffset = offset;
        int afterOffset = beforeOffset;
        PrefixOnPastePatternType patternType = prefixType == null ? PrefixOnPastePatternType.ADAPTER.getDefault() : prefixType;

        clear();

        if (selectionStart != selectionEnd && !((selectionStart == 0 || chars.charAt(selectionStart - 1) == '\n') && chars.charAt(selectionEnd - 1) == '\n')) {
            beforeOffset = selectionStart;
            afterOffset = selectionEnd;
            hadSelection = true;
        }

        int expandedBeforeOffset = getPreviousWordStartAtOffset(chars, beforeOffset, EditHelpers.WORD_IDENTIFIER, false, true);
        int expandedAfterOffset = getNextWordEndAtOffset(chars, afterOffset, EditHelpers.WORD_IDENTIFIER, false, true);

        if (suffixList != null) {
            // exclude these suffixes from case determination
            InsertedRangeContext e = new InsertedRangeContext(chars, expandedBeforeOffset, expandedAfterOffset, separators);
            String suffix = e.getMatchedSuffix(suffixType, suffixList);
            if (suffix != null && !suffix.isEmpty()) {
                expandedAfterOffset -= suffix.length();
            }
        }

        InsertedRangeContext e = new InsertedRangeContext(chars, expandedBeforeOffset, expandedAfterOffset, separators);
        InsertedRangeContext w = new InsertedRangeContext(chars, beforeOffset, afterOffset, separators);

        toSnakeCase = isCaseAndStyle(e.isAnySnakeCase(), e.isScreamingSnakeCase(), e.isMixedSnakeCase(), e.isFirstCapSnakeCase());
        if (!toSnakeCase) {
            toDashCase = isCaseAndStyle(e.isAnyDashCase(), e.isScreamingDashCase(), e.isMixedDashCase(), e.isFirstCapDashCase());
            if (!toDashCase) {
                toDotCase = isCaseAndStyle(e.isAnyDotCase(), e.isScreamingDotCase(), e.isMixedDotCase(), e.isFirstCapDotCase());
                if (!toDotCase) {
                    toSlashCase = isCaseAndStyle(e.isAnySlashCase(), e.isScreamingSlashCase(), e.isMixedSlashCase(), e.isFirstCapSlashCase());
                    if (!toSlashCase) {
                        if (e.isPascalCase()) {
                            toPascalCase = true;
                        } else if (e.isCamelCase() || e.only(LOWER | DIGITS) && e.first(LOWER)) {
                            toCamelCase = true;
                        } else if (e.only(UPPER | DIGITS) && e.first(UPPER)) {
                            toUpperCase = true;
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
        }

        if (!w.isEmpty()) {
            hadStartOfWord = w.isIdentifierStartBefore(false);
            if (hadStartOfWord) {
                if (toSnakeCase || toDashCase || toDotCase || toSlashCase) {
                    w.makeCamelCase();
                } else if (w.isPascalCase()) {
                    toPascalCase = true;
                    toCamelCase = false;
                    w.makePascalCase();
                } else if (w.isCamelCase() || w.only(LOWER | DIGITS) && w.first(LOWER)) {
                    toCamelCase = true;
                }
            } else {
                if (w.isSnakeCase() || w.isDashCase() || w.isDotCase() || w.isSlashCase()) {
                    w.makeProperCamelCase();
                } else if (w.isPascalCase()) {
                    toPascalCase = true;
                    toCamelCase = false;
                    w.makeProperCamelCase();
                } else if (w.isCamelCase() || w.only(LOWER | DIGITS) && w.first(LOWER)) {
                    toCamelCase = true;
                } else if (w.only(UPPER | DIGITS) && w.first(UPPER)) {
                    toUpperCase = true;
                }
            }
            hadWordWithPrefix = w.getMatchedPrefix(patternType, prefixList);
            if (!hadWordWithPrefix.isEmpty()) {
                // see if has second prefix
                InsertedRangeContext s = new InsertedRangeContext(chars, beforeOffset + hadWordWithPrefix.length(), afterOffset, separators);
                hadWordWithSecondPrefix = s.getSecondMatchedPrefix(patternType, prefixList);
            }
        } else {
            hadStartOfWord = w.isWordStartAtStart;
            if (hadStartOfWord) {
                // TODO: add ability to test if following has prefix and delete the prefix after pasting
            }
        }
    }

    public int preserveFormatAfter(
            final @NotNull EditorCaret editorCaret
            , final @NotNull TextRange range
            , final boolean startWriteAction
            , final boolean selectRange
            , final boolean preserveCamelCase
            , final boolean preserveSnakeCase
            , final boolean preserveScreamingSnakeCase
            , final boolean preserveDashCase
            , final boolean preserveDotCase
            , final boolean preserveSlashCase
            , final boolean removePrefix
            , final boolean addPrefix
            , final @Nullable String[] prefixList
            , final @Nullable PrefixOnPastePatternType prefixType
            , final @Nullable String[] suffixList
            , final @Nullable SuffixOnPastePatternType suffixType
    ) {
        InsertedRangeContext i = preserveFormatAfter(
                editorCaret.getDocumentChars()
                , range
                , preserveCamelCase
                , preserveSnakeCase
                , preserveScreamingSnakeCase
                , preserveDashCase
                , preserveDotCase
                , preserveSlashCase
                , removePrefix
                , addPrefix
                , prefixType
                , prefixList
                , suffixType
                , suffixList
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

    public static int separators(
            final boolean preserveCamelCase
            , final boolean preserveSnakeCase
            , final boolean preserveScreamingSnakeCase
            , final boolean preserveDashCase
            , final boolean preserveDotCase
            , final boolean preserveSlashCase
    ) {
        int separators = 0;
        if (preserveCamelCase || preserveSnakeCase || preserveScreamingSnakeCase) separators |= StudiedWord.UNDER;
        if (preserveDashCase) separators |= StudiedWord.DASH;
        if (preserveDotCase) separators |= StudiedWord.DOT;
        if (preserveSlashCase) separators |= StudiedWord.SLASH;
        return separators;
    }

    public InsertedRangeContext preserveFormatAfter(
            final BasedSequence chars
            , final @NotNull TextRange range
            , final boolean preserveCamelCase
            , final boolean preserveSnakeCase
            , final boolean preserveScreamingSnakeCase
            , final boolean preserveDashCase
            , final boolean preserveDotCase
            , final boolean preserveSlashCase
            , final boolean removePrefix
            , final boolean addPrefix
            , final @Nullable PrefixOnPastePatternType prefixType
            , final @Nullable String[] prefixList
            , final @Nullable SuffixOnPastePatternType suffixType
            , final @Nullable String[] suffixList
    ) {
        BasedSequence insertedRange = (BasedSequence) range.subSequence(chars);
        boolean isSingleLineChar = insertedRange.indexOf('\n') == -1;
        InsertedRangeContext i = null;

        if (isSingleLineChar) {
            // remove prefixes first so camel case adjustment works right
            int separators = separators(
                    preserveCamelCase
                    , preserveSnakeCase
                    , preserveScreamingSnakeCase
                    , preserveDashCase
                    , preserveDotCase
                    , preserveSlashCase
            );

            i = new InsertedRangeContext(chars, range.getStartOffset(), range.getEndOffset(), separators);
            int caretDelta = 0;

            if (!i.isIsolated() || hadSelection) {
                PrefixOnPastePatternType patternType = prefixType == null ? PrefixOnPastePatternType.ADAPTER.getDefault() : prefixType;

                String matchedPrefix = i.getMatchedPrefix(patternType, prefixList);
                if (!matchedPrefix.isEmpty() && removePrefix) {
                    if (hadWordWithPrefix.equals(matchedPrefix) || hadWordWithSecondPrefix.equals(matchedPrefix)) {
                        // leave the prefix
                    } else {
                        // won't be replaced by add, we remove it here
                        // need to check if we are replacing snake case or screaming snake case with a snake cased/screaming snake cased prefix
                        // but need to remove non-letter prefixes if so requested
                        if (i.studiedWord().only(UPPER | LOWER | DIGITS) || !StudiedWord.of(matchedPrefix, separators).only(UPPER | LOWER | DIGITS | UNDER)) {
                            i.removePrefixesOnce(patternType, prefixList);

                            String secondPrefix = i.getSecondMatchedPrefix(patternType, prefixList);
                            if (hadWordWithPrefix.equals(secondPrefix) && !hadWordWithSecondPrefix.equals(secondPrefix)) {
                                // remove the second prefix, it doubles as first
                                i.setPrefixRemoved(false);
                                i.removePrefixesOnce(patternType, prefixList);
                            }
                        }
                    }
                }

                // adjust for camel case preservation
                if (i.studiedWord().only(UPPER | LOWER | UNDER | DIGITS) && (preserveCamelCase || preserveSnakeCase || preserveScreamingSnakeCase)
                        || i.studiedWord().only(DASH | LOWER | DIGITS) && preserveDashCase
                        || i.studiedWord().only(DOT | LOWER | DIGITS) && preserveDotCase
                        || i.studiedWord().only(SLASH | LOWER | DIGITS) && preserveSlashCase
                ) {
                    if (preserveScreamingSnakeCase && toSnakeCase && toScreamingCase) {
                        i.makeScreamingSnakeCase();
                    } else if (preserveSnakeCase && toSnakeCase && !toScreamingCase) {
                        if (toMixedCase) i.makeMixedSnakeCase();
                        else if (toFirstCapCase) i.makeFirstCapSnakeCase();
                        else if (toScreamingCase) i.makeScreamingSnakeCase();
                        else i.makeSnakeCase();
                    } else if (preserveDashCase && toDashCase) {
                        if (toMixedCase) i.makeMixedDashCase();
                        else if (toFirstCapCase) i.makeFirstCapDashCase();
                        else if (toScreamingCase) i.makeScreamingDashCase();
                        else i.makeDashCase();
                    } else if (preserveDotCase && toDotCase) {
                        if (toMixedCase) i.makeMixedDotCase();
                        else if (toFirstCapCase) i.makeFirstCapDotCase();
                        else if (toScreamingCase) i.makeScreamingDotCase();
                        else i.makeDotCase();
                    } else if (preserveSlashCase && toSlashCase) {
                        if (toMixedCase) i.makeMixedSlashCase();
                        else if (toFirstCapCase) i.makeFirstCapSlashCase();
                        else if (toScreamingCase) i.makeScreamingSlashCase();
                        else i.makeSlashCase();
                    } else if (preserveCamelCase) {
                        if (!(startWasOnBound && (i.studiedWord().first(UNDER) || i.studiedWord().last(UNDER)))) {
                            if (toPascalCase) {
                                i.makePascalCase();
                            } else if (toUpperCase) {
                                i.toUpperCase();
                            } else if (toCamelCase) {
                                i.makeCamelCase();
                            }
                        }

                        if (addPrefix && hadStartOfWord && !hadWordWithPrefix.isEmpty() && i.addPrefixOrReplaceMismatchedPrefix(patternType, hadWordWithPrefix, prefixList, hadWordWithSecondPrefix)) {
                            // prefix replaced
                        } else if (startWasUpperCase && i.isLowerCaseAtStart()) {
                            if (removePrefix) {
                                i.removePrefixesOnce(patternType, prefixList);
                                i.prefixToUpperCase(1);
                            }
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
