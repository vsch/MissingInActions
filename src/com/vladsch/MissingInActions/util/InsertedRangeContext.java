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
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

import static com.vladsch.MissingInActions.util.EditHelpers.*;
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

    public InsertedRangeContext(@NotNull BasedSequence charSequence, int beforeOffset, int afterOffset) {
        this.charSequence = charSequence;
        this.textLength = charSequence.length();

        if (beforeOffset < 0) beforeOffset = 0;
        if (afterOffset > textLength) afterOffset = textLength;
        if (beforeOffset > afterOffset) beforeOffset = afterOffset;
        if (afterOffset < beforeOffset) afterOffset = beforeOffset;

        this.beforeOffset = beforeOffset;
        this.afterOffset = afterOffset;
        TextRange range = EditHelpers.getWordRangeAtOffsets(charSequence, beforeOffset, afterOffset, WORD_IDENTIFIER,false,true);
        this.expandedBeforeOffset = range.getStartOffset();
        this.expandedAfterOffset =  range.getEndOffset();

        this.expandedPrefix = expandedBeforeOffset > beforeOffset ? "":charSequence.subSequence(expandedBeforeOffset, beforeOffset).toString();
        this.expandedSuffix = expandedAfterOffset < afterOffset ? "":charSequence.subSequence(afterOffset, expandedAfterOffset).toString();

        this.inserted = charSequence.subSequence(beforeOffset, afterOffset).toString();

        this.charBefore = beforeOffset > 0 && beforeOffset - 1 < textLength ? charSequence.charAt(beforeOffset - 1) : ' ';
        this.charAfter = afterOffset < textLength ? charSequence.charAt(afterOffset) : ' ';
        this.sBefore = String.valueOf(charBefore);
        this.sAfter = String.valueOf(charAfter);
        this.isWordStartAtStart = isWordStart(charSequence, beforeOffset, false);
        this.isWordEndAtEnd = isWordEnd(charSequence, afterOffset, false);

        myWord = inserted;
    }

    // adjust with change to word
    public char charAtStart() { return !myWord.isEmpty() ? myWord.charAt(0) : ' '; }

    public char charAtEnd() { return !myWord.isEmpty() ? myWord.charAt(myWord.length() - 1) : ' '; }

    @NotNull
    public String sAtStart() { return String.valueOf(!myWord.isEmpty() ? myWord.charAt(0) : ' '); }

    @NotNull
    public String sAtEnd() { return String.valueOf(!myWord.isEmpty() ? myWord.charAt(myWord.length() - 1) : ' '); }

    @NotNull
    public String word() { return myWord; }

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
        if (!text.isEmpty() && myWord.startsWith(text)) {
            myWord = myWord.substring(text.length());
            return true;
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
    public InsertedRangeContext makeScreamingSnakeCase() {
        myWord = EditHelpers.makeScreamingSnakeCase(myWord);
        return fixSnakeCase();
    }

    @NotNull
    public InsertedRangeContext makeSnakeCase() {
        myWord = EditHelpers.makeSnakeCase(myWord);
        return fixSnakeCase();
    }

    @NotNull
    public InsertedRangeContext fixSnakeCase() {
        if (charBefore == '_' && charAtStart() == '_') deletePrefix(1);
        else if (!isWordStartAtStart && charBefore != '_' && charAtStart() != '_') prefixWith("_");
        if (charAfter == '_' && charAtEnd() == '_') deleteSuffix(1);
        else if (!isWordEndAtEnd && charAfter != '_' && charAtEnd() != '_') suffixWith("_");
        return this;
    }

    @NotNull
    public InsertedRangeContext makeCamelCase() {
        myWord = EditHelpers.makeCamelCase(myWord);
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
        return myWord.length() > count && EditHelpers.hasNoLowerCase(myWord);
    }

    public boolean hasNoUpperCaseAfterPrefix(final int count) {
        return myWord.length() > count && EditHelpers.hasNoUpperCase(myWord);
    }

    public boolean hasNoLowerCase() { return EditHelpers.hasNoLowerCase(myWord); }

    public boolean hasNoUpperCase() { return EditHelpers.hasNoUpperCase(myWord); }

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

    public boolean isCamelCase() { return EditHelpers.isCamelCase(myWord); }
    public boolean isScreamingSnakeCase() { return EditHelpers.isScreamingSnakeCase(myWord); }
    public boolean isSnakeCase() { return EditHelpers.isSnakeCase(myWord); }

    public boolean isIsolated() { return expandedPrefix.isEmpty() && expandedSuffix.isEmpty(); }
    public boolean isExpandedCamelCase() { return EditHelpers.isCamelCase(expandedPrefix + myWord + expandedSuffix); }
    public boolean isExpandedScreamingSnakeCase() { return EditHelpers.isScreamingSnakeCase(expandedPrefix + myWord + expandedSuffix); }
    public boolean isExpandedSnakeCase() { return EditHelpers.isSnakeCase(expandedPrefix + myWord + expandedSuffix); }

    public boolean canMakeCamelCase() { return EditHelpers.canMakeCamelCase(myWord); }
    public boolean canMakeScreamingSnakeCase() { return EditHelpers.canMakeScreamingSnakeCase(myWord); }
    public boolean canMakeSnakeCase() { return EditHelpers.canMakeSnakeCase(myWord); }

    public boolean canMakeExpandedCamelCase() { return EditHelpers.canMakeCamelCase(expandedPrefix + myWord + expandedSuffix); }
    public boolean canMakeExpandedScreamingSnakeCase() { return EditHelpers.canMakeScreamingSnakeCase(expandedPrefix + myWord + expandedSuffix); }
    public boolean canMakeExpandedSnakeCase() { return EditHelpers.canMakeSnakeCase(expandedPrefix + myWord + expandedSuffix); }

    public boolean isEmpty() { return myWord.isEmpty(); }
    public boolean isNotEmpty() { return !myWord.isEmpty(); }

    // @formatter:off
}
