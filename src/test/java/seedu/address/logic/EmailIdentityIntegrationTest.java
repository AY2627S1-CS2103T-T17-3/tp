package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.EditCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.StatusFilter;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Email;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/**
 * Exercises email identity through parsing, commands, the model, and persisted data.
 */
public class EmailIdentityIntegrationTest {
    @TempDir
    public Path testFolder;

    private Model model;
    private Logic logic;
    private JsonAddressBookStorage storage;
    private Path dataFile;

    @BeforeEach
    public void setUp() {
        dataFile = testFolder.resolve("nusocietydesk.json");
        storage = new JsonAddressBookStorage(dataFile);
        model = new ModelManager(new AddressBook(), new UserPrefs());
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(testFolder.resolve("preferences.json"))));
    }

    @Test
    public void execute_invalidEmail_preservesDirectoryFilterAndFile() throws Exception {
        logic.execute("add n/Alex Tan p/91234567 e/alex@u.nus.edu a/Kent Ridge");
        logic.execute("add n/Jamie Lee p/98765432 e/jamie@u.nus.edu a/Clementi");
        logic.execute("find Alex");
        AddressBook original = new AddressBook(model.getAddressBook());
        List<Person> filtered = List.copyOf(model.getFilteredPersonList());
        String originalFile = Files.readString(dataFile);
        for (String email : new String[] {"", "alex tan@u.nus.edu", "alex@@u.nus.edu",
            "alex@example.com", "alex", "alex@nus.edu", "alex@sub.u.nus.edu", "alex@u.nus.edu.com"}) {
            ParseException error = assertThrows(ParseException.class, () -> logic.execute("edit 1 e/" + email));
            assertEquals(Email.MESSAGE_CONSTRAINTS, error.getMessage());
            ParseException addError = assertThrows(ParseException.class, () ->
                    logic.execute("add n/New Person p/91234567 e/" + email + " a/Kent Ridge"));
            assertEquals(Email.MESSAGE_CONSTRAINTS, addError.getMessage());
            assertEquals(original, model.getAddressBook());
            assertEquals(filtered, model.getFilteredPersonList());
            assertEquals(originalFile, Files.readString(dataFile));
        }
    }

    @Test
    public void execute_overlongEmail_preservesStateAndSkipsSaving() throws Exception {
        logic.execute("add n/Alex Tan e/alex@u.nus.edu");
        logic.execute("add n/Jamie Lee e/jamie@u.nus.edu");
        logic.execute("view active");
        logic.execute("find Alex");
        AddressBook original = new AddressBook(model.getAddressBook());
        List<Person> filtered = List.copyOf(model.getFilteredPersonList());
        String originalFile = Files.readString(dataFile);
        AtomicInteger saveAttempts = new AtomicInteger();
        useCountingStorage(saveAttempts);

        for (String command : List.of("add e/", "edit 1 e/")) {
            ParseException error = assertThrows(ParseException.class, () ->
                    logic.execute(command + "a.".repeat(2000) + "a@u.nus.edu"));
            assertEquals("Email local-part must contain at most 64 characters; received 4001.", error.getMessage());
            assertEquals(original, model.getAddressBook());
            assertEquals(filtered, model.getFilteredPersonList());
            assertEquals(StatusFilter.ACTIVE, model.getStatusFilter());
            assertEquals(originalFile, Files.readString(dataFile));
        }
        assertEquals(0, saveAttempts.get());
        assertEquals("New person added: Email: after@u.nus.edu",
                logic.execute("add e/after@u.nus.edu").getFeedbackToUser());
        assertEquals(1, saveAttempts.get());
        assertEquals(3, storage.readAddressBook().orElseThrow().getPersonList().size());
    }

    @Test
    public void execute_overlongFirstEmail_leavesDataFileAbsent() {
        AtomicInteger saveAttempts = new AtomicInteger();
        useCountingStorage(saveAttempts);
        assertThrows(ParseException.class, () -> logic.execute("add e/" + "a".repeat(65) + "@u.nus.edu"));
        assertEquals(0, saveAttempts.get());
        assertFalse(Files.exists(dataFile));
        assertEquals(0, model.getAddressBook().getPersonList().size());
    }

    @Test
    public void execute_overlongEmail_preservesEarlierValidationPriority() {
        String overlongEmail = "a".repeat(65) + "@gmail.com";
        for (String command : List.of("add", "edit 1")) {
            ParseException phoneError = assertThrows(ParseException.class, () ->
                    logic.execute(command + " p/12 e/" + overlongEmail));
            assertEquals(Phone.MESSAGE_CONSTRAINTS, phoneError.getMessage());
            ParseException duplicateError = assertThrows(ParseException.class, () ->
                    logic.execute(command + " e/" + overlongEmail + " e/after@u.nus.edu"));
            assertEquals("Multiple values specified for the following single-valued field(s): e/",
                    duplicateError.getMessage());
        }
        ParseException tagError = assertThrows(ParseException.class, () ->
                logic.execute("edit 1 e/" + overlongEmail + " t/Exco"));
        assertEquals(EditCommand.MESSAGE_TAG_EDIT_NOT_SUPPORTED, tagError.getMessage());
    }

    @Test
    public void execute_boundaryEmail_persistsAndRemainsSearchable() throws Exception {
        String email = "a".repeat(64) + "@u.nus.edu";
        assertEquals("New person added: Email: " + email,
                logic.execute("add e/" + email.toUpperCase(Locale.ROOT)).getFeedbackToUser());
        assertEquals(email, storage.readAddressBook().orElseThrow().getPersonList().getFirst().getEmail().value);
        for (String command : List.of("find", "findall", "findany")) {
            assertEquals("1 person(s) listed!", logic.execute(command + " e/" + email).getFeedbackToUser());
            assertEquals("0 person(s) listed!", logic.execute(command + " e/" + email + "x").getFeedbackToUser());
        }
    }

    private void useCountingStorage(AtomicInteger saveAttempts) {
        JsonAddressBookStorage countingStorage = new JsonAddressBookStorage(dataFile) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                saveAttempts.incrementAndGet();
                super.saveAddressBook(addressBook);
            }
        };
        logic = new LogicManager(model, new StorageManager(countingStorage,
                new JsonUserPrefsStorage(testFolder.resolve("preferences.json"))));
    }
}
