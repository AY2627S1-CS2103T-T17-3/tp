package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.SearchParserUtil.SEARCH_PREFIXES;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.AllConditionsPredicate;
import seedu.address.model.person.Person;

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
        ArgumentMultimap argMultimap = SearchParserUtil.tokenize(args);
        String invalidFormatMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE_ALL);
        if (!argMultimap.getPreamble().isEmpty()) {
            throw new ParseException(invalidFormatMessage);
        }

        List<Predicate<Person>> predicates = new ArrayList<>();
        for (Prefix prefix : SEARCH_PREFIXES) {
            for (String value : argMultimap.getAllValues(prefix)) {
                if (value.isEmpty()) {
                    throw new ParseException(invalidFormatMessage);
                }
                predicates.add(SearchParserUtil.parseCondition(prefix, value,
                        String.format(FindCommand.MESSAGE_MULTIPLE_CONDITION_VALUES, prefix),
                        FindCommand.MESSAGE_INVALID_PHONE_SUBSTRING_ALL));
            }
        }
        if (predicates.isEmpty()) {
            throw new ParseException(invalidFormatMessage);
        }
        return new FindCommand(new AllConditionsPredicate(predicates));
    }
}
