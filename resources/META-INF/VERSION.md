## Markdown Navigator

[TOC levels=3,6]: # "Version History"

### Version History
- [0.7.0 - Enhancements](#070---enhancements)
- [0.6.2 - Bug Fix and Features](#062---bug-fix-and-features)
- [0.6.1 - Bug Fix and Features](#061---bug-fix-and-features)
- [0.6.0 - Bug Fix and Features](#060---bug-fix-and-features)
- [0.5.1 - Bug Fix](#051---bug-fix)
- [0.5.0 - Initial Release](#050---initial-release)


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

