package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

public class AddressTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Address(null));
    }

    @Test
    public void constructor_invalidAddress_throwsIllegalArgumentException() {
        String invalidAddress = "";
        assertThrows(IllegalArgumentException.class, () -> new Address(invalidAddress));
    }

    @Test
    public void isValidAddress() {
        // null address
        assertThrows(NullPointerException.class, () -> Address.isValidAddress(null));

        // invalid addresses
        assertFalse(Address.isValidAddress("")); // empty string
        assertFalse(Address.isValidAddress(" ")); // spaces only

        // valid addresses
        assertTrue(Address.isValidAddress("Blk 456, Den Road, #01-355"));
        assertTrue(Address.isValidAddress("-")); // one character
        assertTrue(Address.isValidAddress("Leng Inc; 1234 Market St; San Francisco CA 2349879; USA")); // long address
    }

    @Test
    public void isBlankAddress_unicodeWhitespace_rejectsAllCodePointsAndMixtures() {
        int[] whitespaceCodePoints = {
            0x0009, 0x000a, 0x000b, 0x000c, 0x000d, 0x0020, 0x0085, 0x00a0, 0x1680,
            0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005, 0x2006, 0x2007, 0x2008,
            0x2009, 0x200a, 0x2028, 0x2029, 0x202f, 0x205f, 0x3000
        };
        StringBuilder mixedWhitespace = new StringBuilder();
        for (int codePoint : whitespaceCodePoints) {
            String whitespace = new String(Character.toChars(codePoint));
            assertTrue(Address.isBlankAddress(whitespace));
            assertTrue(Address.isBlankAddress(whitespace.repeat(2)));
            assertFalse(Address.isValidAddress(whitespace));
            assertThrows(IllegalArgumentException.class, Address.MESSAGE_CONSTRAINTS, () -> new Address(whitespace));
            mixedWhitespace.append(whitespace);
        }
        String mixture = mixedWhitespace.toString();
        assertTrue(Address.isBlankAddress(mixture));
        assertFalse(Address.isValidAddress(mixture));
        assertThrows(IllegalArgumentException.class, Address.MESSAGE_CONSTRAINTS, () -> new Address(mixture));
    }

    @Test
    public void isBlankAddress_emptyNullAndNonWhitespace_preservesBoundary() {
        assertTrue(Address.isBlankAddress(""));
        assertThrows(NullPointerException.class, () -> Address.isBlankAddress(null));
        assertFalse(Address.isBlankAddress("Kent Ridge"));
        assertFalse(Address.isBlankAddress(" \u200b\ufeff\u3000"));
    }

    @Test
    public void constructor_nonblankUnicodeAddress_preservesValue() {
        for (String value : List.of("東京都新宿区", "Kent\u3000Ridge", "\u3000Kent Ridge\u2003")) {
            assertEquals(value, new Address(value).value);
        }
    }

    @Test
    public void isValidAddress_existingRestrictions_remainInvalid() {
        for (String value : List.of(" Kent Ridge", "\tKent Ridge", "Kent\nRidge", "Kent\u2028Ridge")) {
            assertFalse(Address.isValidAddress(value));
        }
        assertEquals("Addresses must contain at least one non-whitespace character.", Address.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void equals() {
        Address address = new Address("Valid Address");

        // same values -> returns true
        assertTrue(address.equals(new Address("Valid Address")));

        // same object -> returns true
        assertTrue(address.equals(address));

        // null -> returns false
        assertFalse(address.equals(null));

        // different types -> returns false
        assertFalse(address.equals(5.0f));

        // different values -> returns false
        assertFalse(address.equals(new Address("Other Valid Address")));
    }
}
