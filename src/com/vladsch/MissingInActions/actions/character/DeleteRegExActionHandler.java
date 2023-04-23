// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.character;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.editor.actions.EditorActionUtil;
import com.intellij.openapi.ide.CopyPasteManager;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.MissingInActions.util.RegExDeleteProvider;
import com.vladsch.ReverseRegEx.util.ForwardPattern;
import com.vladsch.ReverseRegEx.util.RegExMatcher;
import com.vladsch.ReverseRegEx.util.RegExPattern;
import com.vladsch.ReverseRegEx.util.ReversePattern;

import static com.vladsch.MissingInActions.util.EditHelpers.isHumpBoundWord;

public class DeleteRegExActionHandler extends EditorWriteActionHandler {
    protected enum HumpsMode {
        NONE,
        HUMPS,
        FOLLOW,
        INVERT,
        ;

        boolean isHumpsMode(Editor editor) {
            switch (this) {
                case NONE:
                    return false;
                case HUMPS:
                    return true;
                case FOLLOW:
                    return editor.getSettings().isCamelWords();
                case INVERT:
                    return !editor.getSettings().isCamelWords();
            }
            return false;
        }
    }

    final boolean myIsReverseSearch;
    final HumpsMode myHumpsMode;
    final RegExDeleteProvider myRegExProvider;

    public DeleteRegExActionHandler(RegExDeleteProvider regExProvider, boolean isReverseSearch, final HumpsMode humpsMode) {
        super(true);

        myRegExProvider = regExProvider;
        myHumpsMode = humpsMode;
        myIsReverseSearch = isReverseSearch;
    }

    @Override
    public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
        CommandProcessor.getInstance().setCurrentCommandGroupId(EditorActionUtil.DELETE_COMMAND_GROUP);
        CopyPasteManager.getInstance().stopKillRings();

        if (editor.getSelectionModel().hasSelection()) {
            EditHelpers.deleteSelectedText(editor);
            return;
        }

        boolean isReversed = myIsReverseSearch;
        String regEx = myRegExProvider.getRegEx();
        RegExPattern pattern = isReversed ? ReversePattern.compile("(?:" + regEx + ")$") : ForwardPattern.compile("^(?:" + regEx + ")");
        int caretPos = editor.getCaretModel().getOffset();
        final CharSequence charsSequence = editor.getDocument().getCharsSequence();
        RegExMatcher matcher = pattern.matcher(charsSequence);

        boolean isLineBound = editor.getCaretModel().getCaretCount() > 1 ? myRegExProvider.isMultiCaretLineBound() : myRegExProvider.isLineBound();

        if (isReversed) {
            int lowBound = 0;
            if (isLineBound) {
                int line = editor.getDocument().getLineNumber(caretPos);
                lowBound = editor.getDocument().getLineStartOffset(line);
            }
            matcher.region(lowBound, caretPos);
        } else {
            int highBound = charsSequence.length();
            if (isLineBound) {
                int line = editor.getDocument().getLineNumber(caretPos);
                highBound = editor.getDocument().getLineEndOffset(line);
            }
            matcher.region(caretPos, highBound);
        }
        matcher.useTransparentBounds(true);

        if (matcher.find()) {
            int startOffset = matcher.start();
            int endOffset = matcher.end();

            if (endOffset > startOffset) {
                Document document = editor.getDocument();

                if (myHumpsMode.isHumpsMode(editor)) {
                    // see if we have a camel hump boundary start to end or if reversed, end to start
                    CharSequence text = document.getCharsSequence();

                    if (isReversed) {
                        if (EditHelpers.isIdentifier(text, endOffset - 1)) {
                            // find start
                            int humpStart = endOffset;
                            while (--humpStart >= startOffset) {
                                if (isHumpBoundWord(text, humpStart, true)) {
                                    startOffset = humpStart;
                                    break;
                                }
                            }
                        }
                    } else {
                        if (EditHelpers.isIdentifier(text, startOffset)) {
                            // find start
                            int humpEnd = startOffset + 1;
                            while (++humpEnd < endOffset) {
                                if (isHumpBoundWord(text, humpEnd, false)) {
                                    endOffset = humpEnd;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (endOffset > startOffset) {
                    document.deleteString(startOffset, endOffset);
                }
            }
        }
    }
}
