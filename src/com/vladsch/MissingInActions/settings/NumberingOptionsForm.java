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

package com.vladsch.MissingInActions.settings;

import com.intellij.ui.DocumentAdapter;
import com.vladsch.plugin.util.ListenersRunner;
import com.vladsch.plugin.util.ReEntryGuard;
import com.vladsch.plugin.util.ui.Settable;
import com.vladsch.plugin.util.ui.SettingsComponents;
import com.vladsch.plugin.util.ui.SettingsConfigurable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionListener;

public class NumberingOptionsForm implements SettingsConfigurable<NumberingOptions>, Settable {
    private JPanel myMainPanel;
    private JTextField myFirst;
    private JTextField myLast;
    private JTextField myTemplate;
    private JTextField myStep;
    private JComboBox<String> myNumberingBase;
    private JCheckBox myBitShift;
    private JCheckBox myRepeatSameLine;
    private JRadioButton myUpperCase;
    private JComboBox<String> mySeparatorFrequency;
    private JTextField mySeparator;
    private JTextField myDecimalPoint;
    private JTextField myPrefix;
    private JTextField mySuffix;
    private JRadioButton myLowerCase;
    private JLabel myFirstBase;
    private JLabel myLastBase;
    private JLabel myStepBase;

    private NumberingOptions myOriginalOptions;
    private NumberingOptions myOptions;
    private final SettingsComponents<NumberingOptions> myComponents;
    private final ReEntryGuard myGuard;
    private final ListenersRunner<ChangeListener> myChangeListeners;
    private final ListenersRunner<BaseChangeListener> myBaseChangeListeners;

    public static interface ChangeListener {
        void optionsChanged(NumberingOptions options);
    }

    public static interface BaseChangeListener {
        void baseChanged(NumberingOptions oldOptions, NumberingOptions newOptions);
    }

    public NumberingOptionsForm() {
        this(new NumberingOptions());
    }

    @SuppressWarnings("WeakerAccess")
    public NumberingOptionsForm(NumberingOptions options) {
        this(options, null, null);
    }

    public void addChangeListener(@NotNull ChangeListener changeListener) {
        myChangeListeners.addListener(changeListener);
    }

    public void removeChangeListener(@NotNull ChangeListener changeListener) {
        myChangeListeners.removeListener(changeListener);
    }

    public void addBaseChangeListener(@NotNull BaseChangeListener changeListener) {
        myBaseChangeListeners.addListener(changeListener);
    }

    public void removeBaseChangeListener(@NotNull BaseChangeListener changeListener) {
        myBaseChangeListeners.removeListener(changeListener);
    }

    @SuppressWarnings("WeakerAccess")
    public NumberingOptionsForm(NumberingOptions options, @Nullable ChangeListener changeListener, @Nullable BaseChangeListener baseChangeListener) {
        myOriginalOptions = options.copy();
        myOptions = options.copy();
        myGuard = new ReEntryGuard();
        myChangeListeners = new ListenersRunner<>();
        myBaseChangeListeners = new ListenersRunner<>();
        if (changeListener != null) myChangeListeners.addListener(changeListener);
        if (baseChangeListener != null) myBaseChangeListeners.addListener(baseChangeListener);

        myComponents = new SettingsComponents<NumberingOptions>() {
            @Override
            protected Settable[] createComponents(@NotNull NumberingOptions i) {
                return new Settable[] {
                        component(myFirst, i::getFirst, i::setFirst),
                        component(myLast, i::getLast, i::setLast),
                        component(myTemplate, i::getTemplate, i::setTemplate),
                        component(myStep, i::getStep, i::setStep),
                        component(NumberingBaseType.ADAPTER, myNumberingBase, i::getNumberingBase, i::setNumberingBase),
                        component(myBitShift, i::isBitShift, i::setBitShift),
                        component(myRepeatSameLine, i::isRepeatSameLine, i::setRepeatSameLine),
                        component(SeparatorFrequencyType.ADAPTER, mySeparatorFrequency, i::getSeparatorFrequency, i::setSeparatorFrequency),
                        component(mySeparator, i::getSeparator, i::setSeparator),
                        component(myDecimalPoint, i::getDecimalPoint, i::setDecimalPoint),
                        component(myPrefix, i::getPrefix, i::setPrefix),
                        component(mySuffix, i::getSuffix, i::setSuffix),
                        component(myUpperCase, i::isUpperCase, i::setUpperCase),
                        component(myLowerCase, () -> !i.isUpperCase(), (upperCase) -> i.setUpperCase(!upperCase)),
                };
            }
        };

        ActionListener actionListener = e -> myGuard.ifUnguarded(this::unguarded_UpdateOptions);

        DocumentAdapter documentAdapter = new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                myGuard.ifUnguarded(() -> {
                    unguarded_UpdateOptions();
                });
            }
        };

        myNumberingBase.addActionListener((event) -> {
            myGuard.ifUnguarded(this::unguarded_BaseChanged);
        });

        myBitShift.addActionListener(actionListener);
        myRepeatSameLine.addActionListener(actionListener);
        mySeparatorFrequency.addActionListener(actionListener);
        myUpperCase.addActionListener(actionListener);
        myLowerCase.addActionListener(actionListener);

        myFirst.getDocument().addDocumentListener(documentAdapter);
        myLast.getDocument().addDocumentListener(documentAdapter);
        myTemplate.getDocument().addDocumentListener(documentAdapter);
        myStep.getDocument().addDocumentListener(documentAdapter);
        mySeparator.getDocument().addDocumentListener(documentAdapter);
        myDecimalPoint.getDocument().addDocumentListener(documentAdapter);
        myPrefix.getDocument().addDocumentListener(documentAdapter);
        mySuffix.getDocument().addDocumentListener(documentAdapter);

        reset();
    }

    public void setFirstBase(@NotNull String firstBase) {
        myFirstBase.setText(firstBase);
    }

    public void setLastBase(@NotNull String lastBase) {
        myLastBase.setText(lastBase);
    }

    public void setStepBase(@NotNull String stepBase) {
        myStepBase.setText(stepBase);
    }

    private void unguarded_BaseChanged() {
        int oldNumberingBase = myOptions.getNumberingBase();
        NumberingOptions oldOptions = myOptions.copy();
        NumberingOptions newOptions = myOptions.copy();

        // need to save the numbering base options
        apply(oldOptions);
        oldOptions.setNumberingBase(oldNumberingBase);

        apply(newOptions);

        myBaseChangeListeners.fire(listener -> listener.baseChanged(oldOptions.copy(), newOptions.copy()));

        // now update options
        unguarded_UpdateOptions();
    }

    private void createUIComponents() {
        myNumberingBase = NumberingBaseType.ADAPTER.createComboBox();
        mySeparatorFrequency = SeparatorFrequencyType.ADAPTER.createComboBox();
    }

    private void updateOptions() {
        myGuard.guard(this::unguarded_UpdateOptions);
    }

    private void unguarded_UpdateOptions() {
        // add update code here
        apply(myOptions);

        int base = myOptions.getNumberingBase();
        if (base <= 10) {
            myUpperCase.setVisible(false);
            myLowerCase.setVisible(false);
        } else {
            myUpperCase.setVisible(true);
            myLowerCase.setVisible(true);
        }
        mySeparatorFrequency.setEnabled(!mySeparator.getText().isEmpty());

        myChangeListeners.fire(listener -> listener.optionsChanged(myOptions.copy()));
    }

    public NumberingOptions getOptions() {
        apply(myOptions);
        return myOptions.copy();
    }

    public void setOptions(NumberingOptions options) {
        myOptions = options.copy();
        myOriginalOptions = options.copy();
        reset();
    }

    @Override
    public void reset(@NotNull NumberingOptions instance) {
        setOptions(instance);
    }

    @NotNull
    @Override
    public NumberingOptions apply(@NotNull NumberingOptions instance) {
        myComponents.apply(instance);
        return instance;
    }

    @Override
    public boolean isModified(@NotNull NumberingOptions instance) {
        return myComponents.isModified(instance);
    }

    @Override
    public void reset() {
        myComponents.reset(myOriginalOptions);
    }

    @Override
    public void apply() {
        myComponents.apply(myOptions);
        myOriginalOptions = myOptions.copy();
    }

    @Override
    public boolean isModified() {
        return myComponents.isModified(myOriginalOptions);
    }

    public JComponent getComponent() {
        return myMainPanel;
    }

    @SuppressWarnings("WeakerAccess")
    @Nullable
    public JComponent getPreferredFocusedComponent() {
        return myFirst;
    }
}
