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
import java.util.Optional;
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
     * Reads the complete CSV and collects validated members and record-level rejection reasons in CSV order.
     * CSV record numbers include the header as record 1 and exclude empty lines.
     *
     * @param path The UTF-8 CSV file to read.
     * @return Every member record's original number and either its member or rejection reason.
     * @throws IOException If the file cannot be read or contains malformed CSV.
     * @throws IllegalValueException If the header is invalid or no member records are present.
     */
    public List<CsvMemberRecord> read(Path path) throws IOException, IllegalValueException {
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
                List<CsvMemberRecord> members = new ArrayList<>();
                for (CSVRecord record : records.subList(1, records.size())) {
                    try {
                        members.add(CsvMemberRecord.accepted(record.getRecordNumber(), parseMember(record)));
                    } catch (IllegalValueException e) {
                        members.add(CsvMemberRecord.rejected(record.getRecordNumber(), e.getMessage()));
                    }
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
            String nameValue = record.get(0).trim();
            Optional<Name> name = nameValue.isEmpty() ? Optional.empty() : Optional.of(new Name(nameValue));
            String phoneValue = record.get(1).trim();
            Optional<Phone> phone = phoneValue.isEmpty() ? Optional.empty() : Optional.of(new Phone(phoneValue));
            Email email = new Email(record.get(2).trim());
            String addressValue = record.get(3).trim();
            Optional<Address> address = Address.isBlankAddress(addressValue)
                    ? Optional.empty() : Optional.of(new Address(addressValue));
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
