package bithazard.adaptor.augement.config;

public class ImageLinkConfig {
    private final int minWidth;
    private final int minHeight;
    private final boolean addSurroundingText;

    public ImageLinkConfig(final int minWidth, final int minHeight, final boolean addSurroundingText) {
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.addSurroundingText = addSurroundingText;
    }

    public int getMinWidth() {
        return minWidth;
    }

    public int getMinHeight() {
        return minHeight;
    }

    public boolean isAddSurroundingText() {
        return addSurroundingText;
    }
}
