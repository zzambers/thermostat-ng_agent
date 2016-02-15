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

package com.redhat.thermostat.setup.command.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.service.process.UNIXProcessHandler;
import com.redhat.thermostat.setup.command.internal.SetupCommand;
import com.redhat.thermostat.setup.command.internal.cli.CharArrayMatcher;
import com.redhat.thermostat.setup.command.internal.model.ThermostatSetup;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.utils.keyring.Keyring;

public class SetupCommandTest {

    private SetupCommand cmd;
    private CommandContext ctxt;
    private Arguments mockArgs;
    private Console console;
    private List<String> list = new ArrayList<>();
    private ByteArrayOutputStream outputBaos, errorBaos;
    private PrintStream output, error;
    private CommonPaths paths;
    private UNIXProcessHandler processHandler;
    private Launcher launcher;
    private Keyring keyring;
    private ThermostatSetup thermostatSetup;

    @Before
    public void setUp() {
        processHandler = mock(UNIXProcessHandler.class);
        paths = mock(CommonPaths.class);
        when(paths.getUserClientConfigurationFile()).thenReturn(mock(File.class));
        ctxt = mock(CommandContext.class);
        mockArgs = mock(Arguments.class);
        console = mock(Console.class);

        outputBaos = new ByteArrayOutputStream();
        output = new PrintStream(outputBaos);

        errorBaos = new ByteArrayOutputStream();
        error = new PrintStream(errorBaos);
        launcher = mock(Launcher.class);
        keyring = mock(Keyring.class);
        thermostatSetup = mock(ThermostatSetup.class);

        when(ctxt.getArguments()).thenReturn(mockArgs);
        when(ctxt.getConsole()).thenReturn(console);
        when(console.getError()).thenReturn(error);
        when(console.getOutput()).thenReturn(output);
        when(mockArgs.getNonOptionArguments()).thenReturn(list);
    }

    @Test
    public void testLookAndFeelIsSet() throws CommandException {
        final boolean[] isSet = {false};
        cmd = new SetupCommand() {
            @Override
            void setLookAndFeel() throws InvocationTargetException, InterruptedException {
                isSet[0] = true;
            }

            @Override
            void createMainWindowAndRun(ThermostatSetup setup) {
                //do nothing
            }
        };

        setServices();
        cmd.run(ctxt);

        assertTrue(isSet[0]);
    }

    @Test
    public void testCreateMainWindowIsCalled() throws CommandException {
        final boolean[] isSet = {false};
        cmd = new SetupCommand() {
            @Override
            void setLookAndFeel() throws InvocationTargetException, InterruptedException {
                //do nothing
            }

            @Override
            void createMainWindowAndRun(ThermostatSetup setup) {
                isSet[0] = true;
            }
        };

        setServices();
        cmd.run(ctxt);

        assertTrue(isSet[0]);
    }


    @Test
    public void testPathsNotSetFailure() {
        cmd = createSetupCommand();

        try {
            cmd.run(ctxt);
            fail();
        } catch (CommandException e) {
            assertTrue(e.getMessage().contains("CommonPaths service dependency unavailable"));
        }
    }
    
    @Test
    public void testLauncherNotSetFailure() {
        cmd = createSetupCommand();
        
        cmd.setPaths(mock(CommonPaths.class));
        try {
            cmd.run(ctxt);
            fail();
        } catch (CommandException e) {
            assertTrue(e.getMessage().contains("Launcher service dependency unavailable"));
        }
    }
    
    @Test
    public void verifyOriginalCommandRunsAfterSetup() throws CommandException {
        doTestOriginalCmdRunsAfterSetup("web-storage-service", new String[] {
           "web-storage-service"     
        });
    }
    
    @Test
    public void verifyOriginalCommandRunsAfterSetup2() throws CommandException {
        doTestOriginalCmdRunsAfterSetup("list-vms|||--dbUrl=mongodb://127.0.0.1:25718", new String[] {
           "list-vms", "--dbUrl=mongodb://127.0.0.1:25718"     
        });
    }
    
    @Test
    public void verifySetupAsOrigCommandDoesNotRunAgain() throws CommandException {
        cmd = createSetupCommand();
        setServices();
        
        Arguments args = mock(Arguments.class);
        CommandContext ctxt = mock(CommandContext.class);
        when(ctxt.getArguments()).thenReturn(args);
        when(args.hasArgument("origArgs")).thenReturn(true);
        when(args.getArgument("origArgs")).thenReturn("setup");
        
        cmd.run(ctxt);
        verify(launcher, times(0)).run(argThat(new ArgsMatcher(new String[] { "setup" })), eq(false));
    }
    
    @Test
    public void verifySetupAsOrigCommandNonGui() throws CommandException {
        cmd = createSetupCommand();
        setServices();
        
        Arguments args = mock(Arguments.class);
        CommandContext ctxt = mock(CommandContext.class);
        when(ctxt.getArguments()).thenReturn(args);
        when(args.hasArgument("origArgs")).thenReturn(true);
        when(args.getArgument("origArgs")).thenReturn("setup|||-c");
        when(ctxt.getConsole()).thenReturn(console);
        when(thermostatSetup.isWebAppInstalled()).thenReturn(true);
        when(console.getInput())
            .thenReturn(new ByteArrayInputStream("yes\nmongo\nm\nm\nclient\nc\nc\nagent\na\na\n".getBytes()));
        
        cmd.run(ctxt);
        verify(thermostatSetup).createAgentUser(eq("agent"), argThat(matchesPassword(new char[] { 'a' })));
        verify(thermostatSetup).createClientAdminUser(eq("client"), argThat(matchesPassword(new char[] { 'c' })));
        verify(thermostatSetup).createMongodbUser(eq("mongo"), argThat(matchesPassword(new char[] { 'm' })));
        verify(launcher, times(0)).run(argThat(new ArgsMatcher(new String[] { "setup", "-c" })), eq(false));
    }
    
    private CharArrayMatcher matchesPassword(char[] expected) {
        return new CharArrayMatcher(expected);
    }
    
    private void doTestOriginalCmdRunsAfterSetup(String origArgs, String[] argsList) throws CommandException {
        cmd = createSetupCommand();
        setServices();
        
        Arguments args = mock(Arguments.class);
        CommandContext ctxt = mock(CommandContext.class);
        when(ctxt.getArguments()).thenReturn(args);
        when(args.hasArgument("origArgs")).thenReturn(true);
        when(args.getArgument("origArgs")).thenReturn(origArgs);
        
        cmd.run(ctxt);
        verify(launcher).run(argThat(new ArgsMatcher(argsList)), eq(false));
    }
    
    @Test
    public void testKeyringNotSetFailure() {
        cmd = createSetupCommand();
        
        cmd.setPaths(mock(CommonPaths.class));
        cmd.setLauncher(mock(Launcher.class));
        try {
            cmd.run(ctxt);
            fail();
        } catch (CommandException e) {
            assertTrue(e.getMessage().contains("Keyring service dependency unavailable"));
        }
    }
    
    private SetupCommand createSetupCommand() {
        return new SetupCommand() {
            @Override
            void setLookAndFeel() throws InvocationTargetException, InterruptedException {
                //do nothing
            }

            @Override
            void createMainWindowAndRun(ThermostatSetup setup) {
                //do nothing
            }
            
            @Override
            ThermostatSetup createSetup() {
                return thermostatSetup;
            }
        };
    }
    
    @Test
    public void mergedSetupArgumentsTest() {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument("nonGui")).thenReturn(false);
        String[] origArgs = new String[] { "-c", "--nonGui" };
        SetupCommand.MergedSetupArguments arguments = new SetupCommand.MergedSetupArguments(args, origArgs);
        assertTrue(arguments.hasArgument("nonGui"));
        // short version of nonGui
        origArgs = new String[] { "-c" };
        arguments = new SetupCommand.MergedSetupArguments(args, origArgs);
        assertTrue(arguments.hasArgument("nonGui"));
        // long version
        origArgs = new String[] { "--nonGui" };
        arguments = new SetupCommand.MergedSetupArguments(args, origArgs);
        assertTrue(arguments.hasArgument("nonGui"));
        
        // unrelated option
        when(args.getArgument("something")).thenReturn(null);
        origArgs = new String[] { "--something", "someVal", "-c" };
        arguments = new SetupCommand.MergedSetupArguments(args, origArgs);
        assertEquals("someVal", arguments.getArgument("something"));
        assertTrue(arguments.hasArgument("nonGui"));
    }
    
    private void setServices() {
        cmd.setPaths(paths);
        cmd.setLauncher(launcher);
        cmd.setKeyring(keyring);
        cmd.setProcessHandler(processHandler);
    }
    
    private static class ArgsMatcher extends BaseMatcher<String[]> {

        private final String[] expected;
        
        private ArgsMatcher(String[] expected) {
            this.expected = expected;
        }
        
        @Override
        public boolean matches(Object arg0) {
            if (arg0 == null || arg0.getClass() != String[].class) {
                return false;
            }
            String[] other = (String[])arg0;
            if (other.length != expected.length) {
                return false;
            }
            boolean matches = true;
            for (int i = 0; i < expected.length; i++) {
                matches = matches && Objects.equals(expected[i], other[i]);
            }
            return matches;
        }

        @Override
        public void describeTo(Description arg0) {
            arg0.appendText(Arrays.asList(expected).toString());
        }
        
    }

}

