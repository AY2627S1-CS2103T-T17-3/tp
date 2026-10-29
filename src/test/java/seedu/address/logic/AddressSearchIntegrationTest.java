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
import seedu.address.model.StatusFilter;
import seedu.address.model.person.Person;
import seedu.address.model.person.Status;
import seedu.address.storage.AddressBookLoadResult;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/**
 * Tests address searches through command routing, combined filters, validation and local storage.
 */
public class AddressSearchIntegrationTest {
    @TempDir
    public Path temporaryFolder;

    private final Person alex = new PersonBuilder().withName("Alex Tan").withPhone("91234567")
            .withEmail("alex@u.nus.edu").withTags("publicity")
            .withAddress("12 Kent\u00a0\u2003Ridge Road, #03-45, Singapore 119077").build();
    private final Person beth = new PersonBuilder().withName("Beth Lee").withEmail("beth@u.nus.edu")
            .withTags("exco").withAddress("34 West Road, c/o Alex, #03/45").build();
    private final Person chen = new PersonBuilder().withName("Chen Lim").withEmail("chen@u.nus.edu")
            .withTags("publicity").withAddress("56 Kent Ridge Drive").withStatus(Status.ARCHIVED).build();
    private final Person missingAddress = new PersonBuilder().withName("Kent Ridge").withPhone("119077")
            .withEmail("kent@u.nus.edu").withTags("kent", "publicity").withoutAddress().build();
    private final List<Person> members = List.of(alex, beth, chen, missingAddress);
    private final Model model = new ModelManager();
    private Logic logic;
    private JsonAddressBookStorage addressBookStorage;
    private Path savedFile;

    @BeforeEach
    public void setUp() throws Exception {
        savedFile = temporaryFolder.resolve("nusocietydesk.json");
        addressBookStorage = new JsonAddressBookStorage(savedFile);
        StorageManager storage = new StorageManager(addressBookStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json")));
        logic = new LogicManager(model, storage);
        members.forEach(model::addPerson);
        storage.saveAddressBook(model.getAddressBook());
    }

    @Test
    public void execute_addressSubstrings_matchesOnlyAddressesAcrossAllCommands() throws Exception {
        byte[] savedData = Files.readAllBytes(savedFile);
        for (String command : List.of("find", "findall", "findany")) {
            assertSearchResults(command + " a/kent ridge", List.of(alex, chen));
            assertSearchResults(command + " a/RIDGE", List.of(alex, chen));
            assertSearchResults(command + " a/119077", List.of(alex));
            assertSearchResults(command + " a/#03-45", List.of(alex));
            assertSearchResults(command + " a/03/45", List.of(beth));
            assertSearchResults(command + " a/c/o", List.of(beth));
            assertSearchResults(command + " a/road", List.of(alex, beth));
        }
        assertEquals(members, model.getAddressBook().getPersonList());
        assertArrayEquals(savedData, Files.readAllBytes(savedFile));
    }

    @Test
    public void execute_normalizedWhitespace_matchesAddressPhrases() throws Exception {
        for (String command : List.of("find", "findall", "findany")) {
            for (String whitespace : List.of(" ", "  ", "\t", "\n", "\r\n", "\u0085", "\u00a0", "\u2007",
                    "\u202f", "\u3000", " \t\u2003 ")) {
                assertSearchResults(command + " a/" + whitespace + "KENT" + whitespace + "RIDGE" + whitespace,
                        List.of(alex, chen));
            }
        }
    }

    @Test
    public void execute_nonMatchingLiteralPhrases_displaysEmptyListAndZeroCount() throws Exception {
        for (String command : List.of("find", "findall", "findany")) {
            for (String value : List.of("Kent Road", "Ridge Kent", "KentRidge", "Kent.*Ridge", "Kent*", "-unknown-",
                    "\"Kent Ridge\"", "大学路", "x".repeat(1000))) {
                assertSearchResults(command + " a/" + value, List.of());
            }
        }
    }

    @Test
    public void execute_combinedConditions_respectsAndOrAndDirectoryOrder() throws Exception {
        assertSearchResults("findall a/Kent a/Road", List.of(alex));
        assertSearchResults("findall a/Ridge a/Kent", List.of(alex, chen));
        assertSearchResults("findall n/Alex p/9123 e/alex a/Kent Ridge t/publicity", List.of(alex));
        assertSearchResults("findall t/publicity a/Kent Ridge e/alex p/9123 n/Alex", List.of(alex));
        assertSearchResults("findany a/Kent Ridge t/exco", List.of(alex, beth, chen));
        assertSearchResults("findany a/Kent Ridge a/road", List.of(alex, beth, chen));
        assertSearchResults("findany a/Kent Ridge a/Kent Ridge", List.of(alex, chen));
        assertSearchResults("findany a/Kent Ridge e/kent", List.of(alex, chen, missingAddress));
        assertSearchResults("findall a/Kent Ridge e/kent", List.of());
    }

    @Test
    public void execute_searchAfterEarlierFilter_checksWholeDirectoryAndPreservesStatusView() throws Exception {
        for (String command : List.of("find", "findall", "findany")) {
            logic.execute("view all");
            logic.execute("find e/beth");
            assertSearchResults(command + " a/Kent Ridge", List.of(alex, chen));
            logic.execute("view active");
            assertSearchResults(command + " a/Kent Ridge", List.of(alex));
            logic.execute("view archived");
            assertSearchResults(command + " a/Kent Ridge", List.of(chen));
            assertSearchResults(command + " a/West Road", List.of());
        }
    }

    @Test
    public void execute_blankCondition_preservesRecordsFilterStatusAndSavedData() throws Exception {
        logic.execute("view active");
        logic.execute("find e/beth");
        for (String command : List.of("find", "findall", "findany")) {
            for (String value : List.of("", " \t\r\n ", "\u0085", "\u00a0", "\u2007", "\u2003", "\u202f",
                    "\u3000", " \t\u00a0\u001c\u2003\u202f ")) {
                assertRejectedSearch(command + " a/" + value);
                if (!command.equals("find")) {
                    assertRejectedSearch(command + " a/" + value + " n/Alex");
                    assertRejectedSearch(command + " n/Alex a/" + value);
                }
            }
        }
    }

    @Test
    public void execute_malformedConditions_preservesRecordsFilterStatusAndSavedData() throws Exception {
        logic.execute("view active");
        logic.execute("find e/beth");
        for (String command : List.of("find", "findall", "findany")) {
            for (String input : List.of("A/Kent Ridge", "a/Kent Ridge A/Road", "a/Kent Ridge x/value",
                    "a/Kent Ridge c/o Alex", "a/Kent Ridge #03/45", "Kent a/Ridge", "a/Kent Ridge p/abc",
                    "a/Kent Ridge e/", "a/Kent Ridge n/", "a/Kent Ridge t/", "a/Kent Ridge a/")) {
                assertRejectedSearch(command + " " + input);
            }
        }
        for (String input : List.of("find a/Kent a/Ridge", "find a/Kent n/Alex", "find a/Kent p/9123",
                "find a/Kent e/alex", "find a/Kent t/publicity", "findall a/Kent n/Alex Tan",
                "findany a/Kent t/exco!", "findany a/Kent e/alex beth", "findany p/abc a/Kent")) {
            assertRejectedSearch(input);
        }
    }

    @Test
    public void execute_invalidAddressInEmptyDirectory_failsBeforeFiltering() throws Exception {
        members.forEach(model::deletePerson);
        for (String command : List.of("find", "findall", "findany")) {
            assertThrows(ParseException.class, () -> logic.execute(command + " a/\u00a0"));
            assertThrows(ParseException.class, () -> logic.execute(command + " a/Kent A/Ridge"));
            assertSearchResults(command + " a/Kent Ridge", List.of());
        }
    }

    @Test
    public void execute_searchAfterEditAndReload_usesUpdatedAddress() throws Exception {
        logic.execute("find e/alex");
        logic.execute("edit 1 a/West Campus");
        assertSearchResults("find a/Kent Ridge", List.of(chen));
        Person editedAlex = new PersonBuilder(alex).withAddress("West Campus").build();
        assertSearchResults("find a/West Campus", List.of(editedAlex));

        AddressBookLoadResult loaded = addressBookStorage.loadAddressBook();
        assertEquals(AddressBookLoadResult.Status.SUCCESS, loaded.getStatus());
        model.setAddressBook(loaded.getAddressBook());
        assertSearchResults("find a/West Campus", List.of(editedAlex));
        assertSearchResults("find a/Kent Ridge", List.of(chen));
    }

    @Test
    public void execute_savingDisabled_searchRemainsReadOnly() throws Exception {
        Files.writeString(savedFile, "invalid JSON");
        addressBookStorage.loadAddressBook();
        for (String command : List.of("find", "findall", "findany")) {
            assertSearchResults(command + " a/Kent Ridge", List.of(alex, chen));
            assertEquals("invalid JSON", Files.readString(savedFile));
        }
    }

    /**
     * Verifies the visible members and result count after executing a search.
     */
    private void assertSearchResults(String input, List<Person> expectedMembers) throws Exception {
        CommandResult result = logic.execute(input);
        assertEquals(expectedMembers, model.getFilteredPersonList(), input);
        assertEquals(String.format(Messages.MESSAGE_PERSONS_LISTED_OVERVIEW, expectedMembers.size()),
                result.getFeedbackToUser(), input);
    }

    /**
     * Verifies that a rejected search preserves the current results, status, records and saved data.
     */
    private void assertRejectedSearch(String input) throws Exception {
        List<Person> previousResults = List.copyOf(model.getFilteredPersonList());
        Predicate<? super Person> previousFilter = model.getFilteredPersonListPredicate();
        StatusFilter previousStatus = model.getStatusFilter();
        byte[] savedData = Files.readAllBytes(savedFile);
        assertThrows(ParseException.class, () -> logic.execute(input));
        assertEquals(previousResults, model.getFilteredPersonList(), input);
        assertSame(previousFilter, model.getFilteredPersonListPredicate(), input);
        assertEquals(previousStatus, model.getStatusFilter(), input);
        assertEquals(members, model.getAddressBook().getPersonList(), input);
        assertArrayEquals(savedData, Files.readAllBytes(savedFile), input);
    }
}
