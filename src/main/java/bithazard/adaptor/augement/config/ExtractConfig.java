package bithazard.adaptor.augement.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public final class ExtractConfig {
    private final Scope scope;
    private final Target target;
    private final String cssSelector;
    private final RegexMatch regexMatch;
    private final Pattern regexFind;
    private final String regexReplace;
    private final List<String> metadataNames;
    private final int titleCssElement;

    public enum Scope {
        URL,
        CONTENT
    }

    public enum Target {
        METADATA,
        TITLE,
        LINK,
        ACL_USER,
        ACL_GROUP
    }

    public enum RegexMatch {
        TEXT,
        HTML
    }

    private ExtractConfig(Builder builder) {
        this.scope = builder.scope;
        this.target = builder.target;
        this.cssSelector = builder.cssSelector;
        this.regexMatch = builder.regexMatch;
        this.regexFind = builder.regexFind;
        this.regexReplace = builder.regexReplace;
        this.metadataNames = Collections.unmodifiableList(builder.metadataNames);
        this.titleCssElement = builder.titleCssElement;
    }

    public Scope getScope() {
        return scope;
    }

    public Target getTarget() {
        return target;
    }

    public String getCssSelector() {
        return cssSelector;
    }

    public RegexMatch getRegexMatch() {
        return regexMatch;
    }

    public Pattern getRegexFind() {
        return regexFind;
    }

    public String getRegexReplace() {
        return regexReplace;
    }

    public List<String> getMetadataNames() {
        return metadataNames;
    }

    public int getTitleCssElement() {
        return titleCssElement;
    }

    public static class Builder {
        private static final Logger LOGGER = Logger.getLogger(Builder.class.getName());
        private static final String DEFAULT_REGEX_FIND = ".*";
        private Scope scope;
        private Target target;
        private String cssSelector;
        private RegexMatch regexMatch = RegexMatch.TEXT;
        private Pattern regexFind;
        private String regexReplace;
        private List<String> metadataNames = new ArrayList<>(0);
        private int titleCssElement = 0;

        public Builder(Scope scope, Target target) {
            if (scope == null || target == null) {
                throw new AugmentConfigException("Missing configuration value in extract config: You must at least "
                        + "specify a scope and a target.");
            }
            this.scope = scope;
            this.target = target;
        }

        public Builder cssSelector(String cssSelector) {
            this.cssSelector = cssSelector;
            return this;
        }

        public Builder regexMatch(RegexMatch regexMatch) {
            this.regexMatch = regexMatch;
            return this;
        }

        public Builder regexFind(String regexFind) {
            this.regexFind = Pattern.compile(regexFind);
            return this;
        }

        public Builder regexReplace(String regexReplace) {
            this.regexReplace = regexReplace;
            return this;
        }

        public Builder metadataNames(List<String> metadataNames) {
            List<String> metadataNamesCopy = new ArrayList<>();
            metadataNamesCopy.addAll(metadataNames);
            this.metadataNames = metadataNamesCopy;
            return this;
        }

        public Builder titleCssElement(int titleCssElement) {
            this.titleCssElement = titleCssElement;
            return this;
        }

        public ExtractConfig build() {
            if (regexFind == null) {
                regexFind = Pattern.compile(DEFAULT_REGEX_FIND);
            }
            switch (target) {
                case METADATA:
                    if (metadataNames.size() == 0) {
                        throw new AugmentConfigException("Missing configuration value in extract config: When using target "
                                + "metadata you must supply at least one metadata name.");
                    }
                    if (titleCssElement > 0) {
                        LOGGER.warning("You have configured titleCssElement although the configured target is metadata. "
                                + "Ignoring titleCssElement.");
                    }
                    break;
                default:
                    if (metadataNames.size() > 0) {
                        LOGGER.warning("You have configured metadata names although the configured target is " + target
                                + ". Ignoring metadata names.");
                    }
                    break;
            }
            if (scope == Scope.URL) {
                if (cssSelector != null) {
                    LOGGER.warning("CSS selector cannot be applied if scope is URL. Ignoring configured CSS selector.");
                    cssSelector = null;
                }
                if (metadataNames.size() > 1) {
                    LOGGER.warning("You have configured multiple metadata names and URL as scope. Only one metadata "
                            + "value can be extracted from the URL. Ignoring additionally configured metadata names.");
                }
                if (titleCssElement > 0) {
                    LOGGER.warning("You have configured titleCssElement although the configured scope is URL and no "
                            + "CSS selector is applied there. Ignoring configured titleCssElement.");
                    titleCssElement = 0;
                }
            }
            return new ExtractConfig(this);
        }
    }
}
