package seedu.address.testutil;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.scene.Scene;

/**
 * Shares one JavaFX toolkit across UI tests in a test worker.
 */
public final class JavaFxTestUtil {
    private static boolean isStarted;

    private JavaFxTestUtil() {}

    /**
     * Starts JavaFX once, skipping Linux UI tests when no display is available.
     */
    public static synchronized void initialize() throws InterruptedException {
        if (isStarted) {
            return;
        }
        boolean isLinux = System.getProperty("os.name").startsWith("Linux");
        assumeTrue(!isLinux || System.getenv("DISPLAY") != null, "JavaFX UI tests need a display");
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            started.countDown();
        });
        if (!started.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("JavaFX toolkit did not start");
        }
        isStarted = true;
    }

    /**
     * Runs an operation on the JavaFX thread and propagates failures to the test thread.
     */
    public static <T> T runAndWait(Callable<T> operation) throws Exception {
        FutureTask<T> task = new FutureTask<>(operation);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    /**
     * Runs an operation after the scene's next normal layout pulse, without forcing a layout.
     * Called from the test thread, not the JavaFX thread.
     *
     * @param scene The shown scene whose layout must complete.
     * @param operation The assertions or measurements to run after layout.
     * @return The operation's result.
     * @throws Exception If the operation fails or the pulse does not finish in time.
     */
    public static <T> T runAfterLayoutPulse(Scene scene, Callable<T> operation) throws Exception {
        FutureTask<T> task = new FutureTask<>(operation);
        runAndWait(() -> {
            scene.addPostLayoutPulseListener(new Runnable() {
                @Override
                public void run() {
                    scene.removePostLayoutPulseListener(this);
                    task.run();
                }
            });
            Platform.requestNextPulse();
            return null;
        });
        return task.get(10, TimeUnit.SECONDS);
    }
}
