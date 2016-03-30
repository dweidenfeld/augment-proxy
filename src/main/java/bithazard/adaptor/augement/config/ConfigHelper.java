package bithazard.adaptor.augement.config;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

public final class ConfigHelper {
    private static final Logger LOGGER = Logger.getLogger(ConfigHelper.class.getName());

    public static AdaptorConfig parseAdaptorConfig(final String adaptorConfigFilename) {
        AdaptorConfig.Builder adaptorConfigBuilder = new AdaptorConfig.Builder();
        JSONObject adaptorConfig = (JSONObject) parseJsonConfig(adaptorConfigFilename);
        if (adaptorConfig == null) {
            return adaptorConfigBuilder.build();
        }
        Set<FeedConfig> sitemapConfigs = new LinkedHashSet<>();
        JSONArray sitemapArray = (JSONArray) adaptorConfig.get("sitemap");
        if (sitemapArray != null) {
            for (Object sitemapConfig : sitemapArray) {
                if (sitemapConfig instanceof JSONObject) {
                    JSONObject sitemapConfigObject = (JSONObject) sitemapConfig;
                    String sitemapUrl = (String) sitemapConfigObject.get("url");
                    if (sitemapUrl == null) {
                        throw new AugmentConfigException("Sitemap config must at least have an URL.");
                    }
                    Boolean addMetadata = (Boolean) sitemapConfigObject.get("addMetadata");
                    if (addMetadata == null) {
                        addMetadata = false;
                    }
                    sitemapConfigs.add(new FeedConfig(sitemapUrl, addMetadata));
                } else {
                    throw new AugmentConfigException("Adaptor config is malformed. Expecting an array of objects inside sitemap.");
                }
            }
        }
        adaptorConfigBuilder.sitemapConfig(sitemapConfigs);
        Set<FeedConfig> rssConfigs = new LinkedHashSet<>();
        JSONArray rssArray = (JSONArray) adaptorConfig.get("rss");
        if (rssArray != null) {
            for (Object rssConfig : rssArray) {
                if (rssConfig instanceof JSONObject) {
                    JSONObject rssConfigObject = (JSONObject) rssConfig;
                    String rssUrl = (String) rssConfigObject.get("url");
                    if (rssUrl == null) {
                        throw new AugmentConfigException("RSS config must at least have an URL.");
                    }
                    Boolean addMetadata = (Boolean) rssConfigObject.get("addMetadata");
                    if (addMetadata == null) {
                        addMetadata = false;
                    }
                    rssConfigs.add(new FeedConfig(rssUrl, addMetadata));
                } else {
                    throw new AugmentConfigException("Adaptor config is malformed. Expecting an array of objects inside rss.");
                }
            }
        }
        adaptorConfigBuilder.rssConfig(rssConfigs);
        Set<FeedConfig> atomConfigs = new LinkedHashSet<>();
        JSONArray atomArray = (JSONArray) adaptorConfig.get("atom");
        if (atomArray != null) {
            for (Object atomConfig : atomArray) {
                if (atomConfig instanceof JSONObject) {
                    JSONObject atomConfigObject = (JSONObject) atomConfig;
                    String atomUrl = (String) atomConfigObject.get("url");
                    if (atomUrl == null) {
                        throw new AugmentConfigException("Atom config must at least have an URL.");
                    }
                    Boolean addMetadata = (Boolean) atomConfigObject.get("addMetadata");
                    if (addMetadata == null) {
                        addMetadata = false;
                    }
                    atomConfigs.add(new FeedConfig(atomUrl, addMetadata));
                } else {
                    throw new AugmentConfigException("Adaptor config is malformed. Expecting an array of objects inside atom.");
                }
            }
        }
        adaptorConfigBuilder.atomConfig(atomConfigs);
        return adaptorConfigBuilder.build();
    }

    public static void initAugmentConfig(String augmentConfigFilename, List<PatternConfig> patternConfigs) {
        JSONArray augmentConfig = (JSONArray) parseJsonConfig(augmentConfigFilename);
        if (augmentConfig == null) {
            return;
        }
        for (Object augmentConfigObject : augmentConfig) {
            if (augmentConfigObject instanceof JSONObject) {
                JSONObject augmentConfigJsonObject = (JSONObject) augmentConfigObject;
                String urlPattern = (String) augmentConfigJsonObject.get("urlPattern");
                PatternConfig.Builder patternConfigBuilder = new PatternConfig.Builder(urlPattern);
                Boolean setNoFollow = (Boolean) augmentConfigJsonObject.get("setNoFollow");
                if (setNoFollow != null) {
                    patternConfigBuilder.setNoFollow(setNoFollow);
                }
                Boolean setCrawlOnce = (Boolean) augmentConfigJsonObject.get("setCrawlOnce");
                if (setCrawlOnce != null) {
                    patternConfigBuilder.setCrawlOnce(setCrawlOnce);
                }
                Boolean sortParameters = (Boolean) augmentConfigJsonObject.get("sortParameters");
                if (sortParameters != null) {
                    patternConfigBuilder.sortParameters(sortParameters);
                }
                Boolean executeJavascript = (Boolean) augmentConfigJsonObject.get("executeJavascript");
                if (executeJavascript != null) {
                    patternConfigBuilder.executeJavascript(executeJavascript);
                }
                Long javascriptTimeout = (Long) augmentConfigJsonObject.get("javascriptTimeout");
                if (javascriptTimeout != null) {
                    patternConfigBuilder.javascriptTimeout(javascriptTimeout);
                }
                String userAgent = (String) augmentConfigJsonObject.get("userAgent");
                if (userAgent != null) {
                    patternConfigBuilder.userAgent(userAgent);
                }
                String omitContentRegex = (String) augmentConfigJsonObject.get("omitContentRegex");
                if (omitContentRegex != null) {
                    patternConfigBuilder.omitContentRegex(omitContentRegex);
                }
                Boolean passGsaHeaders = (Boolean) augmentConfigJsonObject.get("passGsaHeaders");
                if (passGsaHeaders != null) {
                    patternConfigBuilder.passGsaHeaders(passGsaHeaders);
                }
                JSONArray excludeCssSelectors = (JSONArray) augmentConfigJsonObject.get("excludeCssSelectors");
                if (excludeCssSelectors != null) {
                    String[] excludeCssSelectorStrings =
                            (String[]) excludeCssSelectors.toArray(new String[excludeCssSelectors.size()]);
                    patternConfigBuilder.excludeCssSelectors(new LinkedHashSet<>(Arrays.asList(excludeCssSelectorStrings)));
                }
                JSONObject headersToMetadata = (JSONObject) augmentConfigJsonObject.get("headersToMetadata");
                if (headersToMetadata != null) {
                    patternConfigBuilder.headersToMetadata(headersToMetadata);
                }
                JSONObject addRequestHeaders = (JSONObject) augmentConfigJsonObject.get("addRequestHeaders");
                if (addRequestHeaders != null) {
                    patternConfigBuilder.requestHeaders(addRequestHeaders);
                }
                JSONArray removeParameters = (JSONArray) augmentConfigJsonObject.get("removeParameters");
                if (removeParameters != null) {
                    String[] removeParametersStrings =
                            (String[]) removeParameters.toArray(new String[removeParameters.size()]);
                    patternConfigBuilder.removeParameters(new HashSet<>(Arrays.asList(removeParametersStrings)));
                }
                JSONObject imageLinksToPdf = (JSONObject) augmentConfigJsonObject.get("imageLinksToPdf");
                if (imageLinksToPdf != null) {
                    Long minWidth = ((Long) imageLinksToPdf.get("minWidth"));
                    if (minWidth == null) {
                        minWidth = (long) Integer.MAX_VALUE;
                    }
                    Long minHeight = (Long) imageLinksToPdf.get("minHeight");
                    if (minHeight == null) {
                        minHeight = (long) Integer.MAX_VALUE;
                    }
                    Boolean addSurroundingText = (Boolean) imageLinksToPdf.get("addSurroundingText");
                    if (addSurroundingText == null) {
                        addSurroundingText = false;
                    }
                    patternConfigBuilder.imageLinkConfig(new ImageLinkConfig(minWidth.intValue(), minHeight.intValue(), addSurroundingText));
                }
                JSONArray extractConfig = (JSONArray) augmentConfigJsonObject.get("extract");
                if (extractConfig != null) {
                    List<ExtractConfig> extractConfigs = new ArrayList<>();
                    for (Object extractConfigObject : extractConfig) {
                        JSONObject extractConfigJsonObject = (JSONObject) extractConfigObject;
                        String scope = (String) extractConfigJsonObject.get("scope");
                        String target = (String) extractConfigJsonObject.get("target");
                        ExtractConfig.Builder extractConfigBuilder = new ExtractConfig.Builder(
                                ExtractConfig.Scope.valueOf(scope.toUpperCase(Locale.ENGLISH)),
                                ExtractConfig.Target.valueOf(target.toUpperCase(Locale.ENGLISH)));
                        extractConfigBuilder.cssSelector((String) extractConfigJsonObject.get("cssSelector"));
                        String regexMatch = (String) extractConfigJsonObject.get("regexMatch");
                        if (regexMatch != null) {
                            extractConfigBuilder.regexMatch(
                                    ExtractConfig.RegexMatch.valueOf(regexMatch.toUpperCase(Locale.ENGLISH)));
                        }
                        String regexFind = (String) extractConfigJsonObject.get("regexFind");
                        if (regexFind != null) {
                            extractConfigBuilder.regexFind(regexFind);
                        }
                        extractConfigBuilder.regexReplace((String) extractConfigJsonObject.get("regexReplace"));
                        Long titleCssElement = (Long) extractConfigJsonObject.get("titleCssElement");
                        if (titleCssElement != null) {
                            extractConfigBuilder.titleCssElement(titleCssElement.intValue());
                        }
                        JSONArray metadataNames = (JSONArray) extractConfigJsonObject.get("metadataNames");
                        if (metadataNames != null) {
                            String[] metadataNameStrings =
                                    (String[]) metadataNames.toArray(new String[metadataNames.size()]);
                            extractConfigBuilder.metadataNames(Arrays.asList(metadataNameStrings));
                        }
                        extractConfigs.add(extractConfigBuilder.build());
                    }
                    patternConfigBuilder.extractConfigs(extractConfigs);
                }
                patternConfigs.add(patternConfigBuilder.build());
            } else {
                throw new AugmentConfigException("Augment config is malformed. Expecting an array of objects.");
            }
        }
    }

    private static Object parseJsonConfig(final String filename) {
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8);
            return JSONValue.parse(reader);
        } catch (FileNotFoundException e) {
            LOGGER.info("The configuration file " + filename + " could not be found.");
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }
}
