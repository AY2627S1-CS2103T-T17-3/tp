package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.util.Locale;

import seedu.address.logic.commands.ViewCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.StatusFilter;

/** Parses the status view requested by a {@code view} command. */
public class ViewCommandParser implements Parser<ViewCommand> {

    @Override
    public ViewCommand parse(String args) throws ParseException {
        StatusFilter statusFilter = switch (args.trim().toLowerCase(Locale.ROOT)) {
            case "all" -> StatusFilter.ALL;
            case "active" -> StatusFilter.ACTIVE;
            case "archived" -> StatusFilter.ARCHIVED;
            default -> throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT,
                    ViewCommand.MESSAGE_USAGE));
        };
        return new ViewCommand(statusFilter);
    }
}
