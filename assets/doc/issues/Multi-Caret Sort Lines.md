### Multi-Caret Sort Lines

[#7, Add Sort Lines and Multi-Caret Sort Lines](../../../../../issues/7)

[](../../../../../issues) 

Add a sort lines action with changes made to `Sort Lines` plugin:

* [ ] if the file has no selection do nothing
* [ ] if the file has a selection then sort the lines in the selection based on text, with option to ignore indent blanks.
* [ ] When sorting preserve the last line's `,`, `;` or none of these endings based on what is the
  ending of the last line before the sort. That way lists of collections and enum values can be
  sorted without needing to edit.
* [ ] Handle multi-carets by sorting lines based on caret position and whether it has a selection:
    * [ ] lines with carets that have selections are sorted on the selection contents
    * [ ] lines with carets but no selection sorted on text after caret position, with option to not
      sort them but just group together.
    * [ ] lines without carets go to end of the list (sorted or unsorted as an option)
    * [ ] if lines with and without carets are sorted then any blank lines in that set are removed.
* [ ] have an options popup version of the action that will remember its last settings for use
  without popup or just use this once.

