package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_EMAIL;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TAG;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.EmailContainsSubstringPredicate;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.person.PhoneContainsSubstringPredicate;
import seedu.address.model.person.TagMatchesPredicate;

/**
 * Parses input arguments and creates a new FindCommand object.
 */
public class FindCommandParser implements Parser<FindCommand> {

    private static final Pattern SEARCH_PREFIX = Pattern.compile("(?:^|\\s)([^\\s/]+/)");
    private static final Set<String> SUPPORTED_SEARCH_PREFIXES = Set.of(
            PREFIX_NAME.getPrefix(), PREFIX_PHONE.getPrefix(), PREFIX_EMAIL.getPrefix(), PREFIX_TAG.getPrefix());

    /**
     * Parses name keywords, a {@code p/} phone substring, an {@code e/} email substring or a {@code t/} tag
     * into a FindCommand.
     *
     * @throws ParseException if the user input does not conform to the expected format.
     */
    public FindCommand parse(String args) throws ParseException {
        String trimmedArgs = args.trim();
        validateSearchPrefixes(trimmedArgs);
        // Normalize whitespace so prefixes after tabs and newlines are recognized by the tokenizer.
        ArgumentMultimap argMultimap =
                ArgumentTokenizer.tokenize(" " + trimmedArgs.replaceAll("\\s+", " "),
                        PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL, PREFIX_TAG);
        argMultimap.verifyNoDuplicatePrefixesFor(PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL, PREFIX_TAG);

        List<Prefix> searchPrefixes = List.of(PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL, PREFIX_TAG).stream()
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

        if (searchPrefixes.contains(PREFIX_PHONE)) {
            if (trimmedArgs.split("\\s+").length > 1) {
                throw new ParseException(FindCommand.MESSAGE_MULTIPLE_PHONE_SUBSTRINGS);
            }
            if (!trimmedArgs.matches("[0-9]+")) {
                throw new ParseException(FindCommand.MESSAGE_INVALID_PHONE_SUBSTRING);
            }
            return new FindCommand(new PhoneContainsSubstringPredicate(trimmedArgs));
        }

        if (searchPrefixes.contains(PREFIX_EMAIL)) {
            if (trimmedArgs.split("\\s+").length > 1) {
                throw new ParseException(FindCommand.MESSAGE_MULTIPLE_EMAIL_SUBSTRINGS);
            }
            return new FindCommand(new EmailContainsSubstringPredicate(trimmedArgs));
        }

        if (searchPrefixes.contains(PREFIX_TAG)) {
            if (trimmedArgs.split("\\s+").length > 1) {
                throw new ParseException(FindCommand.MESSAGE_MULTIPLE_TAGS);
            }
            return new FindCommand(new TagMatchesPredicate(ParserUtil.parseTag(trimmedArgs)));
        }

        String[] nameKeywords = trimmedArgs.split("\\s+");

        return new FindCommand(new NameContainsKeywordsPredicate(List.of(nameKeywords)));
    }

    /**
     * Rejects unsupported prefixes at argument boundaries before they can become search values.
     *
     * @throws ParseException if a prefix is uppercase or unsupported.
     */
    private void validateSearchPrefixes(String args) throws ParseException {
        Matcher matcher = SEARCH_PREFIX.matcher(args);
        while (matcher.find()) {
            String prefix = matcher.group(1);
            if (SUPPORTED_SEARCH_PREFIXES.contains(prefix)) {
                continue;
            }
            if (SUPPORTED_SEARCH_PREFIXES.contains(prefix.toLowerCase(Locale.ROOT))) {
                throw new ParseException(FindCommand.MESSAGE_UPPERCASE_PREFIX);
            }
            throw new ParseException(String.format(FindCommand.MESSAGE_UNSUPPORTED_PREFIX, prefix));
        }
    }

}
