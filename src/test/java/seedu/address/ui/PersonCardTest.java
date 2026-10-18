package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.TypicalPersons.ALICE;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.control.Label;
import seedu.address.model.person.Person;
import seedu.address.model.person.Status;
import seedu.address.testutil.JavaFxTestUtil;

public class PersonCardTest {

    @BeforeAll
    public static void setUpJavaFx() throws InterruptedException {
        JavaFxTestUtil.initialize();
    }

    @Test
    public void constructor_displaysStatusBadge() throws Exception {
        Person archived = new Person(ALICE.getName(), ALICE.getPhone(), ALICE.getEmail(), ALICE.getAddress(),
                Status.ARCHIVED, ALICE.getTags());

        assertEquals(Status.ACTIVE.badgeName, displayedStatus(ALICE));
        assertEquals("ARCHIVED", displayedStatus(archived));
    }

    private String displayedStatus(Person person) throws Exception {
        return JavaFxTestUtil.runAndWait(() -> {
            PersonCard card = new PersonCard(person, 1);
            Label status = (Label) card.getRoot().lookup("#status");
            return status.getText();
        });
    }
}
