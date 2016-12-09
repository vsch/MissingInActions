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

import static java.lang.Character.*;
import static java.lang.Character.isLetterOrDigit;

@SuppressWarnings("WeakerAccess")
public class WordStudy {
    public static final int EMPTY = 0x0001;
    public static final int NUL = 0x0002;
    public static final int CTRL = 0x0004;
    public static final int SPACE = 0x0008;
    public static final int DIGITS = 0x0010;
    public static final int SYMBOLS = 0x0020;
    public static final int UPPER = 0x0040;
    public static final int LOWER = 0x0080;
    public static final int UNDER = 0x0100;
    public static final int DOT = 0x0200;
    public static final int DASH = 0x0400;
    public static final int SLASH = 0x0800;
    public static final int OTHER = 0x1000;
    public static final int NOT_DEFINED = 0x2000;

    public static final int LETTER = LOWER | UPPER;
    public static final int ALPHANUMERIC = LOWER | UPPER | DIGITS;

    private static int[] ascii = new int[] {
            0x0002, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004,
            0x0004, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004, 0x0004,
            0x0008, 0x0020, 0x0020, 0x0020, 0x0020, 0x0020, 0x0020, 0x0020, 0x0020, 0x0020, 0x0020, 0x0020, 0x0020, 0x0400, 0x0200, 0x0800,
            0x0010, 0x0010, 0x0010, 0x0010, 0x0010, 0x0010, 0x0010, 0x0010, 0x0010, 0x0010, 0x0020, 0x0020, 0x0020, 0x0020, 0x0020, 0x0020,
            0x0020, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040,
            0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0020, 0x0020, 0x0020, 0x0020, 0x0100,
            0x0020, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080,
            0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0020, 0x0020, 0x0020, 0x0020, 0x0020,
            0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000,
            0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000,
            0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x0080, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000,
            0x1000, 0x1000, 0x1000, 0x1000, 0x1000, 0x0080, 0x1000, 0x1000, 0x1000, 0x1000, 0x0080, 0x1000, 0x1000, 0x1000, 0x1000, 0x1000,
            0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040,
            0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x1000, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0040, 0x0080,
            0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080,
            0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x1000, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080, 0x0080,
    };

    private final CharSequence myWord;
    private final int myFirstFlags;
    private final int mySecondFlags;
    private final int myLastFlags;
    private final int myWordFlags;

    public WordStudy(final CharSequence word) {
        myWord = word;
        myFirstFlags = myWord.length() == 0 ? EMPTY : flags(myWord.charAt(0));
        mySecondFlags = myWord.length() < 2 ? EMPTY : flags(myWord.charAt(1));
        myWordFlags = flags(myWord);
        myLastFlags = myWord.length() == 0 ? EMPTY : flags(myWord.charAt(myWord.length() - 1));
    }

    public static WordStudy of(CharSequence word) {
        return new WordStudy(word);
    }

    public CharSequence getWord() {
        return myWord;
    }

    public boolean has(int flags) {
        return has(myWordFlags, flags);
    }

    public boolean not(int flags) {
        return not(myWordFlags, flags);
    }

    public boolean only(int flags) {
        return only(myWordFlags, flags);
    }

    public boolean just(int flags) {
        return just(myWordFlags, flags);
    }

    public boolean all(int flags) {
        return all(myWordFlags, flags);
    }

    public boolean first(int flags) {
        return has(myFirstFlags, flags) && only(myFirstFlags, flags);
    }

    public boolean second(int flags) {
        return has(mySecondFlags, flags) && only(mySecondFlags, flags);
    }

    public boolean last(int flags) {
        return has(myLastFlags, flags) && only(myLastFlags, flags);
    }

    public static int flags(char c) {
        if (c == 0) return NUL;
        else if (c < 0x20) return CTRL;
        else if (c == ' ') return SPACE;
        else if (c == '_') return UNDER;
        else if (c == '.') return DOT;
        else if (c == '-') return DASH;
        else if (c == '/') return SLASH;
        else if (Character.isLowerCase(c)) return LOWER;
        else if (Character.isUpperCase(c)) return UPPER;
        else if (isDigit(c)) return DIGITS;
        else if (c < 128) return SYMBOLS;
        else if (!Character.isDefined(c)) return NOT_DEFINED;
        else return OTHER;
    }

    public static int flags(CharSequence word) {
        int flags = 0;
        int iMax = word.length();
        if (iMax == 0) flags |= EMPTY;
        else {
            for (int i = 0; i < iMax; i++) {
                char c = word.charAt(i);
                if (c < 256) flags |= ascii[c];
                else if (Character.isLowerCase(c)) flags |= LOWER;
                else if (Character.isUpperCase(c)) flags |= UPPER;
                else if (!Character.isDefined(c)) flags |= NOT_DEFINED;
                else flags |= OTHER;
            }
        }
        return flags;
    }

    public static boolean has(int wordFlags, int flags) {
        return (wordFlags & flags) != 0;
    }

    public static boolean not(int wordFlags, int flags) {
        return (wordFlags & flags) == 0;
    }

    public static boolean only(int wordFlags, int flags) {
        return (wordFlags & ~flags) == 0;
    }

    public static boolean just(int wordFlags, int flags) {
        return wordFlags == flags;
    }

    public static boolean all(int wordFlags, int flags) {
        return (wordFlags & flags) == flags;
    }

    // @formatter:off
    public boolean isMixedSnakeCase()           { return only(UNDER | LOWER | UPPER | DIGITS) && all(UNDER | LOWER | UPPER) && first(LOWER|UPPER); }
    public boolean isScreamingSnakeCase()       { return only(UNDER | UPPER | DIGITS) && all(UNDER | UPPER) && first(UNDER|UPPER); }
    public boolean isSnakeCase()                { return only(UNDER | LOWER | DIGITS) && all(UNDER | LOWER) && first(UNDER|LOWER); }
    public boolean isCamelCase()                { return only(LOWER | UPPER | DIGITS) && all(LOWER | UPPER) && first(LOWER|UPPER); }
    public boolean hasNoUpperCase()             { return not(UPPER | EMPTY); }
    public boolean hasNoLowerCase()             { return not(LOWER | EMPTY); }
    public boolean hasUpperCase()               { return has(UPPER); }
    public boolean hasLowerCase()               { return has(LOWER); }
    public boolean hasLowerCaseOrUpperCase()    { return has(LOWER | UPPER); }
    public boolean isLowerCase()                { return just(LOWER); }
    public boolean isUpperCase()                { return just(UPPER); }
    public boolean isProperCamelCase()          { return isCamelCase() && first(LOWER); }
    public boolean isPascalCase()               { return isCamelCase() && first(UPPER); }
    // @formatter:on

    public String makeMixedSnakeCase() {
        StringBuilder sb = new StringBuilder();
        int iMax = myWord.length();
        boolean wasLower = false;
        for (int i = 0; i < iMax; i++) {
            char c = myWord.charAt(i);
            if (Character.isUpperCase(c)) {
                if (wasLower) sb.append('_');
            }
            wasLower = Character.isLowerCase(c);
            sb.append(c);
        }
        return sb.toString();
    }

    public String makeCamelCase() {
        StringBuilder sb = new StringBuilder();
        if (has(UNDER)) {
            int iMax = myWord.length();
            boolean toUpper = false;

            for (int i = 0; i < iMax; i++) {
                char c = myWord.charAt(i);
                if (c == '_') {
                    toUpper = true;
                } else {
                    if (toUpper) sb.append(Character.toUpperCase(c));
                    else sb.append(Character.toLowerCase(c));
                    toUpper = false;
                }
            }
        } else if (only(UPPER | DIGITS) && first(UPPER)) {
            sb.append(myWord.charAt(0));
            sb.append(myWord.toString().substring(1).toLowerCase());
        } else {
            sb.append(myWord);
        }
        return sb.toString();
    }

    public String makeProperCamelCase() {
        String s = makeCamelCase();
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    public String makePascalCase() {
        String s = makeCamelCase();
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public String makeScreamingSnakeCase() {
        return makeMixedSnakeCase().toUpperCase();
    }

    public String makeSnakeCase() {
        return makeMixedSnakeCase().toLowerCase();
    }

    public boolean canBeMixedSnakeCase() {
        if (only(UNDER | UPPER | LOWER | DIGITS) && has(LOWER | UPPER) && first(UNDER | LOWER | UPPER)) {
            String word = makeMixedSnakeCase();
            assert !word.equals(myWord) && WordStudy.of(word).isScreamingSnakeCase();
            return true;
        }
        return false;
    }

    public boolean canBeScreamingSnakeCase() {
        if (only(UNDER | UPPER | LOWER | DIGITS) && has(LOWER | UPPER) && first(UNDER | LOWER | UPPER)) {
            String word = makeScreamingSnakeCase();
            assert !word.equals(myWord) && WordStudy.of(word).isScreamingSnakeCase();
            return true;
        }
        return false;
    }

    public boolean canBeSnakeCase() {
        if (only(UNDER | UPPER | LOWER | DIGITS) && has(LOWER | UPPER) && first(UNDER | LOWER | UPPER)) {
            String word = makeSnakeCase();
            return !word.equals(myWord) && WordStudy.of(word).isSnakeCase();
        }
        return false;
    }

    public boolean canBeCamelCase() {
        if (only(UNDER | UPPER | LOWER | DIGITS) && has(LOWER | UPPER) && first(UNDER | LOWER | UPPER)) {
            String word = makeCamelCase();
            return !word.equals(myWord) && WordStudy.of(word).isCamelCase();
        }
        return false;
    }

    public boolean canBeProperCamelCase() {
        if (only(UNDER | UPPER | LOWER | DIGITS) && has(LOWER | UPPER) && first(UNDER | LOWER | UPPER)) {
            String word = makeProperCamelCase();
            return !word.equals(myWord) && WordStudy.of(word).isProperCamelCase();
        }
        return false;
    }

    public boolean canBePascalCase() {
        if (only(UNDER | UPPER | LOWER | DIGITS) && has(LOWER | UPPER) && first(UNDER | LOWER | UPPER)) {
            String word = makePascalCase();
            return !word.equals(myWord) && WordStudy.of(word).isPascalCase();
        }
        return false;
    }
}
