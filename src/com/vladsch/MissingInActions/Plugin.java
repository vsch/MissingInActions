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
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.BaseComponent;
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
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ui.UIUtil;
import com.vladsch.MissingInActions.actions.character.MiaMultiplePasteAction;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.EditorActionListener;
import com.vladsch.MissingInActions.util.EditorActiveLookupListener;
import com.vladsch.MissingInActions.util.MiaCancelableJobScheduler;
import com.vladsch.MissingInActions.util.SharedCaretStateTransferableData;
import com.vladsch.MissingInActions.util.highlight.MiaWordHighlightProviderImpl;
import com.vladsch.flexmark.util.Pair;
import com.vladsch.plugin.util.AppUtils;
import com.vladsch.plugin.util.HelpersKt;
import com.vladsch.plugin.util.OneTimeRunnable;
import com.vladsch.plugin.util.ui.ColorIterable;
import com.vladsch.plugin.util.ui.CommonUIShortcuts;
import com.vladsch.plugin.util.ui.highlight.HighlightProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
import java.util.function.Consumer;

import static com.vladsch.plugin.util.AppUtils.isParameterHintsForceUpdateAvailable;

public class Plugin extends MiaWordHighlightProviderImpl implements BaseComponent {
    private static final Logger LOG = Logger.getInstance("com.vladsch.MissingInActions");

    final public static int FEATURE_ENHANCED = 1;
    final public static int FEATURE_DEVELOPMENT = 2;
    public static final Editor[] EMPTY_EDITORS = new Editor[0];
    public static final EditorActionListener[] EMPTY_EDITOR_ACTION_LISTENERS = new EditorActionListener[0];

    final private HashMap<Editor, LineSelectionManager> myLineSelectionManagers;
    final private HashMap<AnActionEvent, Editor> myActionEventEditorMap;
    final private HashMap<Editor, LinkedHashSet<EditorActionListener>> myEditorActionListeners;
    final private HashMap<Project, HighlightProvider<ApplicationSettings>> myProjectHighlightProviders;
    final private HashSet<Editor> myPasteOverrideEditors;
    final private AnAction myMultiPasteAction;
    private @Nullable JComponent myPasteOverrideComponent;
    private boolean myInContentManipulation;
    private boolean mySavedShowParameterHints;
    private boolean myDisabledShowParameterHints;
    final private boolean myParameterHintsAvailable;
    private boolean myRegisterCaretStateTransferable;
    private OneTimeRunnable myHighlightSaveTask = OneTimeRunnable.NULL;
    private boolean disableSaveHighlights = true;


//    final private AppRestartRequiredChecker<ApplicationSettings> myRestartRequiredChecker = new AppRestartRequiredChecker<ApplicationSettings>(Bundle.message("settings.restart-required.title"));

    public Plugin() {
        super(ApplicationSettings.getInstance());
        myLineSelectionManagers = new HashMap<>();
        myPasteOverrideEditors = new HashSet<>();
        myMultiPasteAction = new MiaMultiplePasteAction();
        myActionEventEditorMap = new HashMap<>();
        myProjectHighlightProviders = new HashMap<>();
        myEditorActionListeners = new HashMap<>();
        myPasteOverrideComponent = null;
        myParameterHintsAvailable = AppUtils.isParameterHintsAvailable();

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
    }

    @NotNull
    public LineSelectionManager getSelectionManager(Editor editor) {
        return myLineSelectionManagers.computeIfAbsent(editor, e -> new LineSelectionManager(editor));
    }

    @Nullable
    public LineSelectionManager getSelectionManagerOrNull(Editor editor) {
        return myLineSelectionManagers.get(editor);
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
                HashMap<String, Pair<Integer, Integer>> state = getHighlightState();
                mySettings.setHighlightState(getHighlightState());
                mySettings.setHighlightWordsCaseSensitive(isHighlightCaseSensitive());
                mySettings.setHighlightWordsMatchBoundary(isHighlightWordsMatchBoundary());
            });
        }

        super.fireHighlightsChanged();
    }

    @Override
    public void initComponent() {
        super.initComponent();

        SharedCaretStateTransferableData.initialize();
        myDelayedRunner.addRunnable(SharedCaretStateTransferableData::dispose);

        if (myParameterHintsAvailable) {
            mySavedShowParameterHints = EditorSettingsExternalizable.getInstance().isShowParameterNameHints();
            // restore setting on exit, just in case editor change listener didn't
            myDelayedRunner.addRunnable(() -> {
                EditorSettingsExternalizable.getInstance().setShowParameterNameHints(mySavedShowParameterHints);
            });
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
                public void fileOpened(@NotNull final FileEditorManager source, @NotNull final VirtualFile file) {

                }

                @Override
                public void fileClosed(@NotNull final FileEditorManager source, @NotNull final VirtualFile file) {

                }

                @Override
                public void selectionChanged(@NotNull final FileEditorManagerEvent event) {
                    updateEditorParameterHints(getEditorEx(event.getNewEditor()), event.getNewEditor() != event.getOldEditor());
                }
            };

            ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, editorManagerListener);
        }

        final IdeEventQueue.EventDispatcher eventDispatcher = new IdeEventQueue.EventDispatcher() {
            @Override
            public boolean dispatch(@NotNull final AWTEvent e) {
                return Plugin.this.dispatch(e);
            }
        };

        IdeEventQueue.getInstance().addDispatcher(eventDispatcher, this);
        myDelayedRunner.addRunnable(() -> {
            IdeEventQueue.getInstance().removeDispatcher(eventDispatcher);
        });

        ActionManager.getInstance().addAnActionListener(new AnActionListener() {
            @Override
            public void beforeActionPerformed(@NotNull final AnAction action, @NotNull final DataContext dataContext, @NotNull final AnActionEvent event) {
                Plugin.this.beforeActionPerformed(action, dataContext, event);
            }

            @Override
            public void afterActionPerformed(@NotNull final AnAction action, @NotNull final DataContext dataContext, @NotNull final AnActionEvent event) {
                Plugin.this.afterActionPerformed(action, dataContext, event);
            }

            @Override
            public void beforeEditorTyping(final char c, @NotNull final DataContext dataContext) {
                Plugin.this.beforeEditorTyping(c, dataContext);
            }
        });

        final CopyPasteManagerEx copyPasteManager = CopyPasteManagerEx.getInstanceEx();
        final CopyPasteManager.ContentChangedListener contentChangedListener = this::clipboardContentChanged;

        copyPasteManager.addContentChangedListener(contentChangedListener);
        myDelayedRunner.addRunnable(() -> {
            copyPasteManager.removeContentChangedListener(contentChangedListener);
        });
    }

    public void updateEditorParameterHints(final @Nullable Editor activeEditor, boolean forceUpdate) {
        if (myParameterHintsAvailable) {
            EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
            boolean wasShowParameterHints = editorSettings.isShowParameterNameHints();
            boolean wasDisabledShowParameterHints = myDisabledShowParameterHints;

            // now manage parameter hints
            myDisabledShowParameterHints = activeEditor != null && (activeEditor.getCaretModel().getCaretCount() > 1 && ApplicationSettings.getInstance().isDisableParameterInfo());

            if (!(myDisabledShowParameterHints || wasDisabledShowParameterHints)) {
                // can update our saved setting here
                mySavedShowParameterHints = editorSettings.isShowParameterNameHints();
            } else {
                boolean showParameterHints = mySavedShowParameterHints && !myDisabledShowParameterHints;

                if (wasShowParameterHints != showParameterHints) {
                    editorSettings.setShowParameterNameHints(showParameterHints);

                    if (activeEditor != null) {
                        if (isParameterHintsForceUpdateAvailable()) {
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

    public void addEditorActionListener(@NotNull Editor editor, @NotNull EditorActionListener listener) {
        addEditorActionListener(editor, listener, null);
    }

    public void addEditorActionListener(@NotNull Editor editor, @NotNull EditorActionListener listener, @Nullable Disposable parentDisposable) {
        final LinkedHashSet<EditorActionListener> listeners = myEditorActionListeners.computeIfAbsent(editor, editor1 -> new LinkedHashSet<>());
        listeners.add(listener);
        myDelayedRunner.addRunnable(editor, () -> {
            removeEditorActionListener(editor, listener);
        });

        if (parentDisposable != null) {
            Disposer.register(parentDisposable, () -> {
                removeEditorActionListener(editor, listener);
            });
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

    void afterActionPerformed(final AnAction action, final DataContext dataContext, final AnActionEvent event) {
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

    void initProjectComponent(@NotNull Project project) {

    }

    void disposeProjectComponent(@NotNull Project project) {

    }

    void projectOpened(@NotNull Project project) {

    }

    void projectClosed(@NotNull Project project) {
         myProjectHighlightProviders.remove(project);
    }

    public HighlightProvider<ApplicationSettings> getProjectHighlighter(@NotNull Project project) {
         return myProjectHighlightProviders.get(project);
    }

    public void setProjectHighlightProvider(@NotNull Project project, HighlightProvider<ApplicationSettings> highlightProvider) {
        if (highlightProvider == null) {
            myProjectHighlightProviders.remove(project);
        } else {
            myProjectHighlightProviders.put(project, highlightProvider);
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

//        myRestartRequiredChecker.informRestartIfNeeded(settings);
    }

    private void registerPasteOverrides(@NotNull Editor editor) {
        myMultiPasteAction.registerCustomShortcutSet(CommonUIShortcuts.getMultiplePaste(), editor.getContentComponent());
        myPasteOverrideEditors.add(editor);
        myDelayedRunner.addRunnable(myMultiPasteAction, () -> {
            unRegisterPasteOverrides(editor);
        });
    }

    private void unRegisterPasteOverrides(@NotNull Editor editor) {
        final boolean removed = myPasteOverrideEditors.remove(editor);
        if (removed) {
            myMultiPasteAction.unregisterCustomShortcutSet(editor.getContentComponent());
        }
    }

    public static void addEditorActiveLookupListener(@NotNull Editor editor, @NotNull EditorActiveLookupListener listener) {
        assert editor.getProject() != null;
        addEditorActiveLookupListener(editor, listener, null);
    }

    @SuppressWarnings({ "WeakerAccess", "SameParameterValue" })
    public static void addEditorActiveLookupListener(@NotNull Editor editor, @NotNull EditorActiveLookupListener listener, @Nullable Disposable parentDisposable) {
        assert editor.getProject() != null;

        PluginProjectComponent projectComponent = editor.getProject().getComponent(PluginProjectComponent.class);
        if (projectComponent != null) {
            projectComponent.addEditorActiveLookupListener(editor, listener, parentDisposable);
        }
    }

    public static void removeEditorActiveLookupListener(@NotNull Editor editor, @NotNull EditorActiveLookupListener listener) {
        assert editor.getProject() != null;

        PluginProjectComponent projectComponent = editor.getProject().getComponent(PluginProjectComponent.class);
        if (projectComponent != null) {
            projectComponent.removeEditorActiveLookupListener(editor, listener);
        }
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
            PluginProjectComponent projectComponent = editor.getProject().getComponent(PluginProjectComponent.class);
            if (projectComponent != null) {
                projectComponent.editorCreated(editor);
                myDelayedRunner.addRunnable(editor, () -> {
                    projectComponent.editorReleased(editor);
                });
            }
        }

        if (mySettings.isOverrideStandardPaste()) {
            registerPasteOverrides(editor);
            myDelayedRunner.addRunnable(editor, () -> unRegisterPasteOverrides(editor));
        }
    }

    public void forAllProjectEditors(@NotNull Project project, @NotNull Consumer<LineSelectionManager> consumer) {
        for (LineSelectionManager manager : myLineSelectionManagers.values()) {
            consumer.accept(manager);
        }
    }

    // EditorFactoryListener
    private void editorReleased(@NotNull EditorFactoryEvent event) {
        myDelayedRunner.runAllFor(event.getEditor());
    }

    // IdeEventQueue.EventDispatcher
    ///**
    // * This is needed to override paste actions only if there are multiple carets
    // * <p>
    // * Otherwise, formatting after paste will not work right in single caret mode and
    // * without the override multi-caret select after paste and all the smart paste
    // * adjustments don't work because the IDE does not provide data for last pasted
    // * ranges.
    // */
    //private class PasteOverrider implements IdeEventQueue.EventDispatcher {
    //    @Override
    //    public boolean dispatch(@NotNull AWTEvent e) {
    //        if (e instanceof KeyEvent && e.getID() == KeyEvent.KEY_PRESSED) {
    //            Component owner = UIUtil.findParentByCondition(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(), component -> {
    //                return component == myEditor.getComponent();
    //            });
    //
    //            if (owner == myEditor.getComponent()) {
    //                boolean registerPasteOverrides = mySettings.isOverrideStandardPaste() && myEditor.getCaretModel().getCaretCount() > 1;
    //                if (registerPasteOverrides == (myMultiPasteAction == null)) {
    //                    if (!registerPasteOverrides) {
    //                        // unregister them
    //                        unRegisterPasteOverrides();
    //                    } else {
    //                        // we register our own pastes to handle multi-caret
    //                        registerPasteOverrides();
    //                    }
    //                }
    //            }
    //        }
    //        return false;
    //    }
    //}

    private boolean dispatch(@NotNull final AWTEvent e) {
        if (e instanceof KeyEvent && e.getID() == KeyEvent.KEY_PRESSED) {
            final Component owner = UIUtil.findParentByCondition(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(), component -> {
                return component instanceof JTextComponent;
            });

            if (owner != null && owner instanceof JComponent) {
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

    @NotNull
    @Override
    public String getComponentName() {
        return this.getClass().getName();
    }

    public static Plugin getInstance() {
        return ApplicationManager.getApplication().getComponent(Plugin.class);
    }

    public boolean isParameterHintsAvailable() {
        return myParameterHintsAvailable;
    }

    private static int license_features = 0;

    @NotNull public static final String productVersion = "0.1.0";
    private static boolean license_initialized = false;

    public static int getLicensedFeatures() {
        return license_features;
    }

    public static boolean isFeatureLicensed(int feature) {
        return (license_features & feature) == feature;
    }

    public static String getProductId() {
        return Bundle.message("plugin.product-id");
    }

    private static String getProductDisplayName() {
        return Bundle.message("plugin.name");
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

    public static String productVersion() {
        IdeaPluginDescriptor pluginDescriptor = getPluginDescriptor();
        String version = pluginDescriptor.getVersion();
        // truncate version to 3 digits and if had more than 3 append .x, that way
        // no separate product versions need to be created
        String[] parts = version.split("\\.", 4);
        if (parts.length <= 3) {
            return version;
        }

        String sep = "";
        StringBuilder newVersion = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            newVersion.append(sep);
            sep = ".";
            newVersion.append(parts[i]);
        }
        newVersion.append(".x");
        return newVersion.toString();
    }

    public static String fullProductVersion() {
        IdeaPluginDescriptor pluginDescriptor = getPluginDescriptor();
        return pluginDescriptor.getVersion();
    }

    @Nullable
    public static String getPluginCustomPath() {
        String[] variants = { PathManager.getHomePath(), PathManager.getPluginsPath() };

        for (String variant : variants) {
            String path = variant + "/" + getProductId();
            if (LocalFileSystem.getInstance().findFileByPath(path) != null) {
                return path;
            }
        }
        return null;
    }

    @Nullable
    public static String getPluginPath() {
        String[] variants = { PathManager.getPluginsPath() };

        for (String variant : variants) {
            String path = variant + "/" + getProductId();
            if (LocalFileSystem.getInstance().findFileByPath(path) != null) {
                return path;
            }
        }
        return null;
    }

    @Nullable
    public static String getPluginFilePath(String fileName) {
        String path = getPluginCustomPath();
        return path == null ? null : HelpersKt.suffixWith(path, '/') + fileName;
    }
}
