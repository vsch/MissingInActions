// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.util;

import com.intellij.openapi.editor.Caret;
import com.vladsch.MissingInActions.manager.EditorCaret;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Consumer;

public class ActionContext {
    final private HashMap<Caret, CaretSnapshot> myCaretSnapshots;

    public ActionContext() {
        myCaretSnapshots = new HashMap<>();
    }

    public boolean has(@Nullable Caret caret) {
        return myCaretSnapshots.containsKey(caret);
    }

    public void add(@NotNull EditorCaret editorCaret, int index) {
        add(editorCaret, new CaretSnapshot(editorCaret, index));
    }

    public void add(@NotNull EditorCaret editorCaret, @NotNull CaretSnapshot snapshot) {
        myCaretSnapshots.put(editorCaret.getCaret(), snapshot);
    }

    @Nullable
    public CaretSnapshot get(@Nullable Caret caret) {
        return myCaretSnapshots.get(caret);
    }

    @Nullable
    public CaretSnapshot runFor(Caret caret, Consumer<CaretSnapshot> runnable) {
        CaretSnapshot snapshot = myCaretSnapshots.get(caret);
        if (snapshot != null) {
            runnable.accept(snapshot);
        }
        return snapshot;
    }

    @NotNull
    public Set<Caret> caretSet() {
        return myCaretSnapshots.keySet();
    }

    @NotNull
    public Collection<CaretSnapshot> snapshots() {
        return myCaretSnapshots.values();
    }

    public boolean isEmpty() {
        return myCaretSnapshots.isEmpty();
    }
}
