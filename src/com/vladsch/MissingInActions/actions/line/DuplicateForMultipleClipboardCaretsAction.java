// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: May 14, 2002
 * Time: 7:18:30 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.vladsch.MissingInActions.actions.line;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.Editor;
import com.vladsch.MissingInActions.Bundle;
import com.vladsch.MissingInActions.actions.MultiplePasteActionBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JComponent;

// same as duplicate but shows clipboard history popup first
public class DuplicateForMultipleClipboardCaretsAction extends MultiplePasteActionBase {
    @NotNull
    @Override
    protected AnAction getPasteAction(@NotNull final Editor editor, boolean recreateCaretsAction) {
        if (recreateCaretsAction) {
            return new DuplicateForClipboardCaretsAction(true, false);
        } else {
            return new DuplicateForClipboardCaretsAction();
        }
    }

    @Override
    protected boolean wantDuplicatedUserData() {
        return false;
    }

    @Override
    protected boolean isReplaceAware(@NotNull final Editor editor, final boolean recreateCaretsAction) {
        return true;
    }

    @Nullable
    @Override
    protected String getCreateWithCaretsName(int caretCount) {
        return Bundle.message("content-chooser.duplicate-and-paste.label");
    }

    @Nullable
    @Override
    protected String getCreateWithCaretsTooltip(int caretCount) {
        return Bundle.message("content-chooser.duplicate-and-paste.description");
    }

    @Nullable
    @Override
    protected Action getPasteAction(@NotNull final JComponent focusedComponent) {
        return null;
    }

    @NotNull
    @Override
    protected String getContentChooserTitle(@Nullable final Editor editor, @NotNull final JComponent focusedComponent) {
        return editor != null ? Bundle.message("actions.dupe-for-carets-from-history.editor.title") : "Disabled";
    }

    @Override
    protected boolean isEnabled(@Nullable final Editor editor, @NotNull final JComponent focusedComponent) {
        return editor != null /*&& editor.getCaretModel().getCaretCount() == 1*/;
    }
}
