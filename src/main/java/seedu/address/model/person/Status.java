package seedu.address.model.person;

/**
 * Represents a Person's membership status in the address book.
 * The stored value is lowercase, while the badge text is displayed on the person's card.
 */
public enum Status {
    ACTIVE("active", "ACTIVE"),
    ARCHIVED("archived", "FORMER");

    public static final String MESSAGE_CONSTRAINTS = "Status should be either active or archived only.";

    /** Lowercase value used when storing the status. */
    public final String value;

    /** Text displayed in the status badge. */
    public final String badgeName;

    /**
     * Constructs a {@code Status} with its stored value and badge text.
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
