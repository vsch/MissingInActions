// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.character;

@SuppressWarnings("WeakerAccess")
public class NextOrPrevWordStartOrEndHandler extends AbstractNextOrPrevWordHandler {
    final private boolean myNext;
    final private boolean myWithSelection;
    final private boolean myInDifferentHumpsMode;
    final private int myBoundaryFlags;

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
        return myBoundaryFlags;
    }

    public NextOrPrevWordStartOrEndHandler(boolean next, boolean withSelection, boolean inDifferentHumpsMode, int boundaryFlags) {
        super();
        myNext = next;
        myWithSelection = withSelection;
        myInDifferentHumpsMode = inDifferentHumpsMode;
        myBoundaryFlags = boundaryFlags;
    }
}
