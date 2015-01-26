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

package com.redhat.thermostat.client.cli.internal;

import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.cli.CommandRegistry;
import com.redhat.thermostat.common.cli.CommandRegistryImpl;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.common.config.experimental.ConfigurationInfoSource;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.storage.core.DbService;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.BackendInfoDAO;
import com.redhat.thermostat.utils.keyring.Keyring;

public class Activator implements BundleActivator {

    private CommandRegistry reg = null;
    private MultipleServiceTracker connectTracker;

    private MultipleServiceTracker listAgentTracker;
    private final ListAgentsCommand listAgentsCommand = new ListAgentsCommand();

    private MultipleServiceTracker agentInfoTracker;
    private final AgentInfoCommand agentInfoCommand = new AgentInfoCommand();

    private MultipleServiceTracker shellTracker;
    private ServiceTracker dbServiceTracker;
    private ShellCommand shellCommand;

    @Override
    public void start(final BundleContext context) throws Exception {
        reg = new CommandRegistryImpl(context);

        reg.registerCommand("list-vms", new ListVMsCommand());
        reg.registerCommand("vm-info", new VMInfoCommand());
        reg.registerCommand("vm-stat", new VMStatCommand());
        reg.registerCommand("disconnect", new DisconnectCommand());
        reg.registerCommand("clean-data", new CleanDataCommand(context));

        Class<?>[] classes = new Class[] {
            Keyring.class,
            CommonPaths.class,
        };
        connectTracker = new MultipleServiceTracker(context, classes, new Action() {

            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                Keyring keyring = (Keyring) services.get(Keyring.class.getName());
                CommonPaths paths = (CommonPaths) services.get(CommonPaths.class.getName());
                ClientPreferences prefs = new ClientPreferences(paths);
                reg.registerCommand("connect", new ConnectCommand(prefs, keyring));
            }

            @Override
            public void dependenciesUnavailable() {
                reg.unregisterCommand("connect");
            }
            
        });
        connectTracker.open();

        Class<?>[] listAgentClasses = new Class[] {
                AgentInfoDAO.class,
        };
        listAgentTracker = new MultipleServiceTracker(context, listAgentClasses, new Action() {
            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                AgentInfoDAO agentInfoDAO = (AgentInfoDAO) services.get(AgentInfoDAO.class.getName());
                listAgentsCommand.setAgentInfoDAO(agentInfoDAO);
            }

            @Override
            public void dependenciesUnavailable() {
                listAgentsCommand.setAgentInfoDAO(null);
            }
        });
        listAgentTracker.open();

        reg.registerCommand("list-agents", listAgentsCommand);

        Class<?>[] agentInfoClasses = new Class[] {
                AgentInfoDAO.class,
                BackendInfoDAO.class,
        };
        agentInfoTracker = new MultipleServiceTracker(context, agentInfoClasses, new Action() {
            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                AgentInfoDAO agentInfoDAO = (AgentInfoDAO) services.get(AgentInfoDAO.class.getName());
                BackendInfoDAO backendInfoDAO = (BackendInfoDAO) services.get(BackendInfoDAO.class.getName());

                agentInfoCommand.setServices(agentInfoDAO, backendInfoDAO);
            }

            @Override
            public void dependenciesUnavailable() {
                agentInfoCommand.setServices(null, null);
            }
        });
        agentInfoTracker.open();

        reg.registerCommand("agent-info", agentInfoCommand);

        Class<?>[] shellClasses = new Class[] {
                CommonPaths.class,
                ConfigurationInfoSource.class,
        };

        shellTracker = new MultipleServiceTracker(context, shellClasses, new Action() {
            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                CommonPaths paths = (CommonPaths) services.get(CommonPaths.class.getName());
                ConfigurationInfoSource config = (ConfigurationInfoSource) services.get(ConfigurationInfoSource.class.getName());
                shellCommand = new ShellCommand(context, paths, config);
                reg.registerCommand("shell", shellCommand);
            }

            @Override
            public void dependenciesUnavailable() {
                reg.unregisterCommand("shell");
            }
        });
        shellTracker.open();

        dbServiceTracker = new ServiceTracker(context, DbService.class.getName(), new ServiceTrackerCustomizer() {
            @Override
            public Object addingService(ServiceReference serviceReference) {
                DbService dbService = (DbService) context.getService(serviceReference);
                shellCommand.dbServiceAvailable(dbService);
                return dbService;
            }

            @Override
            public void modifiedService(ServiceReference serviceReference, Object o) {
                //Do nothing
            }

            @Override
            public void removedService(ServiceReference serviceReference, Object o) {
                shellCommand.dbServiceUnavailable();
            }
        });
        dbServiceTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        connectTracker.close();
        listAgentTracker.close();
        agentInfoTracker.close();
        shellTracker.close();
        dbServiceTracker.close();
        reg.unregisterCommands();
    }

}

