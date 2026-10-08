package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TAG;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.TagRemoveCommand;
import seedu.address.model.tag.Tag;

/**
 * Tests the single-index, single-tag syntax of {@code TagRemoveCommandParser}.
 */
public class TagRemoveCommandParserTest {

    private final TagRemoveCommandParser parser = new TagRemoveCommandParser();

    @Test
    public void parse_validArgs_returnsCommand() {
        assertParseSuccess(parser, "1 t/Volunteer", new TagRemoveCommand(INDEX_FIRST_PERSON, new Tag("volunteer")));
        assertParseSuccess(parser, "  1   t/ VOLUNTEER  ",
                new TagRemoveCommand(INDEX_FIRST_PERSON, new Tag("volunteer")));
    }

    @Test
    public void parse_invalidIndexOrMissingTag_throwsParseException() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, TagRemoveCommand.MESSAGE_USAGE);
        for (String args : new String[] {"", "1", "t/Volunteer", "0 t/Volunteer", "-1 t/Volunteer",
            "a t/Volunteer", "1 2 t/Volunteer", "1,2 t/Volunteer", "2147483648 t/Volunteer",
            "1 n/Alice t/Volunteer", "1t/Volunteer"}) {
            assertParseFailure(parser, args, expectedMessage);
        }
    }

    @Test
    public void parse_emptyOrInvalidTag_throwsParseException() {
        for (String args : new String[] {"1 t/", "1 t/   ", "1 t/Volunteer!", "1 t/Publicity Team",
            "1 t/Volunteer n/Alice", "1 t/Volunteer x/Other"}) {
            assertParseFailure(parser, args, Tag.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void parse_multipleTags_throwsParseException() {
        String expectedMessage = Messages.getErrorMessageForDuplicatePrefixes(PREFIX_TAG);
        assertParseFailure(parser, "1 t/Volunteer t/Exco", expectedMessage);
        assertParseFailure(parser, "1 t/Volunteer t/Volunteer", expectedMessage);
        assertParseFailure(parser, "1 t/Volunteer t/", expectedMessage);
    }
}
