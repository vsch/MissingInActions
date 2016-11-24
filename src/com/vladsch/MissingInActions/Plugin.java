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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.util.Disposer;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Plugin implements ApplicationComponent, EditorFactoryListener, Disposable {
    final private HashMap<Editor, LineSelectionManager> myAdjusterMap = new HashMap<>();

    final public static int FEATURE_ENHANCED = 1;
    final public static int FEATURE_DEVELOPMENT = 2;

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

    public Plugin() {

    }

    public static boolean isSelectionExtendsPastCaret() {
        return ApplicationSettings.getInstance().isSelectionEndExtended();
    }

    @NotNull
    public LineSelectionManager getSelectionAdjuster(Editor editor) {
        return myAdjusterMap.computeIfAbsent(editor, e -> new LineSelectionManager(editor));
    }

    @Override
    public void dispose() {
    }

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        if (!event.getEditor().isOneLineMode()) {
            LineSelectionManager manager = new LineSelectionManager(event.getEditor());
            myAdjusterMap.put(event.getEditor(), manager);
        }
    }

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        LineSelectionManager manager = myAdjusterMap.remove(event.getEditor());
        if (manager != null) {
            Disposer.dispose(manager);
        }
    }

    @Override
    public void initComponent() {
        EditorFactory.getInstance().addEditorFactoryListener(this, ApplicationManager.getApplication());
    }

    @Override
    public void disposeComponent() {
        for (Map.Entry<Editor, LineSelectionManager> pair : myAdjusterMap.entrySet()) {
            LineSelectionManager manager = myAdjusterMap.remove(pair.getKey());
            if (manager != null) {
                Disposer.dispose(manager);
            }
        }
    }

    @NotNull
    @Override
    public String getComponentName() {
        return this.getClass().getName();
    }

    public static Plugin getInstance() {
        return ApplicationManager.getApplication().getComponent(Plugin.class);
    }
}
