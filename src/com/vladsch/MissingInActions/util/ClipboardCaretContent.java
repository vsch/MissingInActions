// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.util;

import com.intellij.codeInsight.editorActions.TextBlockTransferable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretStateTransferableData;
import com.intellij.openapi.editor.ClipboardTextPerCaretSplitter;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.actions.PasteAction;
import com.intellij.openapi.editor.ex.EditorEx;
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
import java.util.function.BiConsumer;

import static com.intellij.openapi.diagnostic.Logger.getInstance;

@SuppressWarnings({ "WeakerAccess", "UnusedReturnValue", "SameParameterValue" })
public class ClipboardCaretContent {
    private static final Key<ClipboardCaretContent> LAST_PASTED_CLIPBOARD_CARETS = Key.create("LAST_PASTED_CLIPBOARD_CONTEXT");
    private static final Key<CaretOffsetAdjuster> LAST_CARET_OFFSET_ADJUSTER = Key.create("LAST_CARET_OFFSET_ADJUSTER");
    private static final Key<Transferable> LAST_SPLIT_PERMUTED_TRANSFERABLE = Key.create("LAST_CARET_OFFSET_ADJUSTER");
    private static final Logger logger = getInstance("com.vladsch.MissingInActions.util.clipboard_context");

    private final @NotNull Transferable myContent;
    private final @NotNull TextRange[] myTextRanges;
    private final @Nullable int[] myCaretColumns;          // caret logical column position before it was adjusted by CaretOffsetAdjuster
    private final boolean myHadSelection;
    private final @Nullable String[] myTexts;
    private final @NotNull BitSet myFullLines;            // carets or caret terminated by EOL
    private final @NotNull BitSet myCharLines;            // carets or caret not terminated by EOL but more than one line
    private final int myCaretCount;
    private final int myLineCount;
    private final @Nullable RangeMarker myRangeMarker;     // only set if editor supports multiple carets && caret count == 1 && !rangeAtStart && !rangeAtEnd, used to pasted text range after PasteHandler formats the data
    private final boolean myRangeAtTextStart;              // range starts at start of text, so ignore range marker's start
    private final boolean myRangeAtTextEnd;                // range ends at end of text so ignore range marker's end

    private ClipboardCaretContent(
            @NotNull Transferable content, @NotNull final TextRange[] textRanges
            , @Nullable final int[] caretColumns
            , final boolean hadSelection
            , @Nullable final String[] texts
            , final int caretCount
            , final int lineCount
            , final @NotNull BitSet fullLines
            , final @NotNull BitSet charLines
            , final @Nullable RangeMarker rangeMarker
            , final boolean rangeAtTextStart
            , final boolean rangeAtTextEnd
    ) {
        myContent = content;
        myTextRanges = textRanges;
        myCaretColumns = caretColumns;
        myHadSelection = hadSelection;
        myTexts = texts;
        myCaretCount = caretCount;
        myLineCount = lineCount;
        myFullLines = fullLines;
        myCharLines = charLines;
        myRangeMarker = rangeMarker;
        myRangeAtTextStart = rangeAtTextStart;
        myRangeAtTextEnd = rangeAtTextEnd;
    }

    public void dispose() {
        if (myRangeMarker != null && myRangeMarker.isValid()) {
            myRangeMarker.dispose();
        }
    }

    public interface CaretOffsetAdjuster {
        int getOffset(final @NotNull Caret caret, final boolean isFullLine);
    }

    @NotNull
    public TextRange[] getTextRanges() { return myTextRanges; }

    @NotNull
    public Transferable getContent() {
        return myContent;
    }

    @Nullable
    public static TextRange getLastPastedTextRange(final @NotNull Editor editor, int caretIndex) {
        ClipboardCaretContent clipboardCaretContent = getLastPastedClipboardCarets(editor);
        TextRange range = null;
        if (editor.getCaretModel().getCaretCount() == 1 && caretIndex == 0) {
            // special handling for single caret mode
            if (clipboardCaretContent != null) {
                range = clipboardCaretContent.getMarkerRange(editor);
                if (range == null && caretIndex < clipboardCaretContent.myTextRanges.length) {
                    range = clipboardCaretContent.myTextRanges[caretIndex];
                }
            } else {
                range = editor.getUserData(EditorEx.LAST_PASTED_REGION);
            }
        } else if (clipboardCaretContent != null) {
            if (caretIndex < clipboardCaretContent.myTextRanges.length) {
                range = clipboardCaretContent.myTextRanges[caretIndex];
            }
        }

        if (range != null) {
            final int length = editor.getDocument().getTextLength();
            if (range.getStartOffset() >= length) range = TextRange.from(length, length);
            else if (range.getEndOffset() > length) range = TextRange.from(range.getStartOffset(), length);
        }
        return range;
    }

    public static int getLastPastedCaretColumn(final @NotNull Editor editor, int caretIndex) {
        ClipboardCaretContent clipboardCaretContent = getLastPastedClipboardCarets(editor);
        if (clipboardCaretContent != null) {
            return clipboardCaretContent.getCaretColumn(caretIndex);
        }
        return -1;
    }

    @Nullable
    public String[] getTexts() { return myTexts == null ? null : myTexts.clone(); }

    public boolean hasFullLines() {
        return myFullLines.nextSetBit(0) != -1;
    }

    public boolean allFullLines() {
        return myFullLines.nextClearBit(0) >= myCaretCount && myCharLines.nextClearBit(0) >= myCaretCount;
    }

    public boolean hasCharLines() {
        return myCharLines.nextSetBit(0) != -1;
    }

    public boolean allCharLines() {
        return myFullLines.nextSetBit(0) == -1 && myCharLines.nextClearBit(0) >= myCaretCount;
    }

    public boolean hasChars() {
        BitSet bitSet = new BitSet(myCaretCount);
        bitSet.or(myFullLines);
        bitSet.or(myCharLines);

        return bitSet.nextClearBit(0) < myCaretCount;
    }

    public boolean allChars() {
        BitSet bitSet = new BitSet(myCaretCount);
        bitSet.or(myFullLines);
        bitSet.or(myCharLines);

        return bitSet.nextSetBit(0) == -1;
    }

    public int getCaretCount() { return myCaretCount; }

    public int getLineCount() { return myLineCount; }

    public boolean isFullLine(int caretIndex) { return myFullLines.get(caretIndex); }

    public boolean isCharLine(int caretIndex) { return myCharLines.get(caretIndex); }

    public int getCaretColumn(int caretIndex) { return myCaretColumns == null || caretIndex >= myCaretColumns.length ? -1 : myCaretColumns[caretIndex]; }

    public void shiftCaretRangeRight(int caretIndex, int delta) {
        if (caretIndex < myTextRanges.length) {
            TextRange shifted = myTextRanges[caretIndex].shiftRight(delta);
            myTextRanges[caretIndex] = shifted;
        }
    }

    @Nullable
    public TextRange getMarkerRange(final @NotNull Editor editor) {
        final int textLength = editor.getDocument().getTextLength();
        if (myRangeAtTextStart && myRangeAtTextEnd) {
            return TextRange.create(0, textLength);
        } else {
            if (myRangeMarker != null && myRangeMarker.isValid()) {
                final int offset = myHadSelection ? 0 : 1;
                final int startOffset = myRangeAtTextStart ? 0 : Math.max(Math.min(myRangeMarker.getStartOffset() + offset, textLength), 0);
                final int endOffset = myRangeAtTextEnd ? textLength : Math.min(Math.max(myRangeMarker.getEndOffset() - offset, startOffset), textLength);
                return TextRange.create(startOffset, endOffset);
            }
        }
        return null;
    }

    @Nullable
    public static ClipboardCaretContent getLastPastedClipboardCarets(final @NotNull Editor editor) {
        return editor.getUserData(LAST_PASTED_CLIPBOARD_CARETS);
    }

    @Nullable
    public static CaretOffsetAdjuster getLastPastedCaretOffsetAdjuster(final @NotNull Editor editor) {
        return editor.getUserData(LAST_CARET_OFFSET_ADJUSTER);
    }

    public static void setLastPastedClipboardCarets(final @NotNull Editor editor, final @Nullable ClipboardCaretContent clipboardCarets) {
        final ClipboardCaretContent context = editor.getUserData(LAST_PASTED_CLIPBOARD_CARETS);
        if (context != null) {
            context.dispose();
        }
        if (clipboardCarets == null) {
            // clear the last used offset adjuster
            editor.putUserData(LAST_CARET_OFFSET_ADJUSTER, null);
        }
        editor.putUserData(LAST_PASTED_CLIPBOARD_CARETS, clipboardCarets);
    }

    @Nullable
    public static Transferable getTransferable(final @NotNull Editor editor, DataContext dataContext) {
        try {
            //noinspection deprecation, Part of TRANSFERABLE_PROVIDER
            Producer<Transferable> producer = PasteAction.TRANSFERABLE_PROVIDER.getData(dataContext);
            //noinspection UnnecessaryLocalVariable
            Transferable transferable = EditorModificationUtil.getContentsToPasteToEditor(producer);
            return transferable;
        } catch (Throwable e) {
            logger.error("error", e);
        }
        return null;
    }

    /**
     * Get the data about clipboard transferable as if there were just enough carets for each caret in the data
     *
     * @param editor      editor for paste
     * @param dataContext data context for action
     */
    @Nullable
    public static ClipboardCaretContent studyClipboard(final @NotNull Editor editor, DataContext dataContext) {
        try {
            Transferable transferable = getTransferable(editor, dataContext);
            return transferable == null ? null : studyTransferable(editor, transferable);
        } catch (Throwable e) {
            logger.error("error", e);
        }
        return null;
    }

    /**
     * Get the data about clipboard transferable as if there were just enough carets for each caret in the data
     *
     * @param editor                editor for paste
     * @param dataContext           data context for action
     * @param useLastOffsetAdjuster whether to use the last used offset adjuster for caret offset calculations
     */
    @Nullable
    public static ClipboardCaretContent studyPrePasteClipboard(final @NotNull Editor editor, DataContext dataContext, boolean useLastOffsetAdjuster) {
        try {
            Transferable transferable = getTransferable(editor, dataContext);
            return transferable == null ? null : saveLastPastedCaretsForTransferable(editor, transferable, useLastOffsetAdjuster);
        } catch (Throwable e) {
            logger.error("error", e);
        }
        return null;
    }

    /**
     * Get the data about clipboard transferable as if there were just enough carets for each caret in the data
     *
     * @param editor  editor for paste
     * @param content transferable content to study
     */
    @Nullable
    public static ClipboardCaretContent studyTransferable(final @Nullable Editor editor, @NotNull Transferable content) {
        String text = EditorModificationUtil.getStringContent(content);
        if (text == null) return null;

        if (editor != null && editor.getCaretModel().supportsMultipleCarets()) {
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
            BitSet charLines = new BitSet(caretCount);
            int contentOffset = 0;

            for (int i = 0; i < caretCount; i++) {
                String normalizedText = TextBlockTransferable.convertLineSeparators(editor, segments.next());
                texts[i] = normalizedText;
                ranges[i] = new TextRange(contentOffset, contentOffset + normalizedText.length());
                if (normalizedText.endsWith("\n")) fullLines.set(i);
                else if (normalizedText.contains("\n")) charLines.set(i);
                contentOffset += normalizedText.length();
            }
            return new ClipboardCaretContent(content, ranges, null, false, texts, caretCount, lineCount, fullLines, charLines, null, false, false);
        } else {
            String normalizedText = editor == null ? text : TextBlockTransferable.convertLineSeparators(editor, text);
            String[] texts = new String[1];
            texts[0] = normalizedText;
            BitSet fullLines = new BitSet(1);
            BitSet charLines = new BitSet(1);
            if (normalizedText.endsWith("\n")) fullLines.set(0);
            else if (normalizedText.contains("\n")) charLines.set(0);
            return new ClipboardCaretContent(content, new TextRange[] { new TextRange(0, normalizedText.length()) }, null, false, texts, 1, 1, fullLines, charLines, null, false, false);
        }
    }

    @Nullable
    public static ClipboardCaretContent saveLastPastedCaretsForTransferable(final @NotNull Editor editor, @NotNull Transferable content, boolean useLastOffsetAdjuster) {
        if (useLastOffsetAdjuster) {
            return saveLastPastedCaretsForTransferable(editor, content, editor.getUserData(LAST_CARET_OFFSET_ADJUSTER));
        } else {
            return saveLastPastedCaretsForTransferable(editor, content, null);
        }
    }

    @Nullable
    public static ClipboardCaretContent saveLastPastedCaretsForTransferable(final @NotNull Editor editor, @NotNull Transferable content, @Nullable CaretOffsetAdjuster offsetAdjuster) {
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

            // save it so it can be re-used by MiaMultiplePaste
            if (caretData == null) {
                // we make one for a single caret
            }

            editor.putUserData(LAST_CARET_OFFSET_ADJUSTER, offsetAdjuster);

            final TextRange[] ranges = new TextRange[caretCount];
            final int[] columns = new int[caretCount];
            final Iterator<String> segments = new ClipboardTextPerCaretSplitter().split(text, caretData, caretCount).iterator();
            final List<Caret> carets = editor.getCaretModel().getAllCarets();
            BitSet fullLines = new BitSet(caretCount);
            BitSet charLines = new BitSet(caretCount);
            int cumulativeOffset = 0;
            int selectionSize = 0;

            for (int i = 0; i < caretCount; i++) {
                String normalizedText = TextBlockTransferable.convertLineSeparators(editor, segments.next());
                Caret caret = carets.get(i);
                final boolean isFullLine = normalizedText.endsWith("\n");
                columns[i] = caret.getLogicalPosition().column; // save column before possible adjustment
                int originalOffset = caret.getOffset();
                int caretOffset = offsetAdjuster == null ? caret.getOffset() : offsetAdjuster.getOffset(caret, isFullLine);
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
                ranges[i] = new TextRange(caretOffset + cumulativeOffset, caretOffset + cumulativeOffset + normalizedText.length());
                if (i == 0) {
                    selectionSize = selectionDelta;
                }
                if (isFullLine) fullLines.set(i);
                else if (normalizedText.contains("\n")) charLines.set(i);
                cumulativeOffset += normalizedText.length();
                cumulativeOffset -= selectionDelta;
            }

            if (caretCount == 1) {
                // have to use selection size not the range for post-paste offsets
                int startOffset = editor.getCaretModel().getPrimaryCaret().getSelectionStart();
                int endOffset = startOffset + selectionSize;
                boolean rangeAtTextStart = startOffset == 0;
                boolean rangeAtTextEnd = endOffset == editor.getDocument().getTextLength();
                if (selectionSize == 0 && !rangeAtTextStart) startOffset--;
                if (selectionSize == 0 && !rangeAtTextEnd) endOffset++;
                RangeMarker rangeMarker = editor.getDocument().createRangeMarker(startOffset, endOffset);

                return new ClipboardCaretContent(content, ranges, columns, selectionSize != 0, null, caretCount, caretCount, fullLines, charLines, rangeMarker, rangeAtTextStart, rangeAtTextEnd);
            } else {
                return new ClipboardCaretContent(content, ranges, columns, selectionSize != 0, null, caretCount, caretCount, fullLines, charLines, null, false, false);
            }
        } else {
            int caretOffset = editor.getCaretModel().getOffset();
            String normalizedText = TextBlockTransferable.convertLineSeparators(editor, text);
            BitSet fullLines = new BitSet(1);
            BitSet charLines = new BitSet(1);
            if (normalizedText.endsWith("\n")) fullLines.set(0);
            else if (normalizedText.contains("\n")) charLines.set(0);
            return new ClipboardCaretContent(content, new TextRange[] { new TextRange(caretOffset, caretOffset + normalizedText.length()) }, null, false, null, 1, 1, fullLines, charLines, null, false, false);
        }
    }

    @NotNull
    public String getStringRep(final int maxLen, final String showEOL, final boolean addCharFinalEOL, final boolean removeFullLineEOL) {
        return getStringRep(myTexts, null, this, maxLen, showEOL, addCharFinalEOL, removeFullLineEOL);
    }

    @NotNull
    public static String getStringRep(String[] texts, @Nullable BiConsumer<Integer, Integer> rangeConsumer, ClipboardCaretContent caretContent, final int maxLen, final String showEOL, final boolean addCharFinalEOL, final boolean removeFullLineEOL) {
        // convert multi caret text to \n separated ranges
        StringBuilder sb = new StringBuilder();
        if (texts != null) {
            int iMax = texts.length;
            assert caretContent.getCaretCount() >= texts.length;

            int endOffset = 0;

            for (int i = 0; i < iMax; i++) {
                if (maxLen > 0 && sb.length() >= maxLen) {
                    if (sb.length() == maxLen) sb.append(' ');
                    break;
                }

                int startOffset = sb.length();

                if (caretContent.isFullLine(i)) {
                    if (showEOL != null) {
                        sb.append(texts[i].substring(0, texts[i].length() - 1));
                        sb.append(showEOL);
                        if (!removeFullLineEOL || i < iMax - 1) {
                            sb.append('\n');
                        }
                    } else {
                        if (removeFullLineEOL && i == iMax - 1) {
                            sb.append(texts[i].substring(0, texts[i].length() - 1));
                        } else {
                            sb.append(texts[i]);
                        }
                    }
                } else {
                    sb.append(texts[i]);
                    if (addCharFinalEOL || i < iMax - 1) {
                        sb.append('\n');
                    }
                }

                if (rangeConsumer != null) {
                    rangeConsumer.accept(i, endOffset);
                    endOffset += sb.length() - startOffset - texts[i].length();
                }
            }

            final String s = sb.toString();
            return maxLen > 0 && s.length() >= maxLen ? s.substring(0, maxLen) + "..." : s;
        }
        return "";
    }
}
