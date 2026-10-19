package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_EMAIL;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TAG;
import static seedu.address.logic.parser.SearchParserUtil.SEARCH_PREFIXES;

import java.util.List;
import java.util.Map;

import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.NameContainsKeywordsPredicate;

/**
 * Parses input arguments and creates a new FindCommand object.
 */
public class FindCommandParser implements Parser<FindCommand> {

    private static final Map<Prefix, String> MULTIPLE_VALUE_MESSAGES = Map.of(
            PREFIX_NAME, FindCommand.MESSAGE_MULTIPLE_NAME_KEYWORDS,
            PREFIX_PHONE, FindCommand.MESSAGE_MULTIPLE_PHONE_SUBSTRINGS,
            PREFIX_EMAIL, FindCommand.MESSAGE_MULTIPLE_EMAIL_SUBSTRINGS,
            PREFIX_TAG, FindCommand.MESSAGE_MULTIPLE_TAGS);

    /**
     * Parses name keywords, a {@code p/} phone substring, an {@code e/} email substring or a {@code t/} tag
     * into a FindCommand.
     *
     * @throws ParseException if the user input does not conform to the expected format.
     */
    public FindCommand parse(String args) throws ParseException {
        String trimmedArgs = args.trim();
        ArgumentMultimap argMultimap = SearchParserUtil.tokenize(args);
        argMultimap.verifyNoDuplicatePrefixesFor(SEARCH_PREFIXES.toArray(Prefix[]::new));

        List<Prefix> searchPrefixes = SEARCH_PREFIXES.stream()
                .filter(prefix -> argMultimap.getValue(prefix).isPresent())
                .toList();
        if (searchPrefixes.size() > 1) {
            throw new ParseException(FindCommand.MESSAGE_MULTIPLE_FIELDS);
        }

        if (!searchPrefixes.isEmpty()) {
            if (!argMultimap.getPreamble().isEmpty()) {
                throw new ParseException(
                        String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE));
            }
            Prefix searchPrefix = searchPrefixes.getFirst();
            trimmedArgs = argMultimap.getValue(searchPrefix).get();
        }

        if (trimmedArgs.isEmpty()) {
            throw new ParseException(
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE));
        }

        if (!searchPrefixes.isEmpty()) {
            Prefix prefix = searchPrefixes.getFirst();
            return new FindCommand(SearchParserUtil.parseCondition(prefix, trimmedArgs,
                    MULTIPLE_VALUE_MESSAGES.get(prefix), FindCommand.MESSAGE_INVALID_PHONE_SUBSTRING));
        }

        return new FindCommand(new NameContainsKeywordsPredicate(List.of(trimmedArgs.split("\\s+"))));
    }

}
