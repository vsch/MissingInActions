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

import javax.swing.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ComboBoxAdapterImpl<E extends ComboBoxAdaptable<E>> implements ComboBoxAdapter<E> {
    protected final E myDefault;

    public ComboBoxAdapterImpl(E defaultValue) {
        this.myDefault = defaultValue;
    }

    @Override
    public void fillComboBox(JComboBox comboBox, E... exclude) {
        Set<E> excluded = new HashSet<E>(Arrays.asList(exclude));

        for (E item : myDefault.getEnumValues()) {
            if (!excluded.contains(item)) {
                String displayName = item.getDisplayName();
                comboBox.addItem(displayName);
            }
        }
    }

    @Override
    public boolean isAdaptable(ComboBoxAdaptable type) {
        return myDefault.getClass() == type.getClass();
    }

    @Override
    public E findEnum(int intValue) {
        for (E item : myDefault.getEnumValues()) {
            if (item.getIntValue() == intValue) {
                return item;
            }
        }
        return myDefault;
    }

    @Override
    public E get(final JComboBox comboBox) {
        return findEnum((String) comboBox.getSelectedItem());
    }

    @Override
    public E findEnum(String displayName) {
        for (E item : myDefault.getEnumValues()) {
            if (item.getDisplayName().equals(displayName)) {
                return item;
            }
        }
        return myDefault;
    }

    @Override
    public E valueOf(String name) {
        for (E item : myDefault.getEnumValues()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return myDefault;
    }

    @Override
    public E getDefault() {
        return myDefault;
    }

}
