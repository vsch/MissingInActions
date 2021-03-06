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

package com.vladsch.MissingInActions.manager;

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretVisualAttributes;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.settings.CaretThicknessType;
import kotlin.NotImplementedError;
import org.apache.commons.lang.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

import static com.vladsch.MissingInActions.manager.CaretEx.HAVE_VISUAL_ATTRIBUTES;

class CaretHighlighterImpl implements CaretHighlighter {
    @NotNull private final LineSelectionManager myManager;
    @Nullable private CaretEx myPrimaryCaret = null;
    @Nullable private CaretVisualAttributes myPrimaryAttributes = null;
    @Nullable private CaretVisualAttributes myStartMatchedAttributes = null;
    @Nullable private CaretVisualAttributes myStartAttributes = null;
    @Nullable private CaretVisualAttributes myFoundAttributes = null;

    CaretHighlighterImpl(@NotNull LineSelectionManager manager) throws NotImplementedException {
        myManager = manager;

        // will fail if no method implemented
        if (!HAVE_VISUAL_ATTRIBUTES) {
            throw new NotImplementedError("CaretVisualAttributes are not implemented, only in version 2017.1 or later");
        }
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
    @Nullable
    public CaretEx getPrimaryCaret() {
        return myPrimaryCaret;
    }

    @Override
    public void setPrimaryCaret(@Nullable final Caret caret) {
        myPrimaryCaret = caret == null ? null : new CaretEx(caret);
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

        Set<CaretEx> myFoundCarets = myManager.getFoundCarets();
        Set<CaretEx> myStartMatchedCarets = myManager.getStartMatchedCarets();
        Set<CaretEx> myStartCarets = myManager.getStartCarets();

        Set<Long> excludeList = null;

        highlightCaretList(myFoundCarets, CaretAttributeType.DEFAULT, null);

        excludeList = CaretEx.getExcludedCoordinates(excludeList, myFoundCarets);
        highlightCaretList(myStartMatchedCarets, CaretAttributeType.DEFAULT, excludeList);

        excludeList = CaretEx.getExcludedCoordinates(excludeList, myStartMatchedCarets);
        highlightCaretList(myStartCarets, CaretAttributeType.DEFAULT, excludeList);
    }

    @Override
    public void highlightCaretList(@Nullable Collection<CaretEx> carets, @NotNull CaretAttributeType attributeType, @Nullable Set<Long> excludeList) {
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
            for (CaretEx caretEx : carets) {
                if (excludeList != null && excludeList.contains(caretEx.getCoordinates())) continue;
                caretEx.setVisualAttributes(attributes);
            }
        }
    }

    @Override
    public void highlightCarets() {
        int caretCount = myManager.getEditor().getCaretModel().getCaretCount();
        Set<CaretEx> myFoundCarets = myManager.getFoundCarets();
        Set<CaretEx> myStartMatchedCarets = myManager.getStartMatchedCarets();
        Set<CaretEx> myStartCarets = myManager.getStartCarets();

        if (caretCount == 1 || (myFoundCarets == null && myStartMatchedCarets == null && myStartCarets == null)) {
            Caret caret = myManager.getEditor().getCaretModel().getPrimaryCaret();

            removeCaretHighlight();
            myPrimaryCaret = null;

            if (caretCount > 1) {
                if (myPrimaryAttributes != null) {
                    myPrimaryCaret = new CaretEx(caret);
                    myPrimaryCaret.setVisualAttributes(myPrimaryAttributes);
                }
            } else {
                Set<Long> excludeList = null;

                highlightCaretList(myFoundCarets, CaretAttributeType.DEFAULT, null);

                excludeList = CaretEx.getExcludedCoordinates(excludeList, myFoundCarets);
                highlightCaretList(myStartMatchedCarets, CaretAttributeType.DEFAULT, excludeList);

                excludeList = CaretEx.getExcludedCoordinates(excludeList, myStartMatchedCarets);
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
