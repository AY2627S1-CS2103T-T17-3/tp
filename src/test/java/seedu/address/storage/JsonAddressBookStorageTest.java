package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.HOON;
import static seedu.address.testutil.TypicalPersons.IDA;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.AddressBook;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.Person;
import seedu.address.model.person.Status;

public class JsonAddressBookStorageTest {
    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonAddressBookStorageTest");

    @TempDir
    public Path testFolder;

    @Test
    public void readAndSaveAddressBook_absentContactFields_preservesNullsAndLegacyDefaults() throws Exception {
        Path filePath = testFolder.resolve("IncompleteMembers.json");
        String originalJson = """
                {"persons":[
                  {"email":"Alex@U.NUS.EDU"},
                  {"name":null,"phone":null,"email":"jamie@u.nus.edu","address":null,
                   "status":"archived","tags":["Publicity"]},
                  {"name":"Not provided","phone":null,"email":"known@u.nus.edu","address":"NUS",
                   "status":null},
                  {"email":"current@u.nus.edu","status":"active"}
                ]}
                """;
        Files.writeString(filePath, originalJson);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(filePath);
        ReadOnlyAddressBook loaded = storage.readAddressBook().orElseThrow();
        assertEquals(originalJson, Files.readString(filePath));
        Person first = loaded.getPersonList().getFirst();
        assertTrue(first.getName().isEmpty());
        assertTrue(first.getPhone().isEmpty());
        assertTrue(first.getAddress().isEmpty());
        assertEquals("alex@u.nus.edu", first.getEmail().value);
        assertEquals(Status.ACTIVE, first.getStatus());
        assertEquals(Status.ARCHIVED, loaded.getPersonList().get(1).getStatus());
        assertEquals(Status.ACTIVE, loaded.getPersonList().get(2).getStatus());
        assertEquals(Status.ACTIVE, loaded.getPersonList().get(3).getStatus());

        storage.saveAddressBook(loaded);
        JsonNode savedPersons = JsonUtil.fromJsonString(Files.readString(filePath), JsonNode.class).get("persons");
        for (int i = 0; i < 2; i++) {
            for (String field : List.of("name", "phone", "address")) {
                assertTrue(savedPersons.get(i).has(field));
                assertTrue(savedPersons.get(i).get(field).isNull());
            }
        }
        assertEquals("publicity", savedPersons.get(1).get("tags").get(0).asText());
        assertEquals("Not provided", savedPersons.get(2).get("name").asText());
        assertEquals("active", savedPersons.get(0).get("status").asText());
        assertEquals("archived", savedPersons.get(1).get("status").asText());
        assertEquals("active", savedPersons.get(2).get("status").asText());
        assertEquals("active", savedPersons.get(3).get("status").asText());
        assertEquals(loaded, storage.readAddressBook().orElseThrow());
    }

    @Test
    public void readAddressBook_blankSuppliedFields_rejectsEntireFileWithoutRewriting() throws Exception {
        Path filePath = testFolder.resolve("BlankContact.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(filePath);
        for (String field : List.of("name", "phone", "address", "email")) {
            for (String value : List.of("", "   ")) {
                String emailField = field.equals("email") ? "" : "\"email\":\"other@u.nus.edu\",";
                String json = "{\"persons\":[{\"email\":\"valid@u.nus.edu\"},{" + emailField
                        + "\"" + field + "\":\"" + value + "\"}]}";
                Files.writeString(filePath, json);
                assertThrows(DataLoadingException.class, storage::readAddressBook);
                assertEquals(json, Files.readString(filePath));
            }
        }
    }

    @Test
    public void readAddressBook_incompleteDuplicateEmails_rejectsEntireFile() throws Exception {
        Path filePath = testFolder.resolve("DuplicateIncompleteMembers.json");
        String json = """
                {"persons":[{"email":"alex@u.nus.edu"},{"email":"ALEX@U.NUS.EDU"}]}
                """;
        Files.writeString(filePath, json);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(filePath);
        assertThrows(DataLoadingException.class, storage::readAddressBook);
        assertEquals(json, Files.readString(filePath));
    }

    @Test
    public void loadAddressBook_overlongEmails_recoversValidRecordsAndProtectsFile() throws Exception {
        Path filePath = testFolder.resolve("OverlongEmails.json");
        String overlongRecord = "{\"email\":\"" + "a.".repeat(2000) + "a@u.nus.edu\"}";
        for (boolean includeValidRecord : new boolean[] {true, false}) {
            String json = "{\"persons\":[" + overlongRecord
                    + (includeValidRecord ? ",{\"email\":\"valid@u.nus.edu\"}" : "") + "]}";
            Files.writeString(filePath, json);
            JsonAddressBookStorage storage = new JsonAddressBookStorage(filePath);

            AddressBookLoadResult result = storage.loadAddressBook();

            assertEquals(AddressBookLoadResult.Status.PARTIAL, result.getStatus());
            assertEquals(includeValidRecord ? 1 : 0, result.getAddressBook().getPersonList().size());
            if (includeValidRecord) {
                assertEquals("valid@u.nus.edu", result.getAddressBook().getPersonList().getFirst().getEmail().value);
            }
            assertEquals(List.of("Record 1: Email local-part must contain at most 64 characters; received 4001."),
                    result.getErrors());
            assertFalse(storage.isSaveAllowed());
            assertThrows(IOException.class, () -> storage.saveAddressBook(result.getAddressBook()));
            assertThrows(DataLoadingException.class, storage::readAddressBook);
            assertEquals(json, Files.readString(filePath));
        }
    }

    @Test
    public void readAndSaveAddressBook_repairedBoundaryEmail_restoresSavingOnRestart() throws Exception {
        Path filePath = testFolder.resolve("RepairedEmail.json");
        Files.writeString(filePath, "{\"persons\":[{\"email\":\"" + "a".repeat(65) + "@u.nus.edu\"}]}");
        JsonAddressBookStorage protectedStorage = new JsonAddressBookStorage(filePath);
        protectedStorage.loadAddressBook();
        String email = "a".repeat(64) + "@u.nus.edu";
        Files.writeString(filePath, "{\"persons\":[{\"email\":\"" + "A".repeat(64) + "@U.NUS.EDU\"}]}");
        JsonAddressBookStorage restartedStorage = new JsonAddressBookStorage(filePath);

        ReadOnlyAddressBook loaded = restartedStorage.readAddressBook().orElseThrow();
        assertEquals(email, loaded.getPersonList().getFirst().getEmail().value);
        assertFalse(protectedStorage.isSaveAllowed());
        assertTrue(restartedStorage.isSaveAllowed());
        restartedStorage.saveAddressBook(loaded);
        assertEquals(loaded, new JsonAddressBookStorage(filePath).readAddressBook().orElseThrow());
        JsonNode saved = JsonUtil.fromJsonString(Files.readString(filePath), JsonNode.class);
        assertEquals(email, saved.get("persons").get(0).get("email").asText());
    }

    @Test
    public void readAddressBook_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> readAddressBook(null));
    }

    private java.util.Optional<ReadOnlyAddressBook> readAddressBook(String filePath) throws Exception {
        return new JsonAddressBookStorage(Paths.get(filePath)).readAddressBook(addToTestDataPathIfNotNull(filePath));
    }

    private Path addToTestDataPathIfNotNull(String prefsFileInTestDataFolder) {
        return prefsFileInTestDataFolder != null
                ? TEST_DATA_FOLDER.resolve(prefsFileInTestDataFolder)
                : null;
    }

    @Test
    public void read_missingFile_emptyResult() throws Exception {
        assertFalse(readAddressBook("NonExistentFile.json").isPresent());
    }

    @Test
    public void read_notJsonFormat_exceptionThrown() {
        assertThrows(DataLoadingException.class, () -> readAddressBook("notJsonFormatAddressBook.json"));
    }

    @Test
    public void readAddressBook_invalidJsonStructure_throwsDataLoadingException() throws IOException {
        List<String> invalidJsonStructures = List.of("null", "{}", "{\"persons\":null}", "{\"persons\":{}}");
        Path filePath = testFolder.resolve("InvalidJsonStructure.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(filePath);

        for (String invalidJsonStructure : invalidJsonStructures) {
            Files.writeString(filePath, invalidJsonStructure);
            assertThrows(DataLoadingException.class, storage::readAddressBook);
        }
    }

    @Test
    public void readAddressBook_invalidPersonAddressBook_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readAddressBook("invalidPersonAddressBook.json"));
    }

    @Test
    public void readAddressBook_invalidAndValidPersonAddressBook_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readAddressBook("invalidAndValidPersonAddressBook.json"));
    }

    @Test
    public void readAndSaveAddressBook_caseVariantTags_normalizesAndDeduplicatesTags() throws Exception {
        Path filePath = testFolder.resolve("MixedCaseTags.json");
        Files.writeString(filePath, """
                {
                  "persons": [ {
                    "name": "Alice",
                    "phone": "91234567",
                    "email": "alice@u.nus.edu",
                    "address": "Kent Ridge",
                    "tags": [ "Publicity", "publicity", "PUBLICITY", "Exco" ]
                  } ]
                }
                """);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(filePath);
        ReadOnlyAddressBook loaded = storage.readAddressBook().get();

        assertEquals(Set.of("publicity", "exco"), loaded.getPersonList().get(0).getTags().stream()
                .map(tag -> tag.tagName).collect(Collectors.toSet()));
        assertEquals(2, loaded.getPersonList().get(0).getTags().size());

        storage.saveAddressBook(loaded);
        JsonNode savedTags = JsonUtil.fromJsonString(Files.readString(filePath), JsonNode.class)
                .get("persons").get(0).get("tags");
        Set<String> savedNames = new HashSet<>();
        savedTags.forEach(tag -> savedNames.add(tag.asText()));
        assertEquals(Set.of("publicity", "exco"), savedNames);
        assertEquals(2, savedTags.size());
        assertEquals(loaded, storage.readAddressBook().get());
    }

    @Test
    public void readAndSaveAddressBook_allInOrder_success() throws Exception {
        Path filePath = testFolder.resolve("TempAddressBook.json");
        AddressBook original = getTypicalAddressBook();
        JsonAddressBookStorage jsonAddressBookStorage = new JsonAddressBookStorage(filePath);

        // Save in new file and read back
        jsonAddressBookStorage.saveAddressBook(original, filePath);
        ReadOnlyAddressBook readBack = jsonAddressBookStorage.readAddressBook(filePath).get();
        assertEquals(original, new AddressBook(readBack));

        // Modify data, overwrite existing file, and read back
        original.addPerson(HOON);
        original.removePerson(ALICE);
        jsonAddressBookStorage.saveAddressBook(original, filePath);
        readBack = jsonAddressBookStorage.readAddressBook(filePath).get();
        assertEquals(original, new AddressBook(readBack));

        // Save and read without specifying file path
        original.addPerson(IDA);
        jsonAddressBookStorage.saveAddressBook(original); // file path not specified
        readBack = jsonAddressBookStorage.readAddressBook().get(); // file path not specified
        assertEquals(original, new AddressBook(readBack));

    }

    @Test
    public void saveAddressBook_nullAddressBook_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveAddressBook(null, "SomeFile.json"));
    }

    /**
     * Saves {@code addressBook} at the specified {@code filePath}.
     */
    private void saveAddressBook(ReadOnlyAddressBook addressBook, String filePath) {
        try {
            new JsonAddressBookStorage(Paths.get(filePath))
                    .saveAddressBook(addressBook, addToTestDataPathIfNotNull(filePath));
        } catch (IOException ioe) {
            throw new AssertionError("There should not be an error writing to the file.", ioe);
        }
    }

    @Test
    public void saveAddressBook_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveAddressBook(new AddressBook(), null));
    }
}
