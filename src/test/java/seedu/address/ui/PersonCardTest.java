package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.TypicalPersons.ALICE;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
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
            for (String field : new String[] {"name", "phone", "address", "email", "tags", "statusStrip"}) {
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
    public void constructor_statusStripStaysAtLeftAndSpansCardAtDifferentWidths() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            String longEmail = "a".repeat(64) + "@u.nus.edu";
            String[][] tagSets = { {}, {"publicity"}, {"publicity", "logistics", "w".repeat(30)} };
            for (Status status : Status.values()) {
                for (String[] tagSet : tagSets) {
                    Person named = new PersonBuilder(ALICE).withName("Status Test").withEmail(longEmail)
                            .withStatus(status).withTags(tagSet).build();
                    Person unnamed = new Person(Optional.empty(), named.getPhone(), named.getEmail(),
                            named.getAddress(), status, named.getTags());
                    for (Person person : List.of(named, unnamed)) {
                        for (int width : new int[] {340, 900}) {
                            Region card = new PersonCard(person, 1).getRoot();
                            StackPane container = new StackPane(card);
                            Scene scene = new Scene(container, width, 600);
                            scene.getStylesheets().add(getClass().getResource("/view/DarkTheme.css").toExternalForm());
                            container.applyCss();
                            container.layout();

                            Region strip = (Region) card.lookup("#statusStrip");
                            FlowPane tags = (FlowPane) card.lookup("#tags");
                            assertNull(card.lookup("#status"));
                            assertEquals(0, strip.getBoundsInParent().getMinX(), 0.01);
                            assertEquals(4, strip.getWidth(), 0.01);
                            assertEquals(card.getHeight(), strip.getHeight(), 0.01);
                            assertEquals("Status: " + status.badgeName, strip.getAccessibleText());
                            assertEquals(status == Status.ACTIVE ? Color.web("#238558") : Color.web("#f4c542"),
                                    strip.getBackground().getFills().getFirst().getFill());
                            assertEquals(tagSet.length, tags.getChildren().size());
                            assertTrue(tags.getBoundsInParent().getMaxX() <= card.getWidth());
                            for (Node tag : tags.getChildren()) {
                                assertTrue(tag.getBoundsInParent().getMaxX() <= tags.getWidth() + 0.01);
                            }
                            if (tagSet.length > 1 && width == 340) {
                                assertTrue(tags.getChildren().stream()
                                        .anyMatch(tag -> tag.getBoundsInParent().getMinY()
                                                > tags.getChildren().getFirst().getBoundsInParent().getMinY()),
                                        "Multiple tags should wrap in a narrow card");
                            }
                            if (person.getName().isEmpty()) {
                                assertEquals(longEmail, ((TextField) card.lookup("#name")).getText());
                            }
                        }
                    }
                }
            }
            return null;
        });
    }

    @Test
    public void constructor_addressBackgroundIsTransparent() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            Region card = new PersonCard(ALICE, 1).getRoot();
            StackPane container = new StackPane(card);
            Scene scene = new Scene(container, 340, 600);
            scene.getStylesheets().add(getClass().getResource("/view/DarkTheme.css").toExternalForm());
            container.applyCss();
            container.layout();

            TextArea address = (TextArea) card.lookup("#address");
            for (String selector : new String[] {".scroll-pane", ".viewport", ".content"}) {
                Region region = (Region) address.lookup(selector);
                assertEquals(Color.TRANSPARENT, region.getBackground().getFills().getFirst().getFill(),
                        selector + " should not paint a box behind the address");
            }
            return null;
        });
    }

    @Test
    public void listCell_cardFitsViewportAndAddressHeightMatchesContent() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            String longEmail = "a".repeat(64) + "@u.nus.edu";
            String longAddress = "Block 123, Long Road, near the society office, ".repeat(4);
            for (int width : new int[] {340, 900}) {
                for (String addressText : new String[] {"Short address", longAddress}) {
                    Person named = new PersonBuilder(ALICE).withEmail(longEmail).withAddress(addressText).build();
                    Person unnamed = new Person(Optional.empty(), named.getPhone(), named.getEmail(),
                            named.getAddress(), Status.ACTIVE, named.getTags());
                    Region panel = new PersonListPanel(FXCollections.observableArrayList(unnamed)).getRoot();
                    StackPane container = new StackPane(panel);
                    Scene scene = new Scene(container, width, 600);
                    scene.getStylesheets().add(getClass().getResource("/view/DarkTheme.css").toExternalForm());
                    container.applyCss();
                    container.layout();
                    container.layout();

                    ListView<?> list = (ListView<?>) panel.lookup("#personListView");
                    ListCell<?> cell = list.lookupAll(".list-cell").stream()
                            .map(node -> (ListCell<?>) node)
                            .filter(candidate -> candidate.getItem() != null)
                            .findFirst().orElseThrow();
                    Region card = (Region) cell.getGraphic();
                    TextArea address = (TextArea) card.lookup("#address");
                    TextField phone = (TextField) card.lookup("#phone");
                    Node addressTextNode = address.lookup(".text");
                    Node phoneText = phone.lookup(".text");
                    double addressTextX = addressTextNode.localToScene(addressTextNode.getBoundsInLocal()).getMinX();
                    double phoneTextX = phoneText.localToScene(phoneText.getBoundsInLocal()).getMinX();
                    assertEquals(phoneTextX, addressTextX, 2,
                            "Address text should align with the phone and email");
                    assertTrue(address.lookupAll(".scroll-bar").stream()
                            .map(node -> (ScrollBar) node)
                            .filter(bar -> bar.getOrientation() == Orientation.VERTICAL)
                            .noneMatch(Node::isVisible), "Address should not show a scrollbar thumb");
                    assertTrue(card.getWidth() <= cell.getWidth(),
                            "Card width " + card.getWidth() + " exceeds cell width " + cell.getWidth());
                    assertTrue(card.getWidth() <= 600, "Card width " + card.getWidth() + " exceeds the content cap");
                    assertTrue(list.lookupAll(".scroll-bar").stream()
                            .map(node -> (ScrollBar) node)
                            .filter(bar -> bar.getOrientation() == Orientation.HORIZONTAL)
                            .noneMatch(Node::isVisible), "Card should not create a horizontal scrollbar");
                    assertEquals(addressText, address.getTooltip().getText());
                    if (addressText.equals("Short address")) {
                        assertTrue(address.getHeight() < 30,
                                "Short address field height is " + address.getHeight());
                    } else {
                        assertTrue(address.getHeight() <= 60, "Long address field height is " + address.getHeight());
                        if (width == 340) {
                            assertTrue(address.getHeight() > 30,
                                    "Long address should wrap in a narrow card");
                        }
                    }
                }
            }
            return null;
        });
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
}
