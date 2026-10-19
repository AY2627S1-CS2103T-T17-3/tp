package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.ActivateCommand;
import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.ArchiveCommand;
import seedu.address.logic.commands.ClearCommand;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.commands.EditCommand;
import seedu.address.logic.commands.EditCommand.EditPersonDescriptor;
import seedu.address.logic.commands.ExitCommand;
import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.commands.ImportCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.TagAddCommand;
import seedu.address.logic.commands.TagRemoveCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.AllConditionsPredicate;
import seedu.address.model.person.AnyConditionsPredicate;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.person.Person;
import seedu.address.model.person.TagMatchesPredicate;
import seedu.address.model.tag.Tag;
import seedu.address.testutil.EditPersonDescriptorBuilder;
import seedu.address.testutil.PersonBuilder;
import seedu.address.testutil.PersonUtil;

public class AddressBookParserTest {

    private final AddressBookParser parser = new AddressBookParser();

    @Test
    public void parseCommand_import() throws Exception {
        assertEquals(new ImportCommand(Path.of("member contacts.csv")),
                parser.parseCommand("import \"member contacts.csv\""));
    }

    @Test
    public void parseCommand_add() throws Exception {
        Person person = new PersonBuilder().build();
        AddCommand command = (AddCommand) parser.parseCommand(PersonUtil.getAddCommand(person));
        assertEquals(new AddCommand(person), command);
    }

    @Test
    public void parseCommand_archiveAndActivate() throws Exception {
        assertEquals(new ArchiveCommand(INDEX_FIRST_PERSON), parser.parseCommand("archive 1"));
        assertEquals(new ActivateCommand(INDEX_FIRST_PERSON), parser.parseCommand("activate 1"));
    }

    @Test
    public void parseCommand_archiveAndActivateInvalidIndex_throwsParseException() {
        String archiveError = String.format(MESSAGE_INVALID_COMMAND_FORMAT, ArchiveCommand.MESSAGE_USAGE);
        String activateError = String.format(MESSAGE_INVALID_COMMAND_FORMAT, ActivateCommand.MESSAGE_USAGE);
        assertThrows(ParseException.class, archiveError, () -> parser.parseCommand("archive 0"));
        assertThrows(ParseException.class, activateError, () -> parser.parseCommand("activate 1 extra"));
    }

    @Test
    public void parseCommand_clear() throws Exception {
        assertTrue(parser.parseCommand(ClearCommand.COMMAND_WORD) instanceof ClearCommand);
        assertTrue(parser.parseCommand(ClearCommand.COMMAND_WORD + " 3") instanceof ClearCommand);
    }

    @Test
    public void parseCommand_delete() throws Exception {
        DeleteCommand command = (DeleteCommand) parser.parseCommand(
                DeleteCommand.COMMAND_WORD + " " + INDEX_FIRST_PERSON.getOneBased());
        assertEquals(new DeleteCommand(INDEX_FIRST_PERSON), command);
    }

    @Test
    public void parseCommand_edit() throws Exception {
        Person person = new PersonBuilder().build();
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder(person).build();
        EditCommand command = (EditCommand) parser.parseCommand(EditCommand.COMMAND_WORD + " "
                + INDEX_FIRST_PERSON.getOneBased() + " " + PersonUtil.getEditPersonDescriptorDetails(descriptor));
        assertEquals(new EditCommand(INDEX_FIRST_PERSON, descriptor), command);
    }

    @Test
    public void parseCommand_tagAdd() throws Exception {
        assertEquals(new TagAddCommand(INDEX_FIRST_PERSON, new Tag("Volunteer")),
                parser.parseCommand("tag 1 t/Volunteer"));
    }

    @Test
    public void parseCommand_tagRemove() throws Exception {
        assertEquals(new TagRemoveCommand(INDEX_FIRST_PERSON, new Tag("Publicity")),
                parser.parseCommand("untag 1 t/publicity"));
    }

    @Test
    public void parseCommand_exit() throws Exception {
        assertTrue(parser.parseCommand(ExitCommand.COMMAND_WORD) instanceof ExitCommand);
        assertTrue(parser.parseCommand(ExitCommand.COMMAND_WORD + " 3") instanceof ExitCommand);
    }

    @Test
    public void parseCommand_find() throws Exception {
        List<String> keywords = List.of("foo", "bar", "baz");
        FindCommand command = (FindCommand) parser.parseCommand(
                FindCommand.COMMAND_WORD + " " + keywords.stream().collect(Collectors.joining(" ")));
        assertEquals(new FindCommand(new NameContainsKeywordsPredicate(keywords)), command);
    }

    @Test
    public void parseCommand_findAllWithWhitespace_returnsFindCommand() throws Exception {
        FindCommand expectedCommand = new FindCommand(new AllConditionsPredicate(List.of(
                new NameContainsKeywordsPredicate(List.of("John")), new TagMatchesPredicate(new Tag("exco")))));
        for (String separator : List.of(" ", "\t", "\n", "\r\n")) {
            assertEquals(expectedCommand, parser.parseCommand("findall" + separator + "n/John"
                    + separator + "t/EXCO"));
        }
    }

    @Test
    public void parseCommand_invalidFindAll_throwsParseException() {
        String invalidFormatMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE_ALL);
        for (String input : List.of("findall", "findall John", "findall n/John\nt/")) {
            assertThrows(ParseException.class, invalidFormatMessage, () -> parser.parseCommand(input));
        }
        assertThrows(ParseException.class, FindCommand.MESSAGE_INVALID_PHONE_SUBSTRING_ALL, () ->
                parser.parseCommand("findall n/John\np/abc"));
        assertThrows(ParseException.class, String.format(FindCommand.MESSAGE_MULTIPLE_CONDITION_VALUES, "n/"), () ->
                parser.parseCommand("findall n/John\nTan"));
    }

    @Test
    public void parseCommand_findAnyWithWhitespace_returnsFindCommand() throws Exception {
        FindCommand expectedCommand = new FindCommand(new AnyConditionsPredicate(List.of(
                new NameContainsKeywordsPredicate(List.of("John")), new TagMatchesPredicate(new Tag("exco")))));
        for (String separator : List.of(" ", "\t", "\n", "\r\n")) {
            assertEquals(expectedCommand, parser.parseCommand("findany" + separator + "n/John"
                    + separator + "t/EXCO"));
        }
    }

    @Test
    public void parseCommand_invalidFindAny_throwsParseException() {
        String invalidFormatMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE_ANY);
        for (String input : List.of("findany", "findany John", "findany n/John\nt/")) {
            assertThrows(ParseException.class, invalidFormatMessage, () -> parser.parseCommand(input));
        }
        assertThrows(ParseException.class, FindCommand.MESSAGE_INVALID_PHONE_SUBSTRING_ANY, () ->
                parser.parseCommand("findany n/John\np/abc"));
        String multipleValuesMessage = String.format(FindCommand.MESSAGE_MULTIPLE_CONDITION_VALUES_ANY, "n/");
        assertThrows(ParseException.class, multipleValuesMessage, () -> parser.parseCommand("findany n/John\nTan"));
    }

    @Test
    public void parseCommand_nameSearchWithWhitespace_returnsFindCommand() throws Exception {
        for (String separator : List.of(" ", "\t", "\n", "\r\n")) {
            assertEquals(new FindCommand(new NameContainsKeywordsPredicate(List.of("Alice"))),
                    parser.parseCommand("find" + separator + "n/" + separator + "Alice" + separator));
            assertEquals(new FindCommand(new NameContainsKeywordsPredicate(List.of("Alice", "Bob"))),
                    parser.parseCommand("find Alice" + separator + "Bob"));
        }
    }

    @Test
    public void parseCommand_multiplePrefixedNameKeywords_throwsParseException() {
        for (String separator : List.of(" ", "\t", "\n", "\r\n")) {
            assertThrows(ParseException.class, FindCommand.MESSAGE_MULTIPLE_NAME_KEYWORDS, () ->
                    parser.parseCommand("find n/Alice" + separator + "Bob"));
        }
    }

    @Test
    public void parseCommand_multilineNonFindCommand_throwsParseException() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE);
        for (String command : List.of("add n/Alice\ne/alice@u.nus.edu", "clear\n1", "findOther n/Alice\nBob",
                "findn/Alice\nBob", "FIND n/Alice\nBob", "findallOther n/Alice\nt/exco",
                "findalln/Alice\nt/exco", "FINDALL n/Alice\nt/exco", "findanyOther n/Alice\nt/exco",
                "findanyn/Alice\nt/exco", "FINDANY n/Alice\nt/exco")) {
            assertThrows(ParseException.class, expectedMessage, () -> parser.parseCommand(command));
        }
    }

    @Test
    public void parseCommand_multiplePhoneSubstrings_throwsParseException() {
        for (String separator : List.of(" ", "\t", "\n", "\r\n")) {
            assertThrows(ParseException.class, FindCommand.MESSAGE_MULTIPLE_PHONE_SUBSTRINGS, () ->
                    parser.parseCommand("find p/9123" + separator + "1234"));
        }
    }

    @Test
    public void parseCommand_multipleEmailSubstrings_throwsParseException() {
        for (String separator : List.of(" ", "\t", "\n", "\r\n")) {
            assertThrows(ParseException.class, FindCommand.MESSAGE_MULTIPLE_EMAIL_SUBSTRINGS, () ->
                    parser.parseCommand("find e/alice" + separator + "bob"));
        }
    }

    @Test
    public void parseCommand_multipleTags_throwsParseException() {
        for (String separator : List.of(" ", "\t", "\n", "\r\n")) {
            assertThrows(ParseException.class, FindCommand.MESSAGE_MULTIPLE_TAGS, () ->
                    parser.parseCommand("find t/publicity" + separator + "exco"));
        }
    }

    @Test
    public void parseCommand_help() throws Exception {
        assertTrue(parser.parseCommand(HelpCommand.COMMAND_WORD) instanceof HelpCommand);
        assertTrue(parser.parseCommand(HelpCommand.COMMAND_WORD + " 3") instanceof HelpCommand);
    }

    @Test
    public void parseCommand_list() throws Exception {
        assertTrue(parser.parseCommand(ListCommand.COMMAND_WORD) instanceof ListCommand);
        assertTrue(parser.parseCommand(ListCommand.COMMAND_WORD + " 3") instanceof ListCommand);
    }

    @Test
    public void parseCommand_unrecognisedInput_throwsParseException() {
        assertThrows(ParseException.class, String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE), ()
            -> parser.parseCommand(""));
    }

    @Test
    public void parseCommand_unknownCommand_throwsParseException() {
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () -> parser.parseCommand("unknownCommand"));
    }
}
