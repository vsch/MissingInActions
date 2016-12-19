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

import static org.junit.Assert.*;
import static com.vladsch.MissingInActions.settings.NumberSequenceGenerator.*;

public class NumberSequenceGeneratorTest {
    @Test
    public void test_templateNumber() throws Exception {

    }

    @Test
    public void test_templatePart() throws Exception {

    }

    @Test
    public void test_convertNumber() throws Exception {
        int base = 2;
        long min = Long.MIN_VALUE;
        assertEquals("0", convertNumber(0, base).toString());
        assertEquals("1", convertNumber(1, base).toString());
        assertEquals("10", convertNumber(2, base).toString());
        assertEquals("11", convertNumber(3, base).toString());
        assertEquals("1111111111111111111111111111111111111111111111111111111111111111", convertNumber(-1, base).toString());
        assertEquals("1111111111111111111111111111111111111111111111111111111111111110", convertNumber(-2, base).toString());
        assertEquals("1111111111111111111111111111111111111111111111111111111111111101", convertNumber(-3, base).toString());
        assertEquals("111111111111111111111111111111111111111111111111111111111111110", convertNumber(Long.MAX_VALUE-1, base).toString());
        assertEquals("111111111111111111111111111111111111111111111111111111111111111", convertNumber(Long.MAX_VALUE, base).toString());
        assertEquals("1000000000000000000000000000000000000000000000000000000000000000", convertNumber(Long.MIN_VALUE, base).toString());
        assertEquals("1000000000000000000000000000000000000000000000000000000000000001", convertNumber(Long.MIN_VALUE+1, base).toString());
        assertEquals("1000000000000000000000000000000000000000000000000000000000000001", convertNumber(Long.MIN_VALUE + 1, base).toString());
        assertEquals("1000000000000000000000000000000000000000000000000000000000000010", convertNumber(Long.MIN_VALUE + 2, base).toString());
        assertEquals("1000000000000000000000000000000000000000000000000000000000000011", convertNumber(Long.MIN_VALUE + 3, base).toString());
        assertEquals("111111111111111111111111111111111111111111111111111111111111111", convertNumber(Long.MAX_VALUE, base).toString());
        assertEquals("111111111111111111111111111111111111111111111111111111111111110", convertNumber(Long.MAX_VALUE - 1, base).toString());
        assertEquals("111111111111111111111111111111111111111111111111111111111111101", convertNumber(Long.MAX_VALUE - 2, base).toString());

        base = 3;
        min = -NumberSequenceGenerator.COMPLEMENT_BASE[base];
        assertEquals("0", convertNumber(0, base).toString());
        assertEquals("1", convertNumber(1, base).toString());
        assertEquals("2", convertNumber(2, base).toString());
        assertEquals("10", convertNumber(3, base).toString());
        assertEquals("2021110011022210012102010021220101220220", convertNumber(Long.MAX_VALUE-1, base).toString());
        assertEquals("2021110011022210012102010021220101220221", convertNumber(Long.MAX_VALUE, base).toString());
        assertEquals("20201112211200012210120212201002121002011", convertNumber(Long.MIN_VALUE, base).toString());
        assertEquals("20201112211200012210120212201002121002012", convertNumber(Long.MIN_VALUE+1, base).toString());
        assertEquals("22222222222222222222222222222222222222222", convertNumber(-1, base).toString());
        assertEquals("22222222222222222222222222222222222222221", convertNumber(-2, base).toString());
        assertEquals("22222222222222222222222222222222222222220", convertNumber(-3, base).toString());
        assertEquals("22222222222222222222222222222222222222212", convertNumber(-4, base).toString());
        assertEquals("22222222222222222222222222222222222222211", convertNumber(-5, base).toString());
        assertEquals("22000000000000000000000000000000000000000", convertNumber(min, base).toString());
        assertEquals("22000000000000000000000000000000000000001", convertNumber(min+1, base).toString());
        assertEquals("22000000000000000000000000000000000000002", convertNumber(min+2, base).toString());
        assertEquals("22000000000000000000000000000000000000010", convertNumber(min+3, base).toString());
        assertEquals("22000000000000000000000000000000000000011", convertNumber(min+4, base).toString());
        
        base = 4;
        min = Long.MIN_VALUE;
        assertEquals("0", convertNumber(0, base).toString());
        assertEquals("1", convertNumber(1, base).toString());
        assertEquals("2", convertNumber(2, base).toString());
        assertEquals("3", convertNumber(3, base).toString());
        assertEquals("10", convertNumber(4, base).toString());
        assertEquals("13333333333333333333333333333332", convertNumber(Long.MAX_VALUE-1, base).toString());
        assertEquals("13333333333333333333333333333333", convertNumber(Long.MAX_VALUE, base).toString());
        assertEquals("20000000000000000000000000000000", convertNumber(Long.MIN_VALUE, base).toString());
        assertEquals("20000000000000000000000000000001", convertNumber(Long.MIN_VALUE+1, base).toString());
        assertEquals("33333333333333333333333333333333", convertNumber(-1, base).toString());
        assertEquals("33333333333333333333333333333332", convertNumber(-2, base).toString());
        assertEquals("33333333333333333333333333333331", convertNumber(-3, base).toString());
        assertEquals("33333333333333333333333333333330", convertNumber(-4, base).toString());
        assertEquals("33333333333333333333333333333323", convertNumber(-5, base).toString());
        assertEquals("20000000000000000000000000000000", convertNumber(min, base).toString());
        assertEquals("20000000000000000000000000000001", convertNumber(min+1, base).toString());

        base = 5;
        min = -NumberSequenceGenerator.COMPLEMENT_BASE[base];
        assertEquals("0", convertNumber(0, base).toString());
        assertEquals("1", convertNumber(1, base).toString());
        assertEquals("2", convertNumber(2, base).toString());
        assertEquals("3", convertNumber(3, base).toString());
        assertEquals("4", convertNumber(4, base).toString());
        assertEquals("10", convertNumber(5, base).toString());
        assertEquals("1104332401304422434310311211", convertNumber(Long.MAX_VALUE-1, base).toString());
        assertEquals("1104332401304422434310311212", convertNumber(Long.MAX_VALUE, base).toString());
        assertEquals("3340112043140022010134133242", convertNumber(Long.MIN_VALUE, base).toString());
        assertEquals("3340112043140022010134133243", convertNumber(Long.MIN_VALUE+1, base).toString());
        assertEquals("4444444444444444444444444444", convertNumber(-1, base).toString());
        assertEquals("4444444444444444444444444443", convertNumber(-2, base).toString());
        assertEquals("4444444444444444444444444442", convertNumber(-3, base).toString());
        assertEquals("4444444444444444444444444441", convertNumber(-4, base).toString());
        assertEquals("4444444444444444444444444440", convertNumber(-5, base).toString());
        assertEquals("4000000000000000000000000000", convertNumber(min, base).toString());
        assertEquals("4000000000000000000000000001", convertNumber(min+1, base).toString());

        base = 8;
        min = Long.MIN_VALUE;
        assertEquals("0", convertNumber(0, base).toString());
        assertEquals("1", convertNumber(1, base).toString());
        assertEquals("2", convertNumber(2, base).toString());
        assertEquals("3", convertNumber(3, base).toString());
        assertEquals("4", convertNumber(4, base).toString());
        assertEquals("5", convertNumber(5, base).toString());
        assertEquals("6", convertNumber(6, base).toString());
        assertEquals("7", convertNumber(7, base).toString());
        assertEquals("10", convertNumber(8, base).toString());
        assertEquals("777777777777777777776", convertNumber(Long.MAX_VALUE-1, base).toString());
        assertEquals("777777777777777777777", convertNumber(Long.MAX_VALUE, base).toString());
        assertEquals("7000000000000000000000", convertNumber(Long.MIN_VALUE, base).toString());
        assertEquals("7000000000000000000001", convertNumber(Long.MIN_VALUE+1, base).toString());
        assertEquals("7777777777777777777777", convertNumber(-1, base).toString());
        assertEquals("7777777777777777777776", convertNumber(-2, base).toString());
        assertEquals("7777777777777777777775", convertNumber(-3, base).toString());
        assertEquals("7777777777777777777774", convertNumber(-4, base).toString());
        assertEquals("7777777777777777777773", convertNumber(-5, base).toString());

        base = 12;
        min = -NumberSequenceGenerator.COMPLEMENT_BASE[base];
        assertEquals("0", convertNumber(0, base).toString());
        assertEquals("1", convertNumber(1, base).toString());
        assertEquals("2", convertNumber(2, base).toString());
        assertEquals("3", convertNumber(3, base).toString());
        assertEquals("4", convertNumber(4, base).toString());
        assertEquals("5", convertNumber(5, base).toString());
        assertEquals("6", convertNumber(6, base).toString());
        assertEquals("7", convertNumber(7, base).toString());
        assertEquals("8", convertNumber(8, base).toString());
        assertEquals("9", convertNumber(9, base).toString());
        assertEquals("A", convertNumber(10, base).toString());
        assertEquals("B", convertNumber(11, base).toString());
        assertEquals("10", convertNumber(12, base).toString());
        assertEquals("41A792678515120366", convertNumber(Long.MAX_VALUE-1, base).toString());
        assertEquals("41A792678515120367", convertNumber(Long.MAX_VALUE, base).toString());
        assertEquals("7A14295436A6A9B864", convertNumber(Long.MIN_VALUE, base).toString());
        assertEquals("7A14295436A6A9B865", convertNumber(Long.MIN_VALUE+1, base).toString());
        assertEquals("BBBBBBBBBBBBBBBBBB", convertNumber(-1, base).toString());
        assertEquals("BBBBBBBBBBBBBBBBBA", convertNumber(-2, base).toString());
        assertEquals("BBBBBBBBBBBBBBBBB9", convertNumber(-3, base).toString());
        assertEquals("BBBBBBBBBBBBBBBBB8", convertNumber(-4, base).toString());
        assertEquals("BBBBBBBBBBBBBBBBB7", convertNumber(-5, base).toString());
        assertEquals("B00000000000000000", convertNumber(min, base).toString());
        assertEquals("B00000000000000001", convertNumber(min+1, base).toString());

        base = 16;
        min = Long.MIN_VALUE;
        assertEquals("0", convertNumber(0, base).toString());
        assertEquals("1", convertNumber(1, base).toString());
        assertEquals("2", convertNumber(2, base).toString());
        assertEquals("3", convertNumber(3, base).toString());
        assertEquals("4", convertNumber(4, base).toString());
        assertEquals("5", convertNumber(5, base).toString());
        assertEquals("6", convertNumber(6, base).toString());
        assertEquals("7", convertNumber(7, base).toString());
        assertEquals("8", convertNumber(8, base).toString());
        assertEquals("9", convertNumber(9, base).toString());
        assertEquals("A", convertNumber(10, base).toString());
        assertEquals("B", convertNumber(11, base).toString());
        assertEquals("C", convertNumber(12, base).toString());
        assertEquals("D", convertNumber(13, base).toString());
        assertEquals("E", convertNumber(14, base).toString());
        assertEquals("F", convertNumber(15, base).toString());
        assertEquals("10", convertNumber(16, base).toString());
        assertEquals("7FFFFFFFFFFFFFFE", convertNumber(Long.MAX_VALUE-1, base).toString());
        assertEquals("7FFFFFFFFFFFFFFF", convertNumber(Long.MAX_VALUE, base).toString());
        assertEquals("8000000000000000", convertNumber(Long.MIN_VALUE, base).toString());
        assertEquals("8000000000000001", convertNumber(Long.MIN_VALUE+1, base).toString());
        assertEquals("FFFFFFFFFFFFFFFF", convertNumber(-1, base).toString());
        assertEquals("FFFFFFFFFFFFFFFE", convertNumber(-2, base).toString());
        assertEquals("FFFFFFFFFFFFFFFD", convertNumber(-3, base).toString());
        assertEquals("FFFFFFFFFFFFFFFC", convertNumber(-4, base).toString());
        assertEquals("FFFFFFFFFFFFFFFB", convertNumber(-5, base).toString());

        base = 36;
        min = -NumberSequenceGenerator.COMPLEMENT_BASE[base];
        assertEquals("0", convertNumber(0, base).toString());
        assertEquals("1", convertNumber(1, base).toString());
        assertEquals("2", convertNumber(2, base).toString());
        assertEquals("3", convertNumber(3, base).toString());
        assertEquals("4", convertNumber(4, base).toString());
        assertEquals("5", convertNumber(5, base).toString());
        assertEquals("6", convertNumber(6, base).toString());
        assertEquals("7", convertNumber(7, base).toString());
        assertEquals("8", convertNumber(8, base).toString());
        assertEquals("9", convertNumber(9, base).toString());
        assertEquals("A", convertNumber(10, base).toString());
        assertEquals("B", convertNumber(11, base).toString());
        assertEquals("C", convertNumber(12, base).toString());
        assertEquals("D", convertNumber(13, base).toString());
        assertEquals("E", convertNumber(14, base).toString());
        assertEquals("F", convertNumber(15, base).toString());
        assertEquals("G", convertNumber(16, base).toString());
        assertEquals("H", convertNumber(17, base).toString());
        assertEquals("I", convertNumber(18, base).toString());
        assertEquals("J", convertNumber(19, base).toString());
        assertEquals("K", convertNumber(20, base).toString());
        assertEquals("L", convertNumber(21, base).toString());
        assertEquals("M", convertNumber(22, base).toString());
        assertEquals("N", convertNumber(23, base).toString());
        assertEquals("O", convertNumber(24, base).toString());
        assertEquals("P", convertNumber(25, base).toString());
        assertEquals("Q", convertNumber(26, base).toString());
        assertEquals("R", convertNumber(27, base).toString());
        assertEquals("S", convertNumber(28, base).toString());
        assertEquals("T", convertNumber(29, base).toString());
        assertEquals("U", convertNumber(30, base).toString());
        assertEquals("V", convertNumber(31, base).toString());
        assertEquals("W", convertNumber(32, base).toString());
        assertEquals("X", convertNumber(33, base).toString());
        assertEquals("Y", convertNumber(34, base).toString());
        assertEquals("Z", convertNumber(35, base).toString());
        assertEquals("10", convertNumber(36, base).toString());
        assertEquals("1Y2P0IJ32E8E6", convertNumber(Long.MAX_VALUE-1, base).toString());
        assertEquals("1Y2P0IJ32E8E7", convertNumber(Long.MAX_VALUE, base).toString());
        assertEquals("Y1XAZHGWXLRMS", convertNumber(Long.MIN_VALUE, base).toString());
        assertEquals("Y1XAZHGWXLRMT", convertNumber(Long.MIN_VALUE+1, base).toString());
        assertEquals("ZZZZZZZZZZZZZ", convertNumber(-1, base).toString());
        assertEquals("ZZZZZZZZZZZZY", convertNumber(-2, base).toString());
        assertEquals("ZZZZZZZZZZZZX", convertNumber(-3, base).toString());
        assertEquals("ZZZZZZZZZZZZW", convertNumber(-4, base).toString());
        assertEquals("ZZZZZZZZZZZZV", convertNumber(-5, base).toString());
        assertEquals("YZZZZZZZZZZZZ", convertNumber(min-1, base).toString());
        assertEquals("Z000000000000", convertNumber(min, base).toString());
        assertEquals(       "ZIK0ZJ", convertNumber(Integer.MAX_VALUE, base).toString());
        assertEquals("ZZZZZZZ0HFZ0G", convertNumber(Integer.MIN_VALUE, base).toString());
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
