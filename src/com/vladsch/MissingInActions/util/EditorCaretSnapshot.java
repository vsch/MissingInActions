// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.util;

import com.vladsch.MissingInActions.manager.EditorPosition;
import org.jetbrains.annotations.NotNull;

/**
 * Interface that provides read-only access to caret data
 */
public interface EditorCaretSnapshot {
    EditorCaretSnapshot NULL = new EditorCaretSnapshot() {
        // @formatter:off
        @Override public boolean isStartAnchor() { return false; }
        @NotNull @Override public EditorPosition getCaretPosition() { return EditorPosition.NULL; }
        @Override public boolean isLine() { return false; }
        @Override public boolean hasLines() { return false; }
        @Override public boolean hasSelection() { return false; }
        @NotNull @Override public EditorPosition getSelectionStart() { return EditorPosition.NULL; }
        @NotNull @Override public EditorPosition getSelectionEnd() { return EditorPosition.NULL; }
        @Override public int getSelectionLineCount() { return 0; }
        @NotNull @Override public EditorPosition getLineSelectionStart() { return EditorPosition.NULL; }
        @NotNull @Override public EditorPosition getLineSelectionEnd() { return EditorPosition.NULL; }
        @NotNull @Override public EditorPosition getAnchorPosition() { return EditorPosition.NULL; }
        @NotNull @Override public EditorPosition getAntiAnchorPosition() { return EditorPosition.NULL; }
        @Override public int getAnchorColumn() { return -1; }
        @Override public int getColumn() { return 0; }
        @Override public int getIndent() { return 0; }
        @NotNull @Override public EditorCaretSnapshot removeSelection() { return this; }
        // @formatter:on
    };

    // *
    // * EditorCaretSnapshot
    // *
    boolean isStartAnchor();
    @NotNull
    EditorPosition getCaretPosition();
    boolean isLine();
    boolean hasLines();
    boolean hasSelection();

    @NotNull
    EditorPosition getSelectionStart();
    @NotNull
    EditorPosition getSelectionEnd();
    int getSelectionLineCount();
    @NotNull
    EditorPosition getLineSelectionStart();
    @NotNull
    EditorPosition getLineSelectionEnd();
    @NotNull
    EditorPosition getAnchorPosition();
    @NotNull
    EditorPosition getAntiAnchorPosition();
    int getColumn();
    int getIndent();
    int getAnchorColumn();
    @NotNull
    EditorCaretSnapshot removeSelection();
}
