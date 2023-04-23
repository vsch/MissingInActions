// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.character.custom;

import com.vladsch.MissingInActions.actions.character.AbstractNextOrPrevWordHandler;
import com.vladsch.MissingInActions.settings.ApplicationSettings;

public class NextOrPrevWordStartOrEndHandler extends AbstractNextOrPrevWordHandler {
    public enum BoundaryActionType {
        WORD,
        WORD_START,
        WORD_END,
    }

    final private BoundaryActionType myActionType;
    final private boolean myNext;
    final private boolean myWithSelection;
    final private boolean myInDifferentHumpsMode;
    final private ApplicationSettings mySettings;

    public NextOrPrevWordStartOrEndHandler(boolean next, boolean withSelection, boolean inDifferentHumpsMode, BoundaryActionType actionType) {
        super();

        myActionType = actionType;
        myNext = next;
        myWithSelection = withSelection;
        myInDifferentHumpsMode = inDifferentHumpsMode;

        mySettings = ApplicationSettings.getInstance();
    }

    public boolean isNext() {
        return myNext;
    }

    public boolean isWithSelection() {
        return myWithSelection;
    }

    public boolean isInDifferentHumpsMode() {
        return myInDifferentHumpsMode;
    }

    public int getBoundaryFlags() {
        if (myActionType == BoundaryActionType.WORD_END) {
            return myNext ? mySettings.getCustomizedNextWordEndBounds() : mySettings.getCustomizedPrevWordEndBounds();
        }
        if (myActionType == BoundaryActionType.WORD_START) {
            return myNext ? mySettings.getCustomizedNextWordStartBounds() : mySettings.getCustomizedPrevWordStartBounds();
        }
        return myNext ? mySettings.getCustomizedNextWordBounds() : mySettings.getCustomizedPrevWordBounds();
    }
}
