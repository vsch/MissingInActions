// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
package com.vladsch.MissingInActions.util;

import com.intellij.openapi.application.ApplicationManager;
import com.vladsch.plugin.util.CancelableJobScheduler;

public class MiaCancelableJobScheduler extends CancelableJobScheduler {
    public static MiaCancelableJobScheduler getInstance() {
        return ApplicationManager.getApplication().getService(MiaCancelableJobScheduler.class);
    }
}
