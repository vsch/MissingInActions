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

package com.vladsch.MissingInActions.actions.character;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCopyPasteHelper;
import com.intellij.openapi.editor.actions.BasePasteHandler;
import com.intellij.openapi.editor.actions.TextComponentEditorAction;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;

import java.util.List;

public class MiaPasteAction extends TextComponentEditorAction {
    public static Key<TextRange[]> LAST_PASTED_REGIONS = Key.create("LAST_PASTED_REGIONS");

    public MiaPasteAction() {
        super(new Handler());
    }

    private static class Handler extends BasePasteHandler {
        @Override
        public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
            TextRange range = null;
            TextRange[] ranges = null;
            if (myTransferable != null) {
                // need to get selections at all carets so we can adjust the range, it is off by deleted characters,
                // guessing it inserts after the selection and deletes the selection
                List<Caret> carets = editor.getCaretModel().getAllCarets();
                int iMax = carets.size();
                int[] adjustments = new int[iMax];
                for (int i = 0; i < iMax; i++) {
                    caret = carets.get(i);
                    adjustments[i] = caret.getSelectionEnd() - caret.getSelectionStart();
                }

                ranges = EditorCopyPasteHelper.getInstance().pasteTransferable(editor, myTransferable);

                if (ranges != null) {
                    for (int i = 0; i < iMax; i++) {
                        if (i >= ranges.length) break;
                        ranges[i] = ranges[i].shiftRight(-adjustments[i]);
                    }
                }

                if (ranges != null && ranges.length == 1) {
                    range = ranges[0];
                }
            }
            editor.putUserData(LAST_PASTED_REGIONS, ranges);
            editor.putUserData(EditorEx.LAST_PASTED_REGION, range);
        }
    }
}
