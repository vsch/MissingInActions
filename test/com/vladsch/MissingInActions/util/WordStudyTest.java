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

public class WordStudyTest {
    @Test
    public void test_isScreamingSnakeCase() throws Exception {
        assertEquals(true, WordStudy.of("_A").isScreamingSnakeCase());
        assertEquals(true, WordStudy.of("_123A").isScreamingSnakeCase());
        assertEquals(false, WordStudy.of("_123").isScreamingSnakeCase());
        assertEquals(false, WordStudy.of("123").isScreamingSnakeCase());
        assertEquals(false, WordStudy.of("a").isScreamingSnakeCase());
        assertEquals(false, WordStudy.of("A").isScreamingSnakeCase());
        assertEquals(false, WordStudy.of("_a").isScreamingSnakeCase());
        assertEquals(false, WordStudy.of("_123a").isScreamingSnakeCase());
        assertEquals(false, WordStudy.of(" _A").isScreamingSnakeCase());
    }

    @Test
    public void test_isSnakeCase() throws Exception {
        assertEquals(true, WordStudy.of("_a").isSnakeCase());
        assertEquals(true, WordStudy.of("_123a").isSnakeCase());
        assertEquals(false, WordStudy.of("_123").isSnakeCase());
        assertEquals(false, WordStudy.of("123").isSnakeCase());
        assertEquals(false, WordStudy.of("a").isSnakeCase());
        assertEquals(false, WordStudy.of("A").isSnakeCase());
        assertEquals(false, WordStudy.of("_A").isSnakeCase());
        assertEquals(false, WordStudy.of("_123A").isSnakeCase());
        assertEquals(false, WordStudy.of(" _A").isSnakeCase());
    }

    @Test
    public void test_isCamelCase() throws Exception {
        assertEquals(true, WordStudy.of("aA").isCamelCase());
        assertEquals(true, WordStudy.of("Aa").isCamelCase());
        assertEquals(true, WordStudy.of("123aA").isCamelCase());
        assertEquals(true, WordStudy.of("123Aa").isCamelCase());
        assertEquals(false, WordStudy.of("123a").isCamelCase());
        assertEquals(false, WordStudy.of("A").isCamelCase());
        assertEquals(false, WordStudy.of("_a").isCamelCase());
        assertEquals(false, WordStudy.of("_123a").isCamelCase());
        assertEquals(false, WordStudy.of("_123").isCamelCase());
        assertEquals(false, WordStudy.of("123").isCamelCase());
        assertEquals(false, WordStudy.of("a").isCamelCase());
        assertEquals(false, WordStudy.of("A").isCamelCase());
        assertEquals(false, WordStudy.of("_A").isCamelCase());
        assertEquals(false, WordStudy.of("_123A").isCamelCase());
        assertEquals(false, WordStudy.of("_A").isCamelCase());
    }

    @Test
    public void test_isProperCamelCase() throws Exception {
        assertEquals(true, WordStudy.of("aA").isProperCamelCase());
        assertEquals(false, WordStudy.of("123aA").isProperCamelCase());
        assertEquals(false, WordStudy.of("123Aa").isProperCamelCase());
        assertEquals(false, WordStudy.of("Aa").isProperCamelCase());
        assertEquals(false, WordStudy.of("123a").isProperCamelCase());
        assertEquals(false, WordStudy.of("A").isProperCamelCase());
        assertEquals(false, WordStudy.of("_a").isProperCamelCase());
        assertEquals(false, WordStudy.of("_123a").isProperCamelCase());
        assertEquals(false, WordStudy.of("_123").isProperCamelCase());
        assertEquals(false, WordStudy.of("123").isProperCamelCase());
        assertEquals(false, WordStudy.of("a").isProperCamelCase());
        assertEquals(false, WordStudy.of("A").isProperCamelCase());
        assertEquals(false, WordStudy.of("_A").isProperCamelCase());
        assertEquals(false, WordStudy.of("_123A").isProperCamelCase());
        assertEquals(false, WordStudy.of("_A").isProperCamelCase());
    }

    @Test
    public void test_isPascalCase() throws Exception {
        assertEquals(true, WordStudy.of("Aa").isPascalCase());
        assertEquals(false, WordStudy.of("aA").isPascalCase());
        assertEquals(false, WordStudy.of("123aA").isPascalCase());
        assertEquals(false, WordStudy.of("123Aa").isPascalCase());
        assertEquals(false, WordStudy.of("123a").isPascalCase());
        assertEquals(false, WordStudy.of("A").isPascalCase());
        assertEquals(false, WordStudy.of("_a").isPascalCase());
        assertEquals(false, WordStudy.of("_123a").isPascalCase());
        assertEquals(false, WordStudy.of("_123").isPascalCase());
        assertEquals(false, WordStudy.of("123").isPascalCase());
        assertEquals(false, WordStudy.of("a").isPascalCase());
        assertEquals(false, WordStudy.of("A").isPascalCase());
        assertEquals(false, WordStudy.of("_A").isPascalCase());
        assertEquals(false, WordStudy.of("_123A").isPascalCase());
        assertEquals(false, WordStudy.of("_A").isPascalCase());
    }

    @Test
    public void test_canBeScreamingSnakeCase() throws Exception {
        assertEquals(true, WordStudy.of("a_").canBeScreamingSnakeCase());
        assertEquals(true, WordStudy.of("_a").canBeScreamingSnakeCase());
        assertEquals(true, WordStudy.of("aB").canBeScreamingSnakeCase());
        assertEquals(true, WordStudy.of("BaA").canBeScreamingSnakeCase());
        assertEquals(false, WordStudy.of("Ba").canBeScreamingSnakeCase());
        assertEquals(false, WordStudy.of("a").canBeScreamingSnakeCase());
        assertEquals(false, WordStudy.of("A").canBeScreamingSnakeCase());
        assertEquals(false, WordStudy.of("A_").canBeScreamingSnakeCase());
        assertEquals(false, WordStudy.of("123_").canBeScreamingSnakeCase());
        assertEquals(false, WordStudy.of("123").canBeScreamingSnakeCase());
    }

    @Test
    public void test_canBeSnakeCase() throws Exception {
        assertEquals(false, WordStudy.of("a_").canBeSnakeCase());
        assertEquals(false, WordStudy.of("_a").canBeSnakeCase());
        assertEquals(true, WordStudy.of("aB").canBeSnakeCase());
        assertEquals(true, WordStudy.of("BaA").canBeSnakeCase());
        assertEquals(false, WordStudy.of("Ba").canBeSnakeCase());
        assertEquals(false, WordStudy.of("a").canBeSnakeCase());
        assertEquals(false, WordStudy.of("A").canBeSnakeCase());
        assertEquals(true, WordStudy.of("A_").canBeSnakeCase());
        assertEquals(false, WordStudy.of("123_").canBeSnakeCase());
        assertEquals(false, WordStudy.of("123").canBeSnakeCase());
    }

    @Test
    public void test_canBeCamelCase() throws Exception {
        assertEquals(true, WordStudy.of("a_a").canBeCamelCase());
        assertEquals(true, WordStudy.of("a_A").canBeCamelCase());
        assertEquals(true, WordStudy.of("A_a").canBeCamelCase());
        assertEquals(true, WordStudy.of("A_A").canBeCamelCase());
        assertEquals(true, WordStudy.of("A_").canBeCamelCase());
        assertEquals(true, WordStudy.of("_A").canBeCamelCase());
        assertEquals(false, WordStudy.of("aB").canBeCamelCase());
        assertEquals(false, WordStudy.of("Ba").canBeCamelCase());
        assertEquals(false, WordStudy.of("a").canBeCamelCase());
        assertEquals(false, WordStudy.of("A").canBeCamelCase());
        assertEquals(true, WordStudy.of("a_").canBeCamelCase());
        assertEquals(false, WordStudy.of("123_").canBeCamelCase());
        assertEquals(false, WordStudy.of("123").canBeCamelCase());
    }

    @Test
    public void test_canBeProperCamelCase() throws Exception {
        assertEquals(true, WordStudy.of("a_a").canBeProperCamelCase());
        assertEquals(true, WordStudy.of("a_A").canBeProperCamelCase());
        assertEquals(true, WordStudy.of("A_a").canBeProperCamelCase());
        assertEquals(true, WordStudy.of("A_A").canBeProperCamelCase());
        assertEquals(true, WordStudy.of("Abc").canBeProperCamelCase());
        assertEquals(true, WordStudy.of("A_").canBeProperCamelCase());
        assertEquals(true, WordStudy.of("_A").canBeProperCamelCase());
        assertEquals(false, WordStudy.of("aB").canBeProperCamelCase());
        assertEquals(true, WordStudy.of("Ba").canBeProperCamelCase());
        assertEquals(false, WordStudy.of("a").canBeProperCamelCase());
        assertEquals(true, WordStudy.of("A").canBeProperCamelCase());
        assertEquals(true, WordStudy.of("a_").canBeProperCamelCase());
        assertEquals(false, WordStudy.of("123_").canBeProperCamelCase());
        assertEquals(false, WordStudy.of("123").canBeProperCamelCase());
    }

    @Test
    public void test_canBePascalCase() throws Exception {
        assertEquals(false, WordStudy.of("a_aa").canBePascalCase());
        assertEquals(true, WordStudy.of("aa_a").canBePascalCase());
        assertEquals(true, WordStudy.of("aa_aa").canBePascalCase());
        assertEquals(false, WordStudy.of("A_AA").canBePascalCase());
        assertEquals(true, WordStudy.of("AA_A").canBePascalCase());
        assertEquals(true, WordStudy.of("AA_AA").canBePascalCase());
        assertEquals(false, WordStudy.of("A_aa").canBePascalCase());
        assertEquals(true, WordStudy.of("Aa_a").canBePascalCase());
        assertEquals(true, WordStudy.of("Aa_aa").canBePascalCase());
        assertEquals(false, WordStudy.of("aBc").canBePascalCase());
        assertEquals(false, WordStudy.of("a_Aa").canBePascalCase());
        assertEquals(true, WordStudy.of("aa_A").canBePascalCase());
        assertEquals(true, WordStudy.of("aa_Aa").canBePascalCase());
        assertEquals(true, WordStudy.of("aa_aA").canBePascalCase());
        assertEquals(false, WordStudy.of("aBc").canBePascalCase());
        assertEquals(false, WordStudy.of("a_a").canBePascalCase());
        assertEquals(false, WordStudy.of("a_A").canBePascalCase());
        assertEquals(false, WordStudy.of("A_a").canBePascalCase());
        assertEquals(false, WordStudy.of("A_A").canBePascalCase());
        assertEquals(false, WordStudy.of("A_").canBePascalCase());
        assertEquals(false, WordStudy.of("_A").canBePascalCase());
        assertEquals(false, WordStudy.of("aB").canBePascalCase());
        assertEquals(false, WordStudy.of("Ba").canBePascalCase());
        assertEquals(false, WordStudy.of("a").canBePascalCase());
        assertEquals(false, WordStudy.of("A").canBePascalCase());
        assertEquals(false, WordStudy.of("a_").canBePascalCase());
        assertEquals(false, WordStudy.of("123_").canBePascalCase());
        assertEquals(false, WordStudy.of("123").canBePascalCase());
    }

    @Test
    public void test_makeMixedSnakeCase() throws Exception {
        assertEquals("a_B", WordStudy.of("aB").makeMixedSnakeCase());
        assertEquals("a_Bc", WordStudy.of("aBc").makeMixedSnakeCase());
        assertEquals("ABC", WordStudy.of("ABC").makeMixedSnakeCase());
        assertEquals("abc_Def_Hij", WordStudy.of("abcDefHij").makeMixedSnakeCase());
        assertEquals("Abc_Def_Hij", WordStudy.of("AbcDefHij").makeMixedSnakeCase());
    }

    @Test
    public void test_makeCamelCase() throws Exception {
        assertEquals("abcDef", WordStudy.of("abcDef").makeCamelCase());
        assertEquals("AbcDef", WordStudy.of("AbcDef").makeCamelCase());
        assertEquals("abcDef", WordStudy.of("Abc_Def").makeCamelCase());
        assertEquals("abcDef", WordStudy.of("abc_def").makeCamelCase());
        assertEquals("abcDef", WordStudy.of("ABC_DEF").makeCamelCase());
    }

    @Test
    public void test_makeScreamingSnakeCase() throws Exception {
        assertEquals("ABC_DEF", WordStudy.of("abcDef").makeScreamingSnakeCase());
        assertEquals("ABC_DEF", WordStudy.of("AbcDef").makeScreamingSnakeCase());
        assertEquals("ABC_DEF", WordStudy.of("Abc_Def").makeScreamingSnakeCase());
        assertEquals("ABC_DEF", WordStudy.of("abc_def").makeScreamingSnakeCase());
        assertEquals("ABC_DEF", WordStudy.of("ABC_DEF").makeScreamingSnakeCase());
    }

    @Test
    public void test_makeSnakeCase() throws Exception {
        assertEquals("abc_def", WordStudy.of("abcDef").makeSnakeCase());
        assertEquals("abc_def", WordStudy.of("AbcDef").makeSnakeCase());
        assertEquals("abc_def", WordStudy.of("Abc_Def").makeSnakeCase());
        assertEquals("abc_def", WordStudy.of("abc_def").makeSnakeCase());
        assertEquals("abc_def", WordStudy.of("ABC_DEF").makeSnakeCase());
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
