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

package com.vladsch.MissingInActions.util;

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

import static javax.swing.SwingUtilities.isEventDispatchThread;

/**
 * Used to create a task that can be run at most once and the run can be cancelled before
 * it has run, in which case further attempts to run it will do nothing.
 * <p>
 * Can also specify that it should run on the AWT thread, otherwise it will run on the application thread
 * <p>
 * Useful for triggering actions after a delay that may need to be run before the delay triggers
 */
public class OneTimeRunnable extends AwtRunnable implements CancellableRunnable {
    static public final OneTimeRunnable NULL = new OneTimeRunnable(() -> {
    }).cancelled();
    final private AtomicBoolean myHasRun;

    private OneTimeRunnable cancelled() {
        myHasRun.set(true);
        return this;
    }

    public OneTimeRunnable(Runnable command) {
        this(false, command);
    }

    public OneTimeRunnable(boolean awtThread, Runnable command) {
        super(awtThread, command);
        myHasRun = new AtomicBoolean(false);
    }

    /**
     * Cancels the scheduled task run if it has not run yet
     *
     * @return true if cancelled, false if it has already run
     */
    public boolean cancel() {
        return !myHasRun.getAndSet(true);
    }

    @Override
    public void run() {
        if (isAwtThread() && !isEventDispatchThread()) {
            ApplicationManager.getApplication().invokeLater(this);
        } else {
            if (!myHasRun.getAndSet(true)) {
                super.run();
            }
        }
    }

    /**
     * Creates a one-shot runnable that will run after a delay, can be run early, or cancelled
     * <p>
     * the given command will only be executed once, either by the delayed trigger or by the run method.
     * if you want to execute the task early just invoke #run, it will do nothing if the task has already run.
     *
     * @param delay   the time from now to delay execution
     * @param command the task to execute
     * @return a {@link OneTimeRunnable} which will run after the given
     * delay or if {@link #run()} is invoked before {@link #cancel()} is invoked
     * @throws NullPointerException if command is null
     */
    public static OneTimeRunnable schedule(int delay, @NotNull Runnable command) {
        OneTimeRunnable runnable = command instanceof OneTimeRunnable ? (OneTimeRunnable) command : new OneTimeRunnable(command);
        CancelableJobScheduler.getScheduler().schedule(runnable, delay);
        return runnable;
    }
}
