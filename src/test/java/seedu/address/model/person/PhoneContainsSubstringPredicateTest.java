package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import seedu.address.testutil.PersonBuilder;

public class PhoneContainsSubstringPredicateTest {

    @Test
    public void equals() {
        PhoneContainsSubstringPredicate predicate = new PhoneContainsSubstringPredicate("9123");
        assertTrue(predicate.equals(predicate));
        assertTrue(predicate.equals(new PhoneContainsSubstringPredicate("9123")));
        assertFalse(predicate.equals(new PhoneContainsSubstringPredicate("4567")));
        assertFalse(predicate.equals("9123"));
        assertFalse(predicate.equals(null));
    }

    @Test
    public void test_phoneContainsSubstring_returnsTrue() {
        Person person = new PersonBuilder().withPhone("91234567").build();
        assertTrue(new PhoneContainsSubstringPredicate("91234567").test(person));
        assertTrue(new PhoneContainsSubstringPredicate("9123").test(person));
        assertTrue(new PhoneContainsSubstringPredicate("2345").test(person));
        assertTrue(new PhoneContainsSubstringPredicate("4567").test(person));
        assertTrue(new PhoneContainsSubstringPredicate("23").test(person));
        assertTrue(new PhoneContainsSubstringPredicate("7").test(person));
    }

    @Test
    public void test_phoneDoesNotContainSubstring_returnsFalse() {
        Person person = new PersonBuilder().withPhone("91234567").build();
        assertFalse(new PhoneContainsSubstringPredicate("000").test(person));
        assertFalse(new PhoneContainsSubstringPredicate("923").test(person));
        assertFalse(new PhoneContainsSubstringPredicate("991234567").test(person));
    }

    @Test
    public void test_substringMatchesOtherFields_returnsFalse() {
        Person person = new PersonBuilder().withPhone("99999999").withName("9123 Alice")
                .withEmail("9123@example.com").withAddress("9123 Main Street").withTags("9123").build();
        assertFalse(new PhoneContainsSubstringPredicate("9123").test(person));
    }

    @Test
    public void toStringMethod() {
        PhoneContainsSubstringPredicate predicate = new PhoneContainsSubstringPredicate("9123");
        String expected = PhoneContainsSubstringPredicate.class.getCanonicalName() + "{substring=9123}";
        assertEquals(expected, predicate.toString());
    }
}
