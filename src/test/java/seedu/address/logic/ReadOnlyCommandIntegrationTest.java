package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.ExitCommand;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.StatusFilter;
import seedu.address.model.person.Person;
import seedu.address.model.person.Status;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Verifies that retrieving loaded members and exiting do not depend on saving. */
public class ReadOnlyCommandIntegrationTest {
    @TempDir
    public Path temporaryFolder;

    private final Person alex = new PersonBuilder().withName("Alex Tan").withEmail("alex@u.nus.edu").build();
    private final Person bob = new PersonBuilder().withName("Bob Lee").withEmail("bob@u.nus.edu")
            .withTags("exco").build();
    private final Person archivedBob = new PersonBuilder().withoutName().withoutPhone().withoutAddress()
            .withEmail("bob.former@u.nus.edu").withStatus(Status.ARCHIVED).build();
    private final List<Person> members = List.of(alex, bob, archivedBob);
    private final Model model = new ModelManager();
    private Path dataFile;
    private Logic logic;

    @BeforeEach
    public void setUp() throws Exception {
        dataFile = temporaryFolder.resolve("nusocietydesk.json");
        members.forEach(model::addPerson);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(dataFile);
        storage.saveAddressBook(model.getAddressBook());
        // Preserve noncanonical whitespace too: a needless rewrite must be observable.
        Files.writeString(dataFile, Files.readString(dataFile) + "\n");
        logic = createLogic(storage);
    }

    @Test
    public void execute_searchAndList_skipSavingAndRespectStatus() throws Exception {
        byte[] savedBefore = Files.readAllBytes(dataFile);
        logic = createLogic(new JsonAddressBookStorage(dataFile) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) {
                throw new AssertionError("Read-only commands must not attempt saving.");
            }
        });

        assertSearchAndListResults();

        assertArrayEquals(savedBefore, Files.readAllBytes(dataFile));
    }

    @Test
    public void execute_exitAndHelp_skipSavingAndPreserveSession() throws Exception {
        byte[] savedBefore = Files.readAllBytes(dataFile);
        logic = createLogic(new JsonAddressBookStorage(dataFile) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) {
                throw new AssertionError("Exit and Help must not attempt saving.");
            }
        });
        Predicate<Person> search = person -> person.equals(archivedBob);
        model.updateFilteredPersonList(search);
        model.setStatusFilter(StatusFilter.ARCHIVED);

        assertEquals(new CommandResult(ExitCommand.MESSAGE_EXIT_ACKNOWLEDGEMENT, false, true), logic.execute("exit"));
        assertEquals(new CommandResult(HelpCommand.SHOWING_HELP_MESSAGE, true, false), logic.execute("help"));

        assertSame(search, model.getFilteredPersonListPredicate());
        assertEquals(StatusFilter.ARCHIVED, model.getStatusFilter());
        assertEquals(List.of(archivedBob), model.getFilteredPersonList());
        assertEquals(members, model.getAddressBook().getPersonList());
        assertArrayEquals(savedBefore, Files.readAllBytes(dataFile));
    }

    @Test
    public void execute_readOnlyCommands_doNotCreateMissingDataFile() throws Exception {
        Files.delete(dataFile);

        assertSearchAndListResults();
        assertEquals(new CommandResult(ExitCommand.MESSAGE_EXIT_ACKNOWLEDGEMENT, false, true), logic.execute("exit"));
        logic.execute("help");
        logic.execute("view all");

        assertFalse(Files.exists(dataFile));
    }

    @Test
    public void execute_readOnlyCommands_remainAvailableWhenSavingIsDisabled() throws Exception {
        Files.writeString(dataFile, "invalid JSON");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(dataFile);
        storage.loadAddressBook();
        logic = createLogic(storage);

        assertSearchAndListResults();
        assertEquals(new CommandResult(ExitCommand.MESSAGE_EXIT_ACKNOWLEDGEMENT, false, true), logic.execute("exit"));
        assertEquals(new CommandResult(HelpCommand.SHOWING_HELP_MESSAGE, true, false), logic.execute("help"));
        CommandException failure = assertThrows(CommandException.class, () -> logic.execute("clear"));

        assertEquals(JsonAddressBookStorage.MESSAGE_SAVE_DISABLED, failure.getMessage());
        assertEquals(members, model.getAddressBook().getPersonList());
        assertEquals("invalid JSON", Files.readString(dataFile));
    }

    @Test
    public void execute_readOnlyCommands_nonemptyDirectoryDoesNotBlockCommands() throws Exception {
        Path backup = temporaryFolder.resolve("saved.json");
        Files.move(dataFile, backup);
        byte[] savedBefore = Files.readAllBytes(backup);
        Files.createDirectory(dataFile);
        Path obstacle = Files.writeString(dataFile.resolve("keep.txt"), "Keep this file.");

        assertSearchAndListResults();
        assertEquals(new CommandResult(ExitCommand.MESSAGE_EXIT_ACKNOWLEDGEMENT, false, true), logic.execute("exit"));
        logic.execute("help");
        logic.execute("view all");

        assertEquals("Keep this file.", Files.readString(obstacle));
        assertArrayEquals(savedBefore, Files.readAllBytes(backup));
    }

    @Test
    public void execute_memberChanges_saveFailureRestoresRecordsAndSearch() throws Exception {
        Files.delete(dataFile);
        Files.createDirectory(dataFile);
        Path obstacle = Files.writeString(dataFile.resolve("keep.txt"), "Keep this file.");
        Path applicationJar = Files.createFile(temporaryFolder.resolve("NUSocietyDesk.jar"));
        String csvContent = "name,phone,email,address,tags,status\n,,imported@u.nus.edu,,,active\n";
        Path csv = Files.writeString(temporaryFolder.resolve("members.csv"), csvContent);
        logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json"))), applicationJar.toUri().toURL());
        for (String command : List.of("add e/new@u.nus.edu", "edit 1 n/Changed Name", "delete 1", "clear",
                "archive 1", "activate 1", "tag 1 t/newtag", "untag 1 t/exco", "import \"" + csv + "\"")) {
            boolean isActivate = command.startsWith("activate");
            Person target = isActivate ? archivedBob : bob;
            StatusFilter status = isActivate ? StatusFilter.ARCHIVED : StatusFilter.ACTIVE;
            Predicate<Person> search = person -> person.equals(target);
            model.updateFilteredPersonList(search);
            model.setStatusFilter(status);
            CommandException failure = assertThrows(CommandException.class, () ->
                    logic.execute(command, () -> true), command);

            assertTrue(failure.getMessage().contains(LogicManager.MESSAGE_SAVE_REVERTED), failure.getMessage());
            assertEquals(members, model.getAddressBook().getPersonList(), command);
            assertEquals(List.of(target), model.getFilteredPersonList(), command);
            assertSame(search, model.getFilteredPersonListPredicate(), command);
            assertEquals(status, model.getStatusFilter(), command);
            assertEquals("Keep this file.", Files.readString(obstacle), command);
            assertEquals(csvContent, Files.readString(csv), command);
        }
    }

    private Logic createLogic(JsonAddressBookStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json"))));
    }

    private void assertSearchAndListResults() throws Exception {
        for (StatusFilter status : StatusFilter.values()) {
            model.setStatusFilter(status);
            List<Person> matches = switch (status) {
                case ALL -> List.of(bob, archivedBob);
                case ACTIVE -> List.of(bob);
                case ARCHIVED -> List.of(archivedBob);
            };
            for (String command : List.of("find e/bob", "findall e/bob", "findany e/bob")) {
                model.updateFilteredPersonList(person -> person.equals(alex));

                assertEquals(new CommandResult(String.format(Messages.MESSAGE_PERSONS_LISTED_OVERVIEW, matches.size())),
                        logic.execute(command), command + " in " + status);
                assertEquals(matches, model.getFilteredPersonList(), command + " in " + status);
                assertEquals(members, model.getAddressBook().getPersonList());
                assertEquals(status, model.getStatusFilter());
            }

            assertEquals(new CommandResult(ListCommand.MESSAGE_SUCCESS), logic.execute("list"));
            List<Person> listed = switch (status) {
                case ALL -> members;
                case ACTIVE -> List.of(alex, bob);
                case ARCHIVED -> List.of(archivedBob);
            };
            assertEquals(listed, model.getFilteredPersonList());
            assertEquals(members, model.getAddressBook().getPersonList());
            assertEquals(status, model.getStatusFilter());
        }
    }
}
