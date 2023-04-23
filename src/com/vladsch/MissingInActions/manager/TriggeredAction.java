// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.manager;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.util.Computable;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("WeakerAccess")
public class TriggeredAction {
    final private @NotNull AnAction myAction;
    final private @NotNull Computable<Integer> myDelay;
    final private @NotNull Computable<Boolean> myEnabled;

    public TriggeredAction(@NotNull AnAction action, @NotNull Computable<Integer> delay, @NotNull Computable<Boolean> enabled) {
        myAction = action;
        myDelay = delay;
        myEnabled = enabled;
    }

    @NotNull
    public AnAction getAction() {
        return myAction;
    }

    public int getDelay() {
        return myDelay.compute();
    }

    public boolean isEnabled() {
        return myEnabled.compute();
    }
}
