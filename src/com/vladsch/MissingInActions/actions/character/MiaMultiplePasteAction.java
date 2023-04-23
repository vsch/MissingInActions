// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.character;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.Editor;
import com.intellij.ui.UIBundle;
import com.vladsch.MissingInActions.Bundle;
import com.vladsch.MissingInActions.actions.MultiplePasteActionBase;
import com.vladsch.MissingInActions.actions.line.DuplicateForClipboardCaretsAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.text.DefaultEditorKit;

public class MiaMultiplePasteAction extends MultiplePasteActionBase {
    @NotNull
    @Override
    protected AnAction getPasteAction(@NotNull final Editor editor, boolean recreateCaretsAction) {
        if (recreateCaretsAction) {
            return new DuplicateForClipboardCaretsAction(true, true);
        } else {
            return ActionManager.getInstance().getAction(IdeActions.ACTION_PASTE);
        }
    }

    @Override
    protected boolean wantDuplicatedUserData() {
        return true;
    }

    @Override
    protected boolean isReplaceAware(@NotNull final Editor editor, final boolean recreateCaretsAction) {
        return recreateCaretsAction;
    }

    @Nullable
    @Override
    protected String getCreateWithCaretsName(int caretCount) {
        return caretCount > 1 ? null : Bundle.message("content-chooser.add-with-carets.label");
    }

    @Nullable
    @Override
    protected String getCreateWithCaretsTooltip(int caretCount) {
        return caretCount > 1 ? null : Bundle.message("content-chooser.add-with-carets.description");
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
