package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import seedu.address.model.tag.Tag;
import seedu.address.testutil.PersonBuilder;

public class TagMatchesPredicateTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new TagMatchesPredicate(null));
    }

    @Test
    public void equals() {
        TagMatchesPredicate predicate = new TagMatchesPredicate(new Tag("publicity"));
        assertTrue(predicate.equals(predicate));
        assertTrue(predicate.equals(new TagMatchesPredicate(new Tag("publicity"))));
        assertTrue(predicate.equals(new TagMatchesPredicate(new Tag("PUBLICITY"))));
        assertFalse(predicate.equals(new TagMatchesPredicate(new Tag("exco"))));
        assertFalse(predicate.equals(new NameContainsKeywordsPredicate(List.of("publicity"))));
        assertFalse(predicate.equals(null));
    }

    @Test
    public void test_memberHasTag_returnsTrue() {
        Person person = new PersonBuilder().withTags("Publicity", "Exco").build();
        for (String tag : List.of("publicity", "PUBLICITY", "pUbLiCiTy", "exco")) {
            assertTrue(new TagMatchesPredicate(new Tag(tag)).test(person), tag);
        }
    }

    @Test
    public void test_memberDoesNotHaveExactTag_returnsFalse() {
        Person person = new PersonBuilder().withTags("publicity", "exco2025").build();
        for (String tag : List.of("pub", "city", "publicity2026", "exco", "volunteer")) {
            assertFalse(new TagMatchesPredicate(new Tag(tag)).test(person), tag);
        }
    }

    @Test
    public void test_memberHasNoTags_returnsFalse() {
        Person person = new PersonBuilder().withTags().build();
        assertFalse(new TagMatchesPredicate(new Tag("publicity")).test(person));
    }

    @Test
    public void test_tagMatchesOtherFields_returnsFalse() {
        Person person = new PersonBuilder().withName("Publicity").withEmail("publicity@u.nus.edu")
                .withAddress("Publicity Road").withPhone("91234567").withTags("exco").build();
        assertFalse(new TagMatchesPredicate(new Tag("publicity")).test(person));
        assertFalse(new TagMatchesPredicate(new Tag("91234567")).test(person));
    }

    @Test
    public void test_turkishLocale_matchesIgnoringCase() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            Person person = new PersonBuilder().withTags("publicity").build();
            assertTrue(new TagMatchesPredicate(new Tag("PUBLICITY")).test(person));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    public void toStringMethod() {
        TagMatchesPredicate predicate = new TagMatchesPredicate(new Tag("PUBLICITY"));
        String expected = TagMatchesPredicate.class.getCanonicalName() + "{tag=[publicity]}";
        assertEquals(expected, predicate.toString());
    }
}
