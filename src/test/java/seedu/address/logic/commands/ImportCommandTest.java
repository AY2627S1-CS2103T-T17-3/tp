package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.model.person.Status;
import seedu.address.testutil.PersonBuilder;

public class ImportCommandTest {
    private static final String HEADER = "name,phone,email,address,tags,status\n";
    private static final String FIRST_ROW = "Alex Tan,91234567,alex@u.nus.edu,Kent Ridge,Publicity,active\n";

    @TempDir
    public Path testFolder;

    private Model model;
    private Person existingMember;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(new AddressBook(), new UserPrefs());
        existingMember = new PersonBuilder().withEmail("existing@u.nus.edu").withTags("Exco").build();
        model.addPerson(existingMember);
        model.updateFilteredPersonList(person -> false);
    }

    @Test
    public void execute_validCsv_appendsMembersAndShowsEntireDirectory() throws Exception {
        Path file = writeCsv(HEADER + FIRST_ROW
                + "Jamie Lee,98765432,jamie@u.nus.edu,Clementi,,archived\n");
        CommandResult result = new ImportCommand(file).execute(model);
        List<Person> members = model.getAddressBook().getPersonList();

        assertEquals(String.format(ImportCommand.MESSAGE_SUCCESS, 2, file), result.getFeedbackToUser());
        assertEquals(3, members.size());
        assertEquals(existingMember, members.getFirst());
        assertEquals("alex@u.nus.edu", members.get(1).getEmail().value);
        assertEquals(Status.ARCHIVED, members.get(2).getStatus());
        assertEquals(members, model.getFilteredPersonList());
    }

    @Test
    public void execute_duplicateExistingArchivedMemberOutsideFilter_rejectsEntireImport() throws Exception {
        Person archivedMember = new Person(existingMember.getName(), existingMember.getPhone(),
                existingMember.getEmail(), existingMember.getAddress(), Status.ARCHIVED, existingMember.getTags());
        model.setPerson(existingMember, archivedMember);
        Path file = writeCsv(HEADER + FIRST_ROW
                + "Jamie Lee,98765432,EXISTING@U.NUS.EDU,Clementi,,active\n");
        assertRejectedWithoutChanges(file, "CSV record 3: email \"existing@u.nus.edu\" duplicates");
    }

    @Test
    public void execute_duplicateEmailWithinCsv_rejectsEntireImport() throws Exception {
        Path file = writeCsv(HEADER + FIRST_ROW
                + "Jamie Lee,98765432,ALEX@U.NUS.EDU,Clementi,,archived\n");
        assertRejectedWithoutChanges(file, "CSV record 3: email \"alex@u.nus.edu\" duplicates");
    }

    @Test
    public void execute_sameNameAndPhoneDifferentEmails_succeeds() throws Exception {
        Path file = writeCsv(HEADER + FIRST_ROW
                + "Alex Tan,91234567,other.alex@u.nus.edu,Kent Ridge,,active\n");
        new ImportCommand(file).execute(model);
        assertEquals(3, model.getAddressBook().getPersonList().size());
    }

    @Test
    public void execute_invalidLateRecord_rejectsEntireImport() throws Exception {
        Path file = writeCsv(HEADER + FIRST_ROW + "Jamie Lee,12,jamie@u.nus.edu,Clementi,,active\n");
        assertRejectedWithoutChanges(file, "CSV record 3: Phone numbers");
    }

    @Test
    public void execute_malformedLateRecord_rejectsEntireImport() throws Exception {
        Path file = writeCsv(HEADER + FIRST_ROW + "\"unterminated");
        assertRejectedWithoutChanges(file, "No members were imported.");
    }

    @Test
    public void execute_missingFile_preservesRecordsAndFilter() throws Exception {
        AddressBook before = new AddressBook(model.getAddressBook());
        CommandException error = assertThrows(CommandException.class, () ->
                new ImportCommand(testFolder.resolve("missing.csv")).execute(model));
        assertTrue(error.getMessage().contains("No members were imported."));
        assertEquals(before, model.getAddressBook());
        assertTrue(model.getFilteredPersonList().isEmpty());
    }

    private void assertRejectedWithoutChanges(Path file, String expectedReason) throws Exception {
        AddressBook before = new AddressBook(model.getAddressBook());
        List<Person> filteredBefore = List.copyOf(model.getFilteredPersonList());
        String sourceBefore = Files.readString(file);
        CommandException error = assertThrows(CommandException.class, () -> new ImportCommand(file).execute(model));
        assertTrue(error.getMessage().contains(expectedReason), error.getMessage());
        assertEquals(before, model.getAddressBook());
        assertEquals(filteredBefore, model.getFilteredPersonList());
        assertEquals(sourceBefore, Files.readString(file));
    }

    private Path writeCsv(String contents) throws IOException {
        return Files.writeString(testFolder.resolve("members.csv"), contents);
    }
}
