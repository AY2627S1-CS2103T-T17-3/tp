package seedu.address.model.person;

import static java.util.Objects.requireNonNull;

import java.util.function.Predicate;

import seedu.address.commons.util.ToStringBuilder;

/**
 * Tests that a {@code Person}'s phone number contains the given substring.
 */
public class PhoneContainsSubstringPredicate implements Predicate<Person> {
    private final String substring;

    /**
     * Creates a predicate that matches phone numbers containing the given {@code substring}.
     */
    public PhoneContainsSubstringPredicate(String substring) {
        this.substring = requireNonNull(substring);
    }

    @Override
    public boolean test(Person person) {
        return person.getPhone().value.contains(substring);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof PhoneContainsSubstringPredicate otherPhoneContainsSubstringPredicate)) {
            return false;
        }

        return substring.equals(otherPhoneContainsSubstringPredicate.substring);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("substring", substring).toString();
    }
}
