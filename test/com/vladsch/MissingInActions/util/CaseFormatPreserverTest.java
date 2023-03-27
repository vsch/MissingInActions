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

import com.intellij.openapi.util.TextRange;
import com.vladsch.MissingInActions.settings.PrefixOnPastePatternType;
import com.vladsch.MissingInActions.settings.SuffixOnPastePatternType;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.plugin.util.StudiedWord;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CaseFormatPreserverTest {
    private static final String[] prefixes = new String[] { "my", "our", "is", "get", "set" };
    private static final String[] suffixes = new String[] { "_P" };
    private static final String[] regexPrefixes = new String[] { "^(?:my|our|is|get|set)(?=[A-Z])" };
    private static final String[] regexSuffixes = new String[] { "(?:_P)$" };
    private static final String[] regexPhpPrefixes = new String[] { "^(?:my|our|is|get|set)(?=[A-Z])|^\\$" };

    @SuppressWarnings("SameParameterValue")
    private String preserved(
            String template,
            String pasted,
            boolean camelCase,
            boolean snakeCase,
            boolean screamingSnakeCase,
            boolean dashCase,
            boolean dotCase,
            boolean slashCase,
            final boolean removePrefix,
            boolean addPrefix,
            @Nullable PrefixOnPastePatternType prefixPatternType,
            String[] prefixes,
            @Nullable SuffixOnPastePatternType suffixPatternType,
            String[] suffixes
    ) {
        // template: [ ] marks the selection range, | marks caret position
        // pasted is what is pasted,
        // prefix1 & prefix2 prefixes to remove
        // camelCase, snakeCase and screamingSnakeCase are options
        int offset = template.indexOf("|");
        int start = template.indexOf("[");
        int end = template.indexOf("]");

        assert offset != -1;
        assert start <= end;
        assert start == -1 && end == -1 || start != -1 && end != -1;

        if (start != -1) {
            // need to remove it
            template = template.substring(0, start) + template.substring(start + 1);
            if (offset > start) offset--;
            if (end > start) end--;

            template = template.substring(0, offset) + template.substring(offset + 1);
            if (start > offset) start--;
            if (end > offset) end--;

            template = template.substring(0, end) + template.substring(end + 1);
            if (offset > end) offset--;
        } else {
            template = template.substring(0, offset) + template.substring(offset + 1);
            start = offset;
            end = offset;
        }

        CaseFormatPreserver preserver = new CaseFormatPreserver();
        final BasedSequence chars = BasedSequence.of(template);
        int separators = CaseFormatPreserver.separators(
                camelCase,
                snakeCase,
                screamingSnakeCase,
                dashCase,
                dotCase,
                slashCase
        );
        preserver.studyFormatBefore(chars, offset, start, end, prefixPatternType, prefixes, suffixPatternType, suffixes, separators);
        String edited = template.substring(0, start) + pasted + template.substring(end);
        final TextRange range = new TextRange(start, start + pasted.length());
        final BasedSequence chars1 = BasedSequence.of(edited);
        InsertedRangeContext i = preserver.preserveFormatAfter(
                chars1,
                range,
                camelCase,
                snakeCase,
                screamingSnakeCase,
                dashCase,
                dotCase,
                slashCase,
                removePrefix,
                addPrefix,
                prefixPatternType,
                prefixes,
                suffixPatternType,
                suffixes
        );

        String result = i == null ? edited : edited.substring(0, start) + i.word() + edited.substring(start + pasted.length() - i.getCaretDelta());
        return result;
    }

    @Test
    public void test_Basic() throws Exception {
        final PrefixOnPastePatternType prefixPatternType = PrefixOnPastePatternType.CAMEL;
        final SuffixOnPastePatternType suffixPatternType = SuffixOnPastePatternType.ANY;
        String s;

        s = preserved("   int |\n", "myName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int myName\n", s);

        s = preserved("   int | abc\n", "myName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int myName abc\n", s);

        s = preserved("   int |abc\n", "myName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int nameAbc\n", s);

        s = preserved("   int a|bc\n", "myName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int aNameBc\n", s);

        s = preserved("   int ab|c\n", "myName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int abNameC\n", s);

        s = preserved("   int abc|\n", "myName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int abcName\n", s);

        s = preserved("   int [abc]|\n", "myName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int name\n", s);

        s = preserved("   int |[abc]\n", "myName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int name\n", s);

        s = preserved("   int [abcDef]|\n", "myNameAnd", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int nameAnd\n", s);

        s = preserved("   int |[abcDef]\n", "myNameAnd", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int nameAnd\n", s);

        s = preserved("   int abc |\n", "myName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int abc myName\n", s);

        s = preserved("   int [new WordStudy]|(\n", "WordStudy.of", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int WordStudy.of(\n", s);

        s = preserved("  [int]| WordStudy(\n", "myCaret", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("  caret WordStudy(\n", s);

        s = preserved("[WORK_PLAY]|(\n", "myWordStudy", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("WORD_STUDY(\n", s);

        s = preserved("[work_play]|(\n", "myWordStudy", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("word_study(\n", s);

        s = preserved("static [void]| duplicateLine\n", "Couple<Integer> ", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("static Couple<Integer>  duplicateLine\n", s);

        s = preserved("  [Class]| myManager;\n", "myManager", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("  Manager myManager;\n", s);

        s = preserved("  private boolean myRemovePrefixOnPasteType = [false]|;\n", "myRemovePrefixOnPasteType", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("  private boolean myRemovePrefixOnPasteType = removePrefixOnPasteType;\n", s);

        s = preserved("FLAGS[_SOME_NAME]|\n", "myClassMemberName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("FLAGS_CLASS_MEMBER_NAME\n", s);

        s = preserved("flags[_some_name]|\n", "myClassMemberName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("flags_class_member_name\n", s);

        s = preserved("[myClassMemberName]|\n", "myClassMemberName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("myClassMemberName\n", s);

        s = preserved("boolean [myClassMemberName]|\n", "disableGifImages", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("boolean myDisableGifImages\n", s);

        s = preserved("boolean [ourClassMemberName]|\n", "disableGifImages", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("boolean ourDisableGifImages\n", s);

        s = preserved(
                "editor.putUserData([LAST_PASTED_CLIPBOARD_CONTEXT]|, clipboardCaretContent)\n",
                "LastPastedClipboardCarets",
                true,
                true,
                true,
                true,
                true,
                true,
                true, true,
                prefixPatternType,
                prefixes,
                suffixPatternType,
                suffixes);
        assertEquals("editor.putUserData(LAST_PASTED_CLIPBOARD_CARETS, clipboardCaretContent)\n", s);

        s = preserved(
                "editor.putUserData(|[LAST_PASTED_CLIPBOARD_CONTEXT], clipboardCaretContent)\n",
                "LastPastedClipboardCarets",
                true,
                true,
                true,
                true,
                true,
                true,
                true, true,
                prefixPatternType,
                prefixes,
                suffixPatternType,
                suffixes);
        assertEquals("editor.putUserData(LAST_PASTED_CLIPBOARD_CARETS, clipboardCaretContent)\n", s);

        s = preserved("       [CamelCase]|\n", "myLastSelectionMarker", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("       LastSelectionMarker\n", s);

        s = preserved("editor.[getTestString]|()\n", "myReplacement", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("editor.getReplacement()\n", s);

        s = preserved("editor.|appendIf()\n", "test_", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("editor.test_appendIf()\n", s);

        s = preserved("appendIf|()\n", "_test", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("appendIf_test()\n", s);

        s = preserved(" |appendIf()\n", "test_", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals(" test_appendIf()\n", s);

        s = preserved("|appendIf()\n", "test_", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("test_appendIf()\n", s);

        s = preserved("appendIf| \n", "_test", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("appendIf_test \n", s);

        s = preserved("[IS_SORTED]| \n", "isFlat", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("IS_FLAT \n", s);

        s = preserved("get[IsSorted]|\n", "isFlat", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("getIsFlat\n", s);

        s = preserved("[sorted]|\n", "FLAT", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("flat\n", s);

        s = preserved("[SORTED]|\n", "flat", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("FLAT\n", s);

        s = preserved("[SCREAMING_SNAKE]|\n", "dash-case-name", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("DASH_CASE_NAME\n", s);

        s = preserved("[dash-case-name]|\n", "SCREAMING_SNAKE", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("screaming-snake\n", s);

        s = preserved("boolean [isTest]|() { return this == NONE; }\n", "isParsed", true, true, true, true, true, true, true, true, prefixPatternType, regexPhpPrefixes, suffixPatternType, suffixes);
        assertEquals("boolean isParsed() { return this == NONE; }\n", s);

        s = preserved("[name.0.parts]|", "nameTestParts", true, true, true, true, true, true, true, true, prefixPatternType, regexPhpPrefixes, suffixPatternType, suffixes);
        assertEquals("name.test.parts", s);

        s = preserved("[name.with.parts]|", "nameTestParts", true, true, true, true, true, true, true, true, prefixPatternType, regexPhpPrefixes, suffixPatternType, suffixes);
        assertEquals("name.test.parts", s);

        s = preserved("boolean [settingsExtension]|\n", "projectSettingsExtension", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("boolean projectSettingsExtension\n", s);

        s = preserved("Test.[Toggle_case_word]|\n", "ToggleSmartSelect", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("Test.Toggle_smart_select\n", s);

        s = preserved("Test.[getIsTask]()|\n", "isStart", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("Test.getIsStart()\n", s);

        s = preserved("Test.[isTask]()|\n", "myIsStart", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("Test.isStart()\n", s);
        
        s = preserved("Test.[isTask]_P()|\n", "myIsStart", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("Test.isStart_P()\n", s);
    }

    @Test
    public void test_Basic2() throws Exception {
        final PrefixOnPastePatternType patternType = PrefixOnPastePatternType.CAMEL;
        String s;
    }

    @Test
    public void test_Regex2() throws Exception {
        final String[] prefixes = regexPrefixes;
        final PrefixOnPastePatternType patternType = PrefixOnPastePatternType.REGEX;
        String s;
    }

    @Test
    public void test_Regex() throws Exception {
        final String[] prefixes = regexPrefixes;
        final String[] suffixes = regexSuffixes;
        final PrefixOnPastePatternType prefixPatternType = PrefixOnPastePatternType.REGEX;
        final SuffixOnPastePatternType suffixPatternType = SuffixOnPastePatternType.REGEX;
        String s;

        s = preserved("   int |\n", "myName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int myName\n", s);

        s = preserved("   int | abc\n", "myName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int myName abc\n", s);

        s = preserved("   int |abc\n", "myName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int nameAbc\n", s);

        s = preserved("   int a|bc\n", "myName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int aNameBc\n", s);

        s = preserved("   int ab|c\n", "myName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int abNameC\n", s);

        s = preserved("   int abc|\n", "myName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int abcName\n", s);

        s = preserved("   int [abc]|\n", "myName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int name\n", s);

        s = preserved("   int |[abc]\n", "myName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int name\n", s);

        s = preserved("   int abc |\n", "myName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int abc myName\n", s);

        s = preserved("   int [new WordStudy]|(\n", "WordStudy.of", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("   int WordStudy.of(\n", s);

        s = preserved("  [int]| WordStudy(\n", "myCaret", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("  caret WordStudy(\n", s);

        s = preserved("[WORK_PLAY]|(\n", "myWordStudy", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("WORD_STUDY(\n", s);

        s = preserved("[work_play]|(\n", "myWordStudy", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("word_study(\n", s);

        s = preserved("static [void]| duplicateLine\n", "Couple<Integer> ", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("static Couple<Integer>  duplicateLine\n", s);

        s = preserved("  [Class]| myManager;\n", "myManager", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("  Manager myManager;\n", s);

        s = preserved("  private boolean myRemovePrefixOnPasteType = [false]|;\n", "myRemovePrefixOnPasteType", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("  private boolean myRemovePrefixOnPasteType = removePrefixOnPasteType;\n", s);

        s = preserved("FLAGS[_SOME_NAME]|\n", "myClassMemberName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("FLAGS_CLASS_MEMBER_NAME\n", s);

        s = preserved("flags[_some_name]|\n", "myClassMemberName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("flags_class_member_name\n", s);

        s = preserved("[myClassMemberName]|\n", "myClassMemberName", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("myClassMemberName\n", s);

        s = preserved("boolean [myClassMemberName]|\n", "disableGifImages", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("boolean myDisableGifImages\n", s);

        s = preserved("boolean [ourClassMemberName]|\n", "disableGifImages", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("boolean ourDisableGifImages\n", s);

        s = preserved("Test.[Toggle_case_word]|\n", "ToggleSmartSelect", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("Test.Toggle_smart_select\n", s);

        s = preserved(
                "editor.putUserData([LAST_PASTED_CLIPBOARD_CONTEXT]|, clipboardCaretContent)\n",
                "LastPastedClipboardCarets",
                true,
                true,
                true,
                true,
                true,
                true,
                true, true,
                prefixPatternType,
                prefixes,
                suffixPatternType, suffixes);
        assertEquals("editor.putUserData(LAST_PASTED_CLIPBOARD_CARETS, clipboardCaretContent)\n", s);

        s = preserved(
                "editor.putUserData(|[LAST_PASTED_CLIPBOARD_CONTEXT], clipboardCaretContent)\n",
                "LastPastedClipboardCarets",
                true,
                true,
                true,
                true,
                true,
                true,
                true, true,
                prefixPatternType,
                prefixes,
                suffixPatternType, suffixes);
        assertEquals("editor.putUserData(LAST_PASTED_CLIPBOARD_CARETS, clipboardCaretContent)\n", s);

        s = preserved("editor.[getTestString]|()\n", "myReplacement", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("editor.getReplacement()\n", s);

        s = preserved("editor.|appendIf()\n", "test_", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("editor.test_appendIf()\n", s);

        s = preserved("appendIf|()\n", "_test", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("appendIf_test()\n", s);

        s = preserved(" |appendIf()\n", "test_", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals(" test_appendIf()\n", s);

        s = preserved("|appendIf()\n", "test_", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("test_appendIf()\n", s);

        s = preserved("appendIf| \n", "_test", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("appendIf_test \n", s);

        s = preserved("[IS_SORTED]| \n", "isFlat", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("IS_FLAT \n", s);

        s = preserved("get[IsSorted]|\n", "isFlat", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("getIsFlat\n", s);

        s = preserved("[sorted]|\n", "FLAT", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("flat\n", s);

        s = preserved("[SORTED]|\n", "flat", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("FLAT\n", s);

        s = preserved("[SCREAMING_SNAKE]|\n", "dash-case-name", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("DASH_CASE_NAME\n", s);

        s = preserved("[dash-case-name]|\n", "SCREAMING_SNAKE", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("screaming-snake\n", s);

        s = preserved("[$oldPhpName]|\n", "newPhpName", true, true, true, true, true, true, true, true, prefixPatternType, regexPhpPrefixes, suffixPatternType, suffixes);
        assertEquals("$newPhpName\n", s);

        s = preserved("[oldPhpName]|\n", "$newPhpName", true, true, true, true, true, true, false, true, prefixPatternType, regexPhpPrefixes, suffixPatternType, suffixes);
        assertEquals("$newPhpName\n", s);

        s = preserved("[oldPhpName]|\n", "$newPhpName", true, true, true, true, true, true, true, true, prefixPatternType, regexPhpPrefixes, suffixPatternType, suffixes);
        assertEquals("newPhpName\n", s);

        s = preserved("[name.with.parts]|", "nameTestParts", true, true, true, true, true, true, true, true, prefixPatternType, regexPhpPrefixes, suffixPatternType, suffixes);
        assertEquals("name.test.parts", s);

        s = preserved("boolean [settingsExtension]|\n", "projectSettingsExtension", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("boolean projectSettingsExtension\n", s);

        s = preserved("Test.[Toggle_case_word]|\n", "ToggleSmartSelect", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("Test.Toggle_smart_select\n", s);

        s = preserved("Test.[getIsTask]()|\n", "isStart", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("Test.getIsStart()\n", s);

        s = preserved("Test.[isTask]()|\n", "myIsStart", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("Test.isStart()\n", s);
        
        s = preserved("Test.[isTask]_P()|\n", "myIsStart", true, true, true, true, true, true, true, true, prefixPatternType, prefixes, suffixPatternType, suffixes);
        assertEquals("Test.isStart_P()\n", s);
    }

    @Test
    public void table_test() throws Exception {
        boolean allMatch = true;
        for (int i = 0; i < 256; i++) {
            if (StudiedWord.compute((char) i) != StudiedWord.flags((char) i)) {
                allMatch = false;
                break;
            }
        }

        if (!allMatch) {
            System.out.print("int[] ascii = new int[] {");
            for (int i = 0; i < 256; i++) {
                if (i % 16 == 0) System.out.print("\n    ");
                int flags = StudiedWord.compute((char) i);
                System.out.print(String.format("0x%04x, ", flags));
            }
            System.out.print("\n};\n");
        }

        assertTrue(allMatch);
    }
}
