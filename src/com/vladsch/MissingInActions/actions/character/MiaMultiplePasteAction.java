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
 *
 */
package com.vladsch.MissingInActions.actions.character;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.Editor;
import com.intellij.ui.UIBundle;
import com.vladsch.MissingInActions.Bundle;
import com.vladsch.MissingInActions.actions.MultiplePasteActionBase;
import com.vladsch.MissingInActions.actions.line.DuplicateForClipboardCaretsAction;
import com.vladsch.MissingInActions.util.ClipboardCaretContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;

public class MiaMultiplePasteAction extends MultiplePasteActionBase {
    @NotNull
    @Override
    protected AnAction getPasteAction(@NotNull final Editor editor, boolean recreateCaretsAction) {
        if (recreateCaretsAction) {
            return new DuplicateForClipboardCaretsAction(true,true);
        } else {
            return ActionManager.getInstance().getAction(IdeActions.ACTION_PASTE);
        }
    }

    @Nullable
    @Override
    protected String getCreateWithCaretsName(int caretCount) {
        return caretCount > 1 ? null : Bundle.message("content-chooser.add-with-carets.label");
    }

    @Nullable
    @Override
    protected Action getPasteAction(@NotNull final JComponent focusedComponent) {
        return focusedComponent.getActionMap().get(DefaultEditorKit.pasteAction);
    }

    @NotNull
    @Override
    protected String getContentChooserTitle(@Nullable final Editor editor, @NotNull final JComponent focusedComponent) {
        return UIBundle.message("choose.content.to.paste.dialog.title");
    }

    @Override
    protected boolean isEnabled(@Nullable final Editor editor, @NotNull final JComponent focusedComponent) {
        return true;
    }
}
