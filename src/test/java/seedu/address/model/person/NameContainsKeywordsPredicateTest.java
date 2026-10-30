package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import seedu.address.testutil.PersonBuilder;

public class NameContainsKeywordsPredicateTest {

    @Test
    public void equals() {
        List<String> firstPredicateKeywordList = List.of("first");
        List<String> secondPredicateKeywordList = List.of("first", "second");

        NameContainsKeywordsPredicate firstPredicate = new NameContainsKeywordsPredicate(firstPredicateKeywordList);
        NameContainsKeywordsPredicate secondPredicate = new NameContainsKeywordsPredicate(secondPredicateKeywordList);

        // same object -> returns true
        assertTrue(firstPredicate.equals(firstPredicate));

        // same values -> returns true
        NameContainsKeywordsPredicate firstPredicateCopy = new NameContainsKeywordsPredicate(firstPredicateKeywordList);
        assertTrue(firstPredicate.equals(firstPredicateCopy));

        // different types -> returns false
        assertFalse(firstPredicate.equals(1));

        // null -> returns false
        assertFalse(firstPredicate.equals(null));

        // different person -> returns false
        assertFalse(firstPredicate.equals(secondPredicate));
    }

    @Test
    public void test_nameContainsKeywords_returnsTrue() {
        // One keyword
        NameContainsKeywordsPredicate predicate = new NameContainsKeywordsPredicate(List.of("Alice"));
        assertTrue(predicate.test(new PersonBuilder().withName("Alice Bob").build()));

        // Multiple keywords
        predicate = new NameContainsKeywordsPredicate(List.of("Alice", "Bob"));
        assertTrue(predicate.test(new PersonBuilder().withName("Alice Bob").build()));

        // Only one matching keyword
        predicate = new NameContainsKeywordsPredicate(List.of("Bob", "Carol"));
        assertTrue(predicate.test(new PersonBuilder().withName("Alice Carol").build()));

        // Mixed-case keywords
        predicate = new NameContainsKeywordsPredicate(List.of("aLIce", "bOB"));
        assertTrue(predicate.test(new PersonBuilder().withName("Alice Bob").build()));
    }

    @Test
    public void test_nameDoesNotContainKeywords_returnsFalse() {
        // Zero keywords
        NameContainsKeywordsPredicate predicate = new NameContainsKeywordsPredicate(List.of());
        assertFalse(predicate.test(new PersonBuilder().withName("Alice").build()));

        // Non-matching keyword
        predicate = new NameContainsKeywordsPredicate(List.of("Carol"));
        assertFalse(predicate.test(new PersonBuilder().withName("Alice Bob").build()));

        // Keywords match phone, email and address, but do not match name
        predicate = new NameContainsKeywordsPredicate(List.of("12345", "alice@u.nus.edu", "Main", "Street"));
        assertFalse(predicate.test(new PersonBuilder().withName("Alice").withPhone("12345")
                .withEmail("alice@u.nus.edu").withAddress("Main Street").build()));
    }

    @Test
    public void test_partialNameKeywords_returnsFalse() {
        NameContainsKeywordsPredicate predicate = new NameContainsKeywordsPredicate(List.of("Ali", "Bo"));
        assertFalse(predicate.test(new PersonBuilder().withName("Alice Bob").build()));
    }

    @Test
    public void test_plainKeywordsAtPunctuationBoundaries_returnsTrue() {
        for (String name : List.of("Tan, Alex", "Tan,Alex", "Tan (Alex)", "Tan(Alex)", "Tan @ Alex",
                "Tan@Alex", "Tan.Alex", "Tan,,((Alex))")) {
            for (String keyword : List.of("TAN", "alex")) {
                NameContainsKeywordsPredicate predicate = new NameContainsKeywordsPredicate(List.of(keyword));
                assertTrue(predicate.test(new PersonBuilder().withName(name).build()), name + ": " + keyword);
            }
        }
    }

    @Test
    public void test_initialsWithPeriods_matchesPlainKeywords() {
        for (String name : List.of("J. P. Tan", "J.P.Tan")) {
            for (String keyword : List.of("j", "P", "tan")) {
                NameContainsKeywordsPredicate predicate = new NameContainsKeywordsPredicate(List.of(keyword));
                assertTrue(predicate.test(new PersonBuilder().withName(name).build()), name + ": " + keyword);
            }
        }
    }

    @Test
    public void test_compoundKeywordsAtPunctuationBoundaries_returnsTrue() {
        Map<String, String> namesAndKeywords = Map.of(
                "Tan(O'Connor)", "o'CONNOR", "Tan,Anne-Marie", "ANNE-marie", "Ravi(S/O)Kumar", "s/o",
                "Tan@AN/NA", "an/na", "'Anne-Marie'", "Anne-Marie", "(A--B)", "A--B",
                "S//O.", "s//o", "Tan(123)", "123", "Tan@A1", "a1");
        namesAndKeywords.forEach((name, keyword) -> {
            NameContainsKeywordsPredicate predicate = new NameContainsKeywordsPredicate(List.of(keyword));
            assertTrue(predicate.test(new PersonBuilder().withName(name).build()), name + ": " + keyword);
        });
    }

    @Test
    public void test_leadingOrTrailingPunctuation_matchesPlainKeyword() {
        NameContainsKeywordsPredicate predicate = new NameContainsKeywordsPredicate(List.of("aLeX"));
        for (String name : List.of("'Alex", "Alex'", "-Alex-", "/Alex/", "(('Alex-))", ",./-'Alex-'/.,")) {
            assertTrue(predicate.test(new PersonBuilder().withName(name).build()), name);
        }
    }

    @Test
    public void test_internalJoiners_preservesCompoundWords() {
        Map<String, List<String>> partialKeywords = Map.of(
                "Anne-Marie", List.of("Anne", "Marie", "AnneMarie"),
                "O'Connor", List.of("O", "Connor", "OConnor"),
                "S/O", List.of("S", "O", "SO"),
                "AN/NA", List.of("AN", "NA", "ANNA"),
                "A--B", List.of("A", "B", "AB"),
                "O''Connor", List.of("O", "Connor", "OConnor"));
        partialKeywords.forEach((name, keywords) -> {
            assertTrue(new NameContainsKeywordsPredicate(List.of(name))
                    .test(new PersonBuilder().withName(name + " Tan").build()), name);
            for (String keyword : keywords) {
                NameContainsKeywordsPredicate predicate = new NameContainsKeywordsPredicate(List.of(keyword));
                assertFalse(predicate.test(new PersonBuilder().withName(name + " Tan").build()),
                        name + ": " + keyword);
            }
        });
    }

    @Test
    public void test_punctuatedKeywords_matchesOnlyLiteralWhitespaceTokens() {
        Map<String, String> namesAndKeywords = Map.of(
                "Tan, Alex", "tan,", "Tan (Alex)", "(ALEX)", "J. P. Tan", "j.", "Tan @ Alex", "@",
                "Alex - Tan", "-", "Alex / Tan", "/", "Alex ' Tan", "'", "Alex . Tan", ".");
        namesAndKeywords.forEach((name, keyword) -> {
            NameContainsKeywordsPredicate predicate = new NameContainsKeywordsPredicate(List.of(keyword));
            assertTrue(predicate.test(new PersonBuilder().withName(name).build()), name + ": " + keyword);
            assertFalse(predicate.test(new PersonBuilder().withName("Alex Tan").build()), keyword);
        });
        assertFalse(new NameContainsKeywordsPredicate(List.of("@"))
                .test(new PersonBuilder().withName("Tan@Alex").build()));
    }

    @Test
    public void test_punctuationBoundaries_doesNotJoinWordsOrMatchPartialWords() {
        for (String keyword : List.of("TanAlex", "JP", "Ta", "Ale", "Han")) {
            NameContainsKeywordsPredicate predicate = new NameContainsKeywordsPredicate(List.of(keyword));
            assertFalse(predicate.test(new PersonBuilder().withName("Tan,Alex J.P. Hans").build()), keyword);
        }
    }

    @Test
    public void test_regexMetacharactersAndUnsupportedUnicode_returnsFalse() {
        for (String keyword : List.of(".*", "[A-Za-z]+", "(?i)alex", "\\QAlex\\E", "\uff21lex",
                "O\u2019Connor")) {
            NameContainsKeywordsPredicate predicate = new NameContainsKeywordsPredicate(List.of(keyword));
            assertFalse(predicate.test(new PersonBuilder().withName("Alex O'Connor").build()), keyword);
        }
    }

    @Test
    public void test_maximumLengthName_matchesWholeComponentOnly() {
        String keyword = "A".repeat(Name.MAX_LENGTH - 2);
        Person person = new PersonBuilder().withName("(" + keyword + ")").build();
        assertTrue(new NameContainsKeywordsPredicate(List.of(keyword)).test(person));
        assertFalse(new NameContainsKeywordsPredicate(List.of(keyword.substring(1))).test(person));
        assertFalse(new NameContainsKeywordsPredicate(List.of("A".repeat(10_000))).test(person));
    }

    @Test
    public void test_missingName_returnsFalse() {
        NameContainsKeywordsPredicate predicate = new NameContainsKeywordsPredicate(List.of("Alex", "@"));
        assertFalse(predicate.test(new PersonBuilder().withoutName().build()));
    }

    @Test
    public void toStringMethod() {
        List<String> keywords = List.of("keyword1", "keyword2");
        NameContainsKeywordsPredicate predicate = new NameContainsKeywordsPredicate(keywords);

        String expected = NameContainsKeywordsPredicate.class.getCanonicalName() + "{keywords=" + keywords + "}";
        assertEquals(expected, predicate.toString());
    }
}
