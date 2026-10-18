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
Valid complete records still load. `JsonAdaptedPerson` writes a lowercase status; `active` and `archived` load
unchanged, while a missing or null status defaults to active. No migration or schema-version field is introduced.
`JsonAddressBookStorage.loadAddressBook` returns an `AddressBookLoadResult` with a status (`SUCCESS`, `PARTIAL`,
`FAILURE`, or `MISSING`), recovered records, and errors. Each record is deserialized and validated independently.
Invalid records and later duplicate canonical emails are skipped. Error positions refer to the original
one-based positions in the saved `persons` array, including skipped records; a duplicate reports both positions,
for example `Record 3: email "alex@u.nus.edu" duplicates record 2.` Malformed JSON, trailing content, invalid
roots, missing or non-array `persons` values, and read failures produce a failed load with no recovered records.
The existing `readAddressBook` API remains strict and throws `DataLoadingException` on any loading error.

`MainApp.initModelManager` uses recovered records and prepares a warning for `UiManager` to display after the
main window opens. Any partial or failed load disables saving through that storage instance for the rest of the
session, including direct calls to either save overload. `LogicManager` rejects commands that change records
before execution, while `list`, `find`, `help`, and `exit` run without saving. New commands require write access
by default and must explicitly override `Command.isReadOnly` to run during recovery. Repairing the file requires
a restart to restore saving. Only a missing file starts with sample members; a valid empty file stays empty.
Valid mixed-case emails become lowercase in memory on load and in the JSON file after a normal successful save.

### Common classes

Classes used by multiple components are in the `seedu.address.commons` package.

--------------------------------------------------------------------------------------------------------------------

## **Implementation**

This section describes some noteworthy details on how certain features are implemented.

### Recalling submitted commands

`CommandBox` owns one JavaFX-independent `CommandHistory`. A key-pressed filter on its text field consumes
plain Up/Down events, including boundary no-ops, before JavaFX moves the caret. Modified arrows and other
keys are not intercepted. The existing `CommandExecutor` remains the only path to execution; navigation
never invokes it, updates the result display, or accesses `Logic`, `Model` or `Storage`.

The helper uses two `Deque<String>` collections backed by `ArrayDeque`: `beforeCursor` holds older entries
and `afterCursor` holds the selected entry followed by newer entries. Both are ordered oldest first;
their concatenation is the complete retained history. An empty `afterCursor` denotes the draft position.
Up moves the last older entry to the front of `afterCursor`; Down moves its first entry back to the end
of `beforeCursor`. Each arrow transition uses constant-time deque end operations.

The first actual Up transition captures an immutable `InputState` containing exact draft text, selection
anchor and caret. Down past the newest entry returns that snapshot and clears it. Recalled entries place
the caret at the end. Before moving in either direction, the helper saves the selected entry's edited text
in `editedCommands`, keyed by its chronological index (`beforeCursor.size()`). These indices remain stable
until submission. Using indices keeps separate occurrences of the same command independently editable.
The map contains at most one temporary string per retained entry, including empty edits. Reverting an entry
to its submitted text removes its temporary edit. Returning to the draft does not clear edits, so browsing
again restores them. Stored submitted strings and duplicate detection remain unchanged.
A returned empty optional
denotes a no-op; a present result always restores text/selection and explicitly removes error styling,
even when the replacement text is identical. Boundary no-ops preserve edits, selection and styling.

`record` ignores `String.isBlank()` input without changing any state and returns false to prevent execution.
For every other submission it resets the cursor, clears all temporary edits and the saved draft, suppresses
an exact consecutive duplicate, and otherwise appends the raw string. `MAX_HISTORY_SIZE` is 100 across both deques. At capacity,
the oldest entry is removed before appending. Blank inputs and suppressed duplicates do not evict entries;
the draft is separate. Resetting the cursor moves at most 100 entries. No evicted entry is retained elsewhere.
Recording precedes the executor call, so parser, execution, import and save failures are recallable.
Duplicates still execute. Success clears the field; failure retains the input and existing error styling.
After a parser or command exception, `CommandBox` calls `selectLatestAfterFailure` to select the newest entry
without replacing the field or its selection. The first Up therefore reaches the preceding entry, while
Down can return to the failure. If no preceding entry exists, Up is a boundary no-op preserving error styling.
This positioning leaves `draft` null, so there is no duplicate failed-input draft beyond the newest entry.
Down is a boundary no-op when only the newest entry remains in `afterCursor` and there is no saved draft.
Repeated Down presses preserve edits and selection; the next Up still recalls the preceding entry.
Normal browsing captures and restores a real unsent draft as before. Temporary edits survive navigation.

History is local to the command box's lifetime. Focus changes, Help, minimizing and ordinary commands do not
reset it; a new application instance starts empty. It is never serialized or reconstructed from logs.
The existing command syntax, direct parser behavior, data schemas, recovery/save behavior and execution logs
are unchanged. Recalled indices are interpreted against the current displayed list when submitted.

#### History diagnostics and assertions

`CommandHistory` obtains its logger through `LogsCenter`. It emits `FINE` messages for recording, duplicate
suppression, ignored blanks, failure selection, navigation, draft restoration and boundary no-ops. Messages contain counts,
direction and an eviction flag only, never command or draft contents. Examples include
`Command history recorded: size=100, evictedOldest=true.` and
`Command history navigated older: before=99, after=1.` Restoration logs only
`Command history draft restored: size=100.` rather than an additional newer-navigation event.
These normal operations are not warnings. Lazy message suppliers avoid building diagnostic strings when
`FINE` is disabled. Existing logging destinations and default levels are unchanged; enabling `FINE` in the
existing logging configuration exposes the diagnostics.

Java assertions check stable operation boundaries, including no-op returns: non-null deques, combined capacity,
nonblank entries, no adjacent exact duplicates across the complete history, and any saved draft requiring
an active browsing position. Browsing after failure has no saved draft. Draft assertions validate text and
selection bounds; pending-edit assertions check that at most one
non-null edit exists per retained index. Operation assertions check removal prerequisites,
unchanged navigation size, the expected recording size, and cursor/draft reset after submission.
Messages identify invariant failures without including input text. Assertion expressions and invariant helpers
have no side effects; required mutations and all user-input decisions use ordinary control flow. `AssertionError`
is not converted to command feedback. A separate test task verifies behavior with assertions disabled.

JUnit tests cover recording, capacity/eviction (including submission while browsing), draft selection,
retained edits in both directions and across draft visits, independent edits to repeated commands, empty edits,
and clearing edits after submission without changing original submitted history,
duplicate and whitespace rules, failure recall, key modifiers and event consumption, identical-text styling,
and metadata-only logs. GUI tests share one JavaFX toolkit per worker and require a display. See the
[testing guide](Testing.md) for the assertions-disabled check and the manual history scenarios below.

### Archiving and activating members

`archive INDEX` and `activate INDEX` use the index in `Model.getFilteredPersonList`, so they work after a search.
Each command rejects an invalid index or a member already in the requested status. `Person.withStatus` creates a
replacement that retains the member's optional contact details, email and tags. `Model.setPerson` replaces the
record in the directory; its email identity remains unchanged. A successful command resets the displayed list to
the complete directory. Archived members remain visible and searchable, so they can be selected for `activate`.
`LogicManager` saves the new status through the existing JSON storage after each successful command.

### Recording a member with incomplete contact details

`add e/EMAIL [n/NAME] [p/PHONE_NUMBER] [a/ADDRESS] [t/TAG]...` requires only email. All eight combinations
of the other contact fields are accepted. `AddCommandParser` checks for the email prefix and a valid preamble,
rejects repeated contact prefixes, then validates supplied name, supplied phone, email, supplied address and tags
in that order. Omitted optional prefixes become empty optionals; supplied blank values still fail validation.
Existing tokenization is unchanged: recognized prefixes need an ordinary preceding space, and unknown prefix-like
text may become part of a field value. New members have active status and may have tags.

`EditCommand` uses the descriptor's supplied values or the member's existing optionals to construct the replacement.
An omitted edit preserves either the value or its absence. Blank edits cannot clear a detail. Tags and status are
preserved. Tag commands also pass the optional contact details and status through unchanged. Add and edit still
check email conflicts throughout the directory; a repeated add never merges records. Successful add, edit and tag
commands retain the existing behavior of showing the full directory.

Add, edit, tag and delete commands use `Messages.formatProvidedDetails` to omit absent contact fields and empty tags from their success messages.
The formatter label a supplied name as `Name: <person name>` consistently across command confirmations.
Unnamed duplicate errors identify the existing
member by email only. `PersonCard` uses the canonical email as its heading when the name is absent and displays
`Phone: -` or `Address: -` in the existing rows. No new card row, membership state or badge
is introduced. Once supplied, contact values use the existing display style. Name and phone predicates return
false for absent fields. Email search remains available, and display placeholders are never model values.

`JsonAdaptedPerson` maps between optional model values and nullable saved strings. An email-only record can be
represented as `{"name":null,"phone":null,"email":"alex@u.nus.edu","address":null,"status":"active","tags":[]}`.
Reading does not rewrite the file; the next successful command saves explicit nulls. Older versions requiring
complete contact details reject the entire file if it contains an incomplete member. With their existing loading
behavior, they may then overwrite that file after a successful command; users must back up before downgrading.

Automatic saving uses the recovery behavior described under "Saving member data safely" below.
CSV import also accepts absent name, phone and address details, but requires explicit status.

Tests cover optional-field combinations, unchanged validation, explicit null persistence, legacy loading,
incremental completion across reloads, filtered edits, unnamed duplicates, email reuse, searches, tags and deletion.
The import regression checks supplied-field validation and that imported emails conflict with existing
incomplete members. `PersonBuilder` supports absent fields and preserves status when copying a member;
`PersonUtil` omits absent prefixes when generating commands.

### Finding members by contact details or tag

`FindCommandParser` accepts unprefixed name keywords or one of the `n/`, `p/`, `e/` and `t/` search fields.
It validates prefix-like tokens at argument boundaries against the four supported prefixes before they can be
interpreted as search values. A nonempty label followed by `/` at the start of the arguments or after whitespace
is treated as a prefix. Uppercase variants (`N/`, `P/`, `E/` and `T/`) receive lowercase-prefix guidance;
other prefixes such as `G/`, `g/`, `a/` and `name/` receive an unsupported-prefix error naming the invalid prefix.
Slashes within values attached to valid prefixes, such as `e/G/`, retain their literal meaning.
Search values retain their field-specific case rules.
It normalizes whitespace before tokenizing prefixes, so spaces, tabs and line breaks separate fields consistently.
Duplicate prefixes, multiple selected fields, text before a selected field and empty search values are rejected.
Phone and email searches each accept one substring; multiple whitespace-separated fragments are rejected.
Tag searches accept one alphanumeric tag; multiple whitespace-separated tags are rejected with a single-tag error.

`FindCommand` accepts a `Predicate<Person>` and replaces the model's current filter using
`Model.updateFilteredPersonList`. Every search checks the complete directory. The existing results view displays
the matching members and their count, including an empty list and zero for a valid search with no matches.
After applying the filter, `FindCommand` logs the match count at `INFO` level through `LogsCenter`, for example,
`Find completed: 3 members matched.`

`NameContainsKeywordsPredicate` matches any whole name keyword, ignoring case.
`PhoneContainsSubstringPredicate` matches a literal phone substring.
`EmailContainsSubstringPredicate` lowercases the search substring with `Locale.ROOT` and matches it anywhere in
the canonical lowercase email. It does not construct an `Email` or validate the substring as a complete address,
so fragments such as `alice`, `@u.nus.edu` and `+` are accepted. Searching `@u.nus.edu` returns every member
under the existing email domain restriction.

`TagMatchesPredicate` stores a `Tag` validated and normalized through `ParserUtil.parseTag` and checks
`Person.getTags().contains(tag)`. This reuses the existing exact, case-insensitive tag identity without modifying
stored tags or their equality behavior. A member with several tags matches if one equals the search tag;
a member with no tags does not match. Contact details and archive status do not affect tag matching.
No member data or storage schema changes are required.

Parser, predicate and command tests cover exact matching, mixed case, partial names, multiple or absent member tags,
invalid input and searches after an earlier filter. Logic integration tests verify that rejected searches preserve
the previous results, member records and saved data. Multiple-tag and multi-field searches remain unsupported.

### Importing member directories from CSV

`ImportCommandParser` parses a relative or absolute local path, optionally enclosed in double quotes. It preserves
backslashes and performs no file I/O. `AddressBookParser` dispatches `import` through this parser.

`CsvMemberReader` uses Apache Commons CSV 1.14.1 to parse UTF-8 input with standard CSV quoting. It accepts an optional
byte order mark and ignores empty lines. The header must be `name,phone,email,address,tags,status` in that order.
The reader parses the entire file before returning any outcomes, then checks column counts, trims field values,
and constructs existing model objects to reuse their validation and canonicalization. Blank names, phones and
addresses become empty optionals; supplied values are validated normally. Email remains required.
Semicolon-separated tags use a `Set<Tag>`. Status is validated through
`Status.isValidStatus` and supplied explicitly to `Person`; it never defaults to active. Record errors include the
logical CSV record number, counting the header as 1. Each `CsvMemberRecord` retains its original number and either
a validated `Person` or its first validation failure. The immutable result list retains valid and rejected records
in source order, so later duplicate errors use original numbers even after invalid or multiline records.
Empty and header-only files are rejected. Invalid headers, malformed CSV, decoding errors and read failures remain
fatal; no outcomes are applied to the live directory if the file cannot be parsed completely.

`ImportCommand` reads the members and copies the complete existing directory into a temporary `AddressBook`.
It collects validation rejections and checks each valid member against that copy using the existing canonical-email
identity rule. Conflicting records are skipped without updating existing members. Duplicate checks include archived
members, members outside the current filter, and earlier accepted CSV records. Invalid earlier records do not reserve
an email; the first valid occurrence wins. Rejection feedback remains in CSV order.
If at least one record is accepted, it calls `Model.setAddressBook`, resets the filter to show the complete directory,
and returns the imported/rejected counts with every rejected record's number and reason. If all records are rejected,
it throws `CommandException` with zero imports and the rejection details before changing the model; this also prevents
`LogicManager` from rewriting JSON. Fatal file errors likewise throw before model changes.
No model or storage-schema changes are needed. Logs record preparation counts and fatal file failures without
including member fields or rejection details; the preparation log does not claim the later JSON save succeeded.

Before executing an import, `LogicManager` checks that its source differs from the JSON save file. Normalized paths
and `Files.isSameFile` detect path aliases and links; this prevents subsequent automatic saving from overwriting the
import source. The reader only opens the CSV for reading. Successful imports use existing automatic JSON persistence.
As with other commands, a subsequent save failure restores the previous member records and search filter.

Tests cover quoted fields, BOM/CRLF input, fixed headers, optional-field combinations, existing validators, required
statuses, duplicate emails, original record numbers, and late fatal CSV errors. Integration tests verify partial
imports, incomplete-member JSON round trips, source preservation, zero saves when all records are rejected,
source/save file aliases, and save failures.

### Saving member data safely

`LogicManager` copies the directory and captures `Model.getFilteredPersonListPredicate()` before executing a command.
If the subsequent save throws `IOException` (including `AccessDeniedException`) or `SecurityException`, it restores
the directory and the actual predicate, reports that the command was not saved, and logs the failure type and recovery.
Restoring the predicate preserves search behavior for future changes, including searches with no matches.
The command's success result is returned only after saving succeeds. Command logs identify the command class rather
than including raw input or member details.

`JsonAddressBookStorage` serializes the complete directory before calling the package-private `AtomicFileWriter`.
The writer creates a temporary file in the destination's directory, writes and closes its UTF-8 contents, and uses
`Files.move` with `ATOMIC_MOVE` and `REPLACE_EXISTING`. Existing symbolic links are followed so the configured link
is retained. There is no non-atomic fallback: unsupported atomic replacement is a save failure. No fallible cleanup
is performed after successful replacement. Failed writes or moves remove the temporary file where possible; a cleanup
failure is attached to the original exception and logged. A failed first save leaves the destination absent.
This protects against ordinary write/replacement errors; it does not promise recovery from power loss or concurrent
edits by another application. User-preference saving is unchanged.

The startup recovery guard remains in effect: partial or failed loads disable member changes and saves before
execution. This behavior is separate from restoring a command after a normal-session save failure.

Tests inject partial writes and replacement failures, check that the original JSON remains byte-for-byte unchanged,
and verify restoration across add, edit, delete, clear, status, tag and import commands. They also cover permissions,
unsupported atomic moves, a failed first save, continued predicate behavior, retry/reload, and startup protection.

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
| `* * *`  | secretary                                                              | mark former members as archived without deleting their records              | retain their contact history                                                                   |
| `* * *`  | secretary                                                              | activate a member who was archived                                           | correct an incorrect archive or record a returning member                                     |
| `* * *`  | secretary dealing with members who have similar or duplicate names     | distinguish their records using additional information                       | avoid contacting or modifying the wrong member                                                |
| `* * *`  | secretary                                                              | receive precise confirmation and error messages                              | know exactly what changed and how to correct a failed operation                               |
| `* *`    | secretary who has to manage local and international members            | store phone numbers with country codes                                       | contact members who have international phone numbers                                          |
| `* *`    | secretary with incomplete information                                  | record a member using a NUS email and any other available details             | capture the contact immediately and complete it later                                         |
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

1. Secretary requests to add a member, providing a NUS email address and any known name, phone, address or tags.
2. NUSocietyDesk adds the member and displays a confirmation with the member's details.

   Use case ends.

**Extensions**

* 1a. Email is missing, or a provided detail is blank or invalid.

  * 1a1. NUSocietyDesk shows an error message and does not add the member.

    Use case ends.

* 1b. A member record with the same canonical email already exists, regardless of its name or phone number.

  * 1b1. NUSocietyDesk shows a duplicate member record error and does not add the member.

    Use case ends.

**Use case: UC03 - Find and inspect a member**

**MSS**

1. Secretary requests to find members using name keywords, a single phone number substring, a single email substring
   or a single tag.
2. NUSocietyDesk displays matching members with their available contact details, tags and the number of matches.
3. Secretary inspects the displayed details to identify the intended member.

   Use case ends.

**Extensions**

* 1a. Secretary provides no search value.

  * 1a1. NUSocietyDesk shows an error message with the required format.

    Use case ends.

* 1b. Secretary selects more than one search field or repeats a field selection.

  * 1b1. NUSocietyDesk shows an error message explaining the invalid field selection.

    Use case ends.

* 1c. Secretary provides multiple phone number substrings, email substrings or tags in one search.

  * 1c1. NUSocietyDesk shows an error message explaining that the selected search field accepts only one value.

    Use case ends.

* 1d. Secretary provides an invalid tag name or uses an invalid search format.

  * 1d1. NUSocietyDesk shows an error message explaining how to correct the search.

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

* **Active member**: A member whose record has active membership status
* **Archived member record**: A retained record with archived status; it remains visible in list and search results
* **Committee**: A subgroup of the society to which a member belongs, such as Publicity or Logistics
* **Contact details**: A member's name, phone number, email address, and address
* **Duplicate member record**: A member record with the same email after lowercasing as an existing record. Members may share a name or phone number when their emails differ
* **Former member**: A member who is no longer active in the society but whose record is retained
* **Incomplete member record**: A member record containing only some normally expected details, but enough information to identify the member according to the application's rules
* **Member**: A person whose contact and society-related information is stored in NUSocietyDesk
* **Member directory**: The complete collection of member records stored in NUSocietyDesk
* **Member record**: The information stored for one member, including their contact details and any society-related tags
* **Membership status**: A classification indicating a member's current relationship with the society, such as active or former
* **Rejected record**: A record that is not imported because it contains an invalid supplied value,
  is missing a required field, or has a duplicate email. Missing optional contact details alone do not cause rejection
* **Role**: A position or responsibility held by a member in the society, such as President or Treasurer
* **Tag**: A label attached to a member record to represent information such as a committee or role

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

### Recalling commands and restoring drafts

Use a disposable application folder for these checks, particularly `clear` and save-failure tests.

1. On a fresh launch, type a draft, select part of it, and press Up/Down.<br>
   Expected: Empty history leaves text, caret and selection unchanged.
1. Submit `list`, then `lisst`.<br>
   Expected: `list` succeeds and clears the box; `lisst` shows `Unknown command.` and remains with error styling.
   Press Up once: `list` is recalled with normal styling, while the error message remains in the result pane.
   Press Down to return to `lisst`, correct it to `list` and submit; the result is `Listed all persons.`.
   Repeat the invalid submission consecutively: one Up still reaches the preceding entry.
   Edit the failed input, press Down several times, then Up once: Down preserves the edit and selection,
   and Up recalls the preceding entry. Down returns to the retained edit without executing it.
   On a fresh launch with only one failed submission, Up leaves the text, selection and error styling unchanged.
1. Type `find n/Alex`, select text from right to left, and browse backward and forward.<br>
   Expected: Recalled entries put the caret at the end; Down past the newest restores the exact draft and selection.
   Edit two recalled entries and navigate away and back, including a visit to the draft: each edit returns.
   Deleting a recalled entry's text also survives navigation. Submitting edited text records it as a new command
   and clears other temporary edits; earlier submitted entries remain unchanged.
   Up at the oldest and Down at the draft
   preserve text, selection and styling. Held arrows repeat but never wrap or execute commands.
1. Submit the newest command again, then submit spaces only, then repeat that command.<br>
   Expected: Nonblank submissions still execute but occupy one consecutive entry. Spaces alone do nothing.
   Repeat after browsing to an older entry: submission resets browsing and discards the old draft.
1. Submit distinct harmless inputs `list 1` through `list 101` (extra list arguments are ignored).<br>
   Expected: Only `list 2` through `list 101` remain. Repeating `list 101` does not evict `list 2`.
1. Check Shift/Ctrl/Alt/Meta arrows and arrow keys in other controls; press F1, minimize/restore, and reopen Help.<br>
   Expected: Existing shortcuts/control behavior remain; history and drafts survive focus/window changes.
   Navigation alone never changes result feedback. `clear` clears members but leaves prior history recallable.
1. Use the failed-save setup described below and submit a command that cannot be saved.<br>
   Expected: Existing recovery feedback and behavior remain; the submitted command is still recallable and editable.
1. Launch a separate instance from another disposable folder, then restart the original instance.<br>
   Expected: Each new instance has empty history; no history file appears and no entries are loaded from logs.

These physical-keyboard and application-lifetime checks require manual execution; automated tests cover
history transitions and JavaFX event handling without driving a native keyboard.

### Recording and completing an email-only member

Use a disposable application folder. Before launching, create `data/addressbook.json` containing
`{"persons":[]}`. A missing file would load sample members instead.

1. Run `add e/Alex@U.NUS.EDU`.<br>
   Expected: One active member is added with heading `alex@u.nus.edu`, missing phone/address placeholders,
   and feedback `New person added: Email: alex@u.nus.edu`.
1. Close and reopen the app before filling any details.<br>
   Expected: The same incomplete member loads; saved name, phone and address values are null.
1. Run `find e/alex`, then `edit 1 n/Alex Tan`. Close and reopen again.<br>
   Expected: Alex Tan is the heading; phone and address remain absent.
1. Run `edit 1 p/91234567`, then `edit 1 a/Kent Ridge`, and restart.<br>
   Expected: Exactly one complete member remains with all supplied values.
1. Repeat in a populated directory, searching for the target before each edit and checking its displayed index.<br>
   Expected: Only the intended member changes. Each successful edit resets the view to the full directory.
1. Try `add e/new@u.nus.edu p/`, `edit 1 n/`, and a duplicate `add e/ALEX@U.NUS.EDU`.<br>
   Expected: Each is rejected without changing records, filters or saved data.
1. Add `add e/unnamed@u.nus.edu t/Publicity`. Search by its email, add/remove a tag, and delete it.<br>
   Expected: Missing details remain absent through tag changes; command feedback never shows Java optional wrappers.
1. Inspect named and unnamed cards at the supported resolutions and scaling settings.<br>
   Expected: Existing rows remain readable and the heading changes correctly after a name is supplied.

### Importing member directories

Use a disposable application folder. Copy `docs/samples/members.csv` into it as `member contacts.csv`.
Keep a separate copy of this source file for comparison. Start with neither sample email in the directory.
For independent cases below, use a fresh disposable directory or remove members added by earlier imports.
CSV record numbers count the header as 1 and the first member as 2; empty lines do not count.

1. Test case: `import "member contacts.csv"`<br>
   Expected: Two members are appended after existing members, all members are displayed, and feedback states
   `Imported 2 member(s); rejected 0 record(s) from member contacts.csv.`
   Alex Tan has `active` status and the `exco`/`publicity` tags; Jamie Lee has `archived`
   status, displays `ARCHIVED`, and has no tags. The CSV is unchanged. Restart and confirm these records persist.

1. Test case: `find Existing`, followed by importing the same CSV again.<br>
   Expected: Feedback reports zero imports, two rejections, and a duplicate-email reason for records 2 and 3.
   Member records, search results, and saved JSON are unchanged.

1. In a fresh copy of the CSV, change the second member's status to `former`. Run the import in a fresh disposable
   directory, or first remove the previously imported example members.<br>
   Expected: Alex is imported and Jamie is skipped. Feedback reports one import and one rejection, with a status
   validation error for CSV record 3. Existing members and the source CSV are unchanged; all members are displayed.
   Restart and confirm only Alex was added.

1. In fresh copies, repeat with a non-NUS email, missing status, a duplicate case-variant email, or missing/extra
   columns in one member record.<br>
   Expected: Valid records are imported; each rejected record has its original number and a useful reason.
1. Repeat with a reordered header, a header-only file, and an unterminated quoted cell after a valid record.<br>
   Expected: The whole import fails; no valid prefix is imported and records, search results and JSON are unchanged.

1. In a fresh disposable directory, copy `docs/samples/members-partial.csv` into the application folder and run
   `import members-partial.csv`.<br>
   Expected: Two imports and two rejections. Record 3 has an invalid phone; record 4 duplicates record 2's email.
   The final member has no name, phone or address, retains archived status and the publicity tag, and shows its email
   as its heading. Every rejection is readable by scrolling the feedback pane. Restart and confirm both imports persist.
1. Correct the invalid phone in a copy of that CSV and import it again.<br>
   Expected: Only the corrected Jamie record is added. Previously imported emails and the duplicate Alex row are
   rejected. Existing member details and statuses are not overwritten.

1. Test cases: `import`, `import ""`, `import member contacts.csv`, and `import missing.csv`.<br>
   Expected: Format errors explain the command syntax; a missing file gives a file-reading error.
   Member records, current search results, and saved JSON remain unchanged.

These GUI checks are documented for manual execution; automated tests exercise the command and persistence paths.

### Recovering from a failed save

Use the current application build in a disposable folder on a filesystem that supports atomic replacement.
Use the default data-file location, `data/addressbook.json`, and keep a separate backup of that file.
The following steps use a file manager and do not require a particular operating system or shell.

1. Launch with at least two members, including one matching `find e/alex`. Ensure `retry@u.nus.edu` is not already
   present. Run `find e/alex` and note the displayed records.
1. Keep the app running. In a file manager, rename `data/addressbook.json` to `data/addressbook.saved.json`.
   Create a folder named `addressbook.json` inside `data`, and place a text file named `keep.txt` in that folder.
   The nonempty folder prevents the app from replacing the destination with a JSON file.
1. In the app, run `add e/retry@u.nus.edu`.<br>
   Expected: Saving fails; feedback says records and the previous search filter were restored. The original search
   results remain visible and no new member remains in the directory. No success confirmation is displayed.
1. Compare `data/addressbook.saved.json` with the backup.<br>
   Expected: The original JSON is unchanged, the test folder and `keep.txt` still exist, and there is no leftover
   `.nusocietydesk-*.tmp` file in `data` (enable hidden-file display if necessary).
1. Remove only the test folder `data/addressbook.json` and its `keep.txt`, then rename `data/addressbook.saved.json`
   back to `data/addressbook.json`. Restore this path before restarting the app.
1. Retry `add e/retry@u.nus.edu`, then close and restart the app.<br>
   Expected: The add succeeds, all members are displayed, and exactly one new member survives restarting.
1. Make a fresh backup and repeat the path-conflict steps with a valid CSV containing a new member email.<br>
   Expected: The previous records and filter are restored; the original JSON and source CSV remain unchanged.
   Remove the test folder and restore the original JSON filename before closing the app.

These GUI checks require manual execution; automated tests inject failures in the write and replacement operations.

### Finding members by tag

Use a disposable application folder and an empty directory (`clear` removes all members in that copy).
Run these setup commands one at a time:

```text
add n/Tag Search Active e/tagactive@u.nus.edu t/Publicity t/Exco
add e/tagarchived@u.nus.edu t/publicity
add e/tagyear@u.nus.edu t/publicity2026
add e/tagshort@u.nus.edu t/pub
add n/Publicity Example e/taguntagged@u.nus.edu
find e/tagarchived
archive 1
```

The directory now contains five members. The archived member has no name, phone or address.

1. Test cases: `find t/publicity`, `find t/PUBLICITY` and `find t/pUbLiCiTy`.<br>
   Expected: Only `tagactive@u.nus.edu` and `tagarchived@u.nus.edu` are displayed; the count is two.
   The archived member retains the `ARCHIVED` badge. The member named Publicity Example does not match because
   that member has no tags. The `publicity2026` tag does not match.
1. Test case: `find t/pub`.<br>
   Expected: Only `tagshort@u.nus.edu` is displayed; the count is one. Neither `publicity` nor `publicity2026` matches.
1. Test cases: `find t/public` and `find t/volunteer`.<br>
   Expected: Each produces an empty list and a count of zero.
1. Run `find e/tagshort`, followed by `find t/publicity`.<br>
   Expected: The second search finds both publicity members across the complete directory.
1. After running `find t/publicity`, try each of the following invalid commands:<br>
   `find t/`, `find t/publicity exco`, `find t/publicity t/exco`, `find t/publicity n/Tag`,
   `find p/9123 t/publicity`, `find t/publicity e/tagactive`, `find Tag t/publicity` and `find t/publicity!`.<br>
   Expected: Each shows an informative error, and the two publicity members remain displayed.
   Multiple values report "Tag searches accept only one tag. Example: find t/publicity."
1. Test cases: `find N/`, `find P/9123`, `find E/tagactive`, `find T/publicity`, `find T/ E/`
   and `find n/Tag T/publicity`.<br>
   Expected: Each reports "Search prefixes must be lowercase. Use n/, p/, e/ or t/."
   The previous results remain displayed. Lowercase prefixes with uppercase values, such as `find n/TAG`,
   `find e/TAGACTIVE` and `find t/PUBLICITY`, still work.
1. Test cases: `find G/`, `find F/value`, `find H/`, `find g/value`, `find name/Tag`
   and `find n/Tag G/value`.<br>
   Expected: Each reports an unsupported search prefix and lists `n/`, `p/`, `e/` and `t/` as supported options.
   For `G/`, the message is "Unsupported search prefix: G/. Use n/, p/, e/ or t/."
   Previous results, member records and saved data remain unchanged.
1. Test cases: `find e/G/` and `find e/name/Tag`.<br>
   Expected: Both are valid literal email-substring searches with zero matches in the test directory.
   The slashes inside the attached values do not trigger prefix errors.
1. Run `list`, then close and reopen the app.<br>
   Expected: All five records retain their contact details, tags and status. Searching has not changed member data.
   The application log contains an `INFO` completion message with the match count for each successful search.

Automated tests also cover tag values and prefixes separated by tabs or line breaks, and verify that rejected
searches leave saved data unchanged. The GUI checks above are provided for manual execution.

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
      Expected: Each command is rejected with guidance to use `tag` or `untag`. All person details remain
      unchanged.

1. Add and remove individual tags using Tag Test from the previous steps.

   1. Test case: `tag INDEX t/Volunteer`<br>
      Expected: The member has `exco`, `publicity`, and `volunteer`; contact details and other members remain unchanged.

   1. Test case: `tag INDEX t/VOLUNTEER`<br>
      Expected: Success with the same three tags and no duplicate.

   1. Test case: `untag INDEX t/PUBLICITY`<br>
      Expected: Only `exco` and `volunteer` remain.

   1. Repeat `untag INDEX t/publicity`.<br>
      Expected: Success with the same two tags.

   1. Close and reopen the app.<br>
      Expected: The member still has only `exco` and `volunteer`.

   1. Remove the remaining tags with `untag INDEX t/Exco` and `untag INDEX t/Volunteer`.<br>
      Expected: The member has no tags and retains all contact details.

1. Check displayed indices and invalid input.

   1. Run `find Tag`, note Tag Test's displayed index, and run `tag INDEX t/Volunteer` using that index.<br>
      Expected: Only Tag Test gains the tag, and all members are displayed afterward.

   1. Run `find Tag` again and run `untag INDEX t/Volunteer` using the new displayed index.<br>
      Expected: Only Tag Test loses the tag, and all members are displayed afterward.

   1. For each of `tag` and `untag`, try arguments `0 t/Volunteer`, `1 2 t/Volunteer`, `1`, `1 t/`,
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
      Expected: the first valid occurrence and other valid members load. A warning shows the file path,
      recovered-member count, and original positions of the duplicate records. The file remains unchanged.
   1. Run `list`, `find e/alex`, and `help`, then try `clear` and `delete 1`.<br>
      Expected: viewing, searching, and help work. Changes are rejected before modifying any records, with
      instructions to repair the file and restart. The file remains unchanged, including after `exit`.
   1. Correct the second email and relaunch.<br>
      Expected: all records load with lowercase emails, no warning appears, and normal saving resumes.
   1. Repeat with malformed JSON, a null record, or an email with surrounding whitespace.<br>
      Expected: malformed JSON produces a protected empty view. A null record or invalid email is skipped while
      other valid records load. Warnings identify errors, and commands cannot overwrite any of these files.
      A missing file still starts with sample members; a valid empty file stays empty and allows normal saving.

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
