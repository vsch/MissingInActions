// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.util;

import com.vladsch.MissingInActions.settings.NumberingBaseType;
import com.vladsch.MissingInActions.settings.NumberingOptions;
import com.vladsch.flexmark.util.misc.CharPredicate;
import com.vladsch.flexmark.util.misc.Ref;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;

import static java.lang.Long.MIN_VALUE;

public class NumberSequenceGenerator {
    private final NumberingOptions myOptions;
    private final long myFirst;
    private final Long myLast;
    private final long myStep;
    private Long myNext;
    private Integer myLastLine;

    private int firstNumberBase;
    private int lastNumberBase;

    private int stepNumberBase;

    public static final NumberingOptions defaultHex = new NumberingOptions(16, "0x");
    public static final NumberingOptions defaultBin = new NumberingOptions(2, "0b");
    public static final NumberingOptions defaultDec = new NumberingOptions(10, "");
    public static final NumberingOptions defaultOct = new NumberingOptions(8, "0");

    public NumberSequenceGenerator(@NotNull NumberingOptions options, long first, long step, Long last) {
        myOptions = options;
        myFirst = first;
        myStep = step;
        myLast = last;
        myNext = options.isBitShift() && first == 0 ? 1 : first;
        myLastLine = null;
    }

    public int getFirstNumberBase() { return firstNumberBase; }

    public int getLastNumberBase() { return lastNumberBase; }

    public int getStepNumberBase() { return stepNumberBase; }

    /**
     * Extract a number from given sequence based on prefix/suffix, base and template
     *
     * @param charSequence character sequence from which to extract a number
     *
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

    @NotNull
    public String getNumber() {
        if (myNext != null) {
            long next = myNext;
            BasedSequence number = convertNumber(next, myOptions.getNumberingBase(), false);
            if (!myOptions.isUpperCase()) number = number.toLowerCase();
            return templateNumber(number);
        }
        return "";
    }

    public void next(int lineNumber) {
        if (myNext != null && myLastLine != null && (myLastLine != lineNumber || !myOptions.isRepeatSameLine())) {
            // advance next count
            long next = myNext;

            if (myStep > 0) {
                if (myOptions.isBitShift()) {
                    if (next == 0) {
                        next = 1;
                    } else {
                        long rollUpper = (0xffff_ffff_ffff_ffffL << 64 - myStep);
                        long rollLower = (0xffff_ffff_ffff_ffffL >>> 64 - myStep);
                        long prevUpper = next & rollUpper;
                        long prev = next & prevUpper;
                        long prevLower = (prev >>> 64 - myStep) & rollLower;
                        next = ((next << (int) myStep) & ~rollLower) | prevLower;
                    }
                } else {
                    next += myStep;
                }
                myNext = myLast == null || next <= myLast ? next : null;
            } else if (myStep < 0) {
                if (myOptions.isBitShift()) {
                    if (next == 0) {
                        next = 0x8000_0000_0000_0000L;
                    } else {
                        long rollUpper = (0xffff_ffff_ffff_ffffL << 64 + myStep);
                        long rollLower = (0xffff_ffff_ffff_ffffL >>> 64 + myStep);
                        long prevLower = next & rollLower;
                        long prev = next & prevLower;
                        long prevUpper = (prev << 64 + myStep) & rollUpper;
                        next = ((next >>> -myStep) & ~rollUpper) | prevUpper;
                    }
                } else {
                    next += myStep;
                }
                myNext = myLast == null || next >= myLast ? next : null;
            }
        }

        myLastLine = lineNumber;
    }

    /**
     * Extract a number from given sequence based on prefix/suffix, base and template
     *
     * @param charSequence character sequence from which to extract a number
     *
     * @return number or null if unable to extract
     */
    public static long extractNumber(@NotNull CharSequence charSequence, @NotNull NumberingOptions options, @Nullable Ref<Integer> refMaxDigit) {
        BasedSequence chars = BasedSequence.of(charSequence).trim();
        CharPredicate sign = CharPredicate.anyOf('-', '+');

        // trim +/- and count -ve, odd is negative, even is positive
        int numberBase = options.getNumberingBase();
        boolean negative = false;
        while (chars.startsWith(sign)) {
            if (chars.charAt(0) == '-') {
                negative = !negative;
            }
            chars = chars.subSequence(1);
        }
        chars = chars.trim();

        if (!options.getPrefix().trim().isEmpty()) {
            // must be a proper prefix, cannot equal to it
            if (!(chars.startsWith(options.getPrefix()) && !chars.equals(options.getPrefix()))) {
                if (refMaxDigit != null) refMaxDigit.value = numberBase;
                return 0;
            }
            chars = chars.removePrefix(options.getPrefix().trim());
        }
        if (!options.getSuffix().trim().isEmpty()) chars = chars.removeSuffix(options.getSuffix().trim());

        // see if we have a template and it matches somewhat
        long number = 0;
        int maxDigit = refMaxDigit != null ? refMaxDigit.value : numberBase;

        for (int i = 0; i < chars.length(); i++) {
            char c = chars.charAt(i);
            
            if (i == 0) maxDigit = 0;

            int digit = c >= '0' && c <= '9' ? c - '0' : c >= 'a' && c <= 'z' ? c - 'a' + 10 : c >= 'A' && c <= 'Z' ? c - 'A' + 10 : numberBase;
            if (maxDigit < digit) maxDigit = digit;
            if (digit < 0 || digit >= numberBase) continue;
            number *= numberBase;
            number += digit;
        }

        if (negative) {
            number = -number;
        }
        if (refMaxDigit != null) refMaxDigit.value = maxDigit;
        return number;
    }

    public static long tryExtractNumber(@NotNull String charSequence, @Nullable Ref<Integer> matchedNumberBase, @NotNull NumberingOptions... options) {
        long number = 0;

        if (matchedNumberBase != null) matchedNumberBase.value = 0;

        for (NumberingOptions numberingOptions : options) {
            Ref<Integer> maxDigit = new Ref<>(numberingOptions.getNumberingBase());
            number = extractNumber(charSequence, numberingOptions, maxDigit);
            if (maxDigit.value < numberingOptions.getNumberingBase()) {
                if (matchedNumberBase != null) matchedNumberBase.value = numberingOptions.getNumberingBase();
                break;
            }
        }

        return number;
    }

    @NotNull
    public static NumberSequenceGenerator create(@NotNull NumberingOptions options) {
        Ref<Integer> firstBase = new Ref<>(0);
        Ref<Integer> lastBase = new Ref<>(0);
        Ref<Integer> stepBase = new Ref<>(0);
        long first = tryExtractNumber(options.getFirst(), firstBase, defaultOct, options, defaultBin, defaultDec, defaultHex);
        Long last = options.getLast().trim().isEmpty() ? null : tryExtractNumber(options.getLast(), lastBase, defaultOct, options, defaultBin, defaultDec, defaultHex);
        long step = tryExtractNumber(options.getStep(), stepBase, defaultOct, options, defaultBin, defaultDec, defaultHex);
        if (step == 0) step = 1;

        NumberSequenceGenerator generator = new NumberSequenceGenerator(options, first, step, last);

        generator.firstNumberBase = firstBase.value;
        generator.lastNumberBase = lastBase.value;
        generator.stepNumberBase = stepBase.value;
        return generator;
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
     *
     * @return string of the number
     */
    @NotNull
    public String templateNumber(@NotNull CharSequence number) {
        return templateNumber(number,
                myOptions.getPrefix(),
                myOptions.getTemplate(),
                myOptions.getDecimalPoint(),
                myOptions.getSeparator(),
                myOptions.getSeparatorFrequency(),
                myOptions.getSuffix());
    }

    @NotNull
    public static String templateNumber(
            @NotNull CharSequence number,
            @Nullable String prefix,
            @Nullable String template,
            @Nullable String decimalPoint,
            @Nullable String separator,
            int separatorFrequency,
            @Nullable String suffix
    ) {
        StringBuilder sb = new StringBuilder();

        if (prefix != null && !prefix.isEmpty()) sb.append(prefix);

        if (template != null && !template.isEmpty()) {
            BasedSequence templateSeq = BasedSequence.of(template);
            BasedSequence wholePart = templateSeq;
            BasedSequence fractionPart = BasedSequence.NULL;

            if (decimalPoint != null && !decimalPoint.isEmpty()) {
                int pos = templateSeq.lastIndexOf(decimalPoint);

                if (pos >= 0) {
                    wholePart = templateSeq.subSequence(0, pos);
                    fractionPart = templateSeq.subSequence(pos + decimalPoint.length());

                    StringBuilder frac = new StringBuilder();

                    int digitPos = templatePart(frac, number, false, fractionPart, 0, null, 0);
                    if (!wholePart.isEmpty()) {
                        templatePart(sb, number, true, wholePart, digitPos, separator, separatorFrequency);
                    } else {
                        if (separator != null && !separator.isEmpty()) {
                            insertSeparators(sb, number, digitPos, separator, separatorFrequency);
                        } else {
                            sb.append(number.subSequence(0, number.length() - digitPos));
                        }
                    }

                    sb.append(decimalPoint);
                    sb.append(frac);
                } else {
                    templatePart(sb, number, true, wholePart, 0, separator, separatorFrequency);
                }
            } else {
                templatePart(sb, number, true, wholePart, 0, separator, separatorFrequency);
            }
        } else {
            if (separator != null && !separator.isEmpty()) {
                insertSeparators(sb, number, 0, separator, separatorFrequency);
            } else {
                sb.append(number.subSequence(0, number.length()));
            }
        }

        if (suffix != null && !suffix.isEmpty()) sb.append(suffix);
        return sb.toString();
    }

    private static void insertSeparators(StringBuilder sb, @NotNull CharSequence number, int digitPos, @Nullable String separator, int separatorFrequency) {
        int pos;
        BasedSequence sequence = BasedSequence.of(number);

        pos = number.length() - digitPos;
        String out = "";

        while (pos > separatorFrequency && separatorFrequency > 0) {
            out = number.subSequence(pos - separatorFrequency, pos) + out;
            pos -= separatorFrequency;
            if (sequence.subSequence(0, pos).isBlank()) break;
            out = separator + out;
        }

        if (pos > 0) {
            out = number.subSequence(0, pos) + out;
        }

        sb.append(out);
    }

    public static int templatePart(StringBuilder out, CharSequence number, boolean wholePart, CharSequence part, int digitPos, @Nullable String separator, int separatorFrequency) {
        // here we apply the part template to digits of the number, proceeding from the digitPos offset from last digit of number
        // first we need to know how many digit places will be used
        StringBuilder sb = new StringBuilder();
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
        while (digitPos < number.length()) {
            int pos = digits.previousSetBit(lastPos);
            if (pos < 0 || digitPos + 1 == number.length() && number.charAt(0) == '-') break;
            sb.replace(pos, pos + 1, String.valueOf(number.charAt(number.length() - ++digitPos)));

            lastPos = pos - 1;
        }

        if (wholePart && digitPos + 1 == number.length() && number.charAt(0) == '-') {
            // have a -ve sign, we put it in the first position if 0 filled or last pos if space filled, if there is room
            int pos = digits.previousSetBit(lastPos);
            if (pos >= 0) {
                if (sb.charAt(pos) == '0') {
                    while (pos >= 0 && sb.charAt(pos) == '0') pos--;
                    pos++;
                    if (sb.charAt(pos) == '0') {
                        sb.replace(pos, pos + 1, "-");
                        digitPos++;
                    }
                } else {
                    // space filled, put it here
                    sb.replace(pos, pos + 1, "-");
                    digitPos++;
                }
            }
        }

        if (wholePart && separator != null && !separator.isEmpty() && separatorFrequency > 0) {
            insertSeparators(out, sb, 0, separator, separatorFrequency);
        } else {
            out.append(sb);
        }

        return digitPos;
    }

    /**
     * Convert a number to a sequence for given numbering base
     *
     * @param number               number to convert
     * @param alwaysBaseComplement if true or base != 10 use base complement sequence, otherwise use - prefix for negative numbers
     *
     * @return character sequence of that number in selected base
     */

    // REFACTOR: Use Long.toUnsignedString(long base, int radix) for implementation
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
                            sb[--i] = (char) (c + '0');
                        }
                        sb[--i] = '-';
                    }
                } else {
                    long remainder = number;
                    sb = new char[MAX_CHARS_BASE[(int) base]];
                    i = sb.length;
                    int c;

                    while (i > 0) {
                        c = (int) (remainder % base);
                        if (c < 0) c += base;
                        if (remainder >= Long.MIN_VALUE + c) {
                            remainder -= c; // ensure we have -ve extension shifted in
                            remainder = remainder / base;
                        } else {
                            remainder = remainder / base;
                            remainder--;
                        }
                        if (c <= 9) sb[--i] = (char) (c + '0');
                        else sb[--i] = (char) (c - 10 + 'A');
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

        return BasedSequence.of(String.valueOf(sb, i, sb.length - i));
    }

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

    public static final long[] COMPLEMENT_BASE = {
            0,                      // 0
            0,                      // 1
            4611686018427387904L,   // 2 100000000000000000000000000000000000000000000000000000000000000
            4052555153018976267L,   // 3 1000000000000000000000000000000000000000
            4611686018427387904L,   // 4 10000000000000000000000000000000
            7450580596923828125L,   // 5 1000000000000000000000000000
            4738381338321616896L,   // 6 1000000000000000000000000
            3909821048582988049L,   // 7 10000000000000000000000
            1152921504606846976L,   // 8 100000000000000000000
            1350851717672992089L,   // 9 10000000000000000000
            1000000000000000000L,   // 10 1000000000000000000
            5559917313492231481L,   // 11 1000000000000000000
            2218611106740436992L,   // 12 100000000000000000
            8650415919381337933L,   // 13 100000000000000000
            2177953337809371136L,   // 14 10000000000000000
            6568408355712890625L,   // 15 10000000000000000
            1152921504606846976L,   // 16 1000000000000000
            2862423051509815793L,   // 17 1000000000000000
            6746640616477458432L,   // 18 1000000000000000
            799006685782884121L,    // 19 100000000000000
            1638400000000000000L,   // 20 100000000000000
            3243919932521508681L,   // 21 100000000000000
            6221821273427820544L,   // 22 100000000000000
            504036361936467383L,    // 23 10000000000000
            876488338465357824L,    // 24 10000000000000
            1490116119384765625L,   // 25 10000000000000
            2481152873203736576L,   // 26 10000000000000
            4052555153018976267L,   // 27 10000000000000
            6502111422497947648L,   // 28 10000000000000
            353814783205469041L,    // 29 1000000000000
            531441000000000000L,    // 30 1000000000000
            787662783788549761L,    // 31 1000000000000
            1152921504606846976L,   // 32 1000000000000
            1667889514952984961L,   // 33 1000000000000
            2386420683693101056L,   // 34 1000000000000
            3379220508056640625L,   // 35 1000000000000
            4738381338321616896L,   // 36 1000000000000
    };
}
