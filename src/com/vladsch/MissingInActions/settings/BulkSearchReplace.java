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

import java.io.Serializable;

public class BulkSearchReplace implements Serializable {
    boolean myCaseSensitive = true;
    boolean myWholeWord = true;
    @NotNull String mySearchText = "";
    @NotNull String myReplaceText = "";
    @NotNull String myOptionsText = "";

    public BulkSearchReplace() {

    }

    BulkSearchReplace(final boolean caseSensitive, final boolean wholeWord, @NotNull final String searchText, @NotNull final String replaceText, @NotNull final String optionsText) {
        myCaseSensitive = caseSensitive;
        myWholeWord = wholeWord;
        mySearchText = searchText;
        myReplaceText = replaceText;
        myOptionsText = optionsText;
    }

    BulkSearchReplace(final BulkSearchReplace other) {
        myCaseSensitive = other.myCaseSensitive;
        myWholeWord = other.myWholeWord;
        mySearchText = other.mySearchText;
        myReplaceText = other.myReplaceText;
        myOptionsText = other.myOptionsText;
    }

    public void copyFrom(final BulkSearchReplace other) {
        myCaseSensitive = other.myCaseSensitive;
        myWholeWord = other.myWholeWord;
        mySearchText = other.mySearchText;
        myReplaceText = other.myReplaceText;
        myOptionsText = other.myOptionsText;
    }

    public boolean isCaseSensitive() {
        return myCaseSensitive;
    }

    public void setCaseSensitive(final boolean bulkSearchCaseSensitive) {
        myCaseSensitive = bulkSearchCaseSensitive;
    }

    public boolean isWholeWord() {
        return myWholeWord;
    }

    public void setWholeWord(final boolean bulkSearchWholeWord) {
        myWholeWord = bulkSearchWholeWord;
    }

    @NotNull
    public String getSearchText() {
        return mySearchText;
    }

    public void setSearchText(@NotNull final String bulkSearchText) {
        mySearchText = bulkSearchText;
    }

    @NotNull
    public String getReplaceText() {
        return myReplaceText;
    }

    public void setReplaceText(@NotNull final String bulkSearchReplaceText) {
        myReplaceText = bulkSearchReplaceText;
    }

    @NotNull
    public String getOptionsText() {
        return myOptionsText;
    }

    public void setOptionsText(@NotNull final String bulkSearchOptionsText) {
        myOptionsText = bulkSearchOptionsText;
    }
}
