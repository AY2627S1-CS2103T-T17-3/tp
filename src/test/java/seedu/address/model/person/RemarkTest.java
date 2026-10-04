package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class RemarkTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Remark(null));
    }

    @Test
    public void emptyRemark_isAllowed() {
        assertEquals("", new Remark("").value);
    }

    @Test
    public void equalsAndHashCode() {
        Remark remark = new Remark("Likes baseball");
        Remark sameRemark = new Remark("Likes baseball");

        assertTrue(remark.equals(remark));
        assertTrue(remark.equals(sameRemark));
        assertEquals(remark.hashCode(), sameRemark.hashCode());
        assertFalse(remark.equals(null));
        assertFalse(remark.equals("Likes baseball"));
        assertFalse(remark.equals(new Remark("Likes tennis")));
    }

    @Test
    public void toString_returnsValue() {
        assertEquals("Likes baseball", new Remark("Likes baseball").toString());
    }
}
