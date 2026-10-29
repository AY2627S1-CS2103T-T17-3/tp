package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import seedu.address.model.person.Person;
import seedu.address.model.person.Status;
import seedu.address.testutil.JavaFxTestUtil;
import seedu.address.testutil.PersonBuilder;

/** Tests contact wrapping inside the actual scrolling member list. */
public class PersonListPanelTest {

    @BeforeAll
    public static void setUpJavaFx() throws InterruptedException {
        JavaFxTestUtil.initialize();
    }

    @Test
    public void layout_longContacts_wrapsOnlyAddressDuringResizing() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            for (boolean hasName : new boolean[] {true, false}) {
                Person person = createLongContact(hasName, Status.ARCHIVED);
                ObservableList<Person> persons = FXCollections.observableArrayList(person);
                StackPane container = createList(persons, 340, 300);
                ListView<?> list = (ListView<?>) container.lookup("#personListView");
                for (int width : new int[] {340, 1556, 340}) {
                    container.resize(width, 300);
                    layout(container);
                    ListCell<?> cell = findCell(list, 0);
                    Region card = (Region) cell.getGraphic();
                    assertCardFits(list, cell);
                    assertRenderedText(card, "address", "Address: " + "W".repeat(200));
                    String emailField = hasName ? "email" : "name";
                    String emailText = hasName ? "Email: " + person.getEmail().value : person.getEmail().value;
                    assertSingleLineEmail(card, emailField, emailText, width == 340);
                    if (width == 340) {
                        Label address = (Label) card.lookup("#address");
                        Label phone = (Label) card.lookup("#phone");
                        assertTrue(address.getHeight() > phone.getHeight());
                    }
                    assertEquals("ARCHIVED", ((Label) card.lookup("#status")).getText());
                }
            }
            return null;
        });
    }

    @Test
    public void layout_scrollingAndReplacingMembers_keepsCardsWithinViewport() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            ObservableList<Person> persons = FXCollections.observableArrayList();
            Person template = createLongContact(true, Status.ACTIVE);
            IntStream.range(0, 30).forEach(index -> persons.add(new PersonBuilder(template)
                    .withEmail("member" + index + "@u.nus.edu").build()));
            StackPane container = createList(persons, 340, 300);
            ListView<?> list = (ListView<?>) container.lookup("#personListView");
            assertTrue(findScrollBar(list, Orientation.VERTICAL).isVisible());
            for (int index : new int[] {29, 0}) {
                list.scrollTo(index);
                layout(container);
                ListCell<?> cell = findCell(list, index);
                assertCardFits(list, cell);
                assertRenderedText((Region) cell.getGraphic(), "address", "Address: " + "W".repeat(200));
            }

            persons.setAll(createLongContact(false, Status.ACTIVE));
            layout(container);
            assertSingleLineEmail((Region) findCell(list, 0).getGraphic(), "name",
                    "w".repeat(64) + "@u.nus.edu", true);
            persons.set(0, createLongContact(true, Status.ACTIVE));
            layout(container);
            ListCell<?> namedCell = findCell(list, 0);
            assertCardFits(list, namedCell);
            assertSingleLineEmail((Region) namedCell.getGraphic(), "email",
                    "Email: " + "w".repeat(64) + "@u.nus.edu", true);

            persons.setAll(new PersonBuilder().withoutPhone().withoutAddress().build());
            layout(container);
            assertFalse(findScrollBar(list, Orientation.VERTICAL).isVisible());
            assertCardFits(list, findCell(list, 0));
            return null;
        });
    }

    @Test
    public void layout_cardTallerThanViewport_keepsItsEndAndFollowingMemberReachable() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            String[] tags = IntStream.range(0, 60).mapToObj(index -> "committee" + index).toArray(String[]::new);
            Person tallPerson = new PersonBuilder(createLongContact(true, Status.ACTIVE)).withTags(tags).build();
            Person followingPerson = new PersonBuilder().withEmail("following@u.nus.edu").build();
            StackPane container = createList(FXCollections.observableArrayList(tallPerson, followingPerson), 340, 300);
            ListView<?> list = (ListView<?>) container.lookup("#personListView");
            list.getSelectionModel().select(0);
            layout(container);
            ListCell<?> firstCell = findCell(list, 0);
            assertTrue(firstCell.getHeight() > list.getHeight());
            assertCardFits(list, firstCell);
            assertRenderedText((Region) firstCell.getGraphic(), "address", "Address: " + "W".repeat(200));
            ScrollBar verticalBar = findScrollBar(list, Orientation.VERTICAL);
            verticalBar.setValue(verticalBar.getMax());
            layout(container);
            ListCell<?> followingCell = findCell(list, 1);
            assertCardFits(list, followingCell);
            Node viewport = list.lookup(".clipped-container");
            Bounds viewportBounds = viewport.localToScene(viewport.getLayoutBounds());
            Label lastContact = (Label) findCell(list, 0).getGraphic().lookup("#email");
            Bounds contactBounds = lastContact.localToScene(lastContact.getLayoutBounds());
            assertTrue(contactBounds.getMinY() >= viewportBounds.getMinY() - 1,
                    "Contact " + contactBounds + " must be inside viewport " + viewportBounds);
            assertTrue(contactBounds.getMaxY() <= viewportBounds.getMaxY() + 1,
                    "Contact " + contactBounds + " must be inside viewport " + viewportBounds);
            Bounds followingBounds = followingCell.localToScene(followingCell.getLayoutBounds());
            assertTrue(followingBounds.getMinY() < viewportBounds.getMaxY());
            assertTrue(followingBounds.getMaxY() > viewportBounds.getMinY());
            return null;
        });
    }

    @Test
    public void update_wrappedTags_reflowsBeforeScrollingAndAfterCellReuse() throws Exception {
        for (int width : new int[] {340, 760}) {
            for (boolean hasName : new boolean[] {false, true}) {
                PersonBuilder builder = new PersonBuilder().withoutPhone().withoutAddress()
                        .withStatus(hasName ? Status.ACTIVE : Status.ARCHIVED);
                Person person = (hasName ? builder : builder.withoutName()).build();
                ObservableList<Person> persons = FXCollections.observableArrayList(person);
                IntStream.range(1, 20).forEach(index -> persons.add(new PersonBuilder()
                        .withEmail("following" + index + "@u.nus.edu").build()));
                Stage stage = JavaFxTestUtil.runAndWait(() -> {
                    StackPane container = new StackPane(new PersonListPanel(persons).getRoot());
                    Scene scene = new Scene(container, width, 300);
                    scene.getStylesheets().add(getClass().getResource("/view/DarkTheme.css").toExternalForm());
                    Stage window = new Stage();
                    window.setScene(scene);
                    window.show();
                    ListView<?> list = (ListView<?>) container.lookup("#personListView");
                    list.getSelectionModel().select(0);
                    return window;
                });
                try {
                    double firstHeight = replaceTagsAndAssertLayout(stage, persons, person, 1);
                    for (int count = 2; count <= 8; count++) {
                        double height = replaceTagsAndAssertLayout(stage, persons, person, count);
                        if (count == 8) {
                            assertTrue(height > firstHeight, "Multiple tag rows must increase the card height");
                        }
                    }
                    JavaFxTestUtil.runAndWait(() -> {
                        ListView<?> list = (ListView<?>) stage.getScene().lookup("#personListView");
                        list.scrollTo(19);
                        return null;
                    });
                    JavaFxTestUtil.runAfterLayoutPulse(stage.getScene(), () -> {
                        ListView<?> list = (ListView<?>) stage.getScene().lookup("#personListView");
                        assertCardFits(list, findCell(list, 19));
                        list.scrollTo(0);
                        return null;
                    });
                    JavaFxTestUtil.runAfterLayoutPulse(stage.getScene(), () -> {
                        ListView<?> list = (ListView<?>) stage.getScene().lookup("#personListView");
                        assertTagsAbovePhone((Region) findCell(list, 0).getGraphic());
                        list.getSelectionModel().select(0);
                        return null;
                    });
                    double windowWidth = JavaFxTestUtil.runAndWait(() -> {
                        double currentWidth = stage.getWidth();
                        stage.setWidth(currentWidth + 400);
                        return currentWidth;
                    });
                    JavaFxTestUtil.runAfterLayoutPulse(stage.getScene(), () -> {
                        ListView<?> list = (ListView<?>) stage.getScene().lookup("#personListView");
                        assertTagsAbovePhone((Region) findCell(list, 0).getGraphic());
                        stage.setWidth(windowWidth);
                        return null;
                    });
                    JavaFxTestUtil.runAfterLayoutPulse(stage.getScene(), () -> {
                        ListView<?> list = (ListView<?>) stage.getScene().lookup("#personListView");
                        assertTagsAbovePhone((Region) findCell(list, 0).getGraphic());
                        return null;
                    });
                    for (int count = 7; count >= 0; count--) {
                        double height = replaceTagsAndAssertLayout(stage, persons, person, count);
                        if (count == 0) {
                            assertEquals(firstHeight, height, 1, "Removing tags must restore the original card height");
                        }
                    }
                } finally {
                    JavaFxTestUtil.runAndWait(() -> {
                        stage.close();
                        return null;
                    });
                }
            }
        }
    }

    private double replaceTagsAndAssertLayout(Stage stage, ObservableList<Person> persons, Person person, int count)
            throws Exception {
        String[] tags = IntStream.range(0, count)
                .mapToObj(index -> index == 0 ? "exco" : "w".repeat(29) + (char) ('a' + index))
                .toArray(String[]::new);
        JavaFxTestUtil.runAndWait(() -> {
            persons.set(0, new PersonBuilder(person).withTags(tags).build());
            return null;
        });
        return JavaFxTestUtil.runAfterLayoutPulse(stage.getScene(), () -> {
            ListView<?> list = (ListView<?>) stage.getScene().lookup("#personListView");
            ListCell<?> cell = findCell(list, 0);
            assertCardFits(list, cell);
            Region card = (Region) cell.getGraphic();
            assertTagsAbovePhone(card);
            assertEquals(count, ((FlowPane) card.lookup("#tags")).getChildren().size());
            return card.getHeight();
        });
    }

    private void assertTagsAbovePhone(Region card) {
        FlowPane tags = (FlowPane) card.lookup("#tags");
        Label phone = (Label) card.lookup("#phone");
        double phoneTop = phone.localToScene(phone.getLayoutBounds()).getMinY();
        Bounds tagsBounds = tags.localToScene(tags.getLayoutBounds());
        for (Node child : tags.getChildren()) {
            Label label = (Label) child;
            Bounds bounds = label.localToScene(label.getLayoutBounds());
            assertTrue(bounds.getMaxY() <= phoneTop + 1, "Tag rows must end before the phone row");
            assertTrue(bounds.getMaxY() <= tagsBounds.getMaxY() + 1, "All tag rows must fit their allocated height");
            assertTrue(bounds.getMaxX() <= tagsBounds.getMaxX() + 1, "Long tags must fit the available width");
            assertEquals(label.getText(), label.getTooltip().getText());
        }
        Bounds address = card.lookup("#address").localToScene(card.lookup("#address").getLayoutBounds());
        assertTrue(phone.localToScene(phone.getLayoutBounds()).getMaxY() <= address.getMinY() + 1);
        Node email = card.lookup("#email");
        if (email.isVisible() && email.isManaged()) {
            assertTrue(address.getMaxY() <= email.localToScene(email.getLayoutBounds()).getMinY() + 1);
        }
    }

    private Person createLongContact(boolean hasName, Status status) {
        PersonBuilder builder = new PersonBuilder().withName("W".repeat(127)).withPhone("9".repeat(15))
                .withEmail("w".repeat(64) + "@u.nus.edu").withAddress("W".repeat(200))
                .withTags("w".repeat(30), "m".repeat(30), "exco").withStatus(status);
        return (hasName ? builder : builder.withoutName()).build();
    }

    private StackPane createList(ObservableList<Person> persons, int width, int height) {
        StackPane container = new StackPane(new PersonListPanel(persons).getRoot());
        Scene scene = new Scene(container, width, height);
        scene.getStylesheets().add(getClass().getResource("/view/DarkTheme.css").toExternalForm());
        layout(container);
        return container;
    }

    private void layout(StackPane container) {
        // A list layout can create new cards or change the available width when a scrollbar appears.
        for (int pass = 0; pass < 2; pass++) {
            container.applyCss();
            container.layout();
        }
    }

    private ListCell<?> findCell(ListView<?> list, int index) {
        return list.lookupAll(".list-cell").stream()
                .filter(node -> node instanceof ListCell<?> cell && cell.isVisible()
                        && !cell.isEmpty() && cell.getIndex() == index)
                .map(node -> (ListCell<?>) node)
                .findFirst().orElseThrow();
    }

    private ScrollBar findScrollBar(ListView<?> list, Orientation orientation) {
        return list.lookupAll(".scroll-bar").stream()
                .map(node -> (ScrollBar) node)
                .filter(bar -> bar.getOrientation() == orientation)
                .findFirst().orElseThrow();
    }

    private void assertCardFits(ListView<?> list, ListCell<?> cell) {
        assertFalse(findScrollBar(list, Orientation.HORIZONTAL).isVisible(), "Horizontal scrolling is unnecessary");
        Node viewport = list.lookup(".clipped-container");
        assertNotNull(viewport);
        Bounds viewportBounds = viewport.localToScene(viewport.getLayoutBounds());
        Region card = (Region) cell.getGraphic();
        for (String field : new String[] {"id", "name", "phone", "address", "email", "tags", "status"}) {
            Node value = card.lookup("#" + field);
            if (value.isVisible() && value.isManaged()) {
                Bounds bounds = value.localToScene(value.getLayoutBounds());
                assertTrue(bounds.getMinX() >= viewportBounds.getMinX() - 1, field);
                assertTrue(bounds.getMaxX() <= viewportBounds.getMaxX() + 1, field);
                Bounds cardBounds = card.localToScene(card.getLayoutBounds());
                assertTrue(bounds.getMinY() >= cardBounds.getMinY() - 1, field);
                assertTrue(bounds.getMaxY() <= cardBounds.getMaxY() + 1, field);
            }
        }
    }

    private void assertSingleLineEmail(Region card, String field, String expectedText, boolean isTruncated) {
        Label label = (Label) card.lookup("#" + field);
        Text renderedText = (Text) label.lookup(".text");
        assertNotNull(renderedText, field);
        assertFalse(label.isWrapText(), field);
        assertEquals(expectedText, label.getText(), "The email value must not be shortened");
        assertEquals(expectedText, label.getTooltip().getText(), "The tooltip must retain the complete email");
        Text singleLine = new Text(expectedText);
        singleLine.setFont(label.getFont());
        assertEquals(singleLine.getLayoutBounds().getHeight(), renderedText.getLayoutBounds().getHeight(), 1,
                "The email must render on exactly one line");
        if (isTruncated) {
            assertTrue(renderedText.getText().endsWith(label.getEllipsisString()), field);
            assertTrue(renderedText.getText().length() < expectedText.length(), field);
        } else {
            assertEquals(expectedText, renderedText.getText(), field);
        }
    }

    private void assertRenderedText(Region card, String field, String expectedText) {
        Label label = (Label) card.lookup("#" + field);
        Text renderedText = (Text) label.lookup(".text");
        assertNotNull(renderedText, field);
        assertEquals(expectedText, label.getText(), field);
        assertEquals(expectedText, renderedText.getText(), field + " must render without ellipses");
        Bounds bounds = renderedText.getBoundsInParent();
        assertTrue(bounds.getMinY() >= -1, field);
        assertTrue(bounds.getMaxY() <= label.getHeight() + 1, field + " must not be clipped vertically");
    }
}
