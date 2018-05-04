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

import java.util.ArrayList;
import java.util.HashMap;

public class DelayedRunner {
    final private HashMap<Object, ArrayList<Runnable>> myRunnables = new HashMap<>();
    final private Object myUnnamedKey = new Object();

    public DelayedRunner() {

    }

    public void runAll() {
        final Object[] keys = myRunnables.keySet().toArray();
        for (Object key : keys) {
            runAllFor(key);
        }
    }

    public void runAllFor() {
        runAllFor(myUnnamedKey);
    }

    public void runAllFor(Object key) {
        ArrayList<Runnable> runnableList = myRunnables.remove(key);
        if (runnableList != null) {
            for (Runnable runnable : runnableList) {
                runnable.run();
            }
        }
    }

    public void addRunnable(Object key, Runnable runnable) {
        ArrayList<Runnable> list = myRunnables.computeIfAbsent(key, o -> new ArrayList<>());
        list.add(runnable);
    }

    public void addRunnable(Runnable runnable) {
        ArrayList<Runnable> list = myRunnables.computeIfAbsent(myUnnamedKey, o -> new ArrayList<>());
        list.add(runnable);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DelayedRunner)) return false;

        DelayedRunner runner = (DelayedRunner) o;

        return myRunnables.equals(runner.myRunnables);
    }

    @Override
    public int hashCode() {
        return myRunnables.hashCode();
    }
}
