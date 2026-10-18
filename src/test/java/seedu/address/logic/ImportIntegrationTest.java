package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.ImportCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
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

/**
 * Verifies CSV imports through command dispatch, the model, and JSON persistence.
 */
public class ImportIntegrationTest {
    private static final String HEADER = "name,phone,email,address,tags,status\n";
    private static final String FIRST_ROW = "Alex Tan,91234567,Alex@U.NUS.EDU,\"NUS, Kent Ridge\","
            + "Publicity;PUBLICITY,active\n";

    @TempDir
    public Path testFolder;

    private Model model;
    private Logic logic;
    private JsonAddressBookStorage storage;
    private Path dataFile;

    @BeforeEach
    public void setUp() throws Exception {
        dataFile = testFolder.resolve("addressbook.json");
        storage = new JsonAddressBookStorage(dataFile);
        model = new ModelManager(new AddressBook(), new UserPrefs());
        logic = createLogic(storage);
        logic.execute("add n/Existing Member p/91234567 e/existing@u.nus.edu a/Clementi t/Exco");
        logic.execute("find Existing");
    }

    @Test
    public void execute_validImport_savesStatusesAndTagsAndPreservesSource() throws Exception {
        Path file = writeCsv(HEADER + FIRST_ROW
                + "Jamie Lee,98765432,jamie@u.nus.edu,Clementi,,archived\n");
        String sourceBefore = Files.readString(file);
        Person existingBefore = model.getAddressBook().getPersonList().getFirst();

        CommandResult result = logic.execute(importCommand(file));

        assertEquals(String.format(ImportCommand.MESSAGE_SUCCESS, 2, 0, file), result.getFeedbackToUser());
        ReadOnlyAddressBook reloaded = storage.readAddressBook().orElseThrow();
        assertEquals(model.getAddressBook(), reloaded);
        assertEquals(3, reloaded.getPersonList().size());
        assertEquals(existingBefore, reloaded.getPersonList().getFirst());
        assertEquals("alex@u.nus.edu", reloaded.getPersonList().get(1).getEmail().value);
        assertEquals(List.of(new Tag("publicity")), List.copyOf(reloaded.getPersonList().get(1).getTags()));
        assertEquals(Status.ARCHIVED, reloaded.getPersonList().get(2).getStatus());
        assertEquals(reloaded.getPersonList(), model.getFilteredPersonList());
        assertEquals(sourceBefore, Files.readString(file));
    }

    @Test
    public void execute_rejectedImports_preservesRecordsFilterSourceAndSavedFile() throws Exception {
        for (String contents : List.of("", HEADER, "name,email\nAlex,alex@u.nus.edu\n",
                HEADER + "Jamie Lee,98765432,jamie@gmail.com,Clementi,,active\n",
                HEADER + "Jamie Lee,98765432,EXISTING@U.NUS.EDU,Clementi,,active\n",
                HEADER + "Jamie Lee,98765432,jamie@u.nus.edu,Clementi,,\n",
                HEADER + FIRST_ROW + "\"unterminated")) {
            Path file = writeCsv(contents);
            assertRejectedWithoutChanges(file);
        }
    }

    @Test
    public void execute_partialImport_savesOnlyAcceptedMembersAndPreservesSource() throws Exception {
        Path file = writeCsv(HEADER + "Bad!,91234567,alex@u.nus.edu,Kent Ridge,,active\n" + FIRST_ROW
                + ",,EXISTING@U.NUS.EDU,,,active\n"
                + ",,ALEX@U.NUS.EDU,,,archived\n"
                + ",,incomplete@u.nus.edu,,Publicity,archived\n");
        String sourceBefore = Files.readString(file);

        CommandResult result = logic.execute(importCommand(file));

        assertTrue(result.getFeedbackToUser().startsWith(String.format(ImportCommand.MESSAGE_SUCCESS, 2, 3, file)));
        assertTrue(result.getFeedbackToUser().contains("CSV record 2: Names"));
        assertTrue(result.getFeedbackToUser().contains("CSV record 4: email \"existing@u.nus.edu\" duplicates"));
        assertTrue(result.getFeedbackToUser().contains("CSV record 5: email \"alex@u.nus.edu\" duplicates"));
        ReadOnlyAddressBook reloaded = new JsonAddressBookStorage(dataFile).readAddressBook().orElseThrow();
        assertEquals(model.getAddressBook(), reloaded);
        assertEquals(List.of("existing@u.nus.edu", "alex@u.nus.edu", "incomplete@u.nus.edu"),
                reloaded.getPersonList().stream().map(person -> person.getEmail().value).toList());
        Person incomplete = reloaded.getPersonList().get(2);
        assertTrue(incomplete.getName().isEmpty());
        assertTrue(incomplete.getPhone().isEmpty());
        assertTrue(incomplete.getAddress().isEmpty());
        assertEquals(Status.ARCHIVED, incomplete.getStatus());
        assertEquals(List.of(new Tag("publicity")), List.copyOf(incomplete.getTags()));
        assertEquals(reloaded.getPersonList(), model.getFilteredPersonList());
        assertEquals(sourceBefore, Files.readString(file));
    }

    @Test
    public void execute_allRejected_doesNotAttemptSaving() throws Exception {
        AtomicInteger saveAttempts = new AtomicInteger();
        JsonAddressBookStorage countingStorage = new JsonAddressBookStorage(dataFile) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) {
                saveAttempts.incrementAndGet();
            }
        };
        logic = createLogic(countingStorage);
        Path file = writeCsv(HEADER + ",,existing@u.nus.edu,,,active\n"
                + "Invalid!,12,invalid@u.nus.edu,,,active\n");

        assertRejectedWithoutChanges(file);

        assertEquals(0, saveAttempts.get());
    }

    @Test
    public void execute_invalidUtf8AfterValidRecord_rejectsWithoutChangingDirectory() throws Exception {
        Path file = writeCsv(HEADER + FIRST_ROW);
        Files.write(file, new byte[] {(byte) 0xc3, (byte) 0x28}, StandardOpenOption.APPEND);

        assertRejectedWithoutChanges(file);
    }

    @Test
    public void execute_reimport_rejectsWithoutChangingSavedDirectory() throws Exception {
        Path file = writeCsv(HEADER + FIRST_ROW);
        logic.execute(importCommand(file));
        logic.execute("find Existing");
        assertRejectedWithoutChanges(file);
    }

    @Test
    public void execute_missingSource_doesNotSave() throws Exception {
        String savedBefore = Files.readString(dataFile);
        List<Person> filteredBefore = List.copyOf(model.getFilteredPersonList());
        assertThrows(CommandException.class, () -> logic.execute(importCommand(testFolder.resolve("missing.csv"))));
        assertEquals(savedBefore, Files.readString(dataFile));
        assertEquals(filteredBefore, model.getFilteredPersonList());
    }

    @Test
    public void execute_sourceIsSaveFile_rejectsBeforeChangingRecordsOrFile() throws Exception {
        Files.writeString(dataFile, HEADER + FIRST_ROW);
        assertRejectedWithoutChanges(dataFile);
        assertRejectedWithoutChanges(dataFile.getParent().resolve(".").resolve(dataFile.getFileName()));
    }

    @Test
    public void execute_sourceIsHardLinkToSaveFile_rejectsBeforeChangingRecordsOrFile() throws Exception {
        Files.writeString(dataFile, HEADER + FIRST_ROW);
        Path source = Files.createLink(testFolder.resolve("linked.csv"), dataFile);
        assertRejectedWithoutChanges(source);
    }

    @Test
    public void execute_saveFailure_reportsErrorAndRetainsExistingPersistenceBehavior() throws Exception {
        JsonAddressBookStorage failingStorage = new JsonAddressBookStorage(dataFile) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                throw new IOException("Simulated save failure");
            }
        };
        Logic failingLogic = createLogic(failingStorage);
        Path file = writeCsv(HEADER + FIRST_ROW);
        String savedBefore = Files.readString(dataFile);
        String sourceBefore = Files.readString(file);

        CommandException error = assertThrows(CommandException.class, () -> failingLogic.execute(importCommand(file)));

        assertEquals(String.format(LogicManager.FILE_OPS_ERROR_FORMAT, "Simulated save failure"), error.getMessage());
        assertEquals(2, model.getAddressBook().getPersonList().size());
        assertEquals(savedBefore, Files.readString(dataFile));
        assertEquals(sourceBefore, Files.readString(file));
    }

    private void assertRejectedWithoutChanges(Path file) throws Exception {
        AddressBook before = new AddressBook(model.getAddressBook());
        List<Person> filteredBefore = List.copyOf(model.getFilteredPersonList());
        String savedBefore = Files.readString(dataFile);
        byte[] sourceBefore = Files.readAllBytes(file);

        CommandException error = assertThrows(CommandException.class, () -> logic.execute(importCommand(file)));

        assertTrue(error.getMessage().contains("No members were imported."));
        assertEquals(before, model.getAddressBook());
        assertEquals(filteredBefore, model.getFilteredPersonList());
        assertEquals(savedBefore, Files.readString(dataFile));
        assertArrayEquals(sourceBefore, Files.readAllBytes(file));
    }

    private Logic createLogic(JsonAddressBookStorage addressBookStorage) {
        return new LogicManager(model, new StorageManager(addressBookStorage,
                new JsonUserPrefsStorage(testFolder.resolve("preferences.json"))));
    }

    private Path writeCsv(String contents) throws IOException {
        return Files.writeString(testFolder.resolve("member contacts.csv"), contents);
    }

    private String importCommand(Path file) {
        return "import \"" + file + "\"";
    }
}
