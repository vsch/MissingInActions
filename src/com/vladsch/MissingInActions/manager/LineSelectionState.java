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

import org.jetbrains.annotations.Nullable;

public class LineSelectionState {
    private @Nullable Integer myAnchorOffset = null;
    private boolean myIsLine = false;
    
    void reset() {
        myIsLine = false;
        myAnchorOffset = null;
    }

    public int getAnchorOffset(int anchorOffset) {
        return myAnchorOffset == null ? anchorOffset : myAnchorOffset;
    }

    public void setAnchorOffsets(int anchorOffset) {
        myAnchorOffset = anchorOffset;
    }

    public boolean isLine() {
        return myIsLine;
    }

    public void setLine(boolean line) {
        myIsLine = line;
    }

    @Override
    public String toString() {
        return "LineSelectionState{" +
                "myAnchorOffset=" + myAnchorOffset +
                ", myIsLine=" + myIsLine +
                '}';
    }

    public void copyFrom(LineSelectionState other) {
        myAnchorOffset = other.myAnchorOffset;
        myIsLine = other.myIsLine;
    }
}
