package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.CommandResult;
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
    public void execute_plainNameKeywords_matchesPunctuationBoundariesAcrossSearchCommands() throws Exception {
        List<Person> members = addMembers("Tan, Alex", "Tan,Alex", "Tan (Alex)", "Tan(Alex)", "Tan @ Alex",
                "Tan@Alex", "Alex-", "'Ali", "J. P. Tan", "J.P.Tan");
        assertSearch("find aLeX", members.subList(0, 7));
        assertSearch("find ALI", List.of(members.get(7)));
        assertSearch("find j", members.subList(8, 10));
        for (String command : List.of("find", "findall", "findany")) {
            assertSearch(command + " n/aLeX", members.subList(0, 7));
            assertSearch(command + " n/ALI", List.of(members.get(7)));
            assertSearch(command + " n/j", members.subList(8, 10));
            assertSearch(command + " n/@", List.of(members.get(4)));
            assertSearch(command + " n/(alex)", List.of(members.get(2)));
            assertSearch(command + " n/TanAlex", List.of());
        }
    }

    @Test
    public void execute_compoundKeywords_matchesSurroundingPunctuationAcrossSearchCommands() throws Exception {
        List<Person> members = addMembers("Tan(O'Connor)", "Tan,Anne-Marie", "Ravi(S/O)Kumar", "Tan@AN/NA");
        List<String> keywords = List.of("O'CONNOR", "Anne-Marie", "S/O", "an/na");
        for (int i = 0; i < keywords.size(); i++) {
            assertSearch("find " + keywords.get(i), List.of(members.get(i)));
            for (String command : List.of("find", "findall", "findany")) {
                assertSearch(command + " n/" + keywords.get(i), List.of(members.get(i)));
            }
        }
        for (String keyword : List.of("Connor", "Anne", "S", "AN", "OConnor", "AnneMarie", "SO", "ANNA")) {
            assertSearch("find " + keyword, List.of());
        }
    }

    @Test
    public void execute_slashKeywords_matchesLiteralNamesAcrossSearchCommands() throws Exception {
        List<Person> members = addMembers("Ravi S/O Kumar", "Devi D/O Kumar", "AN/NA", "G/value",
                "n/Alice", "N/Alice");
        List<String> keywords = List.of("s/o", "D/O", "AN/NA", "G/value");
        for (int i = 0; i < keywords.size(); i++) {
            assertSearch("find " + keywords.get(i), List.of(members.get(i)));
            for (String command : List.of("find", "findall", "findany")) {
                assertSearch(command + " n/" + keywords.get(i), List.of(members.get(i)));
            }
        }
        for (String separator : List.of(" ", "\t", "\n", "\r\n")) {
            assertSearch("find S/O" + separator + "d/o", members.subList(0, 2));
            assertSearch("find d/o" + separator + "S/O", members.subList(0, 2));
        }
        for (String command : List.of("find", "findall", "findany")) {
            assertSearch(command + " n/n/Alice", members.subList(4, 6));
            assertSearch(command + " n/S", List.of());
            assertSearch(command + " n/AN", List.of());
        }
    }

    @Test
    public void execute_punctuatedNames_combinesConditionsAndRespectsStatusView() throws Exception {
        logic.execute("add e/alex@u.nus.edu n/Tan(Alex) t/exco");
        logic.execute("add e/bob@u.nus.edu n/Bob-Smith t/exco");
        logic.execute("add e/ravi@u.nus.edu n/Ravi S/O Kumar t/publicity");
        logic.execute("add e/unnamed@u.nus.edu t/publicity");
        logic.execute("archive 3");
        logic.execute("view all");
        List<Person> members = List.copyOf(model.getAddressBook().getPersonList());

        assertSearch("findall n/Alex t/exco", List.of(members.get(0)));
        assertSearch("findall n/Alex n/Tan", List.of(members.get(0)));
        assertSearch("findany n/Alex n/S/O", List.of(members.get(0), members.get(2)));
        assertSearch("findany n/Alex e/unnamed", List.of(members.get(0), members.get(3)));
        assertSearch("findall n/Bob t/exco", List.of());
        assertSearch("findall n/S/O p/9", List.of());
        logic.execute("view archived");
        assertSearch("findany n/Alex n/S/O", List.of(members.get(2)));
        logic.execute("view active");
        assertSearch("findany n/Alex n/S/O", List.of(members.get(0)));
    }

    @Test
    public void execute_invalidPunctuatedNameSearch_preservesStateAndSkipsSaving() throws Exception {
        addMembers("Tan(Alex)", "Ravi S/O Kumar");
        logic.execute("view active");
        logic.execute("find Alex");
        AddressBook original = new AddressBook(model.getAddressBook());
        List<Person> filtered = List.copyOf(model.getFilteredPersonList());
        Predicate<? super Person> originalFilter = model.getFilteredPersonListPredicate();
        byte[] originalFile = Files.readAllBytes(dataFile);
        saveAttempts.set(0);

        for (String command : List.of("find n/", "find n/Alex Bob", "find n/Alex n/Bob", "find n/Alex p/9",
                "find Alex n/Bob", "find N/Alex", "find S/O N/Alex", "find n/Alex G/value", "find \u2003",
                "findall S/O", "findall n/S/O n/", "findall n/Alex G/value", "findany D/O",
                "findany n/Alex p/abc", "findany n/Alex G/value")) {
            assertThrows(ParseException.class, () -> logic.execute(command), command);
            assertEquals(original, model.getAddressBook(), command);
            assertEquals(filtered, model.getFilteredPersonList(), command);
            assertSame(originalFilter, model.getFilteredPersonListPredicate(), command);
            assertEquals(StatusFilter.ACTIVE, model.getStatusFilter(), command);
            assertArrayEquals(originalFile, Files.readAllBytes(dataFile), command);
            assertEquals(0, saveAttempts.get(), command);
        }
    }

    /**
     * Adds members with distinct emails and returns them in directory order.
     */
    private List<Person> addMembers(String... names) throws Exception {
        for (int i = 0; i < names.length; i++) {
            logic.execute("add e/member" + i + "@u.nus.edu n/" + names[i]);
        }
        return List.copyOf(model.getAddressBook().getPersonList());
    }

    /**
     * Verifies search results and feedback while ensuring searches never alter or save member data.
     */
    private void assertSearch(String command, List<Person> expected) throws Exception {
        List<Person> members = List.copyOf(model.getAddressBook().getPersonList());
        byte[] savedData = Files.readAllBytes(dataFile);
        int previousSaveAttempts = saveAttempts.get();
        CommandResult result = logic.execute(command);
        assertEquals(expected, model.getFilteredPersonList(), command);
        assertEquals(String.format(Messages.MESSAGE_PERSONS_LISTED_OVERVIEW, expected.size()),
                result.getFeedbackToUser(), command);
        assertEquals(members, model.getAddressBook().getPersonList(), command);
        assertArrayEquals(savedData, Files.readAllBytes(dataFile), command);
        assertEquals(previousSaveAttempts, saveAttempts.get(), command);
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
