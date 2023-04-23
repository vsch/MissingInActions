// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.character.identifier;

import com.vladsch.MissingInActions.actions.character.NextOrPrevWordStartOrEndHandler;

import static com.vladsch.MissingInActions.util.EditHelpers.*;

public class NextOrPrevWordEndHandler extends NextOrPrevWordStartOrEndHandler {
    public NextOrPrevWordEndHandler(boolean next, boolean withSelection, boolean inDifferentHumpsMode) {
        //noinspection ConstantConditionalExpression
        super(next, withSelection, inDifferentHumpsMode,
                (true ? START_OF_LINE : 0)
                        | (true ? END_OF_LINE : 0)
                        | (true ? START_OF_TRAILING_BLANKS | END_OF_LEADING_BLANKS : 0)
                        | (true ? MIA_IDENTIFIER : 0)
                        | (false ? START_OF_WORD : 0)
                        | (true ? END_OF_WORD : 0)
                        | (false ? START_OF_FOLDING_REGION : 0)
                        | (false ? END_OF_FOLDING_REGION : 0)
                        | (false ? MULTI_CARET_SINGLE_LINE : 0)
        );
    }
}
