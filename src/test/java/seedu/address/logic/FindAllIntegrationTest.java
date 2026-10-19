package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static seedu.address.testutil.Assert.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/**
 * Tests findall through command routing, filtering and JSON storage.
 */
public class FindAllIntegrationTest {
    @TempDir
    public Path temporaryFolder;

    private final Person john = new PersonBuilder().withName("John Tan").withPhone("91234567")
            .withEmail("john@u.nus.edu").withTags("exco", "publicity").build();
    private final Person alice = new PersonBuilder().withName("Alice Tan").withPhone("87654321")
            .withEmail("alice@u.nus.edu").withTags("publicity").build();
    private final Model model = new ModelManager();
    private Logic logic;
    private Path savedFile;

    @BeforeEach
    public void setUp() throws Exception {
        savedFile = temporaryFolder.resolve("addressBook.json");
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(savedFile);
        StorageManager storage = new StorageManager(addressBookStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json")));
        logic = new LogicManager(model, storage);
        model.addPerson(john);
        model.addPerson(alice);
        storage.saveAddressBook(model.getAddressBook());
    }

    @Test
    public void execute_validSearch_replacesFilterWithoutChangingRecordsOrSavedData() throws Exception {
        logic.execute("find n/Alice");
        byte[] savedData = Files.readAllBytes(savedFile);
        CommandResult result = logic.execute("findall n/John\nt/exco t/publicity");
        assertEquals(String.format(Messages.MESSAGE_PERSONS_LISTED_OVERVIEW, 1), result.getFeedbackToUser());
        assertEquals(List.of(john), model.getFilteredPersonList());
        assertEquals(List.of(john, alice), model.getAddressBook().getPersonList());
        assertArrayEquals(savedData, Files.readAllBytes(savedFile));
    }

    @Test
    public void execute_invalidCondition_preservesRecordsFilterAndSavedData() throws Exception {
        logic.execute("findall n/Alice t/publicity");
        byte[] savedData = Files.readAllBytes(savedFile);
        Predicate<? super Person> previousFilter = model.getFilteredPersonListPredicate();

        for (String input : List.of("findall", "findall John t/exco", "findall n/John Tan", "findall n/ n/John",
                "findall n/John n/", "findall p/abc p/9123", "findall p/9123 p/abc", "findall n/John p/",
                "findall e/ e/john", "findall e/john e/", "findall n/John e/john alice",
                "findall t/exco! t/exco", "findall t/exco t/exco!", "findall t/ t/exco", "findall t/exco t/",
                "findall n/John T/exco", "findall n/John G/value", "findall n/Nobody p/abc",
                "findall n/John\np/abc")) {
            assertThrows(ParseException.class, () -> logic.execute(input));
            assertEquals(List.of(alice), model.getFilteredPersonList());
            assertSame(previousFilter, model.getFilteredPersonListPredicate());
            assertEquals(List.of(john, alice), model.getAddressBook().getPersonList());
            assertArrayEquals(savedData, Files.readAllBytes(savedFile));
        }
    }

    @Test
    public void execute_invalidConditionInEmptyDirectory_failsBeforeFiltering() throws Exception {
        model.deletePerson(john);
        model.deletePerson(alice);
        assertThrows(ParseException.class, () -> logic.execute("findall n/Nobody p/abc"));
    }

    @Test
    public void execute_savingDisabled_searchRemainsAvailable() throws Exception {
        Files.writeString(savedFile, "invalid JSON");
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(savedFile);
        addressBookStorage.loadAddressBook();
        Logic readOnlyLogic = new LogicManager(model, new StorageManager(addressBookStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"))));

        CommandResult result = readOnlyLogic.execute("findall n/John t/exco");
        assertEquals(String.format(Messages.MESSAGE_PERSONS_LISTED_OVERVIEW, 1), result.getFeedbackToUser());
        assertEquals(List.of(john), model.getFilteredPersonList());
        assertEquals("invalid JSON", Files.readString(savedFile));
    }
}
