package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import seedu.address.model.person.Person;

/**
 * A CSV record's original number and either its validated member or its rejection reason.
 */
public final class CsvMemberRecord {
    private final long recordNumber;
    private final Optional<Person> member;
    private final String rejectionReason;

    private CsvMemberRecord(long recordNumber, Optional<Person> member, String rejectionReason) {
        this.recordNumber = recordNumber;
        this.member = requireNonNull(member);
        this.rejectionReason = requireNonNull(rejectionReason);
        assert recordNumber >= 2 : "Member records follow the header";
        assert member.isPresent() == rejectionReason.isEmpty() : "A record has either a member or a rejection";
    }

    static CsvMemberRecord accepted(long recordNumber, Person member) {
        return new CsvMemberRecord(recordNumber, Optional.of(member), "");
    }

    static CsvMemberRecord rejected(long recordNumber, String reason) {
        return new CsvMemberRecord(recordNumber, Optional.empty(), reason);
    }

    public long getRecordNumber() {
        return recordNumber;
    }

    public Optional<Person> getMember() {
        return member;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}
