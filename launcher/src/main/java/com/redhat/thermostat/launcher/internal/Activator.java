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

package com.redhat.thermostat.launcher.internal;

import java.io.File;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.redhat.thermostat.common.ExitStatus;
import com.redhat.thermostat.common.Launcher;
import com.redhat.thermostat.common.cli.CommandContextFactory;
import com.redhat.thermostat.common.cli.CommandInfoSource;
import com.redhat.thermostat.common.cli.CommandRegistry;
import com.redhat.thermostat.common.cli.CommandRegistryImpl;
import com.redhat.thermostat.common.config.Configuration;
import com.redhat.thermostat.launcher.BundleManager;
import com.redhat.thermostat.utils.keyring.Keyring;

public class Activator implements BundleActivator {
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    class RegisterLauncherCustomizer implements ServiceTrackerCustomizer {

        private ServiceRegistration launcherReg;
        private ServiceRegistration bundleManReg;
        private ServiceRegistration cmdInfoReg;
        private ServiceRegistration exitStatusReg;
        private BundleContext context;
        private BundleManager bundleService;

        RegisterLauncherCustomizer(BundleContext context, BundleManager bundleService) {
            this.context = context;
            this.bundleService = bundleService;
        }

        @Override
        public Object addingService(ServiceReference reference) {
            // keyring is now ready
            Keyring keyring = (Keyring)context.getService(reference);
            Configuration config = bundleService.getConfiguration();

            String commandsDir = config.getConfigurationDir() + File.separator + "commands";
            CommandInfoSource builtInCommandSource =
                    new BuiltInCommandInfoSource(commandsDir, config.getLibRoot());
            CommandInfoSource pluginCommandSource = new PluginCommandInfoSource(
                            config.getLibRoot(), config.getPluginRoot());
            CommandInfoSource commands = new CompoundCommandInfoSource(builtInCommandSource, pluginCommandSource);


            cmdInfoReg = context.registerService(CommandInfoSource.class, commands, null);
            bundleService.setCommandInfoSource(commands);
            // Register Launcher service since FrameworkProvider is waiting for it blockingly.
            LauncherImpl launcher = new LauncherImpl(context,
                    new CommandContextFactory(context), bundleService);
            launcherReg = context.registerService(Launcher.class.getName(), launcher, null);
            bundleManReg = context.registerService(BundleManager.class, bundleService, null);
            ExitStatus exitStatus = new ExitStatusImpl(ExitStatus.EXIT_SUCCESS);
            exitStatusReg = context.registerService(ExitStatus.class, exitStatus, null);
            return keyring;
        }

        @Override
        public void modifiedService(ServiceReference reference, Object service) {
            // nothing
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            // Keyring is gone, remove launcher, et. al. as well
            launcherReg.unregister();
            bundleManReg.unregister();
            cmdInfoReg.unregister();
            exitStatusReg.unregister();
        }

    }

    @SuppressWarnings("rawtypes")
    private ServiceTracker serviceTracker;

    private CommandRegistry registry;

    private ServiceTracker commandInfoSourceTracker;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void start(final BundleContext context) throws Exception {
        BundleManager bundleService = new BundleManagerImpl(new Configuration());
        ServiceTrackerCustomizer customizer = new RegisterLauncherCustomizer(context, bundleService);
        serviceTracker = new ServiceTracker(context, Keyring.class, customizer);
        // Track for Keyring service.
        serviceTracker.open();

        final HelpCommand helpCommand = new HelpCommand();

        commandInfoSourceTracker = new ServiceTracker(context, CommandInfoSource.class, null) {
            @Override
            public Object addingService(ServiceReference reference) {
                CommandInfoSource infoSource = (CommandInfoSource) super.addingService(reference);
                helpCommand.setCommandInfoSource(infoSource);
                return infoSource;
            }

            @Override
            public void removedService(ServiceReference reference, Object service) {
                helpCommand.setCommandInfoSource(null);
                super.removedService(reference, service);
            }
        };
        commandInfoSourceTracker.open();

        registry = new CommandRegistryImpl(context);
        registry.registerCommand(helpCommand);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (serviceTracker != null) {
            serviceTracker.close();
        }
        commandInfoSourceTracker.close();
        registry.unregisterCommands();
    }
}

