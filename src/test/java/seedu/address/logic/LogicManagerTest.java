package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX;
import static seedu.address.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static seedu.address.logic.commands.CommandTestUtil.ADDRESS_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.EMAIL_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.PHONE_DESC_AMY;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TAG;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.AMY;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.EditCommand;
import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.TagAddCommand;
import seedu.address.logic.commands.TagRemoveCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.model.person.Status;
import seedu.address.model.tag.Tag;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

public class LogicManagerTest {
    private static final IOException DUMMY_IO_EXCEPTION = new IOException("dummy IO exception");
    private static final IOException DUMMY_AD_EXCEPTION = new AccessDeniedException("dummy access denied exception");

    @TempDir
    public Path temporaryFolder;

    private Model model = new ModelManager();
    private Logic logic;

    @BeforeEach
    public void setUp() {
        JsonAddressBookStorage addressBookStorage =
                new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"));
        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"));
        StorageManager storage = new StorageManager(addressBookStorage, userPrefsStorage);
        logic = new LogicManager(model, storage);
    }

    @Test
    public void execute_invalidCommandFormat_throwsParseException() {
        String invalidCommand = "uicfhmowqewca";
        assertParseException(invalidCommand, MESSAGE_UNKNOWN_COMMAND);
    }

    @Test
    public void execute_commandExecutionError_throwsCommandException() {
        String deleteCommand = "delete 9";
        assertCommandException(deleteCommand, MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void execute_validCommand_success() throws Exception {
        String listCommand = ListCommand.COMMAND_WORD;
        assertCommandSuccess(listCommand, ListCommand.MESSAGE_SUCCESS, model);
    }

    @Test
    public void execute_archiveAndActivate_persistsStatus() throws Exception {
        Person person = new PersonBuilder().withEmail("member@u.nus.edu").build();
        model.addPerson(person);
        JsonAddressBookStorage savedData = new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"));

        logic.execute("archive 1");
        assertEquals(Status.ARCHIVED, model.getFilteredPersonList().get(0).getStatus());
        assertEquals(Status.ARCHIVED, savedData.readAddressBook().orElseThrow().getPersonList().get(0).getStatus());

        logic.execute("activate 1");
        assertEquals(Status.ACTIVE, model.getFilteredPersonList().get(0).getStatus());
        assertEquals(Status.ACTIVE, savedData.readAddressBook().orElseThrow().getPersonList().get(0).getStatus());
    }

    @Test
    public void execute_emailSearch_displaysMatchesWithoutChangingRecords() throws Exception {
        Person matchingPerson = new PersonBuilder().withName("Morgan Lee").withEmail("alice@u.nus.edu").build();
        Person otherPerson = new PersonBuilder().withName("Alice Tan").withEmail("bob@u.nus.edu").build();
        model.addPerson(matchingPerson);
        model.addPerson(otherPerson);

        CommandResult result = logic.execute("find e/ALICE");

        assertEquals(String.format(Messages.MESSAGE_PERSONS_LISTED_OVERVIEW, 1), result.getFeedbackToUser());
        assertEquals(List.of(matchingPerson), logic.getFilteredPersonList());
        assertEquals(List.of(matchingPerson, otherPerson), model.getAddressBook().getPersonList());
        ReadOnlyAddressBook savedAddressBook = new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"))
                .readAddressBook().orElseThrow();
        assertEquals(model.getAddressBook(), savedAddressBook);
    }

    @Test
    public void execute_nameSearch_displaysMatchesWithoutChangingRecords() throws Exception {
        Person alice = new PersonBuilder().withName("Alice Tan").withEmail("alice@u.nus.edu").build();
        Person bob = new PersonBuilder().withName("Bob Lee").withEmail("bob@u.nus.edu").build();
        model.addPerson(alice);
        model.addPerson(bob);

        CommandResult result = logic.execute("find n/aLiCe");
        assertEquals(String.format(Messages.MESSAGE_PERSONS_LISTED_OVERVIEW, 1), result.getFeedbackToUser());
        assertEquals(List.of(alice), logic.getFilteredPersonList());

        result = logic.execute("find ALICE Bob");
        assertEquals(String.format(Messages.MESSAGE_PERSONS_LISTED_OVERVIEW, 2), result.getFeedbackToUser());
        assertEquals(List.of(alice, bob), logic.getFilteredPersonList());
        assertEquals(List.of(alice, bob), model.getAddressBook().getPersonList());
        ReadOnlyAddressBook savedAddressBook = new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"))
                .readAddressBook().orElseThrow();
        assertEquals(model.getAddressBook(), savedAddressBook);
    }

    @Test
    public void execute_multiplePrefixedNameKeywords_preservesRecordsFilterAndSavedData() throws Exception {
        Person alice = new PersonBuilder().withName("Alice Tan").withEmail("alice@u.nus.edu").build();
        Person bob = new PersonBuilder().withName("Bob Lee").withEmail("bob@u.nus.edu").build();
        model.addPerson(alice);
        model.addPerson(bob);
        logic.execute("find n/Alice");
        assertEquals(List.of(alice), logic.getFilteredPersonList());
        Path savedFile = temporaryFolder.resolve("addressBook.json");
        String savedData = Files.readString(savedFile);

        for (String command : List.of("find n/Alice Bob", "find n/Alice  Bob", "find n/Alice\tBob",
                "find n/Alice\nBob", "find n/Alice\r\nBob", "find n/Alice Alice", "find n/ Alice \tBob \n")) {
            assertThrows(ParseException.class, FindCommand.MESSAGE_MULTIPLE_NAME_KEYWORDS, () ->
                    logic.execute(command));
            assertEquals(List.of(alice), logic.getFilteredPersonList());
            assertEquals(List.of(alice, bob), model.getAddressBook().getPersonList());
            assertEquals(savedData, Files.readString(savedFile));
        }
    }

    @Test
    public void execute_invalidTagSearch_preservesRecordsFilterAndSavedData() throws Exception {
        Person matchingPerson = new PersonBuilder().withEmail("alice@u.nus.edu").withTags("Publicity").build();
        Person otherPerson = new PersonBuilder().withEmail("bob@u.nus.edu").withTags("exco").build();
        model.addPerson(matchingPerson);
        model.addPerson(otherPerson);
        CommandResult result = logic.execute("find t/PUBLICITY");
        assertEquals(String.format(Messages.MESSAGE_PERSONS_LISTED_OVERVIEW, 1), result.getFeedbackToUser());
        assertEquals(List.of(matchingPerson), logic.getFilteredPersonList());
        Path savedFile = temporaryFolder.resolve("addressBook.json");
        String savedData = Files.readString(savedFile);

        for (String command : List.of("find t/", "find t/publicity exco", "find t/publicity t/exco",
                "find t/publicity n/Amy", "find p/9123 t/publicity", "find t/publicity e/alice",
                "find t/publicity!", "find Amy t/publicity", "find N/", "find P/9123", "find E/alice",
                "find T/publicity", "find T/ E/", "find n/Amy T/publicity", "find G/", "find F/value",
                "find H/", "find g/value", "find name/Amy", "find n/Amy G/value", "find t/publicity h/value")) {
            assertThrows(ParseException.class, () -> logic.execute(command));
            assertEquals(List.of(matchingPerson), logic.getFilteredPersonList());
            assertEquals(List.of(matchingPerson, otherPerson), model.getAddressBook().getPersonList());
            assertEquals(savedData, Files.readString(savedFile));
        }
    }

    @Test
    public void execute_invalidPhoneSearch_preservesRecordsAndFilter() throws Exception {
        Person matchingPerson = new PersonBuilder().withPhone("91234567").withEmail("alice@u.nus.edu").build();
        Person otherPerson = new PersonBuilder().withPhone("87654321").withEmail("bob@u.nus.edu").build();
        model.addPerson(matchingPerson);
        model.addPerson(otherPerson);
        logic.execute("find p/9123");
        assertEquals(List.of(matchingPerson), logic.getFilteredPersonList());

        for (String substring : List.of("abc", "91a3", "+659123", "9123-4567", "９１２３", "٩١٢٣")) {
            assertThrows(ParseException.class, FindCommand.MESSAGE_INVALID_PHONE_SUBSTRING, () ->
                    logic.execute("find p/" + substring));
            assertEquals(List.of(matchingPerson), logic.getFilteredPersonList());
            assertEquals(List.of(matchingPerson, otherPerson), model.getAddressBook().getPersonList());
        }
    }

    @Test
    public void execute_invalidEmailSearch_preservesRecordsAndFilter() throws Exception {
        model.addPerson(new PersonBuilder().withEmail("alice@u.nus.edu").build());
        model.addPerson(new PersonBuilder().withEmail("bob@u.nus.edu").build());
        logic.execute("find e/alice");
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.updateFilteredPersonList(person -> person.getEmail().value.equals("alice@u.nus.edu"));

        assertCommandFailure("find e/alice bob", ParseException.class,
                FindCommand.MESSAGE_MULTIPLE_EMAIL_SUBSTRINGS, expectedModel);
        assertCommandFailure("find e/alice n/Amy", ParseException.class,
                FindCommand.MESSAGE_MULTIPLE_FIELDS, expectedModel);
        assertCommandFailure("find e/", ParseException.class,
                String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE), expectedModel);
    }

    @Test
    public void execute_editWithTagArguments_throwsParseExceptionAndPreservesPerson() {
        model.addPerson(new PersonBuilder(AMY).withTags("Publicity", "Exco").build());

        assertParseException("edit 1 t/Publicity", EditCommand.MESSAGE_TAG_EDIT_NOT_SUPPORTED);
        assertParseException("edit 1 t/", EditCommand.MESSAGE_TAG_EDIT_NOT_SUPPORTED);
        assertParseException("edit 1 a/Kent Ridge t/Publicity", EditCommand.MESSAGE_TAG_EDIT_NOT_SUPPORTED);
        assertParseException("edit 1 t/Publicity a/Kent Ridge", EditCommand.MESSAGE_TAG_EDIT_NOT_SUPPORTED);
    }

    @Test
    public void execute_tagAdd_savesUpdatedTags() throws Exception {
        Person person = new PersonBuilder(AMY).withTags("Publicity", "Exco").build();
        model.addPerson(person);
        Person expectedPerson = new PersonBuilder(person).withTags("Publicity", "Exco", "Volunteer").build();
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.setPerson(person, expectedPerson);
        String expectedMessage = String.format(TagAddCommand.MESSAGE_SUCCESS,
                new Tag("Volunteer"), Messages.formatProvidedDetails(expectedPerson));

        assertCommandSuccess("tag 1 t/Volunteer", expectedMessage, expectedModel);
        assertCommandSuccess("tag 1 t/VOLUNTEER", expectedMessage, expectedModel);
        ReadOnlyAddressBook savedAddressBook = new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"))
                .readAddressBook().orElseThrow();
        assertEquals(expectedModel.getAddressBook(), savedAddressBook);
    }

    @Test
    public void execute_tagRemove_savesUpdatedTags() throws Exception {
        Person person = new PersonBuilder(AMY).withTags("Publicity", "Exco").build();
        model.addPerson(person);
        Person expectedPerson = new PersonBuilder(person).withTags("Exco").build();
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.setPerson(person, expectedPerson);
        String expectedMessage = String.format(TagRemoveCommand.MESSAGE_SUCCESS,
                new Tag("publicity"), Messages.formatProvidedDetails(expectedPerson));

        assertCommandSuccess("untag 1 t/publicity", expectedMessage, expectedModel);
        assertCommandSuccess("untag 1 t/PUBLICITY", expectedMessage, expectedModel);
        ReadOnlyAddressBook savedAddressBook = new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"))
                .readAddressBook().orElseThrow();
        assertEquals(expectedModel.getAddressBook(), savedAddressBook);
    }

    @Test
    public void execute_multipleTags_throwsParseExceptionAndPreservesPerson() {
        model.addPerson(new PersonBuilder(AMY).withTags("Publicity", "Exco").build());
        String expectedMessage = Messages.getErrorMessageForDuplicatePrefixes(PREFIX_TAG);
        assertParseException("tag 1 t/Volunteer t/Member", expectedMessage);
        assertParseException("untag 1 t/Publicity t/Exco", expectedMessage);
    }

    @Test
    public void execute_storageThrowsIoException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_IO_EXCEPTION, String.format(
                LogicManager.FILE_OPS_ERROR_FORMAT, DUMMY_IO_EXCEPTION.getMessage()));
    }

    @Test
    public void execute_storageThrowsAdException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_AD_EXCEPTION, String.format(
                LogicManager.FILE_OPS_PERMISSION_ERROR_FORMAT, DUMMY_AD_EXCEPTION.getMessage()));
    }

    @Test
    public void getFilteredPersonList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> logic.getFilteredPersonList().remove(0));
    }

    /**
     * Executes the command and confirms that
     * - no exceptions are thrown <br>
     * - the feedback message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandSuccess(String inputCommand, String expectedMessage,
            Model expectedModel) throws CommandException, ParseException {
        CommandResult result = logic.execute(inputCommand);
        assertEquals(expectedMessage, result.getFeedbackToUser());
        assertEquals(expectedModel, model);
    }

    /**
     * Executes the command, confirms that a ParseException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertParseException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, ParseException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that a CommandException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, CommandException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that the exception is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage) {
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        assertCommandFailure(inputCommand, expectedException, expectedMessage, expectedModel);
    }

    /**
     * Executes the command and confirms that
     * - the {@code expectedException} is thrown <br>
     * - the resulting error message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandSuccess(String, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage, Model expectedModel) {
        assertThrows(expectedException, expectedMessage, () -> logic.execute(inputCommand));
        assertEquals(expectedModel, model);
    }

    /**
     * Tests the Logic component's handling of an {@code IOException} thrown by the Storage component.
     *
     * @param e the exception to be thrown by the Storage component
     * @param expectedMessage the message expected inside exception thrown by the Logic component
     */
    private void assertCommandFailureForExceptionFromStorage(IOException e, String expectedMessage) {
        Path prefPath = temporaryFolder.resolve("ExceptionUserPrefs.json");

        // Inject LogicManager with a JsonAddressBookStorage that throws the IOException e when saving
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(prefPath) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                throw e;
            }
        };

        JsonUserPrefsStorage userPrefsStorage =
                new JsonUserPrefsStorage(temporaryFolder.resolve("ExceptionUserPrefs.json"));
        StorageManager storage = new StorageManager(addressBookStorage, userPrefsStorage);

        logic = new LogicManager(model, storage);

        // Triggers the saveAddressBook method by executing an add command
        String addCommand = AddCommand.COMMAND_WORD + NAME_DESC_AMY + PHONE_DESC_AMY
                + EMAIL_DESC_AMY + ADDRESS_DESC_AMY;
        ModelManager expectedModel = new ModelManager();
        assertCommandFailure(addCommand, CommandException.class, expectedMessage, expectedModel);
    }
}
