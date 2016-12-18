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

package com.vladsch.MissingInActions.settings;

import com.intellij.ui.DocumentAdapter;
import com.vladsch.MissingInActions.util.ListenersRunner;
import com.vladsch.MissingInActions.util.ReEntryGuard;
import com.vladsch.MissingInActions.util.ui.Settable;
import com.vladsch.MissingInActions.util.ui.SettingsComponents;
import com.vladsch.MissingInActions.util.ui.SettingsConfigurable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NumberingOptionsForm implements SettingsConfigurable<NumberingOptions.AsMutable>, Settable {
    private JTextField myFirst;
    private JTextField myLast;
    private JTextField myTemplate;
    private JTextField myStep;
    private JComboBox myNumberingBase;
    private JCheckBox myBitShift;
    private JCheckBox mySameLineRepeat;
    private JTextField mySeparator;
    private JComboBox myComboBox1;

    private NumberingOptions myOriginalOptions;
    private NumberingOptions.AsMutable myOptions;
    private final SettingsComponents<NumberingOptions.AsMutable> myComponents;
    private final ReEntryGuard myGuard;
    private final ListenersRunner<ChangeListener> myListeners;

    public static interface ChangeListener {
        void optionsChanged(NumberingOptions options);
    } 
    
    public NumberingOptionsForm() {
        this(new NumberingOptions.AsMutable());
    }

    @SuppressWarnings("WeakerAccess")
    public NumberingOptionsForm(NumberingOptions.AsMutable options) {
        this(options, null);
    }
    
    @SuppressWarnings("WeakerAccess")
    public NumberingOptionsForm(NumberingOptions.AsMutable options, @Nullable ChangeListener changeListener) {
        myOriginalOptions = options.toImmutable();
        myOptions = options;
        myGuard = new ReEntryGuard();
        myListeners = new ListenersRunner<>();
        if (changeListener != null) myListeners.addListener(changeListener);

        myComponents = new SettingsComponents<NumberingOptions.AsMutable>() {
            @Override
            protected Settable[] getComponents(NumberingOptions.AsMutable i) {
                return new Settable[] {
                        component(myFirst, i::getFirst, i::setFirst),
                        component(myLast, i::getLast, i::setLast),
                        component(myTemplate, i::getTemplate, i::setTemplate),
                        component(myStep, i::getStep, i::setStep),
                        component(NumberingBaseType.ADAPTER, myNumberingBase, i::getNumberingBase, i::setNumberingBase),
                        component(myBitShift, i::isBitShift, i::setBitShift),
                        component(mySameLineRepeat, i::isRepeatSameLine, i::setRepeatSameLine),
                };
            }
        };

        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                myGuard.ifUnguarded(() -> updateOptions());
            }
        };

        DocumentAdapter documentAdapter = new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                myGuard.ifUnguarded(() -> updateOptions());
            }
        };

        myNumberingBase.addActionListener(actionListener);
        myBitShift.addActionListener(actionListener);
        mySameLineRepeat.addActionListener(actionListener);

        myFirst.getDocument().addDocumentListener(documentAdapter);
        myLast.getDocument().addDocumentListener(documentAdapter);
        myTemplate.getDocument().addDocumentListener(documentAdapter);
        myStep.getDocument().addDocumentListener(documentAdapter);
    }

    private void createUIComponents() {
        myNumberingBase = NumberingBaseType.ADAPTER.createComboBox();
    }

    private void updateOptions() {
        myGuard.guard(this::unguarded_UpdateOptions);
    }

    private void unguarded_UpdateOptions() {
        // add update code here
        
        final NumberingOptions value = myOptions.toImmutable();
        myListeners.fire(listener -> listener.optionsChanged(value));
    }

    public NumberingOptions getOptions() {
        return myOptions;
    }

    public void setOptions(NumberingOptions.AsMutable options) {
        myOptions = options;
        myOriginalOptions = options.toImmutable();
        reset();
    }

    public void setOptions(NumberingOptions options) {
        myOriginalOptions = options;
        myOptions = myOriginalOptions.toMutable();
        reset();
    }

    @Override
    public void reset(NumberingOptions.AsMutable instance) {
        setOptions(instance);
    }

    @Override
    public NumberingOptions.AsMutable apply(NumberingOptions.AsMutable instance) {
        myOptions = instance;
        apply();
        return instance;
    }

    @Override
    public boolean isModified(NumberingOptions.AsMutable instance) {
        return myComponents.isModified(instance);
    }

    @Override
    public void reset() {
        myComponents.reset(myOriginalOptions.toMutable());
    }

    @Override
    public void apply() {
        myComponents.apply(myOptions);
        myOriginalOptions = myOptions.toImmutable();
    }

    @Override
    public boolean isModified() {
        return myComponents.isModified(myOriginalOptions.toMutable());
    }
}
