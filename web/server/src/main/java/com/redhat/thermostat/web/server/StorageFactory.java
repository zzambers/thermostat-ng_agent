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


package com.redhat.thermostat.web.server;

import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.shared.config.internal.SSLConfigurationImpl;
import com.redhat.thermostat.storage.config.ConnectionConfiguration;
import com.redhat.thermostat.storage.config.StartupConfiguration;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageProvider;
import com.redhat.thermostat.storage.mongodb.MongoStorageProvider;

class StorageFactory {

    private static Storage storage;

    // Web server is not OSGi, this factory method is workaround.
    static Storage getStorage(String storageClass, final String storageEndpoint, final String username,
                final char[] password, final CommonPaths paths) {
        if (storage != null) {
            return storage;
        }
        StartupConfiguration conf = new ConnectionConfiguration(storageEndpoint, username, password);
        SSLConfiguration sslConf = new SSLConfigurationImpl(paths);
        try {
            StorageProvider provider = (StorageProvider) Class.forName(storageClass).newInstance();
            provider.setConfig(conf, sslConf);
            storage = provider.createStorage();
            storage.getConnection().connect();
            return storage;
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            // This fallback should infact not be used. But it gives us an automatic
            // Import-Package in the OSGi descriptor, which actually *prevents* this same
            // exception from happening (a recursive self-defeating catch-block) :-)
            System.err.println("could not instantiate provider: " + storageClass + ", falling back to MongoStorage");
            e.printStackTrace();
            StorageProvider provider = new MongoStorageProvider();
            provider.setConfig(conf, sslConf);
            storage = provider.createStorage();
            return storage;
        }
    }

    // Testing hook used in WebStorageEndpointTest
    static void setStorage(Storage storage) {
        StorageFactory.storage = storage;
    }
}

