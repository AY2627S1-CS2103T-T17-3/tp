package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.testutil.JavaFxTestUtil;

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
