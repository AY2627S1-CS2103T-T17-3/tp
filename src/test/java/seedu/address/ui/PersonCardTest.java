package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static seedu.address.testutil.TypicalPersons.ALICE;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.FlowPane;
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

    @Test
    public void constructor_allDisplayedDetailsCanBeSelectedAndCopied() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            PersonCard card = new PersonCard(ALICE, 1);
            FlowPane tags = (FlowPane) card.getRoot().lookup("#tags");
            List<TextField> details = List.of(
                    (TextField) card.getRoot().lookup("#id"),
                    (TextField) card.getRoot().lookup("#name"),
                    (TextField) card.getRoot().lookup("#phone"),
                    (TextField) card.getRoot().lookup("#address"),
                    (TextField) card.getRoot().lookup("#email"),
                    (TextField) card.getRoot().lookup("#status"),
                    (TextField) tags.getChildren().getFirst());

            for (TextField detail : details) {
                assertFalse(detail.isEditable());
                detail.selectAll();
                detail.copy();
                assertEquals(detail.getText(), Clipboard.getSystemClipboard().getString());
            }

            TextField name = (TextField) card.getRoot().lookup("#name");
            name.selectRange(0, 5);
            name.copy();
            assertEquals("Alice", Clipboard.getSystemClipboard().getString());
            return null;
        });
    }

    private String displayedStatus(Person person) throws Exception {
        return JavaFxTestUtil.runAndWait(() -> {
            PersonCard card = new PersonCard(person, 1);
            TextField status = (TextField) card.getRoot().lookup("#status");
            return status.getText();
        });
    }
}
