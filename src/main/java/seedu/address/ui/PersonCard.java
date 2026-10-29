package seedu.address.ui;

import java.util.Comparator;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
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
    private Label name;
    @FXML
    private Label id;
    @FXML
    private Label phone;
    @FXML
    private Label address;
    @FXML
    private Label email;
    @FXML
    private Label status;
    @FXML
    private FlowPane tags;

    /**
     * Creates a {@code PersonCard} with the given {@code Person} and index to display.
     */
    public PersonCard(Person person, int displayedIndex) {
        super(FXML);
        this.person = person;
        id.setText(displayedIndex + ". ");
        name.setText(person.getName().map(memberName -> memberName.fullName).orElse(person.getEmail().value));
        phone.setText("Phone: " + person.getPhone().map(memberPhone -> memberPhone.value).orElse("-"));
        address.setText("Address: " + person.getAddress().map(memberAddress -> memberAddress.value).orElse("-"));
        email.setText("Email: " + person.getEmail().value);
        email.setVisible(person.getName().isPresent());
        email.setManaged(person.getName().isPresent());
        for (Label field : List.of(name, phone, address, email)) {
            field.setTooltip(new Tooltip(field.getText()));
        }
        status.setText(person.getStatus().badgeName);
        if (person.getStatus() == Status.ARCHIVED) {
            status.getStyleClass().add("archived");
        }
        tags.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0) {
                tags.setPrefWrapLength(newVal.doubleValue());
            }
        });
        // Preserve natural tag widths when the first height calculation precedes the pane's initial sizing.
        DoubleBinding tagMaxWidth = Bindings.when(tags.widthProperty().greaterThan(0))
                .then(tags.widthProperty()).otherwise(Double.MAX_VALUE);
        person.getTags().stream()
                .sorted(Comparator.comparing(tag -> tag.tagName))
                .forEach(tag -> {
                    Label label = new Label(tag.tagName);
                    label.setMinWidth(0);
                    label.maxWidthProperty().bind(tagMaxWidth);
                    label.setTooltip(new Tooltip(tag.tagName));
                    tags.getChildren().add(label);
                });
    }
}
