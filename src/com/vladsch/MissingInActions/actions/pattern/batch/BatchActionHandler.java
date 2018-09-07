/*
 * Copyright (c) 2016-2018 Vladimir Schneider <vladimir.schneider@gmail.com>
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.vladsch.MissingInActions.actions.pattern.batch;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.vladsch.MissingInActions.BatchSearchReplaceToolWindow;
import com.vladsch.MissingInActions.PluginProjectComponent;
import com.vladsch.MissingInActions.actions.pattern.BatchReplaceForm;
import com.vladsch.MissingInActions.util.CancelableJobScheduler;
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
            BatchSearchReplaceToolWindow toolWindow = PluginProjectComponent.getInstance(project).getSearchReplaceToolWindow();
            if (toolWindow != null) {
                BatchReplaceForm batchSearchReplace = toolWindow.getBatchSearchReplace();
                if (batchSearchReplace != null) {
                    batchSearchReplace.setActiveEditor((EditorEx) editor);
                    return batchSearchReplace.isActionEnabled(myAction);
                }
            }
        }
        return false;
    }

    @Override
    protected void doExecute(Editor editor, @Nullable Caret caret, DataContext dataContext) {
        Project project = dataContext.getData(CommonDataKeys.PROJECT);
        if (editor instanceof EditorEx && project != null) {
            BatchSearchReplaceToolWindow toolWindow = PluginProjectComponent.getInstance(project).getSearchReplaceToolWindow();
            if (toolWindow != null) {
                BatchReplaceForm batchSearchReplace = toolWindow.getBatchSearchReplace();
                if (batchSearchReplace != null) {
                    batchSearchReplace.setActiveEditor((EditorEx) editor);
                    batchSearchReplace.doAction(myAction);
                    CancelableJobScheduler.getInstance().schedule(() -> {
                        editor.getContentComponent().requestFocus();
                    }, 100);
                }
            }
        }
    }
}
