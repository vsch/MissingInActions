// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: May 14, 2002
 * Time: 7:18:30 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.vladsch.MissingInActions.actions.character;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.editor.actions.EditorActionUtil;
import com.intellij.openapi.ide.CopyPasteManager;
import com.vladsch.MissingInActions.util.EditHelpers;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;
import java.util.HashMap;

import static com.vladsch.MissingInActions.util.EditHelpers.END_OF_FOLDING_REGION;
import static com.vladsch.MissingInActions.util.EditHelpers.END_OF_LEADING_BLANKS;
import static com.vladsch.MissingInActions.util.EditHelpers.END_OF_LINE;
import static com.vladsch.MissingInActions.util.EditHelpers.END_OF_WORD;
import static com.vladsch.MissingInActions.util.EditHelpers.MIA_IDENTIFIER;
import static com.vladsch.MissingInActions.util.EditHelpers.START_OF_FOLDING_REGION;
import static com.vladsch.MissingInActions.util.EditHelpers.START_OF_LINE;
import static com.vladsch.MissingInActions.util.EditHelpers.START_OF_TRAILING_BLANKS;
import static com.vladsch.MissingInActions.util.EditHelpers.START_OF_WORD;

public class BackspaceToWordStartNotEolActionHandler extends EditorWriteActionHandler {
    /**
     * We need to provide special processing for quote symbols.
     * <p/>
     * Examples:
     * <table border='1'>
     * <tr>
     * <th>Text before action call</td>
     * <th>Text after action call</td>
     * </tr>
     * <tr>
     * <td>one "two" [caret]</td>
     * <td>one [caret]</td>
     * </tr>
     * <tr>
     * <td>one "two[caret]"</td>
     * <td>one "[caret]"</td>
     * </tr>
     * </table>
     */
    private static final BitSet QUOTE_SYMBOLS = new BitSet();
    static {
        QUOTE_SYMBOLS.set('\'');
        QUOTE_SYMBOLS.set('\"');
    }

    private static final int[] QUOTE_SYMBOLS_ARRAY = QUOTE_SYMBOLS.stream().toArray();

    @NotNull final private HashMap<Integer, Integer> myQuotesNumber = new HashMap<>();
    final private boolean myNegateCamelMode;

    public BackspaceToWordStartNotEolActionHandler(boolean negateCamelMode) {
        super(true);
        myNegateCamelMode = negateCamelMode;
    }

    @Override
    public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
        CommandProcessor.getInstance().setCurrentCommandGroupId(EditorActionUtil.DELETE_COMMAND_GROUP);
        CopyPasteManager.getInstance().stopKillRings();
        if (editor.getSelectionModel().hasSelection()) {
            EditorModificationUtil.deleteSelectedText(editor);
            return;
        }
        backspaceToWordStart(editor);
    }

    private void backspaceToWordStart(Editor editor) {
        boolean camel = editor.getSettings().isCamelWords();
        if (myNegateCamelMode) {
            camel = !camel;
        }
        CharSequence text = editor.getDocument().getCharsSequence();
        CaretModel caretModel = editor.getCaretModel();
        int endOffset = caretModel.getOffset();
        int minOffset = editor.getDocument().getLineStartOffset(caretModel.getLogicalPosition().line);
        //noinspection ConstantConditionalExpression
        int boundaryFlags = (true ? START_OF_LINE : 0)
                | (true ? END_OF_LINE : 0)
                | (true ? START_OF_TRAILING_BLANKS | END_OF_LEADING_BLANKS : 0)
                | (true ? MIA_IDENTIFIER : 0)
                | (true ? START_OF_WORD : 0)
                | (false ? END_OF_WORD : 0)
                | (false ? START_OF_FOLDING_REGION : 0)
                | (false ? END_OF_FOLDING_REGION : 0);

        myQuotesNumber.clear();
        for (int i : QUOTE_SYMBOLS_ARRAY) {
            myQuotesNumber.put(i, 0);
        }
        countQuotes(myQuotesNumber, text, minOffset, endOffset);

        EditHelpers.moveCaretToPreviousWordStartOrEnd(editor, false, camel, boundaryFlags);

        for (int offset = caretModel.getOffset(); offset > minOffset; offset = caretModel.getOffset()) {
            int previous = text.charAt(offset - 1);
            int current = text.charAt(offset);
            if (QUOTE_SYMBOLS.get(current)) {
                if (Character.isWhitespace(previous)) {
                    break;
                } else if (offset < endOffset - 1 && !Character.isJavaIdentifierPart(text.charAt(offset + 1))) {
                    // Handle a situation like ' "one", "two", [caret] '. We want to delete up to the previous literal end here.
                    editor.getCaretModel().moveToOffset(offset + 1);
                    break;
                }
                if (myQuotesNumber.get(current) % 2 == 0) {
                    // Was 'one "two" [caret]', now 'one "two[caret]"', we want to get 'one [caret]"two"'
                    EditHelpers.moveCaretToPreviousWordStartOrEnd(editor, false, camel, boundaryFlags);
                    continue;
                }
                break;
            }

            if (QUOTE_SYMBOLS.get(previous)) {
                if (myQuotesNumber.get(previous) % 2 == 0) {
                    // Was 'one "two[caret]", now 'one "[caret]two"', we want 'one [caret]"two"'
                    editor.getCaretModel().moveToOffset(offset - 1);
                }
            }
            break;
        }

        int startOffset = caretModel.getOffset();
        Document document = editor.getDocument();
        document.deleteString(startOffset, endOffset);
    }

    private static void countQuotes(@NotNull HashMap<Integer, Integer> holder, @NotNull CharSequence text, int start, int end) {
        for (int i = end - 1; i >= start; i--) {
            int c = text.charAt(i);
            if (holder.containsKey(c)) {
                holder.put(c, holder.get(c) + 1);
            }
        }
    }
}
