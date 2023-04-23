// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.pattern.batch;

import com.vladsch.MissingInActions.actions.DumbAwareTextComponentEditorAction;
import com.vladsch.MissingInActions.actions.pattern.BatchReplaceForm;

@SuppressWarnings("WeakerAccess")
public class BatchFindPreviousAction extends DumbAwareTextComponentEditorAction {
    public BatchFindPreviousAction() {
        super(new BatchActionHandler(BatchReplaceForm.FIND_PREV));
    }
}
