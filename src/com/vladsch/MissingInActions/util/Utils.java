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

package com.vladsch.MissingInActions.util;

import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.JBColor;
import com.vladsch.MissingInActions.util.ui.BackgroundColor;
import com.vladsch.MissingInActions.util.ui.HtmlBuilder;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.Color;
import java.awt.Font;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({ "WeakerAccess", "SameParameterValue" })
public class Utils {

    public static String toRgbString(Color color) {
        return (color == null) ? "rgb(0,0,0)" : "rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")";
    }

    public static String withContext(String text, String context, int pos, String prefix, String suffix) {
        StringBuilder sb = new StringBuilder();
        sb.append(text).append('\n');
        sb.append(prefix).append(context).append(suffix).append('\n');
        for (int i = 1; i < prefix.length(); i++) sb.append(' ');
        sb.append('^').append('\n');
        return sb.toString();
    }

    public static Color errorColor() {
        TextAttributes attribute = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.ERRORS_ATTRIBUTES);
        Color color = JBColor.RED;
        if (attribute != null) {
            if (attribute.getForegroundColor() != null) {
                color = attribute.getForegroundColor();
            } else if (attribute.getEffectColor() != null) {
                color = attribute.getEffectColor();
            } else if (attribute.getErrorStripeColor() != null) {
                color = attribute.getErrorStripeColor();
            }
        }
        return color;
    }

    public static Color warningColor() {
        TextAttributes attribute = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.WARNINGS_ATTRIBUTES);
        Color color = JBColor.ORANGE;
        if (attribute != null) {
            if (attribute.getForegroundColor() != null) {
                color = attribute.getForegroundColor();
            } else if (attribute.getEffectColor() != null) {
                color = attribute.getEffectColor();
            } else if (attribute.getErrorStripeColor() != null) {
                color = attribute.getErrorStripeColor();
            }
        }
        return color;
    }

    public static float min(float... values) {
        float value = values.length > 0 ? values[0] : 0;
        for (float v : values) {
            if (value < v) value = v;
        }
        return value;
    }

    public static float max(float... values) {
        float value = values.length > 0 ? values[0] : 0;
        for (float v : values) {
            if (value > v) value = v;
        }
        return value;
    }

    public static float rangeLimit(float value, float min, float max) {
        return max(min(value, max), min);
    }

    public static Color mixedColor(Color originalColor, Color overlayColor) {
        final float[] hsbColor = Color.RGBtoHSB(originalColor.getRed(), originalColor.getGreen(), originalColor.getBlue(), new float[3]);
        ;
        final float[] hsbError = Color.RGBtoHSB(overlayColor.getRed(), overlayColor.getGreen(), overlayColor.getBlue(), new float[3]);
        final float[] hsbMixed = new float[3];

        hsbMixed[0] = hsbError[0];
        hsbMixed[1] = min(max(rangeLimit(hsbColor[1], hsbError[1], 0.3f), 0.5f), 1.0f);
        hsbMixed[2] = min(max(rangeLimit(hsbColor[2], hsbError[2], 0.3f), 0.5f), 1.0f);
        Color errorColor = Color.getHSBColor(hsbMixed[0], hsbMixed[1], hsbMixed[2]);
        return errorColor;
    }

    public static Color errorColor(Color color) {
        return mixedColor(color, errorColor());
    }

    public static Color warningColor(Color color) {
        return mixedColor(color, warningColor());
    }

    public static String join(String[] items, String prefix, String suffix, String itemPrefix, String itemSuffix) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        for (String item : items) {
            sb.append(itemPrefix).append(item).append(itemSuffix);
        }
        sb.append(suffix);
        return sb.toString();
    }

    public static String join(Collection<String> items, String prefix, String suffix, String itemPrefix, String itemSuffix) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        for (String item : items) {
            sb.append(itemPrefix).append(item).append(itemSuffix);
        }
        sb.append(suffix);
        return sb.toString();
    }

    public static String repeat(String text, int repeatCount) {
        if (repeatCount > 0) {
            StringBuilder sb = new StringBuilder(text.length() * repeatCount);
            while (repeatCount-- > 0) {
                sb.append(text);
            }
            return sb.toString();
        }
        return "";
    }

    @Nullable
    @Contract("!null, _ -> !null; null, _-> null")
    public static String toHtmlError(@Nullable String err, boolean withContext) {
        if (err == null) return null;

        if (withContext) {
            Matcher matcher = Pattern.compile("(?:^|\n)(.*\n)(\\s*)\\^(\n?)$").matcher(err);
            if (matcher.find()) {
                String group = matcher.group(2);
                if (group != null && !group.isEmpty()) {
                    int prevLineStart = matcher.group(1) != null ? matcher.start(1) : matcher.start(2);
                    String lastLine = repeat("&nbsp;", group.length());
                    err = err.substring(0, prevLineStart) + "<span style=\"font-family:monospaced\">" + err.substring(prevLineStart, matcher.start(2)).replace(" ", "&nbsp;") + lastLine + "^</span>" + group;
                }
            }
        }
        return err.replace("\n", "<br>");
    }

    public static void setRegExError(String error, JTextPane jTextPane, final Font textFont, final BackgroundColor validTextFieldBackground, final BackgroundColor warningTextFieldBackground) {
        HtmlBuilder html = new HtmlBuilder();
        html.tag("html").style("margin:2px;vertical-align:middle;").attr(validTextFieldBackground, textFont).tag("body");
        //noinspection ConstantConditions
        html.attr(warningTextFieldBackground).tag("div");
        html.append(toHtmlError(error, true));
        html.closeTag("div");
        html.closeTag("body");
        html.closeTag("html");

        jTextPane.setVisible(true);
        jTextPane.setText(html.toFinalizedString());
        jTextPane.revalidate();
        jTextPane.getParent().revalidate();
        jTextPane.getParent().getParent().revalidate();

    }
}

