package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_PERSONS_LISTED_OVERVIEW;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.address.logic.parser.FindAllCommandParser;
import seedu.address.logic.parser.FindCommandParser;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Person;
import seedu.address.model.person.Status;
import seedu.address.testutil.PersonBuilder;

/**
 * Tests findall parsing and execution against a member directory.
 */
public class FindAllCommandTest {
    private final Person johnTan = new PersonBuilder().withName("John Tan").withPhone("00123456")
            .withEmail("john.tan@u.nus.edu").withTags("exco", "publicity").build();
    private final Person johnLee = new PersonBuilder().withName("John Lee").withPhone("91234567")
            .withEmail("john.lee@u.nus.edu").withTags("exco").withStatus(Status.ARCHIVED).build();
    private final Person tanJohn = new PersonBuilder().withName("Tan John").withPhone("00129999")
            .withEmail("tan.john@u.nus.edu").withTags("exco", "publicity").build();
    private final Person incompleteMember = new PersonBuilder().withoutName().withoutPhone().withoutAddress()
            .withEmail("incomplete@u.nus.edu").withTags("exco", "publicity").build();
    private final Person alice = new PersonBuilder().withName("Alice Tan").withPhone("81230000")
            .withEmail("alice@u.nus.edu").withTags("publicity").build();
    private final List<Person> members = List.of(johnLee, johnTan, incompleteMember, alice, tanJohn);
    private final Model model = new ModelManager();

    @BeforeEach
    public void setUp() {
        members.forEach(model::addPerson);
    }

    @Test
    public void execute_repeatedNameConditions_matchesBothWordsInEitherOrder() throws ParseException {
        assertSearchResults("n/JOHN n/tAn", List.of(johnTan, tanJohn));
        assertSearchResults("n/tAn n/JOHN", List.of(johnTan, tanJohn));
    }

    @Test
    public void execute_repeatedPhoneAndEmailConditions_requiresEverySubstring() throws ParseException {
        assertSearchResults("p/001 p/2345", List.of(johnTan));
        assertSearchResults("e/JOHN e/.TAN", List.of(johnTan));
    }

    @Test
    public void execute_repeatedTagConditions_requiresBothTags() throws ParseException {
        assertSearchResults("t/EXCO t/Publicity", List.of(johnTan, incompleteMember, tanJohn));
    }

    @Test
    public void execute_mixedFields_matchesEveryField() throws ParseException {
        assertSearchResults("n/John p/001 e/JOHN t/PUBLICITY", List.of(johnTan, tanJohn));
        assertSearchResults("t/PUBLICITY e/JOHN p/001 n/John", List.of(johnTan, tanJohn));
    }

    @Test
    public void execute_singleCondition_matchesOrdinaryFind() throws ParseException {
        for (String condition : List.of("n/John", "p/001", "e/JOHN", "t/EXCO")) {
            new FindCommandParser().parse(condition).execute(model);
            List<Person> expectedMembers = List.copyOf(model.getFilteredPersonList());
            assertSearchResults(condition, expectedMembers);
        }
    }

    @Test
    public void execute_duplicateAndOverlappingConditions_preservesOrderWithoutDuplicates() throws ParseException {
        assertSearchResults("n/John n/John e/john t/exco t/EXCO", List.of(johnLee, johnTan, tanJohn));
    }

    @Test
    public void execute_noMatches_displaysEmptyListAndZeroCount() throws ParseException {
        for (String input : List.of("n/Nobody", "n/John n/Alice", "n/Joh t/exco", "t/pub e/@u.nus.edu",
                "p/001 p/023", "e/.* t/exco")) {
            assertSearchResults(input, List.of());
        }
    }

    @Test
    public void execute_afterEarlierFilter_searchesCompleteDirectory() throws ParseException {
        new FindCommandParser().parse("n/Alice").execute(model);
        assertEquals(List.of(alice), model.getFilteredPersonList());
        assertSearchResults("n/John t/exco", List.of(johnLee, johnTan, tanJohn));
        assertSearchResults("n/Nobody", List.of());
        assertSearchResults("e/incomplete t/exco", List.of(incompleteMember));
    }

    @Test
    public void execute_archivedAndIncompleteMembers_remainSearchable() throws ParseException {
        assertSearchResults("n/John t/exco", List.of(johnLee, johnTan, tanJohn));
        assertSearchResults("e/incomplete t/exco", List.of(incompleteMember));
        assertSearchResults("e/incomplete n/John", List.of());
        assertSearchResults("e/incomplete p/001", List.of());
    }

    @Test
    public void execute_emptyDirectory_displaysEmptyListAndZeroCount() throws ParseException {
        Model emptyModel = new ModelManager();
        CommandResult result = new FindAllCommandParser().parse("n/John t/exco").execute(emptyModel);
        assertEquals(String.format(MESSAGE_PERSONS_LISTED_OVERVIEW, 0), result.getFeedbackToUser());
        assertTrue(emptyModel.getFilteredPersonList().isEmpty());
    }

    /**
     * Verifies matching members, their order, the result count and preservation of directory records.
     */
    private void assertSearchResults(String input, List<Person> expectedMembers) throws ParseException {
        FindCommand command = new FindAllCommandParser().parse(input);
        assertTrue(command.isReadOnly());
        CommandResult result = command.execute(model);
        assertEquals(String.format(MESSAGE_PERSONS_LISTED_OVERVIEW, expectedMembers.size()),
                result.getFeedbackToUser());
        assertEquals(expectedMembers, model.getFilteredPersonList());
        assertEquals(members, model.getAddressBook().getPersonList());
    }
}
