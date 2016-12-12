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
import com.vladsch.flexmark.util.sequence.SubSequence;
import com.vladsch.flexmark.util.sequence.Substring;
import org.junit.Test;

import static org.junit.Assert.*;

public class CaseFormatPreserverTest {

    String preserved(String template, String pasted, String prefix1, String prefix2, boolean camelCase, boolean snakeCase, boolean screamingSnakeCase, boolean addPrefix) {
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
        final SubSequence chars = new SubSequence(Substring.of(template));
        preserver.studyFormatBefore(chars, offset, start, end, prefix1, prefix2, null);
        String edited = template.substring(0, start) + pasted + template.substring(end);
        final TextRange range = new TextRange(start, start + pasted.length());
        String ranged = range.substring(edited);
        final SubSequence chars1 = new SubSequence(Substring.of(edited));
        InsertedRangeContext i = preserver.preserveFormatAfter(chars1, range, camelCase, snakeCase, screamingSnakeCase, prefix1, prefix2, null, addPrefix);

        String result = i == null ? edited : edited.substring(0, start) + i.word() + edited.substring(start + pasted.length() - i.getCaretDelta());
        return result;
    }

    String preservedRegex(String template, String pasted, String prefix1, String prefix2, boolean camelCase, boolean snakeCase, boolean screamingSnakeCase, boolean addPrefix) {
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

        String regexPrefix1 = "^\\Q" + prefix1 + "\\E";
        String regexPrefix2 = "^\\Q" + prefix2 + "\\E";
        CaseFormatPreserver preserver = new CaseFormatPreserver();
        final SubSequence chars = new SubSequence(Substring.of(template));
        final RemovePrefixOnPasteType type = RemovePrefixOnPasteType.REGEX;
        preserver.studyFormatBefore(chars, offset, start, end, regexPrefix1, regexPrefix2, type);
        String edited = template.substring(0, start) + pasted + template.substring(end);
        final TextRange range = new TextRange(start, start + pasted.length());
        String ranged = range.substring(edited);
        final SubSequence chars1 = new SubSequence(Substring.of(edited));

        InsertedRangeContext i = preserver.preserveFormatAfter(chars1, range, camelCase, snakeCase, screamingSnakeCase, regexPrefix1, regexPrefix2, type, addPrefix);

        String result = i == null ? edited : edited.substring(0, start) + i.word() + edited.substring(start + pasted.length() - i.getCaretDelta());
        return result;
    }

    @Test
    public void test_Basic() throws Exception {
        String s = preserved("   int |\n", "myName", "my", "our", true, true, true, true);
        assertEquals("   int myName\n", s);

        s = preserved("   int | abc\n", "myName", "my", "our", true, true, true, true);
        assertEquals("   int myName abc\n", s);

        s = preserved("   int |abc\n", "myName", "my", "our", true, true, true, true);
        assertEquals("   int nameAbc\n", s);

        s = preserved("   int a|bc\n", "myName", "my", "our", true, true, true, true);
        assertEquals("   int aNameBc\n", s);

        s = preserved("   int ab|c\n", "myName", "my", "our", true, true, true, true);
        assertEquals("   int abNameC\n", s);

        s = preserved("   int abc|\n", "myName", "my", "our", true, true, true, true);
        assertEquals("   int abcName\n", s);

        s = preserved("   int [abc]|\n", "myName", "my", "our", true, true, true, true);
        assertEquals("   int name\n", s);

        s = preserved("   int |[abc]\n", "myName", "my", "our", true, true, true, true);
        assertEquals("   int name\n", s);

        s = preserved("   int [abcDef]|\n", "myNameAnd", "my", "our", true, true, true, true);
        assertEquals("   int nameAnd\n", s);

        s = preserved("   int |[abcDef]\n", "myNameAnd", "my", "our", true, true, true, true);
        assertEquals("   int nameAnd\n", s);

        s = preserved("   int abc |\n", "myName", "my", "our", true, true, true, true);
        assertEquals("   int abc myName\n", s);

        s = preserved("   int [new WordStudy]|(\n", "WordStudy.of", "my", "our", true, true, true, true);
        assertEquals("   int WordStudy.of(\n", s);

        s = preserved("  [int]| WordStudy(\n", "myCaret", "my", "our", true, true, true, true);
        assertEquals("  caret WordStudy(\n", s);

        s = preserved("[WORK_PLAY]|(\n", "myWordStudy", "my", "our", true, true, true, true);
        assertEquals("WORD_STUDY(\n", s);

        s = preserved("[work_play]|(\n", "myWordStudy", "my", "our", true, true, true, true);
        assertEquals("word_study(\n", s);

        s = preserved("static [void]| duplicateLine\n", "Couple<Integer> ", "my", "our", true, true, true, true);
        assertEquals("static Couple<Integer>  duplicateLine\n", s);

        s = preserved("  [Class]| myManager;\n", "myManager", "my", "our", true, true, true, true);
        assertEquals("  Manager myManager;\n", s);

        s = preserved("  private boolean myRemovePrefixOnPasteType = [false]|;\n", "myRemovePrefixOnPasteType", "my", "our", true, true, true, true);
        assertEquals("  private boolean myRemovePrefixOnPasteType = removePrefixOnPasteType;\n", s);

        s = preserved("FLAGS[_SOME_NAME]|\n", "myClassMemberName", "my", "our", true, true, true, true);
        assertEquals("FLAGS_CLASS_MEMBER_NAME\n", s);

        s = preserved("flags[_some_name]|\n", "myClassMemberName", "my", "our", true, true, true, true);
        assertEquals("flags_class_member_name\n", s);

        s = preserved("[myClassMemberName]|\n", "myClassMemberName", "my", "our", true, true, true, true);
        assertEquals("myClassMemberName\n", s);

        s = preserved("boolean [myClassMemberName]|\n", "disableGifImages", "my", "our", true, true, true, true);
        assertEquals("boolean myDisableGifImages\n", s);

        s = preserved("boolean [ourClassMemberName]|\n", "disableGifImages", "my", "our", true, true, true, true);
        assertEquals("boolean ourDisableGifImages\n", s);

        s = preserved("editor.putUserData([LAST_PASTED_CLIPBOARD_CONTEXT]|, clipboardCaretContent)\n", "LastPastedClipboardCarets", "my", "our", true, true, true, true);
        assertEquals("editor.putUserData(LAST_PASTED_CLIPBOARD_CARETS, clipboardCaretContent)\n", s);

        s = preserved("editor.putUserData(|[LAST_PASTED_CLIPBOARD_CONTEXT], clipboardCaretContent)\n", "LastPastedClipboardCarets", "my", "our", true, true, true, true);
        assertEquals("editor.putUserData(LAST_PASTED_CLIPBOARD_CARETS, clipboardCaretContent)\n", s);

        s = preserved("       [CamelCase]|\n", "myLastSelectionMarker", "my", "our", true, true, true, true);
        assertEquals("       LastSelectionMarker\n", s);
    }

    @Test
    public void test_Regex() throws Exception {
        String s = preservedRegex("   int |\n", "myName", "my", "our", true, true, true, true);
        assertEquals("   int myName\n", s);

        s = preservedRegex("   int | abc\n", "myName", "my", "our", true, true, true, true);
        assertEquals("   int myName abc\n", s);

        s = preservedRegex("   int |abc\n", "myName", "my", "our", true, true, true, true);
        assertEquals("   int nameAbc\n", s);

        s = preservedRegex("   int a|bc\n", "myName", "my", "our", true, true, true, true);
        assertEquals("   int aNameBc\n", s);

        s = preservedRegex("   int ab|c\n", "myName", "my", "our", true, true, true, true);
        assertEquals("   int abNameC\n", s);

        s = preservedRegex("   int abc|\n", "myName", "my", "our", true, true, true, true);
        assertEquals("   int abcName\n", s);

        s = preservedRegex("   int [abc]|\n", "myName", "my", "our", true, true, true, true);
        assertEquals("   int name\n", s);

        s = preservedRegex("   int |[abc]\n", "myName", "my", "our", true, true, true, true);
        assertEquals("   int name\n", s);

        s = preservedRegex("   int abc |\n", "myName", "my", "our", true, true, true, true);
        assertEquals("   int abc myName\n", s);

        s = preservedRegex("   int [new WordStudy]|(\n", "WordStudy.of", "my", "our", true, true, true, true);
        assertEquals("   int WordStudy.of(\n", s);

        s = preservedRegex("  [int]| WordStudy(\n", "myCaret", "my", "our", true, true, true, true);
        assertEquals("  caret WordStudy(\n", s);

        s = preservedRegex("[WORK_PLAY]|(\n", "myWordStudy", "my", "our", true, true, true, true);
        assertEquals("WORD_STUDY(\n", s);

        s = preservedRegex("[work_play]|(\n", "myWordStudy", "my", "our", true, true, true, true);
        assertEquals("word_study(\n", s);

        s = preservedRegex("static [void]| duplicateLine\n", "Couple<Integer> ", "my", "our", true, true, true, true);
        assertEquals("static Couple<Integer>  duplicateLine\n", s);

        s = preservedRegex("  [Class]| myManager;\n", "myManager", "my", "our", true, true, true, true);
        assertEquals("  Manager myManager;\n", s);

        s = preservedRegex("  private boolean myRemovePrefixOnPasteType = [false]|;\n", "myRemovePrefixOnPasteType", "my", "our", true, true, true, true);
        assertEquals("  private boolean myRemovePrefixOnPasteType = removePrefixOnPasteType;\n", s);

        s = preservedRegex("FLAGS[_SOME_NAME]|\n", "myClassMemberName", "my", "our", true, true, true, true);
        assertEquals("FLAGS_CLASS_MEMBER_NAME\n", s);

        s = preservedRegex("flags[_some_name]|\n", "myClassMemberName", "my", "our", true, true, true, true);
        assertEquals("flags_class_member_name\n", s);

        s = preservedRegex("[myClassMemberName]|\n", "myClassMemberName", "my", "our", true, true, true, true);
        assertEquals("myClassMemberName\n", s);

        s = preservedRegex("boolean [myClassMemberName]|\n", "disableGifImages", "my", "our", true, true, true, true);
        assertEquals("boolean myDisableGifImages\n", s);

        s = preservedRegex("boolean [ourClassMemberName]|\n", "disableGifImages", "my", "our", true, true, true, true);
        assertEquals("boolean ourDisableGifImages\n", s);

        s = preservedRegex("editor.putUserData([LAST_PASTED_CLIPBOARD_CONTEXT]|, clipboardCaretContent)\n", "LastPastedClipboardCarets", "my", "our", true, true, true, true);
        assertEquals("editor.putUserData(LAST_PASTED_CLIPBOARD_CARETS, clipboardCaretContent)\n", s);

        s = preservedRegex("editor.putUserData(|[LAST_PASTED_CLIPBOARD_CONTEXT], clipboardCaretContent)\n", "LastPastedClipboardCarets", "my", "our", true, true, true, true);
        assertEquals("editor.putUserData(LAST_PASTED_CLIPBOARD_CARETS, clipboardCaretContent)\n", s);
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

        assertEquals(true, allMatch);
    }
}
