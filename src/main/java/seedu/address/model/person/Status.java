package seedu.address.model.person;

/**
 * Represents a Person's membership status in the address book.
 * The stored value is lowercase, while the display name describes the person's status.
 */
public enum Status {
    ACTIVE("active", "ACTIVE"),
    ARCHIVED("archived", "ARCHIVED");

    public static final String MESSAGE_CONSTRAINTS = "Status should be either active or archived only.";

    /** Lowercase value used when storing the status. */
    public final String value;

    /** Human-readable name for the status. */
    public final String badgeName;

    /**
     * Constructs a {@code Status} with its stored value and display name.
     */
    Status(String value, String badgeName) {
        this.value = value;
        this.badgeName = badgeName;
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Returns true if the given string is a valid stored status value.
     */
    public static boolean isValidStatus(String test) {
        return test.equals("active") || test.equals("archived");
    }
}
