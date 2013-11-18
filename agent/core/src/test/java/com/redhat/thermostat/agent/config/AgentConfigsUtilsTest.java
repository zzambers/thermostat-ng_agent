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

package com.redhat.thermostat.agent.config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.testutils.TestUtils;

public class AgentConfigsUtilsTest {

    private static Random random;

    @BeforeClass
    public static void setUpOnce() {
        random = new Random();

        Properties agentProperties = new Properties();
        agentProperties.setProperty("SAVE_ON_EXIT", "true");
        agentProperties.setProperty("CONFIG_LISTEN_ADDRESS", "42.42.42.42:42");

        try {
            TestUtils.setupAgentConfigs(agentProperties);
            File agentConf = TestUtils.getAgentConfFile();
            File agentAuth = TestUtils.getAgentAuthFile();
            // By default system config == user config
            AgentConfigsUtils.setConfigFiles(agentConf, agentConf, agentAuth);
        } catch (IOException e) {
            throw new AssertionError("Unable to create agent configuration", e);
        }
    }
    
    @Test
    public void testCreateAgentConfigs() throws InvalidConfigurationException, IOException {
        AgentStartupConfiguration config = AgentConfigsUtils.createAgentConfigs();        

        Assert.assertFalse(config.purge());
        Assert.assertEquals("42.42.42.42:42", config.getConfigListenAddress());
        Assert.assertEquals("user", config.getUsername());
        Assert.assertEquals("pass", config.getPassword());
    }

    @Test
    public void testAuthConfigFromFile() throws IOException {
        File tmpAuth = createTempAuthFile("username=user\npassword=pass\n");
        AgentStartupConfiguration config = new AgentStartupConfiguration();
        AgentConfigsUtils.setAuthConfigFromFile(tmpAuth, config);
        Assert.assertEquals("user", config.getUsername());
        Assert.assertEquals("pass", config.getPassword());
    }

    @Test
    public void testAuthConfigFromEmptyFile() throws IOException {
        File tmpAuth = createTempAuthFile("");
        AgentStartupConfiguration config = new AgentStartupConfiguration();
        AgentConfigsUtils.setAuthConfigFromFile(tmpAuth, config);
        Assert.assertEquals("", config.getUsername());
        Assert.assertEquals("", config.getPassword());
    }

    // TODO add test to ensure user agent config overrides system agent config.

    @Test
    public void testAuthConfigWithConfigCommentedOut() throws IOException {
        File tmpAuth = createTempAuthFile("#username=user\n#password=pass\n");
        AgentStartupConfiguration config = new AgentStartupConfiguration();
        AgentConfigsUtils.setAuthConfigFromFile(tmpAuth, config);
        Assert.assertEquals("", config.getUsername());
        Assert.assertEquals("", config.getPassword());
    }

    private File createTempAuthFile(String contents) throws IOException {
        String tmpAuthLoc = System.getProperty("java.io.tmpdir") + File.separatorChar +
                Math.abs(random.nextInt());
        File tmpAuth = new File(tmpAuthLoc);
        tmpAuth.deleteOnExit();
        tmpAuth.createNewFile();
        FileWriter authWriter = new FileWriter(tmpAuth);
        authWriter.append(contents);
        authWriter.flush();
        authWriter.close();
        return tmpAuth;
    }

    @Test
    public void testParseAuthConfigData() {
        char[] authData = "username=user\npassword=pass\n".toCharArray();
        AgentStartupConfiguration config = new AgentStartupConfiguration();
        AgentConfigsUtils.parseAuthConfigFromData(authData, authData.length, config);
        Assert.assertEquals("user", config.getUsername());
        Assert.assertEquals("pass", config.getPassword());
    }

    @Test
    public void testParseAuthDataIgnoresComments() {
        char[] authData = "#username=user\n#password=pass\n".toCharArray();
        AgentStartupConfiguration config = new AgentStartupConfiguration();
        AgentConfigsUtils.parseAuthConfigFromData(authData, authData.length, config);
        Assert.assertNull(config.getUsername());
        Assert.assertNull(config.getPassword());
    }

    @Test
    public void testParseAuthDataIgnoresDataAfterLength() {
        char[] authData = "#username=user\n#password=pass\nusername=user\npassword=pass\n".toCharArray();
        AgentStartupConfiguration config = new AgentStartupConfiguration();
        AgentConfigsUtils.parseAuthConfigFromData(authData, 30, config);
        Assert.assertNull(config.getUsername());
        Assert.assertNull(config.getPassword());
    }
}

