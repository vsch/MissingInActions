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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
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
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.HashMap;

public class BatchSearchReplaceToolWindow implements Disposable {
    private final ToolWindow toolWindow;

    private static final String TOOL_WINDOW_ID = Bundle.message("plugin.tool-window.id");
    private static final String BATCH_REPLACE_ID = "BATCH_REPLACE_ID";

    private final Project project;
    private final BatchReplaceForm myBatchSearchReplace;
    private final HashMap<String, Content> myToolWindowContentMap = new HashMap<>();

    public BatchSearchReplaceToolWindow(Project project) {
        this.project = project;
        toolWindow = ToolWindowManager.getInstance(project).registerToolWindow(TOOL_WINDOW_ID, false, ToolWindowAnchor.BOTTOM, project, true);
        toolWindow.setIcon(PluginIcons.Batch_search_Tool);

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();

        myBatchSearchReplace = new BatchReplaceForm(project, ApplicationSettings.getInstance());
        Disposer.register(this, myBatchSearchReplace);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        mainPanel.add(myBatchSearchReplace.getMainPanel());
        Content content = contentFactory.createContent(mainPanel, Bundle.message("batch-search.tool-window.title"), false);
        myToolWindowContentMap.put(BATCH_REPLACE_ID, content);
        toolWindow.getContentManager().addContent(content);
    }

    @Override
    public void dispose() {

    }

    public void unregisterToolWindow() {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        toolWindowManager.unregisterToolWindow(TOOL_WINDOW_ID);
    }

    public BatchReplaceForm getBatchSearchReplace() {
        return myBatchSearchReplace;
    }

    public void setActiveEditor(Editor editor) {
        myBatchSearchReplace.setActiveEditor(editor);
    }

    public boolean shouldNotUpdateHighlighters(@Nullable Editor editor) {
        return myBatchSearchReplace.shouldNotUpdateHighlighters(editor);
    }

    public void setUpdateHighlighters() {
        myBatchSearchReplace.updateHighlighters();
    }

    public void activate() {
        if (toolWindow != null) {
            toolWindow.show(null);
        }
    }

    public void hide() {
        if (toolWindow != null) {
            toolWindow.hide(null);
        }
    }

    public boolean isShowing() {
        if (toolWindow != null) {
            return toolWindow.isVisible();
        }
        return false;
    }

    public ContentManager getContentManager() {
        return toolWindow.getContentManager();
    }
}
