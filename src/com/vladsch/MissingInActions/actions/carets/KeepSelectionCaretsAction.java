// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: May 14, 2002
 * Time: 7:40:40 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.vladsch.MissingInActions.actions.carets;

import com.vladsch.MissingInActions.actions.LineSelectionAware;

public class KeepSelectionCaretsAction extends RemoveLineCaretsActionBase implements LineSelectionAware {
    public KeepSelectionCaretsAction() {
        super(OpType.REMOVE_WITHOUT_SELECTION);
    }
}
