package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
import seedu.address.model.person.Address;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.model.tag.Tag;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Exercises field length limits through parsing, commands, searches, and persisted data. */
public class FieldLengthValidationIntegrationTest {
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
    public void execute_overlongContactFields_preservesStateAndSkipsSaving() throws Exception {
        prepareFilteredDirectory();
        String[][] invalidFields = {
            {"n/", "A".repeat(128), Name.MESSAGE_CONSTRAINTS},
            {"p/", "9".repeat(16), Phone.MESSAGE_CONSTRAINTS},
            {"a/", "A".repeat(201), Address.MESSAGE_CONSTRAINTS}
        };
        for (String[] field : invalidFields) {
            for (String value : List.of(field[1], field[1].repeat(100))) {
                for (String command : List.of("add e/rejected@u.nus.edu ", "edit 1 ")) {
                    assertRejectedCommandPreservesState(command + field[0] + value, field[2]);
                }
            }
        }
        assertEquals(0, saveAttempts.get());
        logic.execute("edit 1 n/After");
        assertEquals(1, saveAttempts.get());
        assertEquals("After", storage.readAddressBook().orElseThrow().getPersonList().getFirst()
                .getName().orElseThrow().fullName);
    }

    @Test
    public void execute_overlongTags_rejectsAddTagUntagAndTagSearches() throws Exception {
        prepareFilteredDirectory();
        for (String tag : List.of("a".repeat(31), "a".repeat(10000))) {
            for (String command : List.of("add e/rejected@u.nus.edu t/", "tag 1 t/", "untag 1 t/",
                    "find t/", "findall t/", "findany t/")) {
                assertRejectedCommandPreservesState(command + tag, Tag.MESSAGE_CONSTRAINTS);
            }
            assertRejectedCommandPreservesState("add e/rejected@u.nus.edu t/valid t/" + tag,
                    Tag.MESSAGE_CONSTRAINTS);
        }
        assertRejectedCommandPreservesState("edit 1 t/" + "a".repeat(31),
                EditCommand.MESSAGE_TAG_EDIT_NOT_SUPPORTED);
        assertEquals(0, saveAttempts.get());
    }

    @Test
    public void execute_overlongFirstAdd_leavesDataFileAbsent() {
        for (String argument : List.of("n/" + "A".repeat(128), "p/" + "9".repeat(16),
                "a/" + "A".repeat(201), "t/" + "a".repeat(31))) {
            assertThrows(ParseException.class, () -> logic.execute("add e/rejected@u.nus.edu " + argument));
        }
        assertEquals(0, saveAttempts.get());
        assertFalse(Files.exists(dataFile));
        assertEquals(0, model.getAddressBook().getPersonList().size());
    }

    @Test
    public void execute_maximumLengths_persistsEditsAndRetainsSearchRules() throws Exception {
        String name = "A".repeat(127);
        String phone = "9".repeat(15);
        String address = "A".repeat(200);
        String tag = "a".repeat(30);
        logic.execute("add e/boundary@u.nus.edu n/" + name + " p/" + phone + " a/" + address + " t/"
                + tag.toUpperCase(Locale.ROOT));
        Person member = model.getAddressBook().getPersonList().getFirst();
        assertEquals(new Name(name), member.getName().orElseThrow());
        assertEquals(new Phone(phone), member.getPhone().orElseThrow());
        assertEquals(new Address(address), member.getAddress().orElseThrow());
        assertEquals(Set.of(new Tag(tag)), member.getTags());
        assertEquals(model.getAddressBook(), storage.readAddressBook().orElseThrow());

        logic.execute("edit 1 n/" + "B".repeat(127) + " p/" + "8".repeat(15) + " a/" + "B".repeat(200));
        member = model.getAddressBook().getPersonList().getFirst();
        assertEquals(new Name("B".repeat(127)), member.getName().orElseThrow());
        assertEquals(new Phone("8".repeat(15)), member.getPhone().orElseThrow());
        assertEquals(new Address("B".repeat(200)), member.getAddress().orElseThrow());
        assertEquals(Set.of(new Tag(tag)), member.getTags());
        assertEquals(model.getAddressBook(), storage.readAddressBook().orElseThrow());

        logic.execute("untag 1 t/" + tag);
        assertEquals(Set.of(), model.getAddressBook().getPersonList().getFirst().getTags());
        logic.execute("tag 1 t/" + tag);
        assertEquals(model.getAddressBook(), storage.readAddressBook().orElseThrow());
        for (String command : List.of("find", "findall", "findany")) {
            for (String condition : List.of("t/" + tag, "p/8", "p/88", "n/" + "B".repeat(127))) {
                assertEquals("1 person(s) listed!", logic.execute(command + " " + condition).getFeedbackToUser());
            }
            for (String condition : List.of("p/" + "8".repeat(16), "n/" + "B".repeat(128))) {
                assertEquals("0 person(s) listed!", logic.execute(command + " " + condition).getFeedbackToUser());
            }
        }
    }

    private void prepareFilteredDirectory() throws Exception {
        logic.execute("add n/Alex e/alex@u.nus.edu t/exco");
        logic.execute("add n/Jamie e/jamie@u.nus.edu");
        logic.execute("view active");
        logic.execute("find Alex");
        saveAttempts.set(0);
    }

    private void assertRejectedCommandPreservesState(String command, String expectedMessage) throws Exception {
        AddressBook original = new AddressBook(model.getAddressBook());
        List<Person> filtered = List.copyOf(model.getFilteredPersonList());
        String originalFile = Files.readString(dataFile);
        ParseException error = assertThrows(ParseException.class, () -> logic.execute(command));
        assertEquals(expectedMessage, error.getMessage());
        assertEquals(original, model.getAddressBook());
        assertEquals(filtered, model.getFilteredPersonList());
        assertEquals(StatusFilter.ACTIVE, model.getStatusFilter());
        assertEquals(originalFile, Files.readString(dataFile));
    }
}
