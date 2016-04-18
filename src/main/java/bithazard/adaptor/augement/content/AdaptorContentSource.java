package bithazard.adaptor.augement.content;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class AdaptorContentSource extends AbstractContentSource {
    @Override
    public Status retrieveContent(final String url, final String cookieHeaderValue) throws IOException {
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
    public BufferedImage getContentAsImage() throws IOException {
        return null;
    }

    @Override
    public void writeContent(final OutputStream outputStream) throws IOException {

    }

    @Override
    public void writeImageAsPdf(final BufferedImage image, final OutputStream outputStream) throws IOException {

    }

    @Override
    public void respondRedirect(final OutputStream outputStream) {

    }
}
