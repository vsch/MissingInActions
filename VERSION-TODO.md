## Missing In Actions

&nbsp;<details id="todo"><summary>**Table of Contents**</summary>

[TOC levels=3,6]: # "Version History"

### Version History
- [Next 1.8.0.xxx - Dev Builds](#next-180xxx---dev-builds)
- [Next 1.9.xx - Dev Builds](#next-19xx---dev-builds)
- [Next 1.8.0.26 - Dev Build](#next-18026---dev-build)
- [1.8.0.24 - Dev Build](#18024---dev-build)
- [1.8.0.22 - Dev Build](#18022---dev-build)
- [1.8.0.20 - Dev Build](#18020---dev-build)
- [1.8.0.18 - Dev Build](#18018---dev-build)
- [1.8.0.16 - Dev Build](#18016---dev-build)
- [1.8.0.14 - Dev Build](#18014---dev-build)
- [1.8.0.12 - Dev Build](#18012---dev-build)
- [1.8.0.10 - Dev Build](#18010---dev-build)
- [1.8.0 - Release](#180---release)
- [1.7.5.69 - Dev Build](#17569---dev-build)
- [1.7.5.67 - Dev Build](#17567---dev-build)
- [1.7.5.65 - Dev Build](#17565---dev-build)
- [1.7.5.63 - Dev Build](#17563---dev-build)
- [1.7.4 - Bug Fix Release](#174---bug-fix-release)
- [1.7.2 - Bug Fix Release](#172---bug-fix-release)
- [1.7.0 - Enhancement Release](#170---enhancement-release)
- [1.6.20 - Bug Fix Release](#1620---bug-fix-release)
- [1.6.18 - Bug Fix Release](#1618---bug-fix-release)
- [1.6.16 - Bug Fix Release](#1616---bug-fix-release)
- [1.6.14 - Bug Fix Release](#1614---bug-fix-release)
- [1.6.12 - Bug Fix Release](#1612---bug-fix-release)
- [1.6.10 - Bug Fix Release](#1610---bug-fix-release)
- [Next 1.6.8 - Bug Fix Release](#next-168---bug-fix-release)
- [Next 1.6.6 - Bug Fix Release](#next-166---bug-fix-release)
- [1.6.4 - Bug Fix Release](#164---bug-fix-release)
- [1.6.2 - Bug Fix Release](#162---bug-fix-release)
- [1.6.0 - Bug Fix & Enhancement Release](#160---bug-fix--enhancement-release)
- [1.5.0 - Bug Fix & Enhancement Release](#150---bug-fix--enhancement-release)
- [1.4.8 - Bug Fix Release](#148---bug-fix-release)
- [1.4.6 - Bug Fix Release](#146---bug-fix-release)
- [1.4.4 - Bug Fix Release](#144---bug-fix-release)
- [1.4.2 - Enhancement Release](#142---enhancement-release)
- [1.4.0 - Bug Fix Release](#140---bug-fix-release)
- [1.3.0 - Bug Fix Release](#130---bug-fix-release)
- [1.2.0 - Enhancement Release](#120---enhancement-release)
- [1.1.7 - Enhancement Release](#117---enhancement-release)
- [1.1.6 - Enhancement Release](#116---enhancement-release)
- [1.1.5 - Enhancement Release](#115---enhancement-release)
- [1.1.4 - Enhancement Release](#114---enhancement-release)
- [1.1.3 - Bug Fix & Enhancement Release](#113---bug-fix--enhancement-release)
- [1.1.2 - Enhancement Release](#112---enhancement-release)
- [1.1.1 - Enhancement Release](#111---enhancement-release)
- [1.1.0 - Enhancement Release](#110---enhancement-release)
- [1.0.0 - Bug Fixes and Enhancements](#100---bug-fixes-and-enhancements)
- [0.8.3 - Bug Fixes and Enhancements](#083---bug-fixes-and-enhancements)
- [0.8.2 - Bug Fixes and Enhancements](#082---bug-fixes-and-enhancements)
- [0.8.0 - Bug Fixes and New Features](#080---bug-fixes-and-new-features)
- [0.7.2 - Refactoring and Code Cleanup](#072---refactoring-and-code-cleanup)
- [0.7.0 - Enhancements](#070---enhancements)
- [0.6.2 - Bug Fix and Features](#062---bug-fix-and-features)
- [0.6.1 - Bug Fix and Features](#061---bug-fix-and-features)
- [0.6.0 - Bug Fix and Features](#060---bug-fix-and-features)
- [0.5.1 - Bug Fix](#051---bug-fix)
- [0.5.0 - Initial Release](#050---initial-release)

&nbsp;</details>

### Next 1.8.0.xxx - Dev Builds

+ [ ] Fix: batch search/replace:
  + [ ] Add: export/import a single profile to batch search/replace management as opposed to all
        profiles. Keeping all profiles per project and importing/exporting a single profile
        between projects is useful.
    + [ ] Add: copy profile under a new name and/or to another open project batch search window,
          with overwrite prompting.
  + [ ] Add: separate `highlight in editor` buttons to search and replace editors to allow
        having both of these colored with the same colors to allow using color alignment for
        validation
  * [ ] Add: line marker icons to search editor allow toggling `!` error and `-` unused coloring
        for keywords
+ [ ] Add: per language configuration of what constitutes an identifier character set to
      override Java default for custom word actions for languages other than Java (dynamically
      based on file language type)
  + [ ] Add: default base prefix/suffix for the language to use when none in per language number
        format settings
  + [ ] Add: reserved word list detection for preserve on paste. If pasting over reserved word
        then don't make any changes to the pasted content.
+ [ ] Fix: toggle spawn search options should not commit carets but force a change in search. So
      starting a search with smart prefix mode and toggling smart prefix mode should result in
      the same thing as toggling smart prefix mode then doing spawn search.
  + [ ] Fix: toggle spawn search smart prefix
  + [ ] Fix: toggle spawn search match boundary
+ [ ] Fix: selecting to top of file should select to start of line if caret is already at top or
      using line mouse selection.
+ [ ] Fix: selecting to bottom (when no terminating EOL) should select to end of line if caret
      is already on last line or using line mouse selection.
+ [ ] Add: save isolation ranges in editor state
+ [ ] Add: change the SmartKeepLineCarets action to first keep code lines, if all carets are
      already on code lines then remove those whose code lines contain nothing but brackets,
      parentheses, braces, commas and semicolons. This will allow to quickly isolate lines that
      require editing without changing the selection to remove carets from lines that contain
      only context and termination elements.
* Highlighted word actions:
  + [ ] Add: highlight words from Clipboard selected carets (should use history dialog with just
        a button that will use caret data for highlights).
  + [ ] Add: Sort highlighted word colors (by their content case sensitive/insensitive/
  + [ ] Add: Copy highlighted words in file/selection, to copy as caret selections all unique
        highlights which are present in the current file to the clipboard.
* Fix: number generator
  + [ ] Fix: automatically switch dialog to correct base hex, use current setting's prefix to
        determine base.
    + [ ] Add: per language base prefix/suffix settings to allow prefix/suffix by language
    + [ ] Fix: generate number to use current selection to get starting number if it is numeric
          after stripping prefix/suffix
* [ ] Add: status bar with information about selection: lines, code, comment and blank line
      count.
* [ ] Add: Readme and Wiki Write up of Paste from History enhancements.
* [ ] Add: `add/remove caret above/below` actions which do not try to preserve same range the
      way IDE actions for these do.
  * Up: if primary is top: add caret above, if bottom remove and move up, if neither move it to
    the top.
  * Down: if primary is top, remove top and move down, if bottom then extend down, if neither,
    then move to bottom.
* [ ] Fix: add/remove highlight word should use current word at caret if no carets have
      selections
* [ ] Fix: need straighter carets to work with selections so can trim selections to straight
      line.
* Fix: search spawn carets, if the preceding chars form a smart prefix the pattern should not
  include that prefix in the list of matches, otherwise matches at start.
  * [ ] Fix: really need to handle it like paste and examine each found instance to see if it is
        really a match. Then can also implement case changes for the match.
  * [ ] Add: add all smart paste case variations in searching spawn carets. ie. remove prefix
        and generate selected case variations.

### Next 1.9.xx - Dev Builds

+ [ ] Add: option and functionality to change duplicate lines action to only operate on a line
      once even if there are many carets present on that line. ie. Remove duplicate carets on
      the line before the action and re-create them after the action
+ [ ] Fix: backspace to line indent should backspace to beginning of line if at or before
      indent.
+ [ ] Add: option to detect trailing `,` and `;` on previous lines when duplicating single line
      and add to line above automatically and remove from last duped line if original did not
      have this suffix.
+ [ ] Add: option to remove selection after copy when starting with no selection one is created
      by the action.
+ [ ] Fix: [ToDo_StackTraces.txt](ToDo_StackTraces.txt)
+ [ ] Fix: keep code carets does not remove carets in Java multi-line comments
+ [ ] Fix: if pasting adjustment after removing/adding prefixes results in the pasted text to be
      the same as original then paste unmodified from the clipboard.

### Next 1.8.0.26 - Dev Build

* Fix: project view node highlight to use bold style for matched text

### 1.8.0.24 - Dev Build

* Fix: update to split flexmark-util

### 1.8.0.22 - Dev Build

* Fix: update to latest flexmark-util

### 1.8.0.20 - Dev Build

* Fix: update to latest flexmark-util

### 1.8.0.18 - Dev Build

* Fix: project view highlighting to match partials, case insensitive and use word boundary.

### 1.8.0.16 - Dev Build

* Add: option to use word highlights to highlight Project View nodes. Only full case sensitive
  match of the selected word is used for the match. No partial matches or case insensitive is
  supported at the moment. If the node text matches the highlighted word then the node is
  highlighted.

### 1.8.0.14 - Dev Build

* Fix: do not restore last pasted text selection if the clipboard content does not contain any
  caret information.

### 1.8.0.12 - Dev Build

* Fix: update to latest flexmark-util

### 1.8.0.10 - Dev Build

* Fix: refactor code to use `BitFieldSet` for `TypedRangeHighlightProvider` flags and make
  `WordHighlightProvider` flags inherit ide highlight flags so as not to conflict with them.
* Fix: Remove highlighted word does not work when from saved highlights on IDE exit. Workaround:
  highlight word again and then remove highlight.
* Fix: index out of bounds on string rep computation
* Fix: paste from history did not convert single line character content to line content
* Fix: change line/caret toggle to go from character selection to carets without intervening
  line selection
* Fix: 2020.1 compatibility, Should be called at least in the state COMPONENTS_LOADED

### 1.8.0 - Release

* Fix: change since build to `193`

### 1.7.5.69 - Dev Build

* Fix: update flexmark-java libs

### 1.7.5.67 - Dev Build

* Fix: update flexmark-java libs
* Fix: disposing of batch search replace window,form and editors on project close.

### 1.7.5.65 - Dev Build

* Fix: update flexmark-java libs

### 1.7.5.63 - Dev Build

* Fix: more missed disposables

#### 1.7.5.61 - Dev Build

* Fix: move components to services
* Fix: missed dispose disposables

#### 1.7.5.59 - Dev Build

* Fix: add app version based application service/component requesting

#### 1.7.5.57 - Dev Build

* Fix: remove deprecated icon use
* Fix: disposed editor accessed on settings change.

#### 1.7.5.55 - Dev Build

* Fix: exception caused by trying to set highlight provider on editor for isDefaultProject()
  true.
* Fix: editors double release
* Fix: search spawn carets numeric only worked if spawn search word boundaries was enabled.

#### 1.7.5.53 - Dev Build

* Fix: search spawn carets, if the preceding chars form a smart prefix the pattern should not
  include that prefix in the list of matches, otherwise matches at start.
* Fix: index exception when deleting clipboard history in paste from history
* Fix: NPE caused by editor in settings open for default project.

#### 1.7.5.51 - Dev Build

* Fix: NPE in batch search replace
* Fix: word Highlighter to test first/last char of word being `\w` before respecting provided
  boundary flags. Otherwise no highlights will be done for the pattern.
* Fix: change number generator for shift sequences so that if start is 0, then first number is
  0, next number is 1 if step is >=0 and 0x8000_0000_0000_0000 if <0.
  * Fix: change number generator to rollover from 1 to 0x8000_0000_0000_0000 when step <0 and
    0x8000_0000_0000_0000 to 1 when >=0
* Add: option `Preserve primary caret on ESCAPE` to prevent the IDE from loosing primary caret
  information on ESCAPE clearing multi-carets.
* Fix: on search spawn carets, leave the primary caret where it was an make spawned carets
  secondary. That way ESCAPE will leave the primary where it was.
* Fix: batch search/replace highlights would not be shown in tool window editors like search,
  etc.
* Fix: update flexmark-java, change deprecated method use.

#### 1.7.5.49 - Dev Build

* Fix: update to flexmark-java utils 0.59.50

#### 1.7.5.47 - Dev Build

* Fix: backward search on `\\` generated pattern syntax exception. Reverse regex Dev Build
  handle odd backslash quoted `\Q\\E` sequences.
* Fix: batch search replace update of paste from clipboard button state would hand the IDE
  trying to get OS X clipboard. Just left the button enabled for now.

#### 1.7.5.45 - Dev Build

* Fix: word highlighter to sort in reverse order

#### 1.7.5.43 - Dev Build

* Fix: update `flexmark-java-utils` to 0.59.42

#### 1.7.5.41 - Dev Build

* Fix: missing setting for spawn search boundary matching.

#### 1.7.5.39 - Dev Build

* Add: option for spawn search to ignore backward word boundaries. ie. Forward search ignores
  begin word break, backward search ignores end word break.
* Fix: option for case insensitive highlights to actually disable case sensitivity for
  highlights and show words in color of first selected word color.
* Add: option for word highlighting to not respect word boundary from the selection. When not
  matching boundary `abc` highlight will match `abc` in `abcd` and `zabc` and `zabcd`.

#### 1.7.5.37 - Dev Build

* Fix: preserve caret line to closest available after smart remove carets or remove
  blank/non-blank/comment, etc.

#### 1.7.5.35 - Dev Build

* Fix: update flexmark-utils

#### 1.7.5.33 - Dev Build

* Fix: when toggling line/carets try to keep the primary caret position closest to offset before
  action.
  * when creating multi-carets from selection make the primary caret closest to the primary
    caret before the action.
  * when toggling selection keep caret offset at primary caret offset
  * when accepting found/not-found search carets

#### 1.7.5.31 - Dev Build

* Highlighted word actions:
  * Fix: automatically turn on words highlight mode when adding a highlighted word
  * Add: Select highlighted words in file or current selection to create multi-caret selection
    from highlighted words. Will use document, single selection or multi-caret selection for
    limiting search of highlighted words.
  * Add: `Add Selection to Word Highlights in Tandem` will tandem highlight words on the same
    line to the same color. Either when originally highlighted or added to existing highlights
    when there is a highlight on the same line. Allows to color align related string on the same
    line for visual validation elsewhere. A limitation is that a word pattern can only have one
    color associated with it, so multiple tandem color of the same word will result it in being
    the last tandem color assigned.
* Fix: spawn search for smart paste to add prefixed variations if current word has no prefix.
* Add: save highlight state to local settings, restore on IDE startup.
* Fix: merged transferable would erroneously append EOL to multi-line char caret content causing
  it to turn into a line selection.
* Add: `Delete Replaced Content after Paste` paste from history option to delete generated
  content after pasting
* Add: auto delete after paste macro replaced clipboard content if option enabled
* Fix: preserve on paste to properly handle double prefix and not remove:
  * paste `isStart` on `getIsTask` should result in `getIsStart` not `getStart`
  * paste `myIsStart` on `isTask` should result in `isStart` not `isIsStart`
* Fix: move line selection up/down with:
  * No change: column changes to 0 when caret at start of line (caused by exception in update
    after move line handler in IDE injected language parsing)
  * To start/To End: column changes between start/end because it did not distinguish between
    line/non-line selections.
  * Remove: anchor/anti-anchor settings, they don't make sense and make the caret toggle with
    every move.
* Fix: toggle direction of selection to set all directions to same orientation, if different
  orientations set all to end.
* Fix: keep/remove carets actions to convert selection to carets before their operation.
* Fix: caret invalidation causing caret invalid assertion failure.
* Fix: search spawn without matching all numeric patterns needs to respect begin/end word
  constraints of the caret position if it would be a numeric search if spawn numeric was
  enabled. ie. search forward from `|` in `2.9.0.5|7/2.9.7.57` should result in
  `2.9.0.5[7]|/2.9.7.5[7]|` not `2.9.0.5[7]|/2.9.[7]|.5[7]|`
* Fix: backward caret spawning search ignored numeric/hex numeric settings
* Add: spawn prefix search option to support paste prefix alternates so spawn on `myText` will
  select `myText`, `ourText`, `getText`, `isText`, `setText` if `my`, `our`, `get`, `set` and
  `is` are preserved prefixes.
* Fix: snake case to recognize capitalized first letter(s), pasting on:
  * `This_item_name` will change pasted text from `ThatTextString` to `That_text_string`
  * `This_Item_Name` will change pasted text from `ThatTextString` to `That_Text_String`
  * This applies to all case styles: dash, dot, snake, slash. Will preserve: first cap style
    (first letter capitalized, rest lowercase), mixed case style (first letter of each word
    capitalized), lower case or screaming case.
* Fix: paste from history
  * Fix: paste spliced & quoted to work if current editor is a plain Swing editor (text boxes)
  * Add: `To Line` action to convert multiple selection to merged **line** content without
    having to paste it. Opposite of `To Carets`
  * Add: allow preview macro replacement results to be selected and copied as new clipboard
    content.
  * Fix: user macro variable replacement and macro variable replacement are now two independent
    options.
  * Fix: user variable case sensitive only if not using smart paste options for replacement.
  * Fix: replacements from clipboard would duplicate for every caret in replacement content even
    when nothing matched---confusing. Now ignore clipboard
  * Add: search highlighting if replacing macro variables or user variable in pasted text is
    enabled.
  * Add: option to show text after replacements, with replaced regions highlighted.
  * Add: `__Filepath__` variants to replaced variables to replace with file path which will be
    pasted into.
    * `__Filepath__` : file path as is.
    * `__FILEPATH__` : file path all caps.
    * `__filepath__` : file path lowercase.
    * `__File-path__` : file path with leading `/` removed and `/` replaced by `-`.
    * `__FILE-PATH__` : file path as above but in all caps.
    * `__file-path__` : file path as above but in lowercase.
    * `__File.path__` : file path with leading `/` removed and `/` replaced by `.`.
    * `__FILE.PATH__` : file path as above but in all caps.
    * `__file.path__` : file path as above but in lowercase.
  * Fix: replacement of user macro would not be done if there were not macro variables defined.
  * Fix: user replace macro with clipboard content would fail because of index mismatch.
* Fix: non-regex prefix set to followed by uppercase letter had erroneous test. When pasting
  `projectSettings` on to `settingsText` results in `setProjectSettings`
* Fix: update to latest plugin-utils.
* Fix: extract highlight code core to plugin-utils.
* Fix: invalid caret validation after edit causing Caret is invalid state exception
* Fix: update to flexmark-util 0.50.42

### 1.7.4 - Bug Fix Release

* Fix: [#23, Plugin changes built-in IDEA behavior]
  * Disable caret adjustments for cut/duplicate line if line adjustments are disabled
  * Disable caret adjustments for cut if delete line selection caret adjustments are disabled.
* Fix: update to latest plugin-util
* Fix: handle spurious IOException in shared transferable
* Add: `resources/search/searchableOptions.xml` for full text search across all configuration
  settings.
* Fix: too strict clipboard content comparison during replacement would not replace shared
  content in some circumstances. Replaced with string comparison of clipboard content.
* Fix: Moved png and svg icons to different sub-directories. IDE versions 2017 generates a gray
  scale icon from svg generated by Illustrator but it will try to load svg icon when png is
  requested if both are in the same directory.

### 1.7.2 - Bug Fix Release

* Fix: clipboard sharing code prevented duplicated caret content from being automatically
  deleted.
* Fix: When `User defined macro` is disabled but selected, paste from history does duplication
  by whatever clipboard content is selected.
* Fix: when adding/removing/clearing highlighters using actions, hide batch search/replace
  window.
* Fix: change `Without Formatting` to `No Formatting`, shorten to take less space.
* Add: tooltip text to paste from history buttons.
* Add: highlights of significant options when mouse hovers over corresponding button.

### 1.7.0 - Enhancement Release

* Fix: shortened all clipboard history paste buttons to remove common `Paste` prefix
* Add: separate `Quoted Spliced` button to eliminate sticky quoted flag affecting `Spliced`
* Add: multiple caret clipboard content is now shared between IDE instances so copying in one
  and pasting in another does not loose the clipboard caret information.
* Fix: incorrect paste location for duplicate for carets and paste if pasting in trailing
  blanks. Cannot reproduce.
* Fix: incorrect duplicate for carets result if primary caret has selection and is not at the
  start or end of its selection.

### 1.6.20 - Bug Fix Release

* Fix: clear out empty stored selections
* Add: option to turn off IDE parameter info when using multi-caret mode
* Fix: handling of `beforeActionPerformed` without corresponding `afterActionPerformed`.
  Otherwise, the plugin would think all subsequent actions are nested actions and not handle
  selection storage/recall or do proper cleanup in afterActionPerformed.

  This happens on action exception or premature before call then no actual `actionPerformed`
  call on the action, like `com.intellij.openapi.actionSystem.impl.ActionButton.performAction`
  which fires before action then checks to see if there is a context component and if not
  returns without corresponding `actionPerformed` on the action and `afterActionPerformed`
  callbacks.
* Fix: update to latest libs
* Fix: missing toolbar button for `Recall selection from list`

### 1.6.18 - Bug Fix Release

* Fix: switch to flexmark-util.html.ui helpers

### 1.6.16 - Bug Fix Release

* Add: plugin logo
* Add: option for spawn carets for digits to use: standard spawn, base 10 digit select, hex
  digit select.
* Change: refactor code to common plugin-util library
* Fix: bump up dependencies to newer versions
* Fix: reduce tool window border
* Fix: settings prefixes on paste pattern to always proper regex regardless of what the pattern
  type.

### 1.6.14 - Bug Fix Release

* Fix: 2016.3 paste from history compatibility

### 1.6.12 - Bug Fix Release

* Fix: 2018.3 EAP API Change
* Recompile for 2018.3 EAP
* Fix: exception in batch search/replace tandem mode delete with empty text pane

### 1.6.10 - Bug Fix Release

* Fix: NPE in plugin content handling

### Next 1.6.8 - Bug Fix Release

* Fix: #22, Clipboard history: ignore empty copy/paste entries (No characters, only whitespaces
  and line returns). Add new option "Keep only latest blank clipboard content" in Settings >
  Missing In Actions > Paste. When enabled, only the latest blank clipboard content is kept in
  history.

  **NOTE:** removal of old blank content is done when empty content is copied to the clipboard.

### Next 1.6.6 - Bug Fix Release

* Fix: batch search/replace window exception in DataGrip, when no project use user home dir.
* Fix: Enable batch replace tool window to work in dumb mode
* Fix: make default directory for export/import batch search replace files, one directory above
  `.idea` if project file parent dir has that name.

### 1.6.4 - Bug Fix Release

* Fix: release error

### 1.6.2 - Bug Fix Release

* Fix: 2016.3 compatibility
* Fix: error attribute used was only defined in 2017, changed to ERRORS_ATTRIBUTES
* Fix: update for latest API
* Fix: tool window icon to 13x13

### 1.6.0 - Bug Fix & Enhancement Release

* Fix: recall selection from list action not working
* Fix: deleting all lines in batch replace tandem mode
* Fix: add index validation to stashed range marker removal
* Add: batch replace word options for error highlight (!) and warning highlight (?) to allow
  flagging error and warnings as search strings
* Fix: batch replace options lost leading blank lines when saved or exported options
* Fix: after paste adjustment range out of bounds exception
* Fix: disabling `To Carets` button in multi-paste dialog on opening dialog
* Add: paste w/o formatting action to multi-paste dialog
* Add: batch replace window test for caret offset being in found range before replacing. Fixes
  issue with mouse click action assigned to
* Add: convert to carets action in paste selection dialog to convert multi-line clipboard
  contents to multi-caret selection.

### 1.5.0 - Bug Fix & Enhancement Release

* Add: `-` to batch search options, when it is at beginning of line disables the row from search
  replace ops. Quick way to disable some and enable others. Really need a dropdown or check box
  to control this in the margin.
* Add: copy batch search regex to clipboard to allow global search using IDE functionality
* Fix: carets keep/drop not checking if a file has a `virtualFile()` before trying to get
  `PsiFile`
* Add: `Replace with Selection Text` and `Replace with Selection Text from List` actions to
  replace currently selected text with text from recalled selection.
* Change: replace batch replace highlights checkbox and tandem edit checkbox with toggle buttons
  to save space.
* Add: batch replace sort up/down for search and replace editors. Case sensitivity for the sort
  controlled by the `Case` checkbox.
* Add: batch replace `Tandem Edit` mode, deleting or inserting lines in one editor panel
  replicates the action to the other two. Allows to keep the search/replace/option lines aligned
  when adding/removing lines.
* Fix: batch replace to set pending if replace invoked but not enabled, to allow mouse click for
  replace. If word is found then will replace. Resets pending in 250 ms.
* Fix: batch replace to disable replace/replace all/exclude if document is not writable.
* Add: after batch replace Next, Prev, Replace, Replace All, Exclude/Include and Reset, give
  focus back to the editor.
* Fix: batch replace Case and Word checkboxes did not update highlights when their value
  changed.
* Change: select word and move to next will select this word or if not identifier, the previous
  word. Reverse for select prev word: select this word if not identifier then select next word
  and move to previous word.
* Fix: different camel humps mode delete would consider trailing underscores as part of the last
  word part. Now stops at underscores.
* Fix: disposed editor being passed through to active editor for batch replace tool window
* Add: Select word and move to next/previous word. If caret not on identifier then first move it
  to next/prev identifier then perform action.
* Add: Camel Humps and Different Camel Humps mode versions of custom delete/backspace actions.
* Add: Next/Previous highlighted word action to move caret to next/previous highlighted word
* Fix: batch replace all was too slow trying to use `findPrev()`. Now does does not update
  highlights or caret. Just modifies document.
* Add: Toggle on paste preserve action and setting to disable on paste preserve case
  functionality without having to change the options when it gets in the way.
* Fix: NPE when caret spawning/searching from active caret search
* Fix: reduce toolbar size by moving most actions to popup menus: isolation, highlights, carets.
* Fix: saving a new preset would reset the preset name and load the search/replace options for
  the first preset in the preset list.
* Fix: refactor highlight and isolation line code into re-usable classes.
* Fix: check box with color button rendering, add margin, border to color button and
  anti-aliased drawing of border.
* Fix: file editor to editor conversion to work properly, FileEditor -> TextEditor ->
  .getEditor().
* Add: highlights and caret change handling for batch search/replace.
* Add: tool window for batch search/replace instead of modal
* Add: presets and import/export to batch search/replace to allow selecting a set of presets.
* Add: customizable backspace and delete actions with variations: spaces, alternating word, word
  excluding 1 space, and word. All are customizable with regex for selecting what is deleted.
* Add: Batch search/replace action and dialog. Each line in search will be replaced by
  corresponding line in replace. Third pane is options, each is a character: `c` - case
  sensitive, `i` - case insensitive, `w` - word (same as `be`), `e` - end word boundary, `b` -
  begin word boundary.
* Fix: toolbar combo action incompatibility with 2018.2 intellij-community master branch
* Fix: plugin description forgot to update in plugin.xml for 1.4.8
* Change: change caret search regex for multi-caret search on non-identifier character c to c
  instead of exact c count, and caret spawning regex to match the exact char count with
  preceding and following character not being c to prevent finding stretches with greater count.

### 1.4.8 - Bug Fix Release

* Fix: on paste add prefixes would only be honored if remove prefixes was enabled
* Fix: camel case preserve paste, adding a non-letter/digit prefix to not uppercase first letter
  of pasted content. To fix auto prefixing of `$` for php when pasting non-variable name over
  variable.
* Add: Single line caret spawning search now enabled with carets on multiple lines if at least
  one line has more than one caret. This allows the spawning carets to spawn on multiple lines,
  extending usefulness of this beyond single line search spawn.
* Add: Single line caret spawning search now limits the spawned carets to the existing selection
  of the caret, if there is one. This allows spawning carets in a limited text range.
* Add: Smart Keep/Remove carets add remove non-selection carets if both selection and
  non-selection carets exist.
* Add: paste selection dialog options to join lines/multiple caret content and optionally quote
  individual items with open quote, close quote and delimiter text customizable.
* Add: multi-caret actions: keep-carets with selection and keep carets without selection.
* Add: clear isolated lines action to clear isolation. Select All, add to isolated is
  un-intuitive.
* Fix: numbering without selections for carets would mess up selections.
* Fix: exception when inserting number sequence with last caret being on the last line of the
  document without an EOL.
* Change: search spawning caret action when non-identifier or space now will search for a span
  of matching characters. For example caret on `&&` will spawn a caret on every occurrence of
  `&&` instead of just `&` as before which produced a useless result 99% of the time.
* Fix: caret spawning search forward/backward regex when word starts or ends on `$` which regex
  does not consider an identifier character so `\b` does not handle the word break properly.
* Fix: Highlights sorted in reverse length order to allow longer matches to succeed before
  possibly shorter sub-strings.
* Fix: @NotNull argument must not be null exception.

### 1.4.6 - Bug Fix Release

* Fix: change "Recall Selection from List" to "Recall Selection" to shorten toolbar real-estate
* Fix: change "Swap Selection Text from List" to "Swap Text" to shorten toolbar real-estate
* Fix: newly opened files would not show word highlights until they were modified.

### 1.4.4 - Bug Fix Release

* Fix: removed "Swap Text" action which did nothing because it was for internal use. Added "Swap
  Selection Text from List" which was the original intention to show the available list of
  selections to use for swapping currently selected text and one selected from a list.
* Fix: removed "Selections" action which did nothing because it was for internal use. Added
  "Recall Selection from List" which was the original intention to show the available list of
  selections to recall one of them.
* Fix: renamed "Pop Last Selection" to "Recall Last Selection"
* Fix: editor not disposed in renumber dialog

### 1.4.2 - Enhancement Release

* Add: replace arbitrary string to another string on paste. Useful for quick pasting of template
  code with changes without needing to edit after paste (treat this as a user provided macro
  variable).

  Can add RegEx search or plain string. Plain string search is case sensitive.
* Add: replace user string on paste and duplicate for every character caret of another content.
  Plain string search is not case sensitive.

  For example: if pasting `int FormatAbc(int abc) { myAbc = abc; return myAbc; } ` with the user
  search string set to `Abc`, with clipboard data for the replacement contains 3 carets with
  `def`, `myHij` and `setKlmnop` then the paste will result in the following being pasted in:

  ```
  int FormatDef(int def) { myDef = def; return myDef; }
  int FormatHij(int hij) { myHij = hij; return myHij; }
  int FormatKlmnop(int klmnop) { myKlmnop = klmnop; return myKlmnop; }
  ```
* Add: Replace Macro Variables on Enhanced paste and on duplicate for carets paste. Currently
  only file name derivations are supported. When pasting in a file with name
  `multi-line-image-url` the following will be changed as shown:
  * `__Filename__` to `multi-line-image-url` (as is)
  * `__FILENAME__` to `MULTILINEIMAGEURL` (uppercase)
  * `__filename__` to `multilineimageurl` (lowercase)
  * `__FileName__` to `MultiLineImageUrl` (pascal case)
  * `__fileName__` to `multiLineImageUrl` (camel case)
  * `__file-name__` to `multi-line-image-url` (dash case)
  * `__FILE-NAME__` to `MULTI-LINE-IMAGE-URL` (screaming dash case)
  * `__file.name__` to `multi.line.image.url` (dot case)
  * `__FILE.NAME__` to `MULTI.LINE.IMAGE.URL` (screaming dot case)
  * `__file_name__` to `multi_line_image_url` (snake case)
  * `__FILE_NAME__` to `MULTI_LINE_IMAGE_URL` (screaming snake case)
  * `__file/name__` to `multi/line/image/url` (slash case)
  * `__FILE/NAME__` to `MULTI/LINE/IMAGE/URL` (screaming slash case)

### 1.4.0 - Bug Fix Release

* Fix: NPE when projects are being rapidly opened and closed.
* Fix: #17, Caret don't move across tab-indented lines
* Fix: Remove highlighted word carets would not remove the last selection if all carets
  contained highlighted word selections
* Fix: size of color chip when using HiDPI displays that need scaling.

### 1.3.0 - Bug Fix Release

* Fix: #16, Hide disabled buttons breaks Recall Selection List and Swap Selection actions
* Fix: #15, Selection continuation with the mouse and Shift modifier is broken
* Add: multi-caret search accept not found carets action to allow excluding carets with matching
  search position.

### 1.2.0 - Enhancement Release

* release

### 1.1.7 - Enhancement Release

* Fix: Exclude $ from being considered as part of identifier for purposes of determining word
  start/end boundary for highlighted words.
* Fix: conversion from dash, dot and slash to snake case was not working.
* Add: hide disabled toolbar buttons option to settings
* Add: button versions of recall selection and swap text to eliminate button text to save
  toolbar real-estate

### 1.1.6 - Enhancement Release

* Change: make `Line Selection Mode`, `Forward Search Caret Spawning` and `Backward Search Caret
  Spawning` actions toggle actions to show when active.
* Fix: line selections not working in editor text fields in settings
* Add: Word Highlighting Actions, toolbar buttons and settings:
  * Toggle highlight word mode
    ![Toggle Word Highlights](/resources/icons/png/Toggle_word_highlights%402x.png) : turns
    highlighted words on/off. Can be used to turn off highlights without clearing highlight word
    list.
  * Toggle highlight word case sensitive mode
    ![Toggle Case Sensitive Highlights](/resources/icons/png/Toggle_case_sensitive_highlights%402x.png)
    \: toggles highlight word case sensitive matching on/off.
  * Clear highlighted words
    ![Clear Word Highlights](/resources/icons/png/Clear_word_highlights%402x.png) : clears all
    highlighted words.
  * Add selection to highlighted words
    ![Add Word Highlight](/resources/icons/png/Add_word_highlight%402x.png) : adds the current
    carets' selection to list of highlighted words.
  * Remove selection from highlighted words
    ![Remove Word Highlight](/resources/icons/png/Remove_word_highlight%402x.png) : adds the
    current carets' selection to list of highlighted words.
  * Keep carets whose selections are highlighted words
    ![Keep Word Highlighted Carets](/resources/icons/png/Keep_word_highlighted_carets%402x.png)
    \: removes all carets without selection or whose selection text is not a highlighted word.
  * Remove selection from highlighted words.
    ![Remove Word Highlighted Carets](/resources/icons/png/Remove_word_highlighted_carets%402x.png)
    \: removes all carets whose selection text is a highlighted word.
  * Setting allows to define the background colors to be used for highlighted words based on
    hue, saturation and brightness boundaries and steps.

    ![Assets Tools Settings Extras](/assets/images/ToolsSettings_WordHighlights.png)

### 1.1.5 - Enhancement Release

* Change: Recall selection from list action text from `Recall Selection` to `Selections` to
  shorten the toolbar button.
* Change: split settings into tabbed pane per category

### 1.1.4 - Enhancement Release

* Add: Line Isolation Mode to "highlight" the isolated lines by "lowlighting" the non isolated
  lines. Especially useful when duplicating a method or methods for modification. This allows
  isolating the copies which are to be modified so that they are not confused with the
  originals.
* Add: Dark scheme color persistence. Colors in settings reflect the current Dark/Light scheme
  selection.

### 1.1.3 - Bug Fix & Enhancement Release

* Change: allow overlapping selection text swapping by eliminating overlapping part of
  selections from the swap.
* Fix: exception in some cases when swapping text and more than set limit of stored selections
  is already present.

### 1.1.2 - Enhancement Release

* Fix: bump up version compatibility to 162.*

### 1.1.1 - Enhancement Release

* Fix: exception caused by api change

### 1.1.0 - Enhancement Release

* Add: Selection Stack to automatically store the last N selections for a file
* Add: Recall/Swap selection actions and toolbar buttons
* Add: Swap Selection Text actions and toolbar buttons to swap currently selected text with text
  from a stored selection.

### 1.0.0 - Bug Fixes and Enhancements

* Fix: toolbar icons for consistency and bigger direction arrows.
* Add: caret search action detects when caret context is on a hex, decimal or octal number.
* Change: make MIA application settings shared so that they can be imported and exported.
* Add: `Tab Align Text at Carets` to push non-whitespace text after each caret so that all
  carets are aligned and on a tab stop. Quick way to tab align jagged carets and following text.
* Add: MissingInActions toolbar
* Add: edit caret search options with dialog to show preview of caret search by pattern.
* Add: caret visual attributes for start search carets that have a match on the line to
  highlight start locations which have matches.
* Add: `Accept Found Search Carets` action to only leave the found caret locations.
* Add: `Cancel Found Search Carets` action and settings option to `Cancel Caret Search on
  ESCAPE` to cancel caret search and restore carets to only the start search location carets.
* Add: **dash-case**, **dot.case** and **slash/case** to on paste preservation options
* Add: multi-caret filtering with Forward/Backward Spawn Caret Action when invoked with multiple
  carets. Also does color and weight change if 2017 EAP.
* Add: number generating action. For now only for multiple carets.
  * Sequences 0-9, A-Z for number bases 2-36
  * Prefix/Suffix options to add to generated number
  * Sequences can be 0 left filled to any width
  * Arithmetic or Shift with Step and Direction
  * Start/Stop number, carets whose number is outside the range insert nothing
* Add: Caret spawning search actions for Forward and Backward whose behavior depends on the text
  at caret and whether there is a selection.
  * if a single caret exists then:
    * if caret is at ' ' or '\t' then will spawn a caret after every span of spaces that ends on
      a non-space. Will select the intervening spaces for each caret
    * if caret is on identifier start then will spawn a caret for every occurrence of identifier
      and select the identifier.
    * if caret is on identifier character, but not start of identifier, then will spawn a caret
      for every occurrence of identifier that ends in same text as one from caret to end of
      identifier and select the matched identifier portion
    * otherwise will spawn a caret on every occurrence of the character at caret, selecting
      trimmed intervening characters between carets.
  * if multiple carets exist then spawning of only a single carets is done by using the pattern
    as determined by the primary caret per above rules with addition of hex (with or without 0x
    prefix), decimal or octal will search for numeric sequence.

    For each caret the pattern search is applied and if found a caret is placed at the location.
    Original caret position is treated as search start positions and match location is called
    the found caret position.

    Start positions are affected by caret movement actions and the pattern search applied at the
    new location. This allows the search start to be modified after the pattern is set.

    Found positions will be the only carets that remain on any non-caret movement actions or on
    typing a character.

    This functionality allows creating a set of carets on all lines and then filtering and
    changing the location of carets used for editing by matching a pattern at the primary caret
    location.

    :information_source: With IDE versions **2017.1 EAP** and newer the plugin allows changing
    the caret appearance for: primary, start and found carets making it easy to see where the
    search starts and where the pattern is matched. Plugin configuration settings under settings
    in Tools > Missing In Actions:
* behavior is also affected by number of carets and selection:
  * if no selections that spans lines then action is limited to a single line of the caret
  * if no two carets are on the same line then affected range for each caret is expanded to full
    lines
  * any selection for a caret is used to expand the range for that caret to include the
    selection and caret offset. For best use, define two shortcuts: one for forward spawning
    action and one for backward one. I was used to having these on my Ctrl+Tab for forward and
    Ctrl+Shift+Tab for backward search. Since these are taken on OS X, I assigned `⌥⌘⇥` for
    forward and `⌥⌘⇧⇥` for backward spawning search instead. A bit awkward but usable.

### 0.8.3 - Bug Fixes and Enhancements

* Fix: select up at left edge when line is blank selects the current line instead of just the
  above line (start/end not extended).
* Fix: #12, Previous Word does not move caret if line contains tabs
* Fix: when doing on paste fix ups of snake case don't convert to camelCase if pasted prefix has
  an underscore at the end or pasted suffix has one at the start.
* Fix: nested action tracking would get off count and some triggered actions and cleanup would
  not be performed.
* Add: option, enabled by default, to delete repeated content created for duplicate for
  clipboard carets, after it is pasted. The content has many carets and is useful for a
  particular combination of lines and carets that are duplicated. Keeping it in history is a
  waste of resources and can be confusing when you thought you were pasting the content you
  selected for duplication, but instead are getting the split/repeated content created for
  pasting into duplicated lines.
* Add: select on paste multi-caret enable predicate separate from non-multi-caret
* Fix: remove/add prefix on paste: if pasting over a prefix with a mismatched prefix, would
  remove the original prefix from pasted word but not add the prefix that existed in the word
  being pasted over.
* Fix: regex prefix option was not being used in actual code on paste adjustment and used camel
  prefix instead.
* Change: only one string is used for regex and regular prefixes, regex since it can represent
  all prefixes and regular string is now | separated list of prefixes.
* Fix: `StraightenCarets` action now disabled if there are selections
* Add: Dupe for Clipboard carets will also handle multi-caret input:
  * duplicated block spans all carets
    * if have selections
      * then only keep carets with selections
    * otherwise
      * if span == 1, keep all carets
      * if have no selections
        * if same number of carets on each type of line:code, comment, blank, of the block then
          keep all carets
        * otherwise, assume that the first and last caret were used to mark the span of lines to
          duplicate, and remove them, duplicating the rest of the carets
    * clipboard data is duplicated for every caret so that the first block will have first caret
      content for every caret in the block, second second, etc If there are 3 carets with text1,
      text2 and text3 on clipboard and 3 carets in the line then after dupe, the clipboard will
      contain 9 carets: text1,text1,text1,text2,text2,text2,text3,text3,text3
* Add: MultiPaste override for all editor fields to make it consistent across the IDE.
* Fix: MultiPaste would override even when disabled in settings.

### 0.8.2 - Bug Fixes and Enhancements

* Fix: when pasting line selections the range marker offset was setup for post-paste operation
  instead of pre-paste, which made it inaccurate after re-indent or re-format was applied after
  pasting.
* Add: Trailing EOL indicator when enabled to short string in multi paste content list
* Create an API for editor specific listeners in application component and forward these to the
  appropriate editor specific listener. For `beforeActionPerformed`/`afterActionPerformed`
  listeners create map entry on event in `beforeActionPerformed` to editor and use this in the
  `afterActionPerformed` to route to the appropriate listener.
  * automatically unregister on editor removal
  * implement for the following:
    * ActionListener
    * IdeEventQueue.EventDispatcher
    * PropertyChangeListener
* Add: second button to multi-paste dialog for paste with carets when regular paste from
  history, and duplicate and paste when invoked from duplicate for clipboard carets.
* Fix: paste range tracking with selection if after paste code changed indentation
* Add: regex testing dialog and button in settings
* Fix: after pasting non-line content, caret position should be left unmolested
* Fix: selection adjust after copy should not change a pre-existing multi-line char selection to
  a line selection.
* Add: MiaMultiplePasteAction now displays extensive information about the clipboard content;
  * Whether an entry is multi-caret or single caret or text
  * For each entry shows the caret lines and whether they are line selections, char selections
    or multi-line char selections
  * Updates when the text in the preview is copied to the clipboard
* Fix: Mia Paste no longer need for making multi-caret select pasted text nor preserve case
  format, using caret listener instead
* Fix: #10, Select Pasted Text wrong range selected if pasted text is adjusted by PasteHandler
* Add: range marker to keep track of pasted text when caret count == 1. PasteHandler formats the
  code after paste and messes up the offsets.

### 0.8.0 - Bug Fixes and New Features

* Add: pictographic of dupe for carets on clipboard to readme
* Add: adding prefixes when pasting over words with prefix to smart paste when pasting over an
  identifier that had a prefix.
* Fix: line selecting up when line from is completely empty just moves up without selecting
* Add: `Duplicate Line or Selection for Carets on Clipboard` action which will duplicate line or
  selection to match number of carets in the clipboard and create a caret on the first line of
  each copied block. No need to count how many duplicates you need for multi-caret text on
  clipboard or to place carets for it. Just copy multi-caret selection, then go to line or
  select lines to be duplicated and action!, `Duplicate Line or Selection for Carets on
  Clipboard` that is.

  If the selection is a character selection within a line, will duplicate the line and carets
  with this selection. Makes it quick to create variants by:

  1. copying multi-caret selection
  2. select part of the line to be replaced
  3. dupe for clipboard carets
  4. paste
* Add: smart paste functionality.
  * preserve camel/pascal case, screaming snake case and snake case on char paste based on
    context of where it is pasted and what is pasted.
  * remove prefix on paste if pasting text next to or inside an identifier, with two
    configurable prefixes by default: `my` and `our` The two combined together allow you to
    select a member with prefix and paste it anywhere. MIA will adjust the pasted text to match
    the format at destination: camel case, snake case, screaming snake case. Inserting and
    deleting underscores as needed, adjusting case after the pasted text if needed. 90% of the
    time there is no need to edit results after pasting, the other 10% hit undo and get the text
    as it was before MIA modified it.
* Add: caret visual attributes handling when available (coming in 2017.1) in preparation for
  caret filtering and spawning actions.
* Add: select to end of file and select to beginning of file as line selection ops (with an
  option in settings)
* Fix: Copy moves to bottom since the mark is always start anchored
* Add: line comment caret remove/keep actions. Rename the others to fit in the remove/keep
  format. Line Comment is a line that either begins with a line comment prefix, or one that
  begins with a block comment prefix and ends in a block comment prefix.
  * `MissingInActions.KeepBlankLineCarets`: keep only carets on blank lines
  * `MissingInActions.KeepCodeLineCarets`: keep only carets on lines which are not blank and not
    line comments
  * `MissingInActions.KeepLineCommentCarets`: keep only carets on line comment lines
  * `MissingInActions.RemoveBlankLineCarets`: remove carets which are not on blank lines
  * `MissingInActions.RemoveCodeLineCarets`: remove carets which are on blank or line comment
    lines
  * `MissingInActions.RemoveLineCommentCarets`: remove carets which are not on line comment
    lines
  * `MissingInActions.SmartKeepLineCarets`: useful for quickly isolating code, or comment lines.
    If have carets on code lines, remove non-code lines; else if have carets on line comment
    lines then remove non-comment lines, else nothing.
  * `MissingInActions.SmartRemoveLineCarets`: opposite of smart keep. Useful for isolating
    carets so unneeded lines can be deleted. If have carets on code lines, remove code lines;
    else if have carets on line comment lines then remove comment lines, else nothing. In all
    cases if the operation would remove all carets then nothing is done.

### 0.7.2 - Refactoring and Code Cleanup

* Fix: copy in multi-caret mode with selections moves the first caret to end of selection.
  Disable caret movement for copy in multi-caret mode.
* Fix: move lines up/down looses caret column
* Fix: selecting lines down gets stuck at blank lines when shrinking selection and anchor is at
  column 1.
* Fix: toggle caret looses the original anchor column
* Fix: move lines up/down with selection having anchor at column 1 moves caret to column 1
  instead of preserving it.
* Add: options on where to move the caret on selection move: for up and for down.
* Fix: check if editor has pop-up and do nothing for up/down/left/right keys. Test that
  left/right is needed.
* Change: switch all code to use `EditorCaret` for manipulating selections
* Change: completely rewritten the abstraction layer. Now much better and easier to maintain.
* Add: Selection extends at start/end, 4 combinations
* Add: Typing deletes line selection option
* Add: options for select pasted and duplicate: always, if 1 or more, 2 or more,...,5 or more
  lines
* Add: indent/un-indent to preserve caret column position
* Fix: disable trying to preserve column when soft-wraps are on. Just freezes the caret at a
  location since vertical movement causes horizontal in the visual position space.
* Fix: disabling delete on typing for line selections would sometimes disable it for non line
  selections.

### 0.7.0 - Enhancements

* Add: Fixes for actions in Auto Line Mode:
  * Toggle Case: remove selection after if there was not one before
  * Copy: if no selection before then leave the caret column unchanged
  * Cut, Duplicate, Duplicate Lines: if no selection before action then remove selection after
    action, if selection was line selection before then restore caret column position after
* Fix: Delayed Auto Indent Lines to do a better job of preserving caret column and added a fix
  for bug in IDE action that does not adjust selection start if it is not at the left margin.
* Add: Select pasted text option to keep the selection so it can be operated on with option to
  only select if pasted text contains at least one line--contains end of line.
* Fix: toggle multi-caret/selection would loose the last line of selection if the caret was at
  the left margin.
* Add: option to remove selection created by toggle case. Will leave selection made before in
  tact.
* Fix: delete to line end not eol was deleting one character less than end of line
* Add: Option to fix duplicate line or selection to duplicate before the selection if caret is
  at the head of the selection (actually if the selection was started from the end).

  Allows fast duplication of a block of code up, instead of always down and having to move it up
  over a copy of itself.
* Fix: paste will now convert to line selection if it can trim/expand the pasted selection to
  full lines if the trimming or expansion affects whitespace chars only.
* Add: hyperlink in settings to enable/disable virtual space from MIA options panel. Also
  remembers if it was turned on from the panel and if line mode is disabled offers a link to
  disable it.
* Add: Identifier Word variations, will only stop on java identifiers, spaces and
  non-identifiers are treated as equal
* Add: Customized Word variations can set if these will stop on:
  * start of line/end of line
  * indent/trailing blanks on a line
  * identifier camel humps or word camel humps rules
  * normal, stay on same line and stay on same line in multi-caret mode
* Add: action to toggle camel humps mode, with option to make mouse setting follow camel humps
  setting.
* Add: action to toggle identifier/word mode for customized word actions, customized next word
  is taken as the value to toggle with the rest following.
* Add: Delete and Backspace white space only, used to pull jagged edge alignment into a straight
  line.
* Add: settings UI for custom next/prev word end/start, start of word and end of word variations

### 0.6.2 - Bug Fix and Features

* Add: option to Auto Indent Lines after move lines up/down, with a configurable delay
* Add: skeleton code for Patterned next/prev actions to be used with multi-carets to search for
  text occurrence and place the caret at the start/end (for backwards) searches. Actions are
  coming in the next release.
* Change: refactor the code for cleaner layout now that the training wheels are coming off.

### 0.6.1 - Bug Fix and Features

* Fix: remove generic action logic it was messing everything and did not work for generic
  actions anyway.
* Fix: return true for dumb aware so actions work during indexing
* Add: `Change To Trimmed Line Selection` action, when converting to line selection, any
  whitespace to end of line from the start of selection and any white space from end of
  selection to start of line are left out of the line mark. Intended to go from this:

  ![Character Selection](https://github.com/vsch/MissingInActions/raw/master/assets/images/CharacterSelection.png)

  To:

  ![Trimmed Line Selection](https://github.com/vsch/MissingInActions/raw/master/assets/images/TrimmedLineSelection.png)

### 0.6.0 - Bug Fix and Features

* Fix: toggle carets/selection would not work during indexing, was missing dumb aware flag.
* Change: implementation to work with standard actions where possible and adjust their operation
  to work with line selections.
* Add: config options to enable, disable and tweak behaviour

### 0.5.1 - Bug Fix

* Fix: missing dependency in `plugin.xml`

### 0.5.0 - Initial Release

* Add: Next/Prev Word variations
* Add: Multiple caret friendly versions of editing actions that will not inadvertently move
  carets to other lines
* Add: Line based selection mode that can be toggled between line/char mode
* Add: Line based mouse selections: more than one line automatically switches to line selection
  without moving caret to column 1
* Add: Switch selection direction to move the caret to the other end of the selection
* Add: caret straightening and toggle between selection and multiple carets to allow quick
  creation of multiple carets with line filtering:
  * on all lines
  * only on non-blank lines
  * only on blank lines

[#23, Plugin changes built-in IDEA behavior]: https://github.com/vsch/MissingInActions/issues/23
[Readme]: https://github.com/vsch/MissingInActions/blob/master/README.md

