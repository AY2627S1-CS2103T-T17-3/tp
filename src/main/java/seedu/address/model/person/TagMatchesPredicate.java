package seedu.address.model.person;

import static java.util.Objects.requireNonNull;

import java.util.function.Predicate;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.tag.Tag;

/**
 * Tests that a {@code Person} has the given tag, using exact, case-insensitive tag identity.
 */
public class TagMatchesPredicate implements Predicate<Person> {
    private final Tag tag;

    /**
     * Creates a predicate that matches members with the given {@code tag}.
     */
    public TagMatchesPredicate(Tag tag) {
        this.tag = requireNonNull(tag);
    }

    @Override
    public boolean test(Person person) {
        return person.getTags().contains(tag);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof TagMatchesPredicate otherTagMatchesPredicate)) {
            return false;
        }

        return tag.equals(otherTagMatchesPredicate.tag);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("tag", tag).toString();
    }
}
