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

import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import com.intellij.ide.CopyPasteManagerEx;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.CaretStateTransferableData;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.ide.CopyPasteManager;
import com.vladsch.plugin.util.clipboard.AugmentedTextBlockTransferable;
import com.vladsch.plugin.util.clipboard.ClipboardUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.BiFunction;

import static com.vladsch.plugin.util.AppUtils.isClipboardChangeNotificationsAvailable;

public class SharedCaretStateTransferableData implements TextBlockTransferableData, Externalizable {
    private static final Logger LOG = ClipboardUtils.LOG;
    private final static ExtensionPointName<SharedClipboardDataProvider> EP_NAME = ExtensionPointName.create("com.vladsch.shared.clipboard.data.provider");

    private static final DataFlavor SHARED_FLAVOR = ClipboardUtils.createDataFlavor("application/x-caret-state", "Shared Caret State", SharedCaretStateTransferableData.class, null, true);
    private static final int[] EMPTY_OFFSETS = new int[0];

    private static boolean inReplaceContent = false;
    private static boolean sharingCaretState = false;
    private static boolean initialized = false;
    private static HashMap<String, BiFunction<Transferable, DataFlavor, Object>> sharedDataLoaders = new HashMap<>();

    private int[] startOffsets;
    private int[] endOffsets;

    public SharedCaretStateTransferableData() {
        this(EMPTY_OFFSETS, EMPTY_OFFSETS);
    }

    public SharedCaretStateTransferableData(@NotNull CaretStateTransferableData other) {
        this(other.startOffsets, other.endOffsets);
    }

    private SharedCaretStateTransferableData(@NotNull final int[] startOffsets, @NotNull final int[] endOffsets) {
        this.startOffsets = startOffsets;
        this.endOffsets = endOffsets;
    }

    public int[] getStartOffsets() {
        return startOffsets;
    }

    public int[] getEndOffsets() {
        return endOffsets;
    }

    public DataFlavor getFlavor() {
        return SHARED_FLAVOR;
    }

    @Override
    public int getPriority() {
        return -100; // uses -ve of that for sorting up
    }

    @Override
    public int getOffsetCount() {
        return startOffsets.length + endOffsets.length;
    }

    @Override
    public int getOffsets(final int[] offsets, final int index) {
        System.arraycopy(startOffsets, 0, offsets, index, startOffsets.length);
        System.arraycopy(endOffsets, 0, offsets, index + startOffsets.length, endOffsets.length);
        return index + getOffsetCount();
    }

    @Override
    public int setOffsets(final int[] offsets, final int index) {
        System.arraycopy(offsets, index, startOffsets, 0, startOffsets.length);
        System.arraycopy(offsets, index + startOffsets.length, endOffsets, 0, endOffsets.length);
        return index + getOffsetCount();
    }

    // this will not change unless CaretStateTransferableData adds more data, unlikely
    static final long serialVersionUID = 0;

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        writeIntArray(out, startOffsets);
        writeIntArray(out, endOffsets);
    }

    private void writeIntArray(final ObjectOutput out, int[] data) throws IOException {
        int iMax = data.length;
        out.writeInt(iMax);

        for (int i = 0; i < iMax; i++) {
            out.writeInt(data[i]);
        }
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        startOffsets = readIntArray(in);
        endOffsets = readIntArray(in);
    }

    private int[] readIntArray(final ObjectInput in) throws IOException {
        int iMax = in.readInt();
        if (iMax == 0) return EMPTY_OFFSETS;

        int[] data = new int[iMax];

        for (int i = 0; i < iMax; i++) {
            data[i] = in.readInt();
        }

        return data;
    }

    final private static CopyPasteManager.ContentChangedListener ourContentChangedListener = new CopyPasteManager.ContentChangedListener() {
        @Override
        public void contentChanged(@Nullable final Transferable oldTransferable, final Transferable newTransferable) {
            replaceClipboardIfNeeded();
        }
    };

    final private static FlavorListener ourFlavorListener = new FlavorListener() {
        @Override
        public void flavorsChanged(FlavorEvent e) {
            replaceClipboardIfNeeded();
        }
    };

    private static SharedCaretStateTransferableData getSharedDataOrNull(@NotNull Transferable transferable) {
        try {
            return (SharedCaretStateTransferableData) transferable.getTransferData(SHARED_FLAVOR);
        } catch (UnsupportedFlavorException | IOException e) {
            LOG.warn(e);
        }
        return null;
    }

    private static class SharedClipboardDataBuilderImpl implements SharedClipboardDataBuilder {
        LinkedHashMap<TextBlockTransferableData, BiFunction<Transferable, DataFlavor, Object>> augmentedData = new LinkedHashMap<>();

        public void addSharedClipboardData(@NotNull TextBlockTransferableData textBlock, @Nullable BiFunction<Transferable, DataFlavor, Object> dataLoader) {
            String mimeType = textBlock.getFlavor().getMimeType();
            augmentedData.put(textBlock, dataLoader);
        }

        public boolean isEmpty() {
            return augmentedData.isEmpty();
        }
    }

    private static void replaceClipboardIfNeeded() {
        if (!inReplaceContent) {
            inReplaceContent = true;

            if (LOG.isDebugEnabled()) LOG.debug("Entering replaceClipboardIfNeeded update");

            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    Transferable transferable;
                    transferable = CopyPasteManager.getInstance().getContents();

                    if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        SharedClipboardDataBuilderImpl builder = new SharedClipboardDataBuilderImpl();
                        boolean caretSupported = transferable.isDataFlavorSupported(CaretStateTransferableData.FLAVOR);
                        boolean sharedCaretSupported = transferable.isDataFlavorSupported(SHARED_FLAVOR);

                        if (sharingCaretState) {
                            if (caretSupported && !sharedCaretSupported) {
                                // need to add shared state
                                CaretStateTransferableData caretData = (CaretStateTransferableData) ClipboardUtils.getTransferDataOrNull(transferable, CaretStateTransferableData.FLAVOR);
                                if (caretData != null) {
                                    builder.addSharedClipboardData(new SharedCaretStateTransferableData(caretData), (transferable1, flavor) -> getSharedDataOrNull(transferable1));
                                }
                            } else if (!caretSupported && sharedCaretSupported) {
                                // need to add caret state
                                SharedCaretStateTransferableData sharedCaretData = getSharedDataOrNull(transferable);
                                if (sharedCaretData != null) {
                                    builder.addSharedClipboardData(new CaretStateTransferableData(sharedCaretData.startOffsets, sharedCaretData.endOffsets), null);
                                }
                            }
                        }

                        // let EPs contribute
                        for (SharedClipboardDataProvider provider : EP_NAME.getExtensions()) {
                            provider.addSharedClipboardData(transferable, builder);
                        }

                        if (!builder.isEmpty()) {
                            Transferable newTransferable = AugmentedTextBlockTransferable.create(transferable, builder.augmentedData, sharedDataLoaders);
                            if (LOG.isDebugEnabled()) LOG.debug("Replacing clipboard content with " + newTransferable);

                            ApplicationManager.getApplication().invokeLater(() -> {
                                // need to check if the transferable is still available, otherwise we are adding one that was deleted before this call
                                try {
                                    CopyPasteManagerEx copyPasteManager = CopyPasteManagerEx.getInstanceEx();

                                    Transferable[] allContents = copyPasteManager.getAllContents();
                                    final Transferable firstTransferable = allContents.length > 0 ? allContents[0] : null;

                                    if (Objects.equals(getStringContent(firstTransferable), getStringContent(transferable))) {
                                        // still here and first
                                        copyPasteManager.setContents(newTransferable);
                                        if (LOG.isDebugEnabled()) LOG.debug("Exiting replaceClipboardIfNeeded update done.");
                                    } else {
                                        if (LOG.isDebugEnabled()) LOG.debug("Exiting replaceClipboardIfNeeded transferable was removed or changed, skipping.");
                                    }
                                } finally {
                                    inReplaceContent = false;
                                }
                            }, ModalityState.any());

                            return;
                        }
                    }
                } catch (Exception ignored) {
                }

                if (LOG.isDebugEnabled()) LOG.debug("Exiting replaceClipboardIfNeeded update not needed");
                inReplaceContent = false;
            });
        }
    }

    private static String getStringContent(@Nullable Transferable content) {
        if (content != null) {
            try {
                return (String) content.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException ignore) {

            }
        }
        return null;
    }

    private static <T> void changeFinalValue(final Field field, Object instance, T data) throws IllegalAccessException, NoSuchFieldException {
        makeNonFinal(field);
        field.set(instance, data);
        makeFinal(field);
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

    public static void initialize() {
        if (!initialized) {
            initialized = true;

            for (SharedClipboardDataProvider provider : EP_NAME.getExtensions()) {
                provider.initialize((flavor, loader) -> {
                    sharedDataLoaders.put(flavor.getMimeType(), loader);
                });
            }

            if (!isClipboardChangeNotificationsAvailable()) {
                Clipboard clipboard = ClipboardUtils.getSystemClipboard();
                if (clipboard != null) {
                    clipboard.addFlavorListener(ourFlavorListener);
                    LOG.warn("Registered system clipboard listener for legacy IDE");
                } else {
                    LOG.warn("Could not register system clipboard listener for legacy IDE");
                }
            }

            CopyPasteManager.getInstance().addContentChangedListener(ourContentChangedListener);
            ApplicationManager.getApplication().invokeLater(SharedCaretStateTransferableData::replaceClipboardIfNeeded);
        }
    }

    public static void dispose() {
        if (initialized) {
            initialized = false;
            CopyPasteManager.getInstance().removeContentChangedListener(ourContentChangedListener);
        }
    }

    public static void shareCaretStateTransferable() {
        sharingCaretState = true;
        if (initialized) ApplicationManager.getApplication().invokeLater(SharedCaretStateTransferableData::replaceClipboardIfNeeded);
    }

    public static void unshareCaretStateTransferable() {
        sharingCaretState = false;
    }
}
