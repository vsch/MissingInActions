/*
 * Copyright (c) 2016-2020 Vladimir Schneider <vladimir.schneider@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.vladsch.MissingInActions.util.highlight;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ProjectViewNodeDecorator;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.packageDependencies.ui.PackageDependenciesNode;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.vladsch.MissingInActions.Plugin;
import com.vladsch.MissingInActions.settings.ApplicationSettings;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MiaProjectViewNodeDecorator implements ProjectViewNodeDecorator {
    public static final TextAttributes NULL_ATTRIBUTES = new TextAttributes();
    final ApplicationSettings mySettings;
    final Plugin myPlugin;

    public MiaProjectViewNodeDecorator() {
        mySettings = ApplicationSettings.getInstance();
        myPlugin = Plugin.getInstance();
    }

    @Override
    public void decorate(ProjectViewNode node, PresentationData presentation) {
        if (mySettings.isHighlightProjectViewNodes() && myPlugin.isHighlightsMode()) {
            myPlugin.getHighlightPattern();

            Map<String, Integer> highlightWordFlags = myPlugin.getHighlightRangeFlags();
            if (highlightWordFlags != null) {
                String text = presentation.getPresentableText();
                if (StringUtil.isEmpty(text)) text = node.getValue().toString();
                if (text != null) {
                    int index = myPlugin.getHighlightRangeIndex(text);
                    if (index >= 0) {
                        String range = myPlugin.getAdjustedRange(text);
                        Integer flags = highlightWordFlags.get(range);
                        if (flags != null) {
                            TextAttributes attributes = myPlugin.getHighlightAttributes(index, flags, 0, text.length(), null, null, EffectType.BOLD_DOTTED_LINE, 0);

                            if (attributes != null) {
                                Color backgroundColor = attributes.getBackgroundColor();
                                if (backgroundColor != null) {
                                    // NOTE: background color in PresentableNodeDescriptor.ColoredFragment is not combined with forcedForegroundColor because SimpleTextAttributes.toTextAttributes() passes null for bgColor instead of myBgColor
                                    if (presentation.getColoredText().isEmpty()) {
                                        // use attribute key and text
                                        Color forcedForeground = presentation.getForcedTextForeground();
                                        presentation.setForcedTextForeground(null);

                                        TextAttributes textAttributes;
                                        TextAttributesKey textAttributesKey = presentation.getTextAttributesKey();
                                        presentation.setAttributesKey(null); // remove attributes key
                                        if (textAttributesKey == null) textAttributes = NULL_ATTRIBUTES;
                                        else textAttributes = EditorColorsManager.getInstance().getSchemeForCurrentUITheme().getAttributes(textAttributesKey);

                                        if (forcedForeground == null) forcedForeground = textAttributes.getForegroundColor();

                                        // NOTE: the only way to ensure that custom attribute background is used is to set non-null foreground color
                                        if (forcedForeground == null) forcedForeground = EditorColorsManager.getInstance().getSchemeForCurrentUITheme().getDefaultForeground();

                                        SimpleTextAttributes simpleTextAttributes = new SimpleTextAttributes(backgroundColor, forcedForeground, null, SimpleTextAttributes.STYLE_OPAQUE);
                                        presentation.addText(text, simpleTextAttributes);
                                        String location = presentation.getLocationString();
                                        if (!StringUtil.isEmpty(location)) {
                                            SimpleTextAttributes simpleAttributes = SimpleTextAttributes.merge(simpleTextAttributes, SimpleTextAttributes.GRAYED_ATTRIBUTES);
                                            presentation.addText(presentation.getLocationPrefix() + location + presentation.getLocationSuffix(), simpleAttributes);
                                        }
                                    } else {
                                        List<PresentableNodeDescriptor.ColoredFragment> list = new ArrayList<>(presentation.getColoredText());
                                        presentation.clearText();
                                        presentation.setAttributesKey(null); // remove attributes key
                                        presentation.setForcedTextForeground(null); // remove forced foreground it eliminates background

                                        for (PresentableNodeDescriptor.ColoredFragment fragment : list) {
                                            SimpleTextAttributes textAttributes = fragment.getAttributes();
                                            if (!backgroundColor.equals(textAttributes.getBgColor())) {
                                                presentation.addText(new PresentableNodeDescriptor.ColoredFragment(fragment.getText(), fragment.getToolTip()
                                                        , new SimpleTextAttributes(backgroundColor, textAttributes.getFgColor(), textAttributes.getWaveColor(), textAttributes.getStyle() | SimpleTextAttributes.STYLE_OPAQUE)));
                                            } else {
                                                presentation.addText(fragment);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void decorate(PackageDependenciesNode node, ColoredTreeCellRenderer cellRenderer) {

    }
}
