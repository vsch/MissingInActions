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

package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.vladsch.MissingInActions.Bundle;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

public class BatchReplaceDialog extends DialogWrapper {
    JPanel myMainPanel;
    private BatchReplaceForm myBatchReplaceForm;

    public BatchReplaceDialog(JComponent parent, @NotNull Project project, @NotNull ApplicationSettings applicationSettings, @NotNull EditorEx editor) {
        super(parent, false);

        setTitle(Bundle.message("caret-search.options-dialog.title"));

        myBatchReplaceForm = new BatchReplaceForm(project, applicationSettings);
        myMainPanel.add(myBatchReplaceForm.myMainPanel, BorderLayout.CENTER);
        myBatchReplaceForm.setActiveEditor(editor);

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return myMainPanel;
    }

    protected class MyCancelAction extends DialogWrapperAction {
        MyCancelAction() {
            super(Bundle.message("batch-search.close-button.label"));
        }

        @Override
        protected void doAction(ActionEvent e) {
            doCancelAction();
        }
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        super.createDefaultActions();
        return new Action[] { /*getOKAction(),*/ new MyCancelAction() };
    }

    public static boolean showDialog(JComponent parent, @NotNull Project project, @NotNull ApplicationSettings applicationSettings, @NotNull EditorEx editor) {
        BatchReplaceDialog dialog = new BatchReplaceDialog(parent, project, applicationSettings, editor);
        boolean save = dialog.showAndGet();
        dialog.myBatchReplaceForm.saveSettings();
        dialog.myBatchReplaceForm.disposeEditors();
        return save;
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        //String error = checkRegEx(myPattern);
        //
        //if (!error.isEmpty()) {
        //    return new ValidationInfo(error, myPattern);
        //}
        return super.doValidate();
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return "MissingInActions.BatchReplaceDialog";
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return myBatchReplaceForm.mySearchEditor != null ? myBatchReplaceForm.mySearchEditor.getContentComponent() : myMainPanel;
    }
}
