package seedu.address.ui;

import java.util.Comparator;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.AccessibleRole;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
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
    private static final int ADDRESS_TEXT_MARGIN = 8;
    private static final int MAX_ADDRESS_LINES = 3;

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
    private TextArea address;
    @FXML
    private TextField email;
    @FXML
    private Region statusStrip;
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
        sizeAddressToContent();
        setCopyableText(email, person.getEmail().value);
        String statusText = "Status: " + person.getStatus().badgeName;
        statusStrip.setAccessibleRole(AccessibleRole.TEXT);
        statusStrip.setAccessibleText(statusText);
        Tooltip.install(cardPane, new Tooltip(statusText));
        if (person.getStatus() == Status.ARCHIVED) {
            statusStrip.getStyleClass().add("archived");
        }
        person.getTags().stream()
                .sorted(Comparator.comparing(tag -> tag.tagName))
                .forEach(tag -> {
                    TextField tagField = new TextField();
                    tagField.getStyleClass().add("copyable-tag");
                    tagField.setMinWidth(0);
                    tagField.maxWidthProperty().bind(tags.widthProperty());
                    setCopyableText(tagField, tag.tagName);
                    tags.getChildren().add(tagField);
                });
    }

    /**
     * Makes card text selectable without giving its field the usual input appearance.
     */
    private static void setCopyableText(TextInputControl field, String value) {
        field.setText(value);
        field.setEditable(false);
        field.getStyleClass().add("card-copy-field");
        field.setTooltip(new Tooltip(value));

        if (field instanceof TextField textField) {
            Text measuredText = new Text();
            measuredText.textProperty().bind(textField.textProperty());
            measuredText.fontProperty().bind(textField.fontProperty());
            textField.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> {
                return Math.ceil(measuredText.getLayoutBounds().getWidth())
                        + textField.getInsets().getLeft() + textField.getInsets().getRight() + 2;
            }, measuredText.layoutBoundsProperty(), textField.paddingProperty()));
        }
    }

    /** Keeps short addresses to one line and lets longer ones use up to three lines. */
    private void sizeAddressToContent() {
        Text measuredText = new Text();
        measuredText.textProperty().bind(address.textProperty());
        measuredText.fontProperty().bind(address.fontProperty());
        Text oneLine = new Text("Ag");
        oneLine.fontProperty().bind(address.fontProperty());
        address.prefHeightProperty().bind(Bindings.createDoubleBinding(() -> {
            measuredText.setWrappingWidth(Math.max(1, address.getWidth() - address.getInsets().getLeft()
                    - address.getInsets().getRight() - ADDRESS_TEXT_MARGIN));
            return Math.ceil(Math.min(measuredText.getLayoutBounds().getHeight(),
                    MAX_ADDRESS_LINES * oneLine.getLayoutBounds().getHeight())) + ADDRESS_TEXT_MARGIN;
        }, address.widthProperty(), address.paddingProperty(), address.textProperty(), address.fontProperty()));
    }
}
