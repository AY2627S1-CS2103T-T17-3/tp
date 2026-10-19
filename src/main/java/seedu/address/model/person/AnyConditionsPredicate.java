package seedu.address.model.person;

import static seedu.address.commons.util.AppUtil.checkArgument;

import java.util.List;
import java.util.function.Predicate;

import seedu.address.commons.util.ToStringBuilder;

/**
 * Tests that a member matches at least one supplied search condition.
 */
public class AnyConditionsPredicate implements Predicate<Person> {
    private final List<Predicate<Person>> predicates;

    /**
     * Creates a predicate from a nonempty list of conditions, preserving a copy of the list.
     */
    public AnyConditionsPredicate(List<Predicate<Person>> predicates) {
        this.predicates = List.copyOf(predicates);
        checkArgument(!this.predicates.isEmpty(), "At least one search condition is required.");
    }

    @Override
    public boolean test(Person person) {
        return predicates.stream().anyMatch(predicate -> predicate.test(person));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof AnyConditionsPredicate otherPredicate)) {
            return false;
        }
        return predicates.equals(otherPredicate.predicates);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("predicates", predicates).toString();
    }
}
