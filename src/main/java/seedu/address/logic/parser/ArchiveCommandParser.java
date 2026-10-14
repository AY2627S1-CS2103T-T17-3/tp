package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.ArchiveCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses input arguments and creates an {@code ArchiveCommand}.
 */
public class ArchiveCommandParser implements Parser<ArchiveCommand> {

    /**
     * Parses the displayed index of the member to archive.
     *
     * @throws ParseException if the input does not contain exactly one valid index.
     */
    public ArchiveCommand parse(String args) throws ParseException {
        try {
            Index index = ParserUtil.parseIndex(args);
            return new ArchiveCommand(index);
        } catch (ParseException pe) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, ArchiveCommand.MESSAGE_USAGE), pe);
        }
    }
}
