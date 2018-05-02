/*
 * Copyright (c) 2016-2017 Vladimir Schneider <vladimir.schneider@gmail.com>
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

import org.jetbrains.annotations.NotNull;

import javax.swing.JComboBox;

public interface ComboBoxAdapter<E extends ComboBoxAdaptable<E>> {
    boolean isAdaptable(ComboBoxAdaptable type);
    boolean onFirst(int intValue, OnMap map);
    boolean onAll(int intValue, OnMap map);
    void fillComboBox(JComboBox comboBox, ComboBoxAdaptable... exclude);
    @SuppressWarnings("UnusedReturnValue")
    boolean setComboBoxSelection(JComboBox comboBox, final ComboBoxAdaptable selection);
    @NotNull
    E findEnum(int intValue);
    @NotNull
    E findEnum(String displayName);
    @NotNull
    E findEnumName(String name);
    @NotNull
    E get(JComboBox comboBox);
    @NotNull
    E valueOf(String name);
    @NotNull
    E getDefault();
    boolean isBoolean();
}
