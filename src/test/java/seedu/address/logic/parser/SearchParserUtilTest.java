package seedu.address.logic.parser;

import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.FindCommand;

/**
 * Tests defensive validation in the shared search parser utility.
 */
public class SearchParserUtilTest {

    @Test
    public void parseCondition_unsupportedPrefix_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, "Unsupported search prefix: a/", () ->
                SearchParserUtil.parseCondition(new Prefix("a/"), "MainStreet",
                        FindCommand.MESSAGE_MULTIPLE_CONDITION_VALUES, FindCommand.MESSAGE_INVALID_PHONE_SUBSTRING));
    }
}
