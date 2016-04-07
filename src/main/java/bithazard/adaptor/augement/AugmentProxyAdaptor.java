package bithazard.adaptor.augement;

import bithazard.adaptor.augement.cache.SitemapCache;
import bithazard.adaptor.augement.config.AdaptorConfig;
import bithazard.adaptor.augement.config.AugmentConfigException;
import bithazard.adaptor.augement.config.ConfigHelper;
import bithazard.adaptor.augement.config.FeedConfig;
import bithazard.adaptor.augement.config.ImageLinkConfig;
import bithazard.adaptor.augement.config.PatternConfig;
import bithazard.adaptor.augement.content.BrowserContentSource;
import bithazard.adaptor.augement.content.ContentSource;
import bithazard.adaptor.augement.content.HttpContentSource;
import bithazard.sitemap.parser.model.Link;
import bithazard.sitemap.parser.model.SitemapEntry;
import com.google.enterprise.adaptor.AbstractAdaptor;
import com.google.enterprise.adaptor.AdaptorContext;
import com.google.enterprise.adaptor.Application;
import com.google.enterprise.adaptor.Config;
import com.google.enterprise.adaptor.DocId;
import com.google.enterprise.adaptor.DocIdPusher;
import com.google.enterprise.adaptor.PollingIncrementalLister;
import com.google.enterprise.adaptor.Request;
import com.google.enterprise.adaptor.Response;
import com.google.enterprise.adaptor.StartupException;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

//TODOs:
//- support for RSS and ATOM feeds. Send all links as feed in the getDocIds method (cache links and meta information)
//-- possibility to configure whether meta information of RSS and ATOM should be fed
//- act as proxy for other adaptors by calling the respective methods directly
public class AugmentProxyAdaptor extends AbstractAdaptor implements PollingIncrementalLister {
    private static final Logger LOGGER = Logger.getLogger(AugmentProxyAdaptor.class.getName());
    private static final ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        }
    };
    private static final String ADAPTOR_INCREMENTAL_POLL_PERIOD_SECS = "adaptor.incrementalPollPeriodSecs";
    private static final String SERVER_DOC_ID_PATH = "server.docIdPath";
    private static final String DOC_ID_IS_URL = "docId.isUrl";
    private static final String ADAPTOR_CONFIG = "adaptor.config";
    private static final String AUGMENT_CONFIG = "augment.config";
    private AdaptorConfig adaptorConfig;
    private final List<PatternConfig> patternConfigs = new ArrayList<>();
    private SitemapCache sitemapCache;
    private long incrementalPollingInterval;

    public static void main(String[] args) {
        Application.main(new AugmentProxyAdaptor(), args);
    }

    @Override
    public void initConfig(Config config) {
        config.overrideKey(SERVER_DOC_ID_PATH, "/");
        config.overrideKey(DOC_ID_IS_URL, "true");
        config.addKey(ADAPTOR_CONFIG, "adaptor-config.json");
        config.addKey(AUGMENT_CONFIG, "augment-config.json");
    }

    @Override
    public void init(AdaptorContext context) throws Exception {
        Config config = context.getConfig();
        boolean docIdIsUrl = Boolean.parseBoolean(config.getValue(DOC_ID_IS_URL));
        if (!docIdIsUrl) {
            LOGGER.warning("Property " + DOC_ID_IS_URL + " was set to false in the config. The adaptor will most "
                    + "likely not work correctly. Please make sure you know what you are doing.");
        }
        String docIdPath = config.getValue(SERVER_DOC_ID_PATH);
        if (!"/".equals(docIdPath)) {
            LOGGER.warning("Property " + SERVER_DOC_ID_PATH + " was set to something other than '/' in the config. You "
                    + "will not be able to configure this path in the GSA proxy servers config.");
        }
        this.incrementalPollingInterval = Long.parseLong(config.getValue(ADAPTOR_INCREMENTAL_POLL_PERIOD_SECS));

        String adaptorConfigFilename = config.getValue(ADAPTOR_CONFIG);
        try {
            this.adaptorConfig = ConfigHelper.parseAdaptorConfig(adaptorConfigFilename);
        } catch (AugmentConfigException ex) {
            throw new StartupException(ex);
        }
        if (adaptorConfig.getSitemapConfig().size() > 0) {
            this.sitemapCache = new SitemapCache();
        }

        String augmentConfigFilename = config.getValue(AUGMENT_CONFIG);
        try {
            ConfigHelper.initAugmentConfig(augmentConfigFilename, patternConfigs);
        } catch (AugmentConfigException | IllegalArgumentException ex) {
            throw new StartupException(ex);
        }
    }

    @Override
    public void getDocIds(DocIdPusher docIdPusher) throws IOException, InterruptedException {
        List<DocIdPusher.Record> records = new ArrayList<>();
        addSitemapRecordsAfter(records, null);

        docIdPusher.pushRecords(records);
    }

    @Override
    public void getModifiedDocIds(DocIdPusher docIdPusher) throws IOException, InterruptedException {
        List<DocIdPusher.Record> records = new ArrayList<>();
        long lastModifiedDocsPush = new Date().getTime() - this.incrementalPollingInterval * 1000;
        Date lastModifiedDocsPushDate = new Date(lastModifiedDocsPush);
        addSitemapRecordsAfter(records, lastModifiedDocsPushDate);

        docIdPusher.pushRecords(records);
    }

    private void addSitemapRecordsAfter(final List<DocIdPusher.Record> records, final Date minDate) {
        for (FeedConfig feedConfig : adaptorConfig.getSitemapConfig()) {
            Collection<SitemapEntry> sitemapEntries = this.sitemapCache.getSitemapEntriesAfter(feedConfig.getUrl(), minDate);
            for (SitemapEntry sitemapEntry : sitemapEntries) {
                DocId docId = new DocId(sitemapEntry.getLoc());
                DocIdPusher.Record.Builder builder = new DocIdPusher.Record.Builder(docId);
                builder.setLastModified(sitemapEntry.getLastMod());
                records.add(builder.build());
            }
        }
    }

    @Override
    public void getDocContent(Request request, Response response) throws IOException, InterruptedException {
        String requestUrl = request.getDocId().getUniqueId();
        LOGGER.fine("Processing request for URL " + requestUrl);
        for (PatternConfig config : patternConfigs) {
            if (config.getUrlPattern().matcher(requestUrl).matches()) {
                ContentSource contentSource;
                if (config.isExecuteJavascript()) {
                    contentSource = new BrowserContentSource(config);
                } else {
                    contentSource = new HttpContentSource(config);
                }
                ContentSource.Status status = contentSource.retrieveContent(requestUrl);
                if (status == ContentSource.Status.NOT_FOUND) {
                    response.respondNotFound();
                    return;
                }
                String content = null;
                if (status == ContentSource.Status.HTML_CONTENT) {
                    content = contentSource.getContentAsString();
                    if (config.getOmitContentRegex() != null && config.getOmitContentRegex().matcher(content).matches()) {
                        response.respondNotFound();
                        return;
                    }
                }

                Map<String, String> headers = contentSource.getHeaders();
                HttpHeaderHelper.transferDefaultHeadersToResponse(headers, response);
                if (config.isPassGsaHeaders()) {
                    HttpHeaderHelper.transferGsaHeadersToResponse(headers, response);
                }
                HttpHeaderHelper.convertHeadersToMetadata(response, headers, config.getHeadersToMetadata());
                HttpHeaderHelper.setHeaders(response, config.isSetNoFollow(), config.isSetCrawlOnce(),
                        config.isSetNoArchive(), config.isSetNoIndex());

                addSitemapMetadata(response, adaptorConfig.getSitemapConfig(), requestUrl);

                if (status == ContentSource.Status.BINARY_CONTENT) {
                    ImageLinkConfig imageLinkConfig = config.getImageLinkConfig();
                    if (imageLinkConfig != null && requestUrl.contains(ImageLinkHelper.CONVERT_TO_PDF_PARAMETER)) {
                        BufferedImage image = contentSource.getContentAsImage();
                        if (image.getWidth() < imageLinkConfig.getMinWidth() || image.getHeight() < imageLinkConfig.getMinHeight()) {
                            response.respondNotFound();
                            return;
                        }
                        response.setContentType("application/pdf");
                        ImageLinkHelper.setDisplayUrl(response, requestUrl);
                        ImageLinkHelper.addImageMetadata(response, image);
                        contentSource.writeImageAsPdf(image, response.getOutputStream());
                    } else {
                        contentSource.writeContent(response.getOutputStream());
                    }
                    return;
                }
                assert content != null;
                if (config.getExcludeCssSelectors().size() > 0 || config.getExtractConfigs().size() > 0 || config.isSortParameters()
                        || config.getRemoveParameters().size() > 0 || config.getImageLinkConfig() != null) {
                    Document document = Jsoup.parse(content);
                    ExtractHelper.excludeContent(document, config.getExcludeCssSelectors());
                    ExtractHelper.extractContent(response, document, requestUrl, config.getExtractConfigs());
                    ExtractHelper.modifyUrlParameters(document, config.isSortParameters(), config.getRemoveParameters());
                    ImageLinkHelper.addImageLinksAsPdf(document, config.getImageLinkConfig());
                    content = document.outerHtml();
                }
                IOUtils.write(content, response.getOutputStream());
                return;
            }
        }
        LOGGER.fine("Found no matching pattern for URL " + requestUrl + " - returning 404.");
        response.respondNotFound();
    }

    private void addSitemapMetadata(final Response response, final Set<FeedConfig> sitemapConfigs, final String requestUrl) {
        FeedConfig matchingSitemapConfig = null;
        for (FeedConfig sitemapConfig : sitemapConfigs) {
            String applicableUrl = getApplicableUrl(sitemapConfig.getUrl());
            if (requestUrl.startsWith(applicableUrl)) {
                matchingSitemapConfig = sitemapConfig;
            }
        }
        if (matchingSitemapConfig == null || !matchingSitemapConfig.isAddMetadata()) {
            return;
        }
        SitemapEntry sitemapEntry = sitemapCache.getSitemapEntry(matchingSitemapConfig.getUrl(), requestUrl);
        if (sitemapEntry.getChangeFreq() != null) {
            response.addMetadata("sitemap-change-frequency", sitemapEntry.getChangeFreq().toString());
        }
        if (sitemapEntry.getPriority() != null) {
            response.addMetadata("sitemap-priority", sitemapEntry.getPriority().toString());
        }
        if (sitemapEntry.getLastMod() != null) {
            response.addMetadata("sitemap-date", DATE_FORMAT.get().format(sitemapEntry.getLastMod()));
        }
        if (sitemapEntry.getLinks() != null) {
            for (Link currentLink : sitemapEntry.getLinks()) {
                if (currentLink.getHref().equals(requestUrl)) {
                    response.addMetadata("sitemap-language", currentLink.getLang());
                    break;
                }
            }

        }
    }

    private String getApplicableUrl(final String url) {
        return url.substring(0, url.lastIndexOf("/"));
    }
}
