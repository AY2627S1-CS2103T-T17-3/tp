package seedu.address.model.person;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.tag.Tag;

/**
 * Represents a Person in the address book.
 * Guarantees: email, status, tags and optional containers are not null; supplied values are validated and immutable.
 */
public class Person {

    // Contact fields (email determines identity)
    private final Optional<Name> name;
    private final Optional<Phone> phone;
    private final Email email;

    // Data fields
    private final Optional<Address> address;
    private final Status status;
    private final Set<Tag> tags = new HashSet<>();

    /**
     * Creates an active person with complete contact details. Every argument must be non-null.
     */
    public Person(Name name, Phone phone, Email email, Address address, Set<Tag> tags) {
        this(name, phone, email, address, Status.ACTIVE, tags);
    }

    /**
     * Creates a person with the given status and details. Every field must be present and not null.
     */
    public Person(Name name, Phone phone, Email email, Address address, Status status, Set<Tag> tags) {
        this(Optional.of(name), Optional.of(phone), email, Optional.of(address), status, tags);
    }

    /**
     * Creates a person whose name, phone or address may be absent.
     * Every argument, including each optional container, must be non-null.
     */
    public Person(Optional<Name> name, Optional<Phone> phone, Email email, Optional<Address> address,
            Status status, Set<Tag> tags) {
        requireAllNonNull(name, phone, email, address, status, tags);
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.status = status;
        this.tags.addAll(tags);
    }

    public Optional<Name> getName() {
        return name;
    }

    public Optional<Phone> getPhone() {
        return phone;
    }

    public Email getEmail() {
        return email;
    }

    public Optional<Address> getAddress() {
        return address;
    }

    public Status getStatus() {
        return status;
    }

    /**
     * Returns an immutable tag set, which throws {@code UnsupportedOperationException}
     * if modification is attempted.
     */
    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    /**
     * Returns true if both persons have the same canonical email.
     * This defines a weaker notion of equality between two persons.
     */
    public boolean isSamePerson(Person otherPerson) {
        if (otherPerson == this) {
            return true;
        }

        return otherPerson != null
                && otherPerson.getEmail().equals(getEmail());
    }

    /**
     * Returns true if both persons have the same identity and data fields.
     * This defines a stronger notion of equality between two persons.
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Person otherPerson)) {
            return false;
        }

        return name.equals(otherPerson.name)
                && phone.equals(otherPerson.phone)
                && email.equals(otherPerson.email)
                && address.equals(otherPerson.address)
                && tags.equals(otherPerson.tags)
                && status.equals(otherPerson.status);
    }

    @Override
    public int hashCode() {
        // use this method for custom fields hashing instead of implementing your own
        return Objects.hash(name, phone, email, address, status, tags);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("name", name.orElse(null))
                .add("phone", phone.orElse(null))
                .add("email", email)
                .add("address", address.orElse(null))
                .add("tags", tags)
                .add("status", status)
                .toString();
    }

}
