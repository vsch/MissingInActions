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
