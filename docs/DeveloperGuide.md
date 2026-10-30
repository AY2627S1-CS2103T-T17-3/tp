---
  layout: default.md
  title: "Developer Guide"
  pageNav: 3
---

# NUSocietyDesk Developer Guide

<!-- * Table of Contents -->
<page-nav-print />

--------------------------------------------------------------------------------------------------------------------

## **Acknowledgements**

NUSocietyDesk acknowledges the following open-source projects:

* [SE-EDU's AddressBook-Level3 (AB3)](https://github.com/se-edu/addressbook-level3), from which our codebase,
  architecture and documentation are adapted.
* [JavaFX](https://openjfx.io/), [Jackson](https://github.com/FasterXML/jackson) and
  [Apache Commons CSV](https://commons.apache.org/proper/commons-csv/) for the graphical interface,
  JSON processing and CSV importing.
* [JUnit 5](https://docs.junit.org/5.14.4/overview.html) for automated testing.
* [MarkBind](https://markbind.org/) and [PlantUML](https://plantuml.com/) for generating the documentation
  website and UML diagrams.

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

**`Main`** (consisting of classes [`Main`](https://github.com/AY2627S1-CS2103T-T17-3/tp/blob/master/src/main/java/seedu/address/Main.java) and [`MainApp`](https://github.com/AY2627S1-CS2103T-T17-3/tp/blob/master/src/main/java/seedu/address/MainApp.java)) is in charge of the app launch and shut down.
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

The **API** of this component is specified in [`Ui.java`](https://github.com/AY2627S1-CS2103T-T17-3/tp/blob/master/src/main/java/seedu/address/ui/Ui.java)

<puml src="diagrams/UiClassDiagram.puml" alt="Structure of the UI Component"/>

The UI consists of a `MainWindow` and its parts, such as `CommandBox`, `ResultDisplay`, `PersonListPanel`, and `StatusBarFooter`. All of these, including `MainWindow`, inherit from the abstract `UiPart` class, which captures common behavior among classes that represent visible GUI parts.

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/AY2627S1-CS2103T-T17-3/tp/blob/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/AY2627S1-CS2103T-T17-3/tp/blob/master/src/main/java/seedu/address/ui/MainWindow.java).

The `UI` component,

* executes user commands using the `Logic` component.
* listens for changes to `Model` data so that the UI can be updated with the modified data.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
* depends on some classes in the `Model` component because it displays `Person` objects from the model.

### Logic component

**API** : [`Logic.java`](https://github.com/AY2627S1-CS2103T-T17-3/tp/blob/master/src/main/java/seedu/address/logic/Logic.java)

Here's a (partial) class diagram of the `Logic` component:

<puml src="diagrams/LogicClassDiagram.puml" width="550"/>

The sequence diagram below illustrates the interactions within the `Logic` component, taking `execute("delete 1")` API call as an example.
It shows the successful save path; [save-failure recovery](#saving-member-data-safely) is detailed separately.

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
* `ViewCommandParser` is shown separately because it validates a single status value directly, without prefix tokenization.

### Model component
**API** : [`Model.java`](https://github.com/AY2627S1-CS2103T-T17-3/tp/blob/master/src/main/java/seedu/address/model/Model.java)

<puml src="diagrams/ModelClassDiagram.puml" alt="Model classes and per-member tag identity" />


The `Model` component,

* stores the address book data i.e., all `Person` objects (which are contained in a `UniquePersonList` object).
* exposes one filtered `ObservableList<Person>` for the current search and status view. The UI observes this list, so it updates when either filter changes.
* stores a `UserPrefs` object that represents the user’s preferences (currently, just the GUI settings). This is exposed to the outside as a `ReadOnlyUserPrefs` object.
* does not depend on any of the other three components (as the `Model` represents data entities of the domain, they should make sense on their own without depending on other components)

`Tag` normalizes valid alphanumeric names to lowercase with `Locale.ROOT`. Its `equals` and `hashCode` use that
normalized name, so `Person`'s `Set<Tag>` collapses `Publicity` and `publicity` into one tag. Command parsing,
CSV import, and JSON loading all construct `Tag` objects and share this rule. Saving writes the lowercase names.

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
no new Unicode support is introduced. The normalized local-part has an inclusive maximum of 64 UTF-16 units.
Different local-part aliases remain distinct.
The suffix is literal: `@nus.edu`, `@sub.u.nus.edu`, `@u.nus.edu.com`, and `@uXnusXedu` are invalid.
Construction, command parsing, CSV import, and JSON loading share the
[email-validation rule](#validating-email-length-safely).
Email-substring searches retain their existing independent rules, including substrings longer than 64 characters.

`Person.isSamePerson` compares canonical emails only. Members can share a name or phone number when their emails
differ. `UniquePersonList` uses this identity rule for addition, replacement, and bulk replacement, while
`Person.equals` still compares every field. Add and edit detect email conflicts across the complete directory,
including members outside the displayed filter, and identify the existing member by email and, when present,
name without a displayed index. Re-entering one's own email or changing only its case remains valid; changing an email releases
the previous one for reuse.

`EditCommand` preserves tags; `EditPersonDescriptor` accepts only name, phone, email, and address.
`EditCommandParser` still tokenizes `t/` so it can reject tag arguments explicitly instead of including them in another
field's value.

`TagAddCommand` and `TagRemoveCommand` each accept one displayed index and one tag. Their parsers reuse
`ParserUtil.parseIndex`, `ParserUtil.parseTag`, and duplicate-prefix validation to enforce this scope.
Each command resolves the person from `Model.getFilteredPersonList()`, copies their immutable tag set,
and adds or removes the requested `Tag` using the existing case-insensitive identity rule. It then constructs
a replacement `Person` with unchanged contact details and calls `Model.setPerson`.

Adding an existing tag or removing an absent tag throws a `CommandException` and leaves the record unchanged. Removing
the final tag is allowed. After success, both commands clear the search and show members in the selected
status view, matching `EditCommand`. `LogicManager` saves the updated address book through the existing
storage flow; no storage schema or UI changes are required.

The following sequence shows a successful `tag 1 t/treasurer` command. `untag` follows the same replacement
pattern, but rejects a tag that the member does not have. Saving and rollback are detailed in
[saving member data safely](#saving-member-data-safely). The referenced `Persist changed directory` frame
follows a snapshot taken before the tag command executes.

<puml src="diagrams/TagSequenceDiagram.puml" alt="Sequence of a tag command updating a member" />


<box type="info" seamless>

**Note:** The alternative, arguably more object-oriented, design below keeps a unique list of tags in `AddressBook`, and each `Person` references tags from that list. This lets `AddressBook` maintain one `Tag` object per unique tag instead of each `Person` holding its own `Tag` objects.<br>

<puml src="diagrams/BetterModelClassDiagram.puml" width="450" />
</box>


### Storage component

**API** : [`Storage.java`](https://github.com/AY2627S1-CS2103T-T17-3/tp/blob/master/src/main/java/seedu/address/storage/Storage.java)

<puml src="diagrams/StorageClassDiagram.puml" alt="Storage classes and the atomic file writer" />

The `Storage` component,
* can save both address book data and user preference data in JSON format, and read them back into corresponding objects.
* is implemented by `StorageManager`, which delegates the actual JSON file access to `JsonAddressBookStorage` and `JsonUserPrefsStorage` (one class per data file).
* depends on some classes in the `Model` component (because the `Storage` component's job is to save/retrieve objects that belong to the `Model`)

`MainApp` configures `data/nusocietydesk.json` and `preferences.json` beside the packaged JAR. It passes
these paths to storage, and the member data path to the UI. When running from compiled classes during
development, or when the code source location is unavailable, both paths are relative to the working directory.

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
before execution, while `list`, `view`, `find`, `findall`, `findany`, `help`, and `exit` run without saving.
New commands require write access by default and must explicitly override `Command.isReadOnly` to run during
recovery. Repairing the file requires a restart to restore saving. Only a missing file starts with sample members;
a valid empty file stays empty.
Valid mixed-case emails become lowercase in memory on load and in the JSON file after a normal successful save.

The following diagram shows the command guard after parsing. A protected session permits read-only commands
without saving and rejects commands that require write access before they execute.

<puml src="diagrams/ProtectedCommandExecutionActivityDiagram.puml" alt="Command execution guard during protected recovery" />

### Common classes

Classes used by multiple components are in the `seedu.address.commons` package.

--------------------------------------------------------------------------------------------------------------------

## **Implementation**

This section describes some noteworthy details on how certain features are implemented.

### Viewing the offline command reference

`AddressBookParser` delegates help arguments to `HelpCommandParser`, which implements `Parser<HelpCommand>`.
The help parser creates a command only when its trimmed arguments are empty; surrounding whitespace is allowed.
Extra arguments, such as `help 1`, produce the existing invalid-command-format error with `HelpCommand.MESSAGE_USAGE`
before any command executes or the Help window opens. Other commands retain their argument handling.
`HelpCommand` provides a single bundled `MESSAGE_HELP` string containing a description, usage, and example for each
of the 16 implemented commands, including all five `find` forms and a notation legend. Examples are text only and
are never executed.
The command does not read or change the model. It remains read-only for startup recovery and overrides
`requiresSave()` to return false, so `LogicManager` also skips saving in normal sessions, including when saving
would fail. No model, storage-schema, or preference changes are needed.

The command returns `SHOWING_HELP_MESSAGE` (`Opened help window.`) with `showHelp` true and `exit` false.
`MainWindow` displays this short acknowledgement through `ResultDisplay` and opens or focuses the existing
`HelpWindow`. The window sets its read-only, wrapping `commandReference` text area to `HelpCommand.MESSAGE_HELP`.
Its vertical layout keeps the existing User Guide message and Copy URL button above the scrollable reference.
The Help window is resizable, with a default content size of 760 by 560 pixels and minimum window dimensions of
450 by 300 pixels. Submissions retain existing command-history behavior.

F1 and Help → Help open the same window with both the guide link and the reference. The User Guide message and
Copy URL action are unchanged, and the URL is not repeated in the reference. There is no browser integration,
bundled full guide, new dependency, or persisted help state. The known minimized-help-window behavior remains unchanged.

`HelpCommandTest` checks the exact reference and parses its examples without executing them. `HelpCommandParserTest`
covers empty and whitespace arguments, rejected arguments, and null input. `AddressBookParserTest` checks dispatch
and existing error handling. `LogicManagerTest` checks unchanged records, search and status, missing and existing
files, a failing save implementation, and startup recovery. `MainWindowTest` checks the short acknowledgement,
reference placement below the guide link, wrapping, popup reuse, and F1/menu access with an unsent draft.
It also checks that rejected help arguments retain editable input with error styling and do not open a popup.

### Confirming a directory clear

`AddressBookParser` delegates `clear` arguments to `ClearCommandParser`. Only empty trimmed arguments are
accepted. Extra arguments produce `MESSAGE_INVALID_COMMAND_FORMAT` with `ClearCommand.MESSAGE_USAGE` before
confirmation or execution.

`MainWindow` calls `Logic.execute(commandText, clearConfirmation)` with a callback that opens a JavaFX confirmation
dialog. After parsing and checking startup recovery protection, `LogicManager` invokes this callback only for
`ClearCommand`, before taking a snapshot, executing the command, or saving. The single-argument `execute` method
supplies no approval and therefore cancels a valid `clear`. The logic layer has no JavaFX dialog dependency.

The dialog describes deletion of all members, including archived, hidden, existing, and imported members, and
suggests `delete INDEX` for one member. Cancel is the default and initially focused button. Escape, closing the
dialog, and Cancel return false, producing `Clear cancelled.` without changing records, search, status view, or
the saved file. Cancellation is a completed submission: the command box clears and the submission remains in history.
Only **Delete all members** returns true. `ClearCommand` then empties the entire directory through the existing
save-and-rollback flow. The status view is retained. Even an empty directory requires confirmation.

`ClearCommandParserTest` and `AddressBookParserTest` cover strict syntax and surrounding whitespace.
`ClearCommandIntegrationTest` checks confirmation timing, cancellation without saving, persisted deletion of hidden
and archived members, save-failure restoration, and startup recovery protection. `MainWindowTest` exercises the
actual dialog, safe defaults, keyboard cancellation, window closure, and confirmed clearing.

### Preventing duplicate records when adding members

`Email` stores valid NUS email addresses in lowercase using `Locale.ROOT`, so `Alex@U.NUS.EDU` and
`alex@u.nus.edu` identify the same member. Names and phone numbers do not determine identity.
After parsing and validation, `AddCommand.execute` calls `Model.hasPerson` to check the candidate against the
complete directory, including archived members and members hidden by the current search or status view.

The activity diagram below gives a high-level overview of the duplicate-email decision when adding a member.
It assumes the supplied details are valid and the application permits member changes. A duplicate email leads
to rejection, while a unique email allows the member to be added. Parsing, validation and persistence are
outside the diagram's scope.

<puml src="diagrams/AddMemberActivityDiagram.puml" alt="Activity diagram with duplicate and unique email guards, rejection and addition paths, and a merge before the activity ends" />

If the email already exists, the command locates the existing member and throws a `CommandException` with
`Messages.formatDuplicatePerson`. The error identifies the existing member by email and, when available, name.
No records or filters change, no save is attempted, and the records are never merged or overwritten.
For example, adding `e/ALEX@U.NUS.EDU` is rejected when `alex@u.nus.edu` already belongs to an archived member.

Otherwise, `Model.addPerson` adds the active member and clears the search while retaining the selected status
view. `AddCommand` returns its result to `LogicManager`, which follows the shared
[save/recovery flow](#saving-member-data-safely) before returning success to the UI. A save failure restores
the previous directory and search filter. `AddCommandTest` covers the add and duplicate-error branches.

### Recalling submitted commands

#### UI ownership and history state

`CommandBox` owns one JavaFX-independent `CommandHistory`. A key-pressed filter on its text field consumes
plain Up/Down events, including boundary no-ops, before JavaFX moves the caret. Modified arrows and other
keys are not intercepted. The existing `CommandExecutor` remains the only path to execution; navigation
never invokes it, updates the result display, or accesses `Logic`, `Model` or `Storage`.

`CommandExecutor` is supplied by `MainWindow` as `this::executeCommand`; it is only called on submission.
`InputState` is a nested immutable record used for navigation inputs and results as well as the saved draft.

The helper uses two `Deque<String>` collections backed by `ArrayDeque`: `commandsBeforeCursor` holds older entries
and `commandsAfterCursor` holds the selected entry followed by newer entries. Both are ordered oldest first;
their concatenation is the complete retained history. An empty `commandsAfterCursor` denotes the draft position.
Up moves the last older entry to the front of `commandsAfterCursor`; Down moves its first entry back to the end
of `commandsBeforeCursor`. Each arrow transition uses constant-time deque end operations.

#### Recalling an entry and restoring a draft

The first actual Up transition captures an immutable `InputState` containing exact draft text, selection
anchor and caret. Down past the newest entry returns that snapshot and clears it. Recalled entries place
the caret at the end. Before moving in either direction, the helper saves the selected entry's edited text
in `editedCommands`, keyed by its chronological index (`commandsBeforeCursor.size()`). These indices remain stable
until submission. Using indices keeps separate occurrences of the same command independently editable.
The map contains at most one temporary string per retained entry, including empty edits. Reverting an entry
to its submitted text removes its temporary edit. Returning to the draft does not clear edits, so browsing
again restores them. Stored submitted strings and duplicate detection remain unchanged.
A returned empty optional
denotes a no-op; a present result always restores text/selection and explicitly removes error styling,
even when the replacement text is identical. Boundary no-ops preserve edits, selection and styling.

For example, after submitting `list` and `help`, the user types an unsent draft and presses Up to recall
`help`. Now `commandsBeforeCursor` contains `["list"]` and `commandsAfterCursor` contains `["help"]`,
so the selected entry's index is 1. If the user edits the recalled text and presses Down, the helper stores the temporary edit under
that index and restores the draft's exact text and selection. The next Up restores the edit with the caret
at the end, while the submitted string remains `help`. None of these navigation steps executes a command.

#### Recording submissions and handling failures

`record` ignores `String.isBlank()` input without changing any state. `CommandBox` separately checks
`String.isBlank()` to decide whether to execute the submission.
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
Down is a boundary no-op when only the newest entry remains in `commandsAfterCursor` and there is no saved draft.
Repeated Down presses preserve edits and selection; the next Up still recalls the preceding entry.
Normal browsing captures and restores a real unsent draft as before. Temporary edits survive navigation.

For example, after `list` succeeds and `lisst` fails, the cursor selects `lisst` with no saved draft.
Up recalls `list`; Down returns to `lisst`; another Down is a no-op. This avoids showing the failed input
twice. An exact consecutive duplicate still reaches the executor, but neither adds an entry nor evicts one.

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
record in the directory; its email identity remains unchanged. A successful command clears the search while
retaining the selected status view. Archived members appear in All or Archived view and can be selected for
`activate` there. Changing a member's status can remove them from the current view.
`LogicManager` saves the new status through the existing JSON storage after each successful command.

### Filtering the member list by status

The button in `MainWindow.fxml` starts at `ALL`. `MainWindow.handleStatusFilterButton` cycles the state
`ALL → ACTIVE → ARCHIVED → ALL` through `Logic.setStatusFilter`. The `view all|active|archived` command sets
the same state directly. `LogicManager` passes both paths to `ModelManager`, the source of truth for the selected
view. `MainWindow` reads the state to update the button text and colour after either path changes it.
After a button click, `MainWindow` replaces the previous result feedback using `ViewCommand.MESSAGE_SUCCESS`,
matching the typed command's confirmation.
This is an in-memory setting and starts at All on each launch.

`ViewCommandParser` accepts exactly one status, ignoring case. `ViewCommand` is read-only so it works during
data recovery; it also skips the normal save because selecting a view does not change member records.

The sequence diagram shows a typed `view archived` followed by one button click. Both paths change the same
model filter and update the list; the typed command also synchronizes the button after execution. The command
path shown is a normal writable session. In recovery mode, `LogicManager` permits this read-only command and
executes it without saving as well.

<puml src="diagrams/StatusViewSequenceDiagram.puml" alt="Typed view command and status button update the same model filter and visible list" />

`ModelManager` keeps one `FilteredList<Person>` and two independent filter settings: the current search
predicate and the selected `StatusFilter`. Each change recomputes the list predicate as search **and** status.
Changing views therefore keeps a search; a new `find`, `findall`, or `findany` searches the complete directory
and replaces the previous search while the status view stays selected. `list` clears the search predicate but
keeps the status view. Commands that clear a search after a successful data change do the same. Find feedback
counts only members visible in the selected view. `edit`, `tag`, `untag`, `archive`, `activate`, and `delete`
resolve their index against this same `FilteredList<Person>` at execution time. They reject an index beyond the
visible list, even when that index exists in the full directory. Edits, tag changes, archives, and activations
then clear the search but retain the status view; deletion leaves the current search in place.

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
commands clear the search and show members in the selected status view.

Add, edit, tag and delete commands use `Messages.formatProvidedDetails` to omit absent contact fields and empty tags from their success messages.
The formatter label a supplied name as `Name: <person name>` consistently across command confirmations.
Unnamed duplicate errors identify the existing member by email only.
`PersonCard` uses the canonical email as its heading when the name is absent. Its separate email label is then
both invisible and unmanaged, so it contributes no layout space. When a name is present, the heading uses that
name and the email row is visible and managed. **Headings never wrap:** both names and emails used as headings
stay on one line, with an ellipsis when they exceed the available width. Phone and email rows also remain on
one line. Full values are available in tooltips and are not shortened in the model or saved data.
Phone, address and email rows always include `Phone: `,
`Address: ` and `Email: ` respectively; missing phone and address values use `-` after their labels.
Full-text tooltips include these prefixes. The content `VBox` in `PersonListCard.fxml` uses `TOP_LEFT`
alignment, five pixels of top padding and a shared 105-pixel minimum height. Short, single-line headings and
shared fields keep the same offsets from the top of each card when the last email row is hidden. Unused space
stays below the content. Cards with short details share the same height; wrapped address text and additional
rows of tag badges can make cards taller, so their height is not fixed. All other card fields remain on one
line. The heading row uses `TOP_LEFT` so the index stays aligned with the heading. Fields below a wrapped
address or additional tag rows move down naturally.

These display rules apply to active and archived members regardless of whether they were added, imported or
loaded from JSON. `PersonListPanel` constructs a new card when a member is replaced, so supplying a name through
`edit` restores the email row through the existing list refresh. No new model state or storage field is needed.
Name and phone predicates return false for absent fields. Email search remains available, and display
placeholders are never model values. A supplied address of `-` remains valid and looks the same as a missing
address; this display change does not alter validation.

`JsonAdaptedPerson` maps between optional model values and nullable saved strings. An email-only record can be
represented as `{"name":null,"phone":null,"email":"alex@u.nus.edu","address":null,"status":"active","tags":[]}`.
Reading does not rewrite the file; the next successfully saved command writes explicit nulls. Older versions requiring
complete contact details reject the entire file if it contains an incomplete member. With their existing loading
behavior, they may then overwrite that file after a successful command; users must back up before downgrading.

Automatic saving uses [save-failure recovery](#saving-member-data-safely).
[CSV import](#importing-member-directories-from-csv) also accepts absent name, phone, and address, but requires status.

Tests cover optional-field combinations, unchanged validation, explicit null persistence, legacy loading,
incremental completion across reloads, filtered edits, unnamed duplicates, email reuse, searches, tags and deletion.
The import regression checks supplied-field validation and that imported emails conflict with existing
incomplete members. `PersonBuilder` supports absent fields and preserves status when copying a member;
`PersonUtil` omits absent prefixes when generating commands.
`PersonCardTest` checks all eight optional-contact combinations, exact labels and tooltips, conditional email
visibility and layout participation, and narrow named and unnamed cards containing maximum-length emails.
It also compares card heights and shared field positions at narrow and wide widths, and checks that wrapped
addresses and tags can expand a card without shifting its heading or overflowing vertically.
`MainWindowTest` checks that editing an unnamed member's email updates the heading and supplying a name restores
the email row through the command submission and list refresh path.

### Rejecting Unicode blank addresses

`Address.isBlankAddress` recognizes empty strings and strings consisting only of Unicode `White_Space`
characters, including U+3000, U+2003, and nonbreaking spaces. It uses a separate `\p{IsWhite_Space}*` pattern.
`Address.isValidAddress` retains its original `[^\s].*` condition and additionally rejects these blanks;
the original expression's flags, leading-character restrictions, and line-terminator behavior are unchanged.
The shared error is
`Addresses must contain at least one non-whitespace character and be at most 200 characters long.`
U+200B ZERO WIDTH SPACE and U+FEFF BOM are outside this blank classification.

Add/edit reuse this model validation through `ParserUtil.parseAddress`, after the existing `trim()` calls.
Invalid supplied addresses fail before execution or saving, preserving records, search, status view, and JSON.
Omitted addresses retain their optional-field semantics. Nonblank address strings receive no new normalization.
The top-level add/edit parser can reject U+0085, U+2028, and U+2029 as command-format errors before address
validation; tokenization and error precedence remain unchanged.

`CsvMemberReader` uses `Address.isBlankAddress` after its existing field trimming to map Unicode blank cells
to absent addresses. Commons CSV still ignores surrounding Java whitespace in unquoted fields before the
reader calls `trim()`; quoted Unicode spaces inside a cell can therefore reach the model unchanged.
Tests include both quoted and unquoted blanks. Other fields retain their current rules.

JSON strings use the shared validator without trimming. Legacy Unicode blank addresses follow the existing
[protected recovery path](#storage-component): the affected member is skipped, valid members load, errors retain
original record positions, and saving is disabled until repair and restart. Strict `readAddressBook` still throws
`DataLoadingException` on any error. Missing or null addresses remain absent; no schema or migration is added.

`AddressTest` covers all 25 whitespace code points, mixtures, classification boundaries, and existing restrictions.
Add/edit parser and logic tests cover rejection, exact feedback, and state/file preservation. CSV reader and
import integration tests cover absent-address persistence; `AddressBookRecoveryTest` covers protected JSON loading.

### Bounding contact and tag lengths

`Name`, `Phone`, `Address`, and `Tag` each define a `MAX_LENGTH` constant and enforce it in their existing
`isValid...` method before matching a regular expression. Constructors reuse these validators.
The name character rules are described below. Optional contact fields, address blank classification, and
lowercase tag identity are independent of the length limits. `Phone.VALIDATION_REGEX` also expresses its
3-to-15-digit range.

| Field | Inclusive length range | Basis for the maximum |
| --- | --- | --- |
| Name | 1 to 127 | NUS's documented full-name requirement is fewer than 128 characters. |
| Phone | 3 to 15 digits | The international E.164 numbering limit is 15 digits; the existing minimum of 3 is an application rule. |
| Address | 1 to 200, with nonblank text | The published Myinfo Singapore address components fit into 174 characters in the representation below, leaving 26 for additional formatting or accommodation details. |
| Each tag | 1 to 30 | An application policy for concise member categories; `internationalstudentambassadors` fits at exactly 30 characters. |

The name maximum follows the [NUS Canvas name data standard](https://nus.atlassian.net/wiki/spaces/canvasinstructor/pages/270172201),
which says a full name must be less than 128 characters. We adopt its length bound. This is a local product
choice, not a universal maximum for human names.

The phone upper bound follows [ITU-T Recommendation E.164](https://www.itu.int/rec/T-REC-E.164/en).
The app continues accepting digits only and does not validate country-code allocation, number ownership, or
whether a number is in service. Its 3-digit minimum also allows short numbers already accepted by the app.
Using the E.164 upper bound does not make this a complete E.164 validator.

We size addresses for NUS students' current Singapore residences, including campus rooms and rented apartments.
The published [Myinfo v4 `AddressSG` schema](https://public.cloud.myinfo.gov.sg/myinfo/api/myinfo-kyc-v4.0.html)
allows block/house number 10, street name 66, building name 65, floor 3, unit 5, and postal code 6 characters.
Together with the 9 letters in `Singapore`, these components total 164 characters. In the representation
`[block] [street], #[floor]-[unit], [building], Singapore [postal]`, spaces and punctuation add 10, giving 174.
The 200-character cap adds 26 characters of headroom. This is a calculated bound for that schema and formatting,
not a claim that Singapore has an officially longest address or a nationwide 200-character maximum.
The address remains free text; geographical or postal-code existence checks are not introduced.

Tags describe member categories such as `exco`, `publicity`, and `volunteer`. The 30-character cap allows longer
descriptive categories while limiting oversized labels and feedback. It is our product policy, not an external
tagging standard, and it does not limit the number of distinct tags a member can have.

Lengths use `String.length()` (UTF-16 code units), including spaces and punctuation where permitted. Add/edit
and tag commands apply their existing `trim()` before validation. CSV import retains its current trimming and
missing-field rules. JSON loading validates saved values without trimming. Every path uses the model limits:
command failures become `ParseException`, CSV failures identify and reject the affected record, and JSON
failures follow the protected recovery path with changes and saving disabled until repair and restart.
Legacy overlong values are never silently shortened. Updated constraints messages state the relevant maximum.

Name and phone search values retain their independent keyword/substring rules; for example, a one-digit phone
substring remains valid. Exact tag searches in `find`, `findall`, and `findany` use `ParserUtil.parseTag` and
therefore enforce the same 30-character tag maximum. `edit` continues rejecting every `t/` argument.

The member card constrains its content to the allocated width. Only the address label wraps its text; JavaFX
handles word boundaries and uninterrupted values without inserting line breaks into model values or label text.
The tag `FlowPane` places whole badges on additional rows when they cannot all fit on one row. Individual tags
do not wrap internally. Names, phone numbers and emails, including email headings for unnamed members, remain
on one line. These fields and individual tags can use ellipses, with their full text available in tooltips.
The index and status badge remain visible. Result feedback wraps in its separate pane. Length limits bound
stored values; display constraints accommodate valid long values and narrower windows.

Tag labels in `PersonCard` share a conditional maximum-width binding. Until the tag pane has a positive width,
the labels keep their natural widths so JavaFX can measure the height of wrapped tag rows correctly. After
sizing, each label is constrained to the actual pane width, preserving ellipses and full-text tooltips for long
tags. Binding directly to the initial zero width makes the first height calculation treat all tags as tiny
labels on one row; when the labels expand, extra rows can overlap the phone and address until another layout.
The conditional binding corrects that first measurement without delayed refreshes, forced layouts or changes
to tag commands, validation or storage.

`PersonListViewCell` sets its preferred width to zero so long graphics do not expand the horizontal scroll range.
The existing list-cell skin sizes the resizable card to the allocated cell width, accounting for the list's
vertical scrollbar and insets. Card heights remain computed from the available width; no fixed cell height,
scrollbar measurements or additional width listeners are needed. `MainWindow.fxml` sets the minimum window
width to 800 pixels. `GuiSettings` defaults to a window of 800 by 600 pixels; existing window preferences continue
to load and save through the same preference format.

`PersonListPanelTest` exercises the actual list at 340 and 1556 pixels, including narrow-wide-narrow resizing,
scrolling and cell reuse, member replacement, selection, scrollbar appearance and disappearance, and a card
taller than the viewport. It checks horizontal overflow, card bounds, and the skin's rendered address text for
complete values without ellipses or vertical clipping. Full-text address display is distinct from the
single-line ellipsis truncation used for names, phone numbers and emails. Long email rows and email headings
are checked for single-line rendering with ellipses at narrow widths and complete displayed text at wide
widths, while their label values and tooltips retain the complete email. `PersonCardTest` also checks labels,
tooltips, conditional email-row visibility, non-wrapping headings and email rows, and heading-index alignment.
The minimum-width window check verifies its actual width after showing it.

The tag-update regression uses shown stages and `JavaFxTestUtil.runAfterLayoutPulse` to measure the card after a
normal JavaFX layout pulse. It checks each tag label's bounds against the tag pane and phone row, and verifies
that subsequent contact rows do not overlap. Repeated member replacements cover short and maximum-length tags,
named and unnamed members, both statuses, selection, resizing, cell reuse, and growth and shrinkage across row
boundaries. `MainWindowTest` submits repeated `tag` and `untag` commands against the last visible member without
scrolling between commands. These tests do not call `applyCss`, `layout` or `refresh` to repair the measured card.

Model and parser tests cover maximum-minus-one, maximum, maximum-plus-one, large inputs, and surrounding
argument whitespace. `FieldLengthValidationIntegrationTest` covers command rejection without state or file
changes, no save attempts, successful boundary persistence and edits, and independent search rules.
CSV and JSON tests cover shared limits and protected recovery; JavaFX tests cover command feedback, retry,
and narrow-card layout with maximum-length values and full-text tooltips.

### Supporting punctuation in member names

`Name` accepts letters `A-Z`/`a-z`, digits, ordinary spaces, and `, ( ) / . @ - '`. It requires at least one
letter or digit, rejects leading spaces and unsupported characters (including internal tabs and line breaks),
and counts spaces and punctuation towards the 127-character limit. Command parsing and CSV import retain
their existing surrounding-whitespace trimming; JSON loading validates the stored value as supplied.
Accepted names retain their spelling, case, punctuation, and internal spaces.

The [NUS Office of Admissions name-format guide](https://www.dbs.nus.edu.sg/wp-content/uploads/sites/7/2025/06/nameformatexplanationguide.pdf)
uses names containing `S/O` and `D/O`, providing direct evidence that a slash is meaningful in NUS member names.
The [Myinfo name-data guidance](https://partnersupport.singpass.gov.sg/hc/en-sg/articles/32733563138585-What-are-the-special-characters-allowed-in-Myinfo-Name-data-item)
lists spaces and the complete punctuation set above. Together these sources support accepting ordinary local
name formats instead of requiring secretaries to alter their spelling. We adopt Myinfo's punctuation set;
we retain the application's existing digit acceptance for compatibility, although Myinfo disallows digits.
These are directory input rules, not a claim of complete NUS or Myinfo compliance or universal name support.
Curly apostrophes, accented letters, and non-Latin scripts require a separate product-scope decision.

Add and edit use `ParserUtil.parseName`, CSV import constructs `Name`, and JSON loading uses
`Name.isValidName` and the constructor, so all entry paths share the same rules. Name searches continue to
match whole whitespace-delimited words ignoring case; punctuation remains part of the word. For example,
`find n/O'Connor` matches `O'Connor Tan`, while `find n/Connor` does not. Use a prefixed search such as
`find n/S/O` for a patronymic token so it is interpreted as a name keyword.

`NameTest` covers the supported punctuation, rejected characters and length boundaries. `ParserUtilTest`,
`NameValidationIntegrationTest`, `ImportIntegrationTest`, and `JsonAddressBookStorageTest` verify trimming,
add/edit, searches, rejected-command state preservation, quoted CSV commas, persistence, and successful
JSON loading without protected recovery. `PersonCardTest` verifies exact name labels and tooltip text.

### Validating email length safely

The local-part regular expression contains a repeated group. Matching a sufficiently long, punctuation-separated
candidate can consume excessive stack space and cause `StackOverflowError`. `Email.getValidationError` checks
the normalized local-part length before pattern matching, bounding the recursive group matching.

The helper counts UTF-16 units before the first `@`, or across the whole candidate if `@` is absent. It returns
`Email local-part must contain at most 64 characters; received N.` when that count exceeds 64,
`Email.MESSAGE_CONSTRAINTS` for other invalid values, or an empty optional for a valid email.
`Email.isValidEmail` and the constructor reuse this helper, keeping the validation rule in the model.

Callers adapt the selected error to their existing failure handling. `ParserUtil.parseEmail` throws
`ParseException` before command execution or saving; the UI displays the message and retains editable input
with error styling and history. CSV import adds the original record number and rejects that record while
processing other records. JSON loading adds the saved record position and uses the
[protected recovery path](#storage-component). Each caller retains its existing field-validation order.

### Finding members by contact details or tag

The partial class diagram below shows the shared search design. Parsers validate input and construct predicates;
`FindCommand` applies the supplied predicate through the `Model` interface. `Predicate<Person>` is the common
contract for field matching and AND/OR combinations. `NameContainsKeywordsPredicate` represents the field
predicates; other field predicates and parsing helpers are omitted.

<puml src="diagrams/SearchClassDiagram.puml" alt="Partial search class diagram showing shared command execution and predicate composition" />

`AddressBookParser` routes `find`, `findall` and `findany` to their respective parsers, including multiline
arguments. `SearchParserUtil` validates lowercase `n/`, `p/`, `e/` and `t/` prefixes and normalizes ASCII whitespace
before tokenizing. Unsupported and uppercase prefixes are rejected; slashes within attached values remain literal.

`FindCommandParser` accepts unprefixed name keywords with OR matching, or one prefixed condition. It retains
its restrictions against repeated prefixes, multiple fields and multiple values within a prefixed condition.

`FindAllCommandParser` and `FindAnyCommandParser` use `SearchParserUtil.parseConditions` to require at least
one explicitly prefixed condition. It reads every occurrence with `ArgumentMultimap.getAllValues`, rejects
unprefixed text and blank values, and validates the entire input before returning predicates.
`AllConditionsPredicate` combines these with AND; `AnyConditionsPredicate` combines them with OR.
Invalid conditions therefore reject even an OR search that could otherwise match a member.

`FindCommandParser` and `SearchParserUtil.parseConditions` reject empty or Java-whitespace-only values using
`String.isBlank()` before constructing field predicates, reporting the command's invalid-format message.
This includes EM SPACE (U+2003) and IDEOGRAPHIC SPACE (U+3000), which `trim()` leaves intact. Non-breaking
spaces such as U+00A0 are not blank under these rules. Trimming and splitting retain the `trim()` and `\s+` rules.

All three parsers reuse the field predicates: names match whole words ignoring case, phones match consecutive
ASCII digits, emails match literal substrings ignoring case, and tags match exactly ignoring case.
Email substrings need not be complete addresses; tags are validated through `ParserUtil.parseTag`.
Absent names or phones make the corresponding condition false. The selected status view is applied after
the contact or tag predicate.

Each parser creates one `FindCommand`, which applies its predicate once through `Model.updateFilteredPersonList`.
The search checks the complete directory rather than the previous results. The combined filter preserves
directory order and displays each member matching both search and status once. The command reports and logs
the visible count, including zero for an empty result. It remains read-only for
recovery mode and requires no changes to member data or the storage schema.

Parser, predicate, command and integration tests cover AND/OR matching, repeated prefixes, overlapping matches,
invalid conditions, incomplete and archived members, and searches after an earlier filter. Parser regression
tests cover Unicode whitespace-only values in every search field, alone and alongside valid combined conditions.
Rejected searches preserve the previous results, member records and saved data.

### Importing member directories from CSV

Import adds valid, nonduplicate members while reporting rejected records in CSV order. It separates file parsing
from changes to the live model so a fatal file error cannot leave a partially imported directory. See the
[User Guide](UserGuide.md#importing-member-contacts-from-csv-import) for the CSV format and user-facing examples.

The main responsibilities are:

* `AddressBookParser` dispatches to `ImportCommandParser`, which parses one local path without file I/O.
* `CsvMemberReader` parses the complete UTF-8 file with Apache Commons CSV before validating member records.
  It returns an immutable list of `CsvMemberRecord` outcomes: each contains the original record number and either
  a valid `Person` or its first validation error.
* `ImportCommand` checks those outcomes against a temporary `AddressBook` and applies the accepted members together.

The execution flow is:

1. **Protect the source.** Before execution, `LogicManager` derives the application home from its code location:
   the real JAR's parent folder for packaged runs, or the working directory for IDE/Gradle runs. It resolves the
   source with `Path.toRealPath` and checks containment using `Path.startsWith`, so parent traversal, symbolic
   links to outside files or folders, and similarly named sibling folders cannot escape the boundary. Both
   absolute and working-directory-relative paths are accepted when their real location is inside the home folder.
   It also compares the CSV path with the JSON save path using normalized paths and `Files.isSameFile`, rejecting
   aliases and links to the save file. These checks complete before CSV parsing, model changes, or saving.
   The reader opens the source for reading only.
1. **Read and validate.** The reader checks the fixed six-column header, trims member fields, and reuses the model
   validators. Blank name, phone, and address values become empty optionals. Email and status are required;
   `Status.isValidStatus` accepts only `active` and `archived`. `Tag` and `Set<Tag>` normalize and deduplicate tags.
1. **Stage accepted records.** The command copies the complete directory, including archived and filtered-out members.
   It collects validation errors and checks valid records against the copy's email identity rule, adding each accepted
   member to that copy. This also detects duplicates of earlier accepted CSV records; invalid records reserve no email.
1. **Apply and save.** If at least one record is accepted, the command calls `Model.setAddressBook` and clears the
   search while retaining the selected status view. It compares the accepted members with the resulting visible
   list and adds their hidden count and a `view all` hint when needed. Existing members and rejected records do
   not contribute to that count. It returns counts and rejection details to `LogicManager`, which performs the
   shared [save/recovery flow](#saving-member-data-safely). The UI receives success only after that save completes.

The sequence diagram below expands `ImportCommand.execute`. Source-path checks and the directory/filter snapshot
have already completed in `LogicManager`. Incidental logging calls are omitted.
Where activation bars restart at alternative-branch boundaries, they continue the same pending calls;
this is a workaround for PlantUML's linear activation tracking.

<puml src="diagrams/ImportSequenceDiagram.puml" alt="CSV import validation, duplicate checks, and success or failure outcomes" />

The `Persist changed directory` reference expands to the matching frame in the
[save/recovery diagram](#saving-member-data-safely). Its snapshot is taken before import execution, not after it.

For example, with none of the sample emails already present, [members-partial.csv](samples/members-partial.csv)
stages records 2 and 5, rejects record 3's invalid phone, and rejects record 4 as a duplicate of record 2.
Both accepted members are applied together. Record numbers include the header as 1 and exclude empty lines;
quoted multiline cells do not create extra record numbers. Feedback explains all three numbering rules whenever
there are rejections, including when every member record is rejected.

| Outcome | Effect |
| --- | --- |
| Invalid member fields, wrong column count, or duplicate email | Reject that record and continue with the other outcomes. |
| All records rejected | Throw `CommandException` with every rejection; preserve the model and filter and skip saving. |
| Unreadable file, decoding error, invalid header, malformed CSV, or no member records | Fail before applying any records, even if earlier CSV records were valid. |
| Save failure after applying accepted members | Restore the original directory and filter using the shared save/recovery flow. |

Import logs contain preparation counts or fatal-file failure messages, without member fields or rejection details.
A preparation log does not mean the later save succeeded. `CsvMemberReaderTest` and `ImportCommandTest` cover parsing
and record outcomes; `ImportIntegrationTest` checks persistence, source protection, and failure behavior.

### Saving member data safely

Saving protects both the in-memory directory and the existing JSON file. Commands that change member records return
success to the UI only after persistence succeeds. Read-only commands (`find`, `findall`, `findany`, `list`, `view`,
`help`, and `exit`) return `false` from `requiresSave()` and execute without attempting to save. They remain available
when a previously loaded data file becomes unwritable. In a normal writable session, parsing and pre-execution guards
run first, including confirmation for `clear`. For commands that require saving, `LogicManager` then follows this sequence:

1. **Snapshot.** Copy the directory and capture `Model.getFilteredPersonListPredicate()` before executing the command.
1. **Execute.** Run `command.execute(model)`. A parser or command exception exits before saving; the save-failure
   rollback below does not handle command-execution failures.
1. **Write.** Delegate saving through `StorageManager` to `JsonAddressBookStorage`, which serializes the complete
   directory before invoking its package-private `AtomicFileWriter` helper.
1. **Replace.** The writer creates a temporary file beside the destination, writes and closes its UTF-8 contents,
   then replaces the destination with `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)`. Existing symbolic links are resolved
   to their target so the configured link remains intact. There is no non-atomic fallback.
1. **Return or restore.** After replacement succeeds, return the command result. On `IOException` (including
   `AccessDeniedException`) or `SecurityException` from saving, restore the directory and previous search predicate,
   then throw a `CommandException` explaining the failure. The selected status view stays unchanged. The success
   result is not returned.

The diagram below shows the flow for commands that require saving, including failures during temporary-file writing
or atomic replacement. The `saveAddressBook` call is forwarded through `StorageManager`; that delegation is collapsed into
one arrow. Command-specific model interactions and logging calls are omitted.
As in the import diagram, activation bars redrawn at alternative-branch boundaries continue the pending calls.

<puml src="diagrams/SaveRecoverySequenceDiagram.puml" alt="Directory and filter snapshot, atomic saving, and rollback after a save failure" />

Failures earlier in saving, such as JSON serialization or destination checks, use the same restoration path.
If the command itself throws, execution exits before the `Persist changed directory` frame.

Restoring the search predicate, rather than just the visible list, preserves subsequent search behavior, including searches
that matched no members. For example, if an import updates the model but atomic replacement fails, its accepted
members disappear and the previous search is restored; the existing JSON and CSV source remain unchanged.

After a failed write or replacement, the writer attempts to remove the temporary file. A cleanup failure is attached
to the original exception and logged. No fallible cleanup follows a successful replacement, and a failed first save
leaves the destination absent. Logs identify the command class and recovery metadata without raw input or member details.

This covers ordinary write and replacement failures, not power loss or concurrent edits by another application.
User-preference saving is unchanged. The separate [startup recovery guard](#storage-component) rejects member changes
and disables saving after a partial or failed load; it is not the rollback mechanism described here.

`AtomicFileWriterTest` injects partial-write and replacement failures and checks file preservation and cleanup.
`PersistenceIntegrationTest` verifies record/filter restoration, retry, and reload across commands;
`ImportIntegrationTest` checks the same behavior for imports. See the
[manual save-failure scenario](#recovering-from-a-failed-save) for the GUI check.

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

* has to manage the contact details of 50 to 200 active and former members in total
* currently juggles spreadsheets and chat histories to look up members
* prefers desktop apps over other types of applications
* can type fast, and prefers typing to mouse interactions
* is comfortable using CLI apps

**Value proposition**: Help NUS society secretaries keep member contact details, roles, committees, and membership status accurate and instantly searchable, so routine membership administration takes seconds instead of searching across spreadsheets and chat histories.


### User stories

Priorities: High (must have) - `* * *`, Medium (should have) - `* *`, Low (could have) - `*`

Statuses describe current support and future plans: **Implemented** means the story is supported,
**Planned for future iterations** means the feature is intended for a future iteration and is not currently available.

| Priority | As a …                                                                  | I want to …                                                                 | So that I can…                                                        | Status                        |
| -------- | ----------------------------------------------------------------------- | --------------------------------------------------------------------------- | --------------------------------------------------------------------- | ----------------------------- |
| `* * *`  | secretary with an existing directory                                    | import existing contacts into NUSocietyDesk and review any rejected records | adopt the product without manual re-entry or risking silent data loss | Implemented                   |
| `* * *`  | secretary                                                               | attach consistent society-related information to a member                   | organise contacts according to their place in the society             | Implemented                   |
| `* * *`  | secretary who remembers only part of a member’s information             | search across relevant contact fields                                       | locate the member without knowing their exact name                    | Implemented                   |
| `* * *`  | secretary                                                               | combine name, phone, email and tag search conditions                        | find members by contact details, committee or role                    | Implemented                   |
| `* * *`  | secretary                                                               | change selected member information without replacing unrelated details      | make corrections safely                                               | Implemented                   |
| `* * *`  | secretary                                                               | mark former members as archived without deleting their records              | retain their contact history                                          | Implemented                   |
| `* * *`  | secretary                                                               | activate a member who was archived                                          | correct an incorrect archive or record a returning member             | Implemented                   |
| `* * *`  | secretary                                                               | switch between all, active and archived members while searching             | focus on current members and still retrieve former members            | Implemented                   |
| `* * *`  | secretary dealing with members who have similar or duplicate names      | distinguish their records using additional information                      | avoid contacting or modifying the wrong member                        | Implemented                   |
| `* * *`  | secretary                                                               | receive precise confirmation and error messages                             | know exactly what changed and how to correct a failed operation       | Implemented                   |
| `* *`    | secretary who has to manage local and international members             | store phone numbers with country codes                                      | contact members who have international phone numbers                  | Implemented                   |
| `* *`    | secretary with incomplete information                                   | record a member using a NUS email and any other available details           | capture the contact immediately and complete it later                 | Implemented                   |
| `* *`    | secretary who only has a member’s NUS email                             | create an incomplete record using their NUS email                           | save the contact immediately and complete the remaining details later | Implemented                   |
| `* *`    | secretary processing the same organisational change for several members | update the relevant records together                                        | spend less time on repetitive maintenance                             | Planned for future iterations |
| `* *`    | secretary                                                               | detect and resolve possible duplicate member records                        | prevent conflicting records for the same member in the directory      | Implemented                   |
| `* *`    | secretary who made an incorrect change                                  | reverse a recent data-changing operation                                    | prevent a typing mistake from permanently damaging the directory      | Planned for future iterations |
| `* *`    | secretary reviewing a long list of results                              | order the results predictably                                               | scan them efficiently                                                 | Planned for future iterations |
| `* *`    | frequent user                                                           | repeat or adapt recent commands                                             | complete recurring work with less typing                              | Implemented                   |
| `* *`    | returning user                                                          | rediscover relevant command syntax without leaving my current task          | use the product occasionally without having to relearn it             | Implemented                   |
| `*`      | potential user exploring the product                                    | view representative sample member records                                   | understand the application before using real data                     | Implemented                   |
| `*`      | expert user                                                             | use shorter alternatives for frequent commands                              | enter routine operations faster                                       | Planned for future iterations |
| `*`      | secretary                                                               | view a concise summary of members by status or committee                    | spot obviously inconsistent records                                   | Planned for future iterations |

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

**Use case: UC03 - Find and inspect members**

**MSS**

1. Secretary requests to find members using name, phone, email or tag criteria, choosing whether all or any
   criteria must match when combining conditions.
2. NUSocietyDesk displays matching members with their available contact details, tags and the number of matches.
3. Secretary inspects the displayed details to identify the intended members.

   Use case ends.

**Extensions**

* 1a. Secretary supplies missing or invalid search criteria.

  * 1a1. NUSocietyDesk shows an error message explaining how to correct the search.
  * 1a2. NUSocietyDesk keeps the previous results and member records unchanged.

    Use case ends.

* 2a. No members match the search.

  * 2a1. NUSocietyDesk displays an empty list and reports zero matches.

    Use case ends.

### Non-Functional Requirements

1. **Performance:** NUSocietyDesk should respond to typical commands, such as adding, editing, finding, and listing
   members, within 2 seconds when managing up to 200 member records in total, including active and archived records.
   For commands that modify member data, this response time includes saving the changes.

2. **Capacity:** NUSocietyDesk should support directories containing up to 200 member records in total, counting active
   and archived records together.

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
* **Archived member record**: A retained record with archived status; it appears in All or Archived view when it matches the current search
* **Committee**: A subgroup of the society to which a member belongs, such as Publicity or Logistics
* **Contact details**: A member's name, phone number, email address, and address
* **Duplicate member record**: A member record with the same email after lowercasing as an existing record. Members may share a name or phone number when their emails differ
* **Former member**: A member who is no longer active in the society; their retained record can be marked `archived`
* **Incomplete member record**: A valid member record with an email and at least one of name, phone number, or address absent
* **Member**: A person whose contact and society-related information is stored in NUSocietyDesk
* **Member directory**: All member records stored in NUSocietyDesk, including archived members and members outside the current search results
* **Member record**: One member's contact details, membership status, and tags; their email determines identity
* **Membership status**: A record's `active` or `archived` classification, stored separately from its tags; `former` is not a valid status value
* **Status view**: The GUI's All, Active or Archived selection; it limits the displayed list and search results without changing saved member statuses
* **Rejected record**: A CSV member record skipped because of invalid fields, missing required values,
  an incorrect column count, or a duplicate email. Missing optional contact details alone do not cause rejection
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
      Expected: The GUI opens in All view with eight sample society members in the order listed in the
      [User Guide](UserGuide.md#exploring-the-sample-members), using role and committee tags in lowercase.
      Alex has `president` and `exco`, Charlotte has only `publicity`, and Irfan has no tags.
      The window size may not be optimal.

   1. Run `view archived`, then `view active`.<br>
      Expected: Archived shows only Roy Balakrishnan at displayed index 1 with an `ARCHIVED` badge and
      `volunteer` tag. Active shows the other seven members; Roy is absent.

   1. Run `find e/irfan`.<br>
      Expected: Only Irfan Ibrahim appears, with `irfan@u.nus.edu`, `Phone: -`, `Address: -`, and no tags.

   1. Run `view all`, then `list`, then `find t/publicity`.<br>
      Expected: Charlotte Oliveiro and Morgan Lee (`morgan.one@u.nus.edu`) appear at displayed indices 1 and 2.
      Run `list`, then `find Morgan`: both Morgan Lee records appear, sharing phone `91234567` but with distinct emails.

   1. Run `list`, then `tag 5 t/Volunteer` to save the sample directory. Close and relaunch.<br>
      Expected: All eight members load without a recovery warning. Irfan retains the new tag and absent phone
      and address, Roy remains archived, and both Morgan Lee records remain. A saved directory is loaded as it
      is; the built-in samples do not replace it.

1. Saving window preferences

   1. Resize the window to an optimal size. Move the window to a different location. Close the window.

   1. Relaunch the app by double-clicking the JAR file.<br>
       Expected: The most recent window size and location are retained.

1. Compact display and saved monitor placement (manual check; requires two monitors for the second part)

   1. At 1280×720 resolution with 150% display scaling, launch the app and resize it to its minimum height.<br>
      Expected: The command box, result display, member list, status button and status bar remain visible.

   1. With two monitors connected, maximize the app on the second monitor, close it, and relaunch it.<br>
      Expected: The app reopens on the second monitor, including when the saved maximized-window coordinates
      extend slightly above both displays.

   1. Close the app, disconnect the second monitor, and relaunch it.<br>
      Expected: The window fits within the primary monitor's usable area.

### Viewing command help and the online guide

1. Run `help` with Internet access unavailable.<br>
   Expected: The Help window shows the User Guide link at the top, followed by descriptions, usage, and examples
   for all 16 commands, including all five `find` forms. No browser opens. The command box clears, and the result
   pane shows `Opened help window.`.
1. Resize the Help window, scroll to the last entry and notation legend, select text, and copy it.<br>
   Expected: The entire reference is readable, long lines wrap, and the link and Copy URL button remain above it.
1. Run `help` again after scrolling to the end.<br>
   Expected: It focuses the same Help window with the complete reference; no example executes and the directory is unchanged.
1. Close the Help window, then try `help 1`, `help add`, and `help --guide`.<br>
   Expected: Each reports an invalid-command-format error with help usage guidance, retains the input with error styling,
   and does not open the Help window. Running ` help ` with surrounding spaces still opens it.
1. Select a status view and search for a member, then run `help`.<br>
   Expected: The current search, status view, displayed members, and saved data are unchanged.
1. Type an unsent draft, select part of it, and press F1; repeat using Help → Help.<br>
   Expected: The existing guide URL and Copy URL button appear above the command reference. The draft and its selection
   remain unchanged. Copy URL still supplies the full online guide's address. Restore the window manually if minimized.
1. Use the startup-recovery and failed-save setups documented below, then run `help`.<br>
   Expected: Help still succeeds, does not save, and preserves the existing data file. A missing data file is not
   created by help. Commands that change members retain their existing recovery and save-failure behavior.

### Switching between All, Active and Archived views

Use a disposable application folder with `data/nusocietydesk.json` containing `{"persons":[]}` before launch.

1. Add `add e/active@u.nus.edu` and `add e/former@u.nus.edu`, then run `archive 2`.<br>
   Expected: The button reads `ALL`. Both members appear; the second has an `ARCHIVED` badge.
1. Click the status button once, then again, then a third time.<br>
   Expected: Its text and list change together: `ACTIVE` shows only `active@u.nus.edu`, `ARCHIVED` shows only
   `former@u.nus.edu`, and `ALL` shows both members.
1. Run `find e/active`, then click through Active and Archived.<br>
   Expected: Active shows the matching member, Archived shows an empty list, and the search remains in effect.
1. Run `view active`, then `view archived`, then `view all`.<br>
   Expected: The button and list follow each command. The existing search remains in effect, so `view archived`
   shows no matches and `view all` restores the active match. No member statuses change.
1. Run `view archived`, then `list`.<br>
   Expected: The search clears, the button still reads `ARCHIVED`, and `former@u.nus.edu` appears at index 1.
1. Run `edit 1 n/Former Member`, then `tag 1 t/Alumni`, followed by `untag 1 t/Alumni`.<br>
   Expected: Each command updates `former@u.nus.edu`, the member at index 1 in the current Archived view.
   `active@u.nus.edu` stays unchanged. The selected view remains Archived.
1. Run `activate 1`.<br>
   Expected: The member leaves the Archived list. Switching to All or Active shows both active members.
   Restarting the app returns the button to `ALL`; the activated status remains saved.

### Recalling commands and restoring drafts

Use a disposable application folder for these checks, particularly `clear` and save-failure tests.

1. On a fresh launch, type a draft, select part of it, and press Up/Down.<br>
   Expected: Empty history leaves text, caret and selection unchanged.
1. Submit `list`, then `lisst`.<br>
   Expected: `list` succeeds and clears the box; `lisst` shows `Unknown command.` and remains with error styling.
   Press Up once: `list` is recalled with normal styling, while the error message remains in the result pane.
   Press Down to return to `lisst`, correct it to `list` and submit; the result is `Listed members in the selected status view.`.
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

Use a disposable application folder. Before launching, create `data/nusocietydesk.json` containing
`{"persons":[]}`. A missing file would load sample members instead.

1. Run `add e/Alex@U.NUS.EDU`.<br>
   Expected: One active member is added with heading `alex@u.nus.edu`, rows `Phone: -` and `Address: -`,
   no separate email row, and feedback `New person added: Email: alex@u.nus.edu`.
1. Close and reopen the app before filling any details.<br>
   Expected: The same incomplete member loads with its email row still hidden; saved name, phone and address
   values are null.
1. Run `edit 1 e/alex.updated@u.nus.edu`, then `edit 1 e/alex@u.nus.edu`.<br>
   Expected: The heading changes to the updated email, then back to `alex@u.nus.edu`. No separate email row
   appears while the name is absent.
1. Run `find e/alex`, then `edit 1 n/Alex Tan`. Close and reopen again.<br>
   Expected: Alex Tan is the heading and `Email: alex@u.nus.edu` appears below the address row immediately after
   the edit and after restarting. Phone and address remain `Phone: -` and `Address: -`.
1. Run `edit 1 p/91234567`, then `edit 1 a/Kent Ridge`, and restart.<br>
   Expected: Exactly one complete member remains with rows `Phone: 91234567`, `Address: Kent Ridge` and
   `Email: alex@u.nus.edu`, in that order.
1. Repeat in a populated directory, searching for the target before each edit and checking its displayed index.<br>
   Expected: Only the intended member changes. Each successful edit clears the search and shows members in the
   selected status view.
1. Try `add e/new@u.nus.edu p/`, `edit 1 n/`, and a duplicate `add e/ALEX@U.NUS.EDU`.<br>
   Expected: Each is rejected without changing records, filters or saved data.
1. Add `add e/unnamed@u.nus.edu t/Publicity`. Search by its email, add/remove a tag, and delete it.<br>
   Expected: Missing details remain absent through tag changes; command feedback never shows Java optional wrappers.
1. Inspect named and unnamed cards at the supported resolutions and scaling settings.<br>
   Expected: Existing rows remain readable and the heading changes correctly after a name is supplied.
   Named and unnamed cards with short details have the same height and top spacing. Hiding the duplicate email
   row does not shift the heading, status, phone or address rows. Cards grow for wrapped addresses and additional
   tag rows. Names, phone numbers and emails, including email headings, stay on one line and use ellipses when
   they exceed the available width. Hover over a truncated field to read its full value. Individual tags stay on
   one line, with ellipses and full-text tooltips when needed; the status badge remains readable.

### Rejecting Unicode blank addresses in the command box

Use a rebuilt JAR with Java 25 in a disposable folder containing `data/nusocietydesk.json` with `{"persons":[]}`.
Run `add e/edit@u.nus.edu a/Kent Ridge`, select Active view, then run `find e/edit` and back up the saved JSON.

1. On Windows, copy a command with one actual U+3000 character using PowerShell, then paste and submit it:

   ```powershell
   ('add e/blank@u.nus.edu a/' + [char]0x3000) | Set-Clipboard
   ```

   Expected: `Addresses must contain at least one non-whitespace character and be at most 200 characters long.`
   The command stays editable with error styling. No member is added;
   the card, search, status view, and saved JSON remain unchanged.
1. Repeat with `[char]0x2003` for EM SPACE, then repeat both characters with the command prefix
   `edit 1 n/Changed Member a/`.<br>
   Expected: the same error, with neither the name nor `Kent Ridge` changed. Close and reopen the app to confirm
   that the original member and address remain saved.
1. Import a valid CSV with a quoted address cell containing only these spaces.<br>
   Expected: the member imports with `Address: -`, saves `"address": null`, and retains the missing address after restart.
1. Close the app, back up the JSON, and replace one saved address with `"\u3000"`, then relaunch.<br>
   Expected: a warning identifies the skipped member's original record position with the address error. Other
   valid members load; changes and saving are disabled and the original file is preserved. Close the app, repair
   the address to `null` or a valid value, and restart to restore normal saving.

These command-box and restart checks require manual execution; the JUnit tests cover the shared validation,
command-state preservation, CSV import, and protected storage behavior.

### Importing member directories

Use a disposable application folder in All view. Copy `docs/samples/members.csv` into it as `member contacts.csv`.
Keep a separate copy of the CSV for comparison. Start with neither `alex@u.nus.edu` nor `jamie@u.nus.edu` in the
directory, and confirm startup has not disabled saving. Run `list` to ensure the JSON file exists, then back it up.
For independent cases below, restore that starting state. Keep the app's launch folder fixed so relative paths resolve
to the prepared CSV files. Inspect the full feedback message for rejection details.
CSV record numbers count the header as 1 and the first member as 2; empty lines do not count.
Select `view all` before the following cases unless another status view is specified.

1. Test case: `import "member contacts.csv"`<br>
   Expected: Two members are appended after existing members, all members are displayed, and feedback states
   `Imported 2 member(s); rejected 0 record(s) from member contacts.csv.`
   Alex Tan has `active` status and the `exco`/`publicity` tags; Jamie Lee has `archived`
   status, displays `ARCHIVED`, and has no tags. The CSV is unchanged. Restart and confirm these records persist.

1. After the successful import above, run `find e/alex`, then import the same CSV again.<br>
   Expected: Feedback reports zero imports, two rejections, and a duplicate-email reason for records 2 and 3.
   It explains that record numbers include the header as record 1, blank lines are skipped, and a quoted multiline
   cell counts as one record. There is no hidden-member hint.
   Member records, search results, the status view, and saved JSON are unchanged.

1. Restore the starting directory, run `find e/alex`, select `view archived`, then import the original
   `member contacts.csv`.<br>
   Expected: Both members are imported and saved, Archived remains selected, and Jamie is visible while Alex is hidden.
   Feedback adds `1 imported member(s) are hidden by the current view. Use view all to see them.`
   Run `view all`: both imported members appear, confirming that the previous search was cleared.
1. Restore the starting directory and repeat the preceding case with `view active`.<br>
   Expected: Alex is visible and Jamie is hidden. Feedback reports one hidden imported member and suggests
   `view all`; existing members do not contribute to the hidden-import count.

1. In a fresh copy of the CSV, change the second member's status to `former`. Run the import in a fresh disposable
   directory, or first remove the previously imported example members.<br>
   Expected: Alex is imported and Jamie is skipped. Feedback reports one import and one rejection, with a status
   validation error for CSV record 3 and a note explaining how the header, blank lines, and quoted multiline cells
   are counted.
   Existing members and the source CSV are unchanged; all members are displayed.
   Restart and confirm only Alex was added.

1. In separate fresh copies of the original CSV, change Jamie's email to `jamie@gmail.com`, remove Jamie's status,
   change Jamie's email to `ALEX@U.NUS.EDU`, or remove/add a column in Jamie's record. Restore the starting directory
   before each import.<br>
   Expected: Alex is imported; Jamie is rejected as record 3 with the relevant validation or duplicate-email reason.
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

1. Test case: `import data/nusocietydesk.json`, using the app's configured JSON save file.<br>
   Expected: The source/save-path check rejects the import before parsing the file. Records, search results,
   and the original JSON remain unchanged.

1. Put a copy of the sample CSV alongside the application folder as `outside.csv`, then run
   `import ../outside.csv` while launching from the JAR's folder. Also try the file's absolute path and a CSV in
   a similarly named sibling folder, such as `app-backup` beside `app`.<br>
   Expected: Each import reports that CSV files must be inside the folder containing the JAR or a subfolder.
   Member records, search results, saved JSON, and the source files remain unchanged.
1. Launch the JAR from a different working directory. Import an absolute path inside the JAR's folder, then try
   an absolute path outside it. Restore the starting directory data between successful cases.<br>
   Expected: Only the inside source is accepted; the boundary follows the JAR location, not the launch folder.
1. Where symbolic links are supported, create a file link inside the application folder to `outside.csv`, and a
   directory link inside it to an outside folder containing a CSV. Try importing through each link.<br>
   Expected: Both imports are rejected without changing records, search results, or files. A link whose target
   remains inside the application folder is accepted. A link to the JSON save file is still rejected.

These GUI checks require manual execution. `ImportIntegrationTest` covers persistence and source protection;
`ImportCommandTest` checks the partial-import sample's documented counts.

### Recovering from a failed save

Use the current application build in a disposable folder on a filesystem that supports atomic replacement.
Use the default data-file location, `data/nusocietydesk.json`, and keep a separate backup of that file.
The following steps use a file manager and do not require a particular operating system or shell.

1. Launch in All view with at least two members, including one matching `find e/alex` and one that does not. Confirm startup
   has not disabled saving and `retry@u.nus.edu` is absent. Run `find e/alex` and note the displayed records.
1. Keep the app running. In a file manager, rename `data/nusocietydesk.json` to `data/nusocietydesk.saved.json`.
   Create a folder named `nusocietydesk.json` inside `data`, and place a text file named `keep.txt` in that folder.
   The nonempty folder prevents the app from replacing the destination with a JSON file.
1. In the app, run `add e/retry@u.nus.edu`.<br>
   Expected: Saving fails; feedback says records and the previous search filter were restored. The original search
   results remain visible. Do not run another command until the file path is restored; verify the missing member
   with `list` after restoration. No success confirmation is displayed.
1. Compare `data/nusocietydesk.saved.json` with the backup.<br>
   Expected: The original JSON is unchanged, the test folder and `keep.txt` still exist, and there is no leftover
   `.nusocietydesk-*.tmp` file in `data` (enable hidden-file display if necessary).
1. Remove only the test folder `data/nusocietydesk.json` and its `keep.txt`, then rename `data/nusocietydesk.saved.json`
   back to `data/nusocietydesk.json`. Restore this path before restarting the app.
   Run `list` to confirm that `retry@u.nus.edu` was not added, then run `find e/alex` to restore the test's search.
1. Retry `add e/retry@u.nus.edu`, then close and restart the app.<br>
   Expected: The add succeeds, all members are displayed, and exactly one new member survives restarting.
1. Run `find e/alex` again, make a fresh backup, and repeat the path-conflict steps with a valid CSV containing a new
   member email.<br>
   Expected: The previous records and filter are restored; the original JSON and source CSV remain unchanged.
   Remove the test folder and restore the original JSON filename before closing the app.

These GUI checks require manual execution. `AtomicFileWriterTest`, `PersistenceIntegrationTest`, and
`ImportIntegrationTest` inject failures without changing real user files.

### Combining search conditions

In a disposable application copy, start with an empty directory and add:

```text
add n/John Tan e/john.tan@u.nus.edu t/exco t/publicity
add n/John Lee e/john.lee@u.nus.edu t/exco
add n/Alice Tan e/alice.tan@u.nus.edu t/publicity
```

Run these searches in order; later searches must check the complete directory despite the earlier filter.

| Command | Expected result |
| --- | --- |
| `findall n/John n/Tan t/exco` | John Tan; count 1 |
| `findall t/exco t/publicity` | John Tan; count 1 |
| `findany t/exco t/publicity` | All three members, each once in insertion order; count 3 |
| `findany n/Alice e/john.lee` | John Lee, then Alice Tan; count 2 |
| `findall n/Nobody t/exco` | Empty list; count 0 |

Run `find n/Alice`, then try `findall John t/exco`, `findall n/John Tan` and `findany n/John p/abc`.
Each must report an error and leave Alice Tan displayed, with member records and saved JSON unchanged.

### Rejecting blank search values

Use a nonempty directory and run `list`. Note the displayed members, then paste each command below separately.
Each line contains an actual EM SPACE (U+2003):

```text
find n/ 
find  
find e/ 
findall n/  e/@u.nus.edu
findany e/@u.nus.edu n/ 
```

Expected: Each command reports "Invalid command format!" with its usage guidance and preserves the previous
results, member records and saved JSON. The combined searches must be rejected even though the email condition
matches existing members.

Repeat using IDEOGRAPHIC SPACE (U+3000), copying the character between these brackets: `[　]`.
Also try ordinary spaces and a mixture of ordinary and Unicode spaces as the complete value.
Finally, run `find e/@u.nus.edu`, `findall e/@u.nus.edu` and `findany e/@u.nus.edu`.
Expected: All three valid searches display every member in the selected status view.

### Finding members by name

Use a disposable application folder in All view and an empty directory (`clear` removes all members in that copy).
Run these setup commands one at a time:

```text
add n/Alice Tan e/namealice@u.nus.edu
add n/Bob Lee e/namebob@u.nus.edu
add n/Alison Tan e/namealison@u.nus.edu
```

1. Test cases: `find n/Alice`, `find n/aLiCe`, `find Alice` and `find n/ Alice `.<br>
   Expected: Only Alice Tan is displayed; the count is one. Case and surrounding whitespace do not affect matching.
1. Test cases: `find Alice Bob` and `find Bob ALICE`.<br>
   Expected: Alice Tan and Bob Lee are displayed; the count is two. Either keyword can match, in any order.
1. Test cases: `find n/Ali` and `find Ali`.<br>
   Expected: No members are displayed; the count is zero. Partial words match neither Alice nor Alison.
1. Run `find n/Alice`, then record the displayed result and make a copy of `data/nusocietydesk.json`.<br>
   Try `find n/Alice Bob`, `find n/Alice  Bob` and `find n/Alice Alice`.<br>
   Expected: Each reports "Prefixed name searches accept only one keyword. Use find Alice Bob to match names
   containing Alice OR Bob." Alice Tan remains the only displayed member, and the saved JSON matches the copy.
1. After running `find n/Alice`, try `find n/`, `find n/Alice n/Bob`, `find n/Alice p/9123` and
   `find Alice n/Bob`.<br>
   Expected: Each reports the relevant existing format, duplicate-prefix or multiple-field error.
   Alice Tan remains displayed, and member records and saved JSON remain unchanged.
1. Run `list`, then close and reopen the app.<br>
   Expected: All three records retain their names and email addresses. Searching has not changed member data.

Automated parser and logic tests also cover keywords separated by tabs, LF and CRLF line breaks, including the
error guidance and preservation of previous results, records and saved JSON. The GUI checks above are provided
for manual execution.

### Finding members by phone number

Use a disposable application folder in All view and an empty directory (`clear` removes all members in that copy).
Run these setup commands one at a time:

```text
add n/Alice Tan p/91234567 e/phonealice@u.nus.edu
add n/Bob Lee p/89123000 e/phonebob@u.nus.edu
add n/Carol Lim p/00145678 e/phonecarol@u.nus.edu
add n/9123 Member e/phonemissing@u.nus.edu
```

The `p/` prefix selects the phone field. Its value matches a consecutive sequence of digits anywhere in the
number, including the middle or end.

1. Test cases: `find p/9123` and `find p/ 9123 `.<br>
   Expected: Alice Tan and Bob Lee are displayed; the count is two. A substring can occur anywhere in the phone
   number, and surrounding whitespace is ignored. The member named 9123 Member has no phone and does not match.
1. Test cases: `find p/91234567` and `find p/2345`.<br>
   Expected: Only Alice Tan is displayed; the count is one. Both full numbers and consecutive digit fragments match.
1. Test cases: `find p/9` and `find p/89`.<br>
   Expected: The first displays Alice Tan and Bob Lee; the second displays only Bob Lee.
   Search values may contain fewer than the three digits required when storing a phone number.
1. Test case: `find p/001`.<br>
   Expected: Only Carol Lim is displayed; the count is one. Leading zeros are preserved in the search value.
1. Test cases: `find p/555` and `find p/923`.<br>
   Expected: Each produces an empty list and a count of zero. Digits must occur consecutively, in the given order.
1. Run `find 9123`, followed by `find p/9123`.<br>
   Expected: The first displays only 9123 Member because unprefixed searches consider names.
   The second displays Alice Tan and Bob Lee, searching the complete directory despite the earlier filter.
1. After running `find p/9123`, make a copy of `data/nusocietydesk.json` and try `find p/9123 1234`.<br>
   Expected: The error is "Phone searches accept only one substring. Search each substring in a separate find command."
   Alice Tan and Bob Lee remain displayed, and the saved JSON matches the copy.
1. Test cases: `find p/abc`, `find p/91a3`, `find p/+659123`, `find p/9123-4567` and `find p/９１２３`.<br>
   Expected: Each reports "Phone searches accept digits (0-9) only. Example: find p/9123."
   The previous results, member records and saved JSON remain unchanged.
1. Test cases: `find p/`, `find p/9123 p/4567`, `find p/9123 n/Alice` and `find Alice p/9123`.<br>
   Expected: Each reports the relevant existing format, duplicate-prefix or multiple-field error.
   The previous results, member records and saved JSON remain unchanged.
1. Run `list`, then close and reopen the app.<br>
   Expected: All four records retain their contact details, including Carol's leading zeros and the missing phone
   for 9123 Member. Searching has not changed member data.

Automated parser tests also cover phone substrings and prefixes separated by tabs or line breaks. Logic tests
verify that invalid phone values preserve member records and the previous results. The GUI checks above are
provided for manual execution.

### Finding members by tag

Use a disposable application folder in All view and an empty directory (`clear` removes all members in that copy).
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

### Clearing the entire directory

Use a disposable application folder. Any setup step elsewhere in this guide that runs `clear` also requires
choosing **Delete all members** in its confirmation dialog.

1. Prepare an active member and an archived member with different emails, then search for the active member
   and select Active view. Keep a copy of `data/nusocietydesk.json` for comparison.
1. Submit `clear 1`, `clear all`, `clear member`, and `clear extra_text`.<br>
   Expected: each shows `Invalid command format!` followed by the `clear` usage. No dialog opens, input remains
   editable with error styling, and records, search, view, and the file are unchanged.
1. Submit `clear`.<br>
   Expected: a confirmation explains that all members, including archived members and those outside the search,
   will be permanently deleted. It also warns that existing and imported members will both be deleted and
   suggests `delete INDEX`. Buttons are **Cancel** and **Delete all members**, with Cancel initially focused
   and the default. Records and the file remain unchanged while the dialog is open.
1. Cancel using each of the following, submitting `clear` again between attempts: click Cancel, press Enter
   without moving focus, press Escape, and close the dialog with its window close button.<br>
   Expected: `Clear cancelled.` appears, the command box clears, and records, search, view, and the file are
   unchanged. The prior `clear` submission can still be recalled with the Up arrow.
1. Submit `clear` and use Tab to focus **Delete all members**, then activate it with the keyboard.<br>
   Expected: `Member directory has been cleared!` appears. Both active and archived members are deleted,
   even though only one was displayed. The selected view remains Active and the saved directory is empty.
1. Restart the app.<br>
   Expected: the directory stays empty; sample members are not loaded from a valid empty data file.
1. Submit `clear` with the directory already empty.<br>
   Expected: confirmation is still required. Cancel leaves the directory empty.
1. Add a disposable member and make saving fail using the obstacle described in
   [Recovering from a failed save](#recovering-from-a-failed-save). Submit `clear` and confirm deletion.<br>
   Expected: a saving error replaces success feedback. The records, search, view, and original saved data are
   preserved. A retry requires confirmation again.

### Deleting a person

1. Deleting a person while all persons are being shown

   1. Prerequisites: Select All view, then list all persons using `list`, with multiple persons in the list.

   1. Test case: `delete 1`<br>
      Expected: The first contact is deleted from the list. The status message shows the deleted contact's details.

   1. Test case: `delete 0`<br>
      Expected: No person is deleted. The status message shows error details.

   1. Other incorrect delete commands to try: `delete`, `delete x`, `...` (where x is larger than the list size)<br>
      Expected: Similar to previous.

### Tag identity and editing

1. In a disposable application folder, confirm saving is enabled and `tagtest@u.nus.edu` is absent.
   Add a person with case variants of a tag.

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
      Expected: An error indicates that the member already has the tag. The member's tags and the current
      search filter remain unchanged.

   1. Test case: `untag INDEX t/PUBLICITY`<br>
      Expected: Only `exco` and `volunteer` remain.

   1. Repeat `untag INDEX t/publicity`.<br>
      Expected: The command is rejected with an error indicating the member does not have the tag. The member's tags remain unchanged.

   1. Close and reopen the app.<br>
      Expected: The member still has only `exco` and `volunteer`.

   1. Remove the remaining tags with `untag INDEX t/Exco` and `untag INDEX t/Volunteer`.<br>
      Expected: The member has no tags and retains all contact details.

1. Check displayed indices and invalid input.

   1. Run `find Tag`, note Tag Test's displayed index, and run `tag INDEX t/Volunteer` using that index.<br>
      Expected: Only Tag Test gains the tag; the search clears and the selected status view remains.

   1. Run `find Tag` again and run `untag INDEX t/Volunteer` using the new displayed index.<br>
      Expected: Only Tag Test loses the tag; the search clears and the selected status view remains.

   1. For each of `tag` and `untag`, try arguments `0 t/Volunteer`, `1 2 t/Volunteer`, `1`, `1 t/`,
      `1 t/Publicity Team`, `1 t/Volunteer t/Exco`, and an index larger than the displayed list.<br>
      Expected: Each command is rejected; records and the current filter remain unchanged.

1. Load tags from an existing saved record.

   1. Close the app in the disposable test directory and back up `data/nusocietydesk.json`.
      Set Tag Test's saved tags to `["Publicity", "publicity", "Exco"]`, leaving all other JSON unchanged.

   1. Relaunch the app.<br>
      Expected: The card displays only `exco` and `publicity`.

   1. Execute `tag INDEX t/Exco` for Tag Test's displayed index and inspect the saved record.<br>
      Expected: It contains exactly two tag names, `exco` and `publicity`, regardless of their order in the file.

### Saving data

1. Dealing with invalid data files

   1. In a disposable test directory, run `add n/Alex Tan e/alex@u.nus.edu` to create `data/nusocietydesk.json`,
      close the app, and back it up.
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

1. Keep read-only commands available when saving fails (issue #140).

   1. Use a rebuilt JAR with Java 25 in a disposable folder. Run `clear`,
      `add n/Alex Tan e/alex@u.nus.edu`, `add n/Bob Lee e/bob@u.nus.edu`, then `find e/alex`.
   1. Keep the app open and set `data/nusocietydesk.json` to read-only using Windows file properties.
      Leave the enclosing folder writable.
   1. Run `find e/bob`, `findall e/bob`, and `findany e/bob`, restoring `find e/alex` before each.<br>
      Expected: each Bob search succeeds, shows Bob instead of Alex, and leaves the data file unchanged.
   1. Run `list`, `view active`, `view archived`, and `view all`.<br>
      Expected: listing clears the search within the selected status view. View changes work without saving.
   1. Run `help`, close Help, and press F1 while the command box has focus.<br>
      Expected: both routes open the same Help window.
   1. Run `add e/blocked@u.nus.edu`.<br>
      Expected: a saving error appears. Records, the previous search filter, status view, and saved data are preserved.
      Searching and listing still work after the failed change.
   1. Run `exit` with Help open.<br>
      Expected: the main and Help windows close without a saving error.
   1. Remove the read-only attribute and relaunch. Add a member, restart, and confirm the member persists.
   1. Repeat with a nonempty directory obstacle: while the app is open, rename the JSON file to
      `nusocietydesk.saved.json`, create a directory named `nusocietydesk.json`, and put `keep.txt` inside it.<br>
      Expected: the same read-only commands work and member changes roll back on save failure. The backup and
      `keep.txt` remain unchanged. Restore the original JSON path before restarting.

### Reading long contact details in a narrow member list

Use Java 25 and a rebuilt JAR in a disposable writable folder. These native-window and display-scaling checks
require manual execution; their execution status for this change is **not run**.

1. Add a member with maximum-length contact values using PowerShell to copy the command, then paste and submit:

   ```powershell
   ('add e/' + ('w' * 64) + '@u.nus.edu n/' + ('W' * 127) + ' p/' + ('9' * 15) +
       ' a/' + ('W' * 200) + ' t/' + ('w' * 30)) | Set-Clipboard
   ```

   Expected: success. At the configured minimum window width, the address wraps completely. The name, phone and
   email stay on one line, with ellipses if they exceed the available width; hover to read their full values.
   Tag badges flow onto additional rows when needed; an individual badge can show an ellipsis with a full-text
   tooltip. The index and status badge remain visible. No member-list horizontal scrollbar appears. Resizing
   wide and narrow again recalculates address wrapping, tag rows and card height.
1. Add an unnamed member with a different 64-character email local-part and a long address.<br>
   Expected: the email heading stays on one line and uses an ellipsis if necessary, with its full value available
   in a tooltip. The index stays beside the heading and the separate email row is hidden. Use
   `edit INDEX n/Named Member` with its displayed index: the heading becomes a single-line name and the separate
   email row appears, also on one line with an ellipsis if necessary. Repeat with an archived member.
1. Scroll through enough members to reuse list cells, select a long card, and reach the final member.<br>
   Expected: wrapped address text and all tag rows stay within each card without overlap or clipping, including
   after selection. Names, phone numbers and emails continue using single-line ellipsis truncation when needed.
   A card taller than the viewport remains readable by vertical scrolling, and following members are reachable.
1. Repeat at 1920 by 1080 with 100% and 125% scaling and 1280 by 720 with 150% scaling.<br>
   Expected: all contact text remains accessible. Restart and confirm that full values and window preferences
   persist without storing display line breaks. Command syntax, validation and feedback remain unchanged.

### Adding tags across wrapped rows

Use a rebuilt JAR with Java 25 in a disposable writable folder. Before launch, create
`data/nusocietydesk.json` containing `{"persons":[]}` so the directory starts empty instead of loading samples.
Packaged verification for this change: **passed** for eight `tag` submissions followed by eight `untag`
submissions on the last member in an 18-member list, without scrolling between commands; the rendering was
also inspected. The broader manual scenarios below are **not run**. The JUnit regression submits the same
command path in a shown JavaFX window.

1. Run `add e/taglayout@u.nus.edu`. The only member is at index 1.
1. Add the following tags one command at a time, without scrolling or resizing between commands:

   ```text
   tag 1 t/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
   tag 1 t/bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
   tag 1 t/cccccccccccccccccccccccccccccc
   tag 1 t/dddddddddddddddddddddddddddddd
   tag 1 t/eeeeeeeeeeeeeeeeeeeeeeeeeeeeee
   tag 1 t/ffffffffffffffffffffffffffffff
   ```

   Expected: each command adds one tag with the normal confirmation. As the tags wrap into additional rows,
   the card immediately grows and `Phone: -` and `Address: -` stay below all tags. There is no overlap to repair
   by scrolling. Long individual tags retain ellipses and full-text tooltips when necessary.
1. Remove those tags one at a time using `untag 1 t/TAG`, replacing `TAG` with each exact value above.<br>
   Expected: the card shrinks as tag rows disappear and returns to its original minimum content height when
   the final tag is removed. The email heading, contact placeholders and status remain correct.
1. Repeat with a named member, an archived member, and a member near the bottom of a populated list.<br>
   Expected: no overlap immediately after each command or after scrolling away and back, selecting the card,
   resizing the window or restarting. Addresses continue wrapping in full and tag badges flow onto additional
   rows when needed. Names, phone numbers and emails remain on one line, with ellipses and full-text tooltips
   when necessary. No member-list horizontal scrollbar appears.

### Contact and tag length validation

Use Java 25 and a rebuilt app in a disposable writable folder with normal saving enabled. The limits are
inclusive: name 127 characters, phone 15 digits, address 200 characters, and each tag 30 characters.
Manual execution status for this change: **not run**. Automated JavaFX tests cover submission feedback,
editable failed input, retry, and narrow-card layout; the steps below also check the visible rendering and tooltips.

1. On Windows, copy a command containing maximum-length values with PowerShell, then paste and submit it:

   ```powershell
   ('add e/length.test@u.nus.edu n/' + ('W' * 127) + ' p/' + ('9' * 15) +
       ' a/' + ('W' * 200) + ' t/' + ('W' * 30)) | Set-Clipboard
   ```

   Expected: success. Resize the window to its minimum width and a wider width. The name and tags stay within
   the card, with ellipses where needed; hover to read their full values. The address wraps within the card,
   other members remain reachable, the status badge is visible, and result text wraps within its pane.
   Restart and confirm all values persist without being shortened; the tag is stored in lowercase.
1. Select Active view and search for this member using `find e/length.test`. Back up the JSON file.
   Try each field one character over its maximum in otherwise valid `add` and `edit INDEX` commands:

   | Field | Rejected length | Expected feedback |
   | --- | --- | --- |
   | `n/` | 128 | Names may contain letters A-Z/a-z, digits, spaces and , ( ) / . @ - ' only.<br>Names must contain at least one letter or digit and be at most 127 characters long |
   | `p/` | 16 | Phone numbers should only contain digits, and should be between 3 and 15 digits long |
   | `a/` | 201 | Addresses must contain at least one non-whitespace character and be at most 200 characters long. |
   | `t/` | 31 | Tag names should be alphanumeric and between 1 and 30 characters long |

   Use `tag INDEX t/TAG` and `untag INDEX t/TAG` for the tag checks; `edit` rejects every tag argument separately.
   Expected: the field's constraints replace the previous result; the command remains editable with error styling.
   Member records, search results, status view, and saved JSON remain unchanged. Correct each command to its
   maximum length and retry; it succeeds. Repeat the rejected cases with 10,000 characters to check graceful failure.
1. Search using `find p/9`, `findall p/99`, and `findany t/` followed by the valid 30-character tag.<br>
   Expected: the member matches. Tag searches over 30 characters are rejected with the tag constraints.
1. Import a CSV with a boundary-length member and four other records, each exceeding one field's maximum.<br>
   Expected: the boundary member imports and persists; each rejected record has its original CSV record number
   and field constraints in the feedback. With only rejected records, no saving or state changes occur.
1. Close the app and, in the disposable JSON file, give one member an overlong field or tag. Relaunch.<br>
   Expected: valid members load and the warning identifies the rejected member's original record position and
   constraints. The original file is preserved; changes and saving are disabled. Repair the value and restart
   to restore normal saving. Repeat for each of the four fields.

### Name punctuation

Use Java 25 and a rebuilt app in a disposable writable folder with normal saving enabled.
Manual execution status for this change: **not run**. Automated tests cover command submission, persistence,
searches, CSV and JSON entry paths, and exact JavaFX name-label and tooltip values.

1. Run `add e/EMAIL n/NAME` for each row below, replacing the placeholders with the literal values:

   | EMAIL | NAME |
   | --- | --- |
   | `hyphen@u.nus.edu` | `Anne-Marie Tan` |
   | `apostrophe@u.nus.edu` | `O'Connor Tan` |
   | `initials@u.nus.edu` | `J. P. Tan` |
   | `son@u.nus.edu` | `Ravi S/O Kumar` |
   | `daughter@u.nus.edu` | `Devi D/O Kumar` |
   | `comma@u.nus.edu` | `Tan, Alex` |
   | `parentheses@u.nus.edu` | `Tan (Alex)` |
   | `alias@u.nus.edu` | `Tan @ Alex` |

   Expected: every command succeeds; the displayed name and its tooltip preserve the entered spelling.
   Resize to the minimum window width and hover over ellipsized headings to check the complete tooltip.
1. Run `edit INDEX n/NAME` on one member using each name from the table, then restore its original name.<br>
   Expected: each edit succeeds and preserves punctuation. `find n/O'CONNOR`, `findall n/S/O`, and
   `findany n/D/O` each find the corresponding member; `find n/Connor` does not match `O'Connor`.
1. Restart the app.<br>
   Expected: all eight names persist exactly, no invalid-record warning appears, and normal saving remains enabled.
1. Save the following CSV beside the application JAR as `punctuated-names.csv`, then run
   `import punctuated-names.csv`:

   ```csv
   name,phone,email,address,tags,status
   "Tan, Alex",,csv.comma@u.nus.edu,,,active
   Ravi S/O Kumar,,csv.son@u.nus.edu,,,active
   Tan @ Alex,,csv.alias@u.nus.edu,,,active
   ```

   Expected: all three records import without rejection; their names retain their punctuation after restarting.
   The source CSV remains unchanged.
1. Try `add e/rejected@u.nus.edu n/---`, `edit 1 n/()`, and `edit 1 n/Alex!`.<br>
   Expected: the name constraints appear, input remains editable, and existing records and saved JSON remain unchanged.

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

### Email local-part length validation

Use a rebuilt JAR with Java 25 in a disposable writable folder. Start with an empty directory and confirm that
normal saving is enabled. The boundary commands below contain 64 and 65 `a` characters before `@`, respectively.

1. Run the following command, then restart the app:

   ```text
   add e/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa@u.nus.edu
   ```

   Expected: the member is added and survives restarting with the same email.
1. Select Active view and search for the member. Back up `data/nusocietydesk.json`, then run:

   ```text
   add e/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa@u.nus.edu
   ```

   Expected: `Email local-part must contain at most 64 characters; received 65.` replaces the previous result.
   The full command stays editable with error styling. Records, search, status view, and saved data are unchanged.
1. Repeat with `edit INDEX e/` followed by the same overlong email, using the member's displayed index.<br>
   Expected: the same rejection with no changes. Up/Down can recall the failed command normally.
1. For the original reported payload on Windows, use PowerShell to copy the command, then paste and submit it:

   ```powershell
   ('add e/' + ('a.' * 2000) + 'a@u.nus.edu') | Set-Clipboard
   ```

   Expected: `Email local-part must contain at most 64 characters; received 4001.` appears with no uncaught error.
   Run `add e/after@u.nus.edu`; it succeeds normally.
1. Import a CSV containing a valid record and a record with a 65-character local-part.<br>
   Expected: only the valid record imports; the rejected record's position and length error are reported.
   Repeat with only rejected records: records, filters, and saved data remain unchanged.
1. Close the app and, in the disposable JSON file, give one record an overlong email. Relaunch.<br>
   Expected: other valid records load; the warning identifies the rejected record and length. The original file
   is preserved and member changes are disabled. Repair the file and restart to restore normal saving.

Automated tests cover normalized length boundaries, the original payload and larger malformed candidates,
validation priority, command-state and file preservation, JavaFX feedback/history and retry, CSV record errors,
and protected JSON recovery with boundary-email round trips. The packaged-JAR steps above also check the
Windows JavaFX submission path.

### Email identity

1. On a fresh launch with no data file, run `find Morgan`.<br>
   Expected: both sample members named Morgan Lee appear. They share a phone number but have different emails.
1. In a disposable directory with `data/nusocietydesk.json` containing `{"persons":[]}`, run:
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
