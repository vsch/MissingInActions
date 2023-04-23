// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
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
        String message = CommonBundle.messageOrNull(BUNDLE, key, params);
        return message == null ? key : message;
    }

    public static String message(@PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... params) {
        String message = CommonBundle.messageOrNull(BUNDLE, key, params);
        return message == null ? key : message;
    }

    @Nullable
    public static String messageOrNull(@NotNull ResourceBundle bundle, @NotNull String key, @NotNull Object... params) {
        return CommonBundle.messageOrNull(bundle, key, params);
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
        String message = CommonBundle.messageOrNull(BUNDLE, key, params);
        return message == null ? "" : message;
    }
}
