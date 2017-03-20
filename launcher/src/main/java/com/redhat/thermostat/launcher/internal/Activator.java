/*
 * Copyright 2012-2017 Red Hat, Inc.
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

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ExitStatus;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.MultipleServiceTracker.DependencyProvider;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.ThermostatExtensionRegistry;
import com.redhat.thermostat.common.cli.CommandContextFactory;
import com.redhat.thermostat.common.cli.CommandRegistry;
import com.redhat.thermostat.common.cli.CommandRegistryImpl;
import com.redhat.thermostat.common.cli.CompleterService;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.common.config.experimental.ConfigurationInfoSource;
import com.redhat.thermostat.launcher.BundleManager;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.launcher.internal.CurrentEnvironment.CurrentEnvironmentChangeListener;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.storage.core.DbService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.io.File;
import java.io.IOException;

public class Activator implements BundleActivator {

    private CompleterServiceRegistry completerServiceRegistry;

    @SuppressWarnings({ "rawtypes" })
    class RegisterLauncherAction implements Action {

        private ServiceRegistration launcherReg;
        private ServiceRegistration bundleManReg;
        private ServiceRegistration cmdInfoReg;
        private ServiceRegistration exitStatusReg;
        private ServiceRegistration pluginConfReg;
        private ServiceRegistration commandGroupMetaReg;
        private BundleContext context;
        private CurrentEnvironment env;

        RegisterLauncherAction(BundleContext context, CurrentEnvironment env) {
            this.context = context;
            this.env = env;
        }

        @Override
        public void dependenciesAvailable(DependencyProvider services) {
            CommonPaths paths = services.get(CommonPaths.class);
            ClientPreferences prefs = new ClientPreferences(paths);
            SSLConfiguration sslConf = services.get(SSLConfiguration.class);

            String commandsDir = new File(paths.getSystemConfigurationDirectory(), "commands").toString();
            CommandInfoSource builtInCommandSource =
                    new BuiltInCommandInfoSource(commandsDir, paths.getSystemLibRoot().toString());
            PluginInfoSource pluginSource = new PluginInfoSource(
                            paths.getSystemLibRoot().toString(),
                            paths.getSystemPluginRoot().toString(),
                            paths.getUserPluginRoot().toString(),
                            paths.getSystemPluginConfigurationDirectory().toString(),
                            paths.getUserPluginConfigurationDirectory().toString());

            ConfigurationInfoSource configurations = pluginSource;
            pluginConfReg = context.registerService(ConfigurationInfoSource.class, configurations, null);

            commandGroupMetaReg = context.registerService(CommandGroupMetadataSource.class, pluginSource, null);

            CommandInfoSource commands = new CompoundCommandInfoSource(builtInCommandSource, pluginSource);
            cmdInfoReg = context.registerService(CommandInfoSource.class, commands, null);

            BundleManager bundleService = null;
            try {
                bundleService = new BundleManagerImpl(paths);
            } catch (IOException e) {
                throw new RuntimeException("Could not initialize launcher.", e);
            }

            // Register Launcher service since FrameworkProvider is waiting for it blockingly.
            LauncherImpl launcher = new LauncherImpl(context, new CommandContextFactory(context),
                    bundleService, commands, env, prefs, paths, sslConf);
            launcherReg = context.registerService(Launcher.class.getName(), launcher, null);
            bundleManReg = context.registerService(BundleManager.class, bundleService, null);
            ExitStatus exitStatus = new ExitStatusImpl(ExitStatus.EXIT_SUCCESS);
            exitStatusReg = context.registerService(ExitStatus.class, exitStatus, null);
        }

        @Override
        public void dependenciesUnavailable() {
            // Keyring or CommonPaths are gone, remove launcher, et. al. as well
            launcherReg.unregister();
            bundleManReg.unregister();
            cmdInfoReg.unregister();
            exitStatusReg.unregister();
            pluginConfReg.unregister();
            commandGroupMetaReg.unregister();
        }

    }

    private MultipleServiceTracker launcherDepsTracker;
    private MultipleServiceTracker shellTracker;

    private CommandRegistry registry;

    private ShellCommand shellCommand;
    private TabCompletion tabCompletion;
    @SuppressWarnings("rawtypes")
    private MultipleServiceTracker commandInfoSourceTracker;
    private ServiceTracker dbServiceTracker;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void start(final BundleContext context) throws Exception {
        CurrentEnvironment environment = new CurrentEnvironment(Environment.CLI);

        Class[] launcherDeps = new Class[]{
            CommonPaths.class,
            SSLConfiguration.class,
        };
        registry = new CommandRegistryImpl(context);
        RegisterLauncherAction registerLauncherAction = new RegisterLauncherAction(context, environment);
        launcherDepsTracker = new MultipleServiceTracker(context, launcherDeps,
                registerLauncherAction);
        launcherDepsTracker.open();

        tabCompletion = new TabCompletion();

        completerServiceRegistry = new CompleterServiceRegistry(context);
        completerServiceRegistry.addActionListener(new ActionListener<ThermostatExtensionRegistry.Action>() {
            @Override
            public void actionPerformed(ActionEvent<ThermostatExtensionRegistry.Action> actionEvent) {
                CompleterService service = (CompleterService) actionEvent.getPayload();
                switch (actionEvent.getActionId()) {
                    case SERVICE_ADDED:
                        tabCompletion.addCompleterService(service);
                        break;
                    case SERVICE_REMOVED:
                        tabCompletion.removeCompleterService(service);
                        break;
                    default:
                        throw new NotImplementedException("Unknown action: " + actionEvent.getActionId());
                }
            }
        });

        final HelpCommand helpCommand = new HelpCommand();
        environment.addListener(new CurrentEnvironmentChangeListener() {
            @Override
            public void enviornmentChanged(Environment oldEnv, Environment newEnv) {
                helpCommand.setEnvironment(newEnv);
            }
        });
        helpCommand.setEnvironment(environment.getCurrent());

        final Class<?>[] shellClasses = new Class[] {
                CommonPaths.class,
                ConfigurationInfoSource.class,
        };
        shellTracker = new MultipleServiceTracker(context, shellClasses, new Action() {
            @Override
            public void dependenciesAvailable(DependencyProvider services) {
                CommonPaths paths = services.get(CommonPaths.class);
                ConfigurationInfoSource config = services.get(ConfigurationInfoSource.class);
                shellCommand = new ShellCommand(context, paths, config);
                shellCommand.setTabCompletion(tabCompletion);
                shellCommand.setCompleterServiceRegistry(completerServiceRegistry);
                registry.registerCommand("shell", shellCommand);
            }

            @Override
            public void dependenciesUnavailable() {
                registry.unregisterCommand("shell");
            }
        });
        shellTracker.open();

        final HelpCommandCompleterService helpCommandCompleterService = new HelpCommandCompleterService();
        final Class<?>[] helpCommandClasses = new Class<?>[] {
                CommandInfoSource.class,
                CommandGroupMetadataSource.class
        };
        commandInfoSourceTracker = new MultipleServiceTracker(context, helpCommandClasses, new Action() {
            @Override
            public void dependenciesAvailable(DependencyProvider services) {
                CommandInfoSource infoSource = services.get(CommandInfoSource.class);
                helpCommand.setCommandInfoSource(infoSource);
                helpCommandCompleterService.bindCommandInfoSource(infoSource);
                if (shellCommand != null) {
                    shellCommand.setCommandInfoSource(infoSource);
                }

                CommandGroupMetadataSource commandGroupMetadataSource = services.get(CommandGroupMetadataSource.class);
                helpCommand.setCommandGroupMetadataSource(commandGroupMetadataSource);
            }

            @Override
            public void dependenciesUnavailable() {
                helpCommand.setCommandInfoSource(null);
                helpCommand.setCommandGroupMetadataSource(null);
                helpCommandCompleterService.unbindCommandInfoSource();
                if (shellCommand != null) {
                    shellCommand.setCommandInfoSource(null);
                }
            }
        });
        commandInfoSourceTracker.open();

        dbServiceTracker = new ServiceTracker(context, DbService.class.getName(), new ServiceTrackerCustomizer() {
            @Override
            public Object addingService(ServiceReference serviceReference) {
                if (shellCommand != null) {
                    DbService dbService = (DbService) context.getService(serviceReference);
                    shellCommand.dbServiceAvailable(dbService);
                }
                return context.getService(serviceReference);
            }

            @Override
            public void modifiedService(ServiceReference serviceReference, Object o) {
                //Do nothing
            }

            @Override
            public void removedService(ServiceReference serviceReference, Object o) {
                if (shellCommand != null) {
                    shellCommand.dbServiceUnavailable();
                }
            }
        });
        dbServiceTracker.open();
        registry.registerCommand("help", helpCommand);

        context.registerService(CompleterService.class.getName(), helpCommandCompleterService, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (launcherDepsTracker != null) {
            launcherDepsTracker.close();
        }
        if (commandInfoSourceTracker != null) {
            commandInfoSourceTracker.close();
        }
        if (completerServiceRegistry != null) {
            completerServiceRegistry.stop();
        }
        if (shellTracker != null) {
            shellTracker.close();
        }
        registry.unregisterCommands();
    }
}

