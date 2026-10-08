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
import seedu.address.model.tag.Tag;
import seedu.address.testutil.PersonBuilder;

/**
 * Tests adding a tag through the model, including displayed indices and unchanged data.
 */
public class TagAddCommandTest {

    private final Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

    @Test
    public void constructor_nullArguments_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new TagAddCommand(null, new Tag("Volunteer")));
        assertThrows(NullPointerException.class, () -> new TagAddCommand(INDEX_FIRST_PERSON, null));
    }

    @Test
    public void execute_nullModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                new TagAddCommand(INDEX_FIRST_PERSON, new Tag("Volunteer")).execute(null));
    }

    @Test
    public void execute_newTag_preservesOtherTagsAndDetails() {
        Person person = setTags(INDEX_FIRST_PERSON, "Publicity", "Exco");
        Person expectedPerson = new PersonBuilder(person).withTags("Publicity", "Exco", "Volunteer").build();
        assertTagAdded(INDEX_FIRST_PERSON, "Volunteer", person, expectedPerson);
    }

    @Test
    public void execute_noExistingTags_addsTag() {
        Person person = setTags(INDEX_FIRST_PERSON);
        Person expectedPerson = new PersonBuilder(person).withTags("Volunteer").build();
        assertTagAdded(INDEX_FIRST_PERSON, "Volunteer", person, expectedPerson);
    }

    @Test
    public void execute_existingTagWithDifferentCase_succeedsWithoutChangingTags() {
        Person person = setTags(INDEX_FIRST_PERSON, "Publicity", "Exco");
        assertTagAdded(INDEX_FIRST_PERSON, "PUBLICITY", person, person);
    }

    @Test
    public void execute_filteredList_usesDisplayedIndexAndShowsAllPersons() {
        Person person = setTags(INDEX_SECOND_PERSON, "Publicity", "Exco");
        showPersonAtIndex(model, INDEX_SECOND_PERSON);
        Person expectedPerson = new PersonBuilder(person).withTags("Publicity", "Exco", "Volunteer").build();
        assertTagAdded(INDEX_FIRST_PERSON, "Volunteer", person, expectedPerson);
    }

    @Test
    public void execute_existingTagInFilteredList_showsAllPersons() {
        Person person = setTags(INDEX_SECOND_PERSON, "Volunteer");
        showPersonAtIndex(model, INDEX_SECOND_PERSON);
        assertTagAdded(INDEX_FIRST_PERSON, "VOLUNTEER", person, person);
    }

    @Test
    public void execute_invalidIndexUnfilteredList_throwsCommandException() {
        Index index = Index.fromOneBased(model.getFilteredPersonList().size() + 1);
        assertCommandFailure(new TagAddCommand(index, new Tag("Volunteer")), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void execute_invalidIndexFilteredList_throwsCommandException() {
        showPersonAtIndex(model, INDEX_SECOND_PERSON);
        assertCommandFailure(new TagAddCommand(INDEX_SECOND_PERSON, new Tag("Volunteer")), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void equals() {
        TagAddCommand command = new TagAddCommand(INDEX_FIRST_PERSON, new Tag("Volunteer"));
        assertTrue(command.equals(command));
        assertTrue(command.equals(new TagAddCommand(INDEX_FIRST_PERSON, new Tag("VOLUNTEER"))));
        assertFalse(command.equals(new TagAddCommand(INDEX_SECOND_PERSON, new Tag("Volunteer"))));
        assertFalse(command.equals(new TagAddCommand(INDEX_FIRST_PERSON, new Tag("Publicity"))));
        assertFalse(command.equals(null));
        assertFalse(command.equals(1));
    }

    @Test
    public void toStringMethod() {
        Tag tag = new Tag("Volunteer");
        TagAddCommand command = new TagAddCommand(INDEX_FIRST_PERSON, tag);
        String expected = TagAddCommand.class.getCanonicalName() + "{targetIndex=" + INDEX_FIRST_PERSON
                + ", tag=" + tag + "}";
        assertEquals(expected, command.toString());
    }

    private Person setTags(Index index, String... tags) {
        Person person = model.getFilteredPersonList().get(index.getZeroBased());
        Person updatedPerson = new PersonBuilder(person).withTags(tags).build();
        model.setPerson(person, updatedPerson);
        return updatedPerson;
    }

    private void assertTagAdded(Index index, String tagName, Person originalPerson, Person expectedPerson) {
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.setPerson(originalPerson, expectedPerson);
        Tag tag = new Tag(tagName);
        String expectedMessage = String.format(TagAddCommand.MESSAGE_SUCCESS, tag, Messages.format(expectedPerson));
        assertCommandSuccess(new TagAddCommand(index, tag), model, expectedMessage, expectedModel);
    }
}
