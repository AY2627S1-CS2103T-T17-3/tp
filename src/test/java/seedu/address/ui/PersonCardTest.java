package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import javafx.scene.layout.VBox;
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
    public void constructor_optionalContactDetails_displaysConsistentLabels() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            for (boolean hasName : new boolean[] {false, true}) {
                for (boolean hasPhone : new boolean[] {false, true}) {
                    for (boolean hasAddress : new boolean[] {false, true}) {
                        PersonBuilder builder = new PersonBuilder().withName("Bernice Yu").withPhone("99272758")
                                .withEmail("BerniceYu@U.NUS.EDU").withAddress("Kent Ridge");
                        if (!hasName) {
                            builder.withoutName();
                        }
                        if (!hasPhone) {
                            builder.withoutPhone();
                        }
                        if (!hasAddress) {
                            builder.withoutAddress();
                        }
                        Region card = new PersonCard(builder.build(), 1).getRoot();
                        assertEquals("1. ", ((Label) card.lookup("#id")).getText());
                        assertLabelText(card, "name", hasName ? "Bernice Yu" : "berniceyu@u.nus.edu");
                        assertLabelText(card, "phone", hasPhone ? "Phone: 99272758" : "Phone: -");
                        assertLabelText(card, "address", hasAddress ? "Address: Kent Ridge" : "Address: -");
                        assertLabelText(card, "email", "Email: berniceyu@u.nus.edu");
                        Label email = (Label) card.lookup("#email");
                        assertEquals(hasName, email.isVisible());
                        assertEquals(hasName, email.isManaged());
                    }
                }
            }
            return null;
        });
    }

    @Test
    public void constructor_maximumLengths_fitsNarrowCard() throws Exception {
        Person person = new PersonBuilder(ALICE).withName("W".repeat(127)).withPhone("9".repeat(15))
                .withEmail("w".repeat(64) + "@u.nus.edu").withAddress("W".repeat(200))
                .withTags("w".repeat(30), "m".repeat(30), "exco").build();
        assertFitsNarrowCard(person);
        assertFitsNarrowCard(new PersonBuilder(person).withoutName().build());
    }

    @Test
    public void constructor_shortCards_alignsNamedAndUnnamedContent() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            Person person = new PersonBuilder(ALICE).withoutPhone().withoutAddress().withTags().build();
            for (int width : new int[] {340, 1556}) {
                Region[] cards = layoutNamedAndUnnamedCards(person, width);
                assertEquals(105, cards[0].getHeight(), 0.01);
                assertEquals(cards[0].getHeight(), cards[1].getHeight(), 0.01);
                assertSharedFieldPositions(cards[0], cards[1]);
            }
            return null;
        });
    }

    @Test
    public void constructor_wrappedDetails_keepsContentTopAligned() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            Person person = new PersonBuilder(ALICE).withAddress("W".repeat(200))
                    .withTags("publicity", "studentambassadors", "volunteer", "committee").build();
            Region[] cards = layoutNamedAndUnnamedCards(person, 340);
            assertSharedFieldPositions(cards[0], cards[1]);
            for (Region card : cards) {
                Label phone = (Label) card.lookup("#phone");
                Label address = (Label) card.lookup("#address");
                assertTrue(address.getHeight() > phone.getHeight());
                assertTrue(card.getHeight() > 105);
                for (String field : new String[] {"name", "tags", "status", "phone", "address", "email"}) {
                    Region value = (Region) card.lookup("#" + field);
                    if (value.isVisible() && value.isManaged()) {
                        assertTrue(relativeTop(card, field) >= 0, field);
                        assertTrue(relativeTop(card, field) + value.getHeight() <= card.getHeight(), field);
                    }
                }
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

    private Region[] layoutNamedAndUnnamedCards(Person person, int width) {
        Region named = new PersonCard(person, 1).getRoot();
        Region unnamed = new PersonCard(new PersonBuilder(person).withoutName().build(), 2).getRoot();
        VBox container = new VBox(named, unnamed);
        Scene scene = new Scene(container, width, 800);
        scene.getStylesheets().add(getClass().getResource("/view/DarkTheme.css").toExternalForm());
        container.applyCss();
        container.layout();
        return new Region[] {named, unnamed};
    }

    private void assertSharedFieldPositions(Region named, Region unnamed) {
        assertEquals(5, relativeTop(named, "name"), 0.01);
        assertEquals(5, relativeTop(unnamed, "name"), 0.01);
        for (String field : new String[] {"name", "tags", "status", "phone", "address"}) {
            assertEquals(relativeTop(named, field), relativeTop(unnamed, field), 0.01, field);
        }
    }

    private double relativeTop(Region card, String field) {
        Node value = card.lookup("#" + field);
        return value.localToScene(value.getBoundsInLocal()).getMinY()
                - card.localToScene(card.getBoundsInLocal()).getMinY();
    }

    private void assertLabelText(Region card, String field, String expectedText) {
        Label label = (Label) card.lookup("#" + field);
        assertEquals(expectedText, label.getText(), field);
        assertEquals(expectedText, label.getTooltip().getText(), field);
    }

    private void assertFitsNarrowCard(Person person) throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
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
                if (value.isVisible() && value.isManaged()) {
                    assertTrue(value.localToScene(value.getBoundsInLocal()).getMinX() >= 0, field);
                    assertTrue(value.localToScene(value.getBoundsInLocal()).getMaxX() <= scene.getWidth(), field);
                }
            }
            assertLabelText(card, "name", person.getName().isPresent() ? "W".repeat(127)
                    : "w".repeat(64) + "@u.nus.edu");
            assertLabelText(card, "phone", "Phone: " + "9".repeat(15));
            assertLabelText(card, "address", "Address: " + "W".repeat(200));
            assertLabelText(card, "email", "Email: " + "w".repeat(64) + "@u.nus.edu");
            assertTrue(((Label) card.lookup("#address")).isWrapText());
            assertFalse(((Label) card.lookup("#email")).isWrapText());
            assertFalse(((Label) card.lookup("#name")).isWrapText());
            assertEquals(relativeTop(card, "name"), relativeTop(card, "id"), 0.01);
            FlowPane tags = (FlowPane) card.lookup("#tags");
            for (Node child : tags.getChildren()) {
                Label label = (Label) child;
                assertTrue(label.getBoundsInParent().getMaxX() <= tags.getWidth());
                assertEquals(label.getText(), label.getTooltip().getText());
            }
            return null;
        });
    }

    private String displayedStatus(Person person) throws Exception {
        return JavaFxTestUtil.runAndWait(() -> {
            PersonCard card = new PersonCard(person, 1);
            Label status = (Label) card.getRoot().lookup("#status");
            return status.getText();
        });
    }
}
