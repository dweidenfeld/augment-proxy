package bithazard.adaptor.augement.config;

public final class FeedConfig {
    private final String url;
    private final boolean addMetadata;

    public FeedConfig(final String url, final boolean addMetadata) {
        this.url = url;
        this.addMetadata = addMetadata;
    }

    public String getUrl() {
        return url;
    }

    public boolean isAddMetadata() {
        return addMetadata;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FeedConfig)) {
            return false;
        }

        FeedConfig that = (FeedConfig) o;
        return url.equals(that.url);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}
