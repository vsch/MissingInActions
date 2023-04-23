// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.vladsch.MissingInActions.Bundle;
import com.vladsch.MissingInActions.manager.EditorPositionFactory;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.util.CaretOffsets;
import com.vladsch.MissingInActions.util.NumberSequenceGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

public class RenumberingDialog extends DialogWrapper implements NumberingOptionsForm.ChangeListener, NumberingOptionsForm.BaseChangeListener {
    private JPanel myMainPanel;
    private NumberingOptionsForm myNumberingOptionsForm;
    private JPanel myViewPanel;

    private final @NotNull ApplicationSettings mySettings;
    private final @NotNull EditorEx myEditor;
    private final @NotNull EditorEx myViewer;

    private @Nullable String mySavedNonShiftFirst;
    private boolean myLastBitShift;
    private boolean myRestoreLastNonBitShift;

    private RenumberingDialog(JComponent parent, @NotNull EditorEx editor) {
        super(parent, false);

        setTitle(Bundle.message("renumber.title"));

        mySettings = ApplicationSettings.getInstance();

        myEditor = editor;
        myViewer = createIdeaEditor("");
        myViewPanel.add(myViewer.getComponent(), BorderLayout.CENTER);

        mySavedNonShiftFirst = null;
        myLastBitShift = mySettings.getLastNumberingOptions().isBitShift();
        myRestoreLastNonBitShift = false;

        copyEditorSettings();
        updateResults();

        init();
    }

    @Override
    public void disposeIfNeeded() {
        super.disposeIfNeeded();

        if (!myViewer.isDisposed()) {
            EditorFactory.getInstance().releaseEditor(myViewer);
        }
    }

    @Override
    public void optionsChanged(NumberingOptions options) {
        updateResults();
    }

    @Override
    public void baseChanged(NumberingOptions oldOptions, NumberingOptions newOptions) {
        // save the old base numbering options
        if (myNumberingOptionsForm != null) {
            mySettings.setLastNumberingOptions(oldOptions);

            NumberingOptions newBaseOptions = new NumberingOptions(mySettings.getNumberingBaseOptions(newOptions.getNumberingBase()), newOptions);
            mySettings.setLastNumberingOptions(newBaseOptions);
            myNumberingOptionsForm.setOptions(newBaseOptions);
        }
    }

    private void saveSettings() {
        NumberingOptions options = myNumberingOptionsForm.getOptions();
        mySettings.setLastNumberingOptions(options);
    }

    private String updateResults() {
        if (myNumberingOptionsForm != null) {
            NumberingOptions options = myNumberingOptionsForm.getOptions();
            List<CaretOffsets> carets = new ArrayList<>(myEditor.getCaretModel().getCaretCount());
            StringBuilder sb = new StringBuilder();

            if (options.isBitShift()) {
                long first = NumberSequenceGenerator.tryExtractNumber(
                        options.getFirst()
                        , null
                        , NumberSequenceGenerator.defaultOct
                        , options
                        , NumberSequenceGenerator.defaultBin
                        , NumberSequenceGenerator.defaultDec
                        , NumberSequenceGenerator.defaultHex
                );

                if (first == 0) {
                    // replace it with a 1 
                    if (!myLastBitShift) {
                        mySavedNonShiftFirst = options.getFirst();
                        myRestoreLastNonBitShift = true;
                    }

                    options.setFirst(options.getFirst().substring(0, options.getFirst().length() - 1) + "1");
                    myNumberingOptionsForm.reset(options);
                }
            }

            if (options.isBitShift() != myLastBitShift) {
                if (myRestoreLastNonBitShift && !options.isBitShift()) {
                    // restore saved non-bit-shift options
                    options.setFirst(mySavedNonShiftFirst);
                    myNumberingOptionsForm.reset(options);
                    myRestoreLastNonBitShift = false;
                }

                myLastBitShift = options.isBitShift();
            }

            final NumberSequenceGenerator generator = NumberSequenceGenerator.create(options);

            myNumberingOptionsForm.setFirstBase(generator.getFirstNumberBase() > 1 ? String.format("%-3d",generator.getFirstNumberBase()) : "???");
            myNumberingOptionsForm.setLastBase(generator.getLastNumberBase() > 1 ? String.format("%-3d",generator.getLastNumberBase()) : "???");
            myNumberingOptionsForm.setStepBase(generator.getStepNumberBase() > 1 ? String.format("%-3d",generator.getStepNumberBase()) : "???");

            // copy caret lines to new editor and re-create the carets by replacing selections if they exist
            // or inserting number at carets if no selection is present
            ApplicationManager.getApplication().runReadAction(() -> {
                int line = -1;
                int offset = -1;
                CharSequence chars = myEditor.getDocument().getCharsSequence();
                EditorPositionFactory f = LineSelectionManager.getInstance(myViewer).getPositionFactory();
                int lastVirtual = -1;

                for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                    final int caretLine = caret.getLogicalPosition().line;
                    generator.next(caretLine);
                    String number = generator.getNumber();

                    if (line == -1 || line != caretLine) {
                        // if prev line did not complete, add trailing chars here
                        if (offset >= 0 && line >= 0) {
                            final int endOffset = myEditor.getDocument().getLineEndOffset(line);
                            if (offset < endOffset + 1) {
                                sb.append(chars.subSequence(offset, endOffset + 1));
                                offset = endOffset + 1;
                            }
                        }

                        line = caretLine;
                        offset = myEditor.getDocument().getLineStartOffset(line);
                        lastVirtual = -1;
                    }

                    if (caret.hasSelection()) {
                        // replace selection by number
                        sb.append(chars.subSequence(offset, caret.getSelectionStart()));
                        int pos = sb.length();
                        sb.append(number);
                        int end = sb.length();
                        offset = caret.getSelectionEnd();
                        CaretOffsets offsets = new CaretOffsets(pos, pos, end);
                        carets.add(offsets);
                    } else {
                        // add number at caret but may need to add virtual spaces
                        sb.append(chars.subSequence(offset, caret.getOffset()));

                        int virtualSpaces = caret.getVisualPosition().column - (myEditor.getDocument().getLineEndOffset(caretLine) - myEditor.getDocument().getLineStartOffset(caretLine));
                        if (lastVirtual > 0) {
                            virtualSpaces -= lastVirtual;
                            lastVirtual += virtualSpaces;
                        } else {
                            lastVirtual = virtualSpaces;
                        }

                        while (virtualSpaces-- > 0) sb.append(' ');

                        int pos = sb.length();
                        sb.append(number);
                        int end = sb.length();
                        offset = caret.getOffset();
                        CaretOffsets offsets = new CaretOffsets(pos, pos, end);
                        carets.add(offsets);
                    }
                }

                if (offset >= 0 && line >= 0) {
                    final int endOffset = myEditor.getDocument().getLineEndOffset(line);
                    if (offset < endOffset) {
                        sb.append(chars.subSequence(offset, endOffset));
                    }
                    sb.append('\n');
                }
            });

            // do the editor preview update from source editor, include carets and selections, replacing selections with numbers
            WriteCommandAction.runWriteCommandAction(myViewer.getProject(), () -> {
                final Document document = myViewer.getDocument();
                document.setReadOnly(false);
                //document.replaceString(0, document.getTextLength(), getStringRep(editor, content, false, true, false));
                document.replaceString(0, document.getTextLength(), sb);
                document.setReadOnly(true);
            });

            // create carets in the viewer
            boolean first = true;
            myViewer.getCaretModel().removeSecondaryCarets();

            for (CaretOffsets offsets : carets) {
                Caret caret = first ? myViewer.getCaretModel().getPrimaryCaret() : myViewer.getCaretModel().addCaret(myViewer.offsetToVisualPosition(offsets.pos));
                first = false;

                if (caret != null) {
                    // move to logical position and set selection
                    caret.moveToOffset(offsets.pos);
                    caret.setSelection(offsets.start, offsets.end);
                }
            }
        }
        return "";
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return myMainPanel;
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        super.createDefaultActions();
        return new Action[] { getOKAction(), getCancelAction() };
    }

    public static boolean showDialog(JComponent parent, @NotNull EditorEx editor) {
        RenumberingDialog dialog = new RenumberingDialog(parent, editor);
        boolean save = dialog.showAndGet();
        dialog.saveSettings();
        dialog.releaseEditor();
        return save;
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        String error = updateResults();

        if (!error.isEmpty()) {
            return new ValidationInfo(error, myNumberingOptionsForm.getPreferredFocusedComponent());
        }
        return super.doValidate();
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return "MissingInActions.RegExTestDialog";
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return myNumberingOptionsForm.getPreferredFocusedComponent();
    }

    protected EditorEx createIdeaEditor(CharSequence charSequence) {
        Document doc = EditorFactory.getInstance().createDocument(charSequence);
        Editor editor = EditorFactory.getInstance().createEditor(doc, myEditor.getProject(), myEditor.getVirtualFile().getFileType(), true);
        editor.getSettings().setFoldingOutlineShown(false);
        editor.getSettings().setLineNumbersShown(false);
        editor.getSettings().setLineMarkerAreaShown(false);
        editor.getSettings().setIndentGuidesShown(false);
        return (EditorEx) editor;
    }

    private void releaseEditor() {
        if (!myViewer.isDisposed()) {
            EditorFactory.getInstance().releaseEditor(myViewer);
        }
    }

    private void createUIComponents() {
        myNumberingOptionsForm = new NumberingOptionsForm(ApplicationSettings.getInstance().getLastNumberingOptions(), this, this);
    }

    private static void ignoreErrors(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable ignored) {

        }
    }

    private void copyEditorSettings() {
        //boolean isRightMarginShown(); void setRightMarginShown(boolean val);
        //boolean areGutterIconsShown(); void setGutterIconsShown(boolean gutterIconsShown);
        //boolean isAdditionalPageAtBottom(); void setAdditionalPageAtBottom(boolean val);
        //boolean isAllowSingleLogicalLineFolding(); void setAllowSingleLogicalLineFolding(boolean allow);
        //boolean isAnimatedScrolling(); void setAnimatedScrolling(boolean val);
        //boolean isAutoCodeFoldingEnabled(); void setAutoCodeFoldingEnabled(boolean val);
        //boolean isBlinkCaret(); void setBlinkCaret(boolean blinkCaret);
        //boolean isBlockCursor(); void setBlockCursor(boolean blockCursor);
        //boolean isCamelWords(); void setCamelWords(boolean val);
        //boolean isCaretInsideTabs(); void setCaretInsideTabs(boolean allow);
        //boolean isCaretRowShown(); void setCaretRowShown(boolean caretRowShown);
        //boolean isDndEnabled(); void setDndEnabled(boolean val);
        //boolean isFoldingOutlineShown(); void setFoldingOutlineShown(boolean val);
        //boolean isIndentGuidesShown(); void setIndentGuidesShown(boolean val);
        //boolean isInnerWhitespaceShown(); void setInnerWhitespaceShown(boolean val);
        //boolean isLeadingWhitespaceShown(); void setLeadingWhitespaceShown(boolean val);
        //boolean isLineMarkerAreaShown(); void setLineMarkerAreaShown(boolean lineMarkerAreaShown);
        //boolean isLineNumbersShown(); void setLineNumbersShown(boolean val);
        //boolean isMouseClickSelectionHonorsCamelWords(); void setMouseClickSelectionHonorsCamelWords(boolean val);
        //boolean isRefrainFromScrolling(); void setRefrainFromScrolling(boolean b);
        //boolean isSmartHome(); void setSmartHome(boolean val);
        //boolean isTrailingWhitespaceShown(); void setTrailingWhitespaceShown(boolean val);
        //boolean isUseCustomSoftWrapIndent(); void setUseCustomSoftWrapIndent(boolean useCustomSoftWrapIndent);
        //boolean isUseSoftWraps(); void setUseSoftWraps(boolean use);
        //boolean isVariableInplaceRenameEnabled(); void setVariableInplaceRenameEnabled(boolean val);
        //boolean isVirtualSpace(); void setVirtualSpace(boolean allow);
        //boolean isWheelFontChangeEnabled(); void setWheelFontChangeEnabled(boolean val);
        //boolean isWhitespacesShown(); void setWhitespacesShown(boolean val);
        //int getAdditionalColumnsCount(); void setAdditionalColumnsCount(int additionalColumnsCount);
        //int getAdditionalLinesCount(); void setAdditionalLinesCount(int additionalLinesCount);
        //int getCaretBlinkPeriod(); void setCaretBlinkPeriod(int blinkPeriod);
        //int getCustomSoftWrapIndent(); void setCustomSoftWrapIndent(int indent);
        //int getLineCursorWidth(); void setLineCursorWidth(int width);
        EditorSettings myViewerSettings = myViewer.getSettings();
        EditorSettings myEditorSettings = myEditor.getSettings();
        Project myEditorProject = myEditor.getProject();

        // @formatter:off
        ignoreErrors(()->{myViewerSettings.setRightMarginShown(myEditorSettings.isRightMarginShown()); });
        ignoreErrors(()->{myViewerSettings.setGutterIconsShown(myEditorSettings.areGutterIconsShown()); });
        ignoreErrors(()->{myViewerSettings.setAdditionalPageAtBottom(myEditorSettings.isAdditionalPageAtBottom()); });
        ignoreErrors(()->{myViewerSettings.setAllowSingleLogicalLineFolding(myEditorSettings.isAllowSingleLogicalLineFolding()); });
        ignoreErrors(()->{myViewerSettings.setAnimatedScrolling(myEditorSettings.isAnimatedScrolling()); });
        ignoreErrors(()->{myViewerSettings.setAutoCodeFoldingEnabled(myEditorSettings.isAutoCodeFoldingEnabled()); });
        ignoreErrors(()->{myViewerSettings.setBlinkCaret(myEditorSettings.isBlinkCaret()); });
        ignoreErrors(()->{myViewerSettings.setBlockCursor(myEditorSettings.isBlockCursor()); });
        ignoreErrors(()->{myViewerSettings.setCamelWords(myEditorSettings.isCamelWords()); });
        ignoreErrors(()->{myViewerSettings.setCaretInsideTabs(myEditorSettings.isCaretInsideTabs()); });
        ignoreErrors(()->{myViewerSettings.setCaretRowShown(myEditorSettings.isCaretRowShown()); });
        ignoreErrors(()->{myViewerSettings.setDndEnabled(myEditorSettings.isDndEnabled()); });
        ignoreErrors(()->{myViewerSettings.setFoldingOutlineShown(myEditorSettings.isFoldingOutlineShown()); });
        ignoreErrors(()->{myViewerSettings.setIndentGuidesShown(myEditorSettings.isIndentGuidesShown()); });
        ignoreErrors(()->{myViewerSettings.setInnerWhitespaceShown(myEditorSettings.isInnerWhitespaceShown()); });
        ignoreErrors(()->{myViewerSettings.setLeadingWhitespaceShown(myEditorSettings.isLeadingWhitespaceShown()); });
        ignoreErrors(()->{myViewerSettings.setLineMarkerAreaShown(myEditorSettings.isLineMarkerAreaShown()); });
        ignoreErrors(()->{myViewerSettings.setLineNumbersShown(myEditorSettings.isLineNumbersShown()); });
        ignoreErrors(()->{myViewerSettings.setMouseClickSelectionHonorsCamelWords(myEditorSettings.isMouseClickSelectionHonorsCamelWords()); });
        ignoreErrors(()->{myViewerSettings.setRefrainFromScrolling(myEditorSettings.isRefrainFromScrolling()); });
        ignoreErrors(()->{myViewerSettings.setSmartHome(myEditorSettings.isSmartHome()); });
        ignoreErrors(()->{myViewerSettings.setTrailingWhitespaceShown(myEditorSettings.isTrailingWhitespaceShown()); });
        ignoreErrors(()->{myViewerSettings.setUseCustomSoftWrapIndent(myEditorSettings.isUseCustomSoftWrapIndent()); });
        ignoreErrors(()->{myViewerSettings.setUseSoftWraps(myEditorSettings.isUseSoftWraps()); });
        //ignoreErrors(()->{myViewerSettings.setVariableInplaceRenameEnabled(myEditorSettings.isVariableInplaceRenameEnabled()); });
        ignoreErrors(()->{myViewerSettings.setVirtualSpace(myEditorSettings.isVirtualSpace()); });
        ignoreErrors(()->{myViewerSettings.setWheelFontChangeEnabled(myEditorSettings.isWheelFontChangeEnabled()); });
        ignoreErrors(()->{myViewerSettings.setWhitespacesShown(myEditorSettings.isWhitespacesShown()); });
        //ignoreErrors(()->{myViewerSettings.setAdditionalColumnsCount(myEditorSettings.getAdditionalColumnsCount()); });
        //ignoreErrors(()->{myViewerSettings.setAdditionalLinesCount(myEditorSettings.getAdditionalLinesCount()); });
        ignoreErrors(()->{myViewerSettings.setCaretBlinkPeriod(myEditorSettings.getCaretBlinkPeriod()); });
        ignoreErrors(()->{myViewerSettings.setCustomSoftWrapIndent(myEditorSettings.getCustomSoftWrapIndent()); });
        ignoreErrors(()->{myViewerSettings.setLineCursorWidth(myEditorSettings.getLineCursorWidth()); });
        // @formatter:on

        //boolean isUseTabCharacter(Project project); void setUseTabCharacter(boolean useTabCharacter);
        //boolean isWrapWhenTypingReachesRightMargin(Project project); void setWrapWhenTypingReachesRightMargin(boolean val);
        //int getRightMargin(Project project); void setRightMargin(int myRightMargin);
        //int getTabSize(Project project); void setTabSize(int tabSize);
        // @formatter:off
        ignoreErrors(() -> { myViewerSettings.setUseTabCharacter(myEditorSettings.isUseTabCharacter(myEditorProject)); });
        //ignoreErrors(() -> { myViewerSettings.setWrapWhenTypingReachesRightMargin(myEditorSettings.isWrapWhenTypingReachesRightMargin(myEditorProject)); });
        ignoreErrors(() -> { myViewerSettings.setRightMargin(myEditorSettings.getRightMargin(myEditorProject)); });
        ignoreErrors(() -> { myViewerSettings.setTabSize(myEditorSettings.getTabSize(myEditorProject)); });
        // @formatter:on
    }
}
