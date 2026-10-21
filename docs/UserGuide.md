---
  layout: default.md
  title: "User Guide"
  pageNav: 3
---

# NUSocietyDesk User Guide

NUSocietyDesk is a desktop member directory for NUS society secretaries. It uses typed commands and a graphical member list to manage contacts, roles, committees, and membership status.

<!-- * Table of Contents -->
<page-nav-print />

--------------------------------------------------------------------------------------------------------------------

## Quick start

1. Ensure that Java `25` or later is installed on your computer.<br>
   **Mac users:** Ensure you have the precise JDK version prescribed [here](https://se-education.org/guides/tutorials/javaInstallationMac.html).

1. Get `NUSocietyDesk.jar` from the [project releases](https://github.com/AY2627S1-CS2103T-T17-3/tp/releases), if available, or build it from source with `./gradlew shadowJar` (output: `build/libs/NUSocietyDesk.jar`).

1. Copy the file to the folder you want to use as the home folder for your member directory.

1. Open a terminal, `cd` to the folder containing the JAR file, and run `java -jar NUSocietyDesk.jar`.<br>
   A GUI should appear in a few seconds with sample data. The image below is an early UI concept; the current
   app has an **ALL** status button and uses **ARCHIVED** instead of **FORMER** on member cards.<br>
   ![Ui](images/Ui.png)

1. Type a command in the command box and press Enter to execute it. For example, type **`help`** and press Enter to open the help window.<br>
   The status button starts at **ALL**. Click it to cycle through **ACTIVE**, **ARCHIVED**, and back to **ALL**;
   the member list changes to match the label.
   Some example commands you can try:

   * `list` : Clears the search and lists members in the selected status view.

   * `add e/johnd@u.nus.edu` : Adds a member whose other contact details can be filled in later.

   * `find e/johnd` : Finds members using part of an email address.

   * `edit 1 p/91234567` : Updates the phone number of the 1st displayed member.

   * `tag 1 t/Publicity` : Adds a society tag to the 1st displayed member.

   * `import "members.csv"` : Imports a prepared CSV file and reports rejected records.

   * `delete 3` : Deletes the 3rd contact shown in the current list.

   * `archive 2` : Marks the 2nd displayed member as archived without deleting their record.

   * `activate 2` : Marks the 2nd displayed member as active again.

   * `exit` : Exits the app.

1. Refer to the [Features](#features) section below for details of each command.

--------------------------------------------------------------------------------------------------------------------

## Features

<box type="info" seamless>

**Notes about the command format:**<br>

* Words in `UPPER_CASE` are the parameters to be supplied by the user.<br>
  For example, in `add n/NAME`, replace `NAME` with a value such as `John Doe`.

* Items in square brackets are optional.<br>
  For example, `n/NAME [t/TAG]` can be used as `n/John Doe t/friend` or as `n/John Doe`.

* Items followed by `...` can appear zero or more times.<br>
  For example, `[t/TAG]... ` may be omitted, or written as `t/friend` or `t/friend t/family`.

* Parameters can be in any order.<br>
  For example, if the command specifies `n/NAME p/PHONE_NUMBER`, `p/PHONE_NUMBER n/NAME` is also acceptable.

* Extraneous parameters for commands that take no parameters, such as `help`, `list`, `exit`, and `clear`, are ignored.<br>
  For example, `help 123` is interpreted as `help`.

* If you are using a PDF version of this document, be careful when copying and pasting commands that span multiple lines as space characters surrounding line-breaks may be omitted when copied over to the application.
</box>

### Recalling commands: ↑ and ↓

While the command box has focus, press **↑** to recall the newest submitted command, then press it again
to browse older commands. Press **↓** to browse newer commands. These keys work at any caret position;
they do not filter history by the text you have typed. Holding either key repeats navigation.
After a failed submission, the failed command is already selected in history: one ↑ recalls the preceding
entry, and ↓ can return to the failed command. If there is no preceding entry, ↑ leaves the field unchanged.
At the newest failed entry, ↓ is a no-op, even if you have edited it. Repeated ↓ presses preserve the text
and selection, and the next ↑ still reaches the preceding entry.
Navigation stops at the oldest entry and at your draft, without wrapping or displaying a message.
Shift/Ctrl/Alt/Meta combinations and arrow keys outside the command box retain their usual behavior.

Every non-blank submission is recorded before execution, including commands with syntax errors, commands
that cannot execute, and commands whose save fails. Recalling a command only fills the command box:
it does not run the command or replace the previous result message. Press Enter to execute it against
the current member list. Recalled text has the caret at the end and can be edited normally.

For example:

1. Submit `list`, then `lisst`. The result shows `Unknown command.` and the failed text remains in the command box.
1. Press ↑ once to recall `list`, then ↓ to return to `lisst` with normal input styling.
1. Correct it to `list` and press Enter. The result shows `Listed members in the selected status view.` and the command box clears.
1. Press ↑ twice to recall `list`, then `lisst`.

**Drafts:** When you first press ↑, the app remembers your unsent text and its caret/selection.
Pressing ↓ past the newest entry restores that draft exactly. For example, type `find n/Alex`, browse
older commands, then return to the newest end to recover `find n/Alex`.
Edits to each recalled command are remembered as you navigate up and down, even if you return to your draft
and browse again. This includes deleting all of a recalled command's text. The original submitted entries
remain unchanged until you resubmit edited text as a new command.
Pressing an arrow at a boundary leaves edits and selection unchanged. Submitting a non-blank command
ends browsing and clears the earlier draft and temporary edits to other entries, even if the submission
fails or repeats the newest command. A blank submission does not clear these edits.
Successful submissions clear the field. Failed submissions remain editable, with history positioned on the
failed entry and no duplicate draft position beyond it. Ordinary unsent drafts still restore when browsing
from the command box after a successful submission.

**Retention:** History keeps at most **100 entries**, dropping the oldest when a new entry exceeds that limit.
Consecutive exact duplicates use one entry, but every submission still executes. Spaces and letter case
are preserved, so `list`, ` list ` and `LIST` are distinct entries. Non-consecutive duplicates remain.
Empty or whitespace-only submissions are ignored without changing the field, results or history;
this uses Java whitespace rules (a non-breaking space is not considered blank).
Ignored blanks and consecutive duplicates do not evict entries. The draft does not count toward the limit.

History survives Help, focus changes, minimizing, and `clear` (which clears members only).
It is kept only for the current application session, independently in each running instance, and is lost
on restart. There is no history file or new history command. Existing application logging is unchanged;
recall history is never restored from logs.

### Viewing help: `help`

Shows a message explaining how to access the help page.

![help message](images/helpMessage.png)

Format: `help`


### Adding a person: `add`

Adds a person to the address book.

Format: `add e/EMAIL [n/NAME] [p/PHONE_NUMBER] [a/ADDRESS] [t/TAG]...`

Only email is required. Name, phone number, and address are independently optional: provide any details you know
and omit the other prefixes. For example, `add e/alex@u.nus.edu p/91234567` records an email and phone number.
An incomplete record is a normal active member and supports the existing edit, search, tag, and delete commands.

Supplied values must still be valid. Names contain alphanumeric characters and spaces; phones contain at least
three digits; addresses must be nonblank. An explicit blank value such as `n/`, `p/`, or `a/` is rejected,
including a value containing only spaces. To omit a detail, leave out its prefix entirely. Each contact prefix
can appear only once. Rejected commands leave the directory and current search results unchanged.

For an unnamed member, the card heading displays their email. Missing phone and address rows display
`Phone: -` and `Address: -`. The add confirmation shows only supplied details, omitting missing contact fields
and the tags section when there are no tags. For an email-only member:

```text
New person added: Email: alex@u.nus.edu
```

These placeholders are display text only. They are not stored as contact values or searched as names or phones.
When a name is present, add, edit, tag and delete confirmations label it as `Name: <person name>`.
For example, adding Alex with only a name and email shows `New person added: Name: Alex; Email: alex@u.nus.edu`.
After a successful add, the search is cleared. The new member appears in All or Active view.

Email is required and uniquely identifies a member. Email addresses are compared, displayed, and saved in
lowercase. Only complete email addresses ending with exactly `@u.nus.edu` are accepted. For example,
`Alex@U.NUS.EDU` is accepted and stored as `alex@u.nus.edu`; `alex`, `alex@gmail.com`, `alex@nus.edu`,
`alex@sub.u.nus.edu`, and `alex@u.nus.edu.com` are rejected without changing any records.
The local-part (before `@`) must contain letters or digits, optionally separated by single `+`, `_`, `.`, or `-`
characters. It cannot start or end with punctuation or contain adjacent punctuation.
The existing character rules accept ASCII letters and digits after lowercasing; mailbox existence is not checked.
Different aliases, such as
`alex@u.nus.edu` and `alex+society@u.nus.edu`, count as different addresses. Command parsing trims
surrounding argument whitespace, but email values in the data file cannot contain surrounding whitespace.

Invalid email input produces this message:

```text
Emails must have the format local-part@u.nus.edu.
The local-part must contain letters or digits, optionally separated by single +, _, ., or - characters.
It must not start or end with punctuation or contain consecutive punctuation.
Email addresses are case-insensitive and are stored in lowercase.
```

Members may share a name or phone number if their emails differ. For example, both commands are valid:

* `add n/Alex Tan p/91234567 e/Alex.One@U.NUS.EDU a/Kent Ridge`
* `add n/Alex Tan p/91234567 e/alex.two@u.nus.edu a/Clementi`

Reusing `ALEX.ONE@U.NUS.EDU` for another member is rejected with:
`Email "alex.one@u.nus.edu" is already used by "Alex Tan". No changes were made.`
The check includes members outside the current search results.
It also includes archived members.

If the existing member has no name, the error is:
`Email "alex.one@u.nus.edu" is already used by an existing member. No changes were made.`
Adding the same email again never fills or overwrites the existing record. Use `edit` to add the missing details.

<box type="tip" seamless>

**Tip:** A person can have any number of tags, including zero.
</box>

Tag names are alphanumeric and case-insensitive. They are stored and displayed in lowercase.
For example, adding a member with `t/Publicity t/publicity` gives that member one tag, `publicity`.
Loading saved records applies the same rule within each member's tag set.

Examples:
* `add e/newmember@u.nus.edu n/New Member t/Publicity`
* `add n/John Doe p/98765432 e/johnd@u.nus.edu a/John street, block 123, #01-01`
* `add n/Betsy Crowe t/friend e/betsycrowe@u.nus.edu a/Newgate Prison p/1234567 t/criminal`

### Importing member contacts from CSV: `import`

Adds valid members from a local CSV file and reports records rejected for invalid details or duplicate emails.
Existing members are never updated or merged. The source CSV is never changed.

Format: `import FILE_PATH`

Examples:

* `import members.csv`
* `import "member contacts.csv"`
* `import "C:\Members\member contacts.csv"`

Relative paths start from the folder where you launched the app. Enclose paths containing spaces in double quotes.
Export Google Sheets or Excel data as a **comma-separated UTF-8 CSV** before importing.

#### Prepare the CSV

The [sample CSV](samples/members.csv) contains the required header row followed by two example members:

```csv
name,phone,email,address,tags,status
Alex Tan,91234567,alex@u.nus.edu,"NUS, Kent Ridge",Publicity;Exco,active
Jamie Lee,98765432,jamie@u.nus.edu,Clementi,,archived
```

Copy or download this file and replace the example members with your own data. Keep the lowercase header row
unchanged and include all six columns in every member record, even when optional cells are blank.
Surrounding whitespace is removed from member fields before validation.

| Column | Accepted values |
| --- | --- |
| `name` | Optional. Leave blank if unknown; supplied names follow the `add` rules. |
| `phone` | Optional. Leave blank if unknown; supplied numbers contain at least three digits and no other characters. |
| `email` | Required. A valid email ending with exactly `@u.nus.edu`; stored in lowercase. |
| `address` | Optional. Leave blank if unknown; supplied addresses follow the `add` rules. |
| `tags` | Optional. Separate alphanumeric tags with semicolons, e.g., `Publicity;Exco`. Case variants become one lowercase tag per member. |
| `status` | Required. Exactly `active` or `archived`. |

Whitespace-only name, phone, and address cells count as absent. An empty tags cell means no tags; empty tags
inside a list, such as `publicity;`, are invalid.

The imported status is preserved;
  `archived` members display the `ARCHIVED` badge. Status is separate from tags.

Quote cells containing commas or double quotes. Inside a quoted cell, write a double quote as `""`.
Quoted line breaks are parsed as part of a cell, but line breaks remaining inside a contact value fail validation.
A UTF-8 byte order mark and empty lines are accepted. Empty files and header-only files are rejected.

#### Check the import result

If at least one member is accepted, the app appends accepted members in CSV order, clears the search,
and saves it as JSON. Feedback shows the imported and rejected counts, followed by each rejection's record number
and first reason. The header is record 1; the first member is record 2. Empty lines do not count.
A quoted cell spanning several lines still belongs to one record.

An email conflicts with any existing member, including archived members and members outside the current search
results. Within the CSV, the first valid occurrence wins; later valid occurrences are rejected. An invalid record
does not reserve its email. Matching ignores email case; names and phone numbers do not determine duplicates.

For example, import the [partial-import sample](samples/members-partial.csv) into a directory without its emails.
Alex and the email-only member are accepted; Jamie's phone is invalid, and Duplicate Alex repeats Alex's email:

```text
Imported 2 member(s); rejected 2 record(s) from members-partial.csv.
Rejected records:
CSV record 3: Phone numbers should only contain digits, and should be at least 3 digits long
CSV record 4: email "alex@u.nus.edu" duplicates an existing member or an earlier accepted CSV record.
```

Correct the rejected records and import again. If you retry the complete file, previously imported members are
reported as duplicates. Re-importing an unchanged file adds no members while those emails remain in the directory.

#### Rejections and failures

* **Some records are rejected:** Other valid records are imported. A record with missing or extra columns is
  rejected individually if the CSV itself can still be parsed.
* **Every record is rejected:** Feedback lists all rejection reasons. Member records, search results, and saved
  JSON remain unchanged.
* **The file cannot be read or parsed:** Unreadable or non-UTF-8 files, invalid headers, malformed CSV, empty files,
  and header-only files stop the entire import. No records are added, even if the file starts with valid records.
  Fix the file or path and retry. The app also rejects a source that points to its JSON save file, including a link
  to that file.
* **Automatic saving fails:** The import is reverted. Follow [Saving the data](#saving-the-data) to resolve the
  problem, then retry. The source CSV is unchanged.

### Listing members: `list`

Clears the current search and shows members in the selected status view.

Format: `list`

The GUI starts in **All** view. Each click of the status button cycles **All → Active → Archived → All**.
All shows both statuses. Switching views keeps the current search, while `list` clears it. Indices always refer
to the members currently shown on screen.

`edit`, `tag`, `untag`, `archive`, `activate`, and `delete` use the index in that visible list when the command
is submitted. The index can change when you switch status views, search, or run a command. For example, if an
archived member is third in All view but first in Archived view, `edit 1 p/91234567` updates that archived
member while Archived is selected. An index outside the visible list is rejected, even if it exists in the
complete directory. Recheck the displayed index before another command; successful edits, tag changes, archives,
and activations clear the search but keep the selected status view, while deletion keeps the current search.

For example, run `find t/Publicity`, then switch to Archived to see archived members with that tag. Run `list`
to show all archived members; the button stays on Archived. Switch back to All to see both statuses.

### Editing a person: `edit`

Edits an existing person in the address book.

Format: `edit INDEX [n/NAME] [p/PHONE_NUMBER] [e/EMAIL] [a/ADDRESS]`

* Edits the person at the specified `INDEX`. The index refers to the index number shown in the displayed person list. The index **must be a positive integer** 1, 2, 3, ...
* At least one of the optional fields must be provided.
* Existing values will be updated to the input values.
* Missing contact details can be filled in one at a time. Omitting a field preserves its current value or absence.
  Supplied blank values are rejected; this command cannot clear a name, phone, address, or email.
* Successful edits clear the search and display members in the selected status view. Recheck the displayed index
  before the next edit.
* The confirmation shows the member's current supplied details, including retained values, and omits missing
  fields and empty tags. For an email-only member, it reads `Edited person: Email: 2@u.nus.edu`.
* A provided email must follow the same `@u.nus.edu` rules as `add`. Omitting `e/` preserves the current email.
* Existing tags are preserved. Any `t/` argument, including an empty `t/`, rejects the entire edit.
  Use `tag` or `untag` to change tags.
* Renaming a member or re-entering their own email is allowed. Changing their email releases the previous
  address for reuse. The new email cannot belong to another member, including one outside the displayed list.

Examples:
*  `edit 1 p/91234567 e/johndoe@u.nus.edu` Edits the phone number and email address of the 1st person to be `91234567` and `johndoe@u.nus.edu` respectively.
*  `edit 2 n/Betsy Crower` changes the 2nd displayed person's name to `Betsy Crower` and preserves their tags.
*  `edit 2 t/` is rejected. The member's contact details and tags remain unchanged.

To complete an email-only member, run `find e/newmember@u.nus.edu`, identify the member's displayed index,
and use `edit INDEX n/New Member`. Search again or recheck the index before using `edit INDEX p/91234567`
and `edit INDEX a/Kent Ridge`. Each step saves automatically; you can close and reopen the app between steps.

### Adding a tag to a member: `tag`

Adds one tag to one member, preserving their other tags and contact details.

Format: `tag INDEX t/TAG`

* `INDEX` must be a positive integer referring to a member in the currently displayed list, including search results.
* Exactly one index and one `t/` argument are required. Empty tags, multiple tags, and extra arguments are rejected.
* Tag names must be alphanumeric. Matching is case-insensitive; tags are stored and displayed in lowercase.
* Adding a tag the member already has succeeds with the normal completion message and leaves their tags unchanged.
* After success, the search is cleared, as with `edit`. Check the displayed indices before your next command.

Example: `tag 1 t/Volunteer` adds `volunteer` to the first displayed member. If they already have `publicity`
and `exco`, they will have all three tags afterward.

### Removing a tag from a member: `untag`

Removes one matching tag from one member, preserving their other tags and contact details.

Format: `untag INDEX t/TAG`

* The same index, single-tag, and tag-name rules as `tag` apply.
* Matching is case-insensitive: `t/publicity` also matches a tag entered as `Publicity`.
* Removing a tag the member does not have succeeds with the normal completion message and leaves their tags unchanged.
* Removing the last tag is allowed and leaves the member with no tags.
* After success, the search is cleared, as with `edit`. Check the displayed indices before your next command.

Example: `untag 1 t/publicity` removes only `publicity` from the first displayed member. If their tags were
`publicity`, `exco`, and `volunteer`, they will retain `exco` and `volunteer`.

Both commands save changes automatically. An invalid command leaves member records and the current filter unchanged.

Both confirmations show only supplied contact details. The `Name:` field is omitted when the member has no name,
and missing phone and address fields are omitted. The `Tags:` section appears only when tags remain.
For an email-only member, adding and then removing their only tag produces:

```text
Tag added: [publicity]; Email: 2@u.nus.edu; Tags: [publicity]
Tag removed: [publicity]; Email: 2@u.nus.edu
```

### Locating members by name, phone, email or tag: `find`

Finds members by name keywords, a phone number substring, an email address substring or one exact tag.
Matching members are displayed in a list with index numbers, together with the number of matches.
Use `findall` or `findany` (below) to combine fields or repeat a field.

Formats:

* `find KEYWORD [MORE_KEYWORDS]...` for name searches matching any keyword.
* `find n/KEYWORD` for a name search using one keyword.
* `find p/PHONE_SUBSTRING` for phone searches.
* `find e/EMAIL_SUBSTRING` for email searches.
* `find t/TAG` for tag searches.

**Name searches**

* Use `find n/Alice` or `find Alice` to search for one name keyword; both return the same results.
* The `n/` prefix accepts exactly one nonempty keyword. Commands such as `find n/Alice Bob` and
  `find n/Alice Alice` are rejected, including keywords separated by tabs or line breaks.
  The error is: "Prefixed name searches accept only one keyword. Use find Alice Bob to match names containing
  Alice OR Bob."
* For multiple keywords, omit `n/`: `find Alice Bob` returns members whose names contain Alice or Bob.
  Previously accepted commands such as `find n/Alice Bob` must now be written as `find Alice Bob`.
* Surrounding whitespace is ignored, including whitespace between `n/` and its keyword.
* Name matching is case-insensitive; for example, `hans` matches `Hans`.
* In an unprefixed search, keyword order does not matter: `find Hans Bo` and `find Bo Hans` return the same results.
* Unprefixed searches and `n/` searches consider only names.
* Only full words match; for example, `Han` does not match `Hans`.
* Unprefixed searches return members matching at least one keyword (an `OR` search); for example, `find Hans Bo` returns
  `Hans Gruber` and `Bo Yang`.

**Phone searches**

* The search considers only phone numbers and matches a consecutive sequence of digits anywhere in the number.
  For example, `find p/9103` matches the phone number `91031282`.
* Phone searches support only one substring. Commands such as `find p/9123 1234` are rejected with an error.
  The same applies to substrings separated by tabs or line breaks.
  Use `findany p/9123 p/1234` to match either substring.
* Both partial and full phone numbers are accepted. Even one or two digits can be used, such as `find p/9` or `find p/91`.
* Use only ASCII digits (`0-9`). Letters, punctuation and non-ASCII digits are rejected, for example,
  `find p/abc`, `find p/+659123`, `find p/9123-4567` and `find p/９１２３`.
  The error is: "Phone searches accept digits (0-9) only. Example: find p/9123."
* Leading zeros are preserved: `find p/001` searches for the exact sequence `001`.
  Surrounding whitespace is ignored. Rejected searches leave the displayed member list and member records unchanged.
* Include `p/` to search phone numbers: `find 9103` searches names, while `find p/9103` searches phone numbers.

**Email searches**

* The search considers only email addresses and matches a literal substring anywhere in the address, ignoring case.
  For example, `find e/ALEX` matches `alexyeoh@u.nus.edu`.
* Both partial and full email addresses are accepted. The search value need not be a valid complete email address;
  fragments such as `alex`, `@u.nus.edu` and `+` are accepted.
* Full addresses use the same substring rule. For example, `find e/alice@u.nus.edu` matches both
  `alice@u.nus.edu` and `malice@u.nus.edu`.
* Email searches support only one substring. Commands such as `find e/alex david` are rejected with an error.
  The same applies to substrings separated by tabs or line breaks.
  Use `findany e/alex e/david` to match either substring.
* Include `e/` to search email addresses: `find alex@u.nus.edu` searches names,
  while `find e/alex@u.nus.edu` searches emails.
* `find e/@u.nus.edu` returns every member because all stored email addresses use that domain.

**Tag searches**

* The search considers only assigned tags and matches the complete tag name, ignoring case.
  For example, `find t/publicity` and `find t/PUBLICITY` return the same members.
* Partial names do not match: `find t/pub` matches only the tag `pub`, not `publicity`.
* A member matches if any of their assigned tags equals the requested tag. Members with no tags do not match.
* Supply one alphanumeric tag. Spaces and punctuation are not allowed within a tag name.
* Commands such as `find t/publicity exco` are rejected, including values separated by tabs or line breaks.
  The error is: "Tag searches accept only one tag. Example: find t/publicity."
* Tag searches include active and archived members, as well as members with incomplete contact details.
  Include `t/`: `find publicity` searches names, while `find t/publicity` searches tags.

**Rules for `find`**

* Search one field per command. Combining fields, such as `find n/Alice p/9123`, `find e/alice n/Alice`
  or `find t/publicity e/alice`, is rejected.
* Use lowercase prefixes: `n/`, `p/`, `e/` and `t/`. Uppercase prefixes such as `N/`, `P/`, `E/` and `T/`
  are rejected with: "Search prefixes must be lowercase. Use n/, p/, e/ or t/."
  This rule applies to prefixes; uppercase values such as `find n/ALICE`, `find e/ALICE` and `find t/PUBLICITY`
  remain valid.
* Other field prefixes are unsupported. For example, `find G/`, `find g/alice`, `find name/Alice`
  and `find n/Alice G/value` are rejected. For `G/`, the error is:
  "Unsupported search prefix: G/. Use n/, p/, e/ or t/."
  At the start of the search or after whitespace, a token beginning with a nonempty label followed by `/`
  is treated as a field selection. Slash-containing values attached to a supported prefix, such as `find e/G/`,
  remain literal search values.
* Use each prefix only once. Commands such as `find n/Alice n/Bob`, `find p/9123 p/4567`,
  `find e/alice e/bob` and `find t/publicity t/exco` are rejected.
* Provide a nonempty search value. Commands such as `find`, `find n/`, `find p/`, `find e/` and `find t/` are rejected.
* In a prefixed search, place the prefix before all search text. For example, `find Alice p/9123` is rejected.
* If no members match, the list is empty and the result count is zero.
* Each search checks the complete directory and replaces the previous search results.
* Rejected searches leave the displayed results, member records and saved data unchanged.
* A member with no name cannot match a name search, and a member with no phone cannot match a phone search.
  Use `find e/EMAIL_SUBSTRING` or an assigned tag for members with incomplete contact details.
  Display placeholders do not match searches.

Examples:

* `find John` returns members named `john` and `John Doe`.
* `find n/alex` returns members whose names contain the whole word `alex`, ignoring case.
* `find n/alex david` is rejected; use `find alex david` to match either name keyword.
* `find p/9103` returns members whose phone numbers contain `9103`, including David Li in the sample data.
* `find p/91031282` returns members whose phone numbers contain the full number `91031282`.
* `find e/ALEX` returns members whose email addresses contain `alex`, including Alex Yeoh in the sample data.
* `find e/ALExYeOh@U.NUS.EDU` returns Alex Yeoh in the sample data.
* `find t/publicity` returns every member tagged `publicity`, including members who also have other tags.
* `find t/PUBLICITY` returns the same members as `find t/publicity`.
* `find alex david` returns `Alex Yeoh`, `David Li`<br>
  ![result for 'find alex david'](images/findAlexDavidResult.png)

### Combining search conditions: `findall` and `findany`

`findall` returns members matching **every** condition (AND). `findany` returns members matching
**at least one** condition (OR).

Formats:

```text
findall PREFIX/VALUE [PREFIX/VALUE]...
findany PREFIX/VALUE [PREFIX/VALUE]...
```

* Supply at least one condition. Every condition needs a lowercase `n/`, `p/`, `e/` or `t/` prefix
  and one nonempty value. Field validation and matching follow `find` above.
* Repeat a prefix to add another condition: use `n/John n/Tan`, not `n/John Tan`.
* Every condition must be valid. For example, `findany n/Alice p/abc` is rejected even if Alice exists.
  Rejected searches preserve the previous results, member records and saved data.

| Example | Matches |
| --- | --- |
| `findall n/John n/Tan` | Names containing both whole words, in either order |
| `findall n/John t/exco` | Members whose name contains the whole word John and who have the exco tag |
| `findall t/exco t/publicity` | Members with both tags |
| `findany t/exco t/publicity` | Members with either tag |
| `findany n/Alice e/alice` | Members whose name contains Alice or whose email contains alice |

Each search checks the complete directory and replaces the previous results. The selected status view determines
which matches appear and are counted. Members appear once, in directory order. If no members match in the selected
view, the list is empty and the count is zero. All view includes both active and archived matches.
A missing name or phone fails that condition; with `findany`, another condition can still match.

### Archiving a member: `archive`

Marks a member as archived while keeping their contact details and tags. The badge changes to `ARCHIVED`.

Format: `archive INDEX`

* `INDEX` is a positive integer from the currently displayed list, including search results.
* An already archived member cannot be archived again.
* Archived members appear in All or Archived view and can still be found, edited, and activated.
* A successful command clears the search and saves the new status automatically. In Active view, the archived member
  disappears from the displayed list; in All view, the member remains visible with an `ARCHIVED` badge.

Example: `find n/Jamie` followed by `archive 1` archives the first displayed result.

### Activating a member: `activate`

Marks an archived member as active again. The badge changes to `ACTIVE`.

Format: `activate INDEX`

The displayed-index rules are the same as for `archive`. An already active member cannot be activated again.
The member's other details remain unchanged, and the new status is saved automatically.

Example: switch to Archived view, run `list`, then `activate 2` to activate the second displayed member. It then
appears in All or Active view.

### Deleting a person: `delete`

Deletes the specified person from the address book.

Format: `delete INDEX`

* Deletes the person at the specified `INDEX`.
* The index refers to the index number shown in the displayed person list.
* The index **must be a positive integer** 1, 2, 3, ...

Examples:
* `list` followed by `delete 2` deletes the 2nd member in the selected status view.
* `find Betsy` followed by `delete 1` deletes the 1st person in the results of the `find` command.

The deletion confirmation shows only the member's supplied details. For an email-only member with no tags,
it reads `Deleted person: Email: bobby@u.nus.edu`.

### Clearing all entries: `clear`

Clears all entries from the address book.

Format: `clear`

### Exiting the program: `exit`

Exits the program.

Format: `exit`

### Saving the data

AddressBook automatically saves data after every successful command. You do not need to save manually.

If saving fails, the command's changes are reverted: the previous member records, search filter, and data file
are preserved. The result shows a saving error instead of a success confirmation. For example, an `add` whose save
fails leaves no new member in the directory.

Resolve the reported cause, such as insufficient permissions or disk space, then retry the command. The data folder's
filesystem must support atomic file replacement; otherwise saving fails. Use a supported location for the data file.

If the data file cannot be fully loaded, saving and changes to members are disabled to protect the original file.

### Editing the data file

AddressBook data is saved automatically as a JSON file `[JAR file location]/data/addressbook.json`. Advanced users are welcome to update data directly by editing that data file.

Missing names, phones, and addresses are saved as JSON `null`, for example:

```json
{
  "persons": [
    {
      "name": null,
      "phone": null,
      "email": "alex@u.nus.edu",
      "address": null,
      "status": "active",
      "tags": []
    }
  ]
}
```

For these three optional fields, an omitted property also loads as missing. Empty strings, whitespace-only
strings, and invalid supplied values are rejected. Email must always be present and valid. Older complete
records continue to load. An omitted or `null` status defaults to `active`; a supplied status must be exactly
`active` or `archived`. Loading alone does not rewrite a file; the next successful command saves the resolved status
and absent optional fields as explicit `null` values.

<box type="warning" seamless>

**Caution:**
If some records are invalid, AddressBook loads the valid records and shows a warning with the file path,
recovered-member count, and each rejected record's original position and reason. For duplicate emails
(including case variants), the first valid record is retained. If the JSON syntax or overall structure is
invalid, or the file cannot be read, the app shows an empty member list and a warning.<br>
The original file is preserved. You can use `list`, `find`, `findall`, `findany`, `help`, and `exit`, but member changes
and saving are disabled for that session. Close the app, back up and repair the file, then restart. Normal saving resumes
only when the entire file loads successfully. A missing file still starts with sample members.<br>
Furthermore, certain edits can cause the AddressBook to behave in unexpected ways (e.g., if a value entered is outside of the acceptable range). Therefore, edit the data file only if you are confident that you can update it correctly.
</box>

Valid mixed-case emails appear in lowercase when loaded and are saved in lowercase after the next successful
command. Older versions of the app may reject same-name records saved by this version, so back up the data file
before switching versions.

Versions that require complete contact details reject the entire data file if any member is incomplete.
They may open an empty directory and overwrite the original file after a successful command, including `list`.
Back up the file before switching versions; incomplete records are not compatible with those older versions.

### Archiving data files `[coming in v2.0]`

_Details coming soon ..._

--------------------------------------------------------------------------------------------------------------------

## FAQ

**Q**: How do I transfer my data to another computer?<br>
**A**: Install the app on the other computer and overwrite the data file it creates with the data file from your previous AddressBook home folder.

--------------------------------------------------------------------------------------------------------------------

## Known issues

1. **When using multiple screens**, if you move the application to a secondary screen, and later switch to using only the primary screen, the GUI will open off-screen. The remedy is to delete the `preferences.json` file created by the application before running the application again.
2. **If you minimize the Help Window** and then run the `help` command (or use the `Help` menu, or the keyboard shortcut `F1`) again, the original Help Window will remain minimized, and no new Help Window will appear. The remedy is to manually restore the minimized Help Window.

--------------------------------------------------------------------------------------------------------------------

## Command summary

Action     | Format, Examples
-----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------
**Add**    | `add e/EMAIL [n/NAME] [p/PHONE_NUMBER] [a/ADDRESS] [t/TAG]...` <br> e.g., `add e/jamesho@u.nus.edu` or `add n/James Ho p/22224444 e/jamesho@u.nus.edu a/123, Clementi Rd, 1234665 t/President`
**Activate** | `activate INDEX`<br> e.g., `activate 2`
**Archive** | `archive INDEX`<br> e.g., `archive 2`
**Clear**  | `clear`
**Delete** | `delete INDEX`<br> e.g., `delete 3`
**Edit**   | `edit INDEX [n/NAME] [p/PHONE_NUMBER] [e/EMAIL] [a/ADDRESS]`<br> e.g.,`edit 2 n/James Lee e/jameslee@u.nus.edu`
**Find**   | `find KEYWORD [MORE_KEYWORDS]...`, `find n/KEYWORD`, `find p/PHONE_SUBSTRING`, `find e/EMAIL_SUBSTRING` or `find t/TAG`<br> e.g., `find James Jake`, `find n/James`, `find p/9103`, `find e/alex`, `find t/publicity`
**Find all** | `findall PREFIX/VALUE [PREFIX/VALUE]...`<br> e.g., `findall n/John t/exco`
**Find any** | `findany PREFIX/VALUE [PREFIX/VALUE]...`<br> e.g., `findany n/Alice e/alice`
**List**   | `list`
**Import** | `import FILE_PATH`<br> e.g., `import "member contacts.csv"`
**Add tag** | `tag INDEX t/TAG`<br> e.g., `tag 1 t/Volunteer`
**Remove tag** | `untag INDEX t/TAG`<br> e.g., `untag 1 t/publicity`
**Help**   | `help`
