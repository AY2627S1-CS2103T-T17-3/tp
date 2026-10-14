[![CI Status](https://github.com/AY2627S1-CS2103T-T17-3/tp/workflows/Java%20CI/badge.svg)](https://github.com/AY2627S1-CS2103T-T17-3/tp/actions)
[![codecov](https://codecov.io/gh/AY2627S1-CS2103T-T17-3/tp/graph/badge.svg?token=X6TRWEESQX)](https://codecov.io/gh/AY2627S1-CS2103T-T17-3/tp)

![Ui](docs/images/Ui.png)

The screenshot is a UI concept that includes a `FORMER` badge. New members start Active; archived records loaded from saved data or CSV display `FORMER`.

# NUSocietyDesk

NUSocietyDesk is a member directory application being developed for NUS society secretaries. It aims to make it easier to organise member contact details, track society roles and committees using tags, and find members using partial information.

The planned application will combine typed commands with a graphical interface for viewing member records.

## Project status

**NUSocietyDesk is under development. The project-specific functionality described below has not yet been implemented.** Features and command examples represent the intended minimum viable product (MVP) and may change during development.

## Planned features

| Feature | Intended behaviour |
| --- | --- |
| Add member contacts | Store a member's name, phone number, email address, and address, with optional society-related tags. |
| Organise members with tags | Attach tags such as `Publicity` and `President` to describe committees and roles. |
| Display member status | Show an `ACTIVE` or `FORMER` badge beside a member's tags. Archived members remain in list and search results. |
| Archive and activate members | Change a member's status by their displayed index without deleting contact details. |
| Find members | Search by a partial name, phone number, email address, address, or tag, using one field at a time. |
| List all members | Display the complete member directory and restore the full list after a search. |
| Delete member contacts | Permanently remove a member using their index in the currently displayed list. |
| Save data automatically | Save the directory locally after successful commands and load stored contacts on startup. |
| Receive command feedback | Display success messages and explain invalid input or operations. |

## Planned command examples

These examples illustrate the proposed command syntax; they are not instructions for currently implemented functionality.

### Add a member

```text
add n/Alex Tan p/91234567 e/alex@u.nus.edu a/NUS t/Publicity t/President
```

Name, phone, email, and address will be required. Tags will be optional and repeatable. Each tag must be a single alphanumeric word; tags will be normalised to lowercase and repeated tags stored only once.

New members will have Active status by default. Status is separate from tags and can be changed using `archive` and `activate`.

Contacts with the same phone number will be treated as duplicates. Different members may share the same name.

### Find members

```text
find n/ale
find p/9123
find e/@u.nus.edu
find a/NUS
find t/pub
```

Each command will search one field for a matching substring. Text searches will be case-insensitive. For example, `find t/pub` is intended to match members tagged `publicity`.

### Show all members

```text
list
```

This will clear any search filter and display all stored members.

### Archive or activate a member

```text
archive 2
activate 2
```

Both commands use the member's index in the currently displayed list. Archiving retains the record and shows a `FORMER` badge; activating it shows `ACTIVE` again. Archived members remain visible in the directory.

### Delete a member

```text
delete 2
```

This will permanently remove the member at index `2` in the currently displayed list, including a filtered search result. The MVP will not include a confirmation prompt or undo. To correct a record, the planned MVP workflow is to delete it and add the corrected version.

## Planned data storage

Member records will be stored locally in `data/addressbook.json`. The complete directory will be saved after each successfully executed command and loaded when the application starts. Search filters and displayed indices will not be preserved across sessions.

Saved records include their `active` or `archived` status. Older records without a status load as Active, and the status is written when those records are next saved.

If saving fails, the application will display an error. Changes may remain visible in the current session but may not be retained after restarting.

## MVP scope

The initial scope includes adding, finding, listing, archiving, activating, and deleting member contacts. Roles and committees are represented using tags. New members start Active, while saved or imported Archived records retain their status. Filtering by status is outside the initial scope.

Possible future enhancements include incomplete member records, tag suggestions, combined search conditions, fuzzy matching, sorting, undo, and backups. These are not commitments for the initial version.

## Acknowledgements

NUSocietyDesk builds on AddressBook Level 3 (AB3) as part of the CS2103T team project.
