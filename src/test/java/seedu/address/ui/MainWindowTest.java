package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import seedu.address.logic.Logic;
import seedu.address.logic.LogicManager;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.model.person.Status;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.JavaFxTestUtil;
import seedu.address.testutil.PersonBuilder;

/** Checks that the status button changes the visible list used by commands. */
public class MainWindowTest {

    @TempDir
    public Path temporaryFolder;

    @Test
    public void activityButton_cyclesAllActiveArchivedAndUsesDisplayedIndices() throws Exception {
        JavaFxTestUtil.initialize();
        Person active = new PersonBuilder().withEmail("active@u.nus.edu").build();
        Person archived = new PersonBuilder().withEmail("archived@u.nus.edu")
                .withStatus(Status.ARCHIVED).build();
        AddressBook addressBook = new AddressBook();
        addressBook.addPerson(active);
        addressBook.addPerson(archived);
        ModelManager model = new ModelManager(addressBook, new UserPrefs());
        Path dataFile = temporaryFolder.resolve("addressBook.json");
        Logic logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"))));

        JavaFxTestUtil.runAndWait(() -> {
            Stage stage = new Stage();
            try {
                MainWindow window = new MainWindow(stage, logic, dataFile);
                window.fillInnerParts();
                stage.show();
                stage.getScene().getRoot().applyCss();
                Button toggle = (Button) stage.getScene().lookup("#statusFilterButton");
                @SuppressWarnings("unchecked")
                ListView<Person> list = (ListView<Person>) stage.getScene().lookup("#personListView");
                assertNotNull(toggle);
                assertNotNull(list);
                assertEquals(HBox.class, toggle.getParent().getClass());
                HBox menuRow = (HBox) toggle.getParent();
                assertEquals(Pos.CENTER_LEFT, menuRow.getAlignment());
                assertEquals(MenuBar.class, menuRow.getChildren().get(0).getClass());
                assertEquals("ALL", toggle.getText());
                assertButtonColors(toggle, "#3e7b91", Color.WHITE);
                assertEquals(List.of(active, archived), list.getItems());

                toggle.fire();
                assertEquals("ACTIVE", toggle.getText());
                assertButtonColors(toggle, "#238558", Color.WHITE);
                assertEquals(List.of(active), list.getItems());

                toggle.fire();
                assertEquals("ARCHIVED", toggle.getText());
                assertButtonColors(toggle, "#f4c542", Color.web("#1d1d1d"));
                assertEquals(List.of(archived), list.getItems());

                toggle.fire();
                assertEquals("ALL", toggle.getText());
                assertButtonColors(toggle, "#3e7b91", Color.WHITE);
                assertEquals(List.of(active, archived), list.getItems());

                logic.execute("find e/archived");
                assertEquals(List.of(archived), list.getItems());
                toggle.fire();
                assertEquals(List.of(), list.getItems());
                toggle.fire();
                assertEquals("ARCHIVED", toggle.getText());
                assertEquals(List.of(archived), list.getItems());

                logic.execute("delete 1");
                assertEquals(List.of(active), model.getAddressBook().getPersonList());
                assertEquals(List.of(), list.getItems());

                toggle.fire();
                logic.execute("list");
                assertEquals("ALL", toggle.getText());
                assertEquals(List.of(active), list.getItems());
            } finally {
                stage.close();
            }
            return null;
        });
    }

    @Test
    public void indexedCommands_updateMemberAtDisplayedIndex() throws Exception {
        JavaFxTestUtil.initialize();
        Person activeFirst = new PersonBuilder().withEmail("active-first@u.nus.edu").build();
        Person archivedFirst = new PersonBuilder().withEmail("archived-first@u.nus.edu")
                .withStatus(Status.ARCHIVED).build();
        Person activeSecond = new PersonBuilder().withEmail("active-second@u.nus.edu").build();
        Person archivedSecond = new PersonBuilder().withEmail("archived-second@u.nus.edu")
                .withStatus(Status.ARCHIVED).build();
        AddressBook addressBook = new AddressBook();
        addressBook.addPerson(activeFirst);
        addressBook.addPerson(archivedFirst);
        addressBook.addPerson(activeSecond);
        addressBook.addPerson(archivedSecond);
        ModelManager model = new ModelManager(addressBook, new UserPrefs());
        Path dataFile = temporaryFolder.resolve("indexedAddressBook.json");
        Logic logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("indexedUserPrefs.json"))));

        JavaFxTestUtil.runAndWait(() -> {
            Stage stage = new Stage();
            try {
                MainWindow window = new MainWindow(stage, logic, dataFile);
                window.fillInnerParts();
                stage.show();
                stage.getScene().getRoot().applyCss();
                Button toggle = (Button) stage.getScene().lookup("#statusFilterButton");
                @SuppressWarnings("unchecked")
                ListView<Person> list = (ListView<Person>) stage.getScene().lookup("#personListView");
                assertNotNull(toggle);
                assertNotNull(list);

                toggle.fire();
                assertEquals(List.of(activeFirst, activeSecond), list.getItems());
                logic.execute("find e/active-second");
                assertEquals(List.of(activeSecond), list.getItems());
                logic.execute("edit 1 n/Updated Member");
                Person edited = new PersonBuilder(activeSecond).withName("Updated Member").build();
                assertEquals(List.of(activeFirst, edited), list.getItems());
                assertEquals(archivedFirst, model.getAddressBook().getPersonList().get(1));

                logic.execute("tag 2 t/Volunteer");
                Person tagged = new PersonBuilder(edited).withTags("Volunteer").build();
                assertEquals(List.of(activeFirst, tagged), list.getItems());
                logic.execute("untag 2 t/Volunteer");
                assertEquals(List.of(activeFirst, edited), list.getItems());

                logic.execute("archive 2");
                Person nowArchived = edited.withStatus(Status.ARCHIVED);
                assertEquals(List.of(activeFirst), list.getItems());
                assertEquals(nowArchived, model.getAddressBook().getPersonList().get(2));

                toggle.fire();
                assertEquals(List.of(archivedFirst, nowArchived, archivedSecond), list.getItems());
                logic.execute("find e/archived-second");
                assertEquals(List.of(archivedSecond), list.getItems());
                logic.execute("activate 1");
                Person nowActive = archivedSecond.withStatus(Status.ACTIVE);
                assertEquals(List.of(archivedFirst, nowArchived), list.getItems());
                assertEquals(nowActive, model.getAddressBook().getPersonList().get(3));

                logic.execute("delete 2");
                assertEquals(List.of(archivedFirst), list.getItems());
                assertEquals(List.of(activeFirst, archivedFirst, nowActive), model.getAddressBook().getPersonList());
            } finally {
                stage.close();
            }
            return null;
        });
    }

    @Test
    public void viewCommand_updatesButtonAndListWhilePreservingSearch() throws Exception {
        JavaFxTestUtil.initialize();
        Person active = new PersonBuilder().withEmail("active@u.nus.edu").build();
        Person archived = new PersonBuilder().withEmail("archived@u.nus.edu")
                .withStatus(Status.ARCHIVED).build();
        AddressBook addressBook = new AddressBook();
        addressBook.addPerson(active);
        addressBook.addPerson(archived);
        ModelManager model = new ModelManager(addressBook, new UserPrefs());
        Path dataFile = temporaryFolder.resolve("viewAddressBook.json");
        Logic logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("viewUserPrefs.json"))));

        JavaFxTestUtil.runAndWait(() -> {
            Stage stage = new Stage();
            try {
                MainWindow window = new MainWindow(stage, logic, dataFile);
                window.fillInnerParts();
                stage.show();
                stage.getScene().getRoot().applyCss();
                Button toggle = (Button) stage.getScene().lookup("#statusFilterButton");
                TextField commandField = (TextField) stage.getScene().lookup("#commandTextField");
                @SuppressWarnings("unchecked")
                ListView<Person> list = (ListView<Person>) stage.getScene().lookup("#personListView");
                assertNotNull(toggle);
                assertNotNull(commandField);
                assertNotNull(list);

                commandField.setText("find e/archived");
                commandField.fireEvent(new ActionEvent());
                assertEquals(List.of(archived), list.getItems());

                commandField.setText("view active");
                commandField.fireEvent(new ActionEvent());
                assertEquals("ACTIVE", toggle.getText());
                assertButtonColors(toggle, "#238558", Color.WHITE);
                assertEquals(List.of(), list.getItems());

                commandField.setText("view archived");
                commandField.fireEvent(new ActionEvent());
                assertEquals("ARCHIVED", toggle.getText());
                assertButtonColors(toggle, "#f4c542", Color.web("#1d1d1d"));
                assertEquals(List.of(archived), list.getItems());

                toggle.fire();
                assertEquals("ALL", toggle.getText());
                assertEquals(List.of(archived), list.getItems());
                commandField.setText("view active");
                commandField.fireEvent(new ActionEvent());
                commandField.setText("view all");
                commandField.fireEvent(new ActionEvent());
                assertEquals("ALL", toggle.getText());
                assertButtonColors(toggle, "#3e7b91", Color.WHITE);
                assertEquals(List.of(archived), list.getItems());

                commandField.setText("list");
                commandField.fireEvent(new ActionEvent());
                assertEquals(List.of(active, archived), list.getItems());
            } finally {
                stage.close();
            }
            return null;
        });
    }

    private static void assertButtonColors(Button button, String background, Color text) {
        button.applyCss();
        assertEquals(Color.web(background), button.getBackground().getFills().get(0).getFill());
        assertEquals(text, button.getTextFill());
    }
}
