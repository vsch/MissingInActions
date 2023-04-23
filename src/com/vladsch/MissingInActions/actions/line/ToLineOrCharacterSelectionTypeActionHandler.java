// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.line;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.vladsch.MissingInActions.actions.ActionUtils;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ToLineOrCharacterSelectionTypeActionHandler extends EditorActionHandler {
    final private @Nullable Boolean myMakeLine;

    @SuppressWarnings("WeakerAccess")
    public ToLineOrCharacterSelectionTypeActionHandler(@Nullable Boolean makeLine) {
        super(true);
        myMakeLine = makeLine;
    }

    @Override
    public void doExecute(@NotNull final Editor editor, final @Nullable Caret caret, final DataContext dataContext) {
        final LineSelectionManager manager = LineSelectionManager.getInstance(editor);
        manager.guard(() -> {
            if (!editor.getCaretModel().supportsMultipleCarets() || caret != null) {
                ActionUtils.toggleLineCharacterSelection(manager, caret, wantLine(), wantCharacter(), true);
            } else {
                editor.getCaretModel().runForEachCaret(caret1 -> ActionUtils.toggleLineCharacterSelection(manager, caret1, wantLine(), wantCharacter(), true));
            }
        });
    }

    private boolean wantLine() {
        return myMakeLine == null || myMakeLine;
    }

    private boolean wantCharacter() {
        return myMakeLine == null || !myMakeLine;
    }
}
