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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import com.intellij.util.xmlb.annotations.XCollection;
import com.vladsch.MissingInActions.util.CaseFormatPreserver;
import com.vladsch.flexmark.util.html.ui.Color;
import com.vladsch.flexmark.util.misc.Pair;
import com.vladsch.plugin.util.ui.ColorIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.vladsch.MissingInActions.util.EditHelpers.*;

@State(
        name = "MissingInActions",
        storages = @Storage("MissingInActions.xml")
)
@SuppressWarnings("WeakerAccess")
public class ApplicationSettings extends BatchSearchReplaceSettings implements PersistentStateComponent<ApplicationSettings> {
    // @formatter:off
    private boolean         myAddPrefixOnPaste = false;
    private boolean         myAutoIndent = false;
    private boolean         myCopyLineOrLineSelection = false;
    private boolean         myDeleteOperations = false;
    private boolean         myDuplicateAtStartOrEnd = false;
    private boolean         myIndentUnindent = false;
    private boolean         myIsSelectionEndExtended = false;
    private boolean         myIsSelectionStartExtended = false;
    private boolean         myLeftRightMovement = false;
    private boolean         myMouseCamelHumpsFollow = false;
    private boolean         myMouseLineSelection = false;
    private boolean         myMultiPasteShowEolInViewer = false;
    private boolean         myMultiPasteShowEolInList = true;
    private boolean         myMultiPasteShowInstructions = true;
    private boolean         myMultiPasteShowOptions = true;
    private boolean         myMultiPastePreserveOriginal = true;
    private boolean         myMultiPasteDeleteRepeatedCaretData = true;
    private boolean         myMultiPasteDeleteReplacedCaretData = true;
    private boolean         myReplaceMacroVariables = false;
    private boolean         myOverrideStandardPaste = false;
    private boolean         myPreserveCamelCaseOnPaste = false;
    private boolean         myPreserveScreamingSnakeCaseOnPaste = false;
    private boolean         myPreserveSnakeCaseOnPaste = false;
    private boolean         myPreserveDashCaseOnPaste = false;
    private boolean         myPreserveDotCaseOnPaste = false;
    private boolean         myPreserveSlashCaseOnPaste = false;
    private boolean         myRemovePrefixOnPaste = false;
    private boolean         mySelectPasted = false;
    private boolean         mySelectPastedMultiCaret = false;
    private boolean         myStartEndAsLineSelection = false;
    private boolean         myTypingDeletesLineSelection = false;
    private boolean         myUnselectToggleCase = false;
    private boolean         myUpDownMovement = false;
    private boolean         myUpDownSelection = false;
    private boolean         myWeSetVirtualSpace = true;
    private int             myAutoIndentDelay = 300;
    private int             myAutoLineMode = AutoLineModeType.ADAPTER.getDefault().getIntValue();
    private int             myCaretOnMoveSelectionDown = CaretAdjustmentType.ADAPTER.getDefault().intValue;
    private int             myCaretOnMoveSelectionUp = CaretAdjustmentType.ADAPTER.getDefault().intValue;
    private int             myDuplicateAtStartOrEndPredicate = SelectionPredicateType.WHEN_HAS_1_PLUS_LINES.intValue;
    private int             myLinePasteCaretAdjustment = LinePasteCaretAdjustmentType.NONE.intValue;
    private int             myMouseModifier = MouseModifierType.ADAPTER.getDefault().getIntValue();
    private int             myPrefixOnPastePattern = PrefixOnPastePatternType.ADAPTER.getDefault().intValue;
    private int             mySelectPastedMultiCaretPredicate = SelectionPredicateType.WHEN_HAS_1_PLUS_LINES.intValue;
    private int             mySelectPastedPredicate = SelectionPredicateType.WHEN_HAS_2_PLUS_LINES.intValue;
    private int             myPrimaryCaretThickness = CaretThicknessType.NORMAL.intValue;
    private int             myPrimaryCaretColor = JBColor.BLACK.getRGB();
    private int             myPrimaryCaretDarkColor = 0xCACACA;
    private boolean         myPrimaryCaretColorEnabled = false;
    private int             mySearchStartCaretThickness = CaretThicknessType.THIN.intValue;
    private int             mySearchStartCaretColor = 0xD40047;
    private int             mySearchStartCaretDarkColor = 0xFF4D4D;
    private boolean         mySearchStartCaretColorEnabled = true;
    private int             mySearchFoundCaretThickness = CaretThicknessType.HEAVY.intValue;
    private int             mySearchFoundCaretColor = 0x0306CF;
    private int             mySearchFoundCaretDarkColor = 0x59F3FF;
    private boolean         mySearchFoundCaretColorEnabled = true;
    private int             mySearchStartMatchedCaretThickness = CaretThicknessType.HEAVY.intValue;
    private int             mySearchStartMatchedCaretColor = 0xD40047;
    private int             mySearchStartMatchedCaretDarkColor = 0xFF4D4D;
    private boolean         mySearchStartMatchedCaretColorEnabled = false;
    private boolean         mySearchCancelOnEscape = true;
    private boolean         myPreservePrimaryCaretOnEscape = true;
    private boolean         mySpawnSmartPrefixSearch = true;
    private boolean         mySpawnMatchBoundarySearch = true;
    private int             mySelectionStashLimit = 5;
    private int             myRecalledSelectionColor = 0xFCBEFF;
    private int             myRecalledSelectionDarkColor = 0x844E8A;
    private boolean         myRecalledSelectionColorEnabled = false;
    private boolean         myIsolatedForegroundColorEnabled = false;
    private int             myIsolatedForegroundColor = 0x555555;
    private int             myIsolatedForegroundDarkColor = 0x999999;
    private boolean         myIsolatedBackgroundColorEnabled = true;
    private int             myIsolatedBackgroundColor = 0xF0F0F0;
    private int             myIsolatedBackgroundDarkColor = 0x4D4D4D;
    private @NotNull String myRegexSampleText = "myCamelCaseMember|ourCamelCaseMember|isCamelCaseMember()|getCamelCaseMember()|setCamelCaseMember()";
    private @NotNull String myPrefixesOnPasteText = "my|our|is|get|set";
    private int             myGradientHueMin = ColorIterable.GRADIENT_HUE_MIN;
    private int             myGradientHueMax = ColorIterable.GRADIENT_HUE_MAX;
    private int             myGradientHueSteps = ColorIterable.GRADIENT_HUE_STEPS;
    private int             myGradientSaturationMin = ColorIterable.GRADIENT_SATURATION_MIN;
    private int             myGradientSaturationMax = ColorIterable.GRADIENT_SATURATION_MAX;
    private int             myGradientSaturationSteps = ColorIterable.GRADIENT_SATURATION_STEPS;
    private int             myGradientBrightnessMin = ColorIterable.GRADIENT_BRIGHTNESS_MIN;
    private int             myGradientBrightnessMax = ColorIterable.GRADIENT_BRIGHTNESS_MAX;
    private int             myGradientBrightnessSteps = ColorIterable.GRADIENT_BRIGHTNESS_STEPS;
    private int             myDarkGradientHueMin = ColorIterable.DARK_GRADIENT_HUE_MIN;
    private int             myDarkGradientHueMax = ColorIterable.DARK_GRADIENT_HUE_MAX;
    private int             myDarkGradientHueSteps = ColorIterable.DARK_GRADIENT_HUE_STEPS;
    private int             myDarkGradientSaturationMin = ColorIterable.DARK_GRADIENT_SATURATION_MIN;
    private int             myDarkGradientSaturationMax = ColorIterable.DARK_GRADIENT_SATURATION_MAX;
    private int             myDarkGradientSaturationSteps = ColorIterable.DARK_GRADIENT_SATURATION_STEPS;
    private int             myDarkGradientBrightnessMin = ColorIterable.DARK_GRADIENT_BRIGHTNESS_MIN;
    private int             myDarkGradientBrightnessMax = ColorIterable.DARK_GRADIENT_BRIGHTNESS_MAX;
    private int             myDarkGradientBrightnessSteps = ColorIterable.DARK_GRADIENT_BRIGHTNESS_STEPS;
    private boolean         myHideDisabledButtons = false;
    private boolean         myOnPastePreserve = true;
    private boolean         myReplaceUserDefinedMacro = false;
    private boolean         myRegexUserDefinedMacro = false;
    private boolean         myUserDefinedMacroClipContent = false;
    private @NotNull String myUserDefinedMacroSearch = "";
    private @NotNull String myUserDefinedMacroReplace = "";
    private @NotNull String mySpliceDelimiterText = ", ";
    private @NotNull String myOpenQuoteText = "\"";
    private @NotNull String myClosedQuoteText = "\"";
    private boolean myUserDefinedMacroSmartReplace = true;
    private boolean myBatchHighlightSearchLines = false;
    private boolean myBatchHighlightReplaceLines = false; // highlight search strings using corresponding colors from search strings
    private boolean myBatchTandemEdit = false;
    private boolean myRegisterCaretStateTransferable = true;
    private boolean myShowMacroResultPreview = false;
    private boolean myHighlightWordsCaseSensitive = true;
    private boolean myHighlightWordsMatchBoundary = true;
    private boolean myHighlightProjectViewNodes = false;
    // @formatter:on

    // customizable delete/backspace
    @NotNull String myDeleteSpacesRegEx = "(\\s+)";
    @NotNull String myBackspaceSpacesRegEx = "(\\s+)";
    @NotNull String myDeleteAlternatingRegEx = "(\\s+|[\\w]+|[^\\w\\s]+)";
    @NotNull String myBackspaceAlternatingRegEx = "(\\s+|[\\w]+|[^\\w\\s]+)";
    @NotNull String myDeleteWordExcludingSpaceRegEx = "(\\s[^\\s]+|\\s{2,}|[^\\s]+)";
    @NotNull String myBackspaceWordExcludingSpaceRegEx = "([^\\s]+\\s|\\s{2,}|[^\\s]+)";
    @NotNull String myDeleteWordRegEx = "((?<=\\s|^|\\W)\\s*(?:[\\w]+|[^\\w]+)\\s*|\\s*(?:[\\w]+|[^\\w]+)(?:\\s+(?=\\s|$|\\W)|))";
    @NotNull String myBackspaceWordRegEx = "((?:[\\w]+|[^\\w]+)\\s*|(?<=\\s|^|\\W)(?:[\\w]+|[^\\w]+)\\s*|\\s*(?:[\\w]+|[^\\w]+)\\s*(?=\\s|$|\\W))";
    boolean myDeleteLineBound = false;
    boolean myDeleteMultiCaretLineBound = true;
    boolean myBackspaceLineBound = false;
    boolean myBackspaceMultiCaretLineBound = true;
    boolean myOnlyLatestBlankClipboard = false;
    boolean mySpawnNumericSearch = false;       // match all numeric sequences as opposed to specific string
    boolean mySpawnNumericHexSearch = false;    // include hex digits
    boolean myDisableParameterInfo = false;     // disable parameter hints when multi-caret mode
    boolean myShowGenerateException = false;    // show generate exception action to test handling of recovery

    @XCollection(elementName = "highlightWords") public ArrayList<String> myHighlightWords = new ArrayList<>();
    @XCollection(elementName = "highlightFlags") public ArrayList<Integer> myHighlightFlags = new ArrayList<>();
    @XCollection(elementName = "highlightIndices") public ArrayList<Integer> myHighlightIndices = new ArrayList<>();

    @Transient
    public void setHighlightState(@Nullable Map<String, Pair<Integer, Integer>> state) {
        if (state == null) {
            myHighlightWords = new ArrayList<>();
            myHighlightFlags = new ArrayList<>();
            myHighlightIndices = new ArrayList<>();
        } else {
            ArrayList<String> highlightWords = new ArrayList<>(state.size());
            ArrayList<Integer> highlightFlags = new ArrayList<>(state.size());
            ArrayList<Integer> highlightIndices = new ArrayList<>(state.size());

            int i = 0;
            for (String key : state.keySet()) {
                Pair<Integer, Integer> pair = state.get(key);
                int flags = pair.getFirst();
                int originalIndex = pair.getSecond();
                highlightWords.add(key);
                highlightFlags.add(flags);
                highlightIndices.add(originalIndex);
                i++;
            }
            myHighlightWords = highlightWords;
            myHighlightFlags = highlightFlags;
            myHighlightIndices = highlightIndices;
        }
    }

    @Transient
    public @Nullable
    Map<String, Pair<Integer, Integer>> getHighlightState() {
        if (myHighlightWords.size() == 0 || myHighlightFlags.size() == 0 || myHighlightIndices.size() == 0
                || myHighlightFlags.size() != myHighlightWords.size() || myHighlightIndices.size() != myHighlightWords.size()) {
            return null;
        } else {
            HashMap<String, Pair<Integer, Integer>> state = new HashMap<>(myHighlightWords.size());
            int i = 0;
            for (String key : myHighlightWords) {
                int flags = myHighlightFlags.get(i);
                int originalIndex = myHighlightIndices.get(i);
                Pair<Integer, Integer> pair = Pair.of(flags, originalIndex);
                state.put(key, pair);
                i++;
            }
            return state;
        }
    }

    @NotNull String[] myDeleteBackspaceTests = new String[] {
            "      sample        ",
            "Sample Text with     =    random([]).test ;     ",
            "Sam|ple Text with     =    random([]).test ;     ",
            "Sample Text with     =    random([]).te|st ;     ",
            "(|Sample Text with     =    random([]).te|st ;     ",
            "Sample Text with     =    random([]).te|st|;",
    };

    @NotNull String[] myDeleteBackspaceTestCaretMarkers = new String[] {
            "|",
            "|",
            "|",
            "|",
            "|",
            "|",
    };

    public int getPreserveOnPasteSeparators() {
        return CaseFormatPreserver.separators(
                myPreserveCamelCaseOnPaste
                , myPreserveSnakeCaseOnPaste
                , myPreserveScreamingSnakeCaseOnPaste
                , myPreserveDashCaseOnPaste
                , myPreserveDotCaseOnPaste
                , myPreserveSlashCaseOnPaste
        );
    }

    public boolean isShowMacroResultPreview() { return myShowMacroResultPreview; }

    public void setShowMacroResultPreview(final boolean showMacroResultPreview) { myShowMacroResultPreview = showMacroResultPreview; }

    private NumberingOptions myLastNumberingOptions = new NumberingOptions();
    //    private NumberingBaseOptions myLastNumberingBaseOptions_0 = new NumberingBaseOptions();
//    private NumberingBaseOptions myLastNumberingBaseOptions_1 = new NumberingBaseOptions();
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
    public PrefixOnPastePatternType         getRemovePrefixOnPastePatternType()             { return PrefixOnPastePatternType.ADAPTER.get(myPrefixOnPastePattern); }
    public CaretAdjustmentType              getCaretOnMoveSelectionDownType()               { return CaretAdjustmentType.ADAPTER.get(myCaretOnMoveSelectionDown); }
    public CaretAdjustmentType              getCaretOnMoveSelectionUpType()                 { return CaretAdjustmentType.ADAPTER.get(myCaretOnMoveSelectionUp); }
    public SelectionPredicateType           getDuplicateAtStartOrEndPredicateType()         { return SelectionPredicateType.ADAPTER.get(myDuplicateAtStartOrEndPredicate); }
    public LinePasteCaretAdjustmentType     getLinePasteCaretAdjustmentType()               { return LinePasteCaretAdjustmentType.ADAPTER.get(myLinePasteCaretAdjustment); }
    public SelectionPredicateType           getSelectPastedPredicateType()                  { return SelectionPredicateType.ADAPTER.get(mySelectPastedPredicate); }
    public SelectionPredicateType           getSelectPastedMultiCaretPredicateType()        { return SelectionPredicateType.ADAPTER.get(mySelectPastedMultiCaretPredicate); }
    public CaretThicknessType               getPrimaryCaretThicknessType()                  { return CaretThicknessType.ADAPTER.get(myPrimaryCaretThickness); }
    public CaretThicknessType               getSearchStartCaretThicknessType()              { return CaretThicknessType.ADAPTER.get(mySearchStartCaretThickness); }
    public CaretThicknessType               getSearchStartFoundCaretThicknessType()         { return CaretThicknessType.ADAPTER.get(mySearchStartMatchedCaretThickness); }
    public CaretThicknessType               getSearchFoundCaretThicknessType()              { return CaretThicknessType.ADAPTER.get(mySearchFoundCaretThickness); }
    // @formatter:on

    // customized word flags
    @SuppressWarnings({ "ConstantConditionalExpression" }) final private static int CUSTOMIZED_DEFAULTS = (true ? START_OF_LINE : 0)
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
                // Can build it from the list
                StringBuilder sb = new StringBuilder();
                sb.append("(?:\\Q");
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
                    myPrefixesOnPastePattern = Pattern.compile(sb.toString());
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
        switch (numberingBase) {
// @formatter:off
            case 2: return myLastNumberingBaseOptions_2.copy();
            case 3: return myLastNumberingBaseOptions_3.copy();
            case 4: return myLastNumberingBaseOptions_4.copy();
            case 5: return myLastNumberingBaseOptions_5.copy();
            case 6: return myLastNumberingBaseOptions_6.copy();
            case 7: return myLastNumberingBaseOptions_7.copy();
            case 8: return myLastNumberingBaseOptions_8.copy();
            case 9: return myLastNumberingBaseOptions_9.copy();
            case 10: return myLastNumberingBaseOptions_10.copy();
            case 11: return myLastNumberingBaseOptions_11.copy();
            case 12: return myLastNumberingBaseOptions_12.copy();
            case 13: return myLastNumberingBaseOptions_13.copy();
            case 14: return myLastNumberingBaseOptions_14.copy();
            case 15: return myLastNumberingBaseOptions_15.copy();
            case 16: return myLastNumberingBaseOptions_16.copy();
            case 17: return myLastNumberingBaseOptions_17.copy();
            case 18: return myLastNumberingBaseOptions_18.copy();
            case 19: return myLastNumberingBaseOptions_19.copy();
            case 20: return myLastNumberingBaseOptions_20.copy();
            case 21: return myLastNumberingBaseOptions_21.copy();
            case 22: return myLastNumberingBaseOptions_22.copy();
            case 23: return myLastNumberingBaseOptions_23.copy();
            case 24: return myLastNumberingBaseOptions_24.copy();
            case 25: return myLastNumberingBaseOptions_25.copy();
            case 26: return myLastNumberingBaseOptions_26.copy();
            case 27: return myLastNumberingBaseOptions_27.copy();
            case 28: return myLastNumberingBaseOptions_28.copy();
            case 29: return myLastNumberingBaseOptions_29.copy();
            case 30: return myLastNumberingBaseOptions_30.copy();
            case 31: return myLastNumberingBaseOptions_31.copy();
            case 32: return myLastNumberingBaseOptions_32.copy();
            case 33: return myLastNumberingBaseOptions_33.copy();
            case 34: return myLastNumberingBaseOptions_34.copy();
            case 35: return myLastNumberingBaseOptions_35.copy();
            case 36: return myLastNumberingBaseOptions_36.copy();
            default: return new NumberingBaseOptions();
// @formatter:on
        }
    }

    public void setLastNumberingOptions(NumberingOptions lastNumberingOptions) {
        myLastNumberingOptions = lastNumberingOptions.copy();

        // save these base options with the actual base last used
        int numberingBase = myLastNumberingOptions.getNumberingBase();
        assert numberingBase >= NumberingBaseType.MIN_BASE && numberingBase <= NumberingBaseType.MAX_BASE;

        // copy out
        NumberingBaseOptions numberingBaseOptions = new NumberingBaseOptions(myLastNumberingOptions);
        switch (numberingBase) {
// @formatter:off
            case 2: myLastNumberingBaseOptions_2 = numberingBaseOptions; break;
            case 3: myLastNumberingBaseOptions_3 = numberingBaseOptions; break;
            case 4: myLastNumberingBaseOptions_4 = numberingBaseOptions; break;
            case 5: myLastNumberingBaseOptions_5 = numberingBaseOptions; break;
            case 6: myLastNumberingBaseOptions_6 = numberingBaseOptions; break;
            case 7: myLastNumberingBaseOptions_7 = numberingBaseOptions; break;
            case 8: myLastNumberingBaseOptions_8 = numberingBaseOptions; break;
            case 9: myLastNumberingBaseOptions_9 = numberingBaseOptions; break;
            case 10: myLastNumberingBaseOptions_10 = numberingBaseOptions; break;
            case 11: myLastNumberingBaseOptions_11 = numberingBaseOptions; break;
            case 12: myLastNumberingBaseOptions_12 = numberingBaseOptions; break;
            case 13: myLastNumberingBaseOptions_13 = numberingBaseOptions; break;
            case 14: myLastNumberingBaseOptions_14 = numberingBaseOptions; break;
            case 15: myLastNumberingBaseOptions_15 = numberingBaseOptions; break;
            case 16: myLastNumberingBaseOptions_16 = numberingBaseOptions; break;
            case 17: myLastNumberingBaseOptions_17 = numberingBaseOptions; break;
            case 18: myLastNumberingBaseOptions_18 = numberingBaseOptions; break;
            case 19: myLastNumberingBaseOptions_19 = numberingBaseOptions; break;
            case 20: myLastNumberingBaseOptions_20 = numberingBaseOptions; break;
            case 21: myLastNumberingBaseOptions_21 = numberingBaseOptions; break;
            case 22: myLastNumberingBaseOptions_22 = numberingBaseOptions; break;
            case 23: myLastNumberingBaseOptions_23 = numberingBaseOptions; break;
            case 24: myLastNumberingBaseOptions_24 = numberingBaseOptions; break;
            case 25: myLastNumberingBaseOptions_25 = numberingBaseOptions; break;
            case 26: myLastNumberingBaseOptions_26 = numberingBaseOptions; break;
            case 27: myLastNumberingBaseOptions_27 = numberingBaseOptions; break;
            case 28: myLastNumberingBaseOptions_28 = numberingBaseOptions; break;
            case 29: myLastNumberingBaseOptions_29 = numberingBaseOptions; break;
            case 30: myLastNumberingBaseOptions_30 = numberingBaseOptions; break;
            case 31: myLastNumberingBaseOptions_31 = numberingBaseOptions; break;
            case 32: myLastNumberingBaseOptions_32 = numberingBaseOptions; break;
            case 33: myLastNumberingBaseOptions_33 = numberingBaseOptions; break;
            case 34: myLastNumberingBaseOptions_34 = numberingBaseOptions; break;
            case 35: myLastNumberingBaseOptions_35 = numberingBaseOptions; break;
            default: myLastNumberingBaseOptions_36 = numberingBaseOptions; break;
// @formatter:on
        }
    }

// @formatter:off

    public boolean isHighlightProjectViewNodes() { return myHighlightProjectViewNodes;}
    public void setHighlightProjectViewNodes(boolean highlightProjectViewNodes) { myHighlightProjectViewNodes = highlightProjectViewNodes;}
    public boolean isHighlightWordsCaseSensitive() {return myHighlightWordsCaseSensitive;}
    public void setHighlightWordsCaseSensitive(boolean highlightWordsCaseSensitive) {myHighlightWordsCaseSensitive = highlightWordsCaseSensitive;}
    public boolean isHighlightWordsMatchBoundary() {return myHighlightWordsMatchBoundary;}
    public void setHighlightWordsMatchBoundary(boolean highlightWordsMatchBoundary) {myHighlightWordsMatchBoundary = highlightWordsMatchBoundary;}
    public boolean isRegisterCaretStateTransferable() {return myRegisterCaretStateTransferable;}
    public void setRegisterCaretStateTransferable(final boolean registerCaretStateTransferable) {myRegisterCaretStateTransferable = registerCaretStateTransferable;}
    public boolean isBatchTandemEdit() {return myBatchTandemEdit;}
    public void setBatchTandemEdit(final boolean batchTandemEdit) {myBatchTandemEdit = batchTandemEdit;}
    public boolean isOnPastePreserve() {return myOnPastePreserve;}
    public void setOnPastePreserve(final boolean onPastePreserve) {myOnPastePreserve = onPastePreserve;}
    public boolean isBatchHighlightSearchLines() {return myBatchHighlightSearchLines;}
    public void setBatchHighlightSearchLines(final boolean batchHighlightSearchLines) {myBatchHighlightSearchLines = batchHighlightSearchLines;}
    public boolean isBatchHighlightReplaceLines() {return myBatchHighlightReplaceLines;}
    public void setBatchHighlightReplaceLines(final boolean batchHighlightReplaceLines) {myBatchHighlightReplaceLines = batchHighlightReplaceLines;}
    @NotNull public String[] getDeleteBackspaceTestCaretMarkers() {return myDeleteBackspaceTestCaretMarkers;}
    public void setDeleteBackspaceTestCaretMarkers(@NotNull final String[] deleteBackspaceTestCaretMarkers) {myDeleteBackspaceTestCaretMarkers = deleteBackspaceTestCaretMarkers;}
    @NotNull public String[] getDeleteBackspaceTests() {return myDeleteBackspaceTests;}
    public void setDeleteBackspaceTests(@NotNull final String[] deleteBackspaceTests) {myDeleteBackspaceTests = deleteBackspaceTests;}
    @NotNull public String getDeleteWordExcludingSpaceRegEx() {return myDeleteWordExcludingSpaceRegEx;}
    public void setDeleteWordExcludingSpaceRegEx(@NotNull final String deleteWordExcludingSpaceRegEx) {myDeleteWordExcludingSpaceRegEx = deleteWordExcludingSpaceRegEx;}
    @NotNull public String getBackspaceWordExcludingSpaceRegEx() {return myBackspaceWordExcludingSpaceRegEx;}
    public void setBackspaceWordExcludingSpaceRegEx(@NotNull final String backspaceWordExcludingSpaceRegEx) {myBackspaceWordExcludingSpaceRegEx = backspaceWordExcludingSpaceRegEx;}
    @NotNull public String getDeleteSpacesRegEx() {return myDeleteSpacesRegEx;}
    public void setDeleteSpacesRegEx(@NotNull final String deleteSpacesRegEx) {myDeleteSpacesRegEx = deleteSpacesRegEx;}
    @NotNull public String getDeleteAlternatingRegEx() {return myDeleteAlternatingRegEx;}
    public void setDeleteAlternatingRegEx(@NotNull final String deleteAlternatingRegEx) {myDeleteAlternatingRegEx = deleteAlternatingRegEx;}
    @NotNull public String getDeleteWordRegEx() {return myDeleteWordRegEx;}
    public void setDeleteWordRegEx(@NotNull final String deleteWordRegEx) {myDeleteWordRegEx = deleteWordRegEx;}
    @NotNull public String getBackspaceSpacesRegEx() {return myBackspaceSpacesRegEx;}
    public void setBackspaceSpacesRegEx(@NotNull final String backspaceSpacesRegEx) {myBackspaceSpacesRegEx = backspaceSpacesRegEx;}
    @NotNull public String getBackspaceAlternatingRegEx() {return myBackspaceAlternatingRegEx;}
    public void setBackspaceAlternatingRegEx(@NotNull final String backspaceAlternatingRegEx) {myBackspaceAlternatingRegEx = backspaceAlternatingRegEx;}
    @NotNull public String getBackspaceWordRegEx() {return myBackspaceWordRegEx;}
    public void setBackspaceWordRegEx(@NotNull final String backspaceWordRegEx) {myBackspaceWordRegEx = backspaceWordRegEx;}
    public boolean isDeleteLineBound() {return myDeleteLineBound;}
    public void setDeleteLineBound(final boolean deleteLineBound) {myDeleteLineBound = deleteLineBound;}
    public boolean isDeleteMultiCaretLineBound() {return myDeleteMultiCaretLineBound;}
    public void setDeleteMultiCaretLineBound(final boolean deleteMultiCaretLineBound) {myDeleteMultiCaretLineBound = deleteMultiCaretLineBound;}
    public boolean isBackspaceLineBound() {return myBackspaceLineBound;}
    public void setBackspaceLineBound(final boolean backspaceLineBound) {myBackspaceLineBound = backspaceLineBound;}
    public boolean isBackspaceMultiCaretLineBound() {return myBackspaceMultiCaretLineBound;}
    public void setBackspaceMultiCaretLineBound(final boolean backspaceMultiCaretLineBound) {myBackspaceMultiCaretLineBound = backspaceMultiCaretLineBound;}
    public boolean isOnlyLatestBlankClipboard() {return myOnlyLatestBlankClipboard;}
    public void setOnlyLatestBlankClipboard(final boolean onlyLatestBlankClipboard) {myOnlyLatestBlankClipboard = onlyLatestBlankClipboard;}
    public boolean isSpawnNumericHexSearch() {return mySpawnNumericHexSearch;}
    public void setSpawnNumericHexSearch(final boolean spawnNumericHexSearch) {mySpawnNumericHexSearch = spawnNumericHexSearch;}
    public boolean isSpawnNumericSearch() {return mySpawnNumericSearch;}
    public void setSpawnNumericSearch(final boolean spawnNumericSearch) {mySpawnNumericSearch = spawnNumericSearch;}
    public boolean isDisableParameterInfo() {return myDisableParameterInfo;}
    public void setDisableParameterInfo(final boolean disableParameterInfo) {myDisableParameterInfo = disableParameterInfo;}
    public boolean isShowGenerateException() {return myShowGenerateException;}
    public void setShowGenerateException(final boolean showGenerateException) {myShowGenerateException = showGenerateException;}
    public boolean isHideDisabledButtons() {return myHideDisabledButtons;}
    public void setHideDisabledButtons(final boolean hideDisabledButtons) {myHideDisabledButtons = hideDisabledButtons;}
    public boolean isUserDefinedMacroSmartReplace() {return myUserDefinedMacroSmartReplace;}
    public void setUserDefinedMacroSmartReplace(final boolean userDefinedMacroSmartReplace) {myUserDefinedMacroSmartReplace = userDefinedMacroSmartReplace;}
    public boolean isReplaceUserDefinedMacro() {return myReplaceUserDefinedMacro;}
    public void setReplaceUserDefinedMacro(final boolean replaceUserDefinedMacro) {myReplaceUserDefinedMacro = replaceUserDefinedMacro;}
    public boolean isUserDefinedMacroClipContent() {return myUserDefinedMacroClipContent;}
    public void setUserDefinedMacroClipContent(final boolean userDefinedMacroClipContent) {myUserDefinedMacroClipContent = userDefinedMacroClipContent;}
    public boolean isRegexUserDefinedMacro() {return myRegexUserDefinedMacro;}
    public void setRegexUserDefinedMacro(final boolean regexUserDefinedMacro) {myRegexUserDefinedMacro = regexUserDefinedMacro;}
    @NotNull public String getUserDefinedMacroSearch() { return myUserDefinedMacroSearch; }
    public void setUserDefinedMacroSearch(@NotNull final String userDefinedMacroSearch) { myUserDefinedMacroSearch = userDefinedMacroSearch; }
    @NotNull public String getUserDefinedMacroReplace() { return myUserDefinedMacroReplace; }
    public void setUserDefinedMacroReplace(@NotNull final String userDefinedMacroReplace) { myUserDefinedMacroReplace = userDefinedMacroReplace; }
    @NotNull public String getSpliceDelimiterText() { return mySpliceDelimiterText; }
    public void setSpliceDelimiterText(@NotNull final String spliceDelimiterText) { mySpliceDelimiterText = spliceDelimiterText; }
    @NotNull public String getOpenQuoteText() { return myOpenQuoteText; }
    public void setOpenQuoteText(@NotNull final String openQuoteText) { myOpenQuoteText = openQuoteText; }
    @NotNull public String getClosedQuoteText() { return myClosedQuoteText; }
    public void setClosedQuoteText(@NotNull final String closedQuoteText) { myClosedQuoteText = closedQuoteText; }
    public void setHueMin(final int hueMin) {if (UIUtil.isUnderDarcula()) myDarkGradientHueMin = hueMin; else myGradientHueMin = hueMin;}
    public void setHueMax(final int hueMax) {if (UIUtil.isUnderDarcula()) myDarkGradientHueMax = hueMax; else myGradientHueMax = hueMax;}
    public void setHueSteps(final int hueSteps) {if (UIUtil.isUnderDarcula()) myDarkGradientHueSteps = hueSteps; else myGradientHueSteps = hueSteps;}
    public void setSaturationMin(final int saturationMin) {if (UIUtil.isUnderDarcula()) myDarkGradientSaturationMin = saturationMin; else myGradientSaturationMin = saturationMin;}
    public void setSaturationMax(final int saturationMax) {if (UIUtil.isUnderDarcula()) myDarkGradientSaturationMax = saturationMax; else myGradientSaturationMax = saturationMax;}
    public void setSaturationSteps(final int saturationSteps) {if (UIUtil.isUnderDarcula()) myDarkGradientSaturationSteps = saturationSteps; else myGradientSaturationSteps = saturationSteps;}
    public void setBrightnessMin(final int brightnessMin) {if (UIUtil.isUnderDarcula()) myDarkGradientBrightnessMin = brightnessMin; else myGradientBrightnessMin = brightnessMin;}
    public void setBrightnessMax(final int brightnessMax) {if (UIUtil.isUnderDarcula()) myDarkGradientBrightnessMax = brightnessMax; else myGradientBrightnessMax = brightnessMax;}
    public void setBrightnessSteps(final int brightnessSteps) {if (UIUtil.isUnderDarcula()) myDarkGradientBrightnessSteps = brightnessSteps; else myGradientBrightnessSteps = brightnessSteps;}
    public int getHueMin() { return UIUtil.isUnderDarcula() ? myDarkGradientHueMin : myGradientHueMin; }
    public int getHueMax() { return UIUtil.isUnderDarcula() ? myDarkGradientHueMax : myGradientHueMax; }
    public int getHueSteps() { return UIUtil.isUnderDarcula() ? myDarkGradientHueSteps : myGradientHueSteps; }
    public int getSaturationMin() { return UIUtil.isUnderDarcula() ? myDarkGradientSaturationMin : myGradientSaturationMin; }
    public int getSaturationMax() { return UIUtil.isUnderDarcula() ? myDarkGradientSaturationMax : myGradientSaturationMax; }
    public int getSaturationSteps() { return UIUtil.isUnderDarcula() ? myDarkGradientSaturationSteps : myGradientSaturationSteps; }
    public int getBrightnessMin() { return UIUtil.isUnderDarcula() ? myDarkGradientBrightnessMin : myGradientBrightnessMin; }
    public int getBrightnessMax() { return UIUtil.isUnderDarcula() ? myDarkGradientBrightnessMax : myGradientBrightnessMax; }
    public int getBrightnessSteps() { return UIUtil.isUnderDarcula() ? myDarkGradientBrightnessSteps : myGradientBrightnessSteps; }
    public int getDarkGradientHueMin() { return myDarkGradientHueMin; }
    public void setDarkGradientHueMin(final int darkGradientHueMin) { myDarkGradientHueMin = darkGradientHueMin; }
    public int getDarkGradientHueMax() { return myDarkGradientHueMax; }
    public void setDarkGradientHueMax(final int darkGradientHueMax) { myDarkGradientHueMax = darkGradientHueMax; }
    public int getDarkGradientHueSteps() { return myDarkGradientHueSteps; }
    public void setDarkGradientHueSteps(final int darkGradientHueSteps) { myDarkGradientHueSteps = darkGradientHueSteps; }
    public int getDarkGradientSaturationMin() { return myDarkGradientSaturationMin; }
    public void setDarkGradientSaturationMin(final int darkGradientSaturationMin) { myDarkGradientSaturationMin = darkGradientSaturationMin; }
    public int getDarkGradientSaturationMax() { return myDarkGradientSaturationMax; }
    public void setDarkGradientSaturationMax(final int darkGradientSaturationMax) { myDarkGradientSaturationMax = darkGradientSaturationMax; }
    public int getDarkGradientSaturationSteps() { return myDarkGradientSaturationSteps; }
    public void setDarkGradientSaturationSteps(final int darkGradientSaturationSteps) { myDarkGradientSaturationSteps = darkGradientSaturationSteps; }
    public int getDarkGradientBrightnessMin() { return myDarkGradientBrightnessMin; }
    public void setDarkGradientBrightnessMin(final int darkGradientBrightnessMin) { myDarkGradientBrightnessMin = darkGradientBrightnessMin; }
    public int getDarkGradientBrightnessMax() { return myDarkGradientBrightnessMax; }
    public void setDarkGradientBrightnessMax(final int darkGradientBrightnessMax) { myDarkGradientBrightnessMax = darkGradientBrightnessMax; }
    public int getDarkGradientBrightnessSteps() { return myDarkGradientBrightnessSteps; }
    public void setDarkGradientBrightnessSteps(final int darkGradientBrightnessSteps) { myDarkGradientBrightnessSteps = darkGradientBrightnessSteps; }
    public int getGradientHueMin() { return myGradientHueMin; }
    public void setGradientHueMin(int gradientHueMin) { myGradientHueMin = gradientHueMin; }
    public int getGradientHueMax() { return myGradientHueMax; }
    public void setGradientHueMax(int gradientHueMax) { myGradientHueMax = gradientHueMax; }
    public int getGradientHueSteps() { return myGradientHueSteps; }
    public void setGradientHueSteps(int gradientHueSteps) { myGradientHueSteps = gradientHueSteps; }
    public int getGradientSaturationMin() { return myGradientSaturationMin; }
    public void setGradientSaturationMin(int gradientSaturationMin) { myGradientSaturationMin = gradientSaturationMin; }
    public int getGradientSaturationMax() { return myGradientSaturationMax; }
    public void setGradientSaturationMax(int gradientSaturationMax) { myGradientSaturationMax = gradientSaturationMax; }
    public int getGradientSaturationSteps() { return myGradientSaturationSteps; }
    public void setGradientSaturationSteps(int gradientSaturationSteps) { myGradientSaturationSteps = gradientSaturationSteps; }
    public int getGradientBrightnessMin() { return myGradientBrightnessMin; }
    public void setGradientBrightnessMin(int gradientBrightnessMin) { myGradientBrightnessMin = gradientBrightnessMin; }
    public int getGradientBrightnessMax() { return myGradientBrightnessMax; }
    public void setGradientBrightnessMax(int gradientBrightnessMax) { myGradientBrightnessMax = gradientBrightnessMax; }
    public int getGradientBrightnessSteps() { return myGradientBrightnessSteps; }
    public void setGradientBrightnessSteps(int gradientBrightnessSteps) { myGradientBrightnessSteps = gradientBrightnessSteps; }
    public int getIsolatedForegroundColor() { return myIsolatedForegroundColor; }
    public int getIsolatedForegroundDarkColor() { return myIsolatedForegroundDarkColor; }
    public void isolatedForegroundColorRGB(java.awt.Color color) {if (UIUtil.isUnderDarcula()) myIsolatedForegroundDarkColor = color.getRGB(); else myIsolatedForegroundColor = color.getRGB();}
    public java.awt.Color isolatedForegroundColorRGB() {return Color.of(UIUtil.isUnderDarcula() ? myIsolatedForegroundDarkColor : myIsolatedForegroundColor);}
    public void setIsolatedForegroundColor(final int color) { myIsolatedForegroundColor = color; }
    public void setIsolatedForegroundDarkColor(final int color) { myIsolatedForegroundDarkColor = color; }
    public boolean isIsolatedForegroundColorEnabled() { return myIsolatedForegroundColorEnabled; }
    public void setIsolatedForegroundColorEnabled(final boolean isolatedForegroundColorEnabled) { myIsolatedForegroundColorEnabled = isolatedForegroundColorEnabled; }
    public int getIsolatedBackgroundColor() { return myIsolatedBackgroundColor; }
    public int getIsolatedBackgroundDarkColor() { return myIsolatedBackgroundDarkColor; }
    public void setIsolatedBackgroundColor(final int color) { myIsolatedBackgroundColor = color; }
    public void setIsolatedBackgroundDarkColor(final int color) { myIsolatedBackgroundDarkColor = color; }
    public void isolatedBackgroundColorRGB(java.awt.Color color) { if (UIUtil.isUnderDarcula()) myIsolatedBackgroundDarkColor = color.getRGB(); else myIsolatedBackgroundColor = color.getRGB();}
    public java.awt.Color isolatedBackgroundColorRGB() { return Color.of(UIUtil.isUnderDarcula() ? myIsolatedBackgroundDarkColor : myIsolatedBackgroundColor);}
    public boolean isIsolatedBackgroundColorEnabled() { return myIsolatedBackgroundColorEnabled; }
    public void setIsolatedBackgroundColorEnabled(final boolean isolatedBackgroundColorEnabled) { myIsolatedBackgroundColorEnabled = isolatedBackgroundColorEnabled; }
    public boolean isRecalledSelectionColorEnabled() { return myRecalledSelectionColorEnabled; }
    public void setRecalledSelectionColorEnabled(final boolean primaryCaretColorEnabled) { myRecalledSelectionColorEnabled = primaryCaretColorEnabled; }
    public java.awt.Color recalledSelectionColorRGB() {return Color.of(UIUtil.isUnderDarcula() ? myRecalledSelectionDarkColor : myRecalledSelectionColor);}
    public void recalledSelectionColorRGB(final java.awt.Color color) {if (UIUtil.isUnderDarcula()) myRecalledSelectionDarkColor = color.getRGB(); else myRecalledSelectionColor = color.getRGB();}
    public java.awt.Color primaryCaretColorRGB() { return Color.of(UIUtil.isUnderDarcula() ? myPrimaryCaretDarkColor : myPrimaryCaretColor); }
    public void primaryCaretColorRGB(final java.awt.Color primaryCaretColor) {if (UIUtil.isUnderDarcula()) myPrimaryCaretDarkColor = primaryCaretColor.getRGB(); else myPrimaryCaretColor = primaryCaretColor.getRGB();}
    public java.awt.Color searchStartCaretColorRGB() { return Color.of(UIUtil.isUnderDarcula() ? mySearchStartCaretDarkColor : mySearchStartCaretColor); }
    public void searchStartCaretColorRGB(final java.awt.Color primaryCaretColor) {if (UIUtil.isUnderDarcula()) mySearchStartCaretDarkColor = primaryCaretColor.getRGB(); else mySearchStartCaretColor = primaryCaretColor.getRGB();}
    public int getSearchStartMatchedCaretColor() { return mySearchStartMatchedCaretColor; }
    public void setSearchStartMatchedCaretColor(final int searchStartMatchedCaretColor) { mySearchStartMatchedCaretColor = searchStartMatchedCaretColor; }
    public int getSearchStartMatchedCaretDarkColor() { return mySearchStartMatchedCaretDarkColor; }
    public void setSearchStartMatchedCaretDarkColor(final int searchStartMatchedCaretDarkColor) { mySearchStartMatchedCaretDarkColor = searchStartMatchedCaretDarkColor; }
    public int getRecalledSelectionColor() { return myRecalledSelectionColor; }
    public void setRecalledSelectionColor(final int recalledSelectionColor) { myRecalledSelectionColor = recalledSelectionColor; }
    public int getRecalledSelectionDarkColor() { return myRecalledSelectionDarkColor; }
    public void setRecalledSelectionDarkColor(final int recalledSelectionDarkColor) { myRecalledSelectionDarkColor = recalledSelectionDarkColor; }
    public int getSelectionStashLimit() { return mySelectionStashLimit; }
    public void setSelectionStashLimit(final int selectionStashLimit) { mySelectionStashLimit = selectionStashLimit; }
    public boolean isSearchCancelOnEscape() { return mySearchCancelOnEscape; }
    public void setSearchCancelOnEscape(final boolean searchCancelOnEscape) { mySearchCancelOnEscape = searchCancelOnEscape; }
    public boolean isPreservePrimaryCaretOnEscape() { return myPreservePrimaryCaretOnEscape;}
    public void setPreservePrimaryCaretOnEscape(boolean preservePrimaryCaretOnEscape) { myPreservePrimaryCaretOnEscape = preservePrimaryCaretOnEscape;}
    public boolean isSpawnSmartPrefixSearch() { return mySpawnSmartPrefixSearch;}
    public void setSpawnSmartPrefixSearch(final boolean spawnSmartPrefixSearch) { mySpawnSmartPrefixSearch = spawnSmartPrefixSearch;}
    public boolean isSpawnMatchBoundarySearch() { return mySpawnMatchBoundarySearch;}
    public void setSpawnMatchBoundarySearch(final boolean spawnMatchBoundarySearch) { mySpawnMatchBoundarySearch = spawnMatchBoundarySearch;}
    public int getPrimaryCaretColor() { return myPrimaryCaretColor; }
    public int getPrimaryCaretDarkColor() { return myPrimaryCaretDarkColor; }
    public void setPrimaryCaretColor(final int color) { myPrimaryCaretColor = color; }
    public void setPrimaryCaretDarkColor(final int color) { myPrimaryCaretDarkColor = color; }
    public int getSearchStartCaretColor() { return mySearchStartCaretColor; }
    public int getSearchStartCaretDarkColor() { return mySearchStartCaretDarkColor; }
    public void setSearchStartCaretColor(final int color) { mySearchStartCaretColor = color; }
    public void setSearchStartCaretDarkColor(final int color) { mySearchStartCaretDarkColor = color; }
    public int getSearchFoundCaretColor() { return mySearchFoundCaretColor; }
    public int getSearchFoundCaretDarkColor() { return mySearchFoundCaretDarkColor; }
    public void setSearchFoundCaretColor(final int color) { mySearchFoundCaretColor = color; }
    public void setSearchFoundCaretDarkColor(final int color) { mySearchFoundCaretDarkColor = color; }
    public boolean isPrimaryCaretColorEnabled() { return myPrimaryCaretColorEnabled; }
    public void setPrimaryCaretColorEnabled(final boolean primaryCaretColorEnabled) { myPrimaryCaretColorEnabled = primaryCaretColorEnabled; }
    public int getPrimaryCaretThickness() { return myPrimaryCaretThickness; }
    public void setPrimaryCaretThickness(final int primaryCaretThickness) { myPrimaryCaretThickness = primaryCaretThickness; }
    public boolean isSearchStartCaretColorEnabled() { return mySearchStartCaretColorEnabled; }
    public void setSearchStartCaretColorEnabled(final boolean primaryCaretColorEnabled) { mySearchStartCaretColorEnabled = primaryCaretColorEnabled; }
    public int getSearchStartCaretThickness() { return mySearchStartCaretThickness; }
    public void setSearchStartCaretThickness(final int primaryCaretThickness) { mySearchStartCaretThickness = primaryCaretThickness; }
    public boolean isSearchStartMatchedCaretColorEnabled() { return mySearchStartMatchedCaretColorEnabled; }
    public void setSearchStartMatchedCaretColorEnabled(final boolean primaryCaretColorEnabled) { mySearchStartMatchedCaretColorEnabled = primaryCaretColorEnabled; }
    public int getSearchStartMatchedCaretThickness() { return mySearchStartMatchedCaretThickness; }
    public void setSearchStartMatchedCaretThickness(final int primaryCaretThickness) { mySearchStartMatchedCaretThickness = primaryCaretThickness; }
    public java.awt.Color searchStartMatchedCaretColorRGB() { return Color.of(UIUtil.isUnderDarcula() ? mySearchStartMatchedCaretDarkColor : mySearchStartMatchedCaretColor); }
    public void searchStartMatchedCaretColorRGB(final java.awt.Color primaryCaretColor) {if (UIUtil.isUnderDarcula()) mySearchStartMatchedCaretDarkColor = primaryCaretColor.getRGB(); else mySearchStartMatchedCaretColor = primaryCaretColor.getRGB();}
    public boolean isSearchFoundCaretColorEnabled() { return mySearchFoundCaretColorEnabled; }
    public void setSearchFoundCaretColorEnabled(final boolean primaryCaretColorEnabled) { mySearchFoundCaretColorEnabled = primaryCaretColorEnabled; }
    public int getSearchFoundCaretThickness() { return mySearchFoundCaretThickness; }
    public void setSearchFoundCaretThickness(final int primaryCaretThickness) { mySearchFoundCaretThickness = primaryCaretThickness; }
    public java.awt.Color searchFoundCaretColorRGB() { return Color.of(UIUtil.isUnderDarcula() ? mySearchFoundCaretDarkColor : mySearchFoundCaretColor); }
    public void searchFoundCaretColorRGB(final java.awt.Color primaryCaretColor) {if (UIUtil.isUnderDarcula()) mySearchFoundCaretDarkColor = primaryCaretColor.getRGB(); else mySearchFoundCaretColor = primaryCaretColor.getRGB();}
    public boolean isMultiPasteDeleteRepeatedCaretData() { return myMultiPasteDeleteRepeatedCaretData; }
    public void setMultiPasteDeleteRepeatedCaretData(boolean multiPasteDeleteRepeatedCaretData) { myMultiPasteDeleteRepeatedCaretData = multiPasteDeleteRepeatedCaretData; }
    public boolean isMultiPasteDeleteReplacedCaretData() { return myMultiPasteDeleteReplacedCaretData;}
    public void setMultiPasteDeleteReplacedCaretData(boolean multiPasteDeleteReplacedCaretData) { myMultiPasteDeleteReplacedCaretData = multiPasteDeleteReplacedCaretData;}
    public boolean isReplaceMacroVariables() { return myReplaceMacroVariables; }
    public void setReplaceMacroVariables(boolean replaceMacroVariables) { myReplaceMacroVariables = replaceMacroVariables; }
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
    @NotNull public String getRegexSampleText() { return myRegexSampleText; }
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
    public boolean isPreserveDashCaseOnPaste() { return myPreserveDashCaseOnPaste; }
    public void setPreserveDashCaseOnPaste(final boolean preserveDashCaseOnPaste) { myPreserveDashCaseOnPaste = preserveDashCaseOnPaste; }
    public boolean isPreserveDotCaseOnPaste() { return myPreserveDotCaseOnPaste; }
    public void setPreserveDotCaseOnPaste(final boolean preserveDotCaseOnPaste) { myPreserveDotCaseOnPaste = preserveDotCaseOnPaste; }
    public boolean isPreserveSlashCaseOnPaste() { return myPreserveSlashCaseOnPaste; }
    public void setPreserveSlashCaseOnPaste(final boolean preserveSlashCaseOnPaste) { myPreserveSlashCaseOnPaste = preserveSlashCaseOnPaste; }
    public boolean isRemovePrefixOnPaste() { return myRemovePrefixOnPaste; }
    public void setRemovePrefixOnPaste(boolean removePrefixOnPaste) { myRemovePrefixOnPaste = removePrefixOnPaste; }
    @NotNull public String getPrefixesOnPasteText() { return myPrefixesOnPasteText; }
    public void setPrefixesOnPasteText(@NotNull String prefixesOnPasteText) {myPrefixesOnPasteText = prefixesOnPasteText;myPrefixesOnPasteList = null;myPrefixesOnPastePattern = null;}
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
// @formatter:on

    @Nullable
    public ApplicationSettings getState() {
        return this;
    }

    public void loadState(@NotNull ApplicationSettings applicationSettings) {
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
        return ApplicationManager.getApplication().getService(ApplicationSettings.class);
    }
}
