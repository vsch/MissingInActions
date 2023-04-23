// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.character.custom;

import com.vladsch.MissingInActions.actions.DumbAwareTextComponentEditorAction;
import com.vladsch.MissingInActions.actions.LineSelectionAware;

public class PreviousWordEndInDifferentHumpsModeAction extends DumbAwareTextComponentEditorAction implements LineSelectionAware {
    public PreviousWordEndInDifferentHumpsModeAction() {
        super(new NextOrPrevWordEndHandler(false, false, true));
    }
}
