package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.StatusFilter;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Exercises name punctuation through command parsing, searches, and persistence. */
public class NameValidationIntegrationTest {
    private static final List<String> VALID_NAMES = List.of("Anne-Marie Tan", "O'Connor Tan", "J. P. Tan",
            "Ravi S/O Kumar", "Devi D/O Kumar", "Tan, Alex", "Tan (Alex)", "Tan @ Alex");

    @TempDir
    public Path testFolder;

    private final AtomicInteger saveAttempts = new AtomicInteger();
    private Model model;
    private Logic logic;
    private JsonAddressBookStorage storage;
    private Path dataFile;

    @BeforeEach
    public void setUp() {
        dataFile = testFolder.resolve("members.json");
        storage = new JsonAddressBookStorage(dataFile) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                saveAttempts.incrementAndGet();
                super.saveAddressBook(addressBook);
            }
        };
        model = new ModelManager(new AddressBook(), new UserPrefs());
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(testFolder.resolve("preferences.json"))));
    }

    @Test
    public void execute_addPunctuatedNames_preservesSpellingAfterReload() throws Exception {
        for (int i = 0; i < VALID_NAMES.size(); i++) {
            logic.execute("add e/member" + i + "@u.nus.edu n/  " + VALID_NAMES.get(i) + "  ");
        }
        assertEquals(VALID_NAMES, model.getAddressBook().getPersonList().stream()
                .map(person -> person.getName().orElseThrow().fullName).toList());
        assertEquals(model.getAddressBook(), new JsonAddressBookStorage(dataFile).readAddressBook().orElseThrow());
    }

    @Test
    public void execute_editPunctuatedNames_preservesSpellingAfterReload() throws Exception {
        logic.execute("add e/member@u.nus.edu n/Before");
        for (String value : VALID_NAMES) {
            logic.execute("edit 1 n/  " + value + "  ");
            assertEquals(value, model.getAddressBook().getPersonList().getFirst().getName().orElseThrow().fullName);
            assertEquals(model.getAddressBook(), new JsonAddressBookStorage(dataFile).readAddressBook().orElseThrow());
        }
    }

    @Test
    public void execute_punctuatedNameSearches_matchesWholeWordsIgnoringCase() throws Exception {
        for (int i = 0; i < VALID_NAMES.size(); i++) {
            logic.execute("add e/member" + i + "@u.nus.edu n/" + VALID_NAMES.get(i));
        }
        List<Person> members = List.copyOf(model.getAddressBook().getPersonList());
        List<String> keywords = List.of("ANNE-MARIE", "O'CONNOR", "j.", "s/o", "d/o", "tan,", "(alex)", "@");
        for (String command : List.of("find", "findall", "findany")) {
            for (int i = 0; i < keywords.size(); i++) {
                logic.execute(command + " n/" + keywords.get(i));
                assertEquals(List.of(members.get(i)), model.getFilteredPersonList());
            }
        }
        logic.execute("find O'CONNOR");
        assertEquals(List.of(members.get(1)), model.getFilteredPersonList());
        for (String keyword : List.of("Anne", "Connor")) {
            logic.execute("find " + keyword);
            assertEquals(List.of(), model.getFilteredPersonList());
        }
    }

    @Test
    public void execute_invalidNames_preservesStateAndSkipsSaving() throws Exception {
        logic.execute("add e/existing@u.nus.edu n/Existing Member");
        logic.execute("add e/other@u.nus.edu n/Other Member");
        logic.execute("view active");
        logic.execute("find Existing");
        AddressBook original = new AddressBook(model.getAddressBook());
        List<Person> filtered = List.copyOf(model.getFilteredPersonList());
        String originalFile = Files.readString(dataFile);
        saveAttempts.set(0);

        for (String value : List.of("", "   ", " ,()/.'@-", "Alex!", "Alex\tTan", "A".repeat(127) + "-")) {
            for (String command : List.of("add e/rejected@u.nus.edu n/", "edit 1 n/")) {
                ParseException error = assertThrows(ParseException.class, () -> logic.execute(command + value));
                assertEquals(Name.MESSAGE_CONSTRAINTS, error.getMessage());
                assertEquals(original, model.getAddressBook());
                assertEquals(filtered, model.getFilteredPersonList());
                assertEquals(StatusFilter.ACTIVE, model.getStatusFilter());
                assertEquals(originalFile, Files.readString(dataFile));
                assertEquals(0, saveAttempts.get());
            }
        }
    }
}
