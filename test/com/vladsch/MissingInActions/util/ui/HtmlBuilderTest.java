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

package com.vladsch.MissingInActions.util.ui;

import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("UseJBColor")
public class HtmlBuilderTest {

    @Test
    public void test_Basic() throws Exception {
        final HtmlBuilder fa = new HtmlBuilder(2, true);
        fa.tagIndent("ul", new Runnable() {
            @Override
            public void run() {
                fa.withCondIndent().tagLine("li", new Runnable() {
                    @Override
                    public void run() {
                        fa.text("item1");
                    }
                });
            }
        });
        assertEquals("<ul>\n  <li>item1</li>\n</ul>\n", fa.toFinalizedString());

        final HtmlBuilder fa1 = new HtmlBuilder(2, true);
        fa1.tagIndent("ul", new Runnable() {
            @Override
            public void run() {
                fa1.withCondIndent().tagLine("li", new Runnable() {
                    @Override
                    public void run() {
                        fa1.text("item1");
                        fa1.tagIndent("ul", new Runnable() {
                            @Override
                            public void run() {
                                fa1.withCondIndent().tagLine("li", new Runnable() {
                                    @Override
                                    public void run() {
                                        fa1.text("item1");
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
        assertEquals("<ul>\n  <li>item1\n    <ul>\n      <li>item1</li>\n    </ul>\n  </li>\n</ul>\n", fa1.toFinalizedString());

        final HtmlBuilder fa2 = new HtmlBuilder(2, true);
        fa2.withCondLine().tagIndent("tbody", new Runnable() {
            @Override
            public void run() {

            }
        });
        assertEquals("<tbody></tbody>\n", fa2.toFinalizedString());

        HtmlBuilder fa3 = new HtmlBuilder(2, true);
        fa3.attr("style", "color:#ff0000").span();
        assertEquals("<span style=\"color:#ff0000\"></span>\n", fa3.toFinalizedString());

        fa3 = new HtmlBuilder(2, true);
        fa3.attr("style", "color:#ff0000").attr("style", "color:#00ff00").span();
        assertEquals("<span style=\"color:#00ff00\"></span>\n", fa3.toFinalizedString());

        fa3 = new HtmlBuilder(2, true);
        fa3.attr("style", "color:#ff0000").attr("style", "color:#00ff00").span().closeSpan();
        assertEquals("<span style=\"color:#00ff00\"></span>\n", fa3.toFinalizedString());
    }

    @Test
    public void test_Attr() throws Exception {
        HtmlBuilder fa;

        fa = new HtmlBuilder(2, true);
        //noinspection UseJBColor
        fa.attr(Color.RED).span();
        assertEquals("<span style=\"color:#ff0000;\"></span>\n", fa.toFinalizedString());

        fa = new HtmlBuilder(2, true);
        fa.attr(Color.RED).attr(Color.GREEN).span();
        assertEquals("<span style=\"color:#00ff00;\"></span>\n", fa.toFinalizedString());

        fa = new HtmlBuilder(2, true);
        fa.attr(Color.RED).attr(Color.GREEN).span().closeSpan();
        assertEquals("<span style=\"color:#00ff00;\"></span>\n", fa.toFinalizedString());

        fa = new HtmlBuilder(2, true);
        fa.attr(Color.RED).attr(BackgroundColor.GREEN).span().closeSpan();
        assertEquals("<span style=\"color:#ff0000;background-color:#00ff00;\"></span>\n", fa.toFinalizedString());
    }

    @Test
    public void test_Font() throws Exception {
        final Font font = Font.decode(null);
        HtmlBuilder fa;

        fa = new HtmlBuilder(2, true);
        fa.attr(font).span();
        assertEquals("<span style=\"font-family:Dialog;font-size:12pt;font-style:normal;font-weight:normal;\"></span>\n", fa.toFinalizedString());

        fa = new HtmlBuilder(2, true);
        fa.attr(font).attr(FontStyle.PLAIN).span();
        assertEquals("<span style=\"font-family:Dialog;font-size:12pt;font-style:normal;font-weight:normal;\"></span>\n", fa.toFinalizedString());

        fa = new HtmlBuilder(2, true);
        fa.attr(font).attr(FontStyle.PLAIN).attr(FontStyle.BOLD).span();
        assertEquals("<span style=\"font-family:Dialog;font-size:12pt;font-style:normal;font-weight:bold;\"></span>\n", fa.toFinalizedString());

        fa = new HtmlBuilder(2, true);
        fa.attr(font).attr(FontStyle.PLAIN).attr(FontStyle.ITALIC).span();
        assertEquals("<span style=\"font-family:Dialog;font-size:12pt;font-style:italic;font-weight:normal;\"></span>\n", fa.toFinalizedString());

        fa = new HtmlBuilder(2, true);
        fa.attr(font).attr(FontStyle.PLAIN).attr(FontStyle.BOLD_ITALIC).span();
        assertEquals("<span style=\"font-family:Dialog;font-size:12pt;font-style:italic;font-weight:bold;\"></span>\n", fa.toFinalizedString());

        fa = new HtmlBuilder(2, true);
        fa.attr(font).attr(FontStyle.BOLD).span();
        assertEquals("<span style=\"font-family:Dialog;font-size:12pt;font-style:normal;font-weight:bold;\"></span>\n", fa.toFinalizedString());

        fa = new HtmlBuilder(2, true);
        fa.attr(font).attr(FontStyle.BOLD).attr(FontStyle.PLAIN).span();
        assertEquals("<span style=\"font-family:Dialog;font-size:12pt;font-style:normal;font-weight:normal;\"></span>\n", fa.toFinalizedString());

        fa = new HtmlBuilder(2, true);
        fa.attr(font).attr(FontStyle.BOLD).attr(FontStyle.ITALIC).span();
        assertEquals("<span style=\"font-family:Dialog;font-size:12pt;font-style:italic;font-weight:normal;\"></span>\n", fa.toFinalizedString());

        fa = new HtmlBuilder(2, true);
        fa.attr(font).attr(FontStyle.BOLD).attr(FontStyle.BOLD_ITALIC).span();
        assertEquals("<span style=\"font-family:Dialog;font-size:12pt;font-style:italic;font-weight:bold;\"></span>\n", fa.toFinalizedString());

        fa = new HtmlBuilder(2, true);
        fa.attr(font).attr(FontStyle.ITALIC).span();
        assertEquals("<span style=\"font-family:Dialog;font-size:12pt;font-style:italic;font-weight:normal;\"></span>\n", fa.toFinalizedString());

        fa = new HtmlBuilder(2, true);
        fa.attr(font).attr(FontStyle.ITALIC).attr(FontStyle.PLAIN).span();
        assertEquals("<span style=\"font-family:Dialog;font-size:12pt;font-style:normal;font-weight:normal;\"></span>\n", fa.toFinalizedString());

        fa = new HtmlBuilder(2, true);
        fa.attr(font).attr(FontStyle.ITALIC).attr(FontStyle.BOLD).span();
        assertEquals("<span style=\"font-family:Dialog;font-size:12pt;font-style:normal;font-weight:bold;\"></span>\n", fa.toFinalizedString());

        fa = new HtmlBuilder(2, true);
        fa.attr(font).attr(FontStyle.ITALIC).attr(FontStyle.BOLD_ITALIC).span();
        assertEquals("<span style=\"font-family:Dialog;font-size:12pt;font-style:italic;font-weight:bold;\"></span>\n", fa.toFinalizedString());

        fa = new HtmlBuilder(2, true);
        fa.attr(font).attr(FontStyle.BOLD_ITALIC).span();
        assertEquals("<span style=\"font-family:Dialog;font-size:12pt;font-style:italic;font-weight:bold;\"></span>\n", fa.toFinalizedString());

        fa = new HtmlBuilder(2, true);
        fa.attr(font).attr(FontStyle.BOLD_ITALIC).attr(FontStyle.PLAIN).span();
        assertEquals("<span style=\"font-family:Dialog;font-size:12pt;font-style:normal;font-weight:normal;\"></span>\n", fa.toFinalizedString());

        fa = new HtmlBuilder(2, true);
        fa.attr(font).attr(FontStyle.BOLD_ITALIC).attr(FontStyle.BOLD).span();
        assertEquals("<span style=\"font-family:Dialog;font-size:12pt;font-style:normal;font-weight:bold;\"></span>\n", fa.toFinalizedString());

        fa = new HtmlBuilder(2, true);
        fa.attr(font).attr(FontStyle.BOLD_ITALIC).attr(FontStyle.ITALIC).span();
        assertEquals("<span style=\"font-family:Dialog;font-size:12pt;font-style:italic;font-weight:normal;\"></span>\n", fa.toFinalizedString());

        fa = new HtmlBuilder(2, true);
        fa.attr(font).attr(FontStyle.BOLD).attr(FontStyle.ITALIC).attr(Color.DARK_GRAY).attr(BackgroundColor.LIGHT_GRAY).span();
        assertEquals("<span style=\"font-family:Dialog;font-size:12pt;font-style:italic;font-weight:normal;color:#404040;background-color:#c0c0c0;\"></span>\n", fa.toFinalizedString());
    }
}
