package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import seedu.address.commons.util.ToStringBuilder;

/**
 * Tests that a member's address contains a phrase, ignoring case and differences in whitespace.
 */
public class AddressContainsSubstringPredicate implements Predicate<Person> {
    private static final Pattern WHITESPACE = Pattern.compile("[\\p{IsWhite_Space}\\p{javaWhitespace}]+");

    private final String substring;

    /**
     * Creates a predicate for a nonblank address substring, which may contain multiple words.
     *
     * @throws NullPointerException if {@code substring} is null.
     * @throws IllegalArgumentException if {@code substring} contains only whitespace.
     */
    public AddressContainsSubstringPredicate(String substring) {
        this.substring = normalize(requireNonNull(substring));
        checkArgument(!this.substring.isEmpty(), "Address search substring must not be blank.");
    }

    @Override
    public boolean test(Person person) {
        return person.getAddress().map(address -> normalize(address.value).contains(substring)).orElse(false);
    }

    /**
     * Returns text with lowercase letters and each run of whitespace replaced by a single space.
     */
    private static String normalize(String value) {
        return WHITESPACE.matcher(value).replaceAll(" ").trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof AddressContainsSubstringPredicate otherAddressContainsSubstringPredicate)) {
            return false;
        }

        return substring.equals(otherAddressContainsSubstringPredicate.substring);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("substring", substring).toString();
    }
}
