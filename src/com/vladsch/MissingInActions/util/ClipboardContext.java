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

package com.vladsch.MissingInActions.util;

import com.intellij.codeInsight.editorActions.TextBlockTransferable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actions.PasteAction;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.LineTokenizer;
import com.intellij.util.Producer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.Transferable;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

import static com.intellij.openapi.diagnostic.Logger.getInstance;

@SuppressWarnings({ "WeakerAccess", "UnusedReturnValue" })
public class ClipboardContext {
    public static Key<ClipboardContext> LAST_PASTED_CLIPBOARD_CONTEXT = Key.create("LAST_PASTED_CLIPBOARD_CONTEXT");
    private static final Logger logger = getInstance("com.vladsch.MissingInActions.util.clipboard_context");

    private final @NotNull TextRange[] myTextRanges;
    private final @Nullable String[] myTexts;
    private final @Nullable BitSet myFullLines;
    private final int myCaretCount;
    private final int myLineCount;

    private ClipboardContext(@NotNull final TextRange[] textRanges, @Nullable final String[] texts, final int caretCount, final int lineCount, @Nullable BitSet fullLines) {
        myTextRanges = textRanges;
        myTexts = texts;
        myCaretCount = caretCount;
        myLineCount = lineCount;
        myFullLines = fullLines;
    }

    @NotNull
    public TextRange[] getTextRanges() { return myTextRanges; }

    @Nullable
    public String[] getTexts() { return myTexts; }

    public int getCaretCount() { return myCaretCount; }

    public int getLineCount() { return myLineCount; }

    public boolean isFullLine(int caretIndex) { return myFullLines != null && myFullLines.get(caretIndex); }

    public void shiftCaretRangeRight(int caretIndex, int delta) {
        if (caretIndex < myTextRanges.length) {
            TextRange shifted = myTextRanges[caretIndex].shiftRight(delta);
            myTextRanges[caretIndex] = shifted;
        }
    }

    @Nullable
    public static Transferable getTransferable(final @NotNull Editor editor, DataContext dataContext) {
        Producer<Transferable> producer = PasteAction.TRANSFERABLE_PROVIDER.getData(dataContext);
        //noinspection UnnecessaryLocalVariable
        Transferable transferable = EditorModificationUtil.getContentsToPasteToEditor(producer);
        return transferable;
    }

    /**
     * Get the data about clipboard transferable as if there were just enough carets for each caret in the data
     * @param editor editor for paste
     * @param content transferable content to study
     */
    @Nullable
    public static ClipboardContext studyTransferable(final @NotNull Editor editor, @NotNull Transferable content) {
        String text = EditorModificationUtil.getStringContent(content);
        if (text == null) return null;

        if (editor.getCaretModel().supportsMultipleCarets()) {
            CaretStateTransferableData caretData = null;
            try {
                caretData = content.isDataFlavorSupported(CaretStateTransferableData.FLAVOR)
                        ? (CaretStateTransferableData) content.getTransferData(CaretStateTransferableData.FLAVOR) : null;
            } catch (Exception e) {
                logger.error(e);
            }

            int lineCount = LineTokenizer.calcLineCount(text, true);
            int caretCount = caretData == null ? 1 : caretData.startOffsets.length;

            final TextRange[] ranges = new TextRange[caretCount];
            String[] texts = new String[caretCount];
            final Iterator<String> segments = new ClipboardTextPerCaretSplitter().split(text, caretData, caretCount).iterator();
            BitSet fullLines = new BitSet(caretCount);

            for (int i = 0; i < caretCount; i++) {
                String normalizedText = TextBlockTransferable.convertLineSeparators(editor, segments.next());
                texts[i] = normalizedText;
                ranges[i] = new TextRange(0, normalizedText.length());
                if (normalizedText.endsWith("\n")) fullLines.set(i);
            }
            return new ClipboardContext(ranges, texts, caretCount, lineCount, fullLines);
        } else {
            String normalizedText = TextBlockTransferable.convertLineSeparators(editor, text);
            String[] texts = new String[1];
            texts[1] = normalizedText;
            BitSet fullLines = new BitSet(1);
            if (normalizedText.endsWith("\n")) fullLines.set(0);
            return new ClipboardContext(new TextRange[] { new TextRange(0, normalizedText.length()) }, texts, 1,1, fullLines);
        }
    }

    @Nullable
    public static ClipboardContext studyPrePasteTransferable(final @NotNull Editor editor, @NotNull Transferable content) {
        String text = EditorModificationUtil.getStringContent(content);
        if (text == null) return null;

        if (editor.getCaretModel().supportsMultipleCarets()) {
            CaretStateTransferableData caretData = null;
            int caretCount = editor.getCaretModel().getCaretCount();
            if (caretCount == 1 && editor.isColumnMode()) {
                caretCount = LineTokenizer.calcLineCount(text, true);
            } else {
                try {
                    caretData = content.isDataFlavorSupported(CaretStateTransferableData.FLAVOR)
                            ? (CaretStateTransferableData) content.getTransferData(CaretStateTransferableData.FLAVOR) : null;
                } catch (Exception e) {
                    logger.error(e);
                }
            }

            final TextRange[] ranges = new TextRange[caretCount];
            final Iterator<String> segments = new ClipboardTextPerCaretSplitter().split(text, caretData, caretCount).iterator();
            final List<Caret> carets = editor.getCaretModel().getAllCarets();
            BitSet fullLines = new BitSet(caretCount);
            int cumulativeOffset = 0;
            for (int i = 0; i < caretCount; i++) {
                String normalizedText = TextBlockTransferable.convertLineSeparators(editor, segments.next());
                Caret caret = carets.get(i);
                int caretOffset = caret.getOffset();
                final int selectionStart = caret.getSelectionStart();
                final int selectionEnd = caret.getSelectionEnd();
                int selectionDelta = selectionEnd - selectionStart;

                if (caretOffset > selectionStart && selectionStart != selectionEnd) {
                    if (caretOffset <= selectionEnd) {
                        // the data at location will be deleted affecting the range, bug in IDE code does not address this
                        caretOffset = selectionStart;
                    } else {
                        caretOffset -= selectionEnd - selectionStart;
                    }
                }

                // since no insertions are done yet, we have to adjust for what will be inserted and deleted
                ranges[i] = new TextRange(caretOffset+cumulativeOffset, caretOffset+cumulativeOffset + normalizedText.length());
                if (normalizedText.endsWith("\n")) fullLines.set(i);
                cumulativeOffset += normalizedText.length();
                cumulativeOffset -= selectionDelta;
            }
            return new ClipboardContext(ranges, null, caretCount, caretCount, fullLines);
        } else {
            int caretOffset = editor.getCaretModel().getOffset();
            String normalizedText = TextBlockTransferable.convertLineSeparators(editor, text);
            BitSet fullLines = new BitSet(1);
            if (normalizedText.endsWith("\n")) fullLines.set(0);
            return new ClipboardContext(new TextRange[] { new TextRange(caretOffset, caretOffset + normalizedText.length()) }, null, 1,1, fullLines);
        }
    }
}
