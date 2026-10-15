---
  layout: default.md
  title: "User Guide"
  pageNav: 3
---

# AB-3 User Guide

AddressBook Level 3 (AB3) is a **desktop application for managing contacts, optimized for use through a Command Line Interface (CLI)** while retaining the benefits of a Graphical User Interface (GUI). If you type quickly, AB3 can help you manage contacts faster than traditional GUI applications.

<!-- * Table of Contents -->
<page-nav-print />

--------------------------------------------------------------------------------------------------------------------

## Quick start

1. Ensure that Java `25` or later is installed on your computer.<br>
   **Mac users:** Ensure you have the precise JDK version prescribed [here](https://se-education.org/guides/tutorials/javaInstallationMac.html).

1. Download the latest `.jar` file from [here](https://github.com/se-edu/addressbook-level3/releases).

1. Copy the file to the folder you want to use as the _home folder_ for your AddressBook.

1. Open a terminal, `cd` to the folder containing the JAR file, and run `java -jar addressbook.jar`.<br>
   A GUI similar to the one below should appear in a few seconds. Note how the app contains some sample data.<br>
   ![Ui](images/Ui.png)

1. Type a command in the command box and press Enter to execute it. For example, type **`help`** and press Enter to open the help window.<br>
   Some example commands you can try:

   * `list` : Lists all contacts.

   * `add e/johnd@u.nus.edu` : Adds a member whose other contact details can be filled in later.

   * `delete 3` : Deletes the 3rd contact shown in the current list.

   * `archive 2` : Marks the 2nd displayed member as archived without deleting their record.

   * `activate 2` : Marks the 2nd displayed member as active again.

   * `clear` : Deletes all contacts.

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
After a successful add, all members are displayed in their existing order, with the new member appended.

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
For example, `t/Publicity t/publicity` adds one tag named `publicity`.
Tags in existing saved records follow the same rule when loaded; case variants are merged.

Examples:
* `add e/newmember@u.nus.edu n/New Member t/Publicity`
* `add n/John Doe p/98765432 e/johnd@u.nus.edu a/John street, block 123, #01-01`
* `add n/Betsy Crowe t/friend e/betsycrowe@u.nus.edu a/Newgate Prison p/1234567 t/criminal`

### Importing member contacts from CSV: `import`

Appends an existing member directory from a CSV file. The entire import is rejected if any member is invalid
or has a duplicate email; valid records are not imported separately in this version.

Format: `import FILE_PATH`

Examples:

* `import members.csv`
* `import "member contacts.csv"`
* `import "C:\Members\member contacts.csv"`

Relative paths are resolved from the folder where the application was launched. Use double quotes around paths
containing spaces. The command reads a local file; it does not download a Google Sheet or open an Excel workbook.

Save your spreadsheet as a comma-separated UTF-8 CSV using these lowercase headers in this exact order:

```csv
name,phone,email,address,tags,status
Alex Tan,91234567,alex@u.nus.edu,"NUS, Kent Ridge",Publicity;Exco,active
Jamie Lee,98765432,jamie@u.nus.edu,Clementi,,archived
```

A [sample CSV](samples/members.csv) is available to copy or download. Replace its example members with your own data.

* Every member must have all six columns. Name, phone, email, address, and status must be nonempty.
  Incomplete records are supported through `add` and JSON storage; CSV import still requires complete contact details.
* Contact fields follow the same validation rules as `add`. Surrounding field whitespace is removed.
  Phone numbers contain digits only and have at least three digits. Emails must end with exactly `@u.nus.edu`.
* Separate tags with semicolons, for example `Publicity;Exco`. Leave the tags cell empty for a member with no tags.
  Each tag must be alphanumeric. Tags and emails are normalized to lowercase; case variants of tags are merged.
  Empty tags inside a list, such as `publicity;`, are rejected.
* Status is required and must be exactly `active` or `archived`. It is preserved during import;
  `archived` members display the existing `FORMER` badge. Status is separate from tags.
* Quote cells containing commas or double quotes. Inside a quoted cell, write a double quote as `""`.
  Embedded line breaks are parsed as part of a cell, but existing contact-field rules reject multiline values.
* A UTF-8 byte order mark and empty lines are accepted. An empty file or a header without members is rejected.

On success, members are appended in CSV order, existing records remain unchanged, and the full directory is displayed.
The feedback states how many members were imported. The combined directory is automatically saved as JSON.
The source CSV is never changed. An import source that points to the application's JSON save file is rejected.

Duplicate checks include every existing member, including archived members and members outside the current search
results, as well as other records in the CSV. Matching is by case-insensitive email; shared names or phone numbers
are allowed when emails differ. Re-importing the same CSV is rejected rather than updating existing members.

Invalid records produce a message with a CSV record number and the reason. The header counts as record 1,
empty lines do not count, and record numbers can differ from physical line numbers when quoted cells span lines.
Correct the reported problem and retry the complete file. Invalid headers, malformed CSV, and unreadable files also
reject the import. Rejected imports leave member records, the current search results, and saved JSON unchanged.

If automatic saving fails after a successful validation, an error is shown. Imported members may remain in the current
session without being saved, as with `add`; confirm that saving succeeds before closing the application.

### Listing all persons: `list`

Shows a list of all persons in the address book.

Format: `list`

### Editing a person: `edit`

Edits an existing person in the address book.

Format: `edit INDEX [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS]`

* Edits the person at the specified `INDEX`. The index refers to the index number shown in the displayed person list. The index **must be a positive integer** 1, 2, 3, ...
* At least one of the optional fields must be provided.
* Existing values will be updated to the input values.
* Missing contact details can be filled in one at a time. Omitting a field preserves its current value or absence.
  Supplied blank values are rejected; this command cannot clear a name, phone, address, or email.
* Successful edits display the full directory. Recheck the displayed index before the next edit.
* The confirmation shows the member's current supplied details, including retained values, and omits missing
  fields and empty tags. For an email-only member, it reads `Edited person: Email: 2@u.nus.edu`.
* A provided email must follow the same `@u.nus.edu` rules as `add`. Omitting `e/` preserves the current email.
* Existing tags are preserved. The `edit` command rejects `t/` arguments, including an empty `t/`.
* Renaming a member or re-entering their own email is allowed. Changing their email releases the previous
  address for reuse. The new email cannot belong to another member, including one outside the displayed list.

Examples:
*  `edit 1 p/91234567 e/johndoe@u.nus.edu` Edits the phone number and email address of the 1st person to be `91234567` and `johndoe@u.nus.edu` respectively.
*  `edit 2 n/Betsy Crower` Edits the name of the 2nd person to be `Betsy Crower`.
*  `edit 2 t/` Is rejected because tags cannot be edited with `edit`. No tags are removed and the person's details remain unchanged.

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
* After success, all members are displayed, as with `edit`. Check the displayed indices before your next command.

Example: `tag 1 t/Volunteer` adds `volunteer` to the first displayed member. If they already have `publicity`
and `exco`, they will have all three tags afterward.

### Removing a tag from a member: `untag`

Removes one matching tag from one member, preserving their other tags and contact details.

Format: `untag INDEX t/TAG`

* The same index, single-tag, and tag-name rules as `tag` apply.
* Matching is case-insensitive: `t/publicity` also matches a tag entered as `Publicity`.
* Removing a tag the member does not have succeeds with the normal completion message and leaves their tags unchanged.
* Removing the last tag is allowed and leaves the member with no tags.
* After success, all members are displayed, as with `edit`. Check the displayed indices before your next command.

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

### Locating members by name, phone or email: `find`

Finds members by name keywords, a phone number substring or an email address substring. Matching members are displayed
in a list with index numbers, together with the number of matches.

Formats:

* `find [n/]KEYWORD [MORE_KEYWORDS]...` for name searches.
* `find p/PHONE_SUBSTRING` for phone searches.
* `find e/EMAIL_SUBSTRING` for email searches.

**Name searches**

* The `n/` prefix is optional: `find Alice Bob` and `find n/Alice Bob` return the same results.
* Name matching is case-insensitive; for example, `hans` matches `Hans`.
* Keyword order does not matter; for example, `Hans Bo` matches `Bo Hans`.
* Unprefixed searches and `n/` searches consider only names.
* Only full words match; for example, `Han` does not match `Hans`.
* Members matching at least one keyword are returned (an `OR` search); for example, `Hans Bo` returns
  `Hans Gruber` and `Bo Yang`.

**Phone searches**

* The search considers only phone numbers and matches a consecutive sequence of digits anywhere in the number.
  For example, `find p/9103` matches the phone number `91031282`.
* Phone searches support only one substring. Commands such as `find p/9123 1234` are rejected with an error.
  The same applies to substrings separated by tabs or line breaks.
  Search separately with `find p/9123` and `find p/1234`.
* Both partial and full phone numbers are accepted. Even one or two digits can be used, such as `find p/9` or `find p/91`.
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
  Search separately with `find e/alex` and `find e/david`.
* Include `e/` to search email addresses: `find alex@u.nus.edu` searches names,
  while `find e/alex@u.nus.edu` searches emails.
* `find e/@u.nus.edu` returns every member because all stored email addresses use that domain.

**Search rules**

* Search one field per command. Combining fields, such as `find n/Alice p/9123` or `find e/alice n/Alice`, is rejected.
* Use each prefix only once. Commands such as `find n/Alice n/Bob`, `find p/9123 p/4567` and
  `find e/alice e/bob` are rejected.
* Provide a nonempty search value. Commands such as `find`, `find n/`, `find p/` and `find e/` are rejected.
* In a prefixed search, place the prefix before all search text. For example, `find Alice p/9123` is rejected.
* If no members match, the list is empty and the result count is zero.
* Each search checks the complete directory and replaces the previous search results.
* A member with no name cannot match a name search, and a member with no phone cannot match a phone search.
  Use `find e/EMAIL_SUBSTRING` for email-only members. Display placeholders do not match searches.

Examples:

* `find John` returns members named `john` and `John Doe`.
* `find n/alex david` returns the same members as `find alex david`.
* `find p/9103` returns members whose phone numbers contain `9103`, including David Li in the sample data.
* `find p/91031282` returns members whose phone numbers contain the full number `91031282`.
* `find e/ALEX` returns members whose email addresses contain `alex`, including Alex Yeoh in the sample data.
* `find e/ALExYeOh@U.NUS.EDU` returns Alex Yeoh in the sample data.
* `find alex david` returns `Alex Yeoh`, `David Li`<br>
  ![result for 'find alex david'](images/findAlexDavidResult.png)

### Archiving a member: `archive`

Marks a member as archived while keeping their contact details and tags. The badge changes to `FORMER`.

Format: `archive INDEX`

* `INDEX` is a positive integer from the currently displayed list, including search results.
* An already archived member cannot be archived again.
* Archived members remain visible in `list` and can still be found, edited, and activated.
* A successful command restores the full displayed list and saves the new status automatically.

Example: `find n/Jamie` followed by `archive 1` archives the first displayed result.

### Activating a member: `activate`

Marks an archived member as active again. The badge changes to `ACTIVE`.

Format: `activate INDEX`

The displayed-index rules are the same as for `archive`. An already active member cannot be activated again.
The member's other details remain unchanged, and the new status is saved automatically.

Example: `list` followed by `activate 2` activates the second displayed member.

### Deleting a person: `delete`

Deletes the specified person from the address book.

Format: `delete INDEX`

* Deletes the person at the specified `INDEX`.
* The index refers to the index number shown in the displayed person list.
* The index **must be a positive integer** 1, 2, 3, ...

Examples:
* `list` followed by `delete 2` deletes the 2nd person in the address book.
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

AddressBook automatically saves data after every command. You do not need to save manually.

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
If your changes make the data file invalid, AddressBook opens with an empty address book at the next run. This
includes files with duplicate emails that differ only in letter case. Loading alone leaves the invalid file
unchanged, but the next successful command, including `list`, saves the current in-memory address book over it.
Back up the file and correct it before running commands.<br>
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
**Find**   | `find [n/]KEYWORD [MORE_KEYWORDS]...`, `find p/PHONE_SUBSTRING` or `find e/EMAIL_SUBSTRING`<br> e.g., `find James Jake`, `find n/James Jake`, `find p/9103`, `find e/alex`
**List**   | `list`
**Import** | `import FILE_PATH`<br> e.g., `import "member contacts.csv"`
**Add tag** | `tag INDEX t/TAG`<br> e.g., `tag 1 t/Volunteer`
**Remove tag** | `untag INDEX t/TAG`<br> e.g., `untag 1 t/publicity`
**Help**   | `help`
