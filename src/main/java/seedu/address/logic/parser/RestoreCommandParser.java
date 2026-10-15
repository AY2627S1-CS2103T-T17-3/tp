package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import seedu.address.logic.commands.RestoreCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses a single displayed index into a restore command.
 */
public class RestoreCommandParser implements Parser<RestoreCommand> {

    /**
     * Parses {@code args} into a {@code RestoreCommand}.
     *
     * @throws ParseException if the arguments are not one positive integer index.
     */
    public RestoreCommand parse(String args) throws ParseException {
        try {
            return new RestoreCommand(ParserUtil.parseIndex(args));
        } catch (ParseException pe) {
            throw new ParseException(
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, RestoreCommand.MESSAGE_USAGE), pe);
        }
    }
}
