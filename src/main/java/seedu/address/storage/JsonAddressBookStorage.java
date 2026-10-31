package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.AddressBook;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.Person;

/**
 * A class to access AddressBook data stored as a JSON file on the hard disk.
 */
public class JsonAddressBookStorage {

    public static final String MESSAGE_SAVE_DISABLED = "The data file could not be fully loaded. "
            + "Changes and saving are disabled to protect the original file. "
            + "Repair the file and restart the application.";

    private static final Logger logger = LogsCenter.getLogger(JsonAddressBookStorage.class);
    private static final ObjectMapper treeMapper = new ObjectMapper();
    private static final Set<String> ROOT_PROPERTIES = Set.of("persons");
    private static final Set<String> PERSON_PROPERTIES = Set.of("name", "phone", "email", "address", "status", "tags");

    private Path filePath;
    private final AtomicFileWriter fileWriter;
    private boolean saveAllowed = true;

    public JsonAddressBookStorage(Path filePath) {
        this(filePath, new AtomicFileWriter());
    }

    JsonAddressBookStorage(Path filePath, AtomicFileWriter fileWriter) {
        this.filePath = requireNonNull(filePath);
        this.fileWriter = requireNonNull(fileWriter);
    }

    public Path getAddressBookFilePath() {
        return filePath;
    }

    /**
     * Returns AddressBook data as a {@link ReadOnlyAddressBook}.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if loading the data from storage failed.
     */
    public Optional<ReadOnlyAddressBook> readAddressBook() throws DataLoadingException {
        return readAddressBook(filePath);
    }

    /**
     * Similar to {@link #readAddressBook()}.
     *
     * @param filePath location of the data. Cannot be null.
     * @throws DataLoadingException if loading the data from storage failed.
     */
    public Optional<ReadOnlyAddressBook> readAddressBook(Path filePath) throws DataLoadingException {
        requireNonNull(filePath);
        AddressBookLoadResult result = loadAddressBook(filePath);
        if (result.hasErrors()) {
            throw new DataLoadingException(new IllegalValueException(String.join("\n", result.getErrors())));
        }
        if (result.getStatus() == AddressBookLoadResult.Status.MISSING) {
            return Optional.empty();
        }
        return Optional.of(result.getAddressBook());
    }

    /**
     * Recovers valid records and reports rejected records without writing to the data file.
     * Any loading error disables all saves through this storage instance until the application restarts.
     */
    public AddressBookLoadResult loadAddressBook() {
        return loadAddressBook(filePath);
    }

    private AddressBookLoadResult loadAddressBook(Path path) {
        AddressBookLoadResult result;
        try {
            // Only a confirmed absence is treated as a new file; inaccessible files must be protected.
            if (Files.notExists(path)) {
                return new AddressBookLoadResult(AddressBookLoadResult.Status.MISSING,
                        new AddressBook(), List.of());
            }
            JsonNode root;
            try (Reader reader = Files.newBufferedReader(path);
                    JsonParser parser = treeMapper.getFactory().createParser(reader)) {
                parser.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
                root = treeMapper.readTree(parser);
                if (parser.nextToken() != null) {
                    throw new IOException("Unexpected content after the address-book JSON object.");
                }
            }
            if (root == null || !root.isObject() || !root.path("persons").isArray()) {
                throw new IOException("The data file does not match the expected address-book JSON format.");
            }
            result = recoverRecords(root);
        } catch (IOException | SecurityException e) {
            result = new AddressBookLoadResult(AddressBookLoadResult.Status.FAILURE,
                    new AddressBook(), List.of("Could not read data file: " + e.getMessage()));
        }
        if (result.hasErrors()) {
            saveAllowed = false;
            logger.warning("Data file at " + path + " could not be fully loaded: "
                    + String.join("\n", result.getErrors()));
        }
        return result;
    }

    private AddressBookLoadResult recoverRecords(JsonNode root) {
        AddressBook recovered = new AddressBook();
        List<String> errors = new ArrayList<>();
        reportUnknownProperties(root, ROOT_PROPERTIES, "Data file", errors);
        JsonNode records = root.get("persons");
        Map<String, Integer> originalRecords = new HashMap<>();
        for (int i = 0; i < records.size(); i++) {
            int recordNumber = i + 1;
            try {
                JsonNode record = records.get(i);
                if (!record.isObject()) {
                    throw new IllegalValueException("Member record must be a JSON object.");
                }
                reportUnknownProperties(record, PERSON_PROPERTIES, "Record " + recordNumber, errors);
                Person person = JsonUtil.fromJsonString(record.toString(), JsonAdaptedPerson.class).toModelType();
                Integer existingRecord = originalRecords.get(person.getEmail().value);
                if (existingRecord != null) {
                    errors.add(String.format(JsonSerializableAddressBook.MESSAGE_DUPLICATE_PERSON,
                            recordNumber, person.getEmail(), existingRecord));
                } else {
                    recovered.addPerson(person);
                    originalRecords.put(person.getEmail().value, recordNumber);
                }
            } catch (IllegalValueException | IOException e) {
                errors.add("Record " + recordNumber + ": " + e.getMessage());
            }
        }
        AddressBookLoadResult.Status status = errors.isEmpty()
                ? AddressBookLoadResult.Status.SUCCESS : AddressBookLoadResult.Status.PARTIAL;
        return new AddressBookLoadResult(status, recovered, errors);
    }

    private void reportUnknownProperties(JsonNode object, Set<String> knownProperties, String location,
            List<String> errors) {
        object.fieldNames().forEachRemaining(name -> {
            if (!knownProperties.contains(name)) {
                errors.add(location + ": unrecognized property \"" + name + "\".");
            }
        });
    }

    /** Returns whether this storage instance has avoided loading errors. */
    public boolean isSaveAllowed() {
        return saveAllowed;
    }

    /**
     * Saves the given {@link ReadOnlyAddressBook} to the storage.
     * @param addressBook cannot be null.
     * @throws IOException if there was any problem writing to the file.
     */
    public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
        saveAddressBook(addressBook, filePath);
    }

    /**
     * Similar to {@link #saveAddressBook(ReadOnlyAddressBook)}.
     *
     * @param filePath location of the data. Cannot be null.
     */
    public void saveAddressBook(ReadOnlyAddressBook addressBook, Path filePath) throws IOException {
        requireNonNull(addressBook);
        requireNonNull(filePath);

        if (!saveAllowed) {
            throw new IOException(MESSAGE_SAVE_DISABLED);
        }
        String json = JsonUtil.toJsonString(new JsonSerializableAddressBook(addressBook));
        fileWriter.write(filePath, json);
    }

}
