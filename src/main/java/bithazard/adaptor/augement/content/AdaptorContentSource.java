package bithazard.adaptor.augement.content;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class AdaptorContentSource extends AbstractContentSource {
    @Override
    public Status retrieveContent(String url, String cookieHeaderValue) throws IOException {
        return null;
    }

    @Override
    public Map<String, List<String>> getHeaders() {
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
    public void writeContent(OutputStream outputStream) throws IOException {

    }

    @Override
    public void writeImageAsPdf(BufferedImage image, OutputStream outputStream) throws IOException {

    }

    @Override
    public void respondRedirect(OutputStream outputStream) {

    }
}
