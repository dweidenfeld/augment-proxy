package bithazard.adaptor.augement.content;

import bithazard.adaptor.augement.config.PatternConfig;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public class HttpContentSource implements ContentSource {
    private static final Logger LOGGER = Logger.getLogger(HttpContentSource.class.getName());
    private final PatternConfig config;
    private Connection.Response connectionResponse;

    public HttpContentSource(final PatternConfig config) {
        this.config = config;
    }

    @Override
    public Status retrieveContent(final String requestUrl) throws IOException {
        Connection connection = Jsoup.connect(requestUrl);
        connection.userAgent(config.getUserAgent());
        connection.ignoreContentType(true);
        for (Map.Entry<String, String> requestHttpHeader : config.getRequestHeaders().entrySet()) {
            connection.header(requestHttpHeader.getKey(), requestHttpHeader.getValue());
        }
        try {
            connectionResponse = connection.execute();
        } catch (HttpStatusException e) {
            LOGGER.warning("Got " + e.getStatusCode()  + " from URL " + requestUrl);
            return Status.NOT_FOUND;
        }
        String contentType = connectionResponse.contentType();
        if (contentType.startsWith("text/html") || contentType.startsWith("application/xhtml+xml")) {
            return Status.HTML_CONTENT;
        } else {
            return Status.BINARY_CONTENT;
        }
    }

    @Override
    public Map<String, String> getHeaders() {
        if (connectionResponse == null) {
            throw new IllegalStateException("Call retrieveContent before calling this method.");
        }
        return connectionResponse.headers();
    }

    @Override
    public String getContentAsString() {
        if (connectionResponse == null) {
            throw new IllegalStateException("Call retrieveContent before calling this method.");
        }
        return connectionResponse.body();
    }

    @Override
    public void writeContent(final OutputStream outputStream) throws IOException {
        if (connectionResponse == null) {
            throw new IllegalStateException("Call retrieveContent before calling this method.");
        }
        IOUtils.write(connectionResponse.bodyAsBytes(), outputStream);
    }

    @Override
    public BufferedImage getContentAsImage() throws IOException {
        return ImageIO.read(new ByteArrayInputStream(connectionResponse.bodyAsBytes()));
    }

    @Override
    public void writeImageAsPdf(final BufferedImage image, final OutputStream outputStream) throws IOException {
        PDDocument pdDocument  = new PDDocument();
        PDImageXObject pdImageXObject;
        String contentTypeLowerCase = connectionResponse.contentType().toLowerCase(Locale.ENGLISH);
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
}
