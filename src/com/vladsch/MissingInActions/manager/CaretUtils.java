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
