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

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: May 13, 2002
 * Time: 9:58:23 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.vladsch.MissingInActions.actions;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.ex.EditorEx;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.settings.NumberingOptions;
import com.vladsch.MissingInActions.settings.RenumberingDialog;
import com.vladsch.MissingInActions.util.CaretOffsets;
import com.vladsch.MissingInActions.util.NumberSequenceGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NumberActionHandler extends EditorActionHandler {
    public NumberActionHandler() {
        super(false);
    }

    @Override
    protected boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
        return (!(editor instanceof EditorEx) || editor.getCaretModel().supportsMultipleCarets()) && editor.getCaretModel().getCaretCount() > 1;
    }

    @Override
    public void doExecute(final Editor editor, final @Nullable Caret caret, final DataContext dataContext) {
        final LineSelectionManager manager = LineSelectionManager.getInstance(editor);

        if (editor.getCaretModel().supportsMultipleCarets()) {
            boolean apply = RenumberingDialog.showDialog(editor.getComponent(), (EditorEx) editor);

            if (apply) {
                NumberingOptions options = ApplicationSettings.getInstance().getLastNumberingOptions();
                List<CaretOffsets> carets = new ArrayList<>(editor.getCaretModel().getCaretCount());

                // do the editor preview update from source editor, include carets and selections, replacing selections with numbers
                WriteCommandAction.runWriteCommandAction(editor.getProject(), () -> {
                    final Document document = editor.getDocument();
                    NumberSequenceGenerator generator = NumberSequenceGenerator.create(options);
                    int line = -1;
                    int lastVirtual = -1;

                    for (Caret caret1 : editor.getCaretModel().getAllCarets()) {
                        final int caretLine = caret1.getLogicalPosition().line;
                        generator.next(caretLine);
                        String number = generator.getNumber();

                        if (line == -1 || line != caretLine) {
                            // if prev line did not complete, add trailing chars here
                            line = caretLine;
                            lastVirtual = -1;
                        }

                        if (caret1.hasSelection()) {
                            // replace selection by number
                            final int selectionStart = caret1.getSelectionStart();
                            final int selectionEnd = caret1.getSelectionEnd();
                            document.replaceString(selectionStart, selectionEnd, number);
                            CaretOffsets offsets = new CaretOffsets(selectionStart + number.length(), selectionStart, selectionStart + number.length());
                            carets.add(offsets);
                        } else {
                            // add number at caret but may need to add virtual spaces
                            int virtualSpaces = caret1.getVisualPosition().column - (editor.getDocument().getLineEndOffset(caretLine) - editor.getDocument().getLineStartOffset(caretLine));
                            if (lastVirtual > 0) {
                                virtualSpaces -= lastVirtual;
                                lastVirtual += virtualSpaces;
                            } else {
                                lastVirtual = virtualSpaces;
                                if (virtualSpaces < 0) virtualSpaces = 0;
                            }

                            StringBuilder sb = new StringBuilder();
                            int i = virtualSpaces;
                            while (i-- > 0) sb.append(' ');
                            sb.append(number);
                            final int offset = caret1.getOffset();
                            int lengthOffset = 0;
                            if (offset == document.getTextLength()) {
                                // end of file, no EOL
                                sb.append('\n');
                                lengthOffset = -1;
                            }
                            document.insertString(offset, sb);
                            CaretOffsets offsets = new CaretOffsets(offset + sb.length() + lengthOffset, offset + virtualSpaces, offset + sb.length() + lengthOffset);
                            carets.add(offsets);
                        }
                    }
                });

                boolean first = true;
                editor.getCaretModel().removeSecondaryCarets();

                for (CaretOffsets offsets : carets) {
                    Caret caret1 = first ? editor.getCaretModel().getPrimaryCaret() : editor.getCaretModel().addCaret(editor.offsetToVisualPosition(offsets.pos));
                    first = false;

                    if (caret1 != null) {
                        // move to logical position and set selection
                        caret1.moveToOffset(offsets.pos);
                        caret1.setSelection(offsets.start, offsets.end);
                    }
                }
            }
        }
    }

    private void perform(Editor editor, LineSelectionManager manager, Caret caret) {
        assert caret != null;

        if (caret.hasSelection()) {
            EditorCaret editorCaret = manager.getEditorCaret(caret);
            if (editorCaret.getSelectionLineCount() > 1) {
                int column = editorCaret.getCaretPosition().column;
                editorCaret.setIsStartAnchorUpdateAnchorColumn(!editorCaret.isStartAnchor());
                if (column != 0 && editorCaret.getCaretPosition().column == 0) {
                    editorCaret.restoreColumn(column);
                }
                editorCaret.commit();
            } else {
                // swap start/end
                int startOffset = caret.getSelectionStart();
                int endOffset = caret.getSelectionEnd();
                if (caret.getLeadSelectionOffset() == caret.getSelectionStart()) {
                    caret.moveToOffset(startOffset);
                } else {
                    caret.moveToOffset(endOffset);
                }
                caret.setSelection(endOffset, startOffset);
            }
        }
    }
}
