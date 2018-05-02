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

package com.vladsch.MissingInActions.actions.character.custom;

import com.vladsch.MissingInActions.actions.character.AbstractNextOrPrevWordHandler;
import com.vladsch.MissingInActions.settings.ApplicationSettings;

public class NextOrPrevWordStartOrEndHandler extends AbstractNextOrPrevWordHandler {
    public enum BoundaryActionType {
        WORD,
        WORD_START,
        WORD_END,
    }

    final private BoundaryActionType myActionType;
    final private boolean myNext;
    final private boolean myWithSelection;
    final private boolean myInDifferentHumpsMode;
    final private ApplicationSettings mySettings;

    public NextOrPrevWordStartOrEndHandler(boolean next, boolean withSelection, boolean inDifferentHumpsMode, BoundaryActionType actionType) {
        super();

        myActionType = actionType;
        myNext = next;
        myWithSelection = withSelection;
        myInDifferentHumpsMode = inDifferentHumpsMode;

        mySettings = ApplicationSettings.getInstance();
    }

    public boolean isNext() {
        return myNext;
    }

    public boolean isWithSelection() {
        return myWithSelection;
    }

    public boolean isInDifferentHumpsMode() {
        return myInDifferentHumpsMode;
    }

    public int getBoundaryFlags() {
        if (myActionType == BoundaryActionType.WORD_END) {
            return myNext ? mySettings.getCustomizedNextWordEndBounds() : mySettings.getCustomizedPrevWordEndBounds();
        }
        if (myActionType == BoundaryActionType.WORD_START) {
            return myNext ? mySettings.getCustomizedNextWordStartBounds() : mySettings.getCustomizedPrevWordStartBounds();
        }
        return myNext ? mySettings.getCustomizedNextWordBounds() : mySettings.getCustomizedPrevWordBounds();
    }
}
