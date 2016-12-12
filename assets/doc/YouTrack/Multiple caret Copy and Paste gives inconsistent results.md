## Multiple caret Copy and Paste gives inconsistent results

A common use case for me is to take existing code using multiple carets and apply it in multiple places knowing that when there are more carets than content they will get no content on paste and if there are less then the extra content is ignored.

I noticed that sometimes I would get funny results like copy to clipboard of multiple caret not working and pasting sometimes did not give what I expected but wrote it off to keyboard or finger misfire. Then I discovered to my surprise that it is by "design".

When copying and pasting content for multiple carets, caret count and break down of text for each caret are important but sometimes are treated as a suggestion from the user instead of a command.

1. `CopyPasteManagerEx` ignores caret count and text break down per caret all together when comparing new content to existing content. This makes it treat copied data as equivalent based solely on its string representation, just moving the old to the top of the clipboard stack. This is only correct if pasting in single caret mode multi-caret content or pasting multi-caret content in single caret mode. In multiple caret mode the break down of the content by caret is important.

2. Content created with different caret count than than currently available but where caret count happens to be a multiple of current caret count, then the text lines of multiple caret text on the clipboard will be split or combined to distribute it to available carets. These are inconsistencies and should at least be an option that can be disabled for those of us that use a lot of multi-caret editing and want easily predictable behaviour without resorting to counting carets used for copy and paste actions.

I propose the following changes:

1. Add: a registry key for `editor.strict.multi.caret.state.clipboard.rules` with default of `false` to keep backward compatibility with current behavior, declared in `CopyPasteManager`
2. Modify: `CopyPasteManagerEx`:
    1. when the value is `false` and text representation of old and new match, then remove the old value and add new content. This will eliminate all surprises when copying and pasting multiple caret data. Latest copied multi-caret representation will always be the one pasted.
    2. when the value is `true` always add new content when caret state data differs from what is already in history
3. Modify: `ClipboardTextPerCaretSplitter` when this value is `true` to not do any splitting of lines but return original splits when data exists and empty string when caret count exceeds count in transferable.

Even without strict option set, `CopyPasteManagerEx` should put the new content at the top of the stack and remove the old content if they textually match, instead of only moving old content to the top. This will ensure that the clipboard reflects what the last copy really was and eliminate surprises on paste.

I think these changes can be applied to 2016 versions since the affected code has not seen daylight since 2014. However the patch was made and tested against intellij-community master branch.

Patch attached.

