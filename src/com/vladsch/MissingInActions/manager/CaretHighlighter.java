// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.manager;

import com.intellij.openapi.editor.Caret;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

public interface CaretHighlighter {
    CaretHighlighter NULL = new CaretHighlighter() {
        // @formatter:off
        @Override public void caretAdded(@NotNull Caret caret) { }
        @Override public void caretRemoved(@NotNull Caret caret) { }
        @Override public@Nullable  Caret getPrimaryCaret() { return null; }
        @Override public void highlightCaretList( final@Nullable  Collection<Caret> carets, @NotNull final CaretAttributeType attributeType, @Nullable final Set<Long> excludeList) { }
        @Override public void highlightCarets() { }
        @Override public void settingsChanged(ApplicationSettings settings) { }
        @Override public void removeCaretHighlight() { }
        @Override public void updateCaretHighlights() { }
        @Override public void setPrimaryCaret(@Nullable Caret caret) { }
        // @formatter:on
    };

    void updateCaretHighlights();
    @Nullable
    Caret getPrimaryCaret();
    void settingsChanged(ApplicationSettings settings);
    void removeCaretHighlight();
    void highlightCaretList(@Nullable Collection<Caret> carets, @NotNull CaretAttributeType attributeType, @Nullable Set<Long> excludeList);
    void highlightCarets();
    void caretAdded(@NotNull Caret caret);
    void caretRemoved(@NotNull Caret caret);
    void setPrimaryCaret(@Nullable Caret caret);
}
