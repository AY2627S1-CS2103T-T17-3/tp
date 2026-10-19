package seedu.address.logic.parser;

import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.AnyConditionsPredicate;

/**
 * Parses explicitly prefixed conditions into a search that requires at least one condition to match.
 */
public class FindAnyCommandParser implements Parser<FindCommand> {

    /**
     * Parses and validates every condition before creating a single FindCommand.
     *
     * @throws ParseException if there are no conditions, unprefixed text or an invalid condition.
     */
    public FindCommand parse(String args) throws ParseException {
        return new FindCommand(new AnyConditionsPredicate(SearchParserUtil.parseConditions(args,
                FindCommand.MESSAGE_USAGE_ANY, FindCommand.MESSAGE_MULTIPLE_CONDITION_VALUES_ANY,
                FindCommand.MESSAGE_INVALID_PHONE_SUBSTRING_ANY)));
    }
}
