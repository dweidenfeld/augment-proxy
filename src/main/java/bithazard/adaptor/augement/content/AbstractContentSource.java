package bithazard.adaptor.augement.content;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public abstract class AbstractContentSource implements ContentSource {
    public static String getDomain(String url) {
        String domain;
        int protocolEndPosition = url.indexOf("://");
        if (protocolEndPosition != -1) {
            domain = url.substring(protocolEndPosition + 3);
        } else {
            domain = url;
        }
        int domainEndPosition = domain.indexOf("/");
        if (domainEndPosition != -1) {
            return domain.substring(0, domainEndPosition);
        }
        return domain;
    }

    public Map<String, String> parseCookieHeader(final String cookieHeaderValue) {
        Map<String, String> result = new LinkedHashMap<>();
        String[] cookiePairs = cookieHeaderValue.split(";");
        for (String cookiePair : cookiePairs) {
            String[] cookie = cookiePair.split("=");
            if (cookie.length != 2) {
                continue;
            }
            result.put(cookie[0].trim(), cookie[1].trim());
        }
        return result;
    }

    public void writeImageAsPdf(final BufferedImage image, final OutputStream outputStream, final String contentType)
            throws IOException {
        PDDocument pdDocument  = new PDDocument();
        PDImageXObject pdImageXObject;
        String contentTypeLowerCase = contentType.toLowerCase(Locale.ENGLISH);
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

    public String getRedirectHtml(String url) {
        return "<!DOCTYPE html><html><head><meta http-equiv=\"refresh\" content=\"0;URL='" + url
                + "'\"/></head><body></body></html>";
    }
}
