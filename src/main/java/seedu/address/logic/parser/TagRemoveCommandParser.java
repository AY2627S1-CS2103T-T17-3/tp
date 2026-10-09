package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TAG;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.TagRemoveCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.tag.Tag;

/**
 * Parses arguments for removing one tag from one person.
 */
public class TagRemoveCommandParser implements Parser<TagRemoveCommand> {

    /**
     * Parses {@code args} into a {@code TagRemoveCommand}.
     *
     * @throws ParseException if the arguments do not contain one index and one valid tag.
     */
    public TagRemoveCommand parse(String args) throws ParseException {
        requireNonNull(args);
        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(args, PREFIX_TAG);
        Index index;
        try {
            index = ParserUtil.parseIndex(argMultimap.getPreamble());
        } catch (ParseException pe) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, TagRemoveCommand.MESSAGE_USAGE), pe);
        }

        if (argMultimap.getValue(PREFIX_TAG).isEmpty()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, TagRemoveCommand.MESSAGE_USAGE));
        }
        argMultimap.verifyNoDuplicatePrefixesFor(PREFIX_TAG);
        Tag tag = ParserUtil.parseTag(argMultimap.getValue(PREFIX_TAG).get());
        return new TagRemoveCommand(index, tag);
    }
}
