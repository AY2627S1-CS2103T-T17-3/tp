package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.model.person.Status;
import seedu.address.testutil.PersonBuilder;

/**
 * Verifies that command recovery and safe file replacement work together.
 */
public class PersistenceIntegrationTest {
    @TempDir
    public Path temporaryFolder;

    private Path dataFile;
    private JsonAddressBookStorage savedStorage;
    private AddressBook original;
    private Person visibleMember;
    private Person hiddenMember;
    private Model model;

    @BeforeEach
    public void setUp() throws Exception {
        dataFile = temporaryFolder.resolve("members.json");
        visibleMember = new PersonBuilder().withName("Visible Member").withEmail("visible@u.nus.edu")
                .withTags("Exco").build();
        hiddenMember = new PersonBuilder().withName("Hidden Member").withEmail("hidden@u.nus.edu").build();
        original = new AddressBook();
        original.addPerson(visibleMember);
        original.addPerson(hiddenMember);
        savedStorage = new JsonAddressBookStorage(dataFile);
        savedStorage.saveAddressBook(original);
        resetModel();
    }

    @Test
    public void execute_mutatingCommandsWithReplacementFailure_restoresRecordsFilterAndSavedData() throws Exception {
        for (String command : List.of("add e/new@u.nus.edu", "edit 1 n/Changed Member", "delete 1", "clear",
                "archive 1", "tag 1 t/Volunteer", "untag 1 t/Exco")) {
            resetModel();
            assertSaveFailure(command, failingWriter(false));
        }
    }

    @Test
    public void execute_activateWithSaveFailure_restoresArchivedStatus() throws Exception {
        original.setPerson(visibleMember, visibleMember.withStatus(Status.ARCHIVED));
        savedStorage.saveAddressBook(original);
        resetModel();

        assertSaveFailure("activate 1", failingWriter(false));

        assertEquals(Status.ARCHIVED, model.getFilteredPersonList().getFirst().getStatus());
    }

    @Test
    public void execute_partialTemporaryWrite_restoresRecordsAndFilterAndPreservesSavedData() throws Exception {
        assertSaveFailure("edit 1 e/changed@u.nus.edu", failingWriter(true));
    }

    @Test
    public void execute_saveFailure_restoresPredicateForLaterChanges() throws Exception {
        assertSaveFailure("clear", failingWriter(false));

        Person laterMatch = new PersonBuilder(hiddenMember).withName("Visible Later").build();
        model.setPerson(hiddenMember, laterMatch);

        assertEquals(List.of(visibleMember, laterMatch), model.getFilteredPersonList());
    }

    @Test
    public void execute_saveFailureWithNoMatches_restoresEmptySearch() throws Exception {
        model.updateFilteredPersonList(person -> false);

        assertSaveFailure("add e/new@u.nus.edu", failingWriter(false));

        assertTrue(model.getFilteredPersonList().isEmpty());
    }

    @Test
    public void execute_readOnlyCommandsWithReplacementFailure_applySearchWithoutSaving() throws Exception {
        byte[] savedBefore = Files.readAllBytes(dataFile);
        LogicManager logic = createLogic(new JsonAddressBookStorage(dataFile, failingWriter(false)));

        logic.execute("list");
        assertEquals(List.of(visibleMember, hiddenMember), model.getFilteredPersonList());
        for (String command : List.of("find e/hidden", "findall e/hidden", "findany e/hidden")) {
            resetModel();
            logic = createLogic(new JsonAddressBookStorage(dataFile, failingWriter(false)));
            logic.execute(command);
            assertEquals(List.of(hiddenMember), model.getFilteredPersonList());
        }
        assertTrue(logic.execute("exit").isExit());
        assertEquals(original, model.getAddressBook());
        assertArrayEquals(savedBefore, Files.readAllBytes(dataFile));
        try (var files = Files.list(temporaryFolder)) {
            assertEquals(List.of(dataFile), files.toList());
        }
    }

    @Test
    public void execute_retryAfterSaveFailure_savesOnceAndSurvivesReload() throws Exception {
        assertSaveFailure("add e/new@u.nus.edu", failingWriter(false));

        createLogic(savedStorage).execute("add e/new@u.nus.edu");

        assertEquals(3, model.getAddressBook().getPersonList().size());
        assertEquals(model.getAddressBook(), new JsonAddressBookStorage(dataFile).readAddressBook().orElseThrow());
        assertEquals(model.getAddressBook().getPersonList(), model.getFilteredPersonList());
    }

    @Test
    public void execute_permissionDenied_restoresStateAndExplainsRecovery() throws Exception {
        AtomicFileWriter writer = new AtomicFileWriter() {
            @Override
            void replaceFile(Path temporaryFile, Path destination) throws IOException {
                throw new AccessDeniedException(destination.toString());
            }
        };

        CommandException error = assertSaveFailure("delete 1", writer);

        assertTrue(error.getMessage().contains("insufficient permissions"));
    }

    @Test
    public void execute_securityException_restoresState() throws Exception {
        AtomicFileWriter writer = new AtomicFileWriter() {
            @Override
            void replaceFile(Path temporaryFile, Path destination) {
                throw new SecurityException("Simulated permission failure");
            }
        };

        assertSaveFailure("delete 1", writer);
    }

    @Test
    public void execute_saveFailure_logsRecoveryWithoutMemberDetails() throws Exception {
        Logger logger = LogsCenter.getLogger(LogicManager.class).getParent();
        List<String> messages = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                messages.add(record.getMessage());
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        logger.addHandler(handler);
        try {
            assertSaveFailure("add e/private@u.nus.edu", failingWriter(false));
        } finally {
            logger.removeHandler(handler);
        }

        assertTrue(messages.stream().anyMatch(message -> message.contains("Saving failed (IOException)")));
        assertTrue(messages.stream().anyMatch(message -> message.contains("restored member records")));
        assertFalse(String.join("\n", messages).contains("private@u.nus.edu"));
        assertFalse(String.join("\n", messages).contains(visibleMember.toString()));
    }

    private void resetModel() {
        model = new ModelManager(original, new UserPrefs());
        model.updateFilteredPersonList(person -> person.getName()
                .map(name -> name.fullName.startsWith("Visible")).orElse(false));
    }

    private AtomicFileWriter failingWriter(boolean failsWhileWriting) {
        return new AtomicFileWriter() {
            @Override
            void writeTemporaryFile(Path temporaryFile, String content) throws IOException {
                if (failsWhileWriting) {
                    Files.writeString(temporaryFile, "partial JSON");
                    throw new IOException("Simulated full disk");
                }
                super.writeTemporaryFile(temporaryFile, content);
            }

            @Override
            void replaceFile(Path temporaryFile, Path destination) throws IOException {
                throw new IOException("Simulated replacement failure");
            }
        };
    }

    private CommandException assertSaveFailure(String command, AtomicFileWriter writer) throws Exception {
        byte[] savedBefore = Files.readAllBytes(dataFile);
        List<Person> filteredBefore = List.copyOf(model.getFilteredPersonList());
        LogicManager logic = createLogic(new JsonAddressBookStorage(dataFile, writer));

        CommandException error = assertThrows(CommandException.class, () ->
                logic.execute(command, () -> true), command);

        assertTrue(error.getMessage().contains(LogicManager.MESSAGE_SAVE_REVERTED));
        assertEquals(original, model.getAddressBook(), command);
        assertEquals(filteredBefore, model.getFilteredPersonList(), command);
        assertArrayEquals(savedBefore, Files.readAllBytes(dataFile), command);
        assertEquals(original, savedStorage.readAddressBook().orElseThrow());
        try (var files = Files.list(temporaryFolder)) {
            assertEquals(List.of(dataFile), files.toList());
        }
        return error;
    }

    private LogicManager createLogic(JsonAddressBookStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json"))));
    }
}
