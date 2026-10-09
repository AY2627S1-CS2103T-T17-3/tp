package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TAG;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PERSONS;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import seedu.address.commons.core.index.Index;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;

/**
 * Removes one tag from a person identified using their displayed index, preserving other tags.
 */
public class TagRemoveCommand extends Command {

    public static final String COMMAND_WORD = "tagremove";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Removes one tag from the person identified by the index number in the displayed person list.\n"
            + "Parameters: INDEX (must be a positive integer) " + PREFIX_TAG + "TAG\n"
            + "Example: " + COMMAND_WORD + " 1 " + PREFIX_TAG + "Volunteer";
    public static final String MESSAGE_SUCCESS = "Tag removed: %1$s; Person: %2$s";

    private final Index targetIndex;
    private final Tag tag;

    /**
     * Creates a command to remove {@code tag} from the person at {@code targetIndex} in the displayed list.
     */
    public TagRemoveCommand(Index targetIndex, Tag tag) {
        requireAllNonNull(targetIndex, tag);
        this.targetIndex = targetIndex;
        this.tag = tag;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Person> lastShownList = model.getFilteredPersonList();
        if (targetIndex.getZeroBased() >= lastShownList.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        }

        Person personToEdit = lastShownList.get(targetIndex.getZeroBased());
        Set<Tag> updatedTags = new HashSet<>(personToEdit.getTags());
        updatedTags.remove(tag);
        Person editedPerson = new Person(personToEdit.getName(), personToEdit.getPhone(), personToEdit.getEmail(),
                personToEdit.getAddress(), personToEdit.getStatus(), updatedTags);

        model.setPerson(personToEdit, editedPerson);
        model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        return new CommandResult(String.format(MESSAGE_SUCCESS, tag, Messages.format(editedPerson)));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof TagRemoveCommand otherCommand)) {
            return false;
        }
        return targetIndex.equals(otherCommand.targetIndex) && tag.equals(otherCommand.tag);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("targetIndex", targetIndex)
                .add("tag", tag)
                .toString();
    }
}
