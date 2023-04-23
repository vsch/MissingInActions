// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.actions.character;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.util.EditHelpers;
import org.jetbrains.annotations.NotNull;

public class ToggleCustomIdentifierModeAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ApplicationSettings settings = ApplicationSettings.getInstance();
        boolean identifierMode = (settings.getCustomizedNextWordBounds() & EditHelpers.MIA_IDENTIFIER) != 0;

        settings.setCustomizedNextWordBounds((settings.getCustomizedNextWordBounds() & ~EditHelpers.MIA_IDENTIFIER) | (!identifierMode ? EditHelpers.MIA_IDENTIFIER : 0));
        settings.setCustomizedNextWordStartBounds((settings.getCustomizedNextWordStartBounds() & ~EditHelpers.MIA_IDENTIFIER) | (!identifierMode ? EditHelpers.MIA_IDENTIFIER : 0));
        settings.setCustomizedNextWordEndBounds((settings.getCustomizedNextWordEndBounds() & ~EditHelpers.MIA_IDENTIFIER) | (!identifierMode ? EditHelpers.MIA_IDENTIFIER : 0));
        settings.setCustomizedPrevWordBounds((settings.getCustomizedPrevWordBounds() & ~EditHelpers.MIA_IDENTIFIER) | (!identifierMode ? EditHelpers.MIA_IDENTIFIER : 0));
        settings.setCustomizedPrevWordStartBounds((settings.getCustomizedPrevWordStartBounds() & ~EditHelpers.MIA_IDENTIFIER) | (!identifierMode ? EditHelpers.MIA_IDENTIFIER : 0));
        settings.setCustomizedPrevWordEndBounds((settings.getCustomizedPrevWordEndBounds() & ~EditHelpers.MIA_IDENTIFIER) | (!identifierMode ? EditHelpers.MIA_IDENTIFIER : 0));
    }
}
