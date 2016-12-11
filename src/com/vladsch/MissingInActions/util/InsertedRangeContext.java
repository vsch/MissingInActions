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

import com.intellij.openapi.util.TextRange;
import com.vladsch.MissingInActions.settings.RemovePrefixOnPasteType;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vladsch.MissingInActions.settings.RemovePrefixOnPasteType.REGEX;
import static com.vladsch.MissingInActions.util.EditHelpers.*;
import static com.vladsch.MissingInActions.util.StudiedWord.UPPER;
import static java.lang.Character.isAlphabetic;
import static java.lang.Character.isLowerCase;
import static java.lang.Character.isUpperCase;

@SuppressWarnings({ "WeakerAccess", "UnusedReturnValue", "SameParameterValue" })
public class InsertedRangeContext {
    // final
    public final BasedSequence charSequence;
    public final int expandedBeforeOffset;
    public final int beforeOffset;
    public final int afterOffset;
    public final int expandedAfterOffset;
    public final int textLength;
    public final String inserted;
    public final String expandedPrefix;
    public final String expandedSuffix;

    public final char charBefore;
    public final char charAfter;
    public final String sBefore;
    public final String sAfter;
    public final boolean isWordStartAtStart;
    public final boolean isWordEndAtEnd;

    private String myWord;
    private int myCaretDelta;
    private StudiedWord myStudiedWord;
    private boolean myPrefixRemoved;

    public InsertedRangeContext(@NotNull BasedSequence charSequence, int beforeOffset, int afterOffset) {
        this.charSequence = charSequence;
        this.textLength = charSequence.length();

        if (beforeOffset < 0) beforeOffset = 0;
        if (afterOffset > textLength) afterOffset = textLength;
        if (beforeOffset > afterOffset) beforeOffset = afterOffset;
        if (afterOffset < beforeOffset) afterOffset = beforeOffset;

        this.beforeOffset = beforeOffset;
        this.afterOffset = afterOffset;
        TextRange range = EditHelpers.getWordRangeAtOffsets(charSequence, beforeOffset, afterOffset, WORD_IDENTIFIER, false, true);
        this.expandedBeforeOffset = range.getStartOffset();
        this.expandedAfterOffset = range.getEndOffset();

        this.expandedPrefix = expandedBeforeOffset > beforeOffset ? "" : charSequence.subSequence(expandedBeforeOffset, beforeOffset).toString();
        this.expandedSuffix = expandedAfterOffset < afterOffset ? "" : charSequence.subSequence(afterOffset, expandedAfterOffset).toString();

        this.inserted = charSequence.subSequence(beforeOffset, afterOffset).toString();

        this.charBefore = beforeOffset > 0 && beforeOffset - 1 < textLength ? charSequence.charAt(beforeOffset - 1) : ' ';
        this.charAfter = afterOffset < textLength ? charSequence.charAt(afterOffset) : ' ';
        this.sBefore = String.valueOf(charBefore);
        this.sAfter = String.valueOf(charAfter);
        this.isWordStartAtStart = isWordStart(charSequence, beforeOffset, false);
        this.isWordEndAtEnd = isWordEnd(charSequence, afterOffset, false);

        myWord = inserted;
        myStudiedWord = StudiedWord.of(myWord);
        myCaretDelta = 0;
        myPrefixRemoved = false;
    }

    public int getCaretDelta() {
        return myCaretDelta;
    }

    public void setCaretDelta(final int caretDelta) {
        myCaretDelta = caretDelta;
    }

    public int getCumulativeCaretDelta() {
        return (inserted.length() - word().length() - myCaretDelta);
    }

    public boolean isPrefixRemoved() { return myPrefixRemoved; }

    public void setPrefixRemoved(final boolean prefixRemoved) { myPrefixRemoved = prefixRemoved; }

    // adjust with change to word
    public char charAtStart() { return !myWord.isEmpty() ? myWord.charAt(0) : charAfter; }

    public char charAtEnd() { return !myWord.isEmpty() ? myWord.charAt(myWord.length() - 1) : charAfter; }

    @NotNull
    public String sAtStart() { return String.valueOf(charAtStart()); }

    @NotNull
    public String sAtEnd() { return String.valueOf(charAtEnd()); }

    @NotNull
    public String word() { return myWord; }

    @NotNull
    public StudiedWord studiedWord() {
        if (myWord.equals(myStudiedWord.getWord())) return myStudiedWord;
        myStudiedWord = StudiedWord.of(myWord);
        return myStudiedWord;
    }

    @NotNull
    public String range(int start, int end) { return myWord.substring(start, end); }

    @NotNull
    String prefix(int count) {
        if (count < 0) count = 0;
        else if (count > myWord.length()) count = myWord.length();
        return myWord.substring(0, count);
    }

    @NotNull
    String suffix(int count) {
        if (count < 0) count = 0;
        else if (count > myWord.length()) count = myWord.length();
        return myWord.substring(myWord.length() - count, myWord.length());
    }

    // operations on word
    @NotNull
    public InsertedRangeContext setWord(final String word) {
        this.myWord = word;
        return this;
    }

    @NotNull
    public InsertedRangeContext deletePrefix(final int count) {
        if (count > 0) {
            if (count >= myWord.length()) myWord = "";
            else myWord = myWord.substring(count);
        }
        return this;
    }

    @NotNull
    public InsertedRangeContext deleteSuffix(final int count) {
        if (count > 0) {
            if (count >= myWord.length()) myWord = "";
            else myWord = myWord.substring(0, myWord.length() - count);
        }
        return this;
    }

    public boolean removePrefix(@NotNull String text) {
        if (!text.isEmpty() && myWord.startsWith(text) && myWord.length() > text.length()) {
            myWord = myWord.substring(text.length());
            return true;
        }
        return false;
    }

    public boolean removePrefixOnce(@NotNull String text) {
        if (!myPrefixRemoved) {
            myPrefixRemoved = removePrefix(text);
            return myPrefixRemoved;
        }
        return false;
    }

    public boolean removePrefixesOnce(@Nullable RemovePrefixOnPasteType prefixType, @NotNull String prefix1, @NotNull String prefix2) {
        if (!myPrefixRemoved) {
            RemovePrefixOnPasteType type = prefixType == null ? RemovePrefixOnPasteType.CAMEL : prefixType;
            String matched = type.getMatched(myWord, prefix1, prefix2);
            if (!matched.isEmpty()) {
                return removePrefixOnce(matched);
            }
        }
        return false;
    }

    public String getMatchedPrefix(final RemovePrefixOnPasteType prefixType, final String prefix1, final String prefix2) {
        RemovePrefixOnPasteType type = prefixType == null ? RemovePrefixOnPasteType.CAMEL : prefixType;
        if (type == REGEX) {
            // the first is the match, the second is the replacement to add
            return type.getMatched(myWord, prefix1, prefix2);
        } else {
            if (type.isMatched(myWord, prefix1)) {
                return prefix1;
            } else if (type.isMatched(myWord, prefix2)) {
                return prefix2;
            }
        }
        return "";
    }

    public boolean addPrefixOrReplaceMismatchedPrefix(final RemovePrefixOnPasteType prefixType, final String prefix, final String prefix1, final String prefix2) {
        if (!myPrefixRemoved && !prefix.isEmpty()) {
            RemovePrefixOnPasteType type = prefixType == null ? RemovePrefixOnPasteType.CAMEL : prefixType;
            String matched = type.getMatched(myWord, prefix1, prefix2);
            if (matched.isEmpty()) {
                // no prefix, we add
                prefixWithCamelCase(prefix);
                return true;
            } else if (!matched.equals(prefix)) {
                // we remove then add
                removePrefix(matched);
                prefixWithCamelCase(prefix);
                return true;
            }
        }
        return false;
    }

    public boolean removeSuffix(@NotNull String text) {
        if (!text.isEmpty() && myWord.endsWith(text)) {
            myWord = myWord.substring(0, myWord.length() - text.length());
            return true;
        }
        return false;
    }

    @NotNull
    public InsertedRangeContext replace(int start, int end, @NotNull Function<String, String> op) {
        myWord = myWord.substring(0, start) + op.apply(myWord.substring(start, end).toUpperCase()) + myWord.substring(end);
        return this;
    }

    @NotNull
    public InsertedRangeContext replace(int start, int end, @NotNull String text) {
        myWord = myWord.substring(0, start) + text + myWord.substring(end);
        return this;
    }

    @NotNull
    public InsertedRangeContext suffixWith(@NotNull String text) {
        myWord += text;
        return this;
    }

    @NotNull
    public InsertedRangeContext prefixWith(@NotNull String text) {
        myWord = text + myWord;
        return this;
    }

    @NotNull
    public InsertedRangeContext replacePrefix(int count, @NotNull String text) {
        if (count < 0) count = 0;
        else if (count > myWord.length()) count = myWord.length();
        return replace(0, count, text);
    }

    @NotNull
    public InsertedRangeContext replaceSuffix(int count, @NotNull String text) {
        if (count < 0) count = 0;
        else if (count > myWord.length()) count = myWord.length();
        return replace(myWord.length() - count, myWord.length(), text);
    }

    @NotNull
    public InsertedRangeContext replacePrefix(int count, @NotNull Function<String, String> op) {
        if (count < 0) count = 0;
        else if (count > myWord.length()) count = myWord.length();
        return replace(0, count, op);
    }

    @NotNull
    public InsertedRangeContext replaceSuffix(int count, @NotNull Function<String, String> op) {
        if (count < 0) count = 0;
        else if (count > myWord.length()) count = myWord.length();
        return replace(myWord.length() - count, myWord.length(), op);
    }

    @NotNull
    public InsertedRangeContext delete(int start, int end) { return replace(start, end, ""); }

    @NotNull
    public InsertedRangeContext prefixToUpperCase(int count) { return replacePrefix(count, String::toUpperCase); }

    @NotNull
    public InsertedRangeContext toUpperCase(int start, int end) { return replace(start, end, String::toUpperCase); }

    @NotNull
    public InsertedRangeContext suffixToUpperCase(int count) { return replaceSuffix(count, String::toUpperCase); }

    @NotNull
    public InsertedRangeContext prefixToLowerCase(int count) { return replacePrefix(count, String::toLowerCase); }

    @NotNull
    public InsertedRangeContext toLowerCase(int start, int end) { return replace(start, end, String::toLowerCase); }

    @NotNull
    public InsertedRangeContext suffixToLowerCase(int count) { return replaceSuffix(count, String::toLowerCase); }

    @NotNull
    public InsertedRangeContext suffixWithFromAfter(int count, @Nullable Function<String, String> op) {
        int offset = afterOffset + count;
        if (offset < afterOffset) offset = afterOffset;
        if (offset > textLength) offset = textLength;
        if (op != null) {
            return suffixWith(op.apply(charSequence.subSequence(afterOffset, offset).toString()));
        } else {
            return suffixWith(charSequence.subSequence(afterOffset, offset).toString());
        }
    }

    @NotNull
    public InsertedRangeContext prefixWithFromBefore(int count, @Nullable Function<String, String> op) {
        int offset = beforeOffset - count;
        if (offset < 0) offset = 0;
        if (offset > beforeOffset) offset = beforeOffset;
        if (op != null) {
            return prefixWith(op.apply(charSequence.subSequence(offset, beforeOffset).toString()));
        } else {
            return prefixWith(charSequence.subSequence(offset, beforeOffset).toString());
        }
    }

    @NotNull
    public InsertedRangeContext fixSnakeCase() {
        if (charBefore == '_' && charAtStart() == '_') deletePrefix(1);
        else if (!isWordStartAtStart && charBefore != '_' && charAtStart() != '_') prefixWith("_");
        if (charAfter == '_' && charAtEnd() == '_') deleteSuffix(1);
        else if (!isWordEndAtEnd && charAfter != '_' && charAtEnd() != '_') suffixWith("_");
        return this;
    }

    public boolean isHumpBoundIdentifierAtStart() { return isHumpBoundIdentifierAtStart(null); }

    public boolean isHumpBoundIdentifierAtEnd() { return isHumpBoundIdentifierAtEnd(null); }

    public boolean isHumpBoundIdentifierAtStart(@Nullable Function<String, String> op) {
        return EditHelpers.isHumpBoundIdentifier(sBefore + (op == null ? sAtStart() : op.apply(sAtStart())), 1, true);
    }

    public boolean isHumpBoundIdentifierAtEnd(@Nullable Function<String, String> op) {
        return EditHelpers.isHumpBoundIdentifier(sAtEnd() + (op == null ? sAfter : op.apply(sAfter)), 1, false);
    }

    public boolean hasNoLowerCaseAfterPrefix(final int count) {
        return myWord.length() > count && studiedWord().hasNoLowerCase();
    }

    public boolean hasNoUpperCaseAfterPrefix(final int count) {
        return myWord.length() > count && studiedWord().hasNoUpperCase();
    }

    public boolean hasNoLowerCase() { return studiedWord().hasNoLowerCase(); }

    public boolean hasNoUpperCase() { return studiedWord().hasNoUpperCase(); }

    public boolean isEqualsInserted() { return myWord.equals(inserted); }

    // @formatter:off
    public boolean isIdentifierBefore() { return beforeOffset > 0 && isIdentifier(charSequence, beforeOffset-1); }
    public boolean isIdentifierAfter() { return afterOffset < textLength && isIdentifier(charSequence, afterOffset); }
    public boolean isIdentifierAtStart() { return !isEmpty() && isIdentifier(myWord, 0); }
    public boolean isIdentifierAtEnd() { return !isEmpty() && isIdentifier(myWord, myWord.length()-1); }

    public boolean isIdentifierStartBefore(final boolean isCamel) { return isIdentifierStart(charSequence, beforeOffset, isCamel); }
    public boolean isIdentifierEndBefore(final boolean isCamel) { return isIdentifierEnd(charSequence, beforeOffset, isCamel); }
    public boolean isIdentifierStartAfter(final boolean isCamel) { return isIdentifierStart(charSequence, afterOffset, isCamel); }
    public boolean isIdentifierEndAfter(final boolean isCamel) { return isIdentifierEnd(charSequence, afterOffset, isCamel); }
    public boolean isIdentifierBoundBefore(final boolean isCamel) { return isHumpBoundIdentifier(charSequence, beforeOffset, isCamel); }
    public boolean isIdentifierBoundAfter(final boolean isCamel) { return isHumpBoundIdentifier(charSequence, afterOffset, isCamel); }

    public boolean isLowerCaseBefore() { return isLowerCase(charBefore); }
    public boolean isLowerCaseAtStart() { return isLowerCase(charAtStart()); }
    public boolean isLowerCaseAtEnd() { return isLowerCase(charAtEnd()); }
    public boolean isLowerCaseAfter() { return isLowerCase(charAfter); }

    public boolean isUpperCaseBefore() { return isUpperCase(charBefore); }
    public boolean isUpperCaseAtStart() { return isUpperCase(charAtStart()); }
    public boolean isUpperCaseAtEnd() { return isUpperCase(charAtEnd()); }
    public boolean isUpperCaseAfter() { return isUpperCase(charAfter); }

    public boolean isAlphabeticBefore() { return isAlphabetic(charBefore); }
    public boolean isAlphabeticAtStart() { return isAlphabetic(charAtStart()); }
    public boolean isAlphabeticAtEnd() { return isAlphabetic(charAtEnd()); }
    public boolean isAlphabeticAfter() { return isAlphabetic(charAfter); }

    public boolean isCamelCase() { return studiedWord().isCamelCase(); }
    public boolean isPascalCase() { return studiedWord().isPascalCase(); }
    public boolean isSnakeCase() { return studiedWord().isSnakeCase(); }
    public boolean isScreamingSnakeCase() { return studiedWord().isScreamingSnakeCase(); }

    public boolean isIsolated() { return expandedPrefix.isEmpty() && expandedSuffix.isEmpty(); }

    public boolean isExpandedCamelCase() { return StudiedWord.of(expandedPrefix + myWord + expandedSuffix).isCamelCase(); }
    public boolean isExpandedPascalCase() { return StudiedWord.of(expandedPrefix + myWord + expandedSuffix).isPascalCase(); }
    public boolean isExpandedSnakeCase() { return StudiedWord.of(expandedPrefix + myWord + expandedSuffix).isSnakeCase(); }
    public boolean isExpandedScreamingSnakeCase() { return StudiedWord.of(expandedPrefix + myWord + expandedSuffix).isScreamingSnakeCase(); }

    public boolean canBeCamelCase() { return studiedWord().canBeCamelCase(); }
    public boolean canBePascalCase() { return studiedWord().canBePascalCase(); }
    public boolean canBeSnakeCase() { return studiedWord().canBeSnakeCase(); }
    public boolean canBeScreamingSnakeCase() { return studiedWord().canBeScreamingSnakeCase(); }

    public boolean canBeExpandedCamelCase() { return StudiedWord.of(expandedPrefix + myWord + expandedSuffix).canBeCamelCase(); }

    @NotNull public InsertedRangeContext makeCamelCase() { myWord = studiedWord().makeCamelCase(); return this; }
    @NotNull public InsertedRangeContext makePascalCase() { myWord = studiedWord().makePascalCase(); return this; }
    @NotNull public InsertedRangeContext makeSnakeCase() { myWord = studiedWord().makeSnakeCase(); fixSnakeCase(); return this; }
    @NotNull public InsertedRangeContext makeScreamingSnakeCase() { myWord = studiedWord().makeScreamingSnakeCase(); fixSnakeCase(); return this; }

    public boolean hasUnderscore() { return myWord.indexOf('_') != -1; }
    public boolean hasUpperCase() { return studiedWord().hasUpperCase(); }
    public boolean hasLowerCase() { return studiedWord().hasLowerCase(); }

    public boolean isEmpty() { return myWord.isEmpty(); }
    public boolean isNotEmpty() { return !myWord.isEmpty(); }

    public boolean has(final int flags) { return myStudiedWord.has(flags); }
    public boolean not(final int flags) { return myStudiedWord.not(flags); }
    public boolean only(final int flags) { return myStudiedWord.only(flags); }
    public boolean just(final int flags) { return myStudiedWord.just(flags); }
    public boolean all(final int flags) { return myStudiedWord.all(flags); }
    public boolean first(final int flags) { return myStudiedWord.first(flags); }
    public boolean second(final int flags) { return myStudiedWord.second(flags); }
    public boolean last(final int flags) { return myStudiedWord.last(flags); }

    public InsertedRangeContext prefixWithCamelCase(final String prefix) {
        if (!studiedWord().first(UPPER)) prefixToUpperCase(1);
        prefixWith(prefix);
        return this;
    }

    // @formatter:on
}
