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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
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
import com.vladsch.MissingInActions.settings.BulkSearchReplace;
import com.vladsch.MissingInActions.settings.BulkSearchReplaceSettings;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.MissingInActions.util.Utils;
import com.vladsch.MissingInActions.util.ui.BackgroundColor;
import com.vladsch.ReverseRegEx.util.ForwardMatcher;
import com.vladsch.ReverseRegEx.util.ForwardPattern;
import com.vladsch.ReverseRegEx.util.ReverseMatcher;
import com.vladsch.ReverseRegEx.util.ReversePattern;
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
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BulkReplaceForm implements Disposable {
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
    private final JBPopupMenu myPopupMenuActions;

    EditorEx myEditor;
    EditorEx mySearchEditor;
    EditorEx myReplaceEditor;
    EditorEx myOptionsEditor;

    final BulkSearchReplaceSettings mySettings;

    String myPatternText = null;
    ForwardPattern myForwardPattern = null;
    ReversePattern myReversePattern = null;
    ArrayList<String> myReplacementStrings = null;
    ForwardMatcher myForwardMatcher = null;
    ReverseMatcher myReverseMatcher = null;
    TextRange myFoundRange = null;
    ArrayList<TextRange> myExcludedRanges = null;
    ArrayList<TextRange> mySearchRanges = null;
    ArrayList<TextRange> myReplaceRanges = null;
    ArrayList<TextRange> myOptionsRanges = null;
    final DocumentListener myDocumentListener;
    final CaretListener myCaretListener;
    final VisibleAreaListener myVisibleAreaListener;
    final CaretListener myEditorCaretListener;
    final Project myProject;

    int myFoundIndex = -1;
    boolean myFoundBackwards = false;
    boolean myInUpdate = false;
    long myLastEditorSync = Long.MIN_VALUE;
    Editor myLastSyncEditor = null;

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

    public boolean saveSettings(boolean onlySamples) {
        // save settings return false if regex is not valid
        //mySettings.setSampleText(mySampleText.getText());

        if (!onlySamples) {
            mySettings.getBulkSearchReplace().setWholeWord(myWholeWord.isSelected());
            mySettings.getBulkSearchReplace().setCaseSensitive(myCaseSensitive.isSelected());
            mySettings.getBulkSearchReplace().setSearchText(mySearchEditor.getDocument().getText().trim());
            mySettings.getBulkSearchReplace().setReplaceText(myReplaceEditor.getDocument().getText().trim());
            mySettings.getBulkSearchReplace().setOptionsText(myOptionsEditor.getDocument().getText().trim());
        }

        return onlySamples || checkRegEx(mySearchEditor.getDocument().getText()).isEmpty();
    }

    public void disposeEditors() {
        if (mySearchEditor != null) {
            // release the editors
            mySearchEditor.getDocument().removeDocumentListener(myDocumentListener);
            myReplaceEditor.getDocument().removeDocumentListener(myDocumentListener);
            myOptionsEditor.getDocument().removeDocumentListener(myDocumentListener);

            mySearchEditor.getCaretModel().removeCaretListener(myCaretListener);
            myReplaceEditor.getCaretModel().removeCaretListener(myCaretListener);
            myOptionsEditor.getCaretModel().removeCaretListener(myCaretListener);

            mySearchEditor.getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener);
            myReplaceEditor.getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener);
            myOptionsEditor.getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener);

            EditorFactory.getInstance().releaseEditor(mySearchEditor);
            EditorFactory.getInstance().releaseEditor(myReplaceEditor);
            EditorFactory.getInstance().releaseEditor(myOptionsEditor);

            myEditor = null;
            mySearchEditor = null;
            myReplaceEditor = null;
            myOptionsEditor = null;
        }
    }

    @SuppressWarnings("VariableNotUsedInsideIf")
    public void setActiveEditor(@Nullable EditorEx editor) {
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

            updateOptions(true);
        }
    }

    private class MainEditorCaretListener implements CaretListener {
        @Override
        public void caretPositionChanged(final CaretEvent e) {
            updateOptions(false);
        }

        @Override
        public void caretAdded(final CaretEvent e) {

        }

        @Override
        public void caretRemoved(final CaretEvent e) {

        }
    }

    public BulkReplaceForm(@NotNull Project project, @NotNull BulkSearchReplaceSettings searchReplaceSettings) {
        myProject = project;
        mySettings = searchReplaceSettings;
        String searchText = mySettings.getBulkSearchReplace().getSearchText();
        String replaceText = mySettings.getBulkSearchReplace().getReplaceText();
        String optionsText = mySettings.getBulkSearchReplace().getOptionsText();

        boolean caseSensitive = mySettings.getBulkSearchReplace().isCaseSensitive();
        myCaseSensitive.setSelected(caseSensitive);
        myWholeWord.setSelected(mySettings.getBulkSearchReplace().isWholeWord());

        mySampleText.setVisible(false);

        myEditorCaretListener = new MainEditorCaretListener();

        mySearchEditor = createIdeaEditor(searchText);
        myReplaceEditor = createIdeaEditor(replaceText);
        myOptionsEditor = createIdeaEditor(optionsText);

        mySearchViewPanel.add(mySearchEditor.getComponent(), BorderLayout.CENTER);
        myReplaceViewPanel.add(myReplaceEditor.getComponent(), BorderLayout.CENTER);
        myOptionsViewPanel.add(myOptionsEditor.getComponent(), BorderLayout.CENTER);

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
            BulkSearchReplace oldSettings = mySettings.getPreset(presetName);
            saveSettings(false);

            mySettings.savePreset(presetName);
            if (oldSettings == null) {
                // reload
                fillPresets();
            }
        });

        //myPresets.addItemListener(e -> {
        //    int tmp = 0;
        //});

        myPopupMenuActions = new JBPopupMenu("Actions");
        final JBMenuItem exportXML = new JBMenuItem(Bundle.message("bulk-search.export-xml.label"));
        final JBMenuItem importXML = new JBMenuItem(Bundle.message("bulk-search.import-xml.label"));
        final JBMenuItem exportJSON = new JBMenuItem(Bundle.message("bulk-search.export-json.label"));
        final JBMenuItem importJSON = new JBMenuItem(Bundle.message("bulk-search.import-json.label"));
        final JBMenuItem deletePreset = new JBMenuItem(Bundle.message("bulk-search.delete.label"));

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
                    mySettings.setBulkPresetName(null);
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
            ApplicationManager.getApplication().runWriteAction(() -> {
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
                    ApplicationManager.getApplication().runWriteAction(() -> {
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
                BulkSearchReplace removed = mySettings.getBulkPresets().remove(presetName);
                if (removed != null) {
                    mySettings.setBulkPresetName(null);
                    fillPresets();
                    myPresets.setSelectedIndex(-1);
                }
            }
        });

        exportXML.addActionListener(e -> {
            String title = Bundle.message("bulk-search.export.title");
            String description = Bundle.message("bulk-search.export.description");
            FileSaverDescriptor fileSaverDescriptor = new FileSaverDescriptor(title, description, "xml");
            FileSaverDialogImpl saveDialog = new FileSaverDialogImpl(fileSaverDescriptor, myMainPanel);
            if (myProject != null) {
                VirtualFileWrapper file = saveDialog.save(myProject.getBaseDir(), "bulk-search-replace.xml");
                if (file != null) {
                    try {
                        FileUtil.createParentDirs(file.getFile());
                        FileOutputStream fileWriter = new FileOutputStream(file.getFile());
                        BulkSearchReplaceSettings externalizedSettings = new BulkSearchReplaceSettings(mySettings);
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
            String title = Bundle.message("bulk-search.import.title");
            String description = Bundle.message("bulk-search.import.description");
            fileChooserDescriptor.setTitle(title);
            fileChooserDescriptor.setDescription(description);
            FileChooserDialogImpl fileChooserDialog = new FileChooserDialogImpl(fileChooserDescriptor, myMainPanel, myProject);
            String lastImport = myProject.getBasePath() + "/" + "bulk-search-replace.xml";
            VirtualFile lastImportFile = myProject.getBaseDir();
            if (!lastImport.isEmpty()) {
                File file = new File(lastImport);
                lastImportFile = VirtualFileManager.getInstance().findFileByUrl("file://" + file.getPath());
            }

            VirtualFile[] files = fileChooserDialog.choose(myProject, lastImportFile);
            if (files.length > 0) {
                try {
                    BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(files[0].getPath()));
                    XMLDecoder decoder = new XMLDecoder(inputStream, this, e1 -> {
                        e1.printStackTrace();
                    }, this.getClass().getClassLoader());

                    Object object = decoder.readObject();
                    BulkSearchReplaceSettings externalizedSettings = (BulkSearchReplaceSettings) object;
                    decoder.close();
                    inputStream.close();
                    if (externalizedSettings != null) {
                        mySettings.copyFrom(externalizedSettings);
                        settingsChanged(true);
                    } else {
                        Messages.showErrorDialog("File does not contain exported Bulk Search/Replace settings.", "Import Failure");
                    }
                } catch (Exception e1) {
                    Messages.showErrorDialog(e1.getMessage(), "Import Failure");
                }
            }
        });

        exportJSON.addActionListener(e -> {
            String title = Bundle.message("bulk-search.export.title");
            String description = Bundle.message("bulk-search.export.description");
            FileSaverDescriptor fileSaverDescriptor = new FileSaverDescriptor(title, description, "json");
            FileSaverDialogImpl saveDialog = new FileSaverDialogImpl(fileSaverDescriptor, myMainPanel);
            if (myProject != null) {
                VirtualFileWrapper file = saveDialog.save(myProject.getBaseDir(), "bulk-search-replace.json");
                if (file != null) {
                    try {
                        FileUtil.createParentDirs(file.getFile());
                        FileWriter fileWriter = new FileWriter(file.getFile());
                        saveSettings(false);
                        BulkSearchReplaceSettings externalizedSettings = new BulkSearchReplaceSettings(mySettings);
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
            String title = Bundle.message("bulk-search.import.title");
            String description = Bundle.message("bulk-search.import.description");
            fileChooserDescriptor.setTitle(title);
            fileChooserDescriptor.setDescription(description);
            FileChooserDialogImpl fileChooserDialog = new FileChooserDialogImpl(fileChooserDescriptor, myMainPanel, myProject);
            String lastImport = myProject.getBasePath() + "/" + "bulk-search-replace.json";
            VirtualFile lastImportFile = myProject.getBaseDir();
            if (!lastImport.isEmpty()) {
                File file = new File(lastImport);
                lastImportFile = VirtualFileManager.getInstance().findFileByUrl("file://" + file.getPath());
            }

            VirtualFile[] files = fileChooserDialog.choose(myProject, lastImportFile);
            if (files.length > 0) {
                try {
                    BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(files[0].getPath()));
                    BoxedJsObject settings = BoxedJson.from(inputStream);
                    inputStream.close();

                    BoxedJsObject presets = settings.getJsObject("presets");
                    BulkSearchReplaceSettings externalizedSettings = new BulkSearchReplaceSettings();

                    if (presets.isValid() && importFromJSON(externalizedSettings, presets)) {
                        mySettings.copyFrom(externalizedSettings);
                        settingsChanged(true);
                    } else {
                        Messages.showErrorDialog("File does not contain exported Bulk Search/Replace settings.", "Import Failure");
                    }
                } catch (Exception e1) {
                    Messages.showErrorDialog(e1.getMessage(), "Import Failure");
                }
            }
        });

        myPopupMenuActions.add(exportJSON);
        myPopupMenuActions.add(importJSON);
        myPopupMenuActions.addSeparator();
        myPopupMenuActions.add(exportXML);
        myPopupMenuActions.add(importXML);
        myPopupMenuActions.addSeparator();
        myPopupMenuActions.add(deletePreset);

        myManageActions.setComponentPopupMenu(myPopupMenuActions);

        myManageActions.addActionListener(e -> {
            myPopupMenuActions.show(myManageActions, myManageActions.getWidth() / 10, myManageActions.getHeight() * 85 / 100);
        });

        fillPresets();
        updateOptions(true);
    }

    private boolean importFromJSON(final BulkSearchReplaceSettings settings, final BoxedJsObject presets) {
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
                    BulkSearchReplace searchReplaceSettings = new BulkSearchReplace();
                    searchReplaceSettings.setSearchText(search.toString());
                    searchReplaceSettings.setReplaceText(replace.toString());
                    settings.getBulkPresets().put(presetName, searchReplaceSettings);
                }
            }
        }

        return hadPreset;
    }

    private void exportJSONPresets(final BoxedJsObject presets) {
        // we export presets only
        ArrayList<String> keySet = new ArrayList<>(mySettings.getBulkPresets().keySet());
        keySet.sort(Comparator.naturalOrder());

        for (String presetName : keySet) {
            BoxedJsObject preset = BoxedJson.of();
            presets.put(presetName, preset);

            BulkSearchReplace replaceSettings = mySettings.getPreset(presetName);
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
        String searchText1 = mySettings.getBulkSearchReplace().getSearchText();
        String replaceText1 = mySettings.getBulkSearchReplace().getReplaceText();
        String optionsText1 = mySettings.getBulkSearchReplace().getOptionsText();

        WriteCommandAction.runWriteCommandAction(myProject, () -> {
            mySearchEditor.getDocument().setText(Utils.suffixWith(searchText1, "\n"));
            myReplaceEditor.getDocument().setText(Utils.suffixWith(replaceText1, "\n"));
            myOptionsEditor.getDocument().setText(Utils.suffixWith(optionsText1, "\n"));
        });

        boolean caseSensitive1 = mySettings.getBulkSearchReplace().isCaseSensitive();
        myCaseSensitive.setSelected(caseSensitive1);
        myWholeWord.setSelected(mySettings.getBulkSearchReplace().isWholeWord());

        if (loadPresets) {
            fillPresets();
        }

        myInUpdate = savedInUpdate;
        updateOptions(true);
    }

    public void fillPresets() {
        myPresets.removeAllItems();
        ArrayList<String> presetNames = new ArrayList<>(mySettings.getBulkPresets().keySet());
        presetNames.sort(Comparator.naturalOrder());
        String presetName = mySettings.getBulkPresetName();

        for (String item : presetNames) {
            myPresets.addItem(item);
        }

        if (presetName != null) {
            myPresets.setSelectedItem(presetName);
        } else {
            myPresets.setSelectedIndex(-1);
        }
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
        mySavePreset.setEnabled(!((String) myPresets.getEditor().getItem()).isEmpty());

        if (myEditor == null) {
            // disable everything
            myFindNext.setEnabled(false);
            myFindPrevious.setEnabled(false);
            myReplace.setEnabled(false);
            myReplaceAll.setEnabled(false);
            myExclude.setEnabled(false);
            myReset.setEnabled(false);
        } else {
            // compare line count for search/replace, ignore options, ignore trailing empty lines, EOL is ignored
            myForwardPattern = null;
            myReversePattern = null;
            myForwardMatcher = null;
            myReverseMatcher = null;
            myFoundRange = null;
            myReset.setEnabled(myExcludedRanges != null);

            if (myInUpdate) return;

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

            if (searchReplaceTextChanged) {
                DocumentEx searchEditorDocument = mySearchEditor.getDocument();
                CharSequence searchSequence = searchEditorDocument.getCharsSequence();
                DocumentEx replaceEditorDocument = myReplaceEditor.getDocument();
                CharSequence replaceSequence = replaceEditorDocument.getCharsSequence();
                DocumentEx optionsEditorDocument = myOptionsEditor.getDocument();
                CharSequence optionsSequence = optionsEditorDocument.getCharsSequence();

                ArrayList<String> searchStrings = new ArrayList<>();
                ArrayList<String> replaceStrings = new ArrayList<>();
                ArrayList<String> optionsStrings = new ArrayList<>();
                ArrayList<Integer> indices = new ArrayList<>();
                ArrayList<TextRange> searchRanges = new ArrayList<>();
                ArrayList<TextRange> replaceRanges = new ArrayList<>();
                ArrayList<TextRange> optionsRanges = new ArrayList<>();

                int searchLines = searchEditorDocument.getLineCount();
                int replaceLines = replaceEditorDocument.getLineCount();
                int optionsLines = optionsEditorDocument.getLineCount();
                int iMax = Math.max(searchLines, Math.max(replaceLines, optionsLines));
                boolean hadErrors = false;
                for (int i = 0; i < iMax; i++) {
                    String searchText = null;
                    String replaceText = null;
                    String optionsText = "";
                    if (i < searchLines) {
                        searchText = searchSequence.subSequence(searchEditorDocument.getLineStartOffset(i), searchEditorDocument.getLineEndOffset(i)).toString();
                        searchRanges.add(TextRange.create(searchEditorDocument.getLineStartOffset(i), searchEditorDocument.getLineEndOffset(i)));
                    }

                    if (i < replaceLines) {
                        replaceText = replaceSequence.subSequence(replaceEditorDocument.getLineStartOffset(i), replaceEditorDocument.getLineEndOffset(i)).toString();
                        replaceRanges.add(TextRange.create(replaceEditorDocument.getLineStartOffset(i), replaceEditorDocument.getLineEndOffset(i)));
                    } else {
                        replaceRanges.add(null);
                    }

                    if (i < optionsLines) {
                        optionsText = optionsSequence.subSequence(optionsEditorDocument.getLineStartOffset(i), optionsEditorDocument.getLineEndOffset(i)).toString();
                        optionsRanges.add(TextRange.create(optionsEditorDocument.getLineStartOffset(i), optionsEditorDocument.getLineEndOffset(i)));
                    } else {
                        optionsRanges.add(null);
                    }

                    if (searchText != null && !searchText.isEmpty()) {
                        if (replaceText == null) {
                            // TODO: missing, use empty and highlight
                            replaceText = "";
                        }

                        searchStrings.add(searchText);
                        replaceStrings.add(replaceText);
                        optionsStrings.add(optionsText);
                        indices.add(indices.size());
                    } else {
                        if (replaceText != null && !replaceText.isEmpty()) {
                            // TODO: highlight as ignored
                            hadErrors = true;
                        }
                    }
                }

                // sort search string in reverse length order
                indices.sort(new Comparator<Integer>() {
                    @Override
                    public int compare(final Integer o1, final Integer o2) {
                        return searchStrings.get(o2).length() - searchStrings.get(o1).length();
                    }
                });

                // now create the pattern and replacement
                StringBuilder sb = new StringBuilder();
                myReplacementStrings = new ArrayList<>();
                mySearchRanges = new ArrayList<>();
                myReplaceRanges = new ArrayList<>();
                myOptionsRanges = new ArrayList<>();
                iMax = searchStrings.size();
                for (int i = 0; i < iMax; i++) {
                    if (i > 0) sb.append("|");
                    Integer index = indices.get(i);

                    boolean isCaseSensitive = myCaseSensitive.isSelected();
                    boolean isBeginWord = myWholeWord.isSelected();
                    boolean isEndWord = myWholeWord.isSelected();
                    String options = optionsStrings.get(index);

                    if (options.indexOf('c') != -1) {
                        isCaseSensitive = true;
                    }

                    if (options.indexOf('w') != -1) {
                        isBeginWord = true;
                        isEndWord = true;
                    }

                    if (options.indexOf('b') != -1) {
                        isBeginWord = true;
                    }

                    if (options.indexOf('e') != -1) {
                        isEndWord = true;
                    }

                    sb.append("(").append(isCaseSensitive ? "(?-i)" : "(?i)");
                    if (isBeginWord) sb.append("\\b");
                    sb.append("\\Q").append(searchStrings.get(index)).append("\\E");
                    if (isEndWord) sb.append("\\b");
                    sb.append(")");
                    myReplacementStrings.add(replaceStrings.get(index));
                    mySearchRanges.add(searchRanges.get(index));
                    myReplaceRanges.add(replaceRanges.get(index));
                    myOptionsRanges.add(optionsRanges.get(index));
                }

                myPatternText = sb.toString();
                boolean enabled = !myPatternText.isEmpty();

                myFindNext.setEnabled(enabled);
                myFindPrevious.setEnabled(enabled);
                myReplaceAll.setEnabled(enabled);

                myFoundRange = null;
                updateRangeButtons(false);
            }
        }
    }

    void updateRangeButtons(final boolean searchChanged) {
        myReplace.setEnabled(myFoundRange != null && myFoundIndex != -1);
        myExclude.setEnabled(myFoundRange != null && myFoundIndex != -1);
        if (myExclude.isEnabled() && searchChanged) {
            String message;
            if (!isExcludedRange()) {
                message = Bundle.message("bulk-search.exclude.label");
            } else {
                message = Bundle.message("bulk-search.include.label");
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
    }

    void findNext() {
        if (myEditor == null) return;

        if (myForwardMatcher == null) {
            if (myForwardPattern == null) {
                myForwardPattern = ForwardPattern.compile(myPatternText);
            }
            myForwardMatcher = myForwardPattern.matcher(myEditor.getDocument().getCharsSequence());
        }

        if (myEditor.getCaretModel().getPrimaryCaret().hasSelection()) {
            myForwardMatcher.region(myEditor.getCaretModel().getPrimaryCaret().getSelectionEnd(), myEditor.getDocument().getTextLength());
        } else {
            myForwardMatcher.region(myEditor.getCaretModel().getPrimaryCaret().getOffset(), myEditor.getDocument().getTextLength());
        }

        boolean searchChanged = false;
        if (myForwardMatcher.find()) {
            int iMax = myForwardMatcher.groupCount();
            myFoundIndex = -1;
            myFoundRange = null;
            for (int i = 1; i <= iMax; i++) {
                if (myForwardMatcher.group(i) != null) {
                    myFoundRange = TextRange.create(myForwardMatcher.start(i), myForwardMatcher.end(i));
                    myFoundIndex = i - 1;
                    myFoundBackwards = false;
                    myEditor.getCaretModel().getPrimaryCaret().setSelection(myFoundRange.getStartOffset(), myFoundRange.getEndOffset());
                    EditHelpers.scrollToSelection(myEditor);
                    break;
                }
            }
            myFindPrevious.setEnabled(true);
            searchChanged = true;
        } else {
            if (myEditor.getCaretModel().getPrimaryCaret().hasSelection()) {
                int offset = myEditor.getCaretModel().getPrimaryCaret().getSelectionEnd();
                myEditor.getCaretModel().getPrimaryCaret().setSelection(offset, offset);
                myEditor.getCaretModel().getPrimaryCaret().moveToOffset(offset);
            }
            myFindNext.setEnabled(false);
            myFindPrevious.setEnabled(true);
            myFoundRange = null;
        }
        updateFoundRanges();
        updateRangeButtons(searchChanged);
    }

    void findPrevious() {
        if (myEditor == null) return;

        if (myReverseMatcher == null) {
            if (myReversePattern == null) {
                myReversePattern = ReversePattern.compile(myPatternText);
            }
            myReverseMatcher = myReversePattern.matcher(myEditor.getDocument().getCharsSequence());
        }

        if (myEditor.getCaretModel().getPrimaryCaret().hasSelection()) {
            myReverseMatcher.region(0, myEditor.getCaretModel().getPrimaryCaret().getSelectionStart());
        } else {
            myReverseMatcher.region(0, Math.min(myEditor.getCaretModel().getPrimaryCaret().getOffset(), myEditor.getDocument().getTextLength() - 1));
        }

        boolean searchChanged = false;
        if (myReverseMatcher.find()) {
            myFoundRange = null;
            int iMax = myReverseMatcher.groupCount();
            myFoundIndex = -1;
            for (int i = 1; i <= iMax; i++) {
                if (myReverseMatcher.group(i) != null) {
                    myFoundRange = TextRange.create(myReverseMatcher.start(i), myReverseMatcher.end(i));
                    myFoundIndex = i - 1;
                    myFoundBackwards = true;
                    myEditor.getCaretModel().getPrimaryCaret().setSelection(myFoundRange.getStartOffset(), myFoundRange.getEndOffset());
                    EditHelpers.scrollToSelection(myEditor);
                    break;
                }
            }
            myFindNext.setEnabled(true);
            searchChanged = true;
        } else {
            if (myEditor.getCaretModel().getPrimaryCaret().hasSelection()) {
                int offset = myEditor.getCaretModel().getPrimaryCaret().getSelectionStart();
                myEditor.getCaretModel().getPrimaryCaret().setSelection(offset, offset);
                myEditor.getCaretModel().getPrimaryCaret().moveToOffset(offset);
            }
            myFindPrevious.setEnabled(false);
            myFindNext.setEnabled(true);
            myFoundRange = null;
        }
        updateFoundRanges();
        updateRangeButtons(searchChanged);
    }

    private static void updateFoundRange(final TextRange range, final EditorEx editor) {
        if (range != null) {
            editor.getCaretModel().getPrimaryCaret().setSelection(range.getStartOffset(), range.getEndOffset());
            editor.getCaretModel().getPrimaryCaret().moveToOffset(range.getStartOffset());
        } else {
            int offset = editor.getCaretModel().getOffset();
            editor.getCaretModel().getPrimaryCaret().setSelection(offset, offset);
        }
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
                String replacement = myReplacementStrings.get(myFoundIndex);
                myEditor.getDocument().replaceString(myFoundRange.getStartOffset(), myFoundRange.getEndOffset(), replacement);
                addExclusion(); // we are replacing it, prevent double replacement
                adjustExclusions(myFoundRange, replacement.length());

                myForwardMatcher = null;
                myReverseMatcher = null;
                if (myFoundBackwards) {
                    int offset = myFoundRange.getStartOffset();
                    myEditor.getCaretModel().getPrimaryCaret().setSelection(offset, offset);
                    myEditor.getCaretModel().getPrimaryCaret().moveToOffset(offset);
                    findPrevious();
                } else {
                    int offset = myFoundRange.getEndOffset() - (myFoundRange.getLength() - replacement.length());
                    myEditor.getCaretModel().getPrimaryCaret().setSelection(offset, offset);
                    myEditor.getCaretModel().getPrimaryCaret().moveToOffset(offset);
                    findNext();
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

            if (myFoundBackwards) {
                int offset = myFoundRange.getStartOffset();
                myEditor.getCaretModel().getPrimaryCaret().setSelection(offset, offset);
                myEditor.getCaretModel().getPrimaryCaret().moveToOffset(offset);
                findPrevious();
            } else {
                int offset = myFoundRange.getEndOffset();
                myEditor.getCaretModel().getPrimaryCaret().setSelection(offset, offset);
                myEditor.getCaretModel().getPrimaryCaret().moveToOffset(offset);
                findNext();
            }
        }
    }

    void reset() {
        if (myEditor == null) return;

        if (myExcludedRanges != null) {
            myExcludedRanges = null;
            updateOptions(false);
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

        if (!myPatternText.isEmpty()) {
            WriteCommandAction.runWriteCommandAction(myProject, () -> {
                int caretOffset = myEditor.getCaretModel().getPrimaryCaret().getOffset();
                int caretOffsetStart = myEditor.getCaretModel().getPrimaryCaret().getSelectionStart();
                int caretOffsetEnd = myEditor.getCaretModel().getPrimaryCaret().getSelectionEnd();
                int length = myEditor.getDocument().getTextLength();
                myEditor.getCaretModel().getPrimaryCaret().setSelection(length, length);
                myEditor.getCaretModel().getPrimaryCaret().moveToOffset(length);
                myForwardMatcher = null;
                myReverseMatcher = null;

                while (true) {
                    findPrevious();

                    if (myFoundRange != null && myFoundIndex != -1) {
                        if (isExcludedRange()) {
                            myEditor.getCaretModel().getPrimaryCaret().setSelection(myFoundRange.getStartOffset(), myFoundRange.getStartOffset());
                            myEditor.getCaretModel().getPrimaryCaret().moveToOffset(myFoundRange.getStartOffset());
                            continue;
                        }
                        String replacement = myReplacementStrings.get(myFoundIndex);
                        myEditor.getDocument().replaceString(myFoundRange.getStartOffset(), myFoundRange.getEndOffset(), replacement);
                        adjustExclusions(myFoundRange, replacement.length());
                        myEditor.getCaretModel().getPrimaryCaret().setSelection(myFoundRange.getStartOffset(), myFoundRange.getStartOffset());
                        myEditor.getCaretModel().getPrimaryCaret().moveToOffset(myFoundRange.getStartOffset());
                    } else {
                        break;
                    }
                }

                myEditor.getCaretModel().getPrimaryCaret().setSelection(caretOffsetStart, caretOffsetEnd);
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

    void updateFoundRanges() {
        boolean savedInUpdate = myInUpdate;
        myInUpdate = true;
        if (myFoundRange != null && myFoundIndex != -1) {
            // highlight search and replace and options strings
            updateFoundRange(mySearchRanges.get(myFoundIndex), mySearchEditor);
            updateFoundRange(myReplaceRanges.get(myFoundIndex), myReplaceEditor);
            updateFoundRange(myOptionsRanges.get(myFoundIndex), myOptionsEditor);
        } else {
            updateFoundRange(null, mySearchEditor);
            updateFoundRange(null, myReplaceEditor);
            updateFoundRange(null, myOptionsEditor);
        }

        updateLastEditorSync(null);
        EditHelpers.scrollToSelection(mySearchEditor);
        EditHelpers.scrollToSelection(myReplaceEditor);
        EditHelpers.scrollToSelection(myOptionsEditor);
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
}
