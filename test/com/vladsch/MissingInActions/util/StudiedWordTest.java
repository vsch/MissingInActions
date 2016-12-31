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

import static org.junit.Assert.assertEquals;

public class StudiedWordTest {
    @Test
    public void test_isScreamingSnakeCase() throws Exception {
        assertEquals(true, StudiedWord.of("_A").isScreamingSnakeCase());
        assertEquals(true, StudiedWord.of("_123A").isScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("_123").isScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("123").isScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("a").isScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("A").isScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("_a").isScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("_123a").isScreamingSnakeCase());
        assertEquals(false, StudiedWord.of(" _A").isScreamingSnakeCase());
    }

    @Test
    public void test_isSnakeCase() throws Exception {
        assertEquals(true, StudiedWord.of("_a").isSnakeCase());
        assertEquals(true, StudiedWord.of("_123a").isSnakeCase());
        assertEquals(false, StudiedWord.of("_123").isSnakeCase());
        assertEquals(false, StudiedWord.of("123").isSnakeCase());
        assertEquals(false, StudiedWord.of("a").isSnakeCase());
        assertEquals(false, StudiedWord.of("A").isSnakeCase());
        assertEquals(false, StudiedWord.of("_A").isSnakeCase());
        assertEquals(false, StudiedWord.of("_123A").isSnakeCase());
        assertEquals(false, StudiedWord.of(" _A").isSnakeCase());
    }

    @Test
    public void test_isCamelCase() throws Exception {
        assertEquals(true, StudiedWord.of("aA").isCamelCase());
        assertEquals(true, StudiedWord.of("Aa").isCamelCase());
        assertEquals(false, StudiedWord.of("123aA").isCamelCase());
        assertEquals(false, StudiedWord.of("123Aa").isCamelCase());
        assertEquals(false, StudiedWord.of("123a").isCamelCase());
        assertEquals(false, StudiedWord.of("A").isCamelCase());
        assertEquals(false, StudiedWord.of("_a").isCamelCase());
        assertEquals(false, StudiedWord.of("_123a").isCamelCase());
        assertEquals(false, StudiedWord.of("_123").isCamelCase());
        assertEquals(false, StudiedWord.of("123").isCamelCase());
        assertEquals(false, StudiedWord.of("a").isCamelCase());
        assertEquals(false, StudiedWord.of("A").isCamelCase());
        assertEquals(false, StudiedWord.of("_A").isCamelCase());
        assertEquals(false, StudiedWord.of("_123A").isCamelCase());
        assertEquals(false, StudiedWord.of("_A").isCamelCase());
    }

    @Test
    public void test_isProperCamelCase() throws Exception {
        assertEquals(true, StudiedWord.of("aA").isProperCamelCase());
        assertEquals(false, StudiedWord.of("Aa").isProperCamelCase());
        assertEquals(false, StudiedWord.of("123aA").isProperCamelCase());
        assertEquals(false, StudiedWord.of("123Aa").isProperCamelCase());
        assertEquals(false, StudiedWord.of("Aa").isProperCamelCase());
        assertEquals(false, StudiedWord.of("123a").isProperCamelCase());
        assertEquals(false, StudiedWord.of("A").isProperCamelCase());
        assertEquals(false, StudiedWord.of("_a").isProperCamelCase());
        assertEquals(false, StudiedWord.of("_123a").isProperCamelCase());
        assertEquals(false, StudiedWord.of("_123").isProperCamelCase());
        assertEquals(false, StudiedWord.of("123").isProperCamelCase());
        assertEquals(false, StudiedWord.of("a").isProperCamelCase());
        assertEquals(false, StudiedWord.of("A").isProperCamelCase());
        assertEquals(false, StudiedWord.of("_A").isProperCamelCase());
        assertEquals(false, StudiedWord.of("_123A").isProperCamelCase());
        assertEquals(false, StudiedWord.of("_A").isProperCamelCase());
    }

    @Test
    public void test_isPascalCase() throws Exception {
        assertEquals(true, StudiedWord.of("Aa").isPascalCase());
        assertEquals(false, StudiedWord.of("aA").isPascalCase());
        assertEquals(false, StudiedWord.of("123aA").isPascalCase());
        assertEquals(false, StudiedWord.of("123Aa").isPascalCase());
        assertEquals(false, StudiedWord.of("123a").isPascalCase());
        assertEquals(false, StudiedWord.of("A").isPascalCase());
        assertEquals(false, StudiedWord.of("_a").isPascalCase());
        assertEquals(false, StudiedWord.of("_123a").isPascalCase());
        assertEquals(false, StudiedWord.of("_123").isPascalCase());
        assertEquals(false, StudiedWord.of("123").isPascalCase());
        assertEquals(false, StudiedWord.of("a").isPascalCase());
        assertEquals(false, StudiedWord.of("A").isPascalCase());
        assertEquals(false, StudiedWord.of("_A").isPascalCase());
        assertEquals(false, StudiedWord.of("_123A").isPascalCase());
        assertEquals(false, StudiedWord.of("_A").isPascalCase());
    }

    @Test
    public void test_canBeScreamingSnakeCase() throws Exception {
        assertEquals(true, StudiedWord.of("a_").canBeScreamingSnakeCase());
        assertEquals(true, StudiedWord.of("_a").canBeScreamingSnakeCase());
        assertEquals(true, StudiedWord.of("aB").canBeScreamingSnakeCase());
        assertEquals(true, StudiedWord.of("BaA").canBeScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("Ba").canBeScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("a").canBeScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("A").canBeScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("A_").canBeScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("123_").canBeScreamingSnakeCase());
        assertEquals(false, StudiedWord.of("123").canBeScreamingSnakeCase());
    }

    @Test
    public void test_canBeSnakeCase() throws Exception {
        assertEquals(false, StudiedWord.of("a_").canBeSnakeCase());
        assertEquals(false, StudiedWord.of("_a").canBeSnakeCase());
        assertEquals(true, StudiedWord.of("aB").canBeSnakeCase());
        assertEquals(true, StudiedWord.of("BaA").canBeSnakeCase());
        assertEquals(false, StudiedWord.of("Ba").canBeSnakeCase());
        assertEquals(false, StudiedWord.of("a").canBeSnakeCase());
        assertEquals(false, StudiedWord.of("A").canBeSnakeCase());
        assertEquals(true, StudiedWord.of("A_").canBeSnakeCase());
        assertEquals(false, StudiedWord.of("123_").canBeSnakeCase());
        assertEquals(false, StudiedWord.of("123").canBeSnakeCase());
    }

    @Test
    public void test_canBeCamelCase() throws Exception {
        assertEquals(true, StudiedWord.of("a_a").canBeCamelCase());
        assertEquals(true, StudiedWord.of("a_A").canBeCamelCase());
        assertEquals(true, StudiedWord.of("A_a").canBeCamelCase());
        assertEquals(true, StudiedWord.of("A_A").canBeCamelCase());
        assertEquals(false, StudiedWord.of("A_").canBeCamelCase());
        assertEquals(false, StudiedWord.of("_A").canBeCamelCase());
        assertEquals(false, StudiedWord.of("aB").canBeCamelCase());
        assertEquals(false, StudiedWord.of("Ba").canBeCamelCase());
        assertEquals(false, StudiedWord.of("a").canBeCamelCase());
        assertEquals(false, StudiedWord.of("A").canBeCamelCase());
        assertEquals(false, StudiedWord.of("a_").canBeCamelCase());
        assertEquals(false, StudiedWord.of("123_").canBeCamelCase());
        assertEquals(false, StudiedWord.of("123").canBeCamelCase());
    }

    @Test
    public void test_canBeProperCamelCase() throws Exception {
        assertEquals(true, StudiedWord.of("a_a").canBeProperCamelCase());
        assertEquals(true, StudiedWord.of("a_A").canBeProperCamelCase());
        assertEquals(true, StudiedWord.of("A_a").canBeProperCamelCase());
        assertEquals(true, StudiedWord.of("A_A").canBeProperCamelCase());
        assertEquals(false, StudiedWord.of("Abc").canBeProperCamelCase());
        assertEquals(false, StudiedWord.of("A_").canBeProperCamelCase());
        assertEquals(false, StudiedWord.of("_A").canBeProperCamelCase());
        assertEquals(false, StudiedWord.of("aB").canBeProperCamelCase());
        assertEquals(false, StudiedWord.of("Ba").canBeProperCamelCase());
        assertEquals(false, StudiedWord.of("a").canBeProperCamelCase());
        assertEquals(false, StudiedWord.of("A").canBeProperCamelCase());
        assertEquals(false, StudiedWord.of("a_").canBeProperCamelCase());
        assertEquals(false, StudiedWord.of("123_").canBeProperCamelCase());
        assertEquals(false, StudiedWord.of("123").canBeProperCamelCase());
    }

    @Test
    public void test_canBePascalCase() throws Exception {
        assertEquals(false, StudiedWord.of("a_aa").canBePascalCase());
        assertEquals(true, StudiedWord.of("aa_a").canBePascalCase());
        assertEquals(true, StudiedWord.of("aa_aa").canBePascalCase());
        assertEquals(false, StudiedWord.of("A_AA").canBePascalCase());
        assertEquals(true, StudiedWord.of("AA_A").canBePascalCase());
        assertEquals(true, StudiedWord.of("AA_AA").canBePascalCase());
        assertEquals(false, StudiedWord.of("A_aa").canBePascalCase());
        assertEquals(true, StudiedWord.of("Aa_a").canBePascalCase());
        assertEquals(true, StudiedWord.of("Aa_aa").canBePascalCase());
        assertEquals(false, StudiedWord.of("aBc").canBePascalCase());
        assertEquals(false, StudiedWord.of("a_Aa").canBePascalCase());
        assertEquals(true, StudiedWord.of("aa_A").canBePascalCase());
        assertEquals(true, StudiedWord.of("aa_Aa").canBePascalCase());
        assertEquals(true, StudiedWord.of("aa_aA").canBePascalCase());
        assertEquals(false, StudiedWord.of("aBc").canBePascalCase());
        assertEquals(false, StudiedWord.of("a_a").canBePascalCase());
        assertEquals(false, StudiedWord.of("a_A").canBePascalCase());
        assertEquals(false, StudiedWord.of("A_a").canBePascalCase());
        assertEquals(false, StudiedWord.of("A_A").canBePascalCase());
        assertEquals(false, StudiedWord.of("A_").canBePascalCase());
        assertEquals(false, StudiedWord.of("_A").canBePascalCase());
        assertEquals(false, StudiedWord.of("aB").canBePascalCase());
        assertEquals(false, StudiedWord.of("Ba").canBePascalCase());
        assertEquals(false, StudiedWord.of("a").canBePascalCase());
        assertEquals(false, StudiedWord.of("A").canBePascalCase());
        assertEquals(false, StudiedWord.of("a_").canBePascalCase());
        assertEquals(false, StudiedWord.of("123_").canBePascalCase());
        assertEquals(false, StudiedWord.of("123").canBePascalCase());
    }

    @Test
    public void test_makeMixedSnakeCase() throws Exception {
        assertEquals("a_B", StudiedWord.of("aB").makeMixedSnakeCase());
        assertEquals("a_Bc", StudiedWord.of("aBc").makeMixedSnakeCase());
        assertEquals("ABC", StudiedWord.of("ABC").makeMixedSnakeCase());
        assertEquals("abc_Def_Hij", StudiedWord.of("abcDefHij").makeMixedSnakeCase());
        assertEquals("Abc_Def_Hij", StudiedWord.of("AbcDefHij").makeMixedSnakeCase());
    }

    @Test
    public void test_makeCamelCase() throws Exception {
        assertEquals("abcDef", StudiedWord.of("abcDef").makeCamelCase());
        assertEquals("AbcDef", StudiedWord.of("AbcDef").makeCamelCase());
        assertEquals("abcDef", StudiedWord.of("Abc_Def").makeCamelCase());
        assertEquals("abcDef", StudiedWord.of("abc_def").makeCamelCase());
        assertEquals("abcDef", StudiedWord.of("ABC_DEF").makeCamelCase());
    }

    @Test
    public void test_makeScreamingSnakeCase() throws Exception {
        assertEquals("ABC_DEF", StudiedWord.of("abcDef").makeScreamingSnakeCase());
        assertEquals("ABC_DEF", StudiedWord.of("AbcDef").makeScreamingSnakeCase());
        assertEquals("ABC_DEF", StudiedWord.of("Abc_Def").makeScreamingSnakeCase());
        assertEquals("ABC_DEF", StudiedWord.of("abc_def").makeScreamingSnakeCase());
        assertEquals("ABC_DEF", StudiedWord.of("ABC_DEF").makeScreamingSnakeCase());
    }

    @Test
    public void test_makeSnakeCase() throws Exception {
        assertEquals("abc_def", StudiedWord.of("abcDef").makeSnakeCase());
        assertEquals("abc_def", StudiedWord.of("AbcDef").makeSnakeCase());
        assertEquals("abc_def", StudiedWord.of("Abc_Def").makeSnakeCase());
        assertEquals("abc_def", StudiedWord.of("abc_def").makeSnakeCase());
        assertEquals("abc_def", StudiedWord.of("ABC_DEF").makeSnakeCase());
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
