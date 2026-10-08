package seedu.address.model.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class TagTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Tag(null));
    }

    @Test
    public void constructor_invalidTagName_throwsIllegalArgumentException() {
        String invalidTagName = "";
        assertThrows(IllegalArgumentException.class, () -> new Tag(invalidTagName));
    }

    @Test
    public void isValidTagName() {
        // null tag name
        assertThrows(NullPointerException.class, () -> Tag.isValidTagName(null));

        assertFalse(Tag.isValidTagName(""));
        assertFalse(Tag.isValidTagName(" publicity "));
        assertFalse(Tag.isValidTagName("Publicity Team"));
        assertFalse(Tag.isValidTagName("publicity!"));
        assertTrue(Tag.isValidTagName("Publicity"));
        assertTrue(Tag.isValidTagName("PUBLICITY"));
        assertTrue(Tag.isValidTagName("Exco2026"));
    }

    @Test
    public void constructor_mixedCase_storesLowercaseName() {
        Tag tag = new Tag("PuBliCiTy2026");
        assertEquals("publicity2026", tag.tagName);
        assertEquals("[publicity2026]", tag.toString());
    }

    @Test
    public void constructor_turkishDefaultLocale_storesLocaleIndependentLowercaseName() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            assertEquals("publicity", new Tag("PUBLICITY").tagName);
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    public void equalsAndHashCode_caseVariants_sameIdentity() {
        Tag lowercaseTag = new Tag("publicity");
        Tag mixedCaseTag = new Tag("Publicity");
        Tag uppercaseTag = new Tag("PUBLICITY");

        assertEquals(lowercaseTag, mixedCaseTag);
        assertEquals(mixedCaseTag, lowercaseTag);
        assertEquals(mixedCaseTag, uppercaseTag);
        assertEquals(lowercaseTag, uppercaseTag);
        assertEquals(lowercaseTag.hashCode(), mixedCaseTag.hashCode());
        assertEquals(lowercaseTag.hashCode(), uppercaseTag.hashCode());
        assertFalse(lowercaseTag.equals(new Tag("exco")));
        assertFalse(lowercaseTag.equals(null));
        assertFalse(lowercaseTag.equals("publicity"));
    }

    @Test
    public void hashSet_caseVariants_retainsOneTagAndMatchesCaseInsensitively() {
        Set<Tag> tags = new HashSet<>(List.of(new Tag("Publicity"), new Tag("publicity"), new Tag("PUBLICITY")));

        assertEquals(1, tags.size());
        assertEquals("publicity", tags.iterator().next().tagName);
        assertTrue(tags.contains(new Tag("pUbLiCiTy")));
        assertTrue(tags.remove(new Tag("PUBLICITY")));
        assertTrue(tags.isEmpty());
    }

}
