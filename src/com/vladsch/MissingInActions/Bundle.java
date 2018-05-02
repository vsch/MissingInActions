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

package com.vladsch.MissingInActions;

import com.intellij.CommonBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.PropertyKey;

import java.util.ResourceBundle;

public class Bundle {

    @NonNls
    private static final String BUNDLE_NAME = "com.vladsch.MissingInActions.localization.strings";

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    private Bundle() {
    }

    public static ResourceBundle getBundle() {
        return BUNDLE;
    }

    public static String getString(String key, Object... params) {
        return CommonBundle.message(BUNDLE, key, params);
    }

    public static String message(@PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... params) {
        return CommonBundle.message(BUNDLE, key, params);
    }

    @Nullable
    public static String messageOrNull(
            @NotNull ResourceBundle bundle, @NotNull String key,
            @NotNull Object... params
    ) {
        final String value = CommonBundle.messageOrDefault(bundle, key, key, params);
        if (key.equals(value)) return null;
        return value;
    }

    public static String indexedMessage(@PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... params) {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        int index = 0;
        while (true) {
            String message = messageOrNull(BUNDLE, index++ > 0 ? String.format("%s-%d", key, index) : key, params);
            if (message == null) {
                if (index > 0) break;
            } else {
                sb.append(sep).append(message);
                sep = "\n";
            }
        }
        return sb.toString();
    }

    public static String messageOrBlank(@PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... params) {
        return CommonBundle.messageOrDefault(BUNDLE, key, "", params);
    }
}
