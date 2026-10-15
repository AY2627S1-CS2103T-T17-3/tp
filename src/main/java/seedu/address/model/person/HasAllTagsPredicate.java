package seedu.address.model.person;

import java.util.Set;
import java.util.function.Predicate;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.tag.Tag;

/**
 * Matches a person containing every requested tag using canonical tag identity.
 */
public class HasAllTagsPredicate implements Predicate<Person> {
    private final Set<Tag> tags;

    public HasAllTagsPredicate(Set<Tag> tags) {
        this.tags = Set.copyOf(tags);
    }

    @Override
    public boolean test(Person person) {
        return person.getTags().containsAll(tags);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof HasAllTagsPredicate otherPredicate && tags.equals(otherPredicate.tags);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("tags", tags).toString();
    }
}
