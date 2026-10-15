package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PERSONS;

import java.util.List;

import seedu.address.commons.core.index.Index;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.model.person.Status;

/**
 * Restores a former member identified by their displayed index to active status.
 */
public class RestoreCommand extends Command {

    public static final String COMMAND_WORD = "restore";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Restores a former member identified by the index number in the displayed person list.\n"
            + "Parameters: INDEX (must be a positive integer)\n"
            + "Example: " + COMMAND_WORD + " 1";
    public static final String MESSAGE_SUCCESS = "Restored member to active status: %1$s";
    public static final String MESSAGE_ALREADY_ACTIVE = "Member is already active. No changes were made.";

    private final Index targetIndex;

    /**
     * Creates a command to restore the member at {@code targetIndex} in the displayed list.
     */
    public RestoreCommand(Index targetIndex) {
        this.targetIndex = requireNonNull(targetIndex);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Person> lastShownList = model.getFilteredPersonList();
        if (targetIndex.getZeroBased() >= lastShownList.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        }

        Person personToRestore = lastShownList.get(targetIndex.getZeroBased());
        if (personToRestore.getStatus() == Status.ACTIVE) {
            throw new CommandException(MESSAGE_ALREADY_ACTIVE);
        }

        Person restoredPerson = new Person(personToRestore.getName(), personToRestore.getPhone(),
                personToRestore.getEmail(), personToRestore.getAddress(), Status.ACTIVE, personToRestore.getTags());
        model.setPerson(personToRestore, restoredPerson);
        model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        return new CommandResult(String.format(MESSAGE_SUCCESS, Messages.formatProvidedDetails(restoredPerson)));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        return other instanceof RestoreCommand otherCommand && targetIndex.equals(otherCommand.targetIndex);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("targetIndex", targetIndex).toString();
    }
}
