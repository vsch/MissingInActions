// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.character.custom;

public class NextOrPrevWordEndHandler extends NextOrPrevWordStartOrEndHandler {
    public NextOrPrevWordEndHandler(boolean next, boolean withSelection, boolean inDifferentHumpsMode) {
        super(next, withSelection, inDifferentHumpsMode, BoundaryActionType.WORD_END);
    }
}
