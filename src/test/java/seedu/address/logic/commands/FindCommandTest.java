package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_PERSONS_LISTED_OVERVIEW;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.CARL;
import static seedu.address.testutil.TypicalPersons.DANIEL;
import static seedu.address.testutil.TypicalPersons.ELLE;
import static seedu.address.testutil.TypicalPersons.FIONA;
import static seedu.address.testutil.TypicalPersons.GEORGE;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;
import static seedu.address.testutil.TypicalPersons.getTypicalPersons;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.parser.FindCommandParser;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.EmailContainsSubstringPredicate;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.person.Person;
import seedu.address.model.person.PhoneContainsSubstringPredicate;
import seedu.address.model.person.Status;
import seedu.address.testutil.PersonBuilder;

/**
 * Contains integration tests (interaction with the Model) for {@code FindCommand}.
 */
public class FindCommandTest {
    private Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
    private Model expectedModel = new ModelManager(getTypicalAddressBook(), new UserPrefs());

    @Test
    public void equals() {
        NameContainsKeywordsPredicate firstPredicate =
                new NameContainsKeywordsPredicate(List.of("first"));
        NameContainsKeywordsPredicate secondPredicate =
                new NameContainsKeywordsPredicate(List.of("second"));

        FindCommand findFirstCommand = new FindCommand(firstPredicate);
        FindCommand findSecondCommand = new FindCommand(secondPredicate);

        // same object -> returns true
        assertTrue(findFirstCommand.equals(findFirstCommand));

        // same values -> returns true
        FindCommand findFirstCommandCopy = new FindCommand(firstPredicate);
        assertTrue(findFirstCommand.equals(findFirstCommandCopy));

        // different types -> returns false
        assertFalse(findFirstCommand.equals(1));

        // null -> returns false
        assertFalse(findFirstCommand.equals(null));

        // different person -> returns false
        assertFalse(findFirstCommand.equals(findSecondCommand));
    }

    @Test
    public void execute_zeroKeywords_noPersonFound() {
        String expectedMessage = String.format(MESSAGE_PERSONS_LISTED_OVERVIEW, 0);
        NameContainsKeywordsPredicate predicate = preparePredicate(" ");
        FindCommand command = new FindCommand(predicate);
        expectedModel.updateFilteredPersonList(predicate);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(), model.getFilteredPersonList());
    }

    @Test
    public void execute_multipleKeywords_multiplePersonsFound() {
        String expectedMessage = String.format(MESSAGE_PERSONS_LISTED_OVERVIEW, 3);
        NameContainsKeywordsPredicate predicate = preparePredicate("Kurz Elle Kunz");
        FindCommand command = new FindCommand(predicate);
        expectedModel.updateFilteredPersonList(predicate);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(CARL, ELLE, FIONA), model.getFilteredPersonList());
    }

    @Test
    public void execute_prefixedNameKeywords_multiplePersonsFound() throws ParseException {
        FindCommand command = new FindCommandParser().parse("n/kUrZ eLLe NoMatch");
        expectedModel.updateFilteredPersonList(preparePredicate("kUrZ eLLe NoMatch"));
        String expectedMessage = String.format(MESSAGE_PERSONS_LISTED_OVERVIEW, 2);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(CARL, ELLE), model.getFilteredPersonList());
    }

    @Test
    public void execute_prefixedPartialNameKeyword_noPersonFound() throws ParseException {
        FindCommand command = new FindCommandParser().parse("n/Kur");
        expectedModel.updateFilteredPersonList(preparePredicate("Kur"));
        String expectedMessage = String.format(MESSAGE_PERSONS_LISTED_OVERVIEW, 0);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(), model.getFilteredPersonList());
    }

    @Test
    public void execute_phoneSubstring_multiplePersonsFound() throws ParseException {
        FindCommand command = new FindCommandParser().parse("p/948");
        expectedModel.updateFilteredPersonList(new PhoneContainsSubstringPredicate("948"));
        String expectedMessage = String.format(MESSAGE_PERSONS_LISTED_OVERVIEW, 3);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(ELLE, FIONA, GEORGE), model.getFilteredPersonList());
    }

    @Test
    public void execute_fullPhoneNumber_onePersonFound() throws ParseException {
        FindCommand command = new FindCommandParser().parse("p/95352563");
        expectedModel.updateFilteredPersonList(new PhoneContainsSubstringPredicate("95352563"));
        String expectedMessage = String.format(MESSAGE_PERSONS_LISTED_OVERVIEW, 1);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(CARL), model.getFilteredPersonList());
    }

    @Test
    public void execute_nonMatchingPhoneSubstring_noPersonFound() throws ParseException {
        FindCommand command = new FindCommandParser().parse("p/0000");
        expectedModel.updateFilteredPersonList(new PhoneContainsSubstringPredicate("0000"));
        String expectedMessage = String.format(MESSAGE_PERSONS_LISTED_OVERVIEW, 0);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(), model.getFilteredPersonList());
    }

    @Test
    public void execute_emailSubstring_multiplePersonsFound() throws ParseException {
        FindCommand command = new FindCommandParser().parse("e/iA");
        expectedModel.updateFilteredPersonList(new EmailContainsSubstringPredicate("ia"));
        String expectedMessage = String.format(MESSAGE_PERSONS_LISTED_OVERVIEW, 2);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(DANIEL, FIONA), model.getFilteredPersonList());
    }

    @Test
    public void execute_fullEmailAddress_onePersonFound() throws ParseException {
        FindCommand command = new FindCommandParser().parse("e/HEINZ@U.NUS.EDU");
        expectedModel.updateFilteredPersonList(new EmailContainsSubstringPredicate("heinz@u.nus.edu"));
        String expectedMessage = String.format(MESSAGE_PERSONS_LISTED_OVERVIEW, 1);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(CARL), model.getFilteredPersonList());
    }

    @Test
    public void execute_emailDomain_allPersonsFound() throws ParseException {
        FindCommand command = new FindCommandParser().parse("e/@U.NUS.EDU");
        expectedModel.updateFilteredPersonList(new EmailContainsSubstringPredicate("@u.nus.edu"));
        String expectedMessage = String.format(MESSAGE_PERSONS_LISTED_OVERVIEW, getTypicalPersons().size());
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(getTypicalPersons(), model.getFilteredPersonList());
    }

    @Test
    public void execute_nonMatchingEmailSubstring_noPersonFound() throws ParseException {
        FindCommand command = new FindCommandParser().parse("e/no-such-email");
        expectedModel.updateFilteredPersonList(new EmailContainsSubstringPredicate("no-such-email"));
        String expectedMessage = String.format(MESSAGE_PERSONS_LISTED_OVERVIEW, 0);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(), model.getFilteredPersonList());
    }

    @Test
    public void execute_emailSearchAfterNameSearch_searchesAllPersons() throws ParseException {
        new FindCommandParser().parse("n/Elle").execute(model);
        assertEquals(List.of(ELLE), model.getFilteredPersonList());

        FindCommand command = new FindCommandParser().parse("e/heinz");
        expectedModel.updateFilteredPersonList(new EmailContainsSubstringPredicate("heinz"));
        String expectedMessage = String.format(MESSAGE_PERSONS_LISTED_OVERVIEW, 1);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(CARL), model.getFilteredPersonList());
        assertEquals(getTypicalAddressBook(), model.getAddressBook());
    }

    @Test
    public void execute_tagSearch_multiplePersonsFound() throws ParseException {
        FindCommand command = new FindCommandParser().parse("t/FRIENDS");
        expectedModel.updateFilteredPersonList(person -> List.of(ALICE, BENSON, DANIEL).contains(person));
        String expectedMessage = String.format(MESSAGE_PERSONS_LISTED_OVERVIEW, 3);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(ALICE, BENSON, DANIEL), model.getFilteredPersonList());
        assertEquals(getTypicalAddressBook(), model.getAddressBook());
        assertTrue(command.isReadOnly());
    }

    @Test
    public void execute_partialOrUnknownTag_noPersonFound() throws ParseException {
        for (String tag : List.of("friend", "money", "unknown")) {
            FindCommand command = new FindCommandParser().parse("t/" + tag);
            expectedModel.updateFilteredPersonList(person -> false);
            String expectedMessage = String.format(MESSAGE_PERSONS_LISTED_OVERVIEW, 0);
            assertCommandSuccess(command, model, expectedMessage, expectedModel);
            assertEquals(List.of(), model.getFilteredPersonList());
            assertEquals(getTypicalAddressBook(), model.getAddressBook());
        }
    }

    @Test
    public void execute_tagSearchAfterNameSearch_searchesAllPersons() throws ParseException {
        new FindCommandParser().parse("n/Elle").execute(model);
        assertEquals(List.of(ELLE), model.getFilteredPersonList());

        FindCommand command = new FindCommandParser().parse("t/OWESMONEY");
        expectedModel.updateFilteredPersonList(person -> person.equals(BENSON));
        String expectedMessage = String.format(MESSAGE_PERSONS_LISTED_OVERVIEW, 1);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(BENSON), model.getFilteredPersonList());
        assertEquals(getTypicalAddressBook(), model.getAddressBook());
    }

    @Test
    public void execute_tagSearch_incompleteAndArchivedMembersFound() throws ParseException {
        Person incompleteMember = new PersonBuilder().withoutName().withoutPhone().withoutAddress()
                .withEmail("incomplete@u.nus.edu").withTags("Publicity").build();
        Person archivedMember = new PersonBuilder().withEmail("archived@u.nus.edu")
                .withStatus(Status.ARCHIVED).withTags("publicity", "exco").build();
        model.addPerson(incompleteMember);
        model.addPerson(archivedMember);
        expectedModel.addPerson(incompleteMember);
        expectedModel.addPerson(archivedMember);
        AddressBook originalAddressBook = new AddressBook(model.getAddressBook());

        FindCommand command = new FindCommandParser().parse("t/PUBLICITY");
        expectedModel.updateFilteredPersonList(person -> List.of(incompleteMember, archivedMember).contains(person));
        String expectedMessage = String.format(MESSAGE_PERSONS_LISTED_OVERVIEW, 2);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(incompleteMember, archivedMember), model.getFilteredPersonList());
        assertEquals(originalAddressBook, model.getAddressBook());
    }

    @Test
    public void toStringMethod() {
        NameContainsKeywordsPredicate predicate = new NameContainsKeywordsPredicate(List.of("keyword"));
        FindCommand findCommand = new FindCommand(predicate);
        String expected = FindCommand.class.getCanonicalName() + "{predicate=" + predicate + "}";
        assertEquals(expected, findCommand.toString());
    }

    /**
     * Parses {@code userInput} into a {@code NameContainsKeywordsPredicate}.
     */
    private NameContainsKeywordsPredicate preparePredicate(String userInput) {
        return new NameContainsKeywordsPredicate(List.of(userInput.split("\\s+")));
    }
}
