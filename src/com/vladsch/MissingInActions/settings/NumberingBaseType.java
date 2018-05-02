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

import com.vladsch.MissingInActions.util.ui.ComboBoxAdaptable;
import com.vladsch.MissingInActions.util.ui.ComboBoxAdapter;
import com.vladsch.MissingInActions.util.ui.ComboBoxAdapterImpl;
import org.jetbrains.annotations.NotNull;

public enum NumberingBaseType implements ComboBoxAdaptable<NumberingBaseType> {
    BASE_10(10, "10 - 0-9"),
    BASE_16(16, "16 - 0-9, A-F"),
    BASE_2(2, "2 - 0-1"),
    BASE_8(8, "8 - 0-7"),
    BASE_3(3, "3 - 0-2"),
    BASE_4(4, "4 - 0-3"),
    BASE_5(5, "5 - 0-4"),
    BASE_6(6, "6 - 0-5"),
    BASE_7(7, "7 - 0-6"),
    BASE_9(9, "9 - 0-8"),
    BASE_11(11, "11 - 0-9, A"),
    BASE_12(12, "12 - 0-9, A-B"),
    BASE_13(13, "13 - 0-9, A-C"),
    BASE_14(14, "14 - 0-9, A-D"),
    BASE_15(15, "15 - 0-9, A-E"),
    BASE_17(17, "17 - 0-9, A-G"),
    BASE_18(18, "18 - 0-9, A-H"),
    BASE_19(19, "19 - 0-9, A-I"),
    BASE_20(20, "20 - 0-9, A-J"),
    BASE_21(21, "21 - 0-9, A-K"),
    BASE_22(22, "22 - 0-9, A-L"),
    BASE_23(23, "23 - 0-9, A-M"),
    BASE_24(24, "24 - 0-9, A-N"),
    BASE_25(25, "25 - 0-9, A-O"),
    BASE_26(26, "26 - 0-9, A-P"),
    BASE_27(27, "27 - 0-9, A-Q"),
    BASE_28(28, "28 - 0-9, A-R"),
    BASE_29(29, "29 - 0-9, A-S"),
    BASE_30(30, "30 - 0-9, A-T"),
    BASE_31(31, "31 - 0-9, A-U"),
    BASE_32(32, "32 - 0-9, A-V"),
    BASE_33(33, "33 - 0-9, A-W"),
    BASE_34(34, "34 - 0-9, A-X"),
    BASE_35(35, "35 - 0-9, A-Y"),
    BASE_36(36, "36 - 0-9, A-Z");

    public static final int MAX_BASE = 36;
    public static final int MIN_BASE = 2;

    public SeparatorFrequencyType getSeparatorFrequencyType() {
        switch (intValue) {
            case 10:
                return SeparatorFrequencyType.EVERY_3;
            case 16:
                return SeparatorFrequencyType.EVERY_4;
            case 2:
                return SeparatorFrequencyType.EVERY_8;
            case 8:
                return SeparatorFrequencyType.EVERY_3;
            default:
                return SeparatorFrequencyType.NONE;
        }
    }

    public String getDefaultSeparator() {
        switch (intValue) {
            case 10:
                return ",";
            case 16:
                return "_";
            case 2:
                return "_";
            case 8:
                return "_";
            default:
                return "";
        }
    }

    public String getDefaultTemplate() {
        switch (intValue) {
            case 10:
                return "0";
            case 16:
                return "0x0";
            case 2:
                return "0b0";
            case 8:
                return "00";
            default:
                return "";
        }
    }

    public final int intValue;
    public final @NotNull String displayName;

    NumberingBaseType(int intValue, @NotNull String displayName) {
        this.intValue = intValue;
        this.displayName = displayName;
    }

    public static Static<NumberingBaseType> ADAPTER = new Static<>(new ComboBoxAdapterImpl<>(BASE_10));

    @Override
    public ComboBoxAdapter<NumberingBaseType> getAdapter() {
        return ADAPTER;
    }

    @Override
    public int getIntValue() { return intValue; }

    @NotNull
    public String getDisplayName() { return displayName; }

    @NotNull
    public NumberingBaseType[] getValues() { return values(); }
}
