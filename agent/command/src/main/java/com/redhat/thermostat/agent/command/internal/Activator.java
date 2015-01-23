/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.agent.command.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.redhat.thermostat.agent.command.ConfigurationServer;
import com.redhat.thermostat.agent.command.ReceiverRegistry;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.SSLConfiguration;

public class Activator implements BundleActivator {

    private static final Logger logger = LoggingUtils.getLogger(Activator.class);

    @SuppressWarnings("rawtypes")
    private ServiceRegistration confServerRegistration;
    private ReceiverRegistry receivers;
    @SuppressWarnings("rawtypes")
    private ServiceTracker sslConfigTracker;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void start(final BundleContext context) throws Exception {
        logger.log(Level.INFO, "activating thermostat-agent-confserver");
        receivers = new ReceiverRegistry(context);
        receivers.registerReceiver(new PingReceiver());
        sslConfigTracker = new ServiceTracker(context, SSLConfiguration.class, new ServiceTrackerCustomizer() {

            @Override
            public Object addingService(ServiceReference reference) {
                SSLConfiguration sslConf = (SSLConfiguration) context.getService(reference);
                ConfigurationServerImpl confServer = new ConfigurationServerImpl(new ConfigurationServerContext(context, sslConf));
                confServerRegistration = context.registerService(ConfigurationServer.class.getName(), confServer, null);
                return confServer;
            }

            @Override
            public void modifiedService(ServiceReference reference,
                    Object service) {
                // Do nothing
            }

            @Override
            public void removedService(ServiceReference reference,
                    Object service) {
                confServerRegistration.unregister();
                confServerRegistration = null;
                context.ungetService(reference);
            }});
        sslConfigTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        sslConfigTracker.close();
        if (confServerRegistration != null) {
            confServerRegistration.unregister();
        }
    }

}

