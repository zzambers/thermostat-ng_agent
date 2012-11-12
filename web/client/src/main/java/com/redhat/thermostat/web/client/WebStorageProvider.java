package com.redhat.thermostat.web.client;

import com.redhat.thermostat.common.config.StartupConfiguration;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.common.storage.StorageProvider;

public class WebStorageProvider implements StorageProvider {

    private StartupConfiguration config;
    
    @Override
    public Storage createStorage() {
        return new WebStorage();
    }

    @Override
    public void setConfig(StartupConfiguration config) {
        this.config = config;
    }

    @Override
    public boolean canHandleProtocol() {
        // use http since this might be https at some point
        return config.getDBConnectionString().startsWith("http");
    }

}
