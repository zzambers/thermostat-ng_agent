/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.vm.heap.analysis.command.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.TimeZone;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.thermostat.client.cli.AgentArgument;
import com.redhat.thermostat.client.cli.VmArgument;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.internal.test.TestCommandContextFactory;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.testutils.StubBundleContext;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;

public class ListHeapDumpsCommandTest {

    private static TimeZone defaultTimezone;

    @BeforeClass
    public static void setUpClass() {
        defaultTimezone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterClass
    public static void tearDownClass() {
        TimeZone.setDefault(defaultTimezone);
    }

    @Test
    public void verifyFailsWithoutHostDao() throws Exception {
        StubBundleContext context = new StubBundleContext();
        Command command = new ListHeapDumpsCommand(context);
        
        TestCommandContextFactory factory = new TestCommandContextFactory();
        try {
            command.run(factory.createContext(new SimpleArguments()));
            fail();
        } catch (CommandException agentDaoNotAvailableException) {
            assertEquals("Unable to access agent information (AgentInfoDAO unavailable)",
                    agentDaoNotAvailableException.getMessage());
        }
    }

    @Test
    public void verifyWorksWithoutAnyInformation() throws CommandException {
        AgentInfoDAO agentInfoDAO = mock(AgentInfoDAO.class);
        VmInfoDAO vmInfo = mock(VmInfoDAO.class);
        HeapDAO heapDao = mock(HeapDAO.class);

        StubBundleContext context = new StubBundleContext();
        context.registerService(AgentInfoDAO.class, agentInfoDAO, null);
        context.registerService(VmInfoDAO.class, vmInfo, null);
        context.registerService(HeapDAO.class, heapDao, null);

        Command command = new ListHeapDumpsCommand(context);
        TestCommandContextFactory factory = new TestCommandContextFactory();
        command.run(factory.createContext(new SimpleArguments()));
        assertEquals("AGENT ID VM ID HEAP ID TIMESTAMP\n", factory.getOutput());
    }

    @Test
    public void verifyWorks() throws CommandException {
        AgentId agentId = new AgentId("host-id");
        VmId vmId = new VmId("1");

        HeapInfo heapInfo = new HeapInfo();
        Calendar timestamp = Calendar.getInstance();
        timestamp.set(2012, 5, 7, 15, 32, 0);
        heapInfo.setTimeStamp(timestamp.getTimeInMillis());
        heapInfo.setHeapId("0001");
        heapInfo.setVmId(vmId.get());
        heapInfo.setAgentId(agentId.get());

        HeapDAO heapDao = mock(HeapDAO.class);

        VmInfoDAO vmInfoDAO = mock(VmInfoDAO.class);
        when(vmInfoDAO.getVmIds(agentId)).thenReturn(Collections.singleton(vmId));

        AgentInfoDAO agentInfoDAO = mock(AgentInfoDAO.class);
        when(agentInfoDAO.getAgentIds()).thenReturn(Collections.singleton(agentId));

        when(heapDao.getAllHeapInfo(agentId, vmId)).thenReturn(Arrays.asList(heapInfo));

        StubBundleContext context = new StubBundleContext();
        context.registerService(AgentInfoDAO.class, agentInfoDAO, null);
        context.registerService(VmInfoDAO.class, vmInfoDAO, null);
        context.registerService(HeapDAO.class, heapDao, null);

        Command command = new ListHeapDumpsCommand(context);
        TestCommandContextFactory factory = new TestCommandContextFactory();
        command.run(factory.createContext(new SimpleArguments()));

        String expected = "AGENT ID VM ID HEAP ID TIMESTAMP\n" +
                          "host-id  1     0001    Thu Jun 07 15:32:00 UTC 2012\n";

        assertEquals(expected, factory.getOutput());
    }

    @Test
    public void verifyWorksWithFilterOnHost() throws CommandException {
        AgentId agentId1 = new AgentId("host1");
        VmId vmId1 = new VmId("1");

        AgentId agentId2 = new AgentId("host2");
        VmId vmId2 = new VmId("2");

        HeapInfo heapInfo = new HeapInfo();
        Calendar timestamp = Calendar.getInstance();
        timestamp.set(2012, 5, 7, 15, 32, 0);
        heapInfo.setTimeStamp(timestamp.getTimeInMillis());
        heapInfo.setHeapId("0001");
        heapInfo.setVmId(vmId1.get());
        heapInfo.setAgentId(agentId1.get());

        HeapDAO heapDao = mock(HeapDAO.class);

        VmInfoDAO vmInfo = mock(VmInfoDAO.class);
        when(vmInfo.getVmIds(agentId1)).thenReturn(Collections.singleton(vmId1)).thenReturn(Collections.singleton(vmId2));

        AgentInfoDAO agentInfoDAO = mock(AgentInfoDAO.class);
        when(agentInfoDAO.getAgentIds()).thenReturn(new HashSet<>(Arrays.asList(agentId1, agentId2)));

        when(heapDao.getAllHeapInfo(agentId1, vmId1)).thenReturn(Arrays.asList(heapInfo));
        when(heapDao.getAllHeapInfo(agentId2, vmId2)).thenReturn(Arrays.asList(heapInfo));

        StubBundleContext context = new StubBundleContext();
        context.registerService(AgentInfoDAO.class, agentInfoDAO, null);
        context.registerService(VmInfoDAO.class, vmInfo, null);
        context.registerService(HeapDAO.class, heapDao, null);

        Command command = new ListHeapDumpsCommand(context);
        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();
        args.addArgument(AgentArgument.ARGUMENT_NAME, "host1");

        command.run(factory.createContext(args));

        String expected = "AGENT ID VM ID HEAP ID TIMESTAMP\n" +
                          "host1    1     0001    Thu Jun 07 15:32:00 UTC 2012\n";

        assertEquals(expected, factory.getOutput());
    }

    @Test
    public void verifyWorksWithFilterOnHostAndVM() throws CommandException {
        AgentId agentId1 = new AgentId("host1");
        VmId vmId1 = new VmId("1");

        AgentId agentId2 = new AgentId("host2");
        VmId vmId2 = new VmId("2");

        HeapInfo heapInfo = new HeapInfo();
        Calendar timestamp = Calendar.getInstance();
        timestamp.set(2012, 5, 7, 15, 32, 0);
        heapInfo.setTimeStamp(timestamp.getTimeInMillis());
        heapInfo.setHeapId("0001");
        heapInfo.setVmId(vmId1.get());
        heapInfo.setAgentId(agentId1.get());

        HeapDAO heapDao = mock(HeapDAO.class);
        VmInfoDAO vmInfoDAO = mock(VmInfoDAO.class);

        VmInfo vmInfo1 = new VmInfo("host1", "1", 123, 0, 0, null, null, null, null, null, null, null, null, null,
                null, null,0, "myUsername");
        when(vmInfoDAO.getVmInfo(vmId1)).thenReturn(vmInfo1);

        AgentInfoDAO agentInfoDAO = mock(AgentInfoDAO.class);
        when(agentInfoDAO.getAgentIds()).thenReturn(new HashSet<>(Arrays.asList(agentId1, agentId2)));

        when(heapDao.getAllHeapInfo(agentId1, vmId1)).thenReturn(Arrays.asList(heapInfo));

        StubBundleContext context = new StubBundleContext();
        context.registerService(AgentInfoDAO.class, agentInfoDAO, null);
        context.registerService(VmInfoDAO.class, vmInfoDAO, null);
        context.registerService(HeapDAO.class, heapDao, null);

        Command command = new ListHeapDumpsCommand(context);
        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();
        args.addArgument(AgentArgument.ARGUMENT_NAME, "host1");
        args.addArgument(VmArgument.ARGUMENT_NAME, "1");

        command.run(factory.createContext(args));

        String expected = "AGENT ID VM ID HEAP ID TIMESTAMP\n" +
                          "host1    1     0001    Thu Jun 07 15:32:00 UTC 2012\n";

        assertEquals(expected, factory.getOutput());
    }
}

