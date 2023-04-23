// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.util;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface TextOffsetConsumer {
    void accept(int textIndex, @Nullable String text, int textOffset, int rangeIndex, @NotNull TextRange foundRange, @NotNull TextRange replacedRange,  @Nullable String foundText);
}
