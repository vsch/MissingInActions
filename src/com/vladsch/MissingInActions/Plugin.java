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

package com.vladsch.MissingInActions;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.hints.ParameterHintsPassFactory;
import com.intellij.ide.CopyPasteManagerEx;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.AnActionResult;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.ComponentUtil;
import com.vladsch.MissingInActions.actions.character.MiaMultiplePasteAction;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.EditorActionListener;
import com.vladsch.MissingInActions.util.EditorActiveLookupListener;
import com.vladsch.MissingInActions.util.MiaCancelableJobScheduler;
import com.vladsch.MissingInActions.util.SharedCaretStateTransferableData;
import com.vladsch.MissingInActions.util.highlight.MiaWordHighlightProviderImpl;
import com.vladsch.flexmark.util.misc.Pair;
import com.vladsch.plugin.util.AppUtils;
import com.vladsch.plugin.util.AwtRunnable;
import com.vladsch.plugin.util.OneTimeRunnable;
import com.vladsch.plugin.util.ui.ColorIterable;
import com.vladsch.plugin.util.ui.CommonUIShortcuts;
import com.vladsch.plugin.util.ui.highlight.HighlightListener;
import com.vladsch.plugin.util.ui.highlight.WordHighlightProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.service.SharedThreadPool;

import javax.swing.JComponent;
import javax.swing.text.JTextComponent;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.vladsch.plugin.util.AppUtils.isParameterHintsForceUpdateAvailable;

public class Plugin extends MiaWordHighlightProviderImpl implements Disposable {
    private static final Logger LOG = Logger.getInstance("com.vladsch.MissingInActions");

    @SuppressWarnings("unused") final public static int FEATURE_ENHANCED = 1;
    final public static int FEATURE_DEVELOPMENT = 2;
    public static final Editor[] EMPTY_EDITORS = new Editor[0];
    public static final EditorActionListener[] EMPTY_EDITOR_ACTION_LISTENERS = new EditorActionListener[0];
    final private HashMap<Editor, LineSelectionManager> myLineSelectionManagers;
    final private HashMap<AnActionEvent, Editor> myActionEventEditorMap;
    final private HashMap<Editor, LinkedHashSet<EditorActionListener>> myEditorActionListeners;
    private @Nullable WordHighlightProvider<ApplicationSettings> myProjectHighlightProvider;
    final private HashSet<Editor> myPasteOverrideEditors;
    final private HashSet<Editor> myDoNotUpdateHighlightersEditors;
    final private AnAction myMultiPasteAction;
    private @Nullable JComponent myPasteOverrideComponent;
    private boolean myInContentManipulation;
    private boolean mySavedShowParameterHints;
    private boolean myDisabledShowParameterHints;
    final private boolean myParameterHintsAvailable;
    private boolean myRegisterCaretStateTransferable;
    private OneTimeRunnable myHighlightSaveTask = OneTimeRunnable.NULL;
    private boolean disableSaveHighlights = true;
    private boolean highlightProjectViewNodes;
    private @Nullable HighlightListener mySearchReplaceHighlightListener;
    private OneTimeRunnable myEditorHighlightRunner = OneTimeRunnable.NULL;
    private boolean myInSetProjectHighlighter = false;

    public Plugin() {
        super(ApplicationSettings.getInstance());
        myLineSelectionManagers = new HashMap<>();
        myPasteOverrideEditors = new HashSet<>();
        myDoNotUpdateHighlightersEditors = new HashSet<>();
        myMultiPasteAction = new MiaMultiplePasteAction();
        myActionEventEditorMap = new HashMap<>();
        myProjectHighlightProvider = null;
        myEditorActionListeners = new HashMap<>();
        myPasteOverrideComponent = null;
        myParameterHintsAvailable = AppUtils.isParameterHintsAvailable();
        highlightProjectViewNodes = mySettings.isHighlightProjectViewNodes();

        MiaCancelableJobScheduler.getInstance().schedule(1000, () -> {
            Map<String, Pair<Integer, Integer>> state = mySettings.getHighlightState();
            if (state != null) {
                setHighlightState(state);
                setHighlightCaseSensitive(mySettings.isHighlightWordsCaseSensitive());
                setHighlightWordsMatchBoundary(mySettings.isHighlightWordsMatchBoundary());
                fireHighlightsChanged();
            } else {
                clearHighlights();
            }
            disableSaveHighlights = false;
        });

        initComponent();
    }

    public void registerDoNotUpdateHighlightersEditors(@NotNull Editor editor) {
        myDoNotUpdateHighlightersEditors.add(editor);
    }

    @NotNull
    public LineSelectionManager getSelectionManager(Editor editor) {
        return myLineSelectionManagers.computeIfAbsent(editor, e -> new LineSelectionManager(editor));
    }

    @Override
    public void dispose() {

    }

    @Override
    public void disposeComponent() {
        disableSaveHighlights = true;
        super.disposeComponent();
    }

    @Override
    public void fireHighlightsChanged() {
        if (!disableSaveHighlights) {
            // save highlights in application settings
            myHighlightSaveTask.cancel();
            myHighlightSaveTask = OneTimeRunnable.schedule(MiaCancelableJobScheduler.getInstance(), "Highlight Saver", 500, ModalityState.NON_MODAL, () -> {
                mySettings.setHighlightState(getHighlightState());
                mySettings.setHighlightWordsCaseSensitive(isHighlightCaseSensitive());
                mySettings.setHighlightWordsMatchBoundary(isHighlightWordsMatchBoundary());

                if (mySettings.isHighlightProjectViewNodes()) {
                    updateProjectViews();
                }
            });
        } else {
            myHighlightSaveTask.cancel();
            myHighlightSaveTask = OneTimeRunnable.schedule(MiaCancelableJobScheduler.getInstance(), "Highlight Saver", 500, ModalityState.NON_MODAL, () -> {
                if (mySettings.isHighlightProjectViewNodes()) {
                    updateProjectViews();
                }
            });
        }

        super.fireHighlightsChanged();
    }

    static void updateProjectViews() {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : projects) {
            ProjectView projectView = ProjectView.getInstance(project);
            AbstractProjectViewPane projectViewPane = projectView.getCurrentProjectViewPane();
            if (projectViewPane != null) {
                projectViewPane.updateFromRoot(true);
            }
        }
    }

    public void setHighlightProjectViewNodes(final boolean highlightProjectViewNodes) {
        if (mySettings.isHighlightProjectViewNodes() != highlightProjectViewNodes) {
            mySettings.setHighlightProjectViewNodes(highlightProjectViewNodes);

            if (haveHighlights()) {
                updateProjectViews();
            }
        }
    }

    @Override
    public void initComponent() {
        super.initComponent();

        SharedCaretStateTransferableData.initialize(this);

        myDelayedRunner.addRunnable(SharedCaretStateTransferableData::dispose);

        if (myParameterHintsAvailable) {
            mySavedShowParameterHints = EditorSettingsExternalizable.getInstance().getOptions().SHOW_PARAMETER_NAME_HINTS;

            // restore setting on exit, just in case editor change listener didn't
            myDelayedRunner.addRunnable(() -> EditorSettingsExternalizable.getInstance().getOptions().SHOW_PARAMETER_NAME_HINTS = mySavedShowParameterHints);
        }

        myDisabledShowParameterHints = false;

        // register editor factory listener
        final EditorFactoryListener editorFactoryListener = new EditorFactoryListener() {
            @Override
            public void editorCreated(@NotNull final EditorFactoryEvent event) {
                Plugin.this.editorCreated(event);
            }

            @Override
            public void editorReleased(@NotNull final EditorFactoryEvent event) {
                Plugin.this.editorReleased(event);
            }
        };

        EditorFactory.getInstance().addEditorFactoryListener(editorFactoryListener, this);
        myDelayedRunner.addRunnable(() -> {
            Set<Editor> editorSet = myLineSelectionManagers.keySet();
            Editor[] editors = editorSet.toArray(EMPTY_EDITORS);
            for (Editor editor : editors) {
                LineSelectionManager manager = myLineSelectionManagers.remove(editor);
                if (manager != null) {
                    Disposer.dispose(manager);
                }
            }
        });

        if (myParameterHintsAvailable) {
            FileEditorManagerListener editorManagerListener = new FileEditorManagerListener() {
                @Override
                public void selectionChanged(@NotNull final FileEditorManagerEvent event) {
                    updateEditorParameterHints(getEditorEx(event.getNewEditor()), event.getNewEditor() != event.getOldEditor());
                }
            };

            ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, editorManagerListener);
        }

        final IdeEventQueue.EventDispatcher eventDispatcher = Plugin.this::dispatch;

        IdeEventQueue.getInstance().addDispatcher(eventDispatcher, this);
        myDelayedRunner.addRunnable(() -> IdeEventQueue.getInstance().removeDispatcher(eventDispatcher));

        ApplicationManager.getApplication().invokeLater(() -> SharedThreadPool.getInstance().submit(() -> {
            ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(AnActionListener.TOPIC, new AnActionListener() {
                @Override
                public void beforeActionPerformed(@NotNull final AnAction action, @NotNull final AnActionEvent event) {
                    Plugin.this.beforeActionPerformed(action, event.getDataContext(), event);
                }

                @Override
                public void afterActionPerformed(@NotNull final AnAction action, @NotNull final AnActionEvent event, @NotNull AnActionResult result) {
                    Plugin.this.afterActionPerformed(action, event.getDataContext(), event, result);
                }

                @Override
                public void beforeEditorTyping(final char c, @NotNull final DataContext dataContext) {
                    Plugin.this.beforeEditorTyping(c, dataContext);
                }
            });

            final CopyPasteManagerEx copyPasteManager = CopyPasteManagerEx.getInstanceEx();
            final CopyPasteManager.ContentChangedListener contentChangedListener = this::clipboardContentChanged;

            copyPasteManager.addContentChangedListener(contentChangedListener);
            myDelayedRunner.addRunnable(() -> copyPasteManager.removeContentChangedListener(contentChangedListener));
        }));
    }

    public void updateEditorParameterHints(final @Nullable Editor activeEditor, boolean forceUpdate) {
        if (myParameterHintsAvailable) {
            EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
            boolean wasShowParameterHints = editorSettings.getOptions().SHOW_PARAMETER_NAME_HINTS;
            boolean wasDisabledShowParameterHints = myDisabledShowParameterHints;

            // now manage parameter hints
            myDisabledShowParameterHints = activeEditor != null && (activeEditor.getCaretModel().getCaretCount() > 1 && ApplicationSettings.getInstance().isDisableParameterInfo());

            if (!(myDisabledShowParameterHints || wasDisabledShowParameterHints)) {
                // can update our saved setting here
                mySavedShowParameterHints = editorSettings.getOptions().SHOW_PARAMETER_NAME_HINTS;
            } else {
                boolean showParameterHints = mySavedShowParameterHints && !myDisabledShowParameterHints;

                if (wasShowParameterHints != showParameterHints) {
                    editorSettings.getOptions().SHOW_PARAMETER_NAME_HINTS = showParameterHints;

                    if (activeEditor != null) {
                        if (isParameterHintsForceUpdateAvailable()) {
                            //noinspection UnstableApiUsage
                            ParameterHintsPassFactory.forceHintsUpdateOnNextPass(activeEditor);
                        } else {
                            forceUpdate = true;
                        }
                        if (forceUpdate && activeEditor instanceof EditorEx) {
                            Project project = activeEditor.getProject();
                            VirtualFile virtualFile = ((EditorEx) activeEditor).getVirtualFile();
                            if (project != null && virtualFile != null) {
                                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                                if (psiFile != null) {
                                    DaemonCodeAnalyzer.getInstance(project).restart(psiFile);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Nullable
    public static Editor getEditorEx(final @Nullable FileEditor fileEditor) {
        if (fileEditor != null) {
            if (fileEditor instanceof TextEditor) {
                return ((TextEditor) fileEditor).getEditor();
            }
        }
        return null;
    }

    static String getStringContent(Transferable content) {
        if (content != null) {
            try {
                return (String) content.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException ignore) { }
        }
        return null;
    }

    static boolean isBlank(CharSequence text) {
        final int iMax = text.length();
        for (int i = 0; i < iMax; i++) {
            char c = text.charAt(i);
            if (c != ' ' && c != '\t' && c != '\n') return false;
        }
        return true;
    }

    private void clipboardContentChanged(Transferable oldTransferable, Transferable newTransferable) {
        if (!myInContentManipulation && mySettings.isOnlyLatestBlankClipboard()) {
            String clipString = getStringContent(newTransferable);
            if (clipString != null && isBlank(clipString)) {
                myInContentManipulation = true;
                try {
                    final CopyPasteManagerEx copyPasteManager = CopyPasteManagerEx.getInstanceEx();
                    final Transferable[] allContents = copyPasteManager.getAllContents();
                    final ArrayList<Transferable> toDelete = new ArrayList<>();
                    for (Transferable content : allContents) {
                        if (content != newTransferable) {
                            String contentString = getStringContent(content);
                            if (contentString != null && isBlank(contentString)) {
                                toDelete.add(content);
                            }
                        }
                    }

                    for (Transferable content : toDelete) {
                        copyPasteManager.removeContent(content);
                    }
                } finally {
                    myInContentManipulation = false;
                }
            }
        }
    }

    public void addEditorActionListener(@NotNull Editor editor, @NotNull EditorActionListener listener, @Nullable Disposable parentDisposable) {
        final LinkedHashSet<EditorActionListener> listeners = myEditorActionListeners.computeIfAbsent(editor, editor1 -> new LinkedHashSet<>());
        listeners.add(listener);
        myDelayedRunner.addRunnable(editor, () -> removeEditorActionListener(editor, listener));

        if (parentDisposable != null) {
            Disposer.register(parentDisposable, () -> removeEditorActionListener(editor, listener));
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void removeEditorActionListener(@NotNull Editor editor, @NotNull EditorActionListener listener) {
        final LinkedHashSet<EditorActionListener> listeners = myEditorActionListeners.get(editor);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) myEditorActionListeners.remove(editor);
        }
    }

    void beforeActionPerformed(final AnAction action, final DataContext dataContext, final AnActionEvent event) {
        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        if (editor != null) {
            myActionEventEditorMap.put(event, editor);

            final LinkedHashSet<EditorActionListener> listeners = myEditorActionListeners.get(editor);
            if (listeners != null) {
                EditorActionListener[] actionListeners = listeners.toArray(EMPTY_EDITOR_ACTION_LISTENERS);
                for (EditorActionListener listener : actionListeners) {
                    try {
                        listener.beforeActionPerformed(action, dataContext, event);
                    } catch (Throwable e) {
                        LOG.error("EditorActionListener generated error on beforeActionPerformed", e);
                        removeEditorActionListener(editor, listener);
                    }
                }
            }
        }
    }

    void afterActionPerformed(AnAction action, final DataContext dataContext, AnActionEvent event, @NotNull AnActionResult ignoredResult) {
        Editor editor = myActionEventEditorMap.remove(event);
        if (editor != null) {
            final LinkedHashSet<EditorActionListener> listeners = myEditorActionListeners.get(editor);
            if (listeners != null) {
                EditorActionListener[] actionListeners = listeners.toArray(EMPTY_EDITOR_ACTION_LISTENERS);
                for (EditorActionListener listener : actionListeners) {
                    try {
                        listener.afterActionPerformed(action, dataContext, event);
                    } catch (Throwable e) {
                        LOG.error("EditorActionListener generated error on afterActionPerformed", e);
                        removeEditorActionListener(editor, listener);
                    }
                }
            }
        }
    }

    void beforeEditorTyping(final char c, final DataContext dataContext) {
        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        if (editor != null) {
            final LinkedHashSet<EditorActionListener> listeners = myEditorActionListeners.get(editor);
            if (listeners != null) {
                EditorActionListener[] actionListeners = listeners.toArray(EMPTY_EDITOR_ACTION_LISTENERS);
                for (EditorActionListener listener : actionListeners) {
                    try {
                        listener.beforeEditorTyping(c, dataContext);
                    } catch (Throwable e) {
                        LOG.error("EditorActionListener generated error on beforeEditorTyping", e);
                        removeEditorActionListener(editor, listener);
                    }
                }
            }
        }
    }

    @Override
    public void settingsChanged(final ColorIterable colors, final ApplicationSettings settings) {
        myDelayedRunner.runAllFor(myMultiPasteAction);
        mySettings = settings;

        if (settings.isOverrideStandardPaste()) {
            // run it for all editors
            Set<Editor> editorSet = myLineSelectionManagers.keySet();
            Editor[] editors = editorSet.toArray(EMPTY_EDITORS);
            for (Editor editor : editors) {
                registerPasteOverrides(editor);
            }
        }

        if (myRegisterCaretStateTransferable != mySettings.isRegisterCaretStateTransferable()) {
            if (mySettings.isRegisterCaretStateTransferable()) {
                // register caret state transferable flavour
                myRegisterCaretStateTransferable = true;
                SharedCaretStateTransferableData.shareCaretStateTransferable();
            } else {
                myRegisterCaretStateTransferable = false;
                SharedCaretStateTransferableData.unshareCaretStateTransferable();
            }
        }

        super.settingsChanged(colors, settings);

        if (!mySettings.isHighlightProjectViewNodes() && highlightProjectViewNodes) {
            // need to update project view to remove highlights
            updateProjectViews();
        }

        highlightProjectViewNodes = mySettings.isHighlightProjectViewNodes();

//        myRestartRequiredChecker.informRestartIfNeeded(settings);
    }

    private void registerPasteOverrides(@NotNull Editor editor) {
        myMultiPasteAction.registerCustomShortcutSet(CommonUIShortcuts.getMultiplePaste(), editor.getContentComponent());
        myPasteOverrideEditors.add(editor);
        myDelayedRunner.addRunnable(myMultiPasteAction, () -> unRegisterPasteOverrides(editor));
    }

    private void unRegisterPasteOverrides(@NotNull Editor editor) {
        final boolean removed = myPasteOverrideEditors.remove(editor);
        if (removed) {
            myMultiPasteAction.unregisterCustomShortcutSet(editor.getContentComponent());
        }
    }

    public static void addEditorActiveLookupListener(@NotNull Editor editor, @NotNull EditorActiveLookupListener listener, @Nullable Disposable parentDisposable) {
        assert editor.getProject() != null;

        PluginProjectComponent projectComponent = PluginProjectComponent.getInstance(editor.getProject());
        projectComponent.addEditorActiveLookupListener(editor, listener, parentDisposable);
    }

    public static void removeEditorActiveLookupListener(@NotNull Editor editor, @NotNull EditorActiveLookupListener listener) {
        assert editor.getProject() != null;

        PluginProjectComponent projectComponent = PluginProjectComponent.getInstance(editor.getProject());
        projectComponent.removeEditorActiveLookupListener(editor, listener);
    }

    // EditorFactoryListener
    private void editorCreated(@NotNull EditorFactoryEvent event) {
        final Editor editor = event.getEditor();
        LineSelectionManager manager = new LineSelectionManager(editor);
        myLineSelectionManagers.put(editor, manager);
        myDelayedRunner.addRunnable(editor, () -> {
            myLineSelectionManagers.remove(editor);
            Disposer.dispose(manager);
        });

        if (editor.getProject() != null && !editor.getProject().isDefault()) {
            PluginProjectComponent projectComponent = PluginProjectComponent.getInstance(editor.getProject());
            projectComponent.editorCreated(editor);
            myDelayedRunner.addRunnable(editor, () -> projectComponent.editorReleased(editor));
        }

        if (mySettings.isOverrideStandardPaste()) {
            registerPasteOverrides(editor);
            myDelayedRunner.addRunnable(editor, () -> unRegisterPasteOverrides(editor));
        }
    }

    // EditorFactoryListener
    private void editorReleased(@NotNull EditorFactoryEvent event) {
        myDelayedRunner.runAllFor(event.getEditor());
        myDoNotUpdateHighlightersEditors.remove(event.getEditor());
    }

    // IdeEventQueue.EventDispatcher
    private boolean dispatch(@NotNull final AWTEvent e) {
        if (e instanceof KeyEvent && e.getID() == KeyEvent.KEY_PRESSED) {
            final Component owner = ComponentUtil.findParentByCondition(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()
                    , component -> component instanceof JTextComponent);

            if (owner instanceof JComponent) {
                // register multi-paste if no already registered and remove when focus is lost
                for (Editor editor : myPasteOverrideEditors) {
                    if (owner == editor.getContentComponent()) {
                        return false;
                    }
                }

                if (myPasteOverrideComponent == null && mySettings.isOverrideStandardPaste()) {
                    final FocusAdapter focusAdapter = new FocusAdapter() {
                        @Override
                        public void focusGained(final FocusEvent e) {
                        }

                        @Override
                        public void focusLost(final FocusEvent e) {
                            myDelayedRunner.runAllFor(owner);
                        }
                    };

                    owner.addFocusListener(focusAdapter);
                    myMultiPasteAction.registerCustomShortcutSet(CommonUIShortcuts.getMultiplePaste(), (JComponent) owner);
                    myDelayedRunner.addRunnable(owner, () -> {
                        owner.removeFocusListener(focusAdapter);
                        myMultiPasteAction.unregisterCustomShortcutSet((JComponent) owner);
                        myPasteOverrideComponent = null;
                    });

                    myPasteOverrideComponent = (JComponent) owner;
                }
            }
        }
        return false;
    }

    void projectOpened(@NotNull Project activeProject) {
        if (myProjectHighlightProvider != null) {
            // Need to have its editors highlighted with active batch replace window highlights 
            ApplicationManager.getApplication().invokeLater(() -> updateEditorHighlighters(activeProject));
        }
    }

    public boolean shouldNotUpdateHighlighters(@Nullable Editor editor) {
        return editor == null || editor.isDisposed() || myDoNotUpdateHighlightersEditors.contains(editor);
    }

    void updateEditorHighlighters(@Nullable Project onlyInProject) {
        myEditorHighlightRunner.cancel();
        myEditorHighlightRunner = OneTimeRunnable.schedule(MiaCancelableJobScheduler.getInstance(), 500, new AwtRunnable(true, () -> {
            Editor[] editors = EditorFactory.getInstance().getAllEditors();

            for (Editor editor : editors) {
                if (shouldNotUpdateHighlighters(editor) || (onlyInProject != null && editor.getProject() == onlyInProject)) continue;

                LineSelectionManager selectionManager = LineSelectionManager.getInstance(editor);
                selectionManager.setHighlightProvider(getActiveHighlightProvider());
            }
        }));
    }

    void projectClosed(@NotNull Project ignoredProject) {

    }

    public void setProjectHighlightProvider(@NotNull Project activeProject, WordHighlightProvider<ApplicationSettings> highlightProvider) {
        if (myInSetProjectHighlighter && highlightProvider == null) return;

        try {
            myInSetProjectHighlighter = highlightProvider != null;

            if (myProjectHighlightProvider != null && mySearchReplaceHighlightListener != null) {
                myProjectHighlightProvider.removeHighlightListener(mySearchReplaceHighlightListener);
            }

            mySearchReplaceHighlightListener = null;
            myProjectHighlightProvider = null;

            if (highlightProvider != null) {
                myProjectHighlightProvider = highlightProvider;
                if (mySettings.isHighlightProjectViewNodes()) {
                    mySearchReplaceHighlightListener = new HighlightListener() {
                        @Override
                        public void highlightsUpdated() {
                            if (mySettings.isHighlightProjectViewNodes()) {
                                updateProjectViews();
                            }
                        }
                    };

                    myProjectHighlightProvider.addHighlightListener(mySearchReplaceHighlightListener, this);
                }

                // hide other projects' batch search replace tool window
                Project[] projects = ProjectManager.getInstance().getOpenProjects();
                for (Project project : projects) {
                    if (project != activeProject) {
                        PluginProjectComponent pluginProjectComponent = PluginProjectComponent.getInstance(project);
                        ToolWindow searchReplaceToolWindow = pluginProjectComponent.getSearchReplaceToolWindow();
                        if (searchReplaceToolWindow != null) {
                            searchReplaceToolWindow.hide();
                        }
                    }
                }
            }

            if (!myInSetProjectHighlighter || myProjectHighlightProvider == highlightProvider) {
                // only do this if removing highlight provider did not result in a nested call with a non-null value
                // this way will result in only one call to updateEditorHighlighters0) with the one that will be the new highlight provider
                updateEditorHighlighters(null);

                if (highlightProvider == null) {
                    updateProjectViews();
                }
            }
        } finally {
            myInSetProjectHighlighter = false;
        }
    }

    public static Plugin getInstance() {
        return ApplicationManager.getApplication().getService(Plugin.class);
    }

    public boolean isParameterHintsAvailable() {
        return myParameterHintsAvailable;
    }

    @SuppressWarnings({ "FieldMayBeFinal", "FieldCanBeLocal" }) private static int license_features = 0;

    @SuppressWarnings({ "FieldMayBeFinal", "unused" }) private static boolean license_initialized = false;

    public static boolean isFeatureLicensed(int feature) {
        return (license_features & feature) == feature;
    }

    private static final String PLUGIN_ID = "com.vladsch.MissingInActions";

    public static IdeaPluginDescriptor getPluginDescriptor() {
        IdeaPluginDescriptor[] plugins = PluginManager.getPlugins();
        for (IdeaPluginDescriptor plugin : plugins) {
            if (PLUGIN_ID.equals(plugin.getPluginId().getIdString())) {
                return plugin;
            }
        }

        throw new IllegalStateException("Unexpected, plugin cannot find its own plugin descriptor");
    }

    public static String fullProductVersion() {
        IdeaPluginDescriptor pluginDescriptor = getPluginDescriptor();
        return pluginDescriptor.getVersion();
    }

    public @NotNull WordHighlightProvider<ApplicationSettings> getActiveHighlightProvider() {
        return myProjectHighlightProvider == null ? this : myProjectHighlightProvider;
    }
}
