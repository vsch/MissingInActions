// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.settings;

import org.jetbrains.annotations.NotNull;

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
