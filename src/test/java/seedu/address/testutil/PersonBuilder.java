package seedu.address.testutil;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import seedu.address.model.person.Address;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Status;
import seedu.address.model.tag.Tag;
import seedu.address.model.util.SampleDataUtil;

/**
 * A utility class to help with building Person objects.
 */
public class PersonBuilder {

    public static final String DEFAULT_NAME = "Amy Bee";
    public static final String DEFAULT_PHONE = "85355255";
    public static final String DEFAULT_EMAIL = "amy@u.nus.edu";
    public static final String DEFAULT_ADDRESS = "123, Jurong West Ave 6, #08-111";

    private Optional<Name> name;
    private Optional<Phone> phone;
    private Email email;
    private Optional<Address> address;
    private Status status;
    private Set<Tag> tags;

    /**
     * Creates a {@code PersonBuilder} with the default details.
     */
    public PersonBuilder() {
        name = Optional.of(new Name(DEFAULT_NAME));
        phone = Optional.of(new Phone(DEFAULT_PHONE));
        email = new Email(DEFAULT_EMAIL);
        address = Optional.of(new Address(DEFAULT_ADDRESS));
        status = Status.ACTIVE;
        tags = new HashSet<>();
    }

    /**
     * Initializes the PersonBuilder with the data of {@code personToCopy}.
     */
    public PersonBuilder(Person personToCopy) {
        name = personToCopy.getName();
        phone = personToCopy.getPhone();
        email = personToCopy.getEmail();
        address = personToCopy.getAddress();
        status = personToCopy.getStatus();
        tags = new HashSet<>(personToCopy.getTags());
    }

    /**
     * Sets the {@code Name} of the {@code Person} that we are building.
     */
    public PersonBuilder withName(String name) {
        this.name = Optional.of(new Name(name));
        return this;
    }

    /**
     * Parses the {@code tags} into a {@code Set<Tag>} and sets it to the {@code Person} that we are building.
     */
    public PersonBuilder withTags(String ... tags) {
        this.tags = SampleDataUtil.getTagSet(tags);
        return this;
    }

    /**
     * Sets the {@code Address} of the {@code Person} that we are building.
     */
    public PersonBuilder withAddress(String address) {
        this.address = Optional.of(new Address(address));
        return this;
    }

    /**
     * Sets the {@code Phone} of the {@code Person} that we are building.
     */
    public PersonBuilder withPhone(String phone) {
        this.phone = Optional.of(new Phone(phone));
        return this;
    }

    /**
     * Sets the {@code Email} of the {@code Person} that we are building.
     */
    public PersonBuilder withEmail(String email) {
        this.email = new Email(email);
        return this;
    }

    /**
     * Omits the member's name.
     */
    public PersonBuilder withoutName() {
        name = Optional.empty();
        return this;
    }

    /**
     * Omits the member's phone number.
     */
    public PersonBuilder withoutPhone() {
        phone = Optional.empty();
        return this;
    }

    /**
     * Omits the member's address.
     */
    public PersonBuilder withoutAddress() {
        address = Optional.empty();
        return this;
    }

    /**
     * Sets the member's status.
     */
    public PersonBuilder withStatus(Status status) {
        this.status = status;
        return this;
    }

    /**
     * Builds a member with the specified details.
     */
    public Person build() {
        return new Person(name, phone, email, address, status, tags);
    }

}
