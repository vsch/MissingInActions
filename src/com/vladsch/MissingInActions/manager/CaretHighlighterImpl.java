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

package com.vladsch.MissingInActions.manager;

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretVisualAttributes;
import com.intellij.ui.JBColor;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import kotlin.NotImplementedError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

import static com.vladsch.MissingInActions.manager.CaretEx.HAVE_VISUAL_ATTRIBUTES;

class CaretHighlighterImpl implements CaretHighlighter {
    @NotNull private final LineSelectionManager myManager;
    @Nullable private CaretEx myPrimaryCaret = null;
    @Nullable private CaretEx mySecondaryCaret = null;
    @Nullable private CaretVisualAttributes myPrimaryAttributes = null;
    @Nullable private CaretVisualAttributes mySecondaryAttributes = null;

    CaretHighlighterImpl(@NotNull LineSelectionManager manager) throws NotImplementedException {
        myManager = manager;

        // will fail if no method implemented
        if (!HAVE_VISUAL_ATTRIBUTES) {
            throw new NotImplementedError("CaretVisualAttributes are not implemented, only in version 2017.1 or later");
        }
    }

    @Override
    public void updateCaretHighlights() {
        highlightCarets();
    }

    @Override
    public void settingsChanged(ApplicationSettings settings) {
        myPrimaryAttributes = new CaretVisualAttributes(null, CaretVisualAttributes.Weight.HEAVY);
        mySecondaryAttributes = new CaretVisualAttributes(JBColor.RED, CaretVisualAttributes.Weight.THIN);
    }

    @Override
    public void removeCaretHighlight() {
        if (myPrimaryCaret != null) {
            myPrimaryCaret.setVisualAttributes(CaretVisualAttributes.DEFAULT);
        }
        if (mySecondaryCaret != null) {
            mySecondaryCaret.setVisualAttributes(CaretVisualAttributes.DEFAULT);
        }

        myPrimaryCaret = null;
        mySecondaryCaret = null;
    }

    @Override
    public void highlightCarets() {
        Caret caret = myManager.getEditor().getCaretModel().getPrimaryCaret();

        int caretCount = myManager.getEditor().getCaretModel().getCaretCount();
        if (myPrimaryCaret != null && (caretCount == 1 || myPrimaryCaret != caret)) {
            removeCaretHighlight();
        }

        if (caretCount > 1 && myPrimaryAttributes != null && mySecondaryAttributes != null) {
            myPrimaryCaret = new CaretEx(caret);
            myPrimaryCaret.setVisualAttributes(myPrimaryAttributes);

            List<Caret> carets = caret.getCaretModel().getAllCarets();
            mySecondaryCaret = new CaretEx(myPrimaryCaret.isCaret(carets.get(0)) ? carets.get(1) : carets.get(0));
            mySecondaryCaret.setVisualAttributes(mySecondaryAttributes);
        }
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
