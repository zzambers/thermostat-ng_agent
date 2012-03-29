/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.agent.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;

import com.redhat.thermostat.common.config.ConfigUtils;
import com.redhat.thermostat.common.config.InvalidConfigurationException;

public class AgentConfigsUtils {

    public static AgentStartupConfiguration createAgentConfigs() throws InvalidConfigurationException {
        
        AgentStartupConfiguration config = new AgentStartupConfiguration();
        
        File propertyFile = ConfigUtils.getAgentConfigurationFile();
        
        config.setLogLevel(Level.FINE);
        readAndSetProperties(propertyFile, config);
        
        return config;
    }
    
    private static void readAndSetProperties(File propertyFile, AgentStartupConfiguration configuration)
            throws InvalidConfigurationException
    {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(propertyFile));
            
        } catch (IOException e) {
            throw new InvalidConfigurationException(e);
        }
        
        if (properties.containsKey(AgentProperties.BACKENDS.name())) {
            // this is a command separated list of backends
            String backends = properties.getProperty(AgentProperties.BACKENDS.name());
            configuration.parseBackends(backends.split(","));
            
        } else {
            throw new InvalidConfigurationException(AgentProperties.BACKENDS + " property missing");
        }
        
        if (properties.containsKey(AgentProperties.LOG_LEVEL.name())) {
            String logLevel = properties.getProperty(AgentProperties.LOG_LEVEL.name());
            Level level = getLogLevel(logLevel);
            configuration.setLogLevel(level);
        }
        
        if (properties.containsKey(AgentProperties.DB_URL.name())) {
            String db = properties.getProperty(AgentProperties.DB_URL.name());
            configuration.setDatabaseURL(db);
        }
    }
    
    public static Level getLogLevel(String logLevel) {
        
        Level level = Level.FINE;
        switch (logLevel.toUpperCase()) {
        case "SEVERE":
            level = Level.SEVERE;
            break;
            
        case "INFO":
            level = Level.INFO;
            break;
            
        case "CONFIG":
            level = Level.CONFIG;
            break;
            
        case "FINE":
            level = Level.FINE;
            break;
        
        case "FINER":
            level = Level.FINER;
            break;
        
        case "FINEST":
            level = Level.FINEST;
            break;
        
        case "WARNING":
            level = Level.WARNING;
            break;
        
        case "ALL":
            level = Level.ALL;
            break;
        
        default:
            break;
        }
        
        return level;
    }
}
