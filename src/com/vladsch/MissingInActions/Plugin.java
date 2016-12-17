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

package com.vladsch.MissingInActions;

import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import com.vladsch.MissingInActions.actions.character.MiaMultiplePasteAction;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.settings.ApplicationSettingsListener;
import com.vladsch.MissingInActions.util.CommonUIShortcuts;
import com.vladsch.MissingInActions.util.DelayedRunner;
import com.vladsch.MissingInActions.util.EditorActionListener;
import com.vladsch.MissingInActions.util.EditorActiveLookupListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

public class Plugin implements ApplicationComponent, Disposable {
    private static final Logger LOG = Logger.getInstance("com.vladsch.MissingInActions");

    final public static int FEATURE_ENHANCED = 1;
    final public static int FEATURE_DEVELOPMENT = 2;

    final private @NotNull DelayedRunner myDelayedRunner;
    final private HashMap<Editor, LineSelectionManager> myLineSelectionManagers;
    final private HashMap<AnActionEvent, Editor> myActionEventEditorMap;
    final private HashMap<Editor, LinkedHashSet<EditorActionListener>> myEditorActionListeners;
    final private HashSet<Editor> myPasteOverrideEditors;
    final private AnAction myMultiPasteAction;
    private ApplicationSettings mySettings;
    private @Nullable JComponent myPasteOverrideComponent;

    public Plugin() {
        myDelayedRunner = new DelayedRunner();
        myLineSelectionManagers = new HashMap<>();
        myPasteOverrideEditors = new HashSet<>();
        myMultiPasteAction = new MiaMultiplePasteAction();
        myActionEventEditorMap = new HashMap<>();
        myEditorActionListeners = new HashMap<>();
        myPasteOverrideComponent = null;

        MessageBusConnection messageBusConnection = ApplicationManager.getApplication().getMessageBus().connect(this);
        messageBusConnection.subscribe(ApplicationSettingsListener.TOPIC, this::settingsChanged);
        myDelayedRunner.addRunnable(messageBusConnection::disconnect);
        mySettings = ApplicationSettings.getInstance();
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
    public void initComponent() {
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
            Editor[] editors = editorSet.toArray(new Editor[editorSet.size()]);
            for (Editor editor : editors) {
                LineSelectionManager manager = myLineSelectionManagers.remove(editor);
                if (manager != null) {
                    Disposer.dispose(manager);
                }
            }
        });

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

        ActionManager.getInstance().addAnActionListener(new AnActionListener.Adapter() {
            @Override
            public void beforeActionPerformed(final AnAction action, final DataContext dataContext, final AnActionEvent event) {
                Plugin.this.beforeActionPerformed(action, dataContext, event);
            }

            @Override
            public void afterActionPerformed(final AnAction action, final DataContext dataContext, final AnActionEvent event) {
                Plugin.this.afterActionPerformed(action, dataContext, event);
            }

            @Override
            public void beforeEditorTyping(final char c, final DataContext dataContext) {
                Plugin.this.beforeEditorTyping(c, dataContext);
            }
        });
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

    private void beforeActionPerformed(final AnAction action, final DataContext dataContext, final AnActionEvent event) {
        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        if (editor != null) {
            myActionEventEditorMap.put(event, editor);

            final LinkedHashSet<EditorActionListener> listeners = myEditorActionListeners.get(editor);
            if (listeners != null) {
                EditorActionListener[] actionListeners = listeners.toArray(new EditorActionListener[listeners.size()]);
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

    private void afterActionPerformed(final AnAction action, final DataContext dataContext, final AnActionEvent event) {
        Editor editor = myActionEventEditorMap.remove(event);
        if (editor != null) {
            final LinkedHashSet<EditorActionListener> listeners = myEditorActionListeners.get(editor);
            if (listeners != null) {
                EditorActionListener[] actionListeners = listeners.toArray(new EditorActionListener[listeners.size()]);
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

    private void beforeEditorTyping(final char c, final DataContext dataContext) {
        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        if (editor != null) {
            final LinkedHashSet<EditorActionListener> listeners = myEditorActionListeners.get(editor);
            if (listeners != null) {
                EditorActionListener[] actionListeners = listeners.toArray(new EditorActionListener[listeners.size()]);
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
    public void disposeComponent() {
        myDelayedRunner.runAll();
    }

    void initProjectComponent(@NotNull Project project) {

    }

    void disposeProjectComponent(@NotNull Project project) {

    }

    void projectOpened(@NotNull Project project) {

    }

    void projectClosed(@NotNull Project project) {

    }

    private void settingsChanged(@NotNull ApplicationSettings settings) {
        myDelayedRunner.runAllFor(myMultiPasteAction);
        mySettings = settings;

        if (settings.isOverrideStandardPaste()) {
            // run it for all editors
            Set<Editor> editorSet = myLineSelectionManagers.keySet();
            Editor[] editors = editorSet.toArray(new Editor[editorSet.size()]);
            for (Editor editor : editors) {
                registerPasteOverrides(editor);
            }
        }
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

    public void addEditorActiveLookupListener(@NotNull Editor editor, @NotNull EditorActiveLookupListener listener) {
        assert editor.getProject() != null;
        addEditorActiveLookupListener(editor, listener, null);
    }

    @SuppressWarnings({ "WeakerAccess", "SameParameterValue" })
    public void addEditorActiveLookupListener(@NotNull Editor editor, @NotNull EditorActiveLookupListener listener, @Nullable Disposable parentDisposable) {
        assert editor.getProject() != null;

        PluginProjectComponent projectComponent = editor.getProject().getComponent(PluginProjectComponent.class);
        projectComponent.addEditorActiveLookupListener(editor, listener, parentDisposable);
    }

    public void removeEditorActiveLookupListener(@NotNull Editor editor, @NotNull EditorActiveLookupListener listener) {
        assert editor.getProject() != null;

        PluginProjectComponent projectComponent = editor.getProject().getComponent(PluginProjectComponent.class);
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

        if (editor.getProject() != null) {
            PluginProjectComponent projectComponent = editor.getProject().getComponent(PluginProjectComponent.class);
            projectComponent.editorCreated(editor);
            myDelayedRunner.addRunnable(editor, () -> {
                projectComponent.editorReleased(editor);
            });
        }

        if (mySettings.isOverrideStandardPaste()) {
            registerPasteOverrides(editor);
            myDelayedRunner.addRunnable(editor, () -> unRegisterPasteOverrides(editor));
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

    @SuppressWarnings("FieldCanBeLocal")
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
}
