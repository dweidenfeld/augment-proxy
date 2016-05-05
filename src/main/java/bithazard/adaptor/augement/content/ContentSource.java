package bithazard.adaptor.augement.content;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface ContentSource {
    Status retrieveContent(String url, String cookieHeaderValue) throws IOException;
    Map<String, List<String>> getHeaders();
    String getContentAsString();
    BufferedImage getContentAsImage() throws IOException;
    void writeContent(OutputStream outputStream) throws IOException;
    void writeImageAsPdf(BufferedImage image, OutputStream outputStream) throws IOException;
    void respondRedirect(OutputStream outputStream) throws IOException;

    enum Status {
        HTML_CONTENT,
        BINARY_CONTENT,
        NOT_FOUND,
        REDIRECT;
    }
}
