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
import com.vladsch.flexmark.util.sequence.SubSequence;
import com.vladsch.flexmark.util.sequence.Substring;
import org.junit.Test;

import static org.junit.Assert.*;

public class CaseFormatPreserverTest {

    String preserved(String template, String pasted, String prefix1, String prefix2, boolean camelCase, boolean snakeCase, boolean screamingSnakeCase) {
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
        preserver.studyFormatBefore(chars, offset, start, end, prefix1, prefix2);
        String edited = template.substring(0, start) + pasted + template.substring(end);
        final TextRange range = new TextRange(start, start + pasted.length());
        String ranged = range.substring(edited);
        final SubSequence chars1 = new SubSequence(Substring.of(edited));
        InsertedRangeContext i = preserver.preserveFormatAfter(chars1, range, camelCase, snakeCase, screamingSnakeCase, prefix1, prefix2);

        String result = i == null ? edited : edited.substring(0, start) + i.word() + edited.substring(start + pasted.length() - i.getCaretDelta());
        return result;
    }

    @Test
    public void test_Basic() throws Exception {
        String s = preserved("   int |\n", "myName", "my", "our", true, true, true);
        assertEquals("   int myName\n", s);

        s = preserved("   int | abc\n", "myName", "my", "our", true, true, true);
        assertEquals("   int myName abc\n", s);

        s = preserved("   int |abc\n", "myName", "my", "our", true, true, true);
        assertEquals("   int nameAbc\n", s);

        s = preserved("   int a|bc\n", "myName", "my", "our", true, true, true);
        assertEquals("   int aNameBc\n", s);

        s = preserved("   int ab|c\n", "myName", "my", "our", true, true, true);
        assertEquals("   int abNameC\n", s);

        s = preserved("   int abc|\n", "myName", "my", "our", true, true, true);
        assertEquals("   int abcName\n", s);

        s = preserved("   int abc |\n", "myName", "my", "our", true, true, true);
        assertEquals("   int abc myName\n", s);

        s = preserved("   int [new WordStudy]|(\n", "WordStudy.of", "my", "our", true, true, true);
        assertEquals("   int WordStudy.of(\n", s);

        s = preserved("[WORK_PLAY]|(\n", "myWordStudy", "my", "our", true, true, true);
        assertEquals("WORD_STUDY(\n", s);

        s = preserved("[work_play]|(\n", "myWordStudy", "my", "our", true, true, true);
        assertEquals("word_study(\n", s);
    }

    //@Test
    public void fake_test() throws Exception {
        System.out.print("int[] ascii = new int[] {");
        for (int i = 0; i < 256; i++) {
            if (i % 16 == 0) System.out.print("\n    ");
            int flags = WordStudy.flags((char)i);
            System.out.print(String.format("0x%04x, ",flags));
        }
        System.out.print("\n};\n");
    }
}
