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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

import static com.vladsch.MissingInActions.util.EditHelpers.*;
import static com.vladsch.MissingInActions.util.EditHelpers.END_OF_FOLDING_REGION;
import static com.vladsch.MissingInActions.util.EditHelpers.START_OF_FOLDING_REGION;

@State(
        name = "MissingInActions",
        storages = {
                @Storage(id = "MissingInActionSettings", file = StoragePathMacros.APP_CONFIG + "/MissingInAction.xml", roamingType = RoamingType.DISABLED, deprecated = true),
                @Storage(id = "MissingInActionSettings", file = StoragePathMacros.APP_CONFIG + "/MissingInActions.xml", roamingType = RoamingType.DISABLED)
        }
)
@SuppressWarnings("WeakerAccess")
public class ApplicationSettings implements ApplicationComponent, PersistentStateComponent<ApplicationSettings> {
    // @formatter:off
    private boolean     myAddPrefixOnPaste = false;
    private boolean     myAutoIndent = false;
    private boolean     myCopyLineOrLineSelection = false;
    private boolean     myDeleteOperations = false;
    private boolean     myDuplicateAtStartOrEnd = false;
    private boolean     myIndentUnindent = false;
    private boolean     myIsSelectionEndExtended = false;
    private boolean     myIsSelectionStartExtended = false;
    private boolean     myLeftRightMovement = false;
    private boolean     myMouseCamelHumpsFollow = false;
    private boolean     myMouseLineSelection = false;
    private boolean     myMultiPasteShowEolInViewer = false;
    private boolean     myMultiPasteShowEolInList = true;
    private boolean     myMultiPasteShowInstructions = true;
    private boolean     myMultiPasteShowOptions = true;
    private boolean     myMultiPastePreserveOriginal = true;
    private boolean     myMultiPasteDeleteRepeatedCaretData = true;
    private boolean     myOverrideStandardPaste = false;
    private boolean     myPreserveCamelCaseOnPaste = false;
    private boolean     myPreserveScreamingSnakeCaseOnPaste = false;
    private boolean     myPreserveSnakeCaseOnPaste = false;
    private boolean     myRemovePrefixOnPaste = false;
    private boolean     mySelectPasted = false;
    private boolean     mySelectPastedMultiCaret = false;
    private boolean     myStartEndAsLineSelection = false;
    private boolean     myTypingDeletesLineSelection = false;
    private boolean     myUnselectToggleCase = false;
    private boolean     myUpDownMovement = false;
    private boolean     myUpDownSelection = false;
    private boolean     myWeSetVirtualSpace = true;
    private int         myAutoIndentDelay = 300;
    private int         myAutoLineMode = AutoLineModeType.ADAPTER.getDefault().getIntValue();
    private int         myCaretOnMoveSelectionDown = CaretAdjustmentType.ADAPTER.getDefault().intValue;
    private int         myCaretOnMoveSelectionUp = CaretAdjustmentType.ADAPTER.getDefault().intValue;
    private int         myDuplicateAtStartOrEndPredicate = SelectionPredicateType.WHEN_HAS_1_PLUS_LINES.intValue;
    private int         myLinePasteCaretAdjustment = LinePasteCaretAdjustmentType.NONE.intValue;
    private int         myMouseModifier = MouseModifierType.ADAPTER.getDefault().getIntValue();
    private int         myPrefixOnPastePattern = PrefixOnPastePatternType.ADAPTER.getDefault().intValue;
    private int         mySelectPastedMultiCaretPredicate = SelectionPredicateType.WHEN_HAS_1_PLUS_LINES.intValue;
    private int         mySelectPastedPredicate = SelectionPredicateType.WHEN_HAS_2_PLUS_LINES.intValue;
    private @NotNull String myRegexSampleText = "myCamelCaseMember|ourCamelCaseMember|isCamelCaseMember()|getCamelCaseMember()|setCamelCaseMember()";
    private @NotNull String myPrefixesOnPasteText = "my|our|is|get|set";
    // @formatter:on
    private NumberingOptions myLastNumberingOptions = new NumberingOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_0 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_1 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_2 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_3 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_4 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_5 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_6 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_7 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_8 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_9 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_10 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_11 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_12 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_13 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_14 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_15 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_16 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_17 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_18 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_19 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_20 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_21 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_22 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_23 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_24 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_25 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_26 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_27 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_28 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_29 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_30 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_31 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_32 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_33 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_34 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_35 = new NumberingBaseOptions();
    private NumberingBaseOptions myLastNumberingBaseOptions_36 = new NumberingBaseOptions();

    public static final String PREFIX_DELIMITER = "|";
    public static final String PREFIX_SPLIT_REGEX = "\\s*\\" + PREFIX_DELIMITER + "\\s*";

    // cached values
    private @Nullable Pattern myPrefixesOnPastePattern = null;
    private @Nullable String[] myPrefixesOnPasteList = null;
    private @Nullable String[] myRegexSampleList = null;

    // get/set enums directly
    // @formatter:off
    public PrefixOnPastePatternType getRemovePrefixOnPastePatternType() { return PrefixOnPastePatternType.ADAPTER.get(myPrefixOnPastePattern); }
    public CaretAdjustmentType getCaretOnMoveSelectionDownType() { return CaretAdjustmentType.ADAPTER.get(myCaretOnMoveSelectionDown); }
    public CaretAdjustmentType getCaretOnMoveSelectionUpType() { return CaretAdjustmentType.ADAPTER.get(myCaretOnMoveSelectionUp); }
    public SelectionPredicateType getDuplicateAtStartOrEndPredicateType() { return SelectionPredicateType.ADAPTER.get(myDuplicateAtStartOrEndPredicate); }
    public LinePasteCaretAdjustmentType getLinePasteCaretAdjustmentType() { return LinePasteCaretAdjustmentType.ADAPTER.get(myLinePasteCaretAdjustment); }
    public SelectionPredicateType getSelectPastedPredicateType() { return SelectionPredicateType.ADAPTER.get(mySelectPastedPredicate); }
    public SelectionPredicateType getSelectPastedMultiCaretPredicateType() { return SelectionPredicateType.ADAPTER.get(mySelectPastedMultiCaretPredicate); }
    public void setRemovePrefixOnPastePatternType(PrefixOnPastePatternType prefixOnPastePatternType) { myPrefixOnPastePattern = prefixOnPastePatternType.intValue; }
    public void setCaretOnMoveSelectionDownType(CaretAdjustmentType caretOnMoveSelectionDownType) { myCaretOnMoveSelectionDown = caretOnMoveSelectionDownType.intValue; }
    public void setCaretOnMoveSelectionUpType(CaretAdjustmentType caretOnMoveSelectionUpType) { myCaretOnMoveSelectionUp = caretOnMoveSelectionUpType.intValue; }
    public void setDuplicateAtStartOrEndPredicateType(SelectionPredicateType duplicateAtStartOrEndPredicateType) { myDuplicateAtStartOrEndPredicate = duplicateAtStartOrEndPredicateType.intValue; }
    public void setLinePasteCaretAdjustmentType(LinePasteCaretAdjustmentType linePasteCaretAdjustmentType) { myLinePasteCaretAdjustment = linePasteCaretAdjustmentType.intValue; }
    public void setSelectPastedPredicateType(SelectionPredicateType selectPastedPredicateType) { mySelectPastedPredicate = selectPastedPredicateType.intValue; }
    public void setSelectPastedMultiCaretPredicateType(SelectionPredicateType selectPastedMultiCaretPredicateType) { mySelectPastedMultiCaretPredicate = selectPastedMultiCaretPredicateType.intValue; }
    // @formatter:on

    // customized word flags
    @SuppressWarnings({ "ConstantConditionalExpression", "PointlessBitwiseExpression" })
    final private static int CUSTOMIZED_DEFAULTS = (true ? START_OF_LINE : 0)
            | (true ? END_OF_LINE : 0)
            | (true ? START_OF_TRAILING_BLANKS | END_OF_LEADING_BLANKS : 0)
            | (false ? IDE_WORD : 0)
            | (false ? MIA_WORD : 0)
            | (true ? MIA_IDENTIFIER : 0)
            | (false ? SPACE_DELIMITED : 0)
            | (false ? START_OF_WORD : 0)
            | (false ? END_OF_WORD : 0)
            | (false ? START_OF_FOLDING_REGION : 0)
            | (false ? END_OF_FOLDING_REGION : 0)
            | (false ? SINGLE_LINE : 0)
            | (true ? MULTI_CARET_SINGLE_LINE : 0);

    private int myCustomizedNextWordBounds = CUSTOMIZED_DEFAULTS | START_OF_WORD | END_OF_WORD;
    private int myCustomizedPrevWordBounds = CUSTOMIZED_DEFAULTS | START_OF_WORD | END_OF_WORD;
    private int myCustomizedNextWordStartBounds = CUSTOMIZED_DEFAULTS | START_OF_WORD;
    private int myCustomizedPrevWordStartBounds = CUSTOMIZED_DEFAULTS | START_OF_WORD;
    private int myCustomizedNextWordEndBounds = CUSTOMIZED_DEFAULTS | END_OF_WORD;
    private int myCustomizedPrevWordEndBounds = CUSTOMIZED_DEFAULTS | END_OF_WORD;

    public boolean isLineModeEnabled() {
        return myAutoLineMode != AutoLineModeType.DISABLED.getIntValue() && (
                myMouseLineSelection
                        || myUpDownSelection
                //|| myDeleteOperations
                //|| myUpDownMovement
                //|| myLeftRightMovement
        );
    }

    @Nullable
    public String[] getPrefixesOnPasteList() {
        if (myPrefixesOnPasteList == null && !myPrefixesOnPasteText.isEmpty()) {
            if (myPrefixOnPastePattern != PrefixOnPastePatternType.REGEX.getIntValue()) {
                myPrefixesOnPasteList = myPrefixesOnPasteText.split(PREFIX_SPLIT_REGEX);
            } else {
                myPrefixesOnPasteList = new String[] { myPrefixesOnPasteText };
            }
        }
        return myPrefixesOnPasteList;
    }

    @Nullable
    public String[] getRegexSampleList() {
        if (myRegexSampleList == null && !myRegexSampleText.isEmpty()) {
            myRegexSampleList = myRegexSampleText.split(PREFIX_SPLIT_REGEX);
        }
        return myPrefixesOnPasteList;
    }

    @Nullable
    public Pattern getPrefixesOnPastePattern() {
        if (myPrefixesOnPastePattern == null && !myPrefixesOnPasteText.isEmpty()) {
            if (myPrefixOnPastePattern == PrefixOnPastePatternType.REGEX.getIntValue()) {
                try {
                    myPrefixesOnPastePattern = Pattern.compile(myPrefixesOnPasteText);
                } catch (Throwable error) {
                    myPrefixOnPastePattern = PrefixOnPastePatternType.CAMEL.getIntValue();
                    throw error;
                }
            } else {
                // Can build it from the list, but not now
                StringBuilder sb = new StringBuilder();
                sb.append("^(?:\\Q");
                String sep = "";
                final String[] prefixList = getPrefixesOnPasteList();
                assert prefixList != null;
                for (String prefix : prefixList) {
                    if (!prefix.isEmpty()) {
                        sb.append(sep);
                        sep = "\\E|\\Q";
                        sb.append(prefix);
                    }
                }
                sb.append("\\E)");

                if (myPrefixOnPastePattern == PrefixOnPastePatternType.CAMEL.getIntValue()) {
                    sb.append("(?=[A-Z])");
                }

                try {
                    myPrefixesOnPastePattern = Pattern.compile(myPrefixesOnPasteText);
                } catch (Throwable error) {
                    myPrefixOnPastePattern = PrefixOnPastePatternType.CAMEL.getIntValue();
                    throw error;
                }
            }
        }
        return myPrefixesOnPastePattern;
    }

    public NumberingOptions getLastNumberingOptions() { return myLastNumberingOptions.copy(); }

    public NumberingBaseOptions getNumberingBaseOptions(int numberingBase) {
        assert numberingBase >= NumberingBaseType.MIN_BASE && numberingBase <= NumberingBaseType.MAX_BASE;
        if (numberingBase == 0) return myLastNumberingBaseOptions_0.copy();
        if (numberingBase == 1) return myLastNumberingBaseOptions_1.copy();
        if (numberingBase == 2) return myLastNumberingBaseOptions_2.copy();
        if (numberingBase == 3) return myLastNumberingBaseOptions_3.copy();
        if (numberingBase == 4) return myLastNumberingBaseOptions_4.copy();
        if (numberingBase == 5) return myLastNumberingBaseOptions_5.copy();
        if (numberingBase == 6) return myLastNumberingBaseOptions_6.copy();
        if (numberingBase == 7) return myLastNumberingBaseOptions_7.copy();
        if (numberingBase == 8) return myLastNumberingBaseOptions_8.copy();
        if (numberingBase == 9) return myLastNumberingBaseOptions_9.copy();
        if (numberingBase == 10) return myLastNumberingBaseOptions_10.copy();
        if (numberingBase == 11) return myLastNumberingBaseOptions_11.copy();
        if (numberingBase == 12) return myLastNumberingBaseOptions_12.copy();
        if (numberingBase == 13) return myLastNumberingBaseOptions_13.copy();
        if (numberingBase == 14) return myLastNumberingBaseOptions_14.copy();
        if (numberingBase == 15) return myLastNumberingBaseOptions_15.copy();
        if (numberingBase == 16) return myLastNumberingBaseOptions_16.copy();
        if (numberingBase == 17) return myLastNumberingBaseOptions_17.copy();
        if (numberingBase == 18) return myLastNumberingBaseOptions_18.copy();
        if (numberingBase == 19) return myLastNumberingBaseOptions_19.copy();
        if (numberingBase == 20) return myLastNumberingBaseOptions_20.copy();
        if (numberingBase == 21) return myLastNumberingBaseOptions_21.copy();
        if (numberingBase == 22) return myLastNumberingBaseOptions_22.copy();
        if (numberingBase == 23) return myLastNumberingBaseOptions_23.copy();
        if (numberingBase == 24) return myLastNumberingBaseOptions_24.copy();
        if (numberingBase == 25) return myLastNumberingBaseOptions_25.copy();
        if (numberingBase == 26) return myLastNumberingBaseOptions_26.copy();
        if (numberingBase == 27) return myLastNumberingBaseOptions_27.copy();
        if (numberingBase == 28) return myLastNumberingBaseOptions_28.copy();
        if (numberingBase == 29) return myLastNumberingBaseOptions_29.copy();
        if (numberingBase == 30) return myLastNumberingBaseOptions_30.copy();
        if (numberingBase == 31) return myLastNumberingBaseOptions_31.copy();
        if (numberingBase == 32) return myLastNumberingBaseOptions_32.copy();
        if (numberingBase == 33) return myLastNumberingBaseOptions_33.copy();
        if (numberingBase == 34) return myLastNumberingBaseOptions_34.copy();
        if (numberingBase == 35) return myLastNumberingBaseOptions_35.copy();
        if (numberingBase == 36) return myLastNumberingBaseOptions_36.copy();
        return new NumberingBaseOptions();
    }

    public void setLastNumberingOptions(NumberingOptions lastNumberingOptions) {
        myLastNumberingOptions = lastNumberingOptions.copy();

        // save these base options with the actual base last used
        int numberingBase = myLastNumberingOptions.myNumberingBase;
        assert numberingBase >= NumberingBaseType.MIN_BASE && numberingBase <= NumberingBaseType.MAX_BASE;
        
        // copy out 
        NumberingBaseOptions numberingBaseOptions = new NumberingBaseOptions(myLastNumberingOptions);
        if (numberingBase == 0) myLastNumberingBaseOptions_0 = numberingBaseOptions;
        else if (numberingBase == 1) myLastNumberingBaseOptions_1 = numberingBaseOptions;
        else if (numberingBase == 2) myLastNumberingBaseOptions_2 = numberingBaseOptions;
        else if (numberingBase == 3) myLastNumberingBaseOptions_3 = numberingBaseOptions;
        else if (numberingBase == 4) myLastNumberingBaseOptions_4 = numberingBaseOptions;
        else if (numberingBase == 5) myLastNumberingBaseOptions_5 = numberingBaseOptions;
        else if (numberingBase == 6) myLastNumberingBaseOptions_6 = numberingBaseOptions;
        else if (numberingBase == 7) myLastNumberingBaseOptions_7 = numberingBaseOptions;
        else if (numberingBase == 8) myLastNumberingBaseOptions_8 = numberingBaseOptions;
        else if (numberingBase == 9) myLastNumberingBaseOptions_9 = numberingBaseOptions;
        else if (numberingBase == 10) myLastNumberingBaseOptions_10 = numberingBaseOptions;
        else if (numberingBase == 11) myLastNumberingBaseOptions_11 = numberingBaseOptions;
        else if (numberingBase == 12) myLastNumberingBaseOptions_12 = numberingBaseOptions;
        else if (numberingBase == 13) myLastNumberingBaseOptions_13 = numberingBaseOptions;
        else if (numberingBase == 14) myLastNumberingBaseOptions_14 = numberingBaseOptions;
        else if (numberingBase == 15) myLastNumberingBaseOptions_15 = numberingBaseOptions;
        else if (numberingBase == 16) myLastNumberingBaseOptions_16 = numberingBaseOptions;
        else if (numberingBase == 17) myLastNumberingBaseOptions_17 = numberingBaseOptions;
        else if (numberingBase == 18) myLastNumberingBaseOptions_18 = numberingBaseOptions;
        else if (numberingBase == 19) myLastNumberingBaseOptions_19 = numberingBaseOptions;
        else if (numberingBase == 20) myLastNumberingBaseOptions_20 = numberingBaseOptions;
        else if (numberingBase == 21) myLastNumberingBaseOptions_21 = numberingBaseOptions;
        else if (numberingBase == 22) myLastNumberingBaseOptions_22 = numberingBaseOptions;
        else if (numberingBase == 23) myLastNumberingBaseOptions_23 = numberingBaseOptions;
        else if (numberingBase == 24) myLastNumberingBaseOptions_24 = numberingBaseOptions;
        else if (numberingBase == 25) myLastNumberingBaseOptions_25 = numberingBaseOptions;
        else if (numberingBase == 26) myLastNumberingBaseOptions_26 = numberingBaseOptions;
        else if (numberingBase == 27) myLastNumberingBaseOptions_27 = numberingBaseOptions;
        else if (numberingBase == 28) myLastNumberingBaseOptions_28 = numberingBaseOptions;
        else if (numberingBase == 29) myLastNumberingBaseOptions_29 = numberingBaseOptions;
        else if (numberingBase == 30) myLastNumberingBaseOptions_30 = numberingBaseOptions;
        else if (numberingBase == 31) myLastNumberingBaseOptions_31 = numberingBaseOptions;
        else if (numberingBase == 32) myLastNumberingBaseOptions_32 = numberingBaseOptions;
        else if (numberingBase == 33) myLastNumberingBaseOptions_33 = numberingBaseOptions;
        else if (numberingBase == 34) myLastNumberingBaseOptions_34 = numberingBaseOptions;
        else if (numberingBase == 35) myLastNumberingBaseOptions_35 = numberingBaseOptions;
        else if (numberingBase == 36) myLastNumberingBaseOptions_36 = numberingBaseOptions;
    }

    // @formatter:off
    public NumberingBaseOptions getLastNumberingBaseOptions_0() { return myLastNumberingBaseOptions_0.copy(); } 
    public void setLastNumberingBaseOptions_0(NumberingBaseOptions lastNumberingBaseOptions_0) { myLastNumberingBaseOptions_0 = lastNumberingBaseOptions_0.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_1() { return myLastNumberingBaseOptions_1.copy(); } 
    public void setLastNumberingBaseOptions_1(NumberingBaseOptions lastNumberingBaseOptions_1) { myLastNumberingBaseOptions_1 = lastNumberingBaseOptions_1.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_2() { return myLastNumberingBaseOptions_2.copy(); } 
    public void setLastNumberingBaseOptions_2(NumberingBaseOptions lastNumberingBaseOptions_2) { myLastNumberingBaseOptions_2 = lastNumberingBaseOptions_2.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_3() { return myLastNumberingBaseOptions_3.copy(); } 
    public void setLastNumberingBaseOptions_3(NumberingBaseOptions lastNumberingBaseOptions_3) { myLastNumberingBaseOptions_3 = lastNumberingBaseOptions_3.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_4() { return myLastNumberingBaseOptions_4.copy(); } 
    public void setLastNumberingBaseOptions_4(NumberingBaseOptions lastNumberingBaseOptions_4) { myLastNumberingBaseOptions_4 = lastNumberingBaseOptions_4.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_5() { return myLastNumberingBaseOptions_5.copy(); } 
    public void setLastNumberingBaseOptions_5(NumberingBaseOptions lastNumberingBaseOptions_5) { myLastNumberingBaseOptions_5 = lastNumberingBaseOptions_5.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_6() { return myLastNumberingBaseOptions_6.copy(); } 
    public void setLastNumberingBaseOptions_6(NumberingBaseOptions lastNumberingBaseOptions_6) { myLastNumberingBaseOptions_6 = lastNumberingBaseOptions_6.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_7() { return myLastNumberingBaseOptions_7.copy(); } 
    public void setLastNumberingBaseOptions_7(NumberingBaseOptions lastNumberingBaseOptions_7) { myLastNumberingBaseOptions_7 = lastNumberingBaseOptions_7.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_8() { return myLastNumberingBaseOptions_8.copy(); } 
    public void setLastNumberingBaseOptions_8(NumberingBaseOptions lastNumberingBaseOptions_8) { myLastNumberingBaseOptions_8 = lastNumberingBaseOptions_8.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_9() { return myLastNumberingBaseOptions_9.copy(); } 
    public void setLastNumberingBaseOptions_9(NumberingBaseOptions lastNumberingBaseOptions_9) { myLastNumberingBaseOptions_9 = lastNumberingBaseOptions_9.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_10() { return myLastNumberingBaseOptions_10.copy(); } 
    public void setLastNumberingBaseOptions_10(NumberingBaseOptions lastNumberingBaseOptions_10) { myLastNumberingBaseOptions_10 = lastNumberingBaseOptions_10.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_11() { return myLastNumberingBaseOptions_11.copy(); } 
    public void setLastNumberingBaseOptions_11(NumberingBaseOptions lastNumberingBaseOptions_11) { myLastNumberingBaseOptions_11 = lastNumberingBaseOptions_11.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_12() { return myLastNumberingBaseOptions_12.copy(); } 
    public void setLastNumberingBaseOptions_12(NumberingBaseOptions lastNumberingBaseOptions_12) { myLastNumberingBaseOptions_12 = lastNumberingBaseOptions_12.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_13() { return myLastNumberingBaseOptions_13.copy(); } 
    public void setLastNumberingBaseOptions_13(NumberingBaseOptions lastNumberingBaseOptions_13) { myLastNumberingBaseOptions_13 = lastNumberingBaseOptions_13.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_14() { return myLastNumberingBaseOptions_14.copy(); } 
    public void setLastNumberingBaseOptions_14(NumberingBaseOptions lastNumberingBaseOptions_14) { myLastNumberingBaseOptions_14 = lastNumberingBaseOptions_14.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_15() { return myLastNumberingBaseOptions_15.copy(); } 
    public void setLastNumberingBaseOptions_15(NumberingBaseOptions lastNumberingBaseOptions_15) { myLastNumberingBaseOptions_15 = lastNumberingBaseOptions_15.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_16() { return myLastNumberingBaseOptions_16.copy(); } 
    public void setLastNumberingBaseOptions_16(NumberingBaseOptions lastNumberingBaseOptions_16) { myLastNumberingBaseOptions_16 = lastNumberingBaseOptions_16.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_17() { return myLastNumberingBaseOptions_17.copy(); } 
    public void setLastNumberingBaseOptions_17(NumberingBaseOptions lastNumberingBaseOptions_17) { myLastNumberingBaseOptions_17 = lastNumberingBaseOptions_17.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_18() { return myLastNumberingBaseOptions_18.copy(); } 
    public void setLastNumberingBaseOptions_18(NumberingBaseOptions lastNumberingBaseOptions_18) { myLastNumberingBaseOptions_18 = lastNumberingBaseOptions_18.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_19() { return myLastNumberingBaseOptions_19.copy(); } 
    public void setLastNumberingBaseOptions_19(NumberingBaseOptions lastNumberingBaseOptions_19) { myLastNumberingBaseOptions_19 = lastNumberingBaseOptions_19.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_20() { return myLastNumberingBaseOptions_20.copy(); } 
    public void setLastNumberingBaseOptions_20(NumberingBaseOptions lastNumberingBaseOptions_20) { myLastNumberingBaseOptions_20 = lastNumberingBaseOptions_20.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_21() { return myLastNumberingBaseOptions_21.copy(); } 
    public void setLastNumberingBaseOptions_21(NumberingBaseOptions lastNumberingBaseOptions_21) { myLastNumberingBaseOptions_21 = lastNumberingBaseOptions_21.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_22() { return myLastNumberingBaseOptions_22.copy(); } 
    public void setLastNumberingBaseOptions_22(NumberingBaseOptions lastNumberingBaseOptions_22) { myLastNumberingBaseOptions_22 = lastNumberingBaseOptions_22.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_23() { return myLastNumberingBaseOptions_23.copy(); } 
    public void setLastNumberingBaseOptions_23(NumberingBaseOptions lastNumberingBaseOptions_23) { myLastNumberingBaseOptions_23 = lastNumberingBaseOptions_23.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_24() { return myLastNumberingBaseOptions_24.copy(); } 
    public void setLastNumberingBaseOptions_24(NumberingBaseOptions lastNumberingBaseOptions_24) { myLastNumberingBaseOptions_24 = lastNumberingBaseOptions_24.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_25() { return myLastNumberingBaseOptions_25.copy(); } 
    public void setLastNumberingBaseOptions_25(NumberingBaseOptions lastNumberingBaseOptions_25) { myLastNumberingBaseOptions_25 = lastNumberingBaseOptions_25.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_26() { return myLastNumberingBaseOptions_26.copy(); } 
    public void setLastNumberingBaseOptions_26(NumberingBaseOptions lastNumberingBaseOptions_26) { myLastNumberingBaseOptions_26 = lastNumberingBaseOptions_26.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_27() { return myLastNumberingBaseOptions_27.copy(); } 
    public void setLastNumberingBaseOptions_27(NumberingBaseOptions lastNumberingBaseOptions_27) { myLastNumberingBaseOptions_27 = lastNumberingBaseOptions_27.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_28() { return myLastNumberingBaseOptions_28.copy(); } 
    public void setLastNumberingBaseOptions_28(NumberingBaseOptions lastNumberingBaseOptions_28) { myLastNumberingBaseOptions_28 = lastNumberingBaseOptions_28.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_29() { return myLastNumberingBaseOptions_29.copy(); } 
    public void setLastNumberingBaseOptions_29(NumberingBaseOptions lastNumberingBaseOptions_29) { myLastNumberingBaseOptions_29 = lastNumberingBaseOptions_29.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_30() { return myLastNumberingBaseOptions_30.copy(); } 
    public void setLastNumberingBaseOptions_30(NumberingBaseOptions lastNumberingBaseOptions_30) { myLastNumberingBaseOptions_30 = lastNumberingBaseOptions_30.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_31() { return myLastNumberingBaseOptions_31.copy(); } 
    public void setLastNumberingBaseOptions_31(NumberingBaseOptions lastNumberingBaseOptions_31) { myLastNumberingBaseOptions_31 = lastNumberingBaseOptions_31.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_32() { return myLastNumberingBaseOptions_32.copy(); } 
    public void setLastNumberingBaseOptions_32(NumberingBaseOptions lastNumberingBaseOptions_32) { myLastNumberingBaseOptions_32 = lastNumberingBaseOptions_32.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_33() { return myLastNumberingBaseOptions_33.copy(); } 
    public void setLastNumberingBaseOptions_33(NumberingBaseOptions lastNumberingBaseOptions_33) { myLastNumberingBaseOptions_33 = lastNumberingBaseOptions_33.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_34() { return myLastNumberingBaseOptions_34.copy(); } 
    public void setLastNumberingBaseOptions_34(NumberingBaseOptions lastNumberingBaseOptions_34) { myLastNumberingBaseOptions_34 = lastNumberingBaseOptions_34.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_35() { return myLastNumberingBaseOptions_35.copy(); } 
    public void setLastNumberingBaseOptions_35(NumberingBaseOptions lastNumberingBaseOptions_35) { myLastNumberingBaseOptions_35 = lastNumberingBaseOptions_35.copy(); } 
    public NumberingBaseOptions getLastNumberingBaseOptions_36() { return myLastNumberingBaseOptions_36.copy(); } 
    public void setLastNumberingBaseOptions_36(NumberingBaseOptions lastNumberingBaseOptions_36) { myLastNumberingBaseOptions_36 = lastNumberingBaseOptions_36.copy(); } 
    // @formatter:on

    public boolean isMultiPasteDeleteRepeatedCaretData() { return myMultiPasteDeleteRepeatedCaretData; }
    
    public void setMultiPasteDeleteRepeatedCaretData(boolean multiPasteDeleteRepeatedCaretData) { myMultiPasteDeleteRepeatedCaretData = multiPasteDeleteRepeatedCaretData; }

    public boolean isSelectPastedMultiCaret() { return mySelectPastedMultiCaret; }

    public void setSelectPastedMultiCaret(boolean selectPastedMultiCaret) { mySelectPastedMultiCaret = selectPastedMultiCaret; }

    public boolean isMultiPastePreserveOriginal() { return myMultiPastePreserveOriginal; }

    public void setMultiPastePreserveOriginal(boolean multiPastePreserveOriginal) { myMultiPastePreserveOriginal = multiPastePreserveOriginal; }

    public boolean isMultiPasteShowEolInList() { return myMultiPasteShowEolInList; }

    public void setMultiPasteShowEolInList(final boolean multiPasteShowEolInList) { myMultiPasteShowEolInList = multiPasteShowEolInList; }

    public boolean isMultiPasteShowOptions() { return myMultiPasteShowOptions; }

    public void setMultiPasteShowOptions(final boolean multiPasteShowOptions) { myMultiPasteShowOptions = multiPasteShowOptions; }

    public boolean isMultiPasteShowEolInViewer() { return myMultiPasteShowEolInViewer; }

    public void setMultiPasteShowEolInViewer(final boolean multiPasteShowEolInViewer) { myMultiPasteShowEolInViewer = multiPasteShowEolInViewer; }

    @NotNull
    public String getRegexSampleText() { return myRegexSampleText; }

    public void setRegexSampleText(@NotNull final String regexSampleText) { myRegexSampleText = regexSampleText; }

    public boolean isAddPrefixOnPaste() { return myAddPrefixOnPaste; }

    public void setAddPrefixOnPaste(final boolean addPrefixOnPaste) { myAddPrefixOnPaste = addPrefixOnPaste; }

    public int getPrefixOnPastePattern() { return myPrefixOnPastePattern; }

    public void setPrefixOnPastePattern(final int prefixOnPastePattern) { myPrefixOnPastePattern = prefixOnPastePattern; }

    public int getLinePasteCaretAdjustment() { return myLinePasteCaretAdjustment; }

    public void setLinePasteCaretAdjustment(final int linePasteCaretAdjustment) { myLinePasteCaretAdjustment = linePasteCaretAdjustment; }

    public boolean isMultiPasteShowInstructions() { return myMultiPasteShowInstructions; }

    public void setMultiPasteShowInstructions(final boolean multiPasteShowInstructions) { myMultiPasteShowInstructions = multiPasteShowInstructions; }

    public int getSelectPastedMultiCaretPredicate() { return mySelectPastedMultiCaretPredicate; }

    public void setSelectPastedMultiCaretPredicate(final int selectPastedMultiCaret) { mySelectPastedMultiCaretPredicate = selectPastedMultiCaret; }

    public boolean isOverrideStandardPaste() { return myOverrideStandardPaste; }

    public void setOverrideStandardPaste(final boolean overrideStandardPaste) { myOverrideStandardPaste = overrideStandardPaste; }

    public boolean isPreserveScreamingSnakeCaseOnPaste() { return myPreserveScreamingSnakeCaseOnPaste; }

    public void setPreserveScreamingSnakeCaseOnPaste(boolean preserveScreamingSnakeCaseOnPaste) { myPreserveScreamingSnakeCaseOnPaste = preserveScreamingSnakeCaseOnPaste; }

    public boolean isPreserveSnakeCaseOnPaste() { return myPreserveSnakeCaseOnPaste; }

    public void setPreserveSnakeCaseOnPaste(boolean preserveSnakeCaseOnPaste) { myPreserveSnakeCaseOnPaste = preserveSnakeCaseOnPaste; }

    public boolean isRemovePrefixOnPaste() { return myRemovePrefixOnPaste; }

    public void setRemovePrefixOnPaste(boolean removePrefixOnPaste) { myRemovePrefixOnPaste = removePrefixOnPaste; }

    @NotNull
    public String getPrefixesOnPasteText() { return myPrefixesOnPasteText; }

    public void setPrefixesOnPasteText(@NotNull String prefixesOnPasteText) {
        myPrefixesOnPasteText = prefixesOnPasteText;
        myPrefixesOnPasteList = null;
        myPrefixesOnPastePattern = null;
    }

    public boolean isPreserveCamelCaseOnPaste() { return myPreserveCamelCaseOnPaste; }

    public void setPreserveCamelCaseOnPaste(boolean preserveCamelCaseOnPaste) { myPreserveCamelCaseOnPaste = preserveCamelCaseOnPaste; }

    public boolean isStartEndAsLineSelection() { return myStartEndAsLineSelection; }

    public void setStartEndAsLineSelection(boolean startEndAsLineSelection) { myStartEndAsLineSelection = startEndAsLineSelection; }

    public boolean isCopyLineOrLineSelection() { return myCopyLineOrLineSelection; }

    public void setCopyLineOrLineSelection(boolean copyLineOrLineSelection) { myCopyLineOrLineSelection = copyLineOrLineSelection; }

    public int getCaretOnMoveSelectionDown() { return myCaretOnMoveSelectionDown; }

    public void setCaretOnMoveSelectionDown(int caretOnMoveSelectionDown) { myCaretOnMoveSelectionDown = caretOnMoveSelectionDown; }

    public int getCaretOnMoveSelectionUp() { return myCaretOnMoveSelectionUp; }

    public void setCaretOnMoveSelectionUp(int caretOnMoveSelectionUp) { myCaretOnMoveSelectionUp = caretOnMoveSelectionUp; }

    public boolean isIndentUnindent() { return myIndentUnindent; }

    public void setIndentUnindent(boolean indentUnindent) { myIndentUnindent = indentUnindent; }

    public boolean isTypingDeletesLineSelection() { return myTypingDeletesLineSelection; }

    public void setTypingDeletesLineSelection(boolean typingDeletesLineSelection) { myTypingDeletesLineSelection = typingDeletesLineSelection; }

    public boolean isSelectionStartExtended() { return myIsSelectionStartExtended; }

    public void setSelectionStartExtended(boolean selectionStartExtended) { myIsSelectionStartExtended = selectionStartExtended; }

    public boolean isSelectionEndExtended() { return myIsSelectionEndExtended; }

    public void setSelectionEndExtended(boolean selectionEndExtended) { myIsSelectionEndExtended = selectionEndExtended; }

    public boolean isMouseCamelHumpsFollow() { return myMouseCamelHumpsFollow; }

    public void setMouseCamelHumpsFollow(boolean mouseCamelHumpsFollow) { myMouseCamelHumpsFollow = mouseCamelHumpsFollow; }

    public int getCustomizedNextWordBounds() { return myCustomizedNextWordBounds | START_OF_WORD | END_OF_WORD; }

    public void setCustomizedNextWordBounds(int customizedNextWordBounds) { myCustomizedNextWordBounds = customizedNextWordBounds | START_OF_WORD | END_OF_WORD; }

    public int getCustomizedPrevWordBounds() { return myCustomizedPrevWordBounds | START_OF_WORD | END_OF_WORD; }

    public void setCustomizedPrevWordBounds(int customizedPrevWordBounds) { myCustomizedPrevWordBounds = customizedPrevWordBounds | START_OF_WORD | END_OF_WORD; }

    public int getCustomizedNextWordStartBounds() { return myCustomizedNextWordStartBounds | START_OF_WORD; }

    public void setCustomizedNextWordStartBounds(int customizedNextWordStartBounds) { myCustomizedNextWordStartBounds = customizedNextWordStartBounds | START_OF_WORD; }

    public int getCustomizedPrevWordStartBounds() { return myCustomizedPrevWordStartBounds | START_OF_WORD; }

    public void setCustomizedPrevWordStartBounds(int customizedPrevWordStartBounds) { myCustomizedPrevWordStartBounds = customizedPrevWordStartBounds | START_OF_WORD; }

    public int getCustomizedNextWordEndBounds() { return myCustomizedNextWordEndBounds | END_OF_WORD; }

    public void setCustomizedNextWordEndBounds(int customizedNextWordEndBounds) { myCustomizedNextWordEndBounds = customizedNextWordEndBounds | END_OF_WORD; }

    public int getCustomizedPrevWordEndBounds() { return myCustomizedPrevWordEndBounds | END_OF_WORD; }

    public void setCustomizedPrevWordEndBounds(int customizedPrevWordEndBounds) { myCustomizedPrevWordEndBounds = customizedPrevWordEndBounds | END_OF_WORD; }

    public int getDuplicateAtStartOrEndPredicate() { return myDuplicateAtStartOrEndPredicate; }

    public void setDuplicateAtStartOrEndPredicate(int duplicateAtStartOrEndPredicate) { myDuplicateAtStartOrEndPredicate = duplicateAtStartOrEndPredicate; }

    public boolean isDuplicateAtStartOrEnd() { return myDuplicateAtStartOrEnd; }

    public void setDuplicateAtStartOrEnd(boolean duplicateAtStartOrEnd) { myDuplicateAtStartOrEnd = duplicateAtStartOrEnd; }

    public boolean isWeSetVirtualSpace() { return myWeSetVirtualSpace; }

    public void setWeSetVirtualSpace(boolean weSetVirtualSpace) { myWeSetVirtualSpace = weSetVirtualSpace; }

    public int getSelectPastedPredicate() { return mySelectPastedPredicate; }

    public void setSelectPastedPredicate(int selectPastedPredicate) { mySelectPastedPredicate = selectPastedPredicate; }

    public boolean isUnselectToggleCase() { return myUnselectToggleCase; }

    public void setUnselectToggleCase(boolean unselectToggleCase) { myUnselectToggleCase = unselectToggleCase; }

    public int getMouseModifier() { return myMouseModifier; }

    public void setMouseModifier(int mouseModifier) { myMouseModifier = mouseModifier; }

    public boolean isUpDownSelection() { return myUpDownSelection; }

    public void setUpDownSelection(boolean upDownSelection) { myUpDownSelection = upDownSelection; }

    public boolean isMouseLineSelection() { return myMouseLineSelection; }

    public void setMouseLineSelection(boolean mouseLineSelection) { myMouseLineSelection = mouseLineSelection; }

    public int getAutoLineMode() { return myAutoLineMode; }

    public void setAutoLineMode(int autoLineMode) { myAutoLineMode = autoLineMode; }

    public boolean isDeleteOperations() { return myDeleteOperations; }

    public void setDeleteOperations(boolean deleteOperations) { myDeleteOperations = deleteOperations; }

    public boolean isUpDownMovement() { return myUpDownMovement; }

    public void setUpDownMovement(boolean upDownMovement) { myUpDownMovement = upDownMovement; }

    public boolean isLeftRightMovement() { return myLeftRightMovement; }

    public void setLeftRightMovement(boolean leftRightMovement) { myLeftRightMovement = leftRightMovement; }

    public int getAutoIndentDelay() { return myAutoIndentDelay; }

    public void setAutoIndentDelay(int autoIndentDelay) { myAutoIndentDelay = autoIndentDelay; }

    public boolean isAutoIndent() { return myAutoIndent; }

    public void setAutoIndent(boolean autoIndent) { myAutoIndent = autoIndent; }

    public boolean isSelectPasted() { return mySelectPasted; }

    public void setSelectPasted(boolean selectPasted) { mySelectPasted = selectPasted; }

    @Nullable
    public ApplicationSettings getState() {
        return this;
    }

    public void loadState(ApplicationSettings applicationSettings) {
        XmlSerializerUtil.copyBean(applicationSettings, this);
        notifySettingsChanged();
    }

    public void initComponent() {
    }

    public void disposeComponent() {
    }

    public void notifySettingsChanged() {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(ApplicationSettingsListener.TOPIC).onSettingsChange(this);
    }

    @NotNull
    public String getComponentName() {
        return this.getClass().getName();
    }

    public static ApplicationSettings getInstance() {
        return ApplicationManager.getApplication().getComponent(ApplicationSettings.class);
    }
}
