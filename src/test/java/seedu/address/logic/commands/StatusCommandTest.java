package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/**
 * Tests archiving and activating members through the model.
 */
public class StatusCommandTest {

    private final Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

    @Test
    public void constructor_nullIndex_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new ArchiveCommand(null));
        assertThrows(NullPointerException.class, () -> new ActivateCommand(null));
    }

    @Test
    public void archive_filteredList_usesDisplayedIndexAndPreservesDetails() {
        Person person = model.getFilteredPersonList().get(INDEX_SECOND_PERSON.getZeroBased());
        showPersonAtIndex(model, INDEX_SECOND_PERSON);
        Person archivedPerson = person.withStatus(Status.ARCHIVED);
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.setPerson(person, archivedPerson);

        assertCommandSuccess(new ArchiveCommand(INDEX_FIRST_PERSON), model,
                String.format(ArchiveCommand.MESSAGE_SUCCESS, Messages.formatProvidedDetails(archivedPerson)),
                expectedModel);
    }

    @Test
    public void activate_archivedMember_restoresActiveStatusAndPreservesDetails() {
        Person person = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        Person archivedPerson = person.withStatus(Status.ARCHIVED);
        model.setPerson(person, archivedPerson);
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.setPerson(archivedPerson, person);

        assertCommandSuccess(new ActivateCommand(INDEX_FIRST_PERSON), model,
                String.format(ActivateCommand.MESSAGE_SUCCESS, Messages.formatProvidedDetails(person)), expectedModel);
    }

    @Test
    public void archiveAndActivate_emailOnlyMember_preservesMissingDetails() throws Exception {
        Person original = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        Person emailOnly = new PersonBuilder(original).withoutName().withoutPhone().withoutAddress().withTags().build();
        model.setPerson(original, emailOnly);

        new ArchiveCommand(INDEX_FIRST_PERSON).execute(model);
        assertEquals(emailOnly.withStatus(Status.ARCHIVED), model.getFilteredPersonList().get(0));
        new ActivateCommand(INDEX_FIRST_PERSON).execute(model);
        assertEquals(emailOnly, model.getFilteredPersonList().get(0));
    }

    @Test
    public void archive_alreadyArchived_throwsCommandException() {
        Person person = model.getFilteredPersonList().get(0);
        model.setPerson(person, person.withStatus(Status.ARCHIVED));
        assertCommandFailure(new ArchiveCommand(INDEX_FIRST_PERSON), model, ArchiveCommand.MESSAGE_ALREADY_ARCHIVED);
    }

    @Test
    public void activate_alreadyActive_throwsCommandException() {
        assertCommandFailure(new ActivateCommand(INDEX_FIRST_PERSON), model, ActivateCommand.MESSAGE_ALREADY_ACTIVE);
    }

    @Test
    public void invalidDisplayedIndex_throwsCommandException() {
        Index outOfBounds = Index.fromOneBased(model.getFilteredPersonList().size() + 1);
        assertCommandFailure(new ArchiveCommand(outOfBounds), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        assertCommandFailure(new ActivateCommand(outOfBounds), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void archive_invalidIndexInFilteredList_throwsCommandException() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        assertCommandFailure(new ArchiveCommand(INDEX_SECOND_PERSON), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }
}
