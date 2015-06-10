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

package com.redhat.thermostat.itest;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import expectj.Spawn;

public class DevWebStorageTest extends IntegrationTest {
    
    private static final String THERMOSTAT_DEV_SETUP_SCRIPT = "thermostat-devsetup";

    @Before
    public void setUp() throws Exception {
        clearStorageDataDirectory();
    }

    
    @After
    public void tearDown() throws IOException {
        removeSetupCompleteStampFiles();
        removeAgentAuthFile();

        clearStorageDataDirectory();
    }
    
    private static void removeAgentAuthFile() throws IOException {
        File agentAuth = new File(getUserThermostatHome() , "etc/agent.auth");
        try {
            Files.delete(agentAuth.toPath());
        } catch (NoSuchFileException e) {
            // this is what we wanted, so ignore it.
        }
    }

    @Test
    public void canRunWebStorageService() throws Exception {
        if (isDevelopmentBuild()) {
            // test setup
            removeSetupCompleteStampFiles();
            
            // run the test methods
            runThermostatSetup();
            runWebStorageTest();
        }
        // otherwise service is not build so don't run the
        // test.
    }

    private void runThermostatSetup() throws Exception {
        Spawn setup = spawnScript(THERMOSTAT_DEV_SETUP_SCRIPT, new String[] {});
        setup.expectClose();
        String stdout = setup.getCurrentStandardOutContents();
        // cp -v of thermostat-devsetup should produce this
        assertTrue("was agent.auth copied?", stdout.contains("distribution/target/user-home/etc/agent.auth"));
        assertTrue(stdout.contains("Thermostat setup complete!"));
        assertTrue(stdout.contains("mongodevuser"));
    }

    private void runWebStorageTest() throws Exception {
        Map<String, String> testProperties = getVerboseModeProperties();
        SpawnResult spawnResult = spawnThermostatWithPropertiesSetAndGetProcess(testProperties, "web-storage-service");
        Spawn service = spawnResult.spawn;

        try {
            service.expect("Agent started.");
        } finally {
            // service.stop only stops the agent/webservice.
            killRecursively(spawnResult.process);
            try {
                // On Eclipse IDE runs this recursive killing does not kill
                // mongod. Do it this way to be really sure.
                stopStorage();
            } catch (Exception e) {
                // ignore if second try of stopping storage failed.
            }
        }

        service.expectClose();
    }
}
