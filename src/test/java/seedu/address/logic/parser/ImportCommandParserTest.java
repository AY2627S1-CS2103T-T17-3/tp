package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.ImportCommand;
import seedu.address.logic.parser.exceptions.ParseException;

public class ImportCommandParserTest {
    private final ImportCommandParser parser = new ImportCommandParser();

    @Test
    public void parse_relativeAndQuotedPaths_returnsImportCommandWithoutReadingFile() {
        assertParseSuccess(parser, " members.csv ", new ImportCommand(Path.of("members.csv")));
        assertParseSuccess(parser, " \"member contacts.csv\" ", new ImportCommand(Path.of("member contacts.csv")));
        assertParseSuccess(parser, "\"folder/member contacts.csv\"",
                new ImportCommand(Path.of("folder/member contacts.csv")));
        assertParseSuccess(parser, "\"C:\\Members\\member contacts.csv\"",
                new ImportCommand(Path.of("C:\\Members\\member contacts.csv")));
    }

    @Test
    public void parse_missingUnquotedOrUnbalancedPath_throwsParseException() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, ImportCommand.MESSAGE_USAGE);
        for (String args : List.of("", " ", "\"\"", "\" \"", "members contacts.csv", "\"members.csv",
                "members.csv\"", "\"members.csv\" extra", "\"a.csv\" \"b.csv\"", "\"line\nbreak.csv\"")) {
            assertParseFailure(parser, args, expectedMessage);
        }
    }

    @Test
    public void parse_invalidPath_throwsParseException() {
        ParseException error = assertThrows(ParseException.class, () -> parser.parse("invalid\u0000.csv"));
        assertTrue(error.getMessage().startsWith("Invalid import file path:"));
    }
}
