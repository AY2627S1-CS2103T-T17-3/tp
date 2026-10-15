package seedu.address.model.person;

import static seedu.address.commons.util.AppUtil.checkArgument;

import java.util.List;
import java.util.function.Predicate;

import seedu.address.commons.util.ToStringBuilder;

/**
 * Matches a person only when every supplied search condition matches.
 */
public class AllConditionsPredicate implements Predicate<Person> {
    private final List<Predicate<Person>> conditions;

    /**
     * Creates an immutable combination of at least one condition in canonical field order.
     */
    public AllConditionsPredicate(List<Predicate<Person>> conditions) {
        this.conditions = List.copyOf(conditions);
        checkArgument(!this.conditions.isEmpty(), "At least one search condition is required.");
    }

    @Override
    public boolean test(Person person) {
        return conditions.stream().allMatch(condition -> condition.test(person));
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof AllConditionsPredicate otherPredicate
                && conditions.equals(otherPredicate.conditions);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("conditions", conditions).toString();
    }
}
