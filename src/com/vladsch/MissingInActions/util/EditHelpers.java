// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.util;

import com.intellij.codeInsight.editorActions.TextBlockTransferable;
import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import com.intellij.codeInsight.generation.CommentByBlockCommentHandler;
import com.intellij.lang.Commenter;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actions.EditorActionUtil;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.impl.AbstractFileType;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiWhiteSpace;
import com.vladsch.MissingInActions.actions.DeleteAfterPasteTransferableData;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.MissingInActions.manager.EditorPosition;
import com.vladsch.MissingInActions.manager.EditorPositionFactory;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.flexmark.util.misc.CharPredicate;
import com.vladsch.flexmark.util.misc.Utils;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.Range;
import com.vladsch.flexmark.util.sequence.RepeatedSequence;
import com.vladsch.plugin.util.StudiedWord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPasswordField;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.openapi.diagnostic.Logger.getInstance;
import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isLetterOrDigit;
import static java.lang.Character.isUpperCase;
import static java.lang.Character.isWhitespace;

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

    public static int getNextWordStartOrEndOffset(final @NotNull Editor editor, final int offset, final boolean camel, final int flags, final boolean haveMultiCarets) {
        if (!isSet(flags, BOUNDARY_FLAGS)) return offset;

        Document document = editor.getDocument();

        boolean stopAtTrailingBlanks = isSet(flags, START_OF_TRAILING_BLANKS);
//        boolean stopAtLeadingBlanks = isSet(flags, END_OF_LEADING_BLANKS);
        boolean stopAtStartOfLine = isSet(flags, START_OF_LINE);
        boolean stopAtStartOfWord = isSet(flags, START_OF_WORD);
        boolean stopAtEndOfWord = isSet(flags, END_OF_WORD);
        boolean stopAtStartOfFolding = isSet(flags, START_OF_FOLDING_REGION);
        boolean stopAtEndOfFolding = isSet(flags, END_OF_FOLDING_REGION);
        boolean stopAtEndOfLine = isSet(flags, END_OF_LINE);
//        boolean strictIdentifier = isSet(flags, MIA_IDENTIFIER);
        boolean singleLine = isSet(flags, SINGLE_LINE) || isSet(flags, MULTI_CARET_SINGLE_LINE) && haveMultiCarets;

        if (offset == document.getTextLength()) return offset;

        int lineNumber = editor.offsetToLogicalPosition(offset).line;
        if (lineNumber >= document.getLineCount()) return offset;

        int stopAtLastNonBlank = 0;

        // have to stop at start of character if caret is not at or before first non-blank
        // only applies to start boundary condition
        int lineStartOffset = document.getLineStartOffset(lineNumber);
        if (stopAtTrailingBlanks || stopAtEndOfLine) {
            int lineEndOffset = document.getLineEndOffset(lineNumber);
            int trailingBlanks = countWhiteSpaceReversed(document.getCharsSequence(), lineStartOffset, lineEndOffset);
            if (stopAtTrailingBlanks && offset < lineEndOffset - trailingBlanks) {
                stopAtLastNonBlank = lineEndOffset - trailingBlanks;
            } else if (stopAtEndOfLine && (offset < lineEndOffset || singleLine)) {
                stopAtLastNonBlank = lineEndOffset;
            }
        }

        int maxLineNumber = stopAtLastNonBlank > 0 || lineNumber + 1 > document.getLineCount() ? lineNumber : lineNumber + 1;
        int maxOffset = stopAtLastNonBlank > 0 ? stopAtLastNonBlank :
                (stopAtStartOfLine && lineNumber < maxLineNumber ? document.getLineStartOffset(maxLineNumber) : stopAtEndOfLine ? document.getLineEndOffset(maxLineNumber) : document.getTextLength());

        int newOffset = offset + 1;
        if (newOffset > maxOffset) return offset;

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

        return newOffset;
    }

    public static void moveCaretToNextWordStartOrEnd(@NotNull Editor editor, boolean isWithSelection, boolean camel, int flags) {
        CaretModel caretModel = editor.getCaretModel();
        int offset = caretModel.getOffset();

        int newOffset = getNextWordStartOrEndOffset(editor, caretModel.getOffset(), camel, flags, caretModel.getCaretCount() > 1);

        if (newOffset != offset) {
            SelectionModel selectionModel = editor.getSelectionModel();
            int selectionStart = selectionModel.getLeadSelectionOffset();

            if (editor instanceof EditorImpl) {
                int boundaryOffset = ((EditorImpl) editor).findNearestDirectionBoundary(offset, true);
                if (boundaryOffset >= 0) {
                    newOffset = Math.min(boundaryOffset, newOffset);
                }
            }

            caretModel.moveToOffset(newOffset);
            EditorModificationUtil.scrollToCaret(editor);
            LogicalPosition logicalPosition = caretModel.getLogicalPosition();
            setupSelection(editor, isWithSelection, selectionStart, logicalPosition);
        }
    }

    public static int getPreviousWordStartOrEndOffset(final @NotNull Editor editor, final int offset, final boolean camel, final int flags, final boolean haveMultiCarets) {
        if (!isSet(flags, BOUNDARY_FLAGS)) return offset;

        if (offset == 0) return offset;

        Document document = editor.getDocument();

        boolean stopAtTrailingBlanks = isSet(flags, START_OF_TRAILING_BLANKS);
        boolean stopAtLeadingBlanks = isSet(flags, END_OF_LEADING_BLANKS);
        boolean stopAtStartOfLine = isSet(flags, START_OF_LINE);
        boolean stopAtStartOfWord = isSet(flags, START_OF_WORD);
        boolean stopAtEndOfWord = isSet(flags, END_OF_WORD);
        boolean stopAtEndOfLine = isSet(flags, END_OF_LINE);
        boolean stopAtStartOfFolding = isSet(flags, START_OF_FOLDING_REGION);
        boolean stopAtEndOfFolding = isSet(flags, END_OF_FOLDING_REGION);
//        boolean strictIdentifier = isSet(flags, MIA_IDENTIFIER);
        boolean singleLine = isSet(flags, SINGLE_LINE) || isSet(flags, MULTI_CARET_SINGLE_LINE) && haveMultiCarets;

        LogicalPosition logicalPosition = editor.offsetToLogicalPosition(offset);
        int lineNumber = logicalPosition.line;
        int stopAtIndent = 0;

        // have to stop at start of character if caret is not at or before first non-blank
        int lineStartOffset = document.getLineStartOffset(lineNumber);
        int lineEndOffset = document.getLineEndOffset(lineNumber);

        if (stopAtEndOfLine && logicalPosition.column > editor.offsetToLogicalPosition(lineEndOffset).column) {
            stopAtIndent = lineEndOffset;
        }

        if (stopAtIndent == 0 && stopAtTrailingBlanks) {
            int trailingBlanks = countWhiteSpaceReversed(document.getCharsSequence(), lineStartOffset, lineEndOffset);
            if (offset > lineEndOffset - trailingBlanks) {
                stopAtIndent = lineEndOffset - trailingBlanks;
            }
        }

        if (stopAtIndent == 0 && (stopAtLeadingBlanks || stopAtStartOfLine)) {
            int firstNonBlank = countWhiteSpace(document.getCharsSequence(), lineStartOffset, document.getLineEndOffset(lineNumber));
            LogicalPosition firstNonBlankPosition = editor.offsetToLogicalPosition(lineStartOffset + firstNonBlank);
            if (stopAtLeadingBlanks && logicalPosition.column > firstNonBlankPosition.column) {
                stopAtIndent = lineStartOffset + firstNonBlank;
            } else if (stopAtStartOfLine && (logicalPosition.column != 0 || singleLine)) {
                stopAtIndent = lineStartOffset;
            }
        }

        int minLineNumber = lineNumber == 0 || stopAtIndent > 0 ? lineNumber : lineNumber - 1;
        int minOffset = stopAtIndent > 0 ? stopAtIndent :
                (stopAtEndOfLine && lineNumber > minLineNumber ? document.getLineEndOffset(minLineNumber) : stopAtStartOfLine ? document.getLineStartOffset(minLineNumber) : 0);

        // if virtual spaces are enabled the caret can be after the end so we should pretend it is on the next char after the end
        int newOffset = Math.max(stopAtIndent, offset - 1);
        if (newOffset < minOffset) return offset;

        boolean done = false;
        FoldRegion currentFoldRegion = editor.getFoldingModel().getCollapsedRegionAtOffset(offset - 1);
        if (currentFoldRegion != null) {
            newOffset = currentFoldRegion.getStartOffset();
            if (stopAtStartOfFolding) done = true;
        }

        int wordType = getWordType(flags);
        while (!done) {
            for (; newOffset > minOffset; newOffset--) {
                if (stopAtEndOfWord && isWordTypeEnd(wordType, editor, newOffset, camel)) {
                    done = true;
                    break;
                }
                if (stopAtStartOfWord && isWordTypeStart(wordType, editor, newOffset, camel)) {
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

        return newOffset;
    }

    public static void moveCaretToPreviousWordStartOrEnd(@NotNull Editor editor, boolean isWithSelection, boolean camel, int flags) {
        CaretModel caretModel = editor.getCaretModel();
        int offset = caretModel.getOffset();

        int newOffset = getPreviousWordStartOrEndOffset(editor, caretModel.getOffset(), camel, flags, caretModel.getCaretCount() > 1);

        if (newOffset != offset) {
            SelectionModel selectionModel = editor.getSelectionModel();
            int selectionStart = selectionModel.getLeadSelectionOffset();

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
            LogicalPosition logicalPosition = caretModel.getLogicalPosition();
            setupSelection(editor, isWithSelection, selectionStart, logicalPosition);
        }
    }

    public static boolean isWordTypeStart(int wordType, @NotNull Editor editor, int offset, boolean isCamel) {
        switch (wordType) {
            case WORD_SPACE_DELIMITED:
                return isWhitespaceEnd(editor.getDocument().getCharsSequence(), offset);
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
                return isWhitespaceEnd(charSequence, offset);
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

    public static boolean isWhitespaceEnd(@NotNull CharSequence text, int offset) {
        char prev = offset > 0 ? text.charAt(offset - 1) : 0;
        char current = offset < text.length() ? text.charAt(offset) : 0;

        return (Character.isWhitespace(prev) && !Character.isWhitespace(current));
    }

    public static boolean isIdentifierPart(char c) {
        // add $ since PHP and JavaScript take these as part of identifier
        return c == '$' || isJavaIdentifierPart(c);
    }

    public static boolean isIdentifier(@NotNull CharSequence text, int offset) {
        return offset >= 0 && offset < text.length() && isIdentifierPart(text.charAt(offset));
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

        final char prevChar = editorText.charAt(offset - 1);
        final char curChar = editorText.charAt(offset);
        final char nextChar = offset + 1 < editorText.length() ? editorText.charAt(offset + 1) : 0; // 0x00 is not lowercase.

        return isLowerCaseOrDigit(prevChar) && isUpperCase(curChar) ||
                start && prevChar == '_' && curChar != '_' ||
                !start && prevChar != '_' && curChar == '_' ||
                start && prevChar == '$' && isLetterOrDigit(curChar) ||
                !start && isLetterOrDigit(prevChar) && curChar == '$' ||
                isUpperCase(prevChar) && isUpperCase(curChar) && Character.isLowerCase(nextChar);
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
            editor.getDocument().replaceString(start, end, RepeatedSequence.ofSpaces(end - start));
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

    public static void scrollToSelection(Editor editor) {
        ScrollingModel scrollingModel = editor.getScrollingModel();
        SelectionModel selectionModel = editor.getSelectionModel();
        scrollingModel.scrollTo(editor.offsetToLogicalPosition(selectionModel.getSelectionEnd()), ScrollType.MAKE_VISIBLE);
        scrollingModel.scrollTo(editor.offsetToLogicalPosition(selectionModel.getSelectionStart()), ScrollType.MAKE_VISIBLE);
    }

    public static void restoreState(@Nullable Caret newCaret, CaretState caretState, boolean alwaysSetSelection) {
        if (newCaret != null) {
            if (caretState.getCaretPosition() != null) {
                newCaret.moveToLogicalPosition(caretState.getCaretPosition());
            }

            if (caretState.getSelectionStart() != null && caretState.getSelectionEnd() != null) {
                newCaret.setSelection(
                        newCaret.getEditor().logicalPositionToOffset(caretState.getSelectionStart()),
                        newCaret.getEditor().logicalPositionToOffset(caretState.getSelectionEnd())
                );
            } else if (alwaysSetSelection) {
                newCaret.setSelection(newCaret.getOffset(), newCaret.getOffset());
            }
        }
    }

    @Nullable
    public static Range getCaretRange(@NotNull Caret caret, boolean backwards, boolean lineMode, boolean singleLine) {
        @NotNull Editor editor = caret.getEditor();
        Document document = editor.getDocument();
        int caretOffset = caret.getOffset();
        Range range = null;

        int start;
        int end;

        if (caret.hasSelection()) {
            if (backwards) {
                end = caret.getOffset();
                start = end;
            } else {
                start = caret.getOffset();
                end = start;
            }

            // expand range to start/end of selection
            if (start > caret.getSelectionStart()) {
                start = caret.getSelectionStart();
            }

            if (end < caret.getSelectionEnd()) {
                end = caret.getSelectionEnd();
            }
        } else {
            if (backwards) {
                start = 0;
                end = caret.getOffset();
            } else {
                start = caret.getOffset();
                end = document.getTextLength();
            }
        }

        if (lineMode && (!singleLine || !caret.hasSelection())) {
            // expand range to start/end of line but not if caret has selection and is singleLine search spawning
            int startOffset = document.getLineStartOffset(document.getLineNumber(start));
            int endOffset = document.getLineEndOffset(document.getLineNumber(end));

            if (start > startOffset) {
                start = startOffset;
            }
            if (end < endOffset) {
                end = endOffset;
            }
        }

        if (singleLine) {
            // truncate to the caret line
            int lineNumber = document.getLineNumber(caretOffset);
            int startOffset = document.getLineStartOffset(lineNumber);
            int endOffset = document.getLineEndOffset(lineNumber);

            if (start < startOffset) {
                start = startOffset;
            }
            if (end > endOffset) {
                end = endOffset;
            }
        }

        if (start > end) {
            int tmp = 0;
        } else {
            range = Range.of(start, end);
        }
        return range;
    }

    public static Map<Caret, Range> limitCaretRange(boolean backwards, Map<Caret, Range> rangeMap, boolean wantEmptyRanges) {
        HashMap<Caret, Range> map = new HashMap<>(rangeMap.size());
        Caret[] carets = rangeMap.keySet().toArray(new Caret[0]);
        Range prevRange = null;

        Arrays.sort(carets, Comparator.comparing(Caret::getLogicalPosition));

        if (!backwards) {
            for (int i = carets.length; i-- > 0; ) {
                Caret caret = carets[i];
                Range range = rangeMap.get(caret);
                if (range == null) continue;

                if (prevRange != null) {
                    if (prevRange.getStart() < range.getEnd()) {
                        // truncate end
                        range = range.withEnd(prevRange.getStart());
                    }
                }

                if (range.getSpan() > 0 || wantEmptyRanges && range.getSpan() == 0) {
                    map.put(caret, range);
                    prevRange = range;
                }
            }
        } else {
            for (Caret caret : carets) {
                Range range = rangeMap.get(caret);
                if (range == null) continue;

                if (prevRange != null) {
                    if (prevRange.getEnd() > range.getStart()) {
                        // truncate start
                        range = range.withStart(prevRange.getEnd());
                    }
                }

                if (range.getSpan() > 0 || wantEmptyRanges && range.getSpan() == 0) {
                    map.put(caret, range);
                    prevRange = range;
                }
            }
        }

        return map;
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
        if (wordType != WORD_SPACE_DELIMITED && !isIdentifier(charSequence, offset) && !isWordEnd(
                charSequence,
                offset,
                false
        ) || wordType == WORD_SPACE_DELIMITED && isWhitespaceMiddle(charSequence, offset)) {
            // go forward
            return offset;//getNextWordStartAtOffset(charSequence, offset, wordType, isCamel);
        } else {
            // go backwards
            return getPreviousWordStartAtOffset(charSequence, offset, wordType, isCamel, stopIfNonWord);
        }
    }

    public static int getWordEndAtOffset(CharSequence charSequence, int offset, int wordType, boolean isCamel, boolean stopIfNonWord) {
        if (wordType != WORD_SPACE_DELIMITED && !isIdentifier(charSequence, offset) && !isWordStart(
                charSequence,
                offset,
                false
        ) || wordType == WORD_SPACE_DELIMITED && isWhitespaceMiddle(charSequence, offset)) {
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
        if (elt == null) return new ItemTextRange<>(file.getLanguage(), 0, file.getTextLength());
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
                lineStartOffset += charSequence.countLeading(CharPredicate.SPACE_TAB, lineStartOffset, lineEndOffset);
                lineEndOffset -= charSequence.countTrailing(CharPredicate.SPACE_TAB, lineStartOffset, lineEndOffset);
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
     *
     * @return number of spaces inserted to convert virtual to real spaces
     */
    public static int ensureRealSpaces(@NotNull EditorPosition position) {
        int offset = position.getOffset();
        int textLength = position.getDocument().getTextLength();

        if (offset == textLength) {
            // add a new line at end of document
            position.getDocument().insertString(textLength, "\n");
            offset = position.getOffset();
        }

        final EditorPosition atOffset = position.atOffset(offset);
        if (atOffset.column < position.column) {
            // virtual spaces, add real ones
            final int inserted = position.column - atOffset.column;
            position.getDocument().insertString(offset, RepeatedSequence.ofSpaces(inserted));
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

    private static void mergeCharSelectionCarets(
            final StringBuilder content,
            String sep,
            final List<TextRange> ranges,
            final String[] texts,
            final int startIndex,
            final int endIndex,
            final boolean mergeCharLineSelectionCarets
    ) {
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

    private static String splitCaretText(String text, StringBuilder sb, String sep, List<Integer> starts, List<Integer> ends) {
        int lastPos = 0;
        int length = text.length();
        while (lastPos < length) {
            int pos = text.indexOf('\n', lastPos);
            int endPos = pos == -1 ? length : pos;
            sb.append(sep);
            sep = "\n";
            starts.add(sb.length());
            sb.append(text.substring(lastPos, endPos));
            ends.add(sb.length());
            lastPos = endPos + 1;
        }
        return sep;
    }

    public static Transferable splitCaretTransferable(String... texts) {
        StringBuilder sb = new StringBuilder();
        ArrayList<Integer> starts = new ArrayList<>();
        ArrayList<Integer> ends = new ArrayList<>();
        String sep = "";
        for (String text : texts) {
            sep = splitCaretText(text, sb, sep, starts, ends);
        }

        final List<TextBlockTransferableData> transferableData = new ArrayList<>();

        int i = starts.size();
        int[] startOffsets = new int[i];
        while (i-- > 0) startOffsets[i] = starts.get(i);

        i = ends.size();
        int[] endOffsets = new int[i];
        while (i-- > 0) endOffsets[i] = ends.get(i);

        transferableData.add(new CaretStateTransferableData(startOffsets, endOffsets));
        final Transferable transferable = new TextBlockTransferable(sb.toString(), transferableData, null);
        return transferable;
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

    public static Transferable getTransferable(String text) {
        return getTransferable(text, 0);
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

    public static Couple<Integer> duplicateLineOrSelectedBlockAtCaret(Editor editor, final Document document, @NotNull Caret caret, final boolean moveCaret) {
        if (caret.hasSelection()) {
            int start = caret.getSelectionStart();
            int end = caret.getSelectionEnd();
            String s = document.getCharsSequence().subSequence(start, end).toString();
            document.insertString(end, s);
            if (moveCaret) {
                // select newly copied lines and move there
                caret.moveToOffset(end + s.length());
                editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                caret.removeSelection();
                caret.setSelection(end, end + s.length());
            }
            return Couple.of(end, end + s.length());
        } else {
            return duplicateLinesRange(editor, document, caret, caret.getOffset(), caret.getVisualPosition(), caret.getVisualPosition(), moveCaret);
        }
    }

    @SuppressWarnings("WeakerAccess")
    @Nullable
    public static Couple<Integer> duplicateLinesRange(
            Editor editor,
            Document document,
            @Nullable Caret caret,
            int offset,
            VisualPosition rangeStart,
            VisualPosition rangeEnd,
            boolean moveCaret
    ) {
        Pair<LogicalPosition, LogicalPosition> lines = EditorUtil.calcSurroundingRange(editor, rangeStart, rangeEnd);

        LogicalPosition lineStart = lines.first;
        LogicalPosition nextLineStart = lines.second;
        int start = editor.logicalPositionToOffset(lineStart);
        int end = editor.logicalPositionToOffset(nextLineStart);
        if (end <= start) {
            return null;
        }
        String s = document.getCharsSequence().subSequence(start, end).toString();
        final int lineToCheck = nextLineStart.line - 1;

        int newOffset = end + offset - start;
        if (lineToCheck == document.getLineCount() /* empty document */
                || lineStart.line == document.getLineCount() - 1 /* last line*/
                || document.getLineSeparatorLength(lineToCheck) == 0) {
            s = "\n" + s;
            newOffset++;
        }
        document.insertString(end, s);

        if (moveCaret && caret != null) {
            caret.moveToOffset(newOffset);

            editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        }
        return Couple.of(end, end + s.length());
    }

    @NotNull
    public static Transferable getSplitRepeatedTransferable(@NotNull Editor editor, Transferable content, int repeatCount) {
        ClipboardCaretContent clipboardCaretContent = ClipboardCaretContent.studyTransferable(editor, content);
        if (clipboardCaretContent != null) {
            return getSplitRepeatedTransferable(editor, clipboardCaretContent, repeatCount);
        }
        return content;
    }

    @NotNull
    public static Transferable getSplitRepeatedTransferable(
            final @NotNull Editor editor,
            ClipboardCaretContent clipboardCaretContent,
            int repeatCount
    ) {
        Transferable mergedTransferable;
        String sep = "\n";
        int iMax = clipboardCaretContent.getCaretCount();
        final String[] texts = clipboardCaretContent.getTexts();
        assert texts != null;
        final LineSelectionManager manager = LineSelectionManager.getInstance(editor);

        StringBuilder sb = new StringBuilder();
        List<TextRange> ranges = new ArrayList<>();

        boolean hadChange = false;
        String nextSep = "";
        for (int i = 0; i < iMax; i++) {
            for (int j = 0; j < repeatCount; j++) {
                String replaceOnPaste = manager.replaceOnPaste(texts[i]);
                if (!replaceOnPaste.equals(texts[i])) hadChange = true;
                if (clipboardCaretContent.isFullLine(i)) {
                    sb.append(nextSep);
                    nextSep = "";
                    int startOffset = sb.length();
                    sb.append(replaceOnPaste);
                    int endOffset = sb.length();
                    ranges.add(new TextRange(startOffset, endOffset));
                } else if (clipboardCaretContent.isCharLine(i)) {
                    sb.append(nextSep);
                    int startOffset = sb.length();
                    sb.append(replaceOnPaste);
                    int endOffset = sb.length();
                    nextSep = sep;
                    ranges.add(new TextRange(startOffset, endOffset));
                } else {
                    sb.append(nextSep);
                    nextSep = "";
                    int startOffset = sb.length();
                    sb.append(replaceOnPaste);
                    int endOffset = sb.length();
                    if (iMax > 1 || repeatCount > 1) sb.append(sep);
                    ranges.add(new TextRange(startOffset, endOffset));
                }
            }
        }

        if (hadChange || iMax > 1 && repeatCount > 1) {
            // have an actual change in content
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
            if (iMax > 1 && repeatCount > 1) {
                // only make it auto-deletable if really permuted??
                transferableData.add(new DeleteAfterPasteTransferableData(startOffsets, endOffsets));
            }
            mergedTransferable = new TextBlockTransferable(sb.toString(), transferableData, null);
            //        ClipboardCaretContent mergedClipboard = ClipboardCaretContent.studyTransferable(editor, mergedTransferable);
            return mergedTransferable;
        } else {
            return clipboardCaretContent.getContent();
        }
    }

    @NotNull
    public static String getCorrespondingQuoteLike(final String openingText) {
        StringBuilder sb = new StringBuilder(openingText.length());
        int iMax = openingText.length();
        for (int i = iMax; i-- > 0; ) {
            sb.append(getCorrespondingQuoteLike(openingText.charAt(i)));
        }
        return sb.toString();
    }

    public static final String QUOTES = "'\"`«»";
    public static final String QUOTE_LIKE = "''\"\"``()[]{}||//\\\\<>«»";

    public static char getCorrespondingQuoteLike(final char openingChar) {
        int pos = QUOTE_LIKE.indexOf(openingChar);
        if (pos == -1) return openingChar;
        if (pos % 2 == 0) {
            // opening quote
            return QUOTE_LIKE.charAt(pos + 1);
        } else {
            // closing quote
            return QUOTE_LIKE.charAt(pos - 1);
        }
    }

    public static boolean isQuote(char c) {
        return QUOTES.indexOf(c) != -1;
    }

    public static boolean startsWithQuote(String text) {
        return !text.isEmpty() && isQuote(text.charAt(0));
    }

    public static boolean endsWithQuote(String text) {
        return !text.isEmpty() && isQuote(text.charAt(text.length() - 1));
    }

    @NotNull
    public static Transferable getJoinedTransferable(ClipboardCaretContent clipboardCaretContent, final String delimiter, boolean quoteSplicedItems, final String openQuote, final String closeQuote) {
        Transferable mergedTransferable;
        String sep = "";
        int iMax1 = clipboardCaretContent.getCaretCount();
        final String[] texts = clipboardCaretContent.getTexts();
        assert texts != null;

        StringBuilder sb = new StringBuilder();

        if (openQuote.isEmpty() && closeQuote.isEmpty()) {
            quoteSplicedItems = false;
        }

        // here we build a single text based on spliced lines
        for (int i = 0; i < iMax1; i++) {
            // update manager's replacement
            String[] lines = texts[i].split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    sb.append(sep);
                    if (quoteSplicedItems && !((openQuote.isEmpty() || trimmed.startsWith(openQuote)) && (closeQuote.isEmpty() || trimmed.endsWith(closeQuote)))) {
                        int startOffset = !openQuote.isEmpty() && trimmed.startsWith(openQuote) ? 1 : 0;
                        int endOffset = !closeQuote.isEmpty() && trimmed.endsWith(closeQuote) ? -1 : 0;

                        if (startOffset == 0) sb.append(openQuote);
                        if (openQuote.equals("\"") && closeQuote.equals("\"")) {
                            sb.append(trimmed.replace("\"", "\\\""));
                        } else if (openQuote.equals("'") && closeQuote.equals("'")) {
                            sb.append(trimmed.replace("'", "\\'"));
                        } else {
                            sb.append(trimmed);
                        }
                        if (endOffset == 0) sb.append(closeQuote);
                    } else {
                        sb.append(trimmed);
                    }
                    sep = delimiter;
                }
            }
        }

        final List<TextBlockTransferableData> transferableData = new ArrayList<>();
        mergedTransferable = new TextBlockTransferable(sb.toString(), transferableData, null);
        return mergedTransferable;
    }

    @NotNull
    public static Transferable getReplacedTransferable(
            final @NotNull Editor editor,
            ClipboardCaretContent clipboardCaretContent,
            String[] userData
    ) {
        return getReplacedTransferable(editor, clipboardCaretContent, userData, null);
    }

    @NotNull
    public static Transferable getReplacedTransferable(
            final @NotNull Editor editor,
            ClipboardCaretContent clipboardCaretContent,
            String[] userData,
            @Nullable TextOffsetConsumer offsetConsumer
    ) {
        Transferable mergedTransferable;
        String sep = "\n";
        int iMax = clipboardCaretContent.getCaretCount();
        final String[] texts = clipboardCaretContent.getTexts();
        assert texts != null;
        final LineSelectionManager manager = LineSelectionManager.getInstance(editor);

        StringBuilder sb = new StringBuilder();
        List<TextRange> ranges = new ArrayList<>();
        int[] textIndex = { 0 };
        int[] rangeOffset = { 0 };
        String[] text = { "" };
        @Nullable TextOffsetConsumer offsetRangeConsumer = offsetConsumer == null ? null :
                (dummyIndex, dummyText, dummyOffset, rangeIndex, foundRange, replacedRange, foundText) -> offsetConsumer.accept(textIndex[0], text[0], rangeOffset[0], rangeIndex, foundRange, replacedRange, foundText);

        // here we build a single text based on permutations
        String nextSep = "";
        int jMax = userData == null ? 1 : userData.length;
        for (int i = 0; i < iMax; i++) {
            for (int j = 0; j < jMax; j++) {
                // update manager's replacement
                if (userData != null) manager.setOnPasteUserReplacementText(userData[j]);

                text[0] = texts[i];

                if (clipboardCaretContent.isFullLine(i)) {
                    sb.append(nextSep);
                    nextSep = "";
                    int startOffset = sb.length();
                    sb.append(manager.replaceOnPaste(text[0], offsetRangeConsumer));
                    int endOffset = sb.length();
                    ranges.add(new TextRange(startOffset, endOffset));
                    rangeOffset[0] += endOffset - startOffset;
                } else if (clipboardCaretContent.isCharLine(i)) {
                    sb.append(nextSep);
                    int startOffset = sb.length();
                    sb.append(manager.replaceOnPaste(text[0], offsetRangeConsumer));
                    int endOffset = sb.length();
                    nextSep = sep;
                    ranges.add(new TextRange(startOffset, endOffset));
                    rangeOffset[0] += endOffset - startOffset;
                } else {
                    sb.append(nextSep);
                    nextSep = "";
                    int startOffset = sb.length();
                    sb.append(manager.replaceOnPaste(text[0], offsetRangeConsumer));
                    int endOffset = sb.length();
                    ranges.add(new TextRange(startOffset, endOffset));
                    rangeOffset[0] += endOffset - startOffset;
                }

                textIndex[0]++;
            }
        }

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
        transferableData.add(new DeleteAfterPasteTransferableData(startOffsets, endOffsets));
        mergedTransferable = new TextBlockTransferable(sb.toString(), transferableData, null);
        return mergedTransferable;
    }

    @NotNull
    public static Transferable getReplacedTransferable(
            final @NotNull Editor editor,
            ClipboardCaretContent clipboardCaretContent
    ) {
        return getSplitRepeatedTransferable(editor, clipboardCaretContent, 1);
    }

    @Nullable
    public static HashMap<String, String> getOnPasteReplacements(final @NotNull Editor editor) {
        final ApplicationSettings settings = ApplicationSettings.getInstance();
        if (settings.isReplaceMacroVariables() && editor instanceof EditorEx && ((EditorEx) editor).getVirtualFile() != null) {
            final EditorEx editorEx = (EditorEx) editor;
            final Project project = editorEx.getProject();

            HashMap<String, String> map = new HashMap<>();
            StudiedWord word;

            word = new StudiedWord(editorEx.getVirtualFile().getNameWithoutExtension(), StudiedWord.DOT | StudiedWord.DASH | StudiedWord.UNDER | StudiedWord.SLASH | StudiedWord.SPACE);
            String pascalCase = word.makePascalCase();
            map.put("__Filename__", word.getWord().toString());
            map.put("__FILENAME__", pascalCase.toUpperCase());
            map.put("__filename__", pascalCase.toLowerCase());
            map.put("__FileName__", pascalCase);
            map.put("__fileName__", word.makeProperCamelCase());
            map.put("__file-name__", word.makeDashCase());
            map.put("__FILE-NAME__", word.makeScreamingDashCase());
            map.put("__file.name__", word.makeDotCase());
            map.put("__FILE.NAME__", word.makeScreamingDotCase());
            map.put("__file_name__", word.makeSnakeCase());
            map.put("__FILE_NAME__", word.makeScreamingSnakeCase());
            map.put("__file/name__", word.makeSlashCase());
            map.put("__FILE/NAME__", word.makeScreamingSlashCase());

            String path = editorEx.getVirtualFile().getParent().getPath();
            map.put("__Filepath__", path);
            map.put("__FilePath__", path);
            map.put("__filePath__", path);
            map.put("__FILEPATH__", path.toUpperCase());
            map.put("__filepath__", path.toLowerCase());

            path = Utils.removePrefix(editorEx.getVirtualFile().getParent().getPath(), "/");
            map.put("__File-path__", path.replace('/', '-'));
            map.put("__File-Path__", path.replace('/', '-'));
            map.put("__file-Path__", path.replace('/', '-'));
            map.put("__FILE-PATH__", path.toUpperCase().replace('/', '.'));
            map.put("__file-path__", path.toLowerCase().replace('/', '-'));

            map.put("__File.path__", path.replace('/', '.'));
            map.put("__File.Path__", path.replace('/', '.'));
            map.put("__file.Path__", path.replace('/', '.'));
            map.put("__FILE.PATH__", path.toUpperCase().replace('/', '.'));
            map.put("__file.path__", path.toLowerCase().replace('/', '.'));

            return map;
        }
        return null;
    }

    @NotNull
    public static StudiedCarets studyCarets(@NotNull Editor editor, @NotNull List<Caret> allCarets) {
        StudiedCarets studiedCarets = new StudiedCarets();
        if (!allCarets.isEmpty()) {
            @NotNull LineSelectionManager manager = LineSelectionManager.getInstance(editor);
            final Project project = editor.getProject();
            final PsiFile psiFile = project == null || !(editor instanceof EditorEx) ? null : PsiManager.getInstance(project).findFile(((EditorEx) editor).getVirtualFile());
            final LineCommentProcessor lineCommentProcessor = psiFile == null ? null : new LineCommentProcessor(editor, psiFile);
            HashMap<Integer, EachLineCarets> lineStats = new HashMap<>();
            studiedCarets.lineCommentProcessor = lineCommentProcessor;

            for (Caret caret : allCarets) {
                EditorCaret editorCaret = manager.getEditorCaret(caret);
                studiedCarets.carets.add(editorCaret);
                studiedCarets.range = studiedCarets.range.include(editorCaret.getCaretPosition().line);

                EachLineCarets eachLineCarets = lineStats.computeIfAbsent(editorCaret.getCaretPosition().line, (integer) -> new EachLineCarets());
                eachLineCarets.carets++;
                if (editorCaret.hasSelection()) eachLineCarets.caretSelections++;
            }

            EditorPosition position = studiedCarets.carets.get(0).getCaretPosition();
            ByLineType eachLineCarets = studiedCarets.eachLineCarets;

            for (int line : lineStats.keySet()) {
                EachLineCarets lineCarets = lineStats.get(line);
                position = position.onLine(line);

                studiedCarets.lineCount.total++;
                studiedCarets.caretCount.total += lineCarets.carets;
                studiedCarets.caretSelections.total += lineCarets.caretSelections;
                if (eachLineCarets.total == 0) eachLineCarets.total = lineCarets.carets;
                else if (eachLineCarets.total > 0 && eachLineCarets.total != lineCarets.carets) eachLineCarets.total = -1;

                if (position.isBlankLine()) {
                    studiedCarets.lineCount.blank++;
                    studiedCarets.caretCount.blank += lineCarets.carets;
                    studiedCarets.caretSelections.blank += lineCarets.caretSelections;
                    if (eachLineCarets.blank == 0) eachLineCarets.blank = lineCarets.carets;
                    else if (eachLineCarets.blank > 0 && eachLineCarets.blank != lineCarets.carets) eachLineCarets.blank = -1;
                } else if (lineCommentProcessor != null && lineCommentProcessor.isLineCommented(position.getOffset())) {
                    studiedCarets.lineCount.comment++;
                    studiedCarets.caretCount.comment += lineCarets.carets;
                    studiedCarets.caretSelections.comment += lineCarets.caretSelections;
                    if (eachLineCarets.comment == 0) eachLineCarets.comment = lineCarets.carets;
                    else if (eachLineCarets.comment > 0 && eachLineCarets.comment != lineCarets.carets) eachLineCarets.comment = -1;
                } else {
                    studiedCarets.lineCount.code++;
                    studiedCarets.caretCount.code += lineCarets.carets;
                    studiedCarets.caretSelections.code += lineCarets.caretSelections;
                    if (eachLineCarets.code == 0) eachLineCarets.code = lineCarets.carets;
                    else if (eachLineCarets.code > 0 && eachLineCarets.code != lineCarets.carets) eachLineCarets.code = -1;
                }
            }
        }

        EditorCaret firstCaret = null;
        EditorCaret lastCaret = null;
        for (EditorCaret editorCaret : studiedCarets.carets) {
            if (editorCaret.getCaretPosition().line == studiedCarets.range.getStart()) {
                if (firstCaret == null || firstCaret.getCaretPosition().column > editorCaret.getCaretPosition().column) {
                    firstCaret = editorCaret;
                }
            }
            if (editorCaret.getCaretPosition().line == studiedCarets.range.getEnd()) {
                if (lastCaret == null || lastCaret.getCaretPosition().column > editorCaret.getCaretPosition().column) {
                    lastCaret = editorCaret;
                }
            }
        }

        if (firstCaret == null) {
            firstCaret = studiedCarets.carets.get(0).onLine(studiedCarets.range.getStart());
        }

        if (lastCaret == null) {
            lastCaret = studiedCarets.carets.get(studiedCarets.carets.size() - 1).onLine(studiedCarets.range.getEnd());
        }

        studiedCarets.firstLineCaret = firstCaret;
        studiedCarets.lastLineCaret = lastCaret;
        return studiedCarets;
    }

    public static boolean swapRangeText(final Editor editor, final Range range1, final Range range2) {
        boolean handled = false;
        if (!range1.doesContain(range2) && !range2.doesContain(range1)) {
            // can swap text
            handled = true;

            WriteCommandAction.runWriteCommandAction(editor.getProject(), () -> {
                Range effectiveRange1 = range1.exclude(range2);
                Range effectiveRange2 = range2.exclude(range1);
                Document document = editor.getDocument();
                CharSequence chars = document.getCharsSequence();
                String text1 = chars.subSequence(effectiveRange1.getStart(), effectiveRange1.getEnd()).toString();
                String text2 = chars.subSequence(effectiveRange2.getStart(), effectiveRange2.getEnd()).toString();
                int start1;
                int start2;

                start1 = effectiveRange1.getStart();
                start2 = effectiveRange2.getStart();
                if (effectiveRange1.getStart() < effectiveRange2.getStart()) {
                    // effectiveRange2 first, then effectiveRange1
                    document.replaceString(effectiveRange2.getStart(), effectiveRange2.getEnd(), text1);
                    document.replaceString(effectiveRange1.getStart(), effectiveRange1.getEnd(), text2);

                    start2 -= effectiveRange1.getSpan();
                    start2 += text2.length();
                } else {
                    // effectiveRange1 first, then effectiveRange2
                    document.replaceString(effectiveRange1.getStart(), effectiveRange1.getEnd(), text2);
                    document.replaceString(effectiveRange2.getStart(), effectiveRange2.getEnd(), text1);

                    start1 -= effectiveRange2.getSpan();
                    start1 += text1.length();
                }

                final LineSelectionManager manager = LineSelectionManager.getInstance(editor);

                //if (!effectiveRange1.isEqual(range1)) {
                //    int range1StartDelta = start1 - effectiveRange1.getStart();
                //    int range1EndDelta = range1StartDelta + text2.length() - effectiveRange1.getEnd();
                //    editor.getSelectionModel().setSelection(range1.getStart() + range1StartDelta, range1.getEnd() + range1EndDelta);
                //    manager.pushSelection(true, true, true);
                //}
                //
                //if (!effectiveRange2.isEqual(range2)) {
                //    int range2StartDelta = start2 - effectiveRange2.getStart();
                //    int range2EndDelta = range2StartDelta + text1.length() - effectiveRange2.getEnd();
                //    editor.getSelectionModel().setSelection(range2.getStart() + range2StartDelta, range2.getEnd() + range2EndDelta);
                //    manager.pushSelection(true, true, true);
                //}

                editor.getSelectionModel().setSelection(start1, start1 + text2.length());
                manager.pushSelection(false, false, false);
                editor.getSelectionModel().setSelection(start2, start2 + text1.length());
                scrollToSelection(editor);
            });
        }
        return handled;
    }

    /**
     * Replace the text of range1 (non-overlapping part with range 2) with text from range2 (non-overlapping part of range 2)
     *
     * @param editor
     * @param range1
     * @param range2
     *
     * @return true if neither range is fully contained in the other and changes were made
     */
    public static boolean replaceRangeText(final Editor editor, final Range range1, final Range range2) {
        boolean handled = false;
        if (!range1.doesContain(range2) && !range2.doesContain(range1)) {
            // can replace text
            handled = true;

            WriteCommandAction.runWriteCommandAction(editor.getProject(), () -> {
                Range effectiveRange1 = range1.exclude(range2);
                Range effectiveRange2 = range2.exclude(range1);
                Document document = editor.getDocument();
                CharSequence chars = document.getCharsSequence();
                String text2 = chars.subSequence(effectiveRange2.getStart(), effectiveRange2.getEnd()).toString();
                int start1 = effectiveRange1.getStart();

                document.replaceString(effectiveRange1.getStart(), effectiveRange1.getEnd(), text2);

                editor.getSelectionModel().setSelection(start1, start1 + text2.length());
                scrollToSelection(editor);
            });
        }
        return handled;
    }

    /**
     * Replace the text of range1 (non-overlapping part with range 2) with text from range2 (non-overlapping part of range 2)
     *
     * @param document
     * @param range1
     * @param range2
     *
     * @return Pair first is range1 (non-overlapping) text and second is range2 non-overlapping text
     */
    @Nullable
    public static Pair<String, String> getRangeText(final Document document, final Range range1, final Range range2) {
        if (!range1.doesContain(range2) && !range2.doesContain(range1)) {
            // can replace text
            Object[] pair = new Object[] { null };

            ApplicationManager.getApplication().runReadAction(() -> {
                Range effectiveRange1 = range1.exclude(range2);
                Range effectiveRange2 = range2.exclude(range1);
                CharSequence chars = document.getCharsSequence();
                String text2 = chars.subSequence(effectiveRange2.getStart(), effectiveRange2.getEnd()).toString();
                String text1 = chars.subSequence(effectiveRange1.getStart(), effectiveRange1.getEnd()).toString();
                pair[0] = Pair.create(text1, text2);
            });

            //noinspection unchecked
            return (Pair<String, String>) pair[0];
        }
        return null;
    }

    public static class ByLineType {
        public int blank;
        public int comment;
        public int code;
        public int total;

        public ByLineType() {
            blank = 0;
            comment = 0;
            code = 0;
            total = 0;
        }

        public ByLineType(int blank, int comment, int code, int total) {
            this.blank = blank;
            this.comment = comment;
            this.code = code;
            this.total = total;
        }
    }

    public static class EachLineCarets {
        public int carets;
        public int caretSelections;

        public EachLineCarets() {
            carets = 0;
            caretSelections = 0;
        }
    }

    public static class StudiedCarets {
        public @Nullable LineCommentProcessor lineCommentProcessor;
        public @NotNull ArrayList<EditorCaret> carets;
        public @NotNull ByLineType lineCount;
        public @NotNull ByLineType caretCount;
        public @NotNull ByLineType eachLineCarets;
        public @NotNull ByLineType caretSelections;
        public @NotNull Range range;
        public @Nullable EditorCaret firstLineCaret;
        public @Nullable EditorCaret lastLineCaret;

        public StudiedCarets() {
            lineCommentProcessor = null;
            carets = new ArrayList<>();
            lineCount = new ByLineType();
            caretCount = new ByLineType();
            eachLineCarets = new ByLineType();
            caretSelections = new ByLineType();
            range = Range.NULL;

            firstLineCaret = null;
            lastLineCaret = null;
        }
    }
}
