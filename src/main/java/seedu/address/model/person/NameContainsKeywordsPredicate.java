package seedu.address.model.person;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.commons.util.StringUtil;
import seedu.address.commons.util.ToStringBuilder;

/**
 * Tests that a {@code Person}'s {@code Name} matches any of the keywords given.
 */
public class NameContainsKeywordsPredicate implements Predicate<Person> {
    private static final Pattern NAME_COMPONENT = Pattern.compile("[A-Za-z0-9]+(?:[-'/]+[A-Za-z0-9]+)*");

    private final List<String> keywords;

    /**
     * Creates a predicate that matches any of the given name {@code keywords}.
     */
    public NameContainsKeywordsPredicate(List<String> keywords) {
        this.keywords = keywords;
    }

    @Override
    public boolean test(Person person) {
        return person.getName().map(name -> keywords.stream()
                .anyMatch(keyword -> containsKeyword(name.fullName, keyword))).orElse(false);
    }

    /**
     * Returns whether the name contains a literal word or a complete name component, ignoring case.
     */
    private boolean containsKeyword(String name, String keyword) {
        if (StringUtil.containsWordIgnoreCase(name, keyword)) {
            return true;
        }
        String trimmedKeyword = keyword.trim();
        if (!NAME_COMPONENT.matcher(trimmedKeyword).matches()) {
            return false;
        }

        // Internal hyphens, apostrophes and slashes remain part of the same component.
        Matcher matcher = NAME_COMPONENT.matcher(name);
        while (matcher.find()) {
            if (trimmedKeyword.equalsIgnoreCase(matcher.group())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof NameContainsKeywordsPredicate otherNameContainsKeywordsPredicate)) {
            return false;
        }

        return keywords.equals(otherNameContainsKeywordsPredicate.keywords);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("keywords", keywords).toString();
    }
}
