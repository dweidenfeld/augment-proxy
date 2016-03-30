package bithazard.adaptor.augement.proxy;

import com.google.enterprise.adaptor.AdaptorContext;
import com.google.enterprise.adaptor.AsyncDocIdPusher;
import com.google.enterprise.adaptor.AuthnAuthority;
import com.google.enterprise.adaptor.AuthzAuthority;
import com.google.enterprise.adaptor.Config;
import com.google.enterprise.adaptor.DocIdEncoder;
import com.google.enterprise.adaptor.DocIdPusher;
import com.google.enterprise.adaptor.ExceptionHandler;
import com.google.enterprise.adaptor.PollingIncrementalLister;
import com.google.enterprise.adaptor.SensitiveValueDecoder;
import com.google.enterprise.adaptor.Session;
import com.google.enterprise.adaptor.StatusSource;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class AdaptorContextWrapper implements AdaptorContext {
    private final AdaptorContext adaptorContext;

    public AdaptorContextWrapper(final AdaptorContext adaptorContext) {
        this.adaptorContext = adaptorContext;
    }

    @Override
    public Config getConfig() {
        return null;
    }

    @Override
    public DocIdPusher getDocIdPusher() {
        return null;
    }

    @Override
    public AsyncDocIdPusher getAsyncDocIdPusher() {
        return null;
    }

    @Override
    public DocIdEncoder getDocIdEncoder() {
        return null;
    }

    @Override
    public void addStatusSource(final StatusSource source) {

    }

    @Override
    public void setGetDocIdsFullErrorHandler(final ExceptionHandler handler) {

    }

    @Override
    public ExceptionHandler getGetDocIdsFullErrorHandler() {
        return null;
    }

    @Override
    public void setGetDocIdsIncrementalErrorHandler(final ExceptionHandler handler) {

    }

    @Override
    public ExceptionHandler getGetDocIdsIncrementalErrorHandler() {
        return null;
    }

    @Override
    public SensitiveValueDecoder getSensitiveValueDecoder() {
        return null;
    }

    @Override
    public HttpContext createHttpContext(final String path, final HttpHandler handler) {
        return null;
    }

    @Override
    public Session getUserSession(final HttpExchange ex, final boolean create) {
        return null;
    }

    @Override
    public void setPollingIncrementalLister(final PollingIncrementalLister lister) {

    }

    @Override
    public void setAuthnAuthority(final AuthnAuthority authnAuthority) {

    }

    @Override
    public void setAuthzAuthority(final AuthzAuthority authzAuthority) {

    }
}
