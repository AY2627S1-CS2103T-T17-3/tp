package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import seedu.address.logic.commands.ImportCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses one file path without opening the file or interpreting backslashes as escapes.
 */
public class ImportCommandParser implements Parser<ImportCommand> {
    @Override
    public ImportCommand parse(String args) throws ParseException {
        requireNonNull(args);
        String path = args.trim();
        if (path.startsWith("\"") && path.endsWith("\"") && path.length() >= 2) {
            path = path.substring(1, path.length() - 1);
        } else if (path.matches(".*\\s+.*")) {
            throw invalidFormat();
        }
        if (path.isBlank() || path.contains("\"") || path.contains("\n") || path.contains("\r")) {
            throw invalidFormat();
        }
        try {
            return new ImportCommand(Path.of(path));
        } catch (InvalidPathException e) {
            throw new ParseException("Invalid import file path: " + e.getReason(), e);
        }
    }

    private ParseException invalidFormat() {
        return new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, ImportCommand.MESSAGE_USAGE));
    }
}
