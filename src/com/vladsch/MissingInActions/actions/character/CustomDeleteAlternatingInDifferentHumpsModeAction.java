// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.character;

import com.vladsch.MissingInActions.actions.DumbAwareTextComponentEditorAction;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.RegExDeleteProvider;

public class CustomDeleteAlternatingInDifferentHumpsModeAction extends DumbAwareTextComponentEditorAction {
    public CustomDeleteAlternatingInDifferentHumpsModeAction() {
        super(new DeleteRegExActionHandler(new RegExProvider(), false, DeleteRegExActionHandler.HumpsMode.INVERT));
    }

    static class RegExProvider implements RegExDeleteProvider {

        @Override
        public String getRegEx() {
            return ApplicationSettings.getInstance().getDeleteAlternatingRegEx();
        }

        @Override
        public boolean isLineBound() {
            return ApplicationSettings.getInstance().isDeleteLineBound();
        }

        @Override
        public boolean isMultiCaretLineBound() {
            return ApplicationSettings.getInstance().isDeleteMultiCaretLineBound();
        }
    }
}
