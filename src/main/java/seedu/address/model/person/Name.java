package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * Represents a Person's name in the address book.
 * Guarantees: immutable; is valid as declared in {@link #isValidName(String)}
 */
public class Name {

    public static final int MAX_LENGTH = 127;

    public static final String MESSAGE_CONSTRAINTS =
            "Names may contain letters A-Z/a-z, digits, spaces and , ( ) / . @ - ' only.\n"
            + "Names must contain at least one letter or digit and be at most " + MAX_LENGTH + " characters long";

    /*
     * Allow the punctuation used in local names, while requiring at least one letter or digit.
     * Leading spaces remain invalid; command parsing and CSV import trim surrounding whitespace.
     */
    public static final String VALIDATION_REGEX = "(?! )(?=.*\\p{Alnum})[\\p{Alnum} ,()/.'@-]+";

    public final String fullName;

    /**
     * Constructs a {@code Name}.
     *
     * @param name A valid name.
     */
    public Name(String name) {
        requireNonNull(name);
        checkArgument(isValidName(name), MESSAGE_CONSTRAINTS);
        fullName = name;
    }

    /**
     * Returns true if a given string is a valid name.
     */
    public static boolean isValidName(String test) {
        return test.length() <= MAX_LENGTH && test.matches(VALIDATION_REGEX);
    }


    @Override
    public String toString() {
        return fullName;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Name otherName)) {
            return false;
        }

        return fullName.equals(otherName.fullName);
    }

    @Override
    public int hashCode() {
        return fullName.hashCode();
    }

}
