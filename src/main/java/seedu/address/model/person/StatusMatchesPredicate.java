package seedu.address.model.person;

import static java.util.Objects.requireNonNull;

import java.util.function.Predicate;

import seedu.address.commons.util.ToStringBuilder;

/**
 * Matches the member's stored status, independently of their tags.
 */
public class StatusMatchesPredicate implements Predicate<Person> {
    private final Status status;

    public StatusMatchesPredicate(Status status) {
        this.status = requireNonNull(status);
    }

    @Override
    public boolean test(Person person) {
        return person.getStatus() == status;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof StatusMatchesPredicate otherPredicate
                && status == otherPredicate.status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("status", status).toString();
    }
}
