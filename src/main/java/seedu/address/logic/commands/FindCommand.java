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
    public static final String COMMAND_WORD_ALL = "findall";
    public static final String COMMAND_WORD_ANY = "findany";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Search members using one field.\n"
            + "Name:  " + COMMAND_WORD + " KEYWORD [MORE_KEYWORDS]... (match any keyword)\n"
            + "       " + COMMAND_WORD + " n/KEYWORD (one keyword only)\n"
            + "Phone: " + COMMAND_WORD + " p/SUBSTRING\n"
            + "Email: " + COMMAND_WORD + " e/SUBSTRING\n"
            + "Tag:   " + COMMAND_WORD + " t/TAG (one exact tag, ignoring case)\n"
            + "Examples: " + COMMAND_WORD + " alice bob; " + COMMAND_WORD + " p/9123; " + COMMAND_WORD + " e/alice; "
            + COMMAND_WORD + " t/publicity";

    public static final String MESSAGE_USAGE_ALL = COMMAND_WORD_ALL
            + ": Search members matching every condition (AND).\n"
            + "Format: " + COMMAND_WORD_ALL + " PREFIX/VALUE [PREFIX/VALUE]...\n"
            + "Use n/KEYWORD, p/DIGITS, e/SUBSTRING or t/TAG for every condition.\n"
            + "Provide at least one condition. Repeat prefixes to add conditions; use one value per occurrence.\n"
            + "Names match whole words; tags match exactly; both ignore case.\n"
            + "Phones match consecutive digits; emails match literal substrings, ignoring case.\n"
            + "Example: " + COMMAND_WORD_ALL + " n/John n/Tan t/exco";

    public static final String MESSAGE_MULTIPLE_CONDITION_VALUES = "Each %1$s condition accepts only one value. "
            + "Repeat the prefix for each additional condition, for example: findall n/John n/Tan.";

    public static final String MESSAGE_INVALID_PHONE_SUBSTRING_ALL = "Phone searches accept digits (0-9) only. "
            + "Example: findall p/9123.";

    public static final String MESSAGE_USAGE_ANY = COMMAND_WORD_ANY
            + ": Search members matching at least one condition (OR).\n"
            + "Format: " + COMMAND_WORD_ANY + " PREFIX/VALUE [PREFIX/VALUE]...\n"
            + "Use n/KEYWORD, p/DIGITS, e/SUBSTRING or t/TAG for every condition.\n"
            + "Provide at least one condition. Repeat prefixes to add conditions; use one value per occurrence.\n"
            + "Names match whole words; tags match exactly; both ignore case.\n"
            + "Phones match consecutive digits; emails match literal substrings, ignoring case.\n"
            + "Example: " + COMMAND_WORD_ANY + " n/Alice e/alice t/exco";

    public static final String MESSAGE_MULTIPLE_CONDITION_VALUES_ANY = "Each %1$s condition accepts only one value. "
            + "Repeat the prefix for each additional condition, for example: findany n/John n/Tan.";

    public static final String MESSAGE_INVALID_PHONE_SUBSTRING_ANY = "Phone searches accept digits (0-9) only. "
            + "Example: findany p/9123.";

    public static final String MESSAGE_MULTIPLE_FIELDS = "Specify only one search field per find command.";

    public static final String MESSAGE_MULTIPLE_NAME_KEYWORDS = "Prefixed name searches accept only one keyword. "
            + "Use find Alice Bob to match names containing Alice OR Bob.";

    public static final String MESSAGE_UPPERCASE_PREFIX = "Search prefixes must be lowercase. Use n/, p/, e/ or t/.";

    public static final String MESSAGE_UNSUPPORTED_PREFIX = "Unsupported search prefix: %1$s. "
            + "Use n/, p/, e/ or t/.";

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
