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

package com.vladsch.MissingInActions.util;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class SearchPattern {
    private final @NotNull String myText;
    private final boolean myIsRegex;
    private Pattern myPattern;

    public SearchPattern(final @NotNull String text, final boolean isRegex) {
        myText = text;
        myIsRegex = isRegex;
    }

    @NotNull
    public String getText() {
        return myText;
    }

    public boolean isRegex() {
        return myIsRegex;
    }

    public String getPatternText() {
        return getPatternText(myText, myIsRegex, true);
    }

    public String getPatternText(boolean caseSensitive) {
        return getPatternText(myText, myIsRegex, caseSensitive);
    }

    public static String getPatternText(String text, boolean isRegex, boolean caseSensitive) {
        return isRegex ? text : caseSensitive ? String.format("\\Q%s\\E", text) : String.format("(?i:\\Q%s\\E)", text);
    }

    public Pattern getPattern() {
        if (myPattern == null) {
            myPattern = Pattern.compile(getPatternText());
        }
        return myPattern;
    }
}
