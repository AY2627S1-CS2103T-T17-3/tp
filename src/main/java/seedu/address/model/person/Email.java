package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.util.Locale;
import java.util.Optional;

/**
 * Represents a Person's canonical email in the address book.
 * Stores lowercase using {@link Locale#ROOT} without trimming whitespace.
 * Guarantees: immutable; is valid as declared in {@link #isValidEmail(String)}
 */
public class Email {

    private static final int MAX_LOCAL_PART_LENGTH = 64;

    public static final String MESSAGE_CONSTRAINTS = "Emails must have the format local-part@u.nus.edu.\n"
            + "The local-part must contain letters or digits, optionally separated by single "
            + "+, _, ., or - characters.\n"
            + "It must not start or end with punctuation or contain consecutive punctuation.\n"
            + "Email addresses are case-insensitive and are stored in lowercase.\n"
            + "The local-part must contain at most " + MAX_LOCAL_PART_LENGTH + " characters.";

    private static final String MESSAGE_LOCAL_PART_TOO_LONG =
            "Email local-part must contain at most %1$d characters; received %2$d.";
    // alphanumeric and special characters
    private static final String SPECIAL_CHARACTERS = "+_.-";
    private static final String ALPHANUMERIC_NO_UNDERSCORE = "[^\\W_]+"; // alphanumeric characters except underscore
    private static final String LOCAL_PART_REGEX = "^" + ALPHANUMERIC_NO_UNDERSCORE + "([" + SPECIAL_CHARACTERS + "]"
            + ALPHANUMERIC_NO_UNDERSCORE + ")*";
    public static final String VALIDATION_REGEX = LOCAL_PART_REGEX + "@u\\.nus\\.edu";

    public final String value;

    /**
     * Constructs an {@code Email}.
     *
     * @param email A valid email address.
     */
    public Email(String email) {
        requireNonNull(email);
        Optional<String> error = getValidationError(email);
        checkArgument(isValidEmail(email), error.orElse(MESSAGE_CONSTRAINTS));
        value = normalize(email);
    }

    /**
     * Returns true if a given string is a valid email after normalization.
     */
    public static boolean isValidEmail(String test) {
        return getValidationError(test).isEmpty();
    }

    /**
     * Returns the first validation error after normalization, or an empty optional for a valid email.
     * Checks length before matching the pattern to bound regex recursion. The local-part ends at the
     * first {@code @}; without one, the whole candidate is checked for excessive length.
     *
     * @throws NullPointerException if the email is null.
     */
    public static Optional<String> getValidationError(String email) {
        requireNonNull(email);
        String normalizedEmail = normalize(email);
        int atIndex = normalizedEmail.indexOf('@');
        int localPartLength = atIndex < 0 ? normalizedEmail.length() : atIndex;
        if (localPartLength > MAX_LOCAL_PART_LENGTH) {
            return Optional.of(String.format(Locale.ROOT, MESSAGE_LOCAL_PART_TOO_LONG,
                    MAX_LOCAL_PART_LENGTH, localPartLength));
        }
        return normalizedEmail.matches(VALIDATION_REGEX) ? Optional.empty() : Optional.of(MESSAGE_CONSTRAINTS);
    }

    private static String normalize(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Email otherEmail)) {
            return false;
        }

        return value.equals(otherEmail.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
