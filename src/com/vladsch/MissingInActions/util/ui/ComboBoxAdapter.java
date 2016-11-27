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

package com.vladsch.MissingInActions.util.ui;

import com.intellij.openapi.util.Pair;

import javax.swing.*;

public interface ComboBoxAdapter<E extends ComboBoxAdaptable<E>> {

    static boolean onFirst(ComboBoxAdapter adapter, int intValue, OnMap map) {
        OnIt on = map.on(new OnIt());

        for (Pair<ComboBoxAdaptable, Runnable> doRun : on.getList()) {
            if (doRun.getFirst().getIntValue() == intValue && adapter.isAdaptable(doRun.getFirst())) {
                doRun.getSecond().run();
                return true;
            }
        }
        return false;
    }

    static boolean onAll(ComboBoxAdapter adapter, int intValue, OnMap map) {
        boolean ran = false;
        OnIt on = map.on(new OnIt());

        for (Pair<ComboBoxAdaptable, Runnable> doRun : on.getList()) {
            if (doRun.getFirst().getIntValue() == intValue && adapter.isAdaptable(doRun.getFirst())) {
                doRun.getSecond().run();
                ran = true;
            }
        }
        return ran;
    }

    boolean isAdaptable(ComboBoxAdaptable type);
    void fillComboBox(JComboBox comboBox, E... exclude);
    E findEnum(int intValue);
    E findEnum(String displayName);
    E valueOf(String name);
    E getDefault();
}
