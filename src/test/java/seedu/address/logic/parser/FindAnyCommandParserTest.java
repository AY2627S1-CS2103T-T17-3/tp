package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.FindCommand;
import seedu.address.model.person.AnyConditionsPredicate;
import seedu.address.model.person.EmailContainsSubstringPredicate;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.person.Person;
import seedu.address.model.person.PhoneContainsSubstringPredicate;
import seedu.address.model.person.TagMatchesPredicate;
import seedu.address.model.tag.Tag;

public class FindAnyCommandParserTest {
    private final FindAnyCommandParser parser = new FindAnyCommandParser();

    @Test
    public void parse_singleCondition_returnsFindCommand() {
        Map<String, Predicate<Person>> conditions = Map.of(
                "n/John", new NameContainsKeywordsPredicate(List.of("John")),
                "p/001", new PhoneContainsSubstringPredicate("001"),
                "e/ALICE", new EmailContainsSubstringPredicate("alice"),
                "e/+", new EmailContainsSubstringPredicate("+"),
                "t/EXCO", new TagMatchesPredicate(new Tag("exco")));
        conditions.forEach((input, predicate) -> assertConditions(input, List.of(predicate)));
    }

    @Test
    public void parse_repeatedPrefixes_retainsEveryCondition() {
        assertConditions("n/John n/Tan", List.of(new NameContainsKeywordsPredicate(List.of("John")),
                new NameContainsKeywordsPredicate(List.of("Tan"))));
        assertConditions("p/001 p/23", List.of(new PhoneContainsSubstringPredicate("001"),
                new PhoneContainsSubstringPredicate("23")));
        assertConditions("e/John e/@U.NUS.EDU", List.of(new EmailContainsSubstringPredicate("john"),
                new EmailContainsSubstringPredicate("@u.nus.edu")));
        assertConditions("t/EXCO t/publicity", List.of(new TagMatchesPredicate(new Tag("exco")),
                new TagMatchesPredicate(new Tag("publicity"))));
        assertConditions("n/John n/John", List.of(new NameContainsKeywordsPredicate(List.of("John")),
                new NameContainsKeywordsPredicate(List.of("John"))));
    }

    @Test
    public void parse_mixedFieldsAndWhitespace_returnsFindCommand() {
        List<Predicate<Person>> conditions = List.of(new NameContainsKeywordsPredicate(List.of("John")),
                new NameContainsKeywordsPredicate(List.of("Tan")), new PhoneContainsSubstringPredicate("001"),
                new EmailContainsSubstringPredicate("@u.nus.edu"), new TagMatchesPredicate(new Tag("exco")));
        for (String separator : List.of(" ", "  ", "\t", "\n", "\r\n", " \t\n ")) {
            assertConditions(" n/ John p/ 001 n/ Tan t/ EXCO e/ @U.NUS.EDU ".replace(" ", separator), conditions);
            assertConditions(" t/EXCO n/John e/@U.NUS.EDU n/Tan p/001 ".replace(" ", separator), conditions);
        }
    }

    @Test
    public void parse_missingConditionsOrUnprefixedText_throwsParseException() {
        for (String input : List.of("", " \t\r\n ", "John", "John Tan", "9123", "John t/exco", "John n/Tan")) {
            assertParseFailure(parser, input,
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE_ANY));
        }
    }

    @Test
    public void parse_emptyCondition_throwsParseException() {
        for (String prefix : List.of("n/", "p/", "e/", "t/")) {
            for (String input : List.of(prefix, prefix + " \t\n ", prefix + " n/John", "n/John " + prefix,
                    prefix + " " + prefix + "123", prefix + "123 " + prefix)) {
                assertParseFailure(parser, input,
                        String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE_ANY));
            }
        }
    }

    @Test
    public void parse_unicodeWhitespaceOnlyCondition_throwsParseException() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE_ANY);
        for (String value : List.of("\u2002", "\u2003", "\u3000", " \t\u2003 \n\u3000 ")) {
            for (String prefix : List.of("n/", "p/", "e/", "t/")) {
                assertParseFailure(parser, prefix + value, expectedMessage);
                assertParseFailure(parser, "n/John " + prefix + value, expectedMessage);
                assertParseFailure(parser, prefix + value + " n/John", expectedMessage);
            }
        }
    }

    @Test
    public void parse_multipleValuesInOneCondition_throwsParseException() {
        for (String prefix : List.of("n/", "p/", "e/", "t/")) {
            String expectedMessage = "Each " + prefix + " condition accepts only one value. "
                    + "Repeat the prefix for each additional condition, for example: findany n/John n/Tan.";
            for (String separator : List.of(" ", "  ", "\t", "\n", "\r\n")) {
                assertParseFailure(parser, prefix + "123" + separator + "456", expectedMessage);
                assertParseFailure(parser, prefix + "123" + separator + "123 n/John", expectedMessage);
            }
        }
    }

    @Test
    public void parse_invalidPhoneSubstring_throwsParseException() {
        for (String value : List.of("abc", "91a3", "+659123", "9123-4567", "9123.4567", "９１２３", "٩١٢٣")) {
            String expectedMessage = "Phone searches accept digits (0-9) only. Example: findany p/9123.";
            assertParseFailure(parser, "p/" + value, expectedMessage);
            assertParseFailure(parser, "p/" + value + " p/9123", expectedMessage);
            assertParseFailure(parser, "p/9123 p/" + value, expectedMessage);
            assertParseFailure(parser, "n/John p/" + value, expectedMessage);
        }
    }

    @Test
    public void parse_invalidTag_throwsParseException() {
        for (String value : List.of("exco!", "exco-2026", "exco_2026", "pub/exco", "pub,exco", "*", "ＰＵＢ")) {
            assertParseFailure(parser, "t/" + value, Tag.MESSAGE_CONSTRAINTS);
            assertParseFailure(parser, "t/" + value + " t/exco", Tag.MESSAGE_CONSTRAINTS);
            assertParseFailure(parser, "e/no-match t/exco t/" + value, Tag.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void parse_uppercasePrefix_throwsParseException() {
        for (String prefix : List.of("N/", "P/", "E/", "T/")) {
            for (String separator : List.of(" ", "\t", "\n", "\r\n")) {
                assertParseFailure(parser, prefix, FindCommand.MESSAGE_UPPERCASE_PREFIX);
                assertParseFailure(parser, "n/John" + separator + prefix + "value",
                        FindCommand.MESSAGE_UPPERCASE_PREFIX);
            }
        }
    }

    @Test
    public void parse_unsupportedPrefix_throwsParseException() {
        for (String prefix : List.of("G/", "g/", "a/", "s/", "name/", "tag/", "1/", "?/")) {
            String expectedMessage = "Unsupported search prefix: " + prefix + ". Use n/, p/, e/ or t/.";
            assertParseFailure(parser, prefix, expectedMessage);
            for (String separator : List.of(" ", "\t", "\n", "\r\n")) {
                assertParseFailure(parser, "n/John" + separator + prefix + "value", expectedMessage);
            }
        }
    }

    @Test
    public void parse_slashesInsideValues_returnsFindCommand() {
        assertConditions("n/AN/NA e/N/ e/name/alice", List.of(
                new NameContainsKeywordsPredicate(List.of("AN/NA")), new EmailContainsSubstringPredicate("N/"),
                new EmailContainsSubstringPredicate("name/alice")));
    }

    /**
     * Verifies that all expected conditions are included in the parsed command.
     */
    private void assertConditions(String input, List<Predicate<Person>> predicates) {
        assertParseSuccess(parser, input, new FindCommand(new AnyConditionsPredicate(predicates)));
    }
}
