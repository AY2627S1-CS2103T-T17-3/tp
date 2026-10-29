package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import seedu.address.logic.commands.ClearCommand;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.StatusFilter;
import seedu.address.model.person.Person;
import seedu.address.model.person.Status;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/**
 * Checks that clearing requires approval before changing records or saving.
 */
public class ClearCommandIntegrationTest {
    @TempDir
    public Path temporaryFolder;

    private final ModelManager model = new ModelManager();
    private final Person active = new PersonBuilder().withEmail("active@u.nus.edu").build();
    private final Person archived = new PersonBuilder().withEmail("archived@u.nus.edu")
            .withStatus(Status.ARCHIVED).build();
    private final Predicate<Person> search = person -> person.getEmail().equals(active.getEmail());
    private Path dataFile;
    private JsonAddressBookStorage storage;
    private Logic logic;
    private byte[] savedBefore;

    @BeforeEach
    public void setUp() throws IOException {
        model.addPerson(active);
        model.addPerson(archived);
        model.updateFilteredPersonList(search);
        model.setStatusFilter(StatusFilter.ACTIVE);
        dataFile = temporaryFolder.resolve("members.json");
        storage = new JsonAddressBookStorage(dataFile);
        storage.saveAddressBook(model.getAddressBook());
        savedBefore = Files.readAllBytes(dataFile);
        logic = createLogic(storage);
    }

    @Test
    public void execute_invalidClear_rejectsBeforeConfirmationAndPreservesState() throws Exception {
        for (String command : List.of("clear 1", "clear all", "clear member", "clear extra_text", "clear\t1")) {
            ParseException failure = assertThrows(ParseException.class, () -> logic.execute(command, () -> {
                throw new AssertionError("Invalid clear must not request confirmation.");
            }));
            assertEquals(String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT, ClearCommand.MESSAGE_USAGE),
                    failure.getMessage());
            assertOriginalState();
        }
    }

    @Test
    public void execute_withoutConfirmation_cancelsAndPreservesState() throws Exception {
        assertEquals(new CommandResult(ClearCommand.MESSAGE_CANCELLED), logic.execute("clear"));
        assertOriginalState();
    }

    @Test
    public void execute_cancelledClear_skipsSaveAndPreservesState() throws Exception {
        logic = createLogic(new JsonAddressBookStorage(dataFile) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) {
                throw new AssertionError("Cancelled clear must not attempt to save.");
            }
        });
        AtomicInteger confirmations = new AtomicInteger();

        CommandResult result = logic.execute("clear", () -> {
            confirmations.incrementAndGet();
            return false;
        });

        assertEquals(new CommandResult(ClearCommand.MESSAGE_CANCELLED), result);
        assertEquals(1, confirmations.get());
        assertOriginalState();
    }

    @Test
    public void execute_cancelledClear_doesNotCreateMissingFile() throws Exception {
        Files.delete(dataFile);

        assertEquals(new CommandResult(ClearCommand.MESSAGE_CANCELLED), logic.execute("clear", () -> false));

        assertFalse(Files.exists(dataFile));
        assertEquals(List.of(active, archived), model.getAddressBook().getPersonList());
    }

    @Test
    public void execute_confirmedClear_clearsHiddenAndArchivedMembersAndPersistsEmptyDirectory() throws Exception {
        AtomicInteger confirmations = new AtomicInteger();
        AtomicInteger saves = new AtomicInteger();
        logic = createLogic(new JsonAddressBookStorage(dataFile) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                saves.incrementAndGet();
                super.saveAddressBook(addressBook);
            }
        });

        CommandResult result = logic.execute(" \tclear\r\n", () -> {
            assertEquals(List.of(active, archived), model.getAddressBook().getPersonList());
            assertEquals(List.of(active), model.getFilteredPersonList());
            assertEquals(0, saves.get());
            confirmations.incrementAndGet();
            return true;
        });

        assertEquals(new CommandResult(ClearCommand.MESSAGE_SUCCESS), result);
        assertEquals(1, confirmations.get());
        assertEquals(1, saves.get());
        assertEquals(List.of(), model.getAddressBook().getPersonList());
        assertEquals(List.of(), model.getFilteredPersonList());
        assertEquals(new AddressBook(), storage.readAddressBook().orElseThrow());
        assertEquals(StatusFilter.ACTIVE, model.getStatusFilter());
    }

    @Test
    public void execute_confirmedClearSaveFails_restoresRecordsSearchStatusAndFile() throws Exception {
        logic = createLogic(new JsonAddressBookStorage(dataFile) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                throw new IOException("Cannot save cleared directory.");
            }
        });

        CommandException failure = assertThrows(CommandException.class, () -> logic.execute("clear", () -> true));

        assertEquals(String.format(LogicManager.FILE_OPS_ERROR_FORMAT, "Cannot save cleared directory."),
                failure.getMessage());
        assertOriginalState();
    }

    @Test
    public void execute_savingDisabled_rejectsClearBeforeConfirmation() throws Exception {
        Files.writeString(dataFile, "invalid JSON");
        storage.loadAddressBook();

        CommandException failure = assertThrows(CommandException.class, () -> logic.execute("clear", () -> {
            throw new AssertionError("Protected data must be rejected before requesting confirmation.");
        }));

        assertEquals(JsonAddressBookStorage.MESSAGE_SAVE_DISABLED, failure.getMessage());
        assertEquals("invalid JSON", Files.readString(dataFile));
        assertEquals(List.of(active, archived), model.getAddressBook().getPersonList());
        assertEquals(List.of(active), model.getFilteredPersonList());
        assertSame(search, model.getFilteredPersonListPredicate());
        assertEquals(StatusFilter.ACTIVE, model.getStatusFilter());
    }

    @Test
    public void execute_otherCommands_doesNotRequestClearConfirmation() throws Exception {
        for (String command : List.of("list", "find e/active", "edit 1 n/Changed", "view archived", "help")) {
            logic.execute(command, () -> {
                throw new AssertionError("Only clear may request clear confirmation.");
            });
        }
    }

    private Logic createLogic(JsonAddressBookStorage memberStorage) {
        return new LogicManager(model, new StorageManager(memberStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
    }

    private void assertOriginalState() throws IOException {
        assertEquals(List.of(active, archived), model.getAddressBook().getPersonList());
        assertEquals(List.of(active), model.getFilteredPersonList());
        assertSame(search, model.getFilteredPersonListPredicate());
        assertEquals(StatusFilter.ACTIVE, model.getStatusFilter());
        assertArrayEquals(savedBefore, Files.readAllBytes(dataFile));
    }
}
