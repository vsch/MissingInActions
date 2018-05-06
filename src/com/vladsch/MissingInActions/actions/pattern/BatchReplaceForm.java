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

import com.intellij.ide.CopyPasteManagerEx;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.ex.FileChooserDialogImpl;
import com.intellij.openapi.fileChooser.ex.FileSaverDialogImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.UIUtil;
import com.vladsch.MissingInActions.Bundle;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.settings.BatchSearchReplace;
import com.vladsch.MissingInActions.settings.BatchSearchReplaceSettings;
import com.vladsch.MissingInActions.util.AwtRunnable;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.MissingInActions.util.OneTimeRunnable;
import com.vladsch.MissingInActions.util.Utils;
import com.vladsch.MissingInActions.util.highlight.*;
import com.vladsch.MissingInActions.util.ui.BackgroundColor;
import com.vladsch.boxed.json.BoxedJsObject;
import com.vladsch.boxed.json.BoxedJson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vladsch.MissingInActions.util.highlight.WordHighlightProvider.CASE_INSENSITIVE;

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
    private JBCheckBox myShowHighlights;
    private final JBPopupMenu myPopupMenuActions;

    EditorEx myEditor;
    EditorEx mySearchEditor;
    EditorEx myReplaceEditor;
    EditorEx myOptionsEditor;

    final ApplicationSettings mySettings;

    HashMap<Integer, Integer> myWordIndexToLineMap = null;
    HashMap<Integer, SearchData> myLineSearchData = null;
    int[] myIndexedWordCounts = null;
    TextRange myFoundRange = null;
    boolean myHighlightAllLines = false;

    ArrayList<TextRange> myExcludedRanges = null;
    WordHighlightProvider myEditorSearchHighlightProvider;
    LineHighlightProvider mySearchHighlightProvider;
    LineHighlightProvider myReplaceHighlightProvider;
    LineHighlightProvider myOptionsHighlightProvider;

    final DocumentListener myDocumentListener;
    final CaretListener myCaretListener;
    final VisibleAreaListener myVisibleAreaListener;
    final CaretListener myEditorCaretListener;
    final HighlightListener myHighlightListener;
    final Project myProject;

    int myFoundIndex = -1;
    Boolean myFoundBackwards = null;
    boolean myInUpdate = false;
    long myLastEditorSync = Long.MIN_VALUE;
    Editor myLastSyncEditor = null;
    boolean myIsActive = false;
    OneTimeRunnable myHighlightRunner = OneTimeRunnable.NULL;
    boolean myPendingForcedUpdate = false;

    void updateLastEditorSync(Editor editor) {
        myLastEditorSync = System.currentTimeMillis();
        myLastSyncEditor = editor;
    }

    boolean canSyncEditors(Editor editor, long delayMillis) {
        return editor == myLastSyncEditor || myLastEditorSync + delayMillis < System.currentTimeMillis();
    }

    BackgroundColor getInvalidTextFieldBackground() {
        return BackgroundColor.of(Utils.errorColor(UIUtil.getTextFieldBackground()));
    }

    BackgroundColor getWarningTextFieldBackground() {
        return BackgroundColor.of(Utils.warningColor(UIUtil.getTextFieldBackground()));
    }

    BackgroundColor getValidTextFieldBackground() {
        return BackgroundColor.of(UIUtil.getTextFieldBackground());
    }

    BackgroundColor getSelectedTextFieldBackground() {
        return BackgroundColor.of(mySampleText.getSelectionColor());
    }

    @Override
    public void dispose() {
        disposeEditors();
    }

    public void saveSettings() {
        mySettings.getBatchSearchReplace().setWholeWord(myWholeWord.isSelected());
        mySettings.getBatchSearchReplace().setCaseSensitive(myCaseSensitive.isSelected());
        mySettings.getBatchSearchReplace().setSearchText(mySearchEditor.getDocument().getText().trim());
        mySettings.getBatchSearchReplace().setReplaceText(myReplaceEditor.getDocument().getText().trim());
        mySettings.getBatchSearchReplace().setOptionsText(myOptionsEditor.getDocument().getText().trim());
        mySettings.setBatchHighlightAllLines(myShowHighlights.isSelected());
    }

    public void disposeEditors() {
        if (mySearchEditor != null) {
            // release the editors
            setActiveEditor(null);

            mySearchEditor.getDocument().removeDocumentListener(myDocumentListener);
            myReplaceEditor.getDocument().removeDocumentListener(myDocumentListener);
            myOptionsEditor.getDocument().removeDocumentListener(myDocumentListener);

            mySearchEditor.getCaretModel().removeCaretListener(myCaretListener);
            myReplaceEditor.getCaretModel().removeCaretListener(myCaretListener);
            myOptionsEditor.getCaretModel().removeCaretListener(myCaretListener);

            mySearchEditor.getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener);
            myReplaceEditor.getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener);
            myOptionsEditor.getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener);

            LineSelectionManager.getInstance(mySearchEditor).setHighlightProvider(null);
            LineSelectionManager.getInstance(myReplaceEditor).setHighlightProvider(null);
            LineSelectionManager.getInstance(myOptionsEditor).setHighlightProvider(null);

            EditorFactory.getInstance().releaseEditor(mySearchEditor);
            EditorFactory.getInstance().releaseEditor(myReplaceEditor);
            EditorFactory.getInstance().releaseEditor(myOptionsEditor);

            myEditor = null;
            mySearchEditor = null;
            myReplaceEditor = null;
            myOptionsEditor = null;

            myEditorSearchHighlightProvider.removeHighlightListener(myHighlightListener);
            myEditorSearchHighlightProvider.disposeComponent();
            mySearchHighlightProvider.disposeComponent();
            myReplaceHighlightProvider.disposeComponent();
            myOptionsHighlightProvider.disposeComponent();

            myEditorSearchHighlightProvider = null;
            mySearchHighlightProvider = null;
            myReplaceHighlightProvider = null;
            myOptionsHighlightProvider = null;
        }
    }

    @SuppressWarnings("VariableNotUsedInsideIf")
    public void setActiveEditor(@Nullable EditorEx editor) {
        if (myEditor != editor) {
            myExcludedRanges = null;

            if (myEditor != null && !myEditor.isDisposed()) {
                myEditor.getCaretModel().removeCaretListener(myEditorCaretListener);
                LineSelectionManager.getInstance(myEditor).setHighlightProvider(null);
            }

            myEditor = editor;

            if (myEditor != null) {
                copyEditorSettings(mySearchEditor);
                copyEditorSettings(myReplaceEditor);
                copyEditorSettings(myOptionsEditor);
                myEditor.getCaretModel().addCaretListener(myEditorCaretListener);

                if (myIsActive) {
                    LineSelectionManager.getInstance(myEditor).setHighlightProvider(myEditorSearchHighlightProvider);
                }
            }

            updateOptions(true);
        }
    }

    private class MainEditorCaretListener implements CaretListener {
        @Override
        public void caretPositionChanged(final CaretEvent e) {
            myHighlightRunner.cancel();

            if (!myInUpdate) {
                myFoundBackwards = null;

                myHighlightRunner = OneTimeRunnable.schedule(100, new AwtRunnable(true, () -> {
                    updateOptions(false);
                }));
            }
        }

        @Override
        public void caretAdded(final CaretEvent e) {

        }

        @Override
        public void caretRemoved(final CaretEvent e) {

        }
    }

    private class MainEditorHighlightListener implements HighlightListener {
        @Override
        public void highlightsChanged() {

        }

        @Override
        public void highlightsUpdated() {
            if (!myInUpdate && myEditor != null) {
                Highlighter highlighter = LineSelectionManager.getInstance(myEditor).getHighlighter();
                if (highlighter instanceof WordHighlighter) {
                    myIndexedWordCounts = ((WordHighlighter) highlighter).getIndexedWordCounts();
                }
                LineSelectionManager.getInstance(mySearchEditor).updateHighlights();
                LineSelectionManager.getInstance(myReplaceEditor).updateHighlights();
                LineSelectionManager.getInstance(myOptionsEditor).updateHighlights();
            }
        }
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
        myShowHighlights.setSelected(mySettings.isBatchHighlightAllLines());

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

        myShowHighlights.addActionListener(e -> {
            myHighlightAllLines = myShowHighlights.isSelected();
            updateFoundRanges();
        });
        myHighlightAllLines = myShowHighlights.isSelected();

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

        //myPresets.addItemListener(e -> {
        //    int tmp = 0;
        //});

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
                } catch (UnsupportedFlavorException | IOException e1) {

                }
            }
        });

        myCaseSensitive.addActionListener(actionListener);
        myWholeWord.addActionListener(actionListener);

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

        exportXML.addActionListener(e -> {
            String title = Bundle.message("batch-search.export.title");
            String description = Bundle.message("batch-search.export.description");
            FileSaverDescriptor fileSaverDescriptor = new FileSaverDescriptor(title, description, "xml");
            FileSaverDialogImpl saveDialog = new FileSaverDialogImpl(fileSaverDescriptor, myMainPanel);
            if (myProject != null) {
                VirtualFileWrapper file = saveDialog.save(myProject.getBaseDir(), "batch-search-replace.xml");
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
            if (lastImportFile == null) lastImportFile = myProject.getBaseDir();

            VirtualFile[] files = fileChooserDialog.choose(myProject, lastImportFile);
            if (files.length > 0) {
                try {
                    BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(files[0].getPath()));
                    XMLDecoder decoder = new XMLDecoder(inputStream, this, e1 -> {
                        e1.printStackTrace();
                    }, this.getClass().getClassLoader());

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
            if (myProject != null) {
                VirtualFileWrapper file = saveDialog.save(myProject.getBaseDir(), "batch-search-replace.json");
                if (file != null) {
                    try {
                        FileUtil.createParentDirs(file.getFile());
                        FileWriter fileWriter = new FileWriter(file.getFile());
                        saveSettings();
                        BatchSearchReplaceSettings externalizedSettings = new BatchSearchReplaceSettings(mySettings);
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
            if (lastImportFile == null) lastImportFile = myProject.getBaseDir();

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

        myManageActions.addActionListener(e -> {
            myPopupMenuActions.show(myManageActions, myManageActions.getWidth() / 10, myManageActions.getHeight() * 85 / 100);
        });

        myMainPanel.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                if (myEditor != null) {
                    String propertyName = evt.getPropertyName();
                    if (propertyName.equals("ancestor")) {
                        int tmp = 0;

                        if (evt.getNewValue() != null) {
                            myIsActive = true;
                            LineSelectionManager.getInstance(myEditor).setHighlightProvider(myEditorSearchHighlightProvider);
                            updateOptions(true);
                        } else {
                            myIsActive = false;
                            saveSettings();
                            LineSelectionManager.getInstance(myEditor).setHighlightProvider(null);
                        }
                    } else if (propertyName.equals("Frame.active")) {
                        if (!(boolean) evt.getNewValue()) {
                            saveSettings();
                        }
                        int tmp = 0;
                    }
                }
            }
        });

        fillPresets();
        updateOptions(true);

        myInUpdate = false;
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

    public void settingsChanged(final boolean loadPresets) {
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

    public void fillPresets() {
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

    //private void createUIComponents() {
    //    //noinspection UndesirableClassUsage
    //    myPresets = new JComboBox();
    //}

    protected EditorEx createIdeaEditor(CharSequence charSequence) {
        Document doc = EditorFactory.getInstance().createDocument(charSequence);
        FileType fileType = FileTypeManager.getInstance().getStdFileType("text");
        Editor editor = EditorFactory.getInstance().createEditor(doc, myProject, fileType, false);
        editor.getSettings().setFoldingOutlineShown(false);
        editor.getSettings().setLineNumbersShown(false);
        editor.getSettings().setLineMarkerAreaShown(false);
        editor.getSettings().setIndentGuidesShown(false);
        return (EditorEx) editor;
    }

    void updateOptions(final boolean searchReplaceTextChanged) {
        //noinspection VariableNotUsedInsideIf
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
            boolean clipboardLoadEnabled = false;
            if (contents.length > 1) {
                // we take top two
                try {
                    final String replaceText = (String) contents[0].getTransferData(DataFlavor.stringFlavor);
                    final String searchText = (String) contents[1].getTransferData(DataFlavor.stringFlavor);
                    clipboardLoadEnabled = !replaceText.isEmpty() && !searchText.isEmpty();
                } catch (UnsupportedFlavorException | IOException e1) {
                }
            }

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

                    if (searchText != null && !searchText.isEmpty()) {
                        if (replaceText == null) {
                            // TODO: missing, use empty and highlight
                            replaceText = "";
                        }

                        boolean isCaseSensitive = myCaseSensitive.isSelected();
                        boolean isBeginWord = myWholeWord.isSelected();
                        boolean isEndWord = myWholeWord.isSelected();

                        if (optionsText.indexOf('c') != -1) {
                            isCaseSensitive = true;
                        } else if (optionsText.indexOf('i') != -1) {
                            isCaseSensitive = false;
                        }

                        if (optionsText.indexOf('w') != -1) {
                            isBeginWord = true;
                            isEndWord = true;
                        }

                        if (optionsText.indexOf('b') != -1) {
                            isBeginWord = true;
                        }

                        if (optionsText.indexOf('e') != -1) {
                            isEndWord = true;
                        }

                        SearchData searchData = new SearchData(searchText, replaceText, i, myEditorSearchHighlightProvider.encodeFlags(isBeginWord, isEndWord, isCaseSensitive));

                        int lMax = lineSearchData.size();
                        for (int l = lMax; l-- > 0; ) {
                            SearchData data = lineSearchData.get(l);
                            if ((data.flags & CASE_INSENSITIVE) == 0 && (searchData.flags & CASE_INSENSITIVE) == 0) {
                                // case sensitive, we need to remove all that match
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
                    myEditorSearchHighlightProvider.addHighlightWord(searchData.word, searchData.flags);
                    mySearchHighlightProvider.addHighlightLine(searchData.lineNumber);
                    myReplaceHighlightProvider.addHighlightLine(searchData.lineNumber);
                    myOptionsHighlightProvider.addHighlightLine(searchData.lineNumber);
                    myWordIndexToLineMap.put(i, searchData.lineNumber);
                    myLineSearchData.put(searchData.lineNumber, searchData);
                }

                boolean enabled = myEditorSearchHighlightProvider.getHighlightPattern() != null && !myEditorSearchHighlightProvider.getHighlightPattern().pattern().isEmpty();

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
                WordHighlighter highlighter = (WordHighlighter) LineSelectionManager.getInstance(myEditor).getHighlighter();

                if (highlighter != null) {
                    int offset = myEditor.getCaretModel().getOffset();
                    RangeHighlighter rangeHighlighter = highlighter.getRangeHighlighter(offset);
                    myFoundRange = rangeHighlighter == null ? null : TextRange.create(rangeHighlighter.getStartOffset(), rangeHighlighter.getEndOffset());
                    myFoundIndex = myWordIndexToLineMap.getOrDefault(highlighter.getOriginalIndex(rangeHighlighter), -1);
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

        public SearchData(final String word, final String replace, final int lineNumber, final int flags) {
            this.word = word;
            this.replace = replace;
            this.lineNumber = lineNumber;
            this.flags = flags;
        }
    }

    void updateRangeButtons() {
        myReplace.setEnabled(myFoundRange != null);
        myExclude.setEnabled(myFoundRange != null);
        if (myExclude.isEnabled()) {
            String message;
            if (!isExcludedRange()) {
                message = Bundle.message("batch-search.exclude.label");
            } else {
                message = Bundle.message("batch-search.include.label");
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

        WordHighlighter highlighter = myEditor == null ? null : (WordHighlighter) LineSelectionManager.getInstance(myEditor).getHighlighter();
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

        myReplaceAll.setEnabled(myFindNext.isEnabled() || myFindPrevious.isEnabled() || myReplace.isEnabled());
    }

    void findNext() {
        if (myEditor == null) return;

        WordHighlighter highlighter = (WordHighlighter) LineSelectionManager.getInstance(myEditor).getHighlighter();
        myFoundBackwards = false;

        if (highlighter != null) {
            int offset = myFoundRange != null ? myFoundRange.getEndOffset() : myEditor.getCaretModel().getOffset();
            RangeHighlighter rangeHighlighter = highlighter.getNextRangeHighlighter(offset);
            myFoundRange = rangeHighlighter == null ? null : TextRange.create(rangeHighlighter.getStartOffset(), rangeHighlighter.getEndOffset());
            myFoundIndex = myWordIndexToLineMap.getOrDefault(highlighter.getOriginalIndex(rangeHighlighter), -1);
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
    }

    void findPrevious() {
        if (myEditor == null) return;

        WordHighlighter highlighter = (WordHighlighter) LineSelectionManager.getInstance(myEditor).getHighlighter();
        myFoundBackwards = true;

        if (highlighter != null) {
            int offset = myFoundRange != null ? myFoundRange.getStartOffset() : myEditor.getCaretModel().getOffset();
            RangeHighlighter rangeHighlighter = highlighter.getPreviousRangeHighlighter(offset);
            myFoundRange = rangeHighlighter == null ? null : TextRange.create(rangeHighlighter.getStartOffset(), rangeHighlighter.getEndOffset());
            myFoundIndex = myWordIndexToLineMap.getOrDefault(highlighter.getOriginalIndex(rangeHighlighter), -1);
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
    }

    void adjustExclusions(TextRange foundRange, int replacementLength) {
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
                } else {
                    // nothing to do

                }
            }
        }
    }

    void replace() {
        if (myEditor == null) return;

        if (myFoundRange != null && myFoundIndex != -1) {
            WriteCommandAction.runWriteCommandAction(myProject, () -> {
                String replacement = myLineSearchData.get(myFoundIndex).replace;
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

    void addExclusion() {
        if (myFoundRange != null && myFoundIndex != -1) {
            if (myExcludedRanges == null) {
                myExcludedRanges = new ArrayList<>();
            }
            myExcludedRanges.add(myFoundRange);
        }
    }

    void exclude() {
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
    }

    void reset() {
        if (myEditor == null) return;

        if (myExcludedRanges != null) {
            myExcludedRanges = null;
            updateOptions(false);
            LineSelectionManager.getInstance(myEditor).updateHighlights();
        }
    }

    boolean isExcludedRange() {
        return getExcludedRange() != -1;
    }

    int getExcludedRange() {
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

    void replaceAll() {
        if (myEditor == null) return;

        if (!myEditorSearchHighlightProvider.getHighlightPattern().pattern().isEmpty()) {
            WriteCommandAction.runWriteCommandAction(myProject, () -> {
                int caretOffset = myEditor.getCaretModel().getPrimaryCaret().getOffset();
                int length = myEditor.getDocument().getTextLength();
                myEditor.getCaretModel().getPrimaryCaret().setSelection(length, length);
                myEditor.getCaretModel().getPrimaryCaret().moveToOffset(length);

                while (true) {
                    findPrevious();

                    if (myFoundRange != null && myFoundIndex != -1) {
                        if (isExcludedRange()) {
                            myEditor.getCaretModel().getPrimaryCaret().moveToOffset(myFoundRange.getStartOffset());
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

                myEditor.getCaretModel().getPrimaryCaret().moveToOffset(caretOffset);
            });
        }
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
        ignoreErrors(()->{ myViewerSettings.setRightMarginShown(myEditorSettings.isRightMarginShown()); });
        ignoreErrors(()->{ myViewerSettings.setGutterIconsShown(myEditorSettings.areGutterIconsShown()); });
        ignoreErrors(()->{ myViewerSettings.setAdditionalPageAtBottom(myEditorSettings.isAdditionalPageAtBottom()); });
        ignoreErrors(()->{ myViewerSettings.setAllowSingleLogicalLineFolding(myEditorSettings.isAllowSingleLogicalLineFolding()); });
        ignoreErrors(()->{ myViewerSettings.setAnimatedScrolling(myEditorSettings.isAnimatedScrolling()); });
        ignoreErrors(()->{ myViewerSettings.setAutoCodeFoldingEnabled(myEditorSettings.isAutoCodeFoldingEnabled()); });
        ignoreErrors(()->{ myViewerSettings.setBlinkCaret(myEditorSettings.isBlinkCaret()); });
        ignoreErrors(()->{ myViewerSettings.setBlockCursor(myEditorSettings.isBlockCursor()); });
        ignoreErrors(()->{ myViewerSettings.setCamelWords(myEditorSettings.isCamelWords()); });
        ignoreErrors(()->{ myViewerSettings.setCaretInsideTabs(myEditorSettings.isCaretInsideTabs()); });
        ignoreErrors(()->{ myViewerSettings.setCaretRowShown(myEditorSettings.isCaretRowShown()); });
        ignoreErrors(()->{ myViewerSettings.setDndEnabled(myEditorSettings.isDndEnabled()); });
        ignoreErrors(()->{ myViewerSettings.setFoldingOutlineShown(myEditorSettings.isFoldingOutlineShown()); });
        ignoreErrors(()->{ myViewerSettings.setIndentGuidesShown(myEditorSettings.isIndentGuidesShown()); });
        ignoreErrors(()->{ myViewerSettings.setInnerWhitespaceShown(myEditorSettings.isInnerWhitespaceShown()); });
        ignoreErrors(()->{ myViewerSettings.setLeadingWhitespaceShown(myEditorSettings.isLeadingWhitespaceShown()); });
        //ignoreErrors(()->{ myViewerSettings.setLineMarkerAreaShown(myEditorSettings.isLineMarkerAreaShown()); });
        ignoreErrors(()->{ myViewerSettings.setLineMarkerAreaShown(false); });
        ignoreErrors(()->{ myViewerSettings.setLineNumbersShown(myEditorSettings.isLineNumbersShown()); });
        ignoreErrors(()->{ myViewerSettings.setMouseClickSelectionHonorsCamelWords(myEditorSettings.isMouseClickSelectionHonorsCamelWords()); });
        ignoreErrors(()->{ myViewerSettings.setRefrainFromScrolling(myEditorSettings.isRefrainFromScrolling()); });
        ignoreErrors(()->{ myViewerSettings.setSmartHome(myEditorSettings.isSmartHome()); });
        ignoreErrors(()->{ myViewerSettings.setTrailingWhitespaceShown(myEditorSettings.isTrailingWhitespaceShown()); });
        ignoreErrors(()->{ myViewerSettings.setUseCustomSoftWrapIndent(myEditorSettings.isUseCustomSoftWrapIndent()); });
        ignoreErrors(()->{ myViewerSettings.setUseSoftWraps(myEditorSettings.isUseSoftWraps()); });
        //ignoreErrors(()->{ myViewerSettings.setVariableInplaceRenameEnabled(myEditorSettings.isVariableInplaceRenameEnabled()); });
        ignoreErrors(()->{ myViewerSettings.setVirtualSpace(myEditorSettings.isVirtualSpace()); });
        ignoreErrors(()->{ myViewerSettings.setWheelFontChangeEnabled(myEditorSettings.isWheelFontChangeEnabled()); });
        ignoreErrors(()->{ myViewerSettings.setWhitespacesShown(myEditorSettings.isWhitespacesShown()); });
        //ignoreErrors(()->{ myViewerSettings.setAdditionalColumnsCount(myEditorSettings.getAdditionalColumnsCount()); });
        //ignoreErrors(()->{ myViewerSettings.setAdditionalLinesCount(myEditorSettings.getAdditionalLinesCount()); });
        ignoreErrors(()->{ myViewerSettings.setCaretBlinkPeriod(myEditorSettings.getCaretBlinkPeriod()); });
        ignoreErrors(()->{ myViewerSettings.setCustomSoftWrapIndent(myEditorSettings.getCustomSoftWrapIndent()); });
        ignoreErrors(()->{ myViewerSettings.setLineCursorWidth(myEditorSettings.getLineCursorWidth()); });
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

    void updateFoundRanges() {
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
            Highlighter highlighter = selectionManager.getHighlighter();
            if (highlighter instanceof WordHighlighter) {
                myIndexedWordCounts = ((WordHighlighter) highlighter).getIndexedWordCounts();
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

    public static void adjustOtherEditorCaretLine(final int caretLine, final EditorEx editor) {
        if (caretLine < editor.getDocument().getLineCount()) {
            editor.getCaretModel().getPrimaryCaret().moveToOffset(editor.getDocument().getLineStartOffset(caretLine));
        }
    }

    public static void adjustOtherEditorScrollOffset(final int scrollOffset, final EditorEx editor) {
        editor.getScrollingModel().scrollVertically(scrollOffset);
    }

    private class EditorCaretListener implements CaretListener {
        @Override
        public void caretPositionChanged(final CaretEvent e) {
            Editor caretEditor = e.getEditor();
            if (!myInUpdate && canSyncEditors(caretEditor, DELAY_MILLIS)) {
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
            if (!myInUpdate && canSyncEditors(visibleAreaEditor, DELAY_MILLIS)) {
                updateLastEditorSync(visibleAreaEditor);
                myInUpdate = true;
                try {
                    Rectangle rectangle = e.getNewRectangle();
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

    private class EditorDocumentListener implements DocumentListener {
        @Override
        public void documentChanged(final DocumentEvent event) {
            updateOptions(true);
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
            Utils.setRegExError(error, myTextPane, mySampleText.getFont(), getValidTextFieldBackground(), getWarningTextFieldBackground());
        }
        return error;
    }

    private class SearchWordHighlighterProvider extends WordHighlightProviderImpl {
        public SearchWordHighlighterProvider(@NotNull final ApplicationSettings settings) {
            super(settings);
        }

        @Override
        public WordHighlighter getHighlighter(@NotNull final Editor editor) {
            return new SearchWordHighlighter(this, editor);
        }
    }

    private class EditorLineHighlighterProvider extends LineHighlightProviderImpl {
        public EditorLineHighlighterProvider(@NotNull final ApplicationSettings settings) {
            super(settings);
        }

        @Override
        public boolean isShowHighlights() {
            return isHighlightsMode();
        }

        @Override
        public LineHighlighter getHighlighter(@NotNull final Editor editor) {
            return new EditorLineHighlighter(this, editor);
        }
    }

    private class SearchWordHighlighter extends WordHighlighter {
        public SearchWordHighlighter(@NotNull WordHighlightProvider highlightProvider, @NotNull final Editor editor) {
            super(highlightProvider, editor);
        }

        @Override
        public TextAttributes getAttributes(@Nullable final TextAttributes attributes, final String word, final int startOffset, final int endOffset) {
            if (attributes != null) {
                if (myExcludedRanges != null) {
                    for (TextRange range : myExcludedRanges) {
                        if (range.containsRange(startOffset, endOffset)) {
                            EditorColorsScheme uiTheme = EditorColorsManager.getInstance().getSchemeForCurrentUITheme();
                            Color foreground = uiTheme.getDefaultForeground();
                            return new TextAttributes(attributes.getForegroundColor(), attributes.getBackgroundColor(), foreground, EffectType.STRIKEOUT, attributes.getFontType());
                        }
                    }
                }
                if (myFoundRange != null && myFoundRange.containsRange(startOffset, endOffset)) {
                    EditorColorsScheme uiTheme = EditorColorsManager.getInstance().getSchemeForCurrentUITheme();
                    Color foreground = uiTheme.getDefaultForeground();
                    return new TextAttributes(attributes.getForegroundColor(), attributes.getBackgroundColor(), foreground, EffectType.BOXED, attributes.getFontType());
                }
            }
            return attributes;
        }
    }

    private class EditorLineHighlighter extends LineHighlighter {
        public EditorLineHighlighter(@NotNull LineHighlightProvider highlightProvider, @NotNull final Editor editor) {
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
                EditorColorsScheme uiTheme = EditorColorsManager.getInstance().getSchemeForCurrentUITheme();
                effectColor = uiTheme.getDefaultForeground();
                effectType = EffectType.BOXED;
                selectedLine = true;
            }

            if (attributes == null && BatchReplaceForm.this.myEditor != null) {
                // not used, overridden
                TextAttributesKey attributesKey = CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES;
                EditorColorsScheme uiTheme = EditorColorsManager.getInstance().getSchemeForCurrentUITheme();
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
                    EditorColorsScheme uiTheme = EditorColorsManager.getInstance().getSchemeForCurrentUITheme();
                    attributes = uiTheme.getAttributes(attributesKey);
                } else if ((selectedLine || myHighlightAllLines) && attributes != null) {
                    attributes = new TextAttributes(
                            attributes.getForegroundColor(),
                            attributes.getBackgroundColor(),
                            effectColor,
                            effectType,
                            0
                    );
                } else {
                    attributes = null;
                }
            }

            return attributes;
        }
    }
}
