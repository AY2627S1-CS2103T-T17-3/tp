package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.FindCommand;
import seedu.address.model.person.NameContainsKeywordsPredicate;

public class FindCommandParserTest {

    private FindCommandParser parser = new FindCommandParser();

    @Test
    public void parse_emptyArg_throwsParseException() {
        assertParseFailure(parser, "     ", String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE));
    }

    @Test
    public void parse_validArgs_returnsFindCommand() {
        // no leading and trailing whitespaces
        FindCommand expectedFindCommand =
                new FindCommand(new NameContainsKeywordsPredicate(List.of("Alice", "Bob")));
        assertParseSuccess(parser, "Alice Bob", expectedFindCommand);

        // multiple whitespaces between keywords
        assertParseSuccess(parser, " \n Alice \n \t Bob  \t", expectedFindCommand);
    }

    @Test
    public void parse_namePrefix_returnsFindCommand() {
        FindCommand expectedFindCommand =
                new FindCommand(new NameContainsKeywordsPredicate(List.of("Alice", "Bob")));
        assertParseSuccess(parser, "n/Alice Bob", expectedFindCommand);
        assertParseSuccess(parser, " \n \t n/ \n Alice \t Bob \t", expectedFindCommand);
    }

    @Test
    public void parse_emptyNameValue_throwsParseException() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE);
        assertParseFailure(parser, "n/", expectedMessage);
        assertParseFailure(parser, " n/ \t \n ", expectedMessage);
    }

    @Test
    public void parse_repeatedNamePrefix_throwsParseException() {
        String expectedMessage = Messages.getErrorMessageForDuplicatePrefixes(PREFIX_NAME);
        assertParseFailure(parser, "n/Alice n/Bob", expectedMessage);
        assertParseFailure(parser, "n/Alice n/Alice", expectedMessage);
        assertParseFailure(parser, "n/Alice\tn/Bob", expectedMessage);
        assertParseFailure(parser, "n/Alice\nn/Bob", expectedMessage);
        assertParseFailure(parser, "n/ n/Bob", expectedMessage);
        assertParseFailure(parser, "n/Alice n/", expectedMessage);
    }

    @Test
    public void parse_namePrefixWithPreamble_throwsParseException() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE);
        assertParseFailure(parser, "Alice n/Bob", expectedMessage);
        assertParseFailure(parser, "Alice\tn/Bob", expectedMessage);
    }

}
