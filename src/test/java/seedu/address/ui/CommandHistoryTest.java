package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import seedu.address.ui.CommandHistory.InputState;

public class CommandHistoryTest {
    private final CommandHistory history = new CommandHistory();
    private final InputState draft = new InputState("find n/Alex", 9, 2);
    private InputState currentInput = draft;

    @Test
    public void assertions_configuredMode_matchesTestTask() {
        boolean expected = Boolean.parseBoolean(System.getProperty("history.assertions.enabled", "true"));
        assertEquals(expected, CommandHistory.class.desiredAssertionStatus());
    }

    @Test
    public void navigate_emptyHistory_doesNothing() {
        assertTrue(older(currentInput).isEmpty());
        assertTrue(newer().isEmpty());
    }

    @Test
    public void navigate_multipleEntries_preservesOrderAndDraftSelection() {
        history.record("list");
        history.record("help");
        assertOlder("help");
        assertEquals(InputState.atEnd("list"), older(InputState.atEnd("edited help")).orElseThrow());
        assertTrue(older(InputState.atEnd("edited list")).isEmpty());
        assertEquals(InputState.atEnd("edited help"), newer().orElseThrow());
        assertEquals(draft, newer().orElseThrow());
        assertTrue(newer().isEmpty());

        InputState changedDraft = new InputState("list again", 0, 4);
        assertEquals("edited help", older(changedDraft).orElseThrow().text());
        assertOlder("edited list");
        assertEquals("edited help", newer().orElseThrow().text());
        assertEquals(changedDraft, newer().orElseThrow());
    }

    @Test
    public void record_exactDuplicates_preservesRawTextAndNonconsecutiveDuplicates() {
        for (String command : List.of("list", "list", " list ", "LIST", "list")) {
            assertTrue(history.record(command));
        }
        assertHistoryNewestFirst(List.of("list", "LIST", " list ", "list"));
    }

    @Test
    public void record_blankInput_preservesBrowsingAndDuplicateDetection() {
        history.record("list");
        assertFalse(history.record(""));
        assertFalse(history.record(" \t\n\u2003"));
        history.record("list");
        assertOlder("list");
        assertFalse(history.record(" \t"));
        assertTrue(older(currentInput).isEmpty());
        assertEquals(draft, newer().orElseThrow());
        assertTrue(history.record("\u00a0"));
        assertOlder("\u00a0");
        assertOlder("list");
        assertTrue(older(currentInput).isEmpty());
    }

    @Test
    public void record_atCapacity_retainsOnlyNewestHundred() {
        for (int count : List.of(100, 101, 1000)) {
            CommandHistory boundedHistory = new CommandHistory();
            for (int i = 1; i <= count; i++) {
                boundedHistory.record("command " + i);
            }
            InputState boundedInput = draft;
            for (int i = count; i > count - 100; i--) {
                boundedInput = boundedHistory.older(boundedInput).orElseThrow();
                assertEquals("command " + i, boundedInput.text());
            }
            assertTrue(boundedHistory.older(boundedInput).isEmpty());
            for (int i = count - 98; i <= count; i++) {
                boundedInput = boundedHistory.newer(boundedInput).orElseThrow();
                assertEquals("command " + i, boundedInput.text());
            }
            assertEquals(draft, boundedHistory.newer(boundedInput).orElseThrow());
            assertTrue(boundedHistory.newer(draft).isEmpty());
        }
    }

    @Test
    public void record_duplicateWhileBrowsingAtCapacity_resetsWithoutEviction() {
        populateHistory();
        assertOlder("command 100");
        assertOlder("command 99");
        assertTrue(history.record("command 100"));
        assertTrue(newer().isEmpty());
        assertFalse(history.record(" "));
        for (int i = 100; i >= 1; i--) {
            assertOlder("command " + i);
        }
        assertTrue(older(currentInput).isEmpty());
    }

    @Test
    public void record_recalledOldestAtCapacity_evictsOldestAndAppendsSubmission() {
        populateHistory();
        for (int i = 100; i >= 1; i--) {
            assertOlder("command " + i);
        }
        history.record("command 1");
        InputState newDraft = InputState.atEnd("");
        assertEquals("command 1", older(newDraft).orElseThrow().text());
        for (int i = 100; i >= 2; i--) {
            assertOlder("command " + i);
        }
        assertTrue(older(currentInput).isEmpty());
        for (int i = 0; i < 99; i++) {
            newer();
        }
        assertEquals(newDraft, newer().orElseThrow());
    }

    @Test
    public void record_newSubmissionWhileBrowsing_discardsOldDraft() {
        history.record("list");
        assertOlder("list");
        history.record("help");
        assertTrue(newer().isEmpty());
        InputState newDraft = InputState.atEnd("new draft");
        older(newDraft);
        assertEquals(newDraft, newer().orElseThrow());
        assertTrue(new CommandHistory().older(newDraft).isEmpty());
    }

    @Test
    public void navigate_sameCommandsAtDifferentPositions_keepsEditsSeparate() {
        history.record("list");
        history.record("help");
        history.record("list");
        assertOlder("list");
        older(InputState.atEnd("edited newest list"));
        assertOlder("list");
        currentInput = InputState.atEnd("edited oldest list");
        assertEquals("help", newer().orElseThrow().text());
        assertEquals("edited newest list", newer().orElseThrow().text());
        assertEquals(draft, newer().orElseThrow());
        assertOlder("edited newest list");
        assertOlder("help");
        assertOlder("edited oldest list");
    }

    @Test
    public void navigate_blankAndRevertedEdits_retainsCurrentText() {
        history.record("list");
        assertOlder("list");
        currentInput = InputState.atEnd("");
        assertFalse(history.record(currentInput.text()));
        assertEquals(draft, newer().orElseThrow());
        assertOlder("");
        currentInput = InputState.atEnd("list");
        newer();
        assertOlder("list");
    }

    @Test
    public void record_editedSubmission_keepsOriginalAndClearsOtherEdits() {
        history.record("list");
        history.record("help");
        assertOlder("help");
        older(InputState.atEnd("edited help"));
        currentInput = InputState.atEnd("edited list");
        newer();
        history.record(currentInput.text());
        assertHistoryNewestFirst(List.of("edited help", "help", "list"));
    }

    @Test
    public void record_duplicateAfterEditingAtCapacity_clearsEditsWithoutEviction() {
        populateHistory();
        assertOlder("command 100");
        older(InputState.atEnd("edited newest"));
        history.record("command 100");
        for (int i = 100; i >= 1; i--) {
            assertOlder("command " + i);
        }
        assertTrue(older(currentInput).isEmpty());
        currentInput = InputState.atEnd("edited oldest");
        newer();
        history.record("new command");
        assertOlder("new command");
        for (int i = 100; i >= 2; i--) {
            assertOlder("command " + i);
        }
        assertTrue(older(currentInput).isEmpty());
    }

    @Test
    public void navigate_failedSubmission_hasNoDuplicateDraftPosition() {
        history.record("list");
        history.record("invalid command");
        history.selectLatestAfterFailure();
        currentInput = InputState.atEnd("edited invalid command");
        assertTrue(newer().isEmpty());
        assertTrue(newer().isEmpty());
        assertOlder("list");
        assertEquals("edited invalid command", newer().orElseThrow().text());
        assertTrue(newer().isEmpty());
        assertOlder("list");
        history.record("success");
        currentInput = draft;
        assertOlder("success");
        assertEquals(draft, newer().orElseThrow());
    }

    @Test
    public void navigate_failedSubmissionWithBlankEdit_preservesEditAndBoundary() {
        history.record("list");
        history.record("invalid command");
        history.selectLatestAfterFailure();
        currentInput = InputState.atEnd("");
        assertFalse(history.record(currentInput.text()));
        assertTrue(newer().isEmpty());
        assertOlder("list");
        assertEquals("", newer().orElseThrow().text());
        assertTrue(newer().isEmpty());
        history.record("invalid command");
        history.selectLatestAfterFailure();
        currentInput = InputState.atEnd("invalid command");
        assertOlder("list");
        assertTrue(older(currentInput).isEmpty());
    }

    @Test
    public void operations_fineLogging_recordsMetadataOnly() {
        Logger logger = Logger.getLogger("ab3.CommandHistory");
        Level previousLevel = logger.getLevel();
        boolean previousParentHandlers = logger.getUseParentHandlers();
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new RecordingHandler(records);
        logger.addHandler(handler);
        logger.setLevel(Level.FINE);
        logger.setUseParentHandlers(false);
        try {
            older(currentInput);
            newer();
            history.record(" ");
            history.record("private-command-one");
            history.record("private-command-one");
            history.record("private-command-two");
            older(currentInput);
            older(currentInput);
            newer();
            newer();
            assertEquals(List.of(
                    "Command history navigation ignored: direction=older, reason=boundary.",
                    "Command history navigation ignored: direction=newer, reason=boundary.",
                    "Command history blank submission ignored.",
                    "Command history recorded: size=1, evictedOldest=false.",
                    "Command history duplicate suppressed: size=1.",
                    "Command history recorded: size=2, evictedOldest=false.",
                    "Command history navigated older: before=1, after=1.",
                    "Command history navigated older: before=0, after=2.",
                    "Command history navigated newer: before=1, after=1.",
                    "Command history draft restored: size=2."),
                    records.stream().map(LogRecord::getMessage).toList());
            populateHistory();
            assertEquals("Command history recorded: size=100, evictedOldest=true.",
                    records.getLast().getMessage());
            history.selectLatestAfterFailure();
            assertEquals("Command history selected failed entry: size=100.", records.getLast().getMessage());
            assertTrue(records.stream().allMatch(record -> record.getLevel() == Level.FINE));
            assertTrue(records.stream().noneMatch(record -> record.getMessage().contains("private-command")
                    || record.getMessage().contains(draft.text())));
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(previousLevel);
            logger.setUseParentHandlers(previousParentHandlers);
        }
    }

    private void populateHistory() {
        for (int i = 1; i <= 100; i++) {
            history.record("command " + i);
        }
    }

    private void assertOlder(String expected) {
        assertEquals(InputState.atEnd(expected), older(currentInput).orElseThrow());
    }

    private void assertHistoryNewestFirst(List<String> expected) {
        expected.forEach(this::assertOlder);
        assertTrue(older(currentInput).isEmpty());
    }

    private Optional<InputState> older(InputState input) {
        Optional<InputState> result = history.older(input);
        currentInput = result.orElse(input);
        return result;
    }

    private Optional<InputState> newer() {
        Optional<InputState> result = history.newer(currentInput);
        currentInput = result.orElse(currentInput);
        return result;
    }

    private static class RecordingHandler extends Handler {
        private final List<LogRecord> records;

        RecordingHandler(List<LogRecord> records) {
            this.records = records;
        }

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    }
}
