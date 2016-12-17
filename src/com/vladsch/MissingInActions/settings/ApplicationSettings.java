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
    private boolean     myOverrideStandardPaste = false;
    private boolean     myPreserveCamelCaseOnPaste = false;
    private boolean     myPreserveScreamingSnakeCaseOnPaste = false;
    private boolean     myPreserveSnakeCaseOnPaste = false;
    private boolean     myRemovePrefixOnPaste = false;
    private boolean     mySelectPasted = false;
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
    private int         myRemovePrefixOnPastePattern = RemovePrefixOnPastePatternType.ADAPTER.getDefault().intValue;
    private int         mySelectPastedMultiCaretPredicate = SelectionPredicateType.WHEN_HAS_1_PLUS_LINES.intValue;
    private int         mySelectPastedPredicate = SelectionPredicateType.WHEN_HAS_2_PLUS_LINES.intValue;
    private String      myRegexSample1Text = "myCamelCaseInstanceMember";
    private String      myRegexSample2Text = "ourCamelCaseClassMember";
    private String      myRemovePrefixOnPaste1 = "my";
    private String      myRemovePrefixOnPaste2 = "our";
    // @formatter:on

    // get/set enums directly
    // @formatter:off
    public RemovePrefixOnPastePatternType getRemovePrefixOnPastePatternType() { return RemovePrefixOnPastePatternType.ADAPTER.get(myRemovePrefixOnPastePattern); }
    public CaretAdjustmentType getCaretOnMoveSelectionDownType() { return CaretAdjustmentType.ADAPTER.get(myCaretOnMoveSelectionDown); }
    public CaretAdjustmentType getCaretOnMoveSelectionUpType() { return CaretAdjustmentType.ADAPTER.get(myCaretOnMoveSelectionUp); }
    public SelectionPredicateType getDuplicateAtStartOrEndPredicateType() { return SelectionPredicateType.ADAPTER.get(myDuplicateAtStartOrEndPredicate); }
    public LinePasteCaretAdjustmentType getLinePasteCaretAdjustmentType() { return LinePasteCaretAdjustmentType.ADAPTER.get(myLinePasteCaretAdjustment); }
    public SelectionPredicateType getSelectPastedPredicateType() { return SelectionPredicateType.ADAPTER.get(mySelectPastedPredicate); }
    public SelectionPredicateType getSelectPastedMultiCaretPredicateType() { return SelectionPredicateType.ADAPTER.get(mySelectPastedMultiCaretPredicate); }
    public void setRemovePrefixOnPastePatternType(RemovePrefixOnPastePatternType removePrefixOnPastePatternType) { myRemovePrefixOnPastePattern = removePrefixOnPastePatternType.intValue; }
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

    public boolean isMultiPasteShowEolInList() { return myMultiPasteShowEolInList; }

    public void setMultiPasteShowEolInList(final boolean multiPasteShowEolInList) { myMultiPasteShowEolInList = multiPasteShowEolInList; }

    public boolean isMultiPasteShowOptions() { return myMultiPasteShowOptions; }

    public void setMultiPasteShowOptions(final boolean multiPasteShowOptions) { myMultiPasteShowOptions = multiPasteShowOptions; }

    public boolean isMultiPasteShowEolInViewer() { return myMultiPasteShowEolInViewer; }

    public void setMultiPasteShowEolInViewer(final boolean multiPasteShowEolInViewer) { myMultiPasteShowEolInViewer = multiPasteShowEolInViewer; }

    public String getRegexSample1Text() { return myRegexSample1Text; }

    public void setRegexSample1Text(final String regexSample1Text) { myRegexSample1Text = regexSample1Text; }

    public String getRegexSample2Text() { return myRegexSample2Text; }

    public void setRegexSample2Text(final String regexSample2Text) { myRegexSample2Text = regexSample2Text; }

    public boolean isAddPrefixOnPaste() { return myAddPrefixOnPaste; }

    public void setAddPrefixOnPaste(final boolean addPrefixOnPaste) { myAddPrefixOnPaste = addPrefixOnPaste; }

    public int getRemovePrefixOnPastePattern() { return myRemovePrefixOnPastePattern; }

    public void setRemovePrefixOnPastePattern(final int removePrefixOnPastePattern) { myRemovePrefixOnPastePattern = removePrefixOnPastePattern; }

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

    public String getRemovePrefixOnPaste1() { return myRemovePrefixOnPaste1; }

    public void setRemovePrefixOnPaste1(String removePrefixOnPaste1) { myRemovePrefixOnPaste1 = removePrefixOnPaste1; }

    public String getRemovePrefixOnPaste2() { return myRemovePrefixOnPaste2; }

    public void setRemovePrefixOnPaste2(String removePrefixOnPaste2) { myRemovePrefixOnPaste2 = removePrefixOnPaste2; }

    public boolean isPreserveCamelCaseOnPaste() { return myPreserveCamelCaseOnPaste; }

    public void setPreserveCamelCaseOnPaste(boolean preserveCamelCaseOnPaste) { myPreserveCamelCaseOnPaste = preserveCamelCaseOnPaste; }

    public boolean isStartEndAsLineSelection() { return myStartEndAsLineSelection; }

    public void setStartEndAsLineSelection(boolean startEndAsLineSelection) { myStartEndAsLineSelection = startEndAsLineSelection; }

    public boolean isCopyLineOrLineSelection() { return myCopyLineOrLineSelection; }

    public void setCopyLineOrLineSelection(boolean copyLineOrLineSelection) { myCopyLineOrLineSelection = copyLineOrLineSelection; }

    public int getCaretOnMoveSelectionDown() {
        return myCaretOnMoveSelectionDown;
    }

    public void setCaretOnMoveSelectionDown(int caretOnMoveSelectionDown) {
        myCaretOnMoveSelectionDown = caretOnMoveSelectionDown;
    }

    public int getCaretOnMoveSelectionUp() {
        return myCaretOnMoveSelectionUp;
    }

    public void setCaretOnMoveSelectionUp(int caretOnMoveSelectionUp) {
        myCaretOnMoveSelectionUp = caretOnMoveSelectionUp;
    }

    public boolean isIndentUnindent() {
        return myIndentUnindent;
    }

    public void setIndentUnindent(boolean indentUnindent) {
        myIndentUnindent = indentUnindent;
    }

    public boolean isTypingDeletesLineSelection() {
        return myTypingDeletesLineSelection;
    }

    public void setTypingDeletesLineSelection(boolean typingDeletesLineSelection) {
        myTypingDeletesLineSelection = typingDeletesLineSelection;
    }

    public boolean isSelectionStartExtended() {
        return myIsSelectionStartExtended;
    }

    public void setSelectionStartExtended(boolean selectionStartExtended) {
        myIsSelectionStartExtended = selectionStartExtended;
    }

    public boolean isSelectionEndExtended() {
        return myIsSelectionEndExtended;
    }

    public void setSelectionEndExtended(boolean selectionEndExtended) {
        myIsSelectionEndExtended = selectionEndExtended;
    }

    public boolean isMouseCamelHumpsFollow() {
        return myMouseCamelHumpsFollow;
    }

    public void setMouseCamelHumpsFollow(boolean mouseCamelHumpsFollow) {
        myMouseCamelHumpsFollow = mouseCamelHumpsFollow;
    }

    public int getCustomizedNextWordBounds() {
        return myCustomizedNextWordBounds | START_OF_WORD | END_OF_WORD;
    }

    public void setCustomizedNextWordBounds(int customizedNextWordBounds) {
        myCustomizedNextWordBounds = customizedNextWordBounds | START_OF_WORD | END_OF_WORD;
    }

    public int getCustomizedPrevWordBounds() {
        return myCustomizedPrevWordBounds | START_OF_WORD | END_OF_WORD;
    }

    public void setCustomizedPrevWordBounds(int customizedPrevWordBounds) {
        myCustomizedPrevWordBounds = customizedPrevWordBounds | START_OF_WORD | END_OF_WORD;
    }

    public int getCustomizedNextWordStartBounds() {
        return myCustomizedNextWordStartBounds | START_OF_WORD;
    }

    public void setCustomizedNextWordStartBounds(int customizedNextWordStartBounds) {
        myCustomizedNextWordStartBounds = customizedNextWordStartBounds | START_OF_WORD;
    }

    public int getCustomizedPrevWordStartBounds() {
        return myCustomizedPrevWordStartBounds | START_OF_WORD;
    }

    public void setCustomizedPrevWordStartBounds(int customizedPrevWordStartBounds) {
        myCustomizedPrevWordStartBounds = customizedPrevWordStartBounds | START_OF_WORD;
    }

    public int getCustomizedNextWordEndBounds() {
        return myCustomizedNextWordEndBounds | END_OF_WORD;
    }

    public void setCustomizedNextWordEndBounds(int customizedNextWordEndBounds) {
        myCustomizedNextWordEndBounds = customizedNextWordEndBounds | END_OF_WORD;
    }

    public int getCustomizedPrevWordEndBounds() {
        return myCustomizedPrevWordEndBounds | END_OF_WORD;
    }

    public void setCustomizedPrevWordEndBounds(int customizedPrevWordEndBounds) {
        myCustomizedPrevWordEndBounds = customizedPrevWordEndBounds | END_OF_WORD;
    }

    public int getDuplicateAtStartOrEndPredicate() {
        return myDuplicateAtStartOrEndPredicate;
    }

    public void setDuplicateAtStartOrEndPredicate(int duplicateAtStartOrEndPredicate) {
        myDuplicateAtStartOrEndPredicate = duplicateAtStartOrEndPredicate;
    }

    public boolean isDuplicateAtStartOrEnd() {
        return myDuplicateAtStartOrEnd;
    }

    public void setDuplicateAtStartOrEnd(boolean duplicateAtStartOrEnd) {
        myDuplicateAtStartOrEnd = duplicateAtStartOrEnd;
    }

    public boolean isWeSetVirtualSpace() {
        return myWeSetVirtualSpace;
    }

    public void setWeSetVirtualSpace(boolean weSetVirtualSpace) {
        myWeSetVirtualSpace = weSetVirtualSpace;
    }

    public int getSelectPastedPredicate() {
        return mySelectPastedPredicate;
    }

    public void setSelectPastedPredicate(int selectPastedPredicate) {
        mySelectPastedPredicate = selectPastedPredicate;
    }

    public boolean isUnselectToggleCase() {
        return myUnselectToggleCase;
    }

    public void setUnselectToggleCase(boolean unselectToggleCase) {
        myUnselectToggleCase = unselectToggleCase;
    }

    public int getMouseModifier() {
        return myMouseModifier;
    }

    public void setMouseModifier(int mouseModifier) {
        myMouseModifier = mouseModifier;
    }

    public boolean isUpDownSelection() {
        return myUpDownSelection;
    }

    public void setUpDownSelection(boolean upDownSelection) {
        myUpDownSelection = upDownSelection;
    }

    public boolean isMouseLineSelection() {
        return myMouseLineSelection;
    }

    public void setMouseLineSelection(boolean mouseLineSelection) {
        myMouseLineSelection = mouseLineSelection;
    }

    public int getAutoLineMode() {
        return myAutoLineMode;
    }

    public void setAutoLineMode(int autoLineMode) {
        myAutoLineMode = autoLineMode;
    }

    public boolean isDeleteOperations() {
        return myDeleteOperations;
    }

    public void setDeleteOperations(boolean deleteOperations) {
        myDeleteOperations = deleteOperations;
    }

    public boolean isUpDownMovement() {
        return myUpDownMovement;
    }

    public void setUpDownMovement(boolean upDownMovement) {
        myUpDownMovement = upDownMovement;
    }

    public boolean isLeftRightMovement() {
        return myLeftRightMovement;
    }

    public void setLeftRightMovement(boolean leftRightMovement) {
        myLeftRightMovement = leftRightMovement;
    }

    public int getAutoIndentDelay() {
        return myAutoIndentDelay;
    }

    public void setAutoIndentDelay(int autoIndentDelay) {
        myAutoIndentDelay = autoIndentDelay;
    }

    public boolean isAutoIndent() {
        return myAutoIndent;
    }

    public void setAutoIndent(boolean autoIndent) {
        myAutoIndent = autoIndent;
    }

    public boolean isSelectPasted() {
        return mySelectPasted;
    }

    public void setSelectPasted(boolean selectPasted) {
        mySelectPasted = selectPasted;
    }

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
