// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupManagerListener;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.vladsch.MissingInActions.actions.pattern.BatchReplaceForm;
import com.vladsch.MissingInActions.util.EditorActiveLookupListener;
import com.vladsch.plugin.util.DelayedRunner;
import com.vladsch.plugin.util.LazyFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashSet;

public class PluginProjectComponent implements Disposable {
    private static final Logger LOG = Logger.getInstance("com.vladsch.MissingInActions");

    final @NotNull Project myProject;
    private final HashMap<Editor, LinkedHashSet<EditorActiveLookupListener>> myActiveLookupListeners;
    private final HashMap<Editor, LinkedHashSet<EditorActiveLookupListener>> myInActiveLookupListeners;
    private final @NotNull DelayedRunner myDelayedRunner;
    private final Plugin myPlugin;
    BatchReplaceForm myBatchReplaceForm;
    ToolWindow myToolWindow;

    public PluginProjectComponent(@NotNull Project project) {
        myProject = project;
        myPlugin = Plugin.getInstance();
        myActiveLookupListeners = new HashMap<>();
        myInActiveLookupListeners = new HashMap<>();
        myDelayedRunner = new DelayedRunner();

        // register editor factory listener
        myDelayedRunner.addRunnable(myProject, () -> {
            myInActiveLookupListeners.clear();
            myActiveLookupListeners.clear();
        });

        myProject.getMessageBus().connect(this).subscribe(LookupManagerListener.TOPIC, (LookupManagerListener) PluginProjectComponent.this::propertyChange);

        myPlugin.projectOpened(myProject);
        myDelayedRunner.addRunnable(myProject, () -> myPlugin.projectClosed(myProject));

        // it is either dispose on project, or not dispose at all.
        //noinspection IncorrectParentDisposable
        Disposer.register(myProject, this);
    }

    @Override
    public void dispose() {
        myDelayedRunner.runAll();
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

    void editorCreated(@NotNull Editor ignoredEditor) {

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
            Disposer.register(parentDisposable, () -> removeEditorActiveLookupListener(editor, listener));
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

    public void showBatchSearchReplace() {
        if (myToolWindow != null) {
            myToolWindow.show();
        }
    }

    public void hideBatchSearchReplace() {
        if (myToolWindow != null) {
            myToolWindow.hide();
        }
    }

    public @Nullable ToolWindow getSearchReplaceToolWindow() {
        return myToolWindow;
    }

    public @Nullable BatchReplaceForm getBatchReplaceForm() {
        return myBatchReplaceForm;
    }

    final static private LazyFunction<Project, PluginProjectComponent> NULL = new LazyFunction<>(PluginProjectComponent::new);

    @NotNull
    public static PluginProjectComponent getInstance(@NotNull Project project) {
        if (project.isDefault()) return NULL.getValue(project);
        else return project.getService(PluginProjectComponent.class);
    }
}
