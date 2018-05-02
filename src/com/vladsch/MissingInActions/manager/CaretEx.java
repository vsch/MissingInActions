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

import com.intellij.openapi.editor.*;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CaretEx implements Caret {
    private static final Method ourGetVisualAttributes;
    private static final Method ourSetVisualAttributes;
    private final Caret myCaret;
    static {
        Method[] methods = Caret.class.getMethods();
        Method getVisualAttributes = null;
        Method setVisualAttributes = null;

        for (Method method : methods) {
            if (method.getName().equals("getVisualAttributes")) {
                getVisualAttributes = method;
            }
            if (method.getName().equals("setVisualAttributes")) {
                setVisualAttributes = method;
            }
        }

        ourGetVisualAttributes = getVisualAttributes;
        ourSetVisualAttributes = setVisualAttributes;
    }

    public static final boolean HAVE_VISUAL_ATTRIBUTES = ourGetVisualAttributes != null && ourSetVisualAttributes != null;

    @Nullable
    public static Set<Long> getExcludedCoordinates(@Nullable Set<Long> excludeSet, @Nullable Collection<CaretEx> exclude) {
        if (exclude == null || exclude.isEmpty()) return excludeSet;
        if (excludeSet == null) excludeSet = new HashSet<>(exclude.size());
        for (CaretEx caretEx : exclude) {
            excludeSet.add(caretEx.getCoordinates());
        }
        return excludeSet;
    }

    public CaretEx(Caret caret) {
        myCaret = caret;
    }

    public Caret getCaret() {
        return myCaret;
    }

    public long getCoordinates() {
        return getCoordinates(myCaret);
    }

    public static long getCoordinates(Caret caret) {
        return getCoordinates(caret.getLogicalPosition());
    }

    public static long getCoordinates(LogicalPosition logicalPosition) {
        return ((long) logicalPosition.line << 32) | (logicalPosition.column);
    }

    /**
     * Returns visual attributes currently set for the caret.
     *
     * @see #setVisualAttributes(CaretVisualAttributes)
     */
    @NotNull
    public CaretVisualAttributes getVisualAttributes() {
        CaretVisualAttributes attributes = CaretVisualAttributes.DEFAULT;
        if (ourGetVisualAttributes != null) {
            try {
                attributes = (CaretVisualAttributes) ourGetVisualAttributes.invoke(myCaret);
            } catch (IllegalAccessException | InvocationTargetException ignored) {
                attributes = CaretVisualAttributes.DEFAULT;
            }
        }
        return attributes;
    }

    /**
     * Sets caret's current visual attributes. This can have no effect if editor doesn't support changing caret's visual appearance.
     *
     * @see #getVisualAttributes()
     */
    public void setVisualAttributes(@NotNull CaretVisualAttributes attributes) {
        if (ourSetVisualAttributes != null) {
            try {
                ourSetVisualAttributes.invoke(myCaret, attributes);
            } catch (IllegalAccessException | InvocationTargetException ignored) {
            }
        }
    }

    @Override
    public void dispose() {

    }

    public boolean isCaret(Caret caret) {
        return equals(caret);
    }

    @Override
    @NotNull
    public Editor getEditor() {return myCaret.getEditor();}

    @Override
    @NotNull
    public CaretModel getCaretModel() {return myCaret.getCaretModel();}

    @Override
    public boolean isValid() {return myCaret.isValid();}

    @Override
    public void moveCaretRelatively(int columnShift, int lineShift, boolean withSelection, boolean scrollToCaret) {myCaret.moveCaretRelatively(columnShift, lineShift, withSelection, scrollToCaret);}

    @Override
    public void moveToLogicalPosition(@NotNull LogicalPosition pos) {myCaret.moveToLogicalPosition(pos);}

    @Override
    public void moveToVisualPosition(@NotNull VisualPosition pos) {myCaret.moveToVisualPosition(pos);}

    @Override
    public void moveToOffset(int offset) {myCaret.moveToOffset(offset);}

    @Override
    public void moveToOffset(int offset, boolean locateBeforeSoftWrap) {myCaret.moveToOffset(offset, locateBeforeSoftWrap);}

    @Override
    public boolean isUpToDate() {return myCaret.isUpToDate();}

    @Override
    @NotNull
    public LogicalPosition getLogicalPosition() {return myCaret.getLogicalPosition();}

    @Override
    @NotNull
    public VisualPosition getVisualPosition() {return myCaret.getVisualPosition();}

    @Override
    public int getOffset() {return myCaret.getOffset();}

    @Override
    public int getVisualLineStart() {return myCaret.getVisualLineStart();}

    @Override
    public int getVisualLineEnd() {return myCaret.getVisualLineEnd();}

    @Override
    public int getSelectionStart() {return myCaret.getSelectionStart();}

    @Override
    @NotNull
    public VisualPosition getSelectionStartPosition() {return myCaret.getSelectionStartPosition();}

    @Override
    public int getSelectionEnd() {return myCaret.getSelectionEnd();}

    @Override
    @NotNull
    public VisualPosition getSelectionEndPosition() {return myCaret.getSelectionEndPosition();}

    @Override
    @Nullable
    public String getSelectedText() {return myCaret.getSelectedText();}

    @Override
    public int getLeadSelectionOffset() {return myCaret.getLeadSelectionOffset();}

    @Override
    @NotNull
    public VisualPosition getLeadSelectionPosition() {return myCaret.getLeadSelectionPosition();}

    @Override
    public boolean hasSelection() {return myCaret.hasSelection();}

    @Override
    public void setSelection(int startOffset, int endOffset) {myCaret.setSelection(startOffset, endOffset);}

    @Override
    public void setSelection(int startOffset, int endOffset, boolean updateSystemSelection) {myCaret.setSelection(startOffset, endOffset, updateSystemSelection);}

    @Override
    public void setSelection(int startOffset, @Nullable VisualPosition endPosition, int endOffset) {myCaret.setSelection(startOffset, endPosition, endOffset);}

    @Override
    public void setSelection(@Nullable VisualPosition startPosition, int startOffset, @Nullable VisualPosition endPosition, int endOffset) {myCaret.setSelection(startPosition, startOffset, endPosition, endOffset);}

    @Override
    public void setSelection(@Nullable VisualPosition startPosition, int startOffset, @Nullable VisualPosition endPosition, int endOffset, boolean updateSystemSelection) {myCaret.setSelection(startPosition, startOffset, endPosition, endOffset, updateSystemSelection);}

    @Override
    public void removeSelection() {myCaret.removeSelection();}

    @Override
    public void selectLineAtCaret() {myCaret.selectLineAtCaret();}

    @Override
    public void selectWordAtCaret(boolean honorCamelWordsSettings) {myCaret.selectWordAtCaret(honorCamelWordsSettings);}

    @Override
    @Nullable
    public Caret clone(boolean above) {return myCaret.clone(above);}

    @Override
    public boolean isAtRtlLocation() {return myCaret.isAtRtlLocation();}

    @Override
    public boolean isAtBidiRunBoundary() {return myCaret.isAtBidiRunBoundary();}

    @Override
    @NotNull
    public <T> T putUserDataIfAbsent(@NotNull Key<T> key, @NotNull T value) {return myCaret.putUserDataIfAbsent(key, value);}

    @Override
    public <T> boolean replace(@NotNull Key<T> key, @Nullable T oldValue, @Nullable T newValue) {return myCaret.replace(key, oldValue, newValue);}

    @Override
    @Nullable
    public <T> T getUserData(@NotNull Key<T> key) {return myCaret.getUserData(key);}

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {myCaret.putUserData(key, value);}

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof CaretEx)) {
            if (!(o instanceof Caret)) return false;
            return myCaret.equals(o);
        }

        CaretEx caretEx = (CaretEx) o;

        return myCaret.equals(caretEx.myCaret);
    }

    @Override
    public int hashCode() {
        return myCaret.hashCode();
    }
}
