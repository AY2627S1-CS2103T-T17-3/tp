package seedu.address.model.person;

import static java.util.Objects.requireNonNull;

/**
 * A person's optional remark. An empty value means there is no remark.
 */
public class Remark {

    public final String value;

    public Remark(String remark) {
        value = requireNonNull(remark);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof Remark otherRemark && value.equals(otherRemark.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
