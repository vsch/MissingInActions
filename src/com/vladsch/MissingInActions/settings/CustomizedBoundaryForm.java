// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.settings;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBRadioButton;
import com.vladsch.MissingInActions.util.EditHelpers;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class CustomizedBoundaryForm {
    private JPanel myMainPanel;
    private JBCheckBox myStartOfLine;
    private JBCheckBox myLeadingBlanks;
    private JBCheckBox myTrailingSpaces;
    private JBCheckBox myEndOfLine;
    private JBRadioButton myIdeWord;
    private JBRadioButton myWord;
    private JBRadioButton mySpaceDelimited;
    private JBRadioButton myIdentifier;
    private JBCheckBox mySingleLine;
    private JBCheckBox myMultiCaretSingleLine;
    private JBCheckBox myStartOfWord;
    private JBCheckBox myEndOfWord;

    private int myFlags;

    public CustomizedBoundaryForm() {
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
        if (myLeadingBlanks.isSelected()) flags |= EditHelpers.END_OF_LEADING_BLANKS;
        if (myTrailingSpaces.isSelected()) flags |= EditHelpers.START_OF_TRAILING_BLANKS;
        if (myIdeWord.isSelected()) flags |= EditHelpers.IDE_WORD;
        else if (myWord.isSelected()) flags |= EditHelpers.MIA_WORD;
        else if (myIdentifier.isSelected()) flags |= EditHelpers.MIA_IDENTIFIER;
        else if (mySpaceDelimited.isSelected()) flags |= EditHelpers.SPACE_DELIMITED;
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
        myLeadingBlanks.setSelected((flags & EditHelpers.END_OF_LEADING_BLANKS) != 0);
        myTrailingSpaces.setSelected((flags & EditHelpers.START_OF_TRAILING_BLANKS) != 0);
        myIdeWord.setSelected((flags & EditHelpers.IDE_WORD) != 0);
        myWord.setSelected((flags & EditHelpers.MIA_WORD) != 0);
        myIdentifier.setSelected((flags & EditHelpers.MIA_IDENTIFIER) != 0);
        mySpaceDelimited.setSelected((flags & EditHelpers.SPACE_DELIMITED) != 0);
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
