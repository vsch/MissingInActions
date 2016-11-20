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

import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actions.EditorActionUtil;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.ide.CopyPasteManager;
import com.vladsch.MissingInActions.manager.EditorPositionFactory;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.manager.LineSelectionState;
import com.vladsch.MissingInActions.manager.EditorPosition;
import com.vladsch.flexmark.util.sequence.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EditHelpers {
    public static void moveCaretToNextWordStart(@NotNull Editor editor, boolean isWithSelection, boolean camel, boolean stopAtTrailingBlanks, boolean limitToLine, boolean strictIdentifier) {
        Document document = editor.getDocument();
        SelectionModel selectionModel = editor.getSelectionModel();
        int selectionStart = selectionModel.getLeadSelectionOffset();
        CaretModel caretModel = editor.getCaretModel();
        LogicalPosition position = caretModel.getLogicalPosition();
        LogicalPosition blockSelectionStart = caretModel.getLogicalPosition();

        int offset = caretModel.getOffset();
        if (offset == document.getTextLength()) {
            return;
        }

        int lineNumber = caretModel.getLogicalPosition().line;
        if (lineNumber >= document.getLineCount()) return;
        boolean haveMultiCarets = caretModel.getCaretCount() > 1;

        int stopAtLastNonBlank = 0;

        // have to stop at start of word if caret is not at or before first non-blank
        int lineStartOffset = document.getLineStartOffset(lineNumber);
        if (stopAtTrailingBlanks || strictIdentifier) {
            int lineEndOffset = document.getLineEndOffset(lineNumber);
            int trailingBlanks = countWhiteSpaceReversed(document.getCharsSequence(), lineStartOffset, lineEndOffset);
            if (caretModel.getOffset() < lineEndOffset - trailingBlanks) {
                stopAtLastNonBlank = lineEndOffset - trailingBlanks;
            }
        }

        int maxLineNumber = haveMultiCarets || stopAtLastNonBlank > 0 || lineNumber + 1 > document.getLineCount() ? lineNumber : lineNumber + 1;
        int maxOffset = stopAtLastNonBlank > 0 ? stopAtLastNonBlank : document.getLineEndOffset(maxLineNumber);

        int newOffset = offset + 1;
        if (newOffset > maxOffset) return;

        FoldRegion currentFoldRegion = editor.getFoldingModel().getCollapsedRegionAtOffset(offset);
        if (currentFoldRegion != null) {
            newOffset = currentFoldRegion.getEndOffset();
        }

        for (; newOffset < maxOffset; newOffset++) {
            if (strictIdentifier ? isIdentifierStart(editor, newOffset, camel) : isWordStart(editor, newOffset, camel)) break;
        }
        FoldRegion foldRegion = editor.getFoldingModel().getCollapsedRegionAtOffset(newOffset);
        if (foldRegion != null) {
            newOffset = foldRegion.getStartOffset();
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

    public static void moveCaretToPreviousWordStart(@NotNull Editor editor, boolean isWithSelection, boolean camel, boolean limitToLIne, boolean strictIdentifier) {
        Document document = editor.getDocument();
        SelectionModel selectionModel = editor.getSelectionModel();
        int selectionStart = selectionModel.getLeadSelectionOffset();
        CaretModel caretModel = editor.getCaretModel();
        LogicalPosition blockSelectionStart = caretModel.getLogicalPosition();

        int offset = editor.getCaretModel().getOffset();
        if (offset == 0) return;

        boolean haveMultiCarets = caretModel.getCaretCount() > 1;
        int lineNumber = editor.getCaretModel().getLogicalPosition().line;
        int minLineNumber = haveMultiCarets || lineNumber == 0 ? lineNumber : lineNumber - 1;
        int minOffset = document.getLineStartOffset(minLineNumber);

        int newOffset = offset - 1;
        if (newOffset < minOffset) return;

        FoldRegion currentFoldRegion = editor.getFoldingModel().getCollapsedRegionAtOffset(newOffset - 1);
        if (currentFoldRegion != null) {
            newOffset = currentFoldRegion.getStartOffset();
        }
        for (; newOffset > minOffset; newOffset--) {
            if (strictIdentifier ? isIdentifierStart(editor, newOffset, camel) : isWordStart(editor, newOffset, camel)) break;
        }
        FoldRegion foldRegion = editor.getFoldingModel().getCollapsedRegionAtOffset(newOffset);
        if (foldRegion != null && newOffset > foldRegion.getEndOffset()) {
            newOffset = foldRegion.getEndOffset();
        }

        if (editor instanceof EditorImpl && ((EditorImpl) editor).myUseNewRendering) {
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

    public static void moveCaretToNextWordEnd(@NotNull Editor editor, boolean isWithSelection, boolean camel, boolean limitToLine, boolean strictIdentifier) {
        Document document = editor.getDocument();
        SelectionModel selectionModel = editor.getSelectionModel();
        int selectionStart = selectionModel.getLeadSelectionOffset();
        CaretModel caretModel = editor.getCaretModel();
        LogicalPosition blockSelectionStart = caretModel.getLogicalPosition();

        int offset = caretModel.getOffset();
        if (offset == document.getTextLength()) {
            return;
        }

        boolean haveMultiCarets = caretModel.getCaretCount() > 1;
        int lineNumber = caretModel.getLogicalPosition().line;
        if (lineNumber >= document.getLineCount()) return;
        int maxLineNumber = haveMultiCarets || lineNumber + 1 > document.getLineCount() ? lineNumber : lineNumber + 1;
        int maxOffset = document.getLineEndOffset(maxLineNumber);

        int newOffset = offset + 1;
        if (newOffset > maxOffset) return;

        FoldRegion currentFoldRegion = editor.getFoldingModel().getCollapsedRegionAtOffset(offset);
        if (currentFoldRegion != null) {
            newOffset = currentFoldRegion.getEndOffset();
            // move to word end
        }
        for (; newOffset < maxOffset; newOffset++) {
            if (strictIdentifier ? isIdentifierEnd(editor, newOffset, camel) : isWordEnd(editor, newOffset, camel)) break;
        }
        FoldRegion foldRegion = editor.getFoldingModel().getCollapsedRegionAtOffset(newOffset);
        if (foldRegion != null) {
            newOffset = foldRegion.getStartOffset();
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

    public static void moveCaretToPreviousWordEnd(@NotNull Editor editor, boolean isWithSelection, boolean camel, boolean stopAtTrailingBlanks, boolean limitToLine, boolean strictIdentifier) {
        Document document = editor.getDocument();
        SelectionModel selectionModel = editor.getSelectionModel();
        int selectionStart = selectionModel.getLeadSelectionOffset();
        CaretModel caretModel = editor.getCaretModel();
        LogicalPosition blockSelectionStart = caretModel.getLogicalPosition();

        int offset = editor.getCaretModel().getOffset();
        if (offset == 0) return;

        boolean haveMultiCarets = caretModel.getCaretCount() > 1;
        LogicalPosition position = editor.getCaretModel().getLogicalPosition();
        int lineNumber = position.line;
        int stopAtIndent = 0;

        // have to stop at start of word if caret is not at or before first non-blank
        int lineStartOffset = document.getLineStartOffset(lineNumber);
        if (stopAtTrailingBlanks) {
            int firstNonBlank = countWhiteSpace(document.getCharsSequence(), lineStartOffset, document.getLineEndOffset(lineNumber));
            if (position.column > firstNonBlank) {
                stopAtIndent = lineStartOffset + firstNonBlank;
            }
        }

        int minLineNumber = haveMultiCarets || lineNumber == 0 || stopAtIndent > 0 ? lineNumber : lineNumber - 1;
        int minOffset = stopAtIndent > 0 ? stopAtIndent : document.getLineStartOffset(minLineNumber);

        // if virtual spaces are enabled the caret can be after the end so we should pretend it is on the next char after the end
        int newOffset = blockSelectionStart.column > offset - lineStartOffset ? offset : offset - 1;
        if (newOffset < minOffset) return;

        FoldRegion currentFoldRegion = editor.getFoldingModel().getCollapsedRegionAtOffset(offset - 1);
        if (currentFoldRegion != null) {
            newOffset = currentFoldRegion.getStartOffset();
            // move to end of previous word
        }
        for (; newOffset > minOffset; newOffset--) {
            if (strictIdentifier ? isIdentifierEnd(editor, newOffset, camel) : isWordEnd(editor, newOffset, camel)) break;
        }
        FoldRegion foldRegion = editor.getFoldingModel().getCollapsedRegionAtOffset(newOffset);
        if (foldRegion != null && newOffset > foldRegion.getStartOffset()) {
            newOffset = foldRegion.getEndOffset();
        }

        if (editor instanceof EditorImpl && ((EditorImpl) editor).myUseNewRendering) {
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

    public static boolean isWordStart(@NotNull Editor editor, int offset, boolean isCamel) {
        CharSequence chars = editor.getDocument().getCharsSequence();
        return EditorActionUtil.isWordStart(chars, offset, isCamel);
    }

    public static boolean isWordEnd(@NotNull Editor editor, int offset, boolean isCamel) {
        CharSequence chars = editor.getDocument().getCharsSequence();
        return EditorActionUtil.isWordEnd(chars, offset, isCamel);
    }

    public static boolean isIdentifierStart(@NotNull Editor editor, int offset, boolean isCamel) {
        CharSequence chars = editor.getDocument().getCharsSequence();
        return EditorActionUtil.isWordStart(chars, offset, isCamel);
    }

    public static boolean isIdentifierEnd(@NotNull Editor editor, int offset, boolean isCamel) {
        CharSequence chars = editor.getDocument().getCharsSequence();
        return EditorActionUtil.isWordEnd(chars, offset, isCamel);
    }

    private static void setupSelection(@NotNull Editor editor,
            boolean isWithSelection,
            int selectionStart,
            @NotNull LogicalPosition blockSelectionStart) {
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

    public static boolean isIdentifierStart(@NotNull CharSequence text, int offset, boolean isCamel) {
        char prev = offset > 0 ? text.charAt(offset - 1) : 0;
        char current = text.charAt(offset);

        final boolean prevIsIdentifierPart = Character.isJavaIdentifierPart(prev);
        final boolean currentIsIdentifierPart = Character.isJavaIdentifierPart(current);

        //noinspection SimplifiableIfStatement
        if (!prevIsIdentifierPart && currentIsIdentifierPart) return true;

        return isCamel && prevIsIdentifierPart && currentIsIdentifierPart && isHumpBoundStart(text, offset);
    }

    public static boolean isIdentifierEnd(@NotNull CharSequence text, int offset, boolean isCamel) {
        char prev = offset > 0 ? text.charAt(offset - 1) : 0;
        char current = text.charAt(offset);
        char next = offset + 1 < text.length() ? text.charAt(offset + 1) : 0;

        final boolean prevIsIdentifierPart = Character.isJavaIdentifierPart(prev);
        final boolean currentIsIdentifierPart = Character.isJavaIdentifierPart(current);

        //noinspection SimplifiableIfStatement
        if (prevIsIdentifierPart && !currentIsIdentifierPart) return true;

        return isCamel && prevIsIdentifierPart
                && (Character.isLowerCase(prev) && Character.isUpperCase(current)
                || prev != '_' && current == '_'
                || Character.isUpperCase(prev) && Character.isUpperCase(current) && Character.isLowerCase(next));
    }

    public static boolean isHumpBoundStart(@NotNull CharSequence editorText, int offset) {
        return isHumpBoundWord(editorText, offset, true);
    }

    public static boolean isHumpBoundEnd(@NotNull CharSequence editorText, int offset) {
        return isHumpBoundWord(editorText, offset, false);
    }

    public static boolean isHumpBoundWord(@NotNull CharSequence editorText, int offset, boolean start) {
        if (offset <= 0 || offset >= editorText.length()) return false;
        final char prevChar = editorText.charAt(offset - 1);
        final char curChar = editorText.charAt(offset);

        return isLowerCaseOrDigit(prevChar) && Character.isUpperCase(curChar) ||
                start && prevChar == '_' && curChar != '_' ||
                start && prevChar == '$' && Character.isLetterOrDigit(curChar) ||
                !start && prevChar != '_' && curChar == '_' ||
                !start && Character.isLetterOrDigit(prevChar) && curChar == '$';
    }

    public static boolean isHumpBoundEnd(@NotNull CharSequence editorText, int offset, boolean start) {
        if (offset <= 0 || offset >= editorText.length()) return false;
        final char prevChar = editorText.charAt(offset - 1);
        final char curChar = editorText.charAt(offset);
        final char nextChar = offset + 1 < editorText.length() ? editorText.charAt(offset + 1) : 0; // 0x00 is not lowercase.

        return isLowerCaseOrDigit(prevChar) && Character.isUpperCase(curChar) ||
                !start && prevChar != '_' && curChar == '_' ||
                !start && Character.isLetterOrDigit(prevChar) && curChar == '$';
    }

    private static boolean isLowerCaseOrDigit(char c) {
        return Character.isLowerCase(c) || Character.isDigit(c);
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
            LineSelectionState state = manager.getSelectionState(caret);
            if (state.isLine()) {
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
}
