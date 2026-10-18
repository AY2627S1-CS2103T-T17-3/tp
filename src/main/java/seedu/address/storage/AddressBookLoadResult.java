package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.util.List;

import seedu.address.model.ReadOnlyAddressBook;

/**
 * The records recovered from a data file and the errors encountered while loading it.
 */
public class AddressBookLoadResult {

    /** Distinguishes a complete load, rejected records, an unreadable file, and an absent file. */
    public enum Status {
        SUCCESS, PARTIAL, FAILURE, MISSING
    }

    private final Status status;
    private final ReadOnlyAddressBook addressBook;
    private final List<String> errors;

    AddressBookLoadResult(Status status, ReadOnlyAddressBook addressBook, List<String> errors) {
        this.status = requireNonNull(status);
        this.addressBook = requireNonNull(addressBook);
        this.errors = List.copyOf(errors);
    }

    public Status getStatus() {
        return status;
    }

    public ReadOnlyAddressBook getAddressBook() {
        return addressBook;
    }

    public List<String> getErrors() {
        return errors;
    }

    /** Returns whether loading encountered errors that require protecting the original file. */
    public boolean hasErrors() {
        return status == Status.PARTIAL || status == Status.FAILURE;
    }
}
