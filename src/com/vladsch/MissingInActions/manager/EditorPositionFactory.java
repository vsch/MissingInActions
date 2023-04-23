// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.manager;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("WeakerAccess")
public class EditorPositionFactory {
    final public static EditorPositionFactory NULL = new EditorPositionFactory(null, null);
    final private LineSelectionManager myManager;
    final private Editor myEditor;

    EditorPositionFactory(LineSelectionManager manager) {
        myManager = manager;
        myEditor = manager.getEditor();
    }

    private EditorPositionFactory(LineSelectionManager manager, Editor editor) {
        myManager = manager;
        myEditor = editor;
    }

    @NotNull
    public LineSelectionManager getManager() { return myManager; }

    @NotNull
    public Editor getEditor() { return myEditor; }

    @Nullable
    @Contract("!null->!null; null->null")
    public EditorPosition fromPosition(@Nullable LogicalPosition other) {
        return other == null ? null : other instanceof EditorPosition ? (EditorPosition) other : new EditorPosition(this, other);
    }

    @NotNull
    @SuppressWarnings("SameParameterValue")
    public EditorPosition fromPosition(int line, int column) {
        return new EditorPosition(this, line, column);
    }

    @NotNull
    public EditorPosition fromOffset(int offset) {
        return new EditorPosition(this, myEditor.offsetToLogicalPosition(offset));
    }

    @NotNull
    @SuppressWarnings("WeakerAccess")
    public EditorPosition getDocumentEndPosition() { return new EditorPosition(this, myEditor.offsetToLogicalPosition(myEditor.getDocument().getTextLength())); }

    public int getDocumentTextLength() { return myEditor.getDocument().getTextLength(); }

    @SuppressWarnings("WeakerAccess")
    public int getDocumentLineCount() {
        return myEditor.getDocument().getLineCount();
    }

    public int getOffset(LogicalPosition position) {
        return myEditor.logicalPositionToOffset(position);
    }

    @NotNull
    public BasedSequence getDocumentChars() { return BasedSequence.of(myEditor.getDocument().getCharsSequence()); }

    @NotNull
    public Document getDocument() { return myEditor.getDocument(); }
}
