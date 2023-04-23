// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.manager;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;

import static com.intellij.openapi.diagnostic.Logger.getInstance;

/**
 * Represents a list of editor carets
 *
 * @see EditorCaret
 */
@SuppressWarnings("WeakerAccess")
public class EditorCaretList extends ArrayList<EditorCaret> {
    private static final Logger LOG = getInstance("com.vladsch.MissingInActions.manager");

    private final LineSelectionManager myManager;

    public EditorCaretList(@NotNull LineSelectionManager manager, int initialCapacity) {
        super(initialCapacity);
        myManager = manager;
    }

    public EditorCaretList(@NotNull LineSelectionManager manager) {
        myManager = manager;
    }

    public EditorCaretList(@NotNull EditorCaretList other) {
        super(other);
        myManager = other.myManager;
    }

    public EditorCaretList(@NotNull LineSelectionManager manager, @NotNull Collection<? extends Caret> c) {
        super(c.size());
        myManager = manager;

        // here we convert
        addAllCarets(c);
    }

    public boolean add(Caret caret) {
        return super.add(myManager.getEditorCaret(caret));
    }

    public boolean addAll(@NotNull EditorCaretList other) {
        return super.addAll(other);
    }

    public boolean addAllCarets(Collection<? extends Caret> c) {
        boolean hadValid = false;
        for (Caret caret : c) {
            if (caret.isValid()) {
                add(myManager.getEditorCaret(caret));
                hadValid = true;
            }
        }

        return hadValid;
    }

    public void commit(boolean removeNotInList) {
        commit(removeNotInList, true, true);
    }

    public EditorCaretList filter(@NotNull Predicate<EditorCaret> predicate) {
        EditorCaretList other = new EditorCaretList(myManager);

        for (EditorCaret editorCaret : this) {
            if (predicate.test(editorCaret)) other.add(editorCaret);
        }

        return other.size() == size() ? this : other;
    }


    public boolean all(@NotNull Predicate<EditorCaret> predicate) {
        for (EditorCaret editorCaret : this) {
            if (!predicate.test(editorCaret)) return false;
        }
        return true;
    }

    public boolean any(@NotNull Predicate<EditorCaret> predicate) {
        for (EditorCaret editorCaret : this) {
            if (predicate.test(editorCaret)) return true;
        }
        return false;
    }

    public boolean none(@NotNull Predicate<EditorCaret> predicate) {
        return !any(predicate);
    }

    @Nullable
    public EditorCaret getPrimaryCaret() {
        for (EditorCaret editorCaret : this) {
            if (editorCaret.isPrimary()) return editorCaret;
        }
        return null;
    }

    public int validSize() {
        int count = 0;
        for (EditorCaret editorCaret : this) {
            if (editorCaret.isValid()) count++;
        }
        return count;
    }

    public void commit(boolean removeNotInList, boolean scrollToCarets, boolean scrollToPrimary) {
        EditorCaret primaryCaret = null;
        CaretModel caretModel = myManager.getEditor().getCaretModel();
        int modelCaretCount = caretModel.getCaretCount();
        HashSet<Caret> keepCaretSet = removeNotInList && (modelCaretCount > size() || modelCaretCount != validSize()) ? new HashSet<>() : null;

        for (EditorCaret editorCaret : this) {
            if (keepCaretSet != null) keepCaretSet.add(editorCaret.getCaret());

            if (editorCaret.isPrimary()) {
                primaryCaret = editorCaret;
            } else {
                editorCaret.commit(scrollToCarets);
            }
        }

        if (primaryCaret != null) {
            primaryCaret.commit(scrollToCarets || scrollToPrimary);
        }

        // remove any carets not in the list
        if (keepCaretSet != null) {
            for (Caret caret : caretModel.getAllCarets()) {
                if (!keepCaretSet.contains(caret)) {
                    caretModel.removeCaret(caret);
                }
            }
        }
    }
}
