package seedu.address.model.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.model.tag.Tag;

public class SampleDataUtilTest {

    @Test
    public void getSampleAddressBook_validMembers_keepsAllUniqueEmails() {
        List<Person> members = SampleDataUtil.getSampleAddressBook().getPersonList();

        assertEquals(8, members.size());
        assertEquals(Arrays.asList(SampleDataUtil.getSamplePersons()), members);
        assertEquals(members.size(), members.stream().map(Person::getEmail).distinct().count());
    }

    @Test
    public void getSamplePersons_societyTags_containsRolesAndVariedTagCounts() {
        List<Person> members = Arrays.asList(SampleDataUtil.getSamplePersons());
        Set<Tag> tags = members.stream().flatMap(person -> person.getTags().stream()).collect(Collectors.toSet());

        assertEquals(SampleDataUtil.getTagSet("President", "Secretary", "Exco", "Publicity", "Logistics", "Volunteer"),
                tags);
        assertTrue(members.stream().anyMatch(person -> person.getTags().isEmpty()));
        assertTrue(members.stream().anyMatch(person -> person.getTags().size() == 1));
        assertTrue(members.stream().anyMatch(person -> person.getTags().size() > 1));
    }

    @Test
    public void getSampleAddressBook_sameNameDifferentEmails_containsBothMembers() {
        Name name = new Name("Morgan Lee");
        List<Person> morganLees = SampleDataUtil.getSampleAddressBook().getPersonList().stream()
                .filter(person -> person.getName().filter(name::equals).isPresent())
                .toList();

        assertEquals(2, morganLees.size());
        assertEquals(Set.of(new Email("morgan.one@u.nus.edu"), new Email("morgan.two@u.nus.edu")),
                Set.copyOf(morganLees.stream().map(Person::getEmail).toList()));
        assertEquals(List.of(new Phone("91234567"), new Phone("91234567")),
                morganLees.stream().map(person -> person.getPhone().orElseThrow()).toList());
    }
}
