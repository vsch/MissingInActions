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

import com.intellij.openapi.diagnostic.Logger;
import com.vladsch.MissingInActions.util.OneTimeRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.intellij.openapi.diagnostic.Logger.getInstance;

public class ActionAdjustmentMap {
    final private static Logger logger = getInstance("com.vladsch.MissingInActions.manager");
    final static ActionAdjustmentMap EMPTY = new ActionAdjustmentMap();

    final private HashMap<Class, AdjustmentType> myAdjustmentsMap = new HashMap<>();
    final private HashMap<Class, TriggeredAction> myTriggeredActionsMap = new HashMap<>();

    ActionAdjustmentMap() {
        super();
    }

    public AdjustmentType getAdjustment(Class action) {
        return myAdjustmentsMap.get(action);
    }

    public TriggeredAction getTriggeredAction(Class action) {
        return myTriggeredActionsMap.get(action);
    }

    public boolean hasTriggeredAction(Class action) {
        return myTriggeredActionsMap.containsKey(action);
    }

    public void addActionAdjustment(AdjustmentType adjustments, Class... actions) {
        AdjustmentType adj;
        for (Class action : actions) {
            if ((adj = myAdjustmentsMap.get(action)) != null) {
                logger.error("Action '" + action + "' already has adjustment " + adj + " trying to assign " + adjustments);
            }
            myAdjustmentsMap.put(action, adjustments);
        }
    }
    
    public void addTriggeredAction(TriggeredAction triggeredAction, Class... actions) {
        TriggeredAction adj;
        for (Class action : actions) {
            if ((adj = myTriggeredActionsMap.get(action)) != null) {
                logger.error("Action '" + action + "' already has a triggered action " + adj + " trying to assign " + triggeredAction);
            }
            myTriggeredActionsMap.put(action, triggeredAction);
        }
    }
}
