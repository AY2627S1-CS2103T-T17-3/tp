package seedu.address.model.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;

public class SampleDataUtilTest {

    @Test
    public void getSampleAddressBook_sameNameDifferentEmails_containsBothMembers() {
        Name name = new Name("Morgan Lee");
        List<Person> morganLees = SampleDataUtil.getSampleAddressBook().getPersonList().stream()
                .filter(person -> person.getName().equals(name))
                .toList();

        assertEquals(2, morganLees.size());
        assertEquals(Set.of(new Email("morgan.one@example.com"), new Email("morgan.two@example.com")),
                Set.copyOf(morganLees.stream().map(Person::getEmail).toList()));
        assertEquals(List.of(new Phone("91234567"), new Phone("91234567")),
                morganLees.stream().map(Person::getPhone).toList());
    }
}
