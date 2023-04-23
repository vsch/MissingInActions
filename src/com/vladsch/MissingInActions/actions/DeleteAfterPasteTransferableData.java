// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions;

import com.intellij.openapi.editor.CaretStateTransferableData;

import java.awt.datatransfer.DataFlavor;

public class DeleteAfterPasteTransferableData extends CaretStateTransferableData {
    public static final DataFlavor FLAVOR = new DataFlavor(DeleteAfterPasteTransferableData.class, "Split, Merged Caret state");

    public DeleteAfterPasteTransferableData(int[] startOffsets, int[] endOffsets) {
        super(startOffsets, endOffsets);
    }

    @Override
    public DataFlavor getFlavor() {
        return FLAVOR;
    }
}
