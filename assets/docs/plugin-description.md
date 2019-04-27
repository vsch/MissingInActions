<img src="https://github.com/vsch/MissingInActions/raw/master/resources/icons/png/Mia_logo@2x.png" height="24" width="42" border="0" style="margin-right:10px;">Missing In Actions
===============================================================================================================================================================================

Adds missing editor actions for end of word navigation but that is just the beginning:

* Automatic Selection stack, stores last 5 selections by default. Recall last selection or any
  previous selection from a list.
* Enable Auto Indent Lines after move line/selection up or down actions to have lines indented
  automatically.
* Use Smart Paste to eliminate case change and prefix edits when pasting identifiers. MIA will
  match case and style of identifier at destination when you paste. Undo to get results before
  MIA adjusted them.

  Copy `myColumnData` and paste it over `DEFAULT_VALUE` to get `COLUMN_DATA`, reverse the order
  and get `myDefaultValue`.

  Works when pasting at the **beginning**, **end** and **middle** of identifiers.

  Supports: **camelCase**, **PascalCase**, **snake_case**, **SCREAMING_SNAKE_CASE**,
  **dash-case**, **dot.case**, **slash/case**

  Default prefixes: `my`, `our`, `is`, `get`, `set` to allow pasting over member fields, static
  fields, getters and setters.
* Enable Auto Line Selections and select full lines without loosing time or column position by
  moving the caret to the start of line when selecting or pasting. **Choose** whether you want
  to **paste full line** selections: **above** or **below** the current line regardless of the
  caret's column.
* Toggle between selection and multiple carets on selected lines to save time re-selecting the
  same text again.
* Filter multiple carets saves you time when creating multiple carets by removing carets on
  blank or comment lines so you can edit only code lines.
* Enhanced Paste from History dialog:
  * **combine**, **arrange** and **reverse** the order of content entries
  * **combine multiple** clipboard contents **with caret information intact**
  * **paste and re-create multiple carets** from information already stored on the clipboard
  * **duplicate line/block for each caret** in the clipboard content and **put a caret on the
    first line** of the block, ready for multi-caret select and paste
  * **splice** individual entries into delimited list, optionally quoting each entry
  * see caret information stored on the clipboard for each content entry
* Batch Search/Replace to search/replace multiple strings at the same time
* Many more options and adjustments to make multiple caret text editing fast, efficient and
  easy.

**Plugin website:
[<span style="color:#30A0D8">Missing In Actions GitHub Repo</span>](https://github.com/vsch/MissingInActions)**

**Bug tracking & feature requests:
[<span style="color:#30A0D8">Missing In Actions GitHub Issues</span>](https://github.com/vsch/MissingInActions)**

