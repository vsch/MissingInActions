## Missing In Actions

[TOC levels=3,6]: # "Version History"

### Version History
- [0.8.1 - Bug Fixes and New Features](#081---bug-fixes-and-new-features)
- [0.8.0 - Bug Fixes and New Features](#080---bug-fixes-and-new-features)
- [0.7.2 - Refactoring and Code Cleanup](#072---refactoring-and-code-cleanup)
- [0.7.0 - Enhancements](#070---enhancements)
- [0.6.2 - Bug Fix and Features](#062---bug-fix-and-features)
- [0.6.1 - Bug Fix and Features](#061---bug-fix-and-features)
- [0.6.0 - Bug Fix and Features](#060---bug-fix-and-features)
- [0.5.1 - Bug Fix](#051---bug-fix)
- [0.5.0 - Initial Release](#050---initial-release)


&nbsp;<details id="todo"><summary>**To Do List**</summary>

##### Next Release To Do

- [ ] Add: change the SmartKeepLineCarets action to first keep code lines, if all carets are
      already on code lines then remove those whose code lines contain nothing but brackets,
      parentheses, braces, commas and semicolons. This will allow to quickly isolate lines that
      require editing without changing the selection to remove carets from lines that contain
      only context and termination elements.
- [ ] Add: low-lighting to isolate parts of source a la BeyondEdit
      - [ ] with options to copy/cut low-lighted or not low-lighted blocks
- [ ] Add: global highlight text which will work across files of matching text with actions to:
      - [ ] use spectrum generated for each different word in a rainbow progression so that the
            order in which the highlights were made can be visually verified.
      - [ ] clear all highlights
      - [ ] clear highlight for word at caret or selection
      - [ ] highlight word at caret or selection
      - [ ] next/prev highlight in file
      - [ ] Preferably should only highlight part of the document that is visible. Hidden
            portions highlighted as they become exposed during scrolling.
- [ ] Add: per language configuration of what constitutes an identifier character set to
      override Java default for custom word actions for languages other than Java (dynamically
      based on file language type)
- [ ] Add: icons to caret/selection actions for toolbar buttons
- [ ] Add: toolbar with buttons for caret/selection actions a la debug configs. Specifically:
      - [ ] keep comments
      - [ ] keep blank lines
      - [ ] straighten caret
- [ ] Fix: backspace to line indent should backspace to beginning of line if at or before
      indent.
- [ ] Add: insert numeric sequence in multi-caret mode, with selections should try to determine
      parameters: start number, increment, sequence type and format; without should re-use last
      params with optional action to always show pop-up panel for options.
      - [ ] Sequences 0-9, A-Z for number bases 2-36
      - [ ] Prefix/Suffix options to add to generated number
      - [ ] Sequences can be 0 left filled to any width
      - [ ] Arithmetic or Shift with Step and Direction
      - [ ] Start/Stop number, carets whose number is outside the range insert nothing
- [ ] Add: column aligning `ColumnAligningTabAction`, non-multi-caret mode does tab action, in
      multi-caret mode that has the effect after action:
      - all carets will be at the same column
      - that column will be >= the column before action
      - that column will be a multiple of file's indent space count
      - each caret will be on the first non-whitespace character at or after caret before action
      - if the caret is after the trimmed end of line then treat the end of line as a
        non-whitespace character
      - [ ] for each caret find the range of whitespace from caret to first non-whitespace at or
            after the caret. If caret after last non-whitespace of the line, treat end of line
            as the first non-whitespace character
      - [ ] alignment column is the minimum of all range starts, aligned on tab indent count for
            file.
      - [ ] move each caret to minimum of alignment column and end of whitespace range
      - [ ] if caret after move:
          - [ ] is before the whitespace range end, delete characters to whitespace range
          - [ ] is before alignment column, insert spaces before caret so it is moved to
                alignment column
      - [ ] if no caret columns changed as the result of above steps, add indent spaces to
            alignment column and repeat previous two steps.

&nbsp;</details>

### 0.8.1 - Bug Fixes and New Features

- Add: MiaMultiplePasteAction now displays extensive information about the clipboard content;
    - Whether an entry is multi-caret or single caret or text
    - For each entry shows the caret lines and whether they are line selections, char selections
      or multi-line char selections
    - Updates when the text in the preview is copied to the clipboard
- Fix: Mia Paste no longer need for making multi-caret select pasted text nor preserve case
  format, using caret listener instead
- Fix: #10, Select Pasted Text wrong range selected if pasted text is adjusted by PasteHandler
- Add: range marker to keep track of pasted text when caret count == 1. PasteHandler formats
      the code after paste and messes up the offsets.

### 0.8.0 - Bug Fixes and New Features

- Add: pictographic of dupe for carets on clipboard
- Add: adding prefixes when pasting over words with prefix to smart paste when pasting over an
  identifier that had a prefix.
- Fix: line selecting up when line from is completely empty just moves up without selecting
- Add: `Duplicate Line or Selection for Carets on Clipboard` action which will duplicate line or
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
- Add: smart paste functionality.
    - preserve camel/pascal case, screaming snake case and snake case on char paste based on
      context of where it is pasted and what is pasted.
    - remove prefix on paste if pasting text next to or inside an identifier, with two
      configurable prefixes by default: `my` and `our`

    The two combined together allow you to select a member with prefix and paste it anywhere.
    MIA will adjust the pasted text to match the format at destination: camel case, snake case,
    screaming snake case. Inserting and deleting underscores as needed, adjusting case after the
    pasted text if needed. 90% of the time there is no need to edit results after pasting, the
    other 10% hit undo and get the text as it was before MIA modified it.
- Add: caret visual attributes handling when available (coming in 2017.1) in preparation for
  caret filtering and spawning actions.
- Add: select to end of file and select to beginning of file as line selection ops (with an
  option in settings)
- Fix: Copy moves to bottom since the mark is always start anchored
- Add: line comment caret remove/keep actions. Rename the others to fit in the remove/keep
  format. Line Comment is a line that either begins with a line comment prefix, or one that
  begins with a block comment prefix and ends in a block comment prefix.
    - `MissingInActions.KeepBlankLineCarets`: keep only carets on blank lines
    - `MissingInActions.KeepCodeLineCarets`: keep only carets on lines which are not blank and
      not line comments
    - `MissingInActions.KeepLineCommentCarets`: keep only carets on line comment lines
    - `MissingInActions.RemoveBlankLineCarets`: remove carets which are not on blank lines
    - `MissingInActions.RemoveCodeLineCarets`: remove carets which are on blank or line comment
      lines
    - `MissingInActions.RemoveLineCommentCarets`: remove carets which are not on line comment
      lines
    - `MissingInActions.SmartKeepLineCarets`: useful for quickly isolating code, or comment
      lines. If have carets on code lines, remove non-code lines; else if have carets on line
      comment lines then remove non-comment lines, else nothing.
    - `MissingInActions.SmartRemoveLineCarets`: opposite of smart keep. Useful for isolating
      carets so unneeded lines can be deleted. If have carets on code lines, remove code lines;
      else if have carets on line comment lines then remove comment lines, else nothing.

    In all cases if the operation would remove all carets then nothing is done.

### 0.7.2 - Refactoring and Code Cleanup

- Fix: copy in multi-caret mode with selections moves the first caret to end of selection.
  Disable caret movement for copy in multi-caret mode.
- Fix: move lines up/down looses caret column
- Fix: selecting lines down gets stuck at blank lines when shrinking selection and anchor is at
  column 1.
- Fix: toggle caret looses the original anchor column
- Fix: move lines up/down with selection having anchor at column 1 moves caret to column 1
  instead of preserving it.
- Add: options on where to move the caret on selection move: for up and for down.
- Fix: check if editor has pop-up and do nothing for up/down/left/right keys. Test that
  left/right is needed.
- Change: switch all code to use `EditorCaret` for manipulating selections
- Change: completely rewritten the abstraction layer. Now much better and easier to maintain.
- Add: Selection extends at start/end, 4 combinations
- Add: Typing deletes line selection option
- Add: options for select pasted and duplicate: always, if 1 or more, 2 or more,...,5 or more
  lines
- Add: indent/un-indent to preserve caret column position
- Fix: disable trying to preserve column when soft-wraps are on. Just freezes the caret at a
  location since vertical movement causes horizontal in the visual position space.
- Fix: disabling delete on typing for line selections would sometimes disable it for non line
  selections.

### 0.7.0 - Enhancements

- Add: Fixes for actions in Auto Line Mode:
    - Toggle Case: remove selection after if there was not one before
    - Copy: if no selection before then leave the caret column unchanged
    - Cut, Duplicate, Duplicate Lines: if no selection before action then remove selection after
      action, if selection was line selection before then restore caret column position after
- Fix: Delayed Auto Indent Lines to do a better job of preserving caret column and added a fix
  for bug in IDE action that does not adjust selection start if it is not at the left margin.
- Add: Select pasted text option to keep the selection so it can be operated on with option to
  only select if pasted text contains at least one line--contains end of line.
- Fix: toggle multi-caret/selection would loose the last line of selection if the caret was at
  the left margin.
- Add: option to remove selection created by toggle case. Will leave selection made before in
  tact.
- Fix: delete to line end not eol was deleting one character less than end of line
- Add: Option to fix duplicate line or selection to duplicate before the selection if caret is
  at the head of the selection (actually if the selection was started from the end).

    Allows fast duplication of a block of code up, instead of always down and having to move it
    up over a copy of itself.
- Fix: paste will now convert to line selection if it can trim/expand the pasted selection to
  full lines if the trimming or expansion affects whitespace chars only.
- Add: hyperlink in settings to enable/disable virtual space from MIA options panel. Also
  remembers if it was turned on from the panel and if line mode is disabled offers a link to
  disable it.
- Add: Identifier Word variations, will only stop on java identifiers, spaces and
  non-identifiers are treated as equal
- Add: Customized Word variations can set if these will stop on:
    - start of line/end of line
    - indent/trailing blanks on a line
    - identifier camel humps or word camel humps rules
    - normal, stay on same line and stay on same line in multi-caret mode
- Add: action to toggle camel humps mode, with option to make mouse setting follow camel humps
  setting.
- Add: action to toggle identifier/word mode for customized word actions, customized next word
  is taken as the value to toggle with the rest following.
- Add: Delete and Backspace white space only, used to pull jagged edge alignment into a straight
  line.
- Add: settings UI for custom next/prev word end/start, start of word and end of word variations

### 0.6.2 - Bug Fix and Features

- Add: option to Auto Indent Lines after move lines up/down, with a configurable delay
- Add: skeleton code for Patterned next/prev actions to be used with multi-carets to search for
  text occurrence and place the caret at the start/end (for backwards) searches. Actions are
  coming in the next release.
- Change: refactor the code for cleaner layout now that the training wheels are coming off.

### 0.6.1 - Bug Fix and Features

- Fix: remove generic action logic it was messing everything and did not work for generic
  actions anyway.
- Fix: return true for dumb aware so actions work during indexing
- Add: `Change To Trimmed Line Selection` action, when converting to line selection, any
  whitespace to end of line from the start of selection and any white space from end of
  selection to start of line are left out of the line mark. Intended to go from this:

    ![Character Selection](https://github.com/vsch/MissingInActions/raw/master/assets/images/CharacterSelection.png)

    To:

    ![Trimmed Line Selection](https://github.com/vsch/MissingInActions/raw/master/assets/images/TrimmedLineSelection.png)

### 0.6.0 - Bug Fix and Features

- Fix: toggle carets/selection would not work during indexing, was missing dumb aware flag.
- Change: implementation to work with standard actions where possible and adjust their operation
  to work with line selections.
- Add: config options to enable, disable and tweak behaviour

### 0.5.1 - Bug Fix

- Fix: missing dependency in `plugin.xml`

### 0.5.0 - Initial Release

- Add: Next/Prev Word variations
- Add: Multiple caret friendly versions of editing actions that will not inadvertently move
  carets to other lines
- Add: Line based selection mode that can be toggled between line/char mode
- Add: Line based mouse selections: more than one line automatically switches to line selection
  without moving caret to column 1
- Add: Switch selection direction to move the caret to the other end of the selection
- Add: caret straightening and toggle between selection and multiple carets to allow quick
  creation of multiple carets with line filtering:
    - on all lines
    - only on non-blank lines
    - only on blank lines

[Readme]: https://github.com/vsch/MissingInActions/blob/master/README.md

