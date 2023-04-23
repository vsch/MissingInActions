// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.util.ui.UIUtil;
import com.vladsch.MissingInActions.actions.ActionUtils;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.flexmark.util.html.ui.BackgroundColor;
import com.vladsch.plugin.util.ui.Helpers;
import com.vladsch.plugin.util.ui.TextFieldAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.regex.Pattern;

public class PatternAction extends TextFieldAction {
    protected PatternAction() {
        super();
    }

    protected PatternAction(final String text) {
        super(text);
    }

    protected PatternAction(final String text, final String description, final Icon icon) {
        super(text, description, icon);
    }

    @Override
    protected void updateOnFocusLost(final String text, final Presentation presentation) {

    }

    @Override
    protected void updateOnFocusGained(final String text, final Presentation presentation) {

    }

    @Override
    protected void updateOnTextChange(final String text, final Presentation presentation) {
        try {
            Pattern pattern = Pattern.compile(text);
            presentation.putClientProperty(TEXT_FIELD_BACKGROUND_KEY, null);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            presentation.setDescription(e.getMessage());
            presentation.putClientProperty(TEXT_FIELD_BACKGROUND_KEY, BackgroundColor.of(Helpers.errorColor(UIUtil.getTextFieldBackground())));
        }
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        final EditorEx editor = ActionUtils.getEditor(e);
        boolean enabled = false;

        if (editor != null) {
            LineSelectionManager manager = LineSelectionManager.getInstance(editor);
            RangeLimitedCaretSpawningHandler spawningHandler = manager.getCaretSpawningHandler();
            if (spawningHandler != null) {
                enabled = true;
                presentation.setText(spawningHandler.getPattern());
            }
        }

        if (!enabled) {
            presentation.setText("RegEx");
        }
        presentation.setEnabled(enabled);
        super.update(e);

        e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        // apply new pattern
        Presentation presentation = e.getPresentation();
        String text = presentation.getText();
        if (text != null && !text.isEmpty()) {
            final EditorEx editor = ActionUtils.getEditor(e);
            if (editor != null) {
                LineSelectionManager manager = LineSelectionManager.getInstance(editor);
                final RangeLimitedCaretSpawningHandler spawningHandler = manager.getCaretSpawningHandler();
                if (spawningHandler != null) {
                    try {
                        Pattern pattern = Pattern.compile(text);
                        spawningHandler.setPattern(text);

                        // rerun on new caret position after action
                        manager.guard(() -> {
                            spawningHandler.doAction(manager, editor, null, null);
                        });
                    } catch (IllegalArgumentException | IndexOutOfBoundsException ignored) {
                    }
                }
            }
        }
    }
}
