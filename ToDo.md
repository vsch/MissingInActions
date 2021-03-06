### To Do Wish List

&nbsp;<details id="todo"><summary>**Refactoring To Do List**</summary>

- [ ] Add to `EditorPosition` class all the text testing and moving at caret position. Pass
      these through from `EditorCaret`
      - tests where flags are `EditHelpers` word boundary flags:
          - [ ] `isAtBoundary(int flags)`
          - [ ] isStart/isMiddle/isEnd OfWhitespaces : to test if at start/middle/end of
                whitespace run
          - [ ] isStart/isMiddle/isEnd OfWord : to test if at start/middle/end of whitespace run
      - moves:
          - [ ] atNext/atPrevious Word/Whitespace/NonWhitespace Start/End
          - [x] atStart/atEnd Column
          - [x] atIndent/atTrimmedEnd Column

&nbsp;</details>

&nbsp;<details id="todo"><summary>**To Do List**</summary>

- [ ] Add: per language override on IDE's trim trailing blanks with default if no specific
      override so that Modified Lines could be set for default and IDE set to ALL. 
      - [ ] Options for default are: 
          - IDE Setting
          - None
          - Modified Lines
          - All
      - [ ] Options per language are: 
          - IDE Setting
          - MIA Default
          - None
          - Modified Lines
          - All
- [ ] Add: scope based config for all language type settings 
- [ ] Add: option to disable trimming spaces from a line containing the caret with virtual
      spaces, or restore caret column after save to prevent caret jump.
- [ ] Add: option to fix-up line endings after move line up/down based on context to add/remove
      ,; by looking at what the line had before in that position. Not language based but
      heuristic.
- [ ] Add: option to delete to end of line to leave ; , unless the caret is right on it.
- [ ] Add: option to use visual column for caret restoration. Less jarring with folded regions
      and works with soft wraps.
- [ ] Add: sort lines options pop-up panel for selecting sorting by StringManipulation in
      multi-caret mode.

&nbsp;</details>

&nbsp;<details id="todo"><summary>**BeyondEdit Emulation To Do List**</summary>

- [ ] Add: full set of backspace/delete a la BeyondEdit: spaces, spaces/non-spaces,
      words/non-words/spaces. Escalating Forwards/Backwards Deletes:
      - [x] Spaces only
      - [ ] One of following contiguous stretches of (determined by what is the first character
            in the direction of operation from caret): whitespaces, identifiers, others
      - [ ] One of following contiguous stretches of (determined by what is the first character
            in the direction of operation from caret): whitespaces, non-whitespaces
      - [ ] To next/previous non-space by deleting contiguous whitespaces, contiguous
            non-whitespaces, contiguous whitespaces. With variation depending on the context of
            characters around caret so that after the delete whitespace/non-whitespace boundary
            at caret is preserved:
          - for delete:
              - if caret is on end of word (space delimited) then delete: spaces, non-spaces
              - if caret is on start of word (space delimited) then delete: non-spaces, spaces
              - if caret is in middle of word (non-space before and after) then delete:
                non-spaces
              - if caret is in middle of whitespaces (space before and after) then delete:
                spaces
          - for backspace:
              - if caret is on end of word (space delimited) then backspace: non-spaces, spaces
              - if caret is on start of word (space delimited) then backspace: spaces,
                non-spaces
              - if caret is in middle of word (non-space before and after) then backspace:
                non-spaces
              - if caret is in middle of whitespaces (space before and after) then backspace:
                spaces
      - [ ] To next/previous element as determined by caret context to be used for removing
            elements in lists the goal is to allow comfortable removal of characters to next
            point of context: list items, bracketed expressions, quoted expressions. This item
            needs thinking and experimentation:
          - separated as in comma separated, semi-colon separated, etc. i.e. character(s) used
            for separating items in a list
          - delimited as in round brackets, square brackets, angle brackets, etc. i.e.
            Surrounded in opening and closing character(s)
          - quoted as in singe quotes, double quotes, back-quotes, etc. i.e. Wrapped in same
            character(s)
          - whitespace delimited non-whitespace
- [ ] Add: smart column aligned text editing based on lines around the caret line. Has the
      effect of having column based tab stops for purposes of keeping column formatted text
      aligned when insert/delete is performed:
      - a column aligned line range is surrounded by blank lines
          - can contain blank line if they are surrounded by a non-blank line in the range
          - can contain a range of 2 blank lines:
              - if the caret is on the first line and preceding line is in range and line after
                the second blank line would be in the range if the caret line was in the range.
              - if the caret is on the second line and succeeding line is in range and line
                before the first blank line would be in the range if the caret line was in the
                range.
          - can contain a range of 3 blank lines:
              - if the caret is on the second line and line preceding the first blank line and
                line succeeding the third blank line would be in in range if the caret line was
                in the range
              - if the caret is on the second line and succeeding line is in range and line
                before the first blank line would be in the range if the caret line was in the
                range.
          - contains 3 or more non-blank lines that have at least one dynamic tab stop
          - find a column position before which all lines either have
              - 3 or more spaces before and non-space at column
              - or have 1 space before 1 space after column
              - or have 2 spaces at or after column but before the next dynamic tab stop for the
                line range
      - [ ] Add: smart insert mode to preserve column alignment a la BeyondEdit mode. This one
            is a career decision because it affects typing, delete/backspace, paste (chars),
            delete char selections.
          - [ ] for single char insert, search forward on the line and first stretch of 3 or
                more spaces, delete one of them
          - [ ] for single char delete, search forward on the line for spaces to stretch by
                inserting spaces in the range:
              - 3 or more spaces
              - 2 spaces where the range of spaces ends on a dynamic column
- [ ] Add: Drag/Copy/Move line selection. Dragging a line selection will copy/move it always
      between lines never in the middle.
      - [ ] Visual feedback is a horizontal line at the point of insertion
      - [ ] Default mode is copy with move via a modifier key. Specifically the reverse of word
            processor defaults because in code a copy is used ten times more often
- [ ] Add: Drag/Drop-Replace mode for character selections
      - [ ] The drop target is highlighted to show it will be replaced
      - [ ] Smarts include determining what is being dragged by its surrounding context and
            adjusting the drop zone selection to match: ie. if source is a portion of camel
            humps then drop selects camel hump portion
      - [ ] If source is quoted then drop selects between quotes, if quotes included in source
            then quotes are selected in drop
      - [ ] If source is bracketed then brackets are used as delimiters for drop target
      - [ ] Modifier allows changing camel hump, quoted, bracketed modes
      - [ ] Additional modifiers to be used to manually select the drop range to be replaced
      - [ ] When highlights are implemented then the dropped on portion is to be added to the
            highlights. Optionally starting a Search/Replace operation with parameters that
            match the drop replacement so that further replacements could be done via
            replace/exclude/replace all.

&nbsp;</details>

