/*
 * Copyright (c) 2016-2019 Vladimir Schneider <vladimir.schneider@gmail.com>
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

package com.vladsch.MissingInActions.util;

import com.intellij.codeInsight.editorActions.TextBlockTransferable;
import com.intellij.ide.CopyPasteManagerEx;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.CaretStateTransferableData;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.SystemInfo;
import com.vladsch.plugin.util.clipboard.AugmentedTextBlockTransferable;
import com.vladsch.plugin.util.clipboard.TextBlockDataFlavorRegistrar;
import org.jetbrains.annotations.Nullable;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class SharedCaretStateTransferableData {
    private static final Logger LOG = Logger.getInstance("com.vladsch.plugin.util.clipboard");

    private static final DataFlavor ORIGINAL_FLAVOR = CaretStateTransferableData.FLAVOR;
    private static DataFlavor ourFlavor = null;
    private static final Object ourFlavorLock = new Object();

    private static boolean inReplaceContent = false;

    private static final CopyPasteManager.ContentChangedListener ourContentChangedListener = new CopyPasteManager.ContentChangedListener() {
        @Override
        public void contentChanged(@Nullable final Transferable oldTransferable, final Transferable newTransferable) {
            replaceClipboardIfNeeded();
        }
    };

    private static void replaceClipboardIfNeeded() {
        if (!inReplaceContent) {
            inReplaceContent = true;

            if (LOG.isDebugEnabled()) LOG.debug("Entering replaceClipboardIfNeeded update");

            Transferable transferable = CopyPasteManager.getInstance().getContents();

            if (transferable != null &&
                    !(transferable instanceof AugmentedTextBlockTransferable) &&
                    !(transferable instanceof TextBlockTransferable) &&
                    transferable.isDataFlavorSupported(DataFlavor.stringFlavor) &&
                    transferable.isDataFlavorSupported(CaretStateTransferableData.FLAVOR)) {
                // see if have other flavours on the clipboard 
                Transferable newTransferable = TextBlockDataFlavorRegistrar.getInstance().augmentTransferable(transferable);
                if (LOG.isDebugEnabled()) LOG.debug("Replacing clipboard content with " + newTransferable);

                ApplicationManager.getApplication().invokeLater(() -> {
                    CopyPasteManagerEx.getInstanceEx().setContents(newTransferable);
                    inReplaceContent = false;
                    if (LOG.isDebugEnabled()) LOG.debug("Exiting replaceClipboardIfNeeded update done.");
                }, ModalityState.any());
            } else {
                if (LOG.isDebugEnabled()) LOG.debug("Exiting replaceClipboardIfNeeded update not needed");
                inReplaceContent = false;
            }
        }
    }

    @Nullable
    private static Clipboard getSystemClipboard() {
        try {
            return Toolkit.getDefaultToolkit().getSystemClipboard();
        } catch (IllegalStateException e) {
            if (SystemInfo.isWindows) {
                LOG.debug("Clipboard is busy");
            } else {
                LOG.warn(e);
            }
            return null;
        }
    }

    private static void makeNonFinal(final Field field) throws IllegalAccessException, NoSuchFieldException {
        Field modifiers = field.getClass().getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    }

    private static void makeFinal(final Field field) throws IllegalAccessException, NoSuchFieldException {
        Field modifiers = field.getClass().getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() | Modifier.FINAL);
    }

    public static void shareCaretStateTransferable() {
        // need to replace its FLAVOR with ours using reflection
        if (ourFlavor == null) {
            synchronized (ourFlavorLock) {
                if (ourFlavor == null) {
                    ourFlavor = TextBlockDataFlavorRegistrar.getInstance().getOrCreateDataFlavor("application/x-multi-caret-info",
                            "Shared Caret State",
                            CaretStateTransferableData.class,
                            null,
                            true,
                            null
                    );
                }
            }
        }

        Class caretTransferable = CaretStateTransferableData.class;
        try {
            Field flavor = caretTransferable.getField("FLAVOR");
            makeNonFinal(flavor);
            flavor.set(null, ourFlavor);
            makeFinal(flavor);

            CopyPasteManager.getInstance().addContentChangedListener(ourContentChangedListener);

            ApplicationManager.getApplication().invokeLater(SharedCaretStateTransferableData::replaceClipboardIfNeeded);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void unshareCaretStateTransferable() {
        // restore old value
        Class caretTransferable = CaretStateTransferableData.class;
        try {
            Field flavor = caretTransferable.getField("FLAVOR");
            makeNonFinal(flavor);
            flavor.set(null, ORIGINAL_FLAVOR);
            makeFinal(flavor);

            CopyPasteManager.getInstance().removeContentChangedListener(ourContentChangedListener);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
