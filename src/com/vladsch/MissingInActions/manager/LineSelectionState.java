// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.manager;

@SuppressWarnings("WeakerAccess")
public class LineSelectionState {
    final public int anchorColumn;
    final public boolean isStartAnchor;

    LineSelectionState(int anchorColumn, boolean isStartAnchor) {
        this.anchorColumn = anchorColumn;
        this.isStartAnchor = isStartAnchor;
    }

    LineSelectionState(LineSelectionState other) {
        this.anchorColumn = other.anchorColumn;
        this.isStartAnchor = other.isStartAnchor;
    }

    @Override
    public String toString() {
        return "LineSelectionState{" +
                "anchorColumn=" + anchorColumn +
                ", isStartAnchor=" + isStartAnchor +
                '}';
    }
}
