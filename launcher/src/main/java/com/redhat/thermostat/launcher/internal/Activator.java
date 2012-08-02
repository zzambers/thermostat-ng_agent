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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.bundles.OSGiRegistryService;
import com.redhat.thermostat.common.CommandLoadingBundleActivator;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.cli.CommandContextFactory;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.utils.keyring.Keyring;

public class Activator extends CommandLoadingBundleActivator {

    class RegisterLauncherAction implements Action {

        private BundleContext context;

        RegisterLauncherAction(BundleContext context) {
            this.context = context;
        }
        @Override
        public void doIt() {
            ServiceReference reference = context.getServiceReference(OSGiRegistryService.class);
            OSGiRegistryService bundleService = (OSGiRegistryService) context.getService(reference);
            LauncherImpl launcher = new LauncherImpl(context,
                    new CommandContextFactory(context), bundleService);
            launcherServiceRegistration = context.registerService(Launcher.class.getName(), launcher, null);
        }
        
    }

    @SuppressWarnings("rawtypes")
    private ServiceRegistration launcherServiceRegistration;
    @SuppressWarnings("rawtypes")
    private MultipleServiceTracker tracker;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        tracker = new MultipleServiceTracker(context, new Class[] {OSGiRegistryService.class, Keyring.class}, new RegisterLauncherAction(context));
        tracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        if (launcherServiceRegistration != null) {
            launcherServiceRegistration.unregister();
        }
        if (tracker != null) {
            tracker.close();
        }
    }
}
