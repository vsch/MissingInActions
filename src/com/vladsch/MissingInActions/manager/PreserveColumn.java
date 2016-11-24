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

package com.vladsch.MissingInActions.manager;

public enum PreserveColumn {
    PRESERVE_COLUMN(1),
    INDENT_RELATIVE(2),
    WITH_SELECTION(4),
    WITH_LINES(8),
    WITH_LINE_SELECTION(16);

    final int flags;
    
    public static boolean has(int flags, PreserveColumn option) {
        return (flags & option.flags) != 0;
    }

    public static int getFlags(PreserveColumn...options) {
        int flags = 0;
        for (PreserveColumn option : options) {
            flags |= option.flags;
        }
        return flags;
    }

    PreserveColumn(int flags) {
        this.flags = flags;
    }
}
