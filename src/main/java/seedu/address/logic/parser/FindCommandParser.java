package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;

import java.util.List;

import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.person.PhoneContainsSubstringPredicate;

/**
 * Parses input arguments and creates a new FindCommand object.
 */
public class FindCommandParser implements Parser<FindCommand> {

    /**
     * Parses name keywords or a {@code p/} phone substring and returns a FindCommand for execution.
     *
     * @throws ParseException if the user input does not conform to the expected format.
     */
    public FindCommand parse(String args) throws ParseException {
        String trimmedArgs = args.trim();
        // Normalize whitespace so prefixes after tabs and newlines are recognized by the tokenizer.
        ArgumentMultimap argMultimap =
                ArgumentTokenizer.tokenize(" " + trimmedArgs.replaceAll("\\s+", " "), PREFIX_NAME, PREFIX_PHONE);
        argMultimap.verifyNoDuplicatePrefixesFor(PREFIX_NAME, PREFIX_PHONE);

        boolean hasNamePrefix = argMultimap.getValue(PREFIX_NAME).isPresent();
        boolean hasPhonePrefix = argMultimap.getValue(PREFIX_PHONE).isPresent();
        if (hasNamePrefix && hasPhonePrefix) {
            throw new ParseException(FindCommand.MESSAGE_MULTIPLE_FIELDS);
        }

        if (hasNamePrefix || hasPhonePrefix) {
            if (!argMultimap.getPreamble().isEmpty()) {
                throw new ParseException(
                        String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE));
            }
            Prefix searchPrefix = hasNamePrefix ? PREFIX_NAME : PREFIX_PHONE;
            trimmedArgs = argMultimap.getValue(searchPrefix).get();
        }

        if (trimmedArgs.isEmpty()) {
            throw new ParseException(
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE));
        }

        if (hasPhonePrefix) {
            if (trimmedArgs.split("\\s+").length > 1) {
                throw new ParseException(FindCommand.MESSAGE_MULTIPLE_PHONE_SUBSTRINGS);
            }
            return new FindCommand(new PhoneContainsSubstringPredicate(trimmedArgs));
        }

        String[] nameKeywords = trimmedArgs.split("\\s+");

        return new FindCommand(new NameContainsKeywordsPredicate(List.of(nameKeywords)));
    }

}
