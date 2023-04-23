// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions;

import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import org.jetbrains.annotations.NotNull;

abstract public class DumbAwareEditorAction extends EditorAction implements LineSelectionAware {
    public DumbAwareEditorAction(@NotNull EditorActionHandler defaultHandler) {
        super(defaultHandler);
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }
}
