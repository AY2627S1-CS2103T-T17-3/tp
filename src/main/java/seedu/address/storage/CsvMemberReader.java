package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.model.person.Address;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Status;
import seedu.address.model.tag.Tag;

/**
 * Reads the fixed member CSV template without changing the source file or the directory.
 */
public class CsvMemberReader {
    public static final String HEADER = "name,phone,email,address,tags,status";
    public static final String MESSAGE_HEADER = "Expected CSV header: " + HEADER;
    public static final String MESSAGE_EMPTY = "The CSV must contain a header and at least one member.";

    private static final CSVFormat FORMAT = CSVFormat.RFC4180.builder()
            .setIgnoreEmptyLines(true)
            .setIgnoreSurroundingSpaces(true)
            .get();

    /**
     * Reads and validates all member records, preserving their order and membership statuses.
     * CSV record numbers include the header as record 1 and exclude empty lines.
     *
     * @param path The UTF-8 CSV file to read.
     * @return Validated members in CSV order.
     * @throws IOException If the file cannot be read or contains malformed CSV.
     * @throws IllegalValueException If the header or a member record is invalid, or no members are present.
     */
    public List<Person> read(Path path) throws IOException, IllegalValueException {
        requireNonNull(path);
        try (PushbackReader reader = new PushbackReader(Files.newBufferedReader(path, StandardCharsets.UTF_8))) {
            // Spreadsheet tools may prefix UTF-8 CSV files with a byte order mark.
            int firstCharacter = reader.read();
            if (firstCharacter != -1 && firstCharacter != '\uFEFF') {
                reader.unread(firstCharacter);
            }
            try (CSVParser parser = FORMAT.parse(reader)) {
                List<CSVRecord> records = parser.getRecords();
                if (records.isEmpty()) {
                    throw new IllegalValueException(MESSAGE_EMPTY);
                }
                if (!records.getFirst().toList().equals(List.of(HEADER.split(",")))) {
                    throw new IllegalValueException(MESSAGE_HEADER);
                }
                if (records.size() == 1) {
                    throw new IllegalValueException(MESSAGE_EMPTY);
                }
                List<Person> members = new ArrayList<>();
                for (CSVRecord record : records.subList(1, records.size())) {
                    members.add(parseMember(record));
                }
                return List.copyOf(members);
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private Person parseMember(CSVRecord record) throws IllegalValueException {
        try {
            if (record.size() != 6) {
                throw new IllegalArgumentException("Expected exactly six columns: " + HEADER);
            }
            Name name = new Name(record.get(0).trim());
            Phone phone = new Phone(record.get(1).trim());
            Email email = new Email(record.get(2).trim());
            Address address = new Address(record.get(3).trim());
            Set<Tag> tags = parseTags(record.get(4).trim());
            String statusValue = record.get(5).trim();
            if (!Status.isValidStatus(statusValue)) {
                throw new IllegalArgumentException(Status.MESSAGE_CONSTRAINTS);
            }
            Status status = statusValue.equals("active") ? Status.ACTIVE : Status.ARCHIVED;
            return new Person(name, phone, email, address, status, tags);
        } catch (IllegalArgumentException e) {
            throw new IllegalValueException("CSV record " + record.getRecordNumber() + ": " + e.getMessage(), e);
        }
    }

    private Set<Tag> parseTags(String value) {
        Set<Tag> tags = new HashSet<>();
        if (!value.isEmpty()) {
            for (String tagName : value.split(";", -1)) {
                tags.add(new Tag(tagName.trim()));
            }
        }
        return tags;
    }
}
