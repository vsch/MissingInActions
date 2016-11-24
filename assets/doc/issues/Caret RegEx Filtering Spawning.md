This would include actions that search and filter if not found based on primary caret context:

1. Search forward/backward from caret position
2. Filtering actions: Stop on match, remove caret if not found in range between current caret
   and next caret in multi-caret set
3. Spawn actions: to spawn new carets on match in same line or other lines

Context would determine the initial search criteria and direction:

1. forward search:
    1. on start of word: match whole word
    2. in middle of word: match string to end of word (end of match must be a word boundary)
    3. on space: match on next non-space (has the effect of skipping spaces)
    4. on any other character: match that character

2. backward search:
    1. on end of word: match whole word
    2. in middle of word: match string to start of word (start of match must be a word boundary)
    3. on space: match on previous non-space (has the effect of skipping spaces)
    4. on any other character: match that character

3. Spawning action would spawn carets in the current search range (line or to next caret) based
   on a match and optionally place the found location and select the range of text at the found
   location of the regex matching groups to facilitate replacement without the need to select
   manually first. Additionally, these search filters can have a preserve case setting so that
   the case of the typed characters would be changed to match the originally selected string
   (based on distance from camel humps word break locations).

    For example, search regex `\b[\w]*(End|Start)[\w]*\b`, without case match, with replace
    string `$1` would give the the following (**|** marks the caret search start and **/** marks
    the found result with **{}** giving the selection):

    <pre>
        <b>|</b>public boolean canTrimOrExpandToFullLineSelection() {
        <b>|</b>    if (hasSelection()) {
        <b>|</b>        if (isLine()) {
        <b>|</b>            return true;
        <b>|</b>        } else {
        <b>|</b>            if (mySelection<b style="color:red">/{</b>Start<b style="color:red">}</b>.line != mySelection<b style="color:red">/{</b>End<b style="color:red">}</b>.line) {
        <b>|</b>                if (mySelection<b style="color:red">/{</b>Start<b style="color:red">}</b>.column != 0 || mySelection<b style="color:red">/{</b>End<b style="color:red">}</b>.column != 0) {
        <b>|</b>                    EditorPosition selection<b style="color:red">/{</b>Start<b style="color:red">}</b> = mySelection<b style="color:red">/{</b>Start<b style="color:red">}</b>.toTrimmedOrExpandedFullLine();
        <b>|</b>                    EditorPosition selection<b style="color:red">/{</b>End<b style="color:red">}</b> = mySelection<b style="color:red">/{</b>End<b style="color:red">}</b>.toTrimmedOrExpandedFullLine();
        <b>|</b>                    return selection<b style="color:red">/{</b>Start<b style="color:red">}</b>.column == 0 && selection<b style="color:red">/{</b>End<b style="color:red">}</b>.column == 0;
        <b>|</b>                } else {
        <b>|</b>                    return true;
        <b>|</b>                }
        <b>|</b>            }
        <b>|</b>        }
        <b>|</b>    }
        <b>|</b>    return false;
        <b>|</b>}

  </pre>

Other important features:

1. The location of search start is the current position of every caret marked by a special color
   or thickness of the starting carets.

2. The found location is is where a match was found. All matched locations are marked with
   another caret of a special color or thickness.

3. The found location carets can become the carets (search start location carets removed) by
   hitting an action (Snapshot Found Carets) or by executing any action other than move caret
   left/right/up/down keys which affect the start search carets. This way the search location
   start can be adjusted once you see the results of that search.

4. Results of the search: found/not found should be used to highlight the line of the location
   start caret to show a match or not-found condition at a glance.

5. The primary caret search start location and found location should be thicker or more brightly
   colored than the rest so it could be quickly found.

6. The primary caret should scroll into view either:

    1. when carets are moved up/down/left/right and the option in settings is enabled

    2. when an action to scroll primary caret into view is invoked

7. In addition it would be great if a non-modal panel appeared while the search mode was active:
    1. which faded to translucent when not used, but reverted to opaque when a mnemonic for one
       of its controls was used or mouse is hovered over it.
    2. Allowed editing of the reg ex expression
    3. Allowed switching to non-regex string search with options for: start of word, end of
       word, etc that would be translated to regex automatically.

8. The action invoking the search can be invoked while the previous one is active, with the
   effect of taking a snapshot of the current found carets and using them as the start of search
   location.

