package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_EMAIL;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.FindCommand;
import seedu.address.model.person.EmailContainsSubstringPredicate;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.person.PhoneContainsSubstringPredicate;

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

    @Test
    public void parse_phonePrefix_returnsFindCommand() {
        FindCommand expectedFindCommand = new FindCommand(new PhoneContainsSubstringPredicate("9123"));
        assertParseSuccess(parser, "p/9123", expectedFindCommand);
        assertParseSuccess(parser, " \n \t p/ \t 9123 \n ", expectedFindCommand);
        assertParseSuccess(parser, "p/91234567", new FindCommand(new PhoneContainsSubstringPredicate("91234567")));
        assertParseSuccess(parser, "p/9", new FindCommand(new PhoneContainsSubstringPredicate("9")));
        assertParseSuccess(parser, "p/91", new FindCommand(new PhoneContainsSubstringPredicate("91")));
        assertParseSuccess(parser, "p/0", new FindCommand(new PhoneContainsSubstringPredicate("0")));
        assertParseSuccess(parser, "p/001", new FindCommand(new PhoneContainsSubstringPredicate("001")));
        assertParseSuccess(parser, " \t p/ \t001 \n ",
                new FindCommand(new PhoneContainsSubstringPredicate("001")));
    }

    @Test
    public void parse_nonDigitPhoneSubstring_throwsParseException() {
        String expectedMessage = "Phone searches accept digits (0-9) only. Example: find p/9123.";
        for (String substring : List.of("abc", "91a3", "+659123", "9123-4567", "9123.4567", "９１２３", "٩١٢٣")) {
            assertParseFailure(parser, "p/" + substring, expectedMessage);
        }
        assertParseFailure(parser, " \t p/ \tabc \n ", expectedMessage);
    }

    @Test
    public void parse_unprefixedDigits_returnsNameFindCommand() {
        FindCommand expectedFindCommand = new FindCommand(new NameContainsKeywordsPredicate(List.of("9123")));
        assertParseSuccess(parser, "9123", expectedFindCommand);
    }

    @Test
    public void parse_emptyPhoneValue_throwsParseException() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE);
        assertParseFailure(parser, "p/", expectedMessage);
        assertParseFailure(parser, " p/ \t \n ", expectedMessage);
    }

    @Test
    public void parse_multiplePhoneSubstrings_throwsParseException() {
        String expectedMessage = "Phone searches accept only one substring. "
                + "Search each substring in a separate find command.";
        assertParseFailure(parser, "p/9123 1234", expectedMessage);
        assertParseFailure(parser, "p/9123  1234", expectedMessage);
        assertParseFailure(parser, "p/9123\t1234", expectedMessage);
        assertParseFailure(parser, "p/9123\n1234", expectedMessage);
        assertParseFailure(parser, " p/ \t9123 \n1234 \t", expectedMessage);
        assertParseFailure(parser, "p/abc 1234", expectedMessage);
        assertParseFailure(parser, "p/9123\tabc", expectedMessage);
        assertParseFailure(parser, "p/abc\n1234", expectedMessage);
    }

    @Test
    public void parse_repeatedPhonePrefix_throwsParseException() {
        String expectedMessage = Messages.getErrorMessageForDuplicatePrefixes(PREFIX_PHONE);
        assertParseFailure(parser, "p/9123 p/4567", expectedMessage);
        assertParseFailure(parser, "p/9123\tp/4567", expectedMessage);
        assertParseFailure(parser, "p/9123\np/4567", expectedMessage);
        assertParseFailure(parser, "p/ p/4567", expectedMessage);
        assertParseFailure(parser, "p/9123 p/", expectedMessage);
        assertParseFailure(parser, "p/abc p/4567", expectedMessage);
    }

    @Test
    public void parse_nameAndPhonePrefixes_throwsParseException() {
        String expectedMessage = "Specify only one search field per find command.";
        assertParseFailure(parser, "n/Alice p/9123", expectedMessage);
        assertParseFailure(parser, "p/9123 n/Alice", expectedMessage);
        assertParseFailure(parser, "n/Alice\tp/9123", expectedMessage);
        assertParseFailure(parser, "n/ p/9123", expectedMessage);
        assertParseFailure(parser, "n/Alice p/", expectedMessage);
        assertParseFailure(parser, "n/Alice p/abc", expectedMessage);
        assertParseFailure(parser, "p/abc e/alice", expectedMessage);
    }

    @Test
    public void parse_phonePrefixWithPreamble_throwsParseException() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE);
        assertParseFailure(parser, "Alice p/9123", expectedMessage);
        assertParseFailure(parser, "Alice\tp/9123", expectedMessage);
        assertParseFailure(parser, "Alice p/abc", expectedMessage);
    }

    @Test
    public void parse_emailPrefix_returnsFindCommand() {
        FindCommand expectedFindCommand = new FindCommand(new EmailContainsSubstringPredicate("alice"));
        assertParseSuccess(parser, "e/alice", expectedFindCommand);
        assertParseSuccess(parser, " \n \t e/ \t aLiCe \n ", expectedFindCommand);
        for (String substring : List.of("ALICE@U.NUS.EDU", "@u.nus.edu", "ice@u.", "+", ".", "a", "not-an-email")) {
            assertParseSuccess(parser, "e/" + substring,
                    new FindCommand(new EmailContainsSubstringPredicate(substring)));
        }
    }

    @Test
    public void parse_unprefixedEmail_returnsNameFindCommand() {
        FindCommand expectedFindCommand =
                new FindCommand(new NameContainsKeywordsPredicate(List.of("alice@u.nus.edu")));
        assertParseSuccess(parser, "alice@u.nus.edu", expectedFindCommand);
    }

    @Test
    public void parse_emptyEmailValue_throwsParseException() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE);
        assertParseFailure(parser, "e/", expectedMessage);
        assertParseFailure(parser, " e/ \t \n ", expectedMessage);
    }

    @Test
    public void parse_multipleEmailSubstrings_throwsParseException() {
        String expectedMessage = "Email searches accept only one substring. "
                + "Search each substring in a separate find command.";
        assertParseFailure(parser, "e/alice bob", expectedMessage);
        assertParseFailure(parser, "e/alice  bob", expectedMessage);
        assertParseFailure(parser, "e/alice\tbob", expectedMessage);
        assertParseFailure(parser, "e/alice\nbob", expectedMessage);
        assertParseFailure(parser, " e/ \talice \nbob \t", expectedMessage);
    }

    @Test
    public void parse_repeatedEmailPrefix_throwsParseException() {
        String expectedMessage = Messages.getErrorMessageForDuplicatePrefixes(PREFIX_EMAIL);
        assertParseFailure(parser, "e/alice e/bob", expectedMessage);
        assertParseFailure(parser, "e/alice e/alice", expectedMessage);
        assertParseFailure(parser, "e/alice\te/bob", expectedMessage);
        assertParseFailure(parser, "e/alice\ne/bob", expectedMessage);
        assertParseFailure(parser, "e/ e/bob", expectedMessage);
        assertParseFailure(parser, "e/alice e/", expectedMessage);
    }

    @Test
    public void parse_emailAndOtherPrefixes_throwsParseException() {
        String expectedMessage = "Specify only one search field per find command.";
        for (String args : List.of("e/alice n/Alice", "n/Alice e/alice", "e/alice p/9123", "p/9123 e/alice",
                "n/Alice p/9123 e/alice", "e/alice\tn/Alice", "p/9123\ne/alice", "e/ n/Alice", "e/alice n/",
                "e/ p/9123", "e/alice p/")) {
            assertParseFailure(parser, args, expectedMessage);
        }
    }

    @Test
    public void parse_emailPrefixWithPreamble_throwsParseException() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE);
        assertParseFailure(parser, "Alice e/alice", expectedMessage);
        assertParseFailure(parser, "Alice\te/alice", expectedMessage);
        assertParseFailure(parser, "Alice\ne/alice", expectedMessage);
    }

}
