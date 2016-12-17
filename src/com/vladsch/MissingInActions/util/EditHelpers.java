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
import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import com.intellij.codeInsight.generation.CommentByBlockCommentHandler;
import com.intellij.lang.Commenter;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actions.EditorActionUtil;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.impl.AbstractFileType;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.MissingInActions.manager.EditorPosition;
import com.vladsch.MissingInActions.manager.EditorPositionFactory;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.intellij.openapi.diagnostic.Logger.getInstance;
import static java.lang.Character.*;

@SuppressWarnings({ "SameParameterValue", "WeakerAccess" })
public class EditHelpers {
    private static final Logger LOG = getInstance("com.vladsch.MissingInActions");

    public static final int START_OF_WORD = 0x0001;
    public static final int END_OF_WORD = 0x0002;
    public static final int START_OF_TRAILING_BLANKS = 0x0004;
    public static final int END_OF_LEADING_BLANKS = 0x0008;
    public static final int START_OF_LINE = 0x0010;
    public static final int END_OF_LINE = 0x0020;
    public static final int MIA_IDENTIFIER = 0x0040;
    public static final int START_OF_FOLDING_REGION = 0x0080;
    public static final int END_OF_FOLDING_REGION = 0x0100;
    public static final int SINGLE_LINE = 0x0200;
    public static final int MULTI_CARET_SINGLE_LINE = 0x0400;
    public static final int IDE_WORD = 0x0800;
    public static final int SPACE_DELIMITED = 0x1000;
    public static final int MIA_WORD = 0x2000;

    public static final int WORD_SPACE_DELIMITED = 0;
    public static final int WORD_IDE = 1;
    public static final int WORD_MIA = 2;
    public static final int WORD_IDENTIFIER = 3;

    public static int getWordType(int flags) {
        if ((flags & MIA_IDENTIFIER) != 0) return WORD_IDENTIFIER;
        if ((flags & IDE_WORD) != 0) return WORD_IDE;
        if ((flags & SPACE_DELIMITED) != 0) return WORD_SPACE_DELIMITED;
        return WORD_MIA;
    }

    @SuppressWarnings("PointlessBitwiseExpression")
    public static int BOUNDARY_FLAGS = 0
            | START_OF_WORD
            | END_OF_WORD
            | START_OF_TRAILING_BLANKS
            | END_OF_LEADING_BLANKS
            | START_OF_LINE
            | END_OF_LINE
            | START_OF_FOLDING_REGION
            | END_OF_FOLDING_REGION
            | SINGLE_LINE;

    public static boolean isSet(int options, int flag) {
        return (options & flag) != 0;
    }

    public static void moveCaretToNextWordStartOrEnd(@NotNull Editor editor, boolean isWithSelection, boolean camel, int flags) {
        if (!isSet(flags, BOUNDARY_FLAGS)) return;

        Document document = editor.getDocument();
        SelectionModel selectionModel = editor.getSelectionModel();
        int selectionStart = selectionModel.getLeadSelectionOffset();
        CaretModel caretModel = editor.getCaretModel();
        LogicalPosition blockSelectionStart = caretModel.getLogicalPosition();
        boolean haveMultiCarets = caretModel.getCaretCount() > 1;

        boolean stopAtTrailingBlanks = isSet(flags, START_OF_TRAILING_BLANKS);
        boolean stopAtLeadingBlanks = isSet(flags, END_OF_LEADING_BLANKS);
        boolean stopAtStartOfLine = isSet(flags, START_OF_LINE);
        boolean stopAtStartOfWord = isSet(flags, START_OF_WORD);
        boolean stopAtEndOfWord = isSet(flags, END_OF_WORD);
        boolean stopAtStartOfFolding = isSet(flags, START_OF_FOLDING_REGION);
        boolean stopAtEndOfFolding = isSet(flags, END_OF_FOLDING_REGION);
        boolean stopAtEndOfLine = isSet(flags, END_OF_LINE);
        boolean strictIdentifier = isSet(flags, MIA_IDENTIFIER);
        boolean singleLine = isSet(flags, SINGLE_LINE) || isSet(flags, MULTI_CARET_SINGLE_LINE) && haveMultiCarets;

        int offset = caretModel.getOffset();
        if (offset == document.getTextLength()) {
            return;
        }

        int lineNumber = caretModel.getLogicalPosition().line;
        if (lineNumber >= document.getLineCount()) return;

        int stopAtLastNonBlank = 0;

        // have to stop at start of character if caret is not at or before first non-blank
        // only applies to start boundary condition
        int lineStartOffset = document.getLineStartOffset(lineNumber);
        if (stopAtTrailingBlanks || stopAtEndOfLine) {
            int lineEndOffset = document.getLineEndOffset(lineNumber);
            int trailingBlanks = countWhiteSpaceReversed(document.getCharsSequence(), lineStartOffset, lineEndOffset);
            if (stopAtTrailingBlanks && caretModel.getOffset() < lineEndOffset - trailingBlanks) {
                stopAtLastNonBlank = lineEndOffset - trailingBlanks;
            } else if (stopAtEndOfLine && (caretModel.getOffset() < lineEndOffset || singleLine)) {
                stopAtLastNonBlank = lineEndOffset;
            }
        }

        int maxLineNumber = stopAtLastNonBlank > 0 || lineNumber + 1 > document.getLineCount() ? lineNumber : lineNumber + 1;
        int maxOffset = stopAtLastNonBlank > 0 ? stopAtLastNonBlank :
                (stopAtStartOfLine && lineNumber < maxLineNumber ? document.getLineStartOffset(maxLineNumber) : document.getLineEndOffset(maxLineNumber));

        int newOffset = offset + 1;
        if (newOffset > maxOffset) return;

        boolean done = false;
        FoldRegion currentFoldRegion = editor.getFoldingModel().getCollapsedRegionAtOffset(offset);
        if (currentFoldRegion != null) {
            newOffset = currentFoldRegion.getEndOffset();
            if (stopAtEndOfFolding) done = true;
        }

        int wordType = getWordType(flags);
        while (!done) {
            for (; newOffset < maxOffset; newOffset++) {
                if (stopAtStartOfWord && isWordTypeStart(wordType, editor, newOffset, camel)) {
                    done = true;
                    break;
                }
                if (stopAtEndOfWord && isWordTypeEnd(wordType, editor, newOffset, camel)) {
                    done = true;
                    break;
                }
            }
            if (newOffset >= maxOffset) break;

            FoldRegion foldRegion = editor.getFoldingModel().getCollapsedRegionAtOffset(newOffset);
            if (foldRegion != null) {
                if (stopAtStartOfFolding) {
                    newOffset = foldRegion.getStartOffset();
                    break;
                }
                newOffset = foldRegion.getEndOffset();
                if (stopAtEndOfFolding) break;
            }
        }

        if (editor instanceof EditorImpl) {
            int boundaryOffset = ((EditorImpl) editor).findNearestDirectionBoundary(offset, true);
            if (boundaryOffset >= 0) {
                newOffset = Math.min(boundaryOffset, newOffset);
            }
        }

        caretModel.moveToOffset(newOffset);
        EditorModificationUtil.scrollToCaret(editor);
        setupSelection(editor, isWithSelection, selectionStart, blockSelectionStart);
    }

    public static void moveCaretToPreviousWordStartOrEnd(@NotNull Editor editor, boolean isWithSelection, boolean camel, int flags) {
        if (!isSet(flags, BOUNDARY_FLAGS)) return;

        Document document = editor.getDocument();
        SelectionModel selectionModel = editor.getSelectionModel();
        int selectionStart = selectionModel.getLeadSelectionOffset();
        CaretModel caretModel = editor.getCaretModel();
        LogicalPosition blockSelectionStart = caretModel.getLogicalPosition();
        boolean haveMultiCarets = caretModel.getCaretCount() > 1;

        int offset = caretModel.getOffset();
        if (offset == 0) return;

        boolean stopAtTrailingBlanks = isSet(flags, START_OF_TRAILING_BLANKS);
        boolean stopAtLeadingBlanks = isSet(flags, END_OF_LEADING_BLANKS);
        boolean stopAtStartOfLine = isSet(flags, START_OF_LINE);
        boolean stopAtStartOfWord = isSet(flags, START_OF_WORD);
        boolean stopAtEndOfWord = isSet(flags, END_OF_WORD);
        boolean stopAtEndOfLine = isSet(flags, END_OF_LINE);
        boolean stopAtStartOfFolding = isSet(flags, START_OF_FOLDING_REGION);
        boolean stopAtEndOfFolding = isSet(flags, END_OF_FOLDING_REGION);
        boolean strictIdentifier = isSet(flags, MIA_IDENTIFIER);
        boolean singleLine = isSet(flags, SINGLE_LINE) || isSet(flags, MULTI_CARET_SINGLE_LINE) && haveMultiCarets;

        LogicalPosition position = caretModel.getLogicalPosition();
        int lineNumber = position.line;
        int stopAtIndent = 0;

        // have to stop at start of character if caret is not at or before first non-blank
        int lineStartOffset = document.getLineStartOffset(lineNumber);

        if (stopAtTrailingBlanks) {
            int lineEndOffset = document.getLineEndOffset(lineNumber);
            int trailingBlanks = countWhiteSpaceReversed(document.getCharsSequence(), lineStartOffset, lineEndOffset);
            if (offset > lineEndOffset - trailingBlanks) {
                stopAtIndent = lineEndOffset - trailingBlanks;
            }
        }
        if (stopAtIndent == 0 && (stopAtLeadingBlanks || stopAtStartOfLine)) {
            int firstNonBlank = countWhiteSpace(document.getCharsSequence(), lineStartOffset, document.getLineEndOffset(lineNumber));
            if (stopAtLeadingBlanks && position.column > firstNonBlank) {
                stopAtIndent = lineStartOffset + firstNonBlank;
            } else if (stopAtStartOfLine && (position.column != 0 || singleLine)) {
                stopAtIndent = lineStartOffset;
            }
        }

        int minLineNumber = lineNumber == 0 || stopAtIndent > 0 ? lineNumber : lineNumber - 1;
        int minOffset = stopAtIndent > 0 ? stopAtIndent :
                (stopAtEndOfLine && lineNumber > minLineNumber ? document.getLineEndOffset(minLineNumber) : document.getLineStartOffset(minLineNumber));

        // if virtual spaces are enabled the caret can be after the end so we should pretend it is on the next char after the end
        int newOffset = blockSelectionStart.column > offset - lineStartOffset ? offset : offset - 1;
        if (newOffset < minOffset) return;

        boolean done = false;
        FoldRegion currentFoldRegion = editor.getFoldingModel().getCollapsedRegionAtOffset(offset - 1);
        if (currentFoldRegion != null) {
            newOffset = currentFoldRegion.getStartOffset();
            if (stopAtStartOfFolding) done = true;
        }

        int wordType = getWordType(flags);
        while (!done) {
            for (; newOffset > minOffset; newOffset--) {
                if (stopAtStartOfWord && isWordTypeEnd(wordType, editor, newOffset, camel)) {
                    done = true;
                    break;
                }
                if (stopAtEndOfWord && isWordTypeStart(wordType, editor, newOffset, camel)) {
                    done = true;
                    break;
                }
            }
            if (newOffset <= minOffset) break;

            FoldRegion foldRegion = editor.getFoldingModel().getCollapsedRegionAtOffset(newOffset);
            if (foldRegion != null) {
                if (stopAtEndOfFolding) {
                    newOffset = foldRegion.getEndOffset();
                    break;
                }
                newOffset = foldRegion.getStartOffset();
                if (stopAtStartOfFolding) break;
            }
        }

        if (editor instanceof EditorImpl) {
            int boundaryOffset = ((EditorImpl) editor).findNearestDirectionBoundary(offset, false);
            if (boundaryOffset >= 0) {
                newOffset = Math.max(boundaryOffset, newOffset);
            }
            caretModel.moveToLogicalPosition(editor.offsetToLogicalPosition(newOffset).leanForward(true));
        } else {
            editor.getCaretModel().moveToOffset(newOffset);
        }

        EditorModificationUtil.scrollToCaret(editor);
        setupSelection(editor, isWithSelection, selectionStart, blockSelectionStart);
    }

    public static boolean isWordTypeStart(int wordType, @NotNull Editor editor, int offset, boolean isCamel) {
        switch (wordType) {
            case WORD_SPACE_DELIMITED:
                return isWhitespaceEnd(editor.getDocument().getCharsSequence(), offset, isCamel);
            case WORD_IDE:
                return EditorActionUtil.isWordOrLexemeStart(editor, offset, isCamel);
            case WORD_MIA:
                return isWordStart(editor.getDocument().getCharsSequence(), offset, isCamel);
            case WORD_IDENTIFIER:
                return isIdentifierStart(editor.getDocument().getCharsSequence(), offset, isCamel);
        }
        return false;
    }

    public static boolean isWordTypeEnd(int wordType, @NotNull Editor editor, int offset, boolean isCamel) {
        switch (wordType) {
            case WORD_SPACE_DELIMITED:
                return isWhitespaceStart(editor.getDocument().getCharsSequence(), offset);
            case WORD_IDE:
                return EditorActionUtil.isWordOrLexemeEnd(editor, offset, isCamel);
            case WORD_MIA:
                return isWordEnd(editor.getDocument().getCharsSequence(), offset, isCamel);
            case WORD_IDENTIFIER:
                return isIdentifierEnd(editor.getDocument().getCharsSequence(), offset, isCamel);
        }
        return false;
    }

    public static boolean isWordTypeStart(int wordType, @NotNull CharSequence charSequence, int offset, boolean isCamel) {
        switch (wordType) {
            case WORD_SPACE_DELIMITED:
                return isWhitespaceEnd(charSequence, offset, isCamel);
            case WORD_IDE:
                throw new IllegalArgumentException("wordType: WORD_IDE is only supported with editor parameter based isWordTypeStart function");
            case WORD_MIA:
                return isWordStart(charSequence, offset, isCamel);
            case WORD_IDENTIFIER:
                return isIdentifierStart(charSequence, offset, isCamel);
        }
        return false;
    }

    public static boolean isWordTypeEnd(int wordType, @NotNull CharSequence charSequence, int offset, boolean isCamel) {
        switch (wordType) {
            case WORD_SPACE_DELIMITED:
                return isWhitespaceStart(charSequence, offset);
            case WORD_IDE:
                throw new IllegalArgumentException("wordType: WORD_IDE is only supported with editor parameter based isWordTypeEnd function");
            case WORD_MIA:
                return isWordEnd(charSequence, offset, isCamel);
            case WORD_IDENTIFIER:
                return isIdentifierEnd(charSequence, offset, isCamel);
        }
        return false;
    }

    public static boolean isWordType(int wordType, @NotNull CharSequence charSequence, int offset) {
        switch (wordType) {
            case WORD_SPACE_DELIMITED:
                return offset >= 0 && offset < charSequence.length() && isWhitespace(charSequence.charAt(offset));
            case WORD_IDE:
            case WORD_MIA:
            case WORD_IDENTIFIER:
                return isIdentifier(charSequence, offset);
        }
        return false;
    }

    public static boolean isWordStart(@NotNull Editor editor, int offset, boolean isCamel) {
        CharSequence chars = editor.getDocument().getCharsSequence();
        return isWordStart(chars, offset, isCamel);
    }

    public static boolean isWordEnd(@NotNull Editor editor, int offset, boolean isCamel) {
        CharSequence chars = editor.getDocument().getCharsSequence();
        return isWordEnd(chars, offset, isCamel);
    }

    public static boolean isIdentifierStart(@NotNull Editor editor, int offset, boolean isCamel) {
        CharSequence chars = editor.getDocument().getCharsSequence();
        return isIdentifierStart(chars, offset, isCamel);
    }

    public static boolean isIdentifierEnd(@NotNull Editor editor, int offset, boolean isCamel) {
        CharSequence chars = editor.getDocument().getCharsSequence();
        return isIdentifierEnd(chars, offset, isCamel);
    }

    public static boolean isWhitespaceStart(@NotNull CharSequence text, int offset) {
        char prev = offset > 0 ? text.charAt(offset - 1) : 0;
        char current = offset < text.length() ? text.charAt(offset) : 0;

        return (!Character.isWhitespace(prev) && Character.isWhitespace(current));
    }

    public static boolean isWhitespaceMiddle(@NotNull CharSequence text, int offset) {
        char prev = offset > 0 ? text.charAt(offset - 1) : 0;
        char current = offset < text.length() ? text.charAt(offset) : 0;

        return (Character.isWhitespace(prev) && Character.isWhitespace(current));
    }

    public static boolean isWhitespaceEnd(@NotNull CharSequence text, int offset, boolean isCamel) {
        char prev = offset > 0 ? text.charAt(offset - 1) : 0;
        char current = offset < text.length() ? text.charAt(offset) : 0;

        return (Character.isWhitespace(prev) && !Character.isWhitespace(current));
    }

    public static boolean isIdentifier(@NotNull CharSequence text, int offset) {
        return offset >= 0 && offset < text.length() && Character.isJavaIdentifierPart(text.charAt(offset));
    }

    public static boolean isWordStart(@NotNull CharSequence text, int offset, boolean isCamel) {
        char prev = offset > 0 ? text.charAt(offset - 1) : 0;
        char current = offset < text.length() ? text.charAt(offset) : 0;

        final boolean firstIsIdentifierPart = prev != 0 && Character.isJavaIdentifierPart(prev);
        final boolean secondIsIdentifierPart = current != 0 && Character.isJavaIdentifierPart(current);
        if (!firstIsIdentifierPart && secondIsIdentifierPart) {
            return true;
        }

        if (isCamel && firstIsIdentifierPart && secondIsIdentifierPart && isHumpBoundWord(text, offset, true)) {
            return true;
        }

        return (Character.isWhitespace(prev) || firstIsIdentifierPart) &&
                !Character.isWhitespace(current) && !secondIsIdentifierPart;
    }

    public static boolean isWordEnd(@NotNull CharSequence text, int offset, boolean isCamel) {
        char prev = offset > 0 ? text.charAt(offset - 1) : 0;
        char current = offset < text.length() ? text.charAt(offset) : 0;
        char next = offset + 1 < text.length() ? text.charAt(offset + 1) : 0;

        final boolean firstIsIdentifierPart = prev != 0 && Character.isJavaIdentifierPart(prev);
        final boolean secondIsIdentifierPart = current != 0 && Character.isJavaIdentifierPart(current);
        if (firstIsIdentifierPart && !secondIsIdentifierPart) {
            return true;
        }

        if (isCamel) {
            if (firstIsIdentifierPart
                    && (Character.isLowerCase(prev) && isUpperCase(current)
                    || prev != '_' && current == '_'
                    || isUpperCase(prev) && isUpperCase(current) && Character.isLowerCase(next))) {
                return true;
            }
        }

        return !Character.isWhitespace(prev) && !firstIsIdentifierPart &&
                (Character.isWhitespace(current) || secondIsIdentifierPart);
    }

    public static boolean isIdentifierStart(@NotNull CharSequence text, int offset, boolean isCamel) {
        char prev = offset > 0 ? text.charAt(offset - 1) : 0;
        char current = offset < text.length() ? text.charAt(offset) : 0;

        final boolean prevIsIdentifierPart = prev != 0 && Character.isJavaIdentifierPart(prev);
        final boolean currentIsIdentifierPart = current != 0 && Character.isJavaIdentifierPart(current);

        //noinspection SimplifiableIfStatement
        if (!prevIsIdentifierPart && currentIsIdentifierPart) return true;

        return isCamel && prevIsIdentifierPart && currentIsIdentifierPart && isHumpBoundIdentifier(text, offset, true);
    }

    public static boolean isIdentifierEnd(@NotNull CharSequence text, int offset, boolean isCamel) {
        char prev = offset > 0 ? text.charAt(offset - 1) : 0;
        char current = offset < text.length() ? text.charAt(offset) : 0;
        char next = offset + 1 < text.length() ? text.charAt(offset + 1) : 0;

        final boolean prevIsIdentifierPart = prev != 0 && Character.isJavaIdentifierPart(prev);
        final boolean currentIsIdentifierPart = current != 0 && Character.isJavaIdentifierPart(current);

        //noinspection SimplifiableIfStatement
        if (prevIsIdentifierPart && !currentIsIdentifierPart) return true;

        return isCamel && prevIsIdentifierPart
                && (Character.isLowerCase(prev) && isUpperCase(current)
                || prev != '_' && current == '_'
                || isUpperCase(prev) && isUpperCase(current) && Character.isLowerCase(next));
    }

/*
    public static boolean isHumpBoundStart(@NotNull CharSequence editorText, int offset) {
        return isHumpBoundIdentifier(editorText, offset, true);
    }

    public static boolean isHumpBoundEnd(@NotNull CharSequence editorText, int offset) {
        return isHumpBoundIdentifier(editorText, offset, false);
    }
*/

    public static boolean isHumpBoundWord(@NotNull CharSequence editorText, int offset, boolean start) {
        if (offset <= 0) return start;
        else if (offset >= editorText.length()) return !start;

        final char prevChar = offset > 0 ? editorText.charAt(offset - 1) : 0;
        final char curChar = editorText.charAt(offset);
        final char nextChar = offset + 1 < editorText.length() ? editorText.charAt(offset + 1) : 0; // 0x00 is not lowercase.

        return isLowerCaseOrDigit(prevChar) && isUpperCase(curChar) ||
                start && prevChar == '_' && curChar != '_' ||
                !start && prevChar != '_' && curChar == '_' ||
                start && prevChar == '$' && isLetterOrDigit(curChar) ||
                !start && isLetterOrDigit(prevChar) && curChar == '$' ||
                isUpperCase(prevChar) && isUpperCase(curChar) && Character.isLowerCase(nextChar);
    }

    public static boolean isHumpBoundIdentifier(@NotNull CharSequence editorText, int offset, boolean start) {
        if (offset <= 0) return start;
        else if (offset >= editorText.length()) return !start;
        final char prevChar = editorText.charAt(offset - 1);
        final char curChar = editorText.charAt(offset);

        return isLowerCaseOrDigit(prevChar) && isUpperCase(curChar) ||
                start && prevChar == '_' && curChar != '_' ||
                start && prevChar == '$' && isLetterOrDigit(curChar) ||
                !start && prevChar != '_' && curChar == '_' ||
                !start && isLetterOrDigit(prevChar) && curChar == '$';
    }

    public static boolean isSnakeCaseBound(@NotNull CharSequence editorText, int offset, boolean start) {
        if (offset <= 0) return start;
        else if (offset >= editorText.length()) return !start;

        final char prevChar = editorText.charAt(offset - 1);
        final char curChar = editorText.charAt(offset);

        return start ? prevChar == '_' && curChar != '_' && isLetterOrDigit(curChar) : prevChar != '_' && curChar == '_' && isLetterOrDigit(prevChar);
    }

/*
    public static boolean isHumpBoundEnd(@NotNull CharSequence editorText, int offset, boolean start) {
        if (offset <= 0 || offset >= editorText.length()) return false;
        final char prevChar = editorText.charAt(offset - 1);
        final char curChar = editorText.charAt(offset);
        final char nextChar = offset + 1 < editorText.length() ? editorText.charAt(offset + 1) : 0; // 0x00 is not lowercase.

        return isLowerCaseOrDigit(prevChar) && Character.isUpperCase(curChar) ||
                !start && prevChar != '_' && curChar == '_' ||
                !start && Character.isLetterOrDigit(prevChar) && curChar == '$';
    }
*/

    public static boolean isLowerCaseOrDigit(char c) {
        return Character.isLowerCase(c) || Character.isDigit(c);
    }

    private static void setupSelection(@NotNull Editor editor, boolean isWithSelection, int selectionStart, @NotNull LogicalPosition blockSelectionStart) {
        SelectionModel selectionModel = editor.getSelectionModel();
        CaretModel caretModel = editor.getCaretModel();
        if (isWithSelection) {
            if (editor.isColumnMode() && !caretModel.supportsMultipleCarets()) {
                selectionModel.setBlockSelection(blockSelectionStart, caretModel.getLogicalPosition());
            } else {
                selectionModel.setSelection(selectionStart, caretModel.getVisualPosition(), caretModel.getOffset());
            }
        } else {
            selectionModel.removeSelection();
        }

        EditorActionUtil.selectNonexpandableFold(editor);
    }

    public static int countWhiteSpace(CharSequence chars, int start, int end) {
        int pos = start;
        int length = chars.length();
        if (end > length) end = length;
        while (pos < end) {
            char c = chars.charAt(pos);
            if (c != ' ' && c != '\t') break;
            pos++;
        }
        return pos - start;
    }

    public static int countWhiteSpaceReversed(CharSequence chars, int start, int end) {
        int length = chars.length();
        if (end > length) end = length;
        int pos = end - 1;
        while (pos >= start) {
            char c = chars.charAt(pos);
            if (c != ' ' && c != '\t') break;
            pos--;
        }
        return end - pos - 1;
    }

    public static void deleteSelectedText(@NotNull Editor editor) {
        SelectionModel selectionModel = editor.getSelectionModel();
        if (!selectionModel.hasSelection()) return;

        int selectionStart = selectionModel.getSelectionStart();
        int selectionEnd = selectionModel.getSelectionEnd();

        VisualPosition selectionStartPosition = selectionModel.getSelectionStartPosition();
        if (editor.isColumnMode() && editor.getCaretModel().supportsMultipleCarets() && selectionStartPosition != null) {
            editor.getCaretModel().moveToVisualPosition(selectionStartPosition);
            selectionModel.removeSelection();
            editor.getDocument().deleteString(selectionStart, selectionEnd);
            EditorModificationUtil.scrollToCaret(editor);
        } else {
            // we handle line type selection deletes
            delete(editor, editor.getCaretModel().getPrimaryCaret(), selectionStart, selectionEnd, false);
        }
    }

    public static void deleteSelectedText(@NotNull Editor editor, @NotNull Caret caret) {
        delete(editor, caret, caret.getSelectionStart(), caret.getSelectionEnd(), false);
    }

    public static void deleteSelectedText(@NotNull Editor editor, @NotNull Caret caret, boolean clearOnly) {
        delete(editor, caret, caret.getSelectionStart(), caret.getSelectionEnd(), clearOnly);
    }

    public static void delete(@NotNull Editor editor, @NotNull Caret caret, int start, int end, boolean clearOnly) {
        CopyPasteManager.getInstance().stopKillRings();
        if (clearOnly) {
            editor.getDocument().replaceString(start, end, new RepeatedCharSequence(' ', end - start));
        } else {
            LineSelectionManager manager = LineSelectionManager.getInstance(editor);
            manager.guard(() -> {
                EditorCaret editorCaret = manager.getEditorCaret(caret);
                if (editorCaret.isLine()) {
                    EditorPositionFactory f = manager.getPositionFactory();
                    EditorPosition pos = f.fromPosition(caret.getLogicalPosition());
                    EditorPosition selStart = f.fromOffset(start);

                    editor.getDocument().deleteString(start, end);

                    // in case the caret was in the virtual space, we force it to go back to the real offset
                    caret.moveToLogicalPosition(selStart.atColumn(pos.column));
                } else {
                    editor.getDocument().deleteString(start, end);
                    // in case the caret was in the virtual space, we force it to go back to the real offset
                    caret.moveToOffset(start);
                }
                EditorModificationUtil.scrollToCaret(editor);
            });
        }
    }

    public static void scrollToCaret(Editor editor) {
        EditorModificationUtil.scrollToCaret(editor);
    }

    public static void restoreState(@Nullable Caret newCaret, CaretState caretState, boolean alwaysSetSelection) {
        if (newCaret != null) {
            if (caretState.getCaretPosition() != null) {
                newCaret.moveToLogicalPosition(caretState.getCaretPosition());
            }

            if (caretState.getSelectionStart() != null && caretState.getSelectionEnd() != null) {
                newCaret.setSelection(newCaret.getEditor().logicalPositionToOffset(caretState.getSelectionStart()), newCaret.getEditor().logicalPositionToOffset(caretState.getSelectionEnd()));
            } else if (alwaysSetSelection) {
                newCaret.setSelection(newCaret.getOffset(), newCaret.getOffset());
            }
        }
    }

    public static Range getCaretRange(@NotNull Caret caret, boolean backwards, boolean lineMode, boolean singleLine) {
        @NotNull Editor editor = caret.getEditor();
        Range range;

        if (caret.hasSelection()) {
            range = new Range(caret.getSelectionEnd(), caret.getSelectionStart());
        } else {
            LogicalPosition caretPosition = caret.getLogicalPosition();
            range = backwards ? new Range(0, caret.getOffset()) : new Range(caret.getOffset(), editor.getDocument().getTextLength());

            if (caret.getCaretModel().getCaretCount() > 1) {
                // here we need to figure things out
                List<Caret> carets = caret.getCaretModel().getAllCarets();
                for (Caret other : carets) {
                    if (!backwards) {
                        int span = caret.getOffset() - other.getOffset();
                        if (range.getSpan() > span) {
                            range = range.withEnd(other.getOffset());
                            if (lineMode) {
                                LogicalPosition otherPosition = other.getLogicalPosition();
                                if (caretPosition.line != otherPosition.line) {
                                    // chop off range where the other caret's line ends
                                    range = range.withEnd(editor.getDocument().getLineSeparatorLength(otherPosition.line));
                                }
                            }
                        }
                    } else {

                        int span = caret.getOffset() - other.getOffset();
                        if (range.getSpan() > -span) {
                            range = range.withStart(other.getOffset());

                            if (lineMode) {
                                LogicalPosition otherPosition = other.getLogicalPosition();
                                if (caretPosition.line != otherPosition.line) {
                                    // chop off range where the other caret's line ends
                                    range = range.withStart(editor.getDocument().getLineEndOffset(otherPosition.line));
                                }
                            }
                        }
                    }
                }

                if (singleLine) {
                    // limit it to the caret line
                }
            }
        }
        return range;
    }

    public static int getNextWordStartAtOffset(CharSequence charSequence, int offset, int wordType, boolean isCamel, boolean stopIfNonWord) {
        // move back on line to start of word
        int newOffset = offset;
        int length = charSequence.length();
        do {
            if (isWordTypeStart(wordType, charSequence, newOffset, isCamel)) {
                return newOffset;
            }
            if (stopIfNonWord && !isWordType(wordType, charSequence, newOffset)) break;
            newOffset++;
        } while (newOffset < length);

        return offset;
    }

    public static int getPreviousWordStartAtOffset(CharSequence charSequence, int offset, int wordType, boolean isCamel, boolean stopIfNonWord) {
        // move back on line to start of word
        int newOffset = offset;
        do {
            if (isWordTypeStart(wordType, charSequence, newOffset, isCamel)) {
                return newOffset;
            }
            if (stopIfNonWord && !isWordTypeEnd(wordType, charSequence, newOffset, false) && !isWordType(wordType, charSequence, newOffset)) break;
            newOffset--;
        } while (newOffset >= 0);

        return offset;
    }

    public static int getPreviousWordEndAtOffset(CharSequence charSequence, int offset, int wordType, boolean isCamel, boolean stopIfNonWord) {
        // move back on line to start of word
        int newOffset = offset;
        do {
            if (isWordTypeEnd(wordType, charSequence, newOffset, isCamel)) {
                return newOffset;
            }
            if (stopIfNonWord && !isWordType(wordType, charSequence, newOffset)) break;
            newOffset--;
        } while (newOffset >= 0);

        return offset;
    }

    public static int getNextWordEndAtOffset(CharSequence charSequence, int offset, int wordType, boolean isCamel, boolean stopIfNonWord) {
        // move back on line to start of word
        int newOffset = offset;
        int length = charSequence.length();
        do {
            if (isWordTypeEnd(wordType, charSequence, newOffset, isCamel)) {
                return newOffset;
            }
            if (stopIfNonWord && !isWordType(wordType, charSequence, newOffset)) break;
            newOffset++;
        } while (newOffset <= length);

        return offset;
    }

    public static int getWordStartAtOffset(CharSequence charSequence, int offset, int wordType, boolean isCamel, boolean stopIfNonWord) {
        if (wordType != WORD_SPACE_DELIMITED && !isIdentifier(charSequence, offset) && !isWordEnd(charSequence, offset, false) || wordType == WORD_SPACE_DELIMITED && isWhitespaceMiddle(charSequence, offset)) {
            // go forward
            return offset;//getNextWordStartAtOffset(charSequence, offset, wordType, isCamel);
        } else {
            // go backwards
            return getPreviousWordStartAtOffset(charSequence, offset, wordType, isCamel, stopIfNonWord);
        }
    }

    public static int getWordEndAtOffset(CharSequence charSequence, int offset, int wordType, boolean isCamel, boolean stopIfNonWord) {
        if (wordType != WORD_SPACE_DELIMITED && !isIdentifier(charSequence, offset) && !isWordStart(charSequence, offset, false) || wordType == WORD_SPACE_DELIMITED && isWhitespaceMiddle(charSequence, offset)) {
            // go backwards
            return offset; //getPreviousWordEndAtOffset(charSequence, offset, wordType, isCamel);
        } else {
            // go forward
            return getNextWordEndAtOffset(charSequence, offset, wordType, isCamel, stopIfNonWord);
        }
    }

    public static TextRange getWordRangeAtOffsets(CharSequence charSequence, int start, int end, int wordType, boolean isCamel, boolean stopIfNonWord) {
        if (start < 0) start = 0;
        if (end > charSequence.length()) end = charSequence.length();
        if (start > end) start = end;
        if (end < start) end = start;

        int startOffset = getWordStartAtOffset(charSequence, start, wordType, isCamel, stopIfNonWord);
        int endOffset = getWordEndAtOffset(charSequence, Math.max(startOffset, end), wordType, isCamel, stopIfNonWord);

        // trim to word
        while (startOffset < endOffset && !isWordType(wordType, charSequence, startOffset)) startOffset++;
        while (startOffset < endOffset && !isWordType(wordType, charSequence, endOffset - 1)) endOffset--;
        if (stopIfNonWord) {
            if (startOffset > end) startOffset = end;
            if (endOffset < start) endOffset = start;
        }

        return startOffset > endOffset ? new TextRange(start, end) : new TextRange(startOffset, endOffset);
    }

    public static String getWordAtOffsets(String charSequence, int start, int end, int wordType, boolean isCamel, boolean stopIfNonWord) {
        return getWordRangeAtOffsets(charSequence, start, end, wordType, isCamel, stopIfNonWord).substring(charSequence);
    }

    public static CharSequence getWordAtOffsets(CharSequence charSequence, int start, int end, int wordType, boolean isCamel, boolean stopIfNonWord) {
        return getWordRangeAtOffsets(charSequence, start, end, wordType, isCamel, stopIfNonWord).subSequence(charSequence);
    }

    public static BasedSequence getWordAtOffsets(BasedSequence charSequence, int start, int end, int wordType, boolean isCamel, boolean stopIfNonWord) {
        return (BasedSequence) getWordRangeAtOffsets(charSequence, start, end, wordType, isCamel, stopIfNonWord).subSequence(charSequence);
    }

    public static String getWordAtOffset(String charSequence, int offset, int wordType, boolean isCamel, boolean stopIfNonWord) {
        return getWordAtOffsets(charSequence, offset, offset, wordType, isCamel, stopIfNonWord);
    }

    public static CharSequence getWordAtOffset(CharSequence charSequence, int offset, int wordType, boolean isCamel, boolean stopIfNonWord) {
        return getWordAtOffsets(charSequence, offset, offset, wordType, isCamel, stopIfNonWord);
    }

    public static BasedSequence getWordAtOffset(BasedSequence charSequence, int offset, int wordType, boolean isCamel, boolean stopIfNonWord) {
        return getWordAtOffsets(charSequence, offset, offset, wordType, isCamel, stopIfNonWord);
    }

    public static boolean isPasswordEditor(@Nullable Editor editor) {
        return editor != null && editor.getContentComponent() instanceof JPasswordField;
    }

    @NotNull
    public static String getFormatterOnTag(@NotNull Project project) {
        return CodeStyleSettingsManager.getSettings(project).FORMATTER_ON_TAG;
    }

    @NotNull
    public static String getFormatterOffTag(@NotNull Project project) {
        return CodeStyleSettingsManager.getSettings(project).FORMATTER_OFF_TAG;
    }

    public static boolean getFormatterTagsEnabled(@NotNull Project project) {
        return CodeStyleSettingsManager.getSettings(project).FORMATTER_TAGS_ENABLED;
    }

    public static boolean getFormatterRegExEnabled(@NotNull Project project) {
        return CodeStyleSettingsManager.getSettings(project).FORMATTER_TAGS_ACCEPT_REGEXP;
    }

    @Nullable
    public static Pattern getFormatterOnPattern(@NotNull Project project) {
        return CodeStyleSettingsManager.getSettings(project).getFormatterOnPattern();
    }

    @Nullable
    public static Pattern getFormatterOffPattern(@NotNull Project project) {
        return CodeStyleSettingsManager.getSettings(project).getFormatterOffPattern();
    }

    public static int getStartOfLineOffset(@NotNull CharSequence charSequence, int offset) {
        return BasedSequence.of(charSequence).startOfLine(offset);
    }

    public static int getEndOfLineOffset(@NotNull CharSequence charSequence, int offset) {
        return BasedSequence.of(charSequence).endOfLine(offset);
    }

    @NotNull
    public static ItemTextRange<Language> findLanguageRangeFromElement(final PsiElement elt) {
        if (!(elt instanceof PsiFile) && elt.getFirstChild() == null) { //is leaf
            final PsiElement parent = elt.getParent();
            if (parent != null) {
                return new ItemTextRange<>(parent.getLanguage(), parent.getNode().getTextRange());
            }
        }

        return new ItemTextRange<>(elt.getLanguage(), elt.getNode().getTextRange());
    }

    @NotNull
    public static ItemTextRange<Language> getLanguageRangeAtOffset(@NotNull PsiFile file, int offset) {
        final PsiElement elt = file.findElementAt(offset);
        if (elt == null) return new ItemTextRange<Language>(file.getLanguage(), 0, file.getTextLength());
        if (elt instanceof PsiWhiteSpace) {
            TextRange textRange = elt.getTextRange();
            if (!textRange.contains(offset)) {
                LOG.error("PSI corrupted: in file " + file + " (" + file.getViewProvider().getVirtualFile() + ") offset=" + offset + " returned element " + elt + " with text range " + textRange);
            }
            final int decremented = textRange.getStartOffset() - 1;
            if (decremented >= 0) {
                return getLanguageRangeAtOffset(file, decremented);
            }
        }
        return findLanguageRangeFromElement(elt);
    }

    @Nullable
    public static ItemTextRange<Commenter> getCommenterRange(final @NotNull Editor editor, final @Nullable PsiFile file, final int startOffset, final int endOffset) {
        if (file != null) {
            final FileType fileType = file.getFileType();
            if (fileType instanceof AbstractFileType) {
                final Commenter commenter = ((AbstractFileType) fileType).getCommenter();
                if (commenter != null) {
                    return new ItemTextRange<>(commenter, 0, file.getTextLength());
                }
            } else {
                BasedSequence charSequence = BasedSequence.of(editor.getDocument().getCharsSequence());
                int lineStartOffset = charSequence.startOfLine(startOffset);
                int lineEndOffset = charSequence.endOfLine(endOffset);
                lineStartOffset += charSequence.countLeading((String) BasedSequence.WHITESPACE_NO_EOL_CHARS, lineStartOffset, lineEndOffset);
                lineEndOffset -= charSequence.countTrailing(BasedSequence.WHITESPACE_NO_EOL_CHARS, lineStartOffset, lineEndOffset);
                final ItemTextRange<Language> lineStartLanguage = getLanguageRangeAtOffset(file, lineStartOffset);
                final ItemTextRange<Language> lineEndLanguage = getLanguageRangeAtOffset(file, lineEndOffset);
                Commenter commenter = CommentByBlockCommentHandler.getCommenter(file, editor, lineStartLanguage.getItem(), lineEndLanguage.getItem());
                if (commenter != null) {
                    return new ItemTextRange<>(commenter, lineStartLanguage.getStartOffset(), lineEndLanguage.getEndOffset());
                }
            }
        }
        return null;
    }

    /**
     * Insert spaces to make sure position ends on real characters
     *
     * @param position position to which to extend real line
     * @return number of spaces inserted to convert virtual to real spaces
     */
    public static int ensureRealSpaces(@NotNull EditorPosition position) {
        int offset = position.getOffset();
        final EditorPosition atOffset = position.atOffset(offset);
        if (atOffset.column != position.column) {
            // virtual spaces, add real ones
            final int inserted = position.column - atOffset.column;
            position.getDocument().insertString(offset, new RepeatedCharSequence(' ', inserted));
            return inserted;
        }
        return 0;
    }

    @SuppressWarnings("WeakerAccess")
    @NotNull
    public static Transferable getMergedTransferable(@Nullable Editor editor, @NotNull List<Transferable> allContents, @NotNull int[] selectedIndices, boolean mergeCarets) {
        List<ClipboardCaretContent> caretContentList = new ArrayList<>();

        boolean mergeCharSelectionCarets = false;
        boolean mergeCharLineSelectionCarets = false;
        for (int index : selectedIndices) {
            final Transferable transferable = allContents.get(index);
            ClipboardCaretContent caretContent = ClipboardCaretContent.studyTransferable(editor, transferable);
            caretContentList.add(caretContent);
            assert caretContent != null;

            if (mergeCarets) {
                mergeCharLineSelectionCarets |= (caretContent.hasFullLines());
                mergeCharSelectionCarets |= (caretContent.hasFullLines() || caretContent.hasCharLines());
            }
        }

        final List<TextRange> ranges = new ArrayList<>();
        final StringBuilder sb = new StringBuilder();
        String sep = "\n";
        for (ClipboardCaretContent caretContent : caretContentList) {
            int iMax = caretContent.getCaretCount();
            int firstCharSelectionIndex = -1;
            final String[] texts = caretContent.getTexts();
            assert texts != null;
            for (int i = 0; i < iMax; i++) {
                if (caretContent.isFullLine(i)) {
                    if (firstCharSelectionIndex != -1) {
                        // add these accumulated charSelectionCarets as a new block of char lines or full lines
                        mergeCharSelectionCarets(sb, sep, ranges, texts, firstCharSelectionIndex, i, mergeCharLineSelectionCarets);
                        firstCharSelectionIndex = -1;
                    }

                    int startOffset = sb.length();
                    sb.append(texts[i]);
                    int endOffset = sb.length();
                    ranges.add(new TextRange(startOffset, endOffset));
                } else if (caretContent.isCharLine(i)) {
                    if (firstCharSelectionIndex != -1) {
                        // add these accumulated charSelectionCarets as a new block of char lines or full lines
                        mergeCharSelectionCarets(sb, sep, ranges, texts, firstCharSelectionIndex, i, mergeCharLineSelectionCarets);
                        firstCharSelectionIndex = -1;
                    }
                    int startOffset = sb.length();
                    sb.append(texts[i]);
                    int endOffset = sb.length();
                    sb.append(sep);

                    if (mergeCharLineSelectionCarets) ranges.add(new TextRange(startOffset, endOffset + 1));
                    else ranges.add(new TextRange(startOffset, endOffset));
                } else {
                    if (mergeCharSelectionCarets) {
                        if (firstCharSelectionIndex == -1) firstCharSelectionIndex = i;
                    } else {
                        int startOffset = sb.length();
                        sb.append(texts[i]);
                        int endOffset = sb.length();
                        sb.append(sep);
                        ranges.add(new TextRange(startOffset, endOffset));
                    }
                }
            }

            if (firstCharSelectionIndex != -1) {
                // add these accumulated charSelectionCarets as a new block of char lines or full lines
                mergeCharSelectionCarets(sb, sep, ranges, texts, firstCharSelectionIndex, iMax, mergeCharLineSelectionCarets);
                firstCharSelectionIndex = -1;
            }
        }

        // if all are char selections then we can make a combined char selection, otherwise we merge all of them into a single line caret block
        final List<TextBlockTransferableData> transferableData = new ArrayList<>();
        int[] startOffsets = new int[ranges.size()];
        int[] endOffsets = new int[ranges.size()];
        int i = 0;
        for (TextRange range : ranges) {
            startOffsets[i] = range.getStartOffset();
            endOffsets[i] = range.getEndOffset();
            i++;
        }

        transferableData.add(new CaretStateTransferableData(startOffsets, endOffsets));
        final Transferable transferable = new TextBlockTransferable(sb.toString(), transferableData, null);
        return transferable;
    }

    private static void mergeCharSelectionCarets(final StringBuilder content, String sep, final List<TextRange> ranges, final String[] texts, final int startIndex, final int endIndex, final boolean mergeCharLineSelectionCarets) {
        if (startIndex < endIndex) {
            int startOffset = content.length();
            String useSep = startIndex == 0 ? "" : sep;
            for (int i = startIndex; i < endIndex; i++) {
                content.append(useSep);
                useSep = sep;
                content.append(texts[i]);
            }

            if (mergeCharLineSelectionCarets) content.append(sep);
            int endOffset = content.length();
            ranges.add(new TextRange(startOffset, endOffset));
        }
    }

    @SuppressWarnings("WeakerAccess")
    @NotNull
    public static List<Transferable> getSplitTransferable(@Nullable Editor editor, @NotNull List<Transferable> allContents, @NotNull int[] selectedIndices) {
        List<ClipboardCaretContent> caretContentList = new ArrayList<>();

        for (int index : selectedIndices) {
            final Transferable transferable = allContents.get(index);
            ClipboardCaretContent caretContent = ClipboardCaretContent.studyTransferable(editor, transferable);
            caretContentList.add(caretContent);
            assert caretContent != null;
        }

        List<Transferable> splitTransferable = new ArrayList<>();

        for (ClipboardCaretContent caretContent : caretContentList) {
            splitTransferable(splitTransferable, caretContent);
        }
        return splitTransferable;
    }

    public static void splitTransferable(List<Transferable> splitTransferable, ClipboardCaretContent caretContent) {
        String sep = "\n";
        int iMax = caretContent.getCaretCount();
        int firstCharSelectionIndex = -1;
        final String[] texts = caretContent.getTexts();
        assert texts != null;
        for (int i = 0; i < iMax; i++) {
            if (caretContent.isFullLine(i)) {
                splitTransferable.add(getTransferable(texts[i], 0));
            } else if (caretContent.isCharLine(i)) {
                splitTransferable.add(getTransferable(texts[i] + sep, -1));
            } else {
                splitTransferable.add(getTransferable(texts[i] + sep, -1));
            }
        }
    }

    private static Transferable getTransferable(String text, int lengthOffset) {
        final List<TextBlockTransferableData> transferableData = new ArrayList<>();
        int[] startOffsets = new int[1];
        int[] endOffsets = new int[1];
        startOffsets[0] = 0;
        endOffsets[0] = text.length() + lengthOffset;

        transferableData.add(new CaretStateTransferableData(startOffsets, endOffsets));
        final Transferable transferable = new TextBlockTransferable(text, transferableData, null);
        return transferable;
    }
}
