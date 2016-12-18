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

package com.vladsch.MissingInActions.util

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.JBColor
import org.jetbrains.annotations.Contract
import java.awt.Color

inline fun Logger.debug(lazyMessage: () -> String) {
    if (this.isDebugEnabled) this.debug(lazyMessage())
}

fun Color?.toRgbString(): String {
    return if (this == null) "rgb(0,0,0)" else "rgb(" + red + "," + green + "," + blue + ")"
}

fun String.withContext(context: String, pos: Int, prefix: String = "", suffix: String = ""): String {
    val sb = StringBuilder()
    sb.append(this).append('\n')
    sb.append(prefix).append(context).append(suffix).append('\n')
    for (i in 1..pos + prefix.length) sb.append(' ')
    sb.append('^').append('\n')
    return sb.toString()
}

@JvmOverloads
@Contract("!null->!null, null->null")
fun String?.toHtmlError(withContext: Boolean = true): String? {
    var err = this ?: return null
    if (withContext) {
        val match = "(?:^|\n)(.*\n)(\\s*)\\^(\n?)$".toRegex().find(err)
        val group = match?.groups?.get(2)
        if (group != null && !group.range.isEmpty()) {
            val prevLineStart = match?.groups?.get(1)?.range?.start ?: group.range.start
            val lastLine = "&nbsp;".repeat(group.range.endInclusive + 1 - group.range.start)
            err = err.substring(0, prevLineStart) + "<span style=\"font-family:monospaced\">" + err.substring(prevLineStart, group.range.start) + lastLine + "^</span>" + (match?.groupValues?.get(2) ?: "")
        }
    }
    return err.replace("\n", "<br>")
}

fun errorColor(): Color {
    val attribute = EditorColorsManager.getInstance().globalScheme.getAttributes(CodeInsightColors.ERRORS_ATTRIBUTES)
    val color = attribute?.foregroundColor ?: attribute?.effectColor ?: attribute?.errorStripeColor ?: JBColor.RED
    return color
}

fun errorColor(color: Color): Color {
    return mixedColor(color, errorColor())
}

fun warningColor(): Color {
    val attribute = EditorColorsManager.getInstance().globalScheme.getAttributes(CodeInsightColors.WARNINGS_ATTRIBUTES)
    val color = attribute?.foregroundColor ?: attribute?.effectColor ?: attribute?.errorStripeColor ?: JBColor.ORANGE
    return color;
}

fun warningColor(color: Color): Color {
    return mixedColor(color, warningColor())
}

fun mixedColor(originalColor: Color, overlayColor: Color): Color {
    val hsbColor = Color.RGBtoHSB(originalColor.red, originalColor.green, originalColor.blue, FloatArray(3))
    val hsbError = Color.RGBtoHSB(overlayColor.red, overlayColor.green, overlayColor.blue, FloatArray(3))
    val hsbMixed = FloatArray(3)

    hsbMixed[0] = hsbError[0];
    hsbMixed[1] = hsbColor[1].rangeLimit(hsbError[1].max(0.3f).min(0.5f), 1.0f)
    hsbMixed[2] = hsbColor[2].rangeLimit(hsbError[2].max(0.3f).min(0.5f), 1.0f)
    val errorColor = Color.getHSBColor(hsbMixed[0], hsbMixed[1], hsbMixed[2])
    return errorColor
}

