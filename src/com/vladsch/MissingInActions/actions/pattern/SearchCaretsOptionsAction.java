// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.pattern;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.ex.EditorEx;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.settings.RegExSettingsHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SearchCaretsOptionsAction extends EditorAction {
    public SearchCaretsOptionsAction() {
        super(new Handler());
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        super.update(e);
        e.getPresentation().setVisible(!ApplicationSettings.getInstance().isHideDisabledButtons() || e.getPresentation().isEnabled());
    }

    private static class Handler extends EditorActionHandler {
        public Handler() {
            super(false);
        }

        @Override
        protected boolean isEnabledForCaret(@NotNull final Editor editor, @NotNull final Caret caret, final DataContext dataContext) {
            return LineSelectionManager.getInstance(editor).getCaretSpawningHandler() instanceof CaretSpawningSearchHandler;
        }

        @Override
        protected void doExecute(@NotNull final Editor editor, @Nullable final Caret caret, final DataContext dataContext) {
            RangeLimitedCaretSpawningHandler spawningHandler = LineSelectionManager.getInstance(editor).getCaretSpawningHandler();

            if (spawningHandler instanceof CaretSpawningSearchHandler) {
                final CaretSpawningSearchHandler[] handler = { (CaretSpawningSearchHandler) spawningHandler };
                LineSelectionManager manager = LineSelectionManager.getInstance(editor);
                boolean valid = SearchCaretsOptionsDialog.showDialog(editor.getComponent(), new RegExSettingsHolder() {
                    @NotNull
                    @Override
                    public String getPatternText() {
                        return handler[0].getPattern();
                    }

                    @NotNull
                    @Override
                    public String getSampleText() {
                        StringBuilder sb = new StringBuilder();
                        Document document = editor.getDocument();
                        CharSequence chars = document.getCharsSequence();
                        String prefix = "";
                        for (Caret caret1 : editor.getCaretModel().getAllCarets()) {
                            int line = caret1.getLogicalPosition().line;
                            int start = document.getLineStartOffset(line);
                            int end = document.getLineEndOffset(line);
                            sb.append(prefix);
                            sb.append(chars, start, end).append("\n");
                            prefix = ApplicationSettings.PREFIX_DELIMITER;
                        }

                        return sb.toString();
                    }

                    @Override
                    public void setPatternText(final String pattern) {
                        handler[0].setPattern(pattern);
                    }

                    @Override
                    public void setSampleText(final String sampleText) {

                    }

                    @Override
                    public boolean isCaseSensitive() {
                        return handler[0].isCaseSensitive();
                    }

                    @Override
                    public boolean isBackwards() {
                        return handler[0].isBackwards();
                    }

                    @Override
                    public boolean isCaretToGroupEnd() {
                        return handler[0].isCaretToEndGroup();
                    }

                    @Override
                    public void setCaretToGroupEnd(final boolean isCaretToGroupEnd) {
                        if (handler[0].isCaretToEndGroup() != isCaretToGroupEnd) {
                            changeHandler(null).setCaretToEndGroup(isCaretToGroupEnd);
                        }
                    }

                    @Override
                    public void setCaseSensitive(final boolean isCaseSensitive) {
                        if (handler[0].isCaseSensitive() != isCaseSensitive) {
                            changeHandler(null).setCaseSensitive(isCaseSensitive);
                        }
                    }

                    public CaretSpawningSearchHandler changeHandler(Boolean isBackwards) {
                        CaretSpawningSearchHandler searchHandler = new CaretSpawningSearchHandler(isBackwards == null ? handler[0].isBackwards() : isBackwards);
                        searchHandler.copySettings(handler[0], editor);
                        handler[0] = searchHandler;
                        manager.setSearchFoundCaretSpawningHandler(searchHandler);
                        return searchHandler;
                    }

                    @Override
                    public void setBackwards(final boolean isBackwards) {
                        if (handler[0].isBackwards() != isBackwards) {
                            changeHandler(isBackwards);
                        }
                    }
                }, (EditorEx) editor);

                // everything is set, ActionSelectionAdjuster will do the rest
                if (valid) {

                }
            }
        }
    }
}
