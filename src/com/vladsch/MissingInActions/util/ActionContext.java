/*
 * Copyright (c) 2016-2018 Vladimir Schneider <vladimir.schneider@gmail.com>
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

package com.vladsch.MissingInActions.util;

import com.intellij.openapi.editor.Caret;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.flexmark.util.ValueRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

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
    public CaretSnapshot runFor(Caret caret, ValueRunnable<CaretSnapshot> runnable) {
        CaretSnapshot snapshot = myCaretSnapshots.get(caret);
        if (snapshot != null) {
            runnable.run(snapshot);
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
