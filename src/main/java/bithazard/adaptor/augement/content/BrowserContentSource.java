package bithazard.adaptor.augement.content;

import bithazard.adaptor.augement.AugmentProxyException;
import bithazard.adaptor.augement.HttpHeaderHelper;
import bithazard.adaptor.augement.config.PatternConfig;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class BrowserContentSource  extends AbstractContentSource {
    private static final Logger LOGGER = Logger.getLogger(BrowserContentSource.class.getName());
    public static final BrowserVersion DEFAULT_BROWSER_VERSION = BrowserVersion.FIREFOX_45;
    private final PatternConfig config;
    private Page page;
    private String locationHeader;

    public BrowserContentSource(PatternConfig config) {
        this.config = config;
    }

    @Override
    public Status retrieveContent(String url, String cookieHeaderValue) throws IOException {
        BrowserVersion browserVersion = getBrowserVersion(config.getUserAgent());
        try (WebClient webClient = createConfiguredWebClient(browserVersion)) {
            for (Map.Entry<String, String> requestHttpHeader : config.getRequestHeaders().entrySet()) {
                webClient.addRequestHeader(requestHttpHeader.getKey(), requestHttpHeader.getValue());
            }
            if (cookieHeaderValue != null) {
                CookieManager cookieManager = webClient.getCookieManager();
                String domain = getDomain(url);
                Map<String, String> cookies = parseCookieHeader(cookieHeaderValue);
                for (Map.Entry<String, String> cookie : cookies.entrySet()) {
                    cookieManager.addCookie(new Cookie(domain, cookie.getKey(), cookie.getValue()));
                }
            }
            try {
                page = webClient.getPage(url);
            } catch (FailingHttpStatusCodeException e) {
                int statusCode = e.getStatusCode();
                if (statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308) {
                    this.locationHeader = e.getResponse().getResponseHeaderValue("Location");
                    return Status.REDIRECT;
                }
                LOGGER.warning("Got " + statusCode + " - " + e.getStatusMessage() + " from URL " + url);
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
    public Map<String, List<String>> getHeaders() {
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
        String charset = page.getWebResponse().getContentCharsetOrNull();
        if (charset != null && !charset.equals("UTF-8") && Charset.isSupported(charset)) {
            byte byteContent[];
            try {
                byteContent = htmlContent.getBytes(charset);
            } catch (UnsupportedEncodingException e) {
                throw new AugmentProxyException("Charset " + charset + " is not supported, although Charset.isSupported"
                        + " returned true for this charset. This should not be possible.");
            }
            htmlContent = new String(byteContent, StandardCharsets.UTF_8);
        }
        return htmlContent;
    }

    @Override
    public BufferedImage getContentAsImage() throws IOException {
        ensureRetrieveContentCalled();
        return ImageIO.read(page.getWebResponse().getContentAsStream());
    }

    @Override
    public void writeContent(OutputStream outputStream) throws IOException {
        ensureRetrieveContentCalled();
        try (InputStream contentAsStream = page.getWebResponse().getContentAsStream()) {
            IOUtils.copy(contentAsStream, outputStream);
        }
    }

    @Override
    public void writeImageAsPdf(BufferedImage image, OutputStream outputStream) throws IOException {
        ensureRetrieveContentCalled();
        writeImageAsPdf(image, outputStream, page.getWebResponse().getContentType());
    }

    @Override
    public void respondRedirect(OutputStream outputStream) throws IOException {
        ensureRetrieveContentCalled();
        String redirectHtml = getRedirectHtml(locationHeader);
        outputStream.write(redirectHtml.getBytes(StandardCharsets.UTF_8));
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
            browserVersion = new BrowserVersion(userAgent, userAgent, userAgent, 0);
        }
        return browserVersion;
    }

    private static WebClient createConfiguredWebClient(BrowserVersion browserVersion) {
        WebClient webClient = new WebClient(browserVersion);
        webClient.getOptions().setRedirectEnabled(false);
        webClient.getOptions().setTimeout(180000);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setPrintContentOnFailingStatusCode(false);
        webClient.getCookieManager().setCookiesEnabled(true);
        return webClient;
    }
}
