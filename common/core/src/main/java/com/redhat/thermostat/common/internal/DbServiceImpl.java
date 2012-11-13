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

package com.redhat.thermostat.common.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.common.DbService;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.DAOFactoryImpl;
import com.redhat.thermostat.storage.config.StartupConfiguration;
import com.redhat.thermostat.storage.core.ConnectionException;
import com.redhat.thermostat.storage.core.StorageProvider;
import com.redhat.thermostat.storage.core.StorageProviderUtil;

public class DbServiceImpl implements DbService {
    
    @SuppressWarnings("rawtypes")
    private ServiceRegistration registration;
    
    private DAOFactory daoFactory;
    private BundleContext context;
    
    DbServiceImpl(String username, String password, String dbUrl) {
        this(FrameworkUtil.getBundle(DbService.class).getBundleContext(), getDAOFactory(username, password, dbUrl));
    }

    DbServiceImpl(BundleContext context, DAOFactory daoFactory) {
        this.daoFactory = daoFactory;
        this.context = context;
    }

    public void connect() throws ConnectionException {
        daoFactory.getConnection().connect();
        registration = context.registerService(DbService.class, this, null);
        daoFactory.registerDAOsAndStorageAsOSGiServices();
    }
    
    public void disconnect() throws ConnectionException {
        daoFactory.unregisterDAOsAndStorageAsOSGiServices();
        daoFactory.getConnection().disconnect();
        registration.unregister();
    }
    
    /**
     * Factory method for creating a DbService instance.
     * 
     * @param username
     * @param password
     * @param dbUrl
     * @return
     */
    public static DbService create(String username, String password, String dbUrl) {
        return new DbServiceImpl(username, password, dbUrl);
    }

    private static DAOFactory getDAOFactory(String username, String password, String dbUrl) {
        StartupConfiguration config = new ConnectionConfiguration(dbUrl, username, password);
        StorageProvider prov = StorageProviderUtil.getStorageProvider(config);
        return new DAOFactoryImpl(prov);
    }
    
}
