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
 * Activates an archived person identified using their displayed index.
 */
public class ActivateCommand extends Command {

    public static final String COMMAND_WORD = "activate";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Activates the member identified by the index number in the displayed member list.\n"
            + "Parameters: INDEX (must be a positive integer)\n"
            + "Example: " + COMMAND_WORD + " 1";
    public static final String MESSAGE_SUCCESS = "Activated member: %1$s";
    public static final String MESSAGE_ALREADY_ACTIVE = "This member is already active.";

    private final Index targetIndex;

    /**
     * Creates a command to activate the person at {@code targetIndex} in the displayed list.
     */
    public ActivateCommand(Index targetIndex) {
        this.targetIndex = requireNonNull(targetIndex);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Person> lastShownList = model.getFilteredPersonList();
        if (targetIndex.getZeroBased() >= lastShownList.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        }

        Person personToActivate = lastShownList.get(targetIndex.getZeroBased());
        if (personToActivate.getStatus() == Status.ACTIVE) {
            throw new CommandException(MESSAGE_ALREADY_ACTIVE);
        }

        Person activePerson = personToActivate.withStatus(Status.ACTIVE);
        model.setPerson(personToActivate, activePerson);
        model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        return new CommandResult(String.format(MESSAGE_SUCCESS, Messages.formatProvidedDetails(activePerson)));
    }

    @Override
    public boolean equals(Object other) {
        return other == this || (other instanceof ActivateCommand otherCommand
                && targetIndex.equals(otherCommand.targetIndex));
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("targetIndex", targetIndex)
                .toString();
    }
}
