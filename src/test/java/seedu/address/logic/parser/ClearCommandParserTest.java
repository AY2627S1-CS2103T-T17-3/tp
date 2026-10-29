package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.ClearCommand;

/**
 * Tests the no-argument syntax of {@code ClearCommandParser}.
 */
public class ClearCommandParserTest {
    private final ClearCommandParser parser = new ClearCommandParser();

    @Test
    public void parse_emptyOrWhitespaceArgs_returnsClearCommand() throws Exception {
        for (String args : List.of("", " ", "\t", "\n", "\r\n", " \t\r\n ")) {
            assertNotNull(parser.parse(args));
        }
    }

    @Test
    public void parse_nonemptyArgs_throwsParseException() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, ClearCommand.MESSAGE_USAGE);
        for (String args : List.of("1", " 1 ", "all", "member", "extra_text", "n/Alex", "--force", "\"\"",
                "\t1", "\n1", "\u2003", "\u00a0")) {
            assertParseFailure(parser, args, expectedMessage);
        }
    }

    @Test
    public void parse_nullArgs_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
