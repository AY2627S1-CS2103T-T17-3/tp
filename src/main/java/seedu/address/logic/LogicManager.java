package seedu.address.logic;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.commands.Command;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.ImportCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.AddressBookParser;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.StatusFilter;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.Storage;

/**
 * The main LogicManager of the app.
 */
public class LogicManager implements Logic {
    public static final String MESSAGE_SAVE_REVERTED = "\nThe command was not saved. Member records and the previous "
            + "search filter have been restored. Resolve the saving problem and retry the command.";

    public static final String FILE_OPS_ERROR_FORMAT = "Could not save data due to the following error: %s"
            + MESSAGE_SAVE_REVERTED;

    public static final String FILE_OPS_PERMISSION_ERROR_FORMAT =
            "Could not save data to file %s due to insufficient permissions to write to the file or the folder."
            + MESSAGE_SAVE_REVERTED;

    private final Logger logger = LogsCenter.getLogger(LogicManager.class);

    private final Model model;
    private final Storage storage;
    private final AddressBookParser addressBookParser;

    /**
     * Constructs a {@code LogicManager} with the given {@code Model} and {@code Storage}.
     */
    public LogicManager(Model model, Storage storage) {
        this.model = model;
        this.storage = storage;
        addressBookParser = new AddressBookParser();
    }

    @Override
    public CommandResult execute(String commandText) throws CommandException, ParseException {
        CommandResult commandResult;
        Command command = addressBookParser.parseCommand(commandText);
        logger.info("Executing " + command.getClass().getSimpleName());
        if (!storage.isAddressBookSaveAllowed()) {
            if (!command.isReadOnly()) {
                throw new CommandException(JsonAddressBookStorage.MESSAGE_SAVE_DISABLED);
            }
            return command.execute(model);
        }
        if (command instanceof ImportCommand importCommand) {
            verifyImportSource(importCommand.getFilePath());
        }
        AddressBook previousAddressBook = new AddressBook(model.getAddressBook());
        Predicate<? super Person> previousFilter = model.getFilteredPersonListPredicate();
        commandResult = command.execute(model);

        try {
            storage.saveAddressBook(model.getAddressBook());
        } catch (AccessDeniedException e) {
            restoreAfterSaveFailure(previousAddressBook, previousFilter, e);
            throw new CommandException(String.format(FILE_OPS_PERMISSION_ERROR_FORMAT, e.getMessage()), e);
        } catch (IOException | SecurityException ioe) {
            restoreAfterSaveFailure(previousAddressBook, previousFilter, ioe);
            throw new CommandException(String.format(FILE_OPS_ERROR_FORMAT, ioe.getMessage()), ioe);
        }

        return commandResult;
    }

    private void restoreAfterSaveFailure(AddressBook previousAddressBook, Predicate<? super Person> previousFilter,
            Exception failure) {
        model.setAddressBook(previousAddressBook);
        model.updateFilteredPersonList(previousFilter);
        logger.warning("Saving failed (" + failure.getClass().getSimpleName()
                + "); restored member records and the previous search filter.");
    }

    private void verifyImportSource(Path sourceFile) throws CommandException {
        Path dataFile = storage.getAddressBookFilePath();
        try {
            boolean isSamePath = sourceFile.toAbsolutePath().normalize().equals(dataFile.toAbsolutePath().normalize());
            boolean isSameFile = Files.exists(sourceFile) && Files.exists(dataFile)
                    && Files.isSameFile(sourceFile, dataFile);
            if (isSamePath || isSameFile) {
                throw new CommandException(ImportCommand.MESSAGE_SOURCE_IS_DATA_FILE);
            }
        } catch (IOException e) {
            throw new CommandException(String.format(ImportCommand.MESSAGE_FAILURE, sourceFile, e.getMessage()), e);
        }
    }

    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return model.getFilteredPersonList();
    }

    @Override
    public void setStatusFilter(StatusFilter statusFilter) {
        model.setStatusFilter(statusFilter);
    }

    @Override
    public GuiSettings getGuiSettings() {
        return model.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        model.setGuiSettings(guiSettings);
    }
}
