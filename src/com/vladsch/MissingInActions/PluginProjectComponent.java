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

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupManagerListener;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindowManager;
import com.vladsch.MissingInActions.util.EditorActiveLookupListener;
import com.vladsch.plugin.util.DelayedRunner;
import com.vladsch.plugin.util.LazyFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashSet;

public class PluginProjectComponent implements ProjectComponent, Disposable {
    private static final Logger LOG = Logger.getInstance("com.vladsch.MissingInActions");

    final @NotNull Project myProject;
    private final HashMap<Editor, LinkedHashSet<EditorActiveLookupListener>> myActiveLookupListeners;
    private final HashMap<Editor, LinkedHashSet<EditorActiveLookupListener>> myInActiveLookupListeners;
    private final @NotNull DelayedRunner myDelayedRunner;
    private final Plugin myPlugin;
    BatchSearchReplaceToolWindow mySearchReplaceToolWindow;
    private FileEditorManagerListener myEditorManagerListener;

    public PluginProjectComponent(@NotNull Project project) {
        myProject = project;
        myPlugin = Plugin.getInstance();
        myActiveLookupListeners = new HashMap<>();
        myInActiveLookupListeners = new HashMap<>();
        myDelayedRunner = new DelayedRunner();
    }

    @Override
    public void dispose() {
        disposeComponent();
    }

    void propertyChange(Lookup oldValue, Lookup newValue) {
        Editor newEditor = null;
        Editor oldEditor = null;

        if (newValue instanceof LookupImpl) {
            newEditor = newValue.getEditor();
        }
        if (oldValue instanceof LookupImpl) {
            oldEditor = oldValue.getEditor();
        }

        if (oldEditor != newEditor) {
            if (oldEditor != null) {
                final LinkedHashSet<EditorActiveLookupListener> listeners = myInActiveLookupListeners.remove(oldEditor);
                if (listeners != null) {
                    for (EditorActiveLookupListener listener : listeners) {
                        try {
                            listener.exitActiveLookup();
                        } catch (Throwable e) {
                            LOG.error("EditorActiveLookupListener error on exitActiveLookup", e);
                            removeEditorActiveLookupListener(oldEditor, listener);
                        }
                    }
                    myInActiveLookupListeners.remove(oldEditor);
                }
            }
            if (newEditor != null) {
                final LinkedHashSet<EditorActiveLookupListener> listeners = myActiveLookupListeners.get(newEditor);
                if (listeners != null) {
                    final LinkedHashSet<EditorActiveLookupListener> inActiveLookup = myInActiveLookupListeners.computeIfAbsent(newEditor, editor1 -> new LinkedHashSet<>());
                    for (EditorActiveLookupListener listener : listeners) {
                        inActiveLookup.add(listener);
                        try {
                            listener.enterActiveLookup();
                        } catch (Throwable e) {
                            LOG.error("EditorActiveLookupListener error on enterActiveLookup", e);
                            removeEditorActiveLookupListener(newEditor, listener);
                        }
                    }
                }
            }
        }
    }

    void editorCreated(@NotNull Editor editor) {

    }

    void editorReleased(@NotNull Editor editor) {
        myInActiveLookupListeners.remove(editor);
        myActiveLookupListeners.remove(editor);
    }

    void addEditorActiveLookupListener(@NotNull Editor editor, @NotNull EditorActiveLookupListener listener, @Nullable Disposable parentDisposable) {
        assert editor.getProject() == myProject;
        final LinkedHashSet<EditorActiveLookupListener> listeners = myActiveLookupListeners.computeIfAbsent(editor, editor1 -> new LinkedHashSet<>());
        listeners.add(listener);
        if (parentDisposable != null) {
            Disposer.register(parentDisposable, () -> {
                removeEditorActiveLookupListener(editor, listener);
            });
        }
    }

    void removeEditorActiveLookupListener(@NotNull Editor editor, @NotNull EditorActiveLookupListener listener) {
        assert editor.getProject() == myProject;
        final LinkedHashSet<EditorActiveLookupListener> listeners = myActiveLookupListeners.get(editor);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) myActiveLookupListeners.remove(editor);
        }

        final LinkedHashSet<EditorActiveLookupListener> inLookupListeners = myInActiveLookupListeners.get(editor);
        if (inLookupListeners != null) {
            inLookupListeners.remove(listener);
            if (inLookupListeners.isEmpty()) myActiveLookupListeners.remove(editor);
        }
    }

    @Override
    public void projectOpened() {
        // register editor factory listener
        myPlugin.initProjectComponent(myProject);
        myDelayedRunner.addRunnable(myProject, () -> {
            myPlugin.disposeProjectComponent(myProject);
            myInActiveLookupListeners.clear();
            myActiveLookupListeners.clear();
        });

        myProject.getMessageBus().connect(this).subscribe(LookupManagerListener.TOPIC, new LookupManagerListener() {
            @Override
            public void activeLookupChanged(@Nullable Lookup oldLookup, @Nullable Lookup newLookup) {
                PluginProjectComponent.this.propertyChange(oldLookup, newLookup);
            }
        });

        myPlugin.projectOpened(myProject);
        myDelayedRunner.addRunnable(myProject, () -> {
            myPlugin.projectClosed(myProject);
        });

        // NOTE: disable for light project tests, project is never closed and leaves editors unreleased causing test failures.
        if (!ApplicationManager.getApplication().isUnitTestMode() || !ProjectManagerImpl.isLight(myProject)) {
            ToolWindowManager.getInstance(myProject).invokeLater(() -> {
                mySearchReplaceToolWindow = new BatchSearchReplaceToolWindow(myProject);
                Disposer.register(this, mySearchReplaceToolWindow);
            });
        }

        Disposer.register(myProject, this);
    }

    public void showBatchSearchReplace() {
        if (mySearchReplaceToolWindow != null) {
            mySearchReplaceToolWindow.activate();
        }
    }

    public void hideBatchSearchReplace() {
        if (mySearchReplaceToolWindow != null) {
            mySearchReplaceToolWindow.hide();
        }
    }

    public @Nullable BatchSearchReplaceToolWindow getSearchReplaceToolWindow() {
        return mySearchReplaceToolWindow;
    }

    @Override
    public void projectClosed() {
//        myDelayedRunner.runAllFor(myProject);
        myDelayedRunner.runAll();
    }

    @NotNull
    @Override
    public String getComponentName() {
        return this.getClass().getName();
    }

    final static private LazyFunction<Project, PluginProjectComponent> NULL = new LazyFunction<>(PluginProjectComponent::new);

    @NotNull
    public static PluginProjectComponent getInstance(@NotNull Project project) {
        if (project.isDefault()) return NULL.getValue(project);
        else return project.getComponent(PluginProjectComponent.class);
    }
}
