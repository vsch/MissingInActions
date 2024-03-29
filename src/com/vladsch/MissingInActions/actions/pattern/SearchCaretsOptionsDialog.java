// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.UIUtil;
import com.vladsch.MissingInActions.Bundle;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.RegExSettingsHolder;
import com.vladsch.flexmark.util.html.ui.BackgroundColor;
import com.vladsch.flexmark.util.html.ui.HtmlHelpers;
import com.vladsch.plugin.util.ui.Helpers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class SearchCaretsOptionsDialog extends DialogWrapper {
    JPanel myMainPanel;
    private JTextField myPattern;
    JTextField mySampleText;
    private JPanel myViewPanel;
    private JTextPane myTextPane;
    private JBCheckBox myBackwards;
    private JBCheckBox myCaseSensitive;
    private JButton myFocusViewer;
    private JBCheckBox myCaretToEndGroup;

    private final @NotNull EditorEx myEditor;
    private final @NotNull EditorEx myViewer;

    private final RegExSettingsHolder mySettingsHolder;

    private static BackgroundColor getWarningTextFieldBackground() {
        return BackgroundColor.of(Helpers.warningColor(UIUtil.getTextFieldBackground()));
    }

    private static BackgroundColor getValidTextFieldBackground() {
        return BackgroundColor.of(UIUtil.getTextFieldBackground());
    }

    public boolean saveSettings(boolean onlySamples) {
        // save settings return false if regex is not valid
        mySettingsHolder.setSampleText(mySampleText.getText());

        if (!onlySamples) {
            mySettingsHolder.setBackwards(myBackwards.isSelected());
            mySettingsHolder.setCaseSensitive(myCaseSensitive.isSelected());
            mySettingsHolder.setCaretToGroupEnd(myCaretToEndGroup.isSelected());
            mySettingsHolder.setPatternText(myPattern.getText().trim());
        }

        return onlySamples || checkRegEx(myPattern).isEmpty();
    }

    public SearchCaretsOptionsDialog(JComponent parent, @NotNull RegExSettingsHolder settingsHolder, @NotNull EditorEx editor) {
        super(parent, false);

        setTitle(Bundle.message("caret-search.options-dialog.title"));

        mySettingsHolder = settingsHolder;
        String text = settingsHolder.getPatternText();
        myPattern.setText(text);
        boolean caseSensitive = mySettingsHolder.isCaseSensitive();
        myCaseSensitive.setSelected(caseSensitive);
        myBackwards.setSelected(mySettingsHolder.isBackwards());
        myCaretToEndGroup.setSelected(mySettingsHolder.isCaretToGroupEnd());

        mySampleText.setVisible(false);

        myEditor = editor;
        myViewer = createIdeaEditor("");
        myViewPanel.add(myViewer.getComponent(), BorderLayout.CENTER);

        myFocusViewer.addActionListener(e -> {
            if (checkRegEx(myPattern).isEmpty()) {
                updateViewer();
            }
            myViewer.getContentComponent().requestFocus();
        });

        final ActionListener actionListener = e -> {
            if (checkRegEx(myPattern).isEmpty()) {
                updateViewer();
            }
            ApplicationManager.getApplication().invokeLater(() -> myViewer.getContentComponent().requestFocus());
        };

        myCaseSensitive.addActionListener(actionListener);
        myBackwards.addActionListener(actionListener);
        myCaretToEndGroup.addActionListener(actionListener);

        final DocumentAdapter listener = new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                if (checkRegEx(myPattern).isEmpty()) {
                    updateViewer();
                }
            }
        };

        myPattern.getDocument().addDocumentListener(listener);

        copyEditorSettings();

        if (checkRegEx(myPattern).isEmpty()) {
            updateViewer();
        }

        init();
    }

    @Override
    public void disposeIfNeeded() {
        super.disposeIfNeeded();

        if (!myViewer.isDisposed()) {
            EditorFactory.getInstance().releaseEditor(myViewer);
        }
    }

    @SuppressWarnings("SameParameterValue")
    protected EditorEx createIdeaEditor(CharSequence charSequence) {
        Document doc = EditorFactory.getInstance().createDocument(charSequence);
        Editor editor = EditorFactory.getInstance().createEditor(doc, myEditor.getProject(), myEditor.getVirtualFile().getFileType(), true);
        editor.getSettings().setFoldingOutlineShown(false);
        editor.getSettings().setLineNumbersShown(false);
        editor.getSettings().setLineMarkerAreaShown(false);
        editor.getSettings().setIndentGuidesShown(false);
        return (EditorEx) editor;
    }

    private static void ignoreErrors(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable ignored) {

        }
    }

    @SuppressWarnings({ "CommentedOutCode", "GrazieInspection" })
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
        ignoreErrors(()->myViewerSettings.setRightMarginShown(myEditorSettings.isRightMarginShown()));
        ignoreErrors(()->myViewerSettings.setGutterIconsShown(myEditorSettings.areGutterIconsShown()));
        ignoreErrors(()->myViewerSettings.setAdditionalPageAtBottom(myEditorSettings.isAdditionalPageAtBottom()));
        ignoreErrors(()->myViewerSettings.setAllowSingleLogicalLineFolding(myEditorSettings.isAllowSingleLogicalLineFolding()));
        ignoreErrors(()->myViewerSettings.setAnimatedScrolling(myEditorSettings.isAnimatedScrolling()));
        ignoreErrors(()->myViewerSettings.setAutoCodeFoldingEnabled(myEditorSettings.isAutoCodeFoldingEnabled()));
        ignoreErrors(()->myViewerSettings.setBlinkCaret(myEditorSettings.isBlinkCaret()));
        ignoreErrors(()->myViewerSettings.setBlockCursor(myEditorSettings.isBlockCursor()));
        ignoreErrors(()->myViewerSettings.setCamelWords(myEditorSettings.isCamelWords()));
        ignoreErrors(()->myViewerSettings.setCaretInsideTabs(myEditorSettings.isCaretInsideTabs()));
        ignoreErrors(()->myViewerSettings.setCaretRowShown(myEditorSettings.isCaretRowShown()));
        ignoreErrors(()->myViewerSettings.setDndEnabled(myEditorSettings.isDndEnabled()));
        ignoreErrors(()->myViewerSettings.setFoldingOutlineShown(myEditorSettings.isFoldingOutlineShown()));
        ignoreErrors(()->myViewerSettings.setIndentGuidesShown(myEditorSettings.isIndentGuidesShown()));
        ignoreErrors(()->myViewerSettings.setInnerWhitespaceShown(myEditorSettings.isInnerWhitespaceShown()));
        ignoreErrors(()->myViewerSettings.setLeadingWhitespaceShown(myEditorSettings.isLeadingWhitespaceShown()));
        ignoreErrors(()->myViewerSettings.setLineMarkerAreaShown(myEditorSettings.isLineMarkerAreaShown()));
        ignoreErrors(()->myViewerSettings.setLineNumbersShown(myEditorSettings.isLineNumbersShown()));
        ignoreErrors(()->myViewerSettings.setMouseClickSelectionHonorsCamelWords(myEditorSettings.isMouseClickSelectionHonorsCamelWords()));
        ignoreErrors(()->myViewerSettings.setRefrainFromScrolling(myEditorSettings.isRefrainFromScrolling()));
        ignoreErrors(()->myViewerSettings.setSmartHome(myEditorSettings.isSmartHome()));
        ignoreErrors(()->myViewerSettings.setTrailingWhitespaceShown(myEditorSettings.isTrailingWhitespaceShown()));
        ignoreErrors(()->myViewerSettings.setUseCustomSoftWrapIndent(myEditorSettings.isUseCustomSoftWrapIndent()));
        ignoreErrors(()->myViewerSettings.setUseSoftWraps(myEditorSettings.isUseSoftWraps()));
        //ignoreErrors(()->{ myViewerSettings.setVariableInplaceRenameEnabled(myEditorSettings.isVariableInplaceRenameEnabled()); });
        ignoreErrors(()->myViewerSettings.setVirtualSpace(myEditorSettings.isVirtualSpace()));
        ignoreErrors(()->myViewerSettings.setWheelFontChangeEnabled(myEditorSettings.isWheelFontChangeEnabled()));
        ignoreErrors(()->myViewerSettings.setWhitespacesShown(myEditorSettings.isWhitespacesShown()));
        //ignoreErrors(()->{ myViewerSettings.setAdditionalColumnsCount(myEditorSettings.getAdditionalColumnsCount()); });
        //ignoreErrors(()->{ myViewerSettings.setAdditionalLinesCount(myEditorSettings.getAdditionalLinesCount()); });
        ignoreErrors(()->myViewerSettings.setCaretBlinkPeriod(myEditorSettings.getCaretBlinkPeriod()));
        ignoreErrors(()->myViewerSettings.setCustomSoftWrapIndent(myEditorSettings.getCustomSoftWrapIndent()));
        ignoreErrors(()->myViewerSettings.setLineCursorWidth(myEditorSettings.getLineCursorWidth()));
        // @formatter:on

        //boolean isUseTabCharacter(Project project); void setUseTabCharacter(boolean useTabCharacter);
        //boolean isWrapWhenTypingReachesRightMargin(Project project); void setWrapWhenTypingReachesRightMargin(boolean val);
        //int getRightMargin(Project project); void setRightMargin(int myRightMargin);
        //int getTabSize(Project project); void setTabSize(int tabSize);
        // @formatter:off
        ignoreErrors(() -> myViewerSettings.setUseTabCharacter(myEditorSettings.isUseTabCharacter(myEditorProject)));
        //ignoreErrors(() -> { myViewerSettings.setWrapWhenTypingReachesRightMargin(myEditorSettings.isWrapWhenTypingReachesRightMargin(myEditorProject)); });
        ignoreErrors(() -> myViewerSettings.setRightMargin(myEditorSettings.getRightMargin(myEditorProject)));
        ignoreErrors(() -> myViewerSettings.setTabSize(myEditorSettings.getTabSize(myEditorProject)));
        // @formatter:on
    }

    private void updateViewer() {
        List<LogicalPosition> carets = new ArrayList<>(myEditor.getCaretModel().getCaretCount());
        StringBuilder sb = new StringBuilder();
        LineSelectionManager otherManager = LineSelectionManager.getInstance(myEditor);
        List<CaretState> states = otherManager.getStartCaretStates();
        assert states != null;

        states.sort(Comparator.comparing(CaretState::getCaretPosition));

        // copy caret lines to new editor and re-create the carets by replacing selections if they exist
        // or inserting number at carets if no selection is present
        ApplicationManager.getApplication().runReadAction(() -> {
            DocumentEx document = myEditor.getDocument();
            CharSequence chars = document.getCharsSequence();

            int i = 0;
            for (CaretState caret : states) {
                LogicalPosition position = caret.getCaretPosition();
                if (position == null) continue;

                final int caretLine = position.line;
                int start = document.getLineStartOffset(caretLine);
                int end = document.getLineEndOffset(caretLine);
                sb.append(chars.subSequence(start, end)).append('\n');
                carets.add(new LogicalPosition(i++, position.column));
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

        for (LogicalPosition position : carets) {
            Caret caret = first ? myViewer.getCaretModel().getPrimaryCaret() : myViewer.getCaretModel().addCaret(myViewer.logicalToVisualPosition(position));
            first = false;

            if (caret != null) {
                // move to logical position and set selection
                caret.moveToLogicalPosition(position);
                caret.removeSelection();
            }
        }

        CaretSpawningSearchHandler handler = new CaretSpawningSearchHandler(myBackwards.isSelected());

        LineSelectionManager manager = LineSelectionManager.getInstance(myViewer);
        if (otherManager.getCaretSpawningHandler() != null) {
            handler.copySettings((CaretSpawningSearchHandler) otherManager.getCaretSpawningHandler(), myViewer);
        }
        handler.setCaseSensitive(myCaseSensitive.isSelected());
        handler.setCaretToEndGroup(myCaretToEndGroup.isSelected());
        handler.setPattern(myPattern.getText());
        handler.caretsChanged(myViewer);
        handler.doAction(manager, myViewer, null, null);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return myMainPanel;
    }

    @Override
    protected Action @NotNull [] createActions() {
        super.createDefaultActions();
        return new Action[] { getOKAction(), getCancelAction() };
    }

    public static boolean showDialog(JComponent parent, @NotNull RegExSettingsHolder settingsHolder, @NotNull EditorEx editor) {
        SearchCaretsOptionsDialog dialog = new SearchCaretsOptionsDialog(parent, settingsHolder, editor);
        boolean save = dialog.showAndGet();
        return dialog.saveSettings(!save);
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        String error = checkRegEx(myPattern);

        if (!error.isEmpty()) {
            return new ValidationInfo(error, myPattern);
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
        return myPattern;
    }

    private String checkRegEx(final JTextField pattern) {
        final String patternText = pattern.getText().trim();
        String error = "";
        boolean isBadRegEx = false;

        if (!myPattern.getText().trim().isEmpty()) {
            try {
                Pattern.compile(patternText);
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                error = e.getMessage();
                isBadRegEx = true;
            }
        } else {
            error = "empty pattern";
        }

        if (!isBadRegEx) {
            myViewPanel.setVisible(true);
            myTextPane.setVisible(false);
        } else {
            myViewPanel.setVisible(false);
            HtmlHelpers.setRegExError(error, myTextPane, mySampleText.getFont(), getValidTextFieldBackground(), getWarningTextFieldBackground());
        }
        return error;
    }

    private void createUIComponents() {

    }
}
