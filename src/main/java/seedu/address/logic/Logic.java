package seedu.address.logic;

import java.util.function.BooleanSupplier;

import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.StatusFilter;
import seedu.address.model.person.Person;

/**
 * API of the Logic component
 */
public interface Logic {
    /**
     * Executes the command and returns the result.
     * Cancels {@code clear} because no confirmation is supplied.
     * @param commandText The command as entered by the user.
     * @return the result of the command execution.
     * @throws CommandException If an error occurs during command execution.
     * @throws ParseException If an error occurs during parsing.
     */
    CommandResult execute(String commandText) throws CommandException, ParseException;

    /**
     * Executes the command, requesting confirmation before clearing any members.
     *
     * @param commandText The command as entered by the user.
     * @param clearConfirmation Supplies approval for a valid {@code clear} command before any data changes.
     * @return the execution result, or cancellation feedback if clearing is declined.
     * @throws CommandException If an error occurs during command execution.
     * @throws ParseException If an error occurs during parsing.
     */
    CommandResult execute(String commandText, BooleanSupplier clearConfirmation)
            throws CommandException, ParseException;

    /** Returns an unmodifiable view of the filtered list of persons */
    ObservableList<Person> getFilteredPersonList();

    /** Selects which member statuses are shown and used for displayed indices. */
    void setStatusFilter(StatusFilter statusFilter);

    /** Returns the selected status view. */
    StatusFilter getStatusFilter();

    /**
     * Returns the user prefs' GUI settings.
     */
    GuiSettings getGuiSettings();

    /**
     * Set the user prefs' GUI settings.
     */
    void setGuiSettings(GuiSettings guiSettings);
}
