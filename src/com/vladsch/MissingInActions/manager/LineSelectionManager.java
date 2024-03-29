// Copyright 2016-2023 2023 Vladimir Schneider <vladimir.schneider@gmail.com> Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
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
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.ui.ComponentUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.vladsch.MissingInActions.Plugin;
import com.vladsch.MissingInActions.actions.pattern.RangeLimitedCaretSpawningHandler;
import com.vladsch.MissingInActions.settings.ApplicationSettings;
import com.vladsch.MissingInActions.settings.ApplicationSettingsListener;
import com.vladsch.MissingInActions.settings.MouseModifierType;
import com.vladsch.MissingInActions.settings.PrefixOnPastePatternType;
import com.vladsch.MissingInActions.settings.SuffixOnPastePatternType;
import com.vladsch.MissingInActions.util.CaseFormatPreserver;
import com.vladsch.MissingInActions.util.EditHelpers;
import com.vladsch.MissingInActions.util.EditorActiveLookupListener;
import com.vladsch.MissingInActions.util.InsertedRangeContext;
import com.vladsch.MissingInActions.util.MiaCancelableJobScheduler;
import com.vladsch.MissingInActions.util.TextOffsetConsumer;
import com.vladsch.MissingInActions.util.highlight.MiaLineRangeHighlightProviderImpl;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.plugin.util.AwtRunnable;
import com.vladsch.plugin.util.DelayedRunner;
import com.vladsch.plugin.util.OneTimeRunnable;
import com.vladsch.plugin.util.ReEntryGuard;
import com.vladsch.plugin.util.SearchPattern;
import com.vladsch.plugin.util.ui.highlight.HighlightListener;
import com.vladsch.plugin.util.ui.highlight.HighlightProvider;
import com.vladsch.plugin.util.ui.highlight.Highlighter;
import com.vladsch.plugin.util.ui.highlight.LineRangeHighlightProvider;
import com.vladsch.plugin.util.ui.highlight.LineRangeHighlighter;
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
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.intellij.openapi.editor.event.EditorMouseEventArea.EDITING_AREA;
import static com.vladsch.flexmark.util.misc.Utils.rangeLimit;

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
        , Disposable {

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
    @Nullable private Set<Caret> myStartCarets;
    @Nullable private Set<Caret> myStartMatchedCarets;
    @Nullable private Set<Caret> myFoundCarets;
    @Nullable private List<CaretState> myStartCaretStates;
    final @NotNull LineRangeHighlightProvider<ApplicationSettings> myIsolationHighlightProvider;
    @Nullable LineRangeHighlighter<ApplicationSettings> myIsolationHighlighter;
    @NotNull HighlightProvider<ApplicationSettings> myHighlightProvider = Plugin.getInstance();
    OneTimeRunnable myHighlightRunner = OneTimeRunnable.NULL;
    private HashMap<String, String> myOnPasteReplacementMap = null;
    private SearchPattern myOnPasteUserSearchPattern = null;
    @NotNull private String myOnPasteUserReplacementText = "";
    private Pattern myOnPasteSearchPattern = null;
    private final HighlightListener myHighlightListener;
    @Nullable protected Highlighter<ApplicationSettings> myHighlighter = null;
    final @NotNull HighlightListener myIsolatedLinesListener;
    boolean myInSelectionStackPopup = false;

    //private AwtRunnable myInvalidateStoredLineStateRunnable = new AwtRunnable(true, this::invalidateStoredLineState);
    private boolean myIsActiveLookup;  // true if a lookup is active in the editor
    private boolean myIsDisposed = false;

    @Override
    public void dispose() {
        //println("LineSelectionAdjuster disposed");
        myIsDisposed = true;
        clearIsolatedLines();

        myIsolationHighlightProvider.removeHighlightListener(myIsolatedLinesListener);
        myIsolationHighlightProvider.disposeComponent();

        myDelayedRunner.runAll();
        Disposer.dispose(myActionSelectionAdjuster);
        Disposer.dispose(myMessageBusConnection);
        myCaretSpawningHandler = null;
    }

    public boolean isDisposed() {
        return myIsDisposed;
    }

    @NotNull
    public static LineSelectionManager getInstance(@NotNull Editor editor) {
        return Plugin.getInstance().getSelectionManager(editor);
    }

    public void setHighlightProvider(@Nullable HighlightProvider<ApplicationSettings> highlightProvider) {
        HighlightProvider<ApplicationSettings> oldHighlightProvider = myHighlightProvider;
        Plugin plugin = Plugin.getInstance();
        myHighlightProvider = highlightProvider == null ? plugin : highlightProvider;
        if (myHighlightProvider != oldHighlightProvider) {
            removeHighlights();

            if (oldHighlightProvider != plugin) {
                // remove listener
                oldHighlightProvider.removeHighlightListener(myHighlightListener);
            }

            if (myHighlightProvider != plugin && !isDisposed()) {
                // remove listener
                myHighlightProvider.addHighlightListener(myHighlightListener, this);
            }

            myHighlightRunner.cancel();

            myHighlightRunner = OneTimeRunnable.schedule(MiaCancelableJobScheduler.getInstance(), 250, new AwtRunnable(true, this::updateHighlights));
        }
    }

    public boolean isInSelectionStackPopup() {
        return myInSelectionStackPopup;
    }

    public void setInSelectionStackPopup(final boolean inSelectionStackPopup) {
        myInSelectionStackPopup = inSelectionStackPopup;
    }

    @NotNull
    public HighlightProvider<ApplicationSettings> getHighlightProvider() {
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

        myHighlightListener = new HighlightListener() {
            @Override
            public void highlightsUpdated() {

            }

            @Override
            public void highlightsChanged() {
                updateHighlights();
            }
        };
        Plugin plugin = Plugin.getInstance();
        plugin.addHighlightListener(myHighlightListener, this);

        LafManagerListener lafManagerListener = new LafManagerListener() {
            UIManager.LookAndFeelInfo lookAndFeel = LafManager.getInstance().getCurrentLookAndFeel();

            @Override
            public void lookAndFeelChanged(@NotNull final LafManager source) {
                if (myEditor.isDisposed()) return;

                UIManager.LookAndFeelInfo newLookAndFeel = source.getCurrentLookAndFeel();
                if (lookAndFeel != newLookAndFeel) {
                    lookAndFeel = newLookAndFeel;
                    settingsChanged(mySettings);
                }
            }
        };

        //noinspection ThisEscapedInObjectConstruction
        myMessageBusConnection = ApplicationManager.getApplication().getMessageBus().connect(this);
        myMessageBusConnection.subscribe(LafManagerListener.TOPIC, lafManagerListener);

        myCaretHighlighter = caretHighlighter;
        //noinspection ThisEscapedInObjectConstruction
        myActionSelectionAdjuster = new ActionSelectionAdjuster(this, NormalAdjustmentMap.getInstance());

        mySettings = ApplicationSettings.getInstance();
        settingsChanged(mySettings);

        myMessageBusConnection.subscribe(ApplicationSettingsListener.TOPIC, (ApplicationSettingsListener) this::settingsChanged);
        myCaretSpawningHandler = null;
        myStartCarets = null;
        myStartMatchedCarets = null;
        myFoundCarets = null;
        myStartCaretStates = null;

        myIsolationHighlightProvider = new MiaLineRangeHighlightProviderImpl(mySettings);
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
            public void documentChanged(@NotNull final com.intellij.openapi.editor.event.DocumentEvent event) {
                if (myHighlightProvider.isShowHighlights()) {
                    myHighlightRunner.cancel();
                    myHighlightRunner = OneTimeRunnable.schedule(MiaCancelableJobScheduler.getInstance(), 250, new AwtRunnable(true, () -> updateHighlights()));
                }
            }
        };

        //noinspection ThisEscapedInObjectConstruction
        myEditor.getDocument().addDocumentListener(documentListener, this);

        // need to update highlight provider when editor in tool window or otherwise becomes active
        myEditor.getContentComponent().addPropertyChangeListener("ancestor", (evt) -> {
            boolean isActive = evt.getNewValue() != null;

            if (isActive) {
                Project project = myEditor.getProject();
                if (project != null) {
                    // all project mia tool windows should be checked 
                    if (project.isDefault() || !Plugin.getInstance().shouldNotUpdateHighlighters(myEditor)) {
                        setHighlightProvider(plugin.getActiveHighlightProvider());
                    }
                }
            }
        });

        // update if they exist
        updateHighlights();
    }

    public static boolean isCaretAttributeAvailable() {
        return true;
    }

    @Nullable
    public RangeLimitedCaretSpawningHandler getCaretSpawningHandler() {
        return myCaretSpawningHandler;
    }

    @SuppressWarnings("unused")
    @Nullable
    public HashMap<String, String> getOnPasteReplacementMap() {
        return myOnPasteReplacementMap;
    }

    public SearchPattern getOnPasteUserSearchPattern() {
        return myOnPasteUserSearchPattern;
    }

    public void setOnPasteReplacementText(@Nullable final HashMap<String, String> onPasteReplacementMap) {
        myOnPasteReplacementMap = onPasteReplacementMap == null ? null : new HashMap<>(onPasteReplacementMap);
        myOnPasteSearchPattern = null;
    }

    public void setOnPasteUserSearchPattern(@Nullable SearchPattern pattern) {
        if (pattern != null) {
            try {
                if (pattern.isRegex()) {
                    Pattern.compile(pattern.getPatternText());
                }
                myOnPasteUserSearchPattern = pattern;
            } catch (PatternSyntaxException ignored) {
            }
        } else {
            myOnPasteUserSearchPattern = null;
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
        return replaceOnPaste(text, null);
    }

    @NotNull
    public String replaceOnPaste(@NotNull String text, @Nullable TextOffsetConsumer rangeConsumer) {
        if ((myOnPasteReplacementMap == null || myOnPasteReplacementMap.isEmpty()) && myOnPasteUserSearchPattern == null) {
            return text;
        }

        Pattern pattern = getOnPastePattern();
        Matcher matcher = pattern.matcher(text);

        int lastPos = 0;
        final StringBuilder sb = new StringBuilder();
        final ApplicationSettings settings = ApplicationSettings.getInstance();
        final boolean smartReplace = settings.isUserDefinedMacroSmartReplace();
        final BasedSequence chars = BasedSequence.of(text);
        CaseFormatPreserver preserver = new CaseFormatPreserver();
        int separators = settings.getPreserveOnPasteSeparators();
        final PrefixOnPastePatternType prefixPatternType = settings.getRemovePrefixOnPastePatternType();
        final String[] prefixes = settings.getPrefixesOnPasteList();
        final SuffixOnPastePatternType suffixPatternType = settings.getIgnoreSuffixOnPastePatternType();
        final String[] suffixes = settings.getSuffixesOnPasteList();
        int rangeIndex = 0;

        while (matcher.find()) {
            final int start = matcher.start();
            int end = matcher.end();
            if (lastPos < start) {
                sb.append(text, lastPos, start);
            }

            String searchText = matcher.group();
            String replace = myOnPasteReplacementMap == null ? null : myOnPasteReplacementMap.get(searchText);
            int startLength = sb.length();
            int replaceLength;

            if (replace != null) {
                sb.append(replace);
                replaceLength = replace.length();
            } else {
                // must be user text
                searchText = null;
                replaceLength = myOnPasteUserReplacementText.length();

                if (smartReplace) {
                    preserver.studyFormatBefore(chars, 0, start, end, prefixPatternType, prefixes, suffixPatternType, suffixes, separators);
                    String edited = sb + myOnPasteUserReplacementText + text.substring(end);
                    final TextRange range = new TextRange(startLength, startLength + replaceLength);
                    final BasedSequence chars1 = BasedSequence.of(edited);

                    InsertedRangeContext i = preserver.preserveFormatAfter(
                            chars1,
                            range
                            , settings.isPreserveCamelCaseOnPaste()
                            , settings.isPreserveSnakeCaseOnPaste()
                            , settings.isPreserveScreamingSnakeCaseOnPaste()
                            , settings.isPreserveDashCaseOnPaste()
                            , settings.isPreserveDotCaseOnPaste()
                            , settings.isPreserveSlashCaseOnPaste()
                            , settings.isRemovePrefixesOnPaste()
                            , settings.isAddPrefixOnPaste()
                            , settings.getRemovePrefixOnPastePatternType()
                            , settings.getPrefixesOnPasteList()
                            , settings.getIgnoreSuffixOnPastePatternType()
                            , settings.getSuffixesOnPasteList()
                    );

                    if (i == null) {
                        // as is
                        sb.append(myOnPasteUserReplacementText);
                    } else {
                        // extract the changed paste replacement
                        sb.append(i.word());
                        replaceLength = i.word().length();

                        if (i.getCaretDelta() > 0) {
                            // changed the next character(s), we grab it too
                            sb.append(edited.substring(startLength + myOnPasteUserReplacementText.length() - i.getCaretDelta()));
                            end += i.getCaretDelta();
                        }
                    }
                } else {
                    sb.append(myOnPasteUserReplacementText);
                }
            }

            if (rangeConsumer != null) {
                final TextRange replacedRange = new TextRange(startLength, startLength + replaceLength);
                final TextRange searchedRange = new TextRange(matcher.start(), matcher.end());
                rangeConsumer.accept(-1, null, 0, rangeIndex, searchedRange, replacedRange, searchText);
            }

            rangeIndex++;
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
            if (myOnPasteReplacementMap != null) {
                for (String search : myOnPasteReplacementMap.keySet()) {
                    sb.append(splice);

                    sb.append(SearchPattern.getPatternText(search, false, true));
                    splice = "|";
                }
            }

            if (myOnPasteUserSearchPattern != null) {
                sb.append(splice);
                sb.append(myOnPasteUserSearchPattern.getPatternText(!ApplicationSettings.getInstance().isUserDefinedMacroSmartReplace()));
            }

            myOnPasteSearchPattern = Pattern.compile(sb.toString());
        }
        return myOnPasteSearchPattern;
    }

    public void clearSearchFoundCarets() {
        Set<Long> excludeList = null;

        myCaretHighlighter.highlightCaretList(myFoundCarets, CaretAttributeType.DEFAULT, null);

        //noinspection ConstantConditions
        excludeList = CaretUtils.getExcludedCoordinates(excludeList, myFoundCarets);
        myCaretHighlighter.highlightCaretList(myStartMatchedCarets, CaretAttributeType.DEFAULT, excludeList);

        excludeList = CaretUtils.getExcludedCoordinates(excludeList, myStartMatchedCarets);
        myCaretHighlighter.highlightCaretList(myStartCarets, CaretAttributeType.DEFAULT, excludeList);

        myCaretSpawningHandler = null;
        myStartCaretStates = null;
        myStartCarets = null;
        myStartMatchedCarets = null;
        myFoundCarets = null;
        myCaretHighlighter.highlightCarets();
    }

    @Nullable
    public List<CaretState> getStartCaretStates() {
        return myStartCaretStates;
    }

    public @Nullable Set<Caret> getStartCarets() {
        return myStartCarets;
    }

    public @Nullable Set<Caret> getStartMatchedCarets() {
        return myStartMatchedCarets;
    }

    @Nullable
    public Set<Caret> getFoundCarets() {
        return myFoundCarets;
    }

    public void setSearchFoundCaretSpawningHandler(
            @Nullable final RangeLimitedCaretSpawningHandler caretSpawningHandler,
            @Nullable final List<CaretState> startCaretStates,
            @Nullable final Collection<Caret> startCarets,
            @Nullable final Collection<Caret> startMatchedCarets,
            @Nullable final Collection<Caret> foundCarets
    ) {
        myCaretSpawningHandler = caretSpawningHandler;
        myStartCaretStates = startCaretStates;
        setFoundCarets(foundCarets);
        setStartMatchedCarets(startMatchedCarets);
        setStartCarets(startCarets);
    }

    public void setSearchFoundCaretSpawningHandler(@Nullable final RangeLimitedCaretSpawningHandler caretSpawningHandler) {
        myCaretSpawningHandler = caretSpawningHandler;
    }

    @SuppressWarnings("ConstantConditions")
    private void setStartCarets(@Nullable final Collection<Caret> carets) {
        Set<Long> excludeList = null;
        excludeList = CaretUtils.getExcludedCoordinates(excludeList, myFoundCarets);
        excludeList = CaretUtils.getExcludedCoordinates(excludeList, myStartMatchedCarets);

        myCaretHighlighter.highlightCaretList(myStartCarets, CaretAttributeType.DEFAULT, excludeList);
        if (carets == null) {
            myStartCarets = null;
        } else {
            myStartCarets = new HashSet<>(carets.size());
            Caret myPrimaryCaret = myCaretHighlighter.getPrimaryCaret();
            getMatchedCarets(carets, excludeList, myPrimaryCaret, myStartCarets);
            myCaretHighlighter.highlightCaretList(myStartCarets, CaretAttributeType.START, excludeList);
        }
    }

    private void setStartMatchedCarets(@Nullable final Collection<Caret> carets) {
        Set<Long> excludeList = CaretUtils.getExcludedCoordinates(null, myFoundCarets);

        myCaretHighlighter.highlightCaretList(myStartMatchedCarets, CaretAttributeType.DEFAULT, excludeList);
        if (carets == null) {
            myStartMatchedCarets = null;
        } else {
            myStartMatchedCarets = new HashSet<>(carets.size());
            Caret myPrimaryCaret = myCaretHighlighter.getPrimaryCaret();
            getMatchedCarets(carets, excludeList, myPrimaryCaret, myStartMatchedCarets);
            myCaretHighlighter.highlightCaretList(myStartMatchedCarets, CaretAttributeType.START_MATCHED, excludeList);
        }
    }

    private void getMatchedCarets(@NotNull final Collection<Caret> carets, final Set<Long> excludeList, Caret myPrimaryCaret, final Set<Caret> matchedCarets) {
        for (Caret caret : carets) {
            if (excludeList != null && excludeList.contains(CaretUtils.getCoordinates(caret))) continue;

            if (myPrimaryCaret != null && myPrimaryCaret.equals(caret)) {
                myCaretHighlighter.setPrimaryCaret(null);
                myPrimaryCaret = null;
            }
            matchedCarets.add(caret);
        }
    }

    private void setFoundCarets(@Nullable final Collection<Caret> carets) {
        myCaretHighlighter.highlightCaretList(myFoundCarets, CaretAttributeType.DEFAULT, null);
        if (carets == null) {
            myFoundCarets = null;
        } else {
            myFoundCarets = new HashSet<>(carets.size());
            Caret myPrimaryCaret = myCaretHighlighter.getPrimaryCaret();

            for (Caret caret : carets) {
                if (myPrimaryCaret != null && myPrimaryCaret.equals(caret)) {
                    myCaretHighlighter.setPrimaryCaret(null);
                    myPrimaryCaret = null;
                }
                myFoundCarets.add(caret);
            }

            myCaretHighlighter.highlightCaretList(myFoundCarets, CaretAttributeType.FOUND, null);
        }
    }

    @Nullable
    public BitSet getIsolatedLines() {
        return myIsolationHighlightProvider.getHighlightLines();
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
        if (myEditor.isDisposed()) return;

        if (myHighlighter != null) {
            myHighlighter.removeHighlights();
            myHighlighter = null;
        }
    }

    @Nullable
    public Highlighter<ApplicationSettings> getHighlighter() {
        return myHighlighter;
    }

    public void updateHighlights() {
        if (myEditor.isDisposed()) return;

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

    @NotNull
    public Editor getEditor() {
        return myEditor;
    }

    @NotNull
    public EditorCaret getEditorCaret(@NotNull Caret caret) {
        return new EditorCaret(myPositionFactory, caret, getSelectionState(caret));
    }

    @NotNull
    public EditorCaretList getEditorCaretList() {
        return new EditorCaretList(this, myEditor.getCaretModel().getAllCarets());
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
        if (mySettings.isSearchCancelOnEscape() || mySettings.isPreservePrimaryCaretOnEscape()) {
            final IdeEventQueue.EventDispatcher eventDispatcher = LineSelectionManager.this::dispatchEscape;

            IdeEventQueue.getInstance().addDispatcher(eventDispatcher, this);

            myDelayedRunner.addRunnable(() -> IdeEventQueue.getInstance().removeDispatcher(eventDispatcher));
        }
    }

    private boolean dispatchEscape(@NotNull final AWTEvent e) {
        if (myEditor.isDisposed()) return false;

        if (e instanceof KeyEvent && e.getID() == KeyEvent.KEY_PRESSED) {
            if ((((KeyEvent) e).getKeyCode() == KeyEvent.VK_ESCAPE)) {
                final Component owner = ComponentUtil.findParentByCondition(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(), component -> component instanceof JTextComponent);

                if (owner instanceof JComponent) {
                    // register multi-paste if no already registered and remove when focus is lost
                    if (owner == myEditor.getContentComponent()) {

                        if (mySettings.isSearchCancelOnEscape()) {
                            List<CaretState> caretStates = getStartCaretStates();
                            if (caretStates != null) {
                                clearSearchFoundCarets();
                                myEditor.getCaretModel().setCaretsAndSelections(caretStates);
                                return true;
                            }
                        }

                        if (mySettings.isPreservePrimaryCaretOnEscape() && getEditor().getCaretModel().getCaretCount() > 1) {
                            myEditor.getCaretModel().removeSecondaryCarets();
                            myEditor.getSelectionModel().removeSelection();
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    void settingsChanged(@NotNull ApplicationSettings settings) {
        // unhook all the stuff for settings registration
        if (myEditor.isDisposed()) return;

        mySettings = settings;

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

        if (myCaretSpawningHandler != null) {
            //noinspection ReassignedVariable
            Set<Long> excludeList = null;
            myCaretHighlighter.highlightCaretList(myFoundCarets, CaretAttributeType.FOUND, null);

            //noinspection ConstantValue
            excludeList = CaretUtils.getExcludedCoordinates(excludeList, myFoundCarets);
            myCaretHighlighter.highlightCaretList(myStartMatchedCarets, CaretAttributeType.START_MATCHED, excludeList);

            excludeList = CaretUtils.getExcludedCoordinates(excludeList, myStartMatchedCarets);
            myCaretHighlighter.highlightCaretList(myStartCarets, CaretAttributeType.START, excludeList);
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

        addEscapeDispatcher();
    }

    private void hookListeners(ApplicationSettings settings) {
        // wire ourselves in
        if (isLineSelectionSupported()) {
            myEditor.getCaretModel().addCaretListener(this);
            myDelayedRunner.addRunnable("CaretListener", () -> myEditor.getCaretModel().removeCaretListener(this));

            if (myEditor.getProject() != null) {
                // NOTE: LineSelectionManager has lifespan of the editor, so when editor is disposed this listener will be disposed too
                Plugin.addEditorActiveLookupListener(myEditor, this, null);
                myDelayedRunner.addRunnable("ActiveLookup", () -> Plugin.removeEditorActiveLookupListener(myEditor, this));
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

    @SuppressWarnings({ "MethodMayBeStatic", "unused" })
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
    public void mouseClicked(@NotNull EditorMouseEvent e) {

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
    public RangeMarker getEditorSelectionRangeMarker() {
        if (myActionSelectionAdjuster.canSaveSelection()) {
            return myActionSelectionAdjuster.getEditorSelectionRangeMarker();
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

    private static boolean isControlledSelect(EditorMouseEvent e) {
        boolean ctrl = (e.getMouseEvent().getModifiersEx() & (MouseEvent.CTRL_DOWN_MASK)) != 0;
        return ctrl ^ (ApplicationSettings.getInstance().getMouseModifier() == MouseModifierType.CTRL_LINE.intValue);
    }

    @Override
    public void mouseReleased(EditorMouseEvent e) {
        if (e.getArea() == EDITING_AREA && !myEditor.getSettings().isUseSoftWraps() && !myEditor.isColumnMode()) {
            //int offset = myEditor.getCaretModel().getOffset();
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
    public void mouseEntered(@NotNull EditorMouseEvent e) {

    }

    @Override
    public void mouseExited(@NotNull EditorMouseEvent e) {

    }

    @Override
    public void mouseMoved(@NotNull EditorMouseEvent e) {

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
                int textLength = myEditor.getDocument().getTextLength();
                caret.setSelection(rangeLimit(startOffset, 0, textLength), rangeLimit(endOffset, 0, textLength));
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
    public void caretPositionChanged(@NotNull CaretEvent e) {
        myCaretGuard.ifUnguarded(() -> {
            Caret caret = e.getCaret();
            if (myMouseAnchor == -1 && caret != null) {
                myCaretHighlighter.updateCaretHighlights();
            }
        });
    }

    @Override
    public void caretAdded(@NotNull CaretEvent e) {
        int caretCount = myEditor.getCaretModel().getCaretCount();
        if (caretCount == 2) {
            Plugin.getInstance().updateEditorParameterHints(myEditor, true);
        }

        Caret caret = e.getCaret();
        if (myMouseAnchor == -1 && caret != null) {
            myCaretHighlighter.caretAdded(caret);
        }
    }

    @Override
    public void caretRemoved(@NotNull CaretEvent e) {
        int caretCount = myEditor.getCaretModel().getCaretCount();
        if (caretCount == 1) {
            // if caret count becomes 1 due to escape
            Plugin.getInstance().updateEditorParameterHints(myEditor, true);
        }

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
        myActionSelectionAdjuster.runAction(action);
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
