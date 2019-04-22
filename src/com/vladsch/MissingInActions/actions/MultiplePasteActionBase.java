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
package com.vladsch.MissingInActions.actions;

import com.intellij.ide.CopyPasteManagerEx;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.ide.CopyPasteManager.ContentChangedListener;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.TextRange;
import com.vladsch.MissingInActions.Bundle;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.settings.MultiPasteOptionsPane;
import com.vladsch.MissingInActions.util.ClipboardCaretContent;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.flexmark.util.Utils;
import com.vladsch.plugin.util.AwtRunnable;
import com.vladsch.plugin.util.DelayedRunner;
import com.vladsch.plugin.util.SearchPattern;
import com.vladsch.plugin.util.ui.CommonUIShortcuts;
import com.vladsch.plugin.util.ui.ContentChooser;
import icons.PluginIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.intellij.openapi.diagnostic.Logger.getInstance;

public abstract class MultiplePasteActionBase extends AnAction implements DumbAware {
    static final Logger LOG = getInstance("com.vladsch.MissingInActions.actions");
    private static final int PASTE_WITH_CARETS = 1;
    private static final int PASTE_SPLICED = 2;
    private static final int PASTE_SIMPLE = 3;
    private static final int PASTE_SPLICED_AND_QUOTED = 4;
    String myEolText;

    public MultiplePasteActionBase() {
        setEnabledInModalContext(true);
        myEolText = "⏎";
    }

    protected abstract boolean wantDuplicatedUserData();

    @NotNull
    protected abstract AnAction getPasteAction(@NotNull Editor editor, boolean recreateCaretsAction);

    protected abstract boolean isReplaceAware(@NotNull Editor editor, boolean recreateCaretsAction);

    @Nullable
    protected abstract Action getPasteAction(@NotNull JComponent focusedComponent);

    @NotNull
    protected abstract String getContentChooserTitle(@Nullable Editor editor, @NotNull JComponent focusedComponent);

    protected abstract boolean isEnabled(@Nullable Editor editor, @NotNull JComponent focusedComponent);

    @Nullable
    protected abstract String getCreateWithCaretsName(int caretCount);

    @Nullable
    protected abstract String getCreateWithCaretsTooltip(int caretCount);

    @Override
    final public void actionPerformed(final AnActionEvent e) {
        Component component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
        if (!(component instanceof JComponent)) return;

        final ApplicationSettings settings = ApplicationSettings.getInstance();
        final DataContext dataContext = e.getDataContext();
        final Project project = CommonDataKeys.PROJECT.getData(dataContext);
        final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        final JComponent focusedComponent = (JComponent) component;
        final CopyPasteManagerEx copyPasteManager = CopyPasteManagerEx.getInstanceEx();
        final HashMap<Transferable, ClipboardCaretContent> listEntryCarets = new HashMap<>();
        final boolean canCreateMultiCarets = editor != null && editor.getCaretModel().supportsMultipleCarets() && getCreateWithCaretsName(editor.getCaretModel().getCaretCount()) != null;
        final MultiPasteOptionsPane multiPasteOptionsPane = new MultiPasteOptionsPane();
        //final Shortcut[] moveLineUp = CommonUIShortcuts.getMoveLineUp().getShortcuts();
        //final Shortcut[] moveLineDown = CommonUIShortcuts.getMoveLineDown().getShortcuts();
        final boolean[] inContentManipulation = new boolean[] { false };
        final int[] alternateAction = new int[] { 0 };
        final HashMap<Transferable, String> stringTooLongSuffix = new HashMap<>();
        final DelayedRunner delayedRunner = new DelayedRunner();
        final Action[] convertToCaretsAction = new Action[] { null };
        final AnAction simplePasteAction = ActionManager.getInstance().getAction("EditorPasteSimple"); //IdeActions.ACTION_EDITOR_PASTE_SIMPLE);
        final boolean haveSimplePasteAction = simplePasteAction != null;
        final boolean[] convertToCaretsEnabled = new boolean[] { false };

        // Can change according to settings later
        // myEolText = "⏎";

        //noinspection unchecked
        final ContentChooser<Transferable>[] choosers = new ContentChooser[] { null };
        AwtRunnable listUpdater = new AwtRunnable(true, () -> {
            if (choosers[0] != null) {
                choosers[0].updateListContents(true);
                updateClipboardData(editor, copyPasteManager, multiPasteOptionsPane);
            }
        });

        updateClipboardData(editor, copyPasteManager, multiPasteOptionsPane);

        choosers[0] = new ContentChooser<Transferable>(project, getContentChooserTitle(editor, focusedComponent), true, true) {

            private int mySplicedQuotedActionIndex;
            private int mySplicedActionIndex;
            private boolean listenersInitialized = false;

            @Nullable
            protected String getHelpId() {
                return null;
            }

            @Override
            protected void init() {
                super.init();
                addActionListeners();
                Action action = getHelpAction();
                action.setEnabled(false);
            }

            private void addActionListeners() {
                // add action listeners
                final JButton button1 = getButton(myActions[mySplicedActionIndex]);
                if (button1 != null) {
                    button1.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseEntered(final MouseEvent e) {
                            super.mouseEntered(e);
                            multiPasteOptionsPane.setSpliceDelimiterTextHighlight(true);
                        }

                        @Override
                        public void mouseExited(final MouseEvent e) {
                            super.mouseExited(e);
                            multiPasteOptionsPane.setSpliceDelimiterTextHighlight(false);
                        }
                    });
                }

                JButton button2 = getButton(myActions[mySplicedQuotedActionIndex]);
                if (button2 != null) {
                    button2.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseEntered(final MouseEvent e) {
                            super.mouseEntered(e);
                            multiPasteOptionsPane.setQuoteTextHighlight(true);
                        }

                        @Override
                        public void mouseExited(final MouseEvent e) {
                            super.mouseExited(e);
                            multiPasteOptionsPane.setQuoteTextHighlight(false);
                        }
                    });
                }

                JButton optionsButton = getButton(myLeftSideActions[0]);
                if (optionsButton != null) {
                    optionsButton.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseEntered(final MouseEvent e) {
                            super.mouseEntered(e);
                            multiPasteOptionsPane.setPanelHighlight(true);
                        }

                        @Override
                        public void mouseExited(final MouseEvent e) {
                            super.mouseExited(e);
                            multiPasteOptionsPane.setPanelHighlight(false);
                        }
                    });
                }
            }

            @Override
            protected String getStringRepresentationFor(final Transferable content) {
                //return (String) content.getTransferData(DataFlavor.stringFlavor);
                return getStringRep(editor, content, settings.isMultiPasteShowEolInViewer(), false, true);
            }

            @NotNull
            @Override
            protected String getShortStringFor(final Transferable content, final String fullString) {
                ClipboardCaretContent caretContent = getCaretContent(content);
                final int caretCount = caretContent == null ? 1 : caretContent.getCaretCount();
                String contentText = getStringRep(editor, content, false, false, false);
                String eolText = settings.isMultiPasteShowEolInList()
                        && contentText.endsWith("\n") && (caretCount == 1 || caretContent.allFullLines()) ? myEolText : "";
                stringTooLongSuffix.put(content, eolText);
                return String.format("[%d] %s", caretCount, super.getShortStringFor(content, contentText));
            }

            @NotNull
            @Override
            protected String getShortStringTooLongSuffix(Transferable content) {
                final String s = stringTooLongSuffix.get(content);
                return super.getShortStringTooLongSuffix(content) + (s == null ? "" : s);
            }

            @Override
            protected Editor createIdeaEditor(final String text) {
                delayedRunner.runAll();

                final Editor viewer = super.createIdeaEditor(text);
                final EditorSettings settings = viewer.getSettings();
                settings.setFoldingOutlineShown(false);
                settings.setIndentGuidesShown(false);
                settings.setLeadingWhitespaceShown(true);
                settings.setLineMarkerAreaShown(true);
                settings.setLineNumbersShown(true);
                settings.setTrailingWhitespaceShown(true);
                return viewer;
            }

            @Override
            protected void updateViewerForSelection(@NotNull final Editor viewer, @NotNull List<Transferable> allContents, @NotNull int[] selectedIndices) {
                if (viewer.getDocument().getTextLength() > 0) {
                    // can create highlights in the editor to separate carets
                    final ClipboardCaretContent caretContent;
                    final Transferable content;
                    if (selectedIndices.length > 1) {
                        // combine indices
                        content = EditHelpers.getMergedTransferable(editor, allContents, selectedIndices, true);
                        caretContent = ClipboardCaretContent.studyTransferable(editor, content);
                    } else {
                        int index = selectedIndices[0];
                        content = allContents.get(index);
                        caretContent = getCaretContent(content);
                    }

                    assert caretContent != null;

                    // convert multi caret text to \n separated ranges
                    final String[] texts = caretContent.getTexts();

                    updateConvertToCarets(texts);

                    //noinspection VariableNotUsedInsideIf
                    if (texts != null) {
                        updateEditorHighlightRegions(viewer, caretContent, settings.isMultiPasteShowEolInViewer());

                        final FocusAdapter focusAdapter = new FocusAdapter() {
                            @Override
                            public void focusGained(final FocusEvent e) {
                                WriteCommandAction.runWriteCommandAction(viewer.getProject(), () -> {
                                    final Document document = viewer.getDocument();
                                    document.setReadOnly(false);
                                    document.replaceString(0, document.getTextLength(), getStringRep(editor, content, false, true, false));
                                    document.setReadOnly(true);
                                    updateEditorHighlightRegions(viewer, caretContent, false);
                                });
                            }

                            @Override
                            public void focusLost(final FocusEvent e) {
                                if (!viewer.isDisposed()) {
                                    final Document document = viewer.getDocument();
                                    final int textLength = document.getTextLength();
                                    if (textLength > 0) {
                                        WriteCommandAction.runWriteCommandAction(viewer.getProject(), () -> {
                                            document.setReadOnly(false);
                                            document.replaceString(0, document.getTextLength(), getStringRep(editor, content, settings.isMultiPasteShowEolInViewer(), false, true));
                                            document.setReadOnly(true);
                                            updateEditorHighlightRegions(viewer, caretContent, settings.isMultiPasteShowEolInViewer());
                                        });
                                    }
                                }
                            }
                        };

                        viewer.getContentComponent().addFocusListener(focusAdapter);
                        delayedRunner.addRunnable(() -> viewer.getContentComponent().removeFocusListener(focusAdapter));
                    }
                }
            }

            void updateEditorHighlightRegions(@NotNull Editor viewer, ClipboardCaretContent caretContent, boolean multiPasteShowEolInViewer) {
                final TextRange[] textRanges = caretContent.getTextRanges();
                final int iMax = textRanges.length;
                MarkupModelEx markupModel = (MarkupModelEx) DocumentMarkupModel.forDocument(viewer.getDocument(), project, true);
                final GutterIconRenderer charLinesSelection = new CaretIconRenderer(PluginIcons.Clipboard_char_lines_caret);
                final GutterIconRenderer charSelection = new CaretIconRenderer(PluginIcons.Clipboard_char_caret);
                final GutterIconRenderer lineSelection = new CaretIconRenderer(PluginIcons.Clipboard_line_caret);
                int offset = 0;

                markupModel.removeAllHighlighters();

                for (int i = 0; i < iMax; i++) {
                    final TextRange range = textRanges[i];
                    final int startOffset = range.getStartOffset() + offset;
                    offset += caretContent.isFullLine(i) && !multiPasteShowEolInViewer && i == iMax - 1 ? 0 : 1;
                    RangeHighlighter highlighter = markupModel.addLineHighlighter(viewer.offsetToLogicalPosition(startOffset).line, 1, null);
                    highlighter.setGutterIconRenderer(caretContent.isFullLine(i) ? lineSelection : caretContent.isCharLine(i) ? charLinesSelection : charSelection);
                }
            }

            @Override
            protected List<Transferable> getContents() {
                final Transferable[] contents = copyPasteManager.getAllContents();
                final ArrayList<Transferable> list = new ArrayList<>();
                list.addAll(Arrays.asList(contents));
                list.removeIf(content -> {
                    if (content instanceof StringSelection) {
                        try {
                            final String data = (String) content.getTransferData(DataFlavor.stringFlavor);
                            return data.isEmpty();
                        } catch (UnsupportedFlavorException | IOException e1) {
                            LOG.error("", e1);
                        }
                    }
                    return false;
                });
                return list;
            }

            @NotNull
            @Override
            public String getSelectedText() {
                if (choosers[0] == null) {
                    return super.getSelectedText();
                } else {
                    final int[] selectedIndices = choosers[0].getSelectedIndices();
                    if (selectedIndices.length > 1) {
                        // combine indices
                        Transferable content = EditHelpers.getMergedTransferable(editor, choosers[0].getAllContents(), selectedIndices, true);
                        return getStringRep(editor, content, settings.isMultiPasteShowEolInViewer(), false, true);
                    } else {
                        return super.getSelectedText();
                    }
                }
            }

            @Override
            protected void removeContentAt(final Transferable content) {
                copyPasteManager.removeContent(content);
                listEntryCarets.remove(content);
            }

            @Override
            protected void listKeyPressed(final KeyEvent event) {
                ContentChooser<Transferable> chooser = choosers[0];
                if (event.getKeyCode() != 0 && chooser != null) {
                    if (event.getKeyCode() == KeyEvent.VK_UP && (event.getModifiers() & InputEvent.ALT_MASK) != 0) {
                        // move selection up
                        moveSelections(chooser, true);
                        event.consume();
                    } else if (event.getKeyCode() == KeyEvent.VK_DOWN && (event.getModifiers() & InputEvent.ALT_MASK) != 0) {
                        moveSelections(chooser, false);
                        event.consume();
                    }
                }
            }

            protected void updateConvertToCarets(String[] texts) {
                // on creation this is called before the action is created, enabled state is saved so it could be used when
                // action is created
                boolean enabled = false;
                if (texts != null) {
                    for (String text : texts) {
                        if (text.indexOf('\n') != -1) {
                            enabled = true;
                            break;
                        }
                    }
                }

                convertToCaretsEnabled[0] = enabled;
                if (convertToCaretsAction[0] != null) {
                    convertToCaretsAction[0].setEnabled(enabled);
                }
            }

            protected void convertSelectionToCarets() {
                // split current selection to character carets
                ContentChooser<Transferable> chooser = choosers[0];
                final int[] selectedIndices = chooser.getSelectedIndices();

                if (selectedIndices.length > 0) {
                    Transferable content;
                    final ClipboardCaretContent caretContent;

                    if (selectedIndices.length == 1) {
                        content = chooser.getAllContents().get(selectedIndices[0]);
                        caretContent = getCaretContent(content);
                    } else {
                        content = EditHelpers.getMergedTransferable(editor, chooser.getAllContents(), selectedIndices, true);
                        caretContent = ClipboardCaretContent.studyTransferable(editor, content);
                    }

                    if (caretContent != null) {
                        // convert multi caret text to \n separated ranges
                        final String[] texts = caretContent.getTexts();

                        if (texts != null) {
                            Transferable convertedTransferable = EditHelpers.splitCaretTransferable(texts);
                            copyPasteManager.setContents(convertedTransferable);
                        }
                    }
                }
            }

            Action[] myActions;
            Action[] myLeftSideActions;

            @NotNull
            @Override
            protected Action[] createActions() {
                Action[] actions = super.createActions();
                if (canCreateMultiCarets) {
                    int actionOffset = haveSimplePasteAction ? 1 : 0;
                    Action[] multiCaretActions = new Action[actions.length + 3 + actionOffset];
                    System.arraycopy(actions, 0, multiCaretActions, 0, actions.length);
                    //noinspection SerializableInnerClassWithNonSerializableOuterClass
                    Action createWithMultiCarets = new OkAction() {
                        @Override
                        protected void doAction(final ActionEvent e) {
                            alternateAction[0] = PASTE_WITH_CARETS;
                            super.doAction(e);
                        }
                    };
                    final String name = getCreateWithCaretsName(editor.getCaretModel().getCaretCount());
                    final String tooltip = name == null ? Bundle.message("content-chooser.add-with-carets.description") : getCreateWithCaretsTooltip(editor.getCaretModel().getCaretCount());
                    createWithMultiCarets.putValue(Action.NAME, name == null ? Bundle.message("content-chooser.add-with-carets.label") : name);
                    if (tooltip != null) createWithMultiCarets.putValue(Action.SHORT_DESCRIPTION, tooltip);
                    multiCaretActions[actions.length + 2 + actionOffset] = createWithMultiCarets;

                    // create merge comma separate
                    Action spliced = new OkAction() {
                        @Override
                        protected void doAction(final ActionEvent e) {
                            alternateAction[0] = PASTE_SPLICED;
                            super.doAction(e);
                        }
                    };

                    spliced.putValue(Action.NAME, Bundle.message("content-chooser.splice-with-comma.label"));
                    spliced.putValue(Action.SHORT_DESCRIPTION, Bundle.message("content-chooser.splice-with-comma.description"));
                    mySplicedActionIndex = actions.length + actionOffset;

                    multiCaretActions[mySplicedActionIndex] = spliced;

                    // create merge comma separate
                    Action splicedQuoted = new OkAction() {
                        @Override
                        protected void doAction(final ActionEvent e) {
                            alternateAction[0] = PASTE_SPLICED_AND_QUOTED;
                            super.doAction(e);
                        }
                    };

                    splicedQuoted.putValue(Action.NAME, Bundle.message("content-chooser.splice-and-quote.label"));
                    splicedQuoted.putValue(Action.SHORT_DESCRIPTION, Bundle.message("content-chooser.splice-and-quote.description"));
                    mySplicedQuotedActionIndex = actions.length + 1 + actionOffset;
                    multiCaretActions[mySplicedQuotedActionIndex] = splicedQuoted;

                    if (haveSimplePasteAction) {
                        // create simple paste
                        Action pasteSimple = new OkAction() {
                            @Override
                            protected void doAction(final ActionEvent e) {
                                alternateAction[0] = PASTE_SIMPLE;
                                super.doAction(e);
                            }
                        };

                        //String actionText = ActionsBundle.actionText(IdeActions.ACTION_EDITOR_PASTE_SIMPLE);
                        pasteSimple.putValue(Action.NAME, Bundle.message("content-chooser.simple-paste.label"));
                        pasteSimple.putValue(Action.SHORT_DESCRIPTION, Bundle.message("content-chooser.simple-paste.description"));
                        multiCaretActions[actions.length] = pasteSimple;
                    }

                    myActions = multiCaretActions;
                    return multiCaretActions;
                }
                myActions = actions;
                return actions;
            }

            @NotNull
            @Override
            protected Action[] createLeftSideActions() {
                //noinspection SerializableInnerClassWithNonSerializableOuterClass
                Action showOptionsAction = new OkAction() {
                    @SuppressWarnings("SerializableStoresNonSerializable")
                    @Override
                    protected void doAction(final ActionEvent e) {
                        final boolean showOptions = !settings.isMultiPasteShowOptions();
                        settings.setMultiPasteShowOptions(showOptions);
                        putValue(Action.NAME, showOptions ? Bundle.message("content-chooser.hide-options.label") : Bundle.message("content-chooser.show-options.label"));
                        multiPasteOptionsPane.myPanel.setVisible(showOptions);
                    }
                };

                final boolean showOptions = settings.isMultiPasteShowOptions();
                showOptionsAction.putValue(Action.NAME, showOptions ? Bundle.message("content-chooser.hide-options.label") : Bundle.message("content-chooser.show-options.label"));
                showOptionsAction.putValue(Action.SHORT_DESCRIPTION, Bundle.message("content-chooser.show-options.description"));

                Action convertToCarets = new OkAction() {
                    @SuppressWarnings("SerializableStoresNonSerializable")
                    @Override
                    protected void doAction(final ActionEvent e) {
                        convertSelectionToCarets();
                    }
                };

                convertToCarets.putValue(Action.NAME, Bundle.message("content-chooser.convert-to-carets.label"));
                convertToCarets.putValue(Action.SHORT_DESCRIPTION, Bundle.message("content-chooser.convert-to-carets.description"));
                convertToCarets.setEnabled(convertToCaretsEnabled[0]);
                convertToCaretsAction[0] = convertToCarets;
                myLeftSideActions = new Action[] { showOptionsAction, convertToCarets };
                return myLeftSideActions;
            }

            @Nullable
            @Override
            protected JComponent getAboveEditorComponent() {
                String copyShortcut = CommonUIShortcuts.getNthShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_COPY), 1);
                String deleteShortcut = CommonUIShortcuts.getNthShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_DELETE), 1);
                String moveLineUpShortcut = CommonUIShortcuts.getNthShortcutText(ActionManager.getInstance().getAction(CommonUIShortcuts.ACTION_MOVE_LINE_UP_ACTION), 2);
                String moveLineDownShortcut = CommonUIShortcuts.getNthShortcutText(ActionManager.getInstance().getAction(CommonUIShortcuts.ACTION_MOVE_LINE_DOWN_ACTION), 2);
                if (!copyShortcut.isEmpty()) copyShortcut = String.format(" (%s)", copyShortcut);
                if (!deleteShortcut.isEmpty()) deleteShortcut = String.format(" (%s)", deleteShortcut);
                if (!moveLineUpShortcut.isEmpty()) moveLineUpShortcut = String.format(" (%s)", moveLineUpShortcut);
                if (!moveLineDownShortcut.isEmpty()) moveLineDownShortcut = String.format(" (%s)", moveLineDownShortcut);
                String bannerSlogan = Bundle.indexedMessage("content-chooser.above-editor.description", copyShortcut, deleteShortcut, moveLineUpShortcut, moveLineDownShortcut);
                multiPasteOptionsPane.setContentBody(Utils.join(bannerSlogan.split("\n"), "<ul align='left'>", "</ul>", "<li>", "</li>"));
                multiPasteOptionsPane.setSettingsChangedRunnable(() -> {
                    if (choosers[0] != null) {
                        final int[] indices = choosers[0].getSelectedIndices();
                        choosers[0].updateListContents(false);
                        choosers[0].setSelectedIndices(indices);
                    }
                });

                multiPasteOptionsPane.myPanel.setVisible(settings.isMultiPasteShowOptions());
                return multiPasteOptionsPane.myPanel;
            }

            private void moveSelections(final ContentChooser<Transferable> chooser, boolean moveUp) {
                final int[] indices = chooser.getSelectedIndices();
                final List<Transferable> allContents = new ArrayList<>(chooser.getAllContents());
                boolean allConsecutive = true;
                int firstIndex = -1;
                int lastIndex = -1;

                for (int index : indices) {
                    if (firstIndex == -1) {
                        firstIndex = index;
                        lastIndex = index;
                    } else {
                        if (lastIndex + 1 != index) {
                            allConsecutive = false;
                        }
                        lastIndex = index;
                    }
                }

                List<Transferable> moved = new ArrayList<>();

                int anchorIndex = moveUp ? firstIndex : lastIndex;
                for (int index : indices) {
                    if (allConsecutive || index != anchorIndex) {
                        moved.add(allContents.get(index));
                    }
                }

                if (!allConsecutive) {
                    allContents.removeAll(moved);
                    allContents.addAll(anchorIndex + (moveUp ? 1 : 0), moved);
                    reOrderContentList(allContents);
                } else {
                    final int size = lastIndex - firstIndex + 1;
                    final int halfSize = size / 2;
                    if (moveUp) {
                        if (firstIndex <= 0) {
                            // reverse
                            if (halfSize > 0) {
                                for (int i = 0; i < halfSize; i++) {
                                    Transferable first = allContents.get(firstIndex + i);
                                    Transferable last = allContents.get(lastIndex - i);
                                    allContents.set(firstIndex + i, last);
                                    allContents.set(lastIndex - i, first);
                                }
                                reOrderContentList(allContents);
                            }
                        } else {
                            allContents.removeAll(moved);
                            allContents.addAll(firstIndex - 1, moved);
                            reOrderContentList(allContents);
                            anchorIndex = firstIndex - 1;
                        }
                    } else {
                        if (lastIndex + 1 >= allContents.size()) {
                            // reverse order
                            if (halfSize > 0) {
                                for (int i = 0; i < halfSize; i++) {
                                    Transferable first = allContents.get(firstIndex + i);
                                    Transferable last = allContents.get(lastIndex - i);
                                    allContents.set(firstIndex + i, last);
                                    allContents.set(lastIndex - i, first);
                                }
                                reOrderContentList(allContents);
                            }
                        } else {
                            allContents.removeAll(moved);
                            allContents.addAll(lastIndex + 2 - moved.size(), moved);
                            reOrderContentList(allContents);
                            anchorIndex = lastIndex + 1;
                        }
                    }
                }

                chooser.updateListContents(true);
                int index = 0;
                if (moveUp) {
                    for (int i = anchorIndex; index < indices.length; i++) indices[index++] = i;
                } else {
                    for (int i = anchorIndex; index < indices.length; i--) indices[index++] = i;
                }
                chooser.setSelectedIndices(indices);
            }

            private void reOrderContentList(@NotNull List<Transferable> allContents) {
                inContentManipulation[0] = true;
                try {
                    int iMax = allContents.size();
                    Transferable[] currentOrder = copyPasteManager.getAllContents();
                    int i;
                    for (i = iMax; i-- > 0; ) {
                        if (currentOrder[i] != allContents.get(i)) break;
                    }

                    for (i++; i-- > 0; ) {
                        copyPasteManager.moveContentToStackTop(allContents.get(i));
                    }
                } finally {
                    inContentManipulation[0] = false;
                }
            }

            @Override
            protected Icon getListEntryIcon(@NotNull final Transferable content) {
                final ClipboardCaretContent caretContent = getCaretContent(content);
                return caretContent != null && caretContent.getCaretCount() > 1 ? PluginIcons.Clipboard_carets : PluginIcons.Clipboard_text;
            }

            @Nullable
            private ClipboardCaretContent getCaretContent(final @NotNull Transferable content) {
                ClipboardCaretContent caretContent = listEntryCarets.get(content);
                if (!listEntryCarets.containsKey(content)) {
                    caretContent = ClipboardCaretContent.studyTransferable(editor, content);
                    listEntryCarets.put(content, caretContent);
                }
                return caretContent;
            }
        };

        final ContentChooser<Transferable> chooser = choosers[0];
        final ContentChangedListener contentChangedListener = (oldTransferable, newTransferable) -> {
            if (!inContentManipulation[0]) {
                listUpdater.run();
            }
        };

        try {
            copyPasteManager.addContentChangedListener(contentChangedListener);
            if (!chooser.getAllContents().isEmpty()) {
                chooser.show();
            } else {
                chooser.close(DialogWrapper.CANCEL_EXIT_CODE);
            }
        } finally {
            copyPasteManager.removeContentChangedListener(contentChangedListener);
        }

        if (chooser.isOK()) {
            final int[] selectedIndices = chooser.getSelectedIndices();
            if (selectedIndices.length == 1) {
                copyPasteManager.moveContentToStackTop(chooser.getAllContents().get(selectedIndices[0]));
            } else {
                copyPasteManager.setContents(EditHelpers.getMergedTransferable(editor, chooser.getAllContents(), selectedIndices, true));
            }

            if (editor != null) {
                if (editor.isViewer()) return;

                LineSelectionManager manager = LineSelectionManager.getInstance(editor);
                String[] userData = null;
                if (settings.isReplaceMacroVariables() && settings.isIncludeUserDefinedMacro()) {
                    if (settings.isUserDefinedMacroClipContent()) {
                        final Transferable[] allContents = copyPasteManager.getAllContents();
                        int index = 0;
                        for (Transferable item : allContents) {
                            final ClipboardCaretContent caretContent = ClipboardCaretContent.studyTransferable(editor, item);
                            if (caretContent != null && caretContent.allChars()) {
                                if (index == multiPasteOptionsPane.getSelectedClipboardContentIndex()) {
                                    userData = caretContent.getTexts();
                                    break;
                                }
                                index++;
                            }
                        }
                    } else {
                        userData = new String[] { settings.getUserDefinedMacroReplace() };
                    }
                }

                manager.setOnPasteReplacementText(EditHelpers.getOnPasteReplacements(editor));

                if (settings.isIncludeUserDefinedMacro()) {
                    if (userData != null && userData.length > 0) {
                        String search = settings.getUserDefinedMacroSearch();
                        if (!search.isEmpty()) {
                            manager.setOnPasteUserSearchPattern(new SearchPattern(search, settings.isRegexUserDefinedMacro()));
                        }
                    }
                } else {
                    manager.setOnPasteUserSearchPattern(null);
                }

                if (settings.isIncludeUserDefinedMacro()) {
                    if (userData != null && userData.length > 0) {
                        String search = settings.getUserDefinedMacroSearch();
                        if (!search.isEmpty()) {
                            manager.setOnPasteUserSearchPattern(new SearchPattern(search, settings.isRegexUserDefinedMacro()));
                            manager.setOnPasteUserReplacementText(userData[0]);
                        }
                    }
                }

                boolean recreateCarets = alternateAction[0] == PASTE_WITH_CARETS;
                if (alternateAction[0] == PASTE_SPLICED) {
                    // we handle the splicing and adding this content to the clipboard
                    final Transferable contents = CopyPasteManager.getInstance().getContents();
                    if (contents != null) {
                        ClipboardCaretContent clipboardCaretContent = ClipboardCaretContent.studyTransferable(editor, contents);
                        if (clipboardCaretContent != null) {
                            Transferable mergedTransferable = EditHelpers.getJoinedTransferable(clipboardCaretContent
                                    , settings.getSpliceDelimiterText()
                                    , false //settings.isQuoteSplicedItems()
                                    , settings.getOpenQuoteText()
                                    , settings.getClosedQuoteText());
                            CopyPasteManager.getInstance().setContents(mergedTransferable);
                        }
                    }
                } else if (alternateAction[0] == PASTE_SPLICED_AND_QUOTED) {
                    // we handle the splicing and adding this content to the clipboard
                    final Transferable contents = CopyPasteManager.getInstance().getContents();
                    if (contents != null) {
                        ClipboardCaretContent clipboardCaretContent = ClipboardCaretContent.studyTransferable(editor, contents);
                        if (clipboardCaretContent != null) {
                            Transferable mergedTransferable = EditHelpers.getJoinedTransferable(clipboardCaretContent
                                    , settings.getSpliceDelimiterText()
                                    , true//settings.isQuoteSplicedItems()
                                    , settings.getOpenQuoteText()
                                    , settings.getClosedQuoteText());
                            CopyPasteManager.getInstance().setContents(mergedTransferable);
                        }
                    }
                }

                final AnAction pasteAction = alternateAction[0] == PASTE_SPLICED || alternateAction[0] == PASTE_SPLICED_AND_QUOTED ? simplePasteAction : getPasteAction(editor, recreateCarets);

                if (pasteAction != null) {
                    if (manager.haveOnPasteReplacements() && (!isReplaceAware(editor, recreateCarets) || (userData != null && userData.length > 1 && wantDuplicatedUserData()))) {
                        // need to create the replaced content
                        final Transferable contents = CopyPasteManager.getInstance().getContents();
                        if (contents != null) {
                            ClipboardCaretContent clipboardCaretContent = ClipboardCaretContent.studyTransferable(editor, contents);
                            if (clipboardCaretContent != null) {
                                Transferable mergedTransferable = (userData != null && userData.length > 1) ? EditHelpers.getReplacedTransferable(editor, clipboardCaretContent, userData)
                                        : EditHelpers.getReplacedTransferable(editor, clipboardCaretContent);
                                CopyPasteManager.getInstance().setContents(mergedTransferable);
                            }
                        }
                    }

                    AnActionEvent newEvent = new AnActionEvent(e.getInputEvent(),
                            DataManager.getInstance().getDataContext(focusedComponent),
                            e.getPlace(), e.getPresentation(),
                            ActionManager.getInstance(),
                            e.getModifiers());

                    pasteAction.actionPerformed(newEvent);
                }
            } else {
                final Action pasteAction = getPasteAction(focusedComponent);
                if (pasteAction != null) {
                    pasteAction.actionPerformed(new ActionEvent(focusedComponent, ActionEvent.ACTION_PERFORMED, ""));
                }
            }
        }
    }

    public static void updateClipboardData(final Editor editor, final CopyPasteManagerEx copyPasteManager, final MultiPasteOptionsPane myEmptyContentDescription) {
        ArrayList<String> selections = new ArrayList<>();
        final Transferable[] allContents = copyPasteManager.getAllContents();
        int index = 0;
        for (Transferable item : allContents) {
            final ClipboardCaretContent caretContent = ClipboardCaretContent.studyTransferable(editor, item);
            if (caretContent != null && caretContent.allChars()) {
                String displayName = String.format("%d - [%d] %s", index, caretContent.getCaretCount(), caretContent.getStringRep(30, null, false, false));
                selections.add(displayName);
            }
            index++;
        }
        myEmptyContentDescription.updatedClipboardContents(selections);
    }

    @NotNull
    String getStringRep(final @Nullable Editor editor, final Transferable content, final boolean showEOL, final boolean addCharFinalEOL, final boolean removeFullLineEOL) {
        final ClipboardCaretContent caretContent = ClipboardCaretContent.studyTransferable(editor, content);
        if (caretContent != null) {
            return caretContent.getStringRep(0, showEOL ? myEolText : null, addCharFinalEOL, removeFullLineEOL);
        }
        return "";
    }

    @Override
    final public void update(AnActionEvent e) {
        final boolean enabled;

        Object component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
        if (!(component instanceof JComponent)) enabled = false;
        else {
            Editor editor = e.getData(CommonDataKeys.EDITOR);
            if (editor != null) enabled = !editor.isViewer() && isEnabled(editor, (JComponent) component);
            else {
                enabled = isEnabled(null, (JComponent) component) && getPasteAction((JComponent) component) != null;
            }
        }

        if (ActionPlaces.isPopupPlace(e.getPlace())) {
            e.getPresentation().setVisible(enabled);
        } else {
            e.getPresentation().setEnabled(enabled);
            e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || enabled);
        }
    }

    private static class CaretIconRenderer extends GutterIconRenderer {
        private final Icon myIcon;

        public CaretIconRenderer(Icon icon) {
            myIcon = icon;
        }

        @Override
        public boolean equals(final Object obj) {
            //noinspection CallToSimpleGetterFromWithinClass
            return obj instanceof CaretIconRenderer && ((CaretIconRenderer) obj).getIcon() == myIcon;
        }

        @Override
        public int hashCode() {
            return myIcon.hashCode();
        }

        @NotNull
        @Override
        public Icon getIcon() {
            return myIcon;
        }
    }
}
