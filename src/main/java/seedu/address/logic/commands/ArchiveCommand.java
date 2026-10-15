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
 * Archives a person identified using their displayed index.
 */
public class ArchiveCommand extends Command {

    public static final String COMMAND_WORD = "archive";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Archives the member identified by the index number in the displayed member list.\n"
            + "Parameters: INDEX (must be a positive integer)\n"
            + "Example: " + COMMAND_WORD + " 1";
    public static final String MESSAGE_SUCCESS = "Archived member: %1$s";
    public static final String MESSAGE_ALREADY_ARCHIVED = "This member is already archived.";

    private final Index targetIndex;

    /**
     * Creates a command to archive the person at {@code targetIndex} in the displayed list.
     */
    public ArchiveCommand(Index targetIndex) {
        this.targetIndex = requireNonNull(targetIndex);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Person> lastShownList = model.getFilteredPersonList();
        if (targetIndex.getZeroBased() >= lastShownList.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        }

        Person personToArchive = lastShownList.get(targetIndex.getZeroBased());
        if (personToArchive.getStatus() == Status.ARCHIVED) {
            throw new CommandException(MESSAGE_ALREADY_ARCHIVED);
        }

        Person archivedPerson = personToArchive.withStatus(Status.ARCHIVED);
        model.setPerson(personToArchive, archivedPerson);
        model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        return new CommandResult(String.format(MESSAGE_SUCCESS, Messages.formatProvidedDetails(archivedPerson)));
    }

    @Override
    public boolean equals(Object other) {
        return other == this || (other instanceof ArchiveCommand otherCommand
                && targetIndex.equals(otherCommand.targetIndex));
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("targetIndex", targetIndex)
                .toString();
    }
}
