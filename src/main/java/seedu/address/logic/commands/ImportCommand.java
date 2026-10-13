package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PERSONS;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.storage.CsvMemberReader;

/**
 * Appends every member from a valid CSV, rejecting the entire import on any invalid or duplicate record.
 */
public class ImportCommand extends Command {
    public static final String COMMAND_WORD = "import";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Imports all members from a UTF-8 CSV file.\n"
            + "Parameters: FILE_PATH (use double quotes around paths containing spaces)\n"
            + "CSV header: " + CsvMemberReader.HEADER + "\n"
            + "Example: " + COMMAND_WORD + " \"members.csv\"";
    public static final String MESSAGE_SUCCESS = "Imported %1$d member(s) from %2$s.";
    public static final String MESSAGE_FAILURE = "Could not import %1$s: %2$s\nNo members were imported.";
    public static final String MESSAGE_DUPLICATE = "CSV record %1$d: email \"%2$s\" duplicates an existing member "
            + "or an earlier CSV record.";
    public static final String MESSAGE_SOURCE_IS_DATA_FILE = "The import source must differ from the directory's "
            + "JSON save file. No members were imported.";

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
        List<Person> importedMembers;
        try {
            importedMembers = new CsvMemberReader().read(filePath);
        } catch (IOException | IllegalValueException e) {
            throw new CommandException(String.format(MESSAGE_FAILURE, filePath, e.getMessage()), e);
        }

        AddressBook combinedDirectory = new AddressBook(model.getAddressBook());
        for (int i = 0; i < importedMembers.size(); i++) {
            Person member = importedMembers.get(i);
            if (combinedDirectory.hasPerson(member)) {
                String reason = String.format(MESSAGE_DUPLICATE, i + 2, member.getEmail());
                throw new CommandException(String.format(MESSAGE_FAILURE, filePath, reason));
            }
            combinedDirectory.addPerson(member);
        }

        model.setAddressBook(combinedDirectory);
        model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        return new CommandResult(String.format(MESSAGE_SUCCESS, importedMembers.size(), filePath));
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
