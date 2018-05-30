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

package com.vladsch.MissingInActions;

import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.vladsch.MissingInActions.actions.pattern.BatchReplaceForm;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import icons.PluginIcons;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.HashMap;

public class BatchSearchReplaceToolWindow {
    private ToolWindow toolWindow;

    private static final String TOOL_WINDOW_ID = Bundle.message("plugin.tool-window.id");
    private static final String BATCH_REPLACE_ID = "BATCH_REPLACE_ID";

    private Project project;
    private BatchReplaceForm myBatchSearchReplace;
    private HashMap<String, Content> myToolWindowContentMap = new HashMap<>();

    public BatchSearchReplaceToolWindow(Project project) {
        this.project = project;
        toolWindow = ToolWindowManager.getInstance(project).registerToolWindow(TOOL_WINDOW_ID, false, ToolWindowAnchor.BOTTOM);
        toolWindow.setIcon(PluginIcons.Batch_search);

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();

        myBatchSearchReplace = new BatchReplaceForm(project, ApplicationSettings.getInstance());
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(myBatchSearchReplace.getMainPanel());
        Content content = contentFactory.createContent(mainPanel, Bundle.message("batch-search.tool-window.title"), false);
        myToolWindowContentMap.put(BATCH_REPLACE_ID, content);
        toolWindow.getContentManager().addContent(content);

    }

    public void unregisterToolWindow() {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);

        // we need to dispose of all the editors
        Disposer.dispose(myBatchSearchReplace);

        toolWindowManager.unregisterToolWindow(TOOL_WINDOW_ID);
    }

    public BatchReplaceForm getBatchSearchReplace() {
        return myBatchSearchReplace;
    }

    public void setActiveEditor(EditorEx editor) {
        myBatchSearchReplace.setActiveEditor(editor);
    }

    public void activate() {
        if (toolWindow != null) {
            toolWindow.show(null);
        }
    }

    public ContentManager getContentManager() {
        return toolWindow.getContentManager();
    }
}
