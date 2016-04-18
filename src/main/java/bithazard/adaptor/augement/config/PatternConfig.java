package bithazard.adaptor.augement.config;

import bithazard.adaptor.augement.content.BrowserContentSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class PatternConfig {
    private final Pattern urlPattern;
    private final boolean setNoFollow;
    private final boolean setCrawlOnce;
    private final boolean setNoArchive;
    private final boolean setNoIndex;
    private final boolean sortParameters;
    private final boolean executeJavascript;
    private final boolean passGsaHeaders;
    private final boolean tlsTermination;
    private final boolean passRequestCookies;
    private final long javascriptTimeout;
    private final String userAgent;
    private final Pattern omitContentRegex;
    private final Map<String, String> requestHeaders;
    private final Map<String, String> headersToMetadata;
    private final Set<String> excludeCssSelectors;
    private final List<ExtractConfig> extractConfigs;
    private final Set<Pattern> removeParameters;
    private final ImageLinkConfig imageLinkConfig;

    private PatternConfig(Builder builder) {
        this.urlPattern = builder.urlPattern;
        this.setNoFollow = builder.setNoFollow;
        this.setCrawlOnce = builder.setCrawlOnce;
        this.setNoArchive = builder.setNoArchive;
        this.setNoIndex = builder.setNoIndex;
        this.sortParameters = builder.sortParameters;
        this.executeJavascript = builder.executeJavascript;
        this.passGsaHeaders = builder.passGsaHeaders;
        this.tlsTermination = builder.tlsTermination;
        this.passRequestCookies = builder.passRequestCookies;
        this.javascriptTimeout = builder.javascriptTimeout;
        this.userAgent = builder.userAgent;
        this.omitContentRegex = builder.omitContentRegex;
        this.requestHeaders = Collections.unmodifiableMap(builder.requestHeaders);
        this.headersToMetadata = Collections.unmodifiableMap(builder.headersToMetadata);
        this.excludeCssSelectors = Collections.unmodifiableSet(builder.excludeCssSelectors);
        this.extractConfigs = Collections.unmodifiableList(builder.extractConfigs);
        this.removeParameters = Collections.unmodifiableSet(builder.removeParameters);
        this.imageLinkConfig = builder.imageLinkConfig;
    }

    public Pattern getUrlPattern() {
        return urlPattern;
    }

    public boolean isSetNoFollow() {
        return setNoFollow;
    }

    public boolean isSetCrawlOnce() {
        return setCrawlOnce;
    }

    public boolean isSetNoArchive() {
        return setNoArchive;
    }

    public boolean isSetNoIndex() {
        return setNoIndex;
    }

    public boolean isSortParameters() {
        return sortParameters;
    }

    public boolean isExecuteJavascript() {
        return executeJavascript;
    }

    public boolean isPassGsaHeaders() {
        return passGsaHeaders;
    }

    public boolean isTlsTermination() {
        return tlsTermination;
    }

    public boolean isPassRequestCookies() {
        return passRequestCookies;
    }

    public long getJavascriptTimeout() {
        return javascriptTimeout;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Pattern getOmitContentRegex() {
        return omitContentRegex;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public Map<String, String> getHeadersToMetadata() {
        return headersToMetadata;
    }

    public Set<String> getExcludeCssSelectors() {
        return excludeCssSelectors;
    }

    public List<ExtractConfig> getExtractConfigs() {
        return extractConfigs;
    }

    public Set<Pattern> getRemoveParameters() {
        return removeParameters;
    }

    public ImageLinkConfig getImageLinkConfig() {
        return imageLinkConfig;
    }

    public static class Builder {
        private Pattern urlPattern;
        private boolean setNoFollow = false;
        private boolean setCrawlOnce = false;
        private boolean setNoArchive = false;
        private boolean setNoIndex = false;
        private boolean sortParameters = false;
        private boolean executeJavascript = false;
        private boolean passGsaHeaders = false;
        private boolean tlsTermination = false;
        private boolean passRequestCookies = false;
        private long javascriptTimeout = 10000;
        private String userAgent = BrowserContentSource.DEFAULT_BROWSER_VERSION.getUserAgent();
        private Pattern omitContentRegex;
        private Map<String, String> requestHeaders = new HashMap<>(0);
        private Map<String, String> headersToMetadata = new HashMap<>(0);
        private Set<String> excludeCssSelectors = new LinkedHashSet<>(0);
        private List<ExtractConfig> extractConfigs = new ArrayList<>(0);
        private Set<Pattern> removeParameters = new HashSet<>(0);
        private ImageLinkConfig imageLinkConfig;

        public Builder(String urlPattern) {
            if (urlPattern == null) {
                throw new AugmentConfigException("Missing configuration value in augment config: You must at least "
                        + "specify a URL pattern.");
            }
            this.urlPattern = Pattern.compile(urlPattern);
        }

        public Builder sortParameters(boolean sortParameters) {
            this.sortParameters = sortParameters;
            return this;
        }

        public Builder setNoFollow(boolean noFollow) {
            this.setNoFollow = noFollow;
            return this;
        }

        public Builder setCrawlOnce(boolean crawlOnce) {
            this.setCrawlOnce = crawlOnce;
            return this;
        }

        public Builder setNoArchive(boolean noArchive) {
            this.setNoArchive = noArchive;
            return this;
        }

        public Builder setNoIndex(boolean noIndex) {
            this.setNoIndex = noIndex;
            return this;
        }

        public Builder executeJavascript(boolean executeJavascript) {
            this.executeJavascript = executeJavascript;
            return this;
        }

        public Builder passGsaHeaders(boolean passGsaHeaders) {
            this.passGsaHeaders = passGsaHeaders;
            return this;
        }

        public Builder tlsTermination(boolean tlsTermination) {
            this.tlsTermination = tlsTermination;
            return this;
        }

        public Builder passRequestCookies(boolean passRequestCookies) {
            this.passRequestCookies = passRequestCookies;
            return this;
        }

        public Builder javascriptTimeout(long javascriptTimeout) {
            this.javascriptTimeout = javascriptTimeout;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder omitContentRegex(String omitContentRegex) {
            this.omitContentRegex = Pattern.compile(omitContentRegex);
            return this;
        }

        public Builder requestHeaders(Map<String, String> requestHeaders) {
            Map<String, String> requestHeadersCopy = new LinkedHashMap<>(requestHeaders.size());
            requestHeadersCopy.putAll(requestHeaders);
            this.requestHeaders = requestHeadersCopy;
            return this;
        }

        public Builder headersToMetadata(Map<String, String> headersToMetadata) {
            Map<String, String> headersToMetadataCopy = new LinkedHashMap<>(headersToMetadata.size());
            headersToMetadataCopy.putAll(headersToMetadata);
            this.headersToMetadata = headersToMetadataCopy;
            return this;
        }

        public Builder excludeCssSelectors(Set<String> excludeCssSelectors) {
            Set<String> excludeCssSelectorsCopy = new LinkedHashSet<>(excludeCssSelectors.size());
            excludeCssSelectorsCopy.addAll(excludeCssSelectors);
            this.excludeCssSelectors = excludeCssSelectorsCopy;
            return this;
        }

        public Builder extractConfigs(List<ExtractConfig> extractConfigs) {
            List<ExtractConfig> extractConfigsCopy = new ArrayList<>(extractConfigs.size());
            extractConfigsCopy.addAll(extractConfigs);
            this.extractConfigs = extractConfigsCopy;
            return this;
        }

        public Builder removeParameters(Set<String> removeParameters) {
            Set<Pattern> removeParametersCopy = new HashSet<>(removeParameters.size());
            for (String removeParameter : removeParameters) {
                removeParametersCopy.add(Pattern.compile(removeParameter));
            }
            this.removeParameters = removeParametersCopy;
            return this;
        }

        public Builder imageLinkConfig(ImageLinkConfig imageLinkConfig) {
            this.imageLinkConfig = imageLinkConfig;
            return this;
        }

        public PatternConfig build() {
            return new PatternConfig(this);
        }
    }
}
