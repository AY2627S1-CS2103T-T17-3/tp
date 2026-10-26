package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.StatusFilter;
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

        assertEquals(String.format(ImportCommand.MESSAGE_SUCCESS, 2, 0, file), result.getFeedbackToUser());
        assertEquals(3, members.size());
        assertEquals(existingMember, members.getFirst());
        assertEquals("alex@u.nus.edu", members.get(1).getEmail().value);
        assertEquals(Status.ARCHIVED, members.get(2).getStatus());
        assertEquals(members, model.getFilteredPersonList());
    }

    @Test
    public void execute_mixedStatusesInActiveView_countsOnlyAcceptedHiddenMembers() throws Exception {
        model.setPerson(existingMember, existingMember.withStatus(Status.ARCHIVED));
        model.setStatusFilter(StatusFilter.ACTIVE);
        Path file = writeCsv(HEADER + FIRST_ROW
                + ",,hidden@u.nus.edu,,,archived\n"
                + ",,another.hidden@u.nus.edu,,,archived\n"
                + ",,EXISTING@U.NUS.EDU,,,archived\n"
                + ",12,invalid@u.nus.edu,,,archived\n");

        CommandResult result = new ImportCommand(file).execute(model);

        String feedback = result.getFeedbackToUser();
        assertTrue(feedback.startsWith(String.format(ImportCommand.MESSAGE_SUCCESS, 3, 2, file)));
        assertTrue(feedback.contains("Record numbers include the header as record 1."));
        assertTrue(feedback.contains("CSV record 5: email \"existing@u.nus.edu\" duplicates"));
        assertTrue(feedback.contains("CSV record 6: Phone numbers"));
        assertTrue(feedback.endsWith("2 imported member(s) are hidden by the current view. Use view all to see them."));
        assertEquals(StatusFilter.ACTIVE, model.getStatusFilter());
        assertEquals(4, model.getAddressBook().getPersonList().size());
        assertEquals(List.of("alex@u.nus.edu"), model.getFilteredPersonList().stream()
                .map(person -> person.getEmail().value).toList());
    }

    @Test
    public void execute_activeMemberInActiveView_omitsHiddenMemberHint() throws Exception {
        model.setStatusFilter(StatusFilter.ACTIVE);
        Path file = writeCsv(HEADER + FIRST_ROW);

        CommandResult result = new ImportCommand(file).execute(model);

        assertEquals(String.format(ImportCommand.MESSAGE_SUCCESS, 1, 0, file), result.getFeedbackToUser());
        assertEquals(StatusFilter.ACTIVE, model.getStatusFilter());
        assertEquals(model.getAddressBook().getPersonList(), model.getFilteredPersonList());
    }

    @Test
    public void execute_archivedMemberInArchivedView_omitsHiddenMemberHint() throws Exception {
        model.setStatusFilter(StatusFilter.ARCHIVED);
        Path file = writeCsv(HEADER + ",,archived@u.nus.edu,,,archived\n");

        CommandResult result = new ImportCommand(file).execute(model);

        assertEquals(String.format(ImportCommand.MESSAGE_SUCCESS, 1, 0, file), result.getFeedbackToUser());
        assertEquals(StatusFilter.ARCHIVED, model.getStatusFilter());
        assertEquals(List.of("archived@u.nus.edu"), model.getFilteredPersonList().stream()
                .map(person -> person.getEmail().value).toList());
    }

    @Test
    public void execute_duplicateExistingArchivedMemberOutsideFilter_skipsDuplicate() throws Exception {
        Person archivedMember = new Person(existingMember.getName(), existingMember.getPhone(),
                existingMember.getEmail(), existingMember.getAddress(), Status.ARCHIVED, existingMember.getTags());
        model.setPerson(existingMember, archivedMember);
        Path file = writeCsv(HEADER + FIRST_ROW
                + "Jamie Lee,98765432,EXISTING@U.NUS.EDU,Clementi,,active\n");
        CommandResult result = new ImportCommand(file).execute(model);

        assertTrue(result.getFeedbackToUser().startsWith(String.format(ImportCommand.MESSAGE_SUCCESS, 1, 1, file)));
        assertTrue(result.getFeedbackToUser().contains("Record numbers include the header as record 1."));
        assertTrue(result.getFeedbackToUser().contains("CSV record 3: email \"existing@u.nus.edu\" duplicates"));
        assertEquals(2, model.getAddressBook().getPersonList().size());
        assertEquals(archivedMember, model.getAddressBook().getPersonList().getFirst());
        assertEquals("alex@u.nus.edu", model.getAddressBook().getPersonList().get(1).getEmail().value);
    }

    @Test
    public void execute_duplicateEmailWithinCsv_keepsFirstValidOccurrence() throws Exception {
        Path file = writeCsv(HEADER + FIRST_ROW
                + "Jamie Lee,98765432,ALEX@U.NUS.EDU,Clementi,,archived\n");
        CommandResult result = new ImportCommand(file).execute(model);

        assertTrue(result.getFeedbackToUser().startsWith(String.format(ImportCommand.MESSAGE_SUCCESS, 1, 1, file)));
        assertTrue(result.getFeedbackToUser().contains("CSV record 3: email \"alex@u.nus.edu\" duplicates"));
        assertEquals(2, model.getAddressBook().getPersonList().size());
        Person imported = model.getAddressBook().getPersonList().get(1);
        assertEquals("Alex Tan", imported.getName().orElseThrow().fullName);
        assertEquals(Status.ACTIVE, imported.getStatus());
    }

    @Test
    public void execute_sameNameAndPhoneDifferentEmails_succeeds() throws Exception {
        Path file = writeCsv(HEADER + FIRST_ROW
                + "Alex Tan,91234567,other.alex@u.nus.edu,Kent Ridge,,active\n");
        new ImportCommand(file).execute(model);
        assertEquals(3, model.getAddressBook().getPersonList().size());
    }

    @Test
    public void execute_invalidLateRecord_importsValidPrefix() throws Exception {
        Path file = writeCsv(HEADER + FIRST_ROW + "Jamie Lee,12,jamie@u.nus.edu,Clementi,,active\n");
        CommandResult result = new ImportCommand(file).execute(model);

        assertTrue(result.getFeedbackToUser().contains("CSV record 3: Phone numbers"));
        assertEquals(2, model.getAddressBook().getPersonList().size());
    }

    @Test
    public void execute_mixedRecords_reportsEveryRejectionInCsvOrder() throws Exception {
        Path file = writeCsv(HEADER + FIRST_ROW
                + "Bad!,91234567,bad@u.nus.edu,Kent Ridge,,active\n"
                + ",,EXISTING@U.NUS.EDU,,,active\n"
                + "too,few,columns\n"
                + ",,last@u.nus.edu,,,archived\n");
        String sourceBefore = Files.readString(file);

        CommandResult result = new ImportCommand(file).execute(model);

        String feedback = result.getFeedbackToUser();
        assertTrue(feedback.startsWith(String.format(ImportCommand.MESSAGE_SUCCESS, 2, 3, file)));
        assertTrue(feedback.indexOf("CSV record 3:") < feedback.indexOf("CSV record 4:"));
        assertTrue(feedback.indexOf("CSV record 4:") < feedback.indexOf("CSV record 5:"));
        assertEquals(List.of("existing@u.nus.edu", "alex@u.nus.edu", "last@u.nus.edu"),
                model.getAddressBook().getPersonList().stream().map(person -> person.getEmail().value).toList());
        Person incomplete = model.getAddressBook().getPersonList().get(2);
        assertTrue(incomplete.getName().isEmpty());
        assertTrue(incomplete.getPhone().isEmpty());
        assertTrue(incomplete.getAddress().isEmpty());
        assertEquals(Status.ARCHIVED, incomplete.getStatus());
        assertEquals(model.getAddressBook().getPersonList(), model.getFilteredPersonList());
        assertEquals(sourceBefore, Files.readString(file));
    }

    @Test
    public void execute_invalidEarlierOccurrence_doesNotReserveEmail() throws Exception {
        Path file = writeCsv(HEADER + "Alex Tan,12,alex@u.nus.edu,Kent Ridge,,active\n" + FIRST_ROW);

        CommandResult result = new ImportCommand(file).execute(model);

        assertTrue(result.getFeedbackToUser().startsWith(String.format(ImportCommand.MESSAGE_SUCCESS, 1, 1, file)));
        assertTrue(result.getFeedbackToUser().contains("Record numbers include the header as record 1."));
        assertTrue(result.getFeedbackToUser().contains("CSV record 2: Phone numbers"));
        assertEquals("alex@u.nus.edu", model.getAddressBook().getPersonList().get(1).getEmail().value);
    }

    @Test
    public void execute_duplicateAfterInvalidMultilineRecord_reportsOriginalRecordNumber() throws Exception {
        Path file = writeCsv(HEADER + FIRST_ROW
                + "Bad Name,91234567,bad@u.nus.edu,\"Clemen\nti\",,active\n\n"
                + ",,ALEX@U.NUS.EDU,,,active\n");

        CommandResult result = new ImportCommand(file).execute(model);

        assertTrue(result.getFeedbackToUser().contains("CSV record 3: Addresses"));
        assertTrue(result.getFeedbackToUser().contains("CSV record 4: email \"alex@u.nus.edu\" duplicates"));
        assertEquals(2, model.getAddressBook().getPersonList().size());
    }

    @Test
    public void execute_allRejected_reportsCountsAndPreservesRecordsAndFilter() throws Exception {
        model.setStatusFilter(StatusFilter.ARCHIVED);
        Path file = writeCsv(HEADER + ",,existing@u.nus.edu,,,active\n"
                + ",12,invalid@u.nus.edu,,,active\n");

        assertRejectedWithoutChanges(file, String.format(ImportCommand.MESSAGE_SUCCESS, 0, 2, file)
                + "\nRecord numbers include the header as record 1.");
    }

    @Test
    public void execute_partialImportSample_matchesDocumentedCounts() throws Exception {
        Path file = Path.of("docs", "samples", "members-partial.csv");

        CommandResult result = new ImportCommand(file).execute(model);

        assertTrue(result.getFeedbackToUser().startsWith(String.format(ImportCommand.MESSAGE_SUCCESS, 2, 2, file)));
        assertTrue(result.getFeedbackToUser().contains("CSV record 3: Phone numbers"));
        assertTrue(result.getFeedbackToUser().contains("CSV record 4: email \"alex@u.nus.edu\" duplicates"));
        assertEquals(3, model.getAddressBook().getPersonList().size());
    }

    @Test
    public void execute_mixedRecords_logsCountsWithoutMemberDetails() throws Exception {
        Path file = writeCsv(HEADER + FIRST_ROW + ",,EXISTING@U.NUS.EDU,,,active\n");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        StreamHandler handler = new StreamHandler(output, new SimpleFormatter());
        Logger logger = LogsCenter.getLogger(ImportCommand.class).getParent();
        logger.addHandler(handler);
        try {
            new ImportCommand(file).execute(model);
            handler.flush();
        } finally {
            logger.removeHandler(handler);
            handler.close();
        }

        String logs = output.toString(StandardCharsets.UTF_8);
        assertTrue(logs.contains("Prepared CSV import: 1 accepted, 1 rejected."));
        assertFalse(logs.contains("alex@u.nus.edu"));
        assertFalse(logs.contains("existing@u.nus.edu"));
        assertFalse(logs.contains("Alex Tan"));
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
        StatusFilter statusFilterBefore = model.getStatusFilter();
        String sourceBefore = Files.readString(file);
        CommandException error = assertThrows(CommandException.class, () -> new ImportCommand(file).execute(model));
        assertTrue(error.getMessage().contains(expectedReason), error.getMessage());
        assertFalse(error.getMessage().contains("Use view all"));
        assertEquals(before, model.getAddressBook());
        assertEquals(filteredBefore, model.getFilteredPersonList());
        assertEquals(statusFilterBefore, model.getStatusFilter());
        assertEquals(sourceBefore, Files.readString(file));
    }

    private Path writeCsv(String contents) throws IOException {
        return Files.writeString(testFolder.resolve("members.csv"), contents);
    }
}
