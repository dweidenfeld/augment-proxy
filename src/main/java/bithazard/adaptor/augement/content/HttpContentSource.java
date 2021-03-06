package bithazard.adaptor.augement.content;

import bithazard.adaptor.augement.HttpHeaderHelper;
import bithazard.adaptor.augement.config.PatternConfig;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpContentSource extends AbstractContentSource {
    private static final Logger LOGGER = Logger.getLogger(HttpContentSource.class.getName());
    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom().setConnectTimeout(180000).setSocketTimeout(180000).build();
    private final PatternConfig config;
    private Map<String, List<String>> headers;
    private String contentType;
    private byte[] content;
    private String locationHeader;

    public HttpContentSource(PatternConfig config) {
        this.config = config;
    }

    @Override
    public Status retrieveContent(String url, String cookieHeaderValue) throws IOException {
        CookieStore cookieStore = null;
        if (cookieHeaderValue != null) {
            cookieStore = new BasicCookieStore();
            String domain = getDomain(url);
            Map<String, String> cookies = parseCookieHeader(cookieHeaderValue);
            for (Map.Entry<String, String> cookie : cookies.entrySet()) {
                BasicClientCookie clientCookie = new BasicClientCookie(cookie.getKey(), cookie.getValue());
                clientCookie.setDomain(domain);
                cookieStore.addCookie(clientCookie);
            }
        }

        HttpClientBuilder httpClientBuilder = HttpClients.custom().setDefaultCookieStore(cookieStore)
                .setUserAgent(config.getUserAgent()).disableRedirectHandling();
        try {
            SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
            sslContextBuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContextBuilder.build());
            httpClientBuilder.setSSLSocketFactory(sslsf);
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            LOGGER.log(Level.WARNING, "Error creating SSLConnectionSocketFactory: Invalid SSL connections will not be ignored.", e);
        }

        try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setConfig(REQUEST_CONFIG);
            for (Map.Entry<String, String> requestHttpHeader : config.getRequestHeaders().entrySet()) {
                httpGet.setHeader(requestHttpHeader.getKey(), requestHttpHeader.getValue());
            }
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                this.headers = HttpHeaderHelper.convertToHeaderMap(response.getAllHeaders());
                this.contentType = response.getFirstHeader("Content-Type").getValue();
                this.content = EntityUtils.toByteArray(response.getEntity());
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode < 200 || statusCode >= 400) {
                    LOGGER.warning("Got " + statusCode + " from URL " + url);
                    return Status.NOT_FOUND;
                }
                if (statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308) {
                    this.locationHeader = response.getFirstHeader("Location").getValue();
                    return Status.REDIRECT;
                }
                if (contentType.startsWith("text/html") || contentType.startsWith("application/xhtml+xml")) {
                    return Status.HTML_CONTENT;
                } else {
                    return Status.BINARY_CONTENT;
                }
            }
        }
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        if (this.headers == null) {
            throw new IllegalStateException("Call retrieveContent before calling this method.");
        }
        return this.headers;
    }

    @Override
    public String getContentAsString() {
        if (this.content == null) {
            throw new IllegalStateException("Call retrieveContent before calling this method.");
        }
        return new String(this.content, StandardCharsets.UTF_8);
    }

    @Override
    public BufferedImage getContentAsImage() throws IOException {
        if (this.content == null) {
            throw new IllegalStateException("Call retrieveContent before calling this method.");
        }
        return ImageIO.read(new ByteArrayInputStream(this.content));
    }

    @Override
    public void writeContent(OutputStream outputStream) throws IOException {
        if (this.content == null) {
            throw new IllegalStateException("Call retrieveContent before calling this method.");
        }
        IOUtils.write(this.content, outputStream);
    }

    @Override
    public void writeImageAsPdf(BufferedImage image, OutputStream outputStream) throws IOException {
        if (this.contentType == null) {
            throw new IllegalStateException("Call retrieveContent before calling this method.");
        }
        writeImageAsPdf(image, outputStream, this.contentType);
    }

    @Override
    public void respondRedirect(OutputStream outputStream) throws IOException {
        if (this.locationHeader == null) {
            throw new IllegalStateException("Call retrieveContent before calling this method.");
        }
        String redirectHtml = getRedirectHtml(locationHeader);
        outputStream.write(redirectHtml.getBytes(StandardCharsets.UTF_8));
    }
}
