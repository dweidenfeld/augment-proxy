package bithazard.adaptor.augement.content;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public interface ContentSource {
    Status retrieveContent(String requestUrl) throws IOException;
    Map<String, String> getHeaders();
    String getContentAsString();
    void writeContent(OutputStream outputStream) throws IOException;
    BufferedImage getContentAsImage() throws IOException;
    void writeImageAsPdf(BufferedImage image, OutputStream outputStream) throws IOException;

    enum Status {
        HTML_CONTENT,
        BINARY_CONTENT,
        NOT_FOUND;
    }
}
