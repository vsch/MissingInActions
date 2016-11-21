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

import com.intellij.ui.components.JBCheckBox;
import com.vladsch.MissingInActions.util.EditHelpers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CustomizedBoundaryForm {
    private JPanel myMainPanel;
    private JBCheckBox myStartOfLine;
    private JBCheckBox myLeadingBlanks;
    private JBCheckBox myTrailingSpaces;
    private JBCheckBox myEndOfLine;
    private JBCheckBox myStartOfFold;
    private JBCheckBox myEndOfFold;
    private JBCheckBox myIdentifier;
    private JBCheckBox mySingleLine;
    private JBCheckBox myMultiCaretSingleLine;
    private JBCheckBox myStartOfWord;
    private JBCheckBox myEndOfWord;

    private int myFlags;

    public CustomizedBoundaryForm() {
        myStartOfFold.setVisible(false);
        myEndOfFold.setVisible(false);
        myStartOfWord.setEnabled(false);
        myEndOfWord.setEnabled(false);

        mySingleLine.addActionListener(e -> {
            myMultiCaretSingleLine.setEnabled(!mySingleLine.isSelected());
            if (mySingleLine.isSelected()) myMultiCaretSingleLine.setSelected(true);
            else myMultiCaretSingleLine.setSelected((myFlags & EditHelpers.MULTI_CARET_SINGLE_LINE) != 0);
        });

        myMultiCaretSingleLine.addActionListener(e -> {
            myFlags &= ~EditHelpers.MULTI_CARET_SINGLE_LINE;
            if (myMultiCaretSingleLine.isSelected()) myFlags |= EditHelpers.MULTI_CARET_SINGLE_LINE;
        });
    }

    public int getValue() {
        int flags = 0;
        if (myStartOfWord.isSelected()) flags |= EditHelpers.START_OF_WORD;
        if (myEndOfWord.isSelected()) flags |= EditHelpers.END_OF_WORD;
        if (myStartOfLine.isSelected()) flags |= EditHelpers.START_OF_LINE;
        if (myEndOfLine.isSelected()) flags |= EditHelpers.END_OF_LINE;
        if (myStartOfFold.isSelected()) flags |= EditHelpers.START_OF_FOLDING_REGION;
        if (myEndOfFold.isSelected()) flags |= EditHelpers.END_OF_FOLDING_REGION;
        if (myLeadingBlanks.isSelected()) flags |= EditHelpers.END_OF_LEADING_BLANKS;
        if (myTrailingSpaces.isSelected()) flags |= EditHelpers.START_OF_TRAILING_BLANKS;
        if (myIdentifier.isSelected()) flags |= EditHelpers.IDENTIFIER;
        if (mySingleLine.isSelected()) flags |= EditHelpers.SINGLE_LINE;
        if (myMultiCaretSingleLine.isEnabled()) {
            if (myMultiCaretSingleLine.isSelected()) flags |= EditHelpers.MULTI_CARET_SINGLE_LINE;
        } else {
            flags |= (myFlags & EditHelpers.MULTI_CARET_SINGLE_LINE);
        }

        return flags;
    }

    public void setValue(int flags) {
        myFlags = flags;
        myStartOfWord.setSelected((flags & EditHelpers.START_OF_WORD) != 0);
        myEndOfWord.setSelected((flags & EditHelpers.END_OF_WORD) != 0);
        myStartOfLine.setSelected((flags & EditHelpers.START_OF_LINE) != 0);
        myEndOfLine.setSelected((flags & EditHelpers.END_OF_LINE) != 0);
        myStartOfFold.setSelected((flags & EditHelpers.START_OF_FOLDING_REGION) != 0);
        myEndOfFold.setSelected((flags & EditHelpers.END_OF_FOLDING_REGION) != 0);
        myLeadingBlanks.setSelected((flags & EditHelpers.END_OF_LEADING_BLANKS) != 0);
        myTrailingSpaces.setSelected((flags & EditHelpers.START_OF_TRAILING_BLANKS) != 0);
        myIdentifier.setSelected((flags & EditHelpers.IDENTIFIER) != 0);
        mySingleLine.setSelected((flags & EditHelpers.SINGLE_LINE) != 0);
        myMultiCaretSingleLine.setSelected((flags & EditHelpers.MULTI_CARET_SINGLE_LINE) != 0);
    }

    public JComponent getComponent() {
        return myMainPanel;
    }

    protected JPanel getMainFormPanel() {
        return myMainPanel;
    }
}
