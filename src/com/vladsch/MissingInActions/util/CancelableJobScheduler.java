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

import com.intellij.concurrency.JobScheduler;
import com.intellij.openapi.components.ServiceManager;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class CancelableJobScheduler {
    public static CancelableJobScheduler getInstance() {
        return ServiceManager.getService(CancelableJobScheduler.class);
    }

    public static CancelableJobScheduler getScheduler() {
        return ServiceManager.getService(CancelableJobScheduler.class);
    }

    final private SortedArrayList<MyRunnable> myRunnables = new SortedArrayList<>(Comparator.comparingLong(o -> o.myScheduledTickTime));
    private AtomicLong myTickTime;
    final private int myResolution = 25;
    final private TimeUnit myTimeUnit = TimeUnit.MILLISECONDS;

    public CancelableJobScheduler() {

        myTickTime = new AtomicLong(Long.MIN_VALUE);
        JobScheduler.getScheduler().scheduleWithFixedDelay(this::onTimerTick, myResolution, myResolution, myTimeUnit);
    }

    public int getResolution() {
        return myResolution;
    }

    public TimeUnit getTimeUnit() {
        return myTimeUnit;
    }

    private void onTimerTick() {
        long tickTime = myTickTime.addAndGet(myResolution);
        
        // run all tasks whose tickTime is <= tickTime
        MyRunnable dummy = new MyRunnable(tickTime, this::onTimerTick);
        myRunnables.removeIfBefore(dummy, MyRunnable::run);
    }

    public CancellableRunnable schedule(Runnable command, int delay) {
        MyRunnable runnable = new MyRunnable(myTickTime.get() + delay, command);
        myRunnables.add(runnable);
        return runnable;
    }

    private static class MyRunnable implements CancellableRunnable {
        private final Runnable myRunnable;
        private final long myScheduledTickTime;
        private final AtomicBoolean myHasRun = new AtomicBoolean(false);

        MyRunnable(long scheduledTickTime, Runnable runnable) {
            myRunnable = runnable;
            myScheduledTickTime = scheduledTickTime;
        }

        @Override
        public boolean cancel() {
            return !myHasRun.getAndSet(true);
        }

        @Override
        public void run() {
            if (!myHasRun.getAndSet(true)) {
                myRunnable.run();
            }
        }
    }
}
