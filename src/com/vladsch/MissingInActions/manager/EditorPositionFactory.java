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

package com.vladsch.MissingInActions.manager;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.SubSequence;
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
    public BasedSequence getDocumentChars() { return SubSequence.of(myEditor.getDocument().getCharsSequence()); }

    @NotNull
    public Document getDocument() { return myEditor.getDocument(); }
}
