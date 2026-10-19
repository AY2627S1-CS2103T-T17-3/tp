package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_PERSONS_LISTED_OVERVIEW;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.address.logic.parser.FindAllCommandParser;
import seedu.address.logic.parser.FindAnyCommandParser;
import seedu.address.logic.parser.FindCommandParser;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Person;
import seedu.address.model.person.Status;
import seedu.address.testutil.PersonBuilder;

/**
 * Tests findany parsing and execution against a member directory.
 */
public class FindAnyCommandTest {
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
    private final Person bob = new PersonBuilder().withName("Bob Lee").withPhone("88888888")
            .withEmail("bob@u.nus.edu").withTags().build();
    private final List<Person> members = List.of(johnLee, johnTan, incompleteMember, alice, tanJohn, bob);
    private final Model model = new ModelManager();

    @BeforeEach
    public void setUp() {
        members.forEach(model::addPerson);
    }

    @Test
    public void execute_repeatedNameConditions_matchesEitherWord() throws ParseException {
        assertSearchResults("n/JOHN n/tAn", List.of(johnLee, johnTan, alice, tanJohn));
        assertSearchResults("n/tAn n/JOHN", List.of(johnLee, johnTan, alice, tanJohn));
    }

    @Test
    public void execute_repeatedPhoneAndEmailConditions_matchesEitherSubstring() throws ParseException {
        assertSearchResults("p/001 p/9123", List.of(johnLee, johnTan, tanJohn));
        assertSearchResults("e/JOHN e/ALICE", List.of(johnLee, johnTan, alice, tanJohn));
    }

    @Test
    public void execute_repeatedTagConditions_matchesEitherTag() throws ParseException {
        assertSearchResults("t/EXCO t/Publicity", List.of(johnLee, johnTan, incompleteMember, alice, tanJohn));
    }

    @Test
    public void execute_mixedFields_matchesAnyField() throws ParseException {
        assertSearchResults("n/Alice e/john.lee", List.of(johnLee, alice));
        assertSearchResults("n/Alice p/001 e/INCOMPLETE t/EXCO",
                List.of(johnLee, johnTan, incompleteMember, alice, tanJohn));
        assertSearchResults("t/EXCO e/INCOMPLETE p/001 n/Alice",
                List.of(johnLee, johnTan, incompleteMember, alice, tanJohn));
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
        assertSearchResults("n/John n/John e/john t/exco t/EXCO", List.of(johnLee, johnTan, incompleteMember, tanJohn));
        assertSearchResults("p/001 p/001", List.of(johnTan, tanJohn));
    }

    @Test
    public void execute_noMatches_displaysEmptyListAndZeroCount() throws ParseException {
        for (String input : List.of("n/Nobody", "n/Nobody p/7777 e/no-such-email t/unknown",
                "n/Joh t/pub e/.* p/023")) {
            assertSearchResults(input, List.of());
        }
    }

    @Test
    public void execute_afterEarlierFilter_searchesCompleteDirectory() throws ParseException {
        new FindCommandParser().parse("n/Alice").execute(model);
        assertEquals(List.of(alice), model.getFilteredPersonList());
        assertSearchResults("n/John e/incomplete", List.of(johnLee, johnTan, incompleteMember, tanJohn));
        new FindAllCommandParser().parse("n/Nobody t/exco").execute(model);
        assertEquals(List.of(), model.getFilteredPersonList());
        assertSearchResults("p/001", List.of(johnTan, tanJohn));
    }

    @Test
    public void execute_archivedAndIncompleteMembers_remainSearchable() throws ParseException {
        assertSearchResults("n/Nobody e/john.lee", List.of(johnLee));
        assertSearchResults("n/Nobody p/7777 e/incomplete", List.of(incompleteMember));
        assertSearchResults("n/John e/incomplete", List.of(johnLee, johnTan, incompleteMember, tanJohn));
        assertSearchResults("p/001 e/incomplete", List.of(johnTan, incompleteMember, tanJohn));
    }

    @Test
    public void execute_emptyDirectory_displaysEmptyListAndZeroCount() throws ParseException {
        Model emptyModel = new ModelManager();
        CommandResult result = new FindAnyCommandParser().parse("n/John t/exco").execute(emptyModel);
        assertEquals(String.format(MESSAGE_PERSONS_LISTED_OVERVIEW, 0), result.getFeedbackToUser());
        assertTrue(emptyModel.getFilteredPersonList().isEmpty());
    }

    /**
     * Verifies matching members, their order, the result count and preservation of directory records.
     */
    private void assertSearchResults(String input, List<Person> expectedMembers) throws ParseException {
        FindCommand command = new FindAnyCommandParser().parse(input);
        assertTrue(command.isReadOnly());
        CommandResult result = command.execute(model);
        assertEquals(String.format(MESSAGE_PERSONS_LISTED_OVERVIEW, expectedMembers.size()),
                result.getFeedbackToUser());
        assertEquals(expectedMembers, model.getFilteredPersonList());
        assertEquals(members, model.getAddressBook().getPersonList());
    }
}
