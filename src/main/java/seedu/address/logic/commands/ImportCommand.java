package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PERSONS;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.storage.CsvMemberReader;
import seedu.address.storage.CsvMemberRecord;

/**
 * Appends valid, nonduplicate CSV members and reports every rejected record.
 */
public class ImportCommand extends Command {
    public static final String COMMAND_WORD = "import";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Imports valid members from a UTF-8 CSV file.\n"
            + "Parameters: FILE_PATH (use double quotes around paths containing spaces)\n"
            + "CSV header: " + CsvMemberReader.HEADER + "\n"
            + "Example: " + COMMAND_WORD + " \"members.csv\"";
    public static final String MESSAGE_SUCCESS = "Imported %1$d member(s); rejected %2$d record(s) from %3$s.";
    public static final String MESSAGE_FAILURE = "Could not import %1$s: %2$s\nNo members were imported.";
    public static final String MESSAGE_DUPLICATE = "CSV record %1$d: email \"%2$s\" duplicates an existing member "
            + "or an earlier accepted CSV record.";
    public static final String MESSAGE_SOURCE_IS_DATA_FILE = "The import source must differ from the directory's "
            + "JSON save file. No members were imported.";
    public static final String MESSAGE_SOURCE_OUTSIDE_HOME = "Cannot import this file: CSV files must be inside the "
            + "folder containing NUSocietyDesk.jar or one of its subfolders.\n"
            + "Move the CSV into that folder and retry. No members were imported.";

    private static final Logger logger = LogsCenter.getLogger(ImportCommand.class);

    private final Path filePath;

    /**
     * Creates a command to import members from {@code filePath} when executed.
     */
    public ImportCommand(Path filePath) {
        this.filePath = requireNonNull(filePath);
    }

    public Path getFilePath() {
        return filePath;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<CsvMemberRecord> records;
        try {
            records = new CsvMemberReader().read(filePath);
        } catch (IOException | IllegalValueException e) {
            logger.warning("CSV import stopped because the source could not be read or its format was invalid.");
            throw new CommandException(String.format(MESSAGE_FAILURE, filePath, e.getMessage()), e);
        }

        AddressBook combinedDirectory = new AddressBook(model.getAddressBook());
        List<String> rejections = new ArrayList<>();
        int importedCount = 0;
        for (CsvMemberRecord record : records) {
            if (record.getMember().isEmpty()) {
                rejections.add(record.getRejectionReason());
                continue;
            }
            Person member = record.getMember().orElseThrow();
            if (combinedDirectory.hasPerson(member)) {
                rejections.add(String.format(MESSAGE_DUPLICATE, record.getRecordNumber(), member.getEmail()));
                continue;
            }
            combinedDirectory.addPerson(member);
            importedCount++;
        }

        assert importedCount + rejections.size() == records.size() : "Every CSV record has an import outcome";
        logger.info("Prepared CSV import: " + importedCount + " accepted, " + rejections.size() + " rejected.");
        String feedback = String.format(MESSAGE_SUCCESS, importedCount, rejections.size(), filePath);
        if (!rejections.isEmpty()) {
            feedback += "\nRejected records:\n" + String.join("\n", rejections);
        }
        if (importedCount == 0) {
            throw new CommandException(feedback + "\nNo members were imported.");
        }

        model.setAddressBook(combinedDirectory);
        model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        return new CommandResult(feedback);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        return other instanceof ImportCommand otherCommand && filePath.equals(otherCommand.filePath);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("filePath", filePath).toString();
    }
}
