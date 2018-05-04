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
