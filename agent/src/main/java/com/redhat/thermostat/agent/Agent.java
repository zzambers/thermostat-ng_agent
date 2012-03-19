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

package com.redhat.thermostat.agent;

import java.util.UUID;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.config.ConfigurationWatcher;
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.backend.BackendRegistry;
import com.redhat.thermostat.common.LaunchException;
import com.redhat.thermostat.common.config.StartupConfiguration;
import com.redhat.thermostat.common.storage.AgentInformation;
import com.redhat.thermostat.common.storage.BackendInformation;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 * Represents the Agent running on a host.
 */
public class Agent {

    private static final Logger logger = LoggingUtils.getLogger(Agent.class);

    private final UUID id;
    private final BackendRegistry backendRegistry;
    private final StartupConfiguration config;

    private Storage storage;
    private Thread configWatcherThread = null;

    public Agent(BackendRegistry backendRegistry, StartupConfiguration config, Storage storage) {
        this(backendRegistry, UUID.randomUUID(), config, storage);
    }

    public Agent(BackendRegistry registry, UUID agentId, StartupConfiguration config, Storage storage) {
        this.id = agentId;
        this.backendRegistry = registry;
        this.config = config;
        this.storage = storage;
    }

    private void startBackends() throws LaunchException {
        for (Backend be : backendRegistry.getAll()) {
            logger.fine("Attempting to start backend: " + be.getName());
            if (!be.activate()) {
                logger.warning("Issue while starting backend: " + be.getName());
                // When encountering issues during startup, we should not attempt to continue activating.
                stopBackends();
                throw new LaunchException("Could not activate backend: " + be.getName());
            }
        }
    }

    private void stopBackends() {
        for (Backend be : backendRegistry.getAll()) {
            logger.fine("Attempting to stop backend: " +be.getName());
            if (!be.deactivate()) {
                // When encountering issues during shutdown, we should attempt to shut down remaining backends.
                logger.warning("Issue while deactivating backend: " + be.getName());
            }
        }
    }

    public synchronized void start() throws LaunchException {
        if (configWatcherThread == null) {
            startBackends();
            storage.addAgentInformation(createAgentInformation());
            configWatcherThread = new Thread(new ConfigurationWatcher(storage, backendRegistry), "Configuration Watcher");
            configWatcherThread.start();
        } else {
            logger.warning("Attempt to start agent when already started.");
        }
    }

    private AgentInformation createAgentInformation() {
        AgentInformation agentInfo = new AgentInformation();
        agentInfo.setStartTime(config.getStartTime());
        for (Backend backend : backendRegistry.getAll()) {
            BackendInformation backendInfo = new BackendInformation();
            backendInfo.setName(backend.getName());
            backendInfo.setDescription(backend.getDescription());
            backendInfo.setObserveNewJvm(backend.getObserveNewJvm());
            agentInfo.addBackend(backendInfo);
        }
        return agentInfo;
    }

    public synchronized void stop() {
        if (configWatcherThread != null) {
            configWatcherThread.interrupt(); // This thread checks for its own interrupted state and ends if interrupted.
            while (configWatcherThread.isAlive()) {
                try {
                    configWatcherThread.join();
                } catch (InterruptedException e) {
                    logger.fine("Interrupted while waiting for ConfigurationWatcher to die.");
                }
            }
            configWatcherThread = null;
            storage.removeAgentInformation();
            stopBackends();
            if (config.getLocalMode()) {
                storage.purge();
            }
        } else {
            logger.warning("Attempt to stop agent which is not active");
        }
    }

    public UUID getId() {
        return id;
    }

}
