package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.TempDirFactory;

import com.fasterxml.jackson.databind.JsonNode;

import seedu.address.commons.util.JsonUtil;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.ImportCommand;
import seedu.address.logic.commands.exceptions.CommandException;
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
    private static final String EXPECTED_RECORD_NUMBERING = "Record numbers include the header as record 1. "
            + "Blank lines are skipped, and a quoted multiline cell counts as one record.";

    @TempDir(factory = BuildDirectoryTempDirFactory.class)
    public Path testFolder;

    private Model model;
    private Logic logic;
    private JsonAddressBookStorage storage;
    private Path dataFile;
    private Path applicationHome;
    private Path applicationJar;

    @BeforeEach
    public void setUp() throws Exception {
        applicationHome = Files.createDirectory(testFolder.resolve("app with spaces"));
        applicationJar = Files.createFile(applicationHome.resolve("NUSocietyDesk.jar"));
        dataFile = applicationHome.resolve("nusocietydesk.json");
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
    public void execute_punctuatedNames_preservesSpellingAndSourceAfterReload() throws Exception {
        Path file = writeCsv(HEADER + """
                Anne-Marie Tan,,hyphen@u.nus.edu,,,active
                O'Connor Tan,,apostrophe@u.nus.edu,,,active
                J. P. Tan,,initials@u.nus.edu,,,active
                Ravi S/O Kumar,,son@u.nus.edu,,,active
                Devi D/O Kumar,,daughter@u.nus.edu,,,active
                "Tan, Alex",,comma@u.nus.edu,,,active
                Tan (Alex),,parentheses@u.nus.edu,,,active
                Tan @ Alex,,alias@u.nus.edu,,,active
                """);
        byte[] originalSource = Files.readAllBytes(file);
        Person existing = model.getAddressBook().getPersonList().getFirst();

        CommandResult result = logic.execute(importCommand(file));

        assertEquals(String.format(ImportCommand.MESSAGE_SUCCESS, 8, 0, file), result.getFeedbackToUser());
        ReadOnlyAddressBook reloaded = new JsonAddressBookStorage(dataFile).readAddressBook().orElseThrow();
        assertEquals(model.getAddressBook(), reloaded);
        assertEquals(existing, reloaded.getPersonList().getFirst());
        assertEquals(List.of("Existing Member", "Anne-Marie Tan", "O'Connor Tan", "J. P. Tan", "Ravi S/O Kumar",
                "Devi D/O Kumar", "Tan, Alex", "Tan (Alex)", "Tan @ Alex"), reloaded.getPersonList().stream()
                .map(person -> person.getName().orElseThrow().fullName).toList());
        assertArrayEquals(originalSource, Files.readAllBytes(file));
    }

    @Test
    public void execute_activeImportInArchivedView_reportsHiddenMemberAndPersistsImport() throws Exception {
        logic.execute("view archived");
        Path file = writeCsv(HEADER + FIRST_ROW);
        String sourceBefore = Files.readString(file);

        CommandResult result = logic.execute(importCommand(file));

        assertEquals(String.format(ImportCommand.MESSAGE_SUCCESS, 1, 0, file)
                + "\n1 imported member(s) are hidden by the current view. Use view all to see them.",
                result.getFeedbackToUser());
        assertEquals(StatusFilter.ARCHIVED, model.getStatusFilter());
        assertTrue(model.getFilteredPersonList().isEmpty());
        ReadOnlyAddressBook reloaded = storage.readAddressBook().orElseThrow();
        assertEquals(model.getAddressBook(), reloaded);
        assertEquals(2, reloaded.getPersonList().size());
        assertEquals("alex@u.nus.edu", reloaded.getPersonList().getLast().getEmail().value);
        assertEquals(sourceBefore, Files.readString(file));

        logic.execute("view all");
        assertEquals(reloaded.getPersonList(), model.getFilteredPersonList());
    }

    @Test
    public void execute_unicodeBlankAddress_savesNullAndPreservesSource() throws Exception {
        Path file = writeCsv(HEADER + ",,blank@u.nus.edu,\"\u3000\u2003\u00a0\",,active\n");
        byte[] sourceBefore = Files.readAllBytes(file);

        CommandResult result = logic.execute(importCommand(file));

        assertEquals(String.format(ImportCommand.MESSAGE_SUCCESS, 1, 0, file), result.getFeedbackToUser());
        ReadOnlyAddressBook reloaded = storage.readAddressBook().orElseThrow();
        assertEquals(model.getAddressBook(), reloaded);
        assertEquals(2, reloaded.getPersonList().size());
        assertEquals("blank@u.nus.edu", reloaded.getPersonList().get(1).getEmail().value);
        assertTrue(reloaded.getPersonList().get(1).getAddress().isEmpty());
        assertTrue(JsonUtil.fromJsonString(Files.readString(dataFile), JsonNode.class)
                .get("persons").get(1).get("address").isNull());
        assertArrayEquals(sourceBefore, Files.readAllBytes(file));
    }

    @Test
    public void execute_missingSaveFile_createsSaveFileAndPersistsImport() throws Exception {
        Path file = writeCsv(HEADER + FIRST_ROW);
        String sourceBefore = Files.readString(file);
        Files.delete(dataFile);
        assertFalse(Files.exists(dataFile));

        CommandResult result = logic.execute(importCommand(file));

        assertEquals(String.format(ImportCommand.MESSAGE_SUCCESS, 1, 0, file), result.getFeedbackToUser());
        assertEquals(model.getAddressBook(), storage.readAddressBook().orElseThrow());
        assertEquals(2, model.getAddressBook().getPersonList().size());
        assertEquals("alex@u.nus.edu", model.getAddressBook().getPersonList().getLast().getEmail().value);
        assertEquals(model.getAddressBook().getPersonList(), model.getFilteredPersonList());
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
    public void execute_fieldLengthLimits_importsBoundaryMemberAndRejectsOverlongRecords() throws Exception {
        String boundaryRow = String.join(",", "A".repeat(127), "9".repeat(15), "boundary@u.nus.edu",
                "A".repeat(200), "T".repeat(30), "active") + "\n";
        String rejectedRows = String.join("\n",
                "A".repeat(128) + ",,name@u.nus.edu,,,active",
                "," + "9".repeat(16) + ",phone@u.nus.edu,,,active",
                ",,address@u.nus.edu," + "A".repeat(201) + ",,active",
                ",,tag@u.nus.edu,," + "t".repeat(31) + ",active") + "\n";
        Path file = writeCsv(HEADER + boundaryRow + rejectedRows);
        String originalSource = Files.readString(file);

        assertEquals(String.format(ImportCommand.MESSAGE_SUCCESS, 1, 4, file)
                + "\n" + EXPECTED_RECORD_NUMBERING + "\nRejected records:\n"
                + "CSV record 3: " + Name.MESSAGE_CONSTRAINTS + "\n"
                + "CSV record 4: " + Phone.MESSAGE_CONSTRAINTS + "\n"
                + "CSV record 5: " + Address.MESSAGE_CONSTRAINTS + "\n"
                + "CSV record 6: " + Tag.MESSAGE_CONSTRAINTS,
                logic.execute(importCommand(file)).getFeedbackToUser());
        assertEquals(model.getAddressBook(), storage.readAddressBook().orElseThrow());
        Person boundaryMember = storage.readAddressBook().orElseThrow().getPersonList().getLast();
        assertEquals(new Name("A".repeat(127)), boundaryMember.getName().orElseThrow());
        assertEquals(new Phone("9".repeat(15)), boundaryMember.getPhone().orElseThrow());
        assertEquals(new Address("A".repeat(200)), boundaryMember.getAddress().orElseThrow());
        assertEquals(List.of(new Tag("t".repeat(30))), List.copyOf(boundaryMember.getTags()));
        assertEquals(originalSource, Files.readString(file));

        logic.execute("find Existing");
        file = writeCsv(HEADER + rejectedRows);
        AtomicInteger saveAttempts = new AtomicInteger();
        logic = createLogic(new JsonAddressBookStorage(dataFile) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) {
                saveAttempts.incrementAndGet();
            }
        });
        assertRejectedWithoutChanges(file);
        assertEquals(0, saveAttempts.get());
    }

    @Test
    public void execute_overlongEmails_reportsRecordErrorsAndImportsBoundaryEmail() throws Exception {
        String boundaryEmail = "a".repeat(64) + "@u.nus.edu";
        String rejectedRow = ",," + "a".repeat(65) + "@u.nus.edu,,,active\n";
        Path file = writeCsv(HEADER + ",, " + boundaryEmail + " ,,,active\n" + rejectedRow);
        String originalSource = Files.readString(file);
        String rejection = "CSV record 3: Email local-part must contain at most 64 characters; received 65.";

        assertEquals(String.format(ImportCommand.MESSAGE_SUCCESS, 1, 1, file)
                + "\n" + EXPECTED_RECORD_NUMBERING + "\nRejected records:\n" + rejection,
                logic.execute(importCommand(file)).getFeedbackToUser());
        assertEquals(boundaryEmail, storage.readAddressBook().orElseThrow().getPersonList().getLast().getEmail().value);
        assertEquals(originalSource, Files.readString(file));

        file = writeCsv(HEADER + rejectedRow);
        String originalFile = Files.readString(dataFile);
        AddressBook originalRecords = new AddressBook(model.getAddressBook());
        logic.execute("find Existing");
        List<Person> originalResults = List.copyOf(model.getFilteredPersonList());
        AtomicInteger saveAttempts = new AtomicInteger();
        logic = createLogic(new JsonAddressBookStorage(dataFile) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) {
                saveAttempts.incrementAndGet();
            }
        });
        String command = importCommand(file);
        CommandException error = assertThrows(CommandException.class, () -> logic.execute(command));
        assertEquals(String.format(ImportCommand.MESSAGE_SUCCESS, 0, 1, file)
                + "\n" + EXPECTED_RECORD_NUMBERING
                + "\nRejected records:\nCSV record 2: Email local-part must contain at most 64 characters; received 65."
                + "\nNo members were imported.", error.getMessage());
        assertEquals(originalRecords, model.getAddressBook());
        assertEquals(originalResults, model.getFilteredPersonList());
        assertEquals(originalFile, Files.readString(dataFile));
        assertEquals(HEADER + rejectedRow, Files.readString(file));
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
    public void execute_parentTraversal_rejectsOutsideSourceWithoutChanges() throws Exception {
        Files.writeString(testFolder.resolve("outside.csv"), HEADER + FIRST_ROW);
        Path source = applicationHome.resolve("..").resolve("outside.csv");

        assertEquals(ImportCommand.MESSAGE_SOURCE_OUTSIDE_HOME, assertRejectedWithoutChanges(source).getMessage());
    }

    @Test
    public void execute_relativeOutsideSource_rejectsWithoutChanges() throws Exception {
        Path outside = Files.writeString(testFolder.resolve("outside.csv"), HEADER + FIRST_ROW);
        Path source = Path.of("").toAbsolutePath().relativize(outside);

        assertEquals(ImportCommand.MESSAGE_SOURCE_OUTSIDE_HOME, assertRejectedWithoutChanges(source).getMessage());
    }

    @Test
    public void execute_absoluteOutsideSource_rejectsBeforeDecodingOrParsing() throws Exception {
        Path source = testFolder.resolve("outside.csv");
        Files.write(source, new byte[] {(byte) 0xc3, (byte) 0x28});

        assertEquals(ImportCommand.MESSAGE_SOURCE_OUTSIDE_HOME, assertRejectedWithoutChanges(source).getMessage());
    }

    @Test
    public void execute_outsideSource_doesNotAttemptSaving() throws Exception {
        AtomicInteger saveAttempts = new AtomicInteger();
        logic = createLogic(new JsonAddressBookStorage(dataFile) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) {
                saveAttempts.incrementAndGet();
            }
        });
        Path source = Files.writeString(testFolder.resolve("outside.csv"), HEADER + FIRST_ROW);

        assertEquals(ImportCommand.MESSAGE_SOURCE_OUTSIDE_HOME, assertRejectedWithoutChanges(source).getMessage());
        assertEquals(0, saveAttempts.get());
    }

    @Test
    public void execute_similarlyNamedSibling_rejectsOutsideSourceWithoutChanges() throws Exception {
        Path sibling = Files.createDirectory(testFolder.resolve("app with spaces sibling"));
        Path source = Files.writeString(sibling.resolve("members.csv"), HEADER + FIRST_ROW);

        assertEquals(ImportCommand.MESSAGE_SOURCE_OUTSIDE_HOME, assertRejectedWithoutChanges(source).getMessage());
    }

    @Test
    public void execute_relativeSourceInsideHome_savesMembers() throws Exception {
        Path source = Path.of("").toAbsolutePath().relativize(writeCsv(HEADER + FIRST_ROW));

        logic.execute(importCommand(source));

        assertEquals(2, storage.readAddressBook().orElseThrow().getPersonList().size());
    }

    @Test
    public void execute_sourceInSubfolder_savesMembers() throws Exception {
        Path folder = Files.createDirectory(applicationHome.resolve("imports"));
        Path source = Files.writeString(folder.resolve("members.csv"), HEADER + FIRST_ROW);

        logic.execute(importCommand(source));

        assertEquals(2, storage.readAddressBook().orElseThrow().getPersonList().size());
    }

    @Test
    public void execute_parentTraversalBackIntoHome_savesMembers() throws Exception {
        Files.createDirectory(applicationHome.resolve("imports"));
        Path source = writeCsv(HEADER + FIRST_ROW);
        Path alias = applicationHome.resolve("imports").resolve("..").resolve(source.getFileName());

        logic.execute(importCommand(alias));

        assertEquals(2, storage.readAddressBook().orElseThrow().getPersonList().size());
    }

    @Test
    public void execute_symbolicLinkToOutsideFile_rejectsWithoutChanges() throws Exception {
        Path target = Files.writeString(testFolder.resolve("outside.csv"), HEADER + FIRST_ROW);
        Path source = createSymbolicLink(applicationHome.resolve("linked.csv"), target);

        assertEquals(ImportCommand.MESSAGE_SOURCE_OUTSIDE_HOME, assertRejectedWithoutChanges(source).getMessage());
    }

    @Test
    public void execute_symbolicLinkToOutsideFolder_rejectsWithoutChanges() throws Exception {
        Path folder = Files.createDirectory(testFolder.resolve("outside"));
        Files.writeString(folder.resolve("members.csv"), HEADER + FIRST_ROW);
        Path link = createSymbolicLink(applicationHome.resolve("linked"), folder);

        assertEquals(ImportCommand.MESSAGE_SOURCE_OUTSIDE_HOME,
                assertRejectedWithoutChanges(link.resolve("members.csv")).getMessage());
    }

    @Test
    public void execute_symbolicLinkInsideHome_savesMembers() throws Exception {
        Path target = writeCsv(HEADER + FIRST_ROW);
        Path source = createSymbolicLink(applicationHome.resolve("linked.csv"), target);

        logic.execute(importCommand(source));

        assertEquals(2, storage.readAddressBook().orElseThrow().getPersonList().size());
    }

    @Test
    public void execute_symbolicLinkHome_savesMembersUsingRealHome() throws Exception {
        Path link = createSymbolicLink(testFolder.resolve("linked-app"), applicationHome);
        Path source = writeCsv(HEADER + FIRST_ROW);
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(applicationHome.resolve("preferences.json"))),
                link.resolve(applicationJar.getFileName()).toUri().toURL());

        logic.execute(importCommand(source));

        assertEquals(2, storage.readAddressBook().orElseThrow().getPersonList().size());
    }

    @Test
    public void execute_classDirectoryRun_allowsWorkingDirectorySource() throws Exception {
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(applicationHome.resolve("preferences.json"))));

        logic.execute(importCommand(Path.of("docs", "samples", "members.csv")));

        assertEquals(3, storage.readAddressBook().orElseThrow().getPersonList().size());
    }

    @Test
    public void execute_classDirectoryRun_rejectsSourceOutsideWorkingDirectory(@TempDir Path outsideFolder)
            throws Exception {
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(applicationHome.resolve("preferences.json"))));
        Path source = Files.writeString(outsideFolder.resolve("members.csv"), HEADER + FIRST_ROW);

        assertEquals(ImportCommand.MESSAGE_SOURCE_OUTSIDE_HOME, assertRejectedWithoutChanges(source).getMessage());
    }

    @Test
    @SuppressWarnings("deprecation") // Deliberately create a URL that cannot be converted to a URI.
    public void execute_invalidApplicationLocation_rejectsWithoutChanges() throws Exception {
        Path source = writeCsv(HEADER + FIRST_ROW);
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(applicationHome.resolve("preferences.json"))),
                new URL("file:/invalid application.jar"));

        CommandException error = assertRejectedWithoutChanges(source);

        assertEquals(String.format(ImportCommand.MESSAGE_FAILURE, source,
                "Could not determine the application home folder."), error.getMessage());
        IOException cause = assertInstanceOf(IOException.class, error.getCause());
        assertInstanceOf(URISyntaxException.class, cause.getCause());
    }

    @Test
    public void execute_sourceIsSaveFile_rejectsBeforeChangingRecordsOrFile() throws Exception {
        Files.writeString(dataFile, HEADER + FIRST_ROW);
        assertEquals(ImportCommand.MESSAGE_SOURCE_IS_DATA_FILE, assertRejectedWithoutChanges(dataFile).getMessage());
        assertEquals(ImportCommand.MESSAGE_SOURCE_IS_DATA_FILE,
                assertRejectedWithoutChanges(dataFile.getParent().resolve(".")
                        .resolve(dataFile.getFileName())).getMessage());
    }

    @Test
    public void execute_sourceIsHardLinkToSaveFile_rejectsBeforeChangingRecordsOrFile() throws Exception {
        Files.writeString(dataFile, HEADER + FIRST_ROW);
        Path source = Files.createLink(applicationHome.resolve("linked.csv"), dataFile);
        assertEquals(ImportCommand.MESSAGE_SOURCE_IS_DATA_FILE, assertRejectedWithoutChanges(source).getMessage());
    }

    @Test
    public void execute_sourceIsSymbolicLinkToSaveFile_rejectsBeforeChangingRecordsOrFile() throws Exception {
        Files.writeString(dataFile, HEADER + FIRST_ROW);
        Path source = createSymbolicLink(applicationHome.resolve("linked.csv"), dataFile);

        assertEquals(ImportCommand.MESSAGE_SOURCE_IS_DATA_FILE, assertRejectedWithoutChanges(source).getMessage());
    }

    @Test
    public void execute_saveFailure_restoresRecordsAndFilterAndPreservesFiles() throws Exception {
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
        AddressBook recordsBefore = new AddressBook(model.getAddressBook());
        List<Person> filteredBefore = List.copyOf(model.getFilteredPersonList());

        CommandException error = assertThrows(CommandException.class, () -> failingLogic.execute(importCommand(file)));

        assertEquals(String.format(LogicManager.FILE_OPS_ERROR_FORMAT, "Simulated save failure"), error.getMessage());
        assertEquals(recordsBefore, model.getAddressBook());
        assertEquals(filteredBefore, model.getFilteredPersonList());
        assertEquals(savedBefore, Files.readString(dataFile));
        assertEquals(sourceBefore, Files.readString(file));
    }

    private CommandException assertRejectedWithoutChanges(Path file) throws Exception {
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
        return error;
    }

    private Logic createLogic(JsonAddressBookStorage addressBookStorage) throws Exception {
        return new LogicManager(model, new StorageManager(addressBookStorage,
                new JsonUserPrefsStorage(applicationHome.resolve("preferences.json"))), applicationJar.toUri().toURL());
    }

    private Path writeCsv(String contents) throws IOException {
        return Files.writeString(applicationHome.resolve("member contacts.csv"), contents);
    }

    private Path createSymbolicLink(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            assumeTrue(false, "Creating symbolic links is unavailable: " + e.getClass().getSimpleName());
        }
        return link;
    }

    private String importCommand(Path file) {
        return "import \"" + file + "\"";
    }

    /**
     * Keeps fixtures on the working directory's drive so relative paths are valid on Windows.
     * JUnit still creates and cleans up a separate directory for each test.
     */
    public static class BuildDirectoryTempDirFactory implements TempDirFactory {
        @Override
        public Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext)
                throws IOException {
            Path buildFolder = Files.createDirectories(Path.of("build").toAbsolutePath());
            return Files.createTempDirectory(buildFolder, "import-test-");
        }
    }
}
