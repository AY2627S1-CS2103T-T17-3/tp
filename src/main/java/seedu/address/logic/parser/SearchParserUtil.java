package seedu.address.logic.parser;

import static seedu.address.logic.parser.CliSyntax.PREFIX_EMAIL;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TAG;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.EmailContainsSubstringPredicate;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.person.Person;
import seedu.address.model.person.PhoneContainsSubstringPredicate;
import seedu.address.model.person.TagMatchesPredicate;

/**
 * Parses the field conditions shared by member search commands.
 */
final class SearchParserUtil {
    static final List<Prefix> SEARCH_PREFIXES = List.of(PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL, PREFIX_TAG);

    private static final Pattern SEARCH_PREFIX = Pattern.compile("(?:^|\\s)([^\\s/]+/)");

    private SearchParserUtil() {
    }

    /**
     * Validates search prefixes and tokenizes arguments, treating spaces, tabs and line breaks consistently.
     *
     * @throws ParseException if an argument-boundary prefix is uppercase or unsupported.
     */
    static ArgumentMultimap tokenize(String args) throws ParseException {
        String trimmedArgs = args.trim();
        Matcher matcher = SEARCH_PREFIX.matcher(trimmedArgs);
        while (matcher.find()) {
            String prefix = matcher.group(1);
            if (SEARCH_PREFIXES.contains(new Prefix(prefix))) {
                continue;
            }
            if (SEARCH_PREFIXES.contains(new Prefix(prefix.toLowerCase(Locale.ROOT)))) {
                throw new ParseException(FindCommand.MESSAGE_UPPERCASE_PREFIX);
            }
            throw new ParseException(String.format(FindCommand.MESSAGE_UNSUPPORTED_PREFIX, prefix));
        }
        return ArgumentTokenizer.tokenize(" " + trimmedArgs.replaceAll("\\s+", " "),
                SEARCH_PREFIXES.toArray(Prefix[]::new));
    }

    /**
     * Validates a nonempty field value and returns its search predicate.
     *
     * @param prefix The supported field prefix.
     * @param value The trimmed, nonempty condition value.
     * @param multipleValuesMessage The command's guidance for multiple values in one condition.
     * @param invalidPhoneMessage The command's guidance for a phone value containing non-digits.
     * @throws ParseException if the value does not satisfy its field's search rules.
     */
    static Predicate<Person> parseCondition(Prefix prefix, String value, String multipleValuesMessage,
            String invalidPhoneMessage) throws ParseException {
        if (value.split("\\s+").length > 1) {
            throw new ParseException(multipleValuesMessage);
        }
        if (prefix.equals(PREFIX_NAME)) {
            return new NameContainsKeywordsPredicate(List.of(value));
        }
        if (prefix.equals(PREFIX_PHONE)) {
            if (!value.matches("[0-9]+")) {
                throw new ParseException(invalidPhoneMessage);
            }
            return new PhoneContainsSubstringPredicate(value);
        }
        if (prefix.equals(PREFIX_EMAIL)) {
            return new EmailContainsSubstringPredicate(value);
        }
        if (prefix.equals(PREFIX_TAG)) {
            return new TagMatchesPredicate(ParserUtil.parseTag(value));
        }
        throw new IllegalArgumentException("Unsupported search prefix: " + prefix);
    }
}
