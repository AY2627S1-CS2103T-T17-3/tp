package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AtomicFileWriterTest {
    @TempDir
    public Path temporaryFolder;

    @Test
    public void write_missingFile_createsParentsAndSavesUtf8() throws Exception {
        Path destination = temporaryFolder.resolve("new/data/members.json");

        new AtomicFileWriter().write(destination, "Member: Renée 陈");

        assertEquals("Member: Renée 陈", Files.readString(destination));
        assertOnlyDestinationRemains(destination);
    }

    @Test
    public void write_existingFile_replacesCompleteContents() throws Exception {
        Path destination = Files.writeString(temporaryFolder.resolve("members.json"), "original data");

        new AtomicFileWriter().write(destination, "replacement");

        assertEquals("replacement", Files.readString(destination));
        assertOnlyDestinationRemains(destination);
    }

    @Test
    public void write_partialTemporaryWrite_preservesOriginalAndRemovesTemporaryFile() throws Exception {
        Path destination = Files.writeString(temporaryFolder.resolve("members.json"), "original data");
        IOException failure = new IOException("Simulated full disk");
        AtomicFileWriter writer = new AtomicFileWriter() {
            @Override
            void writeTemporaryFile(Path temporaryFile, String content) throws IOException {
                Files.writeString(temporaryFile, "partial");
                throw failure;
            }
        };

        assertSame(failure, assertThrows(IOException.class, () -> writer.write(destination, "replacement")));

        assertEquals("original data", Files.readString(destination));
        assertOnlyDestinationRemains(destination);
    }

    @Test
    public void write_replacementFailure_preservesOriginalAndRemovesTemporaryFile() throws Exception {
        assertReplacementFailure(new IOException("Simulated replacement failure"));
    }

    @Test
    public void write_atomicMoveUnsupported_failsWithoutUnsafeFallback() throws Exception {
        assertReplacementFailure(new AtomicMoveNotSupportedException("temporary", "destination", "unsupported"));
    }

    @Test
    public void write_failedFirstSave_leavesDestinationAbsent() throws Exception {
        Path destination = temporaryFolder.resolve("members.json");
        AtomicFileWriter writer = new AtomicFileWriter() {
            @Override
            void replaceFile(Path temporaryFile, Path target) throws IOException {
                throw new IOException("Simulated replacement failure");
            }
        };

        assertThrows(IOException.class, () -> writer.write(destination, "replacement"));

        assertFalse(Files.exists(destination));
        try (var files = Files.list(temporaryFolder)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    public void write_parentIsFile_failsWithoutChangingParent() throws Exception {
        Path parent = Files.writeString(temporaryFolder.resolve("parent"), "original parent");

        assertThrows(IOException.class, () -> new AtomicFileWriter().write(parent.resolve("members.json"), "data"));

        assertEquals("original parent", Files.readString(parent));
    }

    @Test
    public void write_directoryDestination_preservesDirectoryAndContents() throws Exception {
        Path destination = Files.createDirectory(temporaryFolder.resolve("members.json"));
        Path child = Files.writeString(destination.resolve("keep.txt"), "original child");

        assertThrows(IOException.class, () -> new AtomicFileWriter().write(destination, "replacement"));

        assertTrue(Files.isDirectory(destination));
        assertEquals("original child", Files.readString(child));
        assertOnlyDestinationRemains(destination);
    }

    @Test
    public void write_symbolicLink_updatesTargetAndPreservesLink() throws Exception {
        Path destination = Files.writeString(temporaryFolder.resolve("members.json"), "original data");
        Path link = temporaryFolder.resolve("linked.json");
        try {
            Files.createSymbolicLink(link, destination);
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            assumeTrue(false, "Creating symbolic links is unavailable: " + e.getClass().getSimpleName());
        }

        new AtomicFileWriter().write(link, "replacement");

        assertTrue(Files.isSymbolicLink(link));
        assertEquals("replacement", Files.readString(destination));
        assertEquals("replacement", Files.readString(link));
    }

    private void assertReplacementFailure(IOException failure) throws Exception {
        Path destination = Files.writeString(temporaryFolder.resolve("members.json"), "original data");
        AtomicFileWriter writer = new AtomicFileWriter() {
            @Override
            void replaceFile(Path temporaryFile, Path target) throws IOException {
                assertEquals("replacement", Files.readString(temporaryFile));
                assertEquals("original data", Files.readString(target));
                throw failure;
            }
        };

        assertSame(failure, assertThrows(IOException.class, () -> writer.write(destination, "replacement")));

        assertEquals("original data", Files.readString(destination));
        assertOnlyDestinationRemains(destination);
    }

    private void assertOnlyDestinationRemains(Path destination) throws IOException {
        try (var files = Files.list(destination.getParent())) {
            assertEquals(List.of(destination), files.toList());
        }
    }
}
