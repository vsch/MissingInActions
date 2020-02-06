/*
 * Copyright (c) 2016-2018 Vladimir Schneider <vladimir.schneider@gmail.com>
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

package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.codeInsight.daemon.impl.EditorTracker;
import com.intellij.ide.CopyPasteManagerEx;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.VisibleAreaEvent;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.ex.FileChooserDialogImpl;
import com.intellij.openapi.fileChooser.ex.FileSaverDialogImpl;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.TextTransferable;
import com.intellij.util.ui.UIUtil;
import com.vladsch.MissingInActions.Bundle;
import com.vladsch.MissingInActions.Plugin;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.settings.BatchSearchReplace;
import com.vladsch.MissingInActions.settings.BatchSearchReplaceSettings;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.MissingInActions.util.MiaCancelableJobScheduler;
import com.vladsch.MissingInActions.util.highlight.MiaLineHighlightProviderImpl;
import com.vladsch.MissingInActions.util.highlight.MiaWordHighlightProviderImpl;
import com.vladsch.boxed.json.BoxedJsObject;
import com.vladsch.boxed.json.BoxedJson;
import com.vladsch.flexmark.util.html.ui.BackgroundColor;
import com.vladsch.flexmark.util.html.ui.HtmlHelpers;
import com.vladsch.flexmark.util.misc.BitFieldSet;
import com.vladsch.flexmark.util.misc.Utils;
import com.vladsch.flexmark.util.sequence.RepeatedSequence;
import com.vladsch.plugin.util.AwtRunnable;
import com.vladsch.plugin.util.OneTimeRunnable;
import com.vladsch.plugin.util.ui.Helpers;
import com.vladsch.plugin.util.ui.highlight.HighlightListener;
import com.vladsch.plugin.util.ui.highlight.Highlighter;
import com.vladsch.plugin.util.ui.highlight.LineHighlightProvider;
import com.vladsch.plugin.util.ui.highlight.LineHighlighter;
import com.vladsch.plugin.util.ui.highlight.TypedRangeHighlightProviderBase;
import com.vladsch.plugin.util.ui.highlight.WordHighlightProvider;
import com.vladsch.plugin.util.ui.highlight.WordHighlighter;
import icons.PluginIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BatchReplaceForm implements Disposable {
    private static final int DELAY_MILLIS = 250;
    JPanel myMainPanel;
    JTextField mySampleText;
    private JPanel mySearchViewPanel;
    private JPanel myReplaceViewPanel;
    private JPanel myOptionsViewPanel;
    private JTextPane myTextPane;
    private JBCheckBox myWholeWord;
    private JBCheckBox myCaseSensitive;
    private JButton myFindNext;
    private JButton myFindPrevious;
    private JButton myReplace;
    private JButton myReplaceAll;
    private JButton myExclude;
    private JButton mySwapSearchReplace;
    private JButton myGetFromClipboard;
    JComboBox<String> myPresets;
    JButton mySavePreset;
    private JButton myManageActions;
    private JButton myReset;
    private JButton myReplaceSortUp;
    private JButton myReplaceSortDown;
    private JButton mySearchSortUp;
    private JButton mySearchSortDown;
    private JButton myToggleTandemEdit;
    private JButton myToggleSearchHighlights;
    private JButton mySearchCopyRegEx;
    private JButton myToggleReplaceHighlights;
    private final JBPopupMenu myPopupMenuActions;

    private Editor myEditor;
    EditorEx mySearchEditor;
    private EditorEx myReplaceEditor;
    private EditorEx myOptionsEditor;

    private final ApplicationSettings mySettings;

    private @Nullable HashMap<Integer, Integer> myWordIndexToLineMap = null;
    private @Nullable HashMap<Integer, SearchData> myLineSearchData = null;
    private int[] myIndexedWordCounts = null;
    private TextRange myFoundRange = null;
    private boolean myHighlightSearchLines = false;
    private boolean myHighlightReplaceLines = false;
    private boolean myBatchTandemEdit = false;
    private boolean myIsIncludeMode = false;

    private ArrayList<TextRange> myExcludedRanges = null;
    private WordHighlightProvider<ApplicationSettings> myEditorSearchHighlightProvider;
    private LineHighlightProvider<ApplicationSettings> mySearchHighlightProvider;
    private LineHighlightProvider<ApplicationSettings> myReplaceHighlightProvider;
    private LineHighlightProvider<ApplicationSettings> myOptionsHighlightProvider;

    final private DocumentListener myDocumentListener;
    final private CaretListener myCaretListener;
    final private VisibleAreaListener myVisibleAreaListener;
    final private CaretListener myEditorCaretListener;
    final private HighlightListener myHighlightListener;
    final private Project myProject;

    private int myFoundIndex = -1;
    private Boolean myFoundBackwards = null;
    private boolean myInUpdate = false;
    private long myLastEditorSync = Long.MIN_VALUE;
    private Editor myLastSyncEditor = null;
    private boolean myIsActive = false;
    private OneTimeRunnable myHighlightRunner = OneTimeRunnable.NULL;
    private OneTimeRunnable myEditorHighlightRunner = OneTimeRunnable.NULL;
    private boolean myPendingForcedUpdate = false;
    private boolean myPendingReplace = false;
    private boolean myInTandemEdit = false;

    private final Border myDefaultBorder;
    private final Border myDarculaBorder;
    private final Border myNoBorder;

    private void updateLastEditorSync(Editor editor) {
        myLastEditorSync = System.currentTimeMillis();
        myLastSyncEditor = editor;
    }

    private boolean canSyncEditors(Editor editor) {
        return editor == myLastSyncEditor || myLastEditorSync + (long) BatchReplaceForm.DELAY_MILLIS < System.currentTimeMillis();
    }

    BackgroundColor getInvalidTextFieldBackground() {
        return BackgroundColor.of(Helpers.errorColor(UIUtil.getTextFieldBackground()));
    }

    private BackgroundColor getWarningTextFieldBackground() {
        return BackgroundColor.of(Helpers.warningColor(UIUtil.getTextFieldBackground()));
    }

    private BackgroundColor getValidTextFieldBackground() {
        return BackgroundColor.of(UIUtil.getTextFieldBackground());
    }

    BackgroundColor getSelectedTextFieldBackground() {
        return BackgroundColor.of(mySampleText.getSelectionColor());
    }

    @Override
    public void dispose() {
        disposeEditors();
    }

    @SuppressWarnings("WeakerAccess")
    public void saveSettings() {
        mySettings.getBatchSearchReplace().setWholeWord(myWholeWord.isSelected());
        mySettings.getBatchSearchReplace().setCaseSensitive(myCaseSensitive.isSelected());
        mySettings.getBatchSearchReplace().setSearchText(mySearchEditor.getDocument().getText());
        mySettings.getBatchSearchReplace().setReplaceText(myReplaceEditor.getDocument().getText());
        mySettings.getBatchSearchReplace().setOptionsText(myOptionsEditor.getDocument().getText());
        mySettings.setBatchHighlightSearchLines(myHighlightSearchLines);
        mySettings.setBatchHighlightReplaceLines(myHighlightReplaceLines);
        mySettings.setBatchTandemEdit(myBatchTandemEdit);
    }

    @SuppressWarnings("WeakerAccess")
    public void disposeEditors() {
        if (mySearchEditor != null) {
            Editor searchEditor;
            Editor replaceEditor;
            Editor optionsEditor;

            synchronized (this) {
                searchEditor = mySearchEditor;
                replaceEditor = myReplaceEditor;
                optionsEditor = myOptionsEditor;

                mySearchEditor = null;
                myReplaceEditor = null;
                myOptionsEditor = null;
            }

            if (searchEditor != null) {
                // release the editors
                setActiveEditor(null);

                searchEditor.getDocument().removeDocumentListener(myDocumentListener);
                replaceEditor.getDocument().removeDocumentListener(myDocumentListener);
                optionsEditor.getDocument().removeDocumentListener(myDocumentListener);

                searchEditor.getCaretModel().removeCaretListener(myCaretListener);
                replaceEditor.getCaretModel().removeCaretListener(myCaretListener);
                optionsEditor.getCaretModel().removeCaretListener(myCaretListener);

                searchEditor.getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener);
                replaceEditor.getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener);
                optionsEditor.getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener);

                LineSelectionManager.getInstance(searchEditor).setHighlightProvider(null);
                LineSelectionManager.getInstance(replaceEditor).setHighlightProvider(null);
                LineSelectionManager.getInstance(optionsEditor).setHighlightProvider(null);

                myEditor = null;
                myEditorSearchHighlightProvider.removeHighlightListener(myHighlightListener);

                Disposer.dispose(myEditorSearchHighlightProvider);
                Disposer.dispose(mySearchHighlightProvider);
                Disposer.dispose(myReplaceHighlightProvider);
                Disposer.dispose(myOptionsHighlightProvider);

                myEditorSearchHighlightProvider = null;
                mySearchHighlightProvider = null;
                myReplaceHighlightProvider = null;
                myOptionsHighlightProvider = null;

                EditorFactory.getInstance().releaseEditor(searchEditor);
                EditorFactory.getInstance().releaseEditor(replaceEditor);
                EditorFactory.getInstance().releaseEditor(optionsEditor);
            }
        }
    }

    public void setActiveEditor(@Nullable Editor editor) {
        if (shouldNotUpdateHighlighters(editor)) return;

        if (editor != null && editor.isDisposed()) {
            editor = null;
        }

        if (myEditor != editor) {
            myExcludedRanges = null;

            if (myEditor != null && !myEditor.isDisposed()) {
                myEditor.getCaretModel().removeCaretListener(myEditorCaretListener);
            }

            myEditor = editor;

            if (myEditor != null) {
                copyEditorSettings(mySearchEditor);
                copyEditorSettings(myReplaceEditor);
                copyEditorSettings(myOptionsEditor);
                myEditor.getCaretModel().addCaretListener(myEditorCaretListener);
            }

            myHighlightRunner.cancel();

            if (!myInUpdate) {
                myFoundBackwards = null;
                myHighlightRunner = OneTimeRunnable.schedule(MiaCancelableJobScheduler.getInstance(), 100, new AwtRunnable(true, () -> updateOptions(true)));
            }
        }
    }

    public boolean shouldNotUpdateHighlighters(@Nullable Editor editor) {
        return editor == null || editor.isDisposed() || editor == mySearchEditor || editor == myReplaceEditor || editor == myOptionsEditor;
    }

    public void updateHighlighters() {
        myEditorHighlightRunner.cancel();
        myEditorHighlightRunner = OneTimeRunnable.schedule(MiaCancelableJobScheduler.getInstance(), 500, new AwtRunnable(true, () -> {
//            List<Editor> editors = EditorTracker.getInstance(myProject).getActiveEditors();
            Editor[] editors = EditorFactory.getInstance().getAllEditors();
            for (Editor editor : editors) {
                if (shouldNotUpdateHighlighters(editor)) continue;
                LineSelectionManager.getInstance(editor).setHighlightProvider(myIsActive ? myEditorSearchHighlightProvider : null);
            }
        }));
    }

    private class MainEditorCaretListener implements CaretListener {
        @Override
        public void caretPositionChanged(@NotNull final CaretEvent e) {
            myHighlightRunner.cancel();

            if (!myInUpdate) {
                myFoundBackwards = null;
                myHighlightRunner = OneTimeRunnable.schedule(MiaCancelableJobScheduler.getInstance(), 100, new AwtRunnable(true, () -> updateOptions(false)));
            }
        }

        @Override
        public void caretAdded(@NotNull final CaretEvent e) {

        }

        @Override
        public void caretRemoved(@NotNull final CaretEvent e) {

        }
    }

    private class MainEditorHighlightListener implements HighlightListener {
        @Override
        public void highlightsChanged() {

        }

        @Override
        public void highlightsUpdated() {
            if (!myInUpdate && myEditor != null) {
                Highlighter<ApplicationSettings> highlighter = LineSelectionManager.getInstance(myEditor).getHighlighter();
                if (highlighter instanceof WordHighlighter) {
                    myIndexedWordCounts = ((WordHighlighter<ApplicationSettings>) highlighter).getIndexedRangeCounts();
                }
                LineSelectionManager.getInstance(mySearchEditor).updateHighlights();
                LineSelectionManager.getInstance(myReplaceEditor).updateHighlights();
                LineSelectionManager.getInstance(myOptionsEditor).updateHighlights();
            }
        }
    }

    void registerFileEditorListener() {
        FileEditorManagerListener editorManagerListener = new FileEditorManagerListener() {
            @Override
            public void fileOpened(@NotNull final FileEditorManager source, @NotNull final VirtualFile file) {

            }

            @Override
            public void fileClosed(@NotNull final FileEditorManager source, @NotNull final VirtualFile file) {

            }

            @Override
            public void selectionChanged(@NotNull final FileEditorManagerEvent event) {
                Editor activeEditor = Plugin.getEditorEx(event.getNewEditor());
                setActiveEditor(activeEditor);
            }
        };

        myProject.getMessageBus().connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, editorManagerListener);
    }

    public BatchReplaceForm(@NotNull Project project, @NotNull ApplicationSettings applicationSettings) {
        myInUpdate = true;
        myProject = project;
        mySettings = applicationSettings;

        String searchText = mySettings.getBatchSearchReplace().getSearchText();
        String replaceText = mySettings.getBatchSearchReplace().getReplaceText();
        String optionsText = mySettings.getBatchSearchReplace().getOptionsText();

        boolean caseSensitive = mySettings.getBatchSearchReplace().isCaseSensitive();
        myCaseSensitive.setSelected(caseSensitive);
        myWholeWord.setSelected(mySettings.getBatchSearchReplace().isWholeWord());
        myHighlightSearchLines = mySettings.isBatchHighlightSearchLines();
        myHighlightReplaceLines = mySettings.isBatchHighlightReplaceLines();
        myBatchTandemEdit = mySettings.isBatchTandemEdit();

        registerFileEditorListener();

        // FEATURE: enable adding replace strings to highlights and remove line
        myToggleReplaceHighlights.setVisible(false);

        mySampleText.setVisible(false);

        myEditorCaretListener = new MainEditorCaretListener();

        mySearchEditor = createIdeaEditor(Utils.suffixWith(searchText, "\n"));
        myReplaceEditor = createIdeaEditor(Utils.suffixWith(replaceText, "\n"));
        myOptionsEditor = createIdeaEditor(Utils.suffixWith(optionsText, "\n"));

        mySearchViewPanel.add(mySearchEditor.getComponent(), BorderLayout.CENTER);
        myReplaceViewPanel.add(myReplaceEditor.getComponent(), BorderLayout.CENTER);
        myOptionsViewPanel.add(myOptionsEditor.getComponent(), BorderLayout.CENTER);

        myEditorSearchHighlightProvider = new SearchWordHighlighterProvider(mySettings);
        mySearchHighlightProvider = new EditorLineHighlighterProvider(mySettings);
        myReplaceHighlightProvider = new EditorLineHighlighterProvider(mySettings);
        myOptionsHighlightProvider = new EditorLineHighlighterProvider(mySettings);

        myEditorSearchHighlightProvider.initComponent();
        mySearchHighlightProvider.initComponent();
        myReplaceHighlightProvider.initComponent();
        myOptionsHighlightProvider.initComponent();

        LineSelectionManager.getInstance(mySearchEditor).setHighlightProvider(mySearchHighlightProvider);
        LineSelectionManager.getInstance(myReplaceEditor).setHighlightProvider(myReplaceHighlightProvider);
        LineSelectionManager.getInstance(myOptionsEditor).setHighlightProvider(myOptionsHighlightProvider);

        myHighlightListener = new MainEditorHighlightListener();
        myEditorSearchHighlightProvider.addHighlightListener(myHighlightListener, this);

        mySearchSortUp.setIcon(PluginIcons.Sort_up);
        mySearchSortDown.setIcon(PluginIcons.Sort_down);
        myReplaceSortUp.setIcon(PluginIcons.Sort_up);
        myReplaceSortDown.setIcon(PluginIcons.Sort_down);
        mySearchCopyRegEx.setIcon(PluginIcons.Copy_batch_replace_regex);
        mySearchCopyRegEx.setDisabledIcon(PluginIcons.Copy_batch_replace_regex_disabled);

        myDefaultBorder = mySearchViewPanel.getBorder();
        myDarculaBorder = myReplaceViewPanel.getBorder();
        myNoBorder = myOptionsViewPanel.getBorder();
        updateIconButtons();

        mySearchCopyRegEx.addActionListener(e -> copyRegEx(false));

        mySearchSortUp.addActionListener(e -> sortDocument(false, false));
        mySearchSortDown.addActionListener(e -> sortDocument(false, true));
        myReplaceSortUp.addActionListener(e -> sortDocument(true, false));
        myReplaceSortDown.addActionListener(e -> sortDocument(true, true));

        myToggleTandemEdit.addActionListener(e -> {
            myBatchTandemEdit = !myBatchTandemEdit;
            updateIconButtons();
        });

        myToggleSearchHighlights.addActionListener(e -> {
            myHighlightSearchLines = !myHighlightSearchLines;
            updateIconButtons();
            updateFoundRanges();
        });

        myToggleReplaceHighlights.addActionListener(e -> {
            myHighlightReplaceLines = !myHighlightReplaceLines;
            updateIconButtons();
            updateFoundRanges();
        });

        myDocumentListener = new EditorDocumentListener();

        mySearchEditor.getDocument().addDocumentListener(myDocumentListener);
        myReplaceEditor.getDocument().addDocumentListener(myDocumentListener);
        myOptionsEditor.getDocument().addDocumentListener(myDocumentListener);

        myCaretListener = new EditorCaretListener();

        mySearchEditor.getCaretModel().addCaretListener(myCaretListener);
        myReplaceEditor.getCaretModel().addCaretListener(myCaretListener);
        myOptionsEditor.getCaretModel().addCaretListener(myCaretListener);

        myVisibleAreaListener = new EditorVisibleAreaListener();

        mySearchEditor.getScrollingModel().addVisibleAreaListener(myVisibleAreaListener);
        myReplaceEditor.getScrollingModel().addVisibleAreaListener(myVisibleAreaListener);
        myOptionsEditor.getScrollingModel().addVisibleAreaListener(myVisibleAreaListener);

        final ActionListener actionListener = e -> updateOptions(false);
        final ActionListener textChangedActionListener = e -> updateOptions(true);

        myFindNext.addActionListener(e -> findNext());

        myFindPrevious.addActionListener(e -> findPrevious());

        myReplace.addActionListener(e -> replace());

        myReplaceAll.addActionListener(e -> replaceAll());

        myExclude.addActionListener(e -> exclude());

        myReset.addActionListener(e -> reset());

        mySavePreset.addActionListener(e -> {
            String presetName = (String) myPresets.getEditor().getItem();
            BatchSearchReplace oldSettings = mySettings.getPreset(presetName);
            saveSettings();

            mySettings.savePreset(presetName);
            mySettings.setBatchPresetName(presetName);
            if (oldSettings == null) {
                // reload
                fillPresets();
            }
        });

        myPopupMenuActions = new JBPopupMenu("Actions");
        final JBMenuItem exportXML = new JBMenuItem(Bundle.message("batch-search.export-xml.label"));
        final JBMenuItem importXML = new JBMenuItem(Bundle.message("batch-search.import-xml.label"));
        final JBMenuItem exportJSON = new JBMenuItem(Bundle.message("batch-search.export-json.label"));
        final JBMenuItem importJSON = new JBMenuItem(Bundle.message("batch-search.import-json.label"));
        final JBMenuItem deletePreset = new JBMenuItem(Bundle.message("batch-search.delete.label"));
        final JBMenuItem clearAllPresets = new JBMenuItem(Bundle.message("batch-search.clear-all.label"));

        myPresets.addActionListener(e -> {
            if (!myInUpdate) {
                int selected = myPresets.getSelectedIndex();
                if (selected != -1) {
                    // recall the given prefix
                    String presetName = myPresets.getItemAt(selected);
                    mySettings.loadPreset(presetName);
                    settingsChanged(false);
                    deletePreset.setEnabled(true);
                } else {
                    mySettings.setBatchPresetName(null);
                    deletePreset.setEnabled(false);
                }
            }
        });

        myPresets.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent e) {
                mySavePreset.setEnabled(!((String) myPresets.getEditor().getItem()).isEmpty());
            }
        });

        mySwapSearchReplace.addActionListener(e -> {
            boolean savedInUpdate = myInUpdate;
            myInUpdate = true;
            String searchText1 = mySearchEditor.getDocument().getText();
            String replaceText1 = myReplaceEditor.getDocument().getText();
            WriteCommandAction.runWriteCommandAction(myProject, () -> {
                mySearchEditor.getDocument().setText(replaceText1);
                myReplaceEditor.getDocument().setText(searchText1);
            });
            myInUpdate = savedInUpdate;
            updateOptions(true);
        });

        myGetFromClipboard.addActionListener(e -> {
            final CopyPasteManagerEx copyPasteManager = CopyPasteManagerEx.getInstanceEx();
            final Transferable[] contents = copyPasteManager.getAllContents();
            if (contents.length > 1) {
                // we take top two
                try {
                    final String replaceText12 = (String) contents[0].getTransferData(DataFlavor.stringFlavor);
                    final String searchText12 = (String) contents[1].getTransferData(DataFlavor.stringFlavor);
                    boolean savedInUpdate = myInUpdate;
                    myInUpdate = true;
                    WriteCommandAction.runWriteCommandAction(myProject, () -> {
                        mySearchEditor.getDocument().setText(searchText12);
                        myReplaceEditor.getDocument().setText(replaceText12);
                    });
                    myInUpdate = savedInUpdate;
                    updateOptions(true);
                } catch (UnsupportedFlavorException | IOException ignored) {

                }
            }
        });

        myCaseSensitive.addActionListener(textChangedActionListener);
        myWholeWord.addActionListener(textChangedActionListener);

        myPresets.setEditable(true);

        deletePreset.addActionListener(e -> {
            String presetName = (String) myPresets.getSelectedItem();
            if (presetName != null) {
                BatchSearchReplace removed = mySettings.getBatchPresets().remove(presetName);
                if (removed != null) {
                    myInUpdate = true;
                    mySettings.setBatchPresetName(null);
                    BatchSearchReplace batchSearchReplace = new BatchSearchReplace(mySettings.getBatchSearchReplace());
                    fillPresets();
                    //myPresets.setSelectedIndex(-1);
                    mySettings.setBatchSearchReplace(batchSearchReplace);
                    myInUpdate = false;
                    updateOptions(true);
                }
            }
        });

        final VirtualFile projectFile = myProject.getProjectFile();
        final VirtualFile projectDir = projectFile == null ? null : projectFile.getParent();
        final VirtualFile projectBaseDir = projectDir == null ? LocalFileSystem.getInstance().findFileByPath(System.getProperty("user.home")) : projectDir.getName().equals(".idea") ? projectDir.getParent() : projectDir;
        assert projectBaseDir != null;

        exportXML.addActionListener(e -> {
            String title = Bundle.message("batch-search.export.title");
            String description = Bundle.message("batch-search.export.description");
            FileSaverDescriptor fileSaverDescriptor = new FileSaverDescriptor(title, description, "xml");
            FileSaverDialogImpl saveDialog = new FileSaverDialogImpl(fileSaverDescriptor, myMainPanel);
            VirtualFileWrapper file = saveDialog.save(projectBaseDir, "batch-search-replace.xml");

            if (file != null) {
                try {
                    FileUtil.createParentDirs(file.getFile());
                    FileOutputStream fileWriter = new FileOutputStream(file.getFile());
                    BatchSearchReplaceSettings externalizedSettings = new BatchSearchReplaceSettings(mySettings);
                    XMLEncoder xmlEncoder = new XMLEncoder(fileWriter, "UTF-8", true, 0);
                    xmlEncoder.writeObject(externalizedSettings);
                    xmlEncoder.close();
                    fileWriter.close();
                    VirtualFileManager.getInstance().asyncRefresh(null);
                    //JDOMUtil.write(root, file.getFile(), "\n");
                } catch (IOException e1) {
                    Messages.showErrorDialog(e1.getMessage(), "Export Failure");
                }
            }
        });

        importXML.addActionListener(e -> {
            FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false);
            String title = Bundle.message("batch-search.import.title");
            String description = Bundle.message("batch-search.import.description");
            fileChooserDescriptor.setTitle(title);
            fileChooserDescriptor.setDescription(description);
            FileChooserDialogImpl fileChooserDialog = new FileChooserDialogImpl(fileChooserDescriptor, myMainPanel, myProject);
            String lastImport = myProject.getBasePath() + "/" + "batch-search-replace.xml";
            VirtualFile lastImportFile = null;
            if (!lastImport.isEmpty()) {
                File file = new File(lastImport);
                lastImportFile = VirtualFileManager.getInstance().findFileByUrl("file://" + file.getPath());
            }
            if (lastImportFile == null) lastImportFile = projectBaseDir;

            VirtualFile[] files = fileChooserDialog.choose(myProject, lastImportFile);
            if (files.length > 0) {
                try {
                    BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(files[0].getPath()));
                    XMLDecoder decoder = new XMLDecoder(inputStream, this, Throwable::printStackTrace, this.getClass().getClassLoader());

                    Object object = decoder.readObject();
                    BatchSearchReplaceSettings externalizedSettings = (BatchSearchReplaceSettings) object;
                    decoder.close();
                    inputStream.close();
                    if (externalizedSettings != null) {
                        mySettings.copyFrom(externalizedSettings);
                        settingsChanged(true);
                    } else {
                        Messages.showErrorDialog("File does not contain exported Batch Search/Replace settings.", "Import Failure");
                    }
                } catch (Exception e1) {
                    Messages.showErrorDialog(e1.getMessage(), "Import Failure");
                }
            }
        });

        exportJSON.addActionListener(e -> {
            String title = Bundle.message("batch-search.export.title");
            String description = Bundle.message("batch-search.export.description");
            FileSaverDescriptor fileSaverDescriptor = new FileSaverDescriptor(title, description, "json");
            FileSaverDialogImpl saveDialog = new FileSaverDialogImpl(fileSaverDescriptor, myMainPanel);
            VirtualFileWrapper file = saveDialog.save(projectBaseDir, "batch-search-replace.json");
            if (file != null) {
                try {
                    FileUtil.createParentDirs(file.getFile());
                    FileWriter fileWriter = new FileWriter(file.getFile());
                    saveSettings();
                    BoxedJsObject settings = BoxedJson.of();
                    BoxedJsObject presets = BoxedJson.of();
                    settings.put("presets", presets);

                    exportJSONPresets(presets);
                    fileWriter.write(settings.toString());

                    fileWriter.close();
                    VirtualFileManager.getInstance().asyncRefresh(null);
                    //JDOMUtil.write(root, file.getFile(), "\n");
                } catch (IOException e1) {
                    Messages.showErrorDialog(e1.getMessage(), "Export Failure");
                }
            }
        });

        importJSON.addActionListener(e -> {
            FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false);
            String title = Bundle.message("batch-search.import.title");
            String description = Bundle.message("batch-search.import.description");
            fileChooserDescriptor.setTitle(title);
            fileChooserDescriptor.setDescription(description);
            FileChooserDialogImpl fileChooserDialog = new FileChooserDialogImpl(fileChooserDescriptor, myMainPanel, myProject);
            String lastImport = myProject.getBasePath() + "/" + "batch-search-replace.json";
            VirtualFile lastImportFile = null;
            if (!lastImport.isEmpty()) {
                File file = new File(lastImport);
                lastImportFile = VirtualFileManager.getInstance().findFileByUrl("file://" + file.getPath());
            }
            if (lastImportFile == null) lastImportFile = projectBaseDir;

            VirtualFile[] files = fileChooserDialog.choose(myProject, lastImportFile);
            if (files.length > 0) {
                try {
                    BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(files[0].getPath()));
                    BoxedJsObject settings = BoxedJson.from(inputStream);
                    inputStream.close();

                    BoxedJsObject presets = settings.getJsObject("presets");
                    BatchSearchReplaceSettings externalizedSettings = new BatchSearchReplaceSettings();

                    if (presets.isValid() && importFromJSON(externalizedSettings, presets)) {
                        mySettings.copyFrom(externalizedSettings);
                        settingsChanged(true);
                    } else {
                        Messages.showErrorDialog("File does not contain exported Batch Search/Replace settings.", "Import Failure");
                    }
                } catch (Exception e1) {
                    Messages.showErrorDialog(e1.getMessage(), "Import Failure");
                }
            }
        });

        clearAllPresets.addActionListener(e -> {
            mySettings.getBatchPresets().clear();
            mySettings.setBatchPresetName("");
            mySettings.setBatchSearchReplace(new BatchSearchReplace());
            settingsChanged(true);
        });

        myPopupMenuActions.add(exportJSON);
        myPopupMenuActions.add(importJSON);
        myPopupMenuActions.addSeparator();
        myPopupMenuActions.add(exportXML);
        myPopupMenuActions.add(importXML);
        myPopupMenuActions.addSeparator();
        myPopupMenuActions.add(deletePreset);
        myPopupMenuActions.addSeparator();
        myPopupMenuActions.add(clearAllPresets);

        myManageActions.setComponentPopupMenu(myPopupMenuActions);

        myManageActions.addActionListener(e -> myPopupMenuActions.show(myManageActions, myManageActions.getWidth() / 10, myManageActions.getHeight() * 85 / 100));

        myMainPanel.addPropertyChangeListener(evt -> {
            String propertyName = evt.getPropertyName();
            if (propertyName.equals("ancestor")) {
                myIsActive = evt.getNewValue() != null;

                if (myEditor != null) {
                    if (myIsActive) {
                        updateOptions(true);
                    } else {
                        saveSettings();
                    }
                }

                Plugin.getInstance().setProjectHighlightProvider(myProject, myIsActive ? myEditorSearchHighlightProvider : null);
                updateHighlighters();
            } else if (propertyName.equals("Frame.active")) {
                if (!(boolean) evt.getNewValue()) {
                    saveSettings();
                }
            }
        });

        fillPresets();
        updateOptions(true);

        myInUpdate = false;
    }

    @SuppressWarnings("SameParameterValue")
    private void copyRegEx(final boolean replaceText) {
        if (myEditorSearchHighlightProvider.haveHighlights()) {
            Pattern pattern = myEditorSearchHighlightProvider.getHighlightPattern();
            if (pattern != null && !pattern.pattern().isEmpty()) {
                Transferable transferable = new TextTransferable(pattern.pattern());
                CopyPasteManager.getInstance().setContents(transferable);
            }
        }
    }

    private void updateIconButtons() {
        myToggleTandemEdit.setIcon(myBatchTandemEdit ? PluginIcons.Tandem_locked : PluginIcons.Tandem_unlocked);
        myToggleTandemEdit.setSelected(myBatchTandemEdit);

        myToggleSearchHighlights.setIcon(myHighlightSearchLines ? PluginIcons.Toggle_word_highlights : PluginIcons.No_word_highlights);
        myToggleSearchHighlights.setSelected(myHighlightSearchLines);

        myToggleReplaceHighlights.setIcon(myHighlightReplaceLines ? PluginIcons.Toggle_word_highlights : PluginIcons.No_word_highlights);
        myToggleReplaceHighlights.setSelected(myHighlightReplaceLines);

        if (myBatchTandemEdit) {
            mySearchViewPanel.setBorder(UIUtil.isUnderDarcula() ? myDarculaBorder : myDefaultBorder);
            myReplaceViewPanel.setBorder(UIUtil.isUnderDarcula() ? myDarculaBorder : myDefaultBorder);
            myOptionsViewPanel.setBorder(UIUtil.isUnderDarcula() ? myDarculaBorder : myDefaultBorder);
        } else {
            mySearchViewPanel.setBorder(myNoBorder);
            myReplaceViewPanel.setBorder(myNoBorder);
            myOptionsViewPanel.setBorder(myNoBorder);
        }
    }

    private boolean importFromJSON(final BatchSearchReplaceSettings settings, final BoxedJsObject presets) {
        boolean hadPreset = false;

        for (String presetName : presets.keySet()) {
            BoxedJsObject preset = presets.getJsObject(presetName);
            if (preset.isValid()) {
                StringBuilder search = new StringBuilder();
                StringBuilder replace = new StringBuilder();

                boolean hadSearch = false;
                for (String searchText : preset.keySet()) {
                    String replaceText = preset.getString(searchText);
                    search.append(searchText).append("\n");
                    replace.append(replaceText).append("\n");
                    hadSearch = true;
                }

                if (hadSearch) {
                    hadPreset = true;
                    BatchSearchReplace searchReplaceSettings = new BatchSearchReplace();
                    searchReplaceSettings.setSearchText(search.toString());
                    searchReplaceSettings.setReplaceText(replace.toString());
                    settings.getBatchPresets().put(presetName, searchReplaceSettings);
                }
            }
        }

        return hadPreset;
    }

    private void exportJSONPresets(final BoxedJsObject presets) {
        // we export presets only
        ArrayList<String> keySet = new ArrayList<>(mySettings.getBatchPresets().keySet());
        keySet.sort(Comparator.naturalOrder());

        for (String presetName : keySet) {
            BoxedJsObject preset = BoxedJson.of();
            presets.put(presetName, preset);

            BatchSearchReplace replaceSettings = mySettings.getPreset(presetName);
            String[] search = replaceSettings.getSearchText().split("\n");
            String[] replace = replaceSettings.getReplaceText().split("\n");
            String[] options = replaceSettings.getOptionsText().split("\n");

            int searchLines = search.length;
            int replaceLines = replace.length;
            int optionsLines = options.length;
            int iMax = Math.max(searchLines, Math.max(replaceLines, optionsLines));

            boolean hadErrors = false;
            for (int i = 0; i < iMax; i++) {
                String searchText = null;
                String replaceText = null;
                String optionsText = "";

                if (i < searchLines) searchText = search[i];
                if (i < replaceLines) replaceText = replace[i];
                if (i < optionsLines) optionsText = options[i];

                if (searchText != null && !searchText.isEmpty()) {
                    if (replaceText == null) {
                        // TODO: missing, use empty and highlight
                        replaceText = "";
                    }
                    preset.put(searchText, replaceText);
                } else {
                    if (replaceText != null && !replaceText.isEmpty()) {
                        // TODO: highlight as ignored
                        hadErrors = true;
                    }
                }
            }
        }
    }

    private void settingsChanged(final boolean loadPresets) {
        boolean savedInUpdate = myInUpdate;
        myInUpdate = true;
        // update dialog
        String searchText1 = mySettings.getBatchSearchReplace().getSearchText();
        String replaceText1 = mySettings.getBatchSearchReplace().getReplaceText();
        String optionsText1 = mySettings.getBatchSearchReplace().getOptionsText();

        WriteCommandAction.runWriteCommandAction(myProject, () -> {
            mySearchEditor.getDocument().setText(Utils.suffixWith(searchText1, "\n"));
            myReplaceEditor.getDocument().setText(Utils.suffixWith(replaceText1, "\n"));
            myOptionsEditor.getDocument().setText(Utils.suffixWith(optionsText1, "\n"));
        });

        boolean caseSensitive1 = mySettings.getBatchSearchReplace().isCaseSensitive();
        myCaseSensitive.setSelected(caseSensitive1);
        myWholeWord.setSelected(mySettings.getBatchSearchReplace().isWholeWord());

        if (loadPresets) {
            fillPresets();
        }

        myInUpdate = savedInUpdate;
        updateOptions(true);
    }

    private void sortDocument(boolean sortReplace, boolean sortDown) {
        if (myInUpdate) return;
        myInUpdate = true;

        DocumentEx searchEditorDocument = mySearchEditor.getDocument();
        CharSequence searchSequence = searchEditorDocument.getCharsSequence();
        DocumentEx replaceEditorDocument = myReplaceEditor.getDocument();
        CharSequence replaceSequence = replaceEditorDocument.getCharsSequence();
        DocumentEx optionEditorDocument = myOptionsEditor.getDocument();
        CharSequence optionSequence = optionEditorDocument.getCharsSequence();

        int searchLineCount = searchEditorDocument.getLineCount();
        int replaceLineCount = replaceEditorDocument.getLineCount();
        int optionsLineCount = optionEditorDocument.getLineCount();
        int iMax = Math.max(searchLineCount, Math.max(replaceLineCount, optionsLineCount));

        String[] searchLines = new String[iMax];
        String[] replaceLines = new String[iMax];
        String[] optionLines = new String[iMax];
        Integer[] orderLines = new Integer[iMax];

        for (int i = 0; i < iMax; i++) {
            String searchText = "";
            String replaceText = "";
            String optionsText = "";
            if (i < searchLineCount) {
                searchText = searchSequence.subSequence(searchEditorDocument.getLineStartOffset(i), searchEditorDocument.getLineEndOffset(i)).toString();
            }

            if (i < replaceLineCount) {
                replaceText = replaceSequence.subSequence(replaceEditorDocument.getLineStartOffset(i), replaceEditorDocument.getLineEndOffset(i)).toString();
            }

            if (i < optionsLineCount) {
                optionsText = optionSequence.subSequence(optionEditorDocument.getLineStartOffset(i), optionEditorDocument.getLineEndOffset(i)).toString();
            }

            searchLines[i] = searchText;
            replaceLines[i] = replaceText;
            optionLines[i] = optionsText;
            orderLines[i] = i;
        }

        boolean caseSensitive = myCaseSensitive.isSelected();

        Arrays.sort(orderLines, (o1, o2) -> {
            int result;
            if (sortReplace) {
                if (caseSensitive) {
                    result = replaceLines[o1].compareTo(replaceLines[o2]);
                } else {
                    result = replaceLines[o1].compareToIgnoreCase(replaceLines[o2]);
                    if (result == 0) {
                        result = replaceLines[o1].compareTo(replaceLines[o2]);
                    }
                }
            } else {
                if (caseSensitive) {
                    result = searchLines[o1].compareTo(searchLines[o2]);
                } else {
                    result = searchLines[o1].compareToIgnoreCase(searchLines[o2]);
                    if (result == 0) {
                        result = searchLines[o1].compareTo(searchLines[o2]);
                    }
                }
            }
            return sortDown ? -result : result;
        });

        // now we rebuild the editor text
        StringBuilder search = new StringBuilder(Math.max(searchSequence.length() + 1, iMax));
        StringBuilder replace = new StringBuilder(Math.max(replaceSequence.length() + 1, iMax));
        StringBuilder option = new StringBuilder(Math.max(optionSequence.length() + 1, iMax));

        for (int i = 0; i < iMax; i++) {
            String searchLine = searchLines[orderLines[i]];
            String replaceLine = replaceLines[orderLines[i]];
            String optionLine = optionLines[orderLines[i]];
            if (!(searchLine.isEmpty() && replaceLine.isEmpty())) {
                search.append(searchLine).append('\n');
                replace.append(replaceLine).append('\n');
                option.append(optionLine).append('\n');
            }
        }

        WriteCommandAction.runWriteCommandAction(myProject, () -> {
            searchEditorDocument.replaceString(0, searchSequence.length(), search);
            replaceEditorDocument.replaceString(0, replaceSequence.length(), replace);
            optionEditorDocument.replaceString(0, optionSequence.length(), option);
        });

        myInUpdate = false;
        updateOptions(true);
    }

    private void fillPresets() {
        //boolean savedInUpdate = myInUpdate;
        //myInUpdate = true;
        String presetName = mySettings.getBatchPresetName();
        myPresets.removeAllItems();
        ArrayList<String> presetNames = new ArrayList<>(mySettings.getBatchPresets().keySet());
        presetNames.sort(Comparator.naturalOrder());

        for (String item : presetNames) {
            myPresets.addItem(item);
        }

        if (presetName != null) {
            myPresets.setSelectedItem(presetName);
        } else {
            myPresets.setSelectedIndex(-1);
        }
        //myInUpdate = savedInUpdate;
    }

    private EditorEx createIdeaEditor(CharSequence charSequence) {
        Document doc = EditorFactory.getInstance().createDocument(charSequence);
        FileType fileType = FileTypeManager.getInstance().getStdFileType("text");
        Editor editor = EditorFactory.getInstance().createEditor(doc, myProject, fileType, false);
        editor.getSettings().setFoldingOutlineShown(false);
        editor.getSettings().setLineNumbersShown(false);
        editor.getSettings().setLineMarkerAreaShown(false);
        editor.getSettings().setIndentGuidesShown(false);
        return (EditorEx) editor;
    }

    private void updateOptions(final boolean searchReplaceTextChanged) {
        if (myEditor == null || myEditor.isDisposed()) return;

        if (searchReplaceTextChanged) myPendingForcedUpdate = true;

        if (myInUpdate) return;

        mySavePreset.setEnabled(!((String) myPresets.getEditor().getItem()).isEmpty());

        if (myEditor == null) {
            // disable everything
            myFindNext.setEnabled(false);
            myFindPrevious.setEnabled(false);
            myReplace.setEnabled(false);
            myReplaceAll.setEnabled(false);
            myExclude.setEnabled(false);
            myReset.setEnabled(false);
            myFoundRange = null;
        } else {
            // compare line count for search/replace, ignore options, ignore trailing empty lines, EOL is ignored
            myReset.setEnabled(myExcludedRanges != null);

            final CopyPasteManagerEx copyPasteManager = CopyPasteManagerEx.getInstanceEx();
            final Transferable[] contents = copyPasteManager.getAllContents();
            // NOTE: checking has tendency to hang the IDE, no harm in having button enabled
            boolean clipboardLoadEnabled = true;
//            if (contents.length > 1) {
//                // we take top two
//                try {
//                    final String replaceText = (String) contents[0].getTransferData(DataFlavor.stringFlavor);
//                    final String searchText = (String) contents[1].getTransferData(DataFlavor.stringFlavor);
//                    clipboardLoadEnabled = !replaceText.isEmpty() && !searchText.isEmpty();
//                } catch (UnsupportedFlavorException | IOException ignored) {
//                }
//            }

            myGetFromClipboard.setEnabled(clipboardLoadEnabled);

            myFoundRange = null;

            if (searchReplaceTextChanged || myPendingForcedUpdate) {
                myPendingForcedUpdate = false;

                DocumentEx searchEditorDocument = mySearchEditor.getDocument();
                CharSequence searchSequence = searchEditorDocument.getCharsSequence();
                DocumentEx replaceEditorDocument = myReplaceEditor.getDocument();
                CharSequence replaceSequence = replaceEditorDocument.getCharsSequence();
                DocumentEx optionsEditorDocument = myOptionsEditor.getDocument();
                CharSequence optionsSequence = optionsEditorDocument.getCharsSequence();

                myIndexedWordCounts = null;

                myEditorSearchHighlightProvider.enterUpdateRegion();
                mySearchHighlightProvider.enterUpdateRegion();
                myReplaceHighlightProvider.enterUpdateRegion();
                myOptionsHighlightProvider.enterUpdateRegion();

                myEditorSearchHighlightProvider.clearHighlights();
                mySearchHighlightProvider.clearHighlights();
                myReplaceHighlightProvider.clearHighlights();
                myOptionsHighlightProvider.clearHighlights();

                myWordIndexToLineMap = new HashMap<>();
                myLineSearchData = new HashMap<>();

                int searchLines = searchEditorDocument.getLineCount();
                int replaceLines = replaceEditorDocument.getLineCount();
                int optionsLines = optionsEditorDocument.getLineCount();
                int iMax = Math.max(searchLines, Math.max(replaceLines, optionsLines));
                ArrayList<SearchData> lineSearchData = new ArrayList<>();

                for (int i = 0; i < iMax; i++) {
                    String searchText = null;
                    String replaceText = null;
                    String optionsText = "";
                    if (i < searchLines) {
                        searchText = searchSequence.subSequence(searchEditorDocument.getLineStartOffset(i), searchEditorDocument.getLineEndOffset(i)).toString();
                    }

                    if (i < replaceLines) {
                        replaceText = replaceSequence.subSequence(replaceEditorDocument.getLineStartOffset(i), replaceEditorDocument.getLineEndOffset(i)).toString();
                    }

                    if (i < optionsLines) {
                        optionsText = optionsSequence.subSequence(optionsEditorDocument.getLineStartOffset(i), optionsEditorDocument.getLineEndOffset(i)).toString();
                    }

                    if (searchText != null && !searchText.isEmpty() && !optionsText.startsWith("-")) {
                        if (replaceText == null) {
                            // TODO: missing, use empty and highlight
                            replaceText = "";
                        }

                        boolean isCaseSensitive = myCaseSensitive.isSelected();
                        boolean isBeginWord = myWholeWord.isSelected();
                        boolean isEndWord = myWholeWord.isSelected();
                        boolean isError = false;
                        boolean isWarning = false;

                        // implement # marking start of comment
                        int iComment = optionsText.indexOf('#');
                        if (iComment == -1) iComment = optionsText.length();

                        int iC = optionsText.indexOf('c');
                        int iI = optionsText.indexOf('i');
                        int iW = optionsText.indexOf('w');
                        int iB = optionsText.indexOf('b');
                        int iE = optionsText.indexOf('e');
                        int iErr = optionsText.indexOf('!');
                        int iWarn = optionsText.indexOf('?');

                        if (iC >= 0 && iC < iComment) {
                            isCaseSensitive = true;
                        } else {
                            if (iI >= 0 && iI < iComment) {
                                isCaseSensitive = false;
                            }
                        }

                        if (iW >= 0 && iW < iComment) {
                            isBeginWord = true;
                            isEndWord = true;
                        }

                        if (iB >= 0 && iB < iComment) {
                            isBeginWord = true;
                        }

                        if (iE >= 0 && iE < iComment) {
                            isEndWord = true;
                        }

                        if (iErr >= 0 && iErr < iComment) {
                            isError = true;
                        }

                        if (iWarn >= 0 && iWarn < iComment) {
                            isWarning = true;
                        }

                        int ideHighlight = isError ? WordHighlightProvider.F_IDE_ERROR : isWarning ? WordHighlightProvider.F_IDE_WARNING : 0;
                        SearchData searchData = new SearchData(searchText, replaceText, i, myEditorSearchHighlightProvider.encodeFlags(isBeginWord, isEndWord, ideHighlight, isCaseSensitive));

                        int lMax = lineSearchData.size();
                        for (int l = lMax; l-- > 0; ) {
                            SearchData data = lineSearchData.get(l);
                            if (!BitFieldSet.any(data.flags, WordHighlightProvider.F_CASE_SENSITIVITY)) {
                                // case insensitive, we need to remove all that match
                                if (data.word.equals(searchData.word)) {
                                    // we need to delete this one
                                    lineSearchData.remove(l);
                                }
                            } else {
                                // case sensitive, we need to remove all that match
                                if (data.word.equalsIgnoreCase(searchData.word)) {
                                    // we need to delete this one
                                    lineSearchData.remove(l);
                                }
                            }
                        }

                        lineSearchData.add(searchData);
                    }
                }

                iMax = lineSearchData.size();
                for (int i = 0; i < iMax; i++) {
                    SearchData searchData = lineSearchData.get(i);
                    searchData.wordIndex = i;
                    myEditorSearchHighlightProvider.addHighlightRange(searchData.word, searchData.flags);
                    mySearchHighlightProvider.addHighlightLine(searchData.lineNumber);
                    myReplaceHighlightProvider.addHighlightLine(searchData.lineNumber);
                    myOptionsHighlightProvider.addHighlightLine(searchData.lineNumber);
                    myWordIndexToLineMap.put(i, searchData.lineNumber);
                    myLineSearchData.put(searchData.lineNumber, searchData);
                }

                boolean enabled = myEditorSearchHighlightProvider.getHighlightPattern() != null && !myEditorSearchHighlightProvider.getHighlightPattern().pattern().isEmpty();

                mySearchCopyRegEx.setEnabled(enabled);
                myFindNext.setEnabled(enabled);
                myFindPrevious.setEnabled(enabled);
                myReplaceAll.setEnabled(enabled);

                myFoundRange = null;

                myEditorSearchHighlightProvider.leaveUpdateRegion();
                mySearchHighlightProvider.leaveUpdateRegion();
                myReplaceHighlightProvider.leaveUpdateRegion();
                myOptionsHighlightProvider.leaveUpdateRegion();

                LineSelectionManager.getInstance(myEditor).updateHighlights();
            } else {
                WordHighlighter<?> highlighter = (WordHighlighter<?>) LineSelectionManager.getInstance(myEditor).getHighlighter();

                if (highlighter != null) {
                    int offset = myEditor.getCaretModel().getOffset();
                    RangeHighlighter rangeHighlighter = highlighter.getRangeHighlighter(offset);
                    myFoundRange = rangeHighlighter == null ? null : TextRange.create(rangeHighlighter.getStartOffset(), rangeHighlighter.getEndOffset());
                    myFoundIndex = myWordIndexToLineMap == null ? -1 : myWordIndexToLineMap.getOrDefault(highlighter.getOriginalIndex(rangeHighlighter), -1);
                }
            }
        }

        if (!myInUpdate) {
            updateRangeButtons();
            updateFoundRanges();
        }
    }

    private static class SearchData {
        final String word;
        final String replace;
        final int lineNumber;
        final int flags;
        int wordIndex;

        SearchData(final String word, final String replace, final int lineNumber, final int flags) {
            this.word = word;
            this.replace = replace;
            this.lineNumber = lineNumber;
            this.flags = flags;
        }
    }

    private void updateRangeButtons() {
        boolean isWritable = myEditor.getDocument().isWritable();
        myReplace.setEnabled(myFoundRange != null && isWritable);
        myExclude.setEnabled(myFoundRange != null && isWritable);

        if (myReplace.isEnabled() && myPendingReplace) {
            myPendingReplace = false;
            replace();
            return;
        }

        if (myExclude.isEnabled()) {
            String message;
            if (!isExcludedRange()) {
                message = Bundle.message("batch-search.exclude.label");
                myIsIncludeMode = false;
            } else {
                message = Bundle.message("batch-search.include.label");
                myIsIncludeMode = true;
            }

            String replace = message.replace("\u001B", "");
            if (!myExclude.getText().equals(replace)) {
                int pos = message.indexOf('\u001B');
                if (pos != -1) {
                    char shortCut = message.charAt(pos + 1);
                    myExclude.setMnemonic(shortCut);
                }
                myExclude.setText(replace);
            }
        }

        WordHighlighter<?> highlighter = myEditor == null ? null : (WordHighlighter<?>) LineSelectionManager.getInstance(myEditor).getHighlighter();
        if (highlighter != null) {
            if (myFoundRange != null) {
                myFindNext.setEnabled(highlighter.getNextRangeHighlighter(myFoundRange.getEndOffset()) != null);
                myFindPrevious.setEnabled(highlighter.getPreviousRangeHighlighter(myFoundRange.getStartOffset()) != null);
            } else {
                int offset = myEditor.getCaretModel().getOffset();
                myFindNext.setEnabled(highlighter.getNextRangeHighlighter(offset) != null);
                myFindPrevious.setEnabled(highlighter.getPreviousRangeHighlighter(offset) != null);
            }
        }

        myReplaceAll.setEnabled(isWritable && (myFindNext.isEnabled() || myFindPrevious.isEnabled() || myReplace.isEnabled()));
    }

    public static final String FIND_NEXT = "FIND_NEXT";
    public static final String FIND_PREV = "FIND_PREV";
    public static final String REPLACE = "REPLACE";
    public static final String REPLACE_ALL = "REPLACE_ALL";
    public static final String EXCLUDE = "EXCLUDE";
    public static final String INCLUDE = "INCLUDE";
    public static final String RESET = "RESET";
    public static final String TOGGLE_EXCLUDE_INCLUDE = "TOGGLE_EXCLUDE_INCLUDE";
    public static final String TOGGLE_SEARCH_HIGHLIGHT = "TOGGLE_SEARCH_HIGHLIGHT";
    public static final String TOGGLE_REPLACE_HIGHLIGHT = "TOGGLE_REPLACE_HIGHLIGHT";

    public boolean isActionEnabled(String action) {
        switch (action) {
            case FIND_NEXT:
                return myFindNext.isEnabled();
            case FIND_PREV:
                return myFindPrevious.isEnabled();
            case REPLACE:
                return true;
            case REPLACE_ALL:
                return myReplaceAll.isEnabled();
            case EXCLUDE:
                return myExclude.isEnabled() && !myIsIncludeMode;
            case INCLUDE:
                return myExclude.isEnabled() && myIsIncludeMode;
            case RESET:
                return myReset.isEnabled();
            case TOGGLE_EXCLUDE_INCLUDE:
                return myExclude.isEnabled();
            case TOGGLE_SEARCH_HIGHLIGHT:
                return myToggleSearchHighlights.isEnabled();
            case TOGGLE_REPLACE_HIGHLIGHT:
                return myToggleReplaceHighlights.isEnabled();
            default:
                return false;
        }
    }

    public void doAction(String action) {
        switch (action) {
            case FIND_NEXT:
                if (myFindNext.isEnabled()) findNext();
                break;
            case FIND_PREV:
                if (myFindPrevious.isEnabled()) findPrevious();
                break;
            case REPLACE:
                replace();
                break;
            case REPLACE_ALL:
                if (myReplaceAll.isEnabled()) replaceAll();
                break;
            case EXCLUDE:
                if (myExclude.isEnabled() && !myIsIncludeMode) exclude();
                break;
            case INCLUDE:
                if (myExclude.isEnabled() && myIsIncludeMode) exclude();
                break;
            case RESET:
                if (myReset.isEnabled()) reset();
                break;
            case TOGGLE_EXCLUDE_INCLUDE:
                if (myExclude.isEnabled()) exclude();
                break;
            case TOGGLE_SEARCH_HIGHLIGHT:
                if (myToggleSearchHighlights.isEnabled()) myHighlightSearchLines = !myHighlightSearchLines;
                break;
            case TOGGLE_REPLACE_HIGHLIGHT:
                if (myToggleReplaceHighlights.isEnabled()) myHighlightReplaceLines = !myHighlightReplaceLines;
                break;
            default:
        }
    }

    private void focusEditor() {
        if (myEditor != null && !myEditor.isDisposed()) {
            myEditor.getContentComponent().requestFocus();
        }
    }

    private void findNext() {
        if (myEditor == null) return;

        WordHighlighter<?> highlighter = (WordHighlighter<?>) LineSelectionManager.getInstance(myEditor).getHighlighter();
        myFoundBackwards = false;

        if (highlighter != null) {
            int offset = myFoundRange != null ? myFoundRange.getEndOffset() : myEditor.getCaretModel().getOffset();
            RangeHighlighter rangeHighlighter = highlighter.getNextRangeHighlighter(offset);
            myFoundRange = rangeHighlighter == null ? null : TextRange.create(rangeHighlighter.getStartOffset(), rangeHighlighter.getEndOffset());
            myFoundIndex = myWordIndexToLineMap == null ? -1 : myWordIndexToLineMap.getOrDefault(highlighter.getOriginalIndex(rangeHighlighter), -1);
        }

        if (myFoundRange != null && myEditor != null) {
            boolean savedInUpdate = myInUpdate;
            myInUpdate = true;
            myEditor.getCaretModel().getPrimaryCaret().moveToOffset(myFoundBackwards ? myFoundRange.getStartOffset() : myFoundRange.getEndOffset());
            EditHelpers.scrollToSelection(myEditor);
            myInUpdate = savedInUpdate;
        }

        updateFoundRanges();
        updateRangeButtons();
        focusEditor();
    }

    private void findPrevious() {
        if (myEditor == null) return;

        WordHighlighter<?> highlighter = (WordHighlighter<?>) LineSelectionManager.getInstance(myEditor).getHighlighter();
        myFoundBackwards = true;

        if (highlighter != null) {
            int offset = myFoundRange != null ? myFoundRange.getStartOffset() : myEditor.getCaretModel().getOffset();
            RangeHighlighter rangeHighlighter = highlighter.getPreviousRangeHighlighter(offset);
            myFoundRange = rangeHighlighter == null ? null : TextRange.create(rangeHighlighter.getStartOffset(), rangeHighlighter.getEndOffset());
            myFoundIndex = myWordIndexToLineMap == null ? -1 : myWordIndexToLineMap.getOrDefault(highlighter.getOriginalIndex(rangeHighlighter), -1);
        }

        if (myFoundRange != null && myEditor != null) {
            boolean savedInUpdate = myInUpdate;
            myInUpdate = true;
            myEditor.getCaretModel().getPrimaryCaret().moveToOffset(myFoundBackwards ? myFoundRange.getStartOffset() : myFoundRange.getEndOffset());
            EditHelpers.scrollToSelection(myEditor);
            myInUpdate = savedInUpdate;
        }

        updateFoundRanges();
        updateRangeButtons();
        focusEditor();
    }

    private void adjustExclusions(TextRange foundRange, int replacementLength) {
        if (myExcludedRanges != null) {
            int iMax = myExcludedRanges.size();
            int delta = replacementLength - foundRange.getLength();
            for (int i = 0; i < iMax; i++) {
                TextRange range = myExcludedRanges.get(i);
                if (range.getStartOffset() >= foundRange.getEndOffset()) {
                    // need adjustment
                    myExcludedRanges.set(i, range.shiftRight(delta));
                } else if (range.getEndOffset() >= foundRange.getEndOffset()) {
                    // need to adjust end
                    myExcludedRanges.set(i, range.grown(delta));
//                } else {
//                    // nothing to do
                }
            }
        }
    }

    private void replace() {
        if (myEditor == null || !myEditor.getDocument().isWritable()) return;

        boolean handled = false;

        if (myFoundRange != null && myFoundIndex != -1 && !isExcludedRange() && myLineSearchData != null) {
            // Need to double check that the range matches what was found, sometimes the update takes longer and the wrong text can be replaced
            int offset = myEditor.getCaretModel().getOffset();
            if (myFoundRange.getStartOffset() <= offset && offset <= myFoundRange.getEndOffset()) {
                String text = myFoundRange.subSequence(myEditor.getDocument().getCharsSequence()).toString();
                final SearchData searchData = myLineSearchData.get(myFoundIndex);
                String found = searchData.word;
                int caseSensitivity = searchData.flags & WordHighlightProvider.F_CASE_SENSITIVITY;

                if (caseSensitivity == WordHighlightProvider.F_CASE_SENSITIVE && text.equals(found)
                        || caseSensitivity == WordHighlightProvider.F_CASE_INSENSITIVE && text.equalsIgnoreCase(found)
                ) {
                    handled = true;
                    WriteCommandAction.runWriteCommandAction(myProject, () -> {
                        String replacement = searchData.replace;
                        myEditor.getDocument().replaceString(myFoundRange.getStartOffset(), myFoundRange.getEndOffset(), replacement);
                        addExclusion(); // we are replacing it, prevent double replacement
                        adjustExclusions(myFoundRange, replacement.length());

                        if (myFoundBackwards != null) {
                            if (myFoundBackwards) {
                                findPrevious();
                            } else {
                                findNext();
                            }
                        }
                    });
                }
            }
        }

        if (!handled) {
            // must be a click, we will set a pending replace and let it try again
            myPendingReplace = true;
            MiaCancelableJobScheduler.getInstance().schedule(250, () -> myPendingReplace = false);
        }
        focusEditor();
    }

    private void addExclusion() {
        if (myFoundRange != null && myFoundIndex != -1) {
            if (myExcludedRanges == null) {
                myExcludedRanges = new ArrayList<>();
            }
            myExcludedRanges.add(myFoundRange);
        }
    }

    private void exclude() {
        if (myEditor == null) return;

        if (myFoundRange != null && myFoundIndex != -1) {
            int i = getExcludedRange();
            if (i != -1) {
                // include
                myExcludedRanges.remove(i);
                if (myExcludedRanges.isEmpty()) myExcludedRanges = null;
            } else {
                addExclusion();
            }

            LineSelectionManager.getInstance(myEditor).updateHighlights();

            if (myFoundBackwards != null) {
                if (myFoundBackwards) {
                    findPrevious();
                } else {
                    findNext();
                }
            } else {
                updateRangeButtons();
            }
        }
        focusEditor();
    }

    private void reset() {
        if (myEditor == null) return;

        if (myExcludedRanges != null) {
            myExcludedRanges = null;
            updateOptions(false);
            LineSelectionManager.getInstance(myEditor).updateHighlights();
        }
        focusEditor();
    }

    private boolean isExcludedRange() {
        return getExcludedRange() != -1;
    }

    private int getExcludedRange() {
        if (myExcludedRanges != null) {
            int iMax = myExcludedRanges.size();
            for (int i = 0; i < iMax; i++) {
                if (myExcludedRanges.get(i).intersects(myFoundRange.getStartOffset(), myFoundRange.getEndOffset())) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void replaceAll() {
        if (myEditor == null) return;

        if (!myEditorSearchHighlightProvider.getHighlightPattern().pattern().isEmpty()) {
            WriteCommandAction.runWriteCommandAction(myProject, () -> {
                int caretOffset = myEditor.getDocument().getTextLength();
                //myEditor.getCaretModel().getPrimaryCaret().setSelection(length, length);
                //myEditor.getCaretModel().getPrimaryCaret().moveToOffset(length);

                WordHighlighter<?> highlighter = (WordHighlighter<?>) LineSelectionManager.getInstance(myEditor).getHighlighter();
                myFoundBackwards = true;

                if (highlighter != null) {
                    // rolled in find prev code to speed things up since we do not need highlight updates in the loop
                    while (true) {
                        RangeHighlighter rangeHighlighter = highlighter.getPreviousRangeHighlighter(caretOffset);
                        myFoundRange = rangeHighlighter == null ? null : TextRange.create(rangeHighlighter.getStartOffset(), rangeHighlighter.getEndOffset());
                        myFoundIndex = myWordIndexToLineMap == null ? -1 : myWordIndexToLineMap.getOrDefault(highlighter.getOriginalIndex(rangeHighlighter), -1);

                        if (myFoundRange != null && myFoundIndex != -1 && myLineSearchData != null) {
                            if (isExcludedRange()) {
                                caretOffset = myFoundRange.getStartOffset();
                                continue;
                            }
                            String replacement = myLineSearchData.get(myFoundIndex).replace;
                            myEditor.getDocument().replaceString(myFoundRange.getStartOffset(), myFoundRange.getEndOffset(), replacement);
                            adjustExclusions(myFoundRange, replacement.length());
                            caretOffset = myFoundRange.getStartOffset();
                        } else {
                            break;
                        }
                    }
                }

                //myEditor.getCaretModel().getPrimaryCaret().moveToOffset(caretOffset);

                updateFoundRanges();
                updateRangeButtons();
            });
        }
        focusEditor();
    }

    private static void ignoreErrors(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable ignored) {

        }
    }

    private void copyEditorSettings(EditorEx editor) {
        if (myEditor == null) return;

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
        EditorSettings myViewerSettings = editor.getSettings();
        EditorSettings myEditorSettings = myEditor.getSettings();
        Project myEditorProject = myProject;

        // @formatter:off
        ignoreErrors(() -> myViewerSettings.setRightMarginShown(myEditorSettings.isRightMarginShown()));
        ignoreErrors(() -> myViewerSettings.setGutterIconsShown(myEditorSettings.areGutterIconsShown()));
        ignoreErrors(() -> myViewerSettings.setAdditionalPageAtBottom(myEditorSettings.isAdditionalPageAtBottom()));
        ignoreErrors(() -> myViewerSettings.setAllowSingleLogicalLineFolding(myEditorSettings.isAllowSingleLogicalLineFolding()));
        ignoreErrors(() -> myViewerSettings.setAnimatedScrolling(myEditorSettings.isAnimatedScrolling()));
        ignoreErrors(() -> myViewerSettings.setAutoCodeFoldingEnabled(myEditorSettings.isAutoCodeFoldingEnabled()));
        ignoreErrors(() -> myViewerSettings.setBlinkCaret(myEditorSettings.isBlinkCaret()));
        ignoreErrors(() -> myViewerSettings.setBlockCursor(myEditorSettings.isBlockCursor()));
        ignoreErrors(() -> myViewerSettings.setCamelWords(myEditorSettings.isCamelWords()));
        ignoreErrors(() -> myViewerSettings.setCaretInsideTabs(myEditorSettings.isCaretInsideTabs()));
        ignoreErrors(() -> myViewerSettings.setCaretRowShown(myEditorSettings.isCaretRowShown()));
        ignoreErrors(() -> myViewerSettings.setDndEnabled(myEditorSettings.isDndEnabled()));
        ignoreErrors(() -> myViewerSettings.setFoldingOutlineShown(myEditorSettings.isFoldingOutlineShown()));
        ignoreErrors(() -> myViewerSettings.setIndentGuidesShown(myEditorSettings.isIndentGuidesShown()));
        ignoreErrors(() -> myViewerSettings.setInnerWhitespaceShown(myEditorSettings.isInnerWhitespaceShown()));
        ignoreErrors(() -> myViewerSettings.setLeadingWhitespaceShown(myEditorSettings.isLeadingWhitespaceShown()));
        //ignoreErrors(() -> myViewerSettings.setLineMarkerAreaShown(myEditorSettings.isLineMarkerAreaShown()));
        ignoreErrors(() -> myViewerSettings.setLineMarkerAreaShown(false));
        ignoreErrors(() -> myViewerSettings.setLineNumbersShown(myEditorSettings.isLineNumbersShown()));
        ignoreErrors(() -> myViewerSettings.setMouseClickSelectionHonorsCamelWords(myEditorSettings.isMouseClickSelectionHonorsCamelWords()));
        ignoreErrors(() -> myViewerSettings.setRefrainFromScrolling(myEditorSettings.isRefrainFromScrolling()));
        ignoreErrors(() -> myViewerSettings.setSmartHome(myEditorSettings.isSmartHome()));
        ignoreErrors(() -> myViewerSettings.setTrailingWhitespaceShown(myEditorSettings.isTrailingWhitespaceShown()));
        ignoreErrors(() -> myViewerSettings.setUseCustomSoftWrapIndent(myEditorSettings.isUseCustomSoftWrapIndent()));
        ignoreErrors(() -> myViewerSettings.setUseSoftWraps(myEditorSettings.isUseSoftWraps()));
        //ignoreErrors(() -> myViewerSettings.setVariableInplaceRenameEnabled(myEditorSettings.isVariableInplaceRenameEnabled()));
        ignoreErrors(() -> myViewerSettings.setVirtualSpace(myEditorSettings.isVirtualSpace()));
        ignoreErrors(() -> myViewerSettings.setWheelFontChangeEnabled(myEditorSettings.isWheelFontChangeEnabled()));
        ignoreErrors(() -> myViewerSettings.setWhitespacesShown(myEditorSettings.isWhitespacesShown()));
        //ignoreErrors(() -> myViewerSettings.setAdditionalColumnsCount(myEditorSettings.getAdditionalColumnsCount()));
        //ignoreErrors(() -> myViewerSettings.setAdditionalLinesCount(myEditorSettings.getAdditionalLinesCount()));
        ignoreErrors(() -> myViewerSettings.setCaretBlinkPeriod(myEditorSettings.getCaretBlinkPeriod()));
        ignoreErrors(() -> myViewerSettings.setCustomSoftWrapIndent(myEditorSettings.getCustomSoftWrapIndent()));
        ignoreErrors(() -> myViewerSettings.setLineCursorWidth(myEditorSettings.getLineCursorWidth()));
        // @formatter:on

        //boolean isUseTabCharacter(Project project); void setUseTabCharacter(boolean useTabCharacter);
        //boolean isWrapWhenTypingReachesRightMargin(Project project); void setWrapWhenTypingReachesRightMargin(boolean val);
        //int getRightMargin(Project project); void setRightMargin(int myRightMargin);
        //int getTabSize(Project project); void setTabSize(int tabSize);
        // @formatter:off
        ignoreErrors(() -> myViewerSettings.setUseTabCharacter(myEditorSettings.isUseTabCharacter(myEditorProject)));
        //ignoreErrors(() -> myViewerSettings.setWrapWhenTypingReachesRightMargin(myEditorSettings.isWrapWhenTypingReachesRightMargin(myEditorProject)));
        ignoreErrors(() -> myViewerSettings.setRightMargin(myEditorSettings.getRightMargin(myEditorProject)));
        ignoreErrors(() -> myViewerSettings.setTabSize(myEditorSettings.getTabSize(myEditorProject)));
        // @formatter:on
    }

    public JComponent getMainPanel() {
        return myMainPanel;
    }

    private void updateFoundRange(final EditorEx editor) {
        if (myFoundRange != null && myFoundIndex >= 0 && myFoundIndex < editor.getDocument().getLineCount()) {
            // highlight search and replace and options strings
            int offset = editor.getDocument().getLineEndOffset(myFoundIndex);
            editor.getCaretModel().getPrimaryCaret().moveToOffset(offset);
        } else {
            int offset = editor.getCaretModel().getOffset();
            editor.getCaretModel().getPrimaryCaret().setSelection(offset, offset);
        }
    }

    private void updateFoundRanges() {
        boolean savedInUpdate = myInUpdate;
        myInUpdate = true;

        if (myEditor == null) {
            myIndexedWordCounts = new int[0];
            mySearchHighlightProvider.clearHighlights();
            myReplaceHighlightProvider.clearHighlights();
            myOptionsHighlightProvider.clearHighlights();
        } else {
            LineSelectionManager selectionManager = LineSelectionManager.getInstance(myEditor);
            selectionManager.updateHighlights();
            Highlighter<ApplicationSettings> highlighter = selectionManager.getHighlighter();
            if (highlighter instanceof WordHighlighter) {
                myIndexedWordCounts = ((WordHighlighter<ApplicationSettings>) highlighter).getIndexedRangeCounts();
            }

            updateFoundRange(mySearchEditor);
            updateFoundRange(myReplaceEditor);
            updateFoundRange(myOptionsEditor);

            updateLastEditorSync(null);
            EditHelpers.scrollToSelection(mySearchEditor);
            EditHelpers.scrollToSelection(myReplaceEditor);
            EditHelpers.scrollToSelection(myOptionsEditor);

            LineSelectionManager.getInstance(mySearchEditor).updateHighlights();
            LineSelectionManager.getInstance(myReplaceEditor).updateHighlights();
            LineSelectionManager.getInstance(myOptionsEditor).updateHighlights();
        }

        myInUpdate = savedInUpdate;
    }

    private static void adjustOtherEditorCaretLine(final int caretLine, final EditorEx editor) {
        if (caretLine < editor.getDocument().getLineCount()) {
            editor.getCaretModel().getPrimaryCaret().moveToOffset(editor.getDocument().getLineStartOffset(caretLine));
        }
    }

    private static void adjustOtherEditorScrollOffset(final int scrollOffset, final EditorEx editor) {
        editor.getScrollingModel().scrollVertically(scrollOffset);
    }

    private class EditorCaretListener implements CaretListener {
        @Override
        public void caretPositionChanged(final CaretEvent e) {
            Editor caretEditor = e.getEditor();
            if (!myInUpdate && canSyncEditors(caretEditor)) {
                updateLastEditorSync(caretEditor);
                myInUpdate = true;
                try {
                    Caret caret = caretEditor.getCaretModel().getPrimaryCaret();
                    int caretLine = caretEditor.getDocument().getLineNumber(caret.getOffset());

                    if (mySearchEditor != caretEditor) adjustOtherEditorCaretLine(caretLine, mySearchEditor);
                    if (myReplaceEditor != caretEditor) adjustOtherEditorCaretLine(caretLine, myReplaceEditor);
                    if (myOptionsEditor != caretEditor) adjustOtherEditorCaretLine(caretLine, myOptionsEditor);
                } finally {
                    myInUpdate = false;
                }
            }
        }
    }

    private class EditorVisibleAreaListener implements VisibleAreaListener {
        @Override
        public void visibleAreaChanged(final VisibleAreaEvent e) {
            Editor visibleAreaEditor = e.getEditor();
            if (!myInUpdate && canSyncEditors(visibleAreaEditor)) {
                updateLastEditorSync(visibleAreaEditor);
                myInUpdate = true;
                try {
                    int scrollOffset = visibleAreaEditor.getScrollingModel().getVerticalScrollOffset();

                    if (mySearchEditor != visibleAreaEditor) adjustOtherEditorScrollOffset(scrollOffset, mySearchEditor);
                    if (myReplaceEditor != visibleAreaEditor) adjustOtherEditorScrollOffset(scrollOffset, myReplaceEditor);
                    if (myOptionsEditor != visibleAreaEditor) adjustOtherEditorScrollOffset(scrollOffset, myOptionsEditor);
                } finally {
                    myInUpdate = false;
                }
            }
        }
    }

    private static void replicateLineChange(final Document document, int lineNumber, int lineDelta, boolean startOfLine) {
        int lineCount = document.getLineCount();
        if (lineDelta > 0) {
            // inserted
            if (lineNumber >= lineCount) {
                // insert extra lines to make up
                lineDelta += lineNumber - lineCount;
                lineNumber = lineCount - 1;
                if (lineNumber < 0) {
                    //lineDelta += lineNumber;
                    lineNumber = 0;
                }
            }

            int offset = startOfLine ? document.getLineStartOffset(lineNumber) : document.getLineEndOffset(lineNumber);
            document.insertString(offset, RepeatedSequence.repeatOf('\n', lineDelta));
        } else {
            if (lineCount > 0 && lineNumber <= lineCount) {
                // have something to delete
                if (lineNumber - lineDelta >= lineCount) {
                    lineDelta = lineNumber - lineCount;
                    if (lineNumber >= 0) {
                        int offset = lineNumber > 0 ? document.getLineEndOffset(lineNumber - 1) : 0;
                        int endOffset = document.getLineEndOffset(lineNumber - lineDelta - 1) + 1;
                        if (endOffset > document.getTextLength()) endOffset = document.getTextLength();

                        document.deleteString(offset, endOffset);
                    }
                } else {
                    int offset = document.getLineStartOffset(lineNumber);
                    int endOffset = document.getLineEndOffset(lineNumber - lineDelta - 1) + 1;
                    if (endOffset > document.getTextLength()) endOffset = document.getTextLength();

                    document.deleteString(offset, endOffset);
                }
            }
        }
    }

    private static int countOccurrences(CharSequence charSequence, char c) {
        int iMax = charSequence.length();
        int occurrences = 0;
        for (int i = 0; i < iMax; i++) {
            if (charSequence.charAt(i) == c) {
                occurrences++;
            }
        }
        return occurrences;
    }

    private void replicateEdit(final DocumentEvent event) {
        // figure out if lines were added/removed and replicate to other editors
        if (event.isWholeTextReplaced()) {
            // here we turn off tandem edit mode
            myBatchTandemEdit = false;
        } else {
            CharSequence beforeSeq = event.getOldFragment();
            CharSequence afterSeq = event.getNewFragment();
            int beforeLines = countOccurrences(beforeSeq, '\n');
            int afterLines = countOccurrences(afterSeq, '\n');
            Document editedDocument = event.getDocument();

            if (beforeLines != afterLines || editedDocument.getTextLength() == 0) {
                // need to adjust
                WriteCommandAction.runWriteCommandAction(myProject, () -> {
                    int line = editedDocument.getLineNumber(event.getOffset());
                    int lineDelta = afterLines - beforeLines;
                    boolean startOfLine = event.getOffset() == editedDocument.getLineStartOffset(line);
                    if (editedDocument.getTextLength() > 0 && lineDelta < 0 && beforeSeq.length() == 1 && event.getOffset() == editedDocument.getTextLength()) line++;
                    if (editedDocument != mySearchEditor.getDocument()) replicateLineChange(mySearchEditor.getDocument(), line, lineDelta, startOfLine);
                    if (editedDocument != myReplaceEditor.getDocument()) replicateLineChange(myReplaceEditor.getDocument(), line, lineDelta, startOfLine);
                    if (editedDocument != myOptionsEditor.getDocument()) replicateLineChange(myOptionsEditor.getDocument(), line, lineDelta, startOfLine);
                });
            }
        }
    }

    private class EditorDocumentListener implements DocumentListener {
        @Override
        public void documentChanged(@NotNull final DocumentEvent event) {
            if (myBatchTandemEdit) {
                if (!myInTandemEdit && !myInUpdate) {
                    // see if undo in progress
                    UndoManager undoManager = UndoManager.getInstance(myProject);
                    if (!undoManager.isUndoInProgress() && !undoManager.isRedoInProgress()) {
                        try {
                            myInTandemEdit = true;
                            myInUpdate = true;
                            // need to replicate line inserts/deletes to other editors
                            replicateEdit(event);
                        } finally {
                            myInTandemEdit = false;
                            myInUpdate = false;
                        }
                        updateOptions(true);
                    } else {
                        updateOptions(true);
                    }
                }
            } else {
                updateOptions(true);
            }
        }
    }

    private String checkRegEx(final String pattern) {
        final String patternText = pattern.trim();
        //Color validBackground = getValidTextFieldBackground();
        //Color selectedBackground = getSelectedTextFieldBackground();
        //Color invalidBackground = getInvalidTextFieldBackground();
        Color warningBackground = getWarningTextFieldBackground();
        String error = "";
        String warning = "";

        Pattern regexPattern;
        Matcher matcher = null;
        boolean isBadRegEx = false;

        if (!patternText.isEmpty()) {
            try {
                regexPattern = Pattern.compile(patternText);
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                error = e.getMessage();
                isBadRegEx = true;
            }
        } else {
            error = "empty pattern";
        }

        if (!isBadRegEx) {
            mySearchViewPanel.setVisible(true);
            myTextPane.setVisible(false);
        } else {
            mySearchViewPanel.setVisible(false);
            HtmlHelpers.setRegExError(error, myTextPane, mySampleText.getFont(), getValidTextFieldBackground(), getWarningTextFieldBackground());
        }
        return error;
    }

    private class SearchWordHighlighterProvider extends MiaWordHighlightProviderImpl {
        SearchWordHighlighterProvider(@NotNull final ApplicationSettings settings) {
            super(settings);
        }

        @Override
        public WordHighlighter<ApplicationSettings> getHighlighter(@NotNull final Editor editor) {
            return new SearchWordHighlighter(this, editor);
        }
    }

    private class EditorLineHighlighterProvider extends MiaLineHighlightProviderImpl {
        EditorLineHighlighterProvider(@NotNull final ApplicationSettings settings) {
            super(settings);
        }

        @Override
        public boolean isShowHighlights() {
            return isHighlightsMode();
        }

        @Override
        public LineHighlighter<ApplicationSettings> getHighlighter(@NotNull final Editor editor) {
            return new EditorLineHighlighter(this, editor);
        }
    }

    private class SearchWordHighlighter extends WordHighlighter<ApplicationSettings> {
        SearchWordHighlighter(@NotNull WordHighlightProvider<ApplicationSettings> highlightProvider, @NotNull final Editor editor) {
            super(highlightProvider, editor);
        }

        @Override
        public TextAttributes getAttributes(@Nullable final TextAttributes attributes, final String word, final int startOffset, final int endOffset) {
            if (attributes != null) {
                if (myExcludedRanges != null) {
                    for (TextRange range : myExcludedRanges) {
                        if (range.containsRange(startOffset, endOffset)) {
                            EditorColorsScheme uiTheme = EditorColorsManager.getInstance().getGlobalScheme();
                            Color foreground = uiTheme.getDefaultForeground();
                            return new TextAttributes(attributes.getForegroundColor(), attributes.getBackgroundColor(), foreground, EffectType.STRIKEOUT, attributes.getFontType());
                        }
                    }
                }
                if (myFoundRange != null && myFoundRange.containsRange(startOffset, endOffset)) {
                    EditorColorsScheme uiTheme = EditorColorsManager.getInstance().getGlobalScheme();
                    Color foreground = uiTheme.getDefaultForeground();
                    return new TextAttributes(attributes.getForegroundColor(), attributes.getBackgroundColor(), foreground, EffectType.BOXED, attributes.getFontType());
                }
            }
            return attributes;
        }
    }

    private class EditorLineHighlighter extends LineHighlighter<ApplicationSettings> {
        EditorLineHighlighter(@NotNull LineHighlightProvider<ApplicationSettings> highlightProvider, @NotNull final Editor editor) {
            super(highlightProvider, editor);
        }

        @Override
        public RangeHighlighter rangeHighlighterCreated(final RangeHighlighter rangeHighlighter, final int line, final int index, final int startOffset, final int endOffset) {
            //int count = 0;
            //SearchData searchData = myLineSearchData.get(line);
            //if (searchData != null && myIndexedWordCounts != null && searchData.wordIndex < myIndexedWordCounts.length) {
            //    count =  myIndexedWordCounts[searchData.wordIndex];
            //}
            //
            //if (count > 0) {
            //    rangeHighlighter.setErrorStripeTooltip("tooltip");
            //}
            return rangeHighlighter;
        }

        @Override
        public TextAttributes getAttributes(@Nullable TextAttributes attributes, final int line, final int startOffset, final int endOffset) {
            Color effectColor = null;
            EffectType effectType = null;
            boolean selectedLine = false;

            if (myFoundIndex == line) {
                EditorColorsScheme uiTheme = EditorColorsManager.getInstance().getGlobalScheme();
                effectColor = uiTheme.getDefaultForeground();
                effectType = EffectType.BOXED;
                selectedLine = true;
            }

            if (attributes == null && BatchReplaceForm.this.myEditor != null) {
                // not used, overridden
                TextAttributesKey attributesKey = CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES;
                EditorColorsScheme uiTheme = EditorColorsManager.getInstance().getGlobalScheme();
                attributes = uiTheme.getAttributes(attributesKey);
                return new TextAttributes(attributes.getForegroundColor(), attributes.getBackgroundColor(), attributes.getForegroundColor(), EffectType.WAVE_UNDERSCORE, attributes.getFontType());
            } else {
                int count = 0;
                SearchData searchData = myLineSearchData == null ? null : myLineSearchData.get(line);
                if (searchData != null && myIndexedWordCounts != null && searchData.wordIndex < myIndexedWordCounts.length) {
                    count = myIndexedWordCounts[searchData.wordIndex];
                }

                if (count == 0) {
                    TextAttributesKey attributesKey = CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES;
                    EditorColorsScheme uiTheme = EditorColorsManager.getInstance().getGlobalScheme();
                    attributes = uiTheme.getAttributes(attributesKey);
                } else if ((selectedLine || myHighlightSearchLines) && attributes != null) {
                    // see if error or warning highlight
                    int flags = searchData.flags;
                    TextAttributes ideAttributes = TypedRangeHighlightProviderBase.getIdeAttributes(flags);
                    if (ideAttributes != null) {
                        attributes = ideAttributes;
                    } else {
                        attributes = new TextAttributes(
                                attributes.getForegroundColor(),
                                attributes.getBackgroundColor(),
                                effectColor,
                                effectType,
                                0
                        );
                    }
                } else {
                    attributes = null;
                }
            }

            return attributes;
        }
    }
}
