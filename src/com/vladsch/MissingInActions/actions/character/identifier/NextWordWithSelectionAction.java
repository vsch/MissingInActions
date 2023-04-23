// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: May 14, 2002
 * Time: 6:49:27 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.vladsch.MissingInActions.actions.character.identifier;

import com.vladsch.MissingInActions.actions.DumbAwareTextComponentEditorAction;
import com.vladsch.MissingInActions.actions.LineSelectionAware;

public class NextWordWithSelectionAction extends DumbAwareTextComponentEditorAction implements LineSelectionAware {
    public NextWordWithSelectionAction() {
        super(new NextOrPrevWordHandler(true, true, false));
    }
}
