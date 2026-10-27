package seedu.address.ui;

import java.util.Comparator;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import seedu.address.model.person.Person;
import seedu.address.model.person.Status;

/**
 * A UI component that displays information of a {@code Person}.
 */
public class PersonCard extends UiPart<Region> {

    private static final String FXML = "PersonListCard.fxml";

    /**
     * Note: Certain keywords such as "location" and "resources" are reserved keywords in JavaFX.
     * As a consequence, UI elements' variable names cannot be set to such keywords
     * or an exception will be thrown by JavaFX during runtime.
     *
     * @see <a href="https://github.com/se-edu/addressbook-level4/issues/336">The issue on AddressBook level 4</a>
     */

    public final Person person;

    @FXML
    private HBox cardPane;
    @FXML
    private TextField name;
    @FXML
    private TextField id;
    @FXML
    private TextField phone;
    @FXML
    private TextField address;
    @FXML
    private TextField email;
    @FXML
    private TextField status;
    @FXML
    private FlowPane tags;

    /**
     * Creates a {@code PersonCard} with the given {@code Person} and index to display.
     */
    public PersonCard(Person person, int displayedIndex) {
        super(FXML);
        this.person = person;
        setCopyableText(id, displayedIndex + ". ");
        setCopyableText(name, person.getName().map(memberName -> memberName.fullName).orElse(person.getEmail().value));
        setCopyableText(phone, person.getPhone().map(memberPhone -> memberPhone.value)
                .orElse("Phone: -"));
        setCopyableText(address, person.getAddress().map(memberAddress -> memberAddress.value)
                .orElse("Address: -"));
        setCopyableText(email, person.getEmail().value);
        setCopyableText(status, person.getStatus().badgeName);
        if (person.getStatus() == Status.ARCHIVED) {
            status.getStyleClass().add("archived");
        }
        person.getTags().stream()
                .sorted(Comparator.comparing(tag -> tag.tagName))
                .forEach(tag -> {
                    TextField tagField = new TextField();
                    tagField.getStyleClass().add("copyable-tag");
                    setCopyableText(tagField, tag.tagName);
                    tags.getChildren().add(tagField);
                });
    }

    /**
     * Makes card text selectable without giving its field the usual input appearance.
     * The preferred width follows the rendered text, as a Label's width would.
     */
    private static void setCopyableText(TextField field, String value) {
        field.setText(value);
        field.setEditable(false);
        field.getStyleClass().add("card-copy-field");

        Text measuredText = new Text();
        measuredText.textProperty().bind(field.textProperty());
        measuredText.fontProperty().bind(field.fontProperty());
        field.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> {
            return Math.ceil(measuredText.getLayoutBounds().getWidth())
                    + field.getInsets().getLeft() + field.getInsets().getRight() + 2;
        }, measuredText.layoutBoundsProperty(), field.paddingProperty()));
    }
}
