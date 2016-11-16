# Missing in Actions

**You can download it on the [JetBrains plugin page].**

[TOC]: #

### Table of Contents
- [Version 0.6.2 - Improved Implementation](#version-062---improved-implementation)
- [Why the need](#why-the-need)
    - [Multi Caret Friendly Actions](#multi-caret-friendly-actions)
    - [Auto Line Selections](#auto-line-selections)

A collection of useful but missing text editing actions for JetBrains IDEs, including navigation
and selection of start and end of words!

* Next/Previous word Start/End variations based on what marks a word boundary:
    * IDEA version: identifier characters, spaces, lexeme boundaries
    * Words Only: identifier characters, spaces, all the rest

* Action to delete to line indent, to clear to line indent (replace all chars from caret to line
  indent with spaces.

* Auto Line Selection mode and supporting actions to automatically switch selection to full line
  mode if selection spans more than one line.
    * Actions to switch from Auto Line selection to character based selection
    * Auto line selection mode for mouse selections
    * Auto line selection multi-caret friendly actions that will not move carets to another line:
        * Next/Prev word stops at beginning and end of line for the caret
        * Delete to End of Line will not delete that line's EOL when , causing it to join two lines

:warning: Auto Line Selections work best if virtual spaces are enabled allowing the caret
position to not be affected by physical end of line.

## Version 0.6.2 - Improved Implementation

[Version Notes]

![Screen Shot sequence](assets/images/noload/MissingInActions.gif)

![Edit Actions](assets/images/EditActions.png) 

![Edit Actions](/assets/images/ToolsSettings.png) 

## Why the need

IntelliJ development tools are the best of breed when it comes to language support, refactoring
and the rest of intelligent language features but I find they suffer in their text editing
capabilities, especially when it comes multiple caret editing.

I know, many of you would say why use a mouse when the keyboard is so much faster. Not really. I
am a touch typist, 40-45 words a minute, and sometimes find double click or select and drag/drop
would be faster. Especially when the source and destination are not immediately next to each
other.

I wrote and maintained my own editor for over two decades, on the Amiga, then PC DOS then
Windows (3.1 to Vista), only because I could not find the functionality I needed elsewhere. When
I started development on a Mac I no longer wanted to maintain that old war horse which was
getting long in the tooth. I was sure that I was not going to rewrite it for the Mac and decided
that I will give it up and get used to IntelliJ way of editing.

I made the switch but found for some bulk edits I still prefer to fire up Parallels Desktop with
Windows 10 and do the edit in my old workhorse. It was getting harder to use every time because
some things no longer worked under Windows 10 and then horror! The old workhorse stopped working
after a Windows 10 upgrade. I decided it was time to bite the bullet and make a plugin that will
add to IntelliJ what it doesn't know it's missing.

### Multi Caret Friendly Actions

Refactoring and code smarts are great but often a good multi-caret editing will be so much faster,
and sometimes it will be the only way to get it done without having to do a lot of typing and
editing.

All editing actions can be used in multi caret mode but to be really useful actions should not
be too aggressive in their operation. What makes sense when you are editing with a single caret
can quickly turn into a disaster when editing on multiple lines and with different text content.

Standard editing actions make multi-caret mode barely useful and only for a few keystrokes
before your multiple carets are turned into a jumbled mess.

This plugin adds multi caret aware actions:

* Next/Prev word start/end actions will not cross line boundaries. They will stop at column one
  or the end of line. This ensures that you always know where your carets are, on the line where
  you put them.

    These come with variation on what they consider to be a word boundary:

    * Java identifier characters, whitespace and all the rest
    * Java identifier characters and the rest. Use these to jump to identifiers

* Delete to End of Line that does not delete the EOL character, which the standard IDEA action
  does, causing the lines where the caret is located at the end of line, or after it, to be
  spliced to the next line.

    For multi caret operation this is not desired because it causes some lines to be spliced
    while others get truncated as the action implies. The action provided by this plugin will
    only delete the text, making it useful for truncating all lines without splicing them.

    If you want to splice them use Join Lines action provided by the IDE.

* Delete to Indent to delete characters between caret and the line's indent

* Clear to Indent to replace characters between caret and line's indent with spaces. Effectively
  removes all text before caret and indents the line to the caret position.

* Toggle between a selection and multiple carets, variations when switching to multiple carets:
    * carets on all lines
    * carets on non-blank lines only
    * carets on blank lines only

* Caret filter actions:
    * Remove carets on blank lines
    * Remove carets on non-blank lines

* Straighten carets action to move all the carets to the same column position.

* Switch selection direction allows you to switch the anchor of the current selection. Useful
  when you selected the desired text only to notice you want to change the other end of the
  selection. Switching direction will move the caret to the other end so you can proceed with
  the change.


### Auto Line Selections

When selecting text in source code, most of the time if it spans more than one line then you
want full lines. Instead, all editors work like word processors and select characters forcing
you to move the caret to column 1 to select full lines.

Auto line selection actions selects full lines when using vertical movement selection keys:
up, down, page up, page down. While horizontal keys will restore the selection to character
mode, even if it spans more than one line.

The switching is done by the actions and once your expectations are adjusted you will not want
to work without it.

This works best if you have virtual spaces enabled. It will leave the caret column position
unmolested throughout all line based operations regardless of the actual text length of the line.

* Switch/Toggle between auto line and normal character selections

#### Mouse Selections

With auto line selections enabled for mouse selections you get full lines when a selection spans
more than one line and character selections for selections within a line.

Use the Ctrl key while selecting to disable auto line selections. Keep the Ctrl key pressed until
after you release the mouse button, otherwise the selection will be changed to a line selection
when the mouse button is released.

[Version Notes]: /resources/META-INF/VERSION.md

[JetBrains plugin page]: https://plugins.jetbrains.com/plugin?pr=&pluginId=9257
