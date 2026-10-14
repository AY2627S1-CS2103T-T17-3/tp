---
  layout: default.md
  title: "Developer Guide"
  pageNav: 3
---

# AB-3 Developer Guide

<!-- * Table of Contents -->
<page-nav-print />

--------------------------------------------------------------------------------------------------------------------

## **Acknowledgements**

* _{List the sources of reused or adapted ideas, code, documentation, and third-party libraries here, with links to the originals.}_

--------------------------------------------------------------------------------------------------------------------

## **Setting up, getting started**

Refer to the guide [_Setting up and getting started_](SettingUp.md).

--------------------------------------------------------------------------------------------------------------------

## **Design**

### Architecture

<puml src="diagrams/ArchitectureDiagram.puml" width="280" />

The ***Architecture Diagram*** given above explains the high-level design of the App.

The following provides a quick overview of the main components and their interactions.

**Main components of the architecture**

**`Main`** (consisting of classes [`Main`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/Main.java) and [`MainApp`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/MainApp.java)) is in charge of the app launch and shut down.
* At app launch, it initializes the other components in the correct sequence, and connects them up with each other.
* At shut down, it shuts down the other components and invokes cleanup methods where necessary.

The bulk of the app's work is done by the following four components:

* [**`UI`**](#ui-component): The UI of the App.
* [**`Logic`**](#logic-component): The command executor.
* [**`Model`**](#model-component): Holds the data of the App in memory.
* [**`Storage`**](#storage-component): Reads data from, and writes data to, the hard disk.

[**`Commons`**](#common-classes) represents a collection of classes used by multiple other components.

**How the architecture components interact with each other**

The *Sequence Diagram* below shows how the components interact with each other for the scenario where the user issues the command `delete 1`.

<puml src="diagrams/ArchitectureSequenceDiagram.puml" width="574" />

Each of the four main components (also shown in the diagram above),

* defines its *API* in an `interface` with the same name as the Component.
* provides its functionality through a concrete `{Component Name}Manager` class that implements the corresponding API interface.

For example, the `Logic` component defines its API in `Logic.java` and implements it in `LogicManager.java`. Other components interact with a component through its interface rather than its concrete class, preventing them from coupling to that component's implementation, as illustrated in the following partial class diagram.

<puml src="diagrams/ComponentManagers.puml" width="300" />

The sections below give more details of each component.

### UI component

The **API** of this component is specified in [`Ui.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/ui/Ui.java)

<puml src="diagrams/UiClassDiagram.puml" alt="Structure of the UI Component"/>

The UI consists of a `MainWindow` and its parts, such as `CommandBox`, `ResultDisplay`, `PersonListPanel`, and `StatusBarFooter`. All of these, including `MainWindow`, inherit from the abstract `UiPart` class, which captures common behavior among classes that represent visible GUI parts.

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/ui/MainWindow.java).

The `UI` component,

* executes user commands using the `Logic` component.
* listens for changes to `Model` data so that the UI can be updated with the modified data.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
* depends on some classes in the `Model` component because it displays `Person` objects from the model.

### Logic component

**API** : [`Logic.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/logic/Logic.java)

Here's a (partial) class diagram of the `Logic` component:

<puml src="diagrams/LogicClassDiagram.puml" width="550"/>

The sequence diagram below illustrates the interactions within the `Logic` component, taking `execute("delete 1")` API call as an example.

<puml src="diagrams/DeleteSequenceDiagram.puml" alt="Interactions Inside the Logic Component for the `delete 1` Command" />

<box type="info" seamless>

**Note:** The lifeline for `DeleteCommandParser` should end at the destroy marker (X), but due to a limitation of PlantUML, the lifeline continues till the end of diagram.
</box>


How the `Logic` component works:

1. When `Logic` is called upon to execute a command, the command is passed to an `AddressBookParser` object, which in turn creates a parser that matches the command (e.g., `DeleteCommandParser`) and uses it to parse the command.
1. This results in a `Command` object (more precisely, an object of one of its subclasses e.g., `DeleteCommand`) which is executed by the `LogicManager`.
1. The command can communicate with the `Model` when it is executed (e.g. to delete a person).<br>
   Note that although this is shown as a single step in the diagram above for simplicity, the code can require several interactions between the command object and the `Model` to complete the operation.
1. The result of the command execution is encapsulated as a `CommandResult` object which is returned from `Logic`.

Here are the other classes in `Logic` (omitted from the class diagram above) that are used for parsing a user command:

<puml src="diagrams/ParserClasses.puml" width="600"/>

How the parsing works:
* When called upon to parse a user command, the `AddressBookParser` class creates an `XYZCommandParser` (`XYZ` is a placeholder for the specific command name, e.g., `AddCommandParser`). The parser uses the other classes shown above to parse the user command and create an `XYZCommand` object (e.g., `AddCommand`). The `AddressBookParser` returns that object as a `Command` object.
* All `XYZCommandParser` classes, such as `AddCommandParser` and `DeleteCommandParser`, implement the `Parser` interface so they can be treated similarly where appropriate, for example during testing.

### Model component
**API** : [`Model.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/model/Model.java)

<puml src="diagrams/ModelClassDiagram.puml" width="450" />


The `Model` component,

* stores the address book data i.e., all `Person` objects (which are contained in a `UniquePersonList` object).
* stores the `Person` objects selected by the current filter, such as search results, in a separate _filtered_ list. It exposes this list as an unmodifiable `ObservableList<Person>` that the UI can observe and bind to, so the UI updates when the list changes.
* stores a `UserPrefs` object that represents the user’s preferences (currently, just the GUI settings). This is exposed to the outside as a `ReadOnlyUserPrefs` object.
* does not depend on any of the other three components (as the `Model` represents data entities of the domain, they should make sense on their own without depending on other components)

`Tag` validates its input and stores the name in lowercase using `Locale.ROOT`. Equality and hash codes use this
canonical name, so each person's `Set<Tag>` treats case variants as one tag. Command parsing and JSON loading both
construct `Tag` objects and therefore share this identity rule. Saving writes the canonical lowercase names.

`Person` represents name, phone and address as non-null `Optional` containers. Email, status and tags remain
required. The complete-record constructors retain their non-null argument requirements and delegate to the
constructor accepting optional contact details and an explicit status. The corresponding getters return
`Optional<Name>`, `Optional<Phone>` and `Optional<Address>`. The value classes retain their strict validation:
absence is a property of the member, not an empty `Name`, `Phone` or `Address`.

`Email` validates a complete address with the exact domain `u.nus.edu` and stores it in lowercase using `Locale.ROOT`.
It does not trim whitespace; model values and saved strings containing surrounding whitespace are invalid.
Command parsing trims surrounding argument whitespace using the `trim()` behavior.
The existing local-part expression is unchanged: letters and digits may be separated by single `+`, `_`, `.`, or `-`
characters, but punctuation cannot lead, trail, or appear consecutively. Validation uses the normalized whole string;
no new Unicode support or length limit is introduced. Different local-part aliases remain distinct.
The suffix is literal: `@nus.edu`, `@sub.u.nus.edu`, `@u.nus.edu.com`, and `@uXnusXedu` are invalid.
Command parsers and JSON loading reuse this model validation and `Email.MESSAGE_CONSTRAINTS`.

`Person.isSamePerson` compares canonical emails only. Members can share a name or phone number when their emails
differ. `UniquePersonList` uses this identity rule for addition, replacement, and bulk replacement, while
`Person.equals` still compares every field. Add and edit detect email conflicts across the complete directory,
including members outside the displayed filter, and identify the existing member by email and, when present,
name without a displayed index. Re-entering one's own email or changing only its case remains valid; changing an email releases
the previous one for reuse.

`EditCommand` preserves the existing tag set and its descriptor contains only name, phone, email, and address.
`EditCommandParser` still tokenizes `t/` so it can reject tag arguments explicitly instead of including them in another
field's value.

`TagAddCommand` and `TagRemoveCommand` each accept one displayed index and one tag. Their parsers reuse
`ParserUtil.parseIndex`, `ParserUtil.parseTag`, and duplicate-prefix validation to enforce this scope.
Each command resolves the person from `Model.getFilteredPersonList()`, copies their immutable tag set,
and adds or removes the requested `Tag` using the existing case-insensitive identity rule. It then constructs
a replacement `Person` with unchanged contact details and calls `Model.setPerson`.

Adding an existing tag or removing an absent tag succeeds with the normal completion message and leaves the
record unchanged. Removing the final tag is allowed. After success, both commands reset the filter to show all
persons, matching `EditCommand`. `LogicManager` saves the updated address book through the existing storage flow;
no storage schema or UI changes are required.


<box type="info" seamless>

**Note:** The alternative, arguably more object-oriented, design below keeps a unique list of tags in `AddressBook`, and each `Person` references tags from that list. This lets `AddressBook` maintain one `Tag` object per unique tag instead of each `Person` holding its own `Tag` objects.<br>

<puml src="diagrams/BetterModelClassDiagram.puml" width="450" />
</box>


### Storage component

**API** : [`Storage.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/storage/Storage.java)

<puml src="diagrams/StorageClassDiagram.puml" width="550" />

The `Storage` component,
* can save both address book data and user preference data in JSON format, and read them back into corresponding objects.
* is implemented by `StorageManager`, which delegates the actual JSON file access to `JsonAddressBookStorage` and `JsonUserPrefsStorage` (one class per data file).
* depends on some classes in the `Model` component (because the `Storage` component's job is to save/retrieve objects that belong to the `Model`)

The JSON root, property names and email string are unchanged. `JsonAdaptedPerson` writes absent name, phone and
address fields as explicit `null` values. Missing properties and explicit nulls load as empty optionals; supplied
strings still use the existing validators without trimming. Empty or whitespace-only strings are invalid.
Valid complete records still load, and missing/null status still defaults to active. No migration or schema-version
field is introduced. Loading applies the model's email validation
and rejects the entire file on the first invalid record or duplicate canonical email. Record errors use one-based
positions in the saved `persons` array; a duplicate reports both positions, for example
`Record 2: email "alex@u.nus.edu" duplicates record 1.` `JsonAddressBookStorage` also rejects malformed
JSON roots and missing or non-array `persons` values through `DataLoadingException`.

`MainApp.initModelManager` catches `DataLoadingException` and opens the normal UI with an empty address book.
Loading alone leaves the invalid file unchanged. The next successful command, including `list`, saves the
current in-memory directory over that file. Only a missing file starts with sample members. Valid mixed-case
emails become lowercase in memory on load and in the JSON file after a successful command saves.

### Common classes

Classes used by multiple components are in the `seedu.address.commons` package.

--------------------------------------------------------------------------------------------------------------------

## **Implementation**

This section describes some noteworthy details on how certain features are implemented.

### Recording a member with incomplete contact details

`EditCommand` uses the descriptor's supplied values or the member's existing optionals to construct the replacement.
An omitted edit preserves either the value or its absence. Blank edits cannot clear a detail. Tags and status are
preserved. Tag commands also pass the optional contact details and status through unchanged. Add and edit still
check email conflicts throughout the directory; a repeated add never merges records. Successful add, edit and tag
commands retain the existing behavior of showing the full directory.

`Messages.format` renders each absent detail as `Not provided`. Unnamed duplicate errors identify the existing
member by email only. `PersonCard` uses the canonical email as its heading when the name is absent and displays
`Phone: -` or `Address: -` in the existing rows. No new card row, membership state or badge
is introduced. Once supplied, contact values use the existing display style. Name and phone predicates return
false for absent fields. Email search remains available, and display placeholders are never model values.

`JsonAdaptedPerson` maps between optional model values and nullable saved strings. An email-only record can be
represented as `{"name":null,"phone":null,"email":"alex@u.nus.edu","address":null,"status":"active","tags":[]}`.
Reading does not rewrite the file; the next successful command saves explicit nulls. Older versions requiring
complete contact details reject the entire file if it contains an incomplete member. With their existing loading
behavior, they may then overwrite that file after a successful command; users must back up before downgrading.
Automatic saving and save-failure behavior are unchanged: a save failure is reported without rolling back the
in-memory command. CSV import continues to require complete contact details and explicit status.

### Finding members by contact details

`FindCommandParser` accepts unprefixed name keywords or one of the `n/`, `p/` and `e/` search fields.
It normalizes whitespace before tokenizing prefixes, so spaces, tabs and line breaks separate fields consistently.
Duplicate prefixes, multiple selected fields, text before a selected field and empty search values are rejected.
Phone and email searches each accept one substring; multiple whitespace-separated fragments are rejected.

`FindCommand` accepts a `Predicate<Person>` and replaces the model's current filter using
`Model.updateFilteredPersonList`. Every search checks the complete directory. The existing results view displays
the matching members and their count, including an empty list and zero for a valid search with no matches.

`NameContainsKeywordsPredicate` matches any whole name keyword, ignoring case.
`PhoneContainsSubstringPredicate` matches a literal phone substring.
`EmailContainsSubstringPredicate` lowercases the search substring with `Locale.ROOT` and matches it anywhere in
the canonical lowercase email. It does not construct an `Email` or validate the substring as a complete address,
so fragments such as `alice`, `@u.nus.edu` and `+` are accepted. Searching `@u.nus.edu` returns every member
under the existing email domain restriction. No member data or storage schema changes are required.

### Importing member directories from CSV

`ImportCommandParser` parses a relative or absolute local path, optionally enclosed in double quotes. It preserves
backslashes and performs no file I/O. `AddressBookParser` dispatches `import` through this parser.

`CsvMemberReader` uses Apache Commons CSV 1.14.1 to parse UTF-8 input with standard CSV quoting. It accepts an optional
byte order mark and ignores empty lines. The header must be `name,phone,email,address,tags,status` in that order.
The reader parses the entire file, checks column counts, trims field values, and constructs existing model objects to
reuse their validation and canonicalization. Semicolon-separated tags use a `Set<Tag>`. Status is validated through
`Status.isValidStatus` and supplied explicitly to `Person`; it never defaults to active. Record errors include the
logical CSV record number, counting the header as 1. Empty and header-only files are rejected.

`ImportCommand` reads the members and copies the complete existing directory into a temporary `AddressBook`.
It checks each imported member against that copy using the existing canonical-email identity rule before appending
them. This includes archived members, members outside the current filter, and earlier CSV records.
Only after all records pass does it call `Model.setAddressBook` and reset the filter to show the complete directory.
No model or storage-schema changes are needed. Parsing, validation, and duplicate failures throw `CommandException`
before the live directory is changed, so `LogicManager` does not save rejected imports.

Before executing an import, `LogicManager` checks that its source differs from the JSON save file. Normalized paths
and `Files.isSameFile` detect path aliases and links; this prevents subsequent automatic saving from overwriting the
import source. The reader only opens the CSV for reading. Successful imports use existing automatic JSON persistence.
As with other mutating commands, a subsequent save failure is reported but does not roll back the in-memory change.

Tests cover quoted fields, BOM/CRLF input, fixed headers, existing validators, required statuses, duplicate emails,
late CSV errors, source preservation, unchanged filters/files on rejection, successful JSON round trips, source/save
file aliases, and save failures. The first increment intentionally rejects the entire file on any bad record;
partial import with aggregate rejection feedback is a future increment.

### \[Proposed\] Undo/redo feature

#### Proposed Implementation

The proposed undo/redo mechanism is facilitated by `VersionedAddressBook`. It extends `AddressBook` with an undo/redo history, stored internally as an `addressBookStateList` and `currentStatePointer`. Additionally, it implements the following operations:

* `VersionedAddressBook#commit()` -- Saves the current address book state in its history.
* `VersionedAddressBook#undo()` -- Restores the previous address book state from its history.
* `VersionedAddressBook#redo()` -- Restores a previously undone address book state from its history.

These operations are exposed in the `Model` interface as `Model#commitAddressBook()`, `Model#undoAddressBook()` and `Model#redoAddressBook()` respectively.

Given below is an example usage scenario and how the undo/redo mechanism behaves at each step.

Step 1. The user launches the application for the first time. The `VersionedAddressBook` will be initialized with the initial address book state, and the `currentStatePointer` pointing to that single address book state.

<puml src="diagrams/UndoRedoState0.puml" alt="UndoRedoState0" />

Step 2. The user executes `delete 5` command to delete the 5th person in the address book. The `delete` command calls `Model#commitAddressBook()`, causing the modified state of the address book after the `delete 5` command executes to be saved in the `addressBookStateList`, and the `currentStatePointer` is shifted to the newly inserted address book state.

<puml src="diagrams/UndoRedoState1.puml" alt="UndoRedoState1" />

Step 3. The user executes `add n/David …​` to add a new person. The `add` command also calls `Model#commitAddressBook()`, causing another modified address book state to be saved into the `addressBookStateList`.

<puml src="diagrams/UndoRedoState2.puml" alt="UndoRedoState2" />

<box type="info" seamless>

**Note:** If a command fails its execution, it will not call `Model#commitAddressBook()`, so the address book state will not be saved into the `addressBookStateList`.
</box>

Step 4. The user now decides that adding the person was a mistake, and decides to undo that action by executing the `undo` command. The `undo` command will call `Model#undoAddressBook()`, which will shift the `currentStatePointer` once to the left, pointing it to the previous address book state, and restores the address book to that state.

<puml src="diagrams/UndoRedoState3.puml" alt="UndoRedoState3" />


<box type="info" seamless>

**Note:** If the `currentStatePointer` is at index 0, pointing to the initial AddressBook state, then there are no previous AddressBook states to restore. The `undo` command uses `Model#canUndoAddressBook()` to check if this is the case. If so, it will return an error to the user rather
than attempting to perform the undo.
</box>

The following sequence diagram shows how an undo operation goes through the `Logic` component:

<puml src="diagrams/UndoSequenceDiagram-Logic.puml" alt="UndoSequenceDiagram-Logic" />

<box type="info" seamless>

**Note:** The lifeline for `UndoCommand` should end at the destroy marker (X), but due to a limitation of PlantUML, it continues to the end of the diagram.
</box>

Similarly, how an undo operation goes through the `Model` component is shown below:

<puml src="diagrams/UndoSequenceDiagram-Model.puml" alt="UndoSequenceDiagram-Model" />

The `redo` command does the opposite — it calls `Model#redoAddressBook()`, which shifts the `currentStatePointer` once to the right, pointing to the previously undone state, and restores the address book to that state.

<box type="info" seamless>

**Note:** If the `currentStatePointer` is at index `addressBookStateList.size() - 1`, pointing to the latest address book state, then there are no undone AddressBook states to restore. The `redo` command uses `Model#canRedoAddressBook()` to check if this is the case. If so, it will return an error to the user rather than attempting to perform the redo.
</box>

Step 5. The user then decides to execute the command `list`. Commands that do not modify the address book, such as `list`, will usually not call `Model#commitAddressBook()`, `Model#undoAddressBook()` or `Model#redoAddressBook()`. Thus, the `addressBookStateList` remains unchanged.

<puml src="diagrams/UndoRedoState4.puml" alt="UndoRedoState4" />

Step 6. The user executes `clear`, which calls `Model#commitAddressBook()`. Since the `currentStatePointer` is not pointing at the end of the `addressBookStateList`, all address book states after the `currentStatePointer` will be purged. Reason: It no longer makes sense to redo the `add n/David …` command. This is the behavior that most modern desktop applications follow.

<puml src="diagrams/UndoRedoState5.puml" alt="UndoRedoState5" />

The following activity diagram summarizes what happens when a user executes a new command:

<puml src="diagrams/CommitActivityDiagram.puml" width="250" />

#### Design considerations:

**Aspect: How undo & redo execute:**

* **Alternative 1 (current choice):** Saves the entire address book.
  * Pros: Easy to implement.
  * Cons: May have performance issues in terms of memory usage.

* **Alternative 2:** Individual command knows how to undo/redo by
  itself.
  * Pros: Will use less memory (e.g. for `delete`, just save the person being deleted).
  * Cons: We must ensure that the implementation of each individual command is correct.

_{more aspects and alternatives to be added}_

### \[Proposed\] Data archiving

_{Explain here how the data archiving feature will be implemented}_


--------------------------------------------------------------------------------------------------------------------

## **Documentation, logging, testing, dev-ops**

* [Documentation guide](Documentation.md)
* [Testing guide](Testing.md)
* [Logging guide](Logging.md)
* [DevOps guide](DevOps.md)

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Requirements**

### Product scope

**Target user profile**:

Secretary of an NUS student society who

* has to manage the contact details of 50 to 300 active and former members
* currently juggles spreadsheets and chat histories to look up members
* prefers desktop apps over other types of applications
* can type fast, and prefers typing to mouse interactions
* is comfortable using CLI apps

**Value proposition**: Help NUS society secretaries keep member contact details, roles, committees, and membership status accurate and instantly searchable, so routine membership administration takes seconds instead of searching across spreadsheets and chat histories.


### User stories

Priorities: High (must have) - `* * *`, Medium (should have) - `* *`, Low (could have) - `*`

| Priority | As a …                                                                | I want to …                                                                  | So that I can…                                                                                |
|----------|------------------------------------------------------------------------|------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| `* * *`  | secretary with an existing directory                                   | import existing contacts into NUSocietyDesk and review any rejected records  | adopt the product without manual re-entry or risking silent data loss                         |
| `* * *`  | secretary                                                              | attach consistent society-related information to a member                    | organise contacts according to their place in the society                                     |
| `* * *`  | secretary who remembers only part of a member’s information            | search across relevant contact fields                                        | locate the member without knowing their exact name                                            |
| `* * *`  | secretary                                                              | combine multiple search conditions                                           | find members matching specific criteria such as committee, role, and membership status       |
| `* * *`  | secretary                                                              | change selected member information without replacing unrelated details       | make corrections safely                                                                       |
| `* * *`  | secretary                                                              | remove former members from routine results without deleting their records    | keep the active directory relevant                                                            |
| `* * *`  | secretary                                                              | restore a former member to active status                                     | return an incorrectly archived member to routine results                                      |
| `* * *`  | secretary dealing with members who have similar or duplicate names     | distinguish their records using additional information                       | avoid contacting or modifying the wrong member                                                |
| `* * *`  | secretary                                                              | receive precise confirmation and error messages                              | know exactly what changed and how to correct a failed operation                               |
| `* *`    | secretary who has to manage local and international members            | store phone numbers with country codes                                       | contact members who have international phone numbers                                          |
| `* *`    | secretary with incomplete information                                  | record a member using only the available details                             | capture the contact immediately and complete it later                                         |
| `* *`    | secretary who only has a member’s phone number                         | create an incomplete record using that unique phone number                   | save the contact immediately and complete the remaining details later                         |
| `* *`    | secretary processing the same organisational change for several members | update the relevant records together                                         | spend less time on repetitive maintenance                                                     |
| `* *`    | secretary                                                              | detect and resolve possible duplicate member records                         | prevent conflicting records for the same member in the directory                              |
| `* *`    | secretary who made an incorrect change                                 | reverse a recent data-changing operation                                     | prevent a typing mistake from permanently damaging the directory                              |
| `* *`    | secretary reviewing a long list of results                             | order the results predictably                                                | scan them efficiently                                                                          |
| `* *`    | frequent user                                                          | repeat or adapt recent commands                                              | complete recurring work with less typing                                                      |
| `* *`    | returning user                                                         | rediscover relevant command syntax without leaving my current task           | use the product occasionally without having to relearn it                                     |
| `*`      | potential user exploring the product                                   | view representative sample member records                                    | understand the application before using real data                                             |
| `*`      | expert user                                                            | use shorter alternatives for frequent commands                               | enter routine operations faster                                                               |
| `*`      | secretary                                                              | view a concise summary of members by status or committee                     | spot obviously inconsistent records                                                           |

### Use cases

(For all use cases below, the **System** is `NUSocietyDesk` and the **Actor** is the `Secretary`, unless specified otherwise.)

**Use case: UC01 - Delete a member**

**MSS**

1. Secretary requests to list members.
2. NUSocietyDesk shows a list of members.
3. Secretary requests to delete a specific member in the list.
4. NUSocietyDesk deletes the member and confirms the deletion.

   Use case ends.

**Extensions**

* 2a. The list is empty.

  Use case ends.

* 3a. The specified member is not in the list.

  * 3a1. NUSocietyDesk shows an error message.

    Use case resumes at step 2.

**Use case: UC02 - Add a member**

**MSS**

1. Secretary requests to add a member, providing a name, phone number, email address, address, and any tags.
2. NUSocietyDesk adds the member and displays a confirmation with the member's details.

   Use case ends.

**Extensions**

* 1a. Required details are missing, or a provided detail is invalid.

  * 1a1. NUSocietyDesk shows an error message and does not add the member.

    Use case ends.

* 1b. A member record with the same canonical email already exists, regardless of its name or phone number.

  * 1b1. NUSocietyDesk shows a duplicate member record error and does not add the member.

    Use case ends.

**Use case: UC03 - Find and inspect a member**

**MSS**

1. Secretary requests to find members using name keywords, a single phone number substring or a single email substring.
2. NUSocietyDesk displays matching members with their contact details and the number of matches.
3. Secretary inspects the displayed details to identify the intended member.

   Use case ends.

**Extensions**

* 1a. Secretary provides no search value.

  * 1a1. NUSocietyDesk shows an error message with the required format.

    Use case ends.

* 1b. Secretary selects more than one search field or repeats a field selection.

  * 1b1. NUSocietyDesk shows an error message explaining the invalid field selection.

    Use case ends.

* 1c. Secretary provides multiple phone number or email substrings in one search.

  * 1c1. NUSocietyDesk shows an error message explaining that the selected search field accepts only one substring.

    Use case ends.

* 2a. No members match the search.

  * 2a1. NUSocietyDesk displays an empty list and reports zero matches.

    Use case ends.

### Non-Functional Requirements

1. **Performance:** NUSocietyDesk should respond to typical commands within 2 second when managing up to 100 member records.

2. **Capacity:** NUSocietyDesk should support at least 200 active and former member records without noticeable sluggishness during typical usage.

3. **Usability:** A secretary who is familiar with the available commands and has above-average typing speed should be able to complete common tasks—such as adding, updating, and finding a member—faster using commands than with a conventional mouse-driven interface.

4. **Keyboard accessibility:** All core membership-management features should be usable without requiring a mouse.

5. **Reliability:** After a command that modifies member data completes successfully, the updated data should remain available after the application is restarted.

6. **Data integrity:** Invalid commands or member information should be rejected with an informative error message without altering previously stored valid data.

7. **Privacy:** Member information should be stored only on the user's local computer and should not be transmitted to any remote server.

8. **Offline availability:** All core features should remain usable without an Internet connection.

9. **Human-editable storage:** Application data should be stored locally in a human-editable text format, with at least the same level of support for manual file editing as the original AB3 application.

10. **Platform independence:** NUSocietyDesk should work on Windows, Linux, and macOS on a computer with Java 25 installed, without relying on OS-specific features.

11. **Portability:** The application should run without an installer and should be distributed as a single JAR file of no more than 100 MB.

12. **Display compatibility:** The GUI should work well at resolutions of 1920×1080 or higher at 100% and 125% scaling, and remain fully usable at resolutions of 1280×720 or higher at 150% scaling.

### Glossary

* **Active member**: A member whose record is included in routine membership results
* **Archived member record**: A retained former member's record that is excluded from routine membership results without being permanently deleted
* **Committee**: A subgroup of the society to which a member belongs, such as Publicity or Logistics
* **Contact details**: A member's name, phone number, email address, and address
* **Duplicate member record**: A member record with the same email after lowercasing as an existing record. Members may share a name or phone number when their emails differ
* **Former member**: A member who is no longer active in the society but whose record is retained
* **Incomplete member record**: A member record containing only some normally expected details, but enough information to identify the member according to the application's rules
* **Member**: A person whose contact and society-related information is stored in NUSocietyDesk
* **Member directory**: The complete collection of member records stored in NUSocietyDesk
* **Member record**: The information stored for one member, including their contact details and any society-related tags
* **Membership status**: A classification indicating a member's current relationship with the society, such as active or former
* **Rejected record**: A record that is not imported because it contains invalid, missing, or conflicting information
* **Role**: A position or responsibility held by a member in the society, such as President or Treasurer
* **Tag**: A label attached to a member record to represent information such as a committee, role, or membership status

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Instructions for manual testing**

Given below are instructions to test the app manually.

<box type="info" seamless>

**Note:** These instructions only provide a starting point for testers to work on;
testers are expected to do more *exploratory* testing.
</box>

### Launch and shutdown

1. Initial launch

   1. Download the JAR file and copy it into an empty folder.

   1. Double-click the JAR file.<br>
      Expected: The GUI opens with a set of sample contacts. The window size may not be optimal.

1. Saving window preferences

   1. Resize the window to an optimal size. Move the window to a different location. Close the window.

   1. Relaunch the app by double-clicking the JAR file.<br>
       Expected: The most recent window size and location are retained.

1. _{ more test cases … }_

### Importing member directories

Use a disposable application folder. Copy `docs/samples/members.csv` into it as `member contacts.csv`.
Keep a separate copy of this source file for comparison.

1. Test case: `import "member contacts.csv"`<br>
   Expected: Two members are appended after existing members, all members are displayed, and feedback states
   `Imported 2 member(s)`. Alex Tan has `active` status and the `exco`/`publicity` tags; Jamie Lee has `archived`
   status, displays `FORMER`, and has no tags. The CSV is unchanged. Restart and confirm these records persist.

1. Test case: `find Existing`, followed by importing the same CSV again.<br>
   Expected: The import is rejected as a duplicate email. Member records, search results, and saved JSON are unchanged.

1. In a fresh copy of the CSV, change the second member's status to `former`. Run the import in a fresh disposable
   directory, or first remove the previously imported example members.<br>
   Expected: A status validation error identifies CSV record 3. Neither member is imported. The original directory,
   current search results, saved JSON, and source CSV remain unchanged.

1. Repeat with a non-NUS email, missing status, a duplicate case-variant email, missing/extra columns, a reordered
   header, a header-only file, and an unterminated quoted cell after a valid record.<br>
   Expected: Each import is rejected with a useful reason and does not import a valid prefix of the file.

1. Test cases: `import`, `import ""`, `import member contacts.csv`, and `import missing.csv`.<br>
   Expected: Format errors explain the command syntax; a missing file gives a file-reading error.
   Member records, current search results, and saved JSON remain unchanged.

These GUI checks are documented for manual execution; automated tests exercise the command and persistence paths.

### Finding members by email

1. Search using the sample members from a fresh launch with no data file.

   1. Test case: `find e/ALEX`<br>
      Expected: Alex Yeoh is displayed and the result count is one.

   1. Test case: `find e/ALExYeOh@U.NUS.EDU`<br>
      Expected: Alex Yeoh is displayed and the result count is one.

   1. Test case: `find e/@u.nus.edu`<br>
      Expected: All sample members are displayed and the count equals the directory size.

   1. Test case: `find e/no-such-email`<br>
      Expected: The list is empty and the result count is zero.

1. Reject invalid email searches after running `find e/ALEX`.

   1. Test cases: `find e/`, `find e/alex david`, `find e/alex e/david`, `find e/alex n/Alex`,
      `find e/alex p/9103` and `find Alex e/alex`<br>
      Expected: An informative error is displayed. Alex Yeoh remains displayed and member records are unchanged.

1. Search the complete directory after a previous search.

   1. Test case: `find n/David`, followed by `find e/ALEX`<br>
      Expected: The email search displays Alex Yeoh even though he was absent from the preceding results.

### Deleting a person

1. Deleting a person while all persons are being shown

   1. Prerequisites: List all persons using the `list` command, with multiple persons in the list.

   1. Test case: `delete 1`<br>
      Expected: The first contact is deleted from the list. The status message shows the deleted contact's details.

   1. Test case: `delete 0`<br>
      Expected: No person is deleted. The status message shows error details.

   1. Other incorrect delete commands to try: `delete`, `delete x`, `...` (where x is larger than the list size)<br>
      Expected: Similar to previous.

1. _{ more test cases … }_

### Tag identity and editing

1. Add a person with case variants of a tag.

   1. Test case: `add n/Tag Test p/91234567 e/tagtest@u.nus.edu a/Kent Ridge t/Publicity t/publicity t/Exco`<br>
      Expected: The card displays exactly two tags, `exco` and `publicity`, in lowercase.

   1. Run `list` and note the displayed index of Tag Test. In the following commands, replace `INDEX` with that index.

   1. Test case: `edit INDEX p/98765432`<br>
      Expected: The phone number changes and both tags remain.

   1. Test cases: `edit INDEX t/Publicity`, `edit INDEX t/`, and `edit INDEX a/New Address t/Publicity`<br>
      Expected: Each command is rejected with guidance to use `tagadd` or `tagremove`. All person details remain
      unchanged.

1. Add and remove individual tags using Tag Test from the previous steps.

   1. Test case: `tagadd INDEX t/Volunteer`<br>
      Expected: The member has `exco`, `publicity`, and `volunteer`; contact details and other members remain unchanged.

   1. Test case: `tagadd INDEX t/VOLUNTEER`<br>
      Expected: Success with the same three tags and no duplicate.

   1. Test case: `tagremove INDEX t/PUBLICITY`<br>
      Expected: Only `exco` and `volunteer` remain.

   1. Repeat `tagremove INDEX t/publicity`.<br>
      Expected: Success with the same two tags.

   1. Close and reopen the app.<br>
      Expected: The member still has only `exco` and `volunteer`.

   1. Remove the remaining tags with `tagremove INDEX t/Exco` and `tagremove INDEX t/Volunteer`.<br>
      Expected: The member has no tags and retains all contact details.

1. Check displayed indices and invalid input.

   1. Run `find Tag`, note Tag Test's displayed index, and run `tagadd INDEX t/Volunteer` using that index.<br>
      Expected: Only Tag Test gains the tag, and all members are displayed afterward.

   1. Run `find Tag` again and run `tagremove INDEX t/Volunteer` using the new displayed index.<br>
      Expected: Only Tag Test loses the tag, and all members are displayed afterward.

   1. For each of `tagadd` and `tagremove`, try arguments `0 t/Volunteer`, `1 2 t/Volunteer`, `1`, `1 t/`,
      `1 t/Publicity Team`, `1 t/Volunteer t/Exco`, and an index larger than the displayed list.<br>
      Expected: Each command is rejected; records and the current filter remain unchanged.

1. Load tags from an existing saved record.

   1. Close the app in a disposable test directory. Set Tag Test's saved tags to `["Publicity", "publicity", "Exco"]`.

   1. Relaunch the app.<br>
      Expected: The card displays only `exco` and `publicity`.

   1. Execute `list` and inspect the saved record.<br>
      Expected: It contains exactly two tag names, `exco` and `publicity`, regardless of their order in the file.

### Saving data

1. Dealing with invalid data files

   1. In a disposable test directory, run `list` to create `data/addressbook.json`, close the app, and back it up.
   1. Give two saved records the emails `Alex@U.NUS.EDU` and `alex@u.nus.edu`, then relaunch.<br>
      Expected: the normal window opens with an empty member list. Loading leaves the invalid file unchanged.
      Storage reports that record 2 duplicates record 1, but the UI does not display those record positions.
   1. Correct the second email and relaunch.<br>
      Expected: the records load with lowercase emails. The file remains unchanged until a successful command,
      such as `list`, saves it.
   1. Repeat with malformed JSON, a null record, or an email with surrounding whitespace.<br>
      Expected: each file opens an empty member list. Restore the backup before running commands, which could
      overwrite the invalid file. A missing file starts with the sample members instead.

### Email domain validation

1. Add `add n/Nus Test p/91234567 e/Nus.Test@U.NUS.EDU a/Kent Ridge`.<br>
   Expected: success with `nus.test@u.nus.edu` in the result and card. Restart after saving to confirm persistence.
1. Using its displayed index, try edits with `e/nus.test`, `e/nus.test@gmail.com`, `e/nus.test@nus.edu`,
   `e/nus.test@sub.u.nus.edu`, `e/nus.test@u.nus.edu.com`, and `e/nus..test@u.nus.edu`.<br>
   Expected: each shows the shared email-constraints message. Records, filter, and saved file stay unchanged.
1. Try the same invalid emails in otherwise valid `add` commands.<br>
   Expected: the same rejection without adding members.
1. Add a different member with `e/nus.test+society@u.nus.edu`.<br>
   Expected: success because aliases remain distinct. New domains are never appended or substituted automatically.

### Email identity

1. On a fresh launch with no data file, run `find Morgan`.<br>
   Expected: both sample members named Morgan Lee appear. They share a phone number but have different emails.
1. In a disposable directory with `data/addressbook.json` containing `{"persons":[]}`, run:
   * `add n/Alex Tan p/91234567 e/Alex.One@U.NUS.EDU a/Kent Ridge`
   * `add n/Alex Tan p/91234567 e/alex.two@u.nus.edu a/Clementi`
   Expected: both succeed and show lowercase emails.
1. Run `edit 2 e/ALEX.ONE@U.NUS.EDU`.<br>
   Expected: `Email "alex.one@u.nus.edu" is already used by "Alex Tan". No changes were made.`
1. Run `edit 1 n/Alexander Tan e/ALEX.ONE@U.NUS.EDU`, then `find Alex`, then
   `edit 1 e/alex.one@u.nus.edu`.<br>
   Expected: the rename succeeds; `find Alex` shows only Alex Tan. The final edit fails because that email
   belongs to Alexander Tan outside the displayed filter.
1. Run `list`, then `edit 1 e/alex.new@u.nus.edu`, then
   `add n/Alex Tan p/91234567 e/alex.one@u.nus.edu a/Bukit Timah`.<br>
   Expected: both commands succeed. Restart to confirm the saved email changes.
