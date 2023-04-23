/*
 * Copyright (c) 2016-2023 Vladimir Schneider <vladimir.schneider@gmail.com>
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

package com.vladsch.MissingInActions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.vladsch.MissingInActions.actions.pattern.BatchReplaceForm;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;

class BatchSearchReplaceToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow window) {
        PluginProjectComponent projectComponent = PluginProjectComponent.getInstance(project);

        projectComponent.myBatchReplaceForm = new BatchReplaceForm(project, ApplicationSettings.getInstance());
        Disposer.register(projectComponent, projectComponent.myBatchReplaceForm);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        mainPanel.add(projectComponent.myBatchReplaceForm.getMainPanel());
        ContentFactory contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
        Content content = contentFactory.createContent(mainPanel, Bundle.message("batch-search.tool-window.title"), false);
        projectComponent.myToolWindow = window;
        window.getContentManager().addContent(content);
    }

    @Override
    public boolean isApplicable(@NotNull Project project) {
        // NOTE: disable for light project tests, project is never closed and leaves editors unreleased causing test failures.
        return !ApplicationManager.getApplication().isUnitTestMode() || !(project instanceof ProjectEx && ((ProjectEx)project).isLight());
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        // NOTE: disable for light project tests, project is never closed and leaves editors unreleased causing test failures.
        return !ApplicationManager.getApplication().isUnitTestMode() || !(project instanceof ProjectEx && ((ProjectEx)project).isLight());
    }
}
