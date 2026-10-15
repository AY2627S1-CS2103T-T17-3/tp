package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;

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
import seedu.address.model.person.Person;
import seedu.address.model.person.Status;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/**
 * Exercises combined searches through command dispatch, the model and storage.
 */
public class CombinedSearchTest {
    @TempDir
    public Path temporaryFolder;

    private final Person alice = new PersonBuilder().withName("Alice Tan").withPhone("91234567")
            .withEmail("alice@u.nus.edu").withTags("publicity", "president").build();
    private final Person bob = new PersonBuilder(alice).withName("Bob Lee").withEmail("bob@u.nus.edu").build();
    private final Person archived = new PersonBuilder(alice).withEmail("former@u.nus.edu")
            .withStatus(Status.ARCHIVED).build();
    private final Person vicePresident = new PersonBuilder(alice).withEmail("vice@u.nus.edu")
            .withTags("publicity", "vicepresident").build();
    private final Person unnamed = new PersonBuilder(alice).withEmail("unnamed@u.nus.edu")
            .withoutName().withoutPhone().withoutAddress().build();
    private final Person noTags = new PersonBuilder(alice).withEmail("notags@u.nus.edu").withTags().build();
    private final Person differentPhone = new PersonBuilder(alice).withEmail("otherphone@u.nus.edu")
            .withPhone("88888888").build();
    private final Person statusTag = new PersonBuilder(alice).withEmail("statustag@u.nus.edu")
            .withTags("archived").build();
    private Model model;
    private Logic logic;
    private AddressBook original;
    private JsonAddressBookStorage storage;

    @BeforeEach
    public void setUp() throws Exception {
        model = new ModelManager();
        for (Person person : List.of(alice, bob, archived, vicePresident, unnamed, noTags, differentPhone, statusTag)) {
            model.addPerson(person);
        }
        original = new AddressBook(model.getAddressBook());
        storage = new JsonAddressBookStorage(temporaryFolder.resolve("members.json"));
        storage.saveAddressBook(original);
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json"))));
    }

    @Test
    public void execute_allConditions_matchIntersectionOnly() throws Exception {
        assertMatches("find n/Alice Bob p/9123 e/@u.nus.edu t/PUBLICITY t/president s/ACTIVE", alice, bob);
        assertMatches("find s/active t/president e/ALICE p/9123 n/Alice Bob t/publicity", alice);
        assertMatches("find n/Alice Bob t/publicity s/active", alice, bob, vicePresident, differentPhone);
    }

    @Test
    public void execute_exactTags_requiresEveryTagAndIgnoresDuplicateCaseVariants() throws Exception {
        assertMatches("find t/publicity t/PUBLICITY t/president", alice, bob, archived, unnamed, differentPhone);
        assertMatches("find t/pres", new Person[0]);
        assertMatches("find t/president t/vicepresident", new Person[0]);
    }

    @Test
    public void execute_status_isIndependentOfTagsAndDefaultsToBothStatuses() throws Exception {
        assertMatches("find s/ArChIvEd", archived);
        assertMatches("find t/archived s/active", statusTag);
        assertMatches("find t/publicity e/former", archived);
    }

    @Test
    public void execute_absentContactFields_failOnlyRelevantConditions() throws Exception {
        assertMatches("find e/unnamed t/publicity s/active", unnamed);
        assertMatches("find e/unnamed t/publicity n/Alice", new Person[0]);
        assertMatches("find e/unnamed t/publicity p/9123", new Person[0]);
    }

    @Test
    public void execute_newSearch_replacesPreviousFilterAndSupportsNoMatches() throws Exception {
        assertMatches("find e/nobody s/active", new Person[0]);
        assertMatches("find t/publicity s/archived", archived);
        assertMatches("find e/bob s/active", bob);
    }

    @Test
    public void execute_invalidQuery_preservesRecordsFilterAndSavedFile() throws Exception {
        assertMatches("find e/bob s/active", bob);
        String savedBefore = Files.readString(storage.getAddressBookFilePath());
        for (String query : List.of("find t/publicity s/former", "find n/Bob x/value", "find n/Bob t/",
                "find e/bob e/alice", "find s/active s/archived", "find Bob t/publicity",
                "find t/president!", "find p/9123 4567 s/active", "find e/alice bob s/active")) {
            assertThrows(ParseException.class, () -> logic.execute(query));
            assertEquals(List.of(bob), model.getFilteredPersonList());
            assertEquals(original, model.getAddressBook());
            assertEquals(savedBefore, Files.readString(storage.getAddressBookFilePath()));
        }
    }

    private void assertMatches(String query, Person... expected) throws Exception {
        assertEquals(String.format(Messages.MESSAGE_PERSONS_LISTED_OVERVIEW, expected.length),
                logic.execute(query).getFeedbackToUser());
        assertEquals(List.of(expected), model.getFilteredPersonList());
        assertEquals(original, model.getAddressBook());
        assertEquals(original, storage.readAddressBook().orElseThrow());
    }
}
