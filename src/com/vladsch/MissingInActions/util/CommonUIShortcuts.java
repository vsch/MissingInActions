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

package com.vladsch.MissingInActions.util;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CommonUIShortcuts {
    public static final String ACTION_MOVE_LINE_UP_ACTION = "MoveLineUp";
    public static final String ACTION_MOVE_LINE_DOWN_ACTION = "MoveLineDown";

    // @formatter:off
    public static final ShortcutSet ALT_ENTER = CommonShortcuts.ALT_ENTER;
    public static final ShortcutSet CTRL_ENTER = CommonShortcuts.CTRL_ENTER;
    public static final ShortcutSet DOUBLE_CLICK_1 = CommonShortcuts.DOUBLE_CLICK_1;
    public static final ShortcutSet ENTER = CommonShortcuts.ENTER;
    public static final ShortcutSet ESCAPE = CommonShortcuts.ESCAPE;
    public static final ShortcutSet INSERT = CommonShortcuts.INSERT;
    public static final ShortcutSet MOVE_DOWN = CommonShortcuts.MOVE_DOWN;
    public static final ShortcutSet MOVE_UP = CommonShortcuts.MOVE_UP;

    public static KeyStroke getInsertKeystroke() { return CommonShortcuts.getInsertKeystroke(); }
    public static ShortcutSet getCloseActiveWindow() { return CommonShortcuts.getCloseActiveWindow(); }
    public static ShortcutSet getContextHelp() { return CommonShortcuts.getContextHelp(); }
    public static ShortcutSet getCopy() { return CommonShortcuts.getCopy(); }
    public static ShortcutSet getDelete() { return CommonShortcuts.getDelete(); }
    public static ShortcutSet getDiff() { return CommonShortcuts.getDiff(); }
    public static ShortcutSet getDuplicate() { return CommonShortcuts.getDuplicate(); }
    public static ShortcutSet getEditSource() { return CommonShortcuts.getEditSource(); }
    public static ShortcutSet getFind() { return CommonShortcuts.getFind(); }
    public static ShortcutSet getMove() { return CommonShortcuts.getMove(); }
    public static ShortcutSet getMoveDown() { return CommonShortcuts.getMoveDown(); }
    public static ShortcutSet getMoveEnd() { return CommonShortcuts.getMoveEnd(); }
    public static ShortcutSet getMoveHome() { return CommonShortcuts.getMoveHome(); }
    public static ShortcutSet getMovePageDown() { return CommonShortcuts.getMovePageDown(); }
    public static ShortcutSet getMovePageUp() { return CommonShortcuts.getMovePageUp(); }
    public static ShortcutSet getMoveUp() { return CommonShortcuts.getMoveUp(); }
    public static ShortcutSet getNew() { return CommonShortcuts.getNew(); }
    public static ShortcutSet getNewForDialogs() { return CommonShortcuts.getNewForDialogs(); }
    public static ShortcutSet getPaste() { return CommonShortcuts.getPaste(); }
    public static ShortcutSet getRecentFiles() { return CommonShortcuts.getRecentFiles(); }
    public static ShortcutSet getRename() { return CommonShortcuts.getRename(); }
    public static ShortcutSet getRerun() { return CommonShortcuts.getRerun(); }
    public static ShortcutSet getViewSource() { return CommonShortcuts.getViewSource(); }

    public static ShortcutSet getMoveLineUp() { return shortcutsById("MoveLineUp"); }
    public static ShortcutSet getMoveLineDown() { return shortcutsById("MoveLineDown"); }
    public static ShortcutSet getMultiplePaste() { return shortcutsById("PasteMultiple"); }
    // @formatter:on

    @NotNull
    private static CustomShortcutSet shortcutsById(String actionId) {
        Application application = ApplicationManager.getApplication();
        KeymapManager keymapManager = application == null ? null : application.getComponent(KeymapManager.class);
        if (keymapManager == null) {
            return new CustomShortcutSet(Shortcut.EMPTY_ARRAY);
        }
        return new CustomShortcutSet(keymapManager.getActiveKeymap().getShortcuts(actionId));
    }

    @NotNull
    public static CustomShortcutSet shortcutsFrom(ShortcutSet... sets) {
        int count = 0;
        for (ShortcutSet set : sets) {
            count += set.getShortcuts().length;
        }
        Shortcut[] shortcuts = new Shortcut[count];
        count = 0;
        for (ShortcutSet set : sets) {
            int length = set.getShortcuts().length;
            System.arraycopy(set.getShortcuts(), 0, shortcuts, count, length);
            count += length;
        }
        return new CustomShortcutSet(shortcuts);
    }

    @NotNull
    public static String getNthShortcutText(@NotNull AnAction action, int n) {
        Shortcut[] shortcuts = action.getShortcutSet().getShortcuts();
        String shortcutText = "";
        for (Shortcut shortcut : shortcuts) {
            if (shortcut instanceof KeyboardShortcut) {
                String text = KeymapUtil.getShortcutText(shortcut);
                if (!text.isEmpty()) {
                    shortcutText = text;
                    if (--n <= 0) break;
                }
            }
        }
        return shortcutText;
    }
}
