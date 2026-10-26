package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

import seedu.address.commons.core.LogsCenter;

/**
 * Retains submitted commands, temporary entry edits and an unsent draft for one command box's lifetime.
 */
final class CommandHistory {
    private static final int MAX_HISTORY_SIZE = 100;
    private static final Logger logger = LogsCenter.getLogger(CommandHistory.class);

    private final Deque<String> commandsBeforeCursor = new ArrayDeque<>();
    private final Deque<String> commandsAfterCursor = new ArrayDeque<>();
    private final Map<Integer, String> editedCommands = new HashMap<>();
    private InputState draft;

    CommandHistory() {
        assertValidState();
    }

    /**
     * Records a submission before execution, leaving history unchanged for blank input.
     * Consecutive duplicates still end browsing and must still be executed.
     */
    void record(String command) {
        requireNonNull(command);
        assertValidState();
        if (command.isBlank()) {
            logger.fine("Command history blank submission ignored.");
            assertValidState();
            return;
        }

        int previousSize = size();
        resetCursor();
        boolean isDuplicate = command.equals(commandsBeforeCursor.peekLast());
        if (isDuplicate) {
            logger.fine(() -> "Command history duplicate suppressed: size=" + size() + ".");
        } else {
            boolean isEvicted = commandsBeforeCursor.size() == MAX_HISTORY_SIZE;
            if (isEvicted) {
                assert !commandsBeforeCursor.isEmpty() : "Eviction requires an oldest entry";
                commandsBeforeCursor.removeFirst();
            }
            commandsBeforeCursor.addLast(command);
            logger.fine(() -> "Command history recorded: size=" + size() + ", evictedOldest=" + isEvicted + ".");
        }

        assert commandsAfterCursor.isEmpty() && draft == null && editedCommands.isEmpty()
                : "Submission must end browsing";
        assert size() == (isDuplicate ? previousSize : Math.min(previousSize + 1, MAX_HISTORY_SIZE))
                : "Recording must preserve duplicates or append with oldest-first eviction";
        assertValidState();
    }

    /**
     * Selects the latest failed submission without creating a second copy at the draft position.
     */
    void selectLatestAfterFailure() {
        assertValidState();
        int previousSize = size();
        resetCursor();
        assert !commandsBeforeCursor.isEmpty() : "A failure must follow a recorded submission";
        commandsAfterCursor.addFirst(commandsBeforeCursor.removeLast());
        logger.fine(() -> "Command history selected failed entry: size=" + size() + ".");
        assert size() == previousSize : "Selecting a failure must preserve history size";
        assertValidState();
    }

    /**
     * Recalls an older command, retaining entry edits and capturing the draft when entering history.
     * Returns an empty optional at the boundary, distinct from an identical-text transition.
     */
    Optional<InputState> recallOlder(InputState currentInput) {
        requireNonNull(currentInput);
        assertValidState();
        if (commandsBeforeCursor.isEmpty()) {
            logger.fine("Command history navigation ignored: direction=older, reason=boundary.");
            assertValidState();
            return Optional.empty();
        }

        int previousSize = size();
        if (commandsAfterCursor.isEmpty()) {
            draft = currentInput;
        } else {
            rememberEdit(currentInput);
        }
        assert !commandsBeforeCursor.isEmpty() : "Older navigation requires an older entry";
        commandsAfterCursor.addFirst(commandsBeforeCursor.removeLast());
        logger.fine(() -> "Command history navigated older: before=" + commandsBeforeCursor.size()
                + ", after=" + commandsAfterCursor.size() + ".");
        assert size() == previousSize : "Navigation must preserve history size";
        assertValidState();
        return Optional.of(getSelectedInput());
    }

    /**
     * Retains entry edits and recalls a newer command or restores the draft. Returns empty at the boundary.
     */
    Optional<InputState> recallNewer(InputState currentInput) {
        requireNonNull(currentInput);
        assertValidState();
        if (commandsAfterCursor.isEmpty() || (commandsAfterCursor.size() == 1 && draft == null)) {
            logger.fine("Command history navigation ignored: direction=newer, reason=boundary.");
            assertValidState();
            return Optional.empty();
        }

        int previousSize = size();
        rememberEdit(currentInput);
        assert !commandsAfterCursor.isEmpty() : "Newer navigation requires a selected entry";
        commandsBeforeCursor.addLast(commandsAfterCursor.removeFirst());
        InputState nextInput;
        if (commandsAfterCursor.isEmpty()) {
            nextInput = draft;
            draft = null;
            logger.fine(() -> "Command history draft restored: size=" + size() + ".");
        } else {
            nextInput = getSelectedInput();
            logger.fine(() -> "Command history navigated newer: before=" + commandsBeforeCursor.size()
                    + ", after=" + commandsAfterCursor.size() + ".");
        }
        assert size() == previousSize : "Navigation must preserve history size";
        assertValidState();
        return Optional.of(nextInput);
    }

    private void resetCursor() {
        while (!commandsAfterCursor.isEmpty()) {
            assert !commandsAfterCursor.isEmpty() : "Cursor reset requires a selected or newer entry";
            commandsBeforeCursor.addLast(commandsAfterCursor.removeFirst());
        }
        draft = null;
        editedCommands.clear();
    }

    /**
     * Saves an entry's unsubmitted text by its stable chronological index, not by its command string.
     */
    private void rememberEdit(InputState currentInput) {
        assert !commandsAfterCursor.isEmpty() : "Remembering an edit requires a selected entry";
        int index = commandsBeforeCursor.size();
        if (currentInput.text().equals(commandsAfterCursor.peekFirst())) {
            editedCommands.remove(index);
        } else {
            editedCommands.put(index, currentInput.text());
        }
    }

    private InputState getSelectedInput() {
        assert !commandsAfterCursor.isEmpty() : "Recalling input requires a selected entry";
        String text = editedCommands.getOrDefault(commandsBeforeCursor.size(), commandsAfterCursor.peekFirst());
        return InputState.createAtEnd(text);
    }

    private int size() {
        return commandsBeforeCursor.size() + commandsAfterCursor.size();
    }

    private void assertValidState() {
        assert commandsBeforeCursor != null && commandsAfterCursor != null : "History deques must exist";
        assert size() <= MAX_HISTORY_SIZE : "Combined history must not exceed its capacity";
        assert hasValidEntries() : "History must contain nonblank entries without adjacent duplicates";
        assert draft == null || !commandsAfterCursor.isEmpty() : "A saved draft requires an active browsing position";
        assert draft == null || draft.isValid() : "Saved draft text and selection must be valid";
        assert editedCommands.size() <= size() : "Each retained entry can have at most one pending edit";
        assert editedCommands.entrySet().stream().allMatch(entry -> entry.getKey() >= 0
                && entry.getKey() < size() && entry.getValue() != null)
                : "Pending edits must refer to retained entries";
    }

    private boolean hasValidEntries() {
        String previous = null;
        for (String entry : Stream.concat(commandsBeforeCursor.stream(), commandsAfterCursor.stream()).toList()) {
            if (entry == null || entry.isBlank() || entry.equals(previous)) {
                return false;
            }
            previous = entry;
        }
        return true;
    }

    /**
     * Stores input text and selection direction without depending on JavaFX.
     *
     * @param text exact input text.
     * @param anchor selection anchor in the text.
     * @param caret caret position in the text.
     */
    record InputState(String text, int anchor, int caret) {
        InputState {
            requireNonNull(text);
            assert anchor >= 0 && anchor <= text.length() : "Selection anchor must be within input text";
            assert caret >= 0 && caret <= text.length() : "Caret must be within input text";
        }

        static InputState createAtEnd(String text) {
            return new InputState(text, text.length(), text.length());
        }

        private boolean isValid() {
            return text != null && anchor >= 0 && anchor <= text.length() && caret >= 0 && caret <= text.length();
        }
    }
}
