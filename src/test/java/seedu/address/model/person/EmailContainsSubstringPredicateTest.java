package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import seedu.address.testutil.PersonBuilder;

public class EmailContainsSubstringPredicateTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new EmailContainsSubstringPredicate(null));
    }

    @Test
    public void equals() {
        EmailContainsSubstringPredicate predicate = new EmailContainsSubstringPredicate("alice");
        assertTrue(predicate.equals(predicate));
        assertTrue(predicate.equals(new EmailContainsSubstringPredicate("alice")));
        assertTrue(predicate.equals(new EmailContainsSubstringPredicate("ALICE")));
        assertFalse(predicate.equals(new EmailContainsSubstringPredicate("bob")));
        assertFalse(predicate.equals(new NameContainsKeywordsPredicate(List.of("alice"))));
        assertFalse(predicate.equals(null));
    }

    @Test
    public void test_emailContainsSubstring_returnsTrue() {
        Person person = new PersonBuilder().withEmail("Alice.Tan+society@U.NUS.EDU").build();
        for (String substring : List.of("alice.tan+society@u.nus.edu", "ALICE.TAN+SOCIETY@U.NUS.EDU",
                "aLiCe", "tan+soc", "+SOCIETY@U.", "@u.nus.edu", "edu", "a", ".", "+")) {
            assertTrue(new EmailContainsSubstringPredicate(substring).test(person), substring);
        }
    }

    @Test
    public void test_emailDoesNotContainSubstring_returnsFalse() {
        Person person = new PersonBuilder().withEmail("alice@u.nus.edu").build();
        for (String substring : List.of("bob", "alce", "alice@u.nus.edu.com", "alice@nus.edu",
                "alice.*", "alice bob")) {
            assertFalse(new EmailContainsSubstringPredicate(substring).test(person), substring);
        }
    }

    @Test
    public void test_fullAddressMatchesSuffix_returnsTrue() {
        Person person = new PersonBuilder().withEmail("malice@u.nus.edu").build();
        assertTrue(new EmailContainsSubstringPredicate("ALICE@U.NUS.EDU").test(person));
    }

    @Test
    public void test_substringMatchesOtherFields_returnsFalse() {
        Person person = new PersonBuilder().withEmail("bob@u.nus.edu").withName("Alice 9123")
                .withPhone("91234567").withAddress("Alice 9123 Main Street").withTags("alice", "9123").build();
        assertFalse(new EmailContainsSubstringPredicate("alice").test(person));
        assertFalse(new EmailContainsSubstringPredicate("9123").test(person));
    }

    @Test
    public void test_turkishLocale_matchesIgnoringCase() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            Person person = new PersonBuilder().withEmail("irfan@u.nus.edu").build();
            assertTrue(new EmailContainsSubstringPredicate("IRFAN").test(person));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    public void toStringMethod() {
        EmailContainsSubstringPredicate predicate = new EmailContainsSubstringPredicate("ALICE");
        String expected = EmailContainsSubstringPredicate.class.getCanonicalName() + "{substring=alice}";
        assertEquals(expected, predicate.toString());
    }
}
