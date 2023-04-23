// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.settings;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class BatchSearchReplace implements Serializable {
    boolean myCaseSensitive = true;
    boolean myWholeWord = true;
    @NotNull String mySearchText = "";
    @NotNull String myReplaceText = "";
    @NotNull String myOptionsText = "";

    public BatchSearchReplace() {

    }

    public BatchSearchReplace(final boolean caseSensitive, final boolean wholeWord, @NotNull final String searchText, @NotNull final String replaceText, @NotNull final String optionsText) {
        myCaseSensitive = caseSensitive;
        myWholeWord = wholeWord;
        mySearchText = searchText;
        myReplaceText = replaceText;
        myOptionsText = optionsText;
    }

    public BatchSearchReplace(final BatchSearchReplace other) {
        myCaseSensitive = other.myCaseSensitive;
        myWholeWord = other.myWholeWord;
        mySearchText = other.mySearchText;
        myReplaceText = other.myReplaceText;
        myOptionsText = other.myOptionsText;
    }

    public void copyFrom(final BatchSearchReplace other) {
        myCaseSensitive = other.myCaseSensitive;
        myWholeWord = other.myWholeWord;
        mySearchText = other.mySearchText;
        myReplaceText = other.myReplaceText;
        myOptionsText = other.myOptionsText;
    }

    public boolean isCaseSensitive() {
        return myCaseSensitive;
    }

    public void setCaseSensitive(final boolean batchSearchCaseSensitive) {
        myCaseSensitive = batchSearchCaseSensitive;
    }

    public boolean isWholeWord() {
        return myWholeWord;
    }

    public void setWholeWord(final boolean batchSearchWholeWord) {
        myWholeWord = batchSearchWholeWord;
    }

    @NotNull
    public String getSearchText() {
        return mySearchText;
    }

    public void setSearchText(@NotNull final String batchSearchText) {
        mySearchText = batchSearchText;
    }

    @NotNull
    public String getReplaceText() {
        return myReplaceText;
    }

    public void setReplaceText(@NotNull final String batchSearchReplaceText) {
        myReplaceText = batchSearchReplaceText;
    }

    @NotNull
    public String getOptionsText() {
        return myOptionsText;
    }

    public void setOptionsText(@NotNull final String batchSearchOptionsText) {
        // need to change leading blank lines to . so they are stored in XML
        myOptionsText = batchSearchOptionsText;
    }
}
