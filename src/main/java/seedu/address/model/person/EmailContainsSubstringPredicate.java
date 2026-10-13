package seedu.address.model.person;

import static java.util.Objects.requireNonNull;

import java.util.Locale;
import java.util.function.Predicate;

import seedu.address.commons.util.ToStringBuilder;

/**
 * Tests that a {@code Person}'s canonical email contains the given substring, ignoring case.
 */
public class EmailContainsSubstringPredicate implements Predicate<Person> {
    private final String substring;

    /**
     * Creates a predicate that matches emails containing the given {@code substring}, ignoring case.
     * The substring need not be a valid complete email address.
     */
    public EmailContainsSubstringPredicate(String substring) {
        this.substring = requireNonNull(substring).toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean test(Person person) {
        return person.getEmail().value.contains(substring);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof EmailContainsSubstringPredicate otherEmailContainsSubstringPredicate)) {
            return false;
        }

        return substring.equals(otherEmailContainsSubstringPredicate.substring);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("substring", substring).toString();
    }
}
