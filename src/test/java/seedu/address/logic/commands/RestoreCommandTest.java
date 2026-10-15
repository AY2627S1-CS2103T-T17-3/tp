package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.logic.commands.CommandTestUtil.showPersonAtIndex;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static seedu.address.testutil.TypicalIndexes.INDEX_SECOND_PERSON;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.Messages;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.model.person.Status;
import seedu.address.testutil.PersonBuilder;

public class RestoreCommandTest {

    private final Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

    @Test
    public void constructor_nullIndex_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new RestoreCommand(null));
    }

    @Test
    public void execute_nullModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new RestoreCommand(INDEX_FIRST_PERSON).execute(null));
    }

    @Test
    public void execute_archivedMember_preservesAllOtherDetails() {
        Person person = archivePerson(INDEX_SECOND_PERSON);
        assertRestored(INDEX_SECOND_PERSON, person);
    }

    @Test
    public void execute_filteredList_usesDisplayedIndexAndShowsAllMembers() {
        Person person = archivePerson(INDEX_SECOND_PERSON);
        showPersonAtIndex(model, INDEX_SECOND_PERSON);
        assertRestored(INDEX_FIRST_PERSON, person);
    }

    @Test
    public void execute_duplicateNames_restoresOnlySelectedMember() {
        Person first = archivePerson(INDEX_FIRST_PERSON);
        Person second = model.getFilteredPersonList().get(INDEX_SECOND_PERSON.getZeroBased());
        Person sameName = new PersonBuilder(second).withName(first.getName().orElseThrow().fullName)
                .withStatus(Status.ARCHIVED).build();
        model.setPerson(second, sameName);
        assertRestored(INDEX_SECOND_PERSON, sameName);
    }

    @Test
    public void execute_emailOnlyMember_preservesMissingDetails() throws Exception {
        Person original = model.getFilteredPersonList().get(0);
        Person archived = new PersonBuilder(original).withoutName().withoutPhone().withoutAddress()
                .withTags().withStatus(Status.ARCHIVED).build();
        model.setPerson(original, archived);

        CommandResult result = new RestoreCommand(INDEX_FIRST_PERSON).execute(model);

        assertEquals("Restored member to active status: Email: " + archived.getEmail(), result.getFeedbackToUser());
        assertEquals(new PersonBuilder(archived).withStatus(Status.ACTIVE).build(),
                model.getFilteredPersonList().get(0));
    }

    @Test
    public void execute_activeMember_preservesRecordsAndFilter() {
        showPersonAtIndex(model, INDEX_SECOND_PERSON);
        assertCommandFailure(new RestoreCommand(INDEX_FIRST_PERSON), model, RestoreCommand.MESSAGE_ALREADY_ACTIVE);
    }

    @Test
    public void execute_invalidIndexUnfilteredList_throwsCommandException() {
        Index index = Index.fromOneBased(model.getFilteredPersonList().size() + 1);
        assertCommandFailure(new RestoreCommand(index), model, Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void execute_invalidIndexFilteredList_preservesRecordsAndFilter() {
        archivePerson(INDEX_SECOND_PERSON);
        showPersonAtIndex(model, INDEX_SECOND_PERSON);
        assertCommandFailure(new RestoreCommand(INDEX_SECOND_PERSON), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void execute_emptyList_throwsCommandException() {
        assertCommandFailure(new RestoreCommand(INDEX_FIRST_PERSON), new ModelManager(),
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void equals() {
        RestoreCommand command = new RestoreCommand(INDEX_FIRST_PERSON);
        assertTrue(command.equals(command));
        assertTrue(command.equals(new RestoreCommand(INDEX_FIRST_PERSON)));
        assertFalse(command.equals(new RestoreCommand(INDEX_SECOND_PERSON)));
        assertFalse(command.equals(null));
        assertFalse(command.equals(1));
    }

    @Test
    public void toStringMethod() {
        RestoreCommand command = new RestoreCommand(INDEX_FIRST_PERSON);
        assertEquals(RestoreCommand.class.getCanonicalName() + "{targetIndex=" + INDEX_FIRST_PERSON + "}",
                command.toString());
    }

    private Person archivePerson(Index index) {
        Person original = model.getFilteredPersonList().get(index.getZeroBased());
        Person archived = new PersonBuilder(original).withStatus(Status.ARCHIVED).build();
        model.setPerson(original, archived);
        return archived;
    }

    private void assertRestored(Index index, Person archived) {
        Person restored = new PersonBuilder(archived).withStatus(Status.ACTIVE).build();
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.setPerson(archived, restored);
        assertCommandSuccess(new RestoreCommand(index), model,
                String.format(RestoreCommand.MESSAGE_SUCCESS, Messages.formatProvidedDetails(restored)), expectedModel);
    }
}
