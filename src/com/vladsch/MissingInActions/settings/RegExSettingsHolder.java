// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.settings;

import org.jetbrains.annotations.NotNull;

public interface RegExSettingsHolder {
    @NotNull
    String getPatternText();
    @NotNull
    String getSampleText();
    void setPatternText(String pattern1);
    void setSampleText(String sampleText);
    boolean isCaseSensitive();
    boolean isBackwards();
    boolean isCaretToGroupEnd();
    void setCaseSensitive(boolean isCaseSensitive);
    void setBackwards(boolean isBackwards);
    void setCaretToGroupEnd(boolean isCaretToGroupEnd);
}
