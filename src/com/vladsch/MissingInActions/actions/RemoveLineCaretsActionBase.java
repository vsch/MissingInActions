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
 * Date: May 14, 2002
 * Time: 7:40:40 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.vladsch.MissingInActions.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.DumbAware;
import com.intellij.util.text.CharArrayUtil;

public class RemoveLineCaretsActionBase extends AnAction implements DumbAware {
    private final boolean myRemoveBlank;

    public RemoveLineCaretsActionBase(boolean removeBlank) {
        myRemoveBlank = removeBlank;
        setEnabledInModalContext(true);
    }

    @Override
    public boolean isDumbAware() {
        return false;
    }

    @Override
    public void update(AnActionEvent e) {
        EditorEx editor = getEditor(e);
        if (editor == null || editor.isOneLineMode()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setVisible(false);
        } else {
            e.getPresentation().setEnabled(editor.getCaretModel().getCaretCount() > 1);
            e.getPresentation().setVisible(true);
            super.update(e);
        }
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final EditorEx editor = getEditor(e);
        final CaretModel caretModel = editor.getCaretModel();
        final DocumentEx doc = editor.getDocument();

        if (caretModel.getCaretCount() > 1) {
            // switch to line mode from top most caret to bottom most caret
            //boolean removePrimary = false;
            //Caret primaryCaret = caretModel.getPrimaryCaret();
            //Caret lastSecondaryCaret = null;

            for (Caret caret : caretModel.getAllCarets()) {
                int lineNumber = doc.getLineNumber(caret.getOffset());
                int lineEndOffset = doc.getLineEndOffset(lineNumber);
                int lineStartOffset = doc.getLineStartOffset(lineNumber);

                boolean isBlank = CharArrayUtil.isEmptyOrSpaces(doc.getCharsSequence(), lineStartOffset, lineEndOffset);

                if (isBlank == myRemoveBlank) {
                    editor.getCaretModel().removeCaret(caret);
                    //if (caret == primaryCaret) {
                    //    removePrimary = true;
                    //} else {
                    //    editor.getCaretModel().removeCaret(caret);
                    //    lastSecondaryCaret = caret;
                    //}
                }
            }
            //
            //if (removePrimary && lastSecondaryCaret != null) {
            //    // we move primary to the secondary
            //    primaryCaret.moveToOffset(lastSecondaryCaret.getOffset());
            //    caretModel.removeCaret(lastSecondaryCaret);
            //}
        }
    }

    private static EditorEx getEditor(AnActionEvent e) {
        return (EditorEx) CommonDataKeys.EDITOR.getData(e.getDataContext());
    }
}
