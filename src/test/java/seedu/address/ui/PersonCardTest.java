package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.TypicalPersons.ALICE;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.Clipboard;
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
                TextInputControl value = (TextInputControl) card.lookup("#" + field);
                assertEquals(value.getText(), value.getTooltip().getText());
            }
            assertTrue(((TextArea) card.lookup("#address")).isWrapText());
            FlowPane tags = (FlowPane) card.lookup("#tags");
            for (Node child : tags.getChildren()) {
                TextField tag = (TextField) child;
                assertTrue(tag.getBoundsInParent().getMaxX() <= tags.getWidth());
                assertEquals(tag.getText(), tag.getTooltip().getText());
            }
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

    @Test
    public void constructor_allDisplayedDetailsCanBeSelectedAndCopied() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            PersonCard card = new PersonCard(ALICE, 1);
            FlowPane tags = (FlowPane) card.getRoot().lookup("#tags");
            List<TextInputControl> details = List.of(
                    (TextField) card.getRoot().lookup("#id"),
                    (TextField) card.getRoot().lookup("#name"),
                    (TextField) card.getRoot().lookup("#phone"),
                    (TextArea) card.getRoot().lookup("#address"),
                    (TextField) card.getRoot().lookup("#email"),
                    (TextField) card.getRoot().lookup("#status"),
                    (TextField) tags.getChildren().getFirst());

            for (TextInputControl detail : details) {
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
