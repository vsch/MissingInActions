// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.util;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;

public interface EditorActiveLookupListener extends Disposable {
    @NotNull
    Editor getEditor();
    void enterActiveLookup();
    void exitActiveLookup();
}
