/*
 * Copyright (c) 2016-2018 Vladimir Schneider <vladimir.schneider@gmail.com>
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

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.LogicalPosition;
import com.vladsch.MissingInActions.manager.EditorCaret;
import com.vladsch.MissingInActions.manager.EditorPosition;
import com.vladsch.flexmark.util.options.DataKey;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.options.MutableDataSet;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("WeakerAccess")
public class CaretSnapshot extends MutableDataSet implements EditorCaretSnapshot {
    // @formatter:off
    final static public DataKey<EditorCaretSnapshot>    CARET                   = new DataKey<>("CARET", EditorCaretSnapshot.NULL);
    final static public DataKey<Params>                 PARAMS                  = new DataKey<>("PARAMS", value-> new Params(null));

    final static public DataKey<Boolean>                HAS_LINES               = new DataKey<>("HAS_LINES", holder -> CARET.getFrom(holder).hasLines());
    final static public DataKey<Boolean>                HAS_SELECTION           = new DataKey<>("HAS_SELECTION", holder -> CARET.getFrom(holder).hasSelection());
    final static public DataKey<Boolean>                IS_LINE                 = new DataKey<>("IS_LINE", holder -> CARET.getFrom(holder).isLine());
    final static public DataKey<Boolean>                IS_START_ANCHOR         = new DataKey<>("IS_START_ANCHOR", holder -> CARET.getFrom(holder).isStartAnchor());
    final static public DataKey<EditorPosition>         ANCHOR_POSITION         = new DataKey<>("ANCHOR_POSITION", holder -> CARET.getFrom(holder).getAnchorPosition());
    final static public DataKey<EditorPosition>         ANTI_ANCHOR_POSITION    = new DataKey<>("ANTI_ANCHOR_POSITION", holder -> CARET.getFrom(holder).getAntiAnchorPosition());
    final static public DataKey<EditorPosition>         CARET_POSITION          = new DataKey<>("CARET_POSITION", holder -> CARET.getFrom(holder).getCaretPosition());
    final static public DataKey<EditorPosition>         LINE_SELECTION_END      = new DataKey<>("LINE_SELECTION_END", holder -> CARET.getFrom(holder).getLineSelectionEnd());
    final static public DataKey<EditorPosition>         LINE_SELECTION_START    = new DataKey<>("LINE_SELECTION_START", holder -> CARET.getFrom(holder).getLineSelectionStart());
    final static public DataKey<EditorPosition>         SELECTION_END           = new DataKey<>("SELECTION_END", holder -> CARET.getFrom(holder).getSelectionEnd());
    final static public DataKey<EditorPosition>         SELECTION_START         = new DataKey<>("SELECTION_START", holder -> CARET.getFrom(holder).getSelectionStart());
    final static public DataKey<Integer>                ANCHOR_COLUMN           = new DataKey<>("ANCHOR_COLUMN", holder -> CARET.getFrom(holder).getAnchorColumn());
    final static public DataKey<Integer>                COLUMN                  = new DataKey<>("COLUMN", holder -> CARET.getFrom(holder).getColumn());
    final static public DataKey<Integer>                INDENT                  = new DataKey<>("INDENT", holder -> CARET.getFrom(holder).getIndent());
    final static public DataKey<Integer>                SELECTION_LINE_COUNT    = new DataKey<>("SELECTION_LINE_COUNT", holder -> CARET.getFrom(holder).getSelectionLineCount());
    // @formatter:on

    // user params
    public static class Params<T extends Params> {
        @SuppressWarnings("unchecked")
        void set(MutableDataHolder holder) { holder.set(PARAMS, this); }

        protected Params(CaretSnapshot snapshot) { if (snapshot != null) set(snapshot); }
    }

    final private Caret myCaret;
    final private int myIndex;

    public CaretSnapshot(EditorCaret editorCaret, int index) {
        myCaret = editorCaret.getCaret();
        myIndex = index;
        snapshot(editorCaret);
    }

    @NotNull
    public Caret getCaret() {
        return myCaret;
    }

    public int getIndex() {
        return myIndex;
    }

    private void snapshot(EditorCaret editorCaret) {
        set(CARET, editorCaret);

        // take a snapshot of the caret data
        HAS_LINES.getFrom(this);
        HAS_SELECTION.getFrom(this);
        IS_LINE.getFrom(this);
        IS_START_ANCHOR.getFrom(this);
        ANCHOR_POSITION.getFrom(this);
        ANTI_ANCHOR_POSITION.getFrom(this);
        CARET_POSITION.getFrom(this);
        LINE_SELECTION_END.getFrom(this);
        LINE_SELECTION_START.getFrom(this);
        SELECTION_END.getFrom(this);
        SELECTION_START.getFrom(this);
        ANCHOR_COLUMN.getFrom(this);
        COLUMN.getFrom(this);
        INDENT.getFrom(this);
        SELECTION_LINE_COUNT.getFrom(this);

        set(CARET, this);
    }

    // *
    // * EditorCaretSnapshot
    // *

    // @formatter:off
    @Override public boolean                        hasLines()                              { return HAS_LINES.getFrom(this); }
    @Override public boolean                        hasSelection()                          { return HAS_SELECTION.getFrom(this); }
    @Override public boolean                        isLine()                                { return IS_LINE.getFrom(this); }
    @Override public boolean                        isStartAnchor()                         { return IS_START_ANCHOR.getFrom(this); }
    @Override @NotNull public EditorPosition        getAnchorPosition()                     { return ANCHOR_POSITION.getFrom(this); }
    @Override @NotNull public EditorPosition        getAntiAnchorPosition()                 { return ANTI_ANCHOR_POSITION.getFrom(this); }
    @Override @NotNull public EditorPosition        getCaretPosition()                      { return CARET_POSITION.getFrom(this); }
    @Override @NotNull public EditorPosition        getLineSelectionEnd()                   { return LINE_SELECTION_END.getFrom(this); }
    @Override @NotNull public EditorPosition        getLineSelectionStart()                 { return LINE_SELECTION_START.getFrom(this); }
    @Override @NotNull public EditorPosition        getSelectionEnd()                       { return SELECTION_END.getFrom(this); }
    @Override @NotNull public EditorPosition        getSelectionStart()                     { return SELECTION_START.getFrom(this); }
    @Override public int                            getAnchorColumn()                       { return ANCHOR_COLUMN.getFrom(this); }
    @Override public int                            getColumn()                             { return COLUMN.getFrom(this); }
    @Override public int                            getIndent()                             { return INDENT.getFrom(this); }
    @Override public int                            getSelectionLineCount()                 { return SELECTION_LINE_COUNT.getFrom(this); }
    // @formatter:on

    // Helpers
    @NotNull
    public CaretSnapshot restoreColumn() {
        if (!myCaret.getEditor().getSettings().isUseSoftWraps()) {
            myCaret.moveToLogicalPosition(new LogicalPosition(myCaret.getLogicalPosition().line, get(COLUMN)));
        }
        return this;
    }

    public CaretSnapshot restoreColumn(EditorCaret editorCaret) {
        editorCaret.restoreColumn(get(COLUMN));
        return this;
    }

    @NotNull
    @Override
    public CaretSnapshot removeSelection() {
        myCaret.removeSelection();
        return this;
    }
}
