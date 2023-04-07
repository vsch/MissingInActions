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
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.SimpleTextAttributes;
import com.vladsch.MissingInActions.Plugin;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.plugin.util.ui.highlight.WordHighlightProvider;

import java.awt.Color;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            WordHighlightProvider<ApplicationSettings> wordHighlightProvider = myPlugin.getActiveHighlightProvider();
            Pattern pattern = wordHighlightProvider.getHighlightPattern();
            Map<String, Integer> highlightWordFlags = wordHighlightProvider.getHighlightRangeFlags();

            if (highlightWordFlags != null && pattern != null) {
                String text = presentation.getPresentableText();
                if (StringUtil.isEmpty(text)) text = node.getValue().toString();
                if (text != null) {
                    // NOTE: background color in PresentableNodeDescriptor.ColoredFragment is not combined with forcedForegroundColor because SimpleTextAttributes.toTextAttributes() passes null for bgColor instead of myBgColor
                    Matcher matcher = pattern.matcher(text);

                    if (matcher.find()) {
                        // clear previous coloring. Use only highlight word colors.
                        presentation.clearText();

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
                        int style = SimpleTextAttributes.STYLE_BOLD; //node instanceof PsiDirectoryNode ? SimpleTextAttributes.STYLE_BOLD : 0;
                        SimpleTextAttributes plainTextAttributes = new SimpleTextAttributes(null, forcedForeground, null, 0);

                        int lastOffset = 0;
                        do {
                            String group = matcher.group();
                            int startOffset = matcher.start();
                            int endOffset = matcher.end();

                            String range = wordHighlightProvider.getAdjustedRange(group);
                            int flags = highlightWordFlags.getOrDefault(range, 0);
                            int index = wordHighlightProvider.getHighlightRangeIndex(range);

                            TextAttributes attributes = wordHighlightProvider.getHighlightAttributes(index, flags, 0, text.length(), null, null, EffectType.BOLD_DOTTED_LINE, 0);

                            if (attributes != null) {
                                if (lastOffset < startOffset) {
                                    presentation.addText(text.substring(lastOffset, startOffset), plainTextAttributes);
                                }

                                Color backgroundColor = attributes.getBackgroundColor();
                                SimpleTextAttributes simpleTextAttributes = new SimpleTextAttributes(backgroundColor, forcedForeground, null, SimpleTextAttributes.STYLE_OPAQUE | style);
                                presentation.addText(group, simpleTextAttributes);
                            } else {
                                presentation.addText(text.substring(lastOffset, endOffset), plainTextAttributes);
                            }
                            lastOffset = endOffset;
                        } while (matcher.find());

                        if (lastOffset < text.length()) {
                            presentation.addText(text.substring(lastOffset), plainTextAttributes);
                        }

                        String location = presentation.getLocationString();
                        if (!StringUtil.isEmpty(location)) {
                            SimpleTextAttributes simpleAttributes = SimpleTextAttributes.merge(plainTextAttributes, SimpleTextAttributes.GRAYED_ATTRIBUTES);
                            presentation.addText(presentation.getLocationPrefix() + location + presentation.getLocationSuffix(), simpleAttributes);
                        }
                    }
                }
            }
        }
    }
}
