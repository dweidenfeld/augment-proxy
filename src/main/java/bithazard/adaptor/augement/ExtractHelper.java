package bithazard.adaptor.augement;

import bithazard.adaptor.augement.config.ExtractConfig;
import com.google.enterprise.adaptor.Acl;
import com.google.enterprise.adaptor.GroupPrincipal;
import com.google.enterprise.adaptor.Response;
import com.google.enterprise.adaptor.UserPrincipal;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExtractHelper {
    private static final Logger LOGGER = Logger.getLogger(ExtractHelper.class.getName());
    private static final String SKIP_METADATA_NAME = "_";

    public static void excludeContent(Document document, Set<String> excludeCssSelectors) {
        for (String excludeCssSelector : excludeCssSelectors) {
            Elements excludeElement = document.select(excludeCssSelector);
            excludeElement.before("<!--googleoff: index--><!--googleoff: snippet-->");
            excludeElement.after("\n<!--googleon: snippet-->\n<!--googleon: index-->");
        }
    }

    public static void extractContent(Response response, Document document, String requestUrl, List<ExtractConfig> extractConfigs) {
        Map<String, Elements> cssCache = new HashMap<>();
        for (ExtractConfig extractConfig : extractConfigs) {
            List<String> regexResults = getRegexResults(document, requestUrl, cssCache, extractConfig);
            processRegexResults(response, document, requestUrl, extractConfig, regexResults);
        }
    }

    private static List<String> getRegexResults(final Document document, final String requestUrl, final Map<String, Elements> cssCache,
                                                final ExtractConfig extractConfig) {
        List<String> regexResults = new ArrayList<>();
        switch (extractConfig.getScope()) {
            case URL: {
                Matcher matcher = extractConfig.getRegexFind().matcher(requestUrl);
                String regexResult = getRegexResult(matcher, extractConfig.getRegexReplace());
                LOGGER.fine("(" + requestUrl + ") Regex result from URL: " + regexResult);
                regexResults.add(regexResult);
                break;
            }
            case CONTENT: {
                String cssSelector = extractConfig.getCssSelector();
                if (cssSelector == null) {
                    String elementContent;
                    if (extractConfig.getRegexMatch() == ExtractConfig.RegexMatch.TEXT) {
                        elementContent = document.text();
                    } else {
                        elementContent = document.html();
                    }
                    Matcher matcher = extractConfig.getRegexFind().matcher(elementContent);
                    String regexResult = getRegexResult(matcher, extractConfig.getRegexReplace());
                    LOGGER.fine("(" + requestUrl + ") Regex result from content: " + regexResult);
                    regexResults.add(regexResult);
                } else {
                    Elements elements = cssCache.get(cssSelector);
                    if (elements == null) {
                        elements = document.select(cssSelector);
                        cssCache.put(cssSelector, elements);
                    }
                    for (Element element : elements) {
                        String elementContent;
                        if (extractConfig.getRegexMatch() == ExtractConfig.RegexMatch.TEXT) {
                            elementContent = element.text();
                        } else {
                            elementContent = element.html();
                        }
                        Matcher matcher = extractConfig.getRegexFind().matcher(elementContent);
                        String regexResult = getRegexResult(matcher, extractConfig.getRegexReplace());
                        LOGGER.fine("(" + requestUrl + ") Regex result from content: " + regexResult);
                        regexResults.add(regexResult);
                    }
                }
                break;
            }
        }
        return regexResults;
    }

    private static void processRegexResults(final Response response, final Document document, final String requestUrl,
                                            final ExtractConfig extractConfig, final List<String> regexResults) {
        switch (extractConfig.getTarget()) {
            case TITLE: {
                String title = regexResults.get(extractConfig.getTitleCssElement());
                if (title != null) {
                    document.title(title);
                } else {
                    LOGGER.warning("(" + requestUrl + ") Regex result for title was null. Not setting title.");
                }
                break;
            }
            case METADATA: {
                for (int i = 0; i < regexResults.size(); i++) {
                    if (i >= extractConfig.getMetadataNames().size()) {
                        break;
                    }
                    String metadataName = extractConfig.getMetadataNames().get(i);
                    if (SKIP_METADATA_NAME.equals(metadataName)) {
                        continue;
                    }
                    String metadataValue = regexResults.get(i);
                    if (metadataValue != null) {
                        response.addMetadata(metadataName, metadataValue);
                    } else {
                        LOGGER.warning("(" + requestUrl + ") Regex result for metadata was null. Not adding metadata.");
                    }
                }
                break;
            }
            case LINK: {
                for (String regexResult : regexResults) {
                    try {
                        if (regexResult != null) {
                            response.addAnchor(new URI(regexResult), null);
                        } else {
                            LOGGER.warning("(" + requestUrl + ") Regex result for link was null. Not adding link.");
                        }
                    } catch (URISyntaxException e) {
                        LOGGER.warning("(" + requestUrl + ") Regex result for link was an invalid URL (" + regexResult
                                + "). Not adding link.");
                    }
                }
                break;
            }
            case ACL_USER: {
                Acl.Builder aclBuilder = new Acl.Builder();
                List<UserPrincipal> userPrincipals = new ArrayList<>();
                for (String regexResult : regexResults) {
                    if (regexResult != null) {
                        userPrincipals.add(new UserPrincipal(regexResult));
                    } else {
                        LOGGER.warning("(" + requestUrl + ") Regex result for ACL user was null. Not adding ACL user.");
                    }
                }
                aclBuilder.setPermitUsers(userPrincipals);
                aclBuilder.setEverythingCaseInsensitive();
                response.setAcl(aclBuilder.build());
                break;
            }
            case ACL_GROUP: {
                Acl.Builder aclBuilder = new Acl.Builder();
                List<GroupPrincipal> groupPrincipals = new ArrayList<>();
                for (String regexResult : regexResults) {
                    if (regexResult != null) {
                        groupPrincipals.add(new GroupPrincipal(regexResult));
                    } else {
                        LOGGER.warning("(" + requestUrl + ") Regex result for ACL group was null. Not adding ACL group.");
                    }
                }
                aclBuilder.setPermitGroups(groupPrincipals);
                aclBuilder.setEverythingCaseInsensitive();
                response.setAcl(aclBuilder.build());
                break;
            }
        }
    }

    private static String getRegexResult(Matcher matcher, String regexReplace) {
        if (regexReplace == null) {
            if (matcher.find()) {
                return matcher.group();
            } else {
                return null;
            }
        } else {
            if (matcher.find()) {
                return matcher.replaceAll(regexReplace);
            } else {
                return null;
            }
        }
    }

    public static void modifyDocumentLinks(Document document, boolean sortParameters, Set<Pattern> removeParameters,
                                           final boolean tlsTermination) {
        if (!sortParameters && !tlsTermination && removeParameters.size() == 0) {
            return;
        }
        modifyMetaHttpEquivRefresh(document, sortParameters, removeParameters, tlsTermination);
        Elements anchors = document.select("a");
        for (Element anchor : anchors) {
            String href = anchor.attr("href");
            href = getModifiedUrl(href, sortParameters, removeParameters, tlsTermination);
            anchor.attr("href", href);
        }
    }

    private static void modifyMetaHttpEquivRefresh(Document document, boolean sortParameters, Set<Pattern> removeParameters,
                                                   final boolean tlsTermination) {
        Element redirect = document.select("meta[http-equiv=refresh]").first();
        if (redirect != null) {
            String redirectContent = redirect.attr("content");
            int indexOfFirstSemicolon = redirectContent.indexOf(";");
            if (indexOfFirstSemicolon != -1) {
                String redirectSeconds = redirectContent.substring(0, indexOfFirstSemicolon);
                String redirectUrl = redirectContent.substring(indexOfFirstSemicolon + 1);
                int indexOfFirstEquals = redirectUrl.indexOf("=");
                if (indexOfFirstEquals != -1) {
                    String urlAttributeName = redirectUrl.substring(0, indexOfFirstEquals);
                    String urlAttributeValue = redirectUrl.substring(indexOfFirstEquals + 1);
                    String encloseChar = "";
                    if (urlAttributeValue.startsWith("'") && urlAttributeValue.endsWith("'")) {
                        encloseChar = "'";
                        urlAttributeValue = urlAttributeValue.substring(1, urlAttributeValue.length() - 2);
                    } else if (urlAttributeValue.startsWith("\"") && urlAttributeValue.endsWith("\"")) {
                        encloseChar = "\"";
                        urlAttributeValue = urlAttributeValue.substring(1, urlAttributeValue.length() - 2);
                    }
                    urlAttributeValue = getModifiedUrl(urlAttributeValue, sortParameters, removeParameters, tlsTermination);
                    String sortedRedirectContent = redirectSeconds + ";" + urlAttributeName + "=" + encloseChar
                            + urlAttributeValue + encloseChar;
                    redirect.attr("content", sortedRedirectContent);
                }
            }
        }
    }

    private static String getModifiedUrl(String url, boolean sortParameters, Set<Pattern> removeParameters,
                                         final boolean tlsTermination) {
        String modifiedUrl;
        if (tlsTermination) {
            modifiedUrl = getHttpUrl(url);
        } else {
            modifiedUrl = url;
        }
        if (!modifiedUrl.contains("?") || (!modifiedUrl.contains("&") && !modifiedUrl.contains("&amp;"))) {
            return modifiedUrl;
        }
        int parametersStart = modifiedUrl.indexOf("?");
        String baseUrl = modifiedUrl.substring(0, parametersStart);
        String parameters = modifiedUrl.substring(parametersStart + 1);
        Set<String> sortedParameterSet;
        if (sortParameters) {
            sortedParameterSet = new TreeSet<>();
        } else {
            sortedParameterSet = new LinkedHashSet<>();
        }
        int currentPosition = 0;
        int nextPosition = 0;
        while (true) {
            int nextPositionAmpersand = parameters.indexOf("&", currentPosition);
            int nextPositionEncodedAmpersand = parameters.indexOf("&amp;", currentPosition);
            if (nextPositionAmpersand == -1 && nextPositionEncodedAmpersand == -1) {
                break;
            } else if (nextPositionAmpersand != -1 && nextPositionEncodedAmpersand == -1) {
                nextPosition = nextPositionAmpersand;
            } else if (nextPositionAmpersand == -1 && nextPositionEncodedAmpersand != -1) {
                nextPosition = nextPositionEncodedAmpersand;
            } else if (nextPositionAmpersand != -1 && nextPositionEncodedAmpersand != -1) {
                nextPosition = Math.min(nextPositionAmpersand, nextPositionEncodedAmpersand);
            }
            String currentParameter = parameters.substring(currentPosition, nextPosition);
            if (!matchesAnyPattern(currentParameter, removeParameters)) {
                sortedParameterSet.add(currentParameter);
            }
            currentPosition = nextPosition + 1;
        }
        String finalParameter = parameters.substring(currentPosition);
        if (!matchesAnyPattern(finalParameter, removeParameters)) {
            sortedParameterSet.add(finalParameter);
        }
        StringBuilder sortedParameters = new StringBuilder(baseUrl);
        sortedParameters.append("?");
        for (String parameter : sortedParameterSet) {
            sortedParameters.append(parameter).append("&");
        }
        return sortedParameters.substring(0, sortedParameters.length() - 2);
    }

    private static boolean matchesAnyPattern(String string, Set<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(string).matches()) {
                return true;
            }
        }
        return false;
    }

    private static String getHttpUrl(final String url) {
        if (url.startsWith("https://")) {
            return "http" + url.substring(5);
        }
        return url;
    }
}
