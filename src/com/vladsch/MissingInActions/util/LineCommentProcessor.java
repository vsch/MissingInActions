// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.util;

import com.intellij.lang.Commenter;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LineCommentProcessor {
    private final @NotNull Editor myEditor;
    private final @NotNull PsiFile myPsiFile;
    private final @NotNull BasedSequence myChars;
    private @Nullable ItemTextRange<Commenter> myCommenterRange;

    public LineCommentProcessor(@NotNull Editor editor, @NotNull final PsiFile psiFile) {
        myEditor = editor;
        myPsiFile = psiFile;
        myChars = BasedSequence.of(myEditor.getDocument().getCharsSequence());

        myCommenterRange = null;
    }

    @NotNull
    public PsiFile getPsiFile() {
        return myPsiFile;
    }

    @NotNull
    public Editor getEditor() {
        return myEditor;
    }

    private Commenter getCommenterRange(int startOffset, int endOffset) {
        if (myCommenterRange == null || !myCommenterRange.containsRange(startOffset, endOffset)) {
            myCommenterRange = EditHelpers.getCommenterRange(myEditor, myPsiFile, startOffset, endOffset);
        }
        return myCommenterRange == null ? null : myCommenterRange.getItem();
    }

    public boolean isLineCommented(int offset) {
        final int startOfLineOffset = myChars.startOfLine(offset);
        final int endOfLineOffset = myChars.endOfLine(offset);
        return isLineCommented(startOfLineOffset, endOfLineOffset);
    }

    public boolean isLineCommented(final int startOfLineOffset, final int endOfLineOffset) {
        Commenter commenter = getCommenterRange(startOfLineOffset, endOfLineOffset);
        if (commenter != null) {
            String lineCommentPrefix = commenter.getLineCommentPrefix();
            String blockCommentPrefix = commenter.getCommentedBlockCommentPrefix();
            if (blockCommentPrefix == null) blockCommentPrefix = commenter.getBlockCommentPrefix();
            String blockCommentSuffix = commenter.getCommentedBlockCommentSuffix();
            if (blockCommentSuffix == null) blockCommentSuffix = commenter.getBlockCommentSuffix();
            BasedSequence chars = myChars.subSequence(startOfLineOffset, endOfLineOffset);
            BasedSequence trimmed = chars.trim();

            return lineCommentPrefix != null && trimmed.startsWith(lineCommentPrefix)
                    || blockCommentPrefix != null && blockCommentSuffix != null && trimmed.startsWith(blockCommentPrefix) && trimmed.endsWith(blockCommentSuffix);
        } else {
            return false;
        }
    }

    public boolean isFormattingRegion(int startOffset, int endOffset) {
        //// find the first comment with a  formatter directive
        //Project project = psiFile != null ? psiFile.getProject() : editor.getProject();
        //if (project == null || !getFormatterTagsEnabled(project)) return true;
        //
        //// need psi file for this editor
        //if (psiFile == null) {
        //    final VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
        //    if (virtualFile == null) return true;
        //    psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        //}
        //
        //Commenter commenter = findCommenterRange(editor, psiFile,
        //
        //        editor.getDocument()
        //
        //        var node = psiFile.node.lastChildNode
        //while (node != null) {
        //    if (node.startOffset + node.textLength <= offset) {
        //        break;
        //    }
        //    node = node.treePrev;
        //}
        //
        //while (node != null) {
        //    if (node.elementType == MultiMarkdownTypes.HTML_BLOCK) {
        //        var html = node.firstChildNode
        //        while (html != null) {
        //            if (html.elementType == MultiMarkdownTypes.COMMENT) {
        //                val text = node.text.trim().removePrefix("<!--").removeSuffix("-->").trim()
        //                if (text == formatterOnTag) {
        //                    return true;
        //                } else if (text == formatterOffTag) {
        //                    return false;
        //                }
        //            }
        //            html = html.treeNext
        //        }
        //    }
        //    node = node.treePrev
        //}
        return true;
    }
}
