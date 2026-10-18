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

    private final Deque<String> beforeCursor = new ArrayDeque<>();
    private final Deque<String> afterCursor = new ArrayDeque<>();
    private final Map<Integer, String> editedCommands = new HashMap<>();
    private InputState draft;

    CommandHistory() {
        assertValidState();
    }

    /**
     * Records a submission before execution, returning false only for blank input.
     * Consecutive duplicates still end browsing and must still be executed.
     */
    boolean record(String command) {
        requireNonNull(command);
        assertValidState();
        if (command.isBlank()) {
            logger.fine("Command history blank submission ignored.");
            assertValidState();
            return false;
        }

        int previousSize = size();
        resetCursor();
        boolean isDuplicate = command.equals(beforeCursor.peekLast());
        if (isDuplicate) {
            logger.fine(() -> "Command history duplicate suppressed: size=" + size() + ".");
        } else {
            boolean isEvicted = beforeCursor.size() == MAX_HISTORY_SIZE;
            if (isEvicted) {
                assert !beforeCursor.isEmpty() : "Eviction requires an oldest entry";
                beforeCursor.removeFirst();
            }
            beforeCursor.addLast(command);
            logger.fine(() -> "Command history recorded: size=" + size() + ", evictedOldest=" + isEvicted + ".");
        }

        assert afterCursor.isEmpty() && draft == null && editedCommands.isEmpty() : "Submission must end browsing";
        assert size() == (isDuplicate ? previousSize : Math.min(previousSize + 1, MAX_HISTORY_SIZE))
                : "Recording must preserve duplicates or append with oldest-first eviction";
        assertValidState();
        return true;
    }

    /**
     * Selects the latest failed submission without creating a second copy at the draft position.
     */
    void selectLatestAfterFailure() {
        assertValidState();
        int previousSize = size();
        resetCursor();
        assert !beforeCursor.isEmpty() : "A failure must follow a recorded submission";
        afterCursor.addFirst(beforeCursor.removeLast());
        logger.fine(() -> "Command history selected failed entry: size=" + size() + ".");
        assert size() == previousSize : "Selecting a failure must preserve history size";
        assertValidState();
    }

    /**
     * Recalls an older command, retaining entry edits and capturing the draft when entering history.
     * Returns an empty optional at the boundary, distinct from an identical-text transition.
     */
    Optional<InputState> older(InputState currentInput) {
        requireNonNull(currentInput);
        assertValidState();
        if (beforeCursor.isEmpty()) {
            logger.fine("Command history navigation ignored: direction=older, reason=boundary.");
            assertValidState();
            return Optional.empty();
        }

        int previousSize = size();
        if (afterCursor.isEmpty()) {
            draft = currentInput;
        } else {
            rememberEdit(currentInput);
        }
        assert !beforeCursor.isEmpty() : "Older navigation requires an older entry";
        afterCursor.addFirst(beforeCursor.removeLast());
        logger.fine(() -> "Command history navigated older: before=" + beforeCursor.size()
                + ", after=" + afterCursor.size() + ".");
        assert size() == previousSize : "Navigation must preserve history size";
        assertValidState();
        return Optional.of(selectedInput());
    }

    /**
     * Retains entry edits and recalls a newer command or restores the draft. Returns empty at the boundary.
     */
    Optional<InputState> newer(InputState currentInput) {
        requireNonNull(currentInput);
        assertValidState();
        if (afterCursor.isEmpty() || (afterCursor.size() == 1 && draft == null)) {
            logger.fine("Command history navigation ignored: direction=newer, reason=boundary.");
            assertValidState();
            return Optional.empty();
        }

        int previousSize = size();
        rememberEdit(currentInput);
        assert !afterCursor.isEmpty() : "Newer navigation requires a selected entry";
        beforeCursor.addLast(afterCursor.removeFirst());
        InputState nextInput;
        if (afterCursor.isEmpty()) {
            nextInput = draft;
            draft = null;
            logger.fine(() -> "Command history draft restored: size=" + size() + ".");
        } else {
            nextInput = selectedInput();
            logger.fine(() -> "Command history navigated newer: before=" + beforeCursor.size()
                    + ", after=" + afterCursor.size() + ".");
        }
        assert size() == previousSize : "Navigation must preserve history size";
        assertValidState();
        return Optional.of(nextInput);
    }

    private void resetCursor() {
        while (!afterCursor.isEmpty()) {
            assert !afterCursor.isEmpty() : "Cursor reset requires a selected or newer entry";
            beforeCursor.addLast(afterCursor.removeFirst());
        }
        draft = null;
        editedCommands.clear();
    }

    /**
     * Saves an entry's unsubmitted text by its stable chronological index, not by its command string.
     */
    private void rememberEdit(InputState currentInput) {
        assert !afterCursor.isEmpty() : "Remembering an edit requires a selected entry";
        int index = beforeCursor.size();
        if (currentInput.text().equals(afterCursor.peekFirst())) {
            editedCommands.remove(index);
        } else {
            editedCommands.put(index, currentInput.text());
        }
    }

    private InputState selectedInput() {
        assert !afterCursor.isEmpty() : "Recalling input requires a selected entry";
        String text = editedCommands.getOrDefault(beforeCursor.size(), afterCursor.peekFirst());
        return InputState.atEnd(text);
    }

    private int size() {
        return beforeCursor.size() + afterCursor.size();
    }

    private void assertValidState() {
        assert beforeCursor != null && afterCursor != null : "History deques must exist";
        assert size() <= MAX_HISTORY_SIZE : "Combined history must not exceed its capacity";
        assert hasValidEntries() : "History must contain nonblank entries without adjacent duplicates";
        assert draft == null || !afterCursor.isEmpty() : "A saved draft requires an active browsing position";
        assert draft == null || draft.isValid() : "Saved draft text and selection must be valid";
        assert editedCommands.size() <= size() : "Each retained entry can have at most one pending edit";
        assert editedCommands.entrySet().stream().allMatch(entry -> entry.getKey() >= 0
                && entry.getKey() < size() && entry.getValue() != null)
                : "Pending edits must refer to retained entries";
    }

    private boolean hasValidEntries() {
        String previous = null;
        for (String entry : Stream.concat(beforeCursor.stream(), afterCursor.stream()).toList()) {
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

        static InputState atEnd(String text) {
            return new InputState(text, text.length(), text.length());
        }

        private boolean isValid() {
            return text != null && anchor >= 0 && anchor <= text.length() && caret >= 0 && caret <= text.length();
        }
    }
}
