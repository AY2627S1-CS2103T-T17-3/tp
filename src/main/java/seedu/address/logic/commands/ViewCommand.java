package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Locale;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.Model;
import seedu.address.model.StatusFilter;

/** Selects the status view without changing member records or the current search. */
public class ViewCommand extends Command {

    public static final String COMMAND_WORD = "view";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Selects which members are shown.\n"
            + "Format: " + COMMAND_WORD + " all|active|archived\n"
            + "Example: " + COMMAND_WORD + " archived";
    public static final String MESSAGE_SUCCESS = "Switched to %s view.";

    private final StatusFilter statusFilter;

    public ViewCommand(StatusFilter statusFilter) {
        this.statusFilter = requireNonNull(statusFilter);
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean requiresSave() {
        return false;
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.setStatusFilter(statusFilter);
        return new CommandResult(String.format(MESSAGE_SUCCESS, statusFilter.name().toLowerCase(Locale.ROOT)));
    }

    @Override
    public boolean equals(Object other) {
        return other == this || (other instanceof ViewCommand otherCommand
                && statusFilter == otherCommand.statusFilter);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("statusFilter", statusFilter)
                .toString();
    }
}
