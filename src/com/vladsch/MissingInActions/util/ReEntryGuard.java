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

package com.vladsch.MissingInActions.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

public class ReEntryGuard extends AtomicInteger {
    private @Nullable HashSet<Runnable> myOnExitRunnables = null;

    public ReEntryGuard(int initialValue) {
        super(initialValue);
    }

    public ReEntryGuard() {
    }

    private void decrementAndCheckExit() {
        int exited = decrementAndGet();

        if (exited == 0 && myOnExitRunnables != null && !myOnExitRunnables.isEmpty()) {
            for (Runnable runnable : myOnExitRunnables) {
                runnable.run();
            }

            myOnExitRunnables.clear();
        }
    }

    public void ifUnguarded(@NotNull Runnable runnable) {
        ifUnguarded(runnable, null);
    }

    public void ifUnguarded(boolean ifGuardedRunOnExit, @NotNull Runnable runnable) {
        ifUnguarded(runnable, ifGuardedRunOnExit ? runnable : null);
    }

    public void ifUnguarded(@NotNull Runnable runnable, @Nullable Runnable runOnGuardExit) {
        if (getAndIncrement() == 0) {
            runnable.run();
        } else if (runOnGuardExit != null) {
            synchronized (this) {
                if (myOnExitRunnables == null) myOnExitRunnables = new HashSet<>();
                myOnExitRunnables.add(runOnGuardExit);
            }
        }
        decrementAndGet();
    }

    public void guard(@NotNull Runnable runnable) {
        incrementAndGet();
        runnable.run();
        decrementAndCheckExit();
    }

    public boolean unguarded() {
        return get() == 0;
    }
}
