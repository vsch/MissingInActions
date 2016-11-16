## Markdown Navigator

[TOC levels=3,6]: # "Version History"

### Version History
- [0.6.1 - Bug Fix and Features](#061---bug-fix-and-features)
- [0.6.0 - Bug Fix and Features](#060---bug-fix-and-features)
- [0.5.1 - Bug Fix](#051---bug-fix)
- [0.5.0 - Initial Release](#050---initial-release)


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

