package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.AddressBookParser;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Person;
import seedu.address.testutil.JavaFxTestUtil;
import seedu.address.testutil.PersonBuilder;

public class CommandBoxTest {
    private final List<String> executions = new ArrayList<>();
    private CommandBox commandBox;
    private TextField field;

    @BeforeAll
    public static void setUpJavaFx() throws InterruptedException {
        JavaFxTestUtil.initialize();
    }

    @BeforeEach
    public void setUp() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            commandBox = new CommandBox(command -> {
                executions.add(command);
                if (command.startsWith("invalid")) {
                    throw new ParseException("Unknown command.");
                }
                if (command.startsWith("failure")) {
                    throw new CommandException("Execution or save failed.");
                }
                return new CommandResult("Success");
            });
            field = (TextField) commandBox.getRoot().lookup("#commandTextField");
            return null;
        });
    }

    @Test
    public void constructor_displaysHistoryHint() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            assertEquals("Enter command here... (↑/↓ for history)", field.getPromptText());
            return null;
        });
    }

    @Test
    public void paste_searchWhitespace_preservesBoundariesAndValidatesBeforeFiltering() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            Person alice = new PersonBuilder().withName("Alice").withPhone("91234567")
                    .withEmail("alice@u.nus.edu").withTags("publicity").build();
            Person bob = new PersonBuilder().withName("Bob").withEmail("bob@u.nus.edu").build();
            Model model = new ModelManager();
            model.addPerson(alice);
            model.addPerson(bob);
            AddressBookParser parser = new AddressBookParser();
            List<String> errors = new ArrayList<>();
            commandBox = new CommandBox(command -> {
                try {
                    return parser.parseCommand(command).execute(model);
                } catch (ParseException e) {
                    errors.add(e.getMessage());
                    throw e;
                }
            });
            field = (TextField) commandBox.getRoot().lookup("#commandTextField");
            Map<String, String> invalidSearches = Map.of(
                    "find n/Alice%sBob", FindCommand.MESSAGE_MULTIPLE_NAME_KEYWORDS,
                    "find p/9123%s4567", FindCommand.MESSAGE_MULTIPLE_PHONE_SUBSTRINGS,
                    "find e/alice%sbob", FindCommand.MESSAGE_MULTIPLE_EMAIL_SUBSTRINGS,
                    "find t/pub%slicity", FindCommand.MESSAGE_MULTIPLE_TAGS,
                    "findall p/9123%s4567", String.format(FindCommand.MESSAGE_MULTIPLE_CONDITION_VALUES, "p/"),
                    "findany p/9123%s4567", String.format(FindCommand.MESSAGE_MULTIPLE_CONDITION_VALUES_ANY, "p/"));
            Map<String, List<Person>> validSearches = Map.of(
                    "find%sAlice%sBob", List.of(alice, bob),
                    "findall%sn/Alice%st/publicity", List.of(alice),
                    "findany%sn/Alice%sn/Bob", List.of(alice, bob),
                    "find n/%sAlice%s", List.of(alice));
            for (String separator : List.of("\t", "\n", "\r", "\r\n", "\t\r\n")) {
                for (Map.Entry<String, String> search : invalidSearches.entrySet()) {
                    submit("find n/Bob");
                    paste(field, String.format(search.getKey(), separator));
                    assertEquals(String.format(search.getKey(), " "), field.getText());
                    field.fireEvent(new ActionEvent());
                    assertTrue(hasErrorStyle());
                    assertEquals(search.getValue(), errors.getLast());
                    assertEquals(List.of(bob), model.getFilteredPersonList());
                }
                for (Map.Entry<String, List<Person>> search : validSearches.entrySet()) {
                    field.clear();
                    paste(field, String.format(search.getKey(), separator, separator));
                    assertEquals(String.format(search.getKey(), " ", " "), field.getText());
                    field.fireEvent(new ActionEvent());
                    assertFalse(hasErrorStyle());
                    assertEquals(search.getValue(), model.getFilteredPersonList());
                }
            }
            assertEquals(List.of(alice, bob), model.getAddressBook().getPersonList());
            return null;
        });
    }

    @Test
    public void paste_selectionInSearch_preservesCaretUndoAndHistory() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            for (String separator : List.of("\t", "\n", "\r\n", "\t\r\n")) {
                field.setText("find Alice Bob");
                field.selectRange(10, 5);
                paste(field, "Carol" + separator + "David");
                assertEquals("find Carol David Bob", field.getText());
                assertEquals(16, field.getCaretPosition());
                assertEquals(16, field.getAnchor());
                field.undo();
                assertEquals("find Alice Bob", field.getText());
                field.redo();
                assertEquals("find Carol David Bob", field.getText());
                field.fireEvent(new ActionEvent());
                press(KeyCode.UP);
                assertEquals("find Carol David Bob", field.getText());
            }
            return null;
        });
    }

    @Test
    public void paste_partialSearchCommand_normalizesUsingCompleteInput() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            for (String command : List.of("find", "findall", "findany")) {
                field.setText("fi");
                field.positionCaret(field.getLength());
                paste(field, command.substring(2) + "\r\nn/Alice");
                assertEquals(command + " n/Alice", field.getText());
                assertEquals(field.getLength(), field.getCaretPosition());
                field.setText(command + " n/AliceBob");
                field.positionCaret(command.length() + 8);
                paste(field, "\t");
                assertEquals(command + " n/Alice Bob", field.getText());
            }
            field.clear();
            paste(field, "\t\r\nfind n/Alice\r\n");
            assertEquals(" find n/Alice ", field.getText());
            return null;
        });
    }

    @Test
    public void paste_otherCommands_preservesDefaultTextFieldBehavior() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            TextField defaultField = new TextField();
            for (String command : List.of("add", "edit", "list", "help", "clear", "findOther", "findallOther",
                    "findanyOther", "FIND", "FINDALL", "FINDANY")) {
                for (String separator : List.of("\t", "\n", "\r\n")) {
                    field.setText(command + " n/AliceBob");
                    defaultField.setText(field.getText());
                    field.positionCaret(command.length() + 8);
                    defaultField.positionCaret(field.getCaretPosition());
                    paste(field, separator);
                    paste(defaultField, separator);
                    assertEquals(defaultField.getText(), field.getText());
                    assertEquals(defaultField.getCaretPosition(), field.getCaretPosition());
                    assertEquals(defaultField.getAnchor(), field.getAnchor());
                }
            }
            return null;
        });
    }

    @Test
    public void submit_successAndDuplicates_executeOncePerSubmission() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            submit(" list ");
            assertEquals("", field.getText());
            submit(" list ");
            assertEquals(List.of(" list ", " list "), executions);
            press(KeyCode.UP);
            assertEquals(" list ", field.getText());
            assertEquals(field.getLength(), field.getCaretPosition());
            assertEquals(field.getLength(), field.getAnchor());
            press(KeyCode.UP);
            press(KeyCode.DOWN);
            assertEquals("", field.getText());
            assertEquals(2, executions.size());
            return null;
        });
    }

    @Test
    public void submit_failures_remainRecallableAndEditable() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            submit("list");
            String previousCommand = "list";
            for (String command : List.of("invalid syntax", "failure save")) {
                submit(command);
                assertEquals(command, field.getText());
                assertTrue(hasErrorStyle());
                field.selectRange(2, 0);
                press(KeyCode.UP);
                assertEquals(previousCommand, field.getText());
                assertFalse(hasErrorStyle());
                assertEquals(previousCommand.length(), field.getCaretPosition());
                press(KeyCode.DOWN);
                assertEquals(command, field.getText());
                assertEquals(command.length(), field.getCaretPosition());
                press(KeyCode.DOWN);
                assertEquals(command, field.getText());
                assertEquals(command.length(), field.getAnchor());
                assertEquals(command.length(), field.getCaretPosition());
                previousCommand = command;
            }
            submit("corrected");
            assertEquals("", field.getText());
            assertFalse(hasErrorStyle());
            press(KeyCode.UP);
            assertEquals("corrected", field.getText());
            press(KeyCode.UP);
            assertEquals("failure save", field.getText());
            press(KeyCode.UP);
            assertEquals("invalid syntax", field.getText());
            assertEquals(List.of("list", "invalid syntax", "failure save", "corrected"), executions);
            return null;
        });
    }

    @Test
    public void navigate_editedFailureAndRepeatedDown_oneUpRecallsPrevious() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            submit("list");
            for (String failedCommand : List.of("invalid syntax", "failure save")) {
                submit(failedCommand);
                field.setText("edited " + failedCommand);
                field.selectRange(5, 2);
                for (int i = 0; i < 4; i++) {
                    press(KeyCode.DOWN);
                    assertEquals("edited " + failedCommand, field.getText());
                    assertEquals(5, field.getAnchor());
                    assertEquals(2, field.getCaretPosition());
                }
                press(KeyCode.UP);
                String previous = failedCommand.startsWith("invalid") ? "list" : "invalid syntax";
                assertEquals(previous, field.getText());
                press(KeyCode.DOWN);
                assertEquals("edited " + failedCommand, field.getText());
                press(KeyCode.DOWN);
                press(KeyCode.DOWN);
                press(KeyCode.UP);
                assertEquals(previous, field.getText());
            }
            assertEquals(List.of("list", "invalid syntax", "failure save"), executions);
            return null;
        });
    }

    @Test
    public void navigate_onlyFailureAndRepeatedDown_preservesErrorAndSelection() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            submit("invalid syntax");
            field.selectRange(5, 2);
            press(KeyCode.DOWN);
            press(KeyCode.DOWN);
            press(KeyCode.UP);
            assertEquals("invalid syntax", field.getText());
            assertEquals(5, field.getAnchor());
            assertEquals(2, field.getCaretPosition());
            assertTrue(hasErrorStyle());
            return null;
        });
    }

    @Test
    public void submit_firstCommandFails_upPreservesInputAndErrorStyle() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            submit("invalid syntax");
            field.selectRange(5, 2);
            press(KeyCode.UP);
            assertEquals("invalid syntax", field.getText());
            assertEquals(5, field.getAnchor());
            assertEquals(2, field.getCaretPosition());
            assertTrue(hasErrorStyle());
            assertEquals(List.of("invalid syntax"), executions);
            return null;
        });
    }

    @Test
    public void submit_repeatedFailure_oneUpRecallsPreviousAndPreservesEdits() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            submit("list");
            submit("invalid syntax");
            submit("invalid syntax");
            field.setText("corrected command");
            press(KeyCode.UP);
            assertEquals("list", field.getText());
            press(KeyCode.UP);
            assertEquals("list", field.getText());
            press(KeyCode.DOWN);
            assertEquals("corrected command", field.getText());
            field.fireEvent(new ActionEvent());
            assertEquals("", field.getText());
            press(KeyCode.UP);
            assertEquals("corrected command", field.getText());
            press(KeyCode.UP);
            assertEquals("invalid syntax", field.getText());
            assertEquals(List.of("list", "invalid syntax", "invalid syntax", "corrected command"), executions);
            return null;
        });
    }

    @Test
    public void submit_blankWhileBrowsing_preservesDraftCursorAndStyle() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            submit("older");
            submit("newer");
            field.setText("draft");
            field.selectRange(4, 1);
            press(KeyCode.UP);
            for (String blank : List.of("", "   ", "\u2003")) {
                field.setText(blank);
                field.selectRange(blank.length(), 0);
                field.getStyleClass().add(CommandBox.ERROR_STYLE_CLASS);
                field.fireEvent(new ActionEvent());
                assertEquals(blank, field.getText());
                assertEquals(blank.length(), field.getAnchor());
                assertEquals(0, field.getCaretPosition());
                assertTrue(hasErrorStyle());
            }
            press(KeyCode.DOWN);
            assertEquals("draft", field.getText());
            assertEquals(4, field.getAnchor());
            assertEquals(1, field.getCaretPosition());
            assertFalse(hasErrorStyle());
            assertEquals(List.of("older", "newer"), executions);
            submit("\u00a0");
            assertEquals("\u00a0", executions.getLast());
            return null;
        });
    }

    @Test
    public void navigate_editedEntries_retainsEditsAndRestoresDraft() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            submit("list");
            submit("help");
            field.setText("find n/Alex");
            field.selectRange(9, 2);
            press(KeyCode.UP);
            field.setText("edited help");
            press(KeyCode.UP);
            assertEquals("list", field.getText());
            field.setText("edited oldest");
            field.selectRange(4, 1);
            field.getStyleClass().add(CommandBox.ERROR_STYLE_CLASS);
            press(KeyCode.UP);
            assertEquals("edited oldest", field.getText());
            assertEquals(4, field.getAnchor());
            assertEquals(1, field.getCaretPosition());
            assertTrue(hasErrorStyle());
            press(KeyCode.DOWN);
            assertEquals("edited help", field.getText());
            assertFalse(hasErrorStyle());
            press(KeyCode.DOWN);
            assertEquals("find n/Alex", field.getText());
            assertEquals(9, field.getAnchor());
            assertEquals(2, field.getCaretPosition());
            field.getStyleClass().add(CommandBox.ERROR_STYLE_CLASS);
            press(KeyCode.DOWN);
            assertTrue(hasErrorStyle());
            assertEquals(2, field.getCaretPosition());
            press(KeyCode.UP);
            assertEquals("edited help", field.getText());
            press(KeyCode.UP);
            assertEquals("edited oldest", field.getText());
            assertEquals(field.getLength(), field.getCaretPosition());
            assertEquals(2, executions.size());
            return null;
        });
    }

    @Test
    public void navigate_blankEdit_retainsEmptyTextWithoutSubmitting() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            submit("list");
            field.setText("draft");
            press(KeyCode.UP);
            field.clear();
            field.fireEvent(new ActionEvent());
            press(KeyCode.DOWN);
            assertEquals("draft", field.getText());
            press(KeyCode.UP);
            assertEquals("", field.getText());
            assertEquals(List.of("list"), executions);
            return null;
        });
    }

    @Test
    public void submit_retainedEdit_executesEditAndPreservesSubmittedHistory() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            submit("list");
            submit("help");
            press(KeyCode.UP);
            field.setText("edited help");
            press(KeyCode.UP);
            field.setText("edited list");
            press(KeyCode.DOWN);
            assertEquals("edited help", field.getText());
            field.fireEvent(new ActionEvent());
            assertEquals(List.of("list", "help", "edited help"), executions);
            press(KeyCode.UP);
            assertEquals("edited help", field.getText());
            press(KeyCode.UP);
            assertEquals("help", field.getText());
            press(KeyCode.UP);
            assertEquals("list", field.getText());
            return null;
        });
    }

    @Test
    public void submit_duplicateWhileBrowsing_discardsPreviousDraft() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            submit("list");
            field.setText("old draft");
            press(KeyCode.UP);
            field.fireEvent(new ActionEvent());
            press(KeyCode.DOWN);
            assertEquals("", field.getText());
            press(KeyCode.UP);
            assertEquals("list", field.getText());
            press(KeyCode.UP);
            press(KeyCode.DOWN);
            assertEquals("", field.getText());
            assertEquals(List.of("list", "list"), executions);
            return null;
        });
    }

    @Test
    public void submit_failureAtCapacityWhileBrowsing_evictsOnlyOldest() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            for (int i = 1; i <= 100; i++) {
                submit("command " + i);
            }
            press(KeyCode.UP);
            press(KeyCode.UP);
            submit("failure save");
            assertTrue(hasErrorStyle());
            for (int i = 100; i >= 2; i--) {
                press(KeyCode.UP);
                assertEquals("command " + i, field.getText());
            }
            press(KeyCode.UP);
            assertEquals("command 2", field.getText());
            assertEquals(101, executions.size());
            return null;
        });
    }

    @Test
    public void navigate_modifiersOtherControlsAndOtherKeys_leaveHistoryAlone() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            submit("older");
            submit("newer");
            TextField otherField = new TextField("other");
            VBox root = new VBox(commandBox.getRoot(), otherField);
            new Scene(root);
            AtomicBoolean reachedParent = new AtomicBoolean();
            root.addEventHandler(KeyEvent.KEY_PRESSED, event -> reachedParent.set(true));
            for (int modifier = 0; modifier < 4; modifier++) {
                field.setText("draft");
                for (KeyCode code : List.of(KeyCode.UP, KeyCode.DOWN)) {
                    field.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code,
                            modifier == 0, modifier == 1, modifier == 2, modifier == 3));
                    assertEquals("draft", field.getText());
                }
                press(KeyCode.UP);
                assertEquals("newer", field.getText());
                press(KeyCode.DOWN);
            }
            otherField.fireEvent(key(KeyCode.UP));
            assertEquals("draft", field.getText());
            reachedParent.set(false);
            press(KeyCode.F1);
            assertTrue(reachedParent.get());
            reachedParent.set(false);
            press(KeyCode.UP);
            assertFalse(reachedParent.get());
            assertEquals("newer", field.getText());
            assertEquals(2, executions.size());
            return null;
        });
    }

    @Test
    public void navigate_emptyHistory_preservesInputAndConsumesArrows() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            VBox root = new VBox(commandBox.getRoot());
            AtomicBoolean reachedParent = new AtomicBoolean();
            root.addEventHandler(KeyEvent.KEY_PRESSED, event -> reachedParent.set(true));
            field.setText("draft");
            field.selectRange(4, 1);
            field.getStyleClass().add(CommandBox.ERROR_STYLE_CLASS);
            press(KeyCode.UP);
            press(KeyCode.DOWN);
            assertEquals("draft", field.getText());
            assertEquals(4, field.getAnchor());
            assertEquals(1, field.getCaretPosition());
            assertTrue(hasErrorStyle());
            assertFalse(reachedParent.get());
            assertTrue(executions.isEmpty());
            return null;
        });
    }

    @Test
    public void submit_recordsBeforeCallingExecutor() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            CommandBox inspectingBox = new CommandBox(command -> {
                press(KeyCode.UP);
                assertEquals(command, field.getText());
                assertEquals(command.length(), field.getCaretPosition());
                throw new CommandException("Save failed");
            });
            field = (TextField) inspectingBox.getRoot().lookup("#commandTextField");
            field.setText("submitted");
            field.positionCaret(0);
            field.fireEvent(new ActionEvent());
            assertTrue(hasErrorStyle());
            return null;
        });
    }

    private void paste(TextField target, String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        Map<DataFormat, Object> previousContent = new HashMap<>();
        for (DataFormat format : clipboard.getContentTypes()) {
            previousContent.put(format, clipboard.getContent(format));
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        try {
            assertTrue(clipboard.setContent(content));
            target.paste();
        } finally {
            clipboard.setContent(previousContent);
        }
    }

    private void submit(String command) {
        field.setText(command);
        field.fireEvent(new ActionEvent());
    }

    private void press(KeyCode code) {
        field.fireEvent(key(code));
    }

    private KeyEvent key(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false);
    }

    private boolean hasErrorStyle() {
        return field.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS);
    }
}
