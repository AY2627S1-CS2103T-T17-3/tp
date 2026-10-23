package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import seedu.address.logic.Logic;
import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.HelpCommand;
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

/** Checks command feedback, guide access, and the visible list used by commands. */
public class MainWindowTest {

    @TempDir
    public Path temporaryFolder;

    @Test
    public void help_displaysReferenceBelowGuideLinkInSharedPopup() throws Exception {
        JavaFxTestUtil.initialize();
        Path dataFile = temporaryFolder.resolve("addressBook.json");
        Logic logic = new LogicManager(new ModelManager(), new StorageManager(new JsonAddressBookStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"))));

        JavaFxTestUtil.runAndWait(() -> {
            Stage stage = new Stage();
            Stage guideStage = null;
            try {
                MainWindow window = new MainWindow(stage, logic, dataFile);
                window.fillInnerParts();
                stage.setWidth(450);
                stage.setHeight(600);
                stage.show();
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();
                TextField commandField = (TextField) stage.getScene().lookup("#commandTextField");
                TextArea results = (TextArea) stage.getScene().lookup("#resultDisplay");
                List<Window> visibleWindows = List.copyOf(Window.getWindows());

                commandField.requestFocus();
                for (String invalidHelp : List.of("help 1", "help add", "help --guide")) {
                    commandField.setText(invalidHelp);
                    commandField.fireEvent(new ActionEvent());

                    assertEquals("Invalid command format!\n" + HelpCommand.MESSAGE_USAGE, results.getText());
                    assertEquals(invalidHelp, commandField.getText());
                    assertTrue(commandField.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
                    assertEquals(visibleWindows, Window.getWindows());
                }

                commandField.setText("help");
                commandField.fireEvent(new ActionEvent());

                assertEquals(HelpCommand.SHOWING_HELP_MESSAGE, results.getText());
                assertEquals("", commandField.getText());
                assertEquals(visibleWindows.size() + 1, Window.getWindows().size());
                guideStage = Window.getWindows().stream().filter(candidate -> !visibleWindows.contains(candidate))
                        .filter(candidate -> candidate instanceof Stage)
                        .map(candidate -> (Stage) candidate).filter(candidate -> candidate.getTitle().equals("Help"))
                        .findFirst().orElseThrow();
                guideStage.setWidth(450);
                guideStage.setHeight(300);
                guideStage.getScene().getRoot().applyCss();
                guideStage.getScene().getRoot().layout();
                Label guideMessage = (Label) guideStage.getScene().lookup("#helpMessage");
                Button copyButton = (Button) guideStage.getScene().lookup("#copyButton");
                TextArea reference = (TextArea) guideStage.getScene().lookup("#commandReference");
                assertEquals(HelpWindow.HELP_MESSAGE, guideMessage.getText());
                assertEquals("Copy URL", copyButton.getText());
                assertEquals(HelpCommand.MESSAGE_HELP, reference.getText());
                assertTrue(guideStage.isResizable());
                assertTrue(reference.isWrapText());
                assertFalse(reference.isEditable());
                assertTrue(reference.isFocusTraversable());
                assertTrue(reference.getHeight() > 0);
                assertTrue(guideMessage.getParent().getBoundsInParent().getMaxY()
                        <= reference.getBoundsInParent().getMinY());
                assertEquals(0, reference.getCaretPosition());

                commandField.setText("help");
                commandField.fireEvent(new ActionEvent());
                assertEquals(visibleWindows.size() + 1, Window.getWindows().size());
                assertTrue(guideStage.isShowing());
                assertEquals(HelpCommand.MESSAGE_HELP, reference.getText());

                guideStage.hide();
                commandField.setText("unsent draft");
                commandField.selectRange(2, 7);
                commandField.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F1,
                        false, false, false, false));
                assertTrue(guideStage.isShowing());
                assertEquals(HelpCommand.MESSAGE_HELP, reference.getText());
                assertEquals("unsent draft", commandField.getText());
                assertEquals(2, commandField.getAnchor());
                assertEquals(7, commandField.getCaretPosition());

                guideStage.hide();
                MenuBar menuBar = (MenuBar) stage.getScene().lookup("#menuBar");
                menuBar.getMenus().stream().filter(menu -> menu.getText().equals("Help"))
                        .findFirst().orElseThrow().getItems().getFirst().fire();
                assertTrue(guideStage.isShowing());
                assertEquals(HelpCommand.SHOWING_HELP_MESSAGE, results.getText());
                assertEquals("unsent draft", commandField.getText());
            } finally {
                if (guideStage != null) {
                    guideStage.close();
                }
                stage.close();
            }
            return null;
        });
    }

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
