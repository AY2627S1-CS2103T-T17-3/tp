package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.geometry.Insets;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import seedu.address.testutil.JavaFxTestUtil;

public class TagRowTest {
    @BeforeAll
    public static void setUpJavaFx() throws InterruptedException {
        JavaFxTestUtil.initialize();
    }

    @Test
    public void preferredHeight_wrapsTagsAndUsesCurrentWidthWhenUnspecified() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            TagRow row = new TagRow();
            row.setPadding(new Insets(2));
            row.setSpacing(4);
            FlowPane tags = new FlowPane();
            tags.setHgap(2);
            tags.getChildren().addAll(box(30, 10), box(30, 10));
            Region status = box(20, 12);
            row.getChildren().addAll(tags, status);

            double wideHeight = row.computePrefHeight(100);
            double narrowHeight = row.computePrefHeight(60);
            assertTrue(narrowHeight > wideHeight, "Narrow rows must allow the tags to wrap");
            row.resize(60, narrowHeight);
            assertEquals(narrowHeight, row.computePrefHeight(-1), 0.01,
                    "An unspecified width must use the current row width");
            return null;
        });
    }

    @Test
    public void preferredHeight_beforeSizingAndWithMissingChild_usesDefaultLayout() throws Exception {
        JavaFxTestUtil.runAndWait(() -> {
            TagRow row = new TagRow();
            row.getChildren().add(box(30, 12));
            assertEquals(12, row.computePrefHeight(-1), 0.01);
            assertEquals(12, row.computePrefHeight(100), 0.01);
            return null;
        });
    }

    private static Region box(double width, double height) {
        Region region = new Region();
        region.setPrefSize(width, height);
        region.setMinSize(width, height);
        return region;
    }
}
