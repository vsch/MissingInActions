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

package icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public class PluginIcons {
    private static Icon load(String path) {
        return IconLoader.getIcon(path, PluginIcons.class);
    }

    public static final Icon Clipboard_carets = load("/icons/Clipboard_carets.png");
    public static final Icon Clipboard_text = load("/icons/Clipboard_text.png");
    public static final Icon Clipboard_char_lines_caret = load("/icons/Clipboard_char_lines_caret.png");
    public static final Icon Clipboard_line_caret = load("/icons/Clipboard_line_caret.png");
    public static final Icon Clipboard_char_caret = load("/icons/Clipboard_char_caret.png");
    public static final Icon Accept_search = load("/icons/Accept_search.png");
    public static final Icon Backward_search = load("/icons/Backward_search.png");
    public static final Icon Cancel_search = load("/icons/Cancel_search.png");
    public static final Icon Duplicate_multiple_carets = load("/icons/Duplicate_multiple_carets.png");
    public static final Icon Forward_search = load("/icons/Forward_search.png");
    public static final Icon Number_carets = load("/icons/Number_carets.png");
    public static final Icon Primary_next_caret = load("/icons/Primary_next_caret.png");
    public static final Icon Primary_previous_caret = load("/icons/Primary_previous_caret.png");
    public static final Icon Search_options = load("/icons/Search_options.png");
    public static final Icon Smart_keep_carets = load("/icons/Smart_keep_carets.png");
    public static final Icon Keep_blank_carets = load("/icons/Keep_blank_carets.png");
    public static final Icon Straighten_carets = load("/icons/Straighten_carets.png");
    public static final Icon Toggle_carets_selection = load("/icons/Toggle_carets_selection.png");
    public static final Icon Tab_align_text = load("/icons/Tab_align_text.png");

    public static class EditorActions {
        //public static final Icon Strike_through = load("/icons/editor_actions/Strike_through.png"); // 16x16
    }
}
