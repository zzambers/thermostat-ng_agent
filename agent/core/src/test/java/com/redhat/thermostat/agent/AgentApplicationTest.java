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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.ArgumentSpec;
import com.redhat.thermostat.common.cli.SimpleArgumentSpec;

public class AgentApplicationTest {

    // TODO: Test i18nized versions when they come.

    private AgentApplication agent;

    @Before
    public void setUp() {
        agent = new AgentApplication();
    }

    @After
    public void tearDown() {
        agent = null;
    }

    @Test
    public void testName() {
        String name = agent.getName();
        assertEquals("agent", name);
    }

    @Test
    public void testDescription() {
        String description = agent.getDescription();
        assertEquals("starts and stops the thermostat agent", description);
    }

    @Test
    public void testUsage() {
        String usage = agent.getUsage();
        assertEquals("starts and stops the thermostat agent", usage);
    }

    @Test
    public void testAcceptedArguments() {
        Collection<ArgumentSpec> args = agent.getAcceptedArguments();
        assertNotNull(args);
        assertEquals(5, args.size());
        assertTrue(args.contains(new SimpleArgumentSpec("saveOnExit", "s", "save the data on exit", false, false)));
        assertTrue(args.contains(new SimpleArgumentSpec("debug", "launch with debug console enabled")));
        assertTrue(args.contains(new SimpleArgumentSpec("dbUrl", "d", "connect to the given url", true, true)));
        assertTrue(args.contains(new SimpleArgumentSpec("username", "the username to use for authentication", false, true)));
        assertTrue(args.contains(new SimpleArgumentSpec("password", "the password to use for authentication", false, true)));
    }
}
