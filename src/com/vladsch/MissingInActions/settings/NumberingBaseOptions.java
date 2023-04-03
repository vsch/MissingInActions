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

package com.vladsch.MissingInActions.settings;

import org.jetbrains.annotations.NotNull;

public class NumberingBaseOptions {
    private String myStep;
    private boolean myBitShift;
    private boolean myRepeatSameLine;
    private String myTemplate;
    private String myPrefix;
    private String mySuffix;
    private String mySeparator;
    private String myDecimalPoint;
    private int mySeparatorFrequency;
    private boolean myUpperCase;

    public NumberingBaseOptions() {
        this("");
    }
    public NumberingBaseOptions(@NotNull String prefix) {
        myStep = "1";
        myBitShift = false;
        myRepeatSameLine = true;
        myTemplate = "";
        myPrefix = prefix;
        mySuffix = "";
        mySeparator = "";
        myDecimalPoint = "";
        mySeparatorFrequency = SeparatorFrequencyType.NONE.getIntValue();
        myUpperCase = true;
    }

    public NumberingBaseOptions(@NotNull NumberingBaseOptions other) {
        myStep = other.myStep;
        myBitShift = other.myBitShift;
        myRepeatSameLine = other.myRepeatSameLine;
        myTemplate = other.myTemplate;
        myPrefix = other.myPrefix;
        mySuffix = other.mySuffix;
        mySeparator = other.mySeparator;
        myDecimalPoint = other.myDecimalPoint;
        mySeparatorFrequency = other.mySeparatorFrequency;
        myUpperCase = other.myUpperCase;
    }

    public void copyFrom(@NotNull NumberingBaseOptions other) {
        myStep = other.myStep;
        myBitShift = other.myBitShift;
        myRepeatSameLine = other.myRepeatSameLine;
        myTemplate = other.myTemplate;
        myPrefix = other.myPrefix;
        mySuffix = other.mySuffix;
        mySeparator = other.mySeparator;
        myDecimalPoint = other.myDecimalPoint;
        mySeparatorFrequency = other.mySeparatorFrequency;
        myUpperCase = other.myUpperCase;
    }

    public NumberingBaseOptions copy() {
        return new NumberingBaseOptions(this);
    }

    // @formatter:off
    public String getStep() { return myStep; }
    public boolean isBitShift() { return myBitShift; }
    public boolean isRepeatSameLine() { return myRepeatSameLine; }
    public String getTemplate() { return myTemplate; }
    public String getPrefix() { return myPrefix; }
    public String getSuffix() { return mySuffix; }
    public String getSeparator() { return mySeparator; }
    public String getDecimalPoint() { return myDecimalPoint; }
    public boolean isUpperCase() { return myUpperCase; }
    public int getSeparatorFrequency() { return mySeparatorFrequency; }
    public void setStep(String step) { myStep = step; }
    public void setBitShift(boolean bitShift) { myBitShift = bitShift; }
    public void setRepeatSameLine(boolean repeatSameLine) { myRepeatSameLine = repeatSameLine; }
    public void setTemplate(String template) { myTemplate = template; }
    public void setPrefix(String prefix) { myPrefix = prefix; }
    public void setSuffix(String suffix) { mySuffix = suffix; }
    public void setSeparator(String separator) { mySeparator = separator; }
    public void setDecimalPoint(String decimalPoint) { myDecimalPoint = decimalPoint; }
    public void setSeparatorFrequency(int separatorFrequency) { mySeparatorFrequency = separatorFrequency; }
    public void setUpperCase(boolean upperCase) { myUpperCase = upperCase; }
    // @formatter:on

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NumberingBaseOptions)) return false;

        NumberingBaseOptions options = (NumberingBaseOptions) o;

        if (myBitShift != options.myBitShift) return false;
        if (myRepeatSameLine != options.myRepeatSameLine) return false;
        if (mySeparatorFrequency != options.mySeparatorFrequency) return false;
        if (!myStep.equals(options.myStep)) return false;
        if (!myTemplate.equals(options.myTemplate)) return false;
        if (!myPrefix.equals(options.myPrefix)) return false;
        if (!mySuffix.equals(options.mySuffix)) return false;
        if (!mySeparator.equals(options.mySeparator)) return false;
        if (!myDecimalPoint.equals(options.myDecimalPoint)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = myStep.hashCode();
        result = 31 * result + (myBitShift ? 1 : 0);
        result = 31 * result + (myRepeatSameLine ? 1 : 0);
        result = 31 * result + myTemplate.hashCode();
        result = 31 * result + myPrefix.hashCode();
        result = 31 * result + mySuffix.hashCode();
        result = 31 * result + mySeparator.hashCode();
        result = 31 * result + myDecimalPoint.hashCode();
        result = 31 * result + mySeparatorFrequency;
        return result;
    }
}

