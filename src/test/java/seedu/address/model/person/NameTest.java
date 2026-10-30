package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

public class NameTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Name(null));
    }

    @Test
    public void constructor_invalidName_throwsIllegalArgumentException() {
        String invalidName = "";
        assertThrows(IllegalArgumentException.class, () -> new Name(invalidName));
    }

    @Test
    public void isValidName() {
        // null name
        assertThrows(NullPointerException.class, () -> Name.isValidName(null));

        // invalid name
        assertFalse(Name.isValidName("")); // empty string
        assertFalse(Name.isValidName(" ")); // spaces only
        assertFalse(Name.isValidName("^")); // only non-alphanumeric characters
        assertFalse(Name.isValidName("peter*")); // contains unsupported punctuation

        // valid name
        assertTrue(Name.isValidName("peter jack")); // alphabets only
        assertTrue(Name.isValidName("12345")); // numbers only
        assertTrue(Name.isValidName("peter the 2nd")); // alphanumeric characters
        assertTrue(Name.isValidName("Capital Tan")); // with capital letters
        assertTrue(Name.isValidName("David Roger Jackson Ray Jr 2nd")); // long names
    }

    @Test
    public void validation_supportedPunctuation_preservesSpelling() {
        for (String value : List.of("Mary-Anne Tan", "Anne-Marie Tan", "O'Connor Tan", "J. P. Tan",
                "Ravi S/O Kumar", "Devi D/O Kumar", "Tan, Alex", "Tan (Alex)", "Tan @ Alex",
                "'Ali", "(Alex) Tan")) {
            assertTrue(Name.isValidName(value), value);
            assertEquals(value, new Name(value).fullName);
        }
    }

    @Test
    public void validation_punctuationOnly_rejectsName() {
        for (String value : List.of(",", "(", ")", "/", ".", "@", "-", "'", " ,()/.'@-")) {
            assertFalse(Name.isValidName(value), value);
            assertThrows(IllegalArgumentException.class, Name.MESSAGE_CONSTRAINTS, () -> new Name(value));
        }
    }

    @Test
    public void validation_unsupportedCharacters_rejectsName() {
        for (String value : List.of("Alex!", "Alex&", "Alex_", "Alex\\Tan", " Alex", "Alex\tTan",
                "Alex\nTan", "Alex\rTan", "Alex\0Tan", "Alex\u00a0Tan", "\u3000", "Alex\u200bTan",
                "Jos\u00e9 Tan", "O\u2019Connor Tan", "\u9648")) {
            assertFalse(Name.isValidName(value), value);
            assertThrows(IllegalArgumentException.class, Name.MESSAGE_CONSTRAINTS, () -> new Name(value));
        }
    }

    @Test
    public void validation_lengthIncludesPunctuation() {
        String boundaryName = "A".repeat(126) + "-";
        assertTrue(Name.isValidName(boundaryName));
        assertEquals(boundaryName, new Name(boundaryName).fullName);
        assertFalse(Name.isValidName(boundaryName + "'"));
    }

    @Test
    public void validation_lengthBoundaries_acceptsAtMost127Characters() {
        for (int length : new int[] {1, 126, 127}) {
            String value = "A".repeat(length);
            assertTrue(Name.isValidName(value));
            assertEquals(value, new Name(value).fullName);
        }
        for (int length : new int[] {128, 10000}) {
            String value = "A".repeat(length);
            assertFalse(Name.isValidName(value));
            assertThrows(IllegalArgumentException.class, Name.MESSAGE_CONSTRAINTS, () -> new Name(value));
        }
    }

    @Test
    public void validation_lengthIncludesInternalSpaces() {
        assertTrue(Name.isValidName("A " + "B".repeat(125)));
        assertFalse(Name.isValidName("A " + "B".repeat(126)));
    }

    @Test
    public void equals() {
        Name name = new Name("Valid Name");

        // same values -> returns true
        assertTrue(name.equals(new Name("Valid Name")));

        // same object -> returns true
        assertTrue(name.equals(name));

        // null -> returns false
        assertFalse(name.equals(null));

        // different types -> returns false
        assertFalse(name.equals(5.0f));

        // different values -> returns false
        assertFalse(name.equals(new Name("Other Valid Name")));
    }
}
