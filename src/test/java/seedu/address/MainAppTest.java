package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.cert.Certificate;
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
    public void init_packagedJar_usesJarDirectoryForBothFiles() throws Exception {
        Path appFolder = Files.createDirectory(temporaryFolder.resolve("app folder"));
        Path jarFile = Files.createFile(appFolder.resolve("NUSocietyDesk.jar"));
        MainApp app = new MainApp() {
            @Override
            protected Path getApplicationLocation() {
                return jarFile;
            }
        };

        app.init();

        assertEquals(appFolder.resolve("preferences.json"), app.storage.getUserPrefsFilePath());
        assertEquals(appFolder.resolve(Path.of("data", "nusocietydesk.json")),
                app.storage.getAddressBookFilePath());
        assertTrue(Files.isRegularFile(appFolder.resolve("preferences.json")));
        app.stop();
    }

    @Test
    public void getApplicationLocation_availableAndMissingCodeSource() throws Exception {
        Path jarFile = Files.createFile(temporaryFolder.resolve("NUSocietyDesk.jar"));

        assertNotNull(new MainApp().getApplicationLocation());
        assertEquals(jarFile, MainApp.getApplicationLocation(
                new CodeSource(jarFile.toUri().toURL(), (Certificate[]) null)));
        assertNull(MainApp.getApplicationLocation(null));
        assertNull(MainApp.getApplicationLocation(new CodeSource(null, (Certificate[]) null)));
    }

    @Test
    public void resolveFilePath_packagedJar_usesJarDirectory() throws Exception {
        Path appFolder = Files.createDirectory(temporaryFolder.resolve("app folder"));
        Path jarFile = Files.createFile(appFolder.resolve("NUSocietyDesk.jar"));

        assertEquals(appFolder.resolve(Path.of("data", "nusocietydesk.json")),
                MainApp.resolveFilePath(jarFile, Path.of("data", "nusocietydesk.json")));
        assertEquals(appFolder.resolve("preferences.json"),
                MainApp.resolveFilePath(jarFile, Path.of("preferences.json")));
    }

    @Test
    public void resolveFilePath_developmentDirectory_usesWorkingDirectory() {
        assertEquals(Path.of("data", "nusocietydesk.json"),
                MainApp.resolveFilePath(temporaryFolder, Path.of("data", "nusocietydesk.json")));
        assertEquals(Path.of("preferences.json"),
                MainApp.resolveFilePath(temporaryFolder, Path.of("preferences.json")));
    }

    @Test
    public void resolveFilePath_missingCodeSource_usesWorkingDirectory() {
        assertEquals(Path.of("data", "nusocietydesk.json"),
                MainApp.resolveFilePath(null, Path.of("data", "nusocietydesk.json")));
        assertEquals(Path.of("preferences.json"),
                MainApp.resolveFilePath(null, Path.of("preferences.json")));
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
