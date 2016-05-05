package bithazard.adaptor.augement.proxy;

import com.google.enterprise.adaptor.Acl;
import com.google.enterprise.adaptor.Response;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Date;

public class ResponseWrapper implements Response {
    private final Response response;

    public ResponseWrapper(Response response) {
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
    public void setContentType(String contentType) {

    }

    @Override
    public void setLastModified(Date lastModified) {

    }

    @Override
    public void addMetadata(String key, String value) {

    }

    @Override
    public void setAcl(Acl acl) {

    }

    @Override
    public void putNamedResource(String fragment, Acl acl) {

    }

    @Override
    public void setSecure(boolean secure) {

    }

    @Override
    public void addAnchor(URI uri, String text) {

    }

    @Override
    public void setNoIndex(boolean noIndex) {

    }

    @Override
    public void setNoFollow(boolean noFollow) {

    }

    @Override
    public void setNoArchive(boolean noArchive) {

    }

    @Override
    public void setDisplayUrl(URI displayUrl) {

    }

    @Override
    public void setCrawlOnce(boolean crawlOnce) {

    }

    @Override
    public void setLock(boolean lock) {

    }
}
