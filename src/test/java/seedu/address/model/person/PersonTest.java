package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.VALID_ADDRESS_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_EMAIL_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_NAME_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BOB;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import seedu.address.testutil.PersonBuilder;

public class PersonTest {

    @Test
    public void constructor_optionalDetails_rejectsNullContainersAndRequiredFields() {
        Email email = new Email("alex@u.nus.edu");
        assertThrows(NullPointerException.class, () ->
                new Person(null, Optional.empty(), email, Optional.empty(), Status.ACTIVE, Set.of()));
        assertThrows(NullPointerException.class, () ->
                new Person(Optional.empty(), null, email, Optional.empty(), Status.ACTIVE, Set.of()));
        assertThrows(NullPointerException.class, () ->
                new Person(Optional.empty(), Optional.empty(), email, null, Status.ACTIVE, Set.of()));
        assertThrows(NullPointerException.class, () ->
                new Person(Optional.empty(), Optional.empty(), null, Optional.empty(), Status.ACTIVE, Set.of()));
        assertThrows(NullPointerException.class, () ->
                new Person(Optional.empty(), Optional.empty(), email, Optional.empty(), null, Set.of()));
        assertThrows(NullPointerException.class, () ->
                new Person(Optional.empty(), Optional.empty(), email, Optional.empty(), Status.ACTIVE, null));
        assertThrows(NullPointerException.class, () ->
                new Person(null, new Phone("91234567"), email, new Address("NUS"), Set.of()));
    }

    @Test
    public void equals_absentDetails_comparesValuesWithoutChangingIdentity() {
        Person incomplete = new PersonBuilder().withoutName().withoutPhone().withoutAddress()
                .withStatus(Status.ARCHIVED).build();
        Person copy = new PersonBuilder(incomplete).build();
        assertEquals(incomplete, copy);
        assertEquals(incomplete.hashCode(), copy.hashCode());
        assertEquals(Status.ARCHIVED, copy.getStatus());
        assertNotEquals(incomplete, new PersonBuilder(incomplete).withName("Not provided").build());
        assertNotEquals(incomplete, new PersonBuilder(incomplete).withPhone("123").build());
        assertNotEquals(incomplete, new PersonBuilder(incomplete).withAddress("Not provided").build());
        assertTrue(incomplete.isSamePerson(new PersonBuilder(incomplete).withName("Alex").build()));
        assertFalse(incomplete.isSamePerson(new PersonBuilder(incomplete).withEmail("other@u.nus.edu").build()));
    }

    @Test
    public void asObservableList_modifyList_throwsUnsupportedOperationException() {
        Person person = new PersonBuilder().build();
        assertThrows(UnsupportedOperationException.class, () -> person.getTags().remove(0));
    }

    @Test
    public void isSamePerson() {
        // same object -> returns true
        assertTrue(ALICE.isSamePerson(ALICE));

        // null -> returns false
        assertFalse(ALICE.isSamePerson(null));

        // same name, different email -> returns false
        Person editedAlice = new PersonBuilder(ALICE).withPhone(VALID_PHONE_BOB).withEmail(VALID_EMAIL_BOB)
                .withAddress(VALID_ADDRESS_BOB).withTags(VALID_TAG_HUSBAND).build();
        assertFalse(ALICE.isSamePerson(editedAlice));

        // different name, same email -> returns true
        editedAlice = new PersonBuilder(ALICE).withName(VALID_NAME_BOB).build();
        assertTrue(ALICE.isSamePerson(editedAlice));

        // name differs in case, same email -> returns true
        Person editedBob = new PersonBuilder(BOB).withName(VALID_NAME_BOB.toLowerCase()).build();
        assertTrue(BOB.isSamePerson(editedBob));

        // name has trailing spaces, same email -> returns true
        String nameWithTrailingSpaces = VALID_NAME_BOB + " ";
        editedBob = new PersonBuilder(BOB).withName(nameWithTrailingSpaces).build();
        assertTrue(BOB.isSamePerson(editedBob));
    }

    @Test
    public void equals() {
        // same values -> returns true
        Person aliceCopy = new PersonBuilder(ALICE).build();
        assertTrue(ALICE.equals(aliceCopy));

        // same object -> returns true
        assertTrue(ALICE.equals(ALICE));

        // null -> returns false
        assertFalse(ALICE.equals(null));

        // different type -> returns false
        assertFalse(ALICE.equals(5));

        // different person -> returns false
        assertFalse(ALICE.equals(BOB));

        // different name -> returns false
        Person editedAlice = new PersonBuilder(ALICE).withName(VALID_NAME_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different phone -> returns false
        editedAlice = new PersonBuilder(ALICE).withPhone(VALID_PHONE_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different email -> returns false
        editedAlice = new PersonBuilder(ALICE).withEmail(VALID_EMAIL_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different address -> returns false
        editedAlice = new PersonBuilder(ALICE).withAddress(VALID_ADDRESS_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different tags -> returns false
        editedAlice = new PersonBuilder(ALICE).withTags(VALID_TAG_HUSBAND).build();
        assertFalse(ALICE.equals(editedAlice));

        // different status -> returns false
        editedAlice = new Person(ALICE.getName(), ALICE.getPhone(), ALICE.getEmail(), ALICE.getAddress(),
                Status.ARCHIVED, ALICE.getTags());
        assertFalse(ALICE.equals(editedAlice));
    }

    @Test
    public void hashCode_equalPersons_haveSameHashCode() {
        Person aliceCopy = new PersonBuilder(ALICE).build();
        assertEquals(ALICE.hashCode(), aliceCopy.hashCode());
    }

    @Test
    public void toStringMethod() {
        String expected = Person.class.getCanonicalName() + "{name=" + ALICE.getName().orElseThrow()
                + ", phone=" + ALICE.getPhone().orElseThrow()
                + ", email=" + ALICE.getEmail() + ", address=" + ALICE.getAddress().orElseThrow()
                + ", tags=" + ALICE.getTags()
                + ", status=" + ALICE.getStatus() + "}";
        assertEquals(expected, ALICE.toString());
    }
}
