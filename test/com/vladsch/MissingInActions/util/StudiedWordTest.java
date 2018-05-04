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

package com.vladsch.MissingInActions.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StudiedWordTest {
    @Test
    public void test_isScreamingSnakeCase() throws Exception {
        assertEquals(true, StudiedWord.of("_A", StudiedWord.UNDER).isScreamingSnakeCase());
        assertEquals(true, StudiedWord.of("_123A", StudiedWord.UNDER).isScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("_123", StudiedWord.UNDER).isScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("123", StudiedWord.UNDER).isScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("a", StudiedWord.UNDER).isScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("A", StudiedWord.UNDER).isScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("_a", StudiedWord.UNDER).isScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("_123a", StudiedWord.UNDER).isScreamingSnakeCase());
        assertEquals(false, StudiedWord.of(" _A", StudiedWord.UNDER).isScreamingSnakeCase());
    }

    @Test
    public void test_isSnakeCase() throws Exception {
        assertEquals(true, StudiedWord.of("_a", StudiedWord.UNDER).isSnakeCase());
        assertEquals(true, StudiedWord.of("_123a", StudiedWord.UNDER).isSnakeCase());
        assertEquals(false, StudiedWord.of("_123", StudiedWord.UNDER).isSnakeCase());
        assertEquals(false, StudiedWord.of("123", StudiedWord.UNDER).isSnakeCase());
        assertEquals(false, StudiedWord.of("a", StudiedWord.UNDER).isSnakeCase());
        assertEquals(false, StudiedWord.of("A", StudiedWord.UNDER).isSnakeCase());
        assertEquals(false, StudiedWord.of("_A", StudiedWord.UNDER).isSnakeCase());
        assertEquals(false, StudiedWord.of("_123A", StudiedWord.UNDER).isSnakeCase());
        assertEquals(false, StudiedWord.of(" _A", StudiedWord.UNDER).isSnakeCase());
    }

    @Test
    public void test_isCamelCase() throws Exception {
        assertEquals(true, StudiedWord.of("aA", StudiedWord.UNDER).isCamelCase());
        assertEquals(true, StudiedWord.of("Aa", StudiedWord.UNDER).isCamelCase());
        assertEquals(false, StudiedWord.of("123aA", StudiedWord.UNDER).isCamelCase());
        assertEquals(false, StudiedWord.of("123Aa", StudiedWord.UNDER).isCamelCase());
        assertEquals(false, StudiedWord.of("123a", StudiedWord.UNDER).isCamelCase());
        assertEquals(false, StudiedWord.of("A", StudiedWord.UNDER).isCamelCase());
        assertEquals(false, StudiedWord.of("_a", StudiedWord.UNDER).isCamelCase());
        assertEquals(false, StudiedWord.of("_123a", StudiedWord.UNDER).isCamelCase());
        assertEquals(false, StudiedWord.of("_123", StudiedWord.UNDER).isCamelCase());
        assertEquals(false, StudiedWord.of("123", StudiedWord.UNDER).isCamelCase());
        assertEquals(false, StudiedWord.of("a", StudiedWord.UNDER).isCamelCase());
        assertEquals(false, StudiedWord.of("A", StudiedWord.UNDER).isCamelCase());
        assertEquals(false, StudiedWord.of("_A", StudiedWord.UNDER).isCamelCase());
        assertEquals(false, StudiedWord.of("_123A", StudiedWord.UNDER).isCamelCase());
        assertEquals(false, StudiedWord.of("_A", StudiedWord.UNDER).isCamelCase());
    }

    @Test
    public void test_isProperCamelCase() throws Exception {
        assertEquals(true, StudiedWord.of("aA", StudiedWord.UNDER).isProperCamelCase());
        assertEquals(false, StudiedWord.of("Aa", StudiedWord.UNDER).isProperCamelCase());
        assertEquals(false, StudiedWord.of("123aA", StudiedWord.UNDER).isProperCamelCase());
        assertEquals(false, StudiedWord.of("123Aa", StudiedWord.UNDER).isProperCamelCase());
        assertEquals(false, StudiedWord.of("Aa", StudiedWord.UNDER).isProperCamelCase());
        assertEquals(false, StudiedWord.of("123a", StudiedWord.UNDER).isProperCamelCase());
        assertEquals(false, StudiedWord.of("A", StudiedWord.UNDER).isProperCamelCase());
        assertEquals(false, StudiedWord.of("_a", StudiedWord.UNDER).isProperCamelCase());
        assertEquals(false, StudiedWord.of("_123a", StudiedWord.UNDER).isProperCamelCase());
        assertEquals(false, StudiedWord.of("_123", StudiedWord.UNDER).isProperCamelCase());
        assertEquals(false, StudiedWord.of("123", StudiedWord.UNDER).isProperCamelCase());
        assertEquals(false, StudiedWord.of("a", StudiedWord.UNDER).isProperCamelCase());
        assertEquals(false, StudiedWord.of("A", StudiedWord.UNDER).isProperCamelCase());
        assertEquals(false, StudiedWord.of("_A", StudiedWord.UNDER).isProperCamelCase());
        assertEquals(false, StudiedWord.of("_123A", StudiedWord.UNDER).isProperCamelCase());
        assertEquals(false, StudiedWord.of("_A", StudiedWord.UNDER).isProperCamelCase());
    }

    @Test
    public void test_isPascalCase() throws Exception {
        assertEquals(true, StudiedWord.of("Aa", StudiedWord.UNDER).isPascalCase());
        assertEquals(false, StudiedWord.of("aA", StudiedWord.UNDER).isPascalCase());
        assertEquals(false, StudiedWord.of("123aA", StudiedWord.UNDER).isPascalCase());
        assertEquals(false, StudiedWord.of("123Aa", StudiedWord.UNDER).isPascalCase());
        assertEquals(false, StudiedWord.of("123a", StudiedWord.UNDER).isPascalCase());
        assertEquals(false, StudiedWord.of("A", StudiedWord.UNDER).isPascalCase());
        assertEquals(false, StudiedWord.of("_a", StudiedWord.UNDER).isPascalCase());
        assertEquals(false, StudiedWord.of("_123a", StudiedWord.UNDER).isPascalCase());
        assertEquals(false, StudiedWord.of("_123", StudiedWord.UNDER).isPascalCase());
        assertEquals(false, StudiedWord.of("123", StudiedWord.UNDER).isPascalCase());
        assertEquals(false, StudiedWord.of("a", StudiedWord.UNDER).isPascalCase());
        assertEquals(false, StudiedWord.of("A", StudiedWord.UNDER).isPascalCase());
        assertEquals(false, StudiedWord.of("_A", StudiedWord.UNDER).isPascalCase());
        assertEquals(false, StudiedWord.of("_123A", StudiedWord.UNDER).isPascalCase());
        assertEquals(false, StudiedWord.of("_A", StudiedWord.UNDER).isPascalCase());
    }

    @Test
    public void test_canBeScreamingSnakeCase() throws Exception {
        assertEquals(true, StudiedWord.of("a_", StudiedWord.UNDER).canBeScreamingSnakeCase());
        assertEquals(true, StudiedWord.of("_a", StudiedWord.UNDER).canBeScreamingSnakeCase());
        assertEquals(true, StudiedWord.of("aB", StudiedWord.UNDER).canBeScreamingSnakeCase());
        assertEquals(true, StudiedWord.of("BaA", StudiedWord.UNDER).canBeScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("Ba", StudiedWord.UNDER).canBeScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("a", StudiedWord.UNDER).canBeScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("A", StudiedWord.UNDER).canBeScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("A_", StudiedWord.UNDER).canBeScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("123_", StudiedWord.UNDER).canBeScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("123", StudiedWord.UNDER).canBeScreamingSnakeCase());
    }

    @Test
    public void test_canBeSnakeCase() throws Exception {
        assertEquals(false, StudiedWord.of("a_", StudiedWord.UNDER).canBeSnakeCase());
        assertEquals(false, StudiedWord.of("_a", StudiedWord.UNDER).canBeSnakeCase());
        assertEquals(true, StudiedWord.of("aB", StudiedWord.UNDER).canBeSnakeCase());
        assertEquals(true, StudiedWord.of("BaA", StudiedWord.UNDER).canBeSnakeCase());
        assertEquals(false, StudiedWord.of("Ba", StudiedWord.UNDER).canBeSnakeCase());
        assertEquals(false, StudiedWord.of("a", StudiedWord.UNDER).canBeSnakeCase());
        assertEquals(false, StudiedWord.of("A", StudiedWord.UNDER).canBeSnakeCase());
        assertEquals(true, StudiedWord.of("A_", StudiedWord.UNDER).canBeSnakeCase());
        assertEquals(false, StudiedWord.of("123_", StudiedWord.UNDER).canBeSnakeCase());
        assertEquals(false, StudiedWord.of("123", StudiedWord.UNDER).canBeSnakeCase());
    }

    @Test
    public void test_canBeCamelCase() throws Exception {
        assertEquals(true, StudiedWord.of("a_a", StudiedWord.UNDER).canBeCamelCase());
        assertEquals(true, StudiedWord.of("a_A", StudiedWord.UNDER).canBeCamelCase());
        assertEquals(true, StudiedWord.of("A_a", StudiedWord.UNDER).canBeCamelCase());
        assertEquals(true, StudiedWord.of("A_A", StudiedWord.UNDER).canBeCamelCase());
        assertEquals(false, StudiedWord.of("A_", StudiedWord.UNDER).canBeCamelCase());
        assertEquals(false, StudiedWord.of("_A", StudiedWord.UNDER).canBeCamelCase());
        assertEquals(false, StudiedWord.of("aB", StudiedWord.UNDER).canBeCamelCase());
        assertEquals(false, StudiedWord.of("Ba", StudiedWord.UNDER).canBeCamelCase());
        assertEquals(false, StudiedWord.of("a", StudiedWord.UNDER).canBeCamelCase());
        assertEquals(false, StudiedWord.of("A", StudiedWord.UNDER).canBeCamelCase());
        assertEquals(false, StudiedWord.of("a_", StudiedWord.UNDER).canBeCamelCase());
        assertEquals(false, StudiedWord.of("123_", StudiedWord.UNDER).canBeCamelCase());
        assertEquals(false, StudiedWord.of("123", StudiedWord.UNDER).canBeCamelCase());
    }

    @Test
    public void test_canBeProperCamelCase() throws Exception {
        assertEquals(true, StudiedWord.of("a_a", StudiedWord.UNDER).canBeProperCamelCase());
        assertEquals(true, StudiedWord.of("a_A", StudiedWord.UNDER).canBeProperCamelCase());
        assertEquals(true, StudiedWord.of("A_a", StudiedWord.UNDER).canBeProperCamelCase());
        assertEquals(true, StudiedWord.of("A_A", StudiedWord.UNDER).canBeProperCamelCase());
        assertEquals(false, StudiedWord.of("Abc", StudiedWord.UNDER).canBeProperCamelCase());
        assertEquals(false, StudiedWord.of("A_", StudiedWord.UNDER).canBeProperCamelCase());
        assertEquals(false, StudiedWord.of("_A", StudiedWord.UNDER).canBeProperCamelCase());
        assertEquals(false, StudiedWord.of("aB", StudiedWord.UNDER).canBeProperCamelCase());
        assertEquals(false, StudiedWord.of("Ba", StudiedWord.UNDER).canBeProperCamelCase());
        assertEquals(false, StudiedWord.of("a", StudiedWord.UNDER).canBeProperCamelCase());
        assertEquals(false, StudiedWord.of("A", StudiedWord.UNDER).canBeProperCamelCase());
        assertEquals(false, StudiedWord.of("a_", StudiedWord.UNDER).canBeProperCamelCase());
        assertEquals(false, StudiedWord.of("123_", StudiedWord.UNDER).canBeProperCamelCase());
        assertEquals(false, StudiedWord.of("123", StudiedWord.UNDER).canBeProperCamelCase());
    }

    @Test
    public void test_canBePascalCase() throws Exception {
        assertEquals(false, StudiedWord.of("a_aa", StudiedWord.UNDER).canBePascalCase());
        assertEquals(true, StudiedWord.of("aa_a", StudiedWord.UNDER).canBePascalCase());
        assertEquals(true, StudiedWord.of("aa_aa", StudiedWord.UNDER).canBePascalCase());
        assertEquals(false, StudiedWord.of("A_AA", StudiedWord.UNDER).canBePascalCase());
        assertEquals(true, StudiedWord.of("AA_A", StudiedWord.UNDER).canBePascalCase());
        assertEquals(true, StudiedWord.of("AA_AA", StudiedWord.UNDER).canBePascalCase());
        assertEquals(false, StudiedWord.of("A_aa", StudiedWord.UNDER).canBePascalCase());
        assertEquals(true, StudiedWord.of("Aa_a", StudiedWord.UNDER).canBePascalCase());
        assertEquals(true, StudiedWord.of("Aa_aa", StudiedWord.UNDER).canBePascalCase());
        assertEquals(false, StudiedWord.of("aBc", StudiedWord.UNDER).canBePascalCase());
        assertEquals(false, StudiedWord.of("a_Aa", StudiedWord.UNDER).canBePascalCase());
        assertEquals(true, StudiedWord.of("aa_A", StudiedWord.UNDER).canBePascalCase());
        assertEquals(true, StudiedWord.of("aa_Aa", StudiedWord.UNDER).canBePascalCase());
        assertEquals(true, StudiedWord.of("aa_aA", StudiedWord.UNDER).canBePascalCase());
        assertEquals(false, StudiedWord.of("aBc", StudiedWord.UNDER).canBePascalCase());
        assertEquals(false, StudiedWord.of("a_a", StudiedWord.UNDER).canBePascalCase());
        assertEquals(false, StudiedWord.of("a_A", StudiedWord.UNDER).canBePascalCase());
        assertEquals(false, StudiedWord.of("A_a", StudiedWord.UNDER).canBePascalCase());
        assertEquals(false, StudiedWord.of("A_A", StudiedWord.UNDER).canBePascalCase());
        assertEquals(false, StudiedWord.of("A_", StudiedWord.UNDER).canBePascalCase());
        assertEquals(false, StudiedWord.of("_A", StudiedWord.UNDER).canBePascalCase());
        assertEquals(false, StudiedWord.of("aB", StudiedWord.UNDER).canBePascalCase());
        assertEquals(false, StudiedWord.of("Ba", StudiedWord.UNDER).canBePascalCase());
        assertEquals(false, StudiedWord.of("a", StudiedWord.UNDER).canBePascalCase());
        assertEquals(false, StudiedWord.of("A", StudiedWord.UNDER).canBePascalCase());
        assertEquals(false, StudiedWord.of("a_", StudiedWord.UNDER).canBePascalCase());
        assertEquals(false, StudiedWord.of("123_", StudiedWord.UNDER).canBePascalCase());
        assertEquals(false, StudiedWord.of("123", StudiedWord.UNDER).canBePascalCase());
    }

    @Test
    public void test_makeMixedSnakeCase() throws Exception {
        assertEquals("a_B", StudiedWord.of("aB", StudiedWord.UNDER).makeMixedSnakeCase());
        assertEquals("a_Bc", StudiedWord.of("aBc", StudiedWord.UNDER).makeMixedSnakeCase());
        assertEquals("ABC", StudiedWord.of("ABC", StudiedWord.UNDER).makeMixedSnakeCase());
        assertEquals("abc_Def_Hij", StudiedWord.of("abcDefHij", StudiedWord.UNDER).makeMixedSnakeCase());
        assertEquals("Abc_Def_Hij", StudiedWord.of("AbcDefHij", StudiedWord.UNDER).makeMixedSnakeCase());
    }

    @Test
    public void test_makeCamelCase() throws Exception {
        assertEquals("abcDef", StudiedWord.of("abcDef", StudiedWord.UNDER).makeCamelCase());
        assertEquals("AbcDef", StudiedWord.of("AbcDef", StudiedWord.UNDER).makeCamelCase());
        assertEquals("abcDef", StudiedWord.of("Abc_Def", StudiedWord.UNDER).makeCamelCase());
        assertEquals("abcDef", StudiedWord.of("abc_def", StudiedWord.UNDER).makeCamelCase());
        assertEquals("abcDef", StudiedWord.of("ABC_DEF", StudiedWord.UNDER).makeCamelCase());
    }

    @Test
    public void test_makeScreamingSnakeCase() throws Exception {
        assertEquals("ABC_DEF", StudiedWord.of("abcDef", StudiedWord.UNDER).makeScreamingSnakeCase());
        assertEquals("ABC_DEF", StudiedWord.of("AbcDef", StudiedWord.UNDER).makeScreamingSnakeCase());
        assertEquals("ABC_DEF", StudiedWord.of("Abc_Def", StudiedWord.UNDER).makeScreamingSnakeCase());
        assertEquals("ABC_DEF", StudiedWord.of("abc_def", StudiedWord.UNDER).makeScreamingSnakeCase());
        assertEquals("ABC_DEF", StudiedWord.of("ABC_DEF", StudiedWord.UNDER).makeScreamingSnakeCase());
    }

    @Test
    public void test_makeSnakeCase() throws Exception {
        assertEquals("abc_def", StudiedWord.of("abcDef", StudiedWord.UNDER).makeSnakeCase());
        assertEquals("abc_def", StudiedWord.of("AbcDef", StudiedWord.UNDER).makeSnakeCase());
        assertEquals("abc_def", StudiedWord.of("Abc_Def", StudiedWord.UNDER).makeSnakeCase());
        assertEquals("abc_def", StudiedWord.of("abc_def", StudiedWord.UNDER).makeSnakeCase());
        assertEquals("abc_def", StudiedWord.of("ABC_DEF", StudiedWord.UNDER).makeSnakeCase());
    }

    @Test
    public void test_() throws Exception {
        String source = " abcDefHij ";
        int iMax = source.length();
        for (int i = 0; i < iMax - 1; i++) {
            for (int j = i; j < iMax; j++) {
                if (i < 1 && j < 1 || i > source.length() - 1 && j > source.length() - 1) {
                    assertEquals("", EditHelpers.getWordAtOffsets(source, i, j, EditHelpers.WORD_IDENTIFIER, false,true));
                } else {
                    assertEquals(source.trim(), EditHelpers.getWordAtOffsets(source, i, j, EditHelpers.WORD_IDENTIFIER, false,true));
                }
            }
        }
    }
}
