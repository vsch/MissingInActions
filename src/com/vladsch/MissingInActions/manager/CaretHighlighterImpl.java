// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.manager;

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretVisualAttributes;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.settings.CaretThicknessType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

class CaretHighlighterImpl implements CaretHighlighter {
    @NotNull private final LineSelectionManager myManager;
    @Nullable private Caret myPrimaryCaret = null;
    @Nullable private CaretVisualAttributes myPrimaryAttributes = null;
    @Nullable private CaretVisualAttributes myStartMatchedAttributes = null;
    @Nullable private CaretVisualAttributes myStartAttributes = null;
    @Nullable private CaretVisualAttributes myFoundAttributes = null;

    CaretHighlighterImpl(@NotNull LineSelectionManager manager) {
        myManager = manager;
    }

    private static CaretVisualAttributes.Weight getCaretWeight(CaretThicknessType thicknessType) {
        switch (thicknessType) {
            case THIN:
                return CaretVisualAttributes.Weight.THIN;
            case HEAVY:
                return CaretVisualAttributes.Weight.HEAVY;

            default:
            case NORMAL:
                return CaretVisualAttributes.Weight.NORMAL;
        }
    }

    @Override
    public @Nullable Caret getPrimaryCaret() {
        return myPrimaryCaret;
    }

    @Override
    public void setPrimaryCaret(@Nullable final Caret caret) {
        myPrimaryCaret = caret;
    }

    @Override
    public void settingsChanged(ApplicationSettings settings) {
        myPrimaryAttributes = new CaretVisualAttributes(settings.isPrimaryCaretColorEnabled() ? settings.primaryCaretColorRGB() : null, getCaretWeight(settings.getPrimaryCaretThicknessType()));
        myStartAttributes = new CaretVisualAttributes(settings.isSearchStartCaretColorEnabled() ? settings.searchStartCaretColorRGB() : null, getCaretWeight(settings.getSearchStartCaretThicknessType()));
        myStartMatchedAttributes = new CaretVisualAttributes(settings.isSearchStartMatchedCaretColorEnabled() ? settings.searchStartMatchedCaretColorRGB() : myStartAttributes.getColor(), getCaretWeight(settings.getSearchStartFoundCaretThicknessType()));
        myFoundAttributes = new CaretVisualAttributes(settings.isSearchFoundCaretColorEnabled() ? settings.searchFoundCaretColorRGB() : null, getCaretWeight(settings.getSearchFoundCaretThicknessType()));
    }

    @Override
    public void removeCaretHighlight() {
        if (myPrimaryCaret != null) {
            myPrimaryCaret.setVisualAttributes(CaretVisualAttributes.DEFAULT);
        }

        Set<Caret> myFoundCarets = myManager.getFoundCarets();
        Set<Caret> myStartMatchedCarets = myManager.getStartMatchedCarets();
        Set<Caret> myStartCarets = myManager.getStartCarets();

        Set<Long> excludeList = null;

        highlightCaretList(myFoundCarets, CaretAttributeType.DEFAULT, null);

        //noinspection ConstantValue,ReassignedVariable
        excludeList = CaretUtils.getExcludedCoordinates(excludeList, myFoundCarets);
        highlightCaretList(myStartMatchedCarets, CaretAttributeType.DEFAULT, excludeList);

        excludeList = CaretUtils.getExcludedCoordinates(excludeList, myStartMatchedCarets);
        highlightCaretList(myStartCarets, CaretAttributeType.DEFAULT, excludeList);
    }

    @Override
    public void highlightCaretList(@Nullable Collection<Caret> carets, @NotNull CaretAttributeType attributeType, @Nullable Set<Long> excludeList) {
        CaretVisualAttributes attributes = null;

        switch (attributeType) {
            case PRIMARY:
                attributes = myPrimaryAttributes;
                break;
            case START:
                attributes = myStartAttributes;
                break;
            case START_MATCHED:
                attributes = myStartMatchedAttributes;
                break;
            case FOUND:
                attributes = myFoundAttributes;
                break;
        }

        if (attributes == null) attributes = CaretVisualAttributes.DEFAULT;

        if (carets != null && !carets.isEmpty()) {
            for (Caret caret : carets) {
                if (excludeList != null && excludeList.contains(CaretUtils.getCoordinates(caret))) continue;
                caret.setVisualAttributes(attributes);
            }
        }
    }

    @Override
    public void highlightCarets() {
        int caretCount = myManager.getEditor().getCaretModel().getCaretCount();
        Set<Caret> myFoundCarets = myManager.getFoundCarets();
        Set<Caret> myStartMatchedCarets = myManager.getStartMatchedCarets();
        Set<Caret> myStartCarets = myManager.getStartCarets();

        if (caretCount == 1 || (myFoundCarets == null && myStartMatchedCarets == null && myStartCarets == null)) {
            Caret caret = myManager.getEditor().getCaretModel().getPrimaryCaret();

            removeCaretHighlight();
            myPrimaryCaret = null;

            if (caretCount > 1) {
                if (myPrimaryAttributes != null) {
                    myPrimaryCaret = caret;
                    myPrimaryCaret.setVisualAttributes(myPrimaryAttributes);
                }
            } else {
                Set<Long> excludeList = null;

                highlightCaretList(myFoundCarets, CaretAttributeType.DEFAULT, null);

                //noinspection ConstantValue,ReassignedVariable
                excludeList = CaretUtils.getExcludedCoordinates(excludeList, myFoundCarets);
                highlightCaretList(myStartMatchedCarets, CaretAttributeType.DEFAULT, excludeList);

                excludeList = CaretUtils.getExcludedCoordinates(excludeList, myStartMatchedCarets);
                highlightCaretList(myStartCarets, CaretAttributeType.DEFAULT, excludeList);
            }
        }
    }

    @Override
    public void updateCaretHighlights() {
        highlightCarets();
    }

    @Override
    public void caretAdded(@NotNull Caret caret) {
        updateCaretHighlights();
    }

    @Override
    public void caretRemoved(@NotNull Caret caret) {
        updateCaretHighlights();
    }
}
