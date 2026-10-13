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
        List<Person> members = reader.read(file);

        assertEquals(2, members.size());
        assertEquals("Alex Tan", members.getFirst().getName().fullName);
        assertEquals("alex@u.nus.edu", members.getFirst().getEmail().value);
        assertEquals("NUS, Kent Ridge", members.getFirst().getAddress().value);
        assertEquals(Set.of(new Tag("publicity"), new Tag("exco")), members.getFirst().getTags());
        assertEquals(Status.ACTIVE, members.getFirst().getStatus());
        assertEquals("Jamie Lee", members.get(1).getName().fullName);
        assertEquals(Status.ARCHIVED, members.get(1).getStatus());
        assertTrue(members.get(1).getTags().isEmpty());
        assertEquals(original, Files.readString(file));
    }

    @Test
    public void read_bomCrLfAndBlankLines_acceptsSpreadsheetCsv() throws Exception {
        Path file = writeCsv("\uFEFF" + (HEADER + "\n" + ACTIVE_ROW).replace("\n", "\r\n"));
        assertEquals(1, reader.read(file).size());
    }

    @Test
    public void read_quotedAddressAndEscapedQuotes_preservesValue() throws Exception {
        Path file = writeCsv(HEADER
                + "Alex Tan,91234567,alex@u.nus.edu,\"Block \"\"A\"\", Kent Ridge\",,active\n");
        assertEquals("Block \"A\", Kent Ridge", reader.read(file).getFirst().getAddress().value);
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
            IllegalValueException error = assertThrows(IllegalValueException.class, () ->
                    reader.read(writeCsv(HEADER + row)));
            assertTrue(error.getMessage().startsWith("CSV record 2: Expected exactly six columns:"));
        }
    }

    @Test
    public void read_invalidMemberField_usesExistingConstraints() throws Exception {
        String[][] invalidFields = {
            {"", Name.MESSAGE_CONSTRAINTS},
            {"12", Phone.MESSAGE_CONSTRAINTS},
            {"alex@gmail.com", Email.MESSAGE_CONSTRAINTS},
            {"", Address.MESSAGE_CONSTRAINTS},
            {"publicity;", Tag.MESSAGE_CONSTRAINTS},
            {"", Status.MESSAGE_CONSTRAINTS}
        };
        for (int i = 0; i < invalidFields.length; i++) {
            String[] fields = {"Alex Tan", "91234567", "alex@u.nus.edu", "Kent Ridge", "", "active"};
            fields[i] = invalidFields[i][0];
            Path file = writeCsv(HEADER + String.join(",", fields));
            IllegalValueException error = assertThrows(IllegalValueException.class, () -> reader.read(file));
            assertEquals("CSV record 2: " + invalidFields[i][1], error.getMessage());
        }
    }

    @Test
    public void read_invalidStatuses_rejectsRatherThanDefaulting() throws Exception {
        for (String status : List.of("former", "ACTIVE", "inactive")) {
            Path file = writeCsv(HEADER + ACTIVE_ROW.replace(",active", "," + status));
            IllegalValueException error = assertThrows(IllegalValueException.class, () -> reader.read(file));
            assertEquals("CSV record 2: " + Status.MESSAGE_CONSTRAINTS, error.getMessage());
        }
    }

    @Test
    public void read_multilineField_reportsLogicalRecordNumberAndExistingConstraint() throws Exception {
        Path file = writeCsv(HEADER + ACTIVE_ROW + "\n"
                + "Jamie Lee,98765432,jamie@u.nus.edu,\"Clemen\nti\",,active\n");
        IllegalValueException error = assertThrows(IllegalValueException.class, () -> reader.read(file));
        assertEquals("CSV record 3: " + Address.MESSAGE_CONSTRAINTS, error.getMessage());
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
