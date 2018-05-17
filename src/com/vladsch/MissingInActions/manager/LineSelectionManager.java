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

package com.vladsch.MissingInActions.manager;

import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import com.vladsch.MissingInActions.Plugin;
import com.vladsch.MissingInActions.actions.pattern.RangeLimitedCaretSpawningHandler;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.settings.ApplicationSettingsListener;
import com.vladsch.MissingInActions.settings.MouseModifierType;
import com.vladsch.MissingInActions.settings.PrefixOnPastePatternType;
import com.vladsch.MissingInActions.util.*;
import com.vladsch.MissingInActions.util.highlight.*;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.BasedSequenceImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.intellij.openapi.editor.event.EditorMouseEventArea.EDITING_AREA;

/**
 * Adjust a line selection to a normal selection when selection is adjusted by moving the caret
 */
@SuppressWarnings("WeakerAccess")
public class LineSelectionManager implements
        CaretListener
        //, SelectionListener
        , EditorActiveLookupListener
        , EditorMouseListener
        , EditorMouseMotionListener
        , Disposable
{

    private static final String ESCAPE_SEARCH = "ESCAPE";
    final private Editor myEditor;
    final private ReEntryGuard myCaretGuard = new ReEntryGuard();
    final private HashMap<Caret, StoredLineSelectionState> mySelectionStates = new HashMap<>();
    final private @NotNull DelayedRunner myDelayedRunner = new DelayedRunner();
    final private MessageBusConnection myMessageBusConnection;
    final private @NotNull ActionSelectionAdjuster myActionSelectionAdjuster;
    final private @NotNull EditorPositionFactory myPositionFactory;

    final private StoredLineSelectionState myPrimarySelectionState = new StoredLineSelectionState();
    private int myMouseAnchor = -1;
    private boolean myIsSelectionEndExtended;
    private boolean myIsSelectionStartExtended;
    private final CaretHighlighter myCaretHighlighter;
    ApplicationSettings mySettings;
    @Nullable private RangeLimitedCaretSpawningHandler myCaretSpawningHandler;
    @Nullable private Set<CaretEx> myStartCarets;
    @Nullable private Set<CaretEx> myStartMatchedCarets;
    @Nullable private Set<CaretEx> myFoundCarets;
    @Nullable private List<CaretState> myStartCaretStates;
    final @NotNull LineRangeHighlightProvider myIsolationHighlightProvider;
    @Nullable LineRangeHighlighter myIsolationHighlighter;
    @NotNull HighlightProvider myHighlightProvider = Plugin.getInstance();
    OneTimeRunnable myHighlightRunner = OneTimeRunnable.NULL;
    private HashMap<String, String> myOnPasteReplacementMap = null;
    private SearchPattern myOnPasteUserSearchPattern = null;
    @NotNull private String myOnPasteUserReplacementText = "";
    private Pattern myOnPasteSearchPattern = null;
    private final HighlightListener myHighlightListener;
    @Nullable protected Highlighter myHighlighter = null;
    final @NotNull HighlightListener myIsolatedLinesListener;

    //private AwtRunnable myInvalidateStoredLineStateRunnable = new AwtRunnable(true, this::invalidateStoredLineState);
    private boolean myIsActiveLookup;  // true if a lookup is active in the editor
    private final LafManagerListener myLafManagerListener;

    @Override
    public void dispose() {
        //println("LineSelectionAdjuster disposed");
        clearIsolatedLines();

        myIsolationHighlightProvider.removeHighlightListener(myIsolatedLinesListener);
        myIsolationHighlightProvider.disposeComponent();

        myDelayedRunner.runAll();
        myActionSelectionAdjuster.dispose();
        myMessageBusConnection.disconnect();
        myCaretSpawningHandler = null;
    }

    @NotNull
    public static LineSelectionManager getInstance(@NotNull Editor editor) {
        return Plugin.getInstance().getSelectionManager(editor);
    }

    public void setHighlightProvider(@Nullable HighlightProvider highlightProvider) {
        HighlightProvider oldHighlightProvider = myHighlightProvider;
        myHighlightProvider = highlightProvider == null ? Plugin.getInstance() : highlightProvider;
        if (myHighlightProvider != oldHighlightProvider) {
            removeHighlights();

            if (oldHighlightProvider != Plugin.getInstance()) {
                // remove listener
                oldHighlightProvider.removeHighlightListener(myHighlightListener);
            }

            if (myHighlightProvider != Plugin.getInstance()) {
                // remove listener
                myHighlightProvider.addHighlightListener(myHighlightListener, this);
            }

            myHighlightRunner.cancel();

            myHighlightRunner = OneTimeRunnable.schedule(250, new AwtRunnable(true, this::updateHighlights));
        }
    }

    public HighlightProvider getHighlightProvider() {
        return myHighlightProvider;
    }

    public LineSelectionManager(Editor editor) {
        myEditor = editor;
        //noinspection ThisEscapedInObjectConstruction
        myPositionFactory = new EditorPositionFactory(this);

        // this can fail if caret visual attributes are not implemented in the IDE (since 2017.1)
        CaretHighlighter caretHighlighter;
        try {
            //noinspection ThisEscapedInObjectConstruction
            caretHighlighter = new CaretHighlighterImpl(this);
            //caretHighlighter = CaretHighlighter.NULL;
        } catch (Throwable ignored) {
            caretHighlighter = CaretHighlighter.NULL;
        }

        //noinspection ThisEscapedInObjectConstruction
        myHighlightListener = new HighlightListener() {
            @Override
            public void highlightsChanged() {
                updateHighlights();
            }
        };
        Plugin.getInstance().addHighlightListener(myHighlightListener, this);

        myLafManagerListener = new LafManagerListener() {
            UIManager.LookAndFeelInfo lookAndFeel = LafManager.getInstance().getCurrentLookAndFeel();

            @Override
            public void lookAndFeelChanged(final LafManager source) {
                UIManager.LookAndFeelInfo newLookAndFeel = source.getCurrentLookAndFeel();
                if (lookAndFeel != newLookAndFeel) {
                    lookAndFeel = newLookAndFeel;
                    settingsChanged(mySettings);
                }
            }
        };

        LafManager.getInstance().addLafManagerListener(myLafManagerListener);
        myDelayedRunner.addRunnable(() -> {
            LafManager.getInstance().removeLafManagerListener(myLafManagerListener);
        });

        myCaretHighlighter = caretHighlighter;
        //noinspection ThisEscapedInObjectConstruction
        myActionSelectionAdjuster = new ActionSelectionAdjuster(this, NormalAdjustmentMap.getInstance());

        mySettings = ApplicationSettings.getInstance();
        settingsChanged(mySettings);

        //noinspection ThisEscapedInObjectConstruction
        myMessageBusConnection = ApplicationManager.getApplication().getMessageBus().connect(this);
        myMessageBusConnection.subscribe(ApplicationSettingsListener.TOPIC, this::settingsChanged);
        myCaretSpawningHandler = null;
        myStartCarets = null;
        myStartMatchedCarets = null;
        myFoundCarets = null;
        myStartCaretStates = null;

        myIsolationHighlightProvider = new LineRangeHighlightProviderImpl(mySettings);
        myIsolatedLinesListener = new HighlightListener() {
            @Override
            public void highlightsChanged() {
                if (myIsolationHighlightProvider.isShowHighlights()) {
                    if (myIsolationHighlighter == null) myIsolationHighlighter = myIsolationHighlightProvider.getHighlighter(myEditor);
                    myIsolationHighlighter.updateHighlights();
                } else {
                    if (myIsolationHighlighter != null) myIsolationHighlighter.removeHighlights();
                    myIsolationHighlighter = null;
                }
            }
        };
        myIsolationHighlightProvider.addHighlightListener(myIsolatedLinesListener, this);

        DocumentListener documentListener = new DocumentListener() {
            @Override
            public void beforeDocumentChange(final com.intellij.openapi.editor.event.DocumentEvent event) {

            }

            @Override
            public void documentChanged(final com.intellij.openapi.editor.event.DocumentEvent event) {
                if (myHighlightProvider.isShowHighlights()) {
                    myHighlightRunner.cancel();
                    myHighlightRunner = OneTimeRunnable.schedule(250, new AwtRunnable(true, () -> {
                        updateHighlights();
                    }));
                }

                //if (myIsolationHighlighter != null) {
                //    myIsolationHighlighter.updateHighlights();
                //}
            }
        };

        //noinspection ThisEscapedInObjectConstruction
        myEditor.getDocument().addDocumentListener(documentListener, this);

        // update if they exist
        updateHighlights();
    }

    public static boolean isCaretAttributeAvailable() {
        return CaretEx.HAVE_VISUAL_ATTRIBUTES;
    }

    @Nullable
    public RangeLimitedCaretSpawningHandler getCaretSpawningHandler() {
        return myCaretSpawningHandler;
    }

    @Nullable
    public HashMap<String, String> getOnPasteReplacementMap() {
        return myOnPasteReplacementMap;
    }

    public SearchPattern getOnPasteUserSearchPattern() {
        return myOnPasteUserSearchPattern;
    }

    public void setOnPasteReplacementText(@Nullable final HashMap<String, String> onPasteReplacementMap) {
        myOnPasteReplacementMap = onPasteReplacementMap == null ? null : new HashMap<String, String>(onPasteReplacementMap);
        myOnPasteSearchPattern = null;
    }

    public void setOnPasteUserSearchPattern(@Nullable SearchPattern pattern) {
        if (pattern != null) {
            try {
                if (pattern.isRegex()) {
                    Pattern test = Pattern.compile(pattern.getPatternText());
                }
                myOnPasteUserSearchPattern = pattern;
                myOnPasteSearchPattern = null;
            } catch (PatternSyntaxException ignored) {
                myOnPasteSearchPattern = null;
            }
        }
        myOnPasteSearchPattern = null;
    }

    public void setOnPasteUserReplacementText(@NotNull String replacementString) {
        myOnPasteUserReplacementText = replacementString;
    }

    public boolean haveOnPasteReplacements() {
        return (myOnPasteReplacementMap != null && !myOnPasteReplacementMap.isEmpty()) || myOnPasteUserSearchPattern != null;
    }

    @NotNull
    public String replaceOnPaste(@NotNull String text) {
        if (myOnPasteReplacementMap == null || myOnPasteReplacementMap.isEmpty()) {
            return text;
        }

        Pattern pattern = getOnPastePattern();
        Matcher matcher = pattern.matcher(text);

        int lastPos = 0;
        final StringBuilder sb = new StringBuilder();
        final ApplicationSettings settings = ApplicationSettings.getInstance();
        final boolean smartReplace = settings.isUserDefinedMacroSmartReplace();
        final BasedSequence chars = BasedSequenceImpl.of(text);
        CaseFormatPreserver preserver = new CaseFormatPreserver();
        int separators = settings.getPreserveOnPasteSeparators();
        final PrefixOnPastePatternType patternType = settings.getRemovePrefixOnPastePatternType();
        final String[] prefixes = settings.getPrefixesOnPasteList();

        while (matcher.find()) {
            final int start = matcher.start();
            int end = matcher.end();
            if (lastPos < start) {
                sb.append(text, lastPos, start);
            }

            String replace = myOnPasteReplacementMap.get(matcher.group());
            if (replace != null) {
                sb.append(replace);
            } else {
                // must be user text
                if (smartReplace) {
                    preserver.studyFormatBefore(chars, 0, start, end, patternType, prefixes, separators);
                    String edited = sb.toString() + myOnPasteUserReplacementText + text.substring(end);
                    final TextRange range = new TextRange(sb.length(), sb.length() + myOnPasteUserReplacementText.length());
                    final BasedSequence chars1 = BasedSequenceImpl.of(edited);

                    InsertedRangeContext i = preserver.preserveFormatAfter(
                            chars1,
                            range
                            , settings.isPreserveCamelCaseOnPaste()
                            , settings.isPreserveSnakeCaseOnPaste()
                            , settings.isPreserveScreamingSnakeCaseOnPaste()
                            , settings.isPreserveDashCaseOnPaste()
                            , settings.isPreserveDotCaseOnPaste()
                            , settings.isPreserveSlashCaseOnPaste()
                            , settings.isRemovePrefixOnPaste()
                            , settings.isAddPrefixOnPaste()
                            , settings.getRemovePrefixOnPastePatternType()
                            , settings.getPrefixesOnPasteList()
                    );

                    if (i == null) {
                        // as is
                        sb.append(myOnPasteUserReplacementText);
                    } else {
                        // extract the changed paste replacement
                        sb.append(i.word());
                        if (i.getCaretDelta() > 0) {
                            // changed the next character(s), we grab it too
                            sb.append(edited.substring(sb.length() + myOnPasteUserReplacementText.length() - i.getCaretDelta()));
                            end += i.getCaretDelta();
                        }
                    }
                } else {
                    sb.append(myOnPasteUserReplacementText);
                }
            }
            lastPos = end;
        }

        if (lastPos < text.length()) {
            sb.append(text, lastPos, text.length());
        }

        return sb.toString();
    }

    public Pattern getOnPastePattern() {
        if (myOnPasteSearchPattern == null) {
            StringBuilder sb = new StringBuilder();
            String splice = "";
            for (String search : myOnPasteReplacementMap.keySet()) {
                sb.append(splice);

                sb.append(SearchPattern.getPatternText(search, false, true));
                splice = "|";
            }

            if (myOnPasteUserSearchPattern != null) {
                sb.append(splice);
                sb.append(myOnPasteUserSearchPattern.getPatternText(myOnPasteUserSearchPattern.isRegex() || !ApplicationSettings.getInstance().isUserDefinedMacroClipContent()));
                splice = "|";
            }

            myOnPasteSearchPattern = Pattern.compile(sb.toString());
        }
        return myOnPasteSearchPattern;
    }

    public void clearSearchFoundCarets() {
        Set<Long> excludeList = null;

        myCaretHighlighter.highlightCaretList(myFoundCarets, CaretAttributeType.DEFAULT, null);

        //noinspection ConstantConditions
        excludeList = CaretEx.getExcludedCoordinates(excludeList, myFoundCarets);
        myCaretHighlighter.highlightCaretList(myStartMatchedCarets, CaretAttributeType.DEFAULT, excludeList);

        excludeList = CaretEx.getExcludedCoordinates(excludeList, myStartMatchedCarets);
        myCaretHighlighter.highlightCaretList(myStartCarets, CaretAttributeType.DEFAULT, excludeList);

        myCaretSpawningHandler = null;
        myStartCaretStates = null;
        myStartCarets = null;
        myStartMatchedCarets = null;
        myFoundCarets = null;
        myCaretHighlighter.highlightCarets();
        myDelayedRunner.runAllFor(ESCAPE_SEARCH);
    }

    @Nullable
    public List<CaretState> getStartCaretStates() {
        return myStartCaretStates;
    }

    @Nullable
    public Set<CaretEx> getStartCarets() {
        return myStartCarets;
    }

    @Nullable
    public Set<CaretEx> getStartMatchedCarets() {
        return myStartMatchedCarets;
    }

    @Nullable
    public Set<CaretEx> getFoundCarets() {
        return myFoundCarets;
    }

    public void setSearchFoundCaretSpawningHandler(
            @Nullable final RangeLimitedCaretSpawningHandler caretSpawningHandler,
            @Nullable final List<CaretState> startCaretStates,
            @Nullable final Collection<Caret> startCarets,
            @Nullable final Collection<Caret> startMatchedCarets,
            @Nullable final Collection<Caret> foundCarets
    ) {
        if (myCaretSpawningHandler == null) {
            addEscapeDispatcher();
        }

        myCaretSpawningHandler = caretSpawningHandler;
        myStartCaretStates = startCaretStates;
        setFoundCarets(foundCarets);
        setStartMatchedCarets(startMatchedCarets);
        setStartCarets(startCarets);
    }

    public void setSearchFoundCaretSpawningHandler(@Nullable final RangeLimitedCaretSpawningHandler caretSpawningHandler) {
        if (myCaretSpawningHandler == null) {
            addEscapeDispatcher();
        }
        myCaretSpawningHandler = caretSpawningHandler;
    }

    public void setStartCaretStates(@Nullable final List<CaretState> startCaretStates) {
        myStartCaretStates = startCaretStates;
    }

    @SuppressWarnings("ConstantConditions")
    private void setStartCarets(@Nullable final Collection<Caret> carets) {
        Set<Long> excludeList = null;
        excludeList = CaretEx.getExcludedCoordinates(excludeList, myFoundCarets);
        excludeList = CaretEx.getExcludedCoordinates(excludeList, myStartMatchedCarets);

        myCaretHighlighter.highlightCaretList(myStartCarets, CaretAttributeType.DEFAULT, excludeList);
        if (carets == null) {
            myStartCarets = null;
        } else {
            myStartCarets = new HashSet<>(carets.size());
            CaretEx myPrimaryCaret = myCaretHighlighter.getPrimaryCaret();
            getMatchedCarets(carets, excludeList, myPrimaryCaret, myStartCarets);
            myCaretHighlighter.highlightCaretList(myStartCarets, CaretAttributeType.START, excludeList);
        }
    }

    private void setStartMatchedCarets(@Nullable final Collection<Caret> carets) {
        Set<Long> excludeList = CaretEx.getExcludedCoordinates(null, myFoundCarets);

        myCaretHighlighter.highlightCaretList(myStartMatchedCarets, CaretAttributeType.DEFAULT, excludeList);
        if (carets == null) {
            myStartMatchedCarets = null;
        } else {
            myStartMatchedCarets = new HashSet<>(carets.size());
            CaretEx myPrimaryCaret = myCaretHighlighter.getPrimaryCaret();
            getMatchedCarets(carets, excludeList, myPrimaryCaret, myStartMatchedCarets);
            myCaretHighlighter.highlightCaretList(myStartMatchedCarets, CaretAttributeType.START_MATCHED, excludeList);
        }
    }

    private void getMatchedCarets(@NotNull final Collection<Caret> carets, final Set<Long> excludeList, CaretEx myPrimaryCaret, final Set<CaretEx> matchedCarets) {
        for (Caret caret : carets) {
            if (excludeList != null && excludeList.contains(CaretEx.getCoordinates(caret))) continue;

            if (myPrimaryCaret != null && myPrimaryCaret.isCaret(caret)) {
                myCaretHighlighter.setPrimaryCaret(null);
                myPrimaryCaret = null;
            }
            matchedCarets.add(new CaretEx(caret));
        }
    }

    private void setFoundCarets(@Nullable final Collection<Caret> carets) {
        myCaretHighlighter.highlightCaretList(myFoundCarets, CaretAttributeType.DEFAULT, null);
        if (carets == null) {
            myFoundCarets = null;
        } else {
            myFoundCarets = new HashSet<>(carets.size());
            CaretEx myPrimaryCaret = myCaretHighlighter.getPrimaryCaret();

            for (Caret caret : carets) {
                if (myPrimaryCaret != null && myPrimaryCaret.isCaret(caret)) {
                    myCaretHighlighter.setPrimaryCaret(null);
                    myPrimaryCaret = null;
                }
                myFoundCarets.add(new CaretEx(caret));
            }

            myCaretHighlighter.highlightCaretList(myFoundCarets, CaretAttributeType.FOUND, null);
        }
    }

    @Nullable
    public BitSet getIsolatedLines() {
        return myIsolationHighlightProvider.getHighlightLines();
    }

    @Nullable
    public BitSet addIsolatedLines(@NotNull BitSet bitSet) {
        return myIsolationHighlightProvider.addHighlightLines(bitSet);
    }

    @Nullable
    public BitSet removeIsolatedLines(@NotNull BitSet bitSet) {
        return myIsolationHighlightProvider.removeHighlightLines(bitSet);
    }

    public boolean isIsolatedMode() {
        return myIsolationHighlightProvider.isHighlightsMode();
    }

    public boolean haveIsolatedLines() {
        return myIsolationHighlightProvider.haveHighlights();
    }

    public void setIsolatedMode(boolean isolatedMode) {
        myIsolationHighlightProvider.setHighlightsMode(isolatedMode);
    }

    public void clearIsolatedLines() {
        myIsolationHighlightProvider.clearHighlights();
    }

    public void setIsolatedLines(@Nullable BitSet bitSet, @Nullable Boolean isolatedMode) {
        myIsolationHighlightProvider.setHighlightLines(bitSet, isolatedMode);
    }

    public void removeHighlights() {
        if (myHighlighter != null) {
            myHighlighter.removeHighlights();
            myHighlighter = null;
        }
    }

    public Highlighter getHighlighter() {
        return myHighlighter;
    }

    public void updateHighlights() {
        myHighlightRunner.cancel();

        if (myHighlightProvider.isShowHighlights()) {
            if (myHighlighter == null) {
                myHighlighter = myHighlightProvider.getHighlighter(myEditor);
            }
            myHighlighter.updateHighlights();
        } else {
            removeHighlights();
        }
    }

    @NotNull
    public EditorPositionFactory getPositionFactory() {
        return myPositionFactory;
    }

    public CaretHighlighter getCaretHighlighter() {
        return myCaretHighlighter;
    }

    @NotNull
    public Editor getEditor() {
        return myEditor;
    }

    @NotNull
    public EditorCaret getEditorCaret(@NotNull Caret caret) {
        return new EditorCaret(myPositionFactory, caret, getSelectionState(caret));
    }

    @NotNull
    private StoredLineSelectionState getStoredSelectionState(@NotNull Caret caret) {
        if (caret == caret.getCaretModel().getPrimaryCaret()) {
            return myPrimarySelectionState;
        } else {
            return mySelectionStates.computeIfAbsent(caret, k -> new StoredLineSelectionState());
        }
    }

    @Nullable
    private StoredLineSelectionState getStoredSelectionStateIfExists(@NotNull Caret caret) {
        if (caret == caret.getCaretModel().getPrimaryCaret()) {
            return myPrimarySelectionState;
        } else {
            return mySelectionStates.get(caret);
        }
    }

    @NotNull
    public LineSelectionState getSelectionState(@NotNull Caret caret) {
        StoredLineSelectionState state = getStoredSelectionState(caret);
        return new LineSelectionState(state.anchorColumn, state.isStartAnchor);
    }

    void setLineSelectionState(@NotNull Caret caret, int anchorColumn, boolean isStartAnchor) {
        StoredLineSelectionState state = getStoredSelectionState(caret);
        state.anchorColumn = anchorColumn;
        state.isStartAnchor = isStartAnchor;
    }

    public void resetSelectionState(Caret caret) {
        if (caret == caret.getCaretModel().getPrimaryCaret()) {
            myPrimarySelectionState.resetToDefault();
        } else {
            StoredLineSelectionState state = getStoredSelectionStateIfExists(caret);
            if (state != null) {
                mySelectionStates.remove(caret);
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isSelectionEndExtended() {
        return myIsSelectionEndExtended;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isSelectionStartExtended() {
        return myIsSelectionStartExtended;
    }

    private void addEscapeDispatcher() {
        if (mySettings.isSearchCancelOnEscape()) {
            final IdeEventQueue.EventDispatcher eventDispatcher = new IdeEventQueue.EventDispatcher() {
                @Override
                public boolean dispatch(@NotNull final AWTEvent e) {
                    return LineSelectionManager.this.dispatchEscape(e);
                }
            };

            IdeEventQueue.getInstance().addDispatcher(eventDispatcher, this);
            myDelayedRunner.addRunnable(ESCAPE_SEARCH, () -> {
                IdeEventQueue.getInstance().removeDispatcher(eventDispatcher);
            });
        }
    }

    private boolean dispatchEscape(@NotNull final AWTEvent e) {
        if (e instanceof KeyEvent && e.getID() == KeyEvent.KEY_PRESSED) {
            if ((((KeyEvent) e).getKeyCode() == KeyEvent.VK_ESCAPE)) {
                final Component owner = UIUtil.findParentByCondition(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(), component -> component instanceof JTextComponent);

                if (owner != null && owner instanceof JComponent) {
                    // register multi-paste if no already registered and remove when focus is lost
                    if (owner == myEditor.getContentComponent()) {
                        List<CaretState> caretStates = getStartCaretStates();
                        if (caretStates != null) {
                            clearSearchFoundCarets();
                            myEditor.getCaretModel().setCaretsAndSelections(caretStates);
                        }
                        return true;
                    }
                }
            }
        }

        return false;
    }

    void settingsChanged(@NotNull ApplicationSettings settings) {
        // unhook all the stuff for settings registration
        mySettings = settings;

        boolean startExtended = settings.isSelectionStartExtended();
        boolean endExtended = settings.isSelectionEndExtended();

        HashMap<Caret, Boolean> lineCarets = new HashMap<>();
        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
            EditorCaret editorCaret = getEditorCaret(caret);
            if (editorCaret.isLine()) {
                lineCarets.put(caret, true);
                editorCaret
                        .toCharSelection()
                        .normalizeCaretPosition()
                        .commit();
            }
        }

        myDelayedRunner.runAll();

        // change our mode
        myIsSelectionEndExtended = settings.isSelectionEndExtended();
        myIsSelectionStartExtended = settings.isSelectionStartExtended();

        hookListeners(settings);
        myCaretHighlighter.removeCaretHighlight();
        myCaretHighlighter.settingsChanged(settings);
        Set<Long> excludeList = null;

        if (myCaretSpawningHandler != null) {
            myCaretHighlighter.highlightCaretList(myFoundCarets, CaretAttributeType.FOUND, null);

            excludeList = CaretEx.getExcludedCoordinates(excludeList, myFoundCarets);
            myCaretHighlighter.highlightCaretList(myStartMatchedCarets, CaretAttributeType.START_MATCHED, excludeList);

            excludeList = CaretEx.getExcludedCoordinates(excludeList, myStartMatchedCarets);
            myCaretHighlighter.highlightCaretList(myStartCarets, CaretAttributeType.START, excludeList);

            addEscapeDispatcher();
        }

        myActionSelectionAdjuster.setSelectionStashLimit(settings.getSelectionStashLimit());

        // change all selections that were lines back to lines
        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
            if (lineCarets.containsKey(caret)) {
                EditorCaret editorCaret = getEditorCaret(caret);
                editorCaret
                        .toLineSelection()
                        .normalizeCaretPosition()
                        .commit();
            }
        }
    }

    private void hookListeners(ApplicationSettings settings) {
        // wire ourselves in
        if (isLineSelectionSupported()) {
            myEditor.getCaretModel().addCaretListener(this);
            myDelayedRunner.addRunnable("CaretListener", () -> {
                myEditor.getCaretModel().removeCaretListener(this);
            });

            //myEditor.getSelectionModel().addSelectionListener(this);
            //myDelayedRunner.addRunnable("CaretListener", () -> {
            //    myEditor.getSelectionModel().removeSelectionListener(this);
            //});

            if (myEditor.getProject() != null) {
                Plugin.getInstance().addEditorActiveLookupListener(myEditor, this);
            }

            if (settings.isMouseLineSelection()) {
                myEditor.addEditorMouseListener(this);
                myEditor.addEditorMouseMotionListener(this);
                myDelayedRunner.addRunnable("MouseListener", () -> {
                    myEditor.removeEditorMouseListener(this);
                    myEditor.removeEditorMouseMotionListener(this);
                });
            }
        }
    }

    @Override
    public void enterActiveLookup() {
        myIsActiveLookup = true;
    }

    @Override
    public void exitActiveLookup() {
        myIsActiveLookup = false;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isLineSelectionSupported() {
        return !myEditor.isOneLineMode() && !myIsActiveLookup;
    }

    public void guard(Runnable runnable) {
        myCaretGuard.guard(runnable);
    }

    public void ifUnguarded(@NotNull Runnable runnable) {myCaretGuard.ifUnguarded(runnable);}

    public void ifUnguarded(boolean ifGuardedRunOnExit, @NotNull Runnable runnable) {myCaretGuard.ifUnguarded(ifGuardedRunOnExit, runnable);}

    public void ifUnguarded(@NotNull Runnable runnable, @Nullable Runnable runOnGuardExit) {myCaretGuard.ifUnguarded(runnable, runOnGuardExit);}

    public boolean unguarded() {return myCaretGuard.unguarded();}

    private void println(String message) {
        if (Plugin.isFeatureLicensed(Plugin.FEATURE_DEVELOPMENT)) {
            System.out.println(message);
        }
    }

    @Override
    public void mousePressed(EditorMouseEvent e) {
        if (e.getArea() == EDITING_AREA) {
            //int offset = myEditor.getCaretModel().getOffset();
            //println("mouse pressed offset: " + offset /*+ " event:" + e.getMouseEvent() */ + " isConsumed" + e.isConsumed());

            if (myMouseAnchor == -1) {
                // if we have a selection then we need to move the caret out of it otherwise the editor interprets it as a continuation of selection
                if (myActionSelectionAdjuster.canSaveSelection()) {
                    myActionSelectionAdjuster.saveSelectionMarker(myActionSelectionAdjuster.getCurrentSelectionMarker(), false, true, true, true);
                }

                Caret caret = myEditor.getCaretModel().getPrimaryCaret();
                if (caret.hasSelection() && caret.getOffset() != caret.getSelectionStart() && caret.getOffset() != caret.getSelectionEnd()) {
                    // Issue #15, incorrect selection extension when using shift key
                    EditorCaret editorCaret = getEditorCaret(caret);
                    caret.moveToOffset(editorCaret.isStartAnchor() ? caret.getSelectionEnd() : caret.getSelectionStart());
                }
            }
        }
    }

    @Override
    public void mouseClicked(EditorMouseEvent e) {

    }

    public boolean canRecallSelection() {
        return myActionSelectionAdjuster.canRecallSelection();
    }

    public boolean canSwapSelection() {
        return myActionSelectionAdjuster.canSwapSelection();
    }

    public void recallLastSelection(int offsetFromTop, boolean removeSelection, boolean swapWithCurrent, boolean makeVisible) {
        myActionSelectionAdjuster.recallLastSelection(offsetFromTop, removeSelection, swapWithCurrent);
        if (makeVisible) {
            EditHelpers.scrollToSelection(myEditor);
        }
    }

    @Nullable
    public RangeMarker getRangeMarker() {
        if (myActionSelectionAdjuster.canSaveSelection()) {
            return myActionSelectionAdjuster.getCurrentSelectionMarker();
        }
        return null;
    }

    @Nullable
    public RangeMarker getDummyRangeMarker() {
        if (myActionSelectionAdjuster.canSaveSelection()) {
            return myActionSelectionAdjuster.getDummySelectionMarker();
        }
        return null;
    }

    public void pushSelection(@Nullable RangeMarker marker, boolean onlyIfNotTop, boolean onlyIfNotStored, boolean moveToTop) {
        if (marker != null && marker.isValid()) {
            myActionSelectionAdjuster.saveSelectionMarker(marker, false, onlyIfNotTop, onlyIfNotStored, moveToTop);
        }
    }

    public void pushSelection(boolean onlyIfNotTop, boolean onlyIfNotStored, boolean moveToTop) {
        if (myActionSelectionAdjuster.canSaveSelection()) {
            RangeMarker marker = myActionSelectionAdjuster.getCurrentSelectionMarker();
            pushSelection(marker, onlyIfNotTop, onlyIfNotStored, moveToTop);
        }
    }

    public RangeMarker[] getSavedSelections() {
        return myActionSelectionAdjuster.getSavedSelections();
    }

    private boolean isControlledSelect(EditorMouseEvent e) {
        boolean ctrl = (e.getMouseEvent().getModifiers() & (MouseEvent.CTRL_MASK)) != 0;
        return ctrl ^ (ApplicationSettings.getInstance().getMouseModifier() == MouseModifierType.CTRL_LINE.intValue);
    }

    @Override
    public void mouseReleased(EditorMouseEvent e) {
        if (e.getArea() == EDITING_AREA && !myEditor.getSettings().isUseSoftWraps() && !myEditor.isColumnMode()) {
            int offset = myEditor.getCaretModel().getOffset();
            //println("mouse released offset: " + offset + " anchor: " + myMouseAnchor /*+ " event:" + e.getMouseEvent()*/ + " isConsumed " + e.isConsumed());

            // adjust it one final time
            if (myMouseAnchor != -1 && myEditor.getSelectionModel().hasSelection()) {
                final int mouseAnchor = myMouseAnchor;
                final boolean controlledSelect = isControlledSelect(e);
                adjustMouseSelection(mouseAnchor, controlledSelect, true);
            }

            clearSearchFoundCarets();
        }

        myMouseAnchor = -1;
    }

    @Override
    public void mouseEntered(EditorMouseEvent e) {

    }

    @Override
    public void mouseExited(EditorMouseEvent e) {

    }

    @Override
    public void mouseMoved(EditorMouseEvent e) {

    }

    @Override
    public void mouseDragged(EditorMouseEvent e) {
        if (e.getArea() == EDITING_AREA && !myEditor.getSettings().isUseSoftWraps() && !myEditor.isColumnMode()) {
            // TODO: get mouse anchor in Visual
            //println("mouseDragged offset: " + offset + " ctrl:" + isControlledSelect(e) + " anchor: " + myMouseAnchor + " event:" + e.getMouseEvent() + " isConsumed" + e.isConsumed());
            if (myMouseAnchor == -1) {
                // first drag event, take the selection's anchor
                myMouseAnchor = myEditor.getCaretModel().getPrimaryCaret().getLeadSelectionOffset();
            } else {
                final int mouseAnchor = myMouseAnchor;
                adjustMouseSelection(mouseAnchor, isControlledSelect(e), false);
                //e.consume();
            }
        }
    }

    public void adjustMouseSelection(int mouseAnchor, boolean alwaysChar, boolean finalAdjustment) {
        // DONE: in all modes
        Caret caret = myEditor.getCaretModel().getPrimaryCaret();

        // mouse selection is between mouseAnchor and the caret offset
        // if they are on the same line then it is a char mark, else line mark
        final int offset = caret.getOffset();

        boolean isStartAnchor = mouseAnchor <= offset;
        int startOffset = isStartAnchor ? mouseAnchor : offset;
        int endOffset = isStartAnchor ? offset : mouseAnchor;

        StoredLineSelectionState state = getStoredSelectionState(caret);
        state.anchorColumn = myPositionFactory.fromOffset(mouseAnchor).column;
        state.isStartAnchor = isStartAnchor;

        final EditorPosition start = myPositionFactory.fromOffset(startOffset);
        final EditorPosition end = myPositionFactory.fromOffset(endOffset);

        myCaretGuard.guard(() -> {
            if (start.line == end.line || alwaysChar) {
                final EditorPosition pos = myPositionFactory.fromPosition(caret.getLogicalPosition());
                caret.setSelection(startOffset, endOffset);
                caret.moveToLogicalPosition(pos);

                if (finalAdjustment && state != myPrimarySelectionState) {
                    mySelectionStates.remove(caret);
                }
            } else if (!caret.hasSelection()) {
                if (finalAdjustment && state != myPrimarySelectionState) {
                    mySelectionStates.remove(caret);
                } else {
                    state.resetToDefault();
                }
            } else {
                if (finalAdjustment) {
                    // need to adjust final caret position for inside or outside the selection
                    if (myIsSelectionEndExtended) {
                        if (isStartAnchor) {
                            caret.moveToLogicalPosition(end.addLine(-1).atColumn(caret.getLogicalPosition()));
                        } else {
                            caret.moveToLogicalPosition(start.atEndOfLineSelection().atColumn(caret.getLogicalPosition()));
                        }
                    } else {
                        if (isStartAnchor) {
                            caret.moveToLogicalPosition(end.atColumn(caret.getLogicalPosition()));
                        } else {
                            caret.moveToLogicalPosition(start.atColumn(caret.getLogicalPosition()));
                        }
                    }
                    state.isStartAnchor = isStartAnchor;
                    caret.setSelection(isStartAnchor ? startOffset : caret.getOffset(), isStartAnchor ? caret.getOffset() : endOffset);
                    EditorCaret editorCaret = new EditorCaret(myPositionFactory, caret, new LineSelectionState(state.anchorColumn, state.isStartAnchor));
                    editorCaret
                            .toCaretPositionBasedLineSelection(true, false)
                            .normalizeCaretPosition()
                            .commit();
                } else {
                    if (isStartAnchor) {
                        caret.setSelection(start.atStartOfLine().getOffset(), end.atStartOfLine().getOffset());
                    } else {
                        caret.setSelection(start.atStartOfLine().getOffset(), end.atEndOfLineSelection().getOffset());
                    }
                }
            }
        });
    }

    @Override
    public void caretPositionChanged(CaretEvent e) {
        myCaretGuard.ifUnguarded(() -> {
            Caret caret = e.getCaret();
            if (myMouseAnchor == -1 && caret != null) {
                myCaretHighlighter.updateCaretHighlights();
            }
        });
    }

    private void invalidateStoredLineState() {
        // clear any states for carets that don't have selection to eliminate using a stale state
        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
            StoredLineSelectionState state = mySelectionStates.get(caret);
            if (state != null) {
                if (state == myPrimarySelectionState) {
                    state.resetToDefault();
                } else {
                    mySelectionStates.remove(caret);
                }
            }
        }
    }

    //@Override
    //public void selectionChanged(SelectionEvent e) {
    //    //if (e.getEditor() == myEditor) {
    //    //    myCaretGuard.ifUnguarded(myInvalidateStoredLineStateRunnable, false);
    //    //}
    //}

    @Override
    public void caretAdded(CaretEvent e) {
        Caret caret = e.getCaret();
        if (myMouseAnchor == -1 && caret != null) {
            myCaretHighlighter.caretAdded(caret);
        }
    }

    @Override
    public void caretRemoved(CaretEvent e) {
        mySelectionStates.remove(e.getCaret());
        Caret caret = e.getCaret();
        if (myMouseAnchor == -1 && caret != null) {
            myCaretHighlighter.caretRemoved(caret);
        }
    }

    public void updateCaretHighlights() {
        myCaretHighlighter.updateCaretHighlights();
    }

    public void runActionWithAdjustments(final AnAction action) {
        myActionSelectionAdjuster.runAction(action, false);
    }

    private static class StoredLineSelectionState {
        int anchorColumn = -1;
        boolean isStartAnchor = true;

        void resetToDefault() {
            anchorColumn = -1;
            isStartAnchor = true;
        }

        @Override
        public String toString() {
            return "StoredLineSelectionState{" +
                    "anchorColumn=" + anchorColumn +
                    ", isStartAnchor=" + isStartAnchor +
                    '}';
        }
    }
}
