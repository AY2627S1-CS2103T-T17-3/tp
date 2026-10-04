package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.VALID_REMARK_AMY;
import static seedu.address.logic.commands.CommandTestUtil.VALID_REMARK_BOB;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.logic.commands.CommandTestUtil.showPersonAtIndex;
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
import seedu.address.model.person.Remark;
import seedu.address.testutil.PersonBuilder;

/**
 * Tests the remark command against the person model.
 */
public class RemarkCommandTest {

    private final Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

    @Test
    public void execute_addRemark_success() {
        Person original = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        Person edited = new PersonBuilder(original).withRemark(VALID_REMARK_AMY).build();
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.setPerson(original, edited);

        String message = String.format(RemarkCommand.MESSAGE_ADD_REMARK_SUCCESS, Messages.format(edited));
        assertCommandSuccess(new RemarkCommand(INDEX_FIRST_PERSON, new Remark(VALID_REMARK_AMY)),
                model, message, expectedModel);
    }

    @Test
    public void execute_removeRemark_success() {
        Person original = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        Person withRemark = new PersonBuilder(original).withRemark(VALID_REMARK_AMY).build();
        model.setPerson(original, withRemark);
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.setPerson(withRemark, original);

        String message = String.format(RemarkCommand.MESSAGE_DELETE_REMARK_SUCCESS, Messages.format(original));
        assertCommandSuccess(new RemarkCommand(INDEX_FIRST_PERSON, new Remark("")), model, message, expectedModel);
    }

    @Test
    public void execute_validIndexFilteredList_success() {
        showPersonAtIndex(model, INDEX_SECOND_PERSON);
        Person original = model.getFilteredPersonList().get(0);
        Person edited = new PersonBuilder(original).withRemark(VALID_REMARK_BOB).build();
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.setPerson(original, edited);

        String message = String.format(RemarkCommand.MESSAGE_ADD_REMARK_SUCCESS, Messages.format(edited));
        assertCommandSuccess(new RemarkCommand(INDEX_FIRST_PERSON, new Remark(VALID_REMARK_BOB)),
                model, message, expectedModel);
    }

    @Test
    public void execute_invalidIndex_throwsCommandException() {
        Index invalidIndex = Index.fromOneBased(model.getFilteredPersonList().size() + 1);
        assertCommandFailure(new RemarkCommand(invalidIndex, new Remark(VALID_REMARK_AMY)), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void equals() {
        RemarkCommand command = new RemarkCommand(INDEX_FIRST_PERSON, new Remark(VALID_REMARK_AMY));
        assertTrue(command.equals(command));
        assertTrue(command.equals(new RemarkCommand(INDEX_FIRST_PERSON, new Remark(VALID_REMARK_AMY))));
        assertFalse(command.equals(null));
        assertFalse(command.equals(new ClearCommand()));
        assertFalse(command.equals(new RemarkCommand(INDEX_SECOND_PERSON, new Remark(VALID_REMARK_AMY))));
        assertFalse(command.equals(new RemarkCommand(INDEX_FIRST_PERSON, new Remark(VALID_REMARK_BOB))));
    }
}
