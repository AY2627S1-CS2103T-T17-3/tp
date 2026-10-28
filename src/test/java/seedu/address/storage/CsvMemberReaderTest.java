package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.model.person.Address;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Status;
import seedu.address.model.tag.Tag;

public class CsvMemberReaderTest {
    private static final String HEADER = "name,phone,email,address,tags,status\n";
    private static final String ACTIVE_ROW = "Alex Tan,91234567,alex@u.nus.edu,Kent Ridge,,active\n";

    @TempDir
    public Path testFolder;

    private final CsvMemberReader reader = new CsvMemberReader();

    @Test
    public void read_validCsv_preservesOrderStatusesAndCanonicalTags() throws Exception {
        Path file = writeCsv(HEADER + " Alex Tan , 91234567 , Alex@U.NUS.EDU ,\"NUS, Kent Ridge\","
                + "Publicity;publicity;Exco, active \n"
                + "Jamie Lee,98765432,jamie@u.nus.edu,Clementi,,archived\n");
        String original = Files.readString(file);
        List<Person> members = reader.read(file).stream().map(record -> record.getMember().orElseThrow()).toList();

        assertEquals(2, members.size());
        assertEquals("Alex Tan", members.getFirst().getName().orElseThrow().fullName);
        assertEquals("alex@u.nus.edu", members.getFirst().getEmail().value);
        assertEquals("NUS, Kent Ridge", members.getFirst().getAddress().orElseThrow().value);
        assertEquals(Set.of(new Tag("publicity"), new Tag("exco")), members.getFirst().getTags());
        assertEquals(Status.ACTIVE, members.getFirst().getStatus());
        assertEquals("Jamie Lee", members.get(1).getName().orElseThrow().fullName);
        assertEquals(Status.ARCHIVED, members.get(1).getStatus());
        assertTrue(members.get(1).getTags().isEmpty());
        assertEquals(original, Files.readString(file));
    }

    @Test
    public void read_bomCrLfAndBlankLines_acceptsSpreadsheetCsv() throws Exception {
        Path file = writeCsv("\uFEFF" + (HEADER + "\n" + ACTIVE_ROW).replace("\n", "\r\n"));
        assertEquals(1, reader.read(file).size());
        assertTrue(reader.read(file).getFirst().getMember().isPresent());
    }

    @Test
    public void read_quotedAddressAndEscapedQuotes_preservesValue() throws Exception {
        Path file = writeCsv(HEADER
                + "Alex Tan,91234567,alex@u.nus.edu,\"Block \"\"A\"\", Kent Ridge\",,active\n");
        assertEquals("Block \"A\", Kent Ridge",
                reader.read(file).getFirst().getMember().orElseThrow().getAddress().orElseThrow().value);
    }

    @Test
    public void read_emptyOrHeaderOnly_throwsIllegalValueException() throws Exception {
        for (String contents : List.of("", "\n\n", HEADER, HEADER + "\n")) {
            IllegalValueException error = assertThrows(IllegalValueException.class, () ->
                    reader.read(writeCsv(contents)));
            assertEquals(CsvMemberReader.MESSAGE_EMPTY, error.getMessage());
        }
    }

    @Test
    public void read_invalidHeaders_rejectsMissingDuplicateReorderedAndExtraColumns() throws Exception {
        for (String header : List.of("name,phone,email,address,tags", "name,phone,email,address,tags,tags",
                "phone,name,email,address,tags,status", "name,phone,email,address,tags,status,extra",
                "Name,phone,email,address,tags,status")) {
            IllegalValueException error = assertThrows(IllegalValueException.class, () ->
                    reader.read(writeCsv(header + "\n" + ACTIVE_ROW)));
            assertEquals(CsvMemberReader.MESSAGE_HEADER, error.getMessage());
        }
    }

    @Test
    public void read_wrongColumnCount_identifiesRecord() throws Exception {
        for (String row : List.of("Alex Tan,91234567,alex@u.nus.edu,Kent Ridge,",
                "Alex Tan,91234567,alex@u.nus.edu,Kent Ridge,,active,extra")) {
            CsvMemberRecord record = reader.read(writeCsv(HEADER + row)).getFirst();
            assertTrue(record.getMember().isEmpty());
            assertTrue(record.getRejectionReason().startsWith("CSV record 2: Expected exactly six columns:"));
        }
    }

    @Test
    public void read_lengthBoundaries_acceptsValuesAtOrBelowEachMaximum() throws Exception {
        for (int offset : new int[] {-1, 0}) {
            String row = String.join(",", "A".repeat(127 + offset), "9".repeat(15 + offset),
                    "boundary@u.nus.edu", "A".repeat(200 + offset), "T".repeat(30 + offset), "active");
            Person member = reader.read(writeCsv(HEADER + row)).getFirst().getMember().orElseThrow();
            assertEquals(new Name("A".repeat(127 + offset)), member.getName().orElseThrow());
            assertEquals(new Phone("9".repeat(15 + offset)), member.getPhone().orElseThrow());
            assertEquals(new Address("A".repeat(200 + offset)), member.getAddress().orElseThrow());
            assertEquals(Set.of(new Tag("t".repeat(30 + offset))), member.getTags());
        }
    }

    @Test
    public void read_overlongFields_rejectsOnlyAffectedRecordAndPreservesSource() throws Exception {
        String[][] invalidFields = {
            {"A".repeat(128), "9".repeat(16), "A".repeat(201), "t".repeat(31)},
            {Name.MESSAGE_CONSTRAINTS, Phone.MESSAGE_CONSTRAINTS, Address.MESSAGE_CONSTRAINTS, Tag.MESSAGE_CONSTRAINTS}
        };
        int[] columns = {0, 1, 3, 4};
        for (int i = 0; i < columns.length; i++) {
            String[] fields = {"Alex", "91234567", "rejected@u.nus.edu", "Kent Ridge", "exco", "active"};
            fields[columns[i]] = invalidFields[0][i];
            Path file = writeCsv(HEADER + ACTIVE_ROW + String.join(",", fields) + "\n" + ACTIVE_ROW);
            String original = Files.readString(file);
            List<CsvMemberRecord> records = reader.read(file);
            assertTrue(records.getFirst().getMember().isPresent());
            assertTrue(records.get(1).getMember().isEmpty());
            assertEquals("CSV record 3: " + invalidFields[1][i], records.get(1).getRejectionReason());
            assertTrue(records.getLast().getMember().isPresent());
            assertEquals(original, Files.readString(file));
        }
    }

    @Test
    public void read_invalidMemberField_usesExistingConstraints() throws Exception {
        String[][] invalidFields = {
            {"Alex!", Name.MESSAGE_CONSTRAINTS},
            {"12", Phone.MESSAGE_CONSTRAINTS},
            {"alex@gmail.com", Email.MESSAGE_CONSTRAINTS},
            {"\"Clemen\nti\"", Address.MESSAGE_CONSTRAINTS},
            {"publicity;", Tag.MESSAGE_CONSTRAINTS},
            {"", Status.MESSAGE_CONSTRAINTS}
        };
        for (int i = 0; i < invalidFields.length; i++) {
            String[] fields = {"Alex Tan", "91234567", "alex@u.nus.edu", "Kent Ridge", "", "active"};
            fields[i] = invalidFields[i][0];
            Path file = writeCsv(HEADER + String.join(",", fields));
            CsvMemberRecord record = reader.read(file).getFirst();
            assertTrue(record.getMember().isEmpty());
            assertEquals("CSV record 2: " + invalidFields[i][1], record.getRejectionReason());
        }
    }

    @Test
    public void read_invalidStatuses_rejectsRatherThanDefaulting() throws Exception {
        for (String status : List.of("", "former", "ACTIVE", "inactive")) {
            Path file = writeCsv(HEADER + ACTIVE_ROW.replace(",active", "," + status));
            CsvMemberRecord record = reader.read(file).getFirst();
            assertTrue(record.getMember().isEmpty());
            assertEquals("CSV record 2: " + Status.MESSAGE_CONSTRAINTS, record.getRejectionReason());
        }
    }

    @Test
    public void read_multilineField_reportsLogicalRecordNumberAndExistingConstraint() throws Exception {
        Path file = writeCsv(HEADER + ACTIVE_ROW + "\n"
                + "Jamie Lee,98765432,jamie@u.nus.edu,\"Clemen\nti\",,active\n");
        List<CsvMemberRecord> records = reader.read(file);
        assertTrue(records.getFirst().getMember().isPresent());
        assertEquals("CSV record 3: " + Address.MESSAGE_CONSTRAINTS, records.get(1).getRejectionReason());
    }

    @Test
    public void read_optionalContactCombinations_acceptsBlankAndWhitespaceOnlyCells() throws Exception {
        for (int fieldsPresent = 0; fieldsPresent < 8; fieldsPresent++) {
            boolean hasName = (fieldsPresent & 1) != 0;
            boolean hasPhone = (fieldsPresent & 2) != 0;
            boolean hasAddress = (fieldsPresent & 4) != 0;
            String row = String.join(",", hasName ? " Alex Tan " : "   ", hasPhone ? " 91234567 " : "",
                    " Alex@U.NUS.EDU ", hasAddress ? " Kent Ridge " : "   ", "", "archived");

            Person member = reader.read(writeCsv(HEADER + row)).getFirst().getMember().orElseThrow();

            assertEquals(hasName, member.getName().isPresent());
            assertEquals(hasPhone, member.getPhone().isPresent());
            assertEquals(hasAddress, member.getAddress().isPresent());
            assertEquals("alex@u.nus.edu", member.getEmail().value);
            assertEquals(Status.ARCHIVED, member.getStatus());
        }
    }

    @Test
    public void read_unicodeBlankAddressCells_returnsAbsentAddress() throws Exception {
        for (String blank : List.of("\u3000", "\u2003", "\u00a0", "\u2007", "\u202f", " \t\u3000\u2003\u00a0 ")) {
            for (String cell : List.of(blank, "\"" + blank + "\"")) {
                Path file = writeCsv(HEADER + ",,blank@u.nus.edu," + cell + ",,active\n");
                Person member = reader.read(file).getFirst().getMember().orElseThrow();

                assertTrue(member.getAddress().isEmpty());
            }
        }
    }

    @Test
    public void read_nonblankUnicodeAddress_retainsExistingQuotedAndUnquotedTrimming() throws Exception {
        String address = "\u3000Kent\u3000Ridge\u2003";
        Path file = writeCsv(HEADER + ",,quoted@u.nus.edu,\"" + address + "\",,active\n"
                + ",,unquoted@u.nus.edu," + address + ",,active\n");
        List<CsvMemberRecord> records = reader.read(file);

        assertEquals(address, records.get(0).getMember().orElseThrow().getAddress().orElseThrow().value);
        assertEquals("Kent\u3000Ridge", records.get(1).getMember().orElseThrow().getAddress().orElseThrow().value);
    }

    @Test
    public void read_missingEmail_rejectsRecord() throws Exception {
        CsvMemberRecord record = reader.read(writeCsv(HEADER + ",,   ,,,active\n")).getFirst();

        assertTrue(record.getMember().isEmpty());
        assertEquals("CSV record 2: " + Email.MESSAGE_CONSTRAINTS, record.getRejectionReason());
    }

    @Test
    public void read_multipleInvalidRecords_preservesOutcomesAndOriginalRecordNumbers() throws Exception {
        Path file = writeCsv(HEADER + "\n" + ACTIVE_ROW + "\n"
                + "Jamie Lee,12,jamie@u.nus.edu,\"Clemen\nti\",,active\n"
                + "too,few,columns\n" + ",,last@u.nus.edu,,,active\n");

        List<CsvMemberRecord> records = reader.read(file);

        assertEquals(List.of(2L, 3L, 4L, 5L), records.stream().map(CsvMemberRecord::getRecordNumber).toList());
        assertTrue(records.get(0).getMember().isPresent());
        assertEquals("CSV record 3: " + Phone.MESSAGE_CONSTRAINTS, records.get(1).getRejectionReason());
        assertTrue(records.get(2).getRejectionReason().startsWith("CSV record 4: Expected exactly six columns:"));
        assertEquals("last@u.nus.edu", records.get(3).getMember().orElseThrow().getEmail().value);
    }

    @Test
    public void read_malformedCsvAfterValidRecord_throwsIoException() throws Exception {
        Path file = writeCsv(HEADER + ACTIVE_ROW + "\"unterminated");
        assertThrows(IOException.class, () -> reader.read(file));
    }

    @Test
    public void read_nonUtf8File_throwsIoException() throws Exception {
        Path file = testFolder.resolve("invalid.csv");
        Files.write(file, new byte[] {(byte) 0xc3, (byte) 0x28});
        assertThrows(IOException.class, () -> reader.read(file));
    }

    @Test
    public void read_missingFileOrDirectory_throwsIoException() {
        assertThrows(IOException.class, () -> reader.read(testFolder.resolve("missing.csv")));
        assertThrows(IOException.class, () -> reader.read(testFolder));
    }

    private Path writeCsv(String contents) throws IOException {
        return Files.writeString(testFolder.resolve("members.csv"), contents);
    }
}
