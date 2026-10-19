package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_EMAIL;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TAG;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.FindCommand;
import seedu.address.model.person.EmailContainsSubstringPredicate;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.person.PhoneContainsSubstringPredicate;
import seedu.address.model.person.TagMatchesPredicate;
import seedu.address.model.tag.Tag;

public class FindCommandParserTest {

    private FindCommandParser parser = new FindCommandParser();

    @Test
    public void parse_unsupportedPrefix_throwsParseException() {
        for (String prefix : List.of("G/", "F/", "H/", "g/", "f/", "h/", "a/", "name/", "tag/", "1/", "?/")) {
            String expectedMessage = "Unsupported search prefix: " + prefix + ". Use n/, p/, e/ or t/.";
            assertParseFailure(parser, prefix, expectedMessage);
            assertParseFailure(parser, prefix + "value", expectedMessage);
            assertParseFailure(parser, " \t\n " + prefix + "value \t\n ", expectedMessage);
        }
    }

    @Test
    public void parse_unsupportedPrefixAfterOtherArguments_throwsParseException() {
        String expectedMessage = "Unsupported search prefix: G/. Use n/, p/, e/ or t/.";
        for (String args : List.of("Alice G/", "n/Alice G/value", "p/9123 G/value", "e/alice G/value",
                "t/publicity G/value", "G/value n/Alice", "G/ F/ H/")) {
            for (String separator : List.of(" ", "\t", "\n")) {
                assertParseFailure(parser, args.replace(" ", separator), expectedMessage);
            }
        }
    }

    @Test
    public void parse_unsupportedPrefixInsideValue_returnsFindCommand() {
        assertParseSuccess(parser, "e/G/", new FindCommand(new EmailContainsSubstringPredicate("G/")));
        assertParseSuccess(parser, "e/name/alice", new FindCommand(new EmailContainsSubstringPredicate("name/alice")));
        assertParseSuccess(parser, "n/AN/NA", new FindCommand(new NameContainsKeywordsPredicate(List.of("AN/NA"))));
    }

    @Test
    public void parse_uppercasePrefix_throwsParseException() {
        String expectedMessage = "Search prefixes must be lowercase. Use n/, p/, e/ or t/.";
        for (String args : List.of("N/", "P/", "E/", "T/", "N/Alice", "P/9123", "E/alice", "T/publicity")) {
            assertParseFailure(parser, args, expectedMessage);
            assertParseFailure(parser, " \t\n " + args + " \t\n ", expectedMessage);
        }
    }

    @Test
    public void parse_uppercasePrefixAfterOtherArguments_throwsParseException() {
        for (String args : List.of("T/ E/", "T/publicity E/alice", "n/Alice N/Bob", "p/9123 P/4567",
                "e/alice E/bob", "t/publicity T/exco", "Alice N/Bob", "Alice P/9123",
                "Alice E/alice", "Alice T/publicity")) {
            for (String separator : List.of(" ", "\t", "\n")) {
                assertParseFailure(parser, args.replace(" ", separator), FindCommand.MESSAGE_UPPERCASE_PREFIX);
            }
        }
    }

    @Test
    public void parse_lowercasePrefixWithUppercaseValue_returnsFindCommand() {
        assertParseSuccess(parser, "n/ALICE", new FindCommand(new NameContainsKeywordsPredicate(List.of("ALICE"))));
        assertParseSuccess(parser, "e/ALICE", new FindCommand(new EmailContainsSubstringPredicate("alice")));
        assertParseSuccess(parser, "t/PUBLICITY", new FindCommand(new TagMatchesPredicate(new Tag("publicity"))));
        assertParseSuccess(parser, "ALICE", new FindCommand(new NameContainsKeywordsPredicate(List.of("ALICE"))));
    }

    @Test
    public void parse_uppercasePrefixInsideValue_returnsFindCommand() {
        assertParseSuccess(parser, "e/N/", new FindCommand(new EmailContainsSubstringPredicate("N/")));
    }

    @Test
    public void parse_tagPrefix_returnsFindCommand() {
        FindCommand expectedFindCommand = new FindCommand(new TagMatchesPredicate(new Tag("publicity")));
        assertParseSuccess(parser, "t/publicity", expectedFindCommand);
        assertParseSuccess(parser, "t/PUBLICITY", expectedFindCommand);
        assertParseSuccess(parser, " \n \t t/ \t pUbLiCiTy \n ", expectedFindCommand);
        for (String tag : List.of("pub", "exco2026", "123", "a")) {
            assertParseSuccess(parser, "t/" + tag, new FindCommand(new TagMatchesPredicate(new Tag(tag))));
        }
    }

    @Test
    public void parse_emptyTagValue_throwsParseException() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE);
        assertParseFailure(parser, "t/", expectedMessage);
        assertParseFailure(parser, " t/ \t \n ", expectedMessage);
    }

    @Test
    public void parse_invalidTagName_throwsParseException() {
        for (String tag : List.of("publicity!", "exco-2026", "exco_2026", "pub/exco", "pub,exco", "*", "ＰＵＢ")) {
            assertParseFailure(parser, "t/" + tag, Tag.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void parse_multipleTags_throwsParseException() {
        String expectedMessage = "Tag searches accept only one tag. Example: find t/publicity.";
        for (String args : List.of("t/publicity exco", "t/publicity  exco", "t/publicity\texco",
                "t/publicity\nexco", " t/ \tpublicity \nexco \t", "t/publicity publicity")) {
            assertParseFailure(parser, args, expectedMessage);
        }
    }

    @Test
    public void parse_repeatedTagPrefix_throwsParseException() {
        String expectedMessage = Messages.getErrorMessageForDuplicatePrefixes(PREFIX_TAG);
        for (String args : List.of("t/publicity t/exco", "t/publicity t/publicity", "t/publicity\tt/exco",
                "t/publicity\nt/exco", "t/ t/exco", "t/publicity t/")) {
            assertParseFailure(parser, args, expectedMessage);
        }
    }

    @Test
    public void parse_tagAndOtherPrefixes_throwsParseException() {
        String expectedMessage = "Specify only one search field per find command.";
        for (String field : List.of("n/Alice", "p/9123", "e/alice", "n/", "p/", "e/")) {
            for (String separator : List.of(" ", "\t", "\n")) {
                assertParseFailure(parser, "t/publicity" + separator + field, expectedMessage);
                assertParseFailure(parser, field + separator + "t/publicity", expectedMessage);
            }
        }
        assertParseFailure(parser, "t/ n/Alice", expectedMessage);
        assertParseFailure(parser, "t/ p/9123", expectedMessage);
        assertParseFailure(parser, "t/ e/alice", expectedMessage);
    }

    @Test
    public void parse_tagPrefixWithPreamble_throwsParseException() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE);
        assertParseFailure(parser, "Alice t/publicity", expectedMessage);
        assertParseFailure(parser, "Alice\tt/publicity", expectedMessage);
        assertParseFailure(parser, "Alice\nt/publicity", expectedMessage);
    }

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
                new FindCommand(new NameContainsKeywordsPredicate(List.of("Alice")));
        assertParseSuccess(parser, "n/Alice", expectedFindCommand);
        assertParseSuccess(parser, " \r\n \t n/ \n Alice \t", expectedFindCommand);
    }

    @Test
    public void parse_multiplePrefixedNameKeywords_throwsParseException() {
        String expectedMessage = "Prefixed name searches accept only one keyword. "
                + "Use find Alice Bob to match names containing Alice OR Bob.";
        for (String separator : List.of(" ", "  ", "\t", "\n", "\r\n", " \t\n ")) {
            assertParseFailure(parser, "n/Alice" + separator + "Bob", expectedMessage);
            assertParseFailure(parser, "n/Alice" + separator + "Alice", expectedMessage);
            assertParseFailure(parser, " \t n/ Alice" + separator + "Bob \n", expectedMessage);
            assertParseSuccess(parser, "Alice" + separator + "Bob",
                    new FindCommand(new NameContainsKeywordsPredicate(List.of("Alice", "Bob"))));
        }
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
        assertParseFailure(parser, "n/Alice Bob n/Carol", expectedMessage);
    }

    @Test
    public void parse_namePrefixWithPreamble_throwsParseException() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE);
        assertParseFailure(parser, "Alice n/Bob", expectedMessage);
        assertParseFailure(parser, "Alice\tn/Bob", expectedMessage);
        assertParseFailure(parser, "Alice\nn/Bob Carol", expectedMessage);
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
        assertParseFailure(parser, "n/Alice Bob\np/9123", expectedMessage);
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
