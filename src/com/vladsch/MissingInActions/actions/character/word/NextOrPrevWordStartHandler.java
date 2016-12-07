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

package com.vladsch.MissingInActions.actions.character.word;

import com.vladsch.MissingInActions.actions.character.NextOrPrevWordStartOrEndHandler;

import static com.vladsch.MissingInActions.util.EditHelpers.*;

public class NextOrPrevWordStartHandler extends NextOrPrevWordStartOrEndHandler {
    public NextOrPrevWordStartHandler(boolean next, boolean withSelection, boolean inDifferentHumpsMode) {
        //noinspection ConstantConditionalExpression
        super(next, withSelection, inDifferentHumpsMode,
                (true ? START_OF_LINE : 0)
                        | (true ? END_OF_LINE : 0)
                        | (true ? START_OF_TRAILING_BLANKS | END_OF_LEADING_BLANKS : 0)
                        | (false ? MIA_IDENTIFIER : 0)
                        | (true ? START_OF_WORD : 0)
                        | (false ? END_OF_WORD : 0)
                        | (false ? START_OF_FOLDING_REGION : 0)
                        | (false ? END_OF_FOLDING_REGION : 0)
                        | (false ? MULTI_CARET_SINGLE_LINE : 0)
        );
    }
}
