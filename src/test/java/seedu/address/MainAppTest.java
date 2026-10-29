package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.util.SampleDataUtil;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

public class MainAppTest {
    @TempDir
    public Path temporaryFolder;

    @Test
    public void recovery_unknownProperty_preservesFileUntilRepairedAndRestarted() throws Exception {
        Path file = temporaryFolder.resolve("members.json");
        Files.writeString(file, "{\"persons\":[{\"email\":\"alex@u.nus.edu\","
                + "\"phoneNumber\":\"91234567\"}]}");
        byte[] original = Files.readAllBytes(file);
        MainApp app = startModel(file);

        assertTrue(app.getDataLoadingWarning().contains("Record 1: unrecognized property \"phoneNumber\""));
        assertEquals(1, app.model.getAddressBook().getPersonList().size());
        app.logic.execute("list");
        assertThrows(CommandException.class, () -> app.logic.execute("add e/new@u.nus.edu"));
        app.stop();
        assertArrayEquals(original, Files.readAllBytes(file));

        Files.writeString(file, "{\"persons\":[{\"email\":\"alex@u.nus.edu\",\"phone\":\"91234567\"}]}");
        MainApp restarted = startModel(file);
        assertNull(restarted.getDataLoadingWarning());
        restarted.logic.execute("add e/new@u.nus.edu");
        assertEquals(2, restarted.storage.readAddressBook().orElseThrow().getPersonList().size());
        assertEquals("91234567", restarted.model.getAddressBook().getPersonList().get(0)
                .getPhone().orElseThrow().value);
    }

    @Test
    public void recovery_partialLoad_preservesFileAndRecordsThroughCommandsAndShutdown() throws Exception {
        Path file = temporaryFolder.resolve("members.json");
        Files.writeString(file, """
                {"persons":[{"email":"invalid-email"},{"email":"alice@u.nus.edu"}]}
                """);
        byte[] original = Files.readAllBytes(file);
        MainApp app = startModel(file);
        AddressBook recovered = new AddressBook(app.model.getAddressBook());

        assertEquals(1, recovered.getPersonList().size());
        assertTrue(app.getDataLoadingWarning().contains(file.toAbsolutePath().toString()));
        assertTrue(app.getDataLoadingWarning().contains("Recovered 1 member(s)"));
        assertTrue(app.getDataLoadingWarning().contains("Record 1:"));
        assertTrue(app.getDataLoadingWarning().contains("Repair the file and restart"));
        assertArrayEquals(original, Files.readAllBytes(file));

        app.logic.execute("find e/alice");
        assertEquals(1, app.logic.getFilteredPersonList().size());
        app.logic.execute("find e/nobody");
        assertTrue(app.logic.getFilteredPersonList().isEmpty());
        app.logic.execute("list");
        assertEquals(1, app.logic.getFilteredPersonList().size());
        assertEquals(new CommandResult(HelpCommand.SHOWING_HELP_MESSAGE, true, false), app.logic.execute("help"));
        assertArrayEquals(original, Files.readAllBytes(file));

        for (String command : List.of("add e/new@u.nus.edu", "edit 1 e/new@u.nus.edu", "delete 1",
                "clear", "archive 1", "activate 1", "tag 1 t/Friend", "untag 1 t/Friend",
                "import missing.csv")) {
            CommandException error = assertThrows(CommandException.class, () -> app.logic.execute(command));
            assertEquals(JsonAddressBookStorage.MESSAGE_SAVE_DISABLED, error.getMessage());
            assertEquals(recovered, app.model.getAddressBook());
            assertArrayEquals(original, Files.readAllBytes(file));
        }
        assertTrue(app.logic.execute("exit").isExit());
        app.stop();
        assertArrayEquals(original, Files.readAllBytes(file));
    }

    @Test
    public void recovery_failedOrAllInvalidLoad_showsProtectedEmptyView() throws Exception {
        Path file = temporaryFolder.resolve("members.json");
        for (String json : List.of("{", "{\"persons\":[{\"email\":\"invalid\"}]}")) {
            Files.writeString(file, json);
            byte[] original = Files.readAllBytes(file);
            MainApp app = startModel(file);

            assertTrue(app.model.getAddressBook().getPersonList().isEmpty());
            assertTrue(app.getDataLoadingWarning().contains("Recovered 0 member(s)"));
            assertFalse(app.storage.isAddressBookSaveAllowed());
            app.logic.execute("list");
            assertThrows(CommandException.class, () -> app.logic.execute("clear"));
            app.stop();
            assertArrayEquals(original, Files.readAllBytes(file));
        }
    }

    @Test
    public void startup_missingFile_usesSampleDataAndAllowsSaving() throws Exception {
        Path file = temporaryFolder.resolve("members.json");
        MainApp app = startModel(file);

        assertEquals(SampleDataUtil.getSampleAddressBook(), app.model.getAddressBook());
        assertNull(app.getDataLoadingWarning());
        assertTrue(app.storage.isAddressBookSaveAllowed());
        app.logic.execute("list");
        assertFalse(Files.exists(file));
        app.logic.execute("add e/new@u.nus.edu");
        assertEquals(app.model.getAddressBook(), app.storage.readAddressBook().orElseThrow());
    }

    @Test
    public void startup_validEmptyFile_staysEmptyAndAllowsChanges() throws Exception {
        Path file = temporaryFolder.resolve("members.json");
        Files.writeString(file, "{\"persons\":[]}");
        MainApp app = startModel(file);

        assertTrue(app.model.getAddressBook().getPersonList().isEmpty());
        assertNull(app.getDataLoadingWarning());
        app.logic.execute("add e/new@u.nus.edu");
        assertEquals(1, app.storage.readAddressBook().orElseThrow().getPersonList().size());
    }

    @Test
    public void startup_repairedFile_restoresNormalSaving() throws Exception {
        Path file = temporaryFolder.resolve("members.json");
        Files.writeString(file, "{");
        MainApp brokenApp = startModel(file);
        assertFalse(brokenApp.storage.isAddressBookSaveAllowed());

        Files.writeString(file, "{\"persons\":[{\"email\":\"alice@u.nus.edu\"}]}");
        MainApp restartedApp = startModel(file);

        assertNull(restartedApp.getDataLoadingWarning());
        restartedApp.logic.execute("add e/new@u.nus.edu");
        assertEquals(2, restartedApp.storage.readAddressBook().orElseThrow().getPersonList().size());
    }

    private MainApp startModel(Path file) {
        MainApp app = new MainApp();
        app.storage = new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json")));
        app.model = app.initModelManager(app.storage, new UserPrefs());
        app.logic = new LogicManager(app.model, app.storage);
        return app;
    }
}
