package bithazard.adaptor.augement.proxy;

import com.google.enterprise.adaptor.Adaptor;
import com.google.enterprise.adaptor.AdaptorContext;
import com.google.enterprise.adaptor.Config;
import com.google.enterprise.adaptor.DocIdPusher;
import com.google.enterprise.adaptor.PollingIncrementalLister;
import com.google.enterprise.adaptor.Request;
import com.google.enterprise.adaptor.Response;

import java.io.File;
import java.io.IOException;

public final class AdaptorProxy {
    private final Adaptor adaptorInstance;
    private final PollingIncrementalLister pollingIncrementalListerInstance;
    private final File configFile;
    private final Config config;

    public AdaptorProxy(String adaptorClassName, File configFile) {
        try {
            Class<?> adaptorClass = Class.forName(adaptorClassName);
            this.adaptorInstance = (Adaptor) adaptorClass.newInstance();
            if (this.adaptorInstance instanceof PollingIncrementalLister) {
                pollingIncrementalListerInstance = (PollingIncrementalLister) this.adaptorInstance;
            } else {
                pollingIncrementalListerInstance = new NoOpPollingIncrementalLister();
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            throw new AdaptorProxyException(e);
        }
        if (configFile == null || !configFile.exists() || !configFile.isFile()) {
            throw new AdaptorProxyException("Supplied config file for " + adaptorClassName + " is invalid.");
        }
        this.configFile = configFile;
        this.config = new Config();
    }

    public void initConfig() {
        this.adaptorInstance.initConfig(this.config);
        try {
            this.config.load(configFile);
        } catch (IOException e) {
            throw new AdaptorProxyException("Could not load config file " + configFile.getAbsolutePath(), e);
        }
        this.config.validate();
    }

    public void init(AdaptorContext adaptorContext) throws Exception {
        AdaptorContextWrapper adaptorContextWrapper = new AdaptorContextWrapper(adaptorContext);
        this.adaptorInstance.init(adaptorContextWrapper);
    }

    public void getDocContent(Request request, Response response) throws IOException, InterruptedException {
        ResponseWrapper responseWrapper = new ResponseWrapper(response);
        this.adaptorInstance.getDocContent(request, responseWrapper);
    }

    public void getDocIds(DocIdPusher docIdPusher) throws IOException, InterruptedException {
        this.adaptorInstance.getDocIds(docIdPusher);
    }

    public void destroy() {
        this.adaptorInstance.destroy();
    }

    public void getModifiedDocIds(DocIdPusher docIdPusher) throws IOException, InterruptedException {
        this.pollingIncrementalListerInstance.getModifiedDocIds(docIdPusher);
    }

    private static final class NoOpPollingIncrementalLister implements PollingIncrementalLister {
        @Override
        public void getModifiedDocIds(DocIdPusher docIdPusher) throws IOException, InterruptedException {
            //do nothing
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AdaptorProxy)) {
            return false;
        }

        AdaptorProxy that = (AdaptorProxy) obj;

        if (!adaptorInstance.equals(that.adaptorInstance)) {
            return false;
        }
        return configFile.equals(that.configFile);

    }

    @Override
    public int hashCode() {
        int result = adaptorInstance.hashCode();
        result = 31 * result + configFile.hashCode();
        return result;
    }
}
