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
import org.jetbrains.annotations.Nullable;

public class NumberingOptions extends NumberingBaseOptions {
    private String myFirst;
    private String myLast;
    private int myNumberingBase;

    public NumberingOptions() {
        this(NumberingBaseType.ADAPTER.getDefault().getIntValue(), "");
    }
    
    public NumberingOptions(int numberingBase, @NotNull String prefix) {
        super(prefix);
        myFirst = "0";
        myLast = "";
        myNumberingBase = numberingBase;
    }

    public NumberingOptions(@NotNull NumberingOptions other) {
        super(other);
        myFirst = other.myFirst;
        myLast = other.myLast;
        myNumberingBase = other.myNumberingBase;
    }
    
    public void copyFrom(@NotNull NumberingOptions other) {
        super.copyFrom(other);
        
        myFirst = other.myFirst;
        myLast = other.myLast;
        myNumberingBase = other.myNumberingBase;
    }

    public NumberingOptions(@NotNull NumberingBaseOptions baseOptions, @NotNull NumberingOptions other) {
        super(baseOptions);
        myFirst = other.myFirst;
        myLast = other.myLast;
        myNumberingBase = other.myNumberingBase;
    }

    public NumberingOptions copy() {
        return new NumberingOptions(this);
    }

    public NumberingBaseType getNumberingBaseType() {
        return NumberingBaseType.ADAPTER.get(myNumberingBase);
    }

    // @formatter:off
    public String getFirst() { return myFirst; }
    public String getLast() { return myLast; }
    public int getNumberingBase() { return myNumberingBase; }
    // @formatter:on

    // @formatter:off
    public void setFirst(String first) { myFirst = first; }
    public void setLast(String last) { myLast = last; }
    // @formatter:on
    public void setNumberingBase(int numberingBase) {
        assert numberingBase >= NumberingBaseType.MIN_BASE && numberingBase <= NumberingBaseType.MAX_BASE;
        myNumberingBase = numberingBase;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NumberingOptions)) return false;
        if (!super.equals(o)) return false;

        NumberingOptions options = (NumberingOptions) o;

        if (myNumberingBase != options.myNumberingBase) return false;
        if (!myFirst.equals(options.myFirst)) return false;
        return myLast.equals(options.myLast);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + myFirst.hashCode();
        result = 31 * result + myLast.hashCode();
        result = 31 * result + myNumberingBase;
        return result;
    }
}
