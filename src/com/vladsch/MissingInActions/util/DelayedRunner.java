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

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class DelayedRunner {
    final private LinkedHashMap<Object, ArrayList<Runnable>> myRunnables = new LinkedHashMap<>();
    final private Object myUnnamedKey = new Object();

    public DelayedRunner() {

    }

    public void runAll() {
        for (ArrayList<Runnable> runnableList : myRunnables.values()) {
            for (Runnable runnable : runnableList) {
                runnable.run();
            }
        }

        myRunnables.clear();
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
