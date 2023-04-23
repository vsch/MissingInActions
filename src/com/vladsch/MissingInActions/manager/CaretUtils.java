// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.manager;

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.LogicalPosition;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CaretUtils {
    @Nullable
    public static Set<Long> getExcludedCoordinates(@Nullable Set<Long> excludeSet, @Nullable Collection<Caret> exclude) {
        if (exclude == null || exclude.isEmpty()) return excludeSet;
        if (excludeSet == null) excludeSet = new HashSet<>(exclude.size());
        for (Caret caret : exclude) {
            excludeSet.add(getCoordinates(caret));
        }
        return excludeSet;
    }

    public static long getCoordinates(Caret caret) {
        return getCoordinates(caret.getLogicalPosition());
    }

    public static long getCoordinates(LogicalPosition logicalPosition) {
        return ((long) logicalPosition.line << 32) | (logicalPosition.column);
    }
}
