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

package com.redhat.thermostat.setup.command.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.cli.AbstractStateNotifyingCommand;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.common.tools.ApplicationState;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.setup.command.locale.LocaleResources;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.locale.Translate;


public class ThermostatSetupImpl implements ThermostatSetup {
    private static final String WEB_AUTH_FILE = "web.auth";
    private static final String USERS_PROPERTIES = "thermostat-users.properties";
    private static final String ROLES_PROPERTIES = "thermostat-roles.properties";
    private static final String MONGO_INPUT_SCRIPT = "/tmp/mongo-input.js";
    private static final String THERMOSTAT_AGENT = "thermostat-agent";
    private static final String THERMOSTAT_CLIENT = "thermostat-client";
    private static final String THERMOSTAT_CMDC = "thermostat-cmdc";
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final String[] STORAGE_START_ARGS = {"storage", "--start", "--permitLocalhostException"};
    private static final String[] STORAGE_STOP_ARGS = {"storage", "--stop"};
    private static final Set<PosixFilePermission> credFilePermissions = EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE
    );

    private static boolean storageFailed = false;
    private List<ActionListener<ApplicationState>> listeners;
    private String setupTmpUnlockContent;
    private String webApp;
    private String setupUnlockContentRegular;
    private String userDoneFile;
    private String createUserScript;
    private CredentialFinder finder;
    private File setupCompleteFile;
    private File agentAuthFile;
    private Launcher launcher;
    private Properties roleProps;

    public ThermostatSetupImpl(Launcher launcher, CommonPaths paths, Console console) {
        this(launcher, paths, console, new CredentialFinder(paths));
    }

    //package-private constructor for testing
    ThermostatSetupImpl(Launcher launcher, CommonPaths paths, Console console, CredentialFinder finder) {
        this.launcher = launcher;
        this.finder = finder;
        listeners = new ArrayList<>();
        listeners.add(new StorageListener(console));
        setThermostatVars(paths);
    }

    private void setThermostatVars(CommonPaths paths) {
        //set thermostat environment
        createUserScript = paths.getSystemThermostatHome().toString() + "/lib/create-user.js";
        userDoneFile = paths.getUserThermostatHome().toString() + "/data/mongodb-user-done.stamp";
        webApp = paths.getSystemThermostatHome() + "/webapp";
        String setupCompletePath = paths.getUserThermostatHome().toString() + "/data/setup-complete.stamp";
        agentAuthFile = paths.getUserAgentAuthConfigFile();
        setupCompleteFile = new File(setupCompletePath);

        //set stamp complete vars
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss zzz");
        Date date = new Date();
        String timestamp = dateFormat.format(date);
        String programName = "Thermostat Setup";
        setupTmpUnlockContent = "Temporarily unlocked thermostat via " + programName + " on " + timestamp;
        setupUnlockContentRegular = "Created by " + programName + " on " + timestamp;
    }

    //package-private for testing
    void createCredentialFile(File file) throws IOException {
        if (!file.exists()) {
            //create file and set file permissions to 600
            Files.createFile(file.toPath(), PosixFilePermissions.asFileAttribute(credFilePermissions));
        }
    }

    @Override
    public void createMongodbUser(String username, char[] password) throws MongodbUserSetupException {
        try {
            unlockThermostat();

            startStorage();

            byte[] encoded = Files.readAllBytes(Paths.get(createUserScript));
            String mongoInput = new String(encoded);
            mongoInput = mongoInput.replaceAll("\\$USERNAME", username);
            mongoInput = mongoInput.replaceAll("\\$PASSWORD", String.valueOf(password));

            File mongoInputFile = new File(MONGO_INPUT_SCRIPT);
            mongoInputFile.deleteOnExit();
            Files.write(mongoInputFile.toPath(), mongoInput.getBytes());

            int mongoRetVal = runMongo();
            if (mongoRetVal != 0) {
                throw new MongodbUserSetupException("Mongodb user setup failed");
            }

            stopStorage();

            if (isWebAppInstalled()) {
                writeStorageCredentialsFile(username, password);
            }

            File userDoneFile = new File(this.userDoneFile);
            userDoneFile.createNewFile();

            Files.write(setupCompleteFile.toPath(), setupUnlockContentRegular.getBytes());
        } catch (IOException | InterruptedException e) {
            removeTempStampFile();
            throw new MongodbUserSetupException("Error creating Mongodb user", e);
        }
    }

    //package-private for testing
    void unlockThermostat() throws IOException {
        Files.write(setupCompleteFile.toPath(), setupTmpUnlockContent.getBytes());
    }

    private void startStorage() throws MongodbUserSetupException {
        launcher.run(STORAGE_START_ARGS, listeners, false);

        if (storageFailed) {
            throw new MongodbUserSetupException("Thermostat storage failed to start");
        }
    }

    private void stopStorage() throws MongodbUserSetupException {
        launcher.run(STORAGE_STOP_ARGS, listeners, false);

        if (storageFailed) {
            throw new MongodbUserSetupException("Thermostat storage failed to stop");
        }
    }

    //package-private for testing
    int runMongo() throws IOException, InterruptedException {
        ProcessBuilder mongoProcess = new ProcessBuilder("mongo", "127.0.0.1:27518/thermostat", MONGO_INPUT_SCRIPT);
        return mongoProcess.start().waitFor();
    }

    private void writeStorageCredentialsFile(String username, char[] password) throws MongodbUserSetupException {
        try {
            Properties credentialProps = new Properties();
            File credentialsFile = finder.getConfiguration(WEB_AUTH_FILE);
            createCredentialFile(credentialsFile);
            credentialProps.load(new FileInputStream(credentialsFile));
            if (credentialProps.getProperty("username") == null) {
                credentialProps = new Properties();
                credentialProps.setProperty("username", username);
                credentialProps.setProperty("password", String.valueOf(password));
                credentialProps.store(new FileOutputStream(credentialsFile), "Storage Credentials");
            }
        } catch (IOException e) {
            throw new MongodbUserSetupException("Storing credentials to file " + WEB_AUTH_FILE + " failed!", e);
        }
    }

    private void removeTempStampFile() {
        if (setupCompleteFile.exists()) {
            setupCompleteFile.delete();
        }
    }

    @Override
    public void createThermostatUser(String username, char[] password, String[] roles) throws IOException {
        List<String> rolesList = Arrays.asList(roles);

        if (rolesList.containsAll(Arrays.asList(UserRoles.CLIENT_ROLES))) {
            createClientUser(username, password, roles);
        } else if (rolesList.containsAll(Arrays.asList(UserRoles.AGENT_ROLES))) {
            createAgentUser(username, password, roles);
        }
    }

    private void createClientUser(String username, char[] password, String[] roles) throws IOException {
        Properties userProps = new Properties();
        File userPropsFile = finder.getConfiguration(USERS_PROPERTIES);
        createCredentialFile(userPropsFile);
        userProps.load(new FileInputStream(userPropsFile));
        if (userProps.getProperty(username) == null) {
            userProps = new Properties();
            userProps.setProperty(username, String.valueOf(password));
            userProps.store(new FileOutputStream(userPropsFile, true), "Client User");
        }

        setClientRoles(username, roles);
    }

    private void setClientRoles(String username, String[] clientRoles) throws IOException {
        String[] clientUserRoles = new String[] {
                THERMOSTAT_CLIENT,
                THERMOSTAT_CMDC,
                UserRoles.PURGE
        };

        String[] cmdcRoles = new String[] {
                UserRoles.GRANT_CMD_CHANNEL_GARBAGE_COLLECT,
                UserRoles.GRANT_CMD_CHANNEL_DUMP_HEAP,
                UserRoles.GRANT_CMD_CHANNEL_GRANT_THREAD_HARVESTER,
                UserRoles.GRANT_CMD_CHANNEL_KILLVM,
                UserRoles.GRANT_CMD_PROFILE_VM,
                UserRoles.GRANT_CMD_CHANNEL_PING,
                UserRoles.GRANT_CMD_CHANNEL_JMX_TOGGLE_NOTIFICATION,
        };

        setRoleProperty(username, clientUserRoles);
        setRoleProperty(THERMOSTAT_CLIENT, clientRoles);
        setRoleProperty(THERMOSTAT_CMDC, cmdcRoles);

        File rolePropsFile = finder.getConfiguration(ROLES_PROPERTIES);
        createCredentialFile(rolePropsFile);
        if (roleProps.size() > 0) {
            FileOutputStream roleStream = new FileOutputStream(rolePropsFile, true);
            roleProps.store(new PropertiesWriter(roleStream), "Thermostat Client Roles");
        }
    }

    private void createAgentUser(String username, char[] password, String[] roles) throws IOException {
        Properties userProps = new Properties();
        File userPropsFile = finder.getConfiguration(USERS_PROPERTIES);
        createCredentialFile(userPropsFile);
        userProps.load(new FileInputStream(userPropsFile));
        if (userProps.getProperty(username) == null) {
            userProps = new Properties();
            userProps.setProperty(username, String.valueOf(password));
            userProps.store(new FileOutputStream(userPropsFile, true), "Agent User");
        }

        //set agent credentials
        Properties agentProps = new Properties();
        createCredentialFile(agentAuthFile);
        agentProps.load(new FileInputStream(agentAuthFile));
        if (userProps.getProperty("username") == null) {
            agentProps = new Properties();
            agentProps.setProperty("username", username);
            agentProps.setProperty("password", String.valueOf(password));
            agentProps.store(new FileOutputStream(agentAuthFile), "Agent Credentials");
        }

        setAgentRoles(username, roles);
    }

    private void setAgentRoles(String username, String[] agentRoles) throws IOException {
        String[] agentUserRoles = new String[] {
                THERMOSTAT_AGENT
        };
        setRoleProperty(username, agentUserRoles);
        setRoleProperty(THERMOSTAT_AGENT, agentRoles);
        File rolePropsFile = finder.getConfiguration(ROLES_PROPERTIES);
        createCredentialFile(rolePropsFile);
        if (roleProps.size() > 0) {
            FileOutputStream roleStream = new FileOutputStream(rolePropsFile, true);
            roleProps.store(new PropertiesWriter(roleStream), "Thermostat Agent Roles");
        }
    }

    private void setRoleProperty(String attribute, String[] roles) throws IOException {
        Properties existingRoleProps = new Properties();
        File rolePropsFile = finder.getConfiguration(ROLES_PROPERTIES);
        createCredentialFile(rolePropsFile);
        existingRoleProps.load(new FileInputStream(rolePropsFile));
        if (roleProps == null) {
            roleProps = new Properties();
        }
        if (roles.length > 0 && existingRoleProps.getProperty(attribute) == null) {
            StringBuilder rolesBuilder = new StringBuilder();
            for (int i = 0; i < roles.length - 1; i++) {
                rolesBuilder.append(roles[i] + ", " + System.getProperty("line.separator"));
            }
            rolesBuilder.append(roles[roles.length - 1]);
            roleProps.setProperty(attribute, rolesBuilder.toString());
        }
    }

    @Override
    public boolean isWebAppInstalled() {
        Path webAppPath = Paths.get(webApp);
        if (Files.exists(webAppPath)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * The Properties.store() method doesn't allow for new lines. This
     * class is used so that when a property key has multiple associated
     * values, they are written in a readable manner.
     */
    public static class PropertiesWriter extends PrintWriter {

        private static final int INDENT_AMOUNT = 4;

        public PropertiesWriter(OutputStream out) {
            super(out);
        }

        @Override
        public void write(char[] line, int startIdx, int len) {
            for (int i = startIdx; i < len; i++) {
                // interpret new lines as such
                if (isNewLine(line, i)) {
                    i++; // skip 'n' in \n
                    try {
                        out.write('\\');
                        out.write(System.getProperty("line.separator"));
                        // indent following lines
                        for (int j = 0; j < INDENT_AMOUNT; j++) {
                            out.write(' ');
                        }
                    } catch (IOException e) {
                        e.printStackTrace(System.err);
                    }
                } else {
                    try {
                        out.write(line[i]);
                    } catch (IOException e) {
                        e.printStackTrace(System.err);
                    }
                }
            }
        }

        private boolean isNewLine(char[] line, int j) {
            if (j + 1 > line.length) {
                return false;
            }
            if (line[j] == '\\' && line[j + 1] == 'n') {
                return true;
            }
            return false;
        }
    }

    private static class StorageListener implements ActionListener<ApplicationState> {

        private final Console console;

        private StorageListener(Console console) {
            this.console = console;
        }

        @Override
        public void actionPerformed(ActionEvent<ApplicationState> actionEvent) {
            if (actionEvent.getSource() instanceof AbstractStateNotifyingCommand) {
                AbstractStateNotifyingCommand storage = (AbstractStateNotifyingCommand) actionEvent.getSource();
                // Implementation detail: there is a single Storage instance registered
                // as an OSGi service. We remove ourselves as listener so that we don't get
                // notified in the case that the command is invoked by some other means later.
                storage.getNotifier().removeActionListener(this);

                switch (actionEvent.getActionId()) {
                    case START:
                        storageFailed = false;
                        break;
                    case FAIL:
                        console.getOutput().println(translator.localize(LocaleResources.STORAGE_FAILED).getContents());
                        storageFailed = true;
                        break;
                }
            }
        }
    }
}
