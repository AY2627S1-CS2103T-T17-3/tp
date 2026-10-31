package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.StatusFilter;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.EmailContainsSubstringPredicate;
import seedu.address.model.person.Person;
import seedu.address.model.person.Status;
import seedu.address.testutil.PersonBuilder;

public class ViewCommandTest {

    @Test
    public void execute_changesStatusViewAndPreservesSearch() {
        Person activeMatch = new PersonBuilder().withEmail("alex@u.nus.edu").build();
        Person archivedMatch = new PersonBuilder().withEmail("alexold@u.nus.edu")
                .withStatus(Status.ARCHIVED).build();
        Person activeNonmatch = new PersonBuilder().withEmail("bob@u.nus.edu").build();
        AddressBook addressBook = new AddressBook();
        addressBook.addPerson(activeMatch);
        addressBook.addPerson(archivedMatch);
        addressBook.addPerson(activeNonmatch);
        Model model = new ModelManager(addressBook, new UserPrefs());
        model.updateFilteredPersonList(new EmailContainsSubstringPredicate("alex"));

        ViewCommand viewArchived = new ViewCommand(StatusFilter.ARCHIVED);
        String expectedMessage = "Showing archived view.\n"
                + "Any existing search still applies. Use list to clear the search in this view.";
        assertEquals(new CommandResult(expectedMessage), viewArchived.execute(model));
        assertEquals(StatusFilter.ARCHIVED, model.getStatusFilter());
        assertEquals(List.of(archivedMatch), model.getFilteredPersonList());

        new ViewCommand(StatusFilter.ALL).execute(model);
        assertEquals(List.of(activeMatch, archivedMatch), model.getFilteredPersonList());
        new ViewCommand(StatusFilter.ACTIVE).execute(model);
        assertEquals(List.of(activeMatch), model.getFilteredPersonList());
        assertEquals(List.of(activeMatch, archivedMatch, activeNonmatch), model.getAddressBook().getPersonList());
        assertTrue(viewArchived.isReadOnly());
        assertFalse(viewArchived.requiresSave());
    }
}
