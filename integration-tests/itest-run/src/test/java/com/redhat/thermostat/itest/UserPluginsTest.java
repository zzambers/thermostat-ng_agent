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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import expectj.Spawn;

/**
 * Tests plugins installing to the system location.
 *
 */
public class UserPluginsTest extends PluginTest {

    private static NewCommandPlugin userPlugin = new NewCommandPlugin(
            "user",
            "a plugin that is provided by the user",
            "a plugin that is provided by the user",
            USER_PLUGIN_INSTALL_LOCATION);
    private static UnknownExtendsPlugin unknownExtension = new UnknownExtendsPlugin(USER_PLUGIN_HOME + File.separator + "unknown");

    @BeforeClass
    public static void setUpOnce() {
        userPlugin.install();
        unknownExtension.install();
    }
    
    @Before
    public void setup() {
        createFakeSetupCompleteFile();
    }

    @AfterClass
    public static void tearDownOnce() {
        unknownExtension.uninstall();
        userPlugin.uninstall();
    }
    
    @After
    public void tearDown() throws IOException {
        removeSetupCompleteStampFiles();
    }

    @Test
    public void testHelpIsOkay() throws Exception {
        Spawn shell = spawnThermostat("help");
        shell.expectClose();

        String stdOut = shell.getCurrentStandardOutContents();

        assertTrue(stdOut.contains("list of commands"));
        assertTrue(stdOut.contains("help"));
        assertTrue(stdOut.contains("agent"));
        assertTrue(stdOut.contains("gui"));
        assertTrue(stdOut.contains("ping"));
        assertTrue(stdOut.contains("shell"));


        assertTrue(stdOut.contains(userPlugin.getCommandName()));

        assertFalse(stdOut.contains(unknownExtension.getCommandName()));
    }
}
