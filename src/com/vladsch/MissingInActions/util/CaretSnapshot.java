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
import com.vladsch.flexmark.util.data.DataKey;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("WeakerAccess")
public class CaretSnapshot extends MutableDataSet implements EditorCaretSnapshot {
    // @formatter:off
    final static public DataKey<EditorCaretSnapshot>    CARET                   = new DataKey<>("CARET", EditorCaretSnapshot.NULL);
    final static public DataKey<Params<?>>              PARAMS                  = new DataKey<>("PARAMS",new Params<>(null));

    final static public DataKey<Boolean>                HAS_LINES               = new DataKey<>("HAS_LINES", EditorCaretSnapshot.NULL.hasLines(), holder -> CARET.get(holder).hasLines());
    final static public DataKey<Boolean>                HAS_SELECTION           = new DataKey<>("HAS_SELECTION", EditorCaretSnapshot.NULL.hasSelection(), holder -> CARET.get(holder).hasSelection());
    final static public DataKey<Boolean>                IS_LINE                 = new DataKey<>("IS_LINE", EditorCaretSnapshot.NULL.isLine(), holder -> CARET.get(holder).isLine());
    final static public DataKey<Boolean>                IS_START_ANCHOR         = new DataKey<>("IS_START_ANCHOR", EditorCaretSnapshot.NULL.isStartAnchor(), holder -> CARET.get(holder).isStartAnchor());
    final static public DataKey<EditorPosition>         ANCHOR_POSITION         = new DataKey<>("ANCHOR_POSITION", EditorCaretSnapshot.NULL.getAnchorPosition(), holder -> CARET.get(holder).getAnchorPosition());
    final static public DataKey<EditorPosition>         ANTI_ANCHOR_POSITION    = new DataKey<>("ANTI_ANCHOR_POSITION", EditorCaretSnapshot.NULL.getAntiAnchorPosition(), holder -> CARET.get(holder).getAntiAnchorPosition());
    final static public DataKey<EditorPosition>         CARET_POSITION          = new DataKey<>("CARET_POSITION", EditorCaretSnapshot.NULL.getCaretPosition(), holder -> CARET.get(holder).getCaretPosition());
    final static public DataKey<EditorPosition>         LINE_SELECTION_END      = new DataKey<>("LINE_SELECTION_END", EditorCaretSnapshot.NULL.getLineSelectionEnd(), holder -> CARET.get(holder).getLineSelectionEnd());
    final static public DataKey<EditorPosition>         LINE_SELECTION_START    = new DataKey<>("LINE_SELECTION_START", EditorCaretSnapshot.NULL.getLineSelectionStart(), holder -> CARET.get(holder).getLineSelectionStart());
    final static public DataKey<EditorPosition>         SELECTION_END           = new DataKey<>("SELECTION_END", EditorCaretSnapshot.NULL.getSelectionEnd(), holder -> CARET.get(holder).getSelectionEnd());
    final static public DataKey<EditorPosition>         SELECTION_START         = new DataKey<>("SELECTION_START", EditorCaretSnapshot.NULL.getSelectionStart(), holder -> CARET.get(holder).getSelectionStart());
    final static public DataKey<Integer>                ANCHOR_COLUMN           = new DataKey<>("ANCHOR_COLUMN", EditorCaretSnapshot.NULL.getAnchorColumn(), holder -> CARET.get(holder).getAnchorColumn());
    final static public DataKey<Integer>                COLUMN                  = new DataKey<>("COLUMN", EditorCaretSnapshot.NULL.getColumn(), holder -> CARET.get(holder).getColumn());
    final static public DataKey<Integer>                INDENT                  = new DataKey<>("INDENT", EditorCaretSnapshot.NULL.getIndent(), holder -> CARET.get(holder).getIndent());
    final static public DataKey<Integer>                SELECTION_LINE_COUNT    = new DataKey<>("SELECTION_LINE_COUNT", EditorCaretSnapshot.NULL.getSelectionLineCount(), holder -> CARET.get(holder).getSelectionLineCount());
    // @formatter:on

    // user params
    public static class Params<T extends Params<T>> {
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
        HAS_LINES.get(this);
        HAS_SELECTION.get(this);
        IS_LINE.get(this);
        IS_START_ANCHOR.get(this);
        ANCHOR_POSITION.get(this);
        ANTI_ANCHOR_POSITION.get(this);
        CARET_POSITION.get(this);
        LINE_SELECTION_END.get(this);
        LINE_SELECTION_START.get(this);
        SELECTION_END.get(this);
        SELECTION_START.get(this);
        ANCHOR_COLUMN.get(this);
        COLUMN.get(this);
        INDENT.get(this);
        SELECTION_LINE_COUNT.get(this);

        set(CARET, this);
    }

    // *
    // * EditorCaretSnapshot
    // *

    // @formatter:off
    @Override public boolean                        hasLines()                              { return HAS_LINES.get(this);}
    @Override public boolean                        hasSelection()                          { return HAS_SELECTION.get(this);}
    @Override public boolean                        isLine()                                { return IS_LINE.get(this);}
    @Override public boolean                        isStartAnchor()                         { return IS_START_ANCHOR.get(this);}
    @Override @NotNull public EditorPosition        getAnchorPosition()                     { return ANCHOR_POSITION.get(this);}
    @Override @NotNull public EditorPosition        getAntiAnchorPosition()                 { return ANTI_ANCHOR_POSITION.get(this);}
    @Override @NotNull public EditorPosition        getCaretPosition()                      { return CARET_POSITION.get(this);}
    @Override @NotNull public EditorPosition        getLineSelectionEnd()                   { return LINE_SELECTION_END.get(this);}
    @Override @NotNull public EditorPosition        getLineSelectionStart()                 { return LINE_SELECTION_START.get(this);}
    @Override @NotNull public EditorPosition        getSelectionEnd()                       { return SELECTION_END.get(this);}
    @Override @NotNull public EditorPosition        getSelectionStart()                     { return SELECTION_START.get(this);}
    @Override public int                            getAnchorColumn()                       { return ANCHOR_COLUMN.get(this);}
    @Override public int                            getColumn()                             { return COLUMN.get(this);}
    @Override public int                            getIndent()                             { return INDENT.get(this);}
    @Override public int                            getSelectionLineCount()                 { return SELECTION_LINE_COUNT.get(this);}
    // @formatter:on

    // Helpers
    @SuppressWarnings("UnusedReturnValue")
    @NotNull
    public CaretSnapshot restoreColumn() {
        if (myCaret.isValid() && !myCaret.getEditor().getSettings().isUseSoftWraps()) {
            myCaret.moveToLogicalPosition(new LogicalPosition(myCaret.getLogicalPosition().line, COLUMN.get(this)));
        }
        return this;
    }

    public CaretSnapshot restoreColumn(EditorCaret editorCaret) {
        editorCaret.restoreColumn(COLUMN.get(this));
        return this;
    }

    @NotNull
    @Override
    public CaretSnapshot removeSelection() {
        if (myCaret.isValid()) {
            myCaret.removeSelection();
        }
        return this;
    }
}
