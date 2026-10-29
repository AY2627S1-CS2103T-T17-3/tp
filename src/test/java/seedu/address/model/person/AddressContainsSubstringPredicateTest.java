package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import seedu.address.testutil.PersonBuilder;

/**
 * Tests address phrase matching, normalization and absent addresses.
 */
public class AddressContainsSubstringPredicateTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new AddressContainsSubstringPredicate(null));
    }

    @Test
    public void constructor_blankSubstring_throwsIllegalArgumentException() {
        for (String value : List.of("", " \t\r\n ", "\u0085", "\u00a0", "\u2007", "\u202f", "\u3000",
                "\u00a0\u001c\u2003\u202f")) {
            assertThrows(IllegalArgumentException.class, () -> new AddressContainsSubstringPredicate(value));
        }
    }

    @Test
    public void equals() {
        AddressContainsSubstringPredicate predicate = new AddressContainsSubstringPredicate("Kent Ridge");
        assertTrue(predicate.equals(predicate));
        assertTrue(predicate.equals(new AddressContainsSubstringPredicate("  KENT\t\u00a0RIDGE  ")));
        assertFalse(predicate.equals(new AddressContainsSubstringPredicate("Ridge Kent")));
        assertFalse(predicate.equals(new AddressContainsSubstringPredicate("Kent")));
        assertFalse(predicate.equals(new EmailContainsSubstringPredicate("Kent Ridge")));
        assertFalse(predicate.equals(null));
    }

    @Test
    public void test_addressContainsSubstring_returnsTrue() {
        Person person = new PersonBuilder().withAddress("12 Kent Ridge Road, #03-45, Singapore 119077").build();
        for (String substring : List.of("12 Kent Ridge Road, #03-45, Singapore 119077", "12", "kEnT rIdGe",
                "ridge", "Road, #03", "#03-45", "119077", "1907", "d", ",", "-")) {
            assertTrue(new AddressContainsSubstringPredicate(substring).test(person), substring);
        }
    }

    @Test
    public void test_whitespaceDifferences_matchesWithoutChangingAddress() {
        String storedAddress = "\u00a012\tKent\u2007\u2003Ridge\u202fRoad  #03-45\u3000";
        Person person = new PersonBuilder().withAddress(storedAddress).build();
        for (String whitespace : List.of(" ", "  ", "\t", "\n", "\r\n", "\u0085", "\u00a0", "\u2007",
                "\u202f", "\u3000", "\u001c", " \t\u2003 ")) {
            String substring = whitespace + "KENT" + whitespace + "RIDGE" + whitespace + "ROAD" + whitespace;
            assertTrue(new AddressContainsSubstringPredicate(substring).test(person), substring);
        }
        assertEquals(storedAddress, person.getAddress().orElseThrow().value);
    }

    @Test
    public void test_nonConsecutiveOrNonLiteralSubstring_returnsFalse() {
        Person person = new PersonBuilder().withAddress("12 Kent Ridge Road #03-45").build();
        for (String substring : List.of("Kent Road", "Ridge Kent", "Kent.*Ridge", "Kent*", "#03/45",
                "KentRidge", "Kent Ridge Rd", "Not Kent Ridge", "\"Kent Ridge\"", "x".repeat(1000))) {
            assertFalse(new AddressContainsSubstringPredicate(substring).test(person), substring);
        }
    }

    @Test
    public void test_punctuationAndUnicode_matchesLiterally() {
        Person person = new PersonBuilder().withAddress("c/o Alex, #03/45, 大学路 (NUS)").build();
        for (String substring : List.of("C/O", "#03/45", "大学路", "(nus)", "Alex, #03/45")) {
            assertTrue(new AddressContainsSubstringPredicate(substring).test(person), substring);
        }
    }

    @Test
    public void test_substringMatchesOtherFields_returnsFalse() {
        Person person = new PersonBuilder().withAddress("West Road").withName("Kent Ridge")
                .withEmail("kent@u.nus.edu").withPhone("119077").withTags("kent").build();
        assertFalse(new AddressContainsSubstringPredicate("Kent").test(person));
        assertFalse(new AddressContainsSubstringPredicate("119077").test(person));
        assertFalse(new AddressContainsSubstringPredicate("kent@u.nus.edu").test(person));
    }

    @Test
    public void test_absentAddress_returnsFalse() {
        Person person = new PersonBuilder().withoutAddress().build();
        assertFalse(new AddressContainsSubstringPredicate("-").test(person));
        assertFalse(new AddressContainsSubstringPredicate("Kent").test(person));
    }

    @Test
    public void test_turkishLocale_matchesIgnoringCase() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            Person person = new PersonBuilder().withAddress("KENT RIDGE").build();
            assertTrue(new AddressContainsSubstringPredicate("kent ridge").test(person));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    public void toStringMethod() {
        AddressContainsSubstringPredicate predicate = new AddressContainsSubstringPredicate(" KENT\tRIDGE ");
        String expected = AddressContainsSubstringPredicate.class.getCanonicalName() + "{substring=kent ridge}";
        assertEquals(expected, predicate.toString());
    }
}
