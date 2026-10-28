package seedu.address.logic.commands;

import seedu.address.model.Model;

/**
 * Displays an offline reference for every implemented command.
 */
public class HelpCommand extends Command {

    public static final String COMMAND_WORD = "help";

    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Displays descriptions, usage, and examples for all commands.\n"
            + "Example: " + COMMAND_WORD;

    public static final String SHOWING_HELP_MESSAGE = "Opened help window.";

    public static final String MESSAGE_HELP = """
            NUSocietyDesk command reference

            activate - Reactivate an archived member.
            Usage: activate INDEX
            Example: activate 1

            add - Add an active member using an email and any known contact details.
            Usage: add e/EMAIL [n/NAME] [p/PHONE_NUMBER] [a/ADDRESS] [t/TAG]...
            Example: add e/alex@u.nus.edu n/Alex Tan

            archive - Mark an active member as archived while retaining their record.
            Usage: archive INDEX
            Example: archive 1

            clear - Permanently delete every member, including archived and hidden members.
            Usage: clear
            Requires confirmation. Cancel is the default. Extra arguments are rejected.
            Example: clear

            delete - Permanently delete one member from the currently displayed list.
            Usage: delete INDEX
            Example: delete 1

            edit - Update a member's contact details while preserving unspecified fields.
            Usage: edit INDEX [n/NAME] [p/PHONE_NUMBER] [e/EMAIL] [a/ADDRESS]
            Example: edit 1 p/91234567

            exit - Close NUSocietyDesk.
            Usage: exit
            Example: exit

            find - Search members by name, phone, email, or tag.
            Usage:
              find KEYWORD [MORE_KEYWORDS]...
              find n/KEYWORD
              find p/PHONE_SUBSTRING
              find e/EMAIL_SUBSTRING
              find t/TAG
            Example: find e/alex

            findall - Find members matching every supplied search condition.
            Usage: findall PREFIX/VALUE [PREFIX/VALUE]...
            Example: findall n/Alex t/Exco

            findany - Find members matching at least one supplied search condition.
            Usage: findany PREFIX/VALUE [PREFIX/VALUE]...
            Example: findany n/Alex t/Publicity

            help - Display this command reference without opening a website.
            Usage: help
            Example: help

            import - Add valid members from a CSV file and report rejected records.
            Usage: import FILE_PATH
            Example: import "contacts.csv"

            list - Clear the current search and list members in the selected status view.
            Usage: list
            Example: list

            tag - Add one tag to a member while preserving their other tags.
            Usage: tag INDEX t/TAG
            Example: tag 1 t/Volunteer

            untag - Remove one tag from a member while preserving their other tags.
            Usage: untag INDEX t/TAG
            Example: untag 1 t/Volunteer

            view - Select a membership status view while keeping the current search.
            Usage: view all|active|archived
            Example: view archived

            Notation:
              UPPER_CASE denotes a value you supply.
              [ITEM] is optional; ... allows repetition; | separates alternatives.
              INDEX is a positive integer referring to the currently displayed list.
              Search prefixes are n/, p/, e/, and t/.
              For import paths containing spaces, enclose FILE_PATH in double quotes.

            Examples show command syntax; indexed commands require a suitable displayed member.
            Import examples require an existing CSV file in the documented format.
            Viewing help does not execute any example.
            """.stripTrailing();

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
        return new CommandResult(SHOWING_HELP_MESSAGE, true, false);
    }
}
