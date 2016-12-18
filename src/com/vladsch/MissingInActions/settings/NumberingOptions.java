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

import com.vladsch.MissingInActions.util.ui.Immutable;
import com.vladsch.MissingInActions.util.ui.Mutable;
import org.jetbrains.annotations.NotNull;

public class NumberingOptions implements Immutable<NumberingOptions, NumberingOptions.AsMutable> {
    protected String myFirst;
    protected String myLast;
    protected String myTemplate;
    protected String myStep;
    protected int myNumberingBase;
    protected boolean myBitShift;
    protected boolean myRepeatSameLine;

    public NumberingOptions() {
        myFirst = "0";
        myLast = "";
        myTemplate = "";
        myStep = "1";
        myNumberingBase = NumberingBaseType.ADAPTER.getDefault().getIntValue();
        myBitShift = false;
        myRepeatSameLine = true;
    }

    public NumberingOptions(@NotNull NumberingOptions other) {
        myFirst = other.myFirst;
        myLast = other.myLast;
        myTemplate = other.myTemplate;
        myStep = other.myStep;
        myNumberingBase = other.myNumberingBase;
        myBitShift = other.myBitShift;
        myRepeatSameLine = other.myRepeatSameLine;
    }
    
    public NumberingOptions copy() {
        return new NumberingOptions(this);
    }

    public NumberingBaseType getNumberingBaseType() {
        return NumberingBaseType.ADAPTER.get(myNumberingBase);
    }

    @Override
    public AsMutable toMutable() {
        return new AsMutable();
    }

    // @formatter:off
    public boolean isBitShift() { return myBitShift; } 
    public boolean isRepeatSameLine() { return myRepeatSameLine; } 
    public int getNumberingBase() { return myNumberingBase; } 
    public String getFirst() { return myFirst; } 
    public String getLast() { return myLast; } 
    public String getStep() { return myStep; } 
    public String getTemplate() { return myTemplate; } 
    // @formatter:on
    
    // @formatter:off
    protected NumberingOptions setBitShift(boolean bitShift) { this.myBitShift = bitShift; return this; } 
    protected NumberingOptions setFirst(String first) { this.myFirst = first; return this; } 
    protected NumberingOptions setLast(String last) { this.myLast = last; return this; } 
    protected NumberingOptions setNumberingBase(int numberingBase) { this.myNumberingBase = numberingBase; return this; } 
    protected NumberingOptions setRepeatSameLine(boolean repeatSameLine) { this.myRepeatSameLine = repeatSameLine; return this; } 
    protected NumberingOptions setStep(String step) { this.myStep = step; return this; } 
    protected NumberingOptions setTemplate(String template) { this.myTemplate = template; return this; } 
    // @formatter:on
    
    public static class AsMutable extends NumberingOptions implements Mutable<NumberingOptions.AsMutable, NumberingOptions> {
        // @formatter:off
        public NumberingOptions.AsMutable setBitShift(boolean bitShift) { return (AsMutable) super.setBitShift(bitShift); } 
        public NumberingOptions.AsMutable setFirst(String first) { return (AsMutable) super.setFirst(first); } 
        public NumberingOptions.AsMutable setLast(String last) { return (AsMutable) super.setLast(last); } 
        public NumberingOptions.AsMutable setNumberingBase(int numberingBase) { return (AsMutable) super.setNumberingBase(numberingBase); } 
        public NumberingOptions.AsMutable setRepeatSameLine(boolean repeatSameLine) { return (AsMutable) super.setRepeatSameLine(repeatSameLine); } 
        public NumberingOptions.AsMutable setStep(String step) { return (AsMutable) super.setStep(step); } 
        public NumberingOptions.AsMutable setTemplate(String template) { return (AsMutable) super.setTemplate(template); } 
        // @formatter:on

        public AsMutable() {
        }

        public AsMutable(@NotNull NumberingOptions other) {
            super(other);
        }
        
        public NumberingOptions.AsMutable copy() {
            return new AsMutable(this);
        }

        @Override
        public NumberingOptions toImmutable() {
            return new NumberingOptions(this);
        }
    } 
    
}
