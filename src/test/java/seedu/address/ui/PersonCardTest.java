package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.control.Label;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

/**
 * Tests the details shown on a person's card.
 */
public class PersonCardTest {

    @BeforeAll
    public static void initializeJavaFx() throws InterruptedException {
        CountDownLatch startupComplete = new CountDownLatch(1);
        Platform.startup(startupComplete::countDown);
        assertTrue(startupComplete.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void constructor_displaysRemark() {
        Person person = new PersonBuilder().withRemark("Likes baseball").build();
        PersonCard card = new PersonCard(person, 1);
        Label remarkLabel = (Label) card.getRoot().lookup("#remark");
        assertEquals("Likes baseball", remarkLabel.getText());

        Person withoutRemark = new PersonBuilder().build();
        PersonCard emptyRemarkCard = new PersonCard(withoutRemark, 2);
        Label emptyRemarkLabel = (Label) emptyRemarkCard.getRoot().lookup("#remark");
        assertEquals("", emptyRemarkLabel.getText());
    }
}
