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

package com.vladsch.MissingInActions

import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.xml.util.XmlStringUtil

object PluginNotifications {

    val NOTIFICATION_GROUP_UPDATE = NotificationGroup("MissingInActions Update", NotificationDisplayType.STICKY_BALLOON, true, null)
    val NOTIFICATION_GROUP_ACTION = NotificationGroup("MissingInActions File Action", NotificationDisplayType.STICKY_BALLOON, true, null)
    val NOTIFICATION_GROUP_ACTION_ERRORS = NotificationGroup("MissingInActions File Action with errors", NotificationDisplayType.STICKY_BALLOON, true, null)
    val NOTIFICATION_GROUP_DEFAULT = NOTIFICATION_GROUP_ACTION

    val DEFAULT_STAR_ATTR = "BUY"
    val DEFAULT_PLUS_ATTR = "ENHANCED"
    val DEFAULT_TILDE_ATTR = "SPECIALS"

    fun applyHtmlColors(htmlText: String): String {
        val isDarkUITheme = UIUtil.isUnderDarcula()
        val enhColor = if (isDarkUITheme) "#B0A8E6" else "#6106A5"
        val buyColor = if (isDarkUITheme) "#F0A8D4" else "#C02080"
        val specialsColor = if (isDarkUITheme) "#A4EBC5" else "#04964F"
        return htmlText.replace("[[ENHANCED]]", enhColor).replace("[[BUY]]", buyColor).replace("[[SPECIALS]]", specialsColor)
    }

    fun processDashStarList(featureList: String, titleHtml: String? = null, enhAttr: String = DEFAULT_STAR_ATTR): String {
        val features = processDashStarPage(processDashStarItems(featureList,enhAttr), titleHtml, null)
        return applyHtmlColors(features)
    }

    fun processDashStarPage(featuresListHtml: String, titleHtml: String? = null, subTitleHtml: String?): String {
        val subTitle = subTitleHtml ?: ""
        val features = featuresListHtml.wrapWith((if (titleHtml != null && !titleHtml.isEmpty()) """
<h4 style="margin: 0; font-size: ${JBUI.scale(10)}px">$titleHtml</h4>""" else "")+"""
<div style="margin-top: 0; margin-left: ${JBUI.scale(10)}px; margin-bottom: 0; font-size: ${JBUI.scale(9)}px">$subTitle</div>
<ul style="margin-left: ${JBUI.scale(10)}px;">
""", "</ul>")
        return applyHtmlColors(features)
    }

    fun processDashStarItems(featureList: String, starAttr: String = DEFAULT_STAR_ATTR, plusAttr: String = DEFAULT_PLUS_ATTR, tildeAttr: String = DEFAULT_TILDE_ATTR): String {
        //        val featureList = """
        //- Preferences now under <b>Languages & Frameworks</b>
        //- Improved preview update performance
        //- HTML Text tab now has all links as URI's
        //- Preview resolves upsource:// links to upsource server
        //- Dynamic page width setting to stylesheet preferences
        //- New Zoom factor implemented in swing browser for font size
        //* New <span style="color: [[BUY]]"><b>Split Editor</b></span> with preview and HTML Text
        //* Support for <span style="color: [[BUY]]">Open JavaFX</span> with JetBrains bundled JRE on <span style="color: [[BUY]]">OS X</span>
        //* New per file layout and preview/HTML text mode
        //* New CSS & HTML customization options
        //* Shortcuts change Layout and Preview/HTML Text modes
        //* Refactor all link address formats: relative, /, https://, file://
        //* Validates all link address formats: relative, /, https://, file://
        //* Table of Contents Markdown extension [TOC level=<i>N</i>]
        //"""
        val features = featureList.split('\n').fold("") { accum, elem ->
            val item = elem.trim()
            accum + (
                    if (item.startsWith('*')) item.removePrefix("*").trim().wrapWith("<li style=\"color: [[$starAttr]]\"><b>", "</b></li>")
                    else if (item.startsWith('+')) item.removePrefix("+").trim().wrapWith("<li style=\"color: [[$plusAttr]]\"><b>", "</b></li>")
                    else if (item.startsWith('~')) item.removePrefix("~").trim().wrapWith("<li style=\"color: [[$tildeAttr]]\"><b>", "</b></li>")
                    else item.removePrefix("-").trim().wrapWith("<li>", "</li>")
                    )
        }

        return features
    }

    fun makeNotification(message: String,
                         title: String = Bundle.message("plugin.name") + " (" + Plugin.productVersion + ")",
                         listener: NotificationListener? = null,
                         notificationType: NotificationType = NotificationType.INFORMATION,
                         issueNotificationGroup: NotificationGroup = NOTIFICATION_GROUP_DEFAULT,
                         project: Project? = null
    ) {

        val basicListener = listener ?: NotificationListener { notification, hyperlinkEvent ->
            //notification.expire();
            if (hyperlinkEvent.url != null) {
                BrowserUtil.browse(hyperlinkEvent.url.toString())
            }
        }

        issueNotificationGroup.createNotification(title, XmlStringUtil.wrapInHtml(message), notificationType, basicListener).notify(project)
    }
}

