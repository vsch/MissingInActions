// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: May 13, 2002
 * Time: 9:58:23 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.vladsch.MissingInActions.actions.line;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.MissingInActions.manager.EditorCaretList;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ToggleCaretSelectionAnchorActionHandler extends EditorActionHandler {
    public ToggleCaretSelectionAnchorActionHandler() {
        super(false);
    }

    @Override
    public void doExecute(@NotNull final Editor editor, final @Nullable Caret caret, final DataContext dataContext) {
        final LineSelectionManager manager = LineSelectionManager.getInstance(editor);
        assert caret == null : "Action for specific caret not supported";
        manager.guard(() -> {
            CaretModel caretModel = editor.getCaretModel();
            if (!caretModel.supportsMultipleCarets()) {
                EditorCaret editorCaret = manager.getEditorCaret(caretModel.getPrimaryCaret());
                editorCaret.setStartAnchor(!editorCaret.isStartAnchor());
                editorCaret.commit(true);
            } else {
                // make all carets go to same anchor start/end
                EditorCaretList editorCarets = manager.getEditorCaretList();
                boolean allStartAnchor = editorCarets.all(editorCaret -> editorCaret.hasSelection() && editorCaret.isStartAnchor());
                editorCarets.forEach(editorCaret -> editorCaret.setStartAnchor(!allStartAnchor));
                editorCarets.commit(true, false, true);
            }
        });
    }
}
