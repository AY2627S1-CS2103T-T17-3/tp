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

`Email` validates the existing general email format and stores the value in lowercase using `Locale.ROOT`.
It does not trim whitespace; model values and saved strings containing surrounding whitespace are invalid.
Command parsing still trims surrounding argument whitespace. Addresses do not need an `@u.nus.edu` domain,
and different local-part aliases remain distinct.

`Person.isSamePerson` compares canonical emails only. Members can share a name or phone number when their emails
differ. `UniquePersonList` uses this identity rule for addition, replacement, and bulk replacement, while
`Person.equals` still compares every field. Add and edit detect email conflicts across the complete directory,
including members outside the displayed filter, and identify the existing member by name and email without a
displayed index. Re-entering one's own email or changing only its case remains valid; changing an email releases
the previous one for reuse.

`EditCommand` preserves the existing tag set and its descriptor contains only name, phone, email, and address.
`EditCommandParser` still tokenizes `t/` so it can reject tag arguments explicitly instead of including them in another
field's value. Dedicated tag commands should use the same `Tag` identity rule when adding or removing tags.


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

The JSON schema is unchanged: each person retains an `email` string. Loading applies the model's email validation
and rejects the entire file on the first invalid record or duplicate canonical email. Record errors use one-based
positions in the saved `persons` array; a duplicate reports both positions, for example
`Record 2: email "alex@example.com" duplicates record 1.` `JsonAddressBookStorage` also rejects malformed
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

1. Secretary requests to find members using one search field: name keywords or a single phone number substring.
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

* 1c. Secretary provides multiple phone number substrings in one search.

  * 1c1. NUSocietyDesk shows an error message explaining that a phone search accepts only one substring.

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

   1. Test case: `add n/Tag Test p/91234567 e/tagtest@example.com a/Kent Ridge t/Publicity t/publicity t/Exco`<br>
      Expected: The card displays exactly two tags, `exco` and `publicity`, in lowercase.

   1. Run `list` and note the displayed index of Tag Test. In the following commands, replace `INDEX` with that index.

   1. Test case: `edit INDEX p/98765432`<br>
      Expected: The phone number changes and both tags remain.

   1. Test cases: `edit INDEX t/Publicity`, `edit INDEX t/`, and `edit INDEX a/New Address t/Publicity`<br>
      Expected: Each command is rejected with guidance to use `tagadd` or `tagremove`. All person details remain
      unchanged. These dedicated commands are being implemented separately and are not available in this change.

1. Load tags from an existing saved record.

   1. Close the app in a disposable test directory. Set Tag Test's saved tags to `["Publicity", "publicity", "Exco"]`.

   1. Relaunch the app.<br>
      Expected: The card displays only `exco` and `publicity`.

   1. Execute `list` and inspect the saved record.<br>
      Expected: It contains exactly two tag names, `exco` and `publicity`, regardless of their order in the file.

### Saving data

1. Dealing with invalid data files

   1. In a disposable test directory, run `list` to create `data/addressbook.json`, close the app, and back it up.
   1. Give two saved records the emails `Alex@Example.com` and `alex@example.com`, then relaunch.<br>
      Expected: the normal window opens with an empty member list. Loading leaves the invalid file unchanged.
      Storage reports that record 2 duplicates record 1, but the UI does not display those record positions.
   1. Correct the second email and relaunch.<br>
      Expected: the records load with lowercase emails. The file remains unchanged until a successful command,
      such as `list`, saves it.
   1. Repeat with malformed JSON, a null record, or an email with surrounding whitespace.<br>
      Expected: each file opens an empty member list. Restore the backup before running commands, which could
      overwrite the invalid file. A missing file starts with the sample members instead.

### Email identity

1. On a fresh launch with no data file, run `find Morgan`.<br>
   Expected: both sample members named Morgan Lee appear. They share a phone number but have different emails.
1. In a disposable directory with `data/addressbook.json` containing `{"persons":[]}`, run:
   * `add n/Alex Tan p/91234567 e/Alex.One@Example.com a/Kent Ridge`
   * `add n/Alex Tan p/91234567 e/alex.two@example.com a/Clementi`
   Expected: both succeed and show lowercase emails.
1. Run `edit 2 e/ALEX.ONE@EXAMPLE.COM`.<br>
   Expected: `Email "alex.one@example.com" is already used by "Alex Tan". No changes were made.`
1. Run `edit 1 n/Alexander Tan e/ALEX.ONE@EXAMPLE.COM`, then `find Alex`, then
   `edit 1 e/alex.one@example.com`.<br>
   Expected: the rename succeeds; `find Alex` shows only Alex Tan. The final edit fails because that email
   belongs to Alexander Tan outside the displayed filter.
1. Run `list`, then `edit 1 e/alex.new@example.com`, then
   `add n/Alex Tan p/91234567 e/alex.one@example.com a/Bukit Timah`.<br>
   Expected: both commands succeed. Restart to confirm the saved email changes.
