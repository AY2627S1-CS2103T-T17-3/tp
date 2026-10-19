package seedu.address.logic.parser;

import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.AllConditionsPredicate;

/**
 * Parses explicitly prefixed conditions into a search that requires every condition to match.
 */
public class FindAllCommandParser implements Parser<FindCommand> {

    /**
     * Parses and validates every condition before creating a single FindCommand.
     *
     * @throws ParseException if there are no conditions, unprefixed text or an invalid condition.
     */
    public FindCommand parse(String args) throws ParseException {
        return new FindCommand(new AllConditionsPredicate(SearchParserUtil.parseConditions(args,
                FindCommand.MESSAGE_USAGE_ALL, FindCommand.MESSAGE_MULTIPLE_CONDITION_VALUES,
                FindCommand.MESSAGE_INVALID_PHONE_SUBSTRING_ALL)));
    }
}
