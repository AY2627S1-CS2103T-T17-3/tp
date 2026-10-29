package seedu.address.ui;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

/** A row whose preferred height accounts for the space available to wrapped tags. */
public class TagRow extends HBox {
    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    protected double computePrefHeight(double width) {
        if (width < 0 && getWidth() > 0) {
            width = getWidth();
        }
        if (width < 0 || getManagedChildren().size() != 2) {
            return super.computePrefHeight(width);
        }
        Node tags = getManagedChildren().get(0);
        Node status = getManagedChildren().get(1);
        Insets insets = getInsets();
        double contentWidth = Math.max(0, width - insets.getLeft() - insets.getRight());
        double statusWidth = Math.min(status.prefWidth(-1), contentWidth);
        double tagsWidth = Math.max(0, contentWidth - statusWidth - getSpacing());
        double tagsHeight = tags.prefHeight(tagsWidth);
        return insets.getTop() + Math.max(tagsHeight, status.prefHeight(statusWidth)) + insets.getBottom();
    }
}
