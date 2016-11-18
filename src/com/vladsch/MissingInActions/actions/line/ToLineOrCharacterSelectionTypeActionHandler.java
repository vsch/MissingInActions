/*
 * Copyright (c) 2016-2016 Vladimir Schneider <vladimir.schneider@gmail.com>
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
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.manager.LineSelectionState;
import org.jetbrains.annotations.Nullable;

public class ToLineOrCharacterSelectionTypeActionHandler extends EditorActionHandler {
    final private @Nullable Boolean myMakeLine;
    final private @Nullable Boolean myTrimmedLine;

    public ToLineOrCharacterSelectionTypeActionHandler(@Nullable Boolean makeLine, boolean trimmedLine) {
        super(true);
        myMakeLine = makeLine;
        myTrimmedLine = trimmedLine;
    }

    @Override
    public void doExecute(final Editor editor, final @Nullable Caret caret, final DataContext dataContext) {
        final LineSelectionManager adjuster = LineSelectionManager.getInstance(editor);
        adjuster.guard(() -> {
            if (!editor.getCaretModel().supportsMultipleCarets()) {
                perform(editor, adjuster, caret);
            } else {
                if (caret == null) {
                    editor.getCaretModel().runForEachCaret(caret1 -> perform(editor, adjuster, caret1));
                } else {
                    perform(editor, adjuster, caret);
                }
            }
        });
    }

    private boolean wantLine() {
        return myMakeLine == null || myMakeLine;
    }

    private boolean wantCharacter() {
        return myMakeLine == null || !myMakeLine;
    }

    private void perform(Editor editor, LineSelectionManager adjuster, Caret caret) {
        assert caret != null;

        if (caret.hasSelection()) {
            LineSelectionState state = adjuster.getSelectionState(caret);

            if (state.isLine()) {
                if (wantCharacter()) {
                    adjuster.adjustLineSelectionToCharacterSelection(caret, false);
                }
            } else {
                // make it line selection
                if (wantLine()) {
                    adjuster.adjustCharacterSelectionToLineSelection(caret, true, myTrimmedLine);
                }
            }
        }
    }
}
