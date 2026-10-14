package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static seedu.address.testutil.TypicalPersons.ALICE;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.control.Label;
import seedu.address.model.person.Person;
import seedu.address.model.person.Status;

public class PersonCardTest {

    private static boolean toolkitStarted;

    @BeforeAll
    public static void setUpJavaFx() {
        assumeTrue(System.getenv("DISPLAY") != null, "JavaFX UI tests need a display");
        Platform.startup(() -> { });
        toolkitStarted = true;
    }

    @AfterAll
    public static void tearDownJavaFx() {
        if (toolkitStarted) {
            Platform.exit();
        }
    }

    @Test
    public void constructor_displaysStatusBadge() throws Exception {
        Person archived = new Person(ALICE.getName(), ALICE.getPhone(), ALICE.getEmail(), ALICE.getAddress(),
                Status.ARCHIVED, ALICE.getTags());

        assertEquals(Status.ACTIVE.badgeName, displayedStatus(ALICE));
        assertEquals(Status.ARCHIVED.badgeName, displayedStatus(archived));
    }

    private String displayedStatus(Person person) throws Exception {
        FutureTask<String> task = new FutureTask<>(() -> {
            PersonCard card = new PersonCard(person, 1);
            Label status = (Label) card.getRoot().lookup("#status");
            return status.getText();
        });
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }
}
