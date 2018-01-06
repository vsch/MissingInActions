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

import javax.swing.JComboBox;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ComboBoxBooleanAdapterImpl<E extends ComboBoxAdaptable<E>> extends ComboBoxAdapterImpl<E> implements ComboBoxBooleanAdapter<E> {
    protected final E myNonDefault;

    public ComboBoxBooleanAdapterImpl(E falseValue, E trueValue) {
        super(falseValue);
        this.myNonDefault = trueValue;
    }

    @Override
    public void fillComboBox(JComboBox comboBox, ComboBoxAdaptable... exclude) {
        Set<ComboBoxAdaptable> excluded = new HashSet<>(Arrays.asList(exclude));

        comboBox.removeAllItems();
        for (E item : myDefault.getValues()) {
            if (item == myDefault || item == myNonDefault) {
                //noinspection unchecked
                comboBox.addItem(item.getDisplayName());
            }
        }
    }

    @Override
    public E getNonDefault() {
        return myNonDefault;
    }

    @Override
    public boolean isBoolean() {
        return true;
    }
}
