/*
 * Copyright (c) 2016-2016 Vladimir Schneider <vladimir.schneider@gmail.com>
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

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: May 14, 2002
 * Time: 7:18:30 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.vladsch.MissingInActions.actions.line;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.vladsch.MissingInActions.settings.ApplicationSettings;

public class DuplicateForClipboardCaretsAction extends EditorAction {
    public DuplicateForClipboardCaretsAction() {
        super(new DuplicateForClipboardCaretsActionHandler());
    }

    public DuplicateForClipboardCaretsAction(boolean doPaste, boolean insertBlankLine) {
        super(new DuplicateForClipboardCaretsActionHandler(doPaste, insertBlankLine));
    }

    @Override
    public void update(final AnActionEvent e) {
        super.update(e);
        e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
    }

    //@Override
    //public void update(final Editor editor, final Presentation presentation, final DataContext dataContext) {
    //    super.update(editor, presentation, dataContext);
    //    if (editor.getSelectionModel().hasSelection()) {
    //        if (editor.getSelectionModel().getLeadSelectionOffset() == editor.getSelectionModel().getSelectionStart()) {
    //            presentation.setText(Bundle.message("action.duplicate-before-after.after-selection"), true);
    //        } else {
    //            presentation.setText(Bundle.message("action.duplicate-before-after.before-selection"), true);
    //        }
    //    } else {
    //        presentation.setText(EditorBundle.message("action.duplicate.line"), true);
    //    }
    //}
}
