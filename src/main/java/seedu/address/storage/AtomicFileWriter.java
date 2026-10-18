package seedu.address.storage;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;

/**
 * Writes a complete temporary file before atomically replacing the destination.
 * An unsupported atomic move is a save failure; there is no unsafe replacement fallback.
 */
class AtomicFileWriter {
    private static final Logger logger = LogsCenter.getLogger(AtomicFileWriter.class);

    /**
     * Saves UTF-8 content without opening the destination for writing.
     * Existing symbolic links are followed, preserving the configured link itself.
     *
     * @throws IOException If writing or atomic replacement fails.
     */
    void write(Path file, String content) throws IOException {
        Path destination = file.toAbsolutePath();
        if (Files.isSymbolicLink(destination)) {
            destination = destination.toRealPath();
        }
        if (Files.exists(destination) && !Files.isWritable(destination)) {
            throw new AccessDeniedException(destination.toString());
        }
        Files.createDirectories(destination.getParent());
        Path temporaryFile = Files.createTempFile(destination.getParent(), ".nusocietydesk-", ".tmp");
        try {
            writeTemporaryFile(temporaryFile, content);
            replaceFile(temporaryFile, destination);
        } catch (IOException | RuntimeException failure) {
            try {
                Files.deleteIfExists(temporaryFile);
            } catch (IOException | SecurityException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
                logger.warning("Could not remove a temporary member-data file after a failed save.");
            }
            throw failure;
        }
    }

    void writeTemporaryFile(Path temporaryFile, String content) throws IOException {
        Files.writeString(temporaryFile, content);
    }

    void replaceFile(Path temporaryFile, Path destination) throws IOException {
        Files.move(temporaryFile, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
