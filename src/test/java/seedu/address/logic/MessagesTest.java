package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

public class MessagesTest {
    @Test
    public void formatProvidedDetails_tagsSortedByName() {
        Person person = new PersonBuilder().withoutName().withoutPhone().withoutAddress()
                .withTags("volunteer", "exco", "123", "ay2627").build();

        assertEquals("Email: amy@u.nus.edu; Tags: [123][ay2627][exco][volunteer]",
                Messages.formatProvidedDetails(person));
    }

    @Test
    public void formatProvidedDetails_noTags_omitsTags() {
        Person person = new PersonBuilder().withoutName().withoutPhone().withoutAddress().withTags().build();

        assertEquals("Email: amy@u.nus.edu", Messages.formatProvidedDetails(person));
    }
}
