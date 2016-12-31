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

package com.vladsch.MissingInActions.settings;

import org.junit.Test;

import static com.vladsch.MissingInActions.settings.NumberSequenceGenerator.*;
import static org.junit.Assert.assertEquals;

public class NumberSequenceGeneratorTest {
    @Test
    public void test_templateNumber() throws Exception {
        assertEquals("0x00001234", templateNumber("1234", "0x", "00000000", "", "",0,""));
        assertEquals("00001234H", templateNumber("1234", "", "00000000", "", "",0,"H"));
        assertEquals("12.34f", templateNumber("1234", "", ".00", ".", "",0,"f"));
        assertEquals("12.34f", templateNumber("1234", "", "##.00", ".", "",0,"f"));
        assertEquals("1,234,567,890f", templateNumber("1234567890", "", "", ".", ",",3,"f"));
        assertEquals("12,345,678.90f", templateNumber("1234567890", "", ".##", ".", ",",3,"f"));
        assertEquals("0x0000_0012_3456_7890", templateNumber("1234567890", "0x", "0000000000000000", "", "_",4,""));
    }

    @Test
    public void test_templatePart() throws Exception {
        StringBuilder sb;

        sb = new StringBuilder();
        assertEquals(4, templatePart(sb, "1234", true, "0000", 0, "", 0));
        assertEquals("1234", sb.toString());

        sb = new StringBuilder();
        assertEquals(4, templatePart(sb, "1234", true, "00000", 0, "", 0));
        assertEquals("01234", sb.toString());

        sb = new StringBuilder();
        assertEquals(5, templatePart(sb, "-1234", true, "00000", 0, "", 0));
        assertEquals("-1234", sb.toString());

        sb = new StringBuilder();
        assertEquals(5, templatePart(sb, "-1234", true, "000000", 0, "", 0));
        assertEquals("-01234", sb.toString());

        sb = new StringBuilder();
        assertEquals(4, templatePart(sb, "1234", true, "####", 0, "", 0));
        assertEquals("1234", sb.toString());

        sb = new StringBuilder();
        assertEquals(4, templatePart(sb, "1234", true, "#####", 0, "", 0));
        assertEquals(" 1234", sb.toString());

        sb = new StringBuilder();
        assertEquals(5, templatePart(sb, "-1234", true, "#####", 0, "", 0));
        assertEquals("-1234", sb.toString());

        sb = new StringBuilder();
        assertEquals(5, templatePart(sb, "-1234", true, "######", 0, "", 0));
        assertEquals(" -1234", sb.toString());

        sb = new StringBuilder();
        assertEquals(4, templatePart(sb, "1234", true, "00####", 0, "", 0));
        assertEquals("001234", sb.toString());

        sb = new StringBuilder();
        assertEquals(4, templatePart(sb, "1234", true, "0#####", 0, "", 0));
        assertEquals("0 1234", sb.toString());

        sb = new StringBuilder();
        assertEquals(5, templatePart(sb, "-1234", true, "0#####", 0, "", 0));
        assertEquals("0-1234", sb.toString());

        sb = new StringBuilder();
        assertEquals(5, templatePart(sb, "-1234", true, "0#####", 0, "", 0));
        assertEquals("0-1234", sb.toString());
    }

    @Test
    public void test_convertNumber_base2() throws Exception {
        int base = 2;
        long min = Long.MIN_VALUE;
        assertEquals("0", convertNumber(0, base, true).toString());
        assertEquals("1", convertNumber(1, base, true).toString());
        assertEquals("10", convertNumber(2, base, true).toString());
        assertEquals("11", convertNumber(3, base, true).toString());
        assertEquals("1111111111111111111111111111111111111111111111111111111111111111", convertNumber(-1, base, true).toString());
        assertEquals("1111111111111111111111111111111111111111111111111111111111111110", convertNumber(-2, base, true).toString());
        assertEquals("1111111111111111111111111111111111111111111111111111111111111101", convertNumber(-3, base, true).toString());
        assertEquals("111111111111111111111111111111111111111111111111111111111111110", convertNumber(Long.MAX_VALUE - 1, base, true).toString());
        assertEquals("111111111111111111111111111111111111111111111111111111111111111", convertNumber(Long.MAX_VALUE, base, true).toString());
        assertEquals("1000000000000000000000000000000000000000000000000000000000000000", convertNumber(Long.MIN_VALUE, base, true).toString());
        assertEquals("1000000000000000000000000000000000000000000000000000000000000001", convertNumber(Long.MIN_VALUE + 1, base, true).toString());
        assertEquals("1000000000000000000000000000000000000000000000000000000000000001", convertNumber(Long.MIN_VALUE + 1, base, true).toString());
        assertEquals("1000000000000000000000000000000000000000000000000000000000000010", convertNumber(Long.MIN_VALUE + 2, base, true).toString());
        assertEquals("1000000000000000000000000000000000000000000000000000000000000011", convertNumber(Long.MIN_VALUE + 3, base, true).toString());
        assertEquals("111111111111111111111111111111111111111111111111111111111111111", convertNumber(Long.MAX_VALUE, base, true).toString());
        assertEquals("111111111111111111111111111111111111111111111111111111111111110", convertNumber(Long.MAX_VALUE - 1, base, true).toString());
        assertEquals("111111111111111111111111111111111111111111111111111111111111101", convertNumber(Long.MAX_VALUE - 2, base, true).toString());
    }

    @Test
    public void test_convertNumber_base36() {
        int base;
        long min;
        base = 36;
        min = -NumberSequenceGenerator.COMPLEMENT_BASE[base];
        assertEquals("0", convertNumber(0, base, true).toString());
        assertEquals("1", convertNumber(1, base, true).toString());
        assertEquals("2", convertNumber(2, base, true).toString());
        assertEquals("3", convertNumber(3, base, true).toString());
        assertEquals("4", convertNumber(4, base, true).toString());
        assertEquals("5", convertNumber(5, base, true).toString());
        assertEquals("6", convertNumber(6, base, true).toString());
        assertEquals("7", convertNumber(7, base, true).toString());
        assertEquals("8", convertNumber(8, base, true).toString());
        assertEquals("9", convertNumber(9, base, true).toString());
        assertEquals("A", convertNumber(10, base, true).toString());
        assertEquals("B", convertNumber(11, base, true).toString());
        assertEquals("C", convertNumber(12, base, true).toString());
        assertEquals("D", convertNumber(13, base, true).toString());
        assertEquals("E", convertNumber(14, base, true).toString());
        assertEquals("F", convertNumber(15, base, true).toString());
        assertEquals("G", convertNumber(16, base, true).toString());
        assertEquals("H", convertNumber(17, base, true).toString());
        assertEquals("I", convertNumber(18, base, true).toString());
        assertEquals("J", convertNumber(19, base, true).toString());
        assertEquals("K", convertNumber(20, base, true).toString());
        assertEquals("L", convertNumber(21, base, true).toString());
        assertEquals("M", convertNumber(22, base, true).toString());
        assertEquals("N", convertNumber(23, base, true).toString());
        assertEquals("O", convertNumber(24, base, true).toString());
        assertEquals("P", convertNumber(25, base, true).toString());
        assertEquals("Q", convertNumber(26, base, true).toString());
        assertEquals("R", convertNumber(27, base, true).toString());
        assertEquals("S", convertNumber(28, base, true).toString());
        assertEquals("T", convertNumber(29, base, true).toString());
        assertEquals("U", convertNumber(30, base, true).toString());
        assertEquals("V", convertNumber(31, base, true).toString());
        assertEquals("W", convertNumber(32, base, true).toString());
        assertEquals("X", convertNumber(33, base, true).toString());
        assertEquals("Y", convertNumber(34, base, true).toString());
        assertEquals("Z", convertNumber(35, base, true).toString());
        assertEquals("10", convertNumber(36, base, true).toString());
        assertEquals("1Y2P0IJ32E8E6", convertNumber(Long.MAX_VALUE - 1, base, true).toString());
        assertEquals("1Y2P0IJ32E8E7", convertNumber(Long.MAX_VALUE, base, true).toString());
        assertEquals("Y1XAZHGWXLRLS", convertNumber(Long.MIN_VALUE, base, true).toString());
        assertEquals("Y1XAZHGWXLRLT", convertNumber(Long.MIN_VALUE + 1, base, true).toString());
        assertEquals("ZZZZZZZZZZZZZ", convertNumber(-1, base, true).toString());
        assertEquals("ZZZZZZZZZZZZY", convertNumber(-2, base, true).toString());
        assertEquals("ZZZZZZZZZZZZX", convertNumber(-3, base, true).toString());
        assertEquals("ZZZZZZZZZZZZW", convertNumber(-4, base, true).toString());
        assertEquals("ZZZZZZZZZZZZV", convertNumber(-5, base, true).toString());
        assertEquals("YZZZZZZZZZZZZ", convertNumber(min - 1, base, true).toString());
        assertEquals("Z000000000000", convertNumber(min, base, true).toString());
        assertEquals("ZIK0ZJ", convertNumber(Integer.MAX_VALUE, base, true).toString());
        assertEquals("ZZZZZZZ0HFZ0G", convertNumber(Integer.MIN_VALUE, base, true).toString());
    }

    @Test
    public void test_convertNumber_base16() {
        int base;
        long min;
        base = 16;
        min = Long.MIN_VALUE;
        assertEquals("0", convertNumber(0, base, true).toString());
        assertEquals("1", convertNumber(1, base, true).toString());
        assertEquals("2", convertNumber(2, base, true).toString());
        assertEquals("3", convertNumber(3, base, true).toString());
        assertEquals("4", convertNumber(4, base, true).toString());
        assertEquals("5", convertNumber(5, base, true).toString());
        assertEquals("6", convertNumber(6, base, true).toString());
        assertEquals("7", convertNumber(7, base, true).toString());
        assertEquals("8", convertNumber(8, base, true).toString());
        assertEquals("9", convertNumber(9, base, true).toString());
        assertEquals("A", convertNumber(10, base, true).toString());
        assertEquals("B", convertNumber(11, base, true).toString());
        assertEquals("C", convertNumber(12, base, true).toString());
        assertEquals("D", convertNumber(13, base, true).toString());
        assertEquals("E", convertNumber(14, base, true).toString());
        assertEquals("F", convertNumber(15, base, true).toString());
        assertEquals("10", convertNumber(16, base, true).toString());
        assertEquals("7FFFFFFFFFFFFFFE", convertNumber(Long.MAX_VALUE - 1, base, true).toString());
        assertEquals("7FFFFFFFFFFFFFFF", convertNumber(Long.MAX_VALUE, base, true).toString());
        assertEquals("8000000000000000", convertNumber(Long.MIN_VALUE, base, true).toString());
        assertEquals("8000000000000001", convertNumber(Long.MIN_VALUE + 1, base, true).toString());
        assertEquals("FFFFFFFFFFFFFFFF", convertNumber(-1, base, true).toString());
        assertEquals("FFFFFFFFFFFFFFFE", convertNumber(-2, base, true).toString());
        assertEquals("FFFFFFFFFFFFFFFD", convertNumber(-3, base, true).toString());
        assertEquals("FFFFFFFFFFFFFFFC", convertNumber(-4, base, true).toString());
        assertEquals("FFFFFFFFFFFFFFFB", convertNumber(-5, base, true).toString());
    }

    @Test
    public void test_convertNumber_base12() {
        int base;
        long min;
        base = 12;
        min = -NumberSequenceGenerator.COMPLEMENT_BASE[base];
        assertEquals("0", convertNumber(0, base, true).toString());
        assertEquals("1", convertNumber(1, base, true).toString());
        assertEquals("2", convertNumber(2, base, true).toString());
        assertEquals("3", convertNumber(3, base, true).toString());
        assertEquals("4", convertNumber(4, base, true).toString());
        assertEquals("5", convertNumber(5, base, true).toString());
        assertEquals("6", convertNumber(6, base, true).toString());
        assertEquals("7", convertNumber(7, base, true).toString());
        assertEquals("8", convertNumber(8, base, true).toString());
        assertEquals("9", convertNumber(9, base, true).toString());
        assertEquals("A", convertNumber(10, base, true).toString());
        assertEquals("B", convertNumber(11, base, true).toString());
        assertEquals("10", convertNumber(12, base, true).toString());
        assertEquals("41A792678515120366", convertNumber(Long.MAX_VALUE - 1, base, true).toString());
        assertEquals("41A792678515120367", convertNumber(Long.MAX_VALUE, base, true).toString());
        // ((BASE^N - 1) - (MAX_VALUE + 1)) + 1 == MIN_VALUE complement representation
        // 41A792678515120367 + 1
        // 41A792678515120368 -
        // BBBBBBBBBBBBBBBBBB
        // 7A14295436A6A9B853 + 1
        // 7A14295436A6A9B854
        assertEquals("7A14295436A6A9B854", convertNumber(Long.MIN_VALUE, base, true).toString());
        assertEquals("7A14295436A6A9B855", convertNumber(Long.MIN_VALUE + 1, base, true).toString());
        assertEquals("BBBBBBBBBBBBBBBBBB", convertNumber(-1, base, true).toString());
        assertEquals("BBBBBBBBBBBBBBBBBA", convertNumber(-2, base, true).toString());
        assertEquals("BBBBBBBBBBBBBBBBB9", convertNumber(-3, base, true).toString());
        assertEquals("BBBBBBBBBBBBBBBBB8", convertNumber(-4, base, true).toString());
        assertEquals("BBBBBBBBBBBBBBBBB7", convertNumber(-5, base, true).toString());
        assertEquals("B00000000000000000", convertNumber(min, base, true).toString());
        assertEquals("B00000000000000001", convertNumber(min + 1, base, true).toString());
    }

    @Test
    public void test_convertNumber_base10() {
        int base;
        long min;
        base = 10;
        min = -NumberSequenceGenerator.COMPLEMENT_BASE[base];
        assertEquals("0", convertNumber(0, base, true).toString());
        assertEquals("1", convertNumber(1, base, true).toString());
        assertEquals("2", convertNumber(2, base, true).toString());
        assertEquals("3", convertNumber(3, base, true).toString());
        assertEquals("4", convertNumber(4, base, true).toString());
        assertEquals("5", convertNumber(5, base, true).toString());
        assertEquals("6", convertNumber(6, base, true).toString());
        assertEquals("7", convertNumber(7, base, true).toString());
        assertEquals("8", convertNumber(8, base, true).toString());
        assertEquals("9", convertNumber(9, base, true).toString());
        assertEquals("10", convertNumber(10, base, true).toString());
        assertEquals("9223372036854775806", convertNumber(Long.MAX_VALUE - 1, base, false).toString());
        assertEquals("9223372036854775807", convertNumber(Long.MAX_VALUE, base, false).toString());
        assertEquals("-9223372036854775808", convertNumber(Long.MIN_VALUE, base, false).toString());
        // 09999999999999999999
        // 09223372036854775807 -
        // 90776627963145224192 + 1
        // 90776627963145224193
        assertEquals("-9223372036854775807", convertNumber(Long.MIN_VALUE + 1, base, false).toString());
        assertEquals("-1", convertNumber(-1, base, false).toString());
        assertEquals("-2", convertNumber(-2, base, false).toString());
        assertEquals("-3", convertNumber(-3, base, false).toString());
        assertEquals("-4", convertNumber(-4, base, false).toString());
        assertEquals("-5", convertNumber(-5, base, false).toString());
        assertEquals("-1000000000000000000", convertNumber(min, base, false).toString());
        assertEquals("-999999999999999999", convertNumber(min + 1, base, false).toString());
        assertEquals("9223372036854775806", convertNumber(Long.MAX_VALUE - 1, base, true).toString());
        assertEquals("9223372036854775807", convertNumber(Long.MAX_VALUE, base, true).toString());
        assertEquals("90776627963145224192", convertNumber(Long.MIN_VALUE, base, true).toString());
        assertEquals("90776627963145224193", convertNumber(Long.MIN_VALUE + 1, base, true).toString());
        assertEquals("99999999999999999999", convertNumber(-1, base, true).toString());
        assertEquals("99999999999999999998", convertNumber(-2, base, true).toString());
        assertEquals("99999999999999999997", convertNumber(-3, base, true).toString());
        assertEquals("99999999999999999996", convertNumber(-4, base, true).toString());
        assertEquals("99999999999999999995", convertNumber(-5, base, true).toString());
        assertEquals("99000000000000000000", convertNumber(min, base, true).toString());
        assertEquals("99000000000000000001", convertNumber(min + 1, base, true).toString());
    }

    @Test
    public void test_convertNumber_base8() {
        int base;
        long min;
        base = 8;
        min = Long.MIN_VALUE;
        assertEquals("0", convertNumber(0, base, true).toString());
        assertEquals("1", convertNumber(1, base, true).toString());
        assertEquals("2", convertNumber(2, base, true).toString());
        assertEquals("3", convertNumber(3, base, true).toString());
        assertEquals("4", convertNumber(4, base, true).toString());
        assertEquals("5", convertNumber(5, base, true).toString());
        assertEquals("6", convertNumber(6, base, true).toString());
        assertEquals("7", convertNumber(7, base, true).toString());
        assertEquals("10", convertNumber(8, base, true).toString());
        assertEquals("777777777777777777776", convertNumber(Long.MAX_VALUE - 1, base, true).toString());
        assertEquals("777777777777777777777", convertNumber(Long.MAX_VALUE, base, true).toString());
        assertEquals("7000000000000000000000", convertNumber(Long.MIN_VALUE, base, true).toString());
        assertEquals("7000000000000000000001", convertNumber(Long.MIN_VALUE + 1, base, true).toString());
        assertEquals("7777777777777777777777", convertNumber(-1, base, true).toString());
        assertEquals("7777777777777777777776", convertNumber(-2, base, true).toString());
        assertEquals("7777777777777777777775", convertNumber(-3, base, true).toString());
        assertEquals("7777777777777777777774", convertNumber(-4, base, true).toString());
        assertEquals("7777777777777777777773", convertNumber(-5, base, true).toString());
    }

    @Test
    public void test_convertNumber_base5() {
        int base;
        long min;
        base = 5;
        min = -NumberSequenceGenerator.COMPLEMENT_BASE[base];
        assertEquals("0", convertNumber(0, base, true).toString());
        assertEquals("1", convertNumber(1, base, true).toString());
        assertEquals("2", convertNumber(2, base, true).toString());
        assertEquals("3", convertNumber(3, base, true).toString());
        assertEquals("4", convertNumber(4, base, true).toString());
        assertEquals("10", convertNumber(5, base, true).toString());
        assertEquals("1104332401304422434310311211", convertNumber(Long.MAX_VALUE - 1, base, true).toString());

        // : BASE^N - 1 - (MAX_VALUE + 1) + 1 == MIN_VALUE complement representation
        // 1104332401304422434310311212 + 1
        // 1104332401304422434310311213 -
        // 4444444444444444444444444444
        // 3340112043140022010134133231 + 1
        // 3340112043140022010134133232
        assertEquals("1104332401304422434310311212", convertNumber(Long.MAX_VALUE, base, true).toString());
        assertEquals("3340112043140022010134133232", convertNumber(Long.MIN_VALUE, base, true).toString());
        assertEquals("3340112043140022010134133233", convertNumber(Long.MIN_VALUE + 1, base, true).toString());
        assertEquals("4444444444444444444444444444", convertNumber(-1, base, true).toString());
        assertEquals("4444444444444444444444444443", convertNumber(-2, base, true).toString());
        assertEquals("4444444444444444444444444442", convertNumber(-3, base, true).toString());
        assertEquals("4444444444444444444444444441", convertNumber(-4, base, true).toString());
        assertEquals("4444444444444444444444444440", convertNumber(-5, base, true).toString());
        assertEquals("4000000000000000000000000000", convertNumber(min, base, true).toString());
        assertEquals("4000000000000000000000000001", convertNumber(min + 1, base, true).toString());
    }

    @Test
    public void test_convertNumber_base4() {
        int base;
        long min;
        base = 4;
        min = Long.MIN_VALUE;
        assertEquals("0", convertNumber(0, base, true).toString());
        assertEquals("1", convertNumber(1, base, true).toString());
        assertEquals("2", convertNumber(2, base, true).toString());
        assertEquals("3", convertNumber(3, base, true).toString());
        assertEquals("10", convertNumber(4, base, true).toString());
        assertEquals("13333333333333333333333333333332", convertNumber(Long.MAX_VALUE - 1, base, true).toString());
        assertEquals("13333333333333333333333333333333", convertNumber(Long.MAX_VALUE, base, true).toString());
        assertEquals("20000000000000000000000000000000", convertNumber(Long.MIN_VALUE, base, true).toString());
        assertEquals("20000000000000000000000000000001", convertNumber(Long.MIN_VALUE + 1, base, true).toString());
        assertEquals("33333333333333333333333333333333", convertNumber(-1, base, true).toString());
        assertEquals("33333333333333333333333333333332", convertNumber(-2, base, true).toString());
        assertEquals("33333333333333333333333333333331", convertNumber(-3, base, true).toString());
        assertEquals("33333333333333333333333333333330", convertNumber(-4, base, true).toString());
        assertEquals("33333333333333333333333333333323", convertNumber(-5, base, true).toString());
        assertEquals("20000000000000000000000000000000", convertNumber(min, base, true).toString());
        assertEquals("20000000000000000000000000000001", convertNumber(min + 1, base, true).toString());
    }

    @Test
    public void test_convertNumber_base3() {
        int base;
        long min;
        base = 3;
        min = -NumberSequenceGenerator.COMPLEMENT_BASE[base] * 2;
        assertEquals("0", convertNumber(0, base, true).toString());
        assertEquals("1", convertNumber(1, base, true).toString());
        assertEquals("2", convertNumber(2, base, true).toString());
        assertEquals("10", convertNumber(3, base, true).toString());
        assertEquals("2021110011022210012102010021220101220220", convertNumber(Long.MAX_VALUE - 1, base, true).toString());
        assertEquals("2021110011022210012102010021220101220221", convertNumber(Long.MAX_VALUE, base, true).toString());
        // ((BASE^N - 1) - (MAX_VALUE + 1)) + 1 == MIN_VALUE complement representation
        // 02021110011022210012102010021220101220221 + 1
        // 02021110011022210012102010021220101220222 -
        // 22222222222222222222222222222222222222222
        // 20201112211200012210120212201002121002000 + 1
        // 20201112211200012210120212201002121002001
        assertEquals("20201112211200012210120212201002121002001", convertNumber(Long.MIN_VALUE, base, true).toString());
        assertEquals("20201112211200012210120212201002121002002", convertNumber(Long.MIN_VALUE + 1, base, true).toString());
        assertEquals("22222222222222222222222222222222222222222", convertNumber(-1, base, true).toString());
        assertEquals("22222222222222222222222222222222222222221", convertNumber(-2, base, true).toString());
        assertEquals("22222222222222222222222222222222222222220", convertNumber(-3, base, true).toString());
        assertEquals("22222222222222222222222222222222222222212", convertNumber(-4, base, true).toString());
        assertEquals("22222222222222222222222222222222222222211", convertNumber(-5, base, true).toString());
        assertEquals("21000000000000000000000000000000000000000", convertNumber(min, base, true).toString());
        assertEquals("21000000000000000000000000000000000000001", convertNumber(min + 1, base, true).toString());
        assertEquals("21000000000000000000000000000000000000002", convertNumber(min + 2, base, true).toString());
        assertEquals("21000000000000000000000000000000000000010", convertNumber(min + 3, base, true).toString());
        assertEquals("21000000000000000000000000000000000000011", convertNumber(min + 4, base, true).toString());
    }

    @Test
    public void test_convertNumber_base9() {
        int base;
        long min;
        base = 9;
        min = -NumberSequenceGenerator.COMPLEMENT_BASE[base];
        assertEquals("0", convertNumber(0, base, true).toString());
        assertEquals("1", convertNumber(1, base, true).toString());
        assertEquals("2", convertNumber(2, base, true).toString());
        assertEquals("3", convertNumber(3, base, true).toString());
        assertEquals("4", convertNumber(4, base, true).toString());
        assertEquals("5", convertNumber(5, base, true).toString());
        assertEquals("6", convertNumber(6, base, true).toString());
        assertEquals("7", convertNumber(7, base, true).toString());
        assertEquals("8", convertNumber(8, base, true).toString());
        assertEquals("10", convertNumber(9, base, true).toString());
        assertEquals("67404283172107811826", convertNumber(Long.MAX_VALUE - 1, base, true).toString());
        assertEquals("67404283172107811827", convertNumber(Long.MAX_VALUE, base, true).toString());
        // ((BASE^N - 1) - (MAX_VALUE + 1)) + 1 == MIN_VALUE complement representation
        // 67404283172107811827 + 1
        // 67404283172107811828 -
        // 88888888888888888888
        // 21484605716781077060 + 1
        // 21484605716781077061
        assertEquals("21484605716781077061", convertNumber(Long.MIN_VALUE, base, true).toString());
        assertEquals("21484605716781077062", convertNumber(Long.MIN_VALUE + 1, base, true).toString());
        assertEquals("88888888888888888888", convertNumber(-1, base, true).toString());
        assertEquals("88888888888888888887", convertNumber(-2, base, true).toString());
        assertEquals("88888888888888888886", convertNumber(-3, base, true).toString());
        assertEquals("88888888888888888885", convertNumber(-4, base, true).toString());
        assertEquals("88888888888888888884", convertNumber(-5, base, true).toString());
        assertEquals("80000000000000000000", convertNumber(min, base, true).toString());
        assertEquals("80000000000000000001", convertNumber(min + 1, base, true).toString());
        assertEquals("80000000000000000002", convertNumber(min + 2, base, true).toString());
        assertEquals("80000000000000000003", convertNumber(min + 3, base, true).toString());
        assertEquals("80000000000000000004", convertNumber(min + 4, base, true).toString());
    }

    private static long maxPositive(int base) {
        long v = 1;

        while (v <= Long.MAX_VALUE / base) {
            v *= base;
        }
        return v;
    }

    //@Test
    public void max_digits() throws Exception {
        System.out.println("    private static final int[] MAX_CHARS_BASE = new int[] {");
        for (int i = 0; i < 37; i++) {
            if (i < 2) {
                System.out.println("        0, // " + i);
            } else {
                int d = 0;
                long v = 1;
                long l = Long.MAX_VALUE / i + i - Long.MAX_VALUE % i;
                while (v < l) {
                    v *= i;
                    d++;
                }
                if (v == l) d++;
                System.out.println("        " + String.valueOf(d + 1) + ", // " + i);
            }
        }
        System.out.println("    };");
    }

    public static String baseConv(long l, int base) {
        String sb = "";
        while (l > 0) {
            sb = ((l % base)) + sb;
            l -= l % base;
            l /= base;
        }
        return sb;
    }

    //@Test
    public void base_complement() throws Exception {
        System.out.println("    private static final long[] COMPLEMENT_BASE = new long[] {");
        for (int i = 0; i < 37; i++) {
            if (i < 2) {
                System.out.println("        0, // " + i);
            } else {
                int d = 0;
                long l = maxPositive(i);

                if (i > 2 || true) {
                    System.out.print("        " + String.valueOf(l) + "L, // " + i + " ");

                    //l = -l;
                    String sb = "";
                    while (l > 0) {
                        sb = ((l % i)) + sb;
                        l -= l % i;
                        l /= i;
                    }
                    System.out.println(sb);
                } else {
                    System.out.println("        " + String.valueOf(l).substring(1) + "L, // " + i);
                }
            }
        }
        System.out.println("    };");
    }
}
