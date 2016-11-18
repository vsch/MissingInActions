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
import com.vladsch.MissingInActions.util.LogPos;
import org.jetbrains.annotations.Nullable;

import static com.vladsch.MissingInActions.Plugin.getCaretInSelection;

public class ToggleCaretSelectionAnchorActionHandler extends EditorActionHandler {
    public ToggleCaretSelectionAnchorActionHandler() {
        super(true);
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

    private void perform(Editor editor, LineSelectionManager adjuster, Caret caret) {
        assert caret != null;

        if (caret.hasSelection()) {
            LineSelectionState state = adjuster.getSelectionState(caret);

            if (state.isLine()) {
                LogPos.Factory f = LogPos.factory(editor);
                LogPos pos = f.fromPos(caret.getLogicalPosition());
                LogPos start = f.fromOffset(caret.getSelectionStart());
                LogPos end = f.fromOffset(caret.getSelectionEnd());

                if (start.line + 1 < end.line) {
                    boolean startIsAnchor = true;
                    if (getCaretInSelection()) {
                        startIsAnchor = !(pos.line + 1 == end.line);
                        if (startIsAnchor) {
                            caret.moveToLogicalPosition(pos.onLine(end.line - 1));
                        } else {
                            caret.moveToLogicalPosition(pos.onLine(start.line));
                        }
                    } else {
                        startIsAnchor = pos.line != end.line;
                        if (startIsAnchor) {
                            caret.moveToLogicalPosition(pos.onLine(end.line));
                        } else {
                            caret.moveToLogicalPosition(pos.onLine(start.line - (start.line > 0 ? 1 : 0)));
                        }
                    }
                }
            } else {
                // character selection, just move to other side
                int anchorOffset = caret.getLeadSelectionOffset();
                if (anchorOffset == caret.getSelectionEnd()) {
                    caret.moveToOffset(caret.getSelectionEnd());
                } else {
                    caret.moveToOffset(caret.getSelectionStart());
                }
            }
        }
    }
}
