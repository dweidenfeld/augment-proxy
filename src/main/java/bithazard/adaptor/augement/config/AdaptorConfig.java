package bithazard.adaptor.augement.config;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class AdaptorConfig {
    private final Set<FeedConfig> sitemapConfig;
    private final Set<FeedConfig> rssConfig;
    private final Set<FeedConfig> atomConfig;

    private AdaptorConfig(Builder builder) {
        this.sitemapConfig = Collections.unmodifiableSet(builder.sitemapConfig);
        this.rssConfig = Collections.unmodifiableSet(builder.rssConfig);
        this.atomConfig = Collections.unmodifiableSet(builder.atomConfig);
    }

    public Set<FeedConfig> getAtomConfig() {
        return atomConfig;
    }

    public Set<FeedConfig> getRssConfig() {
        return rssConfig;
    }

    public Set<FeedConfig> getSitemapConfig() {
        return sitemapConfig;
    }

    public static class Builder {
        private Set<FeedConfig> sitemapConfig = new LinkedHashSet<>(0);
        private Set<FeedConfig> rssConfig = new LinkedHashSet<>(0);
        private Set<FeedConfig> atomConfig = new LinkedHashSet<>(0);

        public Builder sitemapConfig(Set<FeedConfig> sitemapConfig) {
            Set<FeedConfig> sitemapConfigCopy = new LinkedHashSet<>();
            sitemapConfigCopy.addAll(sitemapConfig);
            this.sitemapConfig = sitemapConfigCopy;
            return this;
        }

        public Builder rssConfig(Set<FeedConfig> rssConfig) {
            Set<FeedConfig> rssConfigCopy = new LinkedHashSet<>();
            rssConfigCopy.addAll(rssConfig);
            this.rssConfig = rssConfigCopy;
            return this;
        }

        public Builder atomConfig(Set<FeedConfig> atomConfig) {
            Set<FeedConfig> atomConfigCopy = new LinkedHashSet<>();
            atomConfigCopy.addAll(atomConfig);
            this.atomConfig = atomConfigCopy;
            return this;
        }

        public AdaptorConfig build() {
            return new AdaptorConfig(this);
        }
    }
}
