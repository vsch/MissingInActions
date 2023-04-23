// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StudiedWordTest {
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
