/*
 * Copyright 2013 Red Hat, Inc.
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

package com.redhat.thermostat.vm.memory.agent.internal;

import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.jvmstat.monitor.HostIdentifier;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;

import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.backend.BackendID;
import com.redhat.thermostat.backend.BackendsProperties;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;

public class VmMemoryBackend extends Backend {

    private static final Logger LOGGER = LoggingUtils.getLogger(VmMemoryBackend.class);

    private VmMemoryStatDAO vmMemoryStats;
    private HostIdentifier hostId;
    private MonitoredHost host;
    private VmMemoryHostListener hostListener;
    private boolean started;

    public VmMemoryBackend(VmMemoryStatDAO vmMemoryStatDAO, Version version) {
        super(new BackendID("VM Memory Backend", VmMemoryBackend.class.getName()));
        this.vmMemoryStats = vmMemoryStatDAO;
        
        setConfigurationValue(BackendsProperties.VENDOR.name(), "Red Hat, Inc.");
        setConfigurationValue(BackendsProperties.DESCRIPTION.name(), "Gathers memory statistics about a JVM");
        setConfigurationValue(BackendsProperties.VERSION.name(), version.getVersionNumber());
        
        try {
            hostId = new HostIdentifier((String) null);
            host = MonitoredHost.getMonitoredHost(hostId);
            hostListener = new VmMemoryHostListener(vmMemoryStats, attachToNewProcessByDefault());
        } catch (MonitorException me) {
            LOGGER.log(Level.WARNING, "Problems with connecting jvmstat to local machine", me);
        } catch (URISyntaxException use) {
            LOGGER.log(Level.WARNING, "Failed to create host identifier", use);
        }
    }

    @Override
    public boolean activate() {
        if (!started && host != null) {
            try {
                host.addHostListener(hostListener);
                started = true;
            } catch (MonitorException me) {
                LOGGER.log(Level.WARNING, "Failed to add host listener", me);
            }
        }
        return started;
    }

    @Override
    public boolean deactivate() {
        if (started && host != null) {
            try {
                host.removeHostListener(hostListener);
                started = false;
            } catch (MonitorException me) {
                LOGGER.log(Level.INFO, "Failed to remove host listener");
            }
        }
        return !started;
    }
    
    @Override
    public boolean isActive() {
        return started;
    }
    
    @Override
    protected void setDAOFactoryAction() {
        // No need for DAOFactory
    }

    @Override
    public String getConfigurationValue(String key) {
        return null;
    }

    @Override
    public boolean attachToNewProcessByDefault() {
        return true;
    }

    @Override
    public int getOrderValue() {
        return ORDER_MEMORY_GROUP + 40;
    }

    /*
     * For testing purposes only.
     */
    void setHost(MonitoredHost host) {
        this.host = host;
    }
    
}
