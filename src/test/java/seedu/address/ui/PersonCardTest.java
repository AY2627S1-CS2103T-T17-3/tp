package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.TypicalPersons.ALICE;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import seedu.address.model.person.Person;
import seedu.address.model.person.Status;
import seedu.address.testutil.JavaFxTestUtil;
import seedu.address.testutil.PersonBuilder;

public class PersonCardTest {

    @BeforeAll
    public static void setUpJavaFx() throws InterruptedException {
        JavaFxTestUtil.initialize();
    }

    @Test
    public void constructor_maximumLengths_fitsNarrowCard() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            Person person = new PersonBuilder(ALICE).withName("W".repeat(127)).withPhone("9".repeat(15))
                    .withAddress("W".repeat(200)).withTags("w".repeat(30), "m".repeat(30), "exco").build();
            Region card = new PersonCard(person, 1).getRoot();
            StackPane container = new StackPane(card);
            Scene scene = new Scene(container, 340, 600);
            scene.getStylesheets().add(getClass().getResource("/view/DarkTheme.css").toExternalForm());
            container.applyCss();
            container.layout();

            assertTrue(card.getWidth() <= scene.getWidth(),
                    "Card width " + card.getWidth() + " exceeds available width " + scene.getWidth());
            for (String field : new String[] {"name", "phone", "address", "email", "tags", "status"}) {
                Region value = (Region) card.lookup("#" + field);
                assertTrue(value.localToScene(value.getBoundsInLocal()).getMinX() >= 0, field);
                assertTrue(value.localToScene(value.getBoundsInLocal()).getMaxX() <= scene.getWidth(), field);
            }
            for (String field : new String[] {"name", "phone", "address", "email"}) {
                Label value = (Label) card.lookup("#" + field);
                assertEquals(value.getText(), value.getTooltip().getText());
            }
            assertTrue(((Label) card.lookup("#address")).isWrapText());
            FlowPane tags = (FlowPane) card.lookup("#tags");
            for (Node child : tags.getChildren()) {
                Label label = (Label) child;
                assertTrue(label.getBoundsInParent().getMaxX() <= tags.getWidth());
                assertEquals(label.getText(), label.getTooltip().getText());
            }
            return null;
        });
    }

    @Test
    public void constructor_manyTags_noLargeGapBeforePhone() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            Person person = new PersonBuilder(ALICE).withTags("exco", "1221212123213", "volunteer",
                    "ay2728", "ay2627", "moretagsgobrrrrr", "testing123", "organisingcommittee",
                    "publicity", "president").build();
            Region card = new PersonCard(person, 1).getRoot();
            StackPane container = new StackPane(card);
            Scene scene = new Scene(container, 400, 600);
            FlowPane tags = (FlowPane) card.lookup("#tags");
            Label phone = (Label) card.lookup("#phone");
            scene.getStylesheets().add(getClass().getResource("/view/DarkTheme.css").toExternalForm());
            card.applyCss();
            container.applyCss();
            container.layout();
            card.applyCss();
            card.layout();
            container.layout();

            Node tagsRow = tags.getParent();
            double tagsBottom = tagsRow.getBoundsInParent().getMaxY();
            double phoneTop = phone.getBoundsInParent().getMinY();
            assertEquals(tagsBottom, phoneTop, 1.0);
            return null;
        });
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
