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

import org.junit.Test;

import static com.vladsch.MissingInActions.util.EditHelpers.*;
import static org.junit.Assert.*;

public class EditHelpersTest {
    @Test
    public void test_isScreamingSnakeCase() throws Exception {
        assertEquals(true, isScreamingSnakeCase("_A"));
        assertEquals(true, isScreamingSnakeCase("_123A"));
        assertEquals(false, isScreamingSnakeCase("_123"));
        assertEquals(false, isScreamingSnakeCase("123"));
        assertEquals(false, isScreamingSnakeCase("a"));
        assertEquals(false, isScreamingSnakeCase("A"));
        assertEquals(false, isScreamingSnakeCase("_a"));
        assertEquals(false, isScreamingSnakeCase("_123a"));
        assertEquals(false, isScreamingSnakeCase(" _A"));
    }

    @Test
    public void test_isSnakeCase() throws Exception {
        assertEquals(true, isSnakeCase("_a"));
        assertEquals(true, isSnakeCase("_123a"));
        assertEquals(false, isSnakeCase("_123"));
        assertEquals(false, isSnakeCase("123"));
        assertEquals(false, isSnakeCase("a"));
        assertEquals(false, isSnakeCase("A"));
        assertEquals(false, isSnakeCase("_A"));
        assertEquals(false, isSnakeCase("_123A"));
        assertEquals(false, isSnakeCase(" _A"));
    }

    @Test
    public void test_isCamelCase() throws Exception {
        assertEquals(true, isCamelCase("aA"));
        assertEquals(true, isCamelCase("Aa"));
        assertEquals(true, isCamelCase("123aA"));
        assertEquals(true, isCamelCase("123Aa"));
        assertEquals(false, isCamelCase("123a"));
        assertEquals(false, isCamelCase("A"));
        assertEquals(false, isCamelCase("_a"));
        assertEquals(false, isCamelCase("_123a"));
        assertEquals(false, isCamelCase("_123"));
        assertEquals(false, isCamelCase("123"));
        assertEquals(false, isCamelCase("a"));
        assertEquals(false, isCamelCase("A"));
        assertEquals(false, isCamelCase("_A"));
        assertEquals(false, isCamelCase("_123A"));
        assertEquals(false, isCamelCase("_A"));
    }

    @Test
    public void test_isProperCamelCase() throws Exception {
        assertEquals(true, isProperCamelCase("aA"));
        assertEquals(false, isProperCamelCase("123aA"));
        assertEquals(false, isProperCamelCase("123Aa"));
        assertEquals(false, isProperCamelCase("Aa"));
        assertEquals(false, isProperCamelCase("123a"));
        assertEquals(false, isProperCamelCase("A"));
        assertEquals(false, isProperCamelCase("_a"));
        assertEquals(false, isProperCamelCase("_123a"));
        assertEquals(false, isProperCamelCase("_123"));
        assertEquals(false, isProperCamelCase("123"));
        assertEquals(false, isProperCamelCase("a"));
        assertEquals(false, isProperCamelCase("A"));
        assertEquals(false, isProperCamelCase("_A"));
        assertEquals(false, isProperCamelCase("_123A"));
        assertEquals(false, isProperCamelCase("_A"));
    }

    @Test
    public void test_isPascalCase() throws Exception {
        assertEquals(true, isPascalCase("Aa"));
        assertEquals(false, isPascalCase("aA"));
        assertEquals(false, isPascalCase("123aA"));
        assertEquals(false, isPascalCase("123Aa"));
        assertEquals(false, isPascalCase("123a"));
        assertEquals(false, isPascalCase("A"));
        assertEquals(false, isPascalCase("_a"));
        assertEquals(false, isPascalCase("_123a"));
        assertEquals(false, isPascalCase("_123"));
        assertEquals(false, isPascalCase("123"));
        assertEquals(false, isPascalCase("a"));
        assertEquals(false, isPascalCase("A"));
        assertEquals(false, isPascalCase("_A"));
        assertEquals(false, isPascalCase("_123A"));
        assertEquals(false, isPascalCase("_A"));
    }

    @Test
    public void test_canMakeScreamingSnakeCase() throws Exception {
        assertEquals(true, canMakeScreamingSnakeCase("a_"));
        assertEquals(true, canMakeScreamingSnakeCase("_a"));
        assertEquals(true, canMakeScreamingSnakeCase("aB"));
        assertEquals(true, canMakeScreamingSnakeCase("BaA"));
        assertEquals(false, canMakeScreamingSnakeCase("Ba"));
        assertEquals(false, canMakeScreamingSnakeCase("a"));
        assertEquals(false, canMakeScreamingSnakeCase("A"));
        assertEquals(false, canMakeScreamingSnakeCase("A_"));
        assertEquals(false, canMakeScreamingSnakeCase("123_"));
        assertEquals(false, canMakeScreamingSnakeCase("123"));
    }

    @Test
    public void test_canMakeSnakeCase() throws Exception {
        assertEquals(false, canMakeSnakeCase("a_"));
        assertEquals(false, canMakeSnakeCase("_a"));
        assertEquals(true, canMakeSnakeCase("aB"));
        assertEquals(true, canMakeSnakeCase("BaA"));
        assertEquals(false, canMakeSnakeCase("Ba"));
        assertEquals(false, canMakeSnakeCase("a"));
        assertEquals(false, canMakeSnakeCase("A"));
        assertEquals(true, canMakeSnakeCase("A_"));
        assertEquals(false, canMakeSnakeCase("123_"));
        assertEquals(false, canMakeSnakeCase("123"));
    }

    @Test
    public void test_canMakeCamelCase() throws Exception {
        assertEquals(true, canMakeCamelCase("a_a"));
        assertEquals(true, canMakeCamelCase("a_A"));
        assertEquals(true, canMakeCamelCase("A_a"));
        assertEquals(true, canMakeCamelCase("A_A"));
        assertEquals(true, canMakeCamelCase("A_"));
        assertEquals(true, canMakeCamelCase("_A"));
        assertEquals(false, canMakeCamelCase("aB"));
        assertEquals(false, canMakeCamelCase("Ba"));
        assertEquals(false, canMakeCamelCase("a"));
        assertEquals(false, canMakeCamelCase("A"));
        assertEquals(true, canMakeCamelCase("a_"));
        assertEquals(false, canMakeCamelCase("123_"));
        assertEquals(false, canMakeCamelCase("123"));
    }

    @Test
    public void test_canMakeProperCamelCase() throws Exception {
        assertEquals(true, canMakeProperCamelCase("a_a"));
        assertEquals(true, canMakeProperCamelCase("a_A"));
        assertEquals(true, canMakeProperCamelCase("A_a"));
        assertEquals(true, canMakeProperCamelCase("A_A"));
        assertEquals(true, canMakeProperCamelCase("Abc"));
        assertEquals(true, canMakeProperCamelCase("A_"));
        assertEquals(true, canMakeProperCamelCase("_A"));
        assertEquals(false, canMakeProperCamelCase("aB"));
        assertEquals(true, canMakeProperCamelCase("Ba"));
        assertEquals(false, canMakeProperCamelCase("a"));
        assertEquals(true, canMakeProperCamelCase("A"));
        assertEquals(true, canMakeProperCamelCase("a_"));
        assertEquals(false, canMakeProperCamelCase("123_"));
        assertEquals(false, canMakeProperCamelCase("123"));
    }

    @Test
    public void test_canMakePascalCase() throws Exception {
        assertEquals(false, canMakePascalCase("a_aa"));
        assertEquals(true, canMakePascalCase("aa_a"));
        assertEquals(true, canMakePascalCase("aa_aa"));
        assertEquals(false, canMakePascalCase("A_AA"));
        assertEquals(true, canMakePascalCase("AA_A"));
        assertEquals(true, canMakePascalCase("AA_AA"));
        assertEquals(false, canMakePascalCase("A_aa"));
        assertEquals(true, canMakePascalCase("Aa_a"));
        assertEquals(true, canMakePascalCase("Aa_aa"));
        assertEquals(false, canMakePascalCase("aBc"));
        assertEquals(false, canMakePascalCase("a_Aa"));
        assertEquals(true, canMakePascalCase("aa_A"));
        assertEquals(true, canMakePascalCase("aa_Aa"));
        assertEquals(true, canMakePascalCase("aa_aA"));
        assertEquals(false, canMakePascalCase("aBc"));
        assertEquals(false, canMakePascalCase("a_a"));
        assertEquals(false, canMakePascalCase("a_A"));
        assertEquals(false, canMakePascalCase("A_a"));
        assertEquals(false, canMakePascalCase("A_A"));
        assertEquals(false, canMakePascalCase("A_"));
        assertEquals(false, canMakePascalCase("_A"));
        assertEquals(false, canMakePascalCase("aB"));
        assertEquals(false, canMakePascalCase("Ba"));
        assertEquals(false, canMakePascalCase("a"));
        assertEquals(false, canMakePascalCase("A"));
        assertEquals(false, canMakePascalCase("a_"));
        assertEquals(false, canMakePascalCase("123_"));
        assertEquals(false, canMakePascalCase("123"));
    }

    @Test
    public void test_makeMixedSnakeCase() throws Exception {
        assertEquals("a_B", makeMixedSnakeCase("aB"));
        assertEquals("a_Bc", makeMixedSnakeCase("aBc"));
        assertEquals("ABC", makeMixedSnakeCase("ABC"));
        assertEquals("abc_Def_Hij", makeMixedSnakeCase("abcDefHij"));
        assertEquals("Abc_Def_Hij", makeMixedSnakeCase("AbcDefHij"));
    }

    @Test
    public void test_makeCamelCase() throws Exception {
        assertEquals("abcDef", makeCamelCase("abcDef"));
        assertEquals("AbcDef", makeCamelCase("AbcDef"));
        assertEquals("abcDef", makeCamelCase("Abc_Def"));
        assertEquals("abcDef", makeCamelCase("abc_def"));
        assertEquals("abcDef", makeCamelCase("ABC_DEF"));
    }

    @Test
    public void test_makeScreamingSnakeCase() throws Exception {
        assertEquals("ABC_DEF", makeScreamingSnakeCase("abcDef"));
        assertEquals("ABC_DEF", makeScreamingSnakeCase("AbcDef"));
        assertEquals("ABC_DEF", makeScreamingSnakeCase("Abc_Def"));
        assertEquals("ABC_DEF", makeScreamingSnakeCase("abc_def"));
        assertEquals("ABC_DEF", makeScreamingSnakeCase("ABC_DEF"));
    }

    @Test
    public void test_makeSnakeCase() throws Exception {
        assertEquals("abc_def", makeSnakeCase("abcDef"));
        assertEquals("abc_def", makeSnakeCase("AbcDef"));
        assertEquals("abc_def", makeSnakeCase("Abc_Def"));
        assertEquals("abc_def", makeSnakeCase("abc_def"));
        assertEquals("abc_def", makeSnakeCase("ABC_DEF"));
    }

    @Test
    public void test_() throws Exception {
        String source = " abcDefHij ";
        int iMax = source.length();
        for (int i = 0; i < iMax - 1; i++) {
            for (int j = i; j < iMax; j++) {
                assertEquals(source.trim(), EditHelpers.getWordAtOffsets(source, i, j, EditHelpers.WORD_IDENTIFIER, false));
            }
        }
    }
}
