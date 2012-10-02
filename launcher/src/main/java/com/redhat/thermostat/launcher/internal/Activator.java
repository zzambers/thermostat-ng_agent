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

package com.redhat.thermostat.launcher.internal;

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.bundles.OSGiRegistry;
import com.redhat.thermostat.common.CommandLoadingBundleActivator;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.cli.CommandContextFactory;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.utils.keyring.Keyring;

public class Activator extends CommandLoadingBundleActivator {

    class RegisterLauncherAction implements Action {

        private BundleContext context;
        private ServiceReference registryReference;

        RegisterLauncherAction(BundleContext context) {
            this.context = context;
        }

        @Override
        public void dependenciesAvailable(Map<String, Object> services) {
            
            registryReference = context.getServiceReference(OSGiRegistry.class);
            OSGiRegistry bundleService = (OSGiRegistry) context.getService(registryReference);
            CommandInfoSource commands = new CommandInfoSource(bundleService.getConfiguration().getThermostatHome());
            informRegistryAboutCommandDependencies(commands, bundleService);
            LauncherImpl launcher = new LauncherImpl(context,
                    new CommandContextFactory(context), bundleService);
            launcherServiceRegistration = context.registerService(Launcher.class.getName(), launcher, null);
        }

        private void informRegistryAboutCommandDependencies(CommandInfoSource commands, OSGiRegistry bundleService) {
            for (CommandInfoImpl info : commands.getCommandInfos()) {
                bundleService.setCommandBundleDependencies(info.getName(),
                    info.getDependencyResourceNames());
            }
        }

        @Override
        public void dependenciesUnavailable() {
            launcherServiceRegistration.unregister();
            context.ungetService(registryReference);
        }

    }

    @SuppressWarnings("rawtypes")
    private ServiceRegistration launcherServiceRegistration;
    private MultipleServiceTracker tracker;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        tracker = new MultipleServiceTracker(context, new Class[] {OSGiRegistry.class, Keyring.class}, new RegisterLauncherAction(context));
        tracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        if (tracker != null) {
            tracker.close();
        }
    }
}
