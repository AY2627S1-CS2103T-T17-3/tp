package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.function.Predicate;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.model.Model;
import seedu.address.model.person.Person;

/**
 * Finds and lists all persons in the address book matching the given predicate.
 */
public class FindCommand extends Command {

    public static final String COMMAND_WORD = "find";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Search members using one field.\n"
            + "Name:  " + COMMAND_WORD + " [n/]KEYWORD [MORE_KEYWORDS]...\n"
            + "Phone: " + COMMAND_WORD + " p/SUBSTRING\n"
            + "Email: " + COMMAND_WORD + " e/SUBSTRING\n"
            + "Tag:   " + COMMAND_WORD + " t/TAG (one exact tag, ignoring case)\n"
            + "Examples: " + COMMAND_WORD + " alice bob; " + COMMAND_WORD + " p/9123; " + COMMAND_WORD + " e/alice; "
            + COMMAND_WORD + " t/publicity";

    public static final String MESSAGE_MULTIPLE_FIELDS = "Specify only one search field per find command.";

    public static final String MESSAGE_UPPERCASE_PREFIX = "Search prefixes must be lowercase. Use n/, p/, e/ or t/.";

    public static final String MESSAGE_MULTIPLE_PHONE_SUBSTRINGS = "Phone searches accept only one substring. "
            + "Search each substring in a separate find command.";

    public static final String MESSAGE_INVALID_PHONE_SUBSTRING = "Phone searches accept digits (0-9) only. "
            + "Example: find p/9123.";

    public static final String MESSAGE_MULTIPLE_EMAIL_SUBSTRINGS = "Email searches accept only one substring. "
            + "Search each substring in a separate find command.";

    public static final String MESSAGE_MULTIPLE_TAGS = "Tag searches accept only one tag. "
            + "Example: find t/publicity.";

    private static final Logger logger = LogsCenter.getLogger(FindCommand.class);

    private final Predicate<Person> predicate;

    /**
     * Creates a command that lists persons matching the given {@code predicate}.
     */
    public FindCommand(Predicate<Person> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.updateFilteredPersonList(predicate);
        int matchCount = model.getFilteredPersonList().size();
        logger.info("Find completed: " + matchCount + " members matched.");
        return new CommandResult(
                String.format(Messages.MESSAGE_PERSONS_LISTED_OVERVIEW, matchCount));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof FindCommand otherFindCommand)) {
            return false;
        }

        return predicate.equals(otherFindCommand.predicate);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("predicate", predicate)
                .toString();
    }
}
