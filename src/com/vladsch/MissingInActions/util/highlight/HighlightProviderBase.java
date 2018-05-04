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

package com.vladsch.MissingInActions.util.highlight;

import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.messages.MessageBusConnection;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.settings.ApplicationSettingsListener;
import com.vladsch.MissingInActions.util.AwtRunnable;
import com.vladsch.MissingInActions.util.ColorIterable;
import com.vladsch.MissingInActions.util.DelayedRunner;
import com.vladsch.MissingInActions.util.OneTimeRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.UIManager;
import java.awt.Color;
import java.util.HashSet;

public abstract class HighlightProviderBase implements HighlightProvider, Disposable {
    protected final LafManagerListener myLafManagerListener;
    protected Color[] myHighlightColors;
    protected int myHighlightColorRepeatIndex;
    final protected @NotNull DelayedRunner myDelayedRunner;
    protected boolean myHighlightsMode = true;
    protected int myInUpdateRegion = 0;
    protected boolean myPendingChanged = false;
    protected @NotNull ApplicationSettings mySettings;

    private OneTimeRunnable myHighlightRunner = OneTimeRunnable.NULL;
    private final HashSet<HighlightListener> myHighlightListeners;

    public HighlightProviderBase(@NotNull ApplicationSettings settings) {
        mySettings = settings;

        myDelayedRunner = new DelayedRunner();
        myHighlightListeners = new HashSet<>();
        myLafManagerListener = new LafManagerListener() {
            UIManager.LookAndFeelInfo lookAndFeel = LafManager.getInstance().getCurrentLookAndFeel();

            @Override
            public void lookAndFeelChanged(final LafManager source) {
                UIManager.LookAndFeelInfo newLookAndFeel = source.getCurrentLookAndFeel();
                if (lookAndFeel != newLookAndFeel) {
                    lookAndFeel = newLookAndFeel;
                    settingsChanged(mySettings);
                }
            }
        };

        //noinspection ThisEscapedInObjectConstruction
        MessageBusConnection messageBusConnection = ApplicationManager.getApplication().getMessageBus().connect(this);
        messageBusConnection.subscribe(ApplicationSettingsListener.TOPIC, this::settingsChanged);
        myDelayedRunner.addRunnable(messageBusConnection::disconnect);
    }

    public void initComponent() {
        LafManager.getInstance().addLafManagerListener(myLafManagerListener);
        myDelayedRunner.addRunnable(() -> {
            LafManager.getInstance().removeLafManagerListener(myLafManagerListener);
        });

        settingsChanged(mySettings);
    }

    public void disposeComponent() {
        myDelayedRunner.runAll();
    }

    @Override
    public void dispose() {
        disposeComponent();
    }

    @Override
    public void settingsChanged(final ApplicationSettings settings) {
        ColorIterable.ColorIterator iterator = new ColorIterable(
                settings.getHueMin(),
                settings.getHueMax(),
                settings.getHueSteps(),
                settings.getSaturationMin(),
                settings.getSaturationMax(),
                settings.getSaturationSteps(),
                settings.getBrightnessMin(),
                settings.getBrightnessMax(),
                settings.getBrightnessSteps()
        ).iterator();

        myHighlightColors = new Color[iterator.getMaxIndex()];
        while (iterator.hasNext()) {
            Color hsbColor = iterator.next();
            myHighlightColors[iterator.getIndex()] = hsbColor;
        }

        myHighlightColorRepeatIndex = myHighlightColors.length - iterator.getHueSteps();

        fireHighlightsChanged();
    }

    @Override
    public void addHighlightListener(@NotNull HighlightListener highlightListener, @NotNull Disposable parent) {
        if (!myHighlightListeners.contains(highlightListener)) {
            myHighlightListeners.add(highlightListener);
            Disposer.register(parent, new Disposable() {
                @Override
                public void dispose() {
                    myHighlightListeners.remove(highlightListener);
                }
            });
        }
    }

    @Override
    public void removeHighlightListener(@NotNull HighlightListener highlightListener) {
        myHighlightListeners.remove(highlightListener);
    }

    @Override
    public void enterUpdateRegion() {
        if (myInUpdateRegion++ == 0) {
            myPendingChanged = false;
        }
    }

    @Override
    public void leaveUpdateRegion() {
        if (--myInUpdateRegion <= 0) {
            if (myInUpdateRegion < 0) {
                throw new IllegalStateException("InUpdateRegion < 0, " + myInUpdateRegion);
            }
            myInUpdateRegion = 0;
            if (myPendingChanged) {
                myPendingChanged = false;
                fireHighlightsChanged();
            }
        }
    }

    @Override
    public void fireHighlightsChanged() {
        myHighlightRunner.cancel();
        if (myInUpdateRegion <= 0) {
            if (!myHighlightListeners.isEmpty()) {
                myHighlightRunner = OneTimeRunnable.schedule(250, new AwtRunnable(true, () -> {
                    for (HighlightListener listener : myHighlightListeners) {
                        if (listener == null) continue;
                        listener.highlightsChanged();
                    }
                }));
            }
        } else {
            myPendingChanged = true;
        }
    }

    public int getHighlightColorRepeatIndex() {
        return myHighlightColorRepeatIndex;
    }

    public Color[] getHighlightColors() {
        return myHighlightColors;
    }

    public boolean isHighlightsMode() {
        return myHighlightsMode;
    }

    public void setHighlightsMode(final boolean highlightsMode) {
        myHighlightsMode = highlightsMode;
        if (haveHighlights()) {
            fireHighlightsChanged();
        }
    }
}
