package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_EMAIL;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_STATUS;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TAG;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.AllConditionsPredicate;
import seedu.address.model.person.EmailContainsSubstringPredicate;
import seedu.address.model.person.HasAllTagsPredicate;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.person.Person;
import seedu.address.model.person.PhoneContainsSubstringPredicate;
import seedu.address.model.person.Status;
import seedu.address.model.person.StatusMatchesPredicate;

/**
 * Parses name keywords or a combination of contact, tag and status conditions.
 */
public class FindCommandParser implements Parser<FindCommand> {
    private static final List<Prefix> SEARCH_PREFIXES =
            List.of(PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL, PREFIX_TAG, PREFIX_STATUS);
    private static final Pattern FIELD_PREFIX = Pattern.compile("(?<!\\S)([a-zA-Z][a-zA-Z0-9_-]*/)");

    /**
     * Parses all conditions before creating a command, so invalid queries never change the filter.
     *
     * @throws ParseException if any condition or field selection is invalid.
     */
    public FindCommand parse(String args) throws ParseException {
        String normalizedArgs = args.trim().replaceAll("\\s+", " ");
        Matcher matcher = FIELD_PREFIX.matcher(normalizedArgs);
        while (matcher.find()) {
            if (!SEARCH_PREFIXES.contains(new Prefix(matcher.group(1)))) {
                throw new ParseException(String.format(FindCommand.MESSAGE_UNSUPPORTED_PREFIX, matcher.group(1)));
            }
        }

        ArgumentMultimap fields = ArgumentTokenizer.tokenize(" " + normalizedArgs,
                SEARCH_PREFIXES.toArray(Prefix[]::new));
        fields.verifyNoDuplicatePrefixesFor(PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL, PREFIX_STATUS);
        boolean hasPrefixes = SEARCH_PREFIXES.stream().anyMatch(prefix -> fields.getValue(prefix).isPresent());
        if (!hasPrefixes) {
            if (normalizedArgs.isEmpty()) {
                throw invalidFormat();
            }
            return new FindCommand(new NameContainsKeywordsPredicate(List.of(normalizedArgs.split(" "))));
        }
        if (!fields.getPreamble().isEmpty()) {
            throw invalidFormat();
        }
        for (Prefix prefix : SEARCH_PREFIXES) {
            if (fields.getAllValues(prefix).stream().anyMatch(String::isEmpty)) {
                throw invalidFormat();
            }
        }

        List<Predicate<Person>> conditions = new ArrayList<>();
        if (fields.getValue(PREFIX_NAME).isPresent()) {
            conditions.add(new NameContainsKeywordsPredicate(List.of(fields.getValue(PREFIX_NAME).get().split(" "))));
        }
        if (fields.getValue(PREFIX_PHONE).isPresent()) {
            String phone = fields.getValue(PREFIX_PHONE).get();
            requireSingleSubstring(phone, FindCommand.MESSAGE_MULTIPLE_PHONE_SUBSTRINGS);
            conditions.add(new PhoneContainsSubstringPredicate(phone));
        }
        if (fields.getValue(PREFIX_EMAIL).isPresent()) {
            String email = fields.getValue(PREFIX_EMAIL).get();
            requireSingleSubstring(email, FindCommand.MESSAGE_MULTIPLE_EMAIL_SUBSTRINGS);
            conditions.add(new EmailContainsSubstringPredicate(email));
        }
        if (fields.getValue(PREFIX_TAG).isPresent()) {
            conditions.add(new HasAllTagsPredicate(ParserUtil.parseTags(fields.getAllValues(PREFIX_TAG))));
        }
        if (fields.getValue(PREFIX_STATUS).isPresent()) {
            String status = fields.getValue(PREFIX_STATUS).get().toLowerCase(Locale.ROOT);
            if (!Status.isValidStatus(status)) {
                throw new ParseException(FindCommand.MESSAGE_INVALID_STATUS);
            }
            conditions.add(new StatusMatchesPredicate(Status.valueOf(status.toUpperCase(Locale.ROOT))));
        }
        return new FindCommand(conditions.size() == 1 ? conditions.getFirst() : new AllConditionsPredicate(conditions));
    }

    private void requireSingleSubstring(String value, String message) throws ParseException {
        if (value.contains(" ")) {
            throw new ParseException(message);
        }
    }

    private ParseException invalidFormat() {
        return new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE));
    }
}
