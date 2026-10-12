package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Email;
import seedu.address.model.person.Person;
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
        dataFile = testFolder.resolve("addressbook.json");
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
}
