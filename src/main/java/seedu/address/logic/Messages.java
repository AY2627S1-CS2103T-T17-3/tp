package seedu.address.logic;

import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import seedu.address.logic.parser.Prefix;
import seedu.address.model.person.Address;
import seedu.address.model.person.Person;

/**
 * Container for user visible messages.
 */
public class Messages {

    public static final String MESSAGE_DUPLICATE_PERSON =
            "Email \"%1$s\" is already used by \"%2$s\". No changes were made.";
    public static final String MESSAGE_DUPLICATE_UNNAMED_PERSON =
            "Email \"%1$s\" is already used by an existing member. No changes were made.";
    public static final String NOT_PROVIDED = "Not provided";

    public static final String MESSAGE_UNKNOWN_COMMAND = "Unknown command.";
    public static final String MESSAGE_INVALID_COMMAND_FORMAT = "Invalid command format!\n%1$s";
    public static final String MESSAGE_INVALID_PERSON_DISPLAYED_INDEX = "The person index provided is invalid.";
    public static final String MESSAGE_PERSONS_LISTED_OVERVIEW = "%1$d person(s) listed!";
    public static final String MESSAGE_DUPLICATE_FIELDS =
                "Multiple values specified for the following single-valued field(s): ";

    private static final String NAME_PREFIX = "Name: ";
    private static final String PHONE_PREFIX = "Phone: ";
    private static final String EMAIL_PREFIX = "Email: ";
    private static final String ADDRESS_PREFIX = "Address: ";
    private static final String TAGS_PREFIX = "Tags: ";

    /**
     * Formats a duplicate error identifying the existing person without a displayed index.
     */
    public static String formatDuplicatePerson(Person person) {
        return person.getName()
                .map(name -> String.format(MESSAGE_DUPLICATE_PERSON, person.getEmail(), name))
                .orElseGet(() -> String.format(MESSAGE_DUPLICATE_UNNAMED_PERSON, person.getEmail()));
    }

    /**
     * Returns an error message indicating the duplicate prefixes.
     */
    public static String getErrorMessageForDuplicatePrefixes(Prefix... duplicatePrefixes) {
        assert duplicatePrefixes.length > 0;

        Set<String> duplicateFields =
                Stream.of(duplicatePrefixes).map(Prefix::toString).collect(Collectors.toSet());

        return MESSAGE_DUPLICATE_FIELDS + String.join(" ", duplicateFields);
    }

    /**
     * Formats the {@code person} for display to the user.
     */
    public static String format(Person person) {
        final StringBuilder builder = new StringBuilder();
        builder.append(person.getName().map(name -> NAME_PREFIX + name.fullName).orElse(NOT_PROVIDED))
                .append("; " + PHONE_PREFIX)
                .append(person.getPhone().map(phone -> phone.value).orElse(NOT_PROVIDED))
                .append("; " + EMAIL_PREFIX)
                .append(person.getEmail())
                .append("; " + ADDRESS_PREFIX)
                .append(person.getAddress().map(address -> address.value).orElse(NOT_PROVIDED))
                .append("; " + TAGS_PREFIX);
        person.getTags().forEach(builder::append);
        return builder.toString();
    }

    /**
     * Formats only the supplied contact details and nonempty tags for command confirmations.
     */
    public static String formatProvidedDetails(Person person) {
        StringJoiner details = new StringJoiner("; ");
        person.getName().ifPresent(name -> details.add(NAME_PREFIX + name.fullName));
        person.getPhone().ifPresent(phone -> details.add(PHONE_PREFIX + phone.value));
        details.add(EMAIL_PREFIX + person.getEmail());
        person.getAddress().ifPresent(address -> details.add(ADDRESS_PREFIX + address.value));
        if (!person.getTags().isEmpty()) {
            details.add(TAGS_PREFIX + person.getTags().stream().map(Object::toString).collect(Collectors.joining()));
        }
        return details.toString();
    }

}
