package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.ActivateCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses input arguments and creates an {@code ActivateCommand}.
 */
public class ActivateCommandParser implements Parser<ActivateCommand> {

    /**
     * Parses the displayed index of the member to activate.
     *
     * @throws ParseException if the input does not contain exactly one valid index.
     */
    public ActivateCommand parse(String args) throws ParseException {
        try {
            Index index = ParserUtil.parseIndex(args);
            return new ActivateCommand(index);
        } catch (ParseException pe) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, ActivateCommand.MESSAGE_USAGE), pe);
        }
    }
}
