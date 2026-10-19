package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import seedu.address.testutil.PersonBuilder;

public class AllConditionsPredicateTest {
    private final Predicate<Person> nameCondition = new NameContainsKeywordsPredicate(List.of("John"));
    private final Predicate<Person> phoneCondition = new PhoneContainsSubstringPredicate("9123");

    @Test
    public void constructor_invalidConditions_throwsException() {
        assertThrows(NullPointerException.class, () -> new AllConditionsPredicate(null));
        assertThrows(NullPointerException.class, () -> new AllConditionsPredicate(Arrays.asList(nameCondition, null)));
        assertThrows(IllegalArgumentException.class, () -> new AllConditionsPredicate(List.of()));
    }

    @Test
    public void test_allConditionsMatch_returnsTrue() {
        Person person = new PersonBuilder().withName("John Tan").withPhone("91234567").build();
        assertTrue(new AllConditionsPredicate(List.of(nameCondition)).test(person));
        assertTrue(new AllConditionsPredicate(List.of(nameCondition, phoneCondition)).test(person));
    }

    @Test
    public void test_anyConditionDoesNotMatch_returnsFalse() {
        AllConditionsPredicate predicate = new AllConditionsPredicate(List.of(nameCondition, phoneCondition));
        assertFalse(predicate.test(new PersonBuilder().withName("John Tan").withPhone("88888888").build()));
        assertFalse(predicate.test(new PersonBuilder().withName("Alice Tan").withPhone("91234567").build()));
        assertFalse(predicate.test(new PersonBuilder().withoutName().withPhone("91234567").build()));
        assertFalse(predicate.test(new PersonBuilder().withName("John Tan").withoutPhone().build()));
    }

    @Test
    public void test_sourceConditionsChanged_preservesOriginalConditions() {
        List<Predicate<Person>> conditions = new ArrayList<>(List.of(nameCondition, phoneCondition));
        AllConditionsPredicate predicate = new AllConditionsPredicate(conditions);
        conditions.clear();
        assertFalse(predicate.test(new PersonBuilder().withName("John Tan").withPhone("88888888").build()));
    }

    @Test
    public void equals() {
        AllConditionsPredicate predicate = new AllConditionsPredicate(List.of(nameCondition, phoneCondition));
        assertTrue(predicate.equals(predicate));
        assertTrue(predicate.equals(new AllConditionsPredicate(List.of(
                new NameContainsKeywordsPredicate(List.of("John")), new PhoneContainsSubstringPredicate("9123")))));
        assertFalse(predicate.equals(new AllConditionsPredicate(List.of(nameCondition))));
        assertFalse(predicate.equals(new AllConditionsPredicate(List.of(nameCondition,
                new PhoneContainsSubstringPredicate("8888")))));
        assertFalse(predicate.equals(nameCondition));
        assertFalse(predicate.equals(null));
    }

    @Test
    public void toStringMethod() {
        List<Predicate<Person>> conditions = List.of(nameCondition, phoneCondition);
        assertEquals(AllConditionsPredicate.class.getCanonicalName() + "{predicates=" + conditions + "}",
                new AllConditionsPredicate(conditions).toString());
    }
}
