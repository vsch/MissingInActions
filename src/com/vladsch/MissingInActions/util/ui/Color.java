/*
 * Copyright (c) 2016-2018 Vladimir Schneider <vladimir.schneider@gmail.com>
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UseJBColor")
public class Color extends java.awt.Color {
    public static final Color NULL = new Color(new java.awt.Color(0, true));
    public final static Color WHITE = new Color(java.awt.Color.WHITE);
    public final static Color LIGHT_GRAY = new Color(java.awt.Color.LIGHT_GRAY);
    public final static Color GRAY = new Color(java.awt.Color.GRAY);
    public final static Color DARK_GRAY = new Color(java.awt.Color.DARK_GRAY);
    public final static Color BLACK = new Color(java.awt.Color.BLACK);
    public final static Color RED = new Color(java.awt.Color.RED);
    public final static Color PINK = new Color(java.awt.Color.PINK);
    public final static Color ORANGE = new Color(java.awt.Color.ORANGE);
    public final static Color YELLOW = new Color(java.awt.Color.YELLOW);
    public final static Color GREEN = new Color(java.awt.Color.GREEN);
    public final static Color MAGENTA = new Color(java.awt.Color.MAGENTA);
    public final static Color CYAN = new Color(java.awt.Color.CYAN);
    public final static Color BLUE = new Color(java.awt.Color.BLUE);

    protected Color(java.awt.Color other) { super(other.getRGB()); }

    protected Color(int rgb) { super(rgb); }

    @NotNull
    public static Color of(java.awt.Color color) { return new Color(color); }

    public static Color of(int rgb) { return new Color(rgb); }

    @NotNull
    public static Color of(@Nullable String colorName) {
        Integer rgb = ColorStyler.getNamedColor(colorName);
        return rgb == null ? NULL : new Color(rgb);
    }
}
