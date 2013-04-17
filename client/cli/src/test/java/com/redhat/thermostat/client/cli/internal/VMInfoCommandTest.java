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

package com.redhat.thermostat.client.cli.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.DAOException;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.test.Bug;
import com.redhat.thermostat.test.TestCommandContextFactory;
import com.redhat.thermostat.testutils.StubBundleContext;

public class VMInfoCommandTest {
    
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
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

    private VMInfoCommand cmd;
    private VmInfoDAO vmsDAO;
    private TestCommandContextFactory cmdCtxFactory;
    private VmRef vm;
    private StubBundleContext context;

    @Before
    public void setUp() {
        context = new StubBundleContext();
        setupCommandContextFactory();

        vmsDAO = mock(VmInfoDAO.class);
        setupDAOs();
    }

    private void setupCommandContextFactory() {
        cmdCtxFactory = new TestCommandContextFactory();
    }

    private void setupDAOs() {
        HostRef host = new HostRef("123", "dummy");
        vm = new VmRef(host, 234, "dummy");
        Calendar start = Calendar.getInstance();
        start.set(2012, 5, 7, 15, 32, 0);
        Calendar end = Calendar.getInstance();
        end.set(2013, 10, 1, 1, 22, 0);
        VmInfo vmInfo = new VmInfo(234, start.getTimeInMillis(), end.getTimeInMillis(), "vmVersion", "javaHome", "mainClass", "commandLine", "vmName", "vmInfo", "vmVersion", "vmArguments", new HashMap<String,String>(), new HashMap<String,String>(), new String[0]);
        when(vmsDAO.getVmInfo(vm)).thenReturn(vmInfo);
        when(vmsDAO.getVmInfo(new VmRef(host, 9876, "dummy"))).thenThrow(new DAOException("Unknown VM ID: 9876"));
        when(vmsDAO.getVMs(host)).thenReturn(Arrays.asList(vm));
    }


    @Test
    public void testVmInfo() throws CommandException {
        context.registerService(VmInfoDAO.class, vmsDAO, null);
        cmd = new VMInfoCommand(context);
        SimpleArguments args = new SimpleArguments();
        args.addArgument("vmId", "234");
        args.addArgument("hostId", "123");
        cmd.run(cmdCtxFactory.createContext(args));
        String expected = "Process ID:      234\n" +
                          "Start time:      Thu Jun 07 15:32:00 UTC 2012\n" +
                          "Stop time:       Fri Nov 01 01:22:00 UTC 2013\n" +
                          "Main class:      mainClass\n" +
                          "Command line:    commandLine\n" +
                          "Java version:    vmVersion\n" +
                          "Virtual machine: vmName\n" +
                          "VM arguments:    vmArguments\n";
        assertEquals(expected, cmdCtxFactory.getOutput());
    }
    
    @Test
    public void testNoVmInfoDAO() throws CommandException {
        cmd = new VMInfoCommand(context);
        SimpleArguments args = new SimpleArguments();
        args.addArgument("vmId", "234");
        args.addArgument("hostId", "123");
        
        try {
            cmd.run(cmdCtxFactory.createContext(args));
            fail();
        } catch (CommandException e) {
            assertEquals(translator.localize(LocaleResources.VM_SERVICE_UNAVAILABLE), e.getMessage());
        }
    }

    @Test
    public void testAllVmInfoForHost() throws CommandException {
        context.registerService(VmInfoDAO.class, vmsDAO, null);
        cmd = new VMInfoCommand(context);
        SimpleArguments args = new SimpleArguments();
        args.addArgument("hostId", "123");
        cmd.run(cmdCtxFactory.createContext(args));
        String expected = "Process ID:      234\n" +
                          "Start time:      Thu Jun 07 15:32:00 UTC 2012\n" +
                          "Stop time:       Fri Nov 01 01:22:00 UTC 2013\n" +
                          "Main class:      mainClass\n" +
                          "Command line:    commandLine\n" +
                          "Java version:    vmVersion\n" +
                          "Virtual machine: vmName\n" +
                          "VM arguments:    vmArguments\n";
        assertEquals(expected, cmdCtxFactory.getOutput());
    }

    @Test
    public void testVmInfoUnknownVM() throws CommandException {
        context.registerService(VmInfoDAO.class, vmsDAO, null);
        cmd = new VMInfoCommand(context);
        SimpleArguments args = new SimpleArguments();
        args.addArgument("vmId", "9876");
        args.addArgument("hostId", "123");
        cmd.run(cmdCtxFactory.createContext(args));
        String expected = "Unknown VM ID: 9876\n";
        assertEquals("", cmdCtxFactory.getOutput());
        assertEquals(expected, cmdCtxFactory.getError());
    }

    @Test
    public void testVmInfoNonNumericalVMID() throws CommandException {
        context.registerService(VmInfoDAO.class, vmsDAO, null);
        cmd = new VMInfoCommand(context);
        SimpleArguments args = new SimpleArguments();
        args.addArgument("vmId", "fluff");
        args.addArgument("hostId", "123");
        try {
            cmd.run(cmdCtxFactory.createContext(args));
        } catch (CommandException ex) {
            String expected = "Invalid VM ID: fluff";
            assertEquals(expected, ex.getMessage());
        }
    }

    @Bug(id="1046",
            summary="CLI vm-info display wrong stop time for living vms",
            url="http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=1046")
    @Test
    public void testStopTime() throws CommandException {
        context.registerService(VmInfoDAO.class, vmsDAO, null);
        cmd = new VMInfoCommand(context);
        Calendar start = Calendar.getInstance();
        start.set(2012, 5, 7, 15, 32, 0);
        VmInfo vmInfo = new VmInfo(234, start.getTimeInMillis(), Long.MIN_VALUE, "vmVersion", "javaHome", "mainClass", "commandLine", "vmName", "vmInfo", "vmVersion", "vmArguments", new HashMap<String,String>(), new HashMap<String,String>(), new String[0]);
        when(vmsDAO.getVmInfo(vm)).thenReturn(vmInfo);

        SimpleArguments args = new SimpleArguments();
        args.addArgument("vmId", "234");
        args.addArgument("hostId", "123");
        cmd.run(cmdCtxFactory.createContext(args));
        String expected = "Process ID:      234\n" +
                          "Start time:      Thu Jun 07 15:32:00 UTC 2012\n" +
                          "Stop time:       <Running>\n" +
                          "Main class:      mainClass\n" +
                          "Command line:    commandLine\n" +
                          "Java version:    vmVersion\n" +
                          "Virtual machine: vmName\n" +
                          "VM arguments:    vmArguments\n";
        assertEquals(expected, cmdCtxFactory.getOutput());
    }

    @Test
    public void testStorageRequired() {
        context.registerService(VmInfoDAO.class, vmsDAO, null);
        cmd = new VMInfoCommand(context);
        assertTrue(cmd.isStorageRequired());
    }
}

