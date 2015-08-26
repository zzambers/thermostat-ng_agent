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

package com.redhat.thermostat.setup.command.internal.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Properties;

import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.shared.config.CommonPaths;

public class ThermostatSetup implements PersistableSetup {

    static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss zzz");
    static final String PROGRAM_NAME = "thermostat setup";
    private static final String THERMOSTAT_AGENT_REC_ROLE_NAME = "thermostat-agent";
    private static final String THERMOSTAT_CLIENT_REC_ROLE_NAME = "thermostat-client";
    private static final String THERMOSTAT_GRANT_CMD_CHANNEL_ALL_REC_ROLE_NAME = "thermostat-cmdc";
    private static final String THERMOSTAT_ADMIN_READ_ALL_REC_ROLE_NAME = "thermostat-admin-read-all";
    private final ThermostatUserSetup userSetup;
    private final MongodbUserSetup mongodbUserSetup;
    private final StructureInformation structureInfo;
    private final CredentialsFileCreator creator;
    private final CommonPaths paths;
    private String agentUserName;
    private char[] agentPassword;
    
    ThermostatSetup(ThermostatUserSetup userSetup, MongodbUserSetup mongodbUserSetup, StructureInformation structureInfo, CommonPaths paths, CredentialsFileCreator creator) {
        this.mongodbUserSetup = mongodbUserSetup;
        this.userSetup = userSetup;
        this.structureInfo = structureInfo;
        this.paths = paths;
        this.creator = creator;
    }

    public void createMongodbUser(String username, char[] password) {
        mongodbUserSetup.createUser(username, password, "Backing storage connection credentials.");
    }

    public void createClientAdminUser(String username, char[] password) {
        userSetup.createUser(username, password, "Thermostat client admin user");
        userSetup.createRecursiveRole(THERMOSTAT_GRANT_CMD_CHANNEL_ALL_REC_ROLE_NAME,
                                      UserRoles.CMD_CHANNEL_GRANT_ALL_ACTIONS,
                                      "Recursive role granting all CMD-channel actions.");
        userSetup.createRecursiveRole(THERMOSTAT_CLIENT_REC_ROLE_NAME,
                                      UserRoles.CLIENT_ROLES,
                                      "Recursive role for Thermostat client users.");
        userSetup.createRecursiveRole(THERMOSTAT_ADMIN_READ_ALL_REC_ROLE_NAME,
                                      UserRoles.ADMIN_READALL,
                                      "Recursive role allowing a user to read all records.");
        userSetup.assignRolesToUser(username, new String[] {
                                                THERMOSTAT_CLIENT_REC_ROLE_NAME,
                                                THERMOSTAT_GRANT_CMD_CHANNEL_ALL_REC_ROLE_NAME,
                                                THERMOSTAT_ADMIN_READ_ALL_REC_ROLE_NAME,
                                                UserRoles.PURGE // Client needs purge for clean-data cmd.
                                              },
                                    "Client admin user username => role assignment."
        );
    }

    public void createAgentUser(String username, char[] password) {
        userSetup.createUser(username, password, "Thermostat agent user");
        userSetup.createRecursiveRole(THERMOSTAT_AGENT_REC_ROLE_NAME,
                                      UserRoles.AGENT_ROLES,
                                      "Recursive role for Thermostat agent users.");
        userSetup.assignRolesToUser(username,
                                    new String[] {
                                        THERMOSTAT_AGENT_REC_ROLE_NAME,
                                        UserRoles.GRANT_FILES_WRITE_ALL // Agent needs perm to write files.
                                    },
                                    "Agent user username => role assignment.");
        // Hold on to creds for persistency later.
        agentUserName = username;
        agentPassword = password;
    }

    @Override
    public void commit() throws IOException {
        // FIXME: report errors
        mongodbUserSetup.commit();
        userSetup.commit();
        writeAgentAuthFile();
    }
    
    private void writeAgentAuthFile() throws IOException {
        Properties credentialProps = new Properties();
        credentialProps.setProperty("username", agentUserName);
        credentialProps.setProperty("password", String.valueOf(agentPassword));
        File credentialsFile = paths.getUserAgentAuthConfigFile();
        creator.create(credentialsFile);
        try (FileOutputStream fout = new FileOutputStream(credentialsFile)) {
            credentialProps.store(fout, "Credentials used for 'thermostat agent' connections.");
        }
    }

    public boolean isWebAppInstalled() {
        return structureInfo.isWebAppInstalled();
    }
    
    public static ThermostatSetup create(Launcher launcher, CommonPaths paths, Console console) {
        CredentialFinder finder = new CredentialFinder(paths);
        CredentialsFileCreator creator = new CredentialsFileCreator();
        StampFiles stampFiles = new StampFiles(paths);
        StructureInformation info = new StructureInformation(paths);
        MongodbUserSetup mongoSetup = new MongodbUserSetup(new UserCredsValidator(), launcher, finder, creator , console, paths, stampFiles, info);
        ThermostatUserSetup userSetup = new ThermostatUserSetup(new UserPropertiesFinder(finder), new UserCredsValidator(), creator, stampFiles);
        return new ThermostatSetup(userSetup, mongoSetup, info, paths, creator);
    }
}
