// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.pattern.batch;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.vladsch.MissingInActions.PluginProjectComponent;
import com.vladsch.MissingInActions.actions.pattern.BatchReplaceForm;
import com.vladsch.MissingInActions.util.MiaCancelableJobScheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("WeakerAccess")
public class BatchActionHandler extends EditorActionHandler {
    private final String myAction;

    public BatchActionHandler(final String action) {
        super(false);
        myAction = action;
    }

    @Override
    protected boolean isEnabledForCaret(@NotNull final Editor editor, @NotNull final Caret caret, final DataContext dataContext) {
        Project project = dataContext.getData(CommonDataKeys.PROJECT);
        if (editor instanceof EditorEx && project != null) {
            BatchReplaceForm batchSearchReplace = PluginProjectComponent.getInstance(project).getBatchReplaceForm();
            if (batchSearchReplace != null) {
                batchSearchReplace.setActiveEditor((EditorEx) editor);
                return batchSearchReplace.isActionEnabled(myAction);
            }
        }
        return false;
    }

    @Override
    protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
        Project project = dataContext.getData(CommonDataKeys.PROJECT);
        if (editor instanceof EditorEx && project != null) {
            BatchReplaceForm batchSearchReplace = PluginProjectComponent.getInstance(project).getBatchReplaceForm();
            if (batchSearchReplace != null) {
                batchSearchReplace.setActiveEditor((EditorEx) editor);
                batchSearchReplace.doAction(myAction);
                MiaCancelableJobScheduler.getInstance().schedule(100, () -> {
                    editor.getContentComponent().requestFocus();
                });
            }
        }
    }
}
