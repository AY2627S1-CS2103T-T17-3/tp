package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.stage.Window;
import seedu.address.testutil.JavaFxTestUtil;

/** Checks the launch guard using real operating-system locks across JVMs. */
public class MainTest {
    @TempDir
    public Path temporaryFolder;

    @Test
    public void main_directoryAlreadyOpen_rendersDialogInFreshJvmAndExitsWhenDismissed() throws Exception {
        Process owner = startLockProcess(temporaryFolder.resolve(".nusocietydesk.lock"));
        try {
            assertEquals("LOCKED", readStatus(owner));
            assertStartupError("already open in another instance");
        } finally {
            stopProcess(owner);
        }
    }

    @Test
    public void main_lockFileCannotBeOpened_rendersErrorWithoutLoadingData() throws Exception {
        Files.createDirectory(temporaryFolder.resolve(".nusocietydesk.lock"));

        assertStartupError("could not lock its application folder");
    }

    @Test
    public void createStartupError_longMessages_displaysFullText() throws Exception {
        JavaFxTestUtil.initialize();
        List<String> messages = List.of(
                "This member directory is already open in another instance of NUSocietyDesk. "
                        + "Close that instance before trying again.",
                "NUSocietyDesk could not lock its application folder. "
                        + "Check that the folder is writable and supports file locking, then try again.");
        for (String message : messages) {
            Alert alert = JavaFxTestUtil.runAndWait(() -> {
                Alert dialog = Main.createStartupError(message);
                dialog.show();
                return dialog;
            });
            try {
                JavaFxTestUtil.runAfterLayoutPulse(alert.getDialogPane().getScene(), () -> {
                    assertNull(alert.getGraphic());
                    Label content = (Label) alert.getDialogPane().lookup(".content");
                    Text renderedText = (Text) content.lookup(".text");
                    assertEquals(message, renderedText.getText());
                    assertTrue(content.getHeight() >= content.prefHeight(content.getWidth()) - 1);
                    return null;
                });
            } finally {
                JavaFxTestUtil.runAndWait(() -> {
                    alert.close();
                    return null;
                });
            }
        }
    }

    @Test
    public void launchWithInstanceLock_existingUnlockedFile_launchesAndReleasesLock() throws Exception {
        Path lockFile = temporaryFolder.resolve(".nusocietydesk.lock");
        Files.writeString(lockFile, "left over from an earlier run");
        AtomicBoolean hasLaunched = new AtomicBoolean();

        assertTrue(Main.launchWithInstanceLock(lockFile, () -> hasLaunched.set(true)));
        assertTrue(hasLaunched.get());
        assertTrue(Files.exists(lockFile));
        assertTrue(Main.launchWithInstanceLock(lockFile, () -> {}));
    }

    @Test
    public void launchWithInstanceLock_launchFails_releasesLock() throws Exception {
        Path lockFile = temporaryFolder.resolve(".nusocietydesk.lock");
        IllegalStateException failure = new IllegalStateException("Startup failed");

        IllegalStateException actualFailure = assertThrows(IllegalStateException.class, () -> {
            Main.launchWithInstanceLock(lockFile, () -> {
                throw failure;
            });
        });
        assertSame(failure, actualFailure);
        assertTrue(Main.launchWithInstanceLock(lockFile, () -> {}));
    }

    @Test
    public void launchWithInstanceLock_lockFileCannotBeOpened_doesNotLaunch() throws Exception {
        Path lockFile = Files.createDirectory(temporaryFolder.resolve(".nusocietydesk.lock"));
        AtomicBoolean hasLaunched = new AtomicBoolean();

        assertThrows(IOException.class, () -> Main.launchWithInstanceLock(lockFile, () -> hasLaunched.set(true)));
        assertFalse(hasLaunched.get());
    }

    @Test
    public void launchWithInstanceLock_otherProcessOwnsLock_blocksUntilOwnerExits() throws Exception {
        Path lockFile = temporaryFolder.resolve(".nusocietydesk.lock");
        Path dataFile = temporaryFolder.resolve("members.json");
        Files.writeString(dataFile, "{\"persons\":[{\"email\":\"first@u.nus.edu\"}]}");
        byte[] savedData = Files.readAllBytes(dataFile);
        AtomicBoolean hasLaunched = new AtomicBoolean();
        Process owner = startLockProcess(lockFile);
        try {
            assertEquals("LOCKED", readStatus(owner));
            assertFalse(Main.launchWithInstanceLock(lockFile, () -> hasLaunched.set(true)));
            assertFalse(hasLaunched.get());
            assertArrayEquals(savedData, Files.readAllBytes(dataFile));

            owner.getOutputStream().write(1);
            owner.getOutputStream().flush();
            assertTrue(owner.waitFor(10, TimeUnit.SECONDS));
            assertEquals(0, owner.exitValue());
            assertTrue(Main.launchWithInstanceLock(lockFile, () -> hasLaunched.set(true)));
            assertTrue(hasLaunched.get());
        } finally {
            stopProcess(owner);
        }
    }

    @Test
    public void launchWithInstanceLock_ownerProcessIsTerminated_allowsRestart() throws Exception {
        Path lockFile = temporaryFolder.resolve(".nusocietydesk.lock");
        Process owner = startLockProcess(lockFile);
        try {
            assertEquals("LOCKED", readStatus(owner));
            assertFalse(Main.launchWithInstanceLock(lockFile, () -> {}));

            stopProcess(owner);

            assertTrue(Files.exists(lockFile));
            assertTrue(Main.launchWithInstanceLock(lockFile, () -> {}));
        } finally {
            stopProcess(owner);
        }
    }

    @Test
    public void launchWithInstanceLock_duringLaunch_blocksOtherProcessButAllowsSeparateFolder() throws Exception {
        Path lockFile = temporaryFolder.resolve(".nusocietydesk.lock");
        Path otherLockFile = Files.createDirectory(temporaryFolder.resolve("other app"))
                .resolve(".nusocietydesk.lock");

        assertTrue(Main.launchWithInstanceLock(lockFile, () -> {
            try {
                Process contender = startLockProcess(lockFile);
                try {
                    assertEquals("BLOCKED", readStatus(contender));
                    assertTrue(contender.waitFor(10, TimeUnit.SECONDS));
                    assertEquals(0, contender.exitValue());
                    assertTrue(Main.launchWithInstanceLock(otherLockFile, () -> {}));
                } finally {
                    stopProcess(contender);
                }
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }));
    }

    private void assertStartupError(String expectedMessage) throws Exception {
        boolean isLinux = System.getProperty("os.name").startsWith("Linux");
        assumeTrue(!isLinux || System.getenv("DISPLAY") != null, "JavaFX UI tests need a display");
        StringJoiner classPath = new StringJoiner(File.pathSeparator);
        classPath.add(System.getProperty("java.class.path"));
        for (ClassLoader loader = MainTest.class.getClassLoader(); loader != null; loader = loader.getParent()) {
            if (loader instanceof URLClassLoader urlLoader) {
                for (URL url : urlLoader.getURLs()) {
                    classPath.add(Path.of(url.toURI()).toString());
                }
            }
        }
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        // Record child-JVM coverage in the same execution file that Gradle reports on.
        ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(argument -> argument.startsWith("-javaagent:") && argument.contains("jacoco"))
                .map(this::resolveCoverageDestination)
                .forEach(command::add);
        command.addAll(List.of("-cp", classPath.toString(), StartupErrorProcess.class.getName(), expectedMessage));
        Path errorLog = temporaryFolder.resolve("startup.err");
        Process contender = new ProcessBuilder(command).directory(temporaryFolder.toFile())
                .redirectError(errorLog.toFile()).start();
        try {
            assertTrue(contender.waitFor(15, TimeUnit.SECONDS), "The dialog did not close on its own");
            assertEquals(0, contender.exitValue(), () -> {
                try {
                    return Files.readString(errorLog);
                } catch (IOException e) {
                    return e.toString();
                }
            });
            assertEquals("RENDERED", readStatus(contender));
            assertFalse(Files.exists(temporaryFolder.resolve("preferences.json")));
            assertFalse(Files.exists(temporaryFolder.resolve("data")));
        } finally {
            stopProcess(contender);
        }
    }

    private String resolveCoverageDestination(String agentArgument) {
        Matcher destination = Pattern.compile("destfile=([^,]+)").matcher(agentArgument);
        if (destination.find()) {
            // The child runs in a temporary folder, so resolve Gradle's output path in the parent JVM.
            String absoluteDestination = "destfile=" + Path.of(destination.group(1)).toAbsolutePath();
            return destination.replaceFirst(Matcher.quoteReplacement(absoluteDestination));
        }
        return agentArgument;
    }

    private Process startLockProcess(Path lockFile) throws Exception {
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        Path testClasses = Path.of(MainTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        return new ProcessBuilder(java.toString(), "-cp", testClasses.toString(),
                LockProcess.class.getName(), lockFile.toString()).start();
    }

    private String readStatus(Process process) throws Exception {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return process.inputReader().readLine();
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }).get(10, TimeUnit.SECONDS);
    }

    private void stopProcess(Process process) throws InterruptedException {
        process.destroyForcibly();
        assertTrue(process.waitFor(10, TimeUnit.SECONDS));
    }

    /** Exercises the actual startup dialog in a fresh JavaFX runtime without forcing its layout. */
    public static class StartupErrorProcess {
        public static void main(String[] args) {
            Main.main(new String[0]);
            Platform.runLater(() -> new AnimationTimer() {
                @Override
                public void handle(long now) {
                    for (Window window : List.copyOf(Window.getWindows())) {
                        Scene scene = window.getScene();
                        if (window.isShowing() && scene != null && scene.getRoot() instanceof DialogPane dialog) {
                            stop();
                            scene.addPostLayoutPulseListener(new Runnable() {
                                @Override
                                public void run() {
                                    scene.removePostLayoutPulseListener(this);
                                    try {
                                        assertFalse(Platform.isNestedLoopRunning(),
                                                "Startup is blocked inside a nested dialog event loop");
                                        Label content = (Label) dialog.lookup(".content");
                                        Text renderedText = (Text) content.lookup(".text");
                                        String message = dialog.getContentText();
                                        assertEquals(message, renderedText.getText());
                                        assertTrue(content.getHeight() >= content.prefHeight(content.getWidth()) - 1);
                                        assertTrue(message.contains(args[0]));
                                        System.out.println("RENDERED");
                                        Button okButton = (Button) dialog.lookupButton(ButtonType.OK);
                                        okButton.fire();
                                    } catch (Throwable failure) {
                                        failure.printStackTrace();
                                        System.exit(1);
                                    }
                                }
                            });
                            Platform.requestNextPulse();
                            return;
                        }
                    }
                }
            }.start());
        }
    }

    /** Holds a native lock in another JVM until signaled through standard input. */
    public static class LockProcess {
        public static void main(String[] args) throws IOException {
            try (FileChannel channel = FileChannel.open(Path.of(args[0]),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                    FileLock lock = channel.tryLock()) {
                System.out.println(lock == null ? "BLOCKED" : "LOCKED");
                if (lock != null) {
                    System.in.read();
                }
            }
        }
    }
}
