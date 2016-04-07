package bithazard.adaptor.augement.content;

import bithazard.adaptor.augement.HttpHeaderHelper;
import bithazard.adaptor.augement.config.PatternConfig;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public class BrowserContentSource implements ContentSource {
    private static final Logger LOGGER = Logger.getLogger(BrowserContentSource.class.getName());
    public static final BrowserVersion DEFAULT_BROWSER_VERSION = BrowserVersion.FIREFOX_38;
    private final PatternConfig config;
    private Page page;

    public BrowserContentSource(final PatternConfig config) {
        this.config = config;
    }

    @Override
    public Status retrieveContent(final String requestUrl) throws IOException {
        BrowserVersion browserVersion = getBrowserVersion(config.getUserAgent());
        try (WebClient webClient = createConfiguredWebClient(browserVersion)) {
            for (Map.Entry<String, String> requestHttpHeader : config.getRequestHeaders().entrySet()) {
                webClient.addRequestHeader(requestHttpHeader.getKey(), requestHttpHeader.getValue());
            }
            try {
                page = webClient.getPage(requestUrl);
            } catch (FailingHttpStatusCodeException e) {
                LOGGER.warning("Got " + e.getStatusCode() + " - " + e.getStatusMessage() + " from URL " + requestUrl);
                return Status.NOT_FOUND;
            }
            webClient.waitForBackgroundJavaScript(config.getJavascriptTimeout());
        }
        if (page instanceof HtmlPage) {
            return Status.HTML_CONTENT;
        } else {
            return Status.BINARY_CONTENT;
        }
    }

    @Override
    public Map<String, String> getHeaders() {
        ensureRetrieveContentCalled();
        //Content-Length, Content-Encoding and Content-MD5 headers are removed in ResponseContentEncoding
        //(Apache httpclient) when content is compressed. These headers cannot be returned here.
        List<NameValuePair> responseHeaders = page.getWebResponse().getResponseHeaders();
        return HttpHeaderHelper.convertToHeaderMap(responseHeaders);
    }

    @Override
    public String getContentAsString() {
        ensureRetrieveContentCalled();
        String htmlContent;
        if (page instanceof HtmlPage) {
            htmlContent = ((HtmlPage) page).asXml();
            htmlContent = htmlContent.replaceFirst("<\\?xml version=\"1.0\" encoding=\"(.+)\"\\?>", "<!DOCTYPE html>");
        } else {
            htmlContent = page.getWebResponse().getContentAsString();
        }
        return htmlContent;
    }

    @Override
    public void writeContent(final OutputStream outputStream) throws IOException {
        ensureRetrieveContentCalled();
        try (InputStream contentAsStream = page.getWebResponse().getContentAsStream()) {
            IOUtils.copy(contentAsStream, outputStream);
        }
    }

    @Override
    public BufferedImage getContentAsImage() throws IOException {
        ensureRetrieveContentCalled();
        return ImageIO.read(page.getWebResponse().getContentAsStream());
    }

    @Override
    public void writeImageAsPdf(final BufferedImage image, final OutputStream outputStream) throws IOException {
        ensureRetrieveContentCalled();
        PDDocument pdDocument  = new PDDocument();
        PDImageXObject pdImageXObject;
        String contentTypeLowerCase = page.getWebResponse().getContentType().toLowerCase(Locale.ENGLISH);
        if (contentTypeLowerCase.contains("image/jpeg") || contentTypeLowerCase.contains("image/jpg")) {
            pdImageXObject = JPEGFactory.createFromImage(pdDocument, image);
        } else {
            pdImageXObject = LosslessFactory.createFromImage(pdDocument, image);
        }
        PDRectangle pdRectangle = new PDRectangle(pdImageXObject.getWidth(), pdImageXObject.getHeight());
        PDPage pdPage = new PDPage();
        pdPage.setMediaBox(pdRectangle);
        pdDocument.addPage(pdPage);
        PDPageContentStream pdPageContentStream = new PDPageContentStream(pdDocument, pdPage);
        pdPageContentStream.drawImage(pdImageXObject, 0.0f, 0.0f);
        pdPageContentStream.close();
        pdDocument.save(outputStream);
        pdDocument.close();
    }

    private void ensureRetrieveContentCalled() {
        if (page == null) {
            throw new IllegalStateException("Call retrieveContent before calling this method.");
        }
    }

    private static BrowserVersion getBrowserVersion(String userAgent) {
        BrowserVersion browserVersion;
        if (userAgent.equals(DEFAULT_BROWSER_VERSION.getUserAgent())) {
            browserVersion = DEFAULT_BROWSER_VERSION;
        } else {
            browserVersion = new BrowserVersion(userAgent, userAgent, userAgent, 0.0F);
        }
        return browserVersion;
    }

    private static WebClient createConfiguredWebClient(BrowserVersion browserVersion) {
        WebClient webClient = new WebClient(browserVersion);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setPrintContentOnFailingStatusCode(false);
        webClient.getCookieManager().setCookiesEnabled(true);
        return webClient;
    }
}
