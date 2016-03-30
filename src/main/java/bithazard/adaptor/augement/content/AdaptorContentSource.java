package bithazard.adaptor.augement.content;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class AdaptorContentSource implements ContentSource {
    @Override
    public Status retrieveContent(final String requestUrl) throws IOException {
        return null;
    }

    @Override
    public Map<String, String> getHeaders() {
        return null;
    }

    @Override
    public String getContentAsString() {
        return null;
    }

    @Override
    public void writeContent(final OutputStream outputStream) throws IOException {

    }

    @Override
    public BufferedImage getContentAsImage() throws IOException {
        return null;
    }

    @Override
    public void writeImageAsPdf(final BufferedImage image, final OutputStream outputStream) throws IOException {

    }
}
