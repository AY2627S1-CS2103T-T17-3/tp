package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import seedu.address.logic.Logic;
import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.ClearCommand;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.StatusFilter;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Address;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Status;
import seedu.address.model.tag.Tag;
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
    public void submit_invalidClear_showsFormatErrorWithoutDialogOrDataChanges() throws Exception {
        JavaFxTestUtil.initialize();
        Path dataFile = temporaryFolder.resolve("invalidClear.json");
        ModelManager model = new ModelManager();
        model.addPerson(new PersonBuilder().withEmail("existing@u.nus.edu").build());
        new JsonAddressBookStorage(dataFile).saveAddressBook(model.getAddressBook());
        String savedBefore = Files.readString(dataFile);
        Logic logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("clearPrefs.json"))));

        JavaFxTestUtil.runAndWait(() -> {
            Stage stage = new Stage();
            try {
                MainWindow window = new MainWindow(stage, logic, dataFile);
                window.fillInnerParts();
                stage.show();
                TextField input = (TextField) stage.getScene().lookup("#commandTextField");
                TextArea results = (TextArea) stage.getScene().lookup("#resultDisplay");
                List<Window> visibleWindows = List.copyOf(Window.getWindows());

                for (String command : List.of("clear 1", "clear all", "clear member", "clear extra_text")) {
                    input.setText(command);
                    input.fireEvent(new ActionEvent());
                    assertEquals("Invalid command format!\n" + ClearCommand.MESSAGE_USAGE, results.getText());
                    assertEquals(command, input.getText());
                    assertTrue(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
                    assertEquals(visibleWindows, Window.getWindows());
                    assertEquals(1, model.getAddressBook().getPersonList().size());
                    assertEquals(savedBefore, Files.readString(dataFile));
                }
            } finally {
                stage.close();
            }
            return null;
        });
    }

    @Test
    public void submit_clearCancellation_preservesRecordsSearchViewAndFile() throws Exception {
        JavaFxTestUtil.initialize();
        Path dataFile = temporaryFolder.resolve("cancelledClear.json");
        ModelManager model = new ModelManager();
        Person active = new PersonBuilder().withEmail("active@u.nus.edu").build();
        Person archived = new PersonBuilder().withEmail("archived@u.nus.edu").withStatus(Status.ARCHIVED).build();
        model.addPerson(active);
        model.addPerson(archived);
        model.updateFilteredPersonList(person -> person.getEmail().equals(active.getEmail()));
        model.setStatusFilter(StatusFilter.ACTIVE);
        new JsonAddressBookStorage(dataFile).saveAddressBook(model.getAddressBook());
        String savedBefore = Files.readString(dataFile);
        Logic logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("clearPrefs.json"))));

        JavaFxTestUtil.runAndWait(() -> {
            Stage stage = new Stage();
            try {
                MainWindow window = new MainWindow(stage, logic, dataFile);
                window.fillInnerParts();
                stage.show();
                TextField input = (TextField) stage.getScene().lookup("#commandTextField");
                TextArea results = (TextArea) stage.getScene().lookup("#resultDisplay");

                for (String action : List.of("cancel", "enter", "escape", "close")) {
                    submitClear(input, pane -> {
                        Button cancel = (Button) pane.lookupButton(ButtonType.CANCEL);
                        Button deleteAll = (Button) pane.lookupButton(pane.getButtonTypes().getLast());
                        assertEquals("Delete all members?", pane.getHeaderText());
                        assertTrue(pane.getContentText().contains("archived members"));
                        assertTrue(pane.getContentText().contains("outside your current search"));
                        assertTrue(pane.getContentText().contains("Existing members and imported members"));
                        assertTrue(pane.getContentText().contains("delete INDEX"));
                        pane.applyCss();
                        pane.layout();
                        Label explanation = (Label) pane.lookup(".content");
                        assertTrue(explanation.isWrapText());
                        assertTrue(explanation.getHeight() >= explanation.prefHeight(explanation.getWidth()) - 1,
                                "The full warning must be visible without truncation.");
                        assertEquals("Delete all members", deleteAll.getText());
                        assertTrue(cancel.isDefaultButton());
                        assertTrue(cancel.isCancelButton());
                        assertFalse(deleteAll.isDefaultButton());
                        assertSame(cancel, pane.getScene().getFocusOwner());
                        Window dialogWindow = pane.getScene().getWindow();
                        assertEquals(stage, ((Stage) dialogWindow).getOwner());
                        assertEquals(List.of(active, archived), model.getAddressBook().getPersonList());
                        assertEquals(savedBefore, assertDoesNotThrow(() -> Files.readString(dataFile)));
                        switch (action) {
                            case "cancel" -> cancel.fire();
                            case "enter" -> fireKey(pane, KeyCode.ENTER);
                            case "escape" -> fireKey(pane, KeyCode.ESCAPE);
                            default -> ((Stage) dialogWindow).close();
                        }
                        assertFalse(dialogWindow.isShowing());
                    });
                    assertEquals(ClearCommand.MESSAGE_CANCELLED, results.getText());
                    assertEquals("", input.getText());
                    assertFalse(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
                    assertEquals(List.of(active, archived), model.getAddressBook().getPersonList());
                    assertEquals(List.of(active), model.getFilteredPersonList());
                    assertEquals(StatusFilter.ACTIVE, model.getStatusFilter());
                    assertEquals("ACTIVE", ((Button) stage.getScene().lookup("#statusFilterButton")).getText());
                    assertEquals(savedBefore, Files.readString(dataFile));
                }
                input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.UP,
                        false, false, false, false));
                assertEquals("clear", input.getText());
            } finally {
                stage.close();
            }
            return null;
        });
    }

    @Test
    public void submit_confirmedClear_deletesAllMembersAndPersistsEmptyDirectory() throws Exception {
        JavaFxTestUtil.initialize();
        Path dataFile = temporaryFolder.resolve("confirmedClear.json");
        ModelManager model = new ModelManager();
        model.addPerson(new PersonBuilder().withEmail("active@u.nus.edu").build());
        model.addPerson(new PersonBuilder().withEmail("archived@u.nus.edu").withStatus(Status.ARCHIVED).build());
        model.setStatusFilter(StatusFilter.ARCHIVED);
        Logic logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("clearPrefs.json"))));

        JavaFxTestUtil.runAndWait(() -> {
            Stage stage = new Stage();
            try {
                MainWindow window = new MainWindow(stage, logic, dataFile);
                window.fillInnerParts();
                stage.show();
                TextField input = (TextField) stage.getScene().lookup("#commandTextField");
                TextArea results = (TextArea) stage.getScene().lookup("#resultDisplay");

                submitClear(input, pane -> {
                    Button deleteAll = (Button) pane.lookupButton(pane.getButtonTypes().getLast());
                    Window dialogWindow = pane.getScene().getWindow();
                    fireKey(pane, KeyCode.TAB);
                    assertSame(deleteAll, pane.getScene().getFocusOwner());
                    fireKey(pane, KeyCode.SPACE);
                    assertFalse(dialogWindow.isShowing());
                });

                assertEquals(ClearCommand.MESSAGE_SUCCESS, results.getText());
                assertEquals("", input.getText());
                assertEquals(List.of(), model.getAddressBook().getPersonList());
                assertEquals(List.of(), model.getFilteredPersonList());
                assertEquals(StatusFilter.ARCHIVED, model.getStatusFilter());
                assertEquals(new AddressBook(), new JsonAddressBookStorage(dataFile).readAddressBook().orElseThrow());

                submitClear(input, pane -> ((Button) pane.lookupButton(ButtonType.CANCEL)).fire());
                assertEquals(ClearCommand.MESSAGE_CANCELLED, results.getText());
            } finally {
                stage.close();
            }
            return null;
        });
    }

    private void submitClear(TextField input, Consumer<DialogPane> response) {
        AtomicBoolean wasShown = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            Stage dialogStage = null;
            try {
                dialogStage = Window.getWindows().stream().filter(candidate -> candidate instanceof Stage)
                        .map(candidate -> (Stage) candidate)
                        .filter(candidate -> candidate.getTitle().equals("Confirm clear")).findFirst().orElseThrow();
                wasShown.set(true);
                response.accept((DialogPane) dialogStage.getScene().getRoot());
            } catch (Throwable error) {
                failure.set(error);
            } finally {
                if (dialogStage != null && dialogStage.isShowing()) {
                    dialogStage.close();
                }
            }
        });
        input.setText("clear");
        input.fireEvent(new ActionEvent());
        assertTrue(wasShown.get(), "Clear must show a confirmation dialog.");
        if (failure.get() != null) {
            throw new AssertionError("Confirmation dialog response failed.", failure.get());
        }
    }

    private void fireKey(DialogPane pane, KeyCode key) {
        Node target = pane.getScene().getFocusOwner();
        assertNotNull(target);
        target.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", key, false, false, false, false));
        target.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", key, false, false, false, false));
    }

    @Test
    public void submit_editUnnamedMember_refreshesHeadingAndEmailRow() throws Exception {
        JavaFxTestUtil.initialize();
        Path dataFile = temporaryFolder.resolve("memberCard.json");
        ModelManager model = new ModelManager(new AddressBook(), new UserPrefs());
        Logic logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("memberCardPrefs.json"))));

        JavaFxTestUtil.runAndWait(() -> {
            Stage stage = new Stage();
            try {
                MainWindow window = new MainWindow(stage, logic, dataFile);
                window.fillInnerParts();
                stage.show();
                TextField input = (TextField) stage.getScene().lookup("#commandTextField");
                TextArea results = (TextArea) stage.getScene().lookup("#resultDisplay");

                input.setText("add e/Alex@U.NUS.EDU");
                input.fireEvent(new ActionEvent());
                assertEquals("New person added: Email: alex@u.nus.edu", results.getText());
                assertDisplayedCard(stage, "alex@u.nus.edu", "alex@u.nus.edu", false);

                input.setText("edit 1 e/alex.new@u.nus.edu");
                input.fireEvent(new ActionEvent());
                assertEquals("Edited person: Email: alex.new@u.nus.edu", results.getText());
                assertDisplayedCard(stage, "alex.new@u.nus.edu", "alex.new@u.nus.edu", false);

                input.setText("edit 1 n/Alex Tan");
                input.fireEvent(new ActionEvent());
                assertEquals("Edited person: Name: Alex Tan; Email: alex.new@u.nus.edu", results.getText());
                assertDisplayedCard(stage, "Alex Tan", "alex.new@u.nus.edu", true);
            } finally {
                stage.close();
            }
            return null;
        });
    }

    @Test
    public void submit_overlongFields_showsConstraintsAndAllowsRetry() throws Exception {
        JavaFxTestUtil.initialize();
        Path dataFile = temporaryFolder.resolve("longFields.json");
        ModelManager model = new ModelManager(new AddressBook(), new UserPrefs());
        Logic logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("fieldPrefs.json"))));

        JavaFxTestUtil.runAndWait(() -> {
            Stage stage = new Stage();
            try {
                MainWindow window = new MainWindow(stage, logic, dataFile);
                window.fillInnerParts();
                stage.setWidth(stage.getMinWidth());
                stage.setHeight(600);
                stage.show();
                assertEquals(stage.getMinWidth(), stage.getWidth(), 1, "The window must use its configured minimum");
                TextField input = (TextField) stage.getScene().lookup("#commandTextField");
                TextArea results = (TextArea) stage.getScene().lookup("#resultDisplay");
                input.setText("add n/Alex e/alex@u.nus.edu");
                input.fireEvent(new ActionEvent());
                String savedBefore = Files.readString(dataFile);
                String[][] cases = {
                    {"add e/rejected@u.nus.edu n/" + "A".repeat(128), Name.MESSAGE_CONSTRAINTS},
                    {"edit 1 p/" + "9".repeat(16), Phone.MESSAGE_CONSTRAINTS},
                    {"edit 1 a/" + "A".repeat(201), Address.MESSAGE_CONSTRAINTS},
                    {"tag 1 t/" + "a".repeat(31), Tag.MESSAGE_CONSTRAINTS}
                };
                for (String[] invalid : cases) {
                    input.setText(invalid[0]);
                    input.fireEvent(new ActionEvent());
                    assertEquals(invalid[1], results.getText());
                    assertEquals(invalid[0], input.getText());
                    assertTrue(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
                    assertEquals(1, model.getAddressBook().getPersonList().size());
                    assertEquals(savedBefore, Files.readString(dataFile));
                }
                input.setText("tag 1 t/" + "w".repeat(30));
                input.fireEvent(new ActionEvent());
                assertEquals("", input.getText());
                assertFalse(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
                assertTrue(results.getText().contains("w".repeat(30)));
                assertTrue(results.isWrapText());
            } finally {
                stage.close();
            }
            return null;
        });
    }

    @Test
    public void submit_overlongEmail_showsValidationAndAllowsRetry() throws Exception {
        JavaFxTestUtil.initialize();
        Path dataFile = temporaryFolder.resolve("longEmail.json");
        ModelManager model = new ModelManager(new AddressBook(), new UserPrefs());
        Logic logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("emailPrefs.json"))));

        JavaFxTestUtil.runAndWait(() -> {
            Stage stage = new Stage();
            try {
                MainWindow window = new MainWindow(stage, logic, dataFile);
                window.fillInnerParts();
                stage.show();
                TextField input = (TextField) stage.getScene().lookup("#commandTextField");
                TextArea results = (TextArea) stage.getScene().lookup("#resultDisplay");
                submitClear(input, pane -> ((Button) pane.lookupButton(pane.getButtonTypes().getLast())).fire());
                assertEquals("Member directory has been cleared!", results.getText());
                String savedBefore = Files.readString(dataFile);
                String command = "add e/" + "a.".repeat(2000) + "a@u.nus.edu";
                input.setText(command);
                input.fireEvent(new ActionEvent());

                assertEquals("Email local-part must contain at most 64 characters; received 4001.", results.getText());
                assertEquals(command, input.getText());
                assertTrue(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
                assertEquals(0, model.getAddressBook().getPersonList().size());
                assertEquals(savedBefore, Files.readString(dataFile));
                input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.UP,
                        false, false, false, false));
                assertEquals("clear", input.getText());
                input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN,
                        false, false, false, false));
                assertEquals(command, input.getText());

                input.setText("add e/after@u.nus.edu");
                input.fireEvent(new ActionEvent());
                assertEquals("New person added: Email: after@u.nus.edu", results.getText());
                assertEquals("", input.getText());
                assertFalse(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
                assertEquals("after@u.nus.edu", model.getAddressBook().getPersonList().getFirst().getEmail().value);

                input.setText("add e/" + "a".repeat(64) + "@u.nus.edu");
                input.fireEvent(new ActionEvent());
                assertEquals("New person added: Email: " + "a".repeat(64) + "@u.nus.edu", results.getText());
                assertEquals("", input.getText());
            } finally {
                stage.close();
            }
            return null;
        });
    }

    @Test
    public void help_displaysReferenceBelowGuideLinkInSharedPopup() throws Exception {
        JavaFxTestUtil.initialize();
        Path dataFile = temporaryFolder.resolve("nusocietydesk.json");
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
        Path dataFile = temporaryFolder.resolve("nusocietydesk.json");
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
    public void activityButton_afterSearch_replacesFeedbackAndPreservesSearch() throws Exception {
        JavaFxTestUtil.initialize();
        Person active = new PersonBuilder().withEmail("active@u.nus.edu").withTags("publicity").build();
        Person archived = new PersonBuilder().withEmail("archived@u.nus.edu")
                .withStatus(Status.ARCHIVED).withTags("publicity").build();
        AddressBook addressBook = new AddressBook();
        addressBook.addPerson(active);
        addressBook.addPerson(archived);
        ModelManager model = new ModelManager(addressBook, new UserPrefs());
        Path dataFile = temporaryFolder.resolve("searchFeedback.json");
        Logic logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("searchFeedbackPrefs.json"))));

        JavaFxTestUtil.runAndWait(() -> {
            Stage stage = new Stage();
            try {
                MainWindow window = new MainWindow(stage, logic, dataFile);
                window.fillInnerParts();
                stage.show();
                TextField input = (TextField) stage.getScene().lookup("#commandTextField");
                TextArea results = (TextArea) stage.getScene().lookup("#resultDisplay");
                Button toggle = (Button) stage.getScene().lookup("#statusFilterButton");
                @SuppressWarnings("unchecked")
                ListView<Person> list = (ListView<Person>) stage.getScene().lookup("#personListView");

                for (String searchCommand : List.of("find e/active", "findall e/active t/publicity",
                        "findany e/active t/unmatched")) {
                    input.setText(searchCommand);
                    input.fireEvent(new ActionEvent());
                    assertEquals("1 person(s) listed!", results.getText());
                    assertEquals(List.of(active), list.getItems());

                    toggle.fire();
                    assertEquals("ACTIVE", toggle.getText());
                    assertEquals("Switched to active view.", results.getText());
                    assertEquals(List.of(active), list.getItems());

                    toggle.fire();
                    assertEquals("ARCHIVED", toggle.getText());
                    assertEquals("Switched to archived view.", results.getText());
                    assertEquals(List.of(), list.getItems());

                    toggle.fire();
                    assertEquals("ALL", toggle.getText());
                    assertEquals("Switched to all view.", results.getText());
                    assertEquals(List.of(active), list.getItems());
                }
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

    @Test
    public void submit_tagCommandsWithManyTags_wrapsResultAndOrdersTagsAlphabetically() throws Exception {
        JavaFxTestUtil.initialize();
        Path dataFile = temporaryFolder.resolve("manyTags.json");
        ModelManager model = new ModelManager(new AddressBook(), new UserPrefs());
        Person person = new PersonBuilder().withName("John Tan").withEmail("john@u.nus.edu")
                .withTags("exco", "1221212123213", "volunteer", "ay2728", "ay2627",
                        "moretagsgobrrrrr", "testing123", "organisingcommittee", "publicity", "president")
                .build();
        model.addPerson(person);
        Logic logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("manyTagsPrefs.json"))));

        JavaFxTestUtil.runAndWait(() -> {
            Stage stage = new Stage();
            try {
                MainWindow window = new MainWindow(stage, logic, dataFile);
                window.fillInnerParts();
                stage.show();
                TextField input = (TextField) stage.getScene().lookup("#commandTextField");
                TextArea results = (TextArea) stage.getScene().lookup("#resultDisplay");

                assertTrue(results.isWrapText());

                input.setText("tag 1 t/testing");
                input.fireEvent(new ActionEvent());

                assertTrue(results.isWrapText());
                String expectedAddedTags = "Tags: [1221212123213][ay2627][ay2728][exco][moretagsgobrrrrr]"
                        + "[organisingcommittee][president][publicity][testing][testing123][volunteer]";
                assertTrue(results.getText().contains(expectedAddedTags));

                input.setText("untag 1 t/testing");
                input.fireEvent(new ActionEvent());

                assertTrue(results.isWrapText());
                String expectedRemovedTags = "Tags: [1221212123213][ay2627][ay2728][exco][moretagsgobrrrrr]"
                        + "[organisingcommittee][president][publicity][testing123][volunteer]";
                assertTrue(results.getText().contains(expectedRemovedTags));
            } finally {
                stage.close();
            }
            return null;
        });
    }

    @Test
    public void submit_repeatedTagCommands_keepsContactRowsBelowTagsWithoutScrolling() throws Exception {
        JavaFxTestUtil.initialize();
        Path dataFile = temporaryFolder.resolve("tagLayout.json");
        ModelManager model = new ModelManager(new AddressBook(), new UserPrefs());
        for (int index = 0; index < 18; index++) {
            model.addPerson(new PersonBuilder().withoutName().withoutPhone().withoutAddress()
                    .withEmail("member" + index + "@u.nus.edu").build());
        }
        Logic logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("tagLayoutPrefs.json"))));
        Stage stage = JavaFxTestUtil.runAndWait(() -> {
            Stage windowStage = new Stage();
            MainWindow window = new MainWindow(windowStage, logic, dataFile);
            window.fillInnerParts();
            windowStage.show();
            return windowStage;
        });
        try {
            JavaFxTestUtil.runAfterLayoutPulse(stage.getScene(), () -> {
                ListView<?> list = (ListView<?>) stage.getScene().lookup("#personListView");
                ScrollBar verticalBar = list.lookupAll(".scroll-bar").stream()
                        .filter(node -> node instanceof ScrollBar bar && bar.getOrientation() == Orientation.VERTICAL)
                        .map(node -> (ScrollBar) node).findFirst().orElseThrow();
                verticalBar.setValue(verticalBar.getMax());
                return null;
            });
            JavaFxTestUtil.runAfterLayoutPulse(stage.getScene(), () -> {
                ListView<?> list = (ListView<?>) stage.getScene().lookup("#personListView");
                ListCell<?> lastCell = list.lookupAll(".list-cell").stream()
                        .filter(node -> node instanceof ListCell<?> cell && cell.isVisible()
                                && !cell.isEmpty() && cell.getIndex() == 17)
                        .map(node -> (ListCell<?>) node).findFirst().orElse(null);
                assertNotNull(lastCell, "The last member card must be rendered before submitting tag commands");
                Node viewport = list.lookup(".clipped-container");
                Bounds viewportBounds = viewport.localToScene(viewport.getLayoutBounds());
                Bounds cellBounds = lastCell.localToScene(lastCell.getLayoutBounds());
                assertTrue(cellBounds.intersects(viewportBounds),
                        "The last member card must be inside the viewport before submitting tag commands");
                return null;
            });
            for (int count = 1; count <= 8; count++) {
                String tag = "a".repeat(29) + (char) ('a' + count);
                submitTagCommandAndAssertLayout(stage, "tag 18 t/" + tag, "Tag added: [" + tag + "]", count);
            }
            assertEquals(8, model.getFilteredPersonList().get(17).getTags().size());
            for (int count = 8; count >= 1; count--) {
                String tag = "a".repeat(29) + (char) ('a' + count);
                submitTagCommandAndAssertLayout(stage, "untag 18 t/" + tag, "Tag removed: [" + tag + "]", count - 1);
            }
            assertTrue(model.getFilteredPersonList().get(17).getTags().isEmpty());
        } finally {
            JavaFxTestUtil.runAndWait(() -> {
                stage.close();
                return null;
            });
        }
    }

    private static void submitTagCommandAndAssertLayout(Stage stage, String command, String feedback, int tagCount)
            throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            TextField input = (TextField) stage.getScene().lookup("#commandTextField");
            input.setText(command);
            input.fireEvent(new ActionEvent());
            assertEquals("", input.getText());
            TextArea results = (TextArea) stage.getScene().lookup("#resultDisplay");
            assertTrue(results.getText().startsWith(feedback));
            return null;
        });
        JavaFxTestUtil.runAfterLayoutPulse(stage.getScene(), () -> {
            ListView<?> list = (ListView<?>) stage.getScene().lookup("#personListView");
            ListCell<?> cell = list.lookupAll(".list-cell").stream()
                    .filter(node -> node instanceof ListCell<?> candidate && candidate.isVisible()
                            && !candidate.isEmpty() && candidate.getIndex() == 17)
                    .map(node -> (ListCell<?>) node).findFirst().orElseThrow();
            FlowPane tags = (FlowPane) cell.getGraphic().lookup("#tags");
            Label phone = (Label) cell.getGraphic().lookup("#phone");
            double phoneTop = phone.localToScene(phone.getLayoutBounds()).getMinY();
            assertEquals(tagCount, tags.getChildren().size());
            for (Node label : tags.getChildren()) {
                assertTrue(label.localToScene(label.getLayoutBounds()).getMaxY() <= phoneTop + 1,
                        "Tag commands must not leave a tag row overlapping the phone");
            }
            return null;
        });
    }

    private static void assertDisplayedCard(Stage stage, String expectedHeading, String expectedEmail,
            boolean hasName) {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
        Label heading = (Label) stage.getScene().lookup("#name");
        Label email = (Label) stage.getScene().lookup("#email");
        assertNotNull(heading);
        assertNotNull(email);
        assertEquals(expectedHeading, heading.getText());
        assertEquals("Email: " + expectedEmail, email.getText());
        assertEquals(hasName, email.isVisible());
        assertEquals(hasName, email.isManaged());
    }
    private static void assertButtonColors(Button button, String background, Color text) {
        button.applyCss();
        assertEquals(Color.web(background), button.getBackground().getFills().get(0).getFill());
        assertEquals(text, button.getTextFill());
    }
}
