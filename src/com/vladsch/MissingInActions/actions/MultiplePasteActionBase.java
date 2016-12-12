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
package com.vladsch.MissingInActions.actions;

import com.intellij.ide.CopyPasteManagerEx;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.vladsch.MissingInActions.Bundle;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.*;
import com.vladsch.MissingInActions.util.ui.ContentChooser;
import com.vladsch.MissingInActions.util.ui.EmptyContentPane;
import icons.PluginIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.intellij.openapi.diagnostic.Logger.getInstance;

public abstract class MultiplePasteActionBase extends AnAction implements DumbAware {
    private static final Logger LOG = getInstance("com.vladsch.MissingInActions.actions");

    public MultiplePasteActionBase() {
        setEnabledInModalContext(true);
    }

    @NotNull
    protected abstract AnAction getPasteAction(@NotNull Editor editor);

    @Nullable
    protected abstract Action getPasteAction(@NotNull JComponent focusedComponent);

    @NotNull
    protected abstract String getContentChooserTitle(@Nullable Editor editor, @NotNull JComponent focusedComponent);

    protected abstract boolean isEnabled(@Nullable Editor editor, @NotNull JComponent focusedComponent);

    @Override
    final public void actionPerformed(final AnActionEvent e) {
        Component component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
        if (!(component instanceof JComponent)) return;

        final DataContext dataContext = e.getDataContext();
        final Project project = CommonDataKeys.PROJECT.getData(dataContext);
        final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        final JComponent focusedComponent = (JComponent) component;
        final CopyPasteManagerEx copyPasteManager = CopyPasteManagerEx.getInstanceEx();
        final HashMap<Transferable, ClipboardCaretContent> listEntryCarets = new HashMap<>();
        final int showEOL = 0;
        final EmptyContentPane myEmptyContentDescription = new EmptyContentPane();
        final boolean showInstructions = ApplicationSettings.getInstance().isOverrideStandardPasteShowInstructions();
        final Shortcut[] moveLineUp = CommonUIShortcuts.getMoveLineUp().getShortcuts();
        final Shortcut[] moveLineDown = CommonUIShortcuts.getMoveLineDown().getShortcuts();
        final CancellableRunnable[] scheduled = new CancellableRunnable[] { CancellableRunnable.NULL };

        //noinspection unchecked
        final ContentChooser<Transferable>[] choosers = new ContentChooser[] { null };
        AwtRunnable listUpdater = new AwtRunnable(true, () -> {
            if (choosers[0] != null) choosers[0].updateListContents();
        });

        choosers[0] = new ContentChooser<Transferable>(project, getContentChooserTitle(editor, focusedComponent), true, true) {
            @Override
            protected String getStringRepresentationFor(final Transferable content) {
                //return (String) content.getTransferData(DataFlavor.stringFlavor);
                final ClipboardCaretContent caretContent = getCaretContent(content);
                if (caretContent != null) {
                    // convert multi caret text to \n separated ranges
                    StringBuilder sb = new StringBuilder();
                    final String[] texts = caretContent.getTexts();
                    if (texts != null) {
                        int iMax = caretContent.getCaretCount();
                        for (int i = 0; i < iMax; i++) {
                            //noinspection ConstantConditions
                            if (showEOL > 0) {
                                if (caretContent.isFullLine(i)) {
                                    sb.append(texts[i].substring(0, texts[i].length() - 1));
                                    sb.append('⏎');
                                }
                                sb.append('\n');
                            } else {
                                sb.append(texts[i]);
                                if (!caretContent.isFullLine(i)) sb.append('\n');
                            }
                        }

                        // remove last EOL so that line count reflects inserted lines
                        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
                            sb.delete(sb.length() - 1, sb.length());
                        }
                        return sb.toString();
                    }
                }
                return "";
            }

            @NotNull
            @Override
            protected String getShortStringFor(final Transferable content, final String fullString) {
                ClipboardCaretContent caretContent = getCaretContent(content);
                return String.format("[%d] %s", caretContent == null ? 1 : caretContent.getCaretCount(), super.getShortStringFor(content, fullString));
            }

            @Override
            protected Editor createIdeaEditor(final String text) {
                final Editor viewer = super.createIdeaEditor(text);
                final EditorSettings settings = viewer.getSettings();
                settings.setFoldingOutlineShown(false);
                settings.setGutterIconsShown(true);
                settings.setIndentGuidesShown(false);
                settings.setLeadingWhitespaceShown(true);
                settings.setLineMarkerAreaShown(true);
                settings.setLineNumbersShown(true);
                settings.setTrailingWhitespaceShown(true);
                return viewer;
            }

            @Override
            protected void updateViewerForSelection(@NotNull final Editor viewer, @NotNull List<Transferable> allContents, @NotNull int[] selectedIndices) {
                if (editor != null && viewer.getDocument().getTextLength() > 0) {
                    if (selectedIndices.length == 1) {
                        // can create highlights in the editor to separate carets
                        int index = selectedIndices[0];
                        Transferable content = allContents.get(index);
                        final ClipboardCaretContent caretContent = getCaretContent(content);
                        if (caretContent != null) {
                            // convert multi caret text to \n separated ranges
                            final String[] texts = caretContent.getTexts();
                            if (texts != null) {
                                int iMax = caretContent.getCaretCount();
                                final TextRange[] textRanges = caretContent.getTextRanges();
                                int offset = 0;

                                TextAttributes oddAttr = editor.getColorsScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
                                TextAttributes evenAttr = editor.getColorsScheme().getAttributes(EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES);
                                MarkupModelEx markupModel = (MarkupModelEx) DocumentMarkupModel.forDocument(viewer.getDocument(), project, true);
                                final GutterIconRenderer charLinesSelection = new CaretIconRenderer(PluginIcons.Clipboard_char_lines_caret);
                                final GutterIconRenderer charSelection = new CaretIconRenderer(PluginIcons.Clipboard_char_caret);
                                final GutterIconRenderer lineSelection = new CaretIconRenderer(PluginIcons.Clipboard_line_caret);

                                for (int i = 0; i < iMax; i++) {
                                    final TextRange range = textRanges[i];
                                    final int startOffset = range.getStartOffset() + offset;
                                    final int endOffset = range.getEndOffset() + offset;
                                    Caret caret = i == 0 ? viewer.getCaretModel().getPrimaryCaret() : viewer.getCaretModel().addCaret(viewer.offsetToVisualPosition(startOffset));
                                    if (caret != null) {
                                        caret.moveToOffset(startOffset);
                                        //caret.setSelection(startOffset, endOffset);
                                        offset += caretContent.isFullLine(i) ? showEOL : 1;
                                    }

                                    RangeHighlighter highlighter = markupModel.addLineHighlighter(viewer.offsetToLogicalPosition(startOffset).line, 1, (i % 2) != 0 ? oddAttr : evenAttr);
                                    highlighter.setGutterIconRenderer(caretContent.isFullLine(i) ? lineSelection : caretContent.isCharLine(i) ? charLinesSelection : charSelection);
                                }

                                viewer.getContentComponent().addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusGained(final FocusEvent e) {
                                        WriteCommandAction.runWriteCommandAction(viewer.getProject(), () -> {
                                            final Document document = viewer.getDocument();
                                            document.setReadOnly(false);
                                            document.insertString(document.getTextLength(), "\n");
                                            document.setReadOnly(true);
                                        });
                                    }

                                    @Override
                                    public void focusLost(final FocusEvent e) {
                                        final Document document = viewer.getDocument();
                                        final int textLength = document.getTextLength();
                                        if (textLength > 0) {
                                            WriteCommandAction.runWriteCommandAction(viewer.getProject(), () -> {
                                                document.setReadOnly(false);
                                                document.deleteString(textLength - 1, textLength);
                                                document.setReadOnly(true);
                                            });
                                        }
                                    }
                                });
                            }
                        }
                    }
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
                        } catch (UnsupportedFlavorException e1) {
                            LOG.error("", e1);
                        } catch (IOException e1) {
                            LOG.error("", e1);
                        }
                    }
                    return false;
                });
                return list;
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
                    if (event.getKeyCode() == KeyEvent.VK_UP && (event.getModifiers() & KeyEvent.ALT_MASK) != 0) {
                        // move selection up
                        final int[] indices = chooser.getSelectedIndices();
                        final List<Transferable> allContents = chooser.getAllContents();
                        int iMax = indices.length;
                        for (int i = iMax; i-- > 0; ) {
                            int index = indices[i];
                            if (index > 0) {
                                // can move it up
                                copyPasteManager.moveContentToStackTop(allContents.get(index));
                                scheduled[0].cancel();
                            }
                        }
                        event.consume();
                        listUpdater.run();
                    } else if (event.getKeyCode() == KeyEvent.VK_DOWN && (event.getModifiers() & KeyEvent.ALT_MASK) != 0) {
                        // move selection down
                        event.consume();
                    }
                }
            }

            @Override
            protected Icon getListEntryIcon(@NotNull final Transferable content) {
                final ClipboardCaretContent caretContent = getCaretContent(content);
                return caretContent != null && caretContent.getCaretCount() > 1 ? PluginIcons.Clipboard_carets : PluginIcons.Clipboard_text;
            }

            @Nullable
            private ClipboardCaretContent getCaretContent(final @NotNull Transferable content) {
                if (editor != null) {
                    ClipboardCaretContent caretContent = listEntryCarets.get(content);
                    if (!listEntryCarets.containsKey(content)) {
                        caretContent = ClipboardCaretContent.studyTransferable(editor, content);
                        listEntryCarets.put(content, caretContent);
                    }
                    return caretContent;
                }
                return null;
            }

            @Nullable
            @Override
            protected JComponent getAboveEditorComponent() {
                if (showInstructions) {
                    String copyShortcut = KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_COPY));
                    String deleteShortcut = KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_DELETE));
                    if (!copyShortcut.isEmpty()) copyShortcut = String.format(" (%s)", copyShortcut);
                    if (!deleteShortcut.isEmpty()) deleteShortcut = String.format(" (%s)", deleteShortcut);
                    String bannerSlogan = Bundle.message("content-chooser.above-editor.description", copyShortcut, deleteShortcut);
                    myEmptyContentDescription.setContentBody(Utils.join(bannerSlogan.split("\n"), "<ul align='left'>", "</ul>", "<li>", "</li>"));
                    return myEmptyContentDescription.myPanel;
                }
                return null;
            }
        };

        final ContentChooser<Transferable> chooser = choosers[0];
        final CopyPasteManager.ContentChangedListener contentChangedListener = new CopyPasteManager.ContentChangedListener() {
            @Override
            public void contentChanged(@Nullable final Transferable oldTransferable, final Transferable newTransferable) {
                scheduled[0].cancel();
                scheduled[0] = OneTimeRunnable.schedule(50, listUpdater);
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
                copyPasteManager.setContents(new StringSelection(chooser.getSelectedText() + "\n"));
            }

            if (editor != null) {
                if (editor.isViewer()) return;

                final AnAction pasteAction = getPasteAction(editor);

                AnActionEvent newEvent = new AnActionEvent(e.getInputEvent(),
                        DataManager.getInstance().getDataContext(focusedComponent),
                        e.getPlace(), e.getPresentation(),
                        ActionManager.getInstance(),
                        e.getModifiers());

                pasteAction.actionPerformed(newEvent);
            } else {
                final Action pasteAction = getPasteAction(focusedComponent);
                if (pasteAction != null) {
                    pasteAction.actionPerformed(new ActionEvent(focusedComponent, ActionEvent.ACTION_PERFORMED, ""));
                }
            }
        }
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
        }
    }

    private static class CaretIconRenderer extends GutterIconRenderer {
        private final Icon myIcon;

        public CaretIconRenderer(Icon icon) {
            myIcon = icon;
        }

        @Override
        public boolean equals(final Object obj) {
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
