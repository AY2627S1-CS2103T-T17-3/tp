package seedu.address;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.layout.Region;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.util.StringUtil;

/**
 * The main entry point to the application.
 *
 * This is a workaround for the following error when MainApp is made the
 * entry point of the application:
 *
 *     Error: JavaFX runtime components are missing, and are required to run this application
 *
 * The reason is that MainApp extends Application. In that case, the
 * LauncherHelper will check for the javafx.graphics module to be present
 * as a named module. We don't use JavaFX via the module system so it can't
 * find the javafx.graphics module, and so the launch is aborted.
 *
 * By having a separate main class (Main) that doesn't extend Application
 * to be the entry point of the application, we avoid this issue.
 */
public class Main {
    private static final String MESSAGE_ALREADY_RUNNING = "This member directory is already open in another "
            + "instance of NUSocietyDesk. Close that instance before trying again.";
    private static Logger logger = LogsCenter.getLogger(Main.class);

    public static void main(String[] args) {

        logger.warning("The warnings about a 'restricted method in java.lang.System' "
            + "and 'enabling native access' appearing below (if any) can be ignored.");
        try {
            Path applicationLocation = MainApp.getApplicationLocation(Main.class.getProtectionDomain().getCodeSource());
            Path lockFile = MainApp.resolveFilePath(applicationLocation, Path.of(".nusocietydesk.lock"));
            if (!launchWithInstanceLock(lockFile, () -> Application.launch(MainApp.class, args))) {
                showStartupError(MESSAGE_ALREADY_RUNNING);
            }
        } catch (IOException | URISyntaxException e) {
            logger.severe("Could not lock the application folder: " + StringUtil.getDetails(e));
            showStartupError("NUSocietyDesk could not lock its application folder. "
                    + "Check that the folder is writable and supports file locking, then try again.");
        }
    }

    /**
     * Holds the folder's lock throughout launch and shutdown, releasing it even if startup fails.
     * The separate lock file is kept on disk because member-data saves replace the JSON file.
     *
     * @return Whether the launch ran; false if another process holds the lock.
     * @throws IOException If the lock file cannot be opened, locked, or closed.
     */
    static boolean launchWithInstanceLock(Path lockFile, Runnable launch) throws IOException {
        try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock lock = channel.tryLock()) {
            if (lock == null) {
                return false;
            }
            launch.run();
            return true;
        }
    }

    private static void showStartupError(String message) {
        Platform.startup(() -> Platform.runLater(() -> {
            Alert alert = createStartupError(message);
            alert.setOnHidden(event -> Platform.exit());
            alert.show();
        }));
    }

    /** Creates a startup error dialog before the main window has opened. */
    static Alert createStartupError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("NUSocietyDesk");
        alert.setHeaderText("Cannot open NUSocietyDesk");
        alert.setGraphic(null);
        alert.setContentText(message);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setResizable(true);
        return alert;
    }
}
