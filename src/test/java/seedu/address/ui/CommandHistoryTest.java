package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        boolean areAssertionsExpected = Boolean.parseBoolean(System.getProperty("history.assertions.enabled", "true"));
        assertEquals(areAssertionsExpected, CommandHistory.class.desiredAssertionStatus());
    }

    @Test
    public void navigate_emptyHistory_doesNothing() {
        assertTrue(recallOlder(currentInput).isEmpty());
        assertTrue(recallNewer().isEmpty());
    }

    @Test
    public void navigate_multipleEntries_preservesOrderAndDraftSelection() {
        history.record("list");
        history.record("help");
        assertOlder("help");
        assertEquals(InputState.createAtEnd("list"), recallOlder(InputState.createAtEnd("edited help")).orElseThrow());
        assertTrue(recallOlder(InputState.createAtEnd("edited list")).isEmpty());
        assertEquals(InputState.createAtEnd("edited help"), recallNewer().orElseThrow());
        assertEquals(draft, recallNewer().orElseThrow());
        assertTrue(recallNewer().isEmpty());

        InputState changedDraft = new InputState("list again", 0, 4);
        assertEquals("edited help", recallOlder(changedDraft).orElseThrow().text());
        assertOlder("edited list");
        assertEquals("edited help", recallNewer().orElseThrow().text());
        assertEquals(changedDraft, recallNewer().orElseThrow());
    }

    @Test
    public void record_exactDuplicates_preservesRawTextAndNonconsecutiveDuplicates() {
        for (String command : List.of("list", "list", " list ", "LIST", "list")) {
            history.record(command);
        }
        assertHistoryNewestFirst(List.of("list", "LIST", " list ", "list"));
    }

    @Test
    public void record_blankInput_preservesBrowsingAndDuplicateDetection() {
        history.record("list");
        history.record("");
        history.record(" \t\n\u2003");
        history.record("list");
        assertOlder("list");
        history.record(" \t");
        assertTrue(recallOlder(currentInput).isEmpty());
        assertEquals(draft, recallNewer().orElseThrow());
        history.record("\u00a0");
        assertOlder("\u00a0");
        assertOlder("list");
        assertTrue(recallOlder(currentInput).isEmpty());
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
                boundedInput = boundedHistory.recallOlder(boundedInput).orElseThrow();
                assertEquals("command " + i, boundedInput.text());
            }
            assertTrue(boundedHistory.recallOlder(boundedInput).isEmpty());
            for (int i = count - 98; i <= count; i++) {
                boundedInput = boundedHistory.recallNewer(boundedInput).orElseThrow();
                assertEquals("command " + i, boundedInput.text());
            }
            assertEquals(draft, boundedHistory.recallNewer(boundedInput).orElseThrow());
            assertTrue(boundedHistory.recallNewer(draft).isEmpty());
        }
    }

    @Test
    public void record_duplicateWhileBrowsingAtCapacity_resetsWithoutEviction() {
        populateHistory();
        assertOlder("command 100");
        assertOlder("command 99");
        history.record("command 100");
        assertTrue(recallNewer().isEmpty());
        history.record(" ");
        for (int i = 100; i >= 1; i--) {
            assertOlder("command " + i);
        }
        assertTrue(recallOlder(currentInput).isEmpty());
    }

    @Test
    public void record_recalledOldestAtCapacity_evictsOldestAndAppendsSubmission() {
        populateHistory();
        for (int i = 100; i >= 1; i--) {
            assertOlder("command " + i);
        }
        history.record("command 1");
        InputState newDraft = InputState.createAtEnd("");
        assertEquals("command 1", recallOlder(newDraft).orElseThrow().text());
        for (int i = 100; i >= 2; i--) {
            assertOlder("command " + i);
        }
        assertTrue(recallOlder(currentInput).isEmpty());
        for (int i = 0; i < 99; i++) {
            recallNewer();
        }
        assertEquals(newDraft, recallNewer().orElseThrow());
    }

    @Test
    public void record_newSubmissionWhileBrowsing_discardsOldDraft() {
        history.record("list");
        assertOlder("list");
        history.record("help");
        assertTrue(recallNewer().isEmpty());
        InputState newDraft = InputState.createAtEnd("new draft");
        recallOlder(newDraft);
        assertEquals(newDraft, recallNewer().orElseThrow());
        assertTrue(new CommandHistory().recallOlder(newDraft).isEmpty());
    }

    @Test
    public void navigate_sameCommandsAtDifferentPositions_keepsEditsSeparate() {
        history.record("list");
        history.record("help");
        history.record("list");
        assertOlder("list");
        recallOlder(InputState.createAtEnd("edited newest list"));
        assertOlder("list");
        currentInput = InputState.createAtEnd("edited oldest list");
        assertEquals("help", recallNewer().orElseThrow().text());
        assertEquals("edited newest list", recallNewer().orElseThrow().text());
        assertEquals(draft, recallNewer().orElseThrow());
        assertOlder("edited newest list");
        assertOlder("help");
        assertOlder("edited oldest list");
    }

    @Test
    public void navigate_blankAndRevertedEdits_retainsCurrentText() {
        history.record("list");
        assertOlder("list");
        currentInput = InputState.createAtEnd("");
        history.record(currentInput.text());
        assertEquals(draft, recallNewer().orElseThrow());
        assertOlder("");
        currentInput = InputState.createAtEnd("list");
        recallNewer();
        assertOlder("list");
    }

    @Test
    public void record_editedSubmission_keepsOriginalAndClearsOtherEdits() {
        history.record("list");
        history.record("help");
        assertOlder("help");
        recallOlder(InputState.createAtEnd("edited help"));
        currentInput = InputState.createAtEnd("edited list");
        recallNewer();
        history.record(currentInput.text());
        assertHistoryNewestFirst(List.of("edited help", "help", "list"));
    }

    @Test
    public void record_duplicateAfterEditingAtCapacity_clearsEditsWithoutEviction() {
        populateHistory();
        assertOlder("command 100");
        recallOlder(InputState.createAtEnd("edited newest"));
        history.record("command 100");
        for (int i = 100; i >= 1; i--) {
            assertOlder("command " + i);
        }
        assertTrue(recallOlder(currentInput).isEmpty());
        currentInput = InputState.createAtEnd("edited oldest");
        recallNewer();
        history.record("new command");
        assertOlder("new command");
        for (int i = 100; i >= 2; i--) {
            assertOlder("command " + i);
        }
        assertTrue(recallOlder(currentInput).isEmpty());
    }

    @Test
    public void navigate_failedSubmission_hasNoDuplicateDraftPosition() {
        history.record("list");
        history.record("invalid command");
        history.selectLatestAfterFailure();
        currentInput = InputState.createAtEnd("edited invalid command");
        assertTrue(recallNewer().isEmpty());
        assertTrue(recallNewer().isEmpty());
        assertOlder("list");
        assertEquals("edited invalid command", recallNewer().orElseThrow().text());
        assertTrue(recallNewer().isEmpty());
        assertOlder("list");
        history.record("success");
        currentInput = draft;
        assertOlder("success");
        assertEquals(draft, recallNewer().orElseThrow());
    }

    @Test
    public void navigate_failedSubmissionWithBlankEdit_preservesEditAndBoundary() {
        history.record("list");
        history.record("invalid command");
        history.selectLatestAfterFailure();
        currentInput = InputState.createAtEnd("");
        history.record(currentInput.text());
        assertTrue(recallNewer().isEmpty());
        assertOlder("list");
        assertEquals("", recallNewer().orElseThrow().text());
        assertTrue(recallNewer().isEmpty());
        history.record("invalid command");
        history.selectLatestAfterFailure();
        currentInput = InputState.createAtEnd("invalid command");
        assertOlder("list");
        assertTrue(recallOlder(currentInput).isEmpty());
    }

    @Test
    public void operations_fineLogging_recordsMetadataOnly() {
        Logger logger = Logger.getLogger("ab3.CommandHistory");
        Level previousLevel = logger.getLevel();
        boolean wereParentHandlersEnabled = logger.getUseParentHandlers();
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new RecordingHandler(records);
        logger.addHandler(handler);
        logger.setLevel(Level.FINE);
        logger.setUseParentHandlers(false);
        try {
            recallOlder(currentInput);
            recallNewer();
            history.record(" ");
            history.record("private-command-one");
            history.record("private-command-one");
            history.record("private-command-two");
            recallOlder(currentInput);
            recallOlder(currentInput);
            recallNewer();
            recallNewer();
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
            logger.setUseParentHandlers(wereParentHandlersEnabled);
        }
    }

    private void populateHistory() {
        for (int i = 1; i <= 100; i++) {
            history.record("command " + i);
        }
    }

    private void assertOlder(String expected) {
        assertEquals(InputState.createAtEnd(expected), recallOlder(currentInput).orElseThrow());
    }

    private void assertHistoryNewestFirst(List<String> expectedCommands) {
        expectedCommands.forEach(this::assertOlder);
        assertTrue(recallOlder(currentInput).isEmpty());
    }

    private Optional<InputState> recallOlder(InputState input) {
        Optional<InputState> result = history.recallOlder(input);
        currentInput = result.orElse(input);
        return result;
    }

    private Optional<InputState> recallNewer() {
        Optional<InputState> result = history.recallNewer(currentInput);
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
