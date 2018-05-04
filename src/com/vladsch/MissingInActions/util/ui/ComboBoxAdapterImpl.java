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

package com.vladsch.MissingInActions.util.ui;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComboBox;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ComboBoxAdapterImpl<E extends ComboBoxAdaptable<E>> implements ComboBoxAdapter<E> {
    final E myDefault;

    public ComboBoxAdapterImpl(E defaultValue) {
        this.myDefault = defaultValue;
    }

    @Override
    public boolean onFirst(int intValue, OnMap map) {
        OnIt on = map.on(new OnIt());

        for (Pair<ComboBoxAdaptable, Runnable> doRun : on.getList()) {
            if (doRun.getFirst().getIntValue() == intValue && isAdaptable(doRun.getFirst())) {
                doRun.getSecond().run();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onAll(int intValue, OnMap map) {
        boolean ran = false;
        OnIt on = map.on(new OnIt());

        for (Pair<ComboBoxAdaptable, Runnable> doRun : on.getList()) {
            if (doRun.getFirst().getIntValue() == intValue && isAdaptable(doRun.getFirst())) {
                doRun.getSecond().run();
                ran = true;
            }
        }
        return ran;
    }

    @Override
    public void fillComboBox(JComboBox comboBox, ComboBoxAdaptable... exclude) {
        Set<ComboBoxAdaptable> excluded = new HashSet<>(Arrays.asList(exclude));

        //if (excluded.contains(myDefault)) {
        //    throw new IllegalStateException("Default item cannot be excluded");
        //}

        comboBox.removeAllItems();
        for (E item : myDefault.getValues()) {
            if (!excluded.contains(item)) {
                String displayName = item.getDisplayName();
                //noinspection unchecked
                comboBox.addItem(displayName);
            }
        }
    }

    @Override
    public boolean setComboBoxSelection(final JComboBox comboBox, final ComboBoxAdaptable selection) {
        int iMax = comboBox.getItemCount();
        int defaultIndex = 0;
        for (int i = 0; i < iMax; i++) {
            final Object item = comboBox.getItemAt(i);
            if (item.equals(selection.getDisplayName())) {
                comboBox.setSelectedIndex(i);
                return true;
            }
            if (item.equals(myDefault.getDisplayName())) {
                defaultIndex = i;
            }
        }
        comboBox.setSelectedIndex(defaultIndex);
        return false;
    }

    @Override
    public boolean isAdaptable(ComboBoxAdaptable type) {
        return myDefault.getClass() == type.getClass();
    }

    @NotNull
    @Override
    public E findEnum(int intValue) {
        for (E item : myDefault.getValues()) {
            if (item.getIntValue() == intValue) {
                return item;
            }
        }
        return myDefault;
    }

    @NotNull
    @Override
    public E get(final JComboBox comboBox) {
        return findEnum((String) comboBox.getSelectedItem());
    }

    @NotNull
    @Override
    public E findEnum(String displayName) {
        for (E item : myDefault.getValues()) {
            if (item.getDisplayName().equals(displayName)) {
                return item;
            }
        }
        return myDefault;
    }

    @NotNull
    @Override
    public E findEnumName(String name) {
        for (E item : myDefault.getValues()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return myDefault;
    }

    @NotNull
    @Override
    public E valueOf(String name) {
        for (E item : myDefault.getValues()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return myDefault;
    }

    @NotNull
    @Override
    public E getDefault() {
        return myDefault;
    }

    @Override
    public boolean isBoolean() {
        return false;
    }
}
