package bithazard.adaptor.augement.proxy;

import com.google.enterprise.adaptor.Acl;
import com.google.enterprise.adaptor.Response;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Date;

public class ResponseWrapper implements Response {
    private final Response response;

    public ResponseWrapper(final Response response) {
        this.response = response;
    }

    @Override
    public void respondNotModified() throws IOException {

    }

    @Override
    public void respondNotFound() throws IOException {

    }

    @Override
    public void respondNoContent() throws IOException {

    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return null;
    }

    @Override
    public void setContentType(final String contentType) {

    }

    @Override
    public void setLastModified(final Date lastModified) {

    }

    @Override
    public void addMetadata(final String key, final String value) {

    }

    @Override
    public void setAcl(final Acl acl) {

    }

    @Override
    public void putNamedResource(final String fragment, final Acl acl) {

    }

    @Override
    public void setSecure(final boolean secure) {

    }

    @Override
    public void addAnchor(final URI uri, final String text) {

    }

    @Override
    public void setNoIndex(final boolean noIndex) {

    }

    @Override
    public void setNoFollow(final boolean noFollow) {

    }

    @Override
    public void setNoArchive(final boolean noArchive) {

    }

    @Override
    public void setDisplayUrl(final URI displayUrl) {

    }

    @Override
    public void setCrawlOnce(final boolean crawlOnce) {

    }

    @Override
    public void setLock(final boolean lock) {

    }
}
