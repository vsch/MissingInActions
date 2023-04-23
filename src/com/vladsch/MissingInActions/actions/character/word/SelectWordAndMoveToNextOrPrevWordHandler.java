// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.character.word;

import com.vladsch.MissingInActions.actions.character.AbstractSelectAndMoveToNextOrPrevWordHandler;

import static com.vladsch.MissingInActions.util.EditHelpers.*;

public class SelectWordAndMoveToNextOrPrevWordHandler extends AbstractSelectAndMoveToNextOrPrevWordHandler {
    final private boolean myNext;
    final private boolean myInDifferentHumpsMode;
    final private int myBoundaryFlags;

    public boolean isNext() {
        return myNext;
    }

    public boolean isInDifferentHumpsMode() {
        return myInDifferentHumpsMode;
    }

    public int getBoundaryFlags() {
        return myBoundaryFlags;
    }

    public SelectWordAndMoveToNextOrPrevWordHandler(boolean next, boolean inDifferentHumpsMode) {
        super();
        myNext = next;
        myInDifferentHumpsMode = inDifferentHumpsMode;
        //noinspection ConstantConditionalExpression
        myBoundaryFlags = (false ? START_OF_LINE : 0)
                | (false ? END_OF_LINE : 0)
                | (false ? START_OF_TRAILING_BLANKS | END_OF_LEADING_BLANKS : 0)
                | (false ? MIA_IDENTIFIER : 0)
                | (true ? START_OF_WORD : 0)
                | (true ? END_OF_WORD : 0)
                | (false ? START_OF_FOLDING_REGION : 0)
                | (false ? END_OF_FOLDING_REGION : 0)
                | (false ? MULTI_CARET_SINGLE_LINE : 0);
    }
}
