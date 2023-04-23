// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.util;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;

public interface EditorActionListener {
    @NotNull
    Editor getEditor();

    void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event);

    /**
     * Note that using <code>dataContext</code> in implementing methods is unsafe - it could have been invalidated by the performed action.
     */
    void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event);

    void beforeEditorTyping(char c, DataContext dataContext);
}
