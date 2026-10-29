package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.AddressBook;
import seedu.address.model.person.Address;
import seedu.address.model.person.Name;
import seedu.address.model.person.Phone;
import seedu.address.model.tag.Tag;

public class AddressBookRecoveryTest {
    @TempDir
    public Path temporaryFolder;

    @Test
    public void loadAddressBook_unknownProperties_reportsAndProtectsRecoveredData() throws Exception {
        Path file = temporaryFolder.resolve("members.json");
        Files.writeString(file, """
                {"version":2,"persons":[
                  {"email":"alex@u.nus.edu","phoneNumber":"91234567","extra":true},
                  {"email":"bob@u.nus.edu","phone":"87654321"}
                ]}
                """);
        byte[] original = Files.readAllBytes(file);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);

        AddressBookLoadResult result = storage.loadAddressBook();

        assertEquals(AddressBookLoadResult.Status.PARTIAL, result.getStatus());
        assertEquals(2, result.getAddressBook().getPersonList().size());
        assertEquals(List.of("Data file: unrecognized property \"version\".",
                "Record 1: unrecognized property \"phoneNumber\".",
                "Record 1: unrecognized property \"extra\"."), result.getErrors());
        assertFalse(storage.isSaveAllowed());
        assertThrows(IOException.class, () -> storage.saveAddressBook(result.getAddressBook()));
        assertArrayEquals(original, Files.readAllBytes(file));
    }

    @Test
    public void loadAddressBook_mixedRecords_recoversValidRecordsAndReportsOriginalPositions() throws Exception {
        Path file = temporaryFolder.resolve("members.json");
        Files.writeString(file, """
                {"persons":[
                  {"email":"invalid-email"},
                  {"email":"Alice@U.NUS.EDU"},
                  {"email":"alice@u.nus.edu"},
                  null,
                  {"email":"badtag@u.nus.edu","tags":[null]},
                  {"email":["wrong-type@u.nus.edu"]},
                  {"email":"bob@u.nus.edu","status":"archived"}
                ]}
                """);
        byte[] original = Files.readAllBytes(file);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);

        AddressBookLoadResult result = storage.loadAddressBook();

        assertEquals(AddressBookLoadResult.Status.PARTIAL, result.getStatus());
        assertEquals(List.of("alice@u.nus.edu", "bob@u.nus.edu"), result.getAddressBook().getPersonList()
                .stream().map(person -> person.getEmail().value).toList());
        assertEquals(5, result.getErrors().size());
        assertTrue(result.getErrors().get(0).startsWith("Record 1: Emails must"));
        assertEquals("Record 3: email \"alice@u.nus.edu\" duplicates record 2.", result.getErrors().get(1));
        assertTrue(result.getErrors().get(2).startsWith("Record 4:"));
        assertTrue(result.getErrors().get(3).startsWith("Record 5: Tags must not contain null"));
        assertTrue(result.getErrors().get(4).startsWith("Record 6:"));
        assertFalse(storage.isSaveAllowed());
        assertThrows(IOException.class, () -> storage.saveAddressBook(result.getAddressBook()));
        assertThrows(IOException.class, () -> storage.saveAddressBook(result.getAddressBook(), file));
        assertArrayEquals(original, Files.readAllBytes(file));
    }

    @Test
    public void loadAddressBook_unicodeBlankAddresses_recoversValidMembersAndProtectsFile() throws Exception {
        Path file = temporaryFolder.resolve("members.json");
        Files.writeString(file, """
                {"persons":[
                  {"email":"valid@u.nus.edu","address":"Kent Ridge"},
                  {"email":"ideographic@u.nus.edu","address":"\u3000"},
                  {"email":"emspace@u.nus.edu","address":"\\u2003"},
                  {"email":"mixed@u.nus.edu","address":"\u00a0\u3000\u2003\u202f"}
                ]}
                """);
        byte[] original = Files.readAllBytes(file);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);

        AddressBookLoadResult result = storage.loadAddressBook();

        assertEquals(AddressBookLoadResult.Status.PARTIAL, result.getStatus());
        assertEquals(List.of("valid@u.nus.edu"), result.getAddressBook().getPersonList().stream()
                .map(person -> person.getEmail().value).toList());
        assertEquals(List.of("Record 2: " + Address.MESSAGE_CONSTRAINTS,
                "Record 3: " + Address.MESSAGE_CONSTRAINTS,
                "Record 4: " + Address.MESSAGE_CONSTRAINTS), result.getErrors());
        assertFalse(storage.isSaveAllowed());
        assertThrows(IOException.class, () -> storage.saveAddressBook(result.getAddressBook()));
        assertThrows(DataLoadingException.class, storage::readAddressBook);
        assertArrayEquals(original, Files.readAllBytes(file));
    }

    @Test
    public void loadAddressBook_overlongFields_recoversBoundaryMemberAndProtectsFile() throws Exception {
        Path file = temporaryFolder.resolve("overlong.json");
        Files.writeString(file, """
                {"persons":[
                  {"email":"valid@u.nus.edu","name":"%s","phone":"%s","address":"%s","tags":["%s"]},
                  {"email":"name@u.nus.edu","name":"%s"},
                  {"email":"phone@u.nus.edu","phone":"%s"},
                  {"email":"address@u.nus.edu","address":"%s"},
                  {"email":"tag@u.nus.edu","tags":["%s"]}
                ]}
                """.formatted("A".repeat(127), "9".repeat(15), "A".repeat(200), "t".repeat(30),
                        "A".repeat(128), "9".repeat(16), "A".repeat(201), "t".repeat(31)));
        byte[] original = Files.readAllBytes(file);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);

        AddressBookLoadResult result = storage.loadAddressBook();

        assertEquals(AddressBookLoadResult.Status.PARTIAL, result.getStatus());
        assertEquals(List.of("valid@u.nus.edu"), result.getAddressBook().getPersonList().stream()
                .map(person -> person.getEmail().value).toList());
        assertEquals(List.of("Record 2: " + Name.MESSAGE_CONSTRAINTS,
                "Record 3: " + Phone.MESSAGE_CONSTRAINTS, "Record 4: " + Address.MESSAGE_CONSTRAINTS,
                "Record 5: " + Tag.MESSAGE_CONSTRAINTS), result.getErrors());
        assertFalse(storage.isSaveAllowed());
        assertThrows(IOException.class, () -> storage.saveAddressBook(result.getAddressBook()));
        assertThrows(DataLoadingException.class, storage::readAddressBook);
        assertArrayEquals(original, Files.readAllBytes(file));
    }

    @Test
    public void loadAddressBook_invalidEarlierOccurrence_keepsFirstValidOccurrence() throws Exception {
        Path file = temporaryFolder.resolve("members.json");
        Files.writeString(file, """
                {"persons":[
                  {"email":"alice@u.nus.edu","phone":"invalid"},
                  {"email":"alice@u.nus.edu"},
                  {"email":"ALICE@U.NUS.EDU"}
                ]}
                """);

        AddressBookLoadResult result = new JsonAddressBookStorage(file).loadAddressBook();

        assertEquals(1, result.getAddressBook().getPersonList().size());
        assertEquals(2, result.getErrors().size());
        assertEquals("Record 3: email \"alice@u.nus.edu\" duplicates record 2.", result.getErrors().get(1));
    }

    @Test
    public void loadAddressBook_invalidFields_reportsEachRejectedRecord() throws Exception {
        List<String> invalidRecords = List.of("{}", "null", "[]", "42",
                "{\"email\":\"invalid-email\"}", "{\"email\":null}",
                "{\"email\":\"a@u.nus.edu\",\"name\":\"\"}",
                "{\"email\":\"a@u.nus.edu\",\"phone\":\"abc\"}",
                "{\"email\":\"a@u.nus.edu\",\"address\":\"\"}",
                "{\"email\":\"a@u.nus.edu\",\"status\":\"unknown\"}",
                "{\"email\":\"a@u.nus.edu\",\"tags\":[\"!\"]}");
        Path file = temporaryFolder.resolve("members.json");
        Files.writeString(file, "{\"persons\":[" + String.join(",", invalidRecords) + "]}");
        byte[] original = Files.readAllBytes(file);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);

        AddressBookLoadResult result = storage.loadAddressBook();

        assertEquals(AddressBookLoadResult.Status.PARTIAL, result.getStatus());
        assertTrue(result.getAddressBook().getPersonList().isEmpty());
        assertEquals(invalidRecords.size(), result.getErrors().size());
        for (int i = 0; i < invalidRecords.size(); i++) {
            assertTrue(result.getErrors().get(i).startsWith("Record " + (i + 1) + ":"));
        }
        assertThrows(IOException.class, () -> storage.saveAddressBook(new AddressBook()));
        assertArrayEquals(original, Files.readAllBytes(file));
    }

    @Test
    public void loadAddressBook_malformedOrWrongStructure_protectsEntireFile() throws Exception {
        List<String> invalidFiles = List.of("", "{", "null", "[]", "{}", "{\"persons\":null}",
                "{\"persons\":{}}", "{\"persons\":[]} {}", "{\"persons\":[]} garbage",
                "{\"persons\":[{\"email\":\"a@u.nus.edu\"},");
        Path file = temporaryFolder.resolve("members.json");
        for (String json : invalidFiles) {
            Files.writeString(file, json);
            byte[] original = Files.readAllBytes(file);
            JsonAddressBookStorage storage = new JsonAddressBookStorage(file);

            AddressBookLoadResult result = storage.loadAddressBook();

            assertEquals(AddressBookLoadResult.Status.FAILURE, result.getStatus(), json);
            assertTrue(result.getAddressBook().getPersonList().isEmpty());
            assertFalse(result.getErrors().isEmpty());
            assertFalse(storage.isSaveAllowed());
            assertThrows(IOException.class, () -> storage.saveAddressBook(new AddressBook()));
            assertArrayEquals(original, Files.readAllBytes(file));
        }
    }

    @Test
    public void loadAddressBook_readFailure_protectsPath() throws Exception {
        Path file = Files.createDirectory(temporaryFolder.resolve("members.json"));
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);

        AddressBookLoadResult result = storage.loadAddressBook();

        assertEquals(AddressBookLoadResult.Status.FAILURE, result.getStatus());
        assertFalse(result.getErrors().isEmpty());
        assertFalse(storage.isSaveAllowed());
        assertThrows(IOException.class, () -> storage.saveAddressBook(new AddressBook()));
        assertTrue(Files.isDirectory(file));
    }

    @Test
    public void loadAddressBook_missingAndValidEmptyFiles_allowSaving() throws Exception {
        Path file = temporaryFolder.resolve("members.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);

        assertEquals(AddressBookLoadResult.Status.MISSING, storage.loadAddressBook().getStatus());
        assertFalse(Files.exists(file));
        assertTrue(storage.isSaveAllowed());
        storage.saveAddressBook(new AddressBook());
        AddressBookLoadResult result = storage.loadAddressBook();
        assertEquals(AddressBookLoadResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getAddressBook().getPersonList().isEmpty());
        assertTrue(result.getErrors().isEmpty());
        assertTrue(storage.isSaveAllowed());
    }

    @Test
    public void readAddressBook_failedStrictRead_alsoBlocksSaving() throws Exception {
        Path file = temporaryFolder.resolve("members.json");
        Files.writeString(file, "{\"persons\":[{\"email\":\"invalid\"}]}");
        byte[] original = Files.readAllBytes(file);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);

        assertThrows(DataLoadingException.class, storage::readAddressBook);
        assertThrows(IOException.class, () -> storage.saveAddressBook(new AddressBook()));
        assertArrayEquals(original, Files.readAllBytes(file));
    }

    @Test
    public void loadAddressBook_repairedFile_requiresNewStorageSessionToSave() throws Exception {
        Path file = temporaryFolder.resolve("members.json");
        Files.writeString(file, "{");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        storage.loadAddressBook();
        Files.writeString(file, "{\"persons\":[{\"email\":\"alice@u.nus.edu\"}]}");

        assertEquals(AddressBookLoadResult.Status.SUCCESS, storage.loadAddressBook().getStatus());
        assertFalse(storage.isSaveAllowed());
        assertThrows(IOException.class, () -> storage.saveAddressBook(new AddressBook()));

        JsonAddressBookStorage restartedStorage = new JsonAddressBookStorage(file);
        AddressBookLoadResult result = restartedStorage.loadAddressBook();
        assertTrue(restartedStorage.isSaveAllowed());
        restartedStorage.saveAddressBook(result.getAddressBook());
        assertEquals(result.getAddressBook(), restartedStorage.readAddressBook().orElseThrow());
    }
}
