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

import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.CharBasedSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;

import static java.lang.Long.MIN_VALUE;

public class NumberSequenceGenerator {
    public static final int[] MAX_CHARS_BASE = new int[] {
            0, // 0
            0, // 1
            64, // 2
            41, // 3
            32, // 4
            28, // 5
            25, // 6
            23, // 7
            22, // 8
            20, // 9
            20, // 10
            19, // 11
            18, // 12
            18, // 13
            17, // 14
            17, // 15
            16, // 16
            16, // 17
            16, // 18
            15, // 19
            15, // 20
            15, // 21
            15, // 22
            14, // 23
            14, // 24
            14, // 25
            14, // 26
            14, // 27
            14, // 28
            13, // 29
            13, // 30
            13, // 31
            13, // 32
            13, // 33
            13, // 34
            13, // 35
            13, // 36
    };

    public static final long[] COMPLEMENT_BASE = new long[] {
            0, // 0
            0, // 1
            4611686018427387904L, // 2 100000000000000000000000000000000000000000000000000000000000000
            4052555153018976267L, // 3 1000000000000000000000000000000000000000
            4611686018427387904L, // 4 10000000000000000000000000000000
            7450580596923828125L, // 5 1000000000000000000000000000
            4738381338321616896L, // 6 1000000000000000000000000
            3909821048582988049L, // 7 10000000000000000000000
            1152921504606846976L, // 8 100000000000000000000
            1350851717672992089L, // 9 10000000000000000000
            1000000000000000000L, // 10 1000000000000000000
            5559917313492231481L, // 11 1000000000000000000
            2218611106740436992L, // 12 100000000000000000
            8650415919381337933L, // 13 100000000000000000
            2177953337809371136L, // 14 10000000000000000
            6568408355712890625L, // 15 10000000000000000
            1152921504606846976L, // 16 1000000000000000
            2862423051509815793L, // 17 1000000000000000
            6746640616477458432L, // 18 1000000000000000
            799006685782884121L, // 19 100000000000000
            1638400000000000000L, // 20 100000000000000
            3243919932521508681L, // 21 100000000000000
            6221821273427820544L, // 22 100000000000000
            504036361936467383L, // 23 10000000000000
            876488338465357824L, // 24 10000000000000
            1490116119384765625L, // 25 10000000000000
            2481152873203736576L, // 26 10000000000000
            4052555153018976267L, // 27 10000000000000
            6502111422497947648L, // 28 10000000000000
            353814783205469041L, // 29 1000000000000
            531441000000000000L, // 30 1000000000000
            787662783788549761L, // 31 1000000000000
            1152921504606846976L, // 32 1000000000000
            1667889514952984961L, // 33 1000000000000
            2386420683693101056L, // 34 1000000000000
            3379220508056640625L, // 35 1000000000000
            4738381338321616896L, // 36 1000000000000
    };

    private final NumberingOptions myOptions;
    private long myFirst;
    private long myLast;

    public NumberSequenceGenerator(@NotNull NumberingOptions options) {
        myOptions = options;
    }

    /**
     * Extract a number from given sequence based on prefix/suffix, base and template
     *
     * @param charSequence character sequence from which to extract a number
     * @return number or null if unable to extract
     */
    @Nullable
    public Long extractNumber(@NotNull CharSequence charSequence) {
        BasedSequence chars = BasedSequence.of(charSequence);

        if (!myOptions.getPrefix().isEmpty()) chars = chars.removePrefix(myOptions.getPrefix());
        if (!myOptions.getSuffix().isEmpty()) chars = chars.removeSuffix(myOptions.getSuffix());

        // see if we have a template and it matches somewhat
        return null;
    }

    /**
     * Format the number according to prefix, suffix, separator, separatorFrequency and template
     * <p>
     * Template special strings:
     * # - represents a variable number of digits, using more than one when need to specify N digits before literal, if no digits left the # replaced by empty string
     * 0 - represents a digit or 0 if no digits left to distribute
     * <p>
     * whole part:
     * 0 - in the first position will truncate the rest of the digits if the rest of the digits are max digit for the base (ie. -ve original number was used)
     * - in any other position represents the digit, or 0 if no more digits
     * # - in the first position will truncate the rest of the digits if the rest of the digits are max digit for the base (ie. -ve original number was used)
     * - in any other position represents the digit, or space if no more digits
     * <p>
     * fractional part:
     * # and 0 represent a single digit
     * <p>
     * Sign of the number is always processed as the most significant digit of the whole part
     * <p>
     * Decimal - used to separate the number into whole and fractional parts by character count
     * Separator - used to add interval spacing in whole part of the number
     * Prefix - added to number after template is applied
     * Suffix - added to number after template is applied
     * <p>
     * \ - used to escape 0's, #'s and \'s
     * \0 - escaped 0, used to add 0 as a literal
     * \# - escaped #, used to add # as a literal
     * \\ - escaped \, used to add \ as a literal
     * <p>
     * Any other characters are interpreted as literals and copied to the final number as is
     * <p>
     * Template is first split into whole and fraction parts
     * then each part is processed separately
     *
     * @param number number to format
     * @return character sequence of the number
     */
    @NotNull
    public BasedSequence templateNumber(@NotNull CharSequence number) {
        StringBuilder sb = new StringBuilder();

        sb.append(myOptions.getPrefix());

        if (!myOptions.getTemplate().isEmpty()) {
            BasedSequence template = BasedSequence.of(myOptions.getTemplate());
            BasedSequence wholePart = template;
            BasedSequence fractionPart = BasedSequence.NULL;

            if (!myOptions.getDecimalPoint().isEmpty()) {
                int pos = template.lastIndexOf(myOptions.getDecimalPoint());

                int base = myOptions.getNumberingBase();
                if (pos >= 0) {
                    wholePart = template.subSequence(0, pos);
                    fractionPart = template.subSequence(pos + myOptions.getDecimalPoint().length());

                    StringBuilder frac = new StringBuilder();

                    int digitPos = templatePart(base, frac, number, false, fractionPart, 0, null, 0);
                    templatePart(base, sb, number, true, wholePart, digitPos, myOptions.getSeparator(), myOptions.getSeparatorFrequency());

                    sb.append(myOptions.getDecimalPoint());
                    sb.append(frac);
                } else {
                    templatePart(base, sb, number, true, wholePart, 0, myOptions.getSeparator(), myOptions.getSeparatorFrequency());
                }
            }
        } else {
            sb.append(number);
        }

        sb.append(myOptions.getSuffix());
        return BasedSequence.of(number);
    }

    public static int templatePart(int base, StringBuilder sb, CharSequence numberChars, boolean wholePart, BasedSequence part, int digitPos, @Nullable String separator, int separatorFrequency) {
        // here we apply the part template to digits of the number, proceeding from the digitPos offset from last digit of number
        BasedSequence number = BasedSequence.of(numberChars);
        // first we need to know how many digit places will be used 
        BitSet digits = new BitSet(sb.length() + part.length());
        int iMax = part.length();
        for (int i = 0; i < iMax; i++) {
            char c = part.charAt(i);
            switch (c) {
                case '\\':
                    if (i + 1 < iMax) {
                        c = part.charAt(++i);
                        if (!(c == '0' || c == '#' || c == '\\')) sb.append('\\');
                        sb.append(c);
                    }
                    break;

                case '#':
                    digits.set(sb.length());
                    sb.append(wholePart ? ' ' : '0');
                    break;

                case '0':
                    digits.set(sb.length());
                    sb.append('0');
                    break;

                default:
                    sb.append(c);
                    break;
            }
        }

        // now we replace digit positions with digits from the number, in reverse order
        int lastPos = sb.length();
        int sepCount = 0;
        while (digitPos++ < number.length()) {
            int pos = digits.previousSetBit(lastPos);
            if (pos < 0 || digitPos == number.length() - 1 && number.charAt(digitPos) == '-') break;
            if (wholePart && separator != null && !separator.isEmpty() && separatorFrequency > 0 && ++sepCount >= separatorFrequency && (separatorFrequency > 1 || lastPos < sb.length())) {
                // add separator and digit
                sb.replace(pos, pos + 1, String.valueOf(number.charAt(number.length() - digitPos)) + separator);
                sepCount = 0;
            } else {
                sb.replace(pos, pos + 1, String.valueOf(number.charAt(number.length() - digitPos)));
            }

            lastPos = pos - 1;
        }

        if (wholePart && digitPos == number.length() - 1 && number.charAt(digitPos) == '-') {
            // have a -ve sign, we put it in the first position if 0 filled or last pos if space filled, if there is room
            int pos = digits.previousSetBit(lastPos);
            if (pos >= 0) {
                if (sb.charAt(pos) == '0') {
                    while (pos >= 0 && sb.charAt(pos) == '0') pos--;
                    pos++;
                    if (sb.charAt(pos) == '0' && pos + 1 < sb.length() && sb.charAt(pos + 1) == '0') {
                        sb.replace(pos + 1, pos + 2, "-");
                    }
                } else {
                    // space filled, put it here
                    sb.replace(pos, pos + 1, "-");
                }
            }
        }
        return digitPos;
    }

    /**
     * Convert a number to a sequence for given numbering base
     *
     * @param number number to convert
     * @param alwaysBaseComplement
     * @return character sequence of that number in selected base
     */
    @NotNull
    public static BasedSequence convertNumber(long number, long base, boolean alwaysBaseComplement) {
        assert base >= NumberingBaseType.MIN_BASE && base <= NumberingBaseType.MAX_BASE;

        char[] sb;
        int i;
        if (number == 0) {
            sb = new char[] { '0' };
            i = 0;
        } else {
            if (number < 0) {
                if (base == 10 && !alwaysBaseComplement) {
                    if (number == MIN_VALUE) {
                        sb = String.valueOf(MIN_VALUE).toCharArray();
                        i = 0;
                    } else {
                        long remainder = -number;
                        sb = new char[MAX_CHARS_BASE[(int) base]];
                        i = sb.length;
                        while (remainder != 0) {
                            int c = (int) (remainder % base);
                            remainder = remainder / base;
                            if (c <= 9) sb[--i] = (char) (c + '0');
                            else sb[--i] = (char) (c - 10 + 'A');
                        }
                        sb[--i] = '-';
                    }
                } else {
                    long remainder = number;
                    sb = new char[MAX_CHARS_BASE[(int) base]];
                    i = sb.length;
                    int c;

                    if (base == 2) {
                        while (i > 0) {
                            c = (int) (remainder % base);
                            if (c < 0) c += base;
                            remainder -= c; // ensure we have -ve extension shifted in
                            remainder = remainder / base;
                            if (c <= 9) sb[--i] = (char) (c + '0');
                            else sb[--i] = (char) (c - 10 + 'A');
                        }
                    } else {
                        while (i > 0) {
                            c = (int) (remainder % base);
                            if (c < 0) c += base;
                            if (i == 1) {
                                int tmp = 0;
                            }
                            if (remainder >= Long.MIN_VALUE+c) remainder -= c; // ensure we have -ve extension shifted in
                            remainder = remainder / base;
                            if (c <= 9) sb[--i] = (char) (c + '0');
                            else sb[--i] = (char) (c - 10 + 'A');
                        }
                    }
                }
            } else {
                long remainder = number;
                sb = new char[MAX_CHARS_BASE[(int) base]];
                i = sb.length;
                while (remainder != 0) {
                    int c = (int) (remainder % base);
                    remainder = remainder / base;
                    if (c <= 9) sb[--i] = (char) (c + '0');
                    else sb[--i] = (char) (c - 10 + 'A');
                }
            }
        }
        return new CharBasedSequence(sb).subSequence(i, sb.length);
    }
}
