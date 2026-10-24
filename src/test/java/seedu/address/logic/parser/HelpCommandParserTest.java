package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.HelpCommand;

/**
 * Tests the no-argument syntax of {@code HelpCommandParser}.
 */
public class HelpCommandParserTest {

    private final HelpCommandParser parser = new HelpCommandParser();

    @Test
    public void parse_emptyOrWhitespaceArgs_returnsHelpCommand() throws Exception {
        for (String args : List.of("", " ", "\t", "\n", "\r\n", " \t\r\n ")) {
            assertNotNull(parser.parse(args));
        }
    }

    @Test
    public void parse_nonemptyArgs_throwsParseException() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE);
        for (String args : List.of("1", " 1 ", "add", "add edit", "delete 0", "--guide", "\t1", "\"\"",
                "\nadd", "\u2003", "\u00a0")) {
            assertParseFailure(parser, args, expectedMessage);
        }
    }

    @Test
    public void parse_nullArgs_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
