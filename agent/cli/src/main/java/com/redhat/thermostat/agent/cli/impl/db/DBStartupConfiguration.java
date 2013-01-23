/*
 * Copyright 2012, 2013 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.agent.cli.impl.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.storage.config.StartupConfiguration;

public class DBStartupConfiguration implements StartupConfiguration {
    
    private File dbPath;
    private File logFile;
    private File pidFile;
    private boolean sslEnabled = false;
    private File sslPemFile;
    private String sslKeyPassphrase;
        
    private long localPort;
    
    private String dbConnectionString;
    
    private String ip;
        
    public DBStartupConfiguration(File properties, File dbPath, File logFile,
            File pidFile) throws InvalidConfigurationException {
        this.dbPath = dbPath;
        this.logFile = logFile;
        this.pidFile = pidFile;
        readAndSetProperties(properties);
    }
    
    public File getDBPath() {
        return dbPath;
    }
    
    public File getLogFile() {
        return logFile;
    }
    
    public File getPidFile() {
        return pidFile;
    }
   
    public void setPort(long localPort) {
        this.localPort = localPort;
    }
    
    public long getPort() {
        return localPort;
    }
    
    void setDBConnectionString(String dbConnectionString) {
        this.dbConnectionString = dbConnectionString;
    }
    
    @Override
    public String getDBConnectionString() {
        return dbConnectionString;
    }

    void setBindIP(String ip) {
        this.ip = ip;
    }
    
    public String getBindIP() {
        return ip;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    /**
     * 
     * @return The file containing the server certificate and the private key in PEM format or null
     *         if nothing was specified in $THERMOSTAT_HOME/storage/db.properties.
     */
    public File getSslPemFile() {
        return sslPemFile;
    }

    void setSslPemFile(File sslPemFile) {
        this.sslPemFile = sslPemFile;
    }

    /**
     * 
     * @return The passphrase for the encrypted server key or null if config was
     *         not set.
     */
    public String getSslKeyPassphrase() {
        return sslKeyPassphrase;
    }

    void setSslKeyPassphrase(String sslKeyPassphrase) {
        this.sslKeyPassphrase = sslKeyPassphrase;
    }
    
    private void readAndSetProperties(File propertyFile) throws InvalidConfigurationException {
        
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(propertyFile));
            
        } catch (IOException e) {
            throw new InvalidConfigurationException(e);
        }
        
        if (properties.containsKey(DBConfig.PORT.name())) {
            String port = (String) properties.get(DBConfig.PORT.name());
            int localPort = Integer.parseInt(port);
            setPort(localPort);
        } else {
            throw new InvalidConfigurationException(DBConfig.PORT + " property missing");
        }
        
        if (properties.containsKey(DBConfig.BIND.name())) {
            String ip = (String) properties.get(DBConfig.BIND.name());
            setBindIP(ip);
        } else {
            throw new InvalidConfigurationException(DBConfig.BIND + " property missing");
        }
        
        // optional config
        String enableSSLConfig = properties.getProperty(DBConfig.SSL_ENABLE.name());
        setSslEnabled(Boolean.parseBoolean(enableSSLConfig));
        
        String pemFile = properties.getProperty(DBConfig.SSL_PEM_FILE.name());
        if (pemFile != null) {
            setSslPemFile(new File(pemFile));
        }
        
        String keyPassPhrase = properties.getProperty(DBConfig.SSL_KEY_PASSWORD.name());
        setSslKeyPassphrase(keyPassPhrase);
        
    }
}

