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

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public interface ComboBoxAdaptable<E> {
    String getDisplayName();
    String name();
    int getIntValue();
    E[] getValues();
    boolean isDefault();

    class Static<T extends ComboBoxAdaptable<T>> implements ComboBoxAdapter<T> {
        protected final @NotNull ComboBoxAdapter<T> ADAPTER;

        public Static(@NotNull ComboBoxAdapter<T> ADAPTER) {
            this.ADAPTER = ADAPTER;
        }

        public T get(JComboBox comboBox) {
            return ADAPTER.findEnum((String) comboBox.getSelectedItem());
        }

        @Override
        public T valueOf(String name) {return ADAPTER.valueOf(name);}

        @Override
        public T getDefault() {return ADAPTER.getDefault();}

        public T get(int value) {
            return ADAPTER.findEnum(value);
        }

        public int getInt(JComboBox comboBox) {
            return ADAPTER.findEnum((String) comboBox.getSelectedItem()).getIntValue();
        }

        public void set(JComboBox comboBox, int intValue) {
            comboBox.setSelectedItem(ADAPTER.findEnum(intValue).getDisplayName());
        }

        public JComboBox createComboBox(ComboBoxAdaptable... exclude) {
            JComboBox comboBox = new JComboBox();
            ADAPTER.fillComboBox(comboBox, exclude);
            return comboBox;
        }

        @Override
        public boolean isAdaptable(ComboBoxAdaptable type) { return ADAPTER.isAdaptable(type); }

        @Override
        public void fillComboBox(JComboBox comboBox, ComboBoxAdaptable[] exclude) { ADAPTER.fillComboBox(comboBox, exclude); }

        @Override
        public T findEnum(int intValue) { return ADAPTER.findEnum(intValue); }

        @Override
        public T findEnum(String displayName) { return ADAPTER.findEnum(displayName); }

        @Override
        public boolean isBoolean() { return ADAPTER.isBoolean(); }

        @Override
        public boolean onFirst(int intValue, OnMap map) { return ADAPTER.onFirst(intValue, map); }

        @Override
        public boolean onAll(int intValue, OnMap map) { return ADAPTER.onAll(intValue, map); }
    }

    class StaticBoolean<T extends ComboBoxAdaptable<T>> extends Static<T> implements ComboBoxBooleanAdapter<T> {
        public StaticBoolean(@NotNull ComboBoxBooleanAdapter<T> ADAPTER) {
            super(ADAPTER);
        }

        @Override
        public T getNonDefault() {return ((ComboBoxBooleanAdapter<T>)ADAPTER).getNonDefault();}
    }
}
