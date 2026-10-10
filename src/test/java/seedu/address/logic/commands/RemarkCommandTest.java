package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.parser.RemarkCommandParser;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.model.person.Remark;
import seedu.address.model.person.Status;
import seedu.address.testutil.PersonBuilder;

public class RemarkCommandTest {

    @Test
    public void execute_addAndClearRemark_preservesArchivedPersonDetails() throws Exception {
        Person original = new PersonBuilder().withTags("Publicity", "Exco").build();
        Person archived = new Person(original.getName(), original.getPhone(), original.getEmail(),
                original.getAddress(), Status.ARCHIVED, original.getTags(), original.getRemark());
        Model model = new ModelManager(new AddressBook(), new UserPrefs());
        model.addPerson(archived);
        RemarkCommandParser parser = new RemarkCommandParser();

        CommandResult addResult = parser.parse("1 r/Likes to swim").execute(model);
        Person expected = new Person(archived.getName(), archived.getPhone(), archived.getEmail(),
                archived.getAddress(), Status.ARCHIVED, archived.getTags(), new Remark("Likes to swim"));
        assertEquals(expected, model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased()));
        assertEquals(String.format(RemarkCommand.MESSAGE_ADD_REMARK_SUCCESS, Messages.format(expected)),
                addResult.getFeedbackToUser());

        CommandResult clearResult = parser.parse("1 r/").execute(model);
        assertEquals(archived, model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased()));
        assertEquals(String.format(RemarkCommand.MESSAGE_DELETE_REMARK_SUCCESS, Messages.format(archived)),
                clearResult.getFeedbackToUser());
    }
}
