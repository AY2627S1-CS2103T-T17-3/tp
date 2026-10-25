package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;

public class EmailTest {

    @Test
    public void getValidationError_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> Email.getValidationError(null));
    }

    @Test
    public void constructor_localPartWithinLimit_acceptsAndNormalizes() {
        for (int length : new int[] {1, 63, 64}) {
            String email = "A".repeat(length) + "@U.NUS.EDU";
            assertTrue(Email.isValidEmail(email));
            assertEquals(Optional.empty(), Email.getValidationError(email));
            assertEquals("a".repeat(length) + "@u.nus.edu", new Email(email).value);
        }
        String separatedEmail = "a.".repeat(31) + "aa@u.nus.edu";
        assertTrue(Email.isValidEmail(separatedEmail));
        assertEquals(separatedEmail, new Email(separatedEmail).value);
    }

    @Test
    public void constructor_overlongLocalPart_rejectsBeforePatternValidation() {
        assertLengthError("a".repeat(65) + "@u.nus.edu", 65);
        assertLengthError("a.".repeat(32) + "a@u.nus.edu", 65);
        assertLengthError("a.".repeat(2000) + "a@u.nus.edu", 4001);
        assertLengthError("a".repeat(65) + "@gmail.com", 65);
        assertLengthError("a".repeat(65) + "@@u.nus.edu", 65);
        assertLengthError("a".repeat(65), 65);
    }

    @Test
    public void getValidationError_largeMalformedCandidates_rejectsWithoutOverflow() {
        assertLengthError("a.".repeat(50_000) + "a@u.nus.edu", 100_001);
        assertLengthError("a.".repeat(50_000), 100_000);
        for (String email : new String[] {"a@" + "a.".repeat(50_000),
            "a@@" + "a.".repeat(50_000)}) {
            assertFalse(Email.isValidEmail(email));
            assertEquals(Optional.of(Email.MESSAGE_CONSTRAINTS), Email.getValidationError(email));
        }
    }

    @Test
    public void getValidationError_localPartLength_countsNormalizedUtf16WithoutTrimming() {
        // Lowercasing capital dotted I expands it to two UTF-16 units, which remain invalid syntax.
        assertEquals(Optional.of(Email.MESSAGE_CONSTRAINTS),
                Email.getValidationError("\u0130".repeat(32) + "@u.nus.edu"));
        assertLengthError("\u0130".repeat(33) + "@u.nus.edu", 66);
        assertLengthError("\ud83d\ude00".repeat(33) + "@u.nus.edu", 66);
        assertLengthError(" " + "a".repeat(64) + "@u.nus.edu", 65);
    }

    @Test
    public void getValidationError_arabicLocale_preservesExactLengthMessage() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ar"));
            assertLengthError("a".repeat(65) + "@u.nus.edu", 65);
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    private static void assertLengthError(String email, int expectedLength) {
        String expected = "Email local-part must contain at most 64 characters; received " + expectedLength + ".";
        assertFalse(Email.isValidEmail(email));
        assertEquals(Optional.of(expected), Email.getValidationError(email));
        assertThrows(IllegalArgumentException.class, expected, () -> new Email(email));
    }

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Email(null));
    }

    @Test
    public void constructor_invalidEmail_throwsIllegalArgumentException() {
        String invalidEmail = "";
        assertThrows(IllegalArgumentException.class, () -> new Email(invalidEmail));
    }

    @Test
    public void isValidEmail() {
        // null email
        assertThrows(NullPointerException.class, () -> Email.isValidEmail(null));

        // blank email
        assertFalse(Email.isValidEmail("")); // empty string
        assertFalse(Email.isValidEmail(" ")); // spaces only

        // missing parts
        assertFalse(Email.isValidEmail("@u.nus.edu")); // missing local part
        assertFalse(Email.isValidEmail("peterjackexample.com")); // missing '@' symbol
        assertFalse(Email.isValidEmail("peterjack@")); // missing domain name

        // invalid parts
        assertFalse(Email.isValidEmail("peterjack@-")); // invalid domain name
        assertFalse(Email.isValidEmail("peterjack@u_nus_edu")); // underscore in domain name
        assertFalse(Email.isValidEmail("peter jack@u.nus.edu")); // spaces in local part
        assertFalse(Email.isValidEmail("peterjack@u nus.edu")); // spaces in domain name
        assertFalse(Email.isValidEmail(" peterjack@u.nus.edu")); // leading space
        assertFalse(Email.isValidEmail("peterjack@u.nus.edu ")); // trailing space
        assertFalse(Email.isValidEmail("peterjack@@u.nus.edu")); // double '@' symbol
        assertFalse(Email.isValidEmail("peter@jack@u.nus.edu")); // '@' symbol in local part
        assertFalse(Email.isValidEmail("-peterjack@u.nus.edu")); // local part starts with a hyphen
        assertFalse(Email.isValidEmail("peterjack-@u.nus.edu")); // local part ends with a hyphen
        assertFalse(Email.isValidEmail("peter..jack@u.nus.edu")); // local part has two consecutive periods
        assertFalse(Email.isValidEmail("peter+-jack@u.nus.edu")); // adjacent punctuation
        assertFalse(Email.isValidEmail("peterjack@example@com")); // '@' symbol in domain name
        assertFalse(Email.isValidEmail("peterjack@.example.com")); // domain name starts with a period
        assertFalse(Email.isValidEmail("peterjack@example.com.")); // domain name ends with a period
        assertFalse(Email.isValidEmail("peterjack@-example.com")); // domain name starts with a hyphen
        assertFalse(Email.isValidEmail("peterjack@example.com-")); // domain name ends with a hyphen
        assertFalse(Email.isValidEmail("peterjack@example.c")); // top level domain has less than two chars

        // valid email
        assertTrue(Email.isValidEmail("PeterJack_1190@u.nus.edu")); // underscore in local part
        assertTrue(Email.isValidEmail("PeterJack.1190@u.nus.edu")); // period in local part
        assertTrue(Email.isValidEmail("PeterJack+1190@u.nus.edu")); // '+' symbol in local part
        assertTrue(Email.isValidEmail("PeterJack-1190@u.nus.edu")); // hyphen in local part
        assertTrue(Email.isValidEmail("a@u.nus.edu")); // minimal local part
        assertTrue(Email.isValidEmail("123@u.nus.edu")); // numeric local part
        assertTrue(Email.isValidEmail("a1+be.d@u.nus.edu")); // mixed separators
        assertTrue(Email.isValidEmail("if.you.dream.it_you.can.do.it@u.nus.edu")); // long local part
        assertTrue(Email.isValidEmail("e1234567@U.NUS.EDU")); // uppercase domain
    }

    @Test
    public void isInvalidEmail_otherDomains_rejectsEmail() {
        for (String email : new String[] {"alex@example.com", "alex@gmail.com", "alex@nus.edu",
            "alex@sub.u.nus.edu", "alex@u.nus.edu.com", "alex@u.nus.edu.", "alex@uXnusXedu",
            "alex@otheru.nus.edu", "alex@u.nus.edux", "alex@localhost", "a@bc", "123@145"}) {
            assertFalse(Email.isValidEmail(email), email);
            assertThrows(IllegalArgumentException.class, Email.MESSAGE_CONSTRAINTS, () -> new Email(email));
        }
    }

    @Test
    public void constructor_mixedCase_canonicalizesEmail() {
        Email email = new Email("Alex.One@U.NUS.EDU");
        Email canonical = new Email("alex.one@u.nus.edu");
        assertEquals(canonical, email);
        assertEquals(canonical.hashCode(), email.hashCode());
        assertEquals("alex.one@u.nus.edu", email.value);
        assertEquals(email.value, email.toString());
    }

    @Test
    public void constructor_turkishLocale_usesRootLocale() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("irfan@u.nus.edu", new Email("IRFAN@U.NUS.EDU").value);
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    public void isValidEmail_whitespace_rejectsEmailWithoutTrimming() {
        for (String email : new String[] {" alex@u.nus.edu", "alex@u.nus.edu ",
            "\talex@u.nus.edu", "alex@u.nus.edu\r\n", "alex\ttan@u.nus.edu", "alex@\nu.nus.edu",
            "\u00a0alex@u.nus.edu", "\u2003alex@u.nus.edu"}) {
            assertFalse(Email.isValidEmail(email));
            assertThrows(IllegalArgumentException.class, () -> new Email(email));
        }
    }

    @Test
    public void equals_aliases_remainDistinct() {
        Email email = new Email("alex@u.nus.edu");
        assertNotEquals(email, new Email("alex+society@u.nus.edu"));
        assertNotEquals(email, new Email("a.lex@u.nus.edu"));
    }

    @Test
    public void equals() {
        Email email = new Email("valid@u.nus.edu");

        // same values -> returns true
        assertTrue(email.equals(new Email("valid@u.nus.edu")));

        // same object -> returns true
        assertTrue(email.equals(email));

        // null -> returns false
        assertFalse(email.equals(null));

        // different types -> returns false
        assertFalse(email.equals(5.0f));

        // different values -> returns false
        assertFalse(email.equals(new Email("other.valid@u.nus.edu")));
    }
}
