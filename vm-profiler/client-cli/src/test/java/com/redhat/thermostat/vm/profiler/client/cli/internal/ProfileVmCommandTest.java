/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.vm.profiler.client.cli.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.client.cli.internal.LocaleResources;
import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.test.TestCommandContextFactory;
import com.redhat.thermostat.vm.profiler.common.ProfileDAO;
import com.redhat.thermostat.vm.profiler.common.ProfileStatusChange;

public class ProfileVmCommandTest {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final String AGENT_ID = "some-agent";
    private static final String VM_ID = "some-vm";
    private static final long SOME_TIMESTAMP = 99;

    /* taken from HostVMArguments: */
    private static final HostRef AGENT = new HostRef(AGENT_ID, "dummy");
    private static final VmRef VM = new VmRef(AGENT, VM_ID, -1, "dummy");

    private TestCommandContextFactory cmdCtxFactory;

    private AgentInfoDAO agentsDao;
    private VmInfoDAO vmsDao;
    private RequestQueue queue;
    private ProfileDAO profileDao;

    private ProfileVmCommand cmd;

    @Before
    public void setUp() {
        cmdCtxFactory = new TestCommandContextFactory();

        agentsDao = mock(AgentInfoDAO.class);
        vmsDao = mock(VmInfoDAO.class);
        queue = mock(RequestQueue.class);
        profileDao = mock(ProfileDAO.class);

        cmd = new ProfileVmCommand();
        cmd.setAgentInfoDAO(agentsDao);
        cmd.setVmInfoDAO(vmsDao);
        cmd.setRequestQueue(queue);
        cmd.setProfileDAO(profileDao);
    }

    @Test (expected=CommandException.class)
    public void needsSubCommand() throws Exception {
        SimpleArguments args = new SimpleArguments();
        args.addArgument("hostId", AGENT_ID);
        args.addArgument("vmId", VM_ID);
        CommandContext ctx = cmdCtxFactory.createContext(args);

        cmd.run(ctx);
    }

    @Test
    public void statusSubCommandShowsNotProfilingWhenNoInformationAvailable() throws Exception {
        AgentInformation agentInfo = mock(AgentInformation.class);
        when(agentsDao.getAgentInformation(AGENT)).thenReturn(agentInfo);

        SimpleArguments args = new SimpleArguments();
        args.addArgument("hostId", AGENT_ID);
        args.addArgument("vmId", VM_ID);
        args.addNonOptionArgument("status");
        CommandContext ctx = cmdCtxFactory.createContext(args);

        cmd.run(ctx);

        assertEquals("Currently profiling: No\n", cmdCtxFactory.getOutput());
    }

    @Test
    public void statusSubCommandShowsCurrentProfilingStatus() throws Exception {
        AgentInformation agentInfo = mock(AgentInformation.class);
        when(agentsDao.getAgentInformation(AGENT)).thenReturn(agentInfo);

        ProfileStatusChange status = new ProfileStatusChange(AGENT_ID, VM_ID, SOME_TIMESTAMP, true);
        when(profileDao.getLatestStatus(VM)).thenReturn(status);

        SimpleArguments args = new SimpleArguments();
        args.addArgument("hostId", AGENT_ID);
        args.addArgument("vmId", VM_ID);
        args.addNonOptionArgument("status");
        CommandContext ctx = cmdCtxFactory.createContext(args);

        cmd.run(ctx);

        assertEquals("Currently profiling: Yes\n", cmdCtxFactory.getOutput());
    }
}
