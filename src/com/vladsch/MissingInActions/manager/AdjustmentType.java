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

public enum AdjustmentType {
    // no adjustment before, no adjustment after 
    NOTHING__NOTHING,
    
    // if line then remove selection, used for left/right keys or they jump to begining/end of selection 
    REMOVE_LINE__NOTHING,
    
    // change line to char selection before, do nothing after 
    TO_CHAR__NOTHING,
    
    // change line to char selection before and change back to line if spans more than one line
    TO_CHAR__TO_LINE,
    
    // change line to char selection before and always change to line after
    TO_CHAR__TO_ALWAYS_LINE,
    
    // if selection is line before action, then after action restore caret column to what it was before action
    IF_LINE__FIX_CARET,
    
    // nothing before, always restore caret column after
    NOTHING__RESTORE_COLUMN,
    
    // nothing before, restore caret column after to position relative to end of line
    NOTHING__RESTORE_COLUMN_LINE_END_RELATIVE,
    
    // move caret to start of selection before, restore caret column after if it is at column 0
    MOVE_TO_START__RESTORE_IF0,

    // nothing before, change to line if it is full line selection
    NOTHING__TO_LINE_IF_LOOKS_IT,
    
    // if did not have selection before then remove it after, ToggleCase leaves its selection behind, but if
    // there was no selection then no need to leave it selected, the next invocation will affect the same text range. Duh!
    IF_NO_SELECTION__REMOVE_SELECTION,

    IF_NO_SELECTION__REMOVE_SELECTION___IF_LINE_RESTORE_COLUMN,
    
    IF_NO_SELECTION__TO_LINE_RESTORE_COLUMN,
}
