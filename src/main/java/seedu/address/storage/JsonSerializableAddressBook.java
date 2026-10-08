package seedu.address.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.model.AddressBook;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.Person;

/**
 * An Immutable AddressBook that is serializable to JSON format.
 */
@JsonRootName(value = "addressbook")
class JsonSerializableAddressBook {

    public static final String MESSAGE_DUPLICATE_PERSON =
            "Record %1$d: email \"%2$s\" duplicates record %3$d.";

    private final List<JsonAdaptedPerson> persons = new ArrayList<>();

    /**
     * Constructs a {@code JsonSerializableAddressBook} with the given persons.
     */
    @JsonCreator
    public JsonSerializableAddressBook(@JsonProperty("persons") List<JsonAdaptedPerson> persons) {
        this.persons.addAll(persons);
    }

    /**
     * Converts a given {@code ReadOnlyAddressBook} into this class for Jackson use.
     *
     * @param source future changes to this will not affect the created {@code JsonSerializableAddressBook}.
     */
    public JsonSerializableAddressBook(ReadOnlyAddressBook source) {
        persons.addAll(source.getPersonList().stream().map(JsonAdaptedPerson::new).collect(Collectors.toList()));
    }

    /**
     * Converts this address book into the model's {@code AddressBook} object.
     *
     * @throws IllegalValueException if there were any data constraints violated.
     */
    public AddressBook toModelType() throws IllegalValueException {
        AddressBook addressBook = new AddressBook();
        for (int i = 0; i < persons.size(); i++) {
            JsonAdaptedPerson jsonAdaptedPerson = persons.get(i);
            Person person;
            try {
                if (jsonAdaptedPerson == null) {
                    throw new IllegalValueException("Member record must not be null.");
                }
                person = jsonAdaptedPerson.toModelType();
            } catch (IllegalValueException e) {
                throw new IllegalValueException("Record " + (i + 1) + ": " + e.getMessage());
            }
            if (addressBook.hasPerson(person)) {
                Person existingPerson = addressBook.getPersonList().stream()
                        .filter(person::isSamePerson).findFirst().orElseThrow();
                int existingRecord = addressBook.getPersonList().indexOf(existingPerson) + 1;
                throw new IllegalValueException(String.format(MESSAGE_DUPLICATE_PERSON,
                        i + 1, person.getEmail(), existingRecord));
            }
            addressBook.addPerson(person);
        }
        return addressBook;
    }

}
